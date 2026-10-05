package dev.assemble.app.feature.character

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import dev.assemble.app.R
import dev.assemble.app.core.designsystem.component.CharacterAvatar
import dev.assemble.app.core.designsystem.component.TraitChip
import dev.assemble.app.core.designsystem.theme.AssembleTheme
import dev.assemble.app.core.model.DataSource
import dev.assemble.app.core.model.Powerstats
import dev.assemble.app.core.ui.traitLabel
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val TabMinHeight = 40.dp
private val GraphHeight = 280.dp
private val GraphCenterAvatar = 64.dp
private val GraphNodeAvatar = 44.dp
private val GraphLabelWidth = 76.dp
private val GraphLabelGap = 12.dp
private val GraphEdgeInset = 44.dp
private val GraphLineWidth = 2.dp
private val LegendDot = 10.dp
private const val NotConnectedAlpha = 0.45f
private val DashOn = 6.dp
private val DashOff = 6.dp

enum class CharacterTab(@StringRes val title: Int) {
    Overview(R.string.character_tab_overview),
    Attributes(R.string.character_tab_attributes),
    Connections(R.string.character_tab_connections),
}

/** Abas em pílula. [tabs] já vem sem as que não têm conteúdo. */
@Composable
internal fun CharacterTabs(tabs: List<CharacterTab>, selected: CharacterTab, onSelect: (CharacterTab) -> Unit) {
    val colors = AssembleTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(AssembleTheme.shapes.pill)
            .background(colors.bg)
            .selectableGroup(),
    ) {
        tabs.forEach { tab ->
            val isSelected = tab == selected
            Box(
                Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = TabMinHeight)
                    .clip(AssembleTheme.shapes.pill)
                    .background(if (isSelected) colors.actionAssemble else Color.Transparent)
                    .selectable(selected = isSelected, role = Role.Tab, onClick = { onSelect(tab) }),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stringResource(tab.title),
                    style = AssembleTheme.typography.small.copy(fontWeight = FontWeight.SemiBold),
                    color = if (isSelected) colors.onActionAssemble else colors.textMuted,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun OverviewTab(content: CharacterProfileUiState.Content) {
    val colors = AssembleTheme.colors
    val spacing = AssembleTheme.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.space4)) {
        if (content.whyYouMatch.isNotEmpty()) {
            SectionCard(stringResource(R.string.why_you_match)) {
                content.whyYouMatch.forEachIndexed { index, item ->
                    if (index > 0) CardDivider()
                    Column(verticalArrangement = Arrangement.spacedBy(spacing.space2)) {
                        Text(
                            stringResource(item.category).uppercase(),
                            style = AssembleTheme.typography.eyebrow,
                            color = colors.textMuted,
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(spacing.space2),
                            verticalArrangement = Arrangement.spacedBy(spacing.space2),
                        ) {
                            item.traits.forEach { TraitChip(label = stringResource(traitLabel(it))) }
                        }
                    }
                }
            }
        }
        if (content.facts.isNotEmpty()) {
            SectionCard(stringResource(R.string.profile_about)) {
                content.facts.forEachIndexed { index, fact ->
                    if (index > 0) CardDivider()
                    FactRow(fact)
                }
            }
        }
    }
}

@Composable
private fun FactRow(fact: ProfileFact) {
    val traitLabels = fact.traits.map { stringResource(traitLabel(it)) }
    val value = fact.text ?: fact.valueRes?.let { stringResource(it) } ?: traitLabels.joinToString()
    Column(verticalArrangement = Arrangement.spacedBy(AssembleTheme.spacing.space1)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(fact.label).uppercase(),
                style = AssembleTheme.typography.eyebrow,
                color = AssembleTheme.colors.textMuted,
            )
            Spacer(Modifier.weight(1f))
            SourceTag(fact.source)
        }
        Text(value, style = AssembleTheme.typography.body, color = AssembleTheme.colors.text)
    }
}

/** Selo da fonte de um dado (Comic Vine, Superhero API…). */
@Composable
internal fun SourceTag(source: DataSource) {
    Text(
        text = stringResource(source.label),
        style = AssembleTheme.typography.small,
        color = AssembleTheme.colors.textMuted,
        modifier = Modifier
            .border(1.dp, AssembleTheme.colors.border, AssembleTheme.shapes.pill)
            .padding(horizontal = AssembleTheme.spacing.space2, vertical = 2.dp),
    )
}

