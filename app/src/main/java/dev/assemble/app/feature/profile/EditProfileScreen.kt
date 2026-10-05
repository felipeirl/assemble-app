package dev.assemble.app.feature.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.TextButton
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import dev.assemble.app.core.designsystem.component.LocalUserPhoto
import dev.assemble.app.core.designsystem.component.SecondaryButton
import dev.assemble.app.core.media.AvatarImage
import java.io.IOException
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.assemble.app.R
import dev.assemble.app.core.designsystem.component.AssembleLargeTopBar
import dev.assemble.app.core.designsystem.component.AvatarPreset
import dev.assemble.app.core.designsystem.component.FramedAvatar
import dev.assemble.app.core.designsystem.component.PrimaryButton
import dev.assemble.app.core.designsystem.component.ProfileCoverArt
import dev.assemble.app.core.designsystem.component.StateView
import dev.assemble.app.core.designsystem.component.StateViewType
import dev.assemble.app.core.designsystem.component.TraitChip
import dev.assemble.app.core.designsystem.component.UserAvatar
import dev.assemble.app.core.designsystem.component.color
import dev.assemble.app.core.designsystem.icon.AssembleIcons
import dev.assemble.app.core.designsystem.theme.AssembleTheme
import dev.assemble.app.core.domain.Achievement
import dev.assemble.app.core.domain.ProfileRules
import dev.assemble.app.core.model.AvatarFrame
import dev.assemble.app.core.model.ProfileAccent
import dev.assemble.app.core.model.ProfileCover
import dev.assemble.app.core.model.ProfilePrompt
import dev.assemble.app.core.model.ProfileStyle
import dev.assemble.app.feature.achievements.info

private val PickerAvatarSize = 56.dp
private val SelectedBorderWidth = 3.dp
private const val BioMaxLines = 4
private const val PromptAnswerMaxLines = 2
private val PreviewCoverHeight = 88.dp
private val PreviewAvatarSize = 72.dp
private val CoverOptionWidth = 76.dp
private val CoverOptionHeight = 48.dp
private val AccentOptionSize = 40.dp
private val FrameOptionWidth = 76.dp
private val FrameOptionAvatarSize = 48.dp
private val LockIconSize = 16.dp
private const val LockedAlpha = 0.4f

/** Ações da edição do perfil; agrupadas para a tela não receber uma lista enorme de parâmetros. */
data class EditProfileActions(
    val onNameChange: (String) -> Unit = {},
    val onBioChange: (String) -> Unit = {},
    val onAvatarChange: (Int) -> Unit = {},
    val onPhotoChange: (String?) -> Unit = {},
    val onPhotoError: () -> Unit = {},
    val onCoverChange: (ProfileCover) -> Unit = {},
    val onAccentChange: (ProfileAccent) -> Unit = {},
    val onFrameChange: (AvatarFrame) -> Unit = {},
    val onPromptChange: (ProfilePrompt) -> Unit = {},
    val onPromptAnswerChange: (String) -> Unit = {},
    val onToggleFeaturedConnection: (String) -> Unit = {},
    val onToggleFeaturedBadge: (Achievement) -> Unit = {},
    val onSave: () -> Unit = {},
)

