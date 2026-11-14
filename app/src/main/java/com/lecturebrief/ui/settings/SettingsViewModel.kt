package com.lecturebrief.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lecturebrief.data.local.prefs.ThemeMode
import com.lecturebrief.data.local.prefs.ThemePreferences
import com.lecturebrief.data.repository.RecordingRepository
import com.lecturebrief.data.repository.SettingsRepository
import com.lecturebrief.ui.state.GatewayConnectionStatus
import com.lecturebrief.ui.state.SettingsUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val recordingRepository: RecordingRepository,
    private val themePreferences: ThemePreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(initialState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            themePreferences.themeMode.collect { mode ->
                _uiState.update { it.copy(themeMode = mode) }
            }
        }
    }

    private fun initialState(): SettingsUiState {
        val config = settingsRepository.getCurrentConfig()
        return SettingsUiState(
            baseUrl = config.baseUrl,
            apiKey = config.apiKey,
            sttModel = config.sttModel,
            chatModel = config.chatModel,
            themeMode = themePreferences.themeMode.value
        )
    }

    fun onThemeModeChange(mode: ThemeMode) {
        themePreferences.setThemeMode(mode)
    }

    fun onBaseUrlChange(value: String) {
        _uiState.update {
            it.copy(
                baseUrl = value,
                isSaved = false,
                connectionStatus = GatewayConnectionStatus.UNTESTED,
                connectionError = null
            )
        }
    }

    fun onApiKeyChange(value: String) {
        _uiState.update {
            it.copy(
                apiKey = value,
                isSaved = false,
                connectionStatus = GatewayConnectionStatus.UNTESTED,
                connectionError = null
            )
        }
    }

    fun onSttModelChange(value: String) {
        _uiState.update { it.copy(sttModel = value, isSaved = false) }
    }

    fun onChatModelChange(value: String) {
        _uiState.update { it.copy(chatModel = value, isSaved = false) }
    }

    fun saveSettings() {
        val current = _uiState.value
        settingsRepository.saveSettings(
            baseUrl = current.baseUrl,
            apiKey = current.apiKey,
            sttModel = current.sttModel,
            chatModel = current.chatModel
        )
        val saved = settingsRepository.getCurrentConfig()
        _uiState.update {
            it.copy(
                baseUrl = saved.baseUrl,
                apiKey = saved.apiKey,
                sttModel = saved.sttModel,
                chatModel = saved.chatModel,
                isSaved = true
            )
        }
    }

    fun testConnection() {
        saveSettings()
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    connectionStatus = GatewayConnectionStatus.VERIFYING,
                    connectionError = null
                )
            }

            settingsRepository.fetchAvailableModels()
                .onSuccess { models ->
                    _uiState.update {
                        it.copy(
                            availableModels = models.map { model -> model.id },
                            connectionStatus = GatewayConnectionStatus.CONNECTED,
                            connectionError = null
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            availableModels = emptyList(),
                            connectionStatus = GatewayConnectionStatus.ERROR,
                            connectionError = throwable.message ?: throwable::class.java.simpleName
                        )
                    }
                }
        }
    }

    fun loadSampleLecture(onLoaded: () -> Unit) {
        viewModelScope.launch {
            recordingRepository.insertSampleLecture()
            onLoaded()
        }
    }

    class Factory(
        private val settingsRepository: SettingsRepository,
        private val recordingRepository: RecordingRepository,
        private val themePreferences: ThemePreferences
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
                "Unknown ViewModel class: ${modelClass.name}"
            }
            return SettingsViewModel(
                settingsRepository = settingsRepository,
                recordingRepository = recordingRepository,
                themePreferences = themePreferences
            ) as T
        }
    }
}
