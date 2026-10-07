package dev.assemble.app.feature.discover

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.assemble.app.AppContainer
import dev.assemble.app.R
import dev.assemble.app.core.data.remote.Overture
import dev.assemble.app.core.designsystem.component.InAppToast
import dev.assemble.app.core.feedback.Cue
import java.io.IOException

private data class OvertureToast(val characterId: String, val name: String, val imageUrl: String?)

/**
 * "Fulano quer dar Assemble com você": aviso dentro do app quando um personagem tenta uma conexão
 * (o backend sorteia isso sem o usuário saber). Toca para abrir a pré-visualização, onde ele dá
 * Assemble (que sempre vira conexão) ou passa. Cada proposta avisa uma vez por abertura do app.
 * Respeita Settings → Notifications → New connections.
 */
@Composable
fun OvertureToastHost(
    container: AppContainer,
    onOpenCharacter: (characterId: String, name: String, imageUrl: String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val source = container.overtureSource ?: return
    var toast by remember { mutableStateOf<OvertureToast?>(null) }
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(source) {
        val announced = mutableSetOf<String>()
        source.pending.collect { overtures ->
            val next = newOverture(overtures, announced) ?: return@collect
            announced += next.characterId
            if (!container.userRepository.settings.value.notifyNewConnections) return@collect
            val character = try {
                container.characterRepository.getCharacter(next.characterId)
            } catch (_: IOException) {
                null // Sem nome não há aviso; a proposta continua pendente para a próxima abertura.
            } ?: return@collect
            toast = OvertureToast(character.id, character.name, character.imageUrl)
            container.feedback.play(Cue.Tick)
            visible = true
        }
    }

    toast?.let { current ->
        InAppToast(
            visible = visible,
            characterName = current.name,
            imageUrl = current.imageUrl,
            message = R.string.toast_overture,
            onClick = {
                visible = false
                onOpenCharacter(current.characterId, current.name, current.imageUrl)
            },
            onDismiss = { visible = false },
            modifier = modifier,
        )
    }
}

/** A proposta pendente mais antiga ainda não avisada, ou null. */
internal fun newOverture(pending: List<Overture>, announced: Set<String>): Overture? =
    pending.firstOrNull { it.characterId !in announced }
