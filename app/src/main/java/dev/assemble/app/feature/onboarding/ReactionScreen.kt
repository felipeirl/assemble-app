package dev.assemble.app.feature.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.assemble.app.R
import dev.assemble.app.core.designsystem.component.AssembleTopBar
import dev.assemble.app.core.designsystem.component.CharacterCardTeaser
import dev.assemble.app.core.designsystem.component.PrimaryButton
import dev.assemble.app.core.designsystem.component.SecondaryButton
import dev.assemble.app.core.designsystem.component.StateView
import dev.assemble.app.core.designsystem.component.StateViewType
import dev.assemble.app.core.designsystem.component.TopBarNavigation
import dev.assemble.app.core.designsystem.component.TopBarTitle
import dev.assemble.app.core.designsystem.theme.AssembleTheme
import dev.assemble.app.core.ui.traitLabel
import dev.assemble.app.feature.discover.DiscoverCard

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
        onReact = { liked ->
            val playing = state.reaction as? ReactionState.Playing
            viewModel.react(liked)
            // O último card leva direto à revelação; voltar até aqui mostra o resumo.
            if (playing != null && playing.index == playing.cards.lastIndex) onDone()
        },
        onRetry = viewModel::loadReactionCards,
        onDone = onDone,
        onBack = onBack,
        modifier = modifier,
    )
}

/** Curti/Pular em personagens conhecidos, só para o app conhecer o gosto. */
@Composable
fun ReactionScreen(
    reaction: ReactionState,
    onReact: (liked: Boolean) -> Unit,
    onRetry: () -> Unit,
    onDone: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AssembleTheme.colors
    val spacing = AssembleTheme.spacing
    val finished = reaction is ReactionState.Playing && reaction.finished
    Scaffold(
        modifier = modifier,
        containerColor = colors.bg,
        topBar = {
            AssembleTopBar(
                title = TopBarTitle.None,
                navigation = TopBarNavigation.Back(onBack),
                actions = {
                    if (!finished) {
                        TextButton(onClick = onDone) {
                            Text(
                                text = stringResource(R.string.onboarding_skip),
                                style = AssembleTheme.typography.caption.copy(fontWeight = FontWeight.SemiBold),
                                color = colors.accentText,
                            )
                        }
                    }
                },
            )
        },
        bottomBar = {
            if (finished) {
                OnboardingFooter {
                    PrimaryButton(stringResource(R.string.onboarding_next), onDone, Modifier.fillMaxWidth())
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = spacing.space4)
                .padding(bottom = spacing.space4),
            verticalArrangement = Arrangement.spacedBy(spacing.space3),
        ) {
            LinearProgressIndicator(
                progress = { (REACTION_STEP + 1f) / ONBOARDING_STEP_COUNT },
                modifier = Modifier.fillMaxWidth(),
                color = colors.actionAssemble,
                trackColor = colors.border,
                drawStopIndicator = {},
            )
            Text(
                text = stringResource(R.string.reaction_title).uppercase(),
                style = AssembleTheme.typography.displayMd,
                color = colors.text,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = stringResource(R.string.reaction_notice),
                style = AssembleTheme.typography.body,
                color = colors.textMuted,
            )
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
                is ReactionState.Playing -> {
                    val card = reaction.current
                    if (card == null) {
                        ReactionSummary(reaction)
                    } else {
                        ReactionCard(card, reaction, onReact)
                    }
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.ReactionCard(
    card: DiscoverCard,
    reaction: ReactionState.Playing,
    onReact: (liked: Boolean) -> Unit,
) {
    val spacing = AssembleTheme.spacing
    Text(
        text = stringResource(R.string.reaction_counter, reaction.index + 1, reaction.cards.size),
        style = AssembleTheme.typography.caption,
        color = AssembleTheme.colors.textMuted,
    )
    key(card.characterId) {
        CharacterCardTeaser(
            name = card.name,
            imageUrl = card.imageUrl,
            traitsInCommon = card.traitsInCommon.map { stringResource(traitLabel(it)) },
            tagline = card.tagline,
            modifier = Modifier.fillMaxWidth().weight(1f),
        )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(spacing.space3)) {
        SecondaryButton(
            text = stringResource(R.string.reaction_dislike),
            onClick = { onReact(false) },
            modifier = Modifier.weight(1f),
        )
        PrimaryButton(
            text = stringResource(R.string.reaction_like),
            onClick = { onReact(true) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ReactionSummary(reaction: ReactionState.Playing) {
    Text(
        text = stringResource(R.string.reaction_summary, reaction.liked, reaction.cards.size),
        style = AssembleTheme.typography.body,
        color = AssembleTheme.colors.text,
    )
}

private val PreviewCards = listOf(
    DiscoverCard("storm", "Storm", null, emptyList(), tagline = "Ororo Munroe controla o clima."),
    DiscoverCard("rocket", "Rocket Raccoon", null, emptyList()),
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
