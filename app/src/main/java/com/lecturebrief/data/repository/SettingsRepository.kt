package com.lecturebrief.data.repository

import com.lecturebrief.data.local.prefs.GatewayConfig
import com.lecturebrief.data.local.prefs.SecureSettingsStorage
import com.lecturebrief.data.remote.OmniRouteApi
import com.lecturebrief.data.remote.OmniRouteErrorFormatter
import com.lecturebrief.data.remote.dto.ModelDto
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

class SettingsRepository(
    private val secureSettingsStorage: SecureSettingsStorage,
    private val omniRouteApi: OmniRouteApi,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    val configFlow: StateFlow<GatewayConfig> = secureSettingsStorage.configFlow

    fun getCurrentConfig(): GatewayConfig = secureSettingsStorage.getCurrentConfig()

    fun saveSettings(
        baseUrl: String,
        apiKey: String,
        sttModel: String,
        chatModel: String
    ) {
        secureSettingsStorage.saveSettings(
            baseUrl = baseUrl,
            apiKey = apiKey,
            sttModel = sttModel,
            chatModel = chatModel
        )
    }

    fun updateGatewayCredentials(baseUrl: String, apiKey: String) {
        secureSettingsStorage.saveGatewayCredentials(baseUrl = baseUrl, apiKey = apiKey)
    }

    fun updateSelectedModels(sttModel: String, chatModel: String) {
        secureSettingsStorage.saveSelectedModels(
            sttModel = sttModel,
            chatModel = chatModel
        )
    }

    fun updateSystemPrompt(systemPrompt: String) {
        secureSettingsStorage.saveSystemPrompt(systemPrompt)
    }

    suspend fun fetchAvailableModels(): Result<List<ModelDto>> = withContext(ioDispatcher) {
        try {
            val response = omniRouteApi.getModels()
            Result.success(response.data.sortedBy { it.id })
        } catch (t: Throwable) {
            Result.failure(IllegalStateException(OmniRouteErrorFormatter.format(t), t))
        }
    }
}
