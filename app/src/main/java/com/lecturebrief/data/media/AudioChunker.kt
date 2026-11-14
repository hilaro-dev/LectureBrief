package com.lecturebrief.data.media

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Format-safe audio chunker that slices oversized or long recordings into <= 4 MB / <= 4 min
 * segments inside cacheDir/chunks/ without slow MediaCodec re-encoding.
 */
class AudioChunker(private val context: Context) {

    suspend fun splitIntoChunksIfNeeded(
        sourceFile: File,
        chunkDurationMs: Long = MAX_CHUNK_DURATION_MS,
        overlapMs: Long = DEFAULT_OVERLAP_MS
    ): List<File> = withContext(Dispatchers.IO) {
        if (!sourceFile.exists()) return@withContext listOf(sourceFile)

        val totalDurationMs = extractDurationMs(sourceFile)
        val underSizeLimit = sourceFile.length() <= MAX_CHUNK_BYTES
        val underDurationLimit = totalDurationMs in 1L..MAX_CHUNK_DURATION_MS

        if (underSizeLimit && underDurationLimit) {
            return@withContext listOf(sourceFile)
        }
        if (totalDurationMs <= 0L && underSizeLimit) {
            return@withContext listOf(sourceFile)
        }

        val chunksDir = File(context.cacheDir, CHUNKS_DIR_NAME).apply {
            if (!exists()) mkdirs()
        }

        val extension = sourceFile.extension.lowercase(Locale.US)
        val trackInfo = inspectAudioTrack(sourceFile)
        val mime = trackInfo?.mime.orEmpty().lowercase(Locale.US)

        val createdChunks = when {
            mime == MediaFormat.MIMETYPE_AUDIO_AAC ||
                mime == "audio/mp4a-latm" ||
                extension == "m4a" ||
                extension == "aac" -> {
                splitAacIntoM4aChunks(
                    sourceFile = sourceFile,
                    chunksDir = chunksDir,
                    chunkDurationMs = chunkDurationMs,
                    overlapMs = overlapMs,
                    fallbackDurationMs = totalDurationMs
                )
            }

            extension == "wav" || mime == "audio/raw" || mime == "audio/wav" || mime == "audio/x-wav" -> {
                splitWavIntoChunks(sourceFile = sourceFile, chunksDir = chunksDir)
            }

            extension == "mp3" || mime == MediaFormat.MIMETYPE_AUDIO_MPEG -> {
                splitMp3IntoChunks(sourceFile = sourceFile, chunksDir = chunksDir)
            }

            else -> emptyList()
        }

        val validChunks = createdChunks.filter { chunk ->
            val valid = chunk.exists() && chunk.length() > MIN_VALID_CHUNK_BYTES
            if (!valid) {
                runCatching { chunk.delete() }
            }
            valid
        }

        if (validChunks.isEmpty()) {
            listOf(sourceFile)
        } else {
            validChunks
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

    private fun inspectAudioTrack(sourceFile: File): AudioTrackInfo? {
        val extractor = MediaExtractor()
        return try {
            extractor.setDataSource(sourceFile.absolutePath)
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME).orEmpty()
                if (mime.startsWith("audio/")) {
                    return AudioTrackInfo(trackIndex = i, format = format, mime = mime)
                }
            }
            null
        } catch (_: Exception) {
            null
        } finally {
            runCatching { extractor.release() }
        }
    }

    private fun splitAacIntoM4aChunks(
        sourceFile: File,
        chunksDir: File,
        chunkDurationMs: Long,
        overlapMs: Long,
        fallbackDurationMs: Long
    ): List<File> {
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
                return emptyList()
            }

            val formatDurationMs = if (audioFormat.containsKey(MediaFormat.KEY_DURATION)) {
                audioFormat.getLong(MediaFormat.KEY_DURATION) / 1000L
            } else {
                fallbackDurationMs
            }

