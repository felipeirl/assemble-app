package dev.assemble.app.feature.profile

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.assemble.app.R
import dev.assemble.app.core.designsystem.component.PrimaryButton
import dev.assemble.app.core.designsystem.icon.AssembleIcons
import dev.assemble.app.core.designsystem.theme.AssembleTheme
import dev.assemble.app.core.designsystem.theme.ThemeMode
import dev.assemble.app.core.media.CropMath
import dev.assemble.app.core.media.PhotoCrop

/** Diâmetro do círculo de recorte em fração do lado da área de ajuste. */
private const val CircleFraction = 0.84f
private const val MaskAlpha = 0.72f
private const val RingAlpha = 0.9f
private const val GridAlpha = 0.45f
private const val GridLines = 3
/** Toque duplo alterna entre o zoom mínimo e este. */
private const val DoubleTapZoom = 2f
private val ToolIconSize = 16.dp

/**
 * Ajuste da foto antes de salvar: arrastar move, pinça ou toque duplo dá zoom, "Girar" vira 90°.
 * A foto sempre cobre o círculo. Escura nos dois temas, como os editores de foto do sistema.
 */
@Composable
internal fun PhotoCropDialog(
    source: Bitmap,
    initial: PhotoCrop,
    onCancel: () -> Unit,
    onConfirm: (PhotoCrop) -> Unit,
) {
    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        AssembleTheme(themeMode = ThemeMode.Dark) {
            PhotoCropContent(source.asImageBitmapOnce(), initial, onCancel, onConfirm)
        }
    }
}

@Composable
private fun Bitmap.asImageBitmapOnce(): ImageBitmap = remember(this) { asImageBitmap() }

@Composable
internal fun PhotoCropContent(
    image: ImageBitmap,
    initial: PhotoCrop,
    onCancel: () -> Unit,
    onConfirm: (PhotoCrop) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AssembleTheme.colors
    val spacing = AssembleTheme.spacing
    val width = image.width
    val height = image.height
    var crop by remember(image) { mutableStateOf(CropMath.clamp(initial, width, height)) }
    var touching by remember { mutableStateOf(false) }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(bottom = spacing.space4),
        verticalArrangement = Arrangement.spacedBy(spacing.space3),
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = spacing.space2), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onCancel) { Text(stringResource(R.string.action_cancel), color = colors.textMuted) }
            Text(
                text = stringResource(R.string.photo_crop_title),
                style = AssembleTheme.typography.body.copy(fontWeight = FontWeight.SemiBold),
                color = colors.text,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = { crop = PhotoCrop() }) { Text(stringResource(R.string.photo_crop_reset), color = colors.accentText) }
        }

        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            val side = minOf(maxWidth, maxHeight)
            val diameter = with(LocalDensity.current) { side.toPx() } * CircleFraction
            val description = stringResource(R.string.photo_crop_area)
            Canvas(
                Modifier
                    .size(side)
                    .pointerInput(image, diameter) {
                        detectTransformGestures { centroid, pan, zoomChange, _ ->
                            val focusX = (centroid.x - size.width / 2f) / diameter
                            val focusY = (centroid.y - size.height / 2f) / diameter
                            val zoomed = CropMath.zoomAround(crop, crop.zoom * zoomChange, focusX, focusY, width, height)
                            crop = CropMath.pan(zoomed, pan.x / diameter, pan.y / diameter, width, height)
                        }
                    }
                    .pointerInput(image, diameter) {
                        detectTapGestures(onDoubleTap = { point ->
                            val target = if (crop.zoom < DoubleTapZoom) DoubleTapZoom else PhotoCrop.MIN_ZOOM
                            val focusX = (point.x - size.width / 2f) / diameter
                            val focusY = (point.y - size.height / 2f) / diameter
                            crop = CropMath.zoomAround(crop, target, focusX, focusY, width, height)
                        })
                    }
                    // Só observa: a grade de terços aparece enquanto há dedo na foto.
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            touching = true
                            do {
                                val event = awaitPointerEvent()
                            } while (event.changes.any { it.pressed })
                            touching = false
                        }
                    }
                    .semantics { contentDescription = description },
            ) {
                drawPhoto(image, crop, diameter)
                drawMask(diameter, maskColor = colors.bg, showGrid = touching)
            }
        }

        Text(
            text = stringResource(R.string.photo_crop_hint),
            style = AssembleTheme.typography.caption,
            color = colors.textMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            Modifier.fillMaxWidth().padding(horizontal = spacing.space4),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.space3),
        ) {
            val zoomLabel = stringResource(R.string.photo_crop_zoom)
            Slider(
                value = crop.zoom,
                onValueChange = { crop = CropMath.zoomAround(crop, it, 0f, 0f, width, height) },
                valueRange = PhotoCrop.MIN_ZOOM..PhotoCrop.MAX_ZOOM,
                colors = SliderDefaults.colors(
                    thumbColor = colors.actionAssemble,
                    activeTrackColor = colors.actionAssemble,
                    inactiveTrackColor = colors.border,
                ),
                modifier = Modifier.weight(1f).semantics { contentDescription = zoomLabel },
            )
            Text(
                text = stringResource(R.string.photo_crop_zoom_value, crop.zoom),
                style = AssembleTheme.typography.small.copy(fontWeight = FontWeight.SemiBold),
                color = colors.textMuted,
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing.space2, Alignment.CenterHorizontally)) {
            ToolButton(stringResource(R.string.photo_crop_rotate), AssembleIcons.Reset) { crop = CropMath.rotate(crop, width, height) }
            ToolButton(stringResource(R.string.photo_crop_center), icon = null) {
                crop = CropMath.clamp(crop.copy(offsetX = 0f, offsetY = 0f), width, height)
            }
        }
        PrimaryButton(
            text = stringResource(R.string.photo_crop_apply),
            onClick = { onConfirm(crop) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.space4),
        )
    }
}

