package dev.assemble.app.feature.discover

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.assemble.app.R
import dev.assemble.app.core.designsystem.component.SecondaryButton
import dev.assemble.app.core.designsystem.icon.AssembleIcons
import dev.assemble.app.core.designsystem.theme.AssembleTheme
import dev.assemble.app.core.designsystem.theme.rememberAnimationsEnabled
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.ZonedDateTime
import java.util.Locale

private const val RingDrawMillis = 1_000
private const val TickMillis = 1_000L
private const val StartAngle = -90f
private const val FullSweep = 360f
private const val SecondsPerMinute = 60
private const val SecondsPerHour = 3_600
private val RingSize = 120.dp
private val RingStroke = 8.dp
private val RingIconSize = 40.dp

/** Tempo até o próximo baralho: o baralho diário vira à meia-noite local. */
internal fun timeUntilNextDeck(now: ZonedDateTime): Duration {
    val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay(now.zone)
    return Duration.between(now, nextMidnight)
}

/** "07:42:15", sempre com dois dígitos. */
internal fun formatCountdown(remaining: Duration): String {
    val total = remaining.seconds.coerceAtLeast(0)
    return String.format(
        Locale.ROOT,
        "%02d:%02d:%02d",
        total / SecondsPerHour,
        total % SecondsPerHour / SecondsPerMinute,
        total % SecondsPerMinute,
    )
}

/**
 * Fim do baralho: um anel que se fecha, "Deck complete" e a contagem até os próximos personagens.
 * Substitui o estado vazio genérico; ajustar preferências continua disponível.
 */
@Composable
fun DeckComplete(onAdjustPreferences: () -> Unit, modifier: Modifier = Modifier) {
    val colors = AssembleTheme.colors
    val spacing = AssembleTheme.spacing
    var remaining by remember { mutableStateOf(timeUntilNextDeck(ZonedDateTime.now())) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(TickMillis)
            remaining = timeUntilNextDeck(ZonedDateTime.now())
        }
    }
    val animationsEnabled = rememberAnimationsEnabled()
    val ring = remember { Animatable(if (animationsEnabled) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (animationsEnabled) ring.animateTo(1f, tween(RingDrawMillis, easing = EaseOutCubic))
    }
    val ringBrush = Brush.sweepGradient(listOf(colors.heroRed, colors.logoPink, colors.heroRed))
    val hours = (remaining.seconds / SecondsPerHour).toInt()
    val minutes = (remaining.seconds % SecondsPerHour / SecondsPerMinute).toInt()
    val countdownDescription = stringResource(
        R.string.discover_next_deck_a11y,
        pluralStringResource(R.plurals.duration_hours, hours, hours),
        pluralStringResource(R.plurals.duration_minutes, minutes, minutes),
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = spacing.space6),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.space3),
    ) {
        Box(Modifier.size(RingSize), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val stroke = RingStroke.toPx()
                val arcSize = Size(size.width - stroke, size.height - stroke)
                val topLeft = Offset(stroke / 2, stroke / 2)
                drawArc(colors.border, 0f, FullSweep, useCenter = false, topLeft = topLeft, size = arcSize, style = Stroke(stroke))
                drawArc(
                    brush = ringBrush,
                    startAngle = StartAngle,
                    sweepAngle = FullSweep * ring.value,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
            }
            Icon(AssembleIcons.CompassFilled, contentDescription = null, tint = colors.accentText, modifier = Modifier.size(RingIconSize))
        }
        Text(
            text = stringResource(R.string.discover_deck_complete_title).uppercase(),
            style = AssembleTheme.typography.displayMd,
            color = colors.text,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = stringResource(R.string.discover_next_deck),
            style = AssembleTheme.typography.caption,
            color = colors.textMuted,
            textAlign = TextAlign.Center,
        )
        Text(
            text = formatCountdown(remaining),
            style = AssembleTheme.typography.h2,
            color = colors.accentText,
            // Lido por minuto, não a cada segundo.
            modifier = Modifier.clearAndSetSemantics { contentDescription = countdownDescription },
        )
        SecondaryButton(
            text = stringResource(R.string.discover_adjust_preferences),
            onClick = onAdjustPreferences,
            modifier = Modifier.padding(top = spacing.space2),
        )
    }
}
