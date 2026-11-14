package com.lecturebrief.ui.state

import androidx.compose.runtime.Immutable
import com.lecturebrief.data.local.db.entity.AudioSourceMode
import com.lecturebrief.data.local.db.entity.ProcessingStatus

@Immutable
data class SummaryUiModel(
    val modelId: String,
    val overviewMarkdown: String,
    val keyTakeaways: List<String>,
    val actionItems: List<String>,
    val createdAtEpochMs: Long
)

@Immutable
data class RecordingDetailUiState(
    val isLoading: Boolean = true,
    val recordingId: Long = 0L,
    val title: String = "",
    val createdAtEpochMs: Long = 0L,
    val durationMs: Long = 0L,
    val playbackPositionMs: Long = 0L,
    val isPlaying: Boolean = false,
    val audioSource: AudioSourceMode = AudioSourceMode.MIC_AND_INTERNAL,
    val status: ProcessingStatus = ProcessingStatus.PENDING,
    val folderId: Long? = null,
    val audioFilePath: String? = null,
    val processingMessage: String? = null,
    val transcriptText: String? = null,
    val summary: SummaryUiModel? = null,
    val errorMessage: String? = null
)
