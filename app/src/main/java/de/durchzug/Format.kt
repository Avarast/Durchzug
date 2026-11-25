package de.durchzug

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Locale

internal fun Double.fmt(decimals: Int = 1) = String.format(Locale.GERMANY, "%.${decimals}f", this)

/** Formats without trailing zeros, e.g. 21.0 -> "21", 21.25 -> "21,25". */
internal fun Double.fmtCompact(): String = DecimalFormat("0.##", DecimalFormatSymbols(Locale.GERMANY)).format(this)

internal fun String.parseDecimal() = trim().replace(',', '.').toDoubleOrNull()

internal val timestampFormat = SimpleDateFormat("dd.MM.yyyy, HH:mm 'Uhr'", Locale.GERMANY)

internal fun formatAge(ms: Long): String {
    val minutes = ms / 60_000
    return when {
        minutes < 1 -> "gerade eben"
        minutes < 60 -> "vor $minutes Min."
        minutes < 48 * 60 -> "vor ${minutes / 60} Std."
        else -> "vor ${minutes / (24 * 60)} Tagen"
    }
}
