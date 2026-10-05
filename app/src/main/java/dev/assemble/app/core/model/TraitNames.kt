package dev.assemble.app.core.model

/**
 * Traço pelo nome usado no contrato da API ("Mutant", "XMen", "Leadership"...). Os nomes não se
 * repetem entre Origin, PowerFamily, Team e Style. Nome desconhecido devolve null: o app ignora
 * em vez de inventar um traço.
 */
fun traitFromName(name: String): Enum<*>? = TRAITS_BY_NAME[name]

/** Converte uma lista do contrato, mantendo a ordem e descartando os desconhecidos. */
fun traitsFromNames(names: List<String>): List<Enum<*>> = names.mapNotNull(::traitFromName)

private val TRAITS_BY_NAME: Map<String, Enum<*>> =
    (Origin.entries + PowerFamily.entries + Team.entries + Style.entries).associateBy { it.name }
