package com.lecturebrief.service

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import androidx.compose.runtime.Immutable
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.lecturebrief.LectureBriefApplication
import com.lecturebrief.MainActivity
import com.lecturebrief.data.local.db.entity.AudioSourceMode
import com.lecturebrief.data.local.db.entity.ProcessingStatus
import com.lecturebrief.data.repository.RecordingRepository
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.sqrt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class RecordingStatus {
    IDLE,
    RECORDING,
    PAUSED
}

@Immutable
data class RecordingState(
    val status: RecordingStatus = RecordingStatus.IDLE,
    val sourceMode: AudioSourceMode = AudioSourceMode.MICROPHONE_ONLY,
    val elapsedSeconds: Long = 0L,
    val normalizedAmplitude: Float = 0f,
    val activeRecordingId: Long? = null,
    val outputFilePath: String? = null,
    val errorMessage: String? = null
)

class AudioRecorderService : Service() {

    inner class LocalBinder : Binder() {
        fun getService(): AudioRecorderService = this@AudioRecorderService
    }

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val recordingRepository: RecordingRepository by lazy {
        (application as LectureBriefApplication).appContainer.recordingRepository
    }

    // Mic-only MediaRecorder path (.m4a)
    private var mediaRecorder: MediaRecorder? = null

    // Internal / Mic+Internal AudioRecord + MediaProjection path (.wav)
    private var mediaProjection: MediaProjection? = null
    private var projectionCallback: MediaProjection.Callback? = null
    private var internalAudioRecord: AudioRecord? = null
    private var micAudioRecord: AudioRecord? = null
    private var pcmCaptureJob: Job? = null
    @Volatile
    private var isCapturePaused: Boolean = false
    @Volatile
    private var latestPcmAmplitude: Float = 0f
    private var totalPcmBytesWritten: Long = 0L

    private var currentSourceMode: AudioSourceMode = AudioSourceMode.MICROPHONE_ONLY
    private var outputFile: File? = null
    private var activeRecordingId: Long? = null

    private var tickerJob: Job? = null
    private var activeSegmentStartMs: Long = 0L
    private var accumulatedActiveMs: Long = 0L
    private var lastNotifiedSecond: Long = -1L

