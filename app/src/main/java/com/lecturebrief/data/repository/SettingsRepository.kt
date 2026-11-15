package com.lecturebrief.data.repository

import com.lecturebrief.data.local.prefs.GatewayConfig
import com.lecturebrief.data.local.prefs.ModelTier
import com.lecturebrief.data.local.prefs.ProviderPreset
import com.lecturebrief.data.local.prefs.SecureSettingsStorage
import com.lecturebrief.data.remote.GeminiModelResolver
import com.lecturebrief.data.remote.OmniRouteApi
import com.lecturebrief.data.remote.OmniRouteClient
import com.lecturebrief.data.remote.OmniRouteErrorFormatter
import com.lecturebrief.data.remote.dto.ModelDto
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

data class ConnectionTestResult(
    val models: List<ModelDto>,
    val summaryMessage: String
)

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
        chatModel: String,
        useSameProvider: Boolean = true,
        sttPreset: ProviderPreset = getCurrentConfig().sttPreset,
        chatPreset: ProviderPreset = getCurrentConfig().chatPreset,
        chatBaseUrl: String = baseUrl,
        chatApiKey: String = apiKey,
        summaryTier: ModelTier = getCurrentConfig().summaryTier
    ) {
        secureSettingsStorage.saveSettings(
            baseUrl = baseUrl,
            apiKey = apiKey,
            sttModel = sttModel,
            chatModel = chatModel,
            useSameProvider = useSameProvider,
            sttPreset = sttPreset,
            chatPreset = chatPreset,
            chatBaseUrl = chatBaseUrl,
            chatApiKey = chatApiKey,
            summaryTier = summaryTier
        )
    }

    fun updateSummaryTier(tier: ModelTier) {
        secureSettingsStorage.saveSummaryTier(tier)
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
            val response = omniRouteApi.getModels(target = OmniRouteClient.TARGET_STT)
            Result.success(response.data.sortedBy { it.id })
        } catch (t: Throwable) {
            Result.failure(IllegalStateException(OmniRouteErrorFormatter.format(t), t))
        }
    }

    suspend fun testConnections(): Result<ConnectionTestResult> = withContext(ioDispatcher) {
        val config = getCurrentConfig()
        val sttResult = runCatching {
            omniRouteApi.getModels(target = OmniRouteClient.TARGET_STT).data.sortedBy { it.id }
        }

        if (config.useSameProvider) {
            return@withContext sttResult.fold(
                onSuccess = { models ->
                    Result.success(
                        ConnectionTestResult(
                            models = models,
                            summaryMessage = "Connected (${models.size} available models)"
                        )
                    )
                },
                onFailure = { t ->
                    Result.failure(IllegalStateException(OmniRouteErrorFormatter.format(t), t))
                }
            )
        }

        val chatResult = if (GeminiModelResolver.isGeminiChatConfig(config)) {
            GeminiModelResolver.resolveBestModel(
                apiKey = config.effectiveChatApiKey,
                tier = config.summaryTier
            ).map { resolution ->
                secureSettingsStorage.saveSelectedModels(
                    sttModel = config.sttModel,
                    chatModel = resolution.resolvedModelId
                )
                resolution.availableModels.map { ModelDto(id = it) }
            }
        } else {
            runCatching {
                omniRouteApi.getModels(target = OmniRouteClient.TARGET_CHAT).data.sortedBy { it.id }
            }
        }

        val sttModels = sttResult.getOrNull()
        val chatModels = chatResult.getOrNull()

        if (sttModels != null && chatModels != null) {
            val combined = (sttModels + chatModels).distinctBy { it.id }.sortedBy { it.id }
            val resolvedChatModel = getCurrentConfig().chatModel
            val chatSummary = if (GeminiModelResolver.isGeminiChatConfig(config)) {
                "Summary: Connected (${chatModels.size} models, active: $resolvedChatModel)"
            } else {
                "Summary: Connected (${chatModels.size} models)"
            }
            Result.success(
                ConnectionTestResult(
                    models = combined,
                    summaryMessage = "STT: Connected (${sttModels.size} models)\n$chatSummary"
                )
            )
        } else {
            val errors = buildList {
                sttResult.exceptionOrNull()?.let { add("STT failed: ${OmniRouteErrorFormatter.format(it)}") }
                chatResult.exceptionOrNull()?.let { add("Summary failed: ${OmniRouteErrorFormatter.format(it)}") }
            }
            Result.failure(IllegalStateException(errors.joinToString("\n")))
        }
    }
}
