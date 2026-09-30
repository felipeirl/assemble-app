package dev.assemble.app.core.designsystem.component

import android.os.Build
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import dev.assemble.app.R
import dev.assemble.app.core.designsystem.icon.AssembleIcons
import dev.assemble.app.core.designsystem.theme.AssembleTheme
import dev.assemble.app.core.designsystem.theme.rememberAnimationsEnabled

private const val UnlockMillis = 400
private const val FallbackOverlayAlpha = 0.9f
private const val LockSpinDegrees = 90f
private val LockedBlur = 12.dp
private val LockIconSize = 32.dp

/**
 * Conteúdo do perfil bloqueado até existir conexão: blur de 12dp (API 31+) ou
 * sobreposição surface a 90% (API < 31), com cadeado e aviso.
 * Quando [locked] muda para false, desbloqueia em 400 ms (blur → 0, cadeado gira e some).
 */
@Composable
fun LockedSection(
    locked: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val animationsEnabled = rememberAnimationsEnabled()
    val spec = tween<Float>(if (animationsEnabled) UnlockMillis else 0)
    val blurRadius by animateDpAsState(
        targetValue = if (locked) LockedBlur else 0.dp,
        animationSpec = tween(if (animationsEnabled) UnlockMillis else 0),
        label = "unlockBlur",
    )
    val lockAlpha by animateFloatAsState(if (locked) 1f else 0f, spec, label = "lockAlpha")
    val lockRotation by animateFloatAsState(if (locked) 0f else LockSpinDegrees, spec, label = "lockRotation")
    val supportsBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val lockedMessage = stringResource(R.string.locked_profile)
    Box(
        modifier = modifier
            .clip(AssembleTheme.shapes.md)
            .then(if (locked) Modifier.clearAndSetSemantics { contentDescription = lockedMessage } else Modifier),
    ) {
        Box(if (supportsBlur) Modifier.blur(blurRadius) else Modifier) {
            content()
        }
        if (lockAlpha > 0f) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer { alpha = lockAlpha }
                    .then(
                        if (supportsBlur) {
                            Modifier
                        } else {
                            Modifier.background(AssembleTheme.colors.surface.copy(alpha = FallbackOverlayAlpha))
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                LockBadge(rotation = lockRotation, message = lockedMessage)
            }
        }
    }
}

@Composable
private fun LockBadge(rotation: Float, message: String) {
    val spacing = AssembleTheme.spacing
    Column(
        modifier = Modifier.padding(spacing.space4),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.space2),
    ) {
        Icon(
            imageVector = AssembleIcons.Lock,
            contentDescription = null,
            tint = AssembleTheme.colors.text,
            modifier = Modifier
                .size(LockIconSize)
                .graphicsLayer { rotationZ = rotation },
        )
        Text(
            text = message,
            style = AssembleTheme.typography.caption.copy(fontWeight = FontWeight.SemiBold),
            color = AssembleTheme.colors.text,
            textAlign = TextAlign.Center,
        )
    }
}

@PreviewLightDark
@Composable
private fun LockedSectionPreview() {
    PreviewSurface {
        LockedSection(locked = true, modifier = Modifier.fillMaxWidth()) {
            Column(
                Modifier.fillMaxWidth().background(AssembleTheme.colors.surface).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Bio", style = AssembleTheme.typography.h2, color = AssembleTheme.colors.text)
                Text(
                    "Lorem ipsum placeholder for preview only.",
                    style = AssembleTheme.typography.body,
                    color = AssembleTheme.colors.text,
                )
            }
        }
    }
}
