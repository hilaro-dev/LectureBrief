package com.lecturebrief.data.local.prefs

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ModelTier(val label: String) {
    LOW("Low"),
    MEDIUM("Medium"),
    HIGH("High")
}

enum class ProviderPreset(
    val displayName: String,
    val defaultBaseUrl: String,
    val defaultSttModel: String,
    val defaultChatModel: String
) {
    GROQ(
        displayName = "Groq Cloud",
        defaultBaseUrl = "https://api.groq.com/openai/v1",
        defaultSttModel = "whisper-large-v3",
        defaultChatModel = "qwen/qwen3.8-27b"
    ),
    GEMINI(
        displayName = "Google Gemini",
        defaultBaseUrl = "https://generativelanguage.googleapis.com/v1beta/openai",
        defaultSttModel = "",
        defaultChatModel = "gemini-3.8-flash"
    ),
    OMNIROUTE(
        displayName = "OmniRoute (PC Gateway)",
        defaultBaseUrl = "http://192.168.1.X:20128/v1",
        defaultSttModel = "groq/whisper-large-v3",
        defaultChatModel = "gemini-3.8-flash"
    ),
    CUSTOM(
        displayName = "Custom OpenAI-Compatible",
        defaultBaseUrl = "",
        defaultSttModel = "",
        defaultChatModel = ""
    );

    fun sttTiers(): List<Pair<ModelTier, String>> = when (this) {
        GROQ -> listOf(
            ModelTier.LOW to "whisper-large-v3-turbo",
            ModelTier.HIGH to "whisper-large-v3"
        )
        OMNIROUTE -> listOf(
            ModelTier.LOW to "groq/whisper-large-v3-turbo",
            ModelTier.HIGH to "groq/whisper-large-v3"
        )
        GEMINI, CUSTOM -> emptyList()
    }

    fun chatTiers(): List<Pair<ModelTier, String>> = when (this) {
        GEMINI -> listOf(
            ModelTier.LOW to "gemini-3.1-flash",
            ModelTier.MEDIUM to "gemini-3.8-flash",
            ModelTier.HIGH to "gemini-4-hydrogen"
        )
        GROQ -> listOf(
            ModelTier.LOW to "openai/gpt-oss-20b",
            ModelTier.MEDIUM to "qwen/qwen3.8-27b",
            ModelTier.HIGH to "openai/gpt-oss-120b"
        )
        OMNIROUTE -> listOf(
            ModelTier.LOW to "gemini-3.1-flash",
            ModelTier.MEDIUM to "gemini-3.8-flash",
            ModelTier.HIGH to "gemini-4-hydrogen"
        )
        CUSTOM -> emptyList()
    }

    companion object {
        fun fromStoredName(name: String?, default: ProviderPreset): ProviderPreset {
            return entries.firstOrNull { it.name == name } ?: default
        }
    }
}

data class GatewayConfig(
    val useSameProvider: Boolean = false,
    val sttPreset: ProviderPreset = ProviderPreset.GROQ,
    val baseUrl: String = SecureSettingsStorage.DEFAULT_BASE_URL,
    val apiKey: String = "",
    val sttModel: String = SecureSettingsStorage.DEFAULT_STT_MODEL,
    val chatPreset: ProviderPreset = ProviderPreset.GEMINI,
    val chatBaseUrl: String = SecureSettingsStorage.DEFAULT_CHAT_BASE_URL,
    val chatApiKey: String = "",
    val chatModel: String = SecureSettingsStorage.DEFAULT_CHAT_MODEL,
    val systemPrompt: String = SecureSettingsStorage.DEFAULT_SYSTEM_PROMPT
) {
    val effectiveChatBaseUrl: String
        get() = if (useSameProvider) baseUrl else chatBaseUrl

    val effectiveChatApiKey: String
        get() = if (useSameProvider) apiKey else chatApiKey
}

class SecureSettingsStorage(context: Context) {

    private val appContext = context.applicationContext

    private val sharedPreferences: SharedPreferences = createSharedPreferences(appContext)

    private val _configFlow = MutableStateFlow(readConfig())
    val configFlow: StateFlow<GatewayConfig> = _configFlow.asStateFlow()

    fun getCurrentConfig(): GatewayConfig = _configFlow.value

    fun saveSettings(
        baseUrl: String,
        apiKey: String,
        sttModel: String,
        chatModel: String,
        useSameProvider: Boolean = false,
        sttPreset: ProviderPreset = _configFlow.value.sttPreset,
        chatPreset: ProviderPreset = _configFlow.value.chatPreset,
        chatBaseUrl: String = DEFAULT_CHAT_BASE_URL,
        chatApiKey: String = ""
    ) {
        val normalizedSttUrl = normalizeBaseUrl(baseUrl, fallback = DEFAULT_BASE_URL)
        val normalizedChatUrl = normalizeBaseUrl(chatBaseUrl, fallback = DEFAULT_CHAT_BASE_URL)
        val normalizedStt = sttModel.trim().ifEmpty { DEFAULT_STT_MODEL }
        val normalizedChat = chatModel.trim().ifEmpty { DEFAULT_CHAT_MODEL }

        sharedPreferences.edit()
            .putBoolean(KEY_USE_SAME_PROVIDER, useSameProvider)
            .putString(KEY_STT_PRESET, sttPreset.name)
            .putString(KEY_BASE_URL, normalizedSttUrl)
            .putString(KEY_API_KEY, apiKey.trim())
            .putString(KEY_STT_MODEL, normalizedStt)
            .putString(KEY_CHAT_PRESET, chatPreset.name)
            .putString(KEY_CHAT_BASE_URL, normalizedChatUrl)
            .putString(KEY_CHAT_API_KEY, chatApiKey.trim())
            .putString(KEY_CHAT_MODEL, normalizedChat)
            .apply()
        _configFlow.value = readConfig()
    }

