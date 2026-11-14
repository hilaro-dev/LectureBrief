package com.lecturebrief.data.remote

import com.lecturebrief.data.local.prefs.GatewayConfig
import com.lecturebrief.data.local.prefs.ModelTier
import com.lecturebrief.data.local.prefs.ProviderPreset
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

data class GeminiResolutionResult(
    val resolvedModelId: String,
    val availableModels: List<String>
)

@Serializable
private data class GeminiModelsListResponse(
    val models: List<GeminiModelEntry> = emptyList()
)

@Serializable
private data class GeminiModelEntry(
    val name: String = "",
    @SerialName("supportedGenerationMethods")
    val supportedGenerationMethods: List<String> = emptyList()
)

/**
 * Queries Google AI Studio's `v1beta/models` endpoint with the user's own API key
 * and resolves the best available Gemini model ID for the chosen quality tier.
 */
object GeminiModelResolver {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val EXCLUDED_KEYWORDS = listOf(
        "embedding",
        "imagen",
        "veo",
        "tts",
        "robotics",
        "computer-use"
    )

    private val LOW_PREFERENCES = listOf(
        "gemini-3.1-flash-lite-preview",
        "gemini-3.1-flash",
        "gemini-2.5-flash-lite",
        "gemini-2.5-flash"
    )

    private val MEDIUM_PREFERENCES = listOf(
        "gemini-3.8-flash",
        "gemini-3-flash-preview",
        "gemini-3.1-flash",
        "gemini-2.5-flash"
    )

    private val HIGH_PREFERENCES = listOf(
        "gemini-4-hydrogen",
        "gemini-3.1-pro-preview",
        "gemini-2.5-pro",
        "gemini-3-flash-preview"
    )

    fun isGeminiChatConfig(config: GatewayConfig): Boolean {
        val activePreset = if (config.useSameProvider) config.sttPreset else config.chatPreset
        return activePreset == ProviderPreset.GEMINI ||
            config.effectiveChatBaseUrl.contains("generativelanguage.googleapis.com", ignoreCase = true)
    }

    fun inferTier(modelId: String): ModelTier {
        val lower = modelId.trim().lowercase()
        ProviderPreset.GEMINI.chatTiers().firstOrNull { it.second == lower }?.let { return it.first }
        return when {
            lower.contains("lite") || lower.contains("3.1-flash") -> ModelTier.LOW
            lower.contains("pro") || lower.contains("hydrogen") || lower.contains("120b") -> ModelTier.HIGH
            else -> ModelTier.MEDIUM
        }
    }

    suspend fun resolveBestModel(
        apiKey: String,
        tier: ModelTier
    ): Result<GeminiResolutionResult> = withContext(Dispatchers.IO) {
        runCatching {
            val trimmedKey = apiKey.trim()
            require(trimmedKey.isNotEmpty()) { "Gemini API key is required" }

            val url = "https://generativelanguage.googleapis.com/v1beta/models"
                .toHttpUrl()
                .newBuilder()
                .addQueryParameter("key", trimmedKey)
                .build()

            val request = Request.Builder()
                .url(url)
                .get()
                .header("Accept", "application/json")
                .build()

            val responseBody = client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    throw IOException("Gemini models lookup failed (HTTP ${response.code}): ${bodyString.take(240)}")
                }
                bodyString
            }

            val parsed = json.decodeFromString<GeminiModelsListResponse>(responseBody)
            val generateCapable = parsed.models
                .filter { entry ->
                    entry.supportedGenerationMethods.any { it.equals("generateContent", ignoreCase = true) }
                }
                .map { it.name.removePrefix("models/").trim() }
                .filter { id ->
                    id.isNotBlank() && EXCLUDED_KEYWORDS.none { kw -> id.contains(kw, ignoreCase = true) }
                }

            val geminiModels = generateCapable.filter { !it.contains("gemma", ignoreCase = true) }
            val candidatePool = geminiModels.ifEmpty { generateCapable }
            require(candidatePool.isNotEmpty()) {
                "No generateContent-capable Gemini models were found for this API key."
            }

            val preferredOrder = when (tier) {
                ModelTier.LOW -> LOW_PREFERENCES
                ModelTier.MEDIUM -> MEDIUM_PREFERENCES
                ModelTier.HIGH -> HIGH_PREFERENCES
            }

            val bestModel = preferredOrder.firstOrNull { preferred ->
                candidatePool.any { it.equals(preferred, ignoreCase = true) }
            } ?: candidatePool.first()

            GeminiResolutionResult(
                resolvedModelId = bestModel,
                availableModels = candidatePool.sorted()
            )
        }
    }
}
