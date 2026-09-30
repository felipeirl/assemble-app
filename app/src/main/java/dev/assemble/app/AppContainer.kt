package dev.assemble.app

import android.content.Context
import dev.assemble.app.core.data.CharacterRepository
import dev.assemble.app.core.data.ChatRepository
import dev.assemble.app.core.data.ConnectionRepository
import dev.assemble.app.core.data.UserRepository
import dev.assemble.app.core.data.local.SettingsDataStore
import dev.assemble.app.core.data.local.settingsDataStore
import dev.assemble.app.core.data.fake.FakeCharacterRepository
import dev.assemble.app.core.data.fake.FakeChatRepository
import dev.assemble.app.core.data.fake.FakeConnectionRepository
import dev.assemble.app.core.data.fake.FakeNetwork
import dev.assemble.app.core.data.fake.FakeUserRepository
import dev.assemble.app.core.data.fake.MockCatalog
import dev.assemble.app.core.data.mock.CHARACTERS_ASSET_PATH
import dev.assemble.app.core.data.mock.MockSeed
import dev.assemble.app.core.domain.CompatibilityCalculator
import dev.assemble.app.core.model.Connection
import dev.assemble.app.feature.discover.AssembleService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.Instant

/** Injeção de dependência manual. Hoje só repositórios falsos; trocar aqui pelos reais depois. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val clock: Clock = Clock.systemDefaultZone()

    /** Trabalho que deve sobreviver à tela (ex.: resposta da IA depois que o usuário sai da conversa). */
    val applicationScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /** Liga/desliga o modo falha de todos os repositórios falsos. */
    val fakeNetwork = FakeNetwork()

    private val catalog = MockCatalog {
        appContext.assets.open(CHARACTERS_ASSET_PATH).bufferedReader().use { it.readText() }
    }

    val characterRepository: CharacterRepository = FakeCharacterRepository(fakeNetwork, catalog)

    private val settingsStore = SettingsDataStore(appContext.settingsDataStore)

    val userRepository: UserRepository = FakeUserRepository(
        network = fakeNetwork,
        initialProfile = MockSeed.initialProfile,
        initialPreferences = MockSeed.initialPreferences,
        settingsStore = settingsStore,
        scope = applicationScope,
    )

    /** false até o DataStore entregar o primeiro valor: o splash segura a tela (evita piscar o tema). */
    val settingsLoaded: StateFlow<Boolean> =
        settingsStore.settings.map { true }.stateIn(applicationScope, SharingStarted.Eagerly, false)

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
                threshold = CompatibilityCalculator.MATCH_THRESHOLD,
                createdAt = now - seed.age,
                profileUnlockSeen = true,
            )
        }
    }
}
