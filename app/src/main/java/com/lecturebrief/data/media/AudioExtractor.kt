package com.lecturebrief.data.media

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ExtractedAudioResult(
    val file: File,
    val displayTitle: String,
    val durationMs: Long
)

class AudioExtractor(
    private val context: Context,
    @Suppress("unused") private val audioCompressor: AudioCompressor = AudioCompressor(context)
) {

    suspend fun extractAudio(uri: Uri): Result<ExtractedAudioResult> = withContext(Dispatchers.IO) {
        runCatching {
            val resolver = context.contentResolver
            val displayName = resolveDisplayName(uri)
            val displayTitle = displayName.substringBeforeLast('.').ifBlank { "Imported Lecture" }
            val extension = displayName.substringAfterLast('.', "").lowercase()
            val mimeType = resolver.getType(uri)?.lowercase().orEmpty()

            val recordingsDir = File(context.filesDir, RECORDINGS_DIR).apply {
                if (!exists()) mkdirs()
            }
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())

            val isDirectAudio = (mimeType.startsWith("audio/") ||
                mimeType == "application/ogg" ||
                mimeType.contains("opus") ||
                extension in DIRECT_AUDIO_EXTENSIONS) &&
                extension !in VIDEO_CONTAINER_EXTENSIONS &&
                !mimeType.startsWith("video/")

            if (isDirectAudio) {
                val targetExt = extension.ifBlank { mimeToAudioExtension(mimeType) }
                val rawFile = File(recordingsDir, "import_$timestamp.$targetExt")
                copyUriToFile(uri, rawFile)
                val durationMs = extractDurationMs(rawFile)
                ExtractedAudioResult(
                    file = rawFile,
                    displayTitle = displayTitle,
                    durationMs = durationMs
                )
            } else {
                val extractedFile = File(recordingsDir, "import_$timestamp.m4a")
                val durationMs = extractAudioTrackToM4a(uri, extractedFile, recordingsDir, timestamp, extension)
                ExtractedAudioResult(
                    file = extractedFile,
                    displayTitle = displayTitle,
                    durationMs = durationMs.takeIf { it > 0L } ?: extractDurationMs(extractedFile)
                )
            }
        }
    }

    private fun extractAudioTrackToM4a(
        uri: Uri,
        targetM4aFile: File,
        recordingsDir: File,
        timestamp: String,
        originalExtension: String
    ): Long {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(context, uri, null)

            var audioTrackIndex = -1
            var audioFormat: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val trackMime = format.getString(MediaFormat.KEY_MIME).orEmpty()
                if (trackMime.startsWith("audio/")) {
                    audioTrackIndex = i
                    audioFormat = format
                    break
                }
            }

            if (audioTrackIndex == -1 || audioFormat == null) {
                throw IllegalStateException("The selected video file has no audio track.")
            }

            val durationMs = if (audioFormat.containsKey(MediaFormat.KEY_DURATION)) {
                audioFormat.getLong(MediaFormat.KEY_DURATION) / 1000L
            } else {
                0L
            }

            // Attempt fast lossless remux of the compressed audio track only (discarding video).
            val remuxResult = runCatching {
                remuxSingleTrackToMp4(extractor, audioTrackIndex, audioFormat, targetM4aFile)
            }

            if (remuxResult.isSuccess && targetM4aFile.exists() && targetM4aFile.length() > 0L) {
                return durationMs.takeIf { it > 0L } ?: extractDurationMs(targetM4aFile)
            }

            runCatching { targetM4aFile.delete() }

            // Fallback: copy raw container if MediaMuxer rejects an unusual audio codec.
            val fallbackExt = originalExtension.ifBlank { "m4a" }
            val fallbackFile = File(recordingsDir, "import_$timestamp.$fallbackExt")
            copyUriToFile(uri, fallbackFile)
            if (fallbackFile.absolutePath != targetM4aFile.absolutePath) {
                fallbackFile.renameTo(targetM4aFile)
            }
            return durationMs
        } finally {
            runCatching { extractor.release() }
        }
    }

    private fun remuxSingleTrackToMp4(
        extractor: MediaExtractor,
        trackIndex: Int,
        trackFormat: MediaFormat,
        outputFile: File
    ) {
        extractor.selectTrack(trackIndex)
        val maxInputSize = if (trackFormat.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
            trackFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE).coerceAtLeast(DEFAULT_BUFFER_SIZE)
        } else {
            DEFAULT_BUFFER_SIZE
        }

        val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        var muxerStarted = false
        try {
            val muxerTrackIndex = muxer.addTrack(trackFormat)
            muxer.start()
            muxerStarted = true

            val buffer = ByteBuffer.allocate(maxInputSize)
            val bufferInfo = MediaCodec.BufferInfo()

            while (true) {
                val sampleSize = extractor.readSampleData(buffer, 0)
                if (sampleSize < 0) break

                bufferInfo.offset = 0
                bufferInfo.size = sampleSize
                bufferInfo.presentationTimeUs = extractor.sampleTime.coerceAtLeast(0L)
                bufferInfo.flags = extractor.sampleFlags
                muxer.writeSampleData(muxerTrackIndex, buffer, bufferInfo)
                if (!extractor.advance()) break
            }
        } finally {
            if (muxerStarted) {
                runCatching { muxer.stop() }
            }
            runCatching { muxer.release() }
            extractor.unselectTrack(trackIndex)
        }
    }

    private fun copyUriToFile(uri: Uri, targetFile: File) {
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(targetFile).use { output ->
                input.copyTo(output)
            }
        } ?: throw IllegalStateException("Unable to open selected file")
    }

    private fun resolveDisplayName(uri: Uri): String {
        context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) {
                    val name = cursor.getString(index)
                    if (!name.isNullOrBlank()) return name
                }
            }
        }
        return uri.lastPathSegment?.substringAfterLast('/')?.ifBlank { null } ?: "Imported Lecture"
    }

    private fun extractDurationMs(file: File): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
        } catch (_: Exception) {
            0L
        } finally {
            runCatching { retriever.release() }
        }
    }

    /**
     * Immediately copies a shared content:// URI into internal cache while temporary read permission
     * is active, preserving or inferring the file extension from ContentResolver or the intent MIME.
     */
    suspend fun stageSharedMedia(uri: Uri, fallbackMimeType: String?): Result<File> =
        withContext(Dispatchers.IO) {
            runCatching {
                val displayName = resolveDisplayName(uri)
                val rawExt = displayName.substringAfterLast('.', "").lowercase()
                val mimeType = (context.contentResolver.getType(uri) ?: fallbackMimeType).orEmpty().lowercase()
                val extension = rawExt.ifBlank { mimeToExtension(mimeType) }

                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                val rawBase = displayName.substringBeforeLast('.').ifBlank { "Shared_$timestamp" }
                val safeBase = rawBase.replace(Regex("[^A-Za-z0-9._ -]"), "_").trim().ifBlank { "Shared_$timestamp" }

                val sharedDir = File(context.cacheDir, "$SHARED_IMPORTS_DIR/$timestamp").apply {
                    if (!exists()) mkdirs()
                }
                val stagedFile = File(sharedDir, "$safeBase.$extension")
                copyUriToFile(uri, stagedFile)
                stagedFile
            }
        }

    private fun mimeToAudioExtension(mimeType: String): String {
        return when {
            mimeType.contains("mpeg") || mimeType.contains("mp3") -> "mp3"
            mimeType.contains("wav") -> "wav"
            mimeType.contains("opus") -> "opus"
            mimeType.contains("ogg") -> "ogg"
            mimeType.contains("aac") -> "aac"
            mimeType.contains("flac") -> "flac"
            else -> "m4a"
        }
    }

    private fun mimeToExtension(mimeType: String): String {
        return when {
            mimeType.startsWith("video/") -> when {
                mimeType.contains("webm") -> "webm"
                mimeType.contains("matroska") || mimeType.contains("mkv") -> "mkv"
                mimeType.contains("quicktime") -> "mov"
                mimeType.contains("3gpp") -> "3gp"
                else -> "mp4"
            }
            else -> mimeToAudioExtension(mimeType)
        }
    }

    companion object {
        private const val RECORDINGS_DIR = "recordings"
        private const val SHARED_IMPORTS_DIR = "shared_imports"
        private const val DEFAULT_BUFFER_SIZE = 256 * 1024

        private val DIRECT_AUDIO_EXTENSIONS = setOf("mp3", "m4a", "wav", "ogg", "opus", "aac", "flac")
        private val VIDEO_CONTAINER_EXTENSIONS = setOf("mp4", "mkv", "mov", "webm", "avi", "3gp")
    }
}