@Composable
fun EditProfileRoute(viewModel: EditProfileViewModel, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    EditProfileScreen(
        state = state,
        onBack = onBack,
        actions = EditProfileActions(
            onNameChange = viewModel::onNameChange,
            onBioChange = viewModel::onBioChange,
            onAvatarChange = viewModel::onAvatarChange,
            onPhotoChange = viewModel::onPhotoChange,
            onPhotoError = viewModel::onPhotoError,
            onCoverChange = viewModel::onCoverChange,
            onAccentChange = viewModel::onAccentChange,
            onFrameChange = viewModel::onFrameChange,
            onPromptChange = viewModel::onPromptChange,
            onPromptAnswerChange = viewModel::onPromptAnswerChange,
            onToggleFeaturedConnection = viewModel::onToggleFeaturedConnection,
            onToggleFeaturedBadge = viewModel::onToggleFeaturedBadge,
            onSave = { viewModel.save(onSaved = onBack) },
        ),
        onRetry = viewModel::load,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    state: EditProfileUiState,
    onBack: () -> Unit,
    actions: EditProfileActions,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = AssembleTheme.colors.bg,
        topBar = {
            AssembleLargeTopBar(title = stringResource(R.string.profile_edit), onBack = onBack, scrollBehavior = scrollBehavior)
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
                // A prévia e as molduras mostram a foto ainda não salva.
                is EditProfileUiState.Form -> CompositionLocalProvider(LocalUserPhoto provides state.photo) {
                    ProfileForm(state, actions)
                }
            }
        }
    }
}

