package dev.assemble.app.core.designsystem.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * Ícones do Assemble: contorno 2dp (inativo) e sólido (ativo), grade 24×24.
 * Bússola, balão, pessoa, X, coração e estados vêm de docs/design-system/preview;
 * desfazer, hambúrguer, cadeado, voltar e enviar seguem o mesmo traço.
 * A cor é aplicada pelo `tint` do Icon.
 */
object AssembleIcons {
    val Compass: ImageVector by lazy {
        lineIcon("Compass", circle(12f, 12f, 9.5f), "M15.5 8.5l-2 5-5 2 2-5z")
    }
    val CompassFilled: ImageVector by lazy {
        filledIcon(
            "CompassFilled",
            evenOdd = "M12 1.5a10.5 10.5 0 1 1 0 21 10.5 10.5 0 0 1 0-21zm4.2 6.3l-6.3 2.1-2.1 6.3 6.3-2.1z",
            nonZero = listOf(circle(12f, 12f, 1.3f)),
        )
    }
    val Chat: ImageVector by lazy {
        lineIcon("Chat", "M12 3a8.5 8.5 0 1 1-4 16l-4 1.2 1.2-3.8A8.5 8.5 0 0 1 12 3z")
    }
    val ChatFilled: ImageVector by lazy {
        filledIcon("ChatFilled", nonZero = listOf("M12 2a9.5 9.5 0 1 1-4.3 18L3 21.5l1.5-4.5A9.5 9.5 0 0 1 12 2z"))
    }
    val Profile: ImageVector by lazy {
        lineIcon(
            "Profile",
            circle(12f, 12f, 9.5f),
            circle(12f, 9.5f, 3f),
            "M6 18.5c1.3-2.3 3.5-3.5 6-3.5s4.7 1.2 6 3.5",
        )
    }
    val ProfileFilled: ImageVector by lazy {
        filledIcon(
            "ProfileFilled",
            evenOdd = "M12 1.5a10.5 10.5 0 1 1 0 21 10.5 10.5 0 0 1 0-21zM12 6a3.5 3.5 0 1 0 0 7 3.5 3.5 0 0 0 0-7z" +
                "m-6.2 12.6A9 9 0 0 0 18.2 18.6C16.8 16.3 14.6 15 12 15s-4.8 1.3-6.2 3.6z",
        )
    }
    val Close: ImageVector by lazy {
        lineIcon("Close", "M6 6l12 12M18 6L6 18", strokeWidth = PassStrokeWidth)
    }
    val Heart: ImageVector by lazy {
        filledIcon(
            "Heart",
            nonZero = listOf("M12 21s-8.5-5.3-8.5-11.2A4.8 4.8 0 0 1 12 7a4.8 4.8 0 0 1 8.5 2.8C20.5 15.7 12 21 12 21z"),
        )
    }
    val Undo: ImageVector by lazy {
        lineIcon("Undo", "M9 14L4 9l5-5", "M4 9h10.5a5.5 5.5 0 0 1 0 11H11")
    }
    val Menu: ImageVector by lazy {
        lineIcon("Menu", "M4 7h16M4 12h16M4 17h16")
    }
    val Lock: ImageVector by lazy {
        lineIcon(
            "Lock",
            "M7 10.5h10a2 2 0 0 1 2 2v6a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2v-6a2 2 0 0 1 2-2z",
            "M8 10.5V7.5a4 4 0 0 1 8 0v3",
        )
    }
    val Back: ImageVector by lazy {
        lineIcon("Back", "M19 12H5", "M11 6l-6 6 6 6")
    }
    val Send: ImageVector by lazy {
        lineIcon("Send", "M4 11.5L20 4l-7.5 16-2.5-6z", "M10 14L20 4")
    }
    val Unavailable: ImageVector by lazy {
        lineIcon("Unavailable", circle(12f, 12f, 9.5f), "M8 12h8")
    }
    val Error: ImageVector by lazy {
        lineIcon("Error", circle(12f, 12f, 9.5f), "M12 7v6M12 16.5v.5")
    }
}

private const val ViewportSize = 24f
private const val LineStrokeWidth = 2f
private const val PassStrokeWidth = 3f
private val IconSize = 24.dp

/** Círculo como path SVG (dois arcos). */
private fun circle(cx: Float, cy: Float, r: Float): String =
    "M${cx - r} ${cy}a$r $r 0 1 0 ${2 * r} 0a$r $r 0 1 0 ${-2 * r} 0z"

private fun iconBuilder(name: String) = ImageVector.Builder(
    name = name,
    defaultWidth = IconSize,
    defaultHeight = IconSize,
    viewportWidth = ViewportSize,
    viewportHeight = ViewportSize,
)

private fun lineIcon(name: String, vararg paths: String, strokeWidth: Float = LineStrokeWidth): ImageVector {
    val builder = iconBuilder(name)
    paths.forEach { path ->
        builder.addPath(
            pathData = addPathNodes(path),
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = strokeWidth,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
    }
    return builder.build()
}

private fun filledIcon(name: String, evenOdd: String? = null, nonZero: List<String> = emptyList()): ImageVector {
    val builder = iconBuilder(name)
    evenOdd?.let {
        builder.addPath(pathData = addPathNodes(it), fill = SolidColor(Color.Black), pathFillType = PathFillType.EvenOdd)
    }
    nonZero.forEach {
        builder.addPath(pathData = addPathNodes(it), fill = SolidColor(Color.Black))
    }
    return builder.build()
}
