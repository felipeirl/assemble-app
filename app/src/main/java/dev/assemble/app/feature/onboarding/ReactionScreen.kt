package dev.assemble.app.feature.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.assemble.app.R
import dev.assemble.app.core.designsystem.component.CharacterArt
import dev.assemble.app.core.designsystem.component.StateView
import dev.assemble.app.core.designsystem.component.StateViewType
import dev.assemble.app.core.designsystem.theme.AssembleTheme
import dev.assemble.app.core.designsystem.theme.rememberAnimationsEnabled
import dev.assemble.app.core.ui.traitLabel
import dev.assemble.app.feature.discover.DiscoverCard
import kotlinx.coroutines.delay

private const val NoneOfThem = -1
private const val PickedHoldMillis = 650L
private const val NoneHoldMillis = 150L
private const val NewPairMillis = 450
private const val WinScale = 1.03f
private const val LoseScale = 0.94f
private const val LoseAlpha = 0.25f
private const val ScrimAlpha = 0.82f
private const val TagAlpha = 0.2f
private const val TaglineAlpha = 0.9f
private const val MaxTags = 3
private val VsSize = 44.dp
private val DuelSpacing = 12.dp

@Composable
fun ReactionRoute(
    viewModel: OnboardingViewModel,
    onDone: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.loadReactionCards() }
    ReactionScreen(
        reaction = state.reaction,
        onReact = { picked ->
            val isLast = (state.reaction as? ReactionState.Playing)?.isLastPair == true
            viewModel.react(picked)
            // O último par leva direto à revelação.
            if (isLast) onDone()
        },
        onRetry = viewModel::loadReactionCards,
        onDone = onDone,
        onBack = onBack,
        modifier = modifier,
    )
}

/** "Este ou aquele": dois personagens por vez; tocar em um ensina o gosto, só isso. */
@Composable
fun ReactionScreen(
    reaction: ReactionState,
    onReact: (picked: Int?) -> Unit,
    onRetry: () -> Unit,
    onDone: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AssembleTheme.colors
    val spacing = AssembleTheme.spacing
    Scaffold(
        modifier = modifier,
        containerColor = colors.bg,
        topBar = { OnboardingTopBar(showBack = true, onBack = onBack, onSkip = onDone) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = spacing.space4),
            verticalArrangement = Arrangement.spacedBy(spacing.space3),
        ) {
            StepProgress(current = REACTION_STEP, total = ONBOARDING_STEP_COUNT)
            when (reaction) {
                ReactionState.Loading -> StateView(StateViewType.Loading(), Modifier.weight(1f))
                ReactionState.Failed -> StateView(
                    StateViewType.Error(
                        title = stringResource(R.string.reaction_error_title),
                        message = stringResource(R.string.reaction_error_message),
                        onRetry = onRetry,
                    ),
                    Modifier.weight(1f),
                )
                is ReactionState.Playing -> Duels(reaction, onReact)
            }
        }
    }
}

