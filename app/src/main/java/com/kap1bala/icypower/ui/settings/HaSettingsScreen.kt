package com.kap1bala.icypower.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kap1bala.icypower.R
import com.kap1bala.icypower.ui.theme.LocalDanger
import com.kap1bala.icypower.ui.theme.LocalRadius
import com.kap1bala.icypower.ui.theme.LocalSpacing
import com.kap1bala.icypower.ui.theme.LocalSuccess

/**
 * Home Assistant settings (route /settings/ha).
 *
 * Antd-style form: outlined text fields with supporting text, primary
 * filled button for Save, OutlinedButton for "Test connection", and a
 * low-emphasis TextButton for "Clear credentials". The visual rhythm
 * mirrors [LanguageScreen] so the Settings graph feels coherent.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HaSettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HaViewModel = viewModel(factory = HaViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val spacing = LocalSpacing.current
    val context = LocalContext.current

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.ha_title)) },
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
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = spacing.md, vertical = spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            Text(
                text = stringResource(R.string.ha_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            OutlinedTextField(
                value = state.baseUrl,
                onValueChange = viewModel::onBaseUrlChange,
                label = { Text(stringResource(R.string.ha_field_url)) },
                placeholder = { Text(stringResource(R.string.ha_field_url_hint)) },
                singleLine = true,
                isError = state.urlError,
                supportingText = state.urlError.takeIf { it }?.let {
                    { Text(stringResource(R.string.ha_field_url_error_blank)) }
                },
                // KeyboardType.Uri + ImeAction.Next was observed to NOT
                // pop the IME on some Android ROMs (URL field tapped but
                // no keyboard). Falling back to KeyboardType.Text keeps
                // `/` `.` `:port` reachable on the standard keyboard and
                // IME pop is reliable.
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    autoCorrectEnabled = false,
                    imeAction = ImeAction.Next,
                    capitalization = KeyboardCapitalization.None,
                ),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = state.tokenDraft,
                onValueChange = viewModel::onTokenDraftChange,
                label = { Text(stringResource(R.string.ha_field_token)) },
                placeholder = { Text(stringResource(R.string.ha_field_token_placeholder)) },
                supportingText = { Text(stringResource(R.string.ha_field_token_hint)) },
                singleLine = true,
                visualTransformation = if (state.showToken)
                    VisualTransformation.None
                else
                    PasswordVisualTransformation(),
                trailingIcon = {
                    val description = stringResource(R.string.ha_toggle_show_token)
                    // material-icons-core doesn't ship Visibility / VisibilityOff;
                    // we render a text toggle instead — equally accessible, no
                    // extra dependency.
                    TextButton(onClick = viewModel::onToggleShowToken) {
                        Text(
                            text = if (state.showToken)
                                stringResource(R.string.ha_token_hide)
                            else
                                stringResource(R.string.ha_token_show),
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                    capitalization = KeyboardCapitalization.None,
                ),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth(),
            )

            // Test + status row
            OutlinedButton(
                onClick = viewModel::testConnection,
                enabled = state.phase == Phase.Idle,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small,
            ) {
                Text(stringResource(R.string.ha_button_test))
            }

            state.statusMessage?.let { status ->
                val (text, color) = when (status) {
                    Status.Ok -> stringResource(R.string.ha_status_ok) to LocalSuccess.current
                    Status.Failed -> (state.errorReason
                        ?: stringResource(R.string.ha_status_failed)) to LocalDanger.current
                    Status.Cleared -> stringResource(R.string.ha_button_clear) to LocalSuccess.current
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = color,
                    modifier = Modifier.padding(top = spacing.xxs),
                )
            }

            // Save (primary)
            Button(
                onClick = { viewModel.saveAndRecreate(context) },
                enabled = state.phase == Phase.Idle,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small,
            ) {
                Text(stringResource(R.string.ha_button_save))
            }

            // Clear (low-emphasis)
            TextButton(
                onClick = { viewModel.clearAllAndRecreate(context) },
                enabled = state.phase == Phase.Idle,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(R.string.ha_button_clear),
                    color = LocalDanger.current,
                )
            }
        }
    }

    // LocalRadius reference kept here so unused-import lint stays quiet if
    // we later want to apply a custom corner radius to the buttons/text
    // fields without re-importing.
    @Suppress("UNUSED_EXPRESSION") LocalRadius.current
}