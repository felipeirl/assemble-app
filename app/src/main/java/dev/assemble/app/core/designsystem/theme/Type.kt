package dev.assemble.app.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import dev.assemble.app.R

val BarlowCondensed = FontFamily(
    Font(R.font.barlow_condensed_extrabold_italic, FontWeight.ExtraBold, FontStyle.Italic),
)

private fun interFont(weight: FontWeight) = Font(
    resId = R.font.inter_variable,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

val Inter = FontFamily(
    interFont(FontWeight.Normal),
    interFont(FontWeight.Medium),
    interFont(FontWeight.SemiBold),
    interFont(FontWeight.Bold),
)

/**
 * Estilos do design system. Display e eyebrow são em caixa alta:
 * converta o texto com `uppercase()` ao usar (TextStyle não transforma caixa).
 */
@Immutable
data class AssembleTypography(
    val displayXl: TextStyle,
    val displayMd: TextStyle,
    val h1: TextStyle,
    val h2: TextStyle,
    val body: TextStyle,
    val caption: TextStyle,
    val eyebrow: TextStyle,
)

val DefaultAssembleTypography = AssembleTypography(
    displayXl = TextStyle(
        fontFamily = BarlowCondensed,
        fontWeight = FontWeight.ExtraBold,
        fontStyle = FontStyle.Italic,
        fontSize = 64.sp,
        lineHeight = 58.sp,
    ),
    displayMd = TextStyle(
        fontFamily = BarlowCondensed,
        fontWeight = FontWeight.ExtraBold,
        fontStyle = FontStyle.Italic,
        fontSize = 28.sp,
        lineHeight = 28.sp,
    ),
    h1 = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 40.sp),
    h2 = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 32.sp),
    body = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    caption = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    eyebrow = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.12.em,
    ),
)

/** Mapeia os estilos do design system para o Typography do M3, para componentes M3 herdarem Inter. */
internal fun AssembleTypography.toMaterialTypography(): Typography = Typography(
    displayLarge = displayXl,
    displayMedium = displayMd,
    displaySmall = displayMd,
    headlineLarge = h1,
    headlineMedium = h2,
    headlineSmall = h2,
    titleLarge = h2,
    titleMedium = body.copy(fontWeight = FontWeight.SemiBold),
    titleSmall = caption.copy(fontWeight = FontWeight.SemiBold),
    bodyLarge = body,
    bodyMedium = caption,
    bodySmall = caption,
    labelLarge = caption.copy(fontWeight = FontWeight.SemiBold),
    labelMedium = eyebrow.copy(letterSpacing = 0.sp),
    labelSmall = eyebrow.copy(letterSpacing = 0.sp),
)

val LocalAssembleTypography = staticCompositionLocalOf { DefaultAssembleTypography }
