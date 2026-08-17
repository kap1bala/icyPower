package com.kap1bala.icypower.ui.theme

/**
 * ⚠ SINGLE SOURCE OF TRUTH FOR COLOR HEX VALUES ⚠
 *
 * Every hex literal in the entire app must come from this file, which in turn
 * mirrors `prompts/ui.md`. Component code must reference MaterialTheme
 * ColorScheme tokens (e.g. `MaterialTheme.colorScheme.surface`) or one of the
 * [LocalWarning] / [LocalDanger] / [LocalSuccess] CompositionLocals exposed
 * from Theme.kt — never these constants directly.
 *
 * If a value is missing here, it is a spec gap in ui.md — fix ui.md first,
 * then mirror the change here. Do not invent hex values in component code.
 */

import androidx.compose.ui.graphics.Color

// ──────────────────────────────────────────────────────────────────────────────
// Light — semantic colors (ui.md §3.2)
// ──────────────────────────────────────────────────────────────────────────────

/** Brand primary: Ant blue-6 (`@blue-6`). */
val Primary = Color(0xFF1677FF)

/** Secondary: Ant purple-6 (`@purple-6`). */
val Secondary = Color(0xFF722ED1)

/** Success: Ant green-6 — 已充电成功 / 已处理. */
val Success = Color(0xFF52C41A)

/** Warning: Ant gold-6 — 即将超期 (`OverdueSeverity.Warning`). */
val Warning = Color(0xFFFAAD14)

/** Danger: Ant red-6 — 严重超期 / 失败. */
val Danger = Color(0xFFFF4D4F)

// ──────────────────────────────────────────────────────────────────────────────
// Light — neutrals (ui.md §3.3)
// ──────────────────────────────────────────────────────────────────────────────

/** Layer 1: page layout background, very light gray. */
val BgLayoutLight = Color(0xFFF5F5F5)

/** Layer 2: card / container surface (white on light). */
val SurfaceLight = Color(0xFFFFFFFF)

/** Layer 3: elevated (Dropdown / Tooltip / Modal body). Equals surface on light. */
val SurfaceElevatedLight = Color(0xFFFFFFFF)

/** 88% black — main text. */
val TextLight = Color(0xE0000000)

/** 65% black — secondary text. */
val TextSecondaryLight = Color(0xA6000000)

/** 45% black — disabled / placeholder-fallback. */
val TextDisabledLight = Color(0x73000000)

/** 25% black — input placeholder. */
val TextPlaceholderLight = Color(0x40000000)

/** 1px control border. */
val BorderLight = Color(0xFFD9D9D9)

/** Strong / focused border. */
val BorderStrongLight = Color(0xFFBFBFBF)

/** List divider line (6% black). */
val DividerLight = Color(0x0F050505)

/** Weak fill (4% black, disabled / hover). */
val FillTertiaryLight = Color(0x0A000000)

/** Even weaker (2%). */
val FillQuaternaryLight = Color(0x05000000)

/** Modal mask (45% black). */
val BgOverlayLight = Color(0x73000000)

/** Foreground on filled primary / secondary (e.g. button text on primary bg). */
val Neutral = Color(0xFFFFFFFF)

/** Inverse text on light surfaces (rarely needed; kept for symmetry). */
val TextInverseLight = Color(0xFFFFFFFF)

// ──────────────────────────────────────────────────────────────────────────────
// Light — soft containers (10% tint, for badge backgrounds)
// ──────────────────────────────────────────────────────────────────────────────

val PrimarySoftLight = Color(0x1A1677FF)
val SecondarySoftLight = Color(0x1A722ED1)
val SuccessSoftLight = Color(0x1A52C41A)
val WarningSoftLight = Color(0x1AFAAD14)
val DangerSoftLight = Color(0x1AFF4D4F)

// ──────────────────────────────────────────────────────────────────────────────
// Dark — semantic colors (ui.md §3.2 dark)
// ──────────────────────────────────────────────────────────────────────────────

/** Layer 1: dark page background. */
val BgLayoutDark = Color(0xFF000000)

/** Layer 2: card / container surface. */
val SurfaceDark = Color(0xFF141414)

/** Layer 3: elevated — Dropdown / Popover / Modal body. */
val SurfaceElevatedDark = Color(0xFF1F1F1F)

/** Layer 4: overlay halo (Menu/Popover shadow side). */
val SurfaceOverlayDark = Color(0xFF303030)

/** Dark primary — blue-6 stays legible on #000 per ui.md §3.2. */
val PrimaryDark = Primary

/** Dark secondary — purple-7 lift for contrast. */
val SecondaryDark = Color(0xFFA77BFF)

/** Dark success — green-6 holds. */
val SuccessDark = Color(0xFF49AA19)

/** Dark warning — gold-7 lift. */
val WarningDark = Color(0xFFFFC53D)

/** Dark danger — red-7 lift. */
val DangerDark = Color(0xFFE5484D)

// ──────────────────────────────────────────────────────────────────────────────
// Dark — neutrals (rgba white on black)
// ──────────────────────────────────────────────────────────────────────────────

/** 88% white — main text on dark surfaces. */
val TextDark = Color(0xE0FFFFFF)

/** 65% white — secondary text. */
val TextSecondaryDark = Color(0xA6FFFFFF)

/** 30% white — disabled. */
val TextDisabledDark = Color(0x4DFFFFFF)

/** 25% white — placeholder. */
val TextPlaceholderDark = Color(0x40FFFFFF)

/** 12% white border. */
val BorderDark = Color(0x1FFFFFFF)

/** 20% white border. */
val BorderStrongDark = Color(0x33FFFFFF)

/** 12% white divider. */
val DividerDark = Color(0x1FFFFFFF)

/** 8% white fill. */
val FillTertiaryDark = Color(0x14FFFFFF)

/** 4% white fill. */
val FillQuaternaryDark = Color(0x0AFFFFFF)

/** 65% black modal mask (dark mode masks stay black-ish). */
val BgOverlayDark = Color(0xA6000000)

/** Inverse text on dark surfaces — used by `inverseOnSurface`. */
val TextInverseDark = Color(0xFF141414)

// ──────────────────────────────────────────────────────────────────────────────
// Dark — soft containers (full color, low value; for badge backgrounds)
// ──────────────────────────────────────────────────────────────────────────────

val PrimarySoftDark = Color(0xFF003A8C)
val SecondarySoftDark = Color(0xFF2C1A4D)
val SuccessSoftDark = Color(0xFF092B00)
val WarningSoftDark = Color(0xFF3E2C00)
val DangerSoftDark = Color(0xFF3D0F0F)

// ──────────────────────────────────────────────────────────────────────────────
// Chart series (ui.md §3.5 — stable across themes; data identity, not UI state)
// ──────────────────────────────────────────────────────────────────────────────

val ChartSeries1 = Color(0xFF1677FF)  // blue-6
val ChartSeries2 = Color(0xFF722ED1)  // purple-6
val ChartSeries3 = Color(0xFF13C2C2)  // cyan-6
val ChartSeries4 = Color(0xFF52C41A)  // green-6
val ChartSeries5 = Color(0xFFFAAD14)  // gold-6
val ChartSeries6 = Color(0xFFFF4D4F)  // red-6
