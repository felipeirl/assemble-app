package dev.assemble.app.feature.character

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.assemble.app.R
import dev.assemble.app.core.designsystem.component.ActionButton
import dev.assemble.app.core.designsystem.component.ActionButtonType
import dev.assemble.app.core.designsystem.component.AssembleTopBar
import dev.assemble.app.core.designsystem.component.CharacterArt
import dev.assemble.app.core.designsystem.component.LockedSection
import dev.assemble.app.core.designsystem.component.MatchBandChip
import dev.assemble.app.core.designsystem.component.SourceLabel
import dev.assemble.app.core.designsystem.component.StateView
import dev.assemble.app.core.designsystem.component.StateViewType
import dev.assemble.app.core.designsystem.component.TopBarNavigation
import dev.assemble.app.core.designsystem.component.TopBarTitle
import dev.assemble.app.core.designsystem.component.TraitChip
import dev.assemble.app.core.designsystem.theme.AssembleTheme
import dev.assemble.app.core.model.MatchBand
import dev.assemble.app.core.model.Origin
import dev.assemble.app.core.model.PowerFamily
import dev.assemble.app.core.ui.traitLabel

private val HeroHeight = 280.dp
private val PlaceholderLineHeight = 14.dp
private val PlaceholderLineFractions = listOf(1f, 0.85f, 0.6f)

@Composable
fun CharacterPreviewRoute(
    viewModel: CharacterPreviewViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    CharacterPreviewScreen(
        state = state,
        onBack = onBack,
        onPass = { viewModel.pass(onDone = onBack) },
        onAssemble = { viewModel.assemble(onDone = onBack) },
        onRetry = viewModel::load,
        modifier = modifier,
    )
}

@Composable
fun CharacterPreviewScreen(
    state: CharacterPreviewUiState,
    onBack: () -> Unit,
    onPass: () -> Unit,
    onAssemble: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val content = state as? CharacterPreviewUiState.Content
    Scaffold(
        modifier = modifier,
        containerColor = AssembleTheme.colors.bg,
        topBar = {
            AssembleTopBar(
                title = content?.let { TopBarTitle.Text(it.name) } ?: TopBarTitle.None,
                navigation = TopBarNavigation.Back(onBack),
            )
        },
        bottomBar = {
            if (content != null) {
                PreviewActions(enabled = !content.acting, onPass = onPass, onAssemble = onAssemble)
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            when (state) {
                CharacterPreviewUiState.Loading -> StateView(StateViewType.Loading(stringResource(R.string.preview_loading)))
                CharacterPreviewUiState.Error -> StateView(
                    StateViewType.Error(
                        title = stringResource(R.string.preview_error_title),
                        message = stringResource(R.string.state_error_connection),
                        onRetry = onRetry,
                    ),
                )
                CharacterPreviewUiState.Unavailable -> StateView(
                    StateViewType.Unavailable(
                        title = stringResource(R.string.character_unavailable_title),
                        message = stringResource(R.string.character_unavailable_message),
                    ),
                )
                is CharacterPreviewUiState.Content -> PreviewContent(state)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PreviewContent(content: CharacterPreviewUiState.Content) {
    val colors = AssembleTheme.colors
    val spacing = AssembleTheme.spacing
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(spacing.space4),
        verticalArrangement = Arrangement.spacedBy(spacing.space4),
    ) {
        Box(Modifier.fillMaxWidth().height(HeroHeight).clip(AssembleTheme.shapes.lg)) {
            CharacterArt(name = content.name, imageUrl = content.imageUrl, modifier = Modifier.fillMaxSize())
            MatchBandChip(
                band = content.band,
                modifier = Modifier.align(Alignment.TopEnd).padding(spacing.space3),
            )
        }
        Text(text = content.name.uppercase(), style = AssembleTheme.typography.displayMd, color = colors.text)
        if (content.traitsInCommon.isNotEmpty()) {
            Text(
                text = stringResource(R.string.card_in_common).uppercase(),
                style = AssembleTheme.typography.eyebrow,
                color = colors.textMuted,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(spacing.space2),
                verticalArrangement = Arrangement.spacedBy(spacing.space2),
            ) {
                content.traitsInCommon.forEach { TraitChip(label = stringResource(traitLabel(it))) }
            }
        }
        // O conteúdo real só existe depois da conexão: aqui ficam apenas linhas de espaço reservado.
        LockedSection(locked = true, modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth().background(colors.surface).padding(spacing.space4),
                verticalArrangement = Arrangement.spacedBy(spacing.space4),
            ) {
                listOf(
                    R.string.profile_bio,
                    R.string.profile_powers,
                    R.string.profile_teams,
                    R.string.profile_first_appearance,
                ).forEach { LockedPlaceholderSection(stringResource(it)) }
            }
        }
        SourceLabel()
    }
}

@Composable
private fun LockedPlaceholderSection(title: String) {
    Column(verticalArrangement = Arrangement.spacedBy(AssembleTheme.spacing.space2)) {
        Text(
            text = title,
            style = AssembleTheme.typography.h2,
            color = AssembleTheme.colors.text,
            modifier = Modifier.semantics { heading() },
        )
        PlaceholderLineFractions.forEach { fraction ->
            Box(
                Modifier
                    .fillMaxWidth(fraction)
                    .height(PlaceholderLineHeight)
                    .background(AssembleTheme.colors.border, AssembleTheme.shapes.sm),
            )
        }
    }
}

@Composable
private fun PreviewActions(enabled: Boolean, onPass: () -> Unit, onAssemble: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AssembleTheme.colors.bg)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(AssembleTheme.spacing.space4),
        horizontalArrangement = Arrangement.spacedBy(AssembleTheme.spacing.space6, Alignment.CenterHorizontally),
    ) {
        ActionButton(type = ActionButtonType.Pass, onClick = onPass, enabled = enabled)
        ActionButton(type = ActionButtonType.Assemble, onClick = onAssemble, enabled = enabled)
    }
}

private val PreviewContentSample = CharacterPreviewUiState.Content(
    name = "Jean Grey",
    imageUrl = null,
    band = MatchBand.High,
    traitsInCommon = listOf(Origin.Mutant, PowerFamily.Mind),
)

@PreviewLightDark
@Composable
private fun CharacterPreviewPreview() {
    AssembleTheme {
        CharacterPreviewScreen(PreviewContentSample, onBack = {}, onPass = {}, onAssemble = {}, onRetry = {})
    }
}

@PreviewLightDark
@Composable
private fun CharacterPreviewUnavailablePreview() {
    AssembleTheme {
        CharacterPreviewScreen(CharacterPreviewUiState.Unavailable, onBack = {}, onPass = {}, onAssemble = {}, onRetry = {})
    }
}
