package dev.assemble.app.core.media

import org.junit.Assert.assertEquals
import org.junit.Test

class CropMathTest {

    private val delta = 1e-4f

    @Test
    fun squarePhotoAtMinimumZoom_cannotMove() {
        val crop = CropMath.pan(PhotoCrop(), 0.3f, -0.2f, width = 100, height = 100)
        assertEquals(0f, crop.offsetX, delta)
        assertEquals(0f, crop.offsetY, delta)
    }

    @Test
    fun landscapePhoto_movesOnlySidewaysUntilTheEdge() {
        // 200×100 cobre o círculo com 2 diâmetros de largura: sobra meio diâmetro de cada lado.
        val crop = CropMath.pan(PhotoCrop(), 0.8f, 0.3f, width = 200, height = 100)
        assertEquals(0.5f, crop.offsetX, delta)
        assertEquals(0f, crop.offsetY, delta)
    }

    @Test
    fun rotatedLandscape_movesOnlyVertically() {
        val turned = PhotoCrop(quarterTurns = 1)
        val crop = CropMath.pan(turned, 0.8f, 0.3f, width = 200, height = 100)
        assertEquals(0f, crop.offsetX, delta)
        assertEquals(0.3f, crop.offsetY, delta)
    }

    @Test
    fun zoomStaysBetweenOneAndFour() {
        assertEquals(PhotoCrop.MAX_ZOOM, CropMath.zoomAround(PhotoCrop(), 10f, 0f, 0f, 100, 100).zoom, delta)
        assertEquals(PhotoCrop.MIN_ZOOM, CropMath.zoomAround(PhotoCrop(zoom = 2f), 0.5f, 0f, 0f, 100, 100).zoom, delta)
    }

    @Test
    fun zoomAround_keepsThePointUnderTheFingers() {
        // Dobrar o zoom com o foco a 0,25 do centro: esse ponto da foto continua sob os dedos.
        val crop = CropMath.zoomAround(PhotoCrop(), 2f, focusX = 0.25f, focusY = 0f, width = 100, height = 100)
        assertEquals(2f, crop.zoom, delta)
        assertEquals(-0.25f, crop.offsetX, delta)
    }

    @Test
    fun zoomingOut_pullsThePhotoBackOverTheCircle() {
        val zoomed = PhotoCrop(zoom = 3f, offsetX = 1f)
        val crop = CropMath.zoomAround(zoomed, 1f, 0f, 0f, 100, 100)
        assertEquals(0f, crop.offsetX, delta)
    }

    @Test
    fun rotate_turnsTheOffsetClockwiseAndWraps() {
        val crop = CropMath.rotate(PhotoCrop(offsetX = 0.4f), width = 200, height = 100)
        assertEquals(1, crop.quarterTurns)
        assertEquals(0f, crop.offsetX, delta)
        assertEquals(0.4f, crop.offsetY, delta)
        assertEquals(0, CropMath.rotate(PhotoCrop(quarterTurns = 3), 100, 100).quarterTurns)
    }

    @Test
    fun scale_coversTheCircleWithTheShortSide() {
        assertEquals(1f / 100, CropMath.scale(200, 100, PhotoCrop()), delta)
        assertEquals(2f / 100, CropMath.scale(200, 100, PhotoCrop(zoom = 2f)), delta)
    }
}
