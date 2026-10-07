package dev.assemble.app.core.data.remote

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.emptyPreferences
import dev.assemble.app.core.data.local.SettingsDataStore
import dev.assemble.app.core.network.ApiAcceptedMessage
import dev.assemble.app.core.network.ApiAssembleAccepted
import dev.assemble.app.core.network.ApiCharacterView
import dev.assemble.app.core.network.ApiDeactivation
import dev.assemble.app.core.network.ApiDeck
import dev.assemble.app.core.network.ApiDeckCard
import dev.assemble.app.core.network.ApiErrorCode
import dev.assemble.app.core.network.ApiException
import dev.assemble.app.core.network.ApiPhotoSignature
import dev.assemble.app.core.network.ApiRegenerationAccepted
import dev.assemble.app.core.network.ApiUserStats
import dev.assemble.app.core.network.AssembleApi
import dev.assemble.app.core.network.DecisionChoice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import java.io.IOException
import java.time.Instant
import androidx.datastore.preferences.core.Preferences as StoredPreferences

// Dublês em memória para testar os repositórios remotos sem Firebase nem rede.

internal val TestNow: Instant = Instant.parse("2026-10-04T12:00:00Z")

/** Roda [block] com um escopo que executa na hora e é cancelado no fim. */
internal fun runRemoteTest(block: suspend (scope: CoroutineScope) -> Unit) = runBlocking {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
    try {
        block(scope)
    } finally {
        scope.cancel()
    }
}

internal class FakeAssembleApi : AssembleApi {
    data class Decision(val characterId: String, val choice: DecisionChoice, val key: String)

    val decisions = mutableListOf<Decision>()
    val sentKeys = mutableListOf<String>()
    var hideChatsCalls = 0
    var deactivateCalls = 0
    var reactivateCalls = 0

    var onDeck: () -> ApiDeck = { throw IOException("deck não configurado") }
    var onDecide: (Decision) -> ApiAssembleAccepted? = { null }
    var onUndo: () -> ApiDeckCard = { throw ApiException(ApiErrorCode.NOTHING_TO_UNDO, 409) }
    var onCharacter: (String) -> ApiCharacterView = { throw ApiException(ApiErrorCode.NOT_FOUND, 404) }
    var onSend: (connectionId: String, text: String, key: String) -> ApiAcceptedMessage = { _, _, _ -> throw IOException("sem rede") }
    val regenerateCalls = mutableListOf<String>()
    val rewinds = mutableListOf<Pair<String, String>>()
    var onRegenerate: (String) -> ApiRegenerationAccepted = { throw IOException("sem rede") }
    var onRewind: (String, String) -> Unit = { _, _ -> }
    var onStats: () -> ApiUserStats = { ApiUserStats(0, 0, 0, 0) }
    var onDeactivate: () -> ApiDeactivation = { ApiDeactivation("2026-11-03T12:00:00Z") }

    override suspend fun deck(): ApiDeck = onDeck()

    override suspend fun decide(characterId: String, choice: DecisionChoice, idempotencyKey: String): ApiAssembleAccepted? {
        val decision = Decision(characterId, choice, idempotencyKey)
        decisions += decision
        return onDecide(decision)
    }

    override suspend fun undo(): ApiDeckCard = onUndo()

    val signals = mutableListOf<Pair<String, Boolean>>()
    var onReactionCards: () -> List<ApiDeckCard> = { emptyList() }

    override suspend fun reactionCards(): List<ApiDeckCard> = onReactionCards()

    override suspend fun tasteSignal(characterId: String, liked: Boolean) {
        signals += characterId to liked
    }

    override suspend fun character(characterId: String): ApiCharacterView = onCharacter(characterId)

    override suspend fun sendMessage(connectionId: String, text: String, idempotencyKey: String): ApiAcceptedMessage {
        sentKeys += idempotencyKey
        return onSend(connectionId, text, idempotencyKey)
    }

    override suspend fun regenerate(connectionId: String): ApiRegenerationAccepted {
        regenerateCalls += connectionId
        return onRegenerate(connectionId)
    }

    override suspend fun rewind(connectionId: String, messageId: String) {
        rewinds += connectionId to messageId
        onRewind(connectionId, messageId)
    }

    override suspend fun stats(): ApiUserStats = onStats()

    var emailVerificationCalls = 0
    var onEmailVerification: () -> Unit = {}

    override suspend fun sendEmailVerification() {
        emailVerificationCalls++
        onEmailVerification()
    }

