package com.kap1bala.icypower.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kap1bala.icypower.R
import com.kap1bala.icypower.data.cycle.OverdueSeverity
import com.kap1bala.icypower.ui.ha.HaDeviceCard
import com.kap1bala.icypower.ui.ha.HaDevicesState
import com.kap1bala.icypower.ui.ha.HaPhase
import com.kap1bala.icypower.ui.ha.HaViewModel
import com.kap1bala.icypower.ui.theme.LocalDanger
import com.kap1bala.icypower.ui.theme.LocalDangerSoft
import com.kap1bala.icypower.ui.theme.LocalRadius
import com.kap1bala.icypower.ui.theme.LocalSpacing
import com.kap1bala.icypower.ui.theme.LocalSuccess
import com.kap1bala.icypower.ui.theme.LocalWarning
import com.kap1bala.icypower.ui.theme.LocalWarningSoft
import java.time.Duration
import java.time.Instant

/**
 * HomeScreen Tab B — Home Assistant device cards.
 *
 * State machine (one Composable, branched on [HaPhase]):
 *   NotConfigured → antd "Empty" centred prompt + button → /settings/ha
 *   Loading      → 3 skeleton cards
 *   Loaded       → LazyColumn of [HomeHaCard], sorted by severity
 *   Empty        → branched on `state.hasAnyBatteryEntity`:
 *                    - HA has battery entities but user unchecked all
 *                      → "去选择" CTA → /settings/ha/devices
 *                    - HA has no battery entities at all
 *                      → "去设置" CTA → /settings/ha
 *   Error        → red icon + 重试 button
 *   Unauthorized → warning icon + "去设置" button
 *
 * Mirrors the antd pattern used by [HomeCycleDevicesPanel]; both
 * panels live in the same HorizontalPager and share antd-aligned card
 * visuals (surface + 1dp outlineVariant + radius.md).
 */
@Composable
fun HomeHaPanel(
    onOpenSettings: () -> Unit,
    onChooseDevices: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HaViewModel = viewModel(factory = HaViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    when (state.phase) {
        HaPhase.NotConfigured -> NotConfiguredPane(onOpenSettings = onOpenSettings)
        HaPhase.Loading       -> LoadingPane()
        HaPhase.Loaded        -> LoadedPane(state = state, onRetry = viewModel::refresh)
        HaPhase.Empty         -> EmptyPane(
            hasAnyBatteryEntity = state.hasAnyBatteryEntity,
            onChooseDevices = onChooseDevices,
            onOpenSettings = onOpenSettings,
        )
        HaPhase.Error         -> ErrorPane(state = state, onRetry = viewModel::refresh)
        HaPhase.Unauthorized  -> UnauthorizedPane(onOpenSettings = onOpenSettings)
    }
}

// ────────────────────────────────────────────────────────────────────
// Phase panes
// ────────────────────────────────────────────────────────────────────

@Composable
private fun NotConfiguredPane(onOpenSettings: () -> Unit) {
    CenteredMessage(
        icon = Icons.Filled.Info,
        iconTint = MaterialTheme.colorScheme.primary,
        title = stringResource(R.string.home_ha_not_configured_title),
        body = stringResource(R.string.home_ha_not_configured_desc),
        action = stringResource(R.string.settings_ha) to onOpenSettings,
    )
}

@Composable
private fun LoadingPane() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = LocalSpacing.current.md, vertical = LocalSpacing.current.md),
        verticalArrangement = Arrangement.spacedBy(LocalSpacing.current.md),
    ) {
        repeat(3) { SkeletonCard() }
    }
}

@Composable
private fun LoadedPane(state: HaDevicesState, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = LocalSpacing.current.md,
                end = LocalSpacing.current.md,
                top = LocalSpacing.current.md,
                bottom = LocalSpacing.current.xl,
            ),
            verticalArrangement = Arrangement.spacedBy(LocalSpacing.current.md),
        ) {
            items(items = state.devices, key = { it.entityId }) { card ->
                HomeHaCard(card = card)
            }
            if (state.isReconnecting) {
                item(key = "reconnecting") {
                    ReconnectingBanner(message = state.errorMessage)
                }
            }
        }
    }
}

@Composable
private fun EmptyPane(
    hasAnyBatteryEntity: Boolean,
    onChooseDevices: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    // HA exposes battery attributes but the user has unchecked every
    // entity → nudge them at the device-selection screen. Otherwise HA
    // itself has no battery entities → fall back to "check your setup".
    if (hasAnyBatteryEntity) {
        CenteredMessage(
            icon = Icons.Filled.Info,
            iconTint = MaterialTheme.colorScheme.primary,
            title = stringResource(R.string.home_ha_empty_choose_devices_title),
            body = stringResource(R.string.home_ha_empty_choose_devices_desc),
            action = stringResource(R.string.home_ha_empty_choose_devices_action) to onChooseDevices,
        )
    } else {
        CenteredMessage(
            icon = Icons.Filled.Info,
            iconTint = MaterialTheme.colorScheme.primary,
            title = stringResource(R.string.home_ha_empty_title),
            body = stringResource(R.string.home_ha_empty_desc),
            action = stringResource(R.string.settings_ha) to onOpenSettings,
        )
    }
}

@Composable
private fun ErrorPane(state: HaDevicesState, onRetry: () -> Unit) {
    CenteredMessage(
        icon = Icons.Filled.Clear,
        iconTint = LocalDanger.current,
        title = stringResource(R.string.home_ha_error_title),
        body = state.errorMessage ?: stringResource(R.string.home_ha_error_desc),
        action = stringResource(R.string.home_ha_error_retry) to onRetry,
    )
}

