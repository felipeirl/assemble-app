package dev.assemble.app.feature.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.assemble.app.R
import dev.assemble.app.core.designsystem.component.ActionButton
import dev.assemble.app.core.designsystem.component.ActionButtonType
import dev.assemble.app.core.designsystem.component.AssembleTopBar
import dev.assemble.app.core.designsystem.component.AvatarPreset
import dev.assemble.app.core.designsystem.component.StateView
import dev.assemble.app.core.designsystem.component.StateViewType
import dev.assemble.app.core.designsystem.component.TopBarNavigation
import dev.assemble.app.core.designsystem.component.TopBarTitle
import dev.assemble.app.core.designsystem.icon.AssembleIcons
import dev.assemble.app.core.designsystem.theme.AssembleTheme
import dev.assemble.app.core.designsystem.theme.rememberAnimationsEnabled
import dev.assemble.app.core.model.MatchBand
import dev.assemble.app.core.model.Origin
import dev.assemble.app.core.model.PowerFamily
import dev.assemble.app.core.model.Team
import kotlinx.coroutines.launch

private val UndoButtonSize = 48.dp
private const val DisabledAlpha = 0.4f

@Composable
fun DiscoverRoute(
    viewModel: DiscoverViewModel,
    onOpenMenu: () -> Unit,
    onOpenPreview: (characterId: String) -> Unit,
    onAdjustPreferences: () -> Unit,
    onStartChat: (connectionId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val profile by viewModel.userProfile.collectAsStateWithLifecycle()
    DiscoverScreen(
        state = state,
        userAvatarPreset = AvatarPreset.fromIndex(profile.avatarPreset),
        onOpenMenu = onOpenMenu,
        onCardClick = onOpenPreview,
        onSwiped = { card, direction ->
            when (direction) {
                SwipeDirection.Pass -> viewModel.pass(card.characterId)
                SwipeDirection.Assemble -> viewModel.assemble(card.characterId)
            }
        },
        onUndo = viewModel::undo,
        onRefresh = { viewModel.load(isRefresh = true) },
        onRetry = { viewModel.load() },
        onAdjustPreferences = onAdjustPreferences,
        onMessageShown = viewModel::onMessageShown,
        onStartChat = { connectionId ->
            viewModel.onMatchDismissed()
            onStartChat(connectionId)
        },
        onKeepDiscovering = viewModel::onMatchDismissed,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen(
    state: DiscoverUiState,
    userAvatarPreset: AvatarPreset = AvatarPreset.Energy,
    onOpenMenu: () -> Unit,
    onCardClick: (characterId: String) -> Unit,
    onSwiped: (DiscoverCard, SwipeDirection) -> Unit,
    onUndo: () -> Unit,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    onAdjustPreferences: () -> Unit,
    onMessageShown: () -> Unit,
    onStartChat: (connectionId: String) -> Unit = {},
    onKeepDiscovering: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    state.match?.let { match ->
        MatchOverlay(
            match = match,
            userAvatarPreset = userAvatarPreset,
            onStartChat = { onStartChat(match.connectionId) },
            onKeepDiscovering = onKeepDiscovering,
        )
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        val text = when (message) {
            DiscoverMessage.NotEnoughInCommon -> context.getString(R.string.discover_not_enough_in_common)
            DiscoverMessage.AssembleFailed -> context.getString(R.string.discover_assemble_failed)
        }
        // Limpar a mensagem só depois: mudar a chave antes cancelaria este efeito.
        snackbarHostState.showSnackbar(text)
        onMessageShown()
    }

    Scaffold(
        modifier = modifier,
        containerColor = AssembleTheme.colors.bg,
        topBar = { AssembleTopBar(title = TopBarTitle.Logo, navigation = TopBarNavigation.Menu(onOpenMenu)) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.refreshing,
            onRefresh = onRefresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // A rolagem vertical (sem conteúdo extra) habilita o gesto de puxar para atualizar.
            BoxWithConstraints(Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = maxHeight)
                        .verticalScroll(rememberScrollState())
                        .padding(AssembleTheme.spacing.space4),
                    verticalArrangement = Arrangement.spacedBy(AssembleTheme.spacing.space5),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    when (val deck = state.deck) {
                        DeckState.Loading -> StateView(
                            StateViewType.Loading(stringResource(R.string.discover_loading)),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        DeckState.Error -> StateView(
                            StateViewType.Error(
                                title = stringResource(R.string.discover_error_title),
                                message = stringResource(R.string.state_error_connection),
                                onRetry = onRetry,
                            ),
                        )
                        DeckState.Empty -> {
                            StateView(
                                StateViewType.Empty(
                                    icon = AssembleIcons.Compass,
                                    title = stringResource(R.string.discover_empty_title),
                                    actionLabel = stringResource(R.string.discover_adjust_preferences),
                                    onAction = onAdjustPreferences,
                                ),
                            )
                            if (state.canUndo) UndoButton(enabled = true, onClick = onUndo)
                        }
                        is DeckState.Content -> DeckContent(
                            cards = deck.cards,
                            canUndo = state.canUndo,
                            onSwiped = onSwiped,
                            onCardClick = onCardClick,
                            onUndo = onUndo,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DeckContent(
    cards: List<DiscoverCard>,
    canUndo: Boolean,
    onSwiped: (DiscoverCard, SwipeDirection) -> Unit,
    onCardClick: (characterId: String) -> Unit,
    onUndo: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val animationsEnabled = rememberAnimationsEnabled()
    val topCard = cards.first()
    val swipeState = remember(topCard.characterId) { SwipeCardState(animationsEnabled) }

    fun swipeWithButton(direction: SwipeDirection) {
        if (swipeState.isLeaving) return
        scope.launch {
            swipeState.swipeOut(direction)
            onSwiped(topCard, direction)
        }
    }

    SwipeCardStack(
        cards = cards,
        swipeState = swipeState,
        onSwiped = onSwiped,
        onCardClick = { onCardClick(it.characterId) },
        modifier = Modifier.fillMaxWidth(),
    )
    Row(
        horizontalArrangement = Arrangement.spacedBy(AssembleTheme.spacing.space5),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ActionButton(type = ActionButtonType.Pass, onClick = { swipeWithButton(SwipeDirection.Pass) })
        UndoButton(enabled = canUndo, onClick = onUndo)
        ActionButton(type = ActionButtonType.Assemble, onClick = { swipeWithButton(SwipeDirection.Assemble) })
    }
}

/** Desfaz o último Pass (uma vez). Neutro e menor que as ações principais. */
@Composable
private fun UndoButton(enabled: Boolean, onClick: () -> Unit) {
    val colors = AssembleTheme.colors
    val shape = AssembleTheme.shapes.pill
    Box(
        modifier = Modifier
            .size(UndoButtonSize)
            .alpha(if (enabled) 1f else DisabledAlpha)
            .clip(shape)
            .background(colors.surface)
            .border(1.dp, colors.border, shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = AssembleIcons.Undo,
            contentDescription = stringResource(R.string.discover_undo),
            tint = colors.textMuted,
        )
    }
}

private val PreviewCards = listOf(
    DiscoverCard("jean-grey", "Jean Grey", null, MatchBand.High, listOf(Origin.Mutant, PowerFamily.Mind, Team.XMen)),
    DiscoverCard("storm", "Storm", null, MatchBand.Possible, listOf(Origin.Mutant, Team.XMen)),
)

@PreviewLightDark
@Composable
private fun DiscoverContentPreview() {
    AssembleTheme {
        DiscoverScreen(
            state = DiscoverUiState(deck = DeckState.Content(PreviewCards), canUndo = true),
            onOpenMenu = {}, onCardClick = {}, onSwiped = { _, _ -> }, onUndo = {}, onRefresh = {},
            onRetry = {}, onAdjustPreferences = {}, onMessageShown = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun DiscoverEmptyPreview() {
    AssembleTheme {
        DiscoverScreen(
            state = DiscoverUiState(deck = DeckState.Empty),
            onOpenMenu = {}, onCardClick = {}, onSwiped = { _, _ -> }, onUndo = {}, onRefresh = {},
            onRetry = {}, onAdjustPreferences = {}, onMessageShown = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun DiscoverErrorPreview() {
    AssembleTheme {
        DiscoverScreen(
            state = DiscoverUiState(deck = DeckState.Error),
            onOpenMenu = {}, onCardClick = {}, onSwiped = { _, _ -> }, onUndo = {}, onRefresh = {},
            onRetry = {}, onAdjustPreferences = {}, onMessageShown = {},
        )
    }
}
