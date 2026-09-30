package dev.assemble.app.feature.help

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import dev.assemble.app.R
import dev.assemble.app.core.designsystem.theme.AssembleTheme
import dev.assemble.app.core.ui.InfoPage
import dev.assemble.app.core.ui.InfoSection

private val HelpSections = listOf(
    InfoSection(R.string.help_discover_title, R.string.help_discover_body),
    InfoSection(R.string.help_match_title, R.string.help_match_body),
    InfoSection(R.string.help_chat_title, R.string.help_chat_body),
    InfoSection(R.string.help_preferences_title, R.string.help_preferences_body),
)

@Composable
fun HelpScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    InfoPage(title = R.string.help_title, sections = HelpSections, onBack = onBack, modifier = modifier)
}

@PreviewLightDark
@Composable
private fun HelpScreenPreview() {
    AssembleTheme { HelpScreen(onBack = {}) }
}