@Composable
private fun ToolButton(label: String, icon: ImageVector?, onClick: () -> Unit) {
    val colors = AssembleTheme.colors
    val pill = AssembleTheme.shapes.pill
    Row(
        modifier = Modifier
            .clip(pill)
            .background(colors.surface)
            .border(1.dp, colors.border, pill)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = AssembleTheme.spacing.space4, vertical = AssembleTheme.spacing.space2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AssembleTheme.spacing.space2),
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = colors.text, modifier = Modifier.size(ToolIconSize))
        Text(label, style = AssembleTheme.typography.small.copy(fontWeight = FontWeight.SemiBold), color = colors.text)
    }
}

/** A mesma transformação do [dev.assemble.app.core.media.AvatarImage.encode], com o círculo de [diameter] px no centro. */
private fun DrawScope.drawPhoto(image: ImageBitmap, crop: PhotoCrop, diameter: Float) {
    val k = CropMath.scale(image.width, image.height, crop) * diameter
    withTransform({
        translate(center.x + crop.offsetX * diameter, center.y + crop.offsetY * diameter)
        rotate(QuarterDegrees * crop.quarterTurns, pivot = Offset.Zero)
        scale(k, k, pivot = Offset.Zero)
    }) {
        drawImage(
            image = image,
            dstOffset = IntOffset(-image.width / 2, -image.height / 2),
            dstSize = IntSize(image.width, image.height),
            filterQuality = FilterQuality.Medium,
        )
    }
}

/** Escurece fora do círculo; com o dedo na foto, mostra a grade de terços dentro dele. */
private fun DrawScope.drawMask(diameter: Float, maskColor: Color, showGrid: Boolean) {
    val radius = diameter / 2
    val circle = Rect(center, radius)
    val outside = Path().apply {
        fillType = PathFillType.EvenOdd
        addRect(Rect(Offset.Zero, size))
        addOval(circle)
    }
    drawPath(outside, maskColor.copy(alpha = MaskAlpha))
    drawCircle(Color.White.copy(alpha = RingAlpha), radius, style = Stroke(1.dp.toPx()))
    if (!showGrid) return
    clipPath(Path().apply { addOval(circle) }) {
        val line = Color.White.copy(alpha = GridAlpha)
        for (i in 1 until GridLines) {
            val x = circle.left + diameter * i / GridLines
            val y = circle.top + diameter * i / GridLines
            drawLine(line, Offset(x, circle.top), Offset(x, circle.bottom))
            drawLine(line, Offset(circle.left, y), Offset(circle.right, y))
        }
    }
}

private const val QuarterDegrees = 90f