    fun saveGatewayCredentials(baseUrl: String, apiKey: String) {
        val normalizedUrl = normalizeBaseUrl(baseUrl)
        sharedPreferences.edit()
            .putString(KEY_BASE_URL, normalizedUrl)
            .putString(KEY_API_KEY, apiKey.trim())
            .apply()
        _configFlow.value = readConfig()
    }

    fun saveSelectedModels(sttModel: String, chatModel: String) {
        sharedPreferences.edit()
            .putString(KEY_STT_MODEL, sttModel.trim().ifEmpty { DEFAULT_STT_MODEL })
            .putString(KEY_CHAT_MODEL, chatModel.trim().ifEmpty { DEFAULT_CHAT_MODEL })
            .apply()
        _configFlow.value = readConfig()
    }

    fun saveSystemPrompt(systemPrompt: String) {
        sharedPreferences.edit()
            .putString(KEY_SYSTEM_PROMPT, systemPrompt.trim())
            .apply()
        _configFlow.value = readConfig()
    }

    fun clearApiKey() {
        sharedPreferences.edit()
            .remove(KEY_API_KEY)
            .remove(KEY_CHAT_API_KEY)
            .apply()
        _configFlow.value = readConfig()
    }

    private fun readConfig(): GatewayConfig {
        val sttUrl = normalizeBaseUrl(
            sharedPreferences.getString(KEY_BASE_URL, DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL,
            fallback = DEFAULT_BASE_URL
        )
        val sttKey = sharedPreferences.getString(KEY_API_KEY, "") ?: ""
        val chatUrl = normalizeBaseUrl(
            sharedPreferences.getString(KEY_CHAT_BASE_URL, DEFAULT_CHAT_BASE_URL) ?: DEFAULT_CHAT_BASE_URL,
            fallback = DEFAULT_CHAT_BASE_URL
        )
        val chatKey = sharedPreferences.getString(KEY_CHAT_API_KEY, "") ?: ""

        return GatewayConfig(
            useSameProvider = sharedPreferences.getBoolean(KEY_USE_SAME_PROVIDER, false),
            sttPreset = ProviderPreset.fromStoredName(
                sharedPreferences.getString(KEY_STT_PRESET, null),
                default = ProviderPreset.GROQ
            ),
            baseUrl = sttUrl,
            apiKey = sttKey,
            sttModel = sharedPreferences.getString(KEY_STT_MODEL, DEFAULT_STT_MODEL)
                ?.takeIf { it.isNotBlank() } ?: DEFAULT_STT_MODEL,
            chatPreset = ProviderPreset.fromStoredName(
                sharedPreferences.getString(KEY_CHAT_PRESET, null),
                default = ProviderPreset.GEMINI
            ),
            chatBaseUrl = chatUrl,
            chatApiKey = chatKey,
            chatModel = sharedPreferences.getString(KEY_CHAT_MODEL, DEFAULT_CHAT_MODEL)
                ?.takeIf { it.isNotBlank() } ?: DEFAULT_CHAT_MODEL,
            systemPrompt = sharedPreferences.getString(KEY_SYSTEM_PROMPT, DEFAULT_SYSTEM_PROMPT)
                ?: DEFAULT_SYSTEM_PROMPT
        )
    }

    private fun createSharedPreferences(context: Context): SharedPreferences {
        return runCatching {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                ENCRYPTED_PREFS_FILE_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }.getOrElse { error ->
            Log.w(
                TAG,
                "EncryptedSharedPreferences unavailable on this device/emulator; falling back to standard SharedPreferences.",
                error
            )
            context.getSharedPreferences(FALLBACK_PREFS_FILE_NAME, Context.MODE_PRIVATE)
        }
    }

    companion object {
        private const val TAG = "SecureSettingsStorage"
        private const val ENCRYPTED_PREFS_FILE_NAME = "lecturebrief_secure_prefs"
        private const val FALLBACK_PREFS_FILE_NAME = "lecturebrief_fallback_prefs"

        private const val KEY_USE_SAME_PROVIDER = "use_same_provider"
        private const val KEY_STT_PRESET = "stt_preset"
        private const val KEY_BASE_URL = "omniroute_base_url"
        private const val KEY_API_KEY = "omniroute_api_key"
        private const val KEY_STT_MODEL = "stt_model"
        private const val KEY_CHAT_PRESET = "chat_preset"
        private const val KEY_CHAT_BASE_URL = "chat_base_url"
        private const val KEY_CHAT_API_KEY = "chat_api_key"
        private const val KEY_CHAT_MODEL = "chat_model"
        private const val KEY_SYSTEM_PROMPT = "system_prompt"

        const val DEFAULT_BASE_URL = "https://api.groq.com/openai/v1"
        const val DEFAULT_CHAT_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/openai"
        const val DEFAULT_STT_MODEL = "whisper-large-v3"
        const val DEFAULT_CHAT_MODEL = "gemini-3.8-flash"
        const val DEFAULT_SYSTEM_PROMPT =
            "You are an academic lecture assistant. Summarize the lecture transcript into a concise overview, bulleted key takeaways, and concrete action items."

        fun normalizeBaseUrl(rawUrl: String, fallback: String = DEFAULT_BASE_URL): String {
            val trimmed = rawUrl.trim().ifEmpty { fallback }
            return trimmed.trimEnd('/')
        }
    }
}
