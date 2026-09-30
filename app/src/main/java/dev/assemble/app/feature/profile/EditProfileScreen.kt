package dev.assemble.app.feature.profile

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.assemble.app.R
import dev.assemble.app.core.designsystem.component.AssembleTopBar
import dev.assemble.app.core.designsystem.component.AvatarPreset
import dev.assemble.app.core.designsystem.component.PrimaryButton
import dev.assemble.app.core.designsystem.component.StateView
import dev.assemble.app.core.designsystem.component.StateViewType
import dev.assemble.app.core.designsystem.component.TopBarNavigation
import dev.assemble.app.core.designsystem.component.TopBarTitle
import dev.assemble.app.core.designsystem.component.UserAvatar
import dev.assemble.app.core.designsystem.theme.AssembleTheme

private val PickerAvatarSize = 56.dp
private val SelectedBorderWidth = 3.dp
private const val BioMaxLines = 4

@Composable
fun EditProfileRoute(viewModel: EditProfileViewModel, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    EditProfileScreen(
        state = state,
        onBack = onBack,
        onNameChange = viewModel::onNameChange,
        onBioChange = viewModel::onBioChange,
        onAvatarChange = viewModel::onAvatarChange,
        onSave = { viewModel.save(onSaved = onBack) },
        onRetry = viewModel::load,
        modifier = modifier,
    )
}

@Composable
fun EditProfileScreen(
    state: EditProfileUiState,
    onBack: () -> Unit,
    onNameChange: (String) -> Unit,
    onBioChange: (String) -> Unit,
    onAvatarChange: (Int) -> Unit,
    onSave: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = AssembleTheme.colors.bg,
        topBar = {
            AssembleTopBar(
                title = TopBarTitle.Text(stringResource(R.string.profile_edit)),
                navigation = TopBarNavigation.Back(onBack),
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            when (state) {
                EditProfileUiState.Loading -> StateView(StateViewType.Loading(), modifier = Modifier.fillMaxWidth())
                EditProfileUiState.Error -> StateView(
                    StateViewType.Error(
                        title = stringResource(R.string.user_profile_error_title),
                        message = stringResource(R.string.state_error_connection),
                        onRetry = onRetry,
                    ),
                )
                is EditProfileUiState.Form -> ProfileForm(state, onNameChange, onBioChange, onAvatarChange, onSave)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProfileForm(
    form: EditProfileUiState.Form,
    onNameChange: (String) -> Unit,
    onBioChange: (String) -> Unit,
    onAvatarChange: (Int) -> Unit,
    onSave: () -> Unit,
) {
    val colors = AssembleTheme.colors
    val spacing = AssembleTheme.spacing
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = colors.actionAssemble,
        unfocusedBorderColor = colors.border,
        cursorColor = colors.actionAssemble,
        focusedLabelColor = colors.accentText,
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(spacing.space4),
        verticalArrangement = Arrangement.spacedBy(spacing.space4),
    ) {
        Text(
            stringResource(R.string.edit_profile_avatar),
            style = AssembleTheme.typography.body.copy(fontWeight = FontWeight.SemiBold),
            color = colors.text,
        )
        FlowRow(
            modifier = Modifier.selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(spacing.space3),
            verticalArrangement = Arrangement.spacedBy(spacing.space3),
        ) {
            AvatarPreset.entries.forEachIndexed { index, preset ->
                val description = stringResource(R.string.edit_profile_avatar_option, index + 1)
                val selected = form.avatarPreset == index
                UserAvatar(
                    preset = preset,
                    size = PickerAvatarSize,
                    modifier = Modifier
                        .border(SelectedBorderWidth, if (selected) colors.text else Color.Transparent, AssembleTheme.shapes.pill)
                        .clip(AssembleTheme.shapes.pill)
                        .selectable(selected = selected, role = Role.RadioButton, onClick = { onAvatarChange(index) })
                        .semantics { contentDescription = description },
                )
            }
        }
        OutlinedTextField(
            value = form.name,
            onValueChange = onNameChange,
            label = { Text(stringResource(R.string.edit_profile_name)) },
            isError = form.showNameError,
            supportingText = if (form.showNameError) {
                { Text(stringResource(R.string.edit_profile_name_required)) }
            } else {
                null
            },
            singleLine = true,
            shape = AssembleTheme.shapes.md,
            colors = fieldColors,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = form.bio,
            onValueChange = onBioChange,
            label = { Text(stringResource(R.string.profile_bio)) },
            maxLines = BioMaxLines,
            shape = AssembleTheme.shapes.md,
            colors = fieldColors,
            modifier = Modifier.fillMaxWidth(),
        )
        if (form.showSaveError) {
            Text(
                text = stringResource(R.string.edit_profile_save_error),
                style = AssembleTheme.typography.caption,
                color = colors.error,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
        PrimaryButton(
            text = stringResource(R.string.action_save),
            onClick = onSave,
            loading = form.saving,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@PreviewLightDark
@Composable
private fun EditProfilePreview() {
    AssembleTheme {
        EditProfileScreen(
            state = EditProfileUiState.Form(name = "Felipe", bio = "", avatarPreset = 2, showNameError = false),
            onBack = {}, onNameChange = {}, onBioChange = {}, onAvatarChange = {}, onSave = {}, onRetry = {},
        )
    }
}
