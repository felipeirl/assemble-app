package dev.assemble.app.feature.help

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import dev.assemble.app.R
import dev.assemble.app.core.designsystem.component.AssembleLargeTopBar
import dev.assemble.app.core.designsystem.icon.AssembleIcons
import dev.assemble.app.core.designsystem.theme.AssembleTheme
import dev.assemble.app.core.designsystem.theme.rememberAnimationsEnabled

/** Uma pergunta da Ajuda e a resposta que abre ao tocar. */
private data class HelpQuestion(@StringRes val question: Int, @StringRes val answer: Int)

private val HelpQuestions = listOf(
    HelpQuestion(R.string.help_discover_title, R.string.help_discover_body),
    HelpQuestion(R.string.help_match_title, R.string.help_match_body),
    HelpQuestion(R.string.help_chat_title, R.string.help_chat_body),
    HelpQuestion(R.string.help_preferences_title, R.string.help_preferences_body),
)

private const val ExpandMillis = 300
private const val ChevronOpenDegrees = 180f
private const val ChevronDampingRatio = 0.6f
private val MinQuestionHeight = 56.dp
private val ChevronSize = 20.dp
private val CardBorderWidth = 1.dp

/** Ajuda em perguntas: cada uma abre a resposta com uma mola suave. A primeira começa aberta. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val spacing = AssembleTheme.spacing
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var openIndex by rememberSaveable { mutableStateOf<Int?>(0) }
    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = AssembleTheme.colors.bg,
        topBar = { AssembleLargeTopBar(title = stringResource(R.string.help_title), onBack = onBack, scrollBehavior = scrollBehavior) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(spacing.space4),
            verticalArrangement = Arrangement.spacedBy(spacing.space3),
        ) {
            HelpQuestions.forEachIndexed { index, item ->
                QuestionCard(
                    question = stringResource(item.question),
                    answer = stringResource(item.answer),
                    expanded = openIndex == index,
                    onToggle = { openIndex = if (openIndex == index) null else index },
                )
            }
        }
    }
}

@Composable
private fun QuestionCard(question: String, answer: String, expanded: Boolean, onToggle: () -> Unit) {
    val colors = AssembleTheme.colors
    val spacing = AssembleTheme.spacing
    val shape = AssembleTheme.shapes.md
    val animationsEnabled = rememberAnimationsEnabled()
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) ChevronOpenDegrees else 0f,
        animationSpec = if (animationsEnabled) spring(dampingRatio = ChevronDampingRatio) else snap(),
        label = "helpChevron",
    )
    val expandedLabel = stringResource(R.string.help_expanded)
    val collapsedLabel = stringResource(R.string.help_collapsed)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface)
            .border(CardBorderWidth, colors.border, shape),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = MinQuestionHeight)
                .clickable(role = Role.Button, onClick = onToggle)
                .semantics { stateDescription = if (expanded) expandedLabel else collapsedLabel }
                .padding(horizontal = spacing.space4, vertical = spacing.space3),
            horizontalArrangement = Arrangement.spacedBy(spacing.space3),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = question,
                style = AssembleTheme.typography.body.copy(fontWeight = FontWeight.SemiBold),
                color = colors.text,
                modifier = Modifier
                    .weight(1f)
                    .semantics { heading() },
            )
            Icon(
                imageVector = AssembleIcons.ChevronDown,
                contentDescription = null,
                tint = colors.accentText,
                modifier = Modifier
                    .size(ChevronSize)
                    .graphicsLayer { rotationZ = chevronRotation },
            )
        }
        val duration = if (animationsEnabled) ExpandMillis else 0
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(tween(duration)) + fadeIn(tween(duration)),
            exit = shrinkVertically(tween(duration)) + fadeOut(tween(duration)),
        ) {
            Text(
                text = answer,
                style = AssembleTheme.typography.caption,
                color = colors.textMuted,
                modifier = Modifier.padding(start = spacing.space4, end = spacing.space4, bottom = spacing.space4),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun HelpScreenPreview() {
    AssembleTheme { HelpScreen(onBack = {}) }
}
