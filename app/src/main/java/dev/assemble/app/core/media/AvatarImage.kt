package dev.assemble.app.core.media

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
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

    /** Lê a imagem de [uri], recorta o centro, reduz e codifica. Falha em [IOException]. */
    suspend fun encode(resolver: ContentResolver, uri: Uri): String = withContext(Dispatchers.IO) {
        try {
            val square = squareThumbnail(decode(resolver, uri))
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

    /** Foto guardada → bitmap; null se o texto estiver corrompido. */
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

    private fun sampleSize(shortSide: Int): Int = (shortSide / SIZE_PX).coerceAtLeast(1)

    private fun squareThumbnail(source: Bitmap): Bitmap {
        val side = minOf(source.width, source.height)
        val cropped = Bitmap.createBitmap(source, (source.width - side) / 2, (source.height - side) / 2, side, side)
        return Bitmap.createScaledBitmap(cropped, SIZE_PX, SIZE_PX, true)
    }

    private fun jpegBase64(bitmap: Bitmap, quality: Int): String {
        val output = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, output)
        return Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
    }
}