    var photoSignatureCalls = 0
    var onPhotoSignature: () -> ApiPhotoSignature = { throw ApiException(ApiErrorCode.PROVIDER_UNAVAILABLE, 503) }

    override suspend fun photoSignature(): ApiPhotoSignature {
        photoSignatureCalls++
        return onPhotoSignature()
    }

    override suspend fun hideChats() {
        hideChatsCalls++
    }

    override suspend fun deactivateAccount(): ApiDeactivation {
        deactivateCalls++
        return onDeactivate()
    }

    override suspend fun reactivateAccount() {
        reactivateCalls++
    }
}

/** Firestore em memória: [ServerTime] vira [TestNow] ao gravar, como o servidor faria. */
internal class InMemoryUserDataStore : UserDataStore {
    val user = MutableStateFlow<Map<String, Any?>?>(null)
    val matches = MutableStateFlow<List<Document>>(emptyList())
    val messages = MutableStateFlow<Map<String, List<Document>>>(emptyMap())
    val decisions = MutableStateFlow<Map<String, Map<String, Any?>>>(emptyMap())
    val merges = mutableListOf<Map<String, Any?>>()
    val matchUpdates = mutableListOf<Pair<String, Map<String, Any?>>>()

    override fun observeUser(uid: String): Flow<Map<String, Any?>?> = user

    override suspend fun mergeUser(uid: String, fields: Map<String, Any?>) {
        merges += fields
        val written = fields.filterValues { it != DeleteField }.mapValues { (_, value) -> if (value == ServerTime) TestNow else value }
        val removed = fields.filterValues { it == DeleteField }.keys
        user.value = (user.value.orEmpty() - removed) + written
    }

    override fun observeMatches(uid: String): Flow<List<Document>> = matches

    override fun observeMessages(uid: String, connectionId: String): Flow<List<Document>> =
        messages.map { it[connectionId].orEmpty() }

    override fun observeDecision(uid: String, characterId: String): Flow<Map<String, Any?>?> =
        decisions.map { it[characterId] }

    override suspend fun updateMatch(uid: String, connectionId: String, fields: Map<String, Any?>) {
        matchUpdates += connectionId to fields
    }
}

internal class FakeAuthGateway(signedInUid: String? = null, override val email: String? = "tony@stark.com") : AuthGateway {
    override val uid = MutableStateFlow(signedInUid)
    override val ready = MutableStateFlow(true)

    /** Próximo login falha com este motivo. */
    var failure: AuthFailure? = null

    override suspend fun idToken(forceRefresh: Boolean): String = "token"

    override suspend fun signIn(email: String, password: String) = enter()

    override suspend fun createAccount(email: String, password: String) = enter()

    var lastGoogleToken: String? = null
    var resetRequests = mutableListOf<String>()

    override suspend fun signInWithGoogle(idToken: String) {
        lastGoogleToken = idToken
        enter()
    }

    override suspend fun sendPasswordReset(email: String) {
        resetRequests += email
    }

    override suspend fun signOut() {
        uid.value = null
    }

    override val emailVerificationPending = MutableStateFlow(false)
    var verifiedAfterReload = true
    var firebaseEmails = 0
    var reloads = 0

    override suspend fun reloadEmailVerified(): Boolean {
        reloads++
        emailVerificationPending.value = !verifiedAfterReload
        return verifiedAfterReload
    }

    override suspend fun sendFirebaseVerificationEmail() {
        firebaseEmails++
    }

    private fun enter() {
        failure?.let { throw AuthException(it) }
        uid.value = SIGNED_IN_UID
    }

    companion object {
        const val SIGNED_IN_UID = "uid-1"
    }
}

private class InMemoryPreferencesStore : DataStore<StoredPreferences> {
    private val state = MutableStateFlow(emptyPreferences())
    override val data: Flow<StoredPreferences> = state

    override suspend fun updateData(transform: suspend (t: StoredPreferences) -> StoredPreferences): StoredPreferences =
        transform(state.value).also { state.value = it }
}

internal fun inMemorySettings(): SettingsDataStore = SettingsDataStore(InMemoryPreferencesStore())

/** Documento de conexão como o backend grava no match. */
internal fun matchDoc(
    characterId: String,
    name: String = characterId,
    createdAt: Instant = TestNow,
    extra: Map<String, Any?> = emptyMap(),
): Document = Document(
    characterId,
    mapOf("characterName" to name, "createdAt" to createdAt, "score" to 80) + extra,
)
