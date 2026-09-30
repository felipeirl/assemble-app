package dev.assemble.app

import android.content.Context
import dev.assemble.app.core.data.CharacterRepository
import dev.assemble.app.core.data.ChatRepository
import dev.assemble.app.core.data.ConnectionRepository
import dev.assemble.app.core.data.UserRepository
import dev.assemble.app.core.data.fake.FakeCharacterRepository
import dev.assemble.app.core.data.fake.FakeChatRepository
import dev.assemble.app.core.data.fake.FakeConnectionRepository
import dev.assemble.app.core.data.fake.FakeNetwork
import dev.assemble.app.core.data.fake.FakeUserRepository
import dev.assemble.app.core.data.fake.MockCatalog
import dev.assemble.app.core.data.mock.CHARACTERS_ASSET_PATH
import dev.assemble.app.core.data.mock.MockSeed
import dev.assemble.app.core.domain.CompatibilityCalculator
import dev.assemble.app.core.model.AppSettings
import dev.assemble.app.core.model.Connection
import dev.assemble.app.feature.discover.AssembleService
import java.time.Clock
import java.time.Instant

/** Injeção de dependência manual. Hoje só repositórios falsos; trocar aqui pelos reais depois. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val clock: Clock = Clock.systemDefaultZone()

    /** Liga/desliga o modo falha de todos os repositórios falsos. */
    val fakeNetwork = FakeNetwork()

    private val catalog = MockCatalog {
        appContext.assets.open(CHARACTERS_ASSET_PATH).bufferedReader().use { it.readText() }
    }

    val characterRepository: CharacterRepository = FakeCharacterRepository(fakeNetwork, catalog)

    val userRepository: UserRepository = FakeUserRepository(
        network = fakeNetwork,
        initialProfile = MockSeed.initialProfile,
        initialPreferences = MockSeed.initialPreferences,
    )

    val connectionRepository: ConnectionRepository = FakeConnectionRepository(
        network = fakeNetwork,
        clock = clock,
        seed = ::seedConnections,
    )

    val chatRepository: ChatRepository = FakeChatRepository(
        network = fakeNetwork,
        clock = clock,
        connections = connectionRepository,
        initialMessages = MockSeed.initialMessages(Instant.now(clock)),
    )

    val assembleService = AssembleService(userRepository, connectionRepository, chatRepository)

    /** Conexões iniciais com o score calculado pelas preferências iniciais do mock. */
    private suspend fun seedConnections(): List<Connection> {
        val characters = catalog.characters().associateBy { it.id }
        val now = Instant.now(clock)
        return MockSeed.initialConnections.mapNotNull { seed ->
            val character = characters[seed.characterId] ?: return@mapNotNull null
            Connection(
                id = seed.id,
                characterId = seed.characterId,
                score = CompatibilityCalculator.score(MockSeed.initialPreferences, character),
                threshold = AppSettings.DEFAULT_MINIMUM_COMPATIBILITY,
                createdAt = now - seed.age,
                profileUnlockSeen = true,
            )
        }
    }
}
