package com.example.domain

import com.example.data.model.TrainingGoal
import com.example.data.model.WorkoutLog
import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class WeeklyProgressEngineTest {

    private fun epochFor(year: Int, month1Based: Int, day: Int, hour: Int = 10): Long {
        return Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month1Based - 1)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun workoutOn(epochMs: Long, distanceKm: Double, durationSec: Long): WorkoutLog {
        return WorkoutLog(
            id = "w_$epochMs",
            userId = "user1",
            title = "Run",
            distanceKm = distanceKm,
            durationSeconds = durationSec,
            avgPaceSecondsPerKm = (durationSec / distanceKm).toLong(),
            createdAt = Timestamp(java.util.Date(epochMs))
        )
    }

    @Test
    fun `weekly progress sums only current week and excludes previous weeks`() {
        // Wednesday Nov 18, 2026 -> week is Mon Nov 16 .. Sun Nov 22
        val nowMs = epochFor(2026, 11, 18, 18)
        val workouts = listOf(
            workoutOn(epochFor(2026, 11, 10), 12.0, 4200L), // previous week
            workoutOn(epochFor(2026, 11, 16), 5.0, 1800L),  // Mon (this week)
            workoutOn(epochFor(2026, 11, 18), 7.5, 2700L)   // Wed (this week)
        )
        val goal = TrainingGoal(id = "g1", title = "10K", weeklyMileageTargetKm = 30L)

        val result = WeeklyProgressEngine.computeWeeklyProgress(workouts, goal, nowMs)

        assertEquals(12.5, result.totalDistanceKm, 0.01)
        assertEquals(2, result.runsCount)
        assertEquals(12.0, result.prevWeekDistanceKm, 0.01)
    }

    @Test
    fun `sunday boundary includes Monday and Sunday of current week and excludes prior Sunday`() {
        // Sunday Nov 22, 2026 -> week must still be Mon Nov 16 .. Sun Nov 22
        val sundayNowMs = epochFor(2026, 11, 22, 23)
        val workouts = listOf(
            workoutOn(epochFor(2026, 11, 15, 20), 15.0, 5400L), // Prior Sunday (last week)
            workoutOn(epochFor(2026, 11, 16, 6), 4.9, 2056L),   // Monday (this week)
            workoutOn(epochFor(2026, 11, 22, 7), 10.1, 3600L)   // Current Sunday (this week)
        )
        val goal = TrainingGoal(id = "g1", title = "HM", weeklyMileageTargetKm = 35L)

        val result = WeeklyProgressEngine.computeWeeklyProgress(workouts, goal, sundayNowMs)

        assertEquals(15.0, result.totalDistanceKm, 0.01)
        assertEquals(2, result.runsCount)
        assertEquals(15.0, result.prevWeekDistanceKm, 0.01)
    }
}
