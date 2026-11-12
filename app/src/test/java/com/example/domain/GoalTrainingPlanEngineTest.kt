package com.example.domain

import com.example.data.model.DistanceCategory
import com.example.data.model.TrainingGoal
import com.example.data.model.WorkoutLog
import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class GoalTrainingPlanEngineTest {

    @Test
    fun testEncodeAndParseGoalMetadataLegacy() {
        val cleanTitle = "Astra Run 10K"
        val targetDateKey = "2026-11-29"
        val trainingDays = listOf(2, 4, 7) // Selasa, Kamis, Minggu

        val encodedTitle = GoalTrainingPlanEngine.encodeGoalTitleWithSchedule(cleanTitle, targetDateKey, trainingDays)
        assertEquals("Astra Run 10K [2026-11-29|2,4,7]", encodedTitle)
        assertTrue(encodedTitle.length <= 120)

        val goal = TrainingGoal(
            id = "goal_astra",
            title = encodedTitle,
            targetDistanceCategory = "10K",
            targetTimeSeconds = 3000L, // 50:00 (pace 5:00/km)
            weeklyMileageTargetKm = 30L
        )

        val meta = GoalTrainingPlanEngine.parseGoalMetadata(goal)
        assertEquals("Astra Run 10K", meta.cleanTitle)
        assertEquals("2026-11-29", meta.targetDateKey)
        assertEquals(listOf(2, 4, 7), meta.trainingDaysIso)
    }

    @Test
    fun testEncodeAndParseGoalMetadataWithStartDate() {
        val cleanTitle = "Astra Run 10K"
        val startDateKey = "2026-11-10"
        val targetDateKey = "2026-11-29"
        val trainingDays = listOf(2, 4, 7)

        val encodedTitle = GoalTrainingPlanEngine.encodeGoalTitleWithSchedule(
            cleanTitle = cleanTitle,
            targetDateKey = targetDateKey,
            trainingDaysIso = trainingDays,
            startDateKey = startDateKey
        )
        assertEquals("Astra Run 10K [2026-11-10~2026-11-29|2,4,7]", encodedTitle)
        assertTrue(encodedTitle.length <= 120)

        val goal = TrainingGoal(
            id = "goal_astra_range",
            title = encodedTitle,
            targetDistanceCategory = "10K",
            targetTimeSeconds = 3000L,
            weeklyMileageTargetKm = 30L
        )

        val meta = GoalTrainingPlanEngine.parseGoalMetadata(goal)
        assertEquals("Astra Run 10K", meta.cleanTitle)
        assertEquals("2026-11-10", meta.startDateKey)
        assertEquals("2026-11-29", meta.targetDateKey)
        assertEquals(listOf(2, 4, 7), meta.trainingDaysIso)
    }

    @Test
    fun testGenerateScheduleStatusesSkippedTodayCompletedUpcoming() {
        val cal = Calendar.getInstance()
        cal.set(2026, Calendar.NOVEMBER, 10, 8, 0, 0) // Tuesday, 10 Nov 2026 (ISO day 2 = Selasa)
        val nowMs = cal.timeInMillis

        // Goal starting 2026-11-03 up to 2026-11-29 with sessions on Selasa (2), Kamis (4), Minggu (7)
        val encodedTitle = GoalTrainingPlanEngine.encodeGoalTitleWithSchedule(
            "Astra Run 10K",
            "2026-11-29",
            listOf(2, 4, 7),
            "2026-11-03"
        )
        val goal = TrainingGoal(
            id = "goal_test",
            title = encodedTitle,
            targetDistanceCategory = "10K",
            targetTimeSeconds = 3000L,
            weeklyMileageTargetKm = 30L,
            createdAt = Timestamp(Date(nowMs - 7L * 86400_000L))
        )

        // Workout on Sunday 8 Nov 2026 (Sunday is ISO 7)
        val calSunday = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 8, 7, 0, 0)
        }
        val workouts = listOf(
            WorkoutLog(
                id = "w_sunday",
                title = "Long Run",
                distanceKm = 10.0,
                durationSeconds = 3300L,
                avgPaceSecondsPerKm = 330L,
                createdAt = Timestamp(Date(calSunday.timeInMillis))
            )
        )

        val sessions = GoalTrainingPlanEngine.generateScheduleForGoal(goal, workouts, nowEpochMs = nowMs)
        assertTrue(sessions.isNotEmpty())

        // 1. Session on 2026-11-08 (Sunday) should be COMPLETED
        val sundaySession = sessions.firstOrNull { it.dateKey == "2026-11-08" }
        assertNotNull(sundaySession)
        assertEquals(ScheduledSessionStatus.COMPLETED, sundaySession?.status)
        assertEquals("w_sunday", sundaySession?.matchedWorkout?.id)

        // 2. Session on 2026-11-05 (Kamis past date with no workout) should be SKIPPED
        val thursdayPastSession = sessions.firstOrNull { it.dateKey == "2026-11-05" }
        if (thursdayPastSession != null) {
            assertEquals(ScheduledSessionStatus.SKIPPED, thursdayPastSession.status)
        }

        // 3. Session on 2026-11-10 (Tuesday = TODAY) with no workout logged yet should be TODAY_PENDING
        val todaySession = sessions.firstOrNull { it.dateKey == "2026-11-10" }
        assertNotNull(todaySession)
        assertEquals(ScheduledSessionStatus.TODAY_PENDING, todaySession?.status)

        // 4. Session on 2026-11-12 (Thursday future) should be UPCOMING
        val futureThursday = sessions.firstOrNull { it.dateKey == "2026-11-12" }
        assertNotNull(futureThursday)
        assertEquals(ScheduledSessionStatus.UPCOMING, futureThursday?.status)

        // 5. Race day on 2026-11-29 should be RACE_DAY
        val raceDaySession = sessions.firstOrNull { it.dateKey == "2026-11-29" }
        assertNotNull(raceDaySession)
        assertEquals(SessionWorkoutType.RACE_DAY, raceDaySession?.workoutType)
    }

    @Test
    fun testAdaptiveAiSwitchesTuesdayToRecoveryAfterHardRun() {
        val cal = Calendar.getInstance()
        cal.set(2026, Calendar.NOVEMBER, 10, 8, 0, 0) // Tuesday, 10 Nov 2026
        val nowMs = cal.timeInMillis

        val encodedTitle = GoalTrainingPlanEngine.encodeGoalTitleWithSchedule(
            "Astra Run 10K",
            "2026-11-29",
            listOf(2, 4, 7),
            "2026-11-10"
        )
        val goal = TrainingGoal(
            id = "goal_adaptive_test",
            title = encodedTitle,
            targetDistanceCategory = "10K",
            targetTimeSeconds = 3000L,
            weeklyMileageTargetKm = 30L
        )

        // User ran a hard 14 km run yesterday (Monday, 9 Nov 2026) with HR 172
        val calMonday = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 9, 18, 30, 0)
        }
        val hardYesterdayWorkout = WorkoutLog(
            id = "w_hard_monday",
            title = "Hard Monday Night Run",
            distanceKm = 14.0,
            durationSeconds = 4200L,
            avgPaceSecondsPerKm = 300L,
            avgHeartRate = 172L,
            createdAt = Timestamp(Date(calMonday.timeInMillis))
        )

        val sessions = GoalTrainingPlanEngine.generateScheduleForGoal(goal, listOf(hardYesterdayWorkout), nowEpochMs = nowMs)
        val tuesdaySession = sessions.firstOrNull { it.dateKey == "2026-11-10" }

        assertNotNull(tuesdaySession)
        // Tuesday should be adaptively switched to RECOVERY because of yesterday's hard run!
        assertEquals(SessionWorkoutType.RECOVERY, tuesdaySession?.workoutType)
        assertTrue(tuesdaySession?.aiReason.orEmpty().contains("Recovery Run"))
    }

    @Test
    fun testTempoRunDistanceIsStrictlyShorterThanLongRun() {
        val cal = Calendar.getInstance()
        cal.set(2026, Calendar.NOVEMBER, 1, 8, 0, 0)
        val nowMs = cal.timeInMillis

        DistanceCategory.entries.forEach { cat ->
            val encodedTitle = GoalTrainingPlanEngine.encodeGoalTitleWithSchedule(
                "Test Run",
                "2026-11-29",
                listOf(2, 4, 7), // Tuesday, Thursday, Sunday
                "2026-11-01"
            )
            val goal = TrainingGoal(
                id = "goal_test_${cat.key}",
                title = encodedTitle,
                targetDistanceCategory = cat.key,
                targetTimeSeconds = 3000L,
                weeklyMileageTargetKm = 40L
            )

            val sessions = GoalTrainingPlanEngine.generateScheduleForGoal(goal, emptyList(), nowEpochMs = nowMs)
            val tempoSession = sessions.firstOrNull { it.workoutType == SessionWorkoutType.TEMPO }
            val longRunSession = sessions.firstOrNull { it.workoutType == SessionWorkoutType.EASY_LONG }

            if (tempoSession != null && longRunSession != null) {
                assertTrue(
                    "Tempo run (${tempoSession.recommendedDistanceKm} km) must be strictly shorter than Long run (${longRunSession.recommendedDistanceKm} km) for category ${cat.key}",
                    tempoSession.recommendedDistanceKm < longRunSession.recommendedDistanceKm
                )
            }
        }
    }
}
