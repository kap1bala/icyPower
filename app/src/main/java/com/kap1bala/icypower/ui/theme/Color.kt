package com.kap1bala.icypower.ui.theme

/**
 * SINGLE SOURCE OF TRUTH FOR COLOR HEX VALUES.
 *
 * Mirrors `prompts/ui.md` (Ant design tokens). Component code must reference
 * MaterialTheme ColorScheme tokens or the LocalWarning/Danger/Success
 * CompositionLocals defined in Theme.kt — never these constants directly.
 */

import androidx.compose.ui.graphics.Color

// ──────────────────────────────────────────────────────────────────────────────
// Light tokens
// ──────────────────────────────────────────────────────────────────────────────

val Primary = Color(0xFF1677FF)
val Secondary = Color(0xFF8B5CF6)
val Success = Color(0xFF16A34A)
val Warning = Color(0xFFD97706)
val Danger = Color(0xFFDC2626)
val SurfaceLight = Color(0xFFFFFFFF)
val TextLight = Color(0xFF111827)
val Neutral = Color(0xFFFFFFFF)

// ──────────────────────────────────────────────────────────────────────────────
// Dark tokens
// ──────────────────────────────────────────────────────────────────────────────

val SurfaceDark = Color(0xFF0B1220)
val SurfaceElevatedDark = Color(0xFF111827)
val SurfaceMutedDark = Color(0xFF1F2937)
val TextDark = Color(0xFFF9FAFB)
val TextMutedDark = Color(0xFF9CA3AF)
val TextInverseDark = Color(0xFF111827)
val BorderDark = Color(0xFF374151)
val BorderStrongDark = Color(0xFF4B5563)
val PrimaryDark = Color(0xFF3B82F6)
val SecondaryDark = Color(0xFFA78BFA)
val SuccessDark = Color(0xFF22C55E)
val WarningDark = Color(0xFFF59E0B)
val DangerDark = Color(0xFFEF4444)
val SuccessSoftDark = Color(0xFF052E1A)
val WarningSoftDark = Color(0xFF3A1F05)
val DangerSoftDark = Color(0xFF3A0F0F)

// Chart series for the dark palette (used by history trends / comparison charts).
val ChartSeries1Dark = Color(0xFF3B82F6)
val ChartSeries2Dark = Color(0xFFA78BFA)
val ChartSeries3Dark = Color(0xFF22C55E)
val ChartSeries4Dark = Color(0xFFF59E0B)
val ChartSeries5Dark = Color(0xFFEF4444)
