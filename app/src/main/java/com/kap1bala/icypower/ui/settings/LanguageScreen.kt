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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kap1bala.icypower.R
import com.kap1bala.icypower.data.i18n.AppLocale
import com.kap1bala.icypower.ui.theme.LocalSpacing

/**
 * Language picker (route `/settings/language`).
 *
 * Three radios following the same antd-style pattern as [AppearanceScreen]:
 *   - title (titleMedium / onSurface) + description (bodySmall / onSurfaceVariant)
 *   - row is its own `selectable` group (Material3 idiom for radiogroup)
 *   - selecting an option persists it and triggers `Activity.recreate()` —
 *     the next attachBaseContext picks up the new locale.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LanguageViewModel = viewModel(factory = LanguageViewModel.Factory),
) {
    val current by viewModel.locale.collectAsStateWithLifecycle()
    val spacing = LocalSpacing.current
    val context = LocalContext.current

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.language_title)) },
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
                LanguageOptionRow(
                    option = option,
                    selected = current == option.locale,
                    onSelect = { viewModel.setLocale(context, option.locale) },
                )
            }
        }
        // `spacing` reference keeps the unused-warning at bay; future
        // inter-row rhythm may pull from it.
        @Suppress("UNUSED_EXPRESSION") spacing
    }
}

@Composable
private fun LanguageOptionRow(
    option: LanguageOption,
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
            onClick = null,
            modifier = Modifier.padding(top = spacing.xxs),
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

private data class LanguageOption(
    val locale: AppLocale,
    val labelRes: Int,
    val descriptionRes: Int,
)

private val OPTIONS = listOf(
    LanguageOption(
        locale = AppLocale.System,
        labelRes = R.string.language_option_system,
        descriptionRes = R.string.language_option_system_desc,
    ),
    LanguageOption(
        locale = AppLocale.Chinese,
        labelRes = R.string.language_option_zh,
        descriptionRes = R.string.language_option_zh_desc,
    ),
    LanguageOption(
        locale = AppLocale.English,
        labelRes = R.string.language_option_en,
        descriptionRes = R.string.language_option_en_desc,
    ),
)
