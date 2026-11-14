package com.lecturebrief.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "summaries",
    foreignKeys = [
        ForeignKey(
            entity = RecordingEntity::class,
            parentColumns = ["id"],
            childColumns = ["recording_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["recording_id"], unique = true)]
)
data class SummaryEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,

    @ColumnInfo(name = "recording_id")
    val recordingId: Long,

    @ColumnInfo(name = "model_id")
    val modelId: String,

    @ColumnInfo(name = "overview_markdown")
    val overviewMarkdown: String,

    @ColumnInfo(name = "key_takeaways_json")
    val keyTakeawaysJson: String,

    @ColumnInfo(name = "action_items_json")
    val actionItemsJson: String,

    @ColumnInfo(name = "prompt_tokens")
    val promptTokens: Int? = null,

    @ColumnInfo(name = "completion_tokens")
    val completionTokens: Int? = null,

    @ColumnInfo(name = "created_at")
    val createdAtEpochMs: Long
)
