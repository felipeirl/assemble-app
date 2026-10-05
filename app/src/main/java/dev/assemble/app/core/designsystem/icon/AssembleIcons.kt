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
    val Info: ImageVector by lazy {
        lineIcon("Info", circle(12f, 12f, 9.5f), "M12 11v6M12 7.5v.5")
    }

    // Menu lateral, configurações e conquistas: mesmo traço de 2dp.
    val Settings: ImageVector by lazy {
        lineIcon(
            "Settings",
            // Engrenagem de 8 dentes: raio 9.6 nas pontas, 7.2 na base.
            "M10.63 4.93L11 2.45L13 2.45L13.37 4.93L16.03 6.03L18.04 4.54L19.46 5.96L17.97 7.97L19.07 10.63" +
                "L21.55 11L21.55 13L19.07 13.37L17.97 16.03L19.46 18.04L18.04 19.46L16.03 17.97L13.37 19.07" +
                "L13 21.55L11 21.55L10.63 19.07L7.97 17.97L5.96 19.46L4.54 18.04L6.03 16.03L4.93 13.37" +
                "L2.45 13L2.45 11L4.93 10.63L6.03 7.97L4.54 5.96L5.96 4.54L7.97 6.03Z",
            circle(12f, 12f, 3f),
        )
    }
    val Help: ImageVector by lazy {
        lineIcon("Help", circle(12f, 12f, 9.5f), "M9.5 9.5a2.5 2.5 0 1 1 3.5 2.3c-.6.3-1 .8-1 1.5v.7", "M12 17v.5")
    }
    val LogOut: ImageVector by lazy {
        lineIcon("LogOut", "M10 20H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h4", "M15 16l4-4-4-4", "M19 12H9")
    }
    val Achievements: ImageVector by lazy {
        lineIcon(
            "Achievements",
            "M8 4.5h8V10a4 4 0 0 1-8 0z",
            "M8 6.5H4.5c0 2.4 1.5 4 3.8 4.3",
            "M16 6.5h3.5c0 2.4-1.5 4-3.8 4.3",
            "M12 14v3.5",
            "M8.5 20h7",
        )
    }
    val Theme: ImageVector by lazy {
        lineIcon("Theme", "M20 14.5A8.5 8.5 0 1 1 9.5 4a7 7 0 0 0 10.5 10.5z")
    }
    val HeartOutline: ImageVector by lazy {
        lineIcon("HeartOutline", "M12 20s-7.5-4.7-7.5-10A4.2 4.2 0 0 1 12 7.5a4.2 4.2 0 0 1 7.5 2.5c0 5.3-7.5 10-7.5 10z")
    }
    val Bell: ImageVector by lazy {
        lineIcon("Bell", "M6 16v-5a6 6 0 0 1 12 0v5l1.5 2h-15z", "M10 20.5a2 2 0 0 0 4 0")
    }
    val Volume: ImageVector by lazy {
        lineIcon("Volume", "M4 9.5v5h3.5l4.5 4v-13l-4.5 4z", "M15.5 9a4 4 0 0 1 0 6", "M18 6.5a8 8 0 0 1 0 11")
    }
    val Vibrate: ImageVector by lazy {
        lineIcon("Vibrate", "M8 4h8a1 1 0 0 1 1 1v14a1 1 0 0 1-1 1H8a1 1 0 0 1-1-1V5a1 1 0 0 1 1-1z", "M3.5 9v6", "M20.5 9v6")
    }
    val Reset: ImageVector by lazy {
        lineIcon("Reset", "M4.5 12a7.5 7.5 0 1 0 2.2-5.3", "M4.5 4.5V9H9")
    }
    val Eye: ImageVector by lazy {
        lineIcon("Eye", "M2.5 12s3.5-6.5 9.5-6.5 9.5 6.5 9.5 6.5-3.5 6.5-9.5 6.5S2.5 12 2.5 12z", circle(12f, 12f, 2.5f))
    }
    val EyeOff: ImageVector by lazy {
        lineIcon(
            "EyeOff",
            "M2.5 12s3.5-6.5 9.5-6.5 9.5 6.5 9.5 6.5-3.5 6.5-9.5 6.5S2.5 12 2.5 12z",
            circle(12f, 12f, 2.5f),
            "M4 4l16 16",
        )
    }
    val Trash: ImageVector by lazy {
        lineIcon("Trash", "M4 7h16", "M9 7V4.5h6V7", "M6 7l1 13h10l1-13", "M10 11v5.5M14 11v5.5")
    }
    val UserRemove: ImageVector by lazy {
        lineIcon("UserRemove", circle(10f, 8f, 3.5f), "M3.5 19.5c1-3 3.5-4.5 6.5-4.5s5.5 1.5 6.5 4.5", "M17 9l4 4M21 9l-4 4")
    }
    val Users: ImageVector by lazy {
        lineIcon(
            "Users",
            circle(9f, 8.5f, 3.2f),
            "M3 19.5c.9-3 3.2-4.5 6-4.5s5.1 1.5 6 4.5",
            "M15.5 5.6a3.2 3.2 0 0 1 0 5.8",
            "M17.5 15.3c1.7.6 2.9 2 3.5 4.2",
        )
    }
    val Shield: ImageVector by lazy {
        lineIcon("Shield", "M12 3l7.5 3v5.5c0 4.5-3.2 8-7.5 9.5-4.3-1.5-7.5-5-7.5-9.5V6z", "M9 12l2.2 2.2L15.5 10")
    }
    // Abas da barra inferior: o mesmo desenho, com traço mais fino (1,5dp).
    val TabCompass: ImageVector by lazy {
        lineIcon("TabCompass", circle(12f, 12f, 9.5f), "M15.5 8.5l-2 5-5 2 2-5z", strokeWidth = TabStrokeWidth)
    }
    val TabChat: ImageVector by lazy {
        lineIcon("TabChat", "M12 3a8.5 8.5 0 1 1-4 16l-4 1.2 1.2-3.8A8.5 8.5 0 0 1 12 3z", strokeWidth = TabStrokeWidth)
    }
    val TabProfile: ImageVector by lazy {
        lineIcon(
            "TabProfile",
            circle(12f, 12f, 9.5f),
            circle(12f, 9.5f, 3f),
            "M6 18.5c1.3-2.3 3.5-3.5 6-3.5s4.7 1.2 6 3.5",
            strokeWidth = TabStrokeWidth,
        )
    }
    val ChevronRight: ImageVector by lazy {
        lineIcon("ChevronRight", "M9.5 6l6 6-6 6")
    }
    val ChevronDown: ImageVector by lazy {
        lineIcon("ChevronDown", "M6 9.5l6 6 6-6")
    }
}

private const val ViewportSize = 24f
private const val LineStrokeWidth = 2f
private const val PassStrokeWidth = 3f
private const val TabStrokeWidth = 1.5f
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