@Composable
private fun ColumnScope.Duels(
    reaction: ReactionState.Playing,
    onReact: (Int?) -> Unit,
) {
    val colors = AssembleTheme.colors
    val animationsEnabled = rememberAnimationsEnabled()
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(R.string.reaction_title),
            style = AssembleTheme.typography.h1,
            color = colors.text,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        Text(
            text = stringResource(R.string.reaction_counter, (reaction.pair + 1).coerceAtMost(reaction.pairs.size), reaction.pairs.size),
            style = AssembleTheme.typography.caption.copy(fontWeight = FontWeight.Bold),
            color = colors.textMuted,
        )
    }
    // O escolhido de cada par fica marcado por um instante antes de entrar o próximo.
    var picked by remember(reaction.pair) { mutableStateOf<Int?>(null) }
    LaunchedEffect(reaction.pair, picked) {
        val choice = picked ?: return@LaunchedEffect
        if (animationsEnabled) delay(if (choice == NoneOfThem) NoneHoldMillis else PickedHoldMillis)
        onReact(choice.takeIf { it != NoneOfThem })
    }
    Box(Modifier.weight(1f).fillMaxWidth()) {
        AnimatedContent(
            targetState = reaction.pair,
            transitionSpec = {
                if (animationsEnabled) {
                    (slideInVertically(tween(NewPairMillis)) { it / 6 } + fadeIn(tween(NewPairMillis))) togetherWith
                        fadeOut(tween(NewPairMillis / 2))
                } else {
                    fadeIn(tween(0)) togetherWith fadeOut(tween(0))
                }
            },
            label = "duelPair",
        ) { pairIndex ->
            val cards = reaction.pairs.getOrElse(pairIndex) { emptyList() }
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(DuelSpacing)) {
                cards.forEachIndexed { index, card ->
                    DuelCard(
                        card = card,
                        state = when {
                            pairIndex != reaction.pair || picked == null -> DuelState.Idle
                            picked == index -> DuelState.Won
                            else -> DuelState.Lost
                        },
                        onClick = { if (picked == null) picked = index },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        if (reaction.current.size == DUEL_SIZE) VsBadge(Modifier.align(Alignment.Center))
    }
    TextButton(
        onClick = { if (picked == null) picked = NoneOfThem },
        modifier = Modifier.align(Alignment.CenterHorizontally),
    ) {
        Text(
            text = stringResource(R.string.reaction_none),
            style = AssembleTheme.typography.caption.copy(fontWeight = FontWeight.Bold),
            color = colors.textMuted,
        )
    }
    Text(
        text = stringResource(R.string.reaction_notice),
        style = AssembleTheme.typography.small,
        color = colors.textMuted,
        modifier = Modifier.padding(bottom = AssembleTheme.spacing.space4),
    )
}

private enum class DuelState { Idle, Won, Lost }

@Composable
private fun VsBadge(modifier: Modifier = Modifier) {
    val colors = AssembleTheme.colors
    Box(
        modifier = modifier
            .size(VsSize)
            .clip(AssembleTheme.shapes.pill)
            .background(colors.bg)
            .border(3.dp, colors.bg, AssembleTheme.shapes.pill),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.reaction_vs),
            style = AssembleTheme.typography.caption.copy(fontWeight = FontWeight.ExtraBold),
            color = colors.text,
        )
    }
}

/** Card grande do duelo: arte, nome, frase e etiquetas. Cresce se for escolhido, apaga se não. */
@Composable
private fun DuelCard(
    card: DiscoverCard,
    state: DuelState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = AssembleTheme.shapes.lg
    val scale by animateFloatAsState(
        targetValue = when (state) {
            DuelState.Won -> WinScale
            DuelState.Lost -> LoseScale
            DuelState.Idle -> 1f
        },
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "duelScale",
    )
    val alpha by animateFloatAsState(if (state == DuelState.Lost) LoseAlpha else 1f, tween(NewPairMillis), label = "duelAlpha")
    val description = card.name
    Box(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale; this.alpha = alpha }
            .shadow(8.dp, shape)
            .clip(shape)
            .then(if (state == DuelState.Won) Modifier.border(3.dp, Color.White, shape) else Modifier)
            .clickable(role = Role.Button, onClickLabel = description, onClick = onClick)
            .semantics { contentDescription = description },
    ) {
        CharacterArt(
            name = card.name,
            imageUrl = card.imageUrl,
            modifier = Modifier.fillMaxSize(),
            initialsStyle = AssembleTheme.typography.displayXl,
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(0.45f to Color.Transparent, 1f to Color(0xFF050812).copy(alpha = ScrimAlpha))),
        )
        Column(
            modifier = Modifier.align(Alignment.BottomStart).padding(AssembleTheme.spacing.space4),
            verticalArrangement = Arrangement.spacedBy(AssembleTheme.spacing.space1),
        ) {
            Text(text = card.name, style = AssembleTheme.typography.displayMd, color = Color.White)
            card.tagline?.let {
                Text(
                    text = it,
                    style = AssembleTheme.typography.caption,
                    color = Color.White.copy(alpha = TaglineAlpha),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            val tags = card.traitsInCommon.take(MaxTags).map { stringResource(traitLabel(it)) }
            if (tags.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(AssembleTheme.spacing.space1)) {
                    tags.forEach { tag ->
                        Text(
                            text = tag,
                            style = AssembleTheme.typography.small.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            modifier = Modifier
                                .clip(AssembleTheme.shapes.pill)
                                .background(Color.White.copy(alpha = TagAlpha))
                                .padding(horizontal = AssembleTheme.spacing.space2, vertical = 2.dp),
                        )
                    }
                }
            }
        }
    }
}

private val PreviewCards = listOf(
    DiscoverCard("storm", "Storm", null, emptyList(), tagline = "Ororo Munroe controla o clima."),
    DiscoverCard("rocket", "Rocket Raccoon", null, emptyList(), tagline = "Guaxinim mercenário e inventor."),
)

@PreviewLightDark
@Composable
private fun ReactionPreview() {
    AssembleTheme {
        ReactionScreen(
            reaction = ReactionState.Playing(PreviewCards),
            onReact = {}, onRetry = {}, onDone = {}, onBack = {},
        )
    }
}
