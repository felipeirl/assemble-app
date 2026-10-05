package dev.assemble.app.feature.chat

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.assemble.app.core.designsystem.theme.AssembleTheme
import dev.assemble.app.core.designsystem.theme.rememberAnimationsEnabled
import dev.assemble.app.core.ui.traitLabel
import kotlinx.coroutines.delay

private const val ChipStaggerMillis = 60
private const val ChipEnterMillis = 350
private val ChipLift = 8.dp
private val ChipBorderWidth = 1.dp
private val ChipMinHeight = 40.dp
private val ChipEasing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)

/** Texto final da sugestão, no idioma do app. */
@Composable
fun ReplySuggestion.resolve(): String = when (val value = arg) {
    null -> stringResource(text)
    is SuggestionArg.Name -> stringResource(text, value.name)
    is SuggestionArg.Trait -> stringResource(text, stringResource(traitLabel(value.trait)))
    is SuggestionArg.Literal -> value.text
}

/** Chave da lista: precisa caber num Bundle (texto), por isso não é o objeto em si. */
private fun ReplySuggestion.stableKey(): String = when (val value = arg) {
    null -> "$text"
    is SuggestionArg.Name -> "$text:name:${value.name}"
    is SuggestionArg.Trait -> "$text:trait:${value.trait.name}"
    is SuggestionArg.Literal -> "literal:${value.text}"
}

/** Respostas sugeridas acima do campo. Um novo conjunto entra em cascata; tocar envia direto. */
@Composable
fun SuggestionChips(suggestions: List<ReplySuggestion>, onSuggestion: (String) -> Unit, modifier: Modifier = Modifier) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(start = AssembleTheme.spacing.space4, end = AssembleTheme.spacing.space4, top = AssembleTheme.spacing.space2),
        horizontalArrangement = Arrangement.spacedBy(AssembleTheme.spacing.space2),
    ) {
        itemsIndexed(suggestions, key = { _, item -> item.stableKey() }) { index, item ->
            SuggestionChip(text = item.resolve(), index = index, onClick = onSuggestion)
        }
    }
}

@Composable
private fun SuggestionChip(text: String, index: Int, onClick: (String) -> Unit) {
    val colors = AssembleTheme.colors
    val shape = AssembleTheme.shapes.pill
    val animationsEnabled = rememberAnimationsEnabled()
    val progress = remember { Animatable(if (animationsEnabled) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (!animationsEnabled) return@LaunchedEffect
        delay((index * ChipStaggerMillis).toLong())
        progress.animateTo(1f, tween(ChipEnterMillis, easing = ChipEasing))
    }
    val liftPx = with(LocalDensity.current) { ChipLift.toPx() }
    Box(
        modifier = Modifier
            .graphicsLayer {
                alpha = progress.value
                translationY = (1f - progress.value) * liftPx
            }
            .clip(shape)
            .border(ChipBorderWidth, colors.accentText, shape)
            .clickable(role = Role.Button, onClick = { onClick(text) })
            .defaultMinSize(minHeight = ChipMinHeight)
            .padding(horizontal = AssembleTheme.spacing.space3, vertical = AssembleTheme.spacing.space2),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = AssembleTheme.typography.caption.copy(fontWeight = FontWeight.SemiBold),
            color = colors.accentText,
        )
    }
}
