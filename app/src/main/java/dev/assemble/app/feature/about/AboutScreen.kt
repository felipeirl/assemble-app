package dev.assemble.app.feature.about

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import dev.assemble.app.R
import dev.assemble.app.core.designsystem.theme.AssembleTheme
import dev.assemble.app.core.ui.InfoPage
import dev.assemble.app.core.ui.InfoSection

private val AboutSections = listOf(
    InfoSection(R.string.about_sources_title, R.string.about_sources_body),
    InfoSection(R.string.about_ai_title, R.string.about_ai_body),
    InfoSection(R.string.about_compatibility_title, R.string.about_compatibility_body),
    InfoSection(R.string.about_project_title, R.string.about_project_body),
)

@Composable
fun AboutScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    InfoPage(title = R.string.about_title, sections = AboutSections, onBack = onBack, modifier = modifier)
}

@PreviewLightDark
@Composable
private fun AboutScreenPreview() {
    AssembleTheme { AboutScreen(onBack = {}) }
}
