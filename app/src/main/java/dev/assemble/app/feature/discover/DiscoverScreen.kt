package dev.assemble.app.feature.discover

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalResources
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
import dev.assemble.app.core.feedback.Cue
import dev.assemble.app.core.feedback.LocalFeedback
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
    onOpenPreview: (DiscoverCard) -> Unit,
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
        onPass = { card -> viewModel.pass(card.characterId) },
        onAssemble = { card -> viewModel.assemble(card.characterId) },
        onUndo = viewModel::undo,
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

@Composable
fun DiscoverScreen(
    state: DiscoverUiState,
    onOpenMenu: () -> Unit,
    onCardClick: (DiscoverCard) -> Unit,
    onPass: (DiscoverCard) -> Unit,
    onUndo: () -> Unit,
    onRetry: () -> Unit,
    onAdjustPreferences: () -> Unit,
    onMessageShown: () -> Unit,
    modifier: Modifier = Modifier,
    onAssemble: (DiscoverCard) -> Unit = {},
    userAvatarPreset: AvatarPreset = AvatarPreset.Energy,
    onStartChat: (connectionId: String) -> Unit = {},
    onKeepDiscovering: () -> Unit = {},
) {
    // Onde o baralho e o botão Assemble estão: o card do match volta pela direita, de onde saiu.
    var cardBounds by remember { mutableStateOf<Rect?>(null) }
    var assembleBounds by remember { mutableStateOf<Rect?>(null) }
    state.match?.let { match ->
        val bounds = cardBounds
        MatchOverlay(
            match = match,
            userAvatarPreset = userAvatarPreset,
            cardBounds = bounds,
            cardOffset = if (bounds != null) Offset(bounds.width * ExitDistanceFactor, 0f) else Offset.Zero,
            cardRotation = MaxRotationDegrees,
            assembleBounds = assembleBounds,
            onStartChat = { onStartChat(match.connectionId) },
            onKeepDiscovering = onKeepDiscovering,
        )
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current
    val feedback = LocalFeedback.current
    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        val text = when (message) {
            DiscoverMessage.AssembleFailed -> resources.getString(R.string.discover_assemble_failed)
        }
        // Limpar a mensagem só depois: mudar a chave antes cancelaria este efeito.
        if (message == DiscoverMessage.AssembleFailed) feedback.play(Cue.Error)
        snackbarHostState.showSnackbar(text)
        onMessageShown()
    }

    Scaffold(
        modifier = modifier,
        containerColor = AssembleTheme.colors.bg,
        topBar = { AssembleTopBar(title = TopBarTitle.Logo, navigation = TopBarNavigation.Menu(onOpenMenu)) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
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
                    DeckComplete(onAdjustPreferences = onAdjustPreferences)
                    if (state.canUndo) UndoButton(enabled = true, onClick = onUndo)
                }
                is DeckState.Content -> DeckContent(
                    cards = deck.cards,
                    canUndo = state.canUndo,
                    onPass = onPass,
                    onAssemble = onAssemble,
                    onCardClick = onCardClick,
                    onUndo = onUndo,
                    onCardBounds = { cardBounds = it },
                    onAssembleBounds = { assembleBounds = it },
                )
            }
        }
        if (state.assembling) {
            AssemblingNotice(Modifier.align(Alignment.TopCenter).padding(top = AssembleTheme.spacing.space2))
        }
        }
    }
}

/** Aviso enquanto o personagem decide o match: com backend, a resposta pode levar vários segundos. */
@Composable
private fun AssemblingNotice(modifier: Modifier = Modifier) {
    val colors = AssembleTheme.colors
    val shape = AssembleTheme.shapes.pill
    Row(
        modifier = modifier
            .clip(shape)
            .background(colors.midnight)
            .padding(horizontal = AssembleTheme.spacing.space4, vertical = AssembleTheme.spacing.space2)
            .semantics { liveRegion = LiveRegionMode.Polite },
        horizontalArrangement = Arrangement.spacedBy(AssembleTheme.spacing.space2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(AssemblingSpinnerSize), strokeWidth = 2.dp, color = Color.White)
        Text(
            text = stringResource(R.string.discover_assembling),
            style = AssembleTheme.typography.caption,
            color = Color.White,
        )
    }
}

