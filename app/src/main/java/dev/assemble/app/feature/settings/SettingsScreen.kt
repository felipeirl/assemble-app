package dev.assemble.app.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.assemble.app.BuildConfig
import dev.assemble.app.R
import dev.assemble.app.core.designsystem.component.AssembleTopBar
import dev.assemble.app.core.designsystem.component.TopBarNavigation
import dev.assemble.app.core.designsystem.component.TopBarTitle
import dev.assemble.app.core.designsystem.theme.AssembleTheme
import dev.assemble.app.core.model.AppSettings
import dev.assemble.app.core.model.ThemePreference
import kotlin.math.roundToInt

private val MinRowHeight = 48.dp

@Composable
fun SettingsRoute(viewModel: SettingsViewModel, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    SettingsScreen(
        settings = settings,
        message = message,
        busy = busy,
        onBack = onBack,
        actions = SettingsActions(
            onThemeChange = viewModel::setTheme,
            onNotifyConnectionsChange = viewModel::setNotifyNewConnections,
            onNotifyMessagesChange = viewModel::setNotifyNewMessages,
            onMinimumCompatibilityChange = viewModel::setMinimumCompatibility,
            onResetPreferences = viewModel::resetPreferences,
            onClearSeen = viewModel::clearSeenCharacters,
            onDeleteChats = viewModel::deleteChats,
            onDeleteAccount = viewModel::deleteAccount,
            onMessageShown = viewModel::onMessageShown,
        ),
        modifier = modifier,
    )
}

/** Callbacks da tela agrupados (são muitos para parâmetros soltos). */
data class SettingsActions(
    val onThemeChange: (ThemePreference) -> Unit,
    val onNotifyConnectionsChange: (Boolean) -> Unit,
    val onNotifyMessagesChange: (Boolean) -> Unit,
    val onMinimumCompatibilityChange: (Int) -> Unit,
    val onResetPreferences: () -> Unit,
    val onClearSeen: () -> Unit,
    val onDeleteChats: () -> Unit,
    val onDeleteAccount: () -> Unit,
    val onMessageShown: () -> Unit,
)

@Composable
fun SettingsScreen(
    settings: AppSettings,
    message: SettingsMessage?,
    busy: Boolean,
    onBack: () -> Unit,
    actions: SettingsActions,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current
    LaunchedEffect(message) {
        val current = message ?: return@LaunchedEffect
        val text = resources.getString(
            when (current) {
                SettingsMessage.PreferencesReset -> R.string.settings_msg_preferences_reset
                SettingsMessage.SeenCleared -> R.string.settings_msg_seen_cleared
                SettingsMessage.ChatsDeleted -> R.string.settings_msg_chats_deleted
                SettingsMessage.ActionFailed -> R.string.settings_msg_action_failed
            },
        )
        // Limpar depois: mudar a chave antes cancelaria este efeito.
        snackbarHostState.showSnackbar(text)
        actions.onMessageShown()
    }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val spacing = AssembleTheme.spacing

    Scaffold(
        modifier = modifier,
        containerColor = AssembleTheme.colors.bg,
        topBar = {
            AssembleTopBar(
                title = TopBarTitle.Text(stringResource(R.string.settings_title)),
                navigation = TopBarNavigation.Back(onBack),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(spacing.space4),
            verticalArrangement = Arrangement.spacedBy(spacing.space6),
        ) {
            Section(stringResource(R.string.settings_appearance)) {
                ThemeSelector(selected = settings.theme, onSelect = actions.onThemeChange)
            }
            Section(stringResource(R.string.settings_notifications)) {
                SwitchRow(stringResource(R.string.settings_notify_connections), settings.notifyNewConnections, actions.onNotifyConnectionsChange)
                SwitchRow(stringResource(R.string.settings_notify_messages), settings.notifyNewMessages, actions.onNotifyMessagesChange)
            }
            Section(stringResource(R.string.settings_discovery)) {
                MinimumCompatibilitySlider(settings.minimumCompatibility, actions.onMinimumCompatibilityChange)
            }
            Section(stringResource(R.string.settings_data)) {
                ActionRow(stringResource(R.string.settings_reset_preferences), enabled = !busy, onClick = actions.onResetPreferences)
                HorizontalDivider(color = AssembleTheme.colors.border)
                ActionRow(stringResource(R.string.settings_clear_seen), enabled = !busy, onClick = actions.onClearSeen)
                HorizontalDivider(color = AssembleTheme.colors.border)
                ActionRow(stringResource(R.string.settings_delete_chats), enabled = !busy, onClick = actions.onDeleteChats)
                HorizontalDivider(color = AssembleTheme.colors.border)
                ActionRow(
                    stringResource(R.string.settings_delete_account),
                    enabled = !busy,
                    color = AssembleTheme.colors.error,
                    onClick = { confirmDelete = true },
                )
            }
            Section(stringResource(R.string.settings_about)) {
                Paragraph(stringResource(R.string.settings_about_source))
                Paragraph(stringResource(R.string.settings_about_ai))
                Paragraph(stringResource(R.string.settings_about_version, BuildConfig.VERSION_NAME))
            }
        }
    }

    if (confirmDelete) {
        DeleteAccountDialog(
            onConfirm = {
                confirmDelete = false
                actions.onDeleteAccount()
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(AssembleTheme.spacing.space2)) {
        Text(
            text = title.uppercase(),
            style = AssembleTheme.typography.eyebrow,
            color = AssembleTheme.colors.textMuted,
            modifier = Modifier.semantics { heading() },
        )
        content()
    }
}

@Composable
private fun ThemeSelector(selected: ThemePreference, onSelect: (ThemePreference) -> Unit) {
    val colors = AssembleTheme.colors
    val options = listOf(
        ThemePreference.Light to R.string.settings_theme_light,
        ThemePreference.Dark to R.string.settings_theme_dark,
        ThemePreference.System to R.string.settings_theme_system,
    )
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, (theme, label) ->
            SegmentedButton(
                selected = theme == selected,
                onClick = { onSelect(theme) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = colors.actionAssemble,
                    activeContentColor = colors.onActionAssemble,
                    activeBorderColor = colors.actionAssemble,
                    inactiveContainerColor = colors.surface,
                    inactiveContentColor = colors.text,
                    inactiveBorderColor = colors.border,
                ),
            ) { Text(stringResource(label)) }
        }
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val colors = AssembleTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = MinRowHeight)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = AssembleTheme.typography.body, color = colors.text, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedTrackColor = colors.actionAssemble,
                checkedThumbColor = colors.onActionAssemble,
                uncheckedTrackColor = colors.surface,
                uncheckedBorderColor = colors.border,
                uncheckedThumbColor = colors.textMuted,
            ),
        )
    }
}

