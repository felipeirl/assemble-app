package dev.assemble.app.feature.login

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Quanto do hero (logo, título, subtítulo) cabe acima das ações do login. */
internal enum class HeroVariant { Full, Compact, Hidden }

/** Logo 72dp + título displayXl em até 3 linhas + subtítulo. */
internal val FullHeroMinHeight = 260.dp

/** Logo 40dp + título displayMd em até 2 linhas. */
internal val CompactHeroMinHeight = 112.dp

/** [available] é a altura que sobra para o hero depois das ações, já sem as barras do sistema. */
internal fun heroVariantFor(available: Dp): HeroVariant = when {
    available >= FullHeroMinHeight -> HeroVariant.Full
    available >= CompactHeroMinHeight -> HeroVariant.Compact
    else -> HeroVariant.Hidden
}
