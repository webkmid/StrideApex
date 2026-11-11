package com.example.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StravaLocalOcrParserTest {

    @Test
    fun testParseImage1SplitsAndPace() {
        val rawOcrText = """
            23:25 1
            Run
            1 7:28 -0
            2 7:39 1
            3 7:30 1
            4 7:28 -1
            Pace
            5:00
            6:00
            7:00
            8:00
            9:00
            10:00
            11:00
            /km
            0.5 km 1.0 km 1.5 km 2.0 km 2.5 km 3.0 km 3.5 km
            Avg Pace 7:32 /km
            Moving Time 30:14
            Avg Elapsed Pace 8:07 /km
            Elapsed Time 32:38
        """.trimIndent()

        val metrics = StravaLocalOcrParser.parseStravaText(rawOcrText)
        assertNotNull(metrics)
        println("Metrics Image 1: $metrics")
        assertEquals(452L, metrics?.avgPaceSecondsPerKm) // 7:32
        assertEquals(1814L, metrics?.durationSeconds) // 30:14
        // Expected distance around 4.01 km (calculated 1814 / 452)
        assertTrue("Distance should be around 4.0 km but was ${metrics?.distanceKm}", metrics!!.distanceKm in 3.9..4.2)
        assertTrue("Splits should contain 4 splits", metrics.splitsSummary.contains("KM 4: 7:28/km"))
    }

    @Test
    fun testParseImage4AnleneRun() {
        val rawOcrText = """
            11:52 1
            Run
            Erza Juan Bastilla with Jakarta Running Festival
            1 November 2026 at 05:31 · Gelora, Special Capital Region of Jakarta
            Anlene Run
            Your fastest 2 miles ever!
            2m 3s
            Distance Avg Pace
            4.92 km 6:59 /km
            Elapsed Time Elevation Gain
            34:26 13 m
            Max Elevation Steps
            21 m 4,806
            Hard
            with 209 others Manage Group
            JRF Blues: Run Down Memory Lane Remove
            Race
            Adidas Evo SL Exo
        """.trimIndent()

        val metrics = StravaLocalOcrParser.parseStravaText(rawOcrText)
        assertNotNull(metrics)
        println("Metrics Image 4: $metrics")
        assertEquals("Anlene Run", metrics?.title)
        assertEquals(4.92, metrics!!.distanceKm, 0.01)
        assertEquals(419L, metrics.avgPaceSecondsPerKm) // 6:59
        assertEquals(2066L, metrics.durationSeconds) // 34:26
        assertEquals(13L, metrics.elevationGainM)
        assertEquals("2026-11-01", metrics.detectedDateStr)
    }
}
