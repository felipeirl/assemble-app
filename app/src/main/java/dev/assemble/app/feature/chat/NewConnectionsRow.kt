package dev.assemble.app.feature.chat

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.assemble.app.R
import dev.assemble.app.core.designsystem.component.CharacterAvatar
import dev.assemble.app.core.designsystem.theme.AssembleTheme
import dev.assemble.app.core.designsystem.theme.rememberAnimationsEnabled

private const val RingTurnMillis = 3_000
private const val FullTurnDegrees = 360f
private val RingSize = 64.dp
private val RingStroke = 3.dp
private val AvatarInRingSize = 52.dp
private val ItemWidth = 72.dp

/**
 * Conexões em que você ainda não mandou mensagem, numa fileira de avatares com anel colorido girando.
 * O anel some da fileira depois da sua primeira mensagem: a conexão passa para a lista de conversas.
 */
@Composable
fun NewConnectionsRow(
    connections: List<ConversationSummary>,
    onOpen: (connectionId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = AssembleTheme.spacing
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(spacing.space2)) {
        Text(
            text = stringResource(R.string.chat_list_new_connections).uppercase(),
            style = AssembleTheme.typography.eyebrow,
            color = AssembleTheme.colors.textMuted,
            modifier = Modifier.padding(horizontal = spacing.space4),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = spacing.space4),
            horizontalArrangement = Arrangement.spacedBy(spacing.space3),
        ) {
            items(connections, key = { it.connectionId }) { connection ->
                NewConnectionItem(connection, onClick = { onOpen(connection.connectionId) })
            }
        }
    }
}

@Composable
private fun NewConnectionItem(connection: ConversationSummary, onClick: () -> Unit) {
    val colors = AssembleTheme.colors
    val description = stringResource(R.string.chat_list_new_connection_a11y, connection.name)
    val animationsEnabled = rememberAnimationsEnabled()
    val transition = rememberInfiniteTransition(label = "newConnectionRing")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = FullTurnDegrees,
        animationSpec = infiniteRepeatable(tween(RingTurnMillis, easing = LinearEasing), RepeatMode.Restart),
        label = "ringRotation",
    )
    val ringBrush = Brush.sweepGradient(listOf(colors.logoPink, colors.heroRed, colors.score, colors.logoPink))
    Column(
        modifier = Modifier
            .width(ItemWidth)
            .clip(AssembleTheme.shapes.md)
            .clickable(role = Role.Button, onClick = onClick)
            .clearAndSetSemantics {
                contentDescription = description
                role = Role.Button
                onClick { onClick(); true }
            }
            .padding(vertical = AssembleTheme.spacing.space1),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AssembleTheme.spacing.space1),
    ) {
        Box(Modifier.size(RingSize), contentAlignment = Alignment.Center) {
            Canvas(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { rotationZ = if (animationsEnabled) rotation else 0f },
            ) {
                val stroke = RingStroke.toPx()
                drawCircle(brush = ringBrush, radius = (size.minDimension - stroke) / 2, style = Stroke(stroke))
            }
            CharacterAvatar(name = connection.name, imageUrl = connection.imageUrl, size = AvatarInRingSize)
        }
        Text(
            text = connection.name,
            style = AssembleTheme.typography.small,
            color = colors.text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}
