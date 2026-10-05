package dev.assemble.app.feature.achievements

import dev.assemble.app.core.data.CharacterRepository
import dev.assemble.app.core.data.ChatRepository
import dev.assemble.app.core.data.ConnectionRepository
import dev.assemble.app.core.data.UserRepository
import dev.assemble.app.core.domain.Achievement
import dev.assemble.app.core.domain.AchievementProgress
import dev.assemble.app.core.domain.AchievementRules
import dev.assemble.app.core.domain.AchievementStats
import dev.assemble.app.core.model.Character
import dev.assemble.app.core.model.MessageAuthor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.IOException

/**
 * Acompanha as conquistas a partir dos dados de uso. [progress] é null até a primeira leitura completa;
 * [unlocks] avisa só o que foi desbloqueado depois dela (o que já valia ao abrir o app não gera aviso).
 */
class AchievementTracker(
    private val characterRepository: CharacterRepository,
    connectionRepository: ConnectionRepository,
    chatRepository: ChatRepository,
    userRepository: UserRepository,
    scope: CoroutineScope,
    /**
     * Com backend, o app não tem o catálogo completo: equipes distintas e personagens vistos vêm
     * de `/v2/me/stats`. Sem backend, null (contagem local).
     */
    private val remoteStats: (suspend () -> RemoteTotals)? = null,
) {
    /** Catálogo lido uma vez; se a leitura falhar, tenta de novo na próxima mudança de conexões. */
    private var catalog: Map<String, Character>? = null

    /** Equipes distintas e vistos do backend, relidos a cada mudança nas conexões. */
    private val totals = connectionRepository.observeConnections().map { connections ->
        val remote = remoteStats
        if (remote != null) {
            try {
                remote()
            } catch (_: IOException) {
                null
            }
        } else {
            val characters = loadCatalog() ?: return@map null
            RemoteTotals(AchievementRules.distinctTeams(connections.mapNotNull { characters[it.characterId] }), charactersSeen = null)
        }
    }

    val progress: StateFlow<List<AchievementProgress>?> = combine(
        connectionRepository.observeConnections(),
        chatRepository.allMessages,
        userRepository.seenCharacterIds,
        totals,
    ) { connections, messages, seen, totals ->
        AchievementRules.evaluate(
            AchievementStats(
                connections = connections.size,
                messagesSent = messages.count { it.author == MessageAuthor.User },
                charactersSeen = maxOf(seen.size, totals?.charactersSeen ?: 0),
                distinctTeams = totals?.distinctTeams,
            ),
        )
    }.stateIn(scope, SharingStarted.Eagerly, null)

    private val _unlocks = MutableSharedFlow<Achievement>(extraBufferCapacity = Achievement.entries.size)
    val unlocks: SharedFlow<Achievement> = _unlocks.asSharedFlow()

    init {
        scope.launch {
            var known: Set<Achievement>? = null
            progress.collect { list ->
                if (list == null) return@collect
                val unlocked = list.filter { it.unlocked }.map { it.achievement }.toSet()
                known?.let { previous -> (unlocked - previous).forEach { _unlocks.emit(it) } }
                known = unlocked
            }
        }
    }

    private suspend fun loadCatalog(): Map<String, Character>? {
        catalog?.let { return it }
        return try {
            characterRepository.getCharacters().associateBy { it.id }.also { catalog = it }
        } catch (_: IOException) {
            null
        }
    }
}

/** Contagens que, com backend, só o servidor conhece. null = desconhecido (a conquista mostra "—"). */
data class RemoteTotals(val distinctTeams: Int?, val charactersSeen: Int?)
