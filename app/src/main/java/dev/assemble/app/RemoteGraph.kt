package dev.assemble.app

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dev.assemble.app.core.data.local.SettingsDataStore
import dev.assemble.app.core.data.remote.RemoteCharacterRepository
import dev.assemble.app.core.data.remote.RemoteChatRepository
import dev.assemble.app.core.data.remote.RemoteConnectionRepository
import dev.assemble.app.core.data.remote.RemoteUserRepository
import dev.assemble.app.core.firebase.FirebaseAuthGateway
import dev.assemble.app.core.firebase.FirebaseSettings
import dev.assemble.app.core.firebase.FirebaseSetup
import dev.assemble.app.core.firebase.FirestoreUserDataStore
import dev.assemble.app.core.network.AssembleApi
import dev.assemble.app.core.network.HttpAssembleApi
import dev.assemble.app.feature.achievements.RemoteTotals
import dev.assemble.app.feature.character.RemoteCharacterDetailsSource
import dev.assemble.app.feature.discover.RemoteDeckSource
import dev.assemble.app.i18n.backendLanguageTag
import dev.assemble.app.i18n.deviceTimeZoneId
import kotlinx.coroutines.CoroutineScope
import java.time.Clock

/**
 * Dependências do app ligado ao backend: Firebase (login e leitura em tempo real) e a API do
 * contrato V2 (decisões, mensagens, perfil do personagem e conta).
 */
class RemoteGraph(
    context: Context,
    backendUrl: String,
    firebaseSettings: FirebaseSettings,
    settingsStore: SettingsDataStore,
    scope: CoroutineScope,
    clock: Clock,
) {
    private val firebaseApp = FirebaseSetup.initialize(context, firebaseSettings)
    private val auth = FirebaseAuthGateway(FirebaseAuth.getInstance(firebaseApp))
    private val store = FirestoreUserDataStore(FirebaseFirestore.getInstance(firebaseApp))

    val api: AssembleApi = HttpAssembleApi(
        baseUrl = backendUrl,
        tokens = auth,
        languageTag = { backendLanguageTag() },
        timeZoneId = { deviceTimeZoneId() },
        onAccountDeactivated = { users.onAccountDeactivated() },
    )

    val users: RemoteUserRepository = RemoteUserRepository(auth, store, api, settingsStore, scope)
    val connections = RemoteConnectionRepository(auth.uid, store, api, scope)
    val characters = RemoteCharacterRepository(api, connections) { backendLanguageTag() }
    val chat = RemoteChatRepository(auth.uid, store, api, connections, scope, clock)
    val deck = RemoteDeckSource(api, characters, users)
    val details = RemoteCharacterDetailsSource(api, characters, connections)

    /** Para as conquistas: o que só o backend sabe contar. */
    suspend fun totals(): RemoteTotals = api.stats().let { RemoteTotals(it.distinctTeams, it.charactersSeen) }
}
