package dev.assemble.app.feature.settings

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.assemble.app.BuildConfig
import dev.assemble.app.R
import dev.assemble.app.core.designsystem.component.AssembleLargeTopBar
import dev.assemble.app.core.designsystem.component.IconTile
import dev.assemble.app.core.designsystem.component.energyGradient
import dev.assemble.app.core.designsystem.icon.AssembleIcons
import dev.assemble.app.core.designsystem.theme.AssembleTheme
import dev.assemble.app.core.designsystem.theme.DarkAssembleColors
import dev.assemble.app.core.designsystem.theme.LightAssembleColors
import dev.assemble.app.core.designsystem.theme.rememberAnimationsEnabled
import dev.assemble.app.core.feedback.Cue
import dev.assemble.app.core.feedback.LocalFeedback
import dev.assemble.app.core.model.AppSettings
import dev.assemble.app.core.model.ThemePreference

private val MinRowHeight = 56.dp
private val GroupBorderWidth = 1.dp
private val SelectedBorderWidth = 2.dp
private val RowDividerInset = 64.dp
private val ChevronSize = 20.dp
private val MiniatureHeight = 92.dp
private val MiniaturePadding = 8.dp
private val MiniatureGap = 6.dp
private val MiniatureCardHeight = 44.dp
private val MiniatureLineHeight = 6.dp
private const val MiniatureLineFraction = 0.7f
private const val SelectedTileScale = 1.04f
private const val TileSpringDamping = 0.6f

