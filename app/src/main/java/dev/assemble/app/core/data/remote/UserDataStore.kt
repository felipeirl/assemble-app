package dev.assemble.app.core.data.remote

import kotlinx.coroutines.flow.Flow

/** Documento lido do Firestore, com datas já convertidas para [java.time.Instant]. */
data class Document(val id: String, val data: Map<String, Any?>)

/** Marca um campo para receber o horário do servidor ao gravar. */
object ServerTime

/** Marca um campo para ser removido do documento ao gravar. */
object DeleteField

/**
 * O que o app lê e grava direto no Firestore (contrato §2). Todo o resto passa pelo backend.
 * As regras do Firestore limitam a escrita aos campos do perfil, às preferências, ao `aiConsent`
 * e a `lastReadAt`/`profileUnlockSeenAt` das conexões.
 */
interface UserDataStore {
    /** Documento `users/{uid}`; null enquanto ele não existir. Falha de leitura sobe como IOException. */
    fun observeUser(uid: String): Flow<Map<String, Any?>?>

    /** Grava os campos em `users/{uid}` sem apagar os outros. */
    suspend fun mergeUser(uid: String, fields: Map<String, Any?>)

    /** Conexões (`users/{uid}/matches`). */
    fun observeMatches(uid: String): Flow<List<Document>>

    /** Mensagens de uma conexão, em ordem de criação. */
    fun observeMessages(uid: String, connectionId: String): Flow<List<Document>>

    suspend fun updateMatch(uid: String, connectionId: String, fields: Map<String, Any?>)
}
