package dev.assemble.app.feature.character

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.assemble.app.R
import dev.assemble.app.core.designsystem.component.AssembleTopBar
import dev.assemble.app.core.designsystem.component.CharacterHero
import dev.assemble.app.core.designsystem.component.HeroBackButton
import dev.assemble.app.core.designsystem.component.LockedSection
import dev.assemble.app.core.designsystem.component.PrimaryButton
import dev.assemble.app.core.designsystem.component.ScoreRing
import dev.assemble.app.core.designsystem.component.StateView
import dev.assemble.app.core.designsystem.component.StateViewType
import dev.assemble.app.core.designsystem.component.TopBarNavigation
import dev.assemble.app.core.designsystem.component.TopBarTitle
import dev.assemble.app.core.designsystem.theme.AssembleTheme
import dev.assemble.app.core.model.Origin
import dev.assemble.app.core.model.PowerFamily
import dev.assemble.app.core.model.Powerstats
import dev.assemble.app.core.model.Style
import dev.assemble.app.core.model.Team
import kotlinx.coroutines.delay

private val ScoreRingSize = 96.dp
private const val TabFadeMillis = 200

/** Pausa antes do desbloqueio da primeira abertura, para o usuário ver o perfil borrado. */
private const val UnlockDelayMillis = 300L

@Composable
fun CharacterProfileRoute(
    viewModel: CharacterProfileViewModel,
    onBack: () -> Unit,
    onOpenChat: (connectionId: String) -> Unit,
    onOpenCharacter: (characterId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    CharacterProfileScreen(
        state = state,
        onBack = onBack,
        onOpenChat = onOpenChat,
        onOpenCharacter = onOpenCharacter,
        onRetry = viewModel::load,
        onUnlockSeen = viewModel::onUnlockSeen,
        modifier = modifier,
    )
}

@Composable
fun CharacterProfileScreen(
    state: CharacterProfileUiState,
    onBack: () -> Unit,
    onOpenChat: (connectionId: String) -> Unit,
    onRetry: () -> Unit,
    onUnlockSeen: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenCharacter: (characterId: String) -> Unit = {},
) {
    val content = state as? CharacterProfileUiState.Content
    Scaffold(
        modifier = modifier,
        containerColor = AssembleTheme.colors.bg,
        bottomBar = {
            if (content != null) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(AssembleTheme.colors.bg)
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(AssembleTheme.spacing.space4),
                ) {
                    PrimaryButton(
                        text = stringResource(R.string.nav_chat),
                        onClick = { onOpenChat(content.connectionId) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            when (state) {
                CharacterProfileUiState.Loading -> StateView(StateViewType.Loading(stringResource(R.string.profile_loading)))
                CharacterProfileUiState.Error -> StateView(
                    StateViewType.Error(
                        title = stringResource(R.string.profile_error_title),
                        message = stringResource(R.string.state_error_connection),
                        onRetry = onRetry,
                    ),
                )
                CharacterProfileUiState.Unavailable -> StateView(
                    StateViewType.Unavailable(
                        title = stringResource(R.string.character_unavailable_title),
                        message = stringResource(R.string.character_unavailable_message),
                    ),
                )
                is CharacterProfileUiState.Content -> ProfileContent(state, onUnlockSeen, onOpenCharacter)
            }
            HeroBackButton(
                onBack = onBack,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(AssembleTheme.spacing.space2),
            )
        }
    }
}

@Composable
private fun ProfileContent(
    content: CharacterProfileUiState.Content,
    onUnlockSeen: () -> Unit,
    onOpenCharacter: (String) -> Unit,
) {
    var locked by remember(content.connectionId) { mutableStateOf(content.unlockPending) }
    val currentOnUnlockSeen by rememberUpdatedState(onUnlockSeen)
    LaunchedEffect(content.connectionId, content.unlockPending) {
        if (content.unlockPending) {
            delay(UnlockDelayMillis)
            locked = false
            currentOnUnlockSeen()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        CharacterHero(name = content.name, imageUrl = content.imageUrl)
        ProfileDetails(content = content, locked = locked, onOpenCharacter = onOpenCharacter)
    }
}

@Composable
private fun ProfileDetails(content: CharacterProfileUiState.Content, locked: Boolean, onOpenCharacter: (String) -> Unit) {
    val colors = AssembleTheme.colors
    val spacing = AssembleTheme.spacing
    Column(
        modifier = Modifier.padding(spacing.space4),
        verticalArrangement = Arrangement.spacedBy(spacing.space4),
    ) {
        ScoreRing(percent = content.score, size = ScoreRingSize, animate = content.unlockPending)

        LockedSection(locked = locked, modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth().background(colors.surface).padding(spacing.space4),
                verticalArrangement = Arrangement.spacedBy(spacing.space4),
            ) {
                val tabs = CharacterTab.entries.filter { it != CharacterTab.Attributes || content.hasAttributes }
                var selected by rememberSaveable { mutableStateOf(CharacterTab.Overview) }
                // Aba que sumiu (sem dados) volta para a visão geral.
                val current = if (selected in tabs) selected else CharacterTab.Overview
                CharacterTabs(tabs = tabs, selected = current, onSelect = { selected = it })
                AnimatedContent(
                    targetState = current,
                    transitionSpec = { fadeIn(tween(TabFadeMillis)) togetherWith fadeOut(tween(TabFadeMillis)) },
                    label = "characterTab",
                ) { tab ->
                    when (tab) {
                        CharacterTab.Overview -> OverviewTab(content)
                        CharacterTab.Attributes -> AttributesTab(content)
                        CharacterTab.Connections -> ConnectionsTab(content, onOpenCharacter)
                    }
                }
                SourcesLabel(content.sources)
            }
        }
    }
}

private val ProfileSample = CharacterProfileUiState.Content(
    connectionId = "connection-jean-grey",
    name = "Jean Grey",
    imageUrl = null,
    score = 77,
    whyYouMatch = listOf(
        WhyYouMatchItem(R.string.profile_origin, listOf(Origin.Mutant)),
        WhyYouMatchItem(R.string.profile_powers, listOf(PowerFamily.Mind)),
        WhyYouMatchItem(R.string.profile_teams, listOf(Team.XMen)),
        WhyYouMatchItem(R.string.profile_style, listOf(Style.Idealist)),
    ),
    facts = listOf(
        ProfileFact(R.string.profile_origin, traits = listOf(Origin.Mutant)),
        ProfileFact(R.string.profile_first_appearance, text = "X-Men #1"),
    ),
    unlockPending = false,
    powerstats = Powerstats(intelligence = 94, strength = 80, speed = 21, durability = 20, power = 92, combat = 70),
    teams = listOf(Team.XMen),
)

@PreviewLightDark
@Composable
private fun CharacterProfilePreview() {
    AssembleTheme {
        CharacterProfileScreen(ProfileSample, onBack = {}, onOpenChat = {}, onRetry = {}, onUnlockSeen = {})
    }
}

@PreviewLightDark
@Composable
private fun CharacterProfileErrorPreview() {
    AssembleTheme {
        CharacterProfileScreen(CharacterProfileUiState.Error, onBack = {}, onOpenChat = {}, onRetry = {}, onUnlockSeen = {})
    }
}
