package com.kap1bala.icypower.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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
 *
 * Token bundles (single source of truth):
 *   - Colors: `LocalWarning` / `LocalDanger` / `LocalSuccess` (Material3
 *     ColorScheme has no slot for `warning` / `success`, so they are
 *     exposed as CompositionLocals here.)
 *   - Spacing scale (xxs..xxl) → `LocalSpacing`
 *   - Radius scale (xs/sm/md/lg) → `LocalRadius`
 *   - Shadow tokens (none / layer1 / layer2 / layer3) → `LocalShadow`
 *   - Motion durations (fast/base/slow) and easing → `LocalMotion`
 *
 * Component code **must** read these via `LocalXxx.current` rather than
 * hardcoding `8.dp` / `Color(0xFF...)`. See `prompts/ui.md` §10.
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

    // Semantic colors that Material3 cannot express (no warning / success slot).
    val warning: Color = if (darkTheme) WarningDark else Warning
    val danger: Color = if (darkTheme) DangerDark else Danger
    val success: Color = if (darkTheme) SuccessDark else Success

    // Soft container colors for badge backgrounds.
    val primarySoft: Color = if (darkTheme) PrimarySoftDark else PrimarySoftLight
    val secondarySoft: Color = if (darkTheme) SecondarySoftDark else SecondarySoftLight
    val successSoft: Color = if (darkTheme) SuccessSoftDark else SuccessSoftLight
    val warningSoft: Color = if (darkTheme) WarningSoftDark else WarningSoftLight
    val dangerSoft: Color = if (darkTheme) DangerSoftDark else DangerSoftLight

    CompositionLocalProvider(
        LocalWarning provides warning,
        LocalDanger provides danger,
        LocalSuccess provides success,
        LocalPrimarySoft provides primarySoft,
        LocalSecondarySoft provides secondarySoft,
        LocalSuccessSoft provides successSoft,
        LocalWarningSoft provides warningSoft,
        LocalDangerSoft provides dangerSoft,
        LocalSpacing provides Spacing(),
        LocalRadius provides Radius(),
        LocalShadow provides ShadowTokens(),
        LocalMotion provides MotionTokens(),
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = IcyPowerShapes,
            content = content,
        )
    }
}

// ════════════════════════════════════════════════════════════════════════════
// Material3 ColorScheme — light + dark
// ════════════════════════════════════════════════════════════════════════════

/**
 * Light scheme built strictly from ui.md §3 token names.
 *
 * Slot mapping notes:
 *   - `background`     → BgLayoutLight (page bg is *not* the card color)
 *   - `surface`        → SurfaceLight (cards)
 *   - `surfaceVariant` → SurfaceElevatedLight (popovers — equals surface on light)
 *   - `outline`        → BorderLight
 *   - `outlineVariant` → DividerLight (list dividers / nav seams)
 *   - `error`          → Danger (Material3 calls this slot `error`, we
 *     expose `LocalDanger` for parity so calling code reads naturally).
 *   - We intentionally **do not** populate `tertiary`; Material3 reserves
 *     that slot for a third accent which the project does not use, and
 *     reusing `Secondary` would muddy the warning/danger hierarchy.
 */
private val LightColors: ColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = Neutral,
    primaryContainer = PrimarySoftLight,
    onPrimaryContainer = Primary,

    secondary = Secondary,
    onSecondary = Neutral,
    secondaryContainer = SecondarySoftLight,
    onSecondaryContainer = Secondary,

    tertiary = Secondary, // see doc above — intentional placeholder
    onTertiary = Neutral,
    tertiaryContainer = SecondarySoftLight,
    onTertiaryContainer = Secondary,

    error = Danger,
    onError = Neutral,
    errorContainer = DangerSoftLight,
    onErrorContainer = Danger,

    background = BgLayoutLight,
    onBackground = TextLight,

    surface = SurfaceLight,
    onSurface = TextLight,
    surfaceVariant = SurfaceElevatedLight,
    onSurfaceVariant = TextSecondaryLight,

    outline = BorderLight,
    outlineVariant = DividerLight,

    inverseSurface = TextLight,
    inverseOnSurface = SurfaceLight,
    inversePrimary = Primary,

    scrim = BgOverlayLight,
    surfaceTint = Primary,
)

/** Dark scheme — ui.md §3.2 dark + §4 layered surfaces. */
private val DarkColors: ColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = TextInverseDark,
    primaryContainer = PrimarySoftDark,
    onPrimaryContainer = PrimaryDark,

    secondary = SecondaryDark,
    onSecondary = TextInverseDark,
    secondaryContainer = SecondarySoftDark,
    onSecondaryContainer = SecondaryDark,

    tertiary = SecondaryDark,
    onTertiary = TextInverseDark,
    tertiaryContainer = SecondarySoftDark,
    onTertiaryContainer = SecondaryDark,

    error = DangerDark,
    onError = TextInverseDark,
    errorContainer = DangerSoftDark,
    onErrorContainer = DangerDark,

    background = BgLayoutDark,
    onBackground = TextDark,

    surface = SurfaceDark,
    onSurface = TextDark,
    surfaceVariant = SurfaceElevatedDark,
    onSurfaceVariant = TextSecondaryDark,

    outline = BorderDark,
    outlineVariant = DividerDark,

    inverseSurface = TextDark,
    inverseOnSurface = SurfaceDark,
    inversePrimary = Primary,

    scrim = BgOverlayDark,
    surfaceTint = PrimaryDark,
)

// ════════════════════════════════════════════════════════════════════════════
// Semantic color CompositionLocals
// ════════════════════════════════════════════════════════════════════════════