            // Ensure each AAC chunk stays under both MAX_CHUNK_DURATION_MS and MAX_CHUNK_BYTES.
            val effectiveChunkDurationMs = if (formatDurationMs > 0L && sourceFile.length() > MAX_CHUNK_BYTES) {
                val bytesPerMs = sourceFile.length().toDouble() / formatDurationMs.toDouble()
                val sizeBoundDurationMs = if (bytesPerMs > 0.0) {
                    ((MAX_CHUNK_BYTES * 0.85) / bytesPerMs).toLong().coerceAtLeast(MIN_CHUNK_DURATION_MS)
                } else {
                    chunkDurationMs
                }
                minOf(chunkDurationMs, sizeBoundDurationMs)
            } else {
                chunkDurationMs
            }

            val stepMs = (effectiveChunkDurationMs - overlapMs).coerceAtLeast(1_000L)

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
                val endMs = startMs + effectiveChunkDurationMs
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

                val timestampBaseUs = minOf(startUs, firstSampleTimeUs)

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
                            bufferInfo.presentationTimeUs = (sampleTimeUs - timestampBaseUs)
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

                if (!remuxSucceeded || !chunkFile.exists() || chunkFile.length() <= MIN_VALID_CHUNK_BYTES) {
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
        return createdChunks
    }

    private fun splitWavIntoChunks(sourceFile: File, chunksDir: File): List<File> {
        val wavInfo = parseWavHeader(sourceFile) ?: return emptyList()
        val dataStart = wavInfo.dataOffset
        val dataTotalBytes = (sourceFile.length() - dataStart).coerceAtLeast(0L)
        if (dataTotalBytes <= MIN_VALID_CHUNK_BYTES) return emptyList()

        val blockAlign = wavInfo.blockAlign.coerceAtLeast(1)
        val rawMaxPayload = (MAX_CHUNK_BYTES - WAV_HEADER_SIZE).coerceAtLeast(blockAlign.toLong())
        val maxPayloadBytes = (rawMaxPayload / blockAlign) * blockAlign

        val createdChunks = mutableListOf<File>()
        val copyBuffer = ByteArray(IO_BUFFER_SIZE)

        RandomAccessFile(sourceFile, "r").use { raf ->
            var offset = 0L
            var chunkIndex = 0
            while (offset < dataTotalBytes) {
                val remaining = dataTotalBytes - offset
                val sliceBytes = minOf(maxPayloadBytes, remaining)
                val alignedSliceBytes = if (sliceBytes == remaining) {
                    sliceBytes
                } else {
                    (sliceBytes / blockAlign) * blockAlign
                }
                if (alignedSliceBytes <= 0L) break

                val chunkFile = File(
                    chunksDir,
                    String.format(Locale.US, "chunk_%02d.wav", chunkIndex)
                )
                val header = buildWavHeader(
                    channels = wavInfo.channels,
                    sampleRate = wavInfo.sampleRate,
                    bitsPerSample = wavInfo.bitsPerSample,
                    dataSizeBytes = alignedSliceBytes
                )

                FileOutputStream(chunkFile).use { out ->
                    out.write(header)
                    raf.seek(dataStart + offset)
                    var left = alignedSliceBytes
                    while (left > 0L) {
                        val toRead = minOf(copyBuffer.size.toLong(), left).toInt()
                        val read = raf.read(copyBuffer, 0, toRead)
                        if (read <= 0) break
                        out.write(copyBuffer, 0, read)
                        left -= read
                    }
                }

                if (chunkFile.exists() && chunkFile.length() > MIN_VALID_CHUNK_BYTES) {
                    createdChunks.add(chunkFile)
                    chunkIndex++
                } else {
                    runCatching { chunkFile.delete() }
                }

                offset += alignedSliceBytes
            }
        }
        return createdChunks
    }

