package dev.assemble.app.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.ui.LocalNavAnimatedContentScope

private const val DiagonalMillis = 250
private const val NoDiagonalRevealKey = "assemble.noDiagonalReveal"

/** Metadado da tela que entra por elemento compartilhado em vez do corte diagonal. */
val NoDiagonalReveal: Map<String, Any> = mapOf(NoDiagonalRevealKey to true)

/**
 * Transição de tela em corte diagonal: a tela que entra aparece a partir do canto superior
 * esquerdo; a que sai some pela região complementar da mesma diagonal.
 * Quando [fadeInstead] devolve true (troca de aba), as duas telas só se cruzam em fade.
 * Com as animações do sistema desligadas, a troca é imediata.
 */
@Composable
fun <T : Any> rememberDiagonalRevealDecorator(
    enabled: Boolean,
    fadeInstead: () -> Boolean = { false },
): NavEntryDecorator<T> =
    remember(enabled) {
        NavEntryDecorator { entry ->
            val animated = enabled && entry.metadata[NoDiagonalRevealKey] != true
            if (animated) DiagonalReveal(fade = fadeInstead()) { entry.Content() } else entry.Content()
        }
    }

/** O NavDisplay não anima nada sozinho: mantém a tela antiga até o corte diagonal terminar. */
fun <S> AnimatedContentTransitionScope<S>.diagonalTransition(enabled: Boolean): ContentTransform =
    EnterTransition.None togetherWith if (enabled) ExitTransition.KeepUntilTransitionsFinished else ExitTransition.None

@Composable
private fun DiagonalReveal(fade: Boolean, content: @Composable () -> Unit) {
    val transition = LocalNavAnimatedContentScope.current.transition
    // 0 → 1 ao entrar; 1 → 0 ao sair.
    val progress by transition.animateFloat(transitionSpec = { tween(DiagonalMillis) }, label = "diagonalReveal") { state ->
        if (state == EnterExitState.Visible) 1f else 0f
    }
    val exiting = transition.targetState == EnterExitState.PostExit
    Box(
        Modifier.graphicsLayer {
            if (fade) {
                alpha = progress
            } else {
                clip = true
                shape = if (exiting) beyondDiagonal(1f - progress) else beforeDiagonal(progress)
            }
        },
    ) {
        content()
    }
}

/** Região até a diagonal x + y = t·(w + h). */
private fun beforeDiagonal(t: Float) = GenericShape { size, _ ->
    val reach = size.width + size.height
    val d = t * reach
    moveTo(0f, 0f)
    lineTo(d, 0f)
    lineTo(0f, d)
    close()
}

/** Região além da diagonal x + y = t·(w + h) (complemento de [beforeDiagonal]). */
private fun beyondDiagonal(t: Float) = GenericShape { size, _ ->
    val reach = size.width + size.height
    val d = t * reach
    moveTo(d, 0f)
    lineTo(reach, 0f)
    lineTo(reach, reach)
    lineTo(0f, reach)
    lineTo(0f, d)
    close()
}
