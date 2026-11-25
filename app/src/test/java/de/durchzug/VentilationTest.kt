package de.durchzug

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VentilationTest {
    @Test
    fun absoluteHumidityAt20CAnd50Percent() =
        assertEquals(8.6, Ventilation.absoluteHumidity(20.0, 50.0), 0.1)

    @Test
    fun coldOutsideAirDriesRoom() {
        val r = Ventilation.calculate(21.0, 65.0, 2.0, 90.0, 2.0)
        assertTrue(r.worthIt)
        assertTrue(r.rhAfter < 40)
    }

    @Test
    fun humidSummerAirIsNotWorthIt() {
        val r = Ventilation.calculate(22.0, 50.0, 28.0, 70.0, 1.0)
        assertFalse(r.worthIt)
    }
}
