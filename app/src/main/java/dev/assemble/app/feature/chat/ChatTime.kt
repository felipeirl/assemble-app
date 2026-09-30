package dev.assemble.app.feature.chat

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/** Hora curta se for hoje; senão, a data curta no formato do aparelho. */
fun formatConversationTime(
    time: Instant,
    now: Instant,
    zone: ZoneId = ZoneId.systemDefault(),
    locale: Locale = Locale.getDefault(),
): String {
    val date = time.atZone(zone)
    val sameDay = date.toLocalDate() == now.atZone(zone).toLocalDate()
    val formatter = if (sameDay) {
        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
    } else {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)
    }
    return formatter.withLocale(locale).format(date)
}
