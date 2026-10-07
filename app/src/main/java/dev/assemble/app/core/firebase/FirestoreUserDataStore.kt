package dev.assemble.app.core.firebase

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import dev.assemble.app.core.data.remote.DeleteField
import dev.assemble.app.core.data.remote.Document
import dev.assemble.app.core.data.remote.ServerTime
import dev.assemble.app.core.data.remote.UserDataStore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.io.IOException
import java.time.Instant
import java.util.Date

/** [UserDataStore] sobre o Firestore, com listeners em tempo real e cache offline do SDK. */
class FirestoreUserDataStore(private val firestore: FirebaseFirestore) : UserDataStore {

    override fun observeUser(uid: String): Flow<Map<String, Any?>?> = callbackFlow {
        val registration = firestore.document("users/$uid").addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(IOException("users/{uid}: ${error.code}", error))
                return@addSnapshotListener
            }
            trySend(snapshot?.normalizedData())
        }
        awaitClose { registration.remove() }
    }

    override suspend fun mergeUser(uid: String, fields: Map<String, Any?>) {
        firestore.document("users/$uid").set(fields.toFirestore(), SetOptions.merge()).awaitIo()
    }

    override fun observeMatches(uid: String): Flow<List<Document>> = callbackFlow {
        val registration = firestore.collection("users/$uid/matches").addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(IOException("matches: ${error.code}", error))
                return@addSnapshotListener
            }
            trySend(snapshot?.documents.orEmpty().map { Document(it.id, it.normalizedData().orEmpty()) })
        }
        awaitClose { registration.remove() }
    }

    override fun observeMessages(uid: String, connectionId: String): Flow<List<Document>> = callbackFlow {
        val registration = firestore.collection("users/$uid/matches/$connectionId/messages")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(IOException("messages: ${error.code}", error))
                    return@addSnapshotListener
                }
                trySend(snapshot?.documents.orEmpty().map { Document(it.id, it.normalizedData().orEmpty()) })
            }
        awaitClose { registration.remove() }
    }

    override fun observeOvertures(uid: String): Flow<List<Document>> = callbackFlow {
        val registration = firestore.collection("users/$uid/overtures").addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(IOException("overtures: ${error.code}", error))
                return@addSnapshotListener
            }
            trySend(snapshot?.documents.orEmpty().map { Document(it.id, it.normalizedData().orEmpty()) })
        }
        awaitClose { registration.remove() }
    }

    override fun observeDecision(uid: String, characterId: String): Flow<Map<String, Any?>?> = callbackFlow {
        val registration = firestore.document("users/$uid/decisions/$characterId").addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(IOException("decisions: ${error.code}", error))
                return@addSnapshotListener
            }
            trySend(snapshot?.normalizedData())
        }
        awaitClose { registration.remove() }
    }

    override suspend fun updateMatch(uid: String, connectionId: String, fields: Map<String, Any?>) {
        firestore.document("users/$uid/matches/$connectionId").update(fields.toFirestore()).awaitIo()
    }
}

/** Datas do Firestore viram [Instant]; horário do servidor ainda pendente vem estimado. */
private fun DocumentSnapshot.normalizedData(): Map<String, Any?>? =
    getData(DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)?.mapValues { (_, value) -> normalize(value) }

private fun normalize(value: Any?): Any? = when (value) {
    is Timestamp -> Instant.ofEpochSecond(value.seconds, value.nanoseconds.toLong())
    is Date -> value.toInstant()
    is Map<*, *> -> value.entries.associate { (key, inner) -> key.toString() to normalize(inner) }
    is List<*> -> value.map(::normalize)
    else -> value
}

private fun Map<String, Any?>.toFirestore(): Map<String, Any?> = mapValues { (_, value) -> toFirestoreValue(value) }

private fun toFirestoreValue(value: Any?): Any? = when (value) {
    ServerTime -> FieldValue.serverTimestamp()
    DeleteField -> FieldValue.delete()
    is Map<*, *> -> value.entries.associate { (key, inner) -> key.toString() to toFirestoreValue(inner) }
    else -> value
}
