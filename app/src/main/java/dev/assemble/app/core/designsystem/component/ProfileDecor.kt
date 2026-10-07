package dev.assemble.app.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.assemble.app.core.designsystem.theme.AssembleColors
import dev.assemble.app.core.designsystem.theme.AssemblePalette
import dev.assemble.app.core.designsystem.theme.AssembleTheme
import dev.assemble.app.core.model.AvatarFrame
import dev.assemble.app.core.model.ProfileAccent
import dev.assemble.app.core.model.ProfileCover
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private val FrameStroke = 3.dp
private val FrameGap = 3.dp
private const val FramedAvatarInnerFraction = 0.86f
private const val BurstAvatarFraction = 0.72f
private const val BurstSpikes = 16
private const val BurstInnerRadius = 0.80f
private const val ComicRays = 24
private const val ComicRayAlpha = 0.14f
private const val ComicCenterX = 0.8f
private const val ComicCenterY = 0.3f
private const val HalftoneCoverAlpha = 0.35f
private const val NightGlowAlpha = 0.55f
private const val NightStars = 40
private const val NightStarSeed = 7
private const val NightStarMaxAlpha = 0.7f
private val NightStarRadius = 1.dp
private val NewsprintPaper = Color(0xFFF2EFE6)
private val NewsprintInk = Color(0xFFBFC4CC)
private val NewsprintLineHeight = 2.dp
private val NewsprintLineGap = 7.dp
private const val NewsprintMastheadFraction = 0.22f
private val BlueprintBlue = Color(0xFF1C4E8A)
private val BlueprintCell = 12.dp
private const val BlueprintLineAlpha = 0.22f
private const val CosmosGlowAlpha = 0.85f
private const val OrbitAvatarFraction = 0.7f
private const val InnerOrbitAlpha = 0.5f
private const val OrbitMoonAngle = -PI / 4
private const val OrbitMoonScale = 1.4f
private const val BoltBadgeFraction = 0.36f
private const val BoltBadgeInnerFraction = 0.82f

/** Hexágono "em pé": insígnias de conquista e moldura de avatar. */
val HexagonShape = GenericShape { size, _ ->
    moveTo(size.width / 2, 0f)
    lineTo(size.width, size.height * 0.25f)
    lineTo(size.width, size.height * 0.75f)
    lineTo(size.width / 2, size.height)
    lineTo(0f, size.height * 0.75f)
    lineTo(0f, size.height * 0.25f)
    close()
}

/** Escudo: topo com ombros e ponta embaixo. Moldura "Escudo". */
val ShieldShape = GenericShape { size, _ ->
    moveTo(size.width / 2, 0f)
    lineTo(size.width, size.height * 0.12f)
    lineTo(size.width, size.height * 0.55f)
    lineTo(size.width / 2, size.height)
    lineTo(0f, size.height * 0.55f)
    lineTo(0f, size.height * 0.12f)
    close()
}

fun ProfileAccent.color(colors: AssembleColors): Color = when (this) {
    ProfileAccent.Pink -> colors.actionAssemble
    ProfileAccent.Red -> colors.heroRed
    ProfileAccent.Blue -> colors.score
    ProfileAccent.Violet -> AssemblePalette.AccentViolet
    ProfileAccent.Gold -> AssemblePalette.AccentGold
    ProfileAccent.Emerald -> AssemblePalette.AccentEmerald
    ProfileAccent.Silver -> AssemblePalette.AccentSilver
}

/** Conteúdo sobre o destaque: midnight nos claros (azul, dourado, esmeralda, prata), branco nos demais. */
fun ProfileAccent.onColor(colors: AssembleColors): Color = when (this) {
    ProfileAccent.Blue, ProfileAccent.Gold, ProfileAccent.Emerald, ProfileAccent.Silver -> colors.onScore
    else -> Color.White
}

