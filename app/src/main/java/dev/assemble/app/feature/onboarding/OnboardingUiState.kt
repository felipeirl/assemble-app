package dev.assemble.app.feature.onboarding

import dev.assemble.app.core.model.PreferenceCategory
import dev.assemble.app.core.model.Preferences

/** Cada passo do onboarding é uma categoria de preferência, na mesma ordem. */
fun onboardingStepAt(index: Int): PreferenceCategory =
    PreferenceCategory.entries[index.coerceIn(PreferenceCategory.entries.indices)]

val PreferenceCategory.isLastStep: Boolean get() = this == PreferenceCategory.entries.last()

data class OnboardingUiState(
    val preferences: Preferences,
    val submitting: Boolean = false,
    val showError: Boolean = false,
)
