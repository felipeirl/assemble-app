package dev.assemble.app.core.network

import coil3.ImageLoader
import coil3.Uri
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import coil3.SingletonImageLoader
import coil3.decode.DataSource
import coil3.decode.ImageSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.Buffer
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

private const val CONNECT_TIMEOUT_MILLIS = 15_000
private const val READ_TIMEOUT_MILLIS = 30_000
private const val HTTP_OK = 200
private const val MAX_IMAGE_BYTES = 10 * 1024 * 1024

// Algumas fontes de imagem (Comic Vine) recusam requisições sem User-Agent.
private const val IMAGE_USER_AGENT = "AssembleApp/1.0 (Android)"

/**
 * O Coil 3 não baixa URLs sozinho: falta o módulo de rede. Este buscador usa a JDK, sem
 * dependência nova, e o cache em memória do Coil evita baixar a mesma arte duas vezes.
 */
private class HttpUrlFetcher(private val url: String, private val options: Options) : Fetcher {
    override suspend fun fetch(): FetchResult = withContext(Dispatchers.IO) {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = CONNECT_TIMEOUT_MILLIS
            readTimeout = READ_TIMEOUT_MILLIS
            setRequestProperty("User-Agent", IMAGE_USER_AGENT)
            setRequestProperty("Accept", "image/*")
        }
        try {
            if (connection.responseCode != HTTP_OK) throw IOException("Imagem ${connection.responseCode}: $url")
            val bytes = connection.inputStream.use { it.readBytes() }
            if (bytes.size > MAX_IMAGE_BYTES) throw IOException("Imagem grande demais: $url")
            SourceFetchResult(
                source = ImageSource(Buffer().write(bytes), options.fileSystem),
                mimeType = connection.contentType?.substringBefore(';'),
                dataSource = DataSource.NETWORK,
            )
        } finally {
            connection.disconnect()
        }
    }

    class Factory : Fetcher.Factory<Uri> {
        override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? =
            if (data.scheme == "http" || data.scheme == "https") HttpUrlFetcher(data.toString(), options) else null
    }
}

object ImageLoading {
    /** Registra o carregador de imagens do app; chamado uma vez na [android.app.Application]. */
    @JvmStatic
    fun install() {
        SingletonImageLoader.setSafe { platformContext ->
            ImageLoader.Builder(platformContext)
                .components { add(HttpUrlFetcher.Factory()) }
                .build()
        }
    }
}