@Composable
internal fun SourcesLabel(sources: List<DataSource>) {
    Text(
        text = stringResource(R.string.sources_label, sources.map { stringResource(it.label) }.joinToString()),
        style = AssembleTheme.typography.small,
        color = AssembleTheme.colors.textMuted,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AttributesTab(content: CharacterProfileUiState.Content) {
    val colors = AssembleTheme.colors
    val spacing = AssembleTheme.spacing
    var compareId by rememberSaveable { mutableStateOf<String?>(null) }
    val compare = content.compareOptions.find { it.characterId == compareId }
    Column(verticalArrangement = Arrangement.spacedBy(spacing.space4)) {
        val stats = content.powerstats
        if (stats != null) {
            SectionCard(stringResource(R.string.attributes_powerstats), source = DataSource.SuperheroApi) {
            val labels = StatLabels.map { stringResource(it) }
            val description = labels.zip(stats.values)
                .map { (label, value) -> stringResource(R.string.attributes_stat_value, label, value) }
                .joinToString()
            RadarChart(
                values = stats.values,
                labels = labels,
                color = colors.actionAssemble,
                compareValues = compare?.powerstats?.values,
                compareColor = colors.score,
                description = description,
                modifier = Modifier.fillMaxWidth().aspectRatio(1f),
            )
            StatGrid(labels, stats, compare, content.name)
            if (content.compareOptions.isNotEmpty()) {
                Text(stringResource(R.string.attributes_compare), style = AssembleTheme.typography.caption, color = colors.textMuted)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(spacing.space2),
                    verticalArrangement = Arrangement.spacedBy(spacing.space2),
                ) {
                    content.compareOptions.forEach { option ->
                        TraitChip(
                            label = option.name,
                            selected = option.characterId == compareId,
                            onSelectedChange = { compareId = if (compareId == option.characterId) null else option.characterId },
                        )
                    }
                }
            }
            }
        }
        if (content.appearance.isNotEmpty()) {
            SectionCard(stringResource(R.string.attributes_appearance), source = DataSource.SuperheroApi) {
                content.appearance.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(spacing.space3)) {
                        row.forEach { fact -> AppearanceTile(fact, Modifier.weight(1f)) }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

/** Um traço físico (altura, cor dos olhos...) num bloco próprio, para ler de relance. */
@Composable
private fun AppearanceTile(fact: ProfileFact, modifier: Modifier = Modifier) {
    val colors = AssembleTheme.colors
    Column(
        modifier = modifier
            .clip(AssembleTheme.shapes.sm)
            .background(colors.bg)
            .padding(horizontal = AssembleTheme.spacing.space3, vertical = AssembleTheme.spacing.space2),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(stringResource(fact.label).uppercase(), style = AssembleTheme.typography.eyebrow, color = colors.textMuted)
        Text(
            fact.text.orEmpty(),
            style = AssembleTheme.typography.body.copy(fontWeight = FontWeight.SemiBold),
            color = colors.text,
        )
    }
}

/** Valores em texto (o radar sozinho não dá o número exato). Comparando, mostra os dois. */
@Composable
private fun StatGrid(labels: List<String>, stats: Powerstats, compare: StatsOption?, name: String) {
    val colors = AssembleTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(AssembleTheme.spacing.space2)) {
        if (compare != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(AssembleTheme.spacing.space4)) {
                LegendItem(colors.actionAssemble, name)
                LegendItem(colors.score, compare.name)
            }
        }
        labels.indices.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(AssembleTheme.spacing.space4)) {
                row.forEach { i ->
                    Row(Modifier.weight(1f)) {
                        Text(labels[i], style = AssembleTheme.typography.caption, color = colors.textMuted, modifier = Modifier.weight(1f))
                        Text(
                            text = listOfNotNull(stats.values[i], compare?.powerstats?.values?.get(i)).joinToString(" · "),
                            style = AssembleTheme.typography.caption.copy(fontWeight = FontWeight.SemiBold),
                            color = colors.text,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ConnectionsTab(content: CharacterProfileUiState.Content, onOpenCharacter: (String) -> Unit) {
    val colors = AssembleTheme.colors
    val spacing = AssembleTheme.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.space4)) {
        if (content.teams.isNotEmpty()) {
            SectionCard(stringResource(R.string.profile_teams)) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(spacing.space2),
                    verticalArrangement = Arrangement.spacedBy(spacing.space2),
                ) {
                    content.teams.forEach { TraitChip(label = stringResource(traitLabel(it))) }
                }
            }
        }
        SectionCard(stringResource(R.string.connections_teammates)) {
            if (content.teammates.isEmpty()) {
                Text(stringResource(R.string.connections_no_teammates), style = AssembleTheme.typography.body, color = colors.textMuted)
            } else {
                TeamGraph(content.name, content.imageUrl, content.teammates, onOpenCharacter)
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.space4)) {
                    LegendItem(colors.actionAssemble, stringResource(R.string.connections_legend_connected))
                    LegendItem(colors.border, stringResource(R.string.connections_legend_not_connected))
                }
            }
        }
        content.relatives?.let { relatives ->
            SectionCard(stringResource(R.string.profile_relatives), source = DataSource.SuperheroApi) {
                Text(relatives, style = AssembleTheme.typography.body, color = colors.text)
            }
        }
    }
}

/** Mapa radial: o personagem no centro e os colegas de equipe em volta. Só os conectados abrem o perfil. */
@Composable
private fun TeamGraph(name: String, imageUrl: String?, nodes: List<TeammateNode>, onOpenCharacter: (String) -> Unit) {
    val colors = AssembleTheme.colors
    BoxWithConstraints(Modifier.fillMaxWidth().height(GraphHeight)) {
        val radius = minOf(maxWidth, maxHeight) / 2 - GraphEdgeInset
        val positions = nodes.indices.map { i ->
            val angle = 2 * PI * i / nodes.size - PI / 2
            Pair((radius.value * cos(angle)).dp, (radius.value * sin(angle)).dp)
        }
        Box(
            Modifier
                .matchParentSize()
                .drawBehind {
                    val dash = PathEffect.dashPathEffect(floatArrayOf(DashOn.toPx(), DashOff.toPx()))
                    nodes.forEachIndexed { i, node ->
                        val (dx, dy) = positions[i]
                        drawLine(
                            color = if (node.connected) colors.actionAssemble else colors.border,
                            start = center,
                            end = center + Offset(dx.toPx(), dy.toPx()),
                            strokeWidth = GraphLineWidth.toPx(),
                            pathEffect = if (node.connected) null else dash,
                        )
                    }
                },
        )
        GraphNode(name, imageUrl, GraphCenterAvatar, 0.dp, 0.dp, connected = true, onClick = null)
        nodes.forEachIndexed { i, node ->
            val (dx, dy) = positions[i]
            GraphNode(
                name = node.name,
                imageUrl = node.imageUrl,
                avatarSize = GraphNodeAvatar,
                dx = dx,
                dy = dy,
                connected = node.connected,
                onClick = if (node.connected) ({ onOpenCharacter(node.characterId) }) else null,
            )
        }
    }
}

@Composable
private fun BoxScope.GraphNode(
    name: String,
    imageUrl: String?,
    avatarSize: Dp,
    dx: Dp,
    dy: Dp,
    connected: Boolean,
    onClick: (() -> Unit)?,
) {
    val nodeAlpha = if (connected) 1f else NotConnectedAlpha
    val clickModifier = if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier
    CharacterAvatar(
        name = name,
        imageUrl = imageUrl,
        size = avatarSize,
        modifier = Modifier
            .align(Alignment.Center)
            .offset(dx, dy)
            .alpha(nodeAlpha)
            .clip(AssembleTheme.shapes.pill)
            .then(clickModifier)
            .semantics { contentDescription = name },
    )
    Text(
        text = name,
        style = AssembleTheme.typography.small,
        color = AssembleTheme.colors.text,
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .align(Alignment.Center)
            .offset { IntOffset(dx.roundToPx(), (dy + avatarSize / 2 + GraphLabelGap).roundToPx()) }
            .width(GraphLabelWidth)
            .alpha(nodeAlpha),
    )
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AssembleTheme.spacing.space1)) {
        Box(Modifier.size(LegendDot).clip(AssembleTheme.shapes.pill).background(color))
        Text(label, style = AssembleTheme.typography.small, color = AssembleTheme.colors.textMuted)
    }
}