/** Capa do perfil, desenhada (sem imagens). Decorativa: nunca leva texto corrido por cima. */
@Composable
fun ProfileCoverArt(cover: ProfileCover, accent: ProfileAccent, modifier: Modifier = Modifier) {
    val colors = AssembleTheme.colors
    val accentColor = accent.color(colors)
    val art = when (cover) {
        ProfileCover.Energy -> Modifier.energyGradient(colors).halftone(color = colors.midnight)
        ProfileCover.Halftone -> Modifier
            .background(accentColor)
            .halftone(color = Color.White, dotRadius = 1.6.dp, spacing = 7.dp, alpha = HalftoneCoverAlpha)
        ProfileCover.Comic -> Modifier.background(accentColor).drawBehind { drawComicRays() }
        ProfileCover.Night -> Modifier
            .background(Brush.verticalGradient(listOf(colors.midnight, accentColor.copy(alpha = NightGlowAlpha))))
            .drawBehind { drawStars() }
        ProfileCover.Headline -> Modifier.background(NewsprintPaper).drawBehind { drawNewsprint(accentColor) }
        ProfileCover.Blueprint -> Modifier.background(BlueprintBlue).drawBehind { drawBlueprintGrid() }
        ProfileCover.Cosmos -> Modifier
            .background(Brush.radialGradient(listOf(accentColor.copy(alpha = CosmosGlowAlpha), colors.midnight)))
            .drawBehind { drawStars() }
    }
    Box(modifier.then(art))
}

private fun DrawScope.drawComicRays() {
    val center = Offset(size.width * ComicCenterX, size.height * ComicCenterY)
    val reach = size.maxDimension * 2
    val step = 2 * PI / ComicRays
    for (i in 0 until ComicRays step 2) {
        val a0 = i * step
        val a1 = a0 + step
        val path = Path().apply {
            moveTo(center.x, center.y)
            lineTo(center.x + (reach * cos(a0)).toFloat(), center.y + (reach * sin(a0)).toFloat())
            lineTo(center.x + (reach * cos(a1)).toFloat(), center.y + (reach * sin(a1)).toFloat())
            close()
        }
        drawPath(path, Color.White.copy(alpha = ComicRayAlpha))
    }
}

private fun DrawScope.drawStars() {
    // Semente fixa: as estrelas ficam no mesmo lugar a cada desenho.
    val random = Random(NightStarSeed)
    repeat(NightStars) {
        drawCircle(
            color = Color.White,
            radius = NightStarRadius.toPx() * (0.5f + random.nextFloat()),
            center = Offset(random.nextFloat() * size.width, random.nextFloat() * size.height * 0.8f),
            alpha = NightStarMaxAlpha * random.nextFloat(),
        )
    }
}

/** Página de jornal: faixa da manchete no destaque e linhas de texto. */
private fun DrawScope.drawNewsprint(accent: Color) {
    val mastheadHeight = size.height * NewsprintMastheadFraction
    drawRect(accent, size = Size(size.width, mastheadHeight))
    val line = NewsprintLineHeight.toPx()
    val step = line + NewsprintLineGap.toPx()
    var y = mastheadHeight + step
    while (y < size.height) {
        drawRect(NewsprintInk, topLeft = Offset(0f, y), size = Size(size.width, line))
        y += step
    }
}

/** Papel de planta técnica: grade branca fina sobre azul. */
private fun DrawScope.drawBlueprintGrid() {
    val cell = BlueprintCell.toPx()
    val line = Color.White.copy(alpha = BlueprintLineAlpha)
    var x = 0f
    while (x < size.width) {
        drawLine(line, Offset(x, 0f), Offset(x, size.height))
        x += cell
    }
    var y = 0f
    while (y < size.height) {
        drawLine(line, Offset(0f, y), Offset(size.width, y))
        y += cell
    }
}