private val AssemblingSpinnerSize = 16.dp

/** Card na altura disponível, botões embaixo. */
@Composable
private fun ColumnScope.DeckContent(
    cards: List<DiscoverCard>,
    canUndo: Boolean,
    onPass: (DiscoverCard) -> Unit,
    onAssemble: (DiscoverCard) -> Unit,
    onCardClick: (DiscoverCard) -> Unit,
    onUndo: () -> Unit,
    onCardBounds: (Rect) -> Unit = {},
    onAssembleBounds: (Rect) -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    val animationsEnabled = rememberAnimationsEnabled()
    val topCard = cards.first()
    val swipeState = remember(topCard.characterId) { SwipeCardState(animationsEnabled) }

    /**
     * Mesma decisão para gesto e botões: o card sai voando e só então o personagem sai do baralho.
     * No Assemble, a resposta do personagem (pop-up de match ou aviso) chega alguns segundos depois.
     */
    fun decide(card: DiscoverCard, direction: SwipeDirection) {
        if (swipeState.isLeaving) return
        swipeState.markLeaving()
        scope.launch {
            swipeState.swipeOut(direction)
            when (direction) {
                SwipeDirection.Pass -> onPass(card)
                SwipeDirection.Assemble -> onAssemble(card)
            }
        }
    }

    fun swipeWithButton(direction: SwipeDirection) = decide(topCard, direction)

    SwipeCardStack(
        cards = cards,
        swipeState = swipeState,
        onDecide = ::decide,
        onCardClick = onCardClick,
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .onGloballyPositioned { onCardBounds(it.boundsInWindow()) },
    )
    Row(
        horizontalArrangement = Arrangement.spacedBy(AssembleTheme.spacing.space5),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ActionButton(type = ActionButtonType.Pass, onClick = { swipeWithButton(SwipeDirection.Pass) })
        UndoButton(enabled = canUndo, onClick = onUndo)
        ActionButton(
            type = ActionButtonType.Assemble,
            onClick = { swipeWithButton(SwipeDirection.Assemble) },
            modifier = Modifier.onGloballyPositioned { onAssembleBounds(it.boundsInWindow()) },
        )
    }
}

/** Desfaz o último Pass (uma vez). Neutro e menor que as ações principais. */
@Composable
private fun UndoButton(enabled: Boolean, onClick: () -> Unit) {
    val colors = AssembleTheme.colors
    val feedback = LocalFeedback.current
    val shape = AssembleTheme.shapes.pill
    Box(
        modifier = Modifier
            .size(UndoButtonSize)
            .alpha(if (enabled) 1f else DisabledAlpha)
            .clip(shape)
            .background(colors.surface)
            .border(1.dp, colors.border, shape)
            .clickable(enabled = enabled, role = Role.Button) {
                feedback.play(Cue.Tick)
                onClick()
            },
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
    DiscoverCard("jean-grey", "Jean Grey", null, listOf(Origin.Mutant, PowerFamily.Mind, Team.XMen)),
    DiscoverCard("storm", "Storm", null, listOf(Origin.Mutant, Team.XMen)),
)

@PreviewLightDark
@Composable
private fun DiscoverContentPreview() {
    AssembleTheme {
        DiscoverScreen(
            state = DiscoverUiState(deck = DeckState.Content(PreviewCards), canUndo = true),
            onOpenMenu = {}, onCardClick = {}, onPass = {}, onUndo = {},
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
            onOpenMenu = {}, onCardClick = {}, onPass = {}, onUndo = {},
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
            onOpenMenu = {}, onCardClick = {}, onPass = {}, onUndo = {},
            onRetry = {}, onAdjustPreferences = {}, onMessageShown = {},
        )
    }
}
