package dev.assemble.app.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.assemble.app.core.data.UserRepository
import dev.assemble.app.core.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
        val saving: Boolean = false,
        val showNameError: Boolean = false,
        val showSaveError: Boolean = false,
    ) : EditProfileUiState

    data object Error : EditProfileUiState
}

class EditProfileViewModel(private val userRepository: UserRepository) : ViewModel() {
    private val state = MutableStateFlow<EditProfileUiState>(EditProfileUiState.Loading)
    val uiState: StateFlow<EditProfileUiState> = state.asStateFlow()

    init {
        load()
    }

    fun load() {
        state.value = EditProfileUiState.Loading
        viewModelScope.launch {
            state.value = try {
                val profile = userRepository.observeProfile().first()
                EditProfileUiState.Form(name = profile.name, bio = profile.bio, avatarPreset = profile.avatarPreset)
            } catch (_: IOException) {
                EditProfileUiState.Error
            }
        }
    }

    fun onNameChange(name: String) = updateForm { it.copy(name = name, showNameError = false) }

    fun onBioChange(bio: String) = updateForm { it.copy(bio = bio) }

    fun onAvatarChange(preset: Int) = updateForm { it.copy(avatarPreset = preset) }

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
        viewModelScope.launch {
            try {
                userRepository.updateProfile(UserProfile(name = name, bio = form.bio.trim(), avatarPreset = form.avatarPreset))
                onSaved()
            } catch (_: IOException) {
                updateForm { it.copy(saving = false, showSaveError = true) }
            }
        }
    }

    private fun updateForm(transform: (EditProfileUiState.Form) -> EditProfileUiState.Form) {
        state.update { current -> if (current is EditProfileUiState.Form) transform(current) else current }
    }
}
