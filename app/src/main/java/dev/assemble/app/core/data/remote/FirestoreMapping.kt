package dev.assemble.app.core.data.remote

import dev.assemble.app.core.model.AvatarFrame
import dev.assemble.app.core.model.Connection
import dev.assemble.app.core.model.Message
import dev.assemble.app.core.model.MessageAuthor
import dev.assemble.app.core.model.MessageStatus
import dev.assemble.app.core.model.Origin
import dev.assemble.app.core.model.PowerFamily
import dev.assemble.app.core.model.Preferences
import dev.assemble.app.core.model.ProfileAccent
import dev.assemble.app.core.model.ProfileCover
import dev.assemble.app.core.model.ProfilePrompt
import dev.assemble.app.core.model.ProfileStyle
import dev.assemble.app.core.model.ProfileTitle
import dev.assemble.app.core.model.Style
import dev.assemble.app.core.model.Team
import dev.assemble.app.core.model.UserProfile
import dev.assemble.app.core.network.ApiStatus
import java.time.Instant

// Conversão entre os documentos do Firestore (contrato §2–§3) e os modelos do app. Funções puras:
// campo ausente ou com tipo inesperado vira o padrão do modelo, nunca um valor inventado.

/** Versão do texto de consentimento de IA aceito no onboarding. */
const val AI_CONSENT_VERSION = "v1"

const val USER_STATUS_DEACTIVATED = "deactivated"

/** Limite de `lookingFor` ("o que você procura numa conversa?"), igual às regras do Firestore. */
const val LOOKING_FOR_MAX = 140
private const val AUTHOR_USER = "USER"
private const val AVATAR_PRESET_MAX = 5
private const val DISPLAY_NAME_MAX = 60
private const val BIO_MAX = 500

/** Tamanho máximo da foto em Base64; as regras do Firestore usam o mesmo limite. */
const val AVATAR_PHOTO_MAX_CHARS = 120_000

/** `users/{uid}` como o app usa. */
data class UserDocument(
    val profile: UserProfile,
    val preferences: Preferences,
    val onboardingCompleted: Boolean,
    val deactivated: Boolean,
)

/** `users/{uid}/matches/{characterId}` (contrato §3, Connection). */
data class MatchDocument(
    val connection: Connection,
    val characterName: String,
    val characterNamePtBR: String?,
    val imageUrl: String?,
    val lastMessagePreview: String?,
    val lastMessageAt: Instant?,
    val lastReadAt: Instant?,
    val suggestions: List<String>,
    val userMessageCount: Int,
    val hidden: Boolean,
    /** Traços em comum que explicam a conexão (`whyYouMatch`), para o pop-up de match. */
    val reasons: List<String> = emptyList(),
)

fun userDocument(data: Map<String, Any?>?, defaultName: String): UserDocument {
    val map = data.orEmpty()
    return UserDocument(
        profile = UserProfile(
            name = (map["displayName"] as? String)?.takeIf { it.isNotBlank() } ?: defaultName,
            bio = map["bio"] as? String ?: "",
            avatarPreset = (map["avatarPreset"] as? Number)?.toInt()?.coerceIn(0, AVATAR_PRESET_MAX) ?: 0,
            style = profileStyle(map["profileStyle"] as? Map<*, *>),
            photo = (map["avatarPhoto"] as? String)?.takeIf { it.isNotBlank() && it.length <= AVATAR_PHOTO_MAX_CHARS },
        ),
        preferences = preferences(map["preferences"] as? Map<*, *>),
        onboardingCompleted = map["onboardingCompletedAt"] != null,
        deactivated = map["status"] == USER_STATUS_DEACTIVATED,
    )
}

fun matchDocument(document: Document): MatchDocument? {
    val data = document.data
    val name = data["characterName"] as? String ?: return null
    val createdAt = data["createdAt"] as? Instant ?: return null
    return MatchDocument(
        connection = Connection(
            id = document.id,
            characterId = document.id,
            score = (data["score"] as? Number)?.toInt() ?: 0,
            // O limiar é decisão do backend; o app só mostra o score.
            threshold = 0,
            createdAt = createdAt,
            profileUnlockSeen = data["profileUnlockSeenAt"] != null,
        ),
        characterName = name,
        characterNamePtBR = (data["characterNamePtBR"] as? String)?.takeIf { it.isNotBlank() },
        imageUrl = (data["imageUrl"] as? String)?.takeIf { it.isNotBlank() },
        lastMessagePreview = data["lastMessagePreview"] as? String,
        lastMessageAt = data["lastMessageAt"] as? Instant,
        lastReadAt = data["lastReadAt"] as? Instant,
        suggestions = (data["suggestions"] as? List<*>).orEmpty().filterIsInstance<String>(),
        userMessageCount = (data["userMessageCount"] as? Number)?.toInt() ?: 0,
        hidden = data["hidden"] == true,
        reasons = (data["whyYouMatch"] as? List<*>).orEmpty()
            .flatMap { item -> ((item as? Map<*, *>)?.get("traits") as? List<*>).orEmpty() }
            .filterIsInstance<String>(),
    )
}

/**
 * Mensagem do Firestore. Oculta ("Delete chats") fica de fora. Do personagem, é lida se chegou
 * até [lastReadAt]. Do usuário recusada pelo guardrail, vira [MessageStatus.Blocked] (o texto não
 * é guardado no servidor).
 */
