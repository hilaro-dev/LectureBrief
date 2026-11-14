package com.lecturebrief.data.local.prefs

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode(val label: String) {
    SYSTEM_DEFAULT("System"),
    LIGHT("Light"),
    DARK("Dark")
}

enum class AppColorPalette(
    val label: String,
    val swatchArgb: Long
) {
    MIDNIGHT_PERIWINKLE("Midnight Periwinkle", 0xFF4F6DF5),
    DEEP_TEAL("Deep Teal", 0xFF1E4D5E),
    ROYAL_VIOLET("Royal Violet", 0xFF7C4DFF),
    EMERALD_FOREST("Emerald Forest", 0xFF2E7D32),
    CRIMSON_ROSE("Crimson Rose", 0xFFC62828),
    WARM_AMBER("Warm Amber", 0xFFEF6C00),
    WALLPAPER("Wallpaper", 0xFF757575)
}

class ThemePreferences(context: Context) {

    private val sharedPreferences: SharedPreferences = context.applicationContext
        .getSharedPreferences(PREFS_FILE_NAME, Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(readThemeMode())
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _colorPalette = MutableStateFlow(readColorPalette())
    val colorPalette: StateFlow<AppColorPalette> = _colorPalette.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        sharedPreferences.edit()
            .putString(KEY_THEME_MODE, mode.name)
            .apply()
        _themeMode.value = mode
    }

    fun setColorPalette(palette: AppColorPalette) {
        sharedPreferences.edit()
            .putString(KEY_COLOR_PALETTE, palette.name)
            .apply()
        _colorPalette.value = palette
    }

    private fun readThemeMode(): ThemeMode {
        val raw = sharedPreferences.getString(KEY_THEME_MODE, ThemeMode.SYSTEM_DEFAULT.name)
        return runCatching { ThemeMode.valueOf(raw ?: "") }
            .getOrDefault(ThemeMode.SYSTEM_DEFAULT)
    }

    private fun readColorPalette(): AppColorPalette {
        val raw = sharedPreferences.getString(KEY_COLOR_PALETTE, AppColorPalette.MIDNIGHT_PERIWINKLE.name)
        return runCatching { AppColorPalette.valueOf(raw ?: "") }
            .getOrDefault(AppColorPalette.MIDNIGHT_PERIWINKLE)
    }

    companion object {
        private const val PREFS_FILE_NAME = "lecturebrief_theme_prefs"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_COLOR_PALETTE = "color_palette"
    }
}
