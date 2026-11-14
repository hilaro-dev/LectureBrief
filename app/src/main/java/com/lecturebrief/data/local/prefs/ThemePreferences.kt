package com.lecturebrief.data.local.prefs

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode(val label: String) {
    SYSTEM_DEFAULT("System Default"),
    LIGHT("Light"),
    DARK("Dark")
}

class ThemePreferences(context: Context) {

    private val sharedPreferences: SharedPreferences = context.applicationContext
        .getSharedPreferences(PREFS_FILE_NAME, Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(readThemeMode())
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        sharedPreferences.edit()
            .putString(KEY_THEME_MODE, mode.name)
            .apply()
        _themeMode.value = mode
    }

    private fun readThemeMode(): ThemeMode {
        val raw = sharedPreferences.getString(KEY_THEME_MODE, ThemeMode.SYSTEM_DEFAULT.name)
        return runCatching { ThemeMode.valueOf(raw ?: "") }
            .getOrDefault(ThemeMode.SYSTEM_DEFAULT)
    }

    companion object {
        private const val PREFS_FILE_NAME = "lecturebrief_theme_prefs"
        private const val KEY_THEME_MODE = "theme_mode"
    }
}
