package dev.assemble.app.core.data.fake

import dev.assemble.app.core.data.mock.parseMockCharacters
import dev.assemble.app.core.model.Character
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Lê o JSON mock uma vez (fora da main thread) e guarda o resultado. */
class MockCatalog(private val readJson: () -> String) {
    private val mutex = Mutex()
    private var cached: List<Character>? = null

    suspend fun characters(): List<Character> = mutex.withLock {
        cached ?: withContext(Dispatchers.IO) { parseMockCharacters(readJson()) }.also { cached = it }
    }
}
