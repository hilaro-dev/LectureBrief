package com.lecturebrief.ui.state

import androidx.compose.runtime.Immutable
import com.lecturebrief.data.local.prefs.AppColorPalette
import com.lecturebrief.data.local.prefs.ModelTier
import com.lecturebrief.data.local.prefs.ProviderPreset
import com.lecturebrief.data.local.prefs.SecureSettingsStorage
import com.lecturebrief.data.local.prefs.ThemeMode

enum class GatewayConnectionStatus {
    UNTESTED,
    VERIFYING,
    CONNECTED,
    ERROR
}

@Immutable
data class SettingsUiState(
    val useSameProvider: Boolean = false,
    val sttPreset: ProviderPreset = ProviderPreset.GROQ,
    val baseUrl: String = SecureSettingsStorage.DEFAULT_BASE_URL,
    val apiKey: String = "",
    val sttModel: String = SecureSettingsStorage.DEFAULT_STT_MODEL,
    val sttTier: ModelTier? = ModelTier.HIGH,
    val chatPreset: ProviderPreset = ProviderPreset.GEMINI,
    val chatBaseUrl: String = SecureSettingsStorage.DEFAULT_CHAT_BASE_URL,
    val chatApiKey: String = "",
    val chatModel: String = SecureSettingsStorage.DEFAULT_CHAT_MODEL,
    val chatTier: ModelTier? = ModelTier.MEDIUM,
    val availableModels: List<String> = emptyList(),
    val connectionStatus: GatewayConnectionStatus = GatewayConnectionStatus.UNTESTED,
    val connectionSummary: String? = null,
    val connectionError: String? = null,
    val isSaved: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM_DEFAULT,
    val colorPalette: AppColorPalette = AppColorPalette.MIDNIGHT_PERIWINKLE
)
