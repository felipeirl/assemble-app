package dev.assemble.app.feature.onboarding

import androidx.annotation.StringRes
import dev.assemble.app.R
import dev.assemble.app.core.model.Origin
import dev.assemble.app.core.model.PowerFamily
import dev.assemble.app.core.model.Preferences
import dev.assemble.app.core.model.Style
import dev.assemble.app.core.model.Team

/** Passos do onboarding, na ordem. */
enum class OnboardingStep(@StringRes val title: Int) {
    Origin(R.string.onboarding_step_origin),
    Powers(R.string.onboarding_step_powers),
    Teams(R.string.onboarding_step_teams),
    Style(R.string.onboarding_step_style),
    Fame(R.string.onboarding_step_fame),
    ;

    val isLast: Boolean get() = this == entries.last()

    companion object {
        fun at(index: Int): OnboardingStep = entries[index.coerceIn(entries.indices)]
    }
}

data class OnboardingUiState(
    val preferences: Preferences,
    val submitting: Boolean = false,
    val showError: Boolean = false,
) {
    /** Escolhas feitas nas categorias de chips ("Any" não conta). */
    val totalChoices: Int
        get() = preferences.origins.size + preferences.powers.size + preferences.teams.size + preferences.styles.size

    val canFinish: Boolean get() = totalChoices >= MIN_CHOICES

    /** Opções da categoria do passo e quais estão selecionadas; vazio no passo de fama. */
    fun optionsFor(step: OnboardingStep): List<Pair<Enum<*>, Boolean>> = when (step) {
        OnboardingStep.Origin -> Origin.entries.map { it to (it in preferences.origins) }
        OnboardingStep.Powers -> PowerFamily.entries.map { it to (it in preferences.powers) }
        OnboardingStep.Teams -> Team.entries.map { it to (it in preferences.teams) }
        OnboardingStep.Style -> Style.entries.map { it to (it in preferences.styles) }
        OnboardingStep.Fame -> emptyList()
    }

    fun isAny(step: OnboardingStep): Boolean = when (step) {
        OnboardingStep.Origin -> preferences.origins.isEmpty()
        OnboardingStep.Powers -> preferences.powers.isEmpty()
        OnboardingStep.Teams -> preferences.teams.isEmpty()
        OnboardingStep.Style -> preferences.styles.isEmpty()
        OnboardingStep.Fame -> false
    }

    companion object {
        const val MIN_CHOICES = 3
    }
}
