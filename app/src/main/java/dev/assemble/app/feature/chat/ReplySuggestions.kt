package dev.assemble.app.feature.chat

import androidx.annotation.StringRes
import dev.assemble.app.R
import dev.assemble.app.core.model.Character
import dev.assemble.app.core.model.Team

/** Parte variável de uma sugestão: um traço do personagem (vira rótulo traduzido) ou o nome dele. */
sealed interface SuggestionArg {
    data class Trait(val trait: Enum<*>) : SuggestionArg
    data class Name(val name: String) : SuggestionArg

    /** Texto pronto, já no idioma do usuário (sugestões geradas pelo backend). */
    data class Literal(val text: String) : SuggestionArg
}

/** Pergunta pronta acima do campo de mensagem. O texto final é montado na tela, no idioma do app. */
data class ReplySuggestion(@StringRes val text: Int, val arg: SuggestionArg? = null)

const val SuggestionsShown = 3

/** Sugestões do backend: texto pronto, até [SuggestionsShown]. */
fun literalSuggestions(texts: List<String>): List<ReplySuggestion> =
    texts.filter { it.isNotBlank() }.take(SuggestionsShown)
        .map { ReplySuggestion(R.string.chat_suggest_literal, SuggestionArg.Literal(it)) }

/**
 * Sugestões a partir dos traços do personagem (no backend, virão junto com a resposta da IA).
 * O cumprimento só aparece antes da sua primeira mensagem; depois o conjunto gira a cada envio.
 */
fun replySuggestions(character: Character, messagesSent: Int, count: Int = SuggestionsShown): List<ReplySuggestion> {
    val questions = buildList {
        character.powers.firstOrNull()?.let { add(ReplySuggestion(R.string.chat_suggest_powers, SuggestionArg.Trait(it))) }
        character.teams.firstOrNull { it != Team.Solo }?.let { add(ReplySuggestion(R.string.chat_suggest_team, SuggestionArg.Trait(it))) }
        add(ReplySuggestion(R.string.chat_suggest_mission))
        add(ReplySuggestion(R.string.chat_suggest_free_time))
        add(ReplySuggestion(R.string.chat_suggest_advice))
    }
    if (messagesSent == 0) {
        return listOf(ReplySuggestion(R.string.chat_suggest_hello, SuggestionArg.Name(character.name))) + questions.take(count - 1)
    }
    val start = ((messagesSent - 1) * count) % questions.size
    return List(minOf(count, questions.size)) { questions[(start + it) % questions.size] }
}
