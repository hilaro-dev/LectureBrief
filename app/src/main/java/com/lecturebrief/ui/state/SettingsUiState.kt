package com.lecturebrief.ui.state

import androidx.compose.runtime.Immutable
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
    val baseUrl: String = SecureSettingsStorage.DEFAULT_BASE_URL,
    val apiKey: String = "",
    val sttModel: String = SecureSettingsStorage.DEFAULT_STT_MODEL,
    val chatModel: String = SecureSettingsStorage.DEFAULT_CHAT_MODEL,
    val availableModels: List<String> = emptyList(),
    val connectionStatus: GatewayConnectionStatus = GatewayConnectionStatus.UNTESTED,
    val connectionError: String? = null,
    val isSaved: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM_DEFAULT
)
