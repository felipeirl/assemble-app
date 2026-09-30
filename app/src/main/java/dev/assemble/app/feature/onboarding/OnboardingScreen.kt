package dev.assemble.app.feature.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.assemble.app.R
import dev.assemble.app.core.designsystem.component.TopBarNavigation
import dev.assemble.app.core.designsystem.component.TopBarTitle
import dev.assemble.app.navigation.ONBOARDING_STEP_COUNT
import dev.assemble.app.navigation.PlaceholderLink
import dev.assemble.app.navigation.PlaceholderScreen

private val StepTitles = listOf(
    R.string.onboarding_step_origin,
    R.string.onboarding_step_powers,
    R.string.onboarding_step_teams,
    R.string.onboarding_step_style,
    R.string.onboarding_step_fame,
)

// Placeholder da etapa 5; tela real na etapa 6.
@Composable
fun OnboardingScreen(
    step: Int,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isLast = step == ONBOARDING_STEP_COUNT - 1
    PlaceholderScreen(
        title = TopBarTitle.Text(stringResource(StepTitles[step.coerceIn(StepTitles.indices)])),
        navigation = if (step > 0) TopBarNavigation.Back(onBack) else TopBarNavigation.None,
        modifier = modifier,
        detail = "${step + 1} / $ONBOARDING_STEP_COUNT",
        links = listOf(
            if (isLast) {
                PlaceholderLink(stringResource(R.string.onboarding_start), onFinish)
            } else {
                PlaceholderLink(stringResource(R.string.onboarding_skip), onNext)
            },
        ),
    )
}