val LocalWarning: androidx.compose.runtime.ProvidableCompositionLocal<Color> =
    staticCompositionLocalOf { Warning }

val LocalDanger: androidx.compose.runtime.ProvidableCompositionLocal<Color> =
    staticCompositionLocalOf { Danger }

val LocalSuccess: androidx.compose.runtime.ProvidableCompositionLocal<Color> =
    staticCompositionLocalOf { Success }

/** Soft container versions — for badge backgrounds and tint zones. */
val LocalPrimarySoft: androidx.compose.runtime.ProvidableCompositionLocal<Color> =
    staticCompositionLocalOf { PrimarySoftLight }

val LocalSecondarySoft: androidx.compose.runtime.ProvidableCompositionLocal<Color> =
    staticCompositionLocalOf { SecondarySoftLight }

val LocalSuccessSoft: androidx.compose.runtime.ProvidableCompositionLocal<Color> =
    staticCompositionLocalOf { SuccessSoftLight }

val LocalWarningSoft: androidx.compose.runtime.ProvidableCompositionLocal<Color> =
    staticCompositionLocalOf { WarningSoftLight }

val LocalDangerSoft: androidx.compose.runtime.ProvidableCompositionLocal<Color> =
    staticCompositionLocalOf { DangerSoftLight }

// ════════════════════════════════════════════════════════════════════════════
// Spacing / Radius / Shadow / Motion tokens (ui.md §6 / §7)
// ════════════════════════════════════════════════════════════════════════════

/** 8-px scale + half-step (4). Every padding/margin in the app goes through here. */
data class Spacing(
    val xxs: Dp = 4.dp,
    val xs: Dp = 8.dp,
    val sm: Dp = 12.dp,
    val md: Dp = 16.dp,
    val lg: Dp = 24.dp,
    val xl: Dp = 32.dp,
    val xxl: Dp = 48.dp,
)

val LocalSpacing: androidx.compose.runtime.ProvidableCompositionLocal<Spacing> =
    staticCompositionLocalOf { Spacing() }

/** Ant Design radius scale: Tag / Button / Card / Panel. */
data class Radius(
    val xs: Dp = 4.dp,  // Tag
    val sm: Dp = 6.dp,  // Button (default)
    val md: Dp = 8.dp,  // Card / Modal
    val lg: Dp = 16.dp, // Hero panels
)

val LocalRadius: androidx.compose.runtime.ProvidableCompositionLocal<Radius> =
    staticCompositionLocalOf { Radius() }

/**
 * Three-layer shadow stack. Compose's `Modifier.shadow(elevation)` does not
 * support multi-layer rgba per the web spec — we expose the antd tokens as
 * [Shape]-shaped values for components that want the antd look *exactly*
 * (e.g. floating dropdowns), and as simple `Dp` elevations for Material3
 * Card.
 */
data class ShadowTokens(
    val none: String = "none",
    val layer1: String = "0 1px 2px 0 rgba(0,0,0,0.03), 0 1px 6px -1px rgba(0,0,0,0.02), 0 2px 4px 0 rgba(0,0,0,0.02)",
    val layer2: String = "0 6px 16px 0 rgba(0,0,0,0.08), 0 3px 6px -4px rgba(0,0,0,0.12), 0 9px 28px 8px rgba(0,0,0,0.05)",
    val layer3: String = "0 6px 16px 0 rgba(0,0,0,0.08), 0 9px 28px 0 rgba(0,0,0,0.05), 0 12px 48px 16px rgba(0,0,0,0.03)",
    /** Material3 Card `defaultElevation`, derived from layer1. */
    val cardElevation: Dp = 1.dp,
    /** Material3 Card `pressedElevation`, derived from layer2. */
    val cardPressedElevation: Dp = 4.dp,
    /** Material3 Card `focusedElevation`, between 1 and 2. */
    val cardFocusedElevation: Dp = 2.dp,
)

val LocalShadow: androidx.compose.runtime.ProvidableCompositionLocal<ShadowTokens> =
    staticCompositionLocalOf { ShadowTokens() }

// NOTE: there are intentionally no top-level `val Spacing` / `val Radius` /
// `val Motion` aliases — they would clash with the data-class names. Use
// `Spacing()` / `Radius()` / `MotionTokens()` directly (the data class's
// default ctor is the project-wide default).

/** Motion tokens (ui.md §7). */
data class MotionTokens(
    val fast: Int = 100,    // 0.1s, in milliseconds (Compose animationSpec)
    val base: Int = 200,
    val slow: Int = 300,
    val easingStandard: androidx.compose.animation.core.Easing = androidx.compose.animation.core.FastOutSlowInEasing,
    val easingDecelerate: androidx.compose.animation.core.Easing = androidx.compose.animation.core.LinearOutSlowInEasing,
    val easingAccelerate: androidx.compose.animation.core.Easing = androidx.compose.animation.core.FastOutLinearInEasing,
)

val LocalMotion: androidx.compose.runtime.ProvidableCompositionLocal<MotionTokens> =
    staticCompositionLocalOf { MotionTokens() }

// ════════════════════════════════════════════════════════════════════════════
// Material3 Shapes — derived from Radius token defaults
// ════════════════════════════════════════════════════════════════════════════

/**
 * Material3 Shapes uses five slots. We map them onto Ant's primary radius
 * steps. Default values come from the [Radius] data class — keep them in
 * sync if you change Radius().
 */
val IcyPowerShapes: Shapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),   // Tag / chip
    small = RoundedCornerShape(6.dp),         // Button (default)
    medium = RoundedCornerShape(8.dp),        // Card / Modal
    large = RoundedCornerShape(16.dp),       // Hero panel
    extraLarge = RoundedCornerShape(16.dp),
)