fun messageFrom(document: Document, connectionId: String, lastReadAt: Instant?): Message? {
    val data = document.data
    if (data["hidden"] == true) return null
    val createdAt = data["createdAt"] as? Instant ?: return null
    val author = if (data["author"] == AUTHOR_USER) MessageAuthor.User else MessageAuthor.Character
    val blocked = data["blocked"] == true
    return Message(
        id = document.id,
        connectionId = connectionId,
        author = author,
        text = data["text"] as? String ?: "",
        sentAt = createdAt,
        status = userMessageStatus(author, blocked, data["status"]),
        read = author == MessageAuthor.User || (lastReadAt != null && !createdAt.isAfter(lastReadAt)),
    )
}

/** Estado da mensagem do usuário gravado pelo backend; a do personagem é sempre enviada. */
private fun userMessageStatus(author: MessageAuthor, blocked: Boolean, status: Any?): MessageStatus = when {
    author != MessageAuthor.User -> MessageStatus.Sent
    blocked || status == ApiStatus.BLOCKED -> MessageStatus.Blocked
    status == ApiStatus.FAILED -> MessageStatus.Failed
    status == ApiStatus.PENDING -> MessageStatus.Sending
    else -> MessageStatus.Sent
}

/**
 * Campos do perfil. A foto só entra quando existe, ou quando é removida ([previousPhoto] não nulo):
 * assim, quem nunca usou foto não depende das regras novas do Firestore para salvar o perfil.
 */
fun profileFields(profile: UserProfile, previousPhoto: String? = null, previousTitle: ProfileTitle? = null): Map<String, Any?> {
    val fields = mapOf(
        "displayName" to profile.name.trim().take(DISPLAY_NAME_MAX),
        "bio" to profile.bio.take(BIO_MAX),
        "avatarPreset" to profile.avatarPreset.coerceIn(0, AVATAR_PRESET_MAX),
        "profileStyle" to profileStyleFields(profile.style, previousTitle),
    )
    return when {
        profile.photo != null -> fields + ("avatarPhoto" to profile.photo)
        previousPhoto != null -> fields + ("avatarPhoto" to DeleteField)
        else -> fields
    }
}

fun preferencesFields(preferences: Preferences): Map<String, Any?> = mapOf(
    "origins" to preferences.origins.sortedBy { it.ordinal }.map { it.name },
    "powers" to preferences.powers.sortedBy { it.ordinal }.map { it.name },
    "teams" to preferences.teams.sortedBy { it.ordinal }.map { it.name },
    "styles" to preferences.styles.sortedBy { it.ordinal }.map { it.name },
    "fame" to preferences.fame.toDouble(),
)

/**
 * O título só entra quando existe; tirado, vai [DeleteField] (a gravação mescla, então omitir manteria o antigo).
 * Sem título antes nem agora, o campo nem entra: não depende das regras novas do Firestore.
 */
fun profileStyleFields(style: ProfileStyle, previousTitle: ProfileTitle? = null): Map<String, Any?> {
    val fields = mapOf(
        "cover" to style.cover.name,
        "accent" to style.accent.name,
        "frame" to style.frame.name,
        "prompt" to style.prompt.name,
        "promptAnswer" to style.promptAnswer.take(ProfileStyle.PROMPT_ANSWER_MAX),
        "featuredConnections" to style.featuredConnections.take(ProfileStyle.FEATURED_MAX),
        "featuredBadges" to style.featuredBadges.distinct().take(ProfileStyle.FEATURED_MAX),
    )
    return when {
        style.title != null -> fields + ("title" to style.title.name)
        previousTitle != null -> fields + ("title" to DeleteField)
        else -> fields
    }
}

private fun preferences(map: Map<*, *>?): Preferences {
    if (map == null) return Preferences.Any
    return Preferences(
        origins = enumSet(map["origins"], Origin.entries),
        powers = enumSet(map["powers"], PowerFamily.entries),
        teams = enumSet(map["teams"], Team.entries),
        styles = enumSet(map["styles"], Style.entries),
        fame = (map["fame"] as? Number)?.toFloat()
            ?.coerceIn(Preferences.FAME_ICONS, Preferences.FAME_HIDDEN_GEMS) ?: Preferences.FAME_DEFAULT,
    )
}

private fun profileStyle(map: Map<*, *>?): ProfileStyle {
    if (map == null) return ProfileStyle()
    val defaults = ProfileStyle()
    return ProfileStyle(
        cover = enumOrNull(map["cover"], ProfileCover.entries) ?: defaults.cover,
        accent = enumOrNull(map["accent"], ProfileAccent.entries) ?: defaults.accent,
        frame = enumOrNull(map["frame"], AvatarFrame.entries) ?: defaults.frame,
        prompt = enumOrNull(map["prompt"], ProfilePrompt.entries) ?: defaults.prompt,
        promptAnswer = (map["promptAnswer"] as? String)?.take(ProfileStyle.PROMPT_ANSWER_MAX) ?: "",
        featuredConnections = (map["featuredConnections"] as? List<*>).orEmpty().filterIsInstance<String>(),
        featuredBadges = (map["featuredBadges"] as? List<*>).orEmpty().filterIsInstance<String>(),
        title = enumOrNull(map["title"], ProfileTitle.entries),
    )
}

private fun <E : Enum<E>> enumSet(value: Any?, entries: List<E>): Set<E> =
    (value as? List<*>).orEmpty().mapNotNullTo(mutableSetOf()) { enumOrNull(it, entries) }

private fun <E : Enum<E>> enumOrNull(value: Any?, entries: List<E>): E? = entries.firstOrNull { it.name == value }