    private fun splitMp3IntoChunks(sourceFile: File, chunksDir: File): List<File> {
        val totalBytes = sourceFile.length()
        if (totalBytes <= MAX_CHUNK_BYTES) return emptyList()

        val createdChunks = mutableListOf<File>()
        val targetChunkBytes = MAX_CHUNK_BYTES - 16 * 1024L

        RandomAccessFile(sourceFile, "r").use { raf ->
            var chunkStart = findInitialMp3Offset(raf, totalBytes)
            var chunkIndex = 0

            while (chunkStart < totalBytes) {
                val tentativeEnd = minOf(chunkStart + targetChunkBytes, totalBytes)
                val chunkEnd = if (tentativeEnd >= totalBytes) {
                    totalBytes
                } else {
                    findNextMp3FrameSync(raf, tentativeEnd, totalBytes) ?: totalBytes
                }

                val sliceLength = chunkEnd - chunkStart
                if (sliceLength <= MIN_VALID_CHUNK_BYTES) break

                val chunkFile = File(
                    chunksDir,
                    String.format(Locale.US, "chunk_%02d.mp3", chunkIndex)
                )

                val copyBuffer = ByteArray(IO_BUFFER_SIZE)
                FileOutputStream(chunkFile).use { out ->
                    raf.seek(chunkStart)
                    var remaining = sliceLength
                    while (remaining > 0L) {
                        val toRead = minOf(copyBuffer.size.toLong(), remaining).toInt()
                        val read = raf.read(copyBuffer, 0, toRead)
                        if (read <= 0) break
                        out.write(copyBuffer, 0, read)
                        remaining -= read
                    }
                }

                if (chunkFile.exists() && chunkFile.length() > MIN_VALID_CHUNK_BYTES) {
                    createdChunks.add(chunkFile)
                    chunkIndex++
                } else {
                    runCatching { chunkFile.delete() }
                }

                chunkStart = chunkEnd
            }
        }

        return createdChunks
    }

    private fun findInitialMp3Offset(raf: RandomAccessFile, totalBytes: Long): Long {
        if (totalBytes < 10L) return 0L
        raf.seek(0L)
        val header = ByteArray(10)
        if (raf.read(header) != 10) return 0L

        // Skip ID3v2 header if present so chunks start cleanly on MPEG frames.
        if (header[0] == 'I'.code.toByte() &&
            header[1] == 'D'.code.toByte() &&
            header[2] == '3'.code.toByte()
        ) {
            val tagSize = ((header[6].toInt() and 0x7F) shl 21) or
                ((header[7].toInt() and 0x7F) shl 14) or
                ((header[8].toInt() and 0x7F) shl 7) or
                (header[9].toInt() and 0x7F)
            val afterId3 = 10L + tagSize.toLong()
            if (afterId3 in 10L until totalBytes) {
                return findNextMp3FrameSync(raf, afterId3, totalBytes) ?: afterId3
            }
        }
        return 0L
    }

    private fun findNextMp3FrameSync(
        raf: RandomAccessFile,
        fromOffset: Long,
        totalBytes: Long
    ): Long? {
        val searchLimit = minOf(fromOffset + MP3_SYNC_SEARCH_WINDOW_BYTES, totalBytes)
        val scanBuffer = ByteArray(IO_BUFFER_SIZE)
        var pos = fromOffset
        var prevByte = -1

        while (pos < searchLimit) {
            raf.seek(pos)
            val toRead = minOf(scanBuffer.size.toLong(), searchLimit - pos).toInt()
            val read = raf.read(scanBuffer, 0, toRead)
            if (read <= 0) break

            for (i in 0 until read) {
                val current = scanBuffer[i].toInt() and 0xFF
                if (prevByte == 0xFF && (current and 0xE0) == 0xE0) {
                    return pos + i - 1L
                }
                prevByte = current
            }
            pos += read
        }
        return null
    }

