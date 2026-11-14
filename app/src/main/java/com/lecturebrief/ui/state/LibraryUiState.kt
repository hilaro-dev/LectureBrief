package com.lecturebrief.ui.state

import androidx.compose.runtime.Immutable
import com.lecturebrief.data.local.db.entity.AudioSourceMode
import com.lecturebrief.data.local.db.entity.FolderEntity
import com.lecturebrief.data.local.db.entity.ProcessingStatus

@Immutable
data class RecordingListItemUiModel(
    val id: Long,
    val title: String,
    val durationMs: Long,
    val createdAtEpochMs: Long,
    val audioSource: AudioSourceMode,
    val status: ProcessingStatus,
    val folderId: Long? = null,
    val audioFilePath: String? = null,
    val processingMessage: String? = null,
    val summaryPreview: String? = null
)

@Immutable
data class LibraryUiState(
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val statusFilter: ProcessingStatus? = null,
    val folders: List<FolderEntity> = emptyList(),
    val selectedFolderId: Long? = null,
    val recordings: List<RecordingListItemUiModel> = emptyList(),
    val errorMessage: String? = null
)