/** Cartão de uma seção: título (com a fonte, se houver) e o conteúdo, sobre a superfície do app. */
@Composable
private fun SectionCard(title: String, source: DataSource? = null, content: @Composable ColumnScope.() -> Unit) {
    val colors = AssembleTheme.colors
    val shape = AssembleTheme.shapes.md
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface)
            .border(1.dp, colors.border, shape)
            .padding(AssembleTheme.spacing.space4),
        verticalArrangement = Arrangement.spacedBy(AssembleTheme.spacing.space3),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionTitle(title, Modifier.weight(1f))
            if (source != null) SourceTag(source)
        }
        content()
    }
}

@Composable
private fun CardDivider() {
    HorizontalDivider(color = AssembleTheme.colors.border)
}

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = AssembleTheme.typography.h2,
        color = AssembleTheme.colors.text,
        modifier = modifier.semantics { heading() },
    )
}

private val StatLabels = listOf(
    R.string.stat_intelligence,
    R.string.stat_strength,
    R.string.stat_speed,
    R.string.stat_durability,
    R.string.stat_power,
    R.string.stat_combat,
)

@get:StringRes
private val DataSource.label: Int
    get() = when (this) {
        DataSource.ComicVine -> R.string.source_name_comic_vine
        DataSource.SuperheroApi -> R.string.source_name_superhero_api
        DataSource.MarvelDatabase -> R.string.source_name_marvel_database
    }
