package dev.assemble.app.core.designsystem.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

private val RadiusSm = RoundedCornerShape(8.dp)
private val RadiusMd = RoundedCornerShape(16.dp)
private val RadiusLg = RoundedCornerShape(24.dp)

/** Raios do design system: sm (chips), md (sheets, bolhas), lg (cards, painéis), pill. */
@Immutable
data class AssembleShapes(
    val sm: Shape = RadiusSm,
    val md: Shape = RadiusMd,
    val lg: Shape = RadiusLg,
    val pill: Shape = CircleShape,
)

internal val AssembleMaterialShapes = Shapes(
    extraSmall = RadiusSm,
    small = RadiusSm,
    medium = RadiusMd,
    large = RadiusLg,
    extraLarge = RadiusLg,
)

val LocalAssembleShapes = staticCompositionLocalOf { AssembleShapes() }

private val CardElevation = 6.dp
private val ActionElevation = 8.dp

enum class AssembleShadow { Card, Action }

/** Sombra dos tokens shadow-card / shadow-action, com a cor do tema. */
fun Modifier.assembleShadow(kind: AssembleShadow, shape: Shape, colors: AssembleColors): Modifier {
    val color = when (kind) {
        AssembleShadow.Card -> colors.shadowCard
        AssembleShadow.Action -> colors.shadowAction
    }
    val elevation = when (kind) {
        AssembleShadow.Card -> CardElevation
        AssembleShadow.Action -> ActionElevation
    }
    return shadow(elevation = elevation, shape = shape, clip = false, ambientColor = color, spotColor = color)
}
