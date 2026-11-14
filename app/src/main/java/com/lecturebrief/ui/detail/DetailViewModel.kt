package com.lecturebrief.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lecturebrief.data.local.db.entity.FolderEntity
import com.lecturebrief.data.local.db.entity.ProcessingStatus
import com.lecturebrief.data.local.db.entity.RecordingWithSummary
import com.lecturebrief.data.repository.ProcessingRepository
import com.lecturebrief.data.repository.RecordingRepository
import com.lecturebrief.ui.state.RecordingDetailUiState
import com.lecturebrief.ui.state.SummaryUiModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DetailViewModel(
    private val recordingId: Long,
    private val recordingRepository: RecordingRepository,
    private val processingRepository: ProcessingRepository
) : ViewModel() {

    private var autoTriggeredPending = false

    val folders: StateFlow<List<FolderEntity>> =
        recordingRepository.observeAllFolders()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    val uiState: StateFlow<RecordingDetailUiState> = combine(
        recordingRepository.observeRecordingById(recordingId)
            .onEach { item ->
                if (item != null && item.recording.status == ProcessingStatus.PENDING && !autoTriggeredPending) {
                    autoTriggeredPending = true
                    processingRepository.enqueueProcessing(recordingId)
                }
            },
        processingRepository.processingProgress
    ) { item, progressMap ->
        item?.toUiState(progressMap[recordingId])
            ?: RecordingDetailUiState(isLoading = false, recordingId = recordingId)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = RecordingDetailUiState(isLoading = true, recordingId = recordingId)
    )

    fun retryProcessing() {
        processingRepository.enqueueProcessing(recordingId)
    }

    fun renameRecording(newTitle: String) {
        viewModelScope.launch {
            recordingRepository.renameRecording(recordingId, newTitle)
        }
    }

    fun moveToFolder(folderId: Long?) {
        viewModelScope.launch {
            recordingRepository.moveRecordingToFolder(recordingId, folderId)
        }
    }

    fun deleteAudioFileOnly() {
        viewModelScope.launch {
            recordingRepository.deleteAudioFileOnly(recordingId)
        }
    }

    fun createFolderAndMove(folderName: String) {
        viewModelScope.launch {
            val trimmed = folderName.trim()
            if (trimmed.isEmpty()) return@launch
            val insertedId = recordingRepository.createFolder(trimmed)
            val targetId = if (insertedId > 0L) {
                insertedId
            } else {
                recordingRepository.observeAllFolders().first()
                    .firstOrNull { it.name.equals(trimmed, ignoreCase = true) }?.id
            }
            recordingRepository.moveRecordingToFolder(recordingId, targetId)
        }
    }

    private fun RecordingWithSummary.toUiState(processingMessage: String?): RecordingDetailUiState {
        return RecordingDetailUiState(
            isLoading = false,
            recordingId = recording.id,
            title = recording.title,
            createdAtEpochMs = recording.createdAtEpochMs,
            durationMs = recording.durationMs,
            audioSource = recording.audioSource,
            status = recording.status,
            folderId = recording.folderId,
            audioFilePath = recording.audioFilePath,
            processingMessage = processingMessage,
            transcriptText = recording.transcriptText,
            summary = summary?.let {
                SummaryUiModel(
                    modelId = it.modelId,
                    overviewMarkdown = it.overviewMarkdown,
                    keyTakeaways = emptyList(),
                    actionItems = emptyList(),
                    createdAtEpochMs = it.createdAtEpochMs
                )
            },
            errorMessage = recording.errorMessage
        )
    }

    class Factory(
        private val recordingId: Long,
        private val recordingRepository: RecordingRepository,
        private val processingRepository: ProcessingRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(DetailViewModel::class.java)) {
                "Unknown ViewModel class: ${modelClass.name}"
            }
            return DetailViewModel(
                recordingId = recordingId,
                recordingRepository = recordingRepository,
                processingRepository = processingRepository
            ) as T
        }
    }
}
