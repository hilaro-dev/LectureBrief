package com.lecturebrief.data.local.prefs

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class GatewayConfig(
    val baseUrl: String = SecureSettingsStorage.DEFAULT_BASE_URL,
    val apiKey: String = "",
    val sttModel: String = SecureSettingsStorage.DEFAULT_STT_MODEL,
    val chatModel: String = SecureSettingsStorage.DEFAULT_CHAT_MODEL,
    val systemPrompt: String = SecureSettingsStorage.DEFAULT_SYSTEM_PROMPT
)

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
        chatModel: String
    ) {
        val normalizedUrl = normalizeBaseUrl(baseUrl)
        val normalizedStt = sttModel.trim().ifEmpty { DEFAULT_STT_MODEL }
        val normalizedChat = chatModel.trim().ifEmpty { DEFAULT_CHAT_MODEL }

        sharedPreferences.edit()
            .putString(KEY_BASE_URL, normalizedUrl)
            .putString(KEY_API_KEY, apiKey.trim())
            .putString(KEY_STT_MODEL, normalizedStt)
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
            .apply()
        _configFlow.value = readConfig()
    }

    private fun readConfig(): GatewayConfig {
        return GatewayConfig(
            baseUrl = normalizeBaseUrl(
                sharedPreferences.getString(KEY_BASE_URL, DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL
            ),
            apiKey = sharedPreferences.getString(KEY_API_KEY, "") ?: "",
            sttModel = sharedPreferences.getString(KEY_STT_MODEL, DEFAULT_STT_MODEL)
                ?.takeIf { it.isNotBlank() } ?: DEFAULT_STT_MODEL,
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

        private const val KEY_BASE_URL = "omniroute_base_url"
        private const val KEY_API_KEY = "omniroute_api_key"
        private const val KEY_STT_MODEL = "stt_model"
        private const val KEY_CHAT_MODEL = "chat_model"
        private const val KEY_SYSTEM_PROMPT = "system_prompt"

        const val DEFAULT_BASE_URL = "http://10.0.2.2:20128/v1"
        const val DEFAULT_STT_MODEL = "whisper-large-v3"
        const val DEFAULT_CHAT_MODEL = "auto"
        const val DEFAULT_SYSTEM_PROMPT =
            "You are an academic lecture assistant. Summarize the lecture transcript into a concise overview, bulleted key takeaways, and concrete action items."

        fun normalizeBaseUrl(rawUrl: String): String {
            val trimmed = rawUrl.trim().ifEmpty { DEFAULT_BASE_URL }
            return trimmed.trimEnd('/')
        }
    }
}
