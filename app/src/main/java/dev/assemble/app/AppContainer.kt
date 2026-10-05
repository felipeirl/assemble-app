package dev.assemble.app

import android.content.Context
import dev.assemble.app.core.data.CharacterRepository
import dev.assemble.app.core.data.ChatRepository
import dev.assemble.app.core.data.ConnectionRepository
import dev.assemble.app.core.data.UserRepository
import dev.assemble.app.core.data.local.MockSessionStore
import dev.assemble.app.core.data.local.SettingsDataStore
import dev.assemble.app.core.data.local.StoredSession
import dev.assemble.app.core.data.local.mockSessionDataStore
import dev.assemble.app.core.data.local.settingsDataStore
import dev.assemble.app.core.feedback.Feedback
import dev.assemble.app.core.feedback.FeedbackPlayer
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
import dev.assemble.app.core.model.SessionState
import dev.assemble.app.feature.achievements.AchievementTracker
import dev.assemble.app.core.firebase.FirebaseSettings
import dev.assemble.app.feature.character.CharacterDetailsSource
import dev.assemble.app.feature.character.LocalCharacterDetailsSource
import dev.assemble.app.feature.discover.AssembleService
import dev.assemble.app.feature.discover.CharacterReplyDelay
import dev.assemble.app.feature.discover.DeckSource
import dev.assemble.app.feature.discover.LocalDeckSource
import kotlin.time.Duration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.Instant

/**
 * Injeção de dependência manual. Com `BACKEND_URL` e as chaves do Firebase no `local.properties`,
 * o app usa o backend ([RemoteGraph]); sem elas, roda com os repositórios simulados.
 */
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

    private val settingsStore = SettingsDataStore(appContext.settingsDataStore)

    private val firebaseSettings = FirebaseSettings(
        apiKey = BuildConfig.FIREBASE_API_KEY,
        applicationId = BuildConfig.FIREBASE_APP_ID,
        projectId = BuildConfig.FIREBASE_PROJECT_ID,
    )

    /** true quando o app fala com o backend de verdade. */
    val usesBackend: Boolean = BuildConfig.BACKEND_URL.isNotBlank() && firebaseSettings.isComplete

    private val remote: RemoteGraph? = if (usesBackend) {
        RemoteGraph(
            appContext, BuildConfig.BACKEND_URL, firebaseSettings, BuildConfig.GOOGLE_WEB_CLIENT_ID,
            settingsStore, applicationScope, clock,
        )
    } else {
        null
    }

    val characterRepository: CharacterRepository = remote?.characters ?: FakeCharacterRepository(fakeNetwork, catalog)

    val userRepository: UserRepository = remote?.users ?: FakeUserRepository(
        network = fakeNetwork,
        initialProfile = MockSeed.initialProfile,
        initialPreferences = MockSeed.initialPreferences,
        settingsStore = settingsStore,
        sessionStore = MockSessionStore(
            dataStore = appContext.mockSessionDataStore,
            defaults = StoredSession(SessionState(), MockSeed.initialPreferences, MockSeed.initialProfile),
        ),
        scope = applicationScope,
    )

    val connectionRepository: ConnectionRepository = remote?.connections ?: FakeConnectionRepository(
        network = fakeNetwork,
        clock = clock,
        seed = ::seedConnections,
    )

    val chatRepository: ChatRepository = remote?.chat ?: FakeChatRepository(
        network = fakeNetwork,
        clock = clock,
        connections = connectionRepository,
        initialMessages = MockSeed.initialMessages(Instant.now(clock)),
    )

    /** Baralho do Discover: o backend decide; sem ele, o cálculo é local. */
    val deckSource: DeckSource = remote?.deck
        ?: LocalDeckSource(characterRepository, userRepository, connectionRepository, chatRepository)

    /** Prévia e perfil completo do personagem. */
    val characterDetails: CharacterDetailsSource = remote?.details
        ?: LocalCharacterDetailsSource(characterRepository, connectionRepository, userRepository)

    /** Som e vibração do app; lê as opções do Settings a cada toque. */
    val feedback: Feedback = FeedbackPlayer(appContext) { userRepository.settings.value }

    val assembleService = AssembleService(
        deckSource = deckSource,
        scope = applicationScope,
        // Com backend, a própria chamada já leva o tempo de gerar a fala de abertura.
        replyDelay = if (remote != null) Duration.ZERO else CharacterReplyDelay,
    )

    val achievementTracker = AchievementTracker(
        characterRepository = characterRepository,
        connectionRepository = connectionRepository,
        chatRepository = chatRepository,
        userRepository = userRepository,
        scope = applicationScope,
        remoteStats = remote?.let { graph -> { graph.totals() } },
    )

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