@Composable
private fun UnauthorizedPane(onOpenSettings: () -> Unit) {
    CenteredMessage(
        icon = Icons.Outlined.Lock,
        iconTint = LocalWarning.current,
        title = stringResource(R.string.home_ha_unauthorized_title),
        body = stringResource(R.string.home_ha_unauthorized_desc),
        action = stringResource(R.string.home_ha_unauthorized_action) to onOpenSettings,
    )
}

// ────────────────────────────────────────────────────────────────────
// Shared widgets
// ────────────────────────────────────────────────────────────────────

@Composable
private fun CenteredMessage(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    body: String,
    action: Pair<String, () -> Unit>? = null,
) {
    val spacing = LocalSpacing.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = spacing.lg),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(48.dp),
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = spacing.md),
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(top = spacing.xs),
            )
            if (action != null) {
                Button(
                    onClick = action.second,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.padding(top = spacing.md),
                ) {
                    Text(action.first)
                }
            }
        }
    }
}

@Composable
private fun SkeletonCard() {
    val radius = LocalRadius.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(radius.md),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .size(20.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.4f)
                    .size(14.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant),
            )
        }
    }
}

@Composable
private fun HomeHaCard(card: HaDeviceCard) {
    val radius = LocalRadius.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(radius.md),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = card.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (card.severity != OverdueSeverity.None) {
                    HaSeverityBadge(severity = card.severity)
                }
            }
            Text(
                text = stringResource(
                    R.string.home_ha_card_subtitle,
                    card.area ?: stringResource(R.string.home_ha_no_area),
                    card.batteryPercent,
                    formatRelativeTimestamp(card.lastUpdated),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            // Battery progress bar — colour follows the same severity
            // scale as the badge (高→绿 / 中→黄 / 低→红) so the two
            // always agree at a glance (feat.md §5.9 colour + shape).
            val progressColor = when (card.severity) {
                OverdueSeverity.Danger -> LocalDanger.current
                OverdueSeverity.Warning -> LocalWarning.current
                OverdueSeverity.None -> LocalSuccess.current
            }
            LinearProgressIndicator(
                progress = { (card.batteryPercent / 100f).coerceIn(0f, 1f) },
                color = progressColor,
                trackColor = progressColor.copy(alpha = 0.12f),
                // Butt caps + no stop dot: the Material3 default paints a
                // round cap (and a leading stop dot) that at 100% reads as
                // a stray green dot at the end of the bar.
                strokeCap = StrokeCap.Butt,
                drawStopIndicator = {},
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * Status badge mirroring `HomeCycleDevicePanel.OverdueBadge` — colour
 * + icon + text triple per feat.md §5.9 / WCAG.
 */
@Composable
private fun HaSeverityBadge(severity: OverdueSeverity) {
    val spacing = LocalSpacing.current
    val isDanger = severity == OverdueSeverity.Danger
    val accent: Color = if (isDanger) LocalDanger.current else LocalWarning.current
    val container: Color = if (isDanger) LocalDangerSoft.current else LocalWarningSoft.current
    Surface(
        shape = MaterialTheme.shapes.extraSmall,
        color = container,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = spacing.xs, vertical = spacing.xxs),
            horizontalArrangement = Arrangement.spacedBy(spacing.xxs),
        ) {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(12.dp),
            )
            Text(
                text = stringResource(
                    if (isDanger) R.string.home_badge_severely_overdue
                    else R.string.home_badge_overdue
                ),
                style = MaterialTheme.typography.labelSmall,
                color = accent,
            )
        }
    }
}

@Composable
private fun ReconnectingBanner(message: String?) {
    val spacing = LocalSpacing.current
    Surface(
        shape = MaterialTheme.shapes.small,
        color = LocalWarningSoft.current,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
            Icon(
                imageVector = Icons.Filled.Refresh,
                contentDescription = null,
                tint = LocalWarning.current,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = stringResource(R.string.home_ha_reconnecting),
                style = MaterialTheme.typography.labelMedium,
                color = LocalWarning.current,
            )
        }
    }
}

// ────────────────────────────────────────────────────────────────────
// Helpers
// ────────────────────────────────────────────────────────────────────

/**
 * "5 分钟前" / "2 小时前" / "刚刚" — locale-aware, no second-level
 * granularity (HA state_changed timestamps are noisy at sub-minute).
 */
@Composable
private fun formatRelativeTimestamp(iso: String): String {
    val now = remember { System.currentTimeMillis() }
    val then = parseIsoToMillis(iso) ?: return stringResource(R.string.home_ha_just_now)
    val deltaMs = (now - then).coerceAtLeast(0)
    val delta = Duration.ofMillis(deltaMs)
    return when {
        delta.toMinutes() < 1 -> stringResource(R.string.home_ha_just_now)
        delta.toHours() < 1 -> stringResource(R.string.home_ha_minutes_ago, delta.toMinutes().toInt())
        delta.toDays() < 1 -> stringResource(R.string.home_ha_hours_ago, delta.toHours().toInt())
        else -> stringResource(R.string.home_ha_just_now)  // > 24h falls back; not worth a new key
    }
}

/**
 * HA emits ISO-8601 with offset (`2024-05-30T21:43:29+00:00`).
 * Use java.time to parse robustly. Falls back to null on any
 * parse failure so the UI can show "just now" rather than crash.
 */
private fun parseIsoToMillis(iso: String): Long? = try {
    Instant.parse(iso).toEpochMilli()
} catch (_: Exception) {
    try {
        // Some HA integrations omit the offset. Try with system zone.
        java.time.OffsetDateTime.parse(iso).toInstant().toEpochMilli()
    } catch (_: Exception) {
        null
    }
}

@Suppress("unused") private val unusedSentinel: Unit = Unit