/** 50–90, de 1 em 1. Grava ao soltar; enquanto arrasta, só a tela muda. */
@Composable
private fun MinimumCompatibilitySlider(value: Int, onValueChange: (Int) -> Unit) {
    val colors = AssembleTheme.colors
    var dragging by remember(value) { mutableFloatStateOf(value.toFloat()) }
    val shown = dragging.roundToInt()
    val label = stringResource(R.string.settings_min_compatibility)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = AssembleTheme.typography.body, color = colors.text, modifier = Modifier.weight(1f))
        Text(
            stringResource(R.string.score_ring_value, shown),
            style = AssembleTheme.typography.body.copy(fontWeight = FontWeight.SemiBold),
            color = colors.text,
        )
    }
    Slider(
        value = dragging,
        onValueChange = { dragging = it },
        onValueChangeFinished = { onValueChange(dragging.roundToInt()) },
        valueRange = AppSettings.MIN_MINIMUM_COMPATIBILITY.toFloat()..AppSettings.MAX_MINIMUM_COMPATIBILITY.toFloat(),
        steps = AppSettings.MAX_MINIMUM_COMPATIBILITY - AppSettings.MIN_MINIMUM_COMPATIBILITY - 1,
        modifier = Modifier.semantics { contentDescription = label },
        colors = SliderDefaults.colors(
            thumbColor = colors.actionAssemble,
            activeTrackColor = colors.actionAssemble,
            inactiveTrackColor = colors.border,
            activeTickColor = Color.Transparent,
            inactiveTickColor = Color.Transparent,
        ),
    )
    Text(stringResource(R.string.settings_min_compatibility_hint), style = AssembleTheme.typography.caption, color = colors.textMuted)
}

@Composable
private fun ActionRow(label: String, enabled: Boolean, onClick: () -> Unit, color: Color = AssembleTheme.colors.text) {
    Text(
        text = label,
        style = AssembleTheme.typography.body,
        color = if (enabled) color else AssembleTheme.colors.textMuted,
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = MinRowHeight)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(vertical = AssembleTheme.spacing.space3),
    )
}

@Composable
private fun Paragraph(text: String) {
    Text(text, style = AssembleTheme.typography.caption, color = AssembleTheme.colors.textMuted)
}

@Composable
private fun DeleteAccountDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val colors = AssembleTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        title = { Text(stringResource(R.string.settings_delete_account_title), color = colors.text) },
        text = { Text(stringResource(R.string.settings_delete_account_message), color = colors.textMuted) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.action_delete), color = colors.error, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel), color = colors.text) }
        },
    )
}

private val PreviewActions = SettingsActions({}, {}, {}, {}, {}, {}, {}, {}, {})

@PreviewLightDark
@Composable
private fun SettingsPreview() {
    AssembleTheme {
        SettingsScreen(settings = AppSettings(), message = null, busy = false, onBack = {}, actions = PreviewActions)
    }
}
