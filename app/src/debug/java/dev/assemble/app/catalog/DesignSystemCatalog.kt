package dev.assemble.app.catalog

import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.assemble.app.R
import dev.assemble.app.core.designsystem.component.energyGradient
import dev.assemble.app.core.designsystem.component.halftone
import dev.assemble.app.core.designsystem.icon.AssembleIcons
import dev.assemble.app.core.designsystem.theme.AssembleColors
import dev.assemble.app.core.designsystem.theme.AssembleShadow
import dev.assemble.app.core.designsystem.theme.AssembleTheme
import dev.assemble.app.core.designsystem.theme.ThemeMode
import dev.assemble.app.core.designsystem.theme.assembleShadow

private val SwatchSize = 56.dp
private val TileSize = 72.dp
private val IconPreviewSize = 28.dp
private val GradientHeight = 160.dp
private val LogoHeight = 64.dp

@Composable
fun DesignSystemCatalog(themeMode: ThemeMode, onThemeModeChange: (ThemeMode) -> Unit) {
    val spacing = AssembleTheme.spacing
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(AssembleTheme.colors.bg),
        contentPadding = WindowInsets.safeDrawing.asPaddingValues(),
        verticalArrangement = Arrangement.spacedBy(spacing.space6),
    ) {
        item { Header(themeMode, onThemeModeChange) }
        item { Section(R.string.catalog_section_colors) { ColorSwatches(AssembleTheme.colors) } }
        item { Section(R.string.catalog_section_type) { TypeSamples() } }
        item { Section(R.string.catalog_section_spacing) { SpacingSamples() } }
        item { Section(R.string.catalog_section_radii) { RadiusSamples() } }
        item { Section(R.string.catalog_section_shadows) { ShadowSamples() } }
        item { Section(R.string.catalog_section_icons) { IconSamples() } }
        item { Section(R.string.catalog_section_gradient) { GradientSample() } }
        item { Section(R.string.catalog_section_logo) { LogoSample() } }
    }
}

@Composable
private fun Header(themeMode: ThemeMode, onThemeModeChange: (ThemeMode) -> Unit) {
    val options = listOf(
        ThemeMode.Light to R.string.catalog_theme_light,
        ThemeMode.Dark to R.string.catalog_theme_dark,
    )
    Column(
        modifier = Modifier.padding(horizontal = AssembleTheme.spacing.space4),
        verticalArrangement = Arrangement.spacedBy(AssembleTheme.spacing.space3),
    ) {
        Text(
            text = stringResource(R.string.catalog_title),
            style = AssembleTheme.typography.h1,
            color = AssembleTheme.colors.text,
        )
        SingleChoiceSegmentedButtonRow {
            options.forEachIndexed { index, (mode, label) ->
                SegmentedButton(
                    selected = themeMode == mode || (themeMode == ThemeMode.System && AssembleTheme.colors.isDark == (mode == ThemeMode.Dark)),
                    onClick = { onThemeModeChange(mode) },
                    shape = SegmentedButtonDefaults.itemShape(index, options.size),
                ) { Text(stringResource(label)) }
            }
        }
    }
}

@Composable
private fun Section(@StringRes title: Int, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.padding(horizontal = AssembleTheme.spacing.space4),
        verticalArrangement = Arrangement.spacedBy(AssembleTheme.spacing.space3),
    ) {
        Text(
            text = stringResource(title).uppercase(),
            style = AssembleTheme.typography.eyebrow,
            color = AssembleTheme.colors.textMuted,
        )
        content()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColorSwatches(colors: AssembleColors) {
    val tokens = listOf(
        "bg" to colors.bg, "surface" to colors.surface, "text" to colors.text,
        "text-muted" to colors.textMuted, "border" to colors.border, "action-pass" to colors.actionPass,
        "pass-ink" to colors.passInk, "action-assemble" to colors.actionAssemble, "accent-text" to colors.accentText,
        "score" to colors.score, "on-score" to colors.onScore, "ai-surface" to colors.aiSurface,
        "error" to colors.error, "hero-red" to colors.heroRed, "deep-red" to colors.deepRed,
        "logo-red" to colors.logoRed, "logo-pink" to colors.logoPink, "midnight" to colors.midnight,
    )
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(AssembleTheme.spacing.space3),
        verticalArrangement = Arrangement.spacedBy(AssembleTheme.spacing.space3),
    ) {
        tokens.forEach { (name, color) -> Swatch(name, color) }
    }
}

@Composable
private fun Swatch(name: String, color: Color) {
    Column(
        modifier = Modifier.width(TileSize),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AssembleTheme.spacing.space1),
    ) {
        Box(
            Modifier
                .size(SwatchSize)
                .background(color, AssembleTheme.shapes.sm)
                .border(1.dp, AssembleTheme.colors.border, AssembleTheme.shapes.sm),
        )
        Text(text = name, style = AssembleTheme.typography.caption, color = AssembleTheme.colors.textMuted)
    }
}

