package dev.assemble.app.core.media

import kotlin.math.max
import kotlin.math.min

/**
 * Ajuste da foto de perfil, medido em diâmetros do recorte (1 = o círculo inteiro), para valer igual na tela
 * de ajuste e no bitmap final. [offsetX]/[offsetY]: onde fica o centro da foto em relação ao centro do círculo.
 */
data class PhotoCrop(
    val zoom: Float = MIN_ZOOM,
    val quarterTurns: Int = 0,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
) {
    companion object {
        const val MIN_ZOOM = 1f
        const val MAX_ZOOM = 4f
    }
}

/** Regras do ajuste: a foto sempre cobre o círculo, o zoom fica entre [PhotoCrop.MIN_ZOOM] e [PhotoCrop.MAX_ZOOM]. */
object CropMath {

    /** Pixels da foto → diâmetros. No zoom mínimo, o lado menor da foto tem exatamente um diâmetro. */
    fun scale(width: Int, height: Int, crop: PhotoCrop): Float = crop.zoom / min(width, height)

    /** Zoom no intervalo e deslocamento limitado para não sobrar borda vazia dentro do círculo. */
    fun clamp(crop: PhotoCrop, width: Int, height: Int): PhotoCrop {
        val zoom = crop.zoom.coerceIn(PhotoCrop.MIN_ZOOM, PhotoCrop.MAX_ZOOM)
        val k = zoom / min(width, height)
        val turned = crop.quarterTurns % 2 == 1
        val shownWidth = (if (turned) height else width) * k
        val shownHeight = (if (turned) width else height) * k
        val maxX = max(0f, (shownWidth - 1f) / 2)
        val maxY = max(0f, (shownHeight - 1f) / 2)
        return crop.copy(
            zoom = zoom,
            offsetX = crop.offsetX.coerceIn(-maxX, maxX),
            offsetY = crop.offsetY.coerceIn(-maxY, maxY),
        )
    }

    /** Arrastar: [dx]/[dy] em diâmetros. */
    fun pan(crop: PhotoCrop, dx: Float, dy: Float, width: Int, height: Int): PhotoCrop =
        clamp(crop.copy(offsetX = crop.offsetX + dx, offsetY = crop.offsetY + dy), width, height)

    /** Zoom mantendo parado o ponto ([focusX], [focusY], em diâmetros a partir do centro) que está sob os dedos. */
    fun zoomAround(crop: PhotoCrop, zoom: Float, focusX: Float, focusY: Float, width: Int, height: Int): PhotoCrop {
        val next = zoom.coerceIn(PhotoCrop.MIN_ZOOM, PhotoCrop.MAX_ZOOM)
        val ratio = next / crop.zoom
        return clamp(
            crop.copy(
                zoom = next,
                offsetX = focusX - (focusX - crop.offsetX) * ratio,
                offsetY = focusY - (focusY - crop.offsetY) * ratio,
            ),
            width,
            height,
        )
    }

    /** Gira 90° no sentido horário; o deslocamento gira junto, para o mesmo pedaço da foto continuar no círculo. */
    fun rotate(crop: PhotoCrop, width: Int, height: Int): PhotoCrop = clamp(
        crop.copy(quarterTurns = (crop.quarterTurns + 1) % QUARTERS, offsetX = -crop.offsetY, offsetY = crop.offsetX),
        width,
        height,
    )

    private const val QUARTERS = 4
}