    private fun parseWavHeader(file: File): WavInfo? {
        if (file.length() < WAV_HEADER_SIZE) return null
        return try {
            FileInputStream(file).use { input ->
                val riff = ByteArray(12)
                if (input.read(riff) != 12) return null
                if (String(riff, 0, 4, Charsets.US_ASCII) != "RIFF" ||
                    String(riff, 8, 4, Charsets.US_ASCII) != "WAVE"
                ) {
                    return null
                }

                var channels = 1
                var sampleRate = 16_000
                var bitsPerSample = 16
                var blockAlign = 2
                var offset = 12L
                val chunkHeader = ByteArray(8)

                while (offset + 8L <= file.length()) {
                    if (input.read(chunkHeader) != 8) break
                    val chunkId = String(chunkHeader, 0, 4, Charsets.US_ASCII)
                    val chunkSize = ByteBuffer.wrap(chunkHeader, 4, 4)
                        .order(ByteOrder.LITTLE_ENDIAN)
                        .int.toLong() and 0xFFFFFFFFL
                    offset += 8L

                    if (chunkId == "fmt " && chunkSize >= 16L) {
                        val fmtBytes = ByteArray(16)
                        if (input.read(fmtBytes) != 16) return null
                        val fmtBuf = ByteBuffer.wrap(fmtBytes).order(ByteOrder.LITTLE_ENDIAN)
                        fmtBuf.short // audioFormat
                        channels = fmtBuf.short.toInt() and 0xFFFF
                        sampleRate = fmtBuf.int
                        fmtBuf.int // byteRate
                        blockAlign = fmtBuf.short.toInt() and 0xFFFF
                        bitsPerSample = fmtBuf.short.toInt() and 0xFFFF
                        val extraToSkip = chunkSize - 16L
                        if (extraToSkip > 0L) skipFully(input, extraToSkip)
                        offset += chunkSize
                    } else if (chunkId == "data") {
                        return WavInfo(
                            channels = channels.coerceAtLeast(1),
                            sampleRate = sampleRate.coerceAtLeast(8_000),
                            bitsPerSample = bitsPerSample.coerceAtLeast(8),
                            blockAlign = blockAlign.coerceAtLeast(1),
                            dataOffset = offset
                        )
                    } else {
                        skipFully(input, chunkSize)
                        offset += chunkSize
                    }

                    if (chunkSize % 2L == 1L) {
                        skipFully(input, 1L)
                        offset += 1L
                    }
                }
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun skipFully(input: FileInputStream, bytes: Long) {
        var remaining = bytes
        while (remaining > 0L) {
            val skipped = input.skip(remaining)
            if (skipped <= 0L) {
                if (input.read() == -1) break
                remaining--
            } else {
                remaining -= skipped
            }
        }
    }

    private fun buildWavHeader(
        channels: Int,
        sampleRate: Int,
        bitsPerSample: Int,
        dataSizeBytes: Long
    ): ByteArray {
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = (channels * bitsPerSample / 8).toShort()
        val totalDataLen = (dataSizeBytes + 36L).toInt()
        val dataLen = dataSizeBytes.toInt()

        return ByteBuffer.allocate(WAV_HEADER_SIZE).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray(Charsets.US_ASCII))
            putInt(totalDataLen)
            put("WAVE".toByteArray(Charsets.US_ASCII))
            put("fmt ".toByteArray(Charsets.US_ASCII))
            putInt(16)
            putShort(1) // PCM
            putShort(channels.toShort())
            putInt(sampleRate)
            putInt(byteRate)
            putShort(blockAlign)
            putShort(bitsPerSample.toShort())
            put("data".toByteArray(Charsets.US_ASCII))
            putInt(dataLen)
        }.array()
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

    private data class AudioTrackInfo(
        val trackIndex: Int,
        val format: MediaFormat,
        val mime: String
    )

    private data class WavInfo(
        val channels: Int,
        val sampleRate: Int,
        val bitsPerSample: Int,
        val blockAlign: Int,
        val dataOffset: Long
    )

    companion object {
        const val MAX_CHUNK_BYTES = 4L * 1024L * 1024L
        const val MAX_CHUNK_DURATION_MS = 4L * 60L * 1000L
        const val DEFAULT_OVERLAP_MS = 2_000L
        private const val MIN_CHUNK_DURATION_MS = 30_000L
        private const val MIN_VALID_CHUNK_BYTES = 1024L
        private const val WAV_HEADER_SIZE = 44
        private const val CHUNKS_DIR_NAME = "chunks"
        private const val DEFAULT_BUFFER_SIZE = 256 * 1024
        private const val IO_BUFFER_SIZE = 32 * 1024
        private const val MP3_SYNC_SEARCH_WINDOW_BYTES = 256L * 1024L
    }
}