@Composable
fun SettingsRoute(viewModel: SettingsViewModel, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    SettingsScreen(
        settings = settings,
        message = message,
        busy = busy,
        canClearSeen = viewModel.canClearSeen,
        accountDeletionDeferred = viewModel.accountDeletionDeferred,
        onBack = onBack,
        actions = SettingsActions(
            onThemeChange = viewModel::setTheme,
            onNotifyConnectionsChange = viewModel::setNotifyNewConnections,
            onNotifyMessagesChange = viewModel::setNotifyNewMessages,
            onSoundChange = viewModel::setSoundEnabled,
            onVibrationChange = viewModel::setVibrationEnabled,
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
    val onSoundChange: (Boolean) -> Unit,
    val onVibrationChange: (Boolean) -> Unit,
    val onResetPreferences: () -> Unit,
    val onClearSeen: () -> Unit,
    val onDeleteChats: () -> Unit,
    val onDeleteAccount: () -> Unit,
    val onMessageShown: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    message: SettingsMessage?,
    busy: Boolean,
    onBack: () -> Unit,
    actions: SettingsActions,
    modifier: Modifier = Modifier,
    canClearSeen: Boolean = true,
    accountDeletionDeferred: Boolean = false,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current
    val feedback = LocalFeedback.current
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
        if (current == SettingsMessage.ActionFailed) feedback.play(Cue.Error)
        // Limpar depois: mudar a chave antes cancelaria este efeito.
        snackbarHostState.showSnackbar(text)
        actions.onMessageShown()
    }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val spacing = AssembleTheme.spacing
    val colors = AssembleTheme.colors
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = colors.bg,
        topBar = {
            AssembleLargeTopBar(title = stringResource(R.string.settings_title), onBack = onBack, scrollBehavior = scrollBehavior)
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(spacing.space4),
            verticalArrangement = Arrangement.spacedBy(spacing.space5),
        ) {
            Section(stringResource(R.string.settings_appearance)) {
                ThemeTiles(selected = settings.theme, onSelect = actions.onThemeChange)
            }
            Section(stringResource(R.string.settings_notifications)) {
                Group {
                    SwitchRow(
                        AssembleIcons.HeartOutline,
                        stringResource(R.string.settings_notify_connections),
                        settings.notifyNewConnections,
                        actions.onNotifyConnectionsChange,
                    )
                    GroupDivider()
                    SwitchRow(
                        AssembleIcons.Chat,
                        stringResource(R.string.settings_notify_messages),
                        settings.notifyNewMessages,
                        actions.onNotifyMessagesChange,
                    )
                }
            }
            Section(stringResource(R.string.settings_sound_haptics)) {
                Group {
                    SwitchRow(
                        AssembleIcons.Volume,
                        stringResource(R.string.settings_sound),
                        settings.soundEnabled,
                        actions.onSoundChange,
                    )
                    GroupDivider()
                    SwitchRow(
                        AssembleIcons.Vibrate,
                        stringResource(R.string.settings_vibration),
                        settings.vibrationEnabled,
                        actions.onVibrationChange,
                    )
                }
                Text(
                    stringResource(R.string.settings_sound_hint),
                    style = AssembleTheme.typography.caption,
                    color = colors.textMuted,
                )
            }
            Section(stringResource(R.string.settings_data)) {
                Group {
                    ActionRow(AssembleIcons.Reset, stringResource(R.string.settings_reset_preferences), enabled = !busy, onClick = actions.onResetPreferences)
                    GroupDivider()
                    if (canClearSeen) {
                        ActionRow(AssembleIcons.EyeOff, stringResource(R.string.settings_clear_seen), enabled = !busy, onClick = actions.onClearSeen)
                        GroupDivider()
                    }
                    ActionRow(AssembleIcons.Trash, stringResource(R.string.settings_delete_chats), enabled = !busy, onClick = actions.onDeleteChats)
                }
                // Excluir a conta fica num grupo próprio, longe das ações reversíveis.
                Group {
                    ActionRow(
                        AssembleIcons.UserRemove,
                        stringResource(R.string.settings_delete_account),
                        enabled = !busy,
                        destructive = true,
                        onClick = { confirmDelete = true },
                    )
                }
            }
            // Fonte e IA ficam na tela Sobre; aqui só a versão, discreta no fim.
            Paragraph(stringResource(R.string.settings_about_version, BuildConfig.VERSION_NAME))
        }
    }

    if (confirmDelete) {
        DeleteAccountDialog(
            message = stringResource(
                if (accountDeletionDeferred) R.string.settings_delete_account_message_deferred else R.string.settings_delete_account_message,
            ),
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

/** Cartão que agrupa linhas relacionadas. */
@Composable
private fun Group(content: @Composable () -> Unit) {
    val colors = AssembleTheme.colors
    val shape = AssembleTheme.shapes.md
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface)
            .border(GroupBorderWidth, colors.border, shape),
    ) {
        content()
    }
}

@Composable
private fun GroupDivider() {
    HorizontalDivider(color = AssembleTheme.colors.border, modifier = Modifier.padding(start = RowDividerInset))
}

/** Tema escolhido por três miniaturas do próprio app; a troca abre em círculo a partir do toque. */
@Composable
private fun ThemeTiles(selected: ThemePreference, onSelect: (ThemePreference) -> Unit) {
    val options = listOf(
        ThemePreference.Light to R.string.settings_theme_light,
        ThemePreference.Dark to R.string.settings_theme_dark,
        ThemePreference.System to R.string.settings_theme_system,
    )
    Row(
        modifier = Modifier.fillMaxWidth().selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(AssembleTheme.spacing.space3),
    ) {
        options.forEach { (theme, label) ->
            ThemeTile(
                theme = theme,
                label = stringResource(label),
                selected = theme == selected,
                onClick = { onSelect(theme) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ThemeTile(theme: ThemePreference, label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = AssembleTheme.colors
    val shape = AssembleTheme.shapes.md
    val animationsEnabled = rememberAnimationsEnabled()
    val scale by animateFloatAsState(
        targetValue = if (selected) SelectedTileScale else 1f,
        animationSpec = if (animationsEnabled) spring(dampingRatio = TileSpringDamping) else snap(),
        label = "themeTileScale",
    )
    Column(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(colors.surface)
            .border(
                width = if (selected) SelectedBorderWidth else GroupBorderWidth,
                color = if (selected) colors.actionAssemble else colors.border,
                shape = shape,
            )
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(AssembleTheme.spacing.space2),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AssembleTheme.spacing.space2),
    ) {
        ThemeMiniature(theme)
        Text(
            text = label,
            style = AssembleTheme.typography.caption.copy(fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal),
            color = colors.text,
        )
    }
}

/** Miniatura do Discover no tema: fundo, card com o gradiente de energia e uma linha de texto. */
@Composable
private fun ThemeMiniature(theme: ThemePreference) {
    val light = LightAssembleColors
    val dark = DarkAssembleColors
    val shape = AssembleTheme.shapes.sm
    Box(
        Modifier
            .fillMaxWidth()
            .height(MiniatureHeight)
            .clip(shape)
            .drawBehind {
                when (theme) {
                    ThemePreference.Light -> drawRect(light.bg)
                    ThemePreference.Dark -> drawRect(dark.bg)
                    ThemePreference.System -> {
                        drawRect(light.bg)
                        val darkHalf = Path().apply {
                            moveTo(size.width, 0f)
                            lineTo(size.width, size.height)
                            lineTo(0f, size.height)
                            close()
                        }
                        drawPath(darkHalf, dark.bg)
                    }
                }
            }
            .padding(MiniaturePadding),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(MiniatureGap)) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(MiniatureCardHeight)
                    .energyGradient(AssembleTheme.colors, shape),
            )
            Box(
                Modifier
                    .fillMaxWidth(MiniatureLineFraction)
                    .height(MiniatureLineHeight)
                    .background(if (theme == ThemePreference.Dark) dark.border else light.border, AssembleTheme.shapes.pill),
            )
        }
    }
}

@Composable
private fun SwitchRow(icon: ImageVector, label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val colors = AssembleTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = MinRowHeight)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(horizontal = AssembleTheme.spacing.space4, vertical = AssembleTheme.spacing.space2),
        horizontalArrangement = Arrangement.spacedBy(AssembleTheme.spacing.space3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconTile(icon)
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

@Composable
private fun ActionRow(icon: ImageVector, label: String, enabled: Boolean, onClick: () -> Unit, destructive: Boolean = false) {
    val colors = AssembleTheme.colors
    val accent = if (destructive) colors.error else colors.accentText
    val textColor = when {
        !enabled -> colors.textMuted
        destructive -> colors.error
        else -> colors.text
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = MinRowHeight)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = AssembleTheme.spacing.space4, vertical = AssembleTheme.spacing.space2),
        horizontalArrangement = Arrangement.spacedBy(AssembleTheme.spacing.space3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconTile(icon, tint = accent)
        Text(label, style = AssembleTheme.typography.body, color = textColor, modifier = Modifier.weight(1f))
        Icon(AssembleIcons.ChevronRight, contentDescription = null, tint = colors.textMuted, modifier = Modifier.size(ChevronSize))
    }
}

@Composable
private fun Paragraph(text: String) {
    Text(text, style = AssembleTheme.typography.caption, color = AssembleTheme.colors.textMuted)
}

@Composable
private fun DeleteAccountDialog(message: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val colors = AssembleTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        title = { Text(stringResource(R.string.settings_delete_account_title), color = colors.text) },
        text = { Text(message, color = colors.textMuted) },
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

private val PreviewActions = SettingsActions({}, {}, {}, {}, {}, {}, {}, {}, {}, {})

@PreviewLightDark
@Composable
private fun SettingsPreview() {
    AssembleTheme {
        SettingsScreen(settings = AppSettings(), message = null, busy = false, onBack = {}, actions = PreviewActions)
    }
}