@Composable
private fun TypeSamples() {
    val type = AssembleTheme.typography
    val samples: List<Pair<TextStyle, Int>> = listOf(
        type.displayXl to R.string.catalog_sample_display_xl,
        type.displayMd to R.string.catalog_sample_display_md,
        type.h1 to R.string.catalog_sample_h1,
        type.h2 to R.string.catalog_sample_h2,
        type.body to R.string.catalog_sample_body,
        type.caption to R.string.catalog_sample_caption,
        type.eyebrow to R.string.catalog_sample_eyebrow,
    )
    Column(verticalArrangement = Arrangement.spacedBy(AssembleTheme.spacing.space2)) {
        samples.forEach { (style, text) ->
            Text(text = stringResource(text), style = style, color = AssembleTheme.colors.text)
        }
    }
}

@Composable
private fun SpacingSamples() {
    val s = AssembleTheme.spacing
    val steps = listOf(
        "space-1" to s.space1, "space-2" to s.space2, "space-3" to s.space3, "space-4" to s.space4,
        "space-5" to s.space5, "space-6" to s.space6, "space-8" to s.space8,
    )
    Column(verticalArrangement = Arrangement.spacedBy(s.space2)) {
        steps.forEach { (name, value) -> SpacingBar(name, value) }
    }
}

@Composable
private fun SpacingBar(name: String, value: Dp) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = name,
            style = AssembleTheme.typography.caption,
            color = AssembleTheme.colors.textMuted,
            modifier = Modifier.width(TileSize),
        )
        Box(Modifier.width(value).height(AssembleTheme.spacing.space4).background(AssembleTheme.colors.actionAssemble))
    }
}

@Composable
private fun RadiusSamples() {
    val shapes = AssembleTheme.shapes
    Row(horizontalArrangement = Arrangement.spacedBy(AssembleTheme.spacing.space4)) {
        listOf("sm" to shapes.sm, "md" to shapes.md, "lg" to shapes.lg, "pill" to shapes.pill).forEach { (name, shape) ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier
                        .size(SwatchSize)
                        .background(AssembleTheme.colors.surface, shape)
                        .border(1.dp, AssembleTheme.colors.border, shape),
                )
                Text(text = name, style = AssembleTheme.typography.caption, color = AssembleTheme.colors.textMuted)
            }
        }
    }
}

@Composable
private fun ShadowSamples() {
    val colors = AssembleTheme.colors
    Row(horizontalArrangement = Arrangement.spacedBy(AssembleTheme.spacing.space6)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .size(TileSize)
                    .assembleShadow(AssembleShadow.Card, AssembleTheme.shapes.lg, colors)
                    .background(colors.surface, AssembleTheme.shapes.lg),
            )
            Text(stringResource(R.string.catalog_shadow_card), style = AssembleTheme.typography.caption, color = colors.textMuted)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .size(TileSize)
                    .assembleShadow(AssembleShadow.Action, AssembleTheme.shapes.pill, colors)
                    .background(colors.actionAssemble, AssembleTheme.shapes.pill),
            )
            Text(stringResource(R.string.catalog_shadow_action), style = AssembleTheme.typography.caption, color = colors.textMuted)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IconSamples() {
    val icons: List<ImageVector> = with(AssembleIcons) {
        listOf(
            Compass, CompassFilled, Chat, ChatFilled, Profile, ProfileFilled, Close, Heart,
            Undo, Menu, Lock, Back, Send, Unavailable, Error,
        )
    }
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(AssembleTheme.spacing.space4),
        verticalArrangement = Arrangement.spacedBy(AssembleTheme.spacing.space4),
    ) {
        icons.forEach { icon ->
            Icon(
                imageVector = icon,
                contentDescription = icon.name,
                tint = AssembleTheme.colors.text,
                modifier = Modifier.size(IconPreviewSize),
            )
        }
    }
}

@Composable
private fun GradientSample() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(GradientHeight)
            .energyGradient(AssembleTheme.colors, AssembleTheme.shapes.lg)
            .halftone(color = AssembleTheme.colors.midnight),
    )
}

@Composable
private fun LogoSample() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(AssembleTheme.colors.midnight, AssembleTheme.shapes.lg)
            .padding(AssembleTheme.spacing.space5),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_assemble_logo),
            contentDescription = stringResource(R.string.app_name),
            modifier = Modifier.height(LogoHeight),
        )
    }
}

@Preview(name = "Light", heightDp = 2000)
@Composable
private fun DesignSystemCatalogLightPreview() {
    AssembleTheme(ThemeMode.Light) { DesignSystemCatalog(ThemeMode.Light) {} }
}

@Preview(name = "Dark", heightDp = 2000)
@Composable
private fun DesignSystemCatalogDarkPreview() {
    AssembleTheme(ThemeMode.Dark) { DesignSystemCatalog(ThemeMode.Dark) {} }
}
