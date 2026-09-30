package dev.assemble.app.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import dev.assemble.app.core.designsystem.theme.AssembleTheme

private val ChipBorderWidth = 1.dp
private val MinTouchTarget = 48.dp

/**
 * Pill de traço. Selecionado = action-assemble com texto branco; não selecionado = surface com borda.
 * Com [onSelectedChange] vira um toggle (alvo mínimo de 48dp); sem ele é só leitura.
 */
@Composable
fun TraitChip(
    label: String,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onSelectedChange: ((Boolean) -> Unit)? = null,
) {
    val colors = AssembleTheme.colors
    val shape = AssembleTheme.shapes.pill
    val container = if (selected) colors.actionAssemble else colors.surface
    val content = if (selected) colors.onActionAssemble else colors.text

    val interactive = if (onSelectedChange != null) {
        Modifier
            .defaultMinSize(minHeight = MinTouchTarget)
            .clip(shape)
            .toggleable(value = selected, role = Role.Checkbox, onValueChange = onSelectedChange)
    } else {
        Modifier
    }

    Box(modifier = modifier.then(interactive), contentAlignment = Alignment.Center) {
        Text(
            text = label,
            style = AssembleTheme.typography.caption.copy(fontWeight = FontWeight.Bold),
            color = content,
            modifier = Modifier
                .background(container, shape)
                .then(if (selected) Modifier else Modifier.border(ChipBorderWidth, colors.border, shape))
                .padding(horizontal = AssembleTheme.spacing.space3, vertical = AssembleTheme.spacing.space1),
        )
    }
}

@PreviewLightDark
@Composable
private fun TraitChipPreview() {
    PreviewSurface {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            TraitChip("Science")
            TraitChip("Humor", selected = true)
            TraitChip("Mutant", selected = false, onSelectedChange = {})
            TraitChip("Mind", selected = true, onSelectedChange = {})
        }
    }
}
