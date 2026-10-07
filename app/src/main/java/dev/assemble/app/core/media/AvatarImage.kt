package dev.assemble.app.core.media

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ImageDecoder
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.util.Base64
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import dev.assemble.app.core.data.remote.AVATAR_PHOTO_MAX_CHARS
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException

/**
 * Foto do perfil: recorte quadrado de 256 px em JPEG, em Base64, para caber no documento do usuário
 * no Firestore (sem Firebase Storage). O perfil só é visto pelo dono.
 */
object AvatarImage {
    const val SIZE_PX = 256
    private const val FIRST_QUALITY = 85
    private const val MIN_QUALITY = 45
    private const val QUALITY_STEP = 10

    /** Lado menor que a foto precisa ter para o zoom máximo ainda render [SIZE_PX] pixels de verdade. */
    private val CROP_SOURCE_PX = (SIZE_PX * PhotoCrop.MAX_ZOOM).toInt()

    /** Lê a imagem de [uri] para a tela de ajuste, reduzida só até onde o zoom máximo continua nítido. */
    suspend fun decodeForCrop(resolver: ContentResolver, uri: Uri): Bitmap = withContext(Dispatchers.IO) {
        try {
            decode(resolver, uri)
        } catch (error: RuntimeException) {
            throw IOException("Imagem ilegível", error)
        }
    }

    /** Desenha o recorte ajustado em [SIZE_PX] e codifica. Falha em [IOException]. */
    suspend fun encode(source: Bitmap, crop: PhotoCrop): String = withContext(Dispatchers.Default) {
        try {
            val square = render(source, crop)
            var quality = FIRST_QUALITY
            var text = jpegBase64(square, quality)
            while (text.length > AVATAR_PHOTO_MAX_CHARS && quality > MIN_QUALITY) {
                quality -= QUALITY_STEP
                text = jpegBase64(square, quality)
            }
            if (text.length > AVATAR_PHOTO_MAX_CHARS) throw IOException("Imagem grande demais")
            text
        } catch (error: RuntimeException) {
            throw IOException("Imagem ilegível", error)
        }
    }

    /** A foto guardada pode ser a URL do Cloudinary (https) ou o JPEG em Base64 de antes. */
    fun isUrl(photo: String): Boolean = photo.startsWith("https://")

    /** Foto em Base64 → bitmap; null se o texto estiver corrompido. */
    fun decode(base64: String): ImageBitmap? = try {
        val bytes = Base64.decode(base64, Base64.NO_WRAP)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
    } catch (_: IllegalArgumentException) {
        null
    }

    private fun decode(resolver: ContentResolver, uri: Uri): Bitmap {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // O ImageDecoder já respeita a rotação gravada na foto.
            return ImageDecoder.decodeBitmap(ImageDecoder.createSource(resolver, uri)) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                decoder.setTargetSampleSize(sampleSize(minOf(info.size.width, info.size.height)))
            }
        }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize(minOf(bounds.outWidth, bounds.outHeight)) }
        return resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            ?: throw IOException("Imagem ilegível")
    }

    private fun sampleSize(shortSide: Int): Int = (shortSide / CROP_SOURCE_PX).coerceAtLeast(1)

    /** Mesma transformação da tela de ajuste: o círculo de 1 diâmetro vira o quadrado de [SIZE_PX]. */
    private fun render(source: Bitmap, crop: PhotoCrop): Bitmap {
        val output = Bitmap.createBitmap(SIZE_PX, SIZE_PX, Bitmap.Config.ARGB_8888)
        val k = CropMath.scale(source.width, source.height, crop) * SIZE_PX
        Canvas(output).apply {
            translate(SIZE_PX / 2f + crop.offsetX * SIZE_PX, SIZE_PX / 2f + crop.offsetY * SIZE_PX)
            rotate(QUARTER_DEGREES * crop.quarterTurns)
            scale(k, k)
            drawBitmap(source, -source.width / 2f, -source.height / 2f, Paint(Paint.FILTER_BITMAP_FLAG))
        }
        return output
    }

    private const val QUARTER_DEGREES = 90f

    private fun jpegBase64(bitmap: Bitmap, quality: Int): String {
        val output = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, output)
        return Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
    }
}
