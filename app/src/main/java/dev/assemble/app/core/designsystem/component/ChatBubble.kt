package dev.assemble.app.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import dev.assemble.app.R
import dev.assemble.app.core.designsystem.theme.AssembleTheme

enum class ChatAuthor { User, Ai }

private const val MaxBubbleWidthFraction = 0.8f
private val BubbleRadius = 16.dp
private val BubbleTailRadius = 4.dp

internal fun bubbleShape(author: ChatAuthor): Shape = when (author) {
    ChatAuthor.User -> RoundedCornerShape(BubbleRadius, BubbleRadius, BubbleTailRadius, BubbleRadius)
    ChatAuthor.Ai -> RoundedCornerShape(BubbleRadius, BubbleRadius, BubbleRadius, BubbleTailRadius)
}

/**
 * Bolha de mensagem. Usuário: direita, action-assemble, texto branco.
 * IA: esquerda, ai-surface, sempre com o selo "AI-generated · fictional".
 */
@Composable
fun ChatBubble(
    text: String,
    author: ChatAuthor,
    modifier: Modifier = Modifier,
) {
    ChatBubbleContainer(author = author, modifier = modifier) {
        Text(
            text = text,
            style = AssembleTheme.typography.body,
            color = when (author) {
                ChatAuthor.User -> AssembleTheme.colors.onActionAssemble
                ChatAuthor.Ai -> AssembleTheme.colors.text
            },
        )
    }
}

/** Alinhamento, largura máxima (80%), forma e cor da bolha; o selo de IA vem antes do conteúdo. */
@Composable
internal fun ChatBubbleContainer(
    author: ChatAuthor,
    modifier: Modifier = Modifier,
    showAiLabel: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colors = AssembleTheme.colors
    val spacing = AssembleTheme.spacing
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .align(if (author == ChatAuthor.User) Alignment.CenterEnd else Alignment.CenterStart)
                .widthIn(max = maxWidth * MaxBubbleWidthFraction)
                .background(
                    color = if (author == ChatAuthor.User) colors.actionAssemble else colors.aiSurface,
                    shape = bubbleShape(author),
                )
                .padding(horizontal = spacing.space4, vertical = spacing.space3),
            verticalArrangement = Arrangement.spacedBy(spacing.space2),
        ) {
            if (author == ChatAuthor.Ai && showAiLabel) AiLabelChip()
            content()
        }
    }
}

/** Selo obrigatório em toda resposta da IA. */
@Composable
fun AiLabelChip(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.ai_label),
        style = AssembleTheme.typography.small.copy(fontWeight = FontWeight.SemiBold),
        color = AssembleTheme.colors.textMuted,
        modifier = modifier
            .background(AssembleTheme.colors.aiSurface, AssembleTheme.shapes.pill)
            .padding(horizontal = AssembleTheme.spacing.space2, vertical = 2.dp),
    )
}

@PreviewLightDark
@Composable
private fun ChatBubblePreview() {
    PreviewSurface {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ChatBubble("What got you into science?", ChatAuthor.User)
            ChatBubble(
                "Honestly? Curiosity and a very radioactive field trip. After that, chemistry felt personal.",
                ChatAuthor.Ai,
            )
            ChatBubble("Ha. Do you still like it?", ChatAuthor.User)
        }
    }
}
