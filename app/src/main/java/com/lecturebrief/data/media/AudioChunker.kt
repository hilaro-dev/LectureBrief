package com.lecturebrief.data.media

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import java.io.File
import java.nio.ByteBuffer
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Fast packet-level audio chunker using MediaExtractor + MediaMuxer (no re-encoding).
 * Slices long recordings into overlapping .m4a segments inside cacheDir/chunks/.
 */
class AudioChunker(private val context: Context) {

    suspend fun splitIntoChunksIfNeeded(
        sourceFile: File,
        chunkDurationMs: Long = DEFAULT_CHUNK_DURATION_MS,
        overlapMs: Long = DEFAULT_OVERLAP_MS
    ): List<File> = withContext(Dispatchers.IO) {
        if (!sourceFile.exists()) return@withContext listOf(sourceFile)

        val totalDurationMs = extractDurationMs(sourceFile)
        val underSizeLimit = sourceFile.length() <= MAX_SINGLE_FILE_BYTES
        val underDurationLimit = totalDurationMs in 1L..MAX_SINGLE_DURATION_MS

        if (underSizeLimit && underDurationLimit) {
            return@withContext listOf(sourceFile)
        }
        // If duration could not be read and size is already small, avoid unnecessary splitting.
        if (totalDurationMs <= 0L && underSizeLimit) {
            return@withContext listOf(sourceFile)
        }

        val chunksDir = File(context.cacheDir, CHUNKS_DIR_NAME).apply {
            if (!exists()) mkdirs()
        }

        val stepMs = (chunkDurationMs - overlapMs).coerceAtLeast(1_000L)
        val createdChunks = mutableListOf<File>()

        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(sourceFile.absolutePath)

            var audioTrackIndex = -1
            var audioFormat: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                if (format.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true) {
                    audioTrackIndex = i
                    audioFormat = format
                    break
                }
            }

            if (audioTrackIndex == -1 || audioFormat == null) {
                return@withContext listOf(sourceFile)
            }

            val formatDurationMs = if (audioFormat.containsKey(MediaFormat.KEY_DURATION)) {
                audioFormat.getLong(MediaFormat.KEY_DURATION) / 1000L
            } else {
                totalDurationMs
            }

            if (underSizeLimit && formatDurationMs in 1L..MAX_SINGLE_DURATION_MS) {
                return@withContext listOf(sourceFile)
            }

            extractor.selectTrack(audioTrackIndex)
            val maxInputSize = if (audioFormat.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                audioFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE).coerceAtLeast(DEFAULT_BUFFER_SIZE)
            } else {
                DEFAULT_BUFFER_SIZE
            }
            val buffer = ByteBuffer.allocate(maxInputSize)
            val bufferInfo = MediaCodec.BufferInfo()

            var chunkIndex = 0
            var startMs = 0L
            var previousFirstSampleTimeUs = -1L

            while (startMs < formatDurationMs || formatDurationMs <= 0L) {
                val endMs = startMs + chunkDurationMs
                val startUs = startMs * 1000L
                val endUs = endMs * 1000L

                val chunkFile = File(
                    chunksDir,
                    String.format(Locale.US, "chunk_%02d.m4a", chunkIndex)
                )

                extractor.seekTo(startUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
                val firstSampleTimeUs = extractor.sampleTime
                if (firstSampleTimeUs < 0L) break
                if (chunkIndex > 0 && firstSampleTimeUs <= previousFirstSampleTimeUs) break
                previousFirstSampleTimeUs = firstSampleTimeUs

                val remuxSucceeded = runCatching {
                    val muxer = MediaMuxer(
                        chunkFile.absolutePath,
                        MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
                    )
                    var muxerStarted = false
                    var sampleCount = 0
                    try {
                        val muxerTrack = muxer.addTrack(audioFormat)
                        muxer.start()
                        muxerStarted = true

                        while (true) {
                            val sampleSize = extractor.readSampleData(buffer, 0)
                            if (sampleSize < 0) break

                            val sampleTimeUs = extractor.sampleTime
                            if (sampleTimeUs < 0L) break
                            if (sampleTimeUs >= endUs && sampleCount > 0) break

                            bufferInfo.offset = 0
                            bufferInfo.size = sampleSize
                            bufferInfo.presentationTimeUs = (sampleTimeUs - firstSampleTimeUs)
                                .coerceAtLeast(0L)
                            bufferInfo.flags = extractor.sampleFlags
                            muxer.writeSampleData(muxerTrack, buffer, bufferInfo)
                            sampleCount++
                            if (!extractor.advance()) break
                        }
                    } finally {
                        if (muxerStarted) runCatching { muxer.stop() }
                        runCatching { muxer.release() }
                    }
                    sampleCount > 0
                }.getOrDefault(false)

                if (!remuxSucceeded || !chunkFile.exists() || chunkFile.length() == 0L) {
                    runCatching { chunkFile.delete() }
                    break
                }

                createdChunks.add(chunkFile)
                chunkIndex++

                if (formatDurationMs > 0L && endMs >= formatDurationMs) break
                startMs += stepMs
            }
        } finally {
            runCatching { extractor.release() }
        }

        if (createdChunks.isEmpty()) {
            listOf(sourceFile)
        } else {
            createdChunks
        }
    }

    fun cleanupChunks(chunks: List<File>) {
        val chunksDirPath = File(context.cacheDir, CHUNKS_DIR_NAME).absolutePath
        chunks.forEach { file ->
            if (file.absolutePath.startsWith(chunksDirPath)) {
                runCatching { file.delete() }
            }
        }
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

    companion object {
        const val MAX_SINGLE_FILE_BYTES = 18L * 1024L * 1024L
        const val MAX_SINGLE_DURATION_MS = 6L * 60L * 1000L
        const val DEFAULT_CHUNK_DURATION_MS = 5L * 60L * 1000L
        const val DEFAULT_OVERLAP_MS = 2_000L
        private const val CHUNKS_DIR_NAME = "chunks"
        private const val DEFAULT_BUFFER_SIZE = 256 * 1024
    }
}