@Composable
private fun ProfileForm(form: EditProfileUiState.Form, actions: EditProfileActions) {
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
        verticalArrangement = Arrangement.spacedBy(spacing.space5),
    ) {
        // Prévia ao vivo: o que os outros veem no topo do perfil.
        Column(verticalArrangement = Arrangement.spacedBy(spacing.space3)) {
            ProfileHeader(
                profile = form.profile,
                archetype = form.archetype,
                coverShape = AssembleTheme.shapes.lg,
                coverHeight = PreviewCoverHeight,
                avatarSize = PreviewAvatarSize,
            )
            ProfilePromptCard(form.style)
        }

        IdentityFields(form, actions, fieldColors)
        CoverPicker(form.style, actions.onCoverChange)
        AccentPicker(form.style.accent, actions.onAccentChange)
        FramePicker(form, actions.onFrameChange)
        PromptFields(form.style, actions, fieldColors)
        FeaturedPicker(
            title = stringResource(R.string.edit_profile_featured_connections, form.style.featuredConnections.size, ProfileStyle.FEATURED_MAX),
            emptyText = stringResource(R.string.edit_profile_no_connections),
            options = form.connections.map { it.characterId to it.name },
            selected = form.style.featuredConnections,
            onToggle = actions.onToggleFeaturedConnection,
        )
        val badges = Achievement.entries.filter { it in form.unlocked }
        FeaturedPicker(
            title = stringResource(R.string.edit_profile_featured_badges, form.style.featuredBadges.size, ProfileStyle.FEATURED_MAX),
            emptyText = stringResource(R.string.edit_profile_no_badges),
            options = badges.map { it.name to stringResource(it.info().title) },
            selected = form.style.featuredBadges,
            onToggle = { name -> badges.find { it.name == name }?.let(actions.onToggleFeaturedBadge) },
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
            onClick = actions.onSave,
            loading = form.saving,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Foto da galeria (seletor do sistema, sem permissão): reduzida e guardada no próprio perfil. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PhotoPicker(form: EditProfileUiState.Form, actions: EditProfileActions) {
    val colors = AssembleTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    actions.onPhotoChange(AvatarImage.encode(context.contentResolver, uri))
                } catch (_: IOException) {
                    actions.onPhotoError()
                }
            }
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(AssembleTheme.spacing.space2)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(AssembleTheme.spacing.space3)) {
            SecondaryButton(
                text = stringResource(R.string.edit_profile_photo_choose),
                onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            )
            if (form.photo != null) {
                TextButton(onClick = { actions.onPhotoChange(null) }) {
                    Text(stringResource(R.string.edit_profile_photo_remove), color = colors.error)
                }
            }
        }
        Text(
            text = stringResource(if (form.showPhotoError) R.string.edit_profile_photo_error else R.string.edit_profile_photo_hint),
            style = AssembleTheme.typography.caption,
            color = if (form.showPhotoError) colors.error else colors.textMuted,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IdentityFields(form: EditProfileUiState.Form, actions: EditProfileActions, fieldColors: TextFieldColors) {
    val colors = AssembleTheme.colors
    val spacing = AssembleTheme.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.space4)) {
        FieldTitle(stringResource(R.string.edit_profile_avatar))
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
                    photo = null,
                    size = PickerAvatarSize,
                    modifier = Modifier
                        .border(SelectedBorderWidth, if (selected) colors.text else Color.Transparent, AssembleTheme.shapes.pill)
                        .clip(AssembleTheme.shapes.pill)
                        .selectable(selected = selected, role = Role.RadioButton, onClick = { actions.onAvatarChange(index) })
                        .semantics { contentDescription = description },
                )
            }
        }
        PhotoPicker(form, actions)
        OutlinedTextField(
            value = form.name,
            onValueChange = actions.onNameChange,
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
            onValueChange = actions.onBioChange,
            label = { Text(stringResource(R.string.profile_bio)) },
            maxLines = BioMaxLines,
            shape = AssembleTheme.shapes.md,
            colors = fieldColors,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CoverPicker(style: ProfileStyle, onChange: (ProfileCover) -> Unit) {
    val spacing = AssembleTheme.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.space3)) {
        FieldTitle(stringResource(R.string.edit_profile_cover))
        FlowRow(
            modifier = Modifier.selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(spacing.space3),
            verticalArrangement = Arrangement.spacedBy(spacing.space3),
        ) {
            ProfileCover.entries.forEach { cover ->
                val selected = style.cover == cover
                OptionTile(
                    label = stringResource(cover.label),
                    selected = selected,
                    onClick = { onChange(cover) },
                    width = CoverOptionWidth,
                ) {
                    ProfileCoverArt(
                        cover = cover,
                        accent = style.accent,
                        modifier = Modifier
                            .width(CoverOptionWidth)
                            .height(CoverOptionHeight)
                            .clip(AssembleTheme.shapes.sm)
                            .border(SelectedBorderWidth, if (selected) AssembleTheme.colors.text else Color.Transparent, AssembleTheme.shapes.sm),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AccentPicker(current: ProfileAccent, onChange: (ProfileAccent) -> Unit) {
    val colors = AssembleTheme.colors
    val spacing = AssembleTheme.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.space3)) {
        FieldTitle(stringResource(R.string.edit_profile_accent))
        FlowRow(
            modifier = Modifier.selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(spacing.space3),
        ) {
            ProfileAccent.entries.forEach { accent ->
                val selected = accent == current
                val description = stringResource(accent.label)
                Box(
                    Modifier
                        .size(AccentOptionSize)
                        .border(SelectedBorderWidth, if (selected) colors.text else Color.Transparent, AssembleTheme.shapes.pill)
                        .padding(SelectedBorderWidth + 2.dp)
                        .clip(AssembleTheme.shapes.pill)
                        .background(accent.color(colors))
                        .selectable(selected = selected, role = Role.RadioButton, onClick = { onChange(accent) })
                        .semantics { contentDescription = description },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FramePicker(form: EditProfileUiState.Form, onChange: (AvatarFrame) -> Unit) {
    val spacing = AssembleTheme.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.space3)) {
        FieldTitle(stringResource(R.string.edit_profile_frame))
        FlowRow(
            modifier = Modifier.selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(spacing.space3),
            verticalArrangement = Arrangement.spacedBy(spacing.space3),
        ) {
            AvatarFrame.entries.forEach { frame ->
                val required = ProfileRules.requiredAchievement(frame)
                val locked = !ProfileRules.isFrameUnlocked(frame, form.unlocked)
                val label = stringResource(frame.label)
                val lockText = required?.takeIf { locked }?.let {
                    stringResource(R.string.edit_profile_frame_locked, stringResource(it.info().title))
                }
                OptionTile(
                    label = lockText ?: label,
                    selected = form.style.frame == frame,
                    onClick = { onChange(frame) },
                    width = FrameOptionWidth,
                    enabled = !locked,
                    description = listOfNotNull(label, lockText).joinToString(". "),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        FramedAvatar(
                            preset = AvatarPreset.fromIndex(form.avatarPreset),
                            frame = frame,
                            accent = form.style.accent,
                            size = FrameOptionAvatarSize,
                            modifier = if (locked) Modifier.alpha(LockedAlpha) else Modifier,
                        )
                        if (locked) {
                            Icon(AssembleIcons.Lock, contentDescription = null, tint = AssembleTheme.colors.text, modifier = Modifier.size(LockIconSize))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PromptFields(style: ProfileStyle, actions: EditProfileActions, fieldColors: TextFieldColors) {
    val spacing = AssembleTheme.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.space3)) {
        FieldTitle(stringResource(R.string.edit_profile_prompt))
        FlowRow(
            modifier = Modifier.selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(spacing.space2),
            verticalArrangement = Arrangement.spacedBy(spacing.space2),
        ) {
            ProfilePrompt.entries.forEach { prompt ->
                TraitChip(
                    label = stringResource(prompt.question),
                    selected = style.prompt == prompt,
                    onSelectedChange = { actions.onPromptChange(prompt) },
                )
            }
        }
        OutlinedTextField(
            value = style.promptAnswer,
            onValueChange = actions.onPromptAnswerChange,
            label = { Text(stringResource(R.string.edit_profile_prompt_answer)) },
            supportingText = {
                Text(
                    stringResource(R.string.edit_profile_counter, style.promptAnswer.length, ProfileStyle.PROMPT_ANSWER_MAX),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End,
                )
            },
            maxLines = PromptAnswerMaxLines,
            shape = AssembleTheme.shapes.md,
            colors = fieldColors,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Escolha de até [ProfileStyle.FEATURED_MAX] itens, na ordem dos toques. Cheio, novos toques são ignorados. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FeaturedPicker(
    title: String,
    emptyText: String,
    options: List<Pair<String, String>>,
    selected: List<String>,
    onToggle: (String) -> Unit,
) {
    val spacing = AssembleTheme.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.space3)) {
        FieldTitle(title)
        if (options.isEmpty()) {
            Text(emptyText, style = AssembleTheme.typography.caption, color = AssembleTheme.colors.textMuted)
            return@Column
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(spacing.space2),
            verticalArrangement = Arrangement.spacedBy(spacing.space2),
        ) {
            options.forEach { (id, label) ->
                TraitChip(label = label, selected = id in selected, onSelectedChange = { onToggle(id) })
            }
        }
    }
}

@Composable
private fun OptionTile(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    width: Dp,
    enabled: Boolean = true,
    description: String = label,
    art: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(width)
            .clip(AssembleTheme.shapes.sm)
            .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = description },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AssembleTheme.spacing.space1),
    ) {
        art()
        Text(
            text = label,
            style = AssembleTheme.typography.small.copy(fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal),
            color = if (selected) AssembleTheme.colors.text else AssembleTheme.colors.textMuted,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun FieldTitle(text: String) {
    Text(
        text,
        style = AssembleTheme.typography.body.copy(fontWeight = FontWeight.SemiBold),
        color = AssembleTheme.colors.text,
        modifier = Modifier.semantics { heading() },
    )
}

@PreviewLightDark
@Composable
private fun EditProfilePreview() {
    AssembleTheme {
        EditProfileScreen(
            state = EditProfileUiState.Form(
                name = "Felipe",
                bio = "",
                avatarPreset = 2,
                style = ProfileStyle(cover = ProfileCover.Comic, accent = ProfileAccent.Violet, promptAnswer = "Flight, obviously"),
                unlocked = setOf(Achievement.FirstConnection, Achievement.TeamUp),
                connections = listOf(ProfileConnection("storm", "Storm", null)),
            ),
            onBack = {},
            actions = EditProfileActions(),
            onRetry = {},
        )
    }
}
