package dev.assemble.app.core.firebase

import com.google.android.gms.tasks.Task
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Espera uma Task do Firebase sem a biblioteca kotlinx-coroutines-play-services. */
suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
    addOnCompleteListener { task ->
        val error = task.exception
        when {
            task.isCanceled -> continuation.cancel()
            error != null -> continuation.resumeWithException(error)
            else -> continuation.resume(task.result)
        }
    }
}

/** Erros do Firebase não são IOException: as telas tratam falha de rede só por IOException. */
suspend fun <T> Task<T>.awaitIo(): T = try {
    await()
} catch (error: IOException) {
    throw error
} catch (error: com.google.firebase.FirebaseException) {
    throw IOException(error.javaClass.simpleName, error)
}
