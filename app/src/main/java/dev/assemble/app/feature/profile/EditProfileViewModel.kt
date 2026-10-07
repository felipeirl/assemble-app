package dev.assemble.app.feature.profile

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.assemble.app.core.data.CharacterRepository
import dev.assemble.app.core.data.ConnectionRepository
import dev.assemble.app.core.data.UserRepository
import dev.assemble.app.core.domain.Achievement
import dev.assemble.app.core.domain.Archetype
import dev.assemble.app.core.domain.ProfileRules
import dev.assemble.app.core.domain.Reward
import dev.assemble.app.core.media.AvatarImage
import dev.assemble.app.core.media.PhotoCrop
import dev.assemble.app.core.model.AvatarFrame
import dev.assemble.app.core.model.ProfileAccent
import dev.assemble.app.core.model.ProfileCover
import dev.assemble.app.core.model.ProfilePrompt
import dev.assemble.app.core.model.ProfileStyle
import dev.assemble.app.core.model.ProfileTitle
import dev.assemble.app.core.model.UserProfile
import dev.assemble.app.feature.achievements.AchievementTracker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException

sealed interface EditProfileUiState {
    data object Loading : EditProfileUiState

    data class Form(
        val name: String,
        val bio: String,
        val avatarPreset: Int,
        val photo: String? = null,
        val style: ProfileStyle = ProfileStyle(),
        val archetype: Archetype? = null,
        /** Conquistas desbloqueadas: liberam molduras e podem ir para os destaques. */
        val unlocked: Set<Achievement> = emptySet(),
        /** Personagens conectados que podem ir para os destaques. */
        val connections: List<ProfileConnection> = emptyList(),
        val saving: Boolean = false,
        val showNameError: Boolean = false,
        val showSaveError: Boolean = false,
        val showPhotoError: Boolean = false,
        /** Há uma foto ajustada nesta visita: dá para reajustar sem escolher de novo na galeria. */
        val canAdjustPhoto: Boolean = false,
    ) : EditProfileUiState {
        val profile: UserProfile get() = UserProfile(name = name, bio = bio, avatarPreset = avatarPreset, style = style, photo = photo)
    }

    data object Error : EditProfileUiState
}

/** Foto aberta na tela de ajuste, a partir de [crop]. */
data class PhotoCropRequest(val source: Bitmap, val crop: PhotoCrop)

