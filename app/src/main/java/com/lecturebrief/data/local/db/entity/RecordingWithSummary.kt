package com.lecturebrief.data.local.db.entity

import androidx.room.Embedded
import androidx.room.Relation

data class RecordingWithSummary(
    @Embedded
    val recording: RecordingEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "recording_id"
    )
    val summary: SummaryEntity?
)
