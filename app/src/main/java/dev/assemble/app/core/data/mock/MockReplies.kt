package dev.assemble.app.core.data.mock

/**
 * Falas mock, ficcionais, no tom de cada personagem. Substituídas pela IA real depois.
 * Não são falas canônicas.
 */
object MockReplies {
    private const val FALLBACK_OPENER = "Hi. Glad you assembled. What's on your mind?"
    private const val FALLBACK_REPLY = "Tell me more."

    private val openers = mapOf(
        "spider-man" to "Hey! Friendly neighborhood check-in. Got a minute between patrols?",
        "iron-man" to "You made it past the security system. Impressive. What do you want to build?",
        "storm" to "The wind carried your name here. Tell me what you're looking for.",
        "captain-america" to "Good to meet you. I'm always glad to talk with someone who shows up.",
        "black-panther" to "Welcome. Speak freely; you are among allies here.",
        "jean-grey" to "Don't worry, I'm not reading your mind. You'd have to invite me in.",
        "rocket" to "Yeah, yeah, I'm a raccoon. Got a problem with that? No? Good. What's up?",
    )

    private val replies = mapOf(
        "spider-man" to listOf(
            "Ha! Okay, but first: do my jokes land? Be honest. Actually, don't.",
            "I'd answer faster, but I'm upside down and the blood is going to my head.",
            "Science tip: web fluid and midterms don't mix. Ask me how I know.",
            "Gotta swing. Somebody's cat is stuck on a very tall building.",
        ),
        "iron-man" to listOf(
            "Short answer: yes. Long answer: yes, with more thrusters.",
            "I ran the numbers. You're more interesting than my inbox.",
            "If it works on the first try, you weren't ambitious enough.",
            "Hold on, the suit wants a firmware update. Again.",
        ),
        "storm" to listOf(
            "Patience is a kind of weather too. It passes, and it returns.",
            "Leading a team means listening before you speak. So I'm listening.",
            "Some days call for calm skies. Others for thunder. Which is today?",
        ),
        "captain-america" to listOf(
            "Doing the right thing rarely feels easy. That's usually how you know.",
            "I'm still catching up on a few decades of music. Recommendations welcome.",
            "A good team isn't about the best players. It's about who has your back.",
        ),
        "black-panther" to listOf(
            "A king plans for the next generation, not the next headline.",
            "Strength is knowing when to hold back.",
            "Technology should serve people, not the other way around.",
        ),
        "jean-grey" to listOf(
            "The mind is louder than people think. Learning to listen gently took years.",
            "Some of my best friends started as students. Some as rivals.",
            "Ask me anything. I'll answer out loud, promise.",
        ),
        "rocket" to listOf(
            "Give me a toolbox and ten minutes. I'll make it explode. On purpose.",
            "Plans are great. Improvising is better.",
            "Don't touch anything shiny. Especially if it's humming.",
        ),
    )

    fun openerFor(characterId: String): String = openers[characterId] ?: FALLBACK_OPENER

    /** Resposta de índice [turn], em ciclo. */
    fun replyFor(characterId: String, turn: Int): String {
        val options = replies[characterId].orEmpty()
        if (options.isEmpty()) return FALLBACK_REPLY
        return options[Math.floorMod(turn, options.size)]
    }
}
