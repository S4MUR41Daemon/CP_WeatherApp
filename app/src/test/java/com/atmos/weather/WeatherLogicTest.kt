package com.atmos.weather

import com.atmos.weather.domain.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherLogicTest {
    @Test fun wmoMapping() {
        assertEquals("CLR", wmo(0).code)
        assertEquals("RAIN", wmo(65).code)
        assertEquals("TSTM", wmo(95).code)
    }
    @Test fun beaufortBuckets() {
        assertEquals(0, beaufort(0.5))
        assertEquals(6, beaufort(45.0))
        assertEquals(12, beaufort(130.0))
    }
    @Test fun dirCardinal() {
        assertEquals("N", dir(0.0))
        assertEquals("E", dir(90.0))
        assertEquals("S", dir(180.0))
        assertEquals("O", dir(270.0))
    }
    @Test fun alertsThresholds() {
        val a = computeAlerts(listOf(95, 61), 70.0, 9.0, 39.0, -3.0, 25.0)
        assertEquals(6, a.size)
    }
    @Test fun pollenLevels() {
        assertEquals("N/D", pollenLevel(null))
        assertEquals("BAJO", pollenLevel(5.0))
        assertEquals("MUY ALTO", pollenLevel(200.0))
    }
    @Test fun rainIntensityWet() {
        assertTrue(rainIntensity(61, 2.0) > 0.0)
        assertEquals(0.0, rainIntensity(0, 0.0), 0.0001)
    }
}
