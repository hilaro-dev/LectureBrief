package com.lecturebrief.data.media

import android.content.Context
import java.io.File

/**
 * Fast pass-through audio handler.
 *
 * MediaCodec PCM decode/re-encode is intentionally avoided because hardware codecs can hang
 * on long or variable-frame-rate streams. Size limits are enforced via fast packet-level
 * time-chunking in [AudioChunker].
 */
class AudioCompressor(@Suppress("unused") private val context: Context) {

    @Suppress("UNUSED_PARAMETER")
    suspend fun compressIfNeeded(
        sourceFile: File,
        forceCompress: Boolean = false
    ): File = sourceFile

    companion object {
        const val MAX_UNCOMPRESSED_BYTES = 18L * 1024L * 1024L
    }
}
