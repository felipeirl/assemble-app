package dev.assemble.app.feature.achievements

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.assemble.app.R
import dev.assemble.app.core.designsystem.theme.AssembleTheme
import dev.assemble.app.core.designsystem.theme.DarkAssembleColors
import dev.assemble.app.core.designsystem.theme.rememberAnimationsEnabled
import dev.assemble.app.core.domain.Achievement
import dev.assemble.app.core.feedback.Cue
import dev.assemble.app.core.feedback.LocalFeedback
import kotlinx.coroutines.delay

private const val VisibleMillis = 2_800L
private const val SlideMillis = 400
private const val LabelAlpha = 0.75f
private val ToastBadgeWidth = 36.dp
private val ToastBadgeHeight = 40.dp

/**
 * Aviso no topo quando uma conquista é desbloqueada com o app aberto. Uma por vez, em fila.
 * Sempre midnight com texto claro, nos dois temas. Tocar abre a tela de conquistas.
 */
@Composable
fun AchievementUnlockHost(tracker: AchievementTracker, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    var current by remember { mutableStateOf<Achievement?>(null) }
    // Guarda a última para o conteúdo não sumir durante a animação de saída.
    var lastShown by remember { mutableStateOf<Achievement?>(null) }
    val feedback = LocalFeedback.current
    LaunchedEffect(tracker) {
        tracker.unlocks.collect { achievement ->
            lastShown = achievement
            current = achievement
            feedback.play(Cue.Achievement)
            delay(VisibleMillis)
            current = null
            delay(SlideMillis.toLong())
        }
    }
    val duration = if (rememberAnimationsEnabled()) SlideMillis else 0
    AnimatedVisibility(
        visible = current != null,
        modifier = modifier,
        enter = slideInVertically(tween(duration)) { -it } + fadeIn(tween(duration)),
        exit = slideOutVertically(tween(duration)) { -it } + fadeOut(tween(duration)),
    ) {
        lastShown?.let { UnlockToast(it, onClick = onOpen) }
    }
}

@Composable
private fun UnlockToast(achievement: Achievement, onClick: () -> Unit) {
    val dark = DarkAssembleColors
    val spacing = AssembleTheme.spacing
    val info = achievement.info()
    Row(
        modifier = Modifier
            .padding(horizontal = spacing.space4)
            .clip(AssembleTheme.shapes.md)
            .background(dark.midnight)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { liveRegion = LiveRegionMode.Polite }
            .padding(horizontal = spacing.space4, vertical = spacing.space3),
        horizontalArrangement = Arrangement.spacedBy(spacing.space3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HexBadge(icon = info.icon, unlocked = true, revealOrder = 0, badgeSize = ToastBadgeWidth to ToastBadgeHeight)
        Column {
            Text(
                text = stringResource(R.string.achievement_unlocked_toast),
                style = AssembleTheme.typography.small,
                color = dark.text.copy(alpha = LabelAlpha),
            )
            Text(
                text = stringResource(info.title),
                style = AssembleTheme.typography.body.copy(fontWeight = FontWeight.SemiBold),
                color = dark.text,
            )
            Text(
                text = stringResource(R.string.achievement_reward, rewardText(achievement.reward)),
                style = AssembleTheme.typography.small,
                color = dark.text.copy(alpha = LabelAlpha),
            )
        }
    }
}
