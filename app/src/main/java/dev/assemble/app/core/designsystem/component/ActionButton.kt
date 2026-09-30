package dev.assemble.app.core.designsystem.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.assemble.app.R
import dev.assemble.app.core.designsystem.icon.AssembleIcons
import dev.assemble.app.core.designsystem.theme.AssembleShadow
import dev.assemble.app.core.designsystem.theme.AssembleTheme
import dev.assemble.app.core.designsystem.theme.assembleShadow

enum class ActionButtonType { Pass, Assemble }

enum class ActionButtonSize(val diameter: Dp, val iconSize: Dp) {
    Large(diameter = 72.dp, iconSize = 32.dp),
    Small(diameter = 52.dp, iconSize = 24.dp),
}

private const val PressedScale = 0.92f
private val PassBorderWidth = 1.dp

/** Botão circular de Pass (neutro) ou Assemble (única ação colorida). */
@Composable
fun ActionButton(
    type: ActionButtonType,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: ActionButtonSize = ActionButtonSize.Large,
    enabled: Boolean = true,
) {
    val colors = AssembleTheme.colors
    val shape = AssembleTheme.shapes.pill
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) PressedScale else 1f, label = "actionButtonScale")

    val (container, content) = when (type) {
        ActionButtonType.Pass -> colors.actionPass to colors.passInk
        ActionButtonType.Assemble -> colors.actionAssemble to colors.onActionAssemble
    }
    val label = stringResource(
        when (type) {
            ActionButtonType.Pass -> R.string.action_pass
            ActionButtonType.Assemble -> R.string.action_assemble
        },
    )

    Box(
        modifier = modifier
            .size(size.diameter)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .assembleShadow(AssembleShadow.Action, shape, colors)
            .clip(shape)
            .background(container)
            .then(if (type == ActionButtonType.Pass) Modifier.border(PassBorderWidth, colors.border, shape) else Modifier)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClickLabel = label,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = when (type) {
                ActionButtonType.Pass -> AssembleIcons.Close
                ActionButtonType.Assemble -> AssembleIcons.Heart
            },
            contentDescription = label,
            tint = content,
            modifier = Modifier.size(size.iconSize),
        )
    }
}

@PreviewLightDark
@Composable
private fun ActionButtonPreview() {
    PreviewSurface {
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
            ActionButton(ActionButtonType.Pass, onClick = {})
            ActionButton(ActionButtonType.Assemble, onClick = {})
            ActionButton(ActionButtonType.Pass, onClick = {}, size = ActionButtonSize.Small)
            ActionButton(ActionButtonType.Assemble, onClick = {}, size = ActionButtonSize.Small)
        }
    }
}
