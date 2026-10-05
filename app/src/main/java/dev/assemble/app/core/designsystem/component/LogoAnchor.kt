package dev.assemble.app.core.designsystem.component

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned

/**
 * Onde o logo está na tela de destino (top bar do Discover ou Login), para a splash encaixar nele.
 * Enquanto [covered], o logo de destino fica invisível: o da splash pousa no lugar dele, sem duplicar.
 */
@Stable
class LogoAnchor {
    var bounds by mutableStateOf<Rect?>(null)
        private set
    var covered by mutableStateOf(false)

    internal fun update(newBounds: Rect) {
        bounds = newBounds
    }
}

/** null fora do app (previews, catálogo): o logo se comporta como imagem comum. */
val LocalLogoAnchor = staticCompositionLocalOf<LogoAnchor?> { null }

/** Marca este logo como destino da splash. Os limites são os do elemento, em coordenadas da raiz. */
fun Modifier.logoAnchor(anchor: LogoAnchor?): Modifier =
    if (anchor == null) {
        this
    } else {
        this
            .onGloballyPositioned { anchor.update(it.boundsInRoot()) }
            .graphicsLayer { alpha = if (anchor.covered) 0f else 1f }
    }
