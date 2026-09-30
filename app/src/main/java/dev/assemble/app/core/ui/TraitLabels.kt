package dev.assemble.app.core.ui

import androidx.annotation.StringRes
import dev.assemble.app.R
import dev.assemble.app.core.model.Origin
import dev.assemble.app.core.model.PowerFamily
import dev.assemble.app.core.model.Style
import dev.assemble.app.core.model.Team

/** Rótulo exibido de cada traço (chips do onboarding, "In common", "Why you match"). */
@get:StringRes
val Origin.label: Int
    get() = when (this) {
        Origin.Human -> R.string.origin_human
        Origin.Mutant -> R.string.origin_mutant
        Origin.Alien -> R.string.origin_alien
        Origin.Robot -> R.string.origin_robot
        Origin.Radiation -> R.string.origin_radiation
        Origin.GodEternal -> R.string.origin_god_eternal
        Origin.Animal -> R.string.origin_animal
        Origin.Cosmic -> R.string.origin_cosmic
        Origin.Infection -> R.string.origin_infection
        Origin.Other -> R.string.origin_other
    }

@get:StringRes
val PowerFamily.label: Int
    get() = when (this) {
        PowerFamily.Strength -> R.string.power_strength
        PowerFamily.Flight -> R.string.power_flight
        PowerFamily.Speed -> R.string.power_speed
        PowerFamily.Mind -> R.string.power_mind
        PowerFamily.Energy -> R.string.power_energy
        PowerFamily.Magic -> R.string.power_magic
        PowerFamily.AgilityCombat -> R.string.power_agility_combat
        PowerFamily.Healing -> R.string.power_healing
        PowerFamily.Shapeshifting -> R.string.power_shapeshifting
        PowerFamily.TechGadgets -> R.string.power_tech_gadgets
    }

@get:StringRes
val Team.label: Int
    get() = when (this) {
        Team.Avengers -> R.string.team_avengers
        Team.XMen -> R.string.team_x_men
        Team.FantasticFour -> R.string.team_fantastic_four
        Team.Guardians -> R.string.team_guardians
        Team.Shield -> R.string.team_shield
        Team.Defenders -> R.string.team_defenders
        Team.Solo -> R.string.team_solo
    }

@get:StringRes
val Style.label: Int
    get() = when (this) {
        Style.Science -> R.string.style_science
        Style.Humor -> R.string.style_humor
        Style.Leadership -> R.string.style_leadership
        Style.Loner -> R.string.style_loner
        Style.Dark -> R.string.style_dark
        Style.Idealist -> R.string.style_idealist
        Style.Rebel -> R.string.style_rebel
        Style.Strategist -> R.string.style_strategist
    }

/** Rótulo de qualquer traço conhecido. */
@StringRes
fun traitLabel(trait: Enum<*>): Int = when (trait) {
    is Origin -> trait.label
    is PowerFamily -> trait.label
    is Team -> trait.label
    is Style -> trait.label
    else -> error("Unknown trait type: ${trait::class.simpleName}")
}
