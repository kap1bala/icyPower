package com.kap1bala.icypower.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Root theme.
 *
 * Material3's ColorScheme has no slot for `warning` / `success`; we expose
 * them via CompositionLocals so components read them as `LocalWarning.current`
 * without touching hex directly.
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
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    val warning: Color = if (darkTheme) WarningDark else Warning
    val danger: Color = if (darkTheme) DangerDark else Danger
    val success: Color = if (darkTheme) SuccessDark else Success

    CompositionLocalProvider(
        LocalWarning provides warning,
        LocalDanger provides danger,
        LocalSuccess provides success,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content,
        )
    }
}

val LocalWarning = staticCompositionLocalOf { Warning }
val LocalDanger = staticCompositionLocalOf { Danger }
val LocalSuccess = staticCompositionLocalOf { Success }

private val LightColors: ColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = Neutral,
    primaryContainer = Primary.copy(alpha = 0.12f),
    onPrimaryContainer = Primary,
    secondary = Secondary,
    onSecondary = Neutral,
    secondaryContainer = Secondary.copy(alpha = 0.12f),
    onSecondaryContainer = Secondary,
    tertiary = Secondary,
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
