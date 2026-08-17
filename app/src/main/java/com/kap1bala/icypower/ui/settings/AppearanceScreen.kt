package com.kap1bala.icypower.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kap1bala.icypower.R
import com.kap1bala.icypower.ui.theme.LocalSpacing
import com.kap1bala.icypower.ui.theme.ThemeMode
import com.kap1bala.icypower.ui.theme.ThemeViewModel

/**
 * 外观设置（route /settings/appearance）。
 *
 * 三个 `Radio` 选项，每项下方带 `bodySmall` 描述当前模式的效果（ui.md §8.8
 * "每个 Radio 下方加 12dp 行高的小字说明当前模式的效果"）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    themeViewModel: ThemeViewModel = viewModel(factory = ThemeViewModel.Factory),
) {
    val current by themeViewModel.themeMode.collectAsStateWithLifecycle()
    val spacing = LocalSpacing.current

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.appearance_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .selectableGroup(),
        ) {
            OPTIONS.forEach { option ->
                ThemeOptionRow(
                    option = option,
                    selected = current == option.mode,
                    onSelect = { themeViewModel.setThemeMode(option.mode) },
                )
            }
        }
        // `spacing` reference keeps the unused-warning at bay if a future
        // iteration wants to apply vertical rhythm between rows.
        @Suppress("UNUSED_EXPRESSION") spacing
    }
}

@Composable
private fun ThemeOptionRow(
    option: ThemeOption,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalSpacing.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onSelect,
                role = Role.RadioButton,
            )
            .padding(horizontal = spacing.md, vertical = spacing.sm),
        verticalAlignment = Alignment.Top,
    ) {
        RadioButton(
            selected = selected,
            onClick = null, // handled by the Row's selectable
            modifier = Modifier.padding(top = spacing.xxs), // visually align with title baseline
        )
        Column(
            modifier = Modifier
                .padding(start = spacing.sm)
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(spacing.xxs),
        ) {
            Text(
                text = stringResource(option.labelRes),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(option.descriptionRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private data class ThemeOption(
    val mode: ThemeMode,
    val labelRes: Int,
    val descriptionRes: Int,
)

private val OPTIONS = listOf(
    ThemeOption(
        mode = ThemeMode.System,
        labelRes = R.string.appearance_option_system,
        descriptionRes = R.string.appearance_option_system_desc,
    ),
    ThemeOption(
        mode = ThemeMode.Light,
        labelRes = R.string.appearance_option_light,
        descriptionRes = R.string.appearance_option_light_desc,
    ),
    ThemeOption(
        mode = ThemeMode.Dark,
        labelRes = R.string.appearance_option_dark,
        descriptionRes = R.string.appearance_option_dark_desc,
    ),
)
