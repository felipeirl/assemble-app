package dev.assemble.app.feature.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.assemble.app.R
import dev.assemble.app.core.data.mock.MockSeed
import dev.assemble.app.core.designsystem.component.AssembleTopBar
import dev.assemble.app.core.designsystem.component.AvatarPreset
import dev.assemble.app.core.designsystem.component.CharacterAvatar
import dev.assemble.app.core.designsystem.component.PrimaryButton
import dev.assemble.app.core.designsystem.component.SecondaryButton
import dev.assemble.app.core.designsystem.component.StateView
import dev.assemble.app.core.designsystem.component.StateViewType
import dev.assemble.app.core.designsystem.component.TopBarNavigation
import dev.assemble.app.core.designsystem.component.TopBarTitle
import dev.assemble.app.core.designsystem.component.TraitChip
import dev.assemble.app.core.designsystem.component.UserAvatar
import dev.assemble.app.core.designsystem.theme.AssembleTheme
import dev.assemble.app.core.model.Origin
import dev.assemble.app.core.model.PowerFamily
import dev.assemble.app.core.model.PreferenceCategory
import dev.assemble.app.core.model.Preferences
import dev.assemble.app.core.model.Team
import dev.assemble.app.core.model.selectAny
import dev.assemble.app.core.model.toggle
import dev.assemble.app.core.model.withFame
import dev.assemble.app.core.ui.PreferenceEditor
import dev.assemble.app.core.ui.preferenceSummary
import dev.assemble.app.core.ui.title
import dev.assemble.app.core.ui.traitLabel
import kotlinx.coroutines.launch

private val ProfileAvatarSize = 88.dp
private val ConnectionAvatarSize = 64.dp
private val ConnectionTileWidth = 88.dp
private val MinRowHeight = 48.dp

