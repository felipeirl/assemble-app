package dev.assemble.app.feature.login

/** Maior tamanho do título do hero (o `displayXl` do tema). */
internal const val HeroMaxFontSp = 64f

/** Menor tamanho do título antes de o hero perder o logo e, por fim, sumir. */
internal const val HeroMinFontSp = 28f

internal const val HeroFontStepSp = 2f

/**
 * Maior tamanho de fonte entre [max] e [min] (de [step] em [step]) para o qual [fits] diz que o
 * título e o subtítulo cabem na sobra acima das ações; null se nem o menor cabe.
 */
internal fun largestFittingFontSp(
    max: Float,
    min: Float,
    step: Float,
    fits: (Float) -> Boolean,
): Float? {
    var size = max
    while (size >= min) {
        if (fits(size)) return size
        size -= step
    }
    return null
}
