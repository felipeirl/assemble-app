package dev.assemble.app.core.model

/** As 5 categorias de preferência, na ordem do onboarding e do perfil. */
enum class PreferenceCategory { Origin, Powers, Teams, Style, Fame }

/** Opções da categoria e quais estão selecionadas; vazio para Fame (slider). */
fun Preferences.options(category: PreferenceCategory): List<Pair<Enum<*>, Boolean>> = when (category) {
    PreferenceCategory.Origin -> Origin.entries.map { it to (it in origins) }
    PreferenceCategory.Powers -> PowerFamily.entries.map { it to (it in powers) }
    PreferenceCategory.Teams -> Team.entries.map { it to (it in teams) }
    PreferenceCategory.Style -> Style.entries.map { it to (it in styles) }
    PreferenceCategory.Fame -> emptyList()
}

/** Itens escolhidos numa categoria de chips, na ordem do enum. */
fun Preferences.selected(category: PreferenceCategory): List<Enum<*>> =
    options(category).filter { it.second }.map { it.first }

/** "Any" = conjunto vazio. Fame nunca é "Any". */
fun Preferences.isAny(category: PreferenceCategory): Boolean =
    category != PreferenceCategory.Fame && selected(category).isEmpty()

fun Preferences.toggle(trait: Enum<*>): Preferences = when (trait) {
    is Origin -> copy(origins = origins.toggled(trait))
    is PowerFamily -> copy(powers = powers.toggled(trait))
    is Team -> copy(teams = teams.toggled(trait))
    is Style -> copy(styles = styles.toggled(trait))
    else -> this
}

fun Preferences.selectAny(category: PreferenceCategory): Preferences = when (category) {
    PreferenceCategory.Origin -> copy(origins = emptySet())
    PreferenceCategory.Powers -> copy(powers = emptySet())
    PreferenceCategory.Teams -> copy(teams = emptySet())
    PreferenceCategory.Style -> copy(styles = emptySet())
    PreferenceCategory.Fame -> this
}

fun Preferences.withFame(value: Float): Preferences =
    copy(fame = value.coerceIn(Preferences.FAME_ICONS, Preferences.FAME_HIDDEN_GEMS))

/** Escolhas nas categorias de chips ("Any" não conta; fama não conta). */
val Preferences.totalChoices: Int
    get() = origins.size + powers.size + teams.size + styles.size

private fun <T> Set<T>.toggled(item: T): Set<T> = if (item in this) this - item else this + item
