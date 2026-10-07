package dev.assemble.app.feature.profile

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.assemble.app.R
import dev.assemble.app.core.designsystem.component.AvatarPreset
import dev.assemble.app.core.designsystem.component.FramedAvatar
import dev.assemble.app.core.designsystem.component.ProfileCoverArt
import dev.assemble.app.core.designsystem.component.color
import dev.assemble.app.core.designsystem.component.onColor
import dev.assemble.app.core.designsystem.theme.AssembleTheme
import dev.assemble.app.core.domain.Archetype
import dev.assemble.app.core.model.AvatarFrame
import dev.assemble.app.core.model.ProfileAccent
import dev.assemble.app.core.model.ProfileCover
import dev.assemble.app.core.model.ProfilePrompt
import dev.assemble.app.core.model.ProfileStyle
import dev.assemble.app.core.model.ProfileTitle
import dev.assemble.app.core.model.UserProfile
import dev.assemble.app.core.ui.traitLabel

private val CoverHeight = 132.dp
private val HeaderAvatarSize = 96.dp
private val PromptBarWidth = 4.dp
private const val ArchetypeSeparator = " · "

/** Capa, avatar com moldura, nome e arquétipo. Usado no perfil e na prévia da edição. */
@Composable
internal fun ProfileHeader(
    profile: UserProfile,
    archetype: Archetype?,
    modifier: Modifier = Modifier,
    coverShape: Shape = RectangleShape,
    coverHeight: Dp = CoverHeight,
    avatarSize: Dp = HeaderAvatarSize,
) {
    val style = profile.style
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
            ProfileCoverArt(style.cover, style.accent, Modifier.fillMaxWidth().height(coverHeight).clip(coverShape))
            FramedAvatar(
                preset = AvatarPreset.fromIndex(profile.avatarPreset),
                frame = style.frame,
                accent = style.accent,
                size = avatarSize,
                modifier = Modifier.offset(y = avatarSize / 2),
            )
        }
        Spacer(Modifier.height(avatarSize / 2 + AssembleTheme.spacing.space2))
        Text(profile.name, style = AssembleTheme.typography.h2, color = AssembleTheme.colors.text, textAlign = TextAlign.Center)
        style.title?.let { title ->
            Text(
                text = stringResource(title.label).uppercase(),
                style = AssembleTheme.typography.eyebrow,
                color = AssembleTheme.colors.accentText,
                textAlign = TextAlign.Center,
            )
        }
        if (archetype != null) {
            Spacer(Modifier.height(AssembleTheme.spacing.space1))
            ArchetypeChip(archetype, style.accent)
        }
    }
}

@Composable
private fun ArchetypeChip(archetype: Archetype, accent: ProfileAccent) {
    val colors = AssembleTheme.colors
    Text(
        text = archetypeText(archetype),
        style = AssembleTheme.typography.small.copy(fontWeight = FontWeight.SemiBold),
        color = accent.onColor(colors),
        modifier = Modifier
            .clip(AssembleTheme.shapes.pill)
            .background(accent.color(colors))
            .padding(horizontal = AssembleTheme.spacing.space3, vertical = AssembleTheme.spacing.space1),
    )
}

@Composable
internal fun archetypeText(archetype: Archetype): String = listOfNotNull(
    archetype.style?.let { stringResource(traitLabel(it)) },
    archetype.origin?.let { stringResource(traitLabel(it)) },
).joinToString(ArchetypeSeparator)

/** Frase de apresentação: pergunta + resposta, com a barra no destaque. Sem resposta, não aparece. */
@Composable
internal fun ProfilePromptCard(style: ProfileStyle, modifier: Modifier = Modifier) {
    if (style.promptAnswer.isBlank()) return
    val colors = AssembleTheme.colors
    Row(
        modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(AssembleTheme.shapes.md)
            .background(colors.surface),
    ) {
        Box(Modifier.width(PromptBarWidth).fillMaxHeight().background(style.accent.color(colors)))
        Column(
            Modifier.padding(AssembleTheme.spacing.space4),
            verticalArrangement = Arrangement.spacedBy(AssembleTheme.spacing.space1),
        ) {
            Text(stringResource(style.prompt.question), style = AssembleTheme.typography.caption, color = colors.textMuted)
            Text(
                style.promptAnswer,
                style = AssembleTheme.typography.body.copy(fontWeight = FontWeight.SemiBold),
                color = colors.text,
            )
        }
    }
}

@get:StringRes
internal val ProfilePrompt.question: Int
    get() = when (this) {
        ProfilePrompt.DreamPower -> R.string.profile_prompt_dream_power
        ProfilePrompt.IdealTeam -> R.string.profile_prompt_ideal_team
        ProfilePrompt.FirstRecruit -> R.string.profile_prompt_first_recruit
    }

@get:StringRes
internal val ProfileCover.label: Int
    get() = when (this) {
        ProfileCover.Energy -> R.string.profile_cover_energy
        ProfileCover.Halftone -> R.string.profile_cover_halftone
        ProfileCover.Comic -> R.string.profile_cover_comic
        ProfileCover.Night -> R.string.profile_cover_night
        ProfileCover.Headline -> R.string.profile_cover_headline
        ProfileCover.Blueprint -> R.string.profile_cover_blueprint
        ProfileCover.Cosmos -> R.string.profile_cover_cosmos
    }

@get:StringRes
internal val ProfileAccent.label: Int
    get() = when (this) {
        ProfileAccent.Pink -> R.string.profile_accent_pink
        ProfileAccent.Red -> R.string.profile_accent_red
        ProfileAccent.Blue -> R.string.profile_accent_blue
        ProfileAccent.Violet -> R.string.profile_accent_violet
        ProfileAccent.Gold -> R.string.profile_accent_gold
        ProfileAccent.Emerald -> R.string.profile_accent_emerald
        ProfileAccent.Silver -> R.string.profile_accent_silver
    }

@get:StringRes
internal val AvatarFrame.label: Int
    get() = when (this) {
        AvatarFrame.Simple -> R.string.profile_frame_simple
        AvatarFrame.Ring -> R.string.profile_frame_ring
        AvatarFrame.Hexagon -> R.string.profile_frame_hexagon
        AvatarFrame.Burst -> R.string.profile_frame_burst
        AvatarFrame.Shield -> R.string.profile_frame_shield
        AvatarFrame.Cosmic -> R.string.profile_frame_cosmic
        AvatarFrame.Lightning -> R.string.profile_frame_lightning
    }

@get:StringRes
internal val ProfileTitle.label: Int
    get() = when (this) {
        ProfileTitle.Recruit -> R.string.profile_title_recruit
        ProfileTitle.IceBreaker -> R.string.profile_title_ice_breaker
        ProfileTitle.Explorer -> R.string.profile_title_explorer
        ProfileTitle.Diplomat -> R.string.profile_title_diplomat
        ProfileTitle.LivingLegend -> R.string.profile_title_living_legend
        ProfileTitle.AlterEgo -> R.string.profile_title_alter_ego
    }
