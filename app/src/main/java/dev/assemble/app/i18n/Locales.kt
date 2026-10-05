package dev.assemble.app.i18n

import java.util.Locale
import java.util.TimeZone

/** Idioma pedido ao backend: pt-BR ou en (contrato §1). O app só tem essas duas traduções. */
fun backendLanguageTag(locale: Locale = Locale.getDefault()): String =
    if (locale.language == "pt") "pt-BR" else "en"

fun isPortuguese(languageTag: String): Boolean = languageTag.startsWith("pt")

/** Fuso IANA do aparelho; define o "dia" do baralho. */
fun deviceTimeZoneId(): String = TimeZone.getDefault().id