@Composable
fun ProfileRoute(
    viewModel: ProfileViewModel,
    onOpenMenu: () -> Unit,
    onEditProfile: () -> Unit,
    onOpenCharacter: (characterId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    ProfileScreen(
        state = state,
        message = message,
        onOpenMenu = onOpenMenu,
        onEditProfile = onEditProfile,
        onOpenCharacter = onOpenCharacter,
        onSavePreferences = viewModel::savePreferences,
        onMessageShown = viewModel::onMessageShown,
        onRetry = viewModel::retry,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    state: ProfileUiState,
    message: ProfileMessage?,
    onOpenMenu: () -> Unit,
    onEditProfile: () -> Unit,
    onOpenCharacter: (characterId: String) -> Unit,
    onSavePreferences: (Preferences) -> Unit,
    onMessageShown: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    LaunchedEffect(message) {
        val current = message ?: return@LaunchedEffect
        val text = when (current) {
            ProfileMessage.PreferencesUpdated -> context.getString(R.string.profile_preferences_updated)
            ProfileMessage.PreferencesFailed -> context.getString(R.string.onboarding_error)
        }
        // Limpar depois: mudar a chave antes cancelaria este efeito.
        snackbarHostState.showSnackbar(text)
        onMessageShown()
    }
    var editing by remember { mutableStateOf<PreferenceCategory?>(null) }

    Scaffold(
        modifier = modifier,
        containerColor = AssembleTheme.colors.bg,
        topBar = {
            AssembleTopBar(
                title = TopBarTitle.Text(stringResource(R.string.nav_profile)),
                navigation = TopBarNavigation.Menu(onOpenMenu),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            when (state) {
                ProfileUiState.Loading -> StateView(StateViewType.Loading(), modifier = Modifier.fillMaxWidth())
                ProfileUiState.Error -> StateView(
                    StateViewType.Error(
                        title = stringResource(R.string.user_profile_error_title),
                        message = stringResource(R.string.state_error_connection),
                        onRetry = onRetry,
                    ),
                )
                is ProfileUiState.Content -> ProfileContent(
                    content = state,
                    onEditProfile = onEditProfile,
                    onEditPreference = { editing = it },
                    onOpenCharacter = onOpenCharacter,
                )
            }
        }
    }

    val content = state as? ProfileUiState.Content
    val category = editing
    if (content != null && category != null) {
        PreferenceSheet(
            category = category,
            initial = content.preferences,
            onDismiss = { editing = null },
            onSave = { updated ->
                editing = null
                onSavePreferences(updated)
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProfileContent(
    content: ProfileUiState.Content,
    onEditProfile: () -> Unit,
    onEditPreference: (PreferenceCategory) -> Unit,
    onOpenCharacter: (String) -> Unit,
) {
    val colors = AssembleTheme.colors
    val spacing = AssembleTheme.spacing
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(spacing.space4),
        verticalArrangement = Arrangement.spacedBy(spacing.space6),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.space2),
        ) {
            UserAvatar(preset = AvatarPreset.fromIndex(content.profile.avatarPreset), size = ProfileAvatarSize)
            Text(content.profile.name, style = AssembleTheme.typography.h2, color = colors.text, textAlign = TextAlign.Center)
            if (content.profile.bio.isNotBlank()) {
                Text(content.profile.bio, style = AssembleTheme.typography.body, color = colors.textMuted, textAlign = TextAlign.Center)
            }
            SecondaryButton(text = stringResource(R.string.profile_edit), onClick = onEditProfile)
        }

        StatsRow(content.stats)

        Section(stringResource(R.string.profile_my_preferences)) {
            PreferenceCategory.entries.forEachIndexed { index, category ->
                if (index > 0) HorizontalDivider(color = colors.border)
                PreferenceRow(category, content.preferences, onClick = { onEditPreference(category) })
            }
        }

        if (content.topTraits.isNotEmpty()) {
            Section(stringResource(R.string.profile_top_traits)) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(spacing.space2),
                    verticalArrangement = Arrangement.spacedBy(spacing.space2),
                ) {
                    content.topTraits.forEach { TraitChip(label = stringResource(traitLabel(it))) }
                }
            }
        }

        Section(stringResource(R.string.profile_stat_connections)) {
            if (content.connections.isEmpty()) {
                Text(stringResource(R.string.chat_list_empty_title), style = AssembleTheme.typography.caption, color = colors.textMuted)
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(spacing.space3),
                    verticalArrangement = Arrangement.spacedBy(spacing.space3),
                ) {
                    content.connections.forEach { connection ->
                        ConnectionTile(connection, onClick = { onOpenCharacter(connection.characterId) })
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsRow(stats: ProfileStats) {
    Row(Modifier.fillMaxWidth()) {
        StatItem(stats.charactersSeen.toString(), stringResource(R.string.profile_stat_seen), Modifier.weight(1f))
        StatItem(stats.connections.toString(), stringResource(R.string.profile_stat_connections), Modifier.weight(1f))
        StatItem(
            value = stats.averageMatch?.let { stringResource(R.string.score_ring_value, it) } ?: stringResource(R.string.value_none),
            label = stringResource(R.string.profile_stat_average),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun StatItem(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.semantics(mergeDescendants = true) {},
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, style = AssembleTheme.typography.h2, color = AssembleTheme.colors.text)
        Text(label, style = AssembleTheme.typography.caption, color = AssembleTheme.colors.textMuted, textAlign = TextAlign.Center)
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(AssembleTheme.spacing.space3)) {
        Text(
            text = title,
            style = AssembleTheme.typography.h2,
            color = AssembleTheme.colors.text,
            modifier = Modifier.semantics { heading() },
        )
        content()
    }
}

@Composable
private fun PreferenceRow(category: PreferenceCategory, preferences: Preferences, onClick: () -> Unit) {
    val colors = AssembleTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = MinRowHeight)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = AssembleTheme.spacing.space3),
        verticalArrangement = Arrangement.spacedBy(AssembleTheme.spacing.space1),
    ) {
        Text(
            stringResource(category.title),
            style = AssembleTheme.typography.body.copy(fontWeight = FontWeight.SemiBold),
            color = colors.text,
        )
        Text(preferenceSummary(category, preferences), style = AssembleTheme.typography.caption, color = colors.textMuted)
    }
}

@Composable
private fun ConnectionTile(connection: ProfileConnection, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(ConnectionTileWidth)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(AssembleTheme.spacing.space1),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AssembleTheme.spacing.space1),
    ) {
        CharacterAvatar(name = connection.name, imageUrl = connection.imageUrl, size = ConnectionAvatarSize)
        Text(
            text = connection.name,
            style = AssembleTheme.typography.small,
            color = AssembleTheme.colors.text,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Edição de uma categoria. Descartar o sheet cancela; "Save" grava. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PreferenceSheet(
    category: PreferenceCategory,
    initial: Preferences,
    onDismiss: () -> Unit,
    onSave: (Preferences) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var draft by remember(category) { mutableStateOf(initial) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AssembleTheme.colors.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AssembleTheme.spacing.space4)
                .padding(bottom = AssembleTheme.spacing.space5),
            verticalArrangement = Arrangement.spacedBy(AssembleTheme.spacing.space4),
        ) {
            Text(
                text = stringResource(category.title).uppercase(),
                style = AssembleTheme.typography.displayMd,
                color = AssembleTheme.colors.text,
                modifier = Modifier.semantics { heading() },
            )
            PreferenceEditor(
                category = category,
                preferences = draft,
                onToggle = { draft = draft.toggle(it) },
                onSelectAny = { draft = draft.selectAny(category) },
                onFameChange = { draft = draft.withFame(it) },
            )
            PrimaryButton(
                text = stringResource(R.string.action_save),
                onClick = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion { onSave(draft) }
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private val ProfileSample = ProfileUiState.Content(
    profile = MockSeed.initialProfile,
    stats = ProfileStats(charactersSeen = 4, connections = 2, averageMatch = 36),
    preferences = MockSeed.initialPreferences,
    topTraits = listOf(Origin.Mutant, PowerFamily.Flight, Team.XMen),
    connections = listOf(
        ProfileConnection("spider-man", "Spider-Man", null),
        ProfileConnection("storm", "Storm", null),
    ),
)

@PreviewLightDark
@Composable
private fun ProfilePreview() {
    AssembleTheme {
        ProfileScreen(
            state = ProfileSample, message = null, onOpenMenu = {}, onEditProfile = {}, onOpenCharacter = {},
            onSavePreferences = {}, onMessageShown = {}, onRetry = {},
        )
    }
}
