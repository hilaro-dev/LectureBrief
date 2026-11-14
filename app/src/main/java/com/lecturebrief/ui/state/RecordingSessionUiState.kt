package com.lecturebrief.ui.state

import androidx.compose.runtime.Immutable
import com.lecturebrief.data.local.db.entity.AudioSourceMode

enum class SessionPhase {
    IDLE,
    AWAITING_PROJECTION_PERMISSION,
    RECORDING,
    PAUSED,
    SAVING,
    ERROR
}

@Immutable
data class RecordingSessionUiState(
    val phase: SessionPhase = SessionPhase.IDLE,
    val sourceMode: AudioSourceMode = AudioSourceMode.MIC_AND_INTERNAL,
    val elapsedMs: Long = 0L,
    val micAmplitude: Float = 0f,
    val internalAmplitude: Float = 0f,
    val activeRecordingId: Long? = null,
    val errorMessage: String? = null
)
