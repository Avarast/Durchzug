package de.durchzug

import kotlin.math.exp
import kotlin.math.roundToInt

/** Physikalische Berechnungen rund ums Lüften (Magnus-Formel). */
object Ventilation {

    /** Sättigungsdampfdruck in hPa (über Wasser bzw. Eis). */
    fun saturationVaporPressure(t: Double): Double =
        if (t >= 0) 6.112 * exp(17.62 * t / (243.12 + t))
        else 6.112 * exp(22.46 * t / (272.62 + t))

    /** Absolute Feuchte in g/m³. */
    fun absoluteHumidity(t: Double, rh: Double): Double =
        216.7 * (rh / 100.0 * saturationVaporPressure(t)) / (273.15 + t)

    /** Relative Feuchte (%) bei Temperatur t für eine gegebene absolute Feuchte. */
    fun relativeHumidity(t: Double, ah: Double): Double =
        ah * (273.15 + t) / (216.7 * saturationVaporPressure(t)) * 100.0

    /**
     * Empfohlene Stoßlüftdauer in Minuten. Je kälter es draußen ist und je
     * stärker der Wind, desto schneller erfolgt der Luftaustausch.
     */
    fun recommendedMinutes(tIn: Double, tOut: Double, windMs: Double): Int {
        val base = when {
            tOut < 0 -> 5.0
            tOut < 5 -> 7.0
            tOut < 10 -> 10.0
            tOut < 15 -> 15.0
            tOut < 20 -> 20.0
            else -> 25.0
        }
        val windFactor = when {
            windMs > 6 -> 0.7
            windMs > 3 -> 0.85
            else -> 1.0
        }
        // Bei geringer Temperaturdifferenz fehlt der thermische Auftrieb
        val dtFactor = if (tIn - tOut < 3) 1.2 else 1.0
        return (base * windFactor * dtFactor).roundToInt().coerceIn(3, 30)
    }

    /** Grobe Windstufen für die manuelle Eingabe mit repräsentativer Geschwindigkeit. */
    enum class Wind(val ms: Double) {
        CALM(0.0),
        LIGHT(2.0),
        MODERATE(4.5),
        STRONG(8.0);

        companion object {
            fun fromMs(ms: Double) = when {
                ms <= 1 -> CALM
                ms <= 3 -> LIGHT
                ms <= 6 -> MODERATE
                else -> STRONG
            }
        }
    }

    data class Result(
        val ahIn: Double,
        val ahOut: Double,
        val rhAfter: Double,
        val minutes: Int,
        val worthIt: Boolean,
    )

    fun calculate(tIn: Double, rhIn: Double, tOut: Double, rhOut: Double, windMs: Double): Result {
        val ahIn = absoluteHumidity(tIn, rhIn)
        val ahOut = absoluteHumidity(tOut, rhOut)
        // Nach dem Luftaustausch erwärmt sich die Außenluft wieder auf Innentemperatur
        val rhAfter = relativeHumidity(tIn, ahOut).coerceIn(0.0, 100.0)
        return Result(
            ahIn = ahIn,
            ahOut = ahOut,
            rhAfter = rhAfter,
            minutes = recommendedMinutes(tIn, tOut, windMs),
            worthIt = ahOut < ahIn,
        )
    }
}
