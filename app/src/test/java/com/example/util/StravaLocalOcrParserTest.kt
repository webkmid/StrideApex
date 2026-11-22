package com.example.util

import com.example.domain.WorkoutRefreshEngine
import com.example.ui.components.parseSplitsSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    @Test
    fun testParseSplitsWithFractionalLastKm() {
        val rawOcrText = """
            22:09 58
            Run
            1 7:54 -3
            2 6:58 2
            3 6:35 1
            4 6:45 0
            0.9 6:45 -1
            Pace
            6:00
            7:00
            8:00
            9:00
            10:00
            11:00
            12:00
            /km
            1 km 2 km 3 km 4 km
            Avg Pace 6:59 /km
            Moving Time 34:16
            Elapsed Time 34:26
            Fastest Split 6:35 /km
        """.trimIndent()

        val metrics = StravaLocalOcrParser.parseStravaText(rawOcrText)
        assertNotNull(metrics)
        assertEquals(2056L, metrics!!.durationSeconds)
        assertEquals(419L, metrics.avgPaceSecondsPerKm)
        assertEquals(4.9, metrics.distanceKm, 0.02)
        assertEquals(
            "KM 1: 7:54/km | KM 2: 6:58/km | KM 3: 6:35/km | KM 4: 6:45/km | KM 0.9: 6:45/km",
            metrics.splitsSummary
        )
    }

    @Test
    fun testParseSplitsWithBarNoiseAndOcrMisreads() {
        val rawOcrText = """
            Run
            I 7:54 ==== -3
            2 6:58 2
            3 6:35 1
            A 6:45 0
            09 6:45 -1
            Pace
            6:00
            /km
            Avg Pace 6:59 /km
            Moving Time 34:16
        """.trimIndent()

        val metrics = StravaLocalOcrParser.parseStravaText(rawOcrText)
        assertNotNull(metrics)
        assertEquals(
            "KM 1: 7:54/km | KM 2: 6:58/km | KM 3: 6:35/km | KM 4: 6:45/km | KM 0.9: 6:45/km",
            metrics!!.splitsSummary
        )
        assertEquals(4.9, metrics.distanceKm, 0.02)
    }

    @Test
    fun testParseSplitsRecoversDroppedFullSplitBeforeFractional() {
        val rawOcrText = """
            Run
            1 7:54 -3
            2 6:58 2
            3 6:35 1
            0.9 6:45 -1
            Pace
            Avg Pace 6:59 /km
            Moving Time 34:16
        """.trimIndent()

        val metrics = StravaLocalOcrParser.parseStravaText(rawOcrText)
        assertNotNull(metrics)
        assertEquals(
            "KM 1: 7:54/km | KM 2: 6:58/km | KM 3: 6:35/km | KM 4: 6:45/km | KM 0.9: 6:45/km",
            metrics!!.splitsSummary
        )
        assertEquals(4.9, metrics.distanceKm, 0.02)
    }

    @Test
    fun testParseSplitsColumnSeparatedBlocks() {
        val rawOcrText = """
            Run
            1
            2
            3
            4
            0.9
            7:54
            6:58
            6:35
            6:45
            6:45
            Pace
            6:00
            7:00
            Avg Pace 6:59 /km
            Moving Time 34:16
        """.trimIndent()

        val metrics = StravaLocalOcrParser.parseStravaText(rawOcrText)
        assertNotNull(metrics)
        assertEquals(
            "KM 1: 7:54/km | KM 2: 6:58/km | KM 3: 6:35/km | KM 4: 6:45/km | KM 0.9: 6:45/km",
            metrics!!.splitsSummary
        )
    }

    @Test
    fun testParseSplitsSummaryDecimalSupportAndRefreshGuard() {
        val summary = "KM 1: 7:54/km | KM 2: 6:58/km | KM 3: 6:35/km | KM 4: 6:45/km | KM 0.9: 6:45/km"
        val parsed = parseSplitsSummary(summary, 419L)
        assertEquals(5, parsed.size)
        assertEquals("0.9", parsed.last().kmLabel)
        assertEquals(0.9, parsed.last().distanceKm, 0.001)
        assertFalse(WorkoutRefreshEngine.needsSplitRecalibration(summary, 4.9, 419L))
    }
}
