package com.kap1bala.icypower.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Root theme.
 *
 * Why no dynamicColor?
 *   feat.md §5.3 forbids color drift away from ui.md. Material3's dynamic
 *   scheme would follow the OS wallpaper — disable it unconditionally.
 *
 * Cold-start flash:
 *   We accept a brief ~50ms flicker from the default light scheme to the
 *   persisted ThemeMode once DataStore resolves. This is Android, not a web
 *   page — there is no equivalent of pre-injecting a <style> tag. The base
 *   `Theme.Material3.DayNight.NoActionBar` in themes.xml already minimizes
 *   the flash on the system window background side.
 */
@Composable
fun IcyPowerTheme(
    themeMode: ThemeMode = ThemeMode.DEFAULT,
    content: @Composable () -> Unit,
) {
    val darkTheme: Boolean = when (themeMode) {
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
        ThemeMode.System -> isSystemInDarkTheme()
    }

    val colorScheme: ColorScheme = if (darkTheme) DarkColors else LightColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // System bars stay transparent (enableEdgeToEdge handles it) —
            // we only need to flip the icon color so they remain legible
            // against our surface.
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}

/**
 * Light scheme built strictly from ui.md `colors:`.
 *
 * Material3 ColorScheme has many slots we don't have 1:1 tokens for; we
 * derive them with conventional pairings (primary → primaryContainer = a
 * desaturated tint, etc.). If a token is added to ui.md later, mirror it
 * here.
 */
private val LightColors: ColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = Neutral,
    primaryContainer = Primary.copy(alpha = 0.12f),
    onPrimaryContainer = Primary,

    secondary = Secondary,
    onSecondary = Neutral,
    secondaryContainer = Secondary.copy(alpha = 0.12f),
    onSecondaryContainer = Secondary,

    tertiary = Secondary, // reuse — ui.md doesn't define a separate tertiary
    onTertiary = Neutral,
    tertiaryContainer = Secondary.copy(alpha = 0.12f),
    onTertiaryContainer = Secondary,

    error = Danger,
    onError = Neutral,
    errorContainer = Danger.copy(alpha = 0.12f),
    onErrorContainer = Danger,

    background = SurfaceLight,
    onBackground = TextLight,

    surface = SurfaceLight,
    onSurface = TextLight,
    surfaceVariant = SurfaceLight,
    onSurfaceVariant = TextLight.copy(alpha = 0.7f),

    outline = TextLight.copy(alpha = 0.20f),
    outlineVariant = TextLight.copy(alpha = 0.10f),

    inverseSurface = TextLight,
    inverseOnSurface = SurfaceLight,
    inversePrimary = Primary,

    scrim = TextLight.copy(alpha = 0.32f),
    surfaceTint = Primary,
)

/**
 * Dark scheme built strictly from ui.md `colorsDark:`.
 */
private val DarkColors: ColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = TextInverseDark,
    primaryContainer = PrimaryDark.copy(alpha = 0.18f),
    onPrimaryContainer = PrimaryDark,

    secondary = SecondaryDark,
    onSecondary = TextInverseDark,
    secondaryContainer = SecondaryDark.copy(alpha = 0.18f),
    onSecondaryContainer = SecondaryDark,

    tertiary = SecondaryDark,
    onTertiary = TextInverseDark,
    tertiaryContainer = SecondaryDark.copy(alpha = 0.18f),
    onTertiaryContainer = SecondaryDark,

    error = DangerDark,
    onError = TextInverseDark,
    errorContainer = DangerSoftDark,
    onErrorContainer = DangerDark,

    background = SurfaceDark,
    onBackground = TextDark,

    surface = SurfaceDark,
    onSurface = TextDark,
    surfaceVariant = SurfaceElevatedDark,
    onSurfaceVariant = TextMutedDark,

    outline = BorderStrongDark,
    outlineVariant = BorderDark,

    inverseSurface = TextDark,
    inverseOnSurface = SurfaceDark,
    inversePrimary = Primary,

    scrim = SurfaceDark.copy(alpha = 0.6f),
    surfaceTint = PrimaryDark,
)
