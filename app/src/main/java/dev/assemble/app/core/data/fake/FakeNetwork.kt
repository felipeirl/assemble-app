package dev.assemble.app.core.data.fake

import kotlinx.coroutines.delay
import java.io.IOException
import kotlin.random.Random

class FakeNetworkException : IOException("Simulated network failure (fake repository failure mode)")

/**
 * Rede simulada dos repositórios falsos: latência de 300–600 ms e um "modo falha"
 * que faz toda chamada lançar [FakeNetworkException], para testar os estados de erro.
 */
class FakeNetwork(private val random: Random = Random.Default) {
    @Volatile
    var failureMode: Boolean = false

    suspend fun call() {
        delay(random.nextLong(MIN_LATENCY_MILLIS, MAX_LATENCY_MILLIS + 1))
        if (failureMode) throw FakeNetworkException()
    }

    companion object {
        const val MIN_LATENCY_MILLIS = 300L
        const val MAX_LATENCY_MILLIS = 600L
    }
}
