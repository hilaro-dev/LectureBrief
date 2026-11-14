package com.lecturebrief.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
enum class AudioSourceMode {
    MICROPHONE_ONLY,
    INTERNAL_ONLY,
    MIC_AND_INTERNAL
}

@Serializable
enum class ProcessingStatus {
    RECORDING,
    PENDING,
    TRANSCRIBING,
    SUMMARIZING,
    DONE,
    ERROR
}

@Entity(
    tableName = "recordings",
    foreignKeys = [
        ForeignKey(
            entity = FolderEntity::class,
            parentColumns = ["id"],
            childColumns = ["folderId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["created_at"]),
        Index(value = ["status"]),
        Index(value = ["folderId"])
    ]
)
data class RecordingEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,

    @ColumnInfo(name = "title")
    var title: String,

    @ColumnInfo(name = "audio_file_path")
    val audioFilePath: String?,

    @ColumnInfo(name = "duration_ms")
    val durationMs: Long,

    @ColumnInfo(name = "file_size_bytes")
    val fileSizeBytes: Long,

    @ColumnInfo(name = "audio_source")
    val audioSource: AudioSourceMode,

    @ColumnInfo(name = "status")
    val status: ProcessingStatus,

    @ColumnInfo(name = "folderId")
    val folderId: Long? = null,

    @ColumnInfo(name = "transcript_text")
    val transcriptText: String? = null,

    @ColumnInfo(name = "transcription_model")
    val transcriptionModel: String? = null,

    @ColumnInfo(name = "language")
    val language: String? = null,

    @ColumnInfo(name = "error_message")
    val errorMessage: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAtEpochMs: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAtEpochMs: Long
)
