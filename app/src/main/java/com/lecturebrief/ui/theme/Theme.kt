package com.lecturebrief.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.lecturebrief.data.local.prefs.AppColorPalette
import com.lecturebrief.data.local.prefs.ThemeMode

private val DefaultLightColorScheme = lightColorScheme()
private val DefaultDarkColorScheme = darkColorScheme()

private val DeepTealLight = lightColorScheme(
    primary = Color(0xFF1E4D5E),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFBFE8F6),
    onPrimaryContainer = Color(0xFF062733),
    secondary = Color(0xFF2A7B88),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCDE9EE),
    onSecondaryContainer = Color(0xFF0A2A30),
    tertiary = Color(0xFFE08E26),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFE0B2),
    onTertiaryContainer = Color(0xFF3A1F00)
)

private val DeepTealDark = darkColorScheme(
    primary = Color(0xFF83D0E5),
    onPrimary = Color(0xFF003544),
    primaryContainer = Color(0xFF1E4D5E),
    onPrimaryContainer = Color(0xFFC6ECF8),
    secondary = Color(0xFF7FD0DC),
    onSecondary = Color(0xFF00363E),
    secondaryContainer = Color(0xFF1A4F58),
    onSecondaryContainer = Color(0xFFCDE9EE),
    tertiary = Color(0xFFFFB74D),
    onTertiary = Color(0xFF422500),
    tertiaryContainer = Color(0xFF5F3800),
    onTertiaryContainer = Color(0xFFFFE0B2)
)

private val RoyalVioletLight = lightColorScheme(
    primary = Color(0xFF6535E8),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE8DDFF),
    onPrimaryContainer = Color(0xFF21005E),
    secondary = Color(0xFF7C4DFF),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFEDE7FF),
    onSecondaryContainer = Color(0xFF28125C),
    tertiary = Color(0xFF9C4DCC),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF6D9FF),
    onTertiaryContainer = Color(0xFF34004A)
)

private val RoyalVioletDark = darkColorScheme(
    primary = Color(0xFFB388FF),
    onPrimary = Color(0xFF2D007E),
    primaryContainer = Color(0xFF4A25B8),
    onPrimaryContainer = Color(0xFFE8DDFF),
    secondary = Color(0xFFCBB2FF),
    onSecondary = Color(0xFF331478),
    secondaryContainer = Color(0xFF47278E),
    onSecondaryContainer = Color(0xFFEDE7FF),
    tertiary = Color(0xFFE3B5FF),
    onTertiary = Color(0xFF4B0C6B),
    tertiaryContainer = Color(0xFF662487),
    onTertiaryContainer = Color(0xFFF6D9FF)
)

private val EmeraldForestLight = lightColorScheme(
    primary = Color(0xFF2E7D32),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFC8E6C9),
    onPrimaryContainer = Color(0xFF0A2E0D),
    secondary = Color(0xFF388E3C),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD7F0D9),
    onSecondaryContainer = Color(0xFF0E3411),
    tertiary = Color(0xFF00796B),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFB2DFDB),
    onTertiaryContainer = Color(0xFF002520)
)

private val EmeraldForestDark = darkColorScheme(
    primary = Color(0xFF81C784),
    onPrimary = Color(0xFF0B380E),
    primaryContainer = Color(0xFF1F5A23),
    onPrimaryContainer = Color(0xFFC8E6C9),
    secondary = Color(0xFFA5D6A7),
    onSecondary = Color(0xFF123F16),
    secondaryContainer = Color(0xFF27582B),
    onSecondaryContainer = Color(0xFFD7F0D9),
    tertiary = Color(0xFF80CBC4),
    onTertiary = Color(0xFF003731),
    tertiaryContainer = Color(0xFF005047),
    onTertiaryContainer = Color(0xFFB2DFDB)
)

private val CrimsonRoseLight = lightColorScheme(
    primary = Color(0xFFC62828),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDAD6),
    onPrimaryContainer = Color(0xFF410002),
    secondary = Color(0xFFD84343),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFE3E0),
    onSecondaryContainer = Color(0xFF4A0A0A),
    tertiary = Color(0xFFAD1457),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFD9E3),
    onTertiaryContainer = Color(0xFF3E001D)
)

private val CrimsonRoseDark = darkColorScheme(
    primary = Color(0xFFEF9A9A),
    onPrimary = Color(0xFF5F0A0A),
    primaryContainer = Color(0xFF8E1B1B),
    onPrimaryContainer = Color(0xFFFFDAD6),
    secondary = Color(0xFFFFB4AB),
    onSecondary = Color(0xFF680C0C),
    secondaryContainer = Color(0xFF872222),
    onSecondaryContainer = Color(0xFFFFE3E0),
    tertiary = Color(0xFFF48FB1),
    onTertiary = Color(0xFF5C0029),
    tertiaryContainer = Color(0xFF801242),
    onTertiaryContainer = Color(0xFFFFD9E3)
)

private val WarmAmberLight = lightColorScheme(
    primary = Color(0xFFEF6C00),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFE0B2),
    onPrimaryContainer = Color(0xFF361700),
    secondary = Color(0xFFF57C00),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFE8CC),
    onSecondaryContainer = Color(0xFF3B1B00),
    tertiary = Color(0xFF8D6E63),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFD7CCC8),
    onTertiaryContainer = Color(0xFF2A1C17)
)

private val WarmAmberDark = darkColorScheme(
    primary = Color(0xFFFFCC80),
    onPrimary = Color(0xFF4B2100),
    primaryContainer = Color(0xFF9C4600),
    onPrimaryContainer = Color(0xFFFFE0B2),
    secondary = Color(0xFFFFB74D),
    onSecondary = Color(0xFF472000),
    secondaryContainer = Color(0xFF7D3B00),
    onSecondaryContainer = Color(0xFFFFE8CC),
    tertiary = Color(0xFFBCAAA4),
    onTertiary = Color(0xFF33231D),
    tertiaryContainer = Color(0xFF523C34),
    onTertiaryContainer = Color(0xFFD7CCC8)
)

@Composable
fun LectureBriefTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM_DEFAULT,
    colorPalette: AppColorPalette = AppColorPalette.MIDNIGHT_PERIWINKLE,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM_DEFAULT -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colorScheme: ColorScheme = when (colorPalette) {
        AppColorPalette.MIDNIGHT_PERIWINKLE -> when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }
            darkTheme -> DefaultDarkColorScheme
            else -> DefaultLightColorScheme
        }
        AppColorPalette.DEEP_TEAL -> if (darkTheme) DeepTealDark else DeepTealLight
        AppColorPalette.ROYAL_VIOLET -> if (darkTheme) RoyalVioletDark else RoyalVioletLight
        AppColorPalette.EMERALD_FOREST -> if (darkTheme) EmeraldForestDark else EmeraldForestLight
        AppColorPalette.CRIMSON_ROSE -> if (darkTheme) CrimsonRoseDark else CrimsonRoseLight
        AppColorPalette.WARM_AMBER -> if (darkTheme) WarmAmberDark else WarmAmberLight
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