/** Avatar do usuário com a moldura escolhida. [gapColor] separa a moldura da capa ou do fundo. */
@Composable
fun FramedAvatar(
    preset: AvatarPreset,
    frame: AvatarFrame,
    accent: ProfileAccent,
    size: Dp,
    modifier: Modifier = Modifier,
    gapColor: Color = AssembleTheme.colors.bg,
) {
    val accentColor = accent.color(AssembleTheme.colors)
    val pill = AssembleTheme.shapes.pill
    when (frame) {
        AvatarFrame.Simple -> UserAvatar(preset, modifier.border(FrameStroke, gapColor, pill), size = size)
        AvatarFrame.Ring -> Box(
            modifier.size(size).background(gapColor, pill).border(FrameStroke, accentColor, pill),
            contentAlignment = Alignment.Center,
        ) {
            UserAvatar(preset, size = size - (FrameStroke + FrameGap) * 2)
        }
        AvatarFrame.Hexagon -> Box(
            modifier.size(size).clip(HexagonShape).background(accentColor),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(size * FramedAvatarInnerFraction + FrameGap).clip(HexagonShape).background(gapColor))
            UserAvatar(preset, size = size * FramedAvatarInnerFraction, shape = HexagonShape)
        }
        AvatarFrame.Burst -> Box(modifier.size(size), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) { drawBurst(accentColor) }
            UserAvatar(
                preset,
                modifier = Modifier.border(FrameStroke, gapColor, pill),
                size = size * BurstAvatarFraction,
            )
        }
        AvatarFrame.Shield -> Box(
            modifier.size(size).clip(ShieldShape).background(accentColor),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(size * FramedAvatarInnerFraction + FrameGap).clip(ShieldShape).background(gapColor))
            UserAvatar(preset, size = size * FramedAvatarInnerFraction, shape = ShieldShape)
        }
        AvatarFrame.Cosmic -> Box(modifier.size(size), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) { drawOrbits(accentColor, FrameStroke.toPx()) }
            UserAvatar(
                preset,
                modifier = Modifier.border(FrameStroke, gapColor, pill),
                size = size * OrbitAvatarFraction,
            )
        }
        AvatarFrame.Lightning -> Box(modifier.size(size)) {
            Box(
                Modifier.fillMaxSize().background(gapColor, pill).border(FrameStroke, accentColor, pill),
                contentAlignment = Alignment.Center,
            ) {
                UserAvatar(preset, size = size - (FrameStroke + FrameGap) * 2)
            }
            val boltColor = accent.onColor(AssembleTheme.colors)
            Canvas(Modifier.size(size * BoltBadgeFraction).align(Alignment.BottomEnd)) {
                drawBoltBadge(accentColor, gapColor, boltColor)
            }
        }
    }
}

private fun DrawScope.drawBurst(color: Color) {
    val outer = size.minDimension / 2
    val inner = outer * BurstInnerRadius
    val step = PI / BurstSpikes
    val path = Path()
    for (i in 0 until BurstSpikes * 2) {
        val radius = if (i % 2 == 0) outer else inner
        val angle = i * step - PI / 2
        val x = center.x + (radius * cos(angle)).toFloat()
        val y = center.y + (radius * sin(angle)).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, color)
}

/** Dois anéis e uma "lua" no anel de fora. */
private fun DrawScope.drawOrbits(color: Color, stroke: Float) {
    val outer = size.minDimension / 2 - stroke / 2
    drawCircle(color, radius = outer, style = Stroke(stroke))
    drawCircle(color.copy(alpha = InnerOrbitAlpha), radius = outer - stroke * 2, style = Stroke(stroke / 2))
    val moon = Offset((outer * cos(OrbitMoonAngle)).toFloat(), (outer * sin(OrbitMoonAngle)).toFloat())
    drawCircle(color, radius = stroke * OrbitMoonScale, center = center + moon)
}

/** Selo redondo com um raio, no canto da moldura "Raio". */
private fun DrawScope.drawBoltBadge(color: Color, gapColor: Color, boltColor: Color) {
    drawCircle(gapColor)
    drawCircle(color, radius = size.minDimension / 2 * BoltBadgeInnerFraction)
    val w = size.width
    val h = size.height
    val bolt = Path().apply {
        moveTo(w * 0.56f, h * 0.18f)
        lineTo(w * 0.30f, h * 0.56f)
        lineTo(w * 0.48f, h * 0.56f)
        lineTo(w * 0.42f, h * 0.84f)
        lineTo(w * 0.70f, h * 0.44f)
        lineTo(w * 0.52f, h * 0.44f)
        close()
    }
    drawPath(bolt, boltColor)
}

@PreviewLightDark
@Composable
private fun ProfileDecorPreview() {
    PreviewSurface {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ProfileCover.entries.forEach { ProfileCoverArt(it, ProfileAccent.Violet, Modifier.fillMaxWidth().height(56.dp)) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AvatarFrame.entries.forEach { FramedAvatar(AvatarPreset.Energy, it, ProfileAccent.Gold, size = 64.dp) }
            }
        }
    }
}