class EditProfileViewModel(
    private val userRepository: UserRepository,
    private val characterRepository: CharacterRepository,
    private val connectionRepository: ConnectionRepository,
    private val achievementTracker: AchievementTracker,
) : ViewModel() {
    private val state = MutableStateFlow<EditProfileUiState>(EditProfileUiState.Loading)
    val uiState: StateFlow<EditProfileUiState> = state.asStateFlow()

    private val cropRequest = MutableStateFlow<PhotoCropRequest?>(null)

    /** Não nulo enquanto a tela de ajuste da foto está aberta. */
    val photoCrop: StateFlow<PhotoCropRequest?> = cropRequest.asStateFlow()

    /** Última foto ajustada nesta visita (só em memória): é o que "Ajustar foto" reabre. */
    private var lastCrop: PhotoCropRequest? = null

    init {
        load()
    }

    fun load() {
        state.value = EditProfileUiState.Loading
        viewModelScope.launch {
            state.value = try {
                val profile = userRepository.observeProfile().first()
                val characters = characterRepository.getCharacters().associateBy { it.id }
                val connections = connectionRepository.observeConnections().first()
                    .mapNotNull { characters[it.characterId] }
                    .map { ProfileConnection(it.id, it.name, it.imageUrl) }
                val unlocked = achievementTracker.progress.filterNotNull().first()
                    .filter { it.unlocked }
                    .mapTo(mutableSetOf()) { it.achievement }
                EditProfileUiState.Form(
                    name = profile.name,
                    bio = profile.bio,
                    avatarPreset = profile.avatarPreset,
                    photo = profile.photo,
                    style = ProfileRules.sanitize(profile.style, unlocked, connections.mapTo(mutableSetOf()) { it.characterId }),
                    archetype = ProfileRules.archetype(userRepository.preferences.value),
                    unlocked = unlocked,
                    connections = connections,
                )
            } catch (_: IOException) {
                EditProfileUiState.Error
            }
        }
    }

    fun onNameChange(name: String) = updateForm { it.copy(name = name, showNameError = false) }

    fun onBioChange(bio: String) = updateForm { it.copy(bio = bio) }

    /** Foto escolhida na galeria: abre o ajuste do zero. */
    fun onPhotoPicked(source: Bitmap) {
        updateForm { it.copy(showPhotoError = false) }
        cropRequest.value = PhotoCropRequest(source, PhotoCrop())
    }

    fun onAdjustPhoto() {
        cropRequest.value = lastCrop
    }

    /** Cancelar mantém a foto que já estava no formulário. */
    fun onCropCancel() {
        cropRequest.value = null
    }

    fun onCropConfirm(crop: PhotoCrop) {
        val request = cropRequest.value ?: return
        cropRequest.value = null
        viewModelScope.launch {
            try {
                val photo = AvatarImage.encode(request.source, crop)
                lastCrop = request.copy(crop = crop)
                updateForm { it.copy(photo = photo, canAdjustPhoto = true, showPhotoError = false) }
            } catch (_: IOException) {
                updateForm { it.copy(showPhotoError = true) }
            }
        }
    }

    fun onRemovePhoto() {
        lastCrop = null
        updateForm { it.copy(photo = null, canAdjustPhoto = false, showPhotoError = false) }
    }

    fun onPhotoError() = updateForm { it.copy(showPhotoError = true) }

    /** Capa, cor, moldura e título bloqueados não são aplicados. */
    fun onCoverChange(cover: ProfileCover) = applyIfUnlocked(Reward.Cover(cover)) { it.copy(cover = cover) }

    fun onAccentChange(accent: ProfileAccent) = applyIfUnlocked(Reward.Accent(accent)) { it.copy(accent = accent) }

    fun onFrameChange(frame: AvatarFrame) = applyIfUnlocked(Reward.Frame(frame)) { it.copy(frame = frame) }

    /** null tira o título. */
    fun onTitleChange(title: ProfileTitle?) {
        if (title == null) updateStyle { it.copy(title = null) } else applyIfUnlocked(Reward.Title(title)) { it.copy(title = title) }
    }

    fun onPromptChange(prompt: ProfilePrompt) = updateStyle { it.copy(prompt = prompt) }

    fun onPromptAnswerChange(answer: String) = updateStyle { it.copy(promptAnswer = answer.take(ProfileStyle.PROMPT_ANSWER_MAX)) }

    fun onToggleFeaturedConnection(characterId: String) = updateStyle {
        it.copy(featuredConnections = ProfileRules.toggleFeatured(it.featuredConnections, characterId))
    }

    fun onToggleFeaturedBadge(achievement: Achievement) = updateStyle {
        it.copy(featuredBadges = ProfileRules.toggleFeatured(it.featuredBadges, achievement.name))
    }

    /** Nome é obrigatório. [onSaved] roda só em sucesso. */
    fun save(onSaved: () -> Unit) {
        val form = state.value as? EditProfileUiState.Form ?: return
        if (form.saving) return
        val name = form.name.trim()
        if (name.isEmpty()) {
            updateForm { it.copy(showNameError = true) }
            return
        }
        updateForm { it.copy(saving = true, showSaveError = false) }
        val style = ProfileRules.sanitize(form.style, form.unlocked, form.connections.mapTo(mutableSetOf()) { it.characterId })
        viewModelScope.launch {
            try {
                userRepository.updateProfile(
                    UserProfile(name = name, bio = form.bio.trim(), avatarPreset = form.avatarPreset, style = style, photo = form.photo),
                )
                onSaved()
            } catch (_: IOException) {
                updateForm { it.copy(saving = false, showSaveError = true) }
            }
        }
    }

    private fun updateStyle(transform: (ProfileStyle) -> ProfileStyle) = updateForm { it.copy(style = transform(it.style)) }

    private fun applyIfUnlocked(reward: Reward, transform: (ProfileStyle) -> ProfileStyle) = updateForm { form ->
        if (ProfileRules.isUnlocked(reward, form.unlocked)) form.copy(style = transform(form.style)) else form
    }

    private fun updateForm(transform: (EditProfileUiState.Form) -> EditProfileUiState.Form) {
        state.update { current -> if (current is EditProfileUiState.Form) transform(current) else current }
    }
}
