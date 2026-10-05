package dev.assemble.app.feature.character

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
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
import kotlinx.coroutines.launch
import dev.assemble.app.core.designsystem.theme.rememberAnimationsEnabled
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.LaunchedEffect
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
import dev.assemble.app.core.designsystem.component.CharacterHero
import dev.assemble.app.core.designsystem.component.HeroBackButton
import dev.assemble.app.core.designsystem.component.LockedSection
import dev.assemble.app.core.designsystem.component.SourceLabel
import dev.assemble.app.core.designsystem.component.StateView
import dev.assemble.app.core.designsystem.component.StateViewType
import dev.assemble.app.core.designsystem.component.TopBarNavigation
import dev.assemble.app.core.designsystem.component.TopBarTitle
import dev.assemble.app.core.designsystem.component.TraitChip
import dev.assemble.app.core.designsystem.theme.AssembleTheme
import dev.assemble.app.core.model.Origin
import dev.assemble.app.core.model.PowerFamily
import dev.assemble.app.core.ui.detailsEnterFromBelow
import dev.assemble.app.core.ui.sharedCharacterArt
import dev.assemble.app.core.ui.traitLabel

private val PlaceholderLineHeight = 14.dp
private val PlaceholderLineFractions = listOf(1f, 0.85f, 0.6f)

private const val ExitMillis = 350
private const val ExitTravelFactor = 1.3f
private const val ExitRotationDegrees = 14f
private const val ExitFadeFactor = 0.3f
private val ExitEasing = CubicBezierEasing(0.4f, 0f, 0.6f, 1f)

/** O que já se sabe do personagem ao abrir (vem do card): a arte aparece no primeiro quadro. */
data class PreviewHero(val characterId: String, val name: String, val imageUrl: String?)

@Composable
fun CharacterPreviewRoute(
    viewModel: CharacterPreviewViewModel,
    hero: PreviewHero?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    CharacterPreviewScreen(
        state = state,
        hero = hero,
        onBack = onBack,
        onPass = { viewModel.pass(onDone = onBack) },
        onAssemble = { viewModel.assemble(onDone = onBack) },
        onRetry = viewModel::load,
        modifier = modifier,
    )
}

/**
 * Pré-visualização antes da conexão. A arte do topo é a mesma do card do Discover e chega voando
 * dele; os detalhes sobem por baixo quando carregam. Ao decidir aqui, a arte sai voando para o lado
 * da decisão (esquerda = Passar, direita = Assemble), como no swipe; se a decisão falhar, volta.
 */
@Composable
fun CharacterPreviewScreen(
    state: CharacterPreviewUiState,
    hero: PreviewHero?,
    onBack: () -> Unit,
    onPass: () -> Unit,
    onAssemble: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val content = state as? CharacterPreviewUiState.Content
    val scope = rememberCoroutineScope()
    val animationsEnabled = rememberAnimationsEnabled()
    val exit = remember { Animatable(0f) }
    var exitDirection by remember { mutableFloatStateOf(0f) }
    val acting = content?.acting == true
    // Decisão que falhou: a tela continua aqui, então a arte volta ao lugar.
    LaunchedEffect(acting) {
        if (!acting && exit.value > 0f) exit.animateTo(0f, tween(ExitMillis))
    }
    fun decide(direction: Float, action: () -> Unit) {
        exitDirection = direction
        if (animationsEnabled) scope.launch { exit.animateTo(1f, tween(ExitMillis, easing = ExitEasing)) }
        action()
    }
    Scaffold(
        modifier = modifier,
        containerColor = AssembleTheme.colors.bg,
        bottomBar = {
            if (content != null) {
                PreviewActions(
                    enabled = !content.acting,
                    onPass = { decide(-1f, onPass) },
                    onAssemble = { decide(1f, onAssemble) },
                )
            }
        },
    ) { padding ->
        val spacing = AssembleTheme.spacing
        Box(Modifier.fillMaxSize().padding(padding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val heroName = hero?.name ?: content?.name
                if (heroName != null) {
                    CharacterHero(
                        name = heroName,
                        imageUrl = hero?.imageUrl ?: content?.imageUrl,
                        artModifier = hero?.let { Modifier.sharedCharacterArt(it.characterId) } ?: Modifier,
                        modifier = Modifier.graphicsLayer {
                            val p = exit.value
                            translationX = exitDirection * p * size.width * ExitTravelFactor
                            rotationZ = exitDirection * p * ExitRotationDegrees
                            alpha = 1f - p * ExitFadeFactor
                        },
                    )
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .detailsEnterFromBelow()
                        .graphicsLayer { alpha = 1f - exit.value }
                        .padding(spacing.space4),
                    verticalArrangement = Arrangement.spacedBy(spacing.space4),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    PreviewDetails(state, onRetry)
                }
            }
            HeroBackButton(onBack = onBack, modifier = Modifier.padding(spacing.space2))
        }
    }
}

@Composable
private fun PreviewDetails(state: CharacterPreviewUiState, onRetry: () -> Unit) {
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PreviewContent(content: CharacterPreviewUiState.Content) {
    val colors = AssembleTheme.colors
    val spacing = AssembleTheme.spacing
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.space4),
    ) {
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
    traitsInCommon = listOf(Origin.Mutant, PowerFamily.Mind),
)

@PreviewLightDark
@Composable
private fun CharacterPreviewPreview() {
    AssembleTheme {
        CharacterPreviewScreen(PreviewContentSample, hero = null, onBack = {}, onPass = {}, onAssemble = {}, onRetry = {})
    }
}

@PreviewLightDark
@Composable
private fun CharacterPreviewUnavailablePreview() {
    AssembleTheme {
        CharacterPreviewScreen(CharacterPreviewUiState.Unavailable, hero = PreviewHero("jean-grey", "Jean Grey", null), onBack = {}, onPass = {}, onAssemble = {}, onRetry = {})
    }
}
