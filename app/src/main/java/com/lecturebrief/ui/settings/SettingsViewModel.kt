package com.lecturebrief.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lecturebrief.data.local.prefs.ModelTier
import com.lecturebrief.data.local.prefs.ProviderPreset
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
        val effectiveChatPreset = if (config.useSameProvider) config.sttPreset else config.chatPreset
        return SettingsUiState(
            useSameProvider = config.useSameProvider,
            sttPreset = config.sttPreset,
            baseUrl = config.baseUrl,
            apiKey = config.apiKey,
            sttModel = config.sttModel,
            sttTier = inferSttTier(config.sttPreset, config.sttModel),
            chatPreset = config.chatPreset,
            chatBaseUrl = config.chatBaseUrl,
            chatApiKey = config.chatApiKey,
            chatModel = config.chatModel,
            chatTier = inferChatTier(effectiveChatPreset, config.chatModel),
            themeMode = themePreferences.themeMode.value
        )
    }

    fun onThemeModeChange(mode: ThemeMode) {
        themePreferences.setThemeMode(mode)
    }

    fun onUseSameProviderChange(enabled: Boolean) {
        _uiState.update { state ->
            val effectiveChatPreset = if (enabled) state.sttPreset else state.chatPreset
            val updatedChatModel = if (enabled && state.sttPreset != ProviderPreset.CUSTOM && state.sttPreset.defaultChatModel.isNotBlank()) {
                state.sttPreset.defaultChatModel
            } else {
                state.chatModel
            }
            state.copy(
                useSameProvider = enabled,
                chatModel = updatedChatModel,
                chatTier = inferChatTier(effectiveChatPreset, updatedChatModel),
                isSaved = false,
                connectionStatus = GatewayConnectionStatus.UNTESTED,
                connectionSummary = null,
                connectionError = null
            )
        }
    }

    fun onSttPresetSelected(preset: ProviderPreset) {
        _uiState.update { state ->
            val updatedBaseUrl = if (preset != ProviderPreset.CUSTOM) {
                preset.defaultBaseUrl
            } else {
                state.baseUrl
            }
            val updatedSttModel = if (preset != ProviderPreset.CUSTOM && preset.defaultSttModel.isNotBlank()) {
                preset.defaultSttModel
            } else {
                state.sttModel
            }
            val updatedChatModel = if (
                state.useSameProvider &&
                preset != ProviderPreset.CUSTOM &&
                preset.defaultChatModel.isNotBlank()
            ) {
                preset.defaultChatModel
            } else {
                state.chatModel
            }
            val effectiveChatPreset = if (state.useSameProvider) preset else state.chatPreset

            state.copy(
                sttPreset = preset,
                baseUrl = updatedBaseUrl,
                sttModel = updatedSttModel,
                sttTier = inferSttTier(preset, updatedSttModel),
                chatPreset = if (state.useSameProvider) preset else state.chatPreset,
                chatModel = updatedChatModel,
                chatTier = inferChatTier(effectiveChatPreset, updatedChatModel),
                isSaved = false,
                connectionStatus = GatewayConnectionStatus.UNTESTED,
                connectionSummary = null,
                connectionError = null
            )
        }
    }

    fun onChatPresetSelected(preset: ProviderPreset) {
        _uiState.update { state ->
            val updatedChatBaseUrl = if (preset != ProviderPreset.CUSTOM) {
                preset.defaultBaseUrl
            } else {
                state.chatBaseUrl
            }
            val updatedChatModel = if (preset != ProviderPreset.CUSTOM && preset.defaultChatModel.isNotBlank()) {
                preset.defaultChatModel
            } else {
                state.chatModel
            }

            state.copy(
                chatPreset = preset,
                chatBaseUrl = updatedChatBaseUrl,
                chatModel = updatedChatModel,
                chatTier = inferChatTier(preset, updatedChatModel),
                isSaved = false,
                connectionStatus = GatewayConnectionStatus.UNTESTED,
                connectionSummary = null,
                connectionError = null
            )
        }
    }

    fun onSttTierSelected(tier: ModelTier) {
        _uiState.update { state ->
            val model = state.sttPreset.sttTiers().firstOrNull { it.first == tier }?.second
                ?: return@update state
            state.copy(
                sttTier = tier,
                sttModel = model,
                isSaved = false
            )
        }
    }

    fun onChatTierSelected(tier: ModelTier) {
        _uiState.update { state ->
            val activePreset = if (state.useSameProvider) state.sttPreset else state.chatPreset
            val model = activePreset.chatTiers().firstOrNull { it.first == tier }?.second
                ?: return@update state
            state.copy(
                chatTier = tier,
                chatModel = model,
                isSaved = false
            )
        }
    }

    fun onBaseUrlChange(value: String) {
        _uiState.update {
            it.copy(
                baseUrl = value,
                isSaved = false,
                connectionStatus = GatewayConnectionStatus.UNTESTED,
                connectionSummary = null,
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
                connectionSummary = null,
                connectionError = null
            )
        }
    }

    fun onChatBaseUrlChange(value: String) {
        _uiState.update {
            it.copy(
                chatBaseUrl = value,
                isSaved = false,
                connectionStatus = GatewayConnectionStatus.UNTESTED,
                connectionSummary = null,
                connectionError = null
            )
        }
    }

    fun onChatApiKeyChange(value: String) {
        _uiState.update {
            it.copy(
                chatApiKey = value,
                isSaved = false,
                connectionStatus = GatewayConnectionStatus.UNTESTED,
                connectionSummary = null,
                connectionError = null
            )
        }
    }

    fun onSttModelChange(value: String) {
        _uiState.update { state ->
            state.copy(
                sttModel = value,
                sttTier = inferSttTier(state.sttPreset, value),
                isSaved = false
            )
        }
    }

    fun onChatModelChange(value: String) {
        _uiState.update { state ->
            val effectivePreset = if (state.useSameProvider) state.sttPreset else state.chatPreset
            state.copy(
                chatModel = value,
                chatTier = inferChatTier(effectivePreset, value),
                isSaved = false
            )
        }
    }

    fun saveSettings() {
        val current = _uiState.value
        settingsRepository.saveSettings(
            baseUrl = current.baseUrl,
            apiKey = current.apiKey,
            sttModel = current.sttModel,
            chatModel = current.chatModel,
            useSameProvider = current.useSameProvider,
            sttPreset = current.sttPreset,
            chatPreset = if (current.useSameProvider) current.sttPreset else current.chatPreset,
            chatBaseUrl = if (current.useSameProvider) current.baseUrl else current.chatBaseUrl,
            chatApiKey = if (current.useSameProvider) current.apiKey else current.chatApiKey
        )
        val saved = settingsRepository.getCurrentConfig()
        val effectiveChatPreset = if (saved.useSameProvider) saved.sttPreset else saved.chatPreset
        _uiState.update {
            it.copy(
                useSameProvider = saved.useSameProvider,
                sttPreset = saved.sttPreset,
                baseUrl = saved.baseUrl,
                apiKey = saved.apiKey,
                sttModel = saved.sttModel,
                sttTier = it.sttTier ?: inferSttTier(saved.sttPreset, saved.sttModel),
                chatPreset = saved.chatPreset,
                chatBaseUrl = saved.chatBaseUrl,
                chatApiKey = saved.chatApiKey,
                chatModel = saved.chatModel,
                chatTier = it.chatTier ?: inferChatTier(effectiveChatPreset, saved.chatModel),
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
                    connectionSummary = null,
                    connectionError = null
                )
            }

            settingsRepository.testConnections()
                .onSuccess { result ->
                    _uiState.update {
                        it.copy(
                            availableModels = result.models.map { model -> model.id },
                            connectionStatus = GatewayConnectionStatus.CONNECTED,
                            connectionSummary = result.summaryMessage,
                            connectionError = null
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            availableModels = emptyList(),
                            connectionStatus = GatewayConnectionStatus.ERROR,
                            connectionSummary = null,
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

    private fun inferSttTier(preset: ProviderPreset, modelId: String): ModelTier? {
        val trimmed = modelId.trim()
        return preset.sttTiers().firstOrNull { it.second == trimmed }?.first
    }

    private fun inferChatTier(preset: ProviderPreset, modelId: String): ModelTier? {
        val trimmed = modelId.trim()
        return preset.chatTiers().firstOrNull { it.second == trimmed }?.first
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