    val stateFlow: StateFlow<RecordingState>
        get() = _recordingState.asStateFlow()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startRecordingFromIntent(intent)
            ACTION_PAUSE -> pauseRecordingInternal()
            ACTION_RESUME -> resumeRecordingInternal()
            ACTION_STOP -> stopRecordingInternal()
        }
        return START_NOT_STICKY
    }

    private fun startRecordingFromIntent(intent: Intent) {
        if (_recordingState.value.status != RecordingStatus.IDLE) return

        val modeName = intent.getStringExtra(EXTRA_AUDIO_SOURCE_MODE)
        val sourceMode = runCatching { AudioSourceMode.valueOf(modeName ?: "") }
            .getOrDefault(AudioSourceMode.MICROPHONE_ONLY)

        val folderIdExtra = intent.getLongExtra(EXTRA_FOLDER_ID, -1L)
        val initialFolderId: Long? = folderIdExtra.takeIf { it > 0L }

        val projectionResultCode = intent.getIntExtra(EXTRA_PROJECTION_RESULT_CODE, Activity.RESULT_CANCELED)
        val projectionData: Intent? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_PROJECTION_DATA, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_PROJECTION_DATA)
        }

        if (sourceMode != AudioSourceMode.MICROPHONE_ONLY &&
            (projectionResultCode != Activity.RESULT_OK || projectionData == null)
        ) {
            _recordingState.value = RecordingState(
                status = RecordingStatus.IDLE,
                sourceMode = sourceMode,
                errorMessage = "Internal audio capture permission was not granted."
            )
            stopSelf()
            return
        }

        currentSourceMode = sourceMode
        isCapturePaused = false
        latestPcmAmplitude = 0f
        totalPcmBytesWritten = 0L

        val recordingsDir = File(filesDir, RECORDINGS_DIR_NAME).apply {
            if (!exists()) mkdirs()
        }
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val extension = if (sourceMode == AudioSourceMode.MICROPHONE_ONLY) "m4a" else "wav"
        val file = File(recordingsDir, "lecture_$timestamp.$extension")
        val title = "Lecture - ${SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date())}"

        val initialState = RecordingState(
            status = RecordingStatus.RECORDING,
            sourceMode = sourceMode,
            elapsedSeconds = 0L,
            normalizedAmplitude = 0f,
            outputFilePath = file.absolutePath
        )

        // Android 14+ requires startForeground() with the exact foregroundServiceType BEFORE
        // calling MediaProjectionManager.getMediaProjection().
        val fgsType = when (sourceMode) {
            AudioSourceMode.MICROPHONE_ONLY -> ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            AudioSourceMode.INTERNAL_ONLY -> ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            AudioSourceMode.MIC_AND_INTERNAL ->
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE or ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
        }

        try {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                buildNotification(initialState),
                fgsType
            )
        } catch (e: SecurityException) {
            _recordingState.value = RecordingState(
                status = RecordingStatus.IDLE,
                sourceMode = sourceMode,
                errorMessage = e.message ?: "Missing foreground service permission."
            )
            stopSelf()
            return
        }

        val started = if (sourceMode == AudioSourceMode.MICROPHONE_ONLY) {
            startMicOnlyMediaRecorder(file)
        } else {
            startAudioPlaybackCapture(
                file = file,
                sourceMode = sourceMode,
                resultCode = projectionResultCode,
                projectionData = projectionData!!
            )
        }

        if (!started) {
            runCatching { file.delete() }
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf()
            return
        }

        outputFile = file
        accumulatedActiveMs = 0L
        activeSegmentStartMs = SystemClock.elapsedRealtime()
        lastNotifiedSecond = 0L
        _recordingState.value = initialState

        serviceScope.launch {
            val insertedId = recordingRepository.createRecordingEntry(
                title = title,
                audioFilePath = file.absolutePath,
                audioSource = sourceMode,
                folderId = initialFolderId,
                status = ProcessingStatus.RECORDING
            )
            activeRecordingId = insertedId
            _recordingState.update { it.copy(activeRecordingId = insertedId) }
        }

        startMeteringAndTimer()
    }

    private fun startMicOnlyMediaRecorder(file: File): Boolean {
        val recorder = createMediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioChannels(1)
            setAudioSamplingRate(SAMPLE_RATE_HZ)
            setAudioEncodingBitRate(BIT_RATE_BPS)
            setOutputFile(file.absolutePath)
        }

        return try {
            recorder.prepare()
            recorder.start()
            mediaRecorder = recorder
            true
        } catch (e: Exception) {
            runCatching { recorder.release() }
            _recordingState.value = RecordingState(
                status = RecordingStatus.IDLE,
                sourceMode = currentSourceMode,
                errorMessage = e.message ?: "Failed to start microphone recording"
            )
            false
        }
    }

    @SuppressLint("MissingPermission")
    private fun startAudioPlaybackCapture(
        file: File,
        sourceMode: AudioSourceMode,
        resultCode: Int,
        projectionData: Intent
    ): Boolean {
        val projectionManager = getSystemService(MediaProjectionManager::class.java)
        val projection = try {
            projectionManager.getMediaProjection(resultCode, projectionData)
        } catch (e: Exception) {
            _recordingState.value = RecordingState(
                status = RecordingStatus.IDLE,
                sourceMode = sourceMode,
                errorMessage = e.message ?: "Failed to obtain MediaProjection"
            )
            return false
        }

        if (projection == null) {
            _recordingState.value = RecordingState(
                status = RecordingStatus.IDLE,
                sourceMode = sourceMode,
                errorMessage = "MediaProjection was unavailable."
            )
            return false
        }

        val callback = object : MediaProjection.Callback() {
            override fun onStop() {
                serviceScope.launch {
                    if (_recordingState.value.status != RecordingStatus.IDLE) {
                        stopRecordingInternal()
                    }
                }
            }
        }
        projection.registerCallback(callback, Handler(Looper.getMainLooper()))
        mediaProjection = projection
        projectionCallback = callback

        val minBufferBytes = AudioRecord.getMinBufferSize(
            SAMPLE_RATE_HZ,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(PCM_CHUNK_SAMPLES * 2)
        val bufferBytes = minBufferBytes * 2

        val audioFormat = AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(SAMPLE_RATE_HZ)
            .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
            .build()

        val playbackConfig = AudioPlaybackCaptureConfiguration.Builder(projection)
            .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
            .addMatchingUsage(AudioAttributes.USAGE_GAME)
            .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
            .build()

        return try {
            val internalRecord = AudioRecord.Builder()
                .setAudioFormat(audioFormat)
                .setBufferSizeInBytes(bufferBytes)
                .setAudioPlaybackCaptureConfig(playbackConfig)
                .build()

            if (internalRecord.state != AudioRecord.STATE_INITIALIZED) {
                internalRecord.release()
                error("Failed to initialize AudioPlaybackCapture AudioRecord")
            }

            val micRecord = if (sourceMode == AudioSourceMode.MIC_AND_INTERNAL) {
                AudioRecord.Builder()
                    .setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION)
                    .setAudioFormat(audioFormat)
                    .setBufferSizeInBytes(bufferBytes)
                    .build()
                    .also { record ->
                        if (record.state != AudioRecord.STATE_INITIALIZED) {
                            record.release()
                            internalRecord.release()
                            error("Failed to initialize microphone AudioRecord")
                        }
                    }
            } else {
                null
            }

            internalAudioRecord = internalRecord
            micAudioRecord = micRecord

            internalRecord.startRecording()
            micRecord?.startRecording()

            pcmCaptureJob = serviceScope.launch(Dispatchers.IO) {
                writePcmStreamToWav(
                    file = file,
                    internalRecord = internalRecord,
                    micRecord = micRecord
                )
            }
            true
        } catch (e: Exception) {
            releasePcmResources()
            _recordingState.value = RecordingState(
                status = RecordingStatus.IDLE,
                sourceMode = sourceMode,
                errorMessage = e.message ?: "Failed to start internal audio capture"
            )
            false
        }
    }

    private fun writePcmStreamToWav(
        file: File,
        internalRecord: AudioRecord,
        micRecord: AudioRecord?
    ) {
        val internalBuffer = ShortArray(PCM_CHUNK_SAMPLES)
        val micBuffer = if (micRecord != null) ShortArray(PCM_CHUNK_SAMPLES) else null
        val byteBuffer = ByteBuffer.allocate(PCM_CHUNK_SAMPLES * 2).order(ByteOrder.LITTLE_ENDIAN)

        FileOutputStream(file).use { fos ->
            // Write placeholder 44-byte WAV header; updated with final sizes when stopped.
            fos.write(ByteArray(WAV_HEADER_SIZE))

            while (pcmCaptureJob?.isActive == true &&
                internalRecord.recordingState == AudioRecord.RECORDSTATE_RECORDING
            ) {
                val internalRead = internalRecord.read(internalBuffer, 0, internalBuffer.size)
                val micRead = if (micRecord != null && micBuffer != null) {
                    micRecord.read(micBuffer, 0, micBuffer.size)
                } else {
                    0
                }

                if (isCapturePaused) {
                    latestPcmAmplitude = 0f
                    continue
                }

                val sampleCount = if (micRecord != null) {
                    maxOf(internalRead.coerceAtLeast(0), micRead.coerceAtLeast(0))
                } else {
                    internalRead.coerceAtLeast(0)
                }

                if (sampleCount <= 0) continue

                byteBuffer.clear()
                var peak = 0

                for (i in 0 until sampleCount) {
                    val internalSample = if (i < internalRead) internalBuffer[i].toInt() else 0
                    val micSample = if (micBuffer != null && i < micRead) micBuffer[i].toInt() else 0
                    val mixed = (internalSample + micSample)
                        .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                    val sampleShort = mixed.toShort()
                    byteBuffer.putShort(sampleShort)

                    val absSample = abs(mixed)
                    if (absSample > peak) peak = absSample
                }

                val bytesToWrite = sampleCount * 2
                fos.write(byteBuffer.array(), 0, bytesToWrite)
                totalPcmBytesWritten += bytesToWrite
                latestPcmAmplitude = sqrt((peak / 32767f).coerceIn(0f, 1f))
            }
            fos.flush()
        }

        updateWavHeader(file, totalPcmBytesWritten)
    }

    private fun updateWavHeader(file: File, pcmDataBytes: Long) {
        if (!file.exists()) return
        runCatching {
            val totalDataLen = pcmDataBytes + 36
            val byteRate = SAMPLE_RATE_HZ * 1 * 16 / 8
            val header = ByteBuffer.allocate(WAV_HEADER_SIZE).order(ByteOrder.LITTLE_ENDIAN).apply {
                put("RIFF".toByteArray(Charsets.US_ASCII))
                putInt(totalDataLen.toInt())
                put("WAVE".toByteArray(Charsets.US_ASCII))
                put("fmt ".toByteArray(Charsets.US_ASCII))
                putInt(16) // PCM chunk size
                putShort(1) // AudioFormat = 1 (PCM)
                putShort(1) // NumChannels = 1 (Mono)
                putInt(SAMPLE_RATE_HZ)
                putInt(byteRate)
                putShort(2) // BlockAlign = NumChannels * BitsPerSample / 8
                putShort(16) // BitsPerSample
                put("data".toByteArray(Charsets.US_ASCII))
                putInt(pcmDataBytes.toInt())
            }.array()

            RandomAccessFile(file, "rw").use { raf ->
                raf.seek(0)
                raf.write(header)
            }
        }
    }

    private fun pauseRecordingInternal() {
        if (_recordingState.value.status != RecordingStatus.RECORDING) return

        if (currentSourceMode == AudioSourceMode.MICROPHONE_ONLY) {
            try {
                mediaRecorder?.pause()
            } catch (e: Exception) {
                _recordingState.update { it.copy(errorMessage = e.message ?: "Failed to pause recording") }
                return
            }
        } else {
            isCapturePaused = true
            latestPcmAmplitude = 0f
        }

        accumulatedActiveMs += (SystemClock.elapsedRealtime() - activeSegmentStartMs).coerceAtLeast(0L)
        tickerJob?.cancel()
        tickerJob = null

        val pausedState = _recordingState.value.copy(
            status = RecordingStatus.PAUSED,
            elapsedSeconds = accumulatedActiveMs / 1000L,
            normalizedAmplitude = 0f
        )
        _recordingState.value = pausedState
        updateNotification(pausedState)
    }

    private fun resumeRecordingInternal() {
        if (_recordingState.value.status != RecordingStatus.PAUSED) return

        if (currentSourceMode == AudioSourceMode.MICROPHONE_ONLY) {
            try {
                mediaRecorder?.resume()
            } catch (e: Exception) {
                _recordingState.update { it.copy(errorMessage = e.message ?: "Failed to resume recording") }
                return
            }
        } else {
            isCapturePaused = false
        }

        activeSegmentStartMs = SystemClock.elapsedRealtime()
        val resumedState = _recordingState.value.copy(
            status = RecordingStatus.RECORDING,
            errorMessage = null
        )
        _recordingState.value = resumedState
        updateNotification(resumedState)
        startMeteringAndTimer()
    }

    private fun stopRecordingInternal() {
        val currentStatus = _recordingState.value.status
        if (currentStatus == RecordingStatus.IDLE) {
            stopSelf()
            return
        }

        tickerJob?.cancel()
        tickerJob = null

        if (currentStatus == RecordingStatus.RECORDING) {
            accumulatedActiveMs += (SystemClock.elapsedRealtime() - activeSegmentStartMs).coerceAtLeast(0L)
        }
        val finalDurationMs = accumulatedActiveMs
        val recordedFile = outputFile
        val recordingId = activeRecordingId
        val wasMicOnly = currentSourceMode == AudioSourceMode.MICROPHONE_ONLY

        var stopSucceeded = true
        if (wasMicOnly) {
            try {
                mediaRecorder?.stop()
            } catch (_: RuntimeException) {
                stopSucceeded = false
            } finally {
                runCatching { mediaRecorder?.release() }
                mediaRecorder = null
            }
        } else {
            val captureJob = pcmCaptureJob
            pcmCaptureJob = null
            captureJob?.cancel()
            releasePcmResources()
        }

        outputFile = null
        activeRecordingId = null
        accumulatedActiveMs = 0L
        lastNotifiedSecond = -1L

        _recordingState.value = RecordingState(
            status = RecordingStatus.IDLE,
            sourceMode = currentSourceMode
        )

        serviceScope.launch {
            if (stopSucceeded && recordedFile != null && recordedFile.exists() && recordedFile.length() > WAV_HEADER_SIZE) {
                if (recordingId != null) {
                    recordingRepository.finalizeRecordedAudio(
                        recordingId = recordingId,
                        durationMs = finalDurationMs,
                        fileSizeBytes = recordedFile.length()
                    )
                }
            } else {
                runCatching { recordedFile?.delete() }
                if (recordingId != null) {
                    recordingRepository.deleteRecording(recordingId)
                }
            }

            ServiceCompat.stopForeground(this@AudioRecorderService, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun releasePcmResources() {
        runCatching { internalAudioRecord?.stop() }
        runCatching { internalAudioRecord?.release() }
        internalAudioRecord = null

        runCatching { micAudioRecord?.stop() }
        runCatching { micAudioRecord?.release() }
        micAudioRecord = null

        projectionCallback?.let { cb ->
            runCatching { mediaProjection?.unregisterCallback(cb) }
        }
        projectionCallback = null

        runCatching { mediaProjection?.stop() }
        mediaProjection = null
    }

    private fun startMeteringAndTimer() {
        tickerJob?.cancel()
        tickerJob = serviceScope.launch {
            while (isActive && _recordingState.value.status == RecordingStatus.RECORDING) {
                val totalActiveMs = accumulatedActiveMs +
                    (SystemClock.elapsedRealtime() - activeSegmentStartMs).coerceAtLeast(0L)
                val elapsedSeconds = totalActiveMs / 1000L

                val normalized = if (currentSourceMode == AudioSourceMode.MICROPHONE_ONLY) {
                    val rawAmplitude = runCatching { mediaRecorder?.maxAmplitude ?: 0 }.getOrDefault(0)
                    sqrt((rawAmplitude / 32767f).coerceIn(0f, 1f))
                } else {
                    latestPcmAmplitude
                }

                val updatedState = _recordingState.value.copy(
                    elapsedSeconds = elapsedSeconds,
                    normalizedAmplitude = normalized
                )
                _recordingState.value = updatedState

                if (elapsedSeconds != lastNotifiedSecond) {
                    lastNotifiedSecond = elapsedSeconds
                    updateNotification(updatedState)
                }

                delay(METERING_INTERVAL_MS)
            }
        }
    }

    private fun createMediaRecorder(): MediaRecorder {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(this)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
    }

    private fun createNotificationChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Lecture Recording",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Persistent notification while recording lecture audio"
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    private fun updateNotification(state: RecordingState) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(state))
    }

    private fun buildNotification(state: RecordingState): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val statusLabel = when (state.status) {
            RecordingStatus.RECORDING -> "Recording"
            RecordingStatus.PAUSED -> "Paused"
            RecordingStatus.IDLE -> "Idle"
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("LectureBrief • $statusLabel")
            .setContentText(formatElapsed(state.elapsedSeconds))
            .setContentIntent(contentPendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)

        if (state.status == RecordingStatus.RECORDING) {
            builder.addAction(
                android.R.drawable.ic_media_pause,
                "Pause",
                serviceActionPendingIntent(ACTION_PAUSE, REQUEST_CODE_PAUSE)
            )
        } else if (state.status == RecordingStatus.PAUSED) {
            builder.addAction(
                android.R.drawable.ic_media_play,
                "Resume",
                serviceActionPendingIntent(ACTION_RESUME, REQUEST_CODE_RESUME)
            )
        }

        builder.addAction(
            android.R.drawable.ic_menu_close_clear_cancel,
            "Stop",
            serviceActionPendingIntent(ACTION_STOP, REQUEST_CODE_STOP)
        )

        return builder.build()
    }

    private fun serviceActionPendingIntent(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(this, AudioRecorderService::class.java).apply {
            this.action = action
        }
        return PendingIntent.getService(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    override fun onDestroy() {
        tickerJob?.cancel()
        pcmCaptureJob?.cancel()
        runCatching { mediaRecorder?.release() }
        mediaRecorder = null
        releasePcmResources()
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "lecturebrief_recording_channel"
        private const val NOTIFICATION_ID = 1001
        private const val RECORDINGS_DIR_NAME = "recordings"

        private const val SAMPLE_RATE_HZ = 16_000
        private const val BIT_RATE_BPS = 64_000
        private const val PCM_CHUNK_SAMPLES = 1024
        private const val WAV_HEADER_SIZE = 44
        private const val METERING_INTERVAL_MS = 100L

        const val ACTION_START = "com.lecturebrief.service.action.START"
        const val ACTION_PAUSE = "com.lecturebrief.service.action.PAUSE"
        const val ACTION_RESUME = "com.lecturebrief.service.action.RESUME"
        const val ACTION_STOP = "com.lecturebrief.service.action.STOP"

        private const val EXTRA_AUDIO_SOURCE_MODE = "extra_audio_source_mode"
        private const val EXTRA_PROJECTION_RESULT_CODE = "extra_projection_result_code"
        private const val EXTRA_PROJECTION_DATA = "extra_projection_data"
        private const val EXTRA_FOLDER_ID = "extra_folder_id"

        private const val REQUEST_CODE_PAUSE = 101
        private const val REQUEST_CODE_RESUME = 102
        private const val REQUEST_CODE_STOP = 103

        private val _recordingState = MutableStateFlow(RecordingState())
        val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()

        fun startRecording(
            context: Context,
            sourceMode: AudioSourceMode = AudioSourceMode.MICROPHONE_ONLY,
            projectionResultCode: Int = Activity.RESULT_CANCELED,
            projectionData: Intent? = null,
            folderId: Long? = null
        ) {
            val intent = Intent(context, AudioRecorderService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_AUDIO_SOURCE_MODE, sourceMode.name)
                putExtra(EXTRA_PROJECTION_RESULT_CODE, projectionResultCode)
                if (projectionData != null) {
                    putExtra(EXTRA_PROJECTION_DATA, projectionData)
                }
                if (folderId != null && folderId > 0L) {
                    putExtra(EXTRA_FOLDER_ID, folderId)
                }
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun pauseRecording(context: Context) {
            val intent = Intent(context, AudioRecorderService::class.java).apply {
                action = ACTION_PAUSE
            }
            context.startService(intent)
        }

        fun resumeRecording(context: Context) {
            val intent = Intent(context, AudioRecorderService::class.java).apply {
                action = ACTION_RESUME
            }
            context.startService(intent)
        }

        fun stopRecording(context: Context) {
            val intent = Intent(context, AudioRecorderService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        fun formatElapsed(totalSeconds: Long): String {
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60
            return if (hours > 0) {
                String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format(Locale.US, "%02d:%02d", minutes, seconds)
            }
        }
    }
}
