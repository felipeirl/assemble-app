package dev.assemble.app.core.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import dev.assemble.app.core.designsystem.component.AssembleTopBar
import dev.assemble.app.core.designsystem.component.TopBarNavigation
import dev.assemble.app.core.designsystem.component.TopBarTitle
import dev.assemble.app.core.designsystem.theme.AssembleTheme

/** Seção de uma página de texto: título + parágrafo. */
data class InfoSection(@StringRes val title: Int, @StringRes val body: Int)

/** Página de texto simples (About, Help). */
@Composable
fun InfoPage(
    @StringRes title: Int,
    sections: List<InfoSection>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AssembleTheme.colors
    val spacing = AssembleTheme.spacing
    Scaffold(
        modifier = modifier,
        containerColor = colors.bg,
        topBar = {
            AssembleTopBar(title = TopBarTitle.Text(stringResource(title)), navigation = TopBarNavigation.Back(onBack))
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(spacing.space4),
            verticalArrangement = Arrangement.spacedBy(spacing.space5),
        ) {
            sections.forEach { section ->
                Column(verticalArrangement = Arrangement.spacedBy(spacing.space2)) {
                    Text(
                        text = stringResource(section.title),
                        style = AssembleTheme.typography.h2,
                        color = colors.text,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(stringResource(section.body), style = AssembleTheme.typography.body, color = colors.textMuted)
                }
            }
        }
    }
}
