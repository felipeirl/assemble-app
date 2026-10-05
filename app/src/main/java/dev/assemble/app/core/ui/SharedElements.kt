package dev.assemble.app.core.ui

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import dev.assemble.app.core.designsystem.theme.rememberAnimationsEnabled

private const val SharedMillis = 450
private const val DetailsMillis = 400
private const val DetailsDelayMillis = 150
private val SharedEasing = CubicBezierEasing(0.65f, 0f, 0.35f, 1f)
private val DetailsEasing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)

/**
 * Escopo de elementos compartilhados entre telas. Só existe dentro da navegação principal;
 * fora dela (catálogo, previews) é null e os modificadores abaixo não fazem nada.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }

/** Chave da arte de um personagem: liga o card do Discover ao topo da pré-visualização. */
fun characterArtKey(characterId: String): String = "character-art-$characterId"

/** A arte do personagem voa entre o card do Discover e a pré-visualização. */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.sharedCharacterArt(characterId: String): Modifier {
    val sharedScope = LocalSharedTransitionScope.current ?: return this
    if (!rememberAnimationsEnabled()) return this
    return with(sharedScope) {
        this@sharedCharacterArt.sharedElement(
            sharedContentState = rememberSharedContentState(characterArtKey(characterId)),
            animatedVisibilityScope = LocalNavAnimatedContentScope.current,
            boundsTransform = { _, _ -> tween(SharedMillis, easing = SharedEasing) },
        )
    }
}

/** Detalhes que sobem por baixo enquanto a arte compartilhada chega. */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.detailsEnterFromBelow(): Modifier {
    if (LocalSharedTransitionScope.current == null || !rememberAnimationsEnabled()) return this
    return with(LocalNavAnimatedContentScope.current) {
        this@detailsEnterFromBelow.animateEnterExit(
            enter = fadeIn(tween(DetailsMillis, DetailsDelayMillis)) +
                slideInVertically(tween(DetailsMillis, DetailsDelayMillis, DetailsEasing)) { it / 6 },
            exit = fadeOut(tween(DetailsMillis / 2)) + slideOutVertically(tween(DetailsMillis / 2)) { it / 6 },
        )
    }
}
