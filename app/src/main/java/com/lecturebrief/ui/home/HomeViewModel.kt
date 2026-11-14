package com.lecturebrief.ui.home

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lecturebrief.data.local.db.entity.AudioSourceMode
import com.lecturebrief.data.local.db.entity.RecordingWithSummary
import com.lecturebrief.data.media.AudioExtractor
import com.lecturebrief.data.repository.ProcessingRepository
import com.lecturebrief.data.repository.RecordingRepository
import com.lecturebrief.service.AudioRecorderService
import com.lecturebrief.service.RecordingState
import com.lecturebrief.service.RecordingStatus
import com.lecturebrief.ui.state.LibraryUiState
import com.lecturebrief.ui.state.RecordingListItemUiModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(
    private val recordingRepository: RecordingRepository,
    private val processingRepository: ProcessingRepository,
    private val audioExtractor: AudioExtractor
) : ViewModel() {

    val recordingState: StateFlow<RecordingState> = AudioRecorderService.recordingState

    private val _selectedSourceMode = MutableStateFlow(AudioSourceMode.MICROPHONE_ONLY)
    val selectedSourceMode: StateFlow<AudioSourceMode> = _selectedSourceMode.asStateFlow()

    private val _selectedFolderId = MutableStateFlow<Long?>(null)

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

    private val _waveformSamples = MutableStateFlow(List(WAVEFORM_BAR_COUNT) { 0f })
    val waveformSamples: StateFlow<List<Float>> = _waveformSamples.asStateFlow()

    val libraryUiState: StateFlow<LibraryUiState> = combine(
        recordingRepository.observeAllRecordings(),
        recordingRepository.observeAllFolders(),
        _selectedFolderId,
        processingRepository.processingProgress
    ) { items, folders, selectedFolderId, progressMap ->
        val filtered = if (selectedFolderId == null) {
            items
        } else {
            items.filter { it.recording.folderId == selectedFolderId }
        }
        LibraryUiState(
            isLoading = false,
            folders = folders,
            selectedFolderId = selectedFolderId,
            recordings = filtered.map { it.toUiModel(progressMap[it.recording.id]) }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LibraryUiState(isLoading = true)
    )

    init {
        viewModelScope.launch {
            AudioRecorderService.recordingState.collect { state ->
                when (state.status) {
                    RecordingStatus.IDLE -> {
                        if (_waveformSamples.value.any { it > 0f }) {
                            _waveformSamples.value = List(WAVEFORM_BAR_COUNT) { 0f }
                        }
                    }

                    RecordingStatus.RECORDING -> {
                        val updated = _waveformSamples.value.drop(1) + state.normalizedAmplitude
                        _waveformSamples.value = updated
                    }

                    RecordingStatus.PAUSED -> Unit
                }
            }
        }
    }

    fun onSourceModeSelected(mode: AudioSourceMode) {
        if (recordingState.value.status == RecordingStatus.IDLE) {
            _selectedSourceMode.value = mode
        }
    }

    fun onFolderSelected(folderId: Long?) {
        _selectedFolderId.value = folderId
    }

    fun createFolder(name: String) {
        viewModelScope.launch {
            runCatching { recordingRepository.createFolder(name) }
        }
    }

    fun deleteFolder(folderId: Long) {
        viewModelScope.launch {
            recordingRepository.deleteFolder(folderId)
            if (_selectedFolderId.value == folderId) {
                _selectedFolderId.value = null
            }
        }
    }

    fun renameRecording(recordingId: Long, newTitle: String) {
        viewModelScope.launch {
            recordingRepository.renameRecording(recordingId, newTitle)
        }
    }

    fun moveRecordingToFolder(recordingId: Long, folderId: Long?) {
        viewModelScope.launch {
            recordingRepository.moveRecordingToFolder(recordingId, folderId)
        }
    }

    fun createFolderAndMoveRecording(recordingId: Long, folderName: String) {
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

    fun importMediaUri(
        uri: Uri,
        onSuccess: (Long) -> Unit,
        onError: (String) -> Unit
    ) {
        if (_isImporting.value) return
        viewModelScope.launch {
            _isImporting.value = true
            try {
                audioExtractor.extractAudio(uri)
                    .onSuccess { extracted ->
                        val recordingId = recordingRepository.createImportedRecording(
                            title = extracted.displayTitle,
                            audioFilePath = extracted.file.absolutePath,
                            durationMs = extracted.durationMs,
                            fileSizeBytes = extracted.file.length(),
                            folderId = _selectedFolderId.value
                        )
                        processingRepository.enqueueProcessing(recordingId)
                        onSuccess(recordingId)
                    }
                    .onFailure { throwable ->
                        onError(throwable.message ?: "Failed to extract audio from file")
                    }
            } finally {
                _isImporting.value = false
            }
        }
    }

    fun deleteAudioFileOnly(recordingId: Long) {
        viewModelScope.launch {
            recordingRepository.deleteAudioFileOnly(recordingId)
        }
    }

    fun deleteRecording(recordingId: Long) {
        viewModelScope.launch {
            recordingRepository.deleteRecording(recordingId)
        }
    }

    private fun RecordingWithSummary.toUiModel(processingMessage: String?): RecordingListItemUiModel {
        return RecordingListItemUiModel(
            id = recording.id,
            title = recording.title,
            durationMs = recording.durationMs,
            createdAtEpochMs = recording.createdAtEpochMs,
            audioSource = recording.audioSource,
            status = recording.status,
            folderId = recording.folderId,
            audioFilePath = recording.audioFilePath,
            processingMessage = processingMessage,
            summaryPreview = summary?.overviewMarkdown
        )
    }

    class Factory(
        private val recordingRepository: RecordingRepository,
        private val processingRepository: ProcessingRepository,
        private val audioExtractor: AudioExtractor
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(HomeViewModel::class.java)) {
                "Unknown ViewModel class: ${modelClass.name}"
            }
            return HomeViewModel(
                recordingRepository = recordingRepository,
                processingRepository = processingRepository,
                audioExtractor = audioExtractor
            ) as T
        }
    }

    companion object {
        private const val WAVEFORM_BAR_COUNT = 32
    }
}
