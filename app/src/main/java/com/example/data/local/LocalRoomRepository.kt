package com.example.data.local

import com.example.data.model.CloudBackupSnapshot
import com.example.data.model.DistanceCategory
import com.example.data.model.PersonalRecord
import com.example.data.model.TrainingGoal
import com.example.data.model.WorkoutLog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import kotlin.math.roundToLong

class LocalRoomRepository(private val dao: LocalTrainingDao) {

    fun observeWorkouts(): Flow<List<WorkoutLog>> =
        dao.observeWorkouts().map { list -> list.map { it.toDomainModel() } }

    fun observePersonalRecords(): Flow<List<PersonalRecord>> =
        dao.observePersonalRecords().map { list -> list.map { it.toDomainModel() } }

    fun observeGoals(): Flow<List<TrainingGoal>> =
        dao.observeGoals().map { list -> list.map { it.toDomainModel() } }

    fun observeBackups(): Flow<List<CloudBackupSnapshot>> =
        dao.observeBackups().map { list -> list.map { it.toDomainModel() } }

    suspend fun addWorkout(workout: WorkoutLog): String {
        val id = workout.id.ifBlank { "local_wk_${UUID.randomUUID()}" }
        val safePace = if (workout.avgPaceSecondsPerKm in 60..3600) {
            workout.avgPaceSecondsPerKm
        } else if (workout.distanceKm > 0.0) {
            (workout.durationSeconds / workout.distanceKm).roundToLong().coerceIn(60L, 3600L)
        } else {
            300L
        }
        dao.insertWorkout(
            LocalWorkoutEntity(
                id = id,
                title = workout.title.trim().take(120).ifBlank { "Training Run" },
                distanceKm = workout.distanceKm.coerceIn(0.1, 500.0),
                durationSeconds = workout.durationSeconds.coerceIn(1L, 864000L),
                avgPaceSecondsPerKm = safePace,
                avgHeartRate = workout.avgHeartRate.coerceIn(0L, 260L),
                cadenceSpm = workout.cadenceSpm.coerceIn(0L, 300L),
                elevationGainM = workout.elevationGainM.coerceIn(0L, 20000L),
                splitsSummary = workout.splitsSummary.take(1000),
                aiCoachingInsight = workout.aiCoachingInsight.take(2000),
                sourceType = if (workout.sourceType in listOf("STRAVA_OCR", "MANUAL")) workout.sourceType else "MANUAL"
            )
        )
        return id
    }

    suspend fun deleteWorkout(workoutId: String) {
        dao.deleteWorkoutById(workoutId)
    }

    suspend fun upsertPersonalRecord(pr: PersonalRecord): String {
        val category = DistanceCategory.fromKey(pr.distanceCategory)
        val docId = "pr_${category.key.lowercase()}"
        val existing = dao.getPersonalRecordById(docId)
        val prevTime = existing?.bestTimeSeconds ?: pr.previousTimeSeconds
        dao.insertPersonalRecord(
            LocalPrEntity(
                id = docId,
                distanceCategory = category.key,
                distanceMeters = category.meters,
                bestTimeSeconds = pr.bestTimeSeconds.coerceIn(1L, 864000L),
                previousTimeSeconds = prevTime.coerceIn(0L, 864000L),
                paceSecondsPerKm = pr.paceSecondsPerKm.coerceIn(60L, 3600L),
                workoutTitle = pr.workoutTitle.trim().take(120).ifBlank { "PR Effort" }
            )
        )
        return docId
    }

    suspend fun addGoal(goal: TrainingGoal): String {
        val id = goal.id.ifBlank { "local_goal_${UUID.randomUUID()}" }
        val category = DistanceCategory.fromKey(goal.targetDistanceCategory)
        dao.insertGoal(
            LocalGoalEntity(
                id = id,
                title = goal.title.trim().take(120).ifBlank { "Race Goal" },
                targetDistanceCategory = category.key,
                targetTimeSeconds = goal.targetTimeSeconds.coerceIn(1L, 864000L),
                weeklyMileageTargetKm = goal.weeklyMileageTargetKm.coerceIn(1L, 400L),
                reminderEnabled = goal.reminderEnabled,
                reminderHour = goal.reminderHour.coerceIn(0L, 23L),
                reminderMinute = goal.reminderMinute.coerceIn(0L, 59L),
                completed = goal.completed
            )
        )
        return id
    }

    suspend fun updateGoal(goal: TrainingGoal) {
        if (goal.id.isBlank()) return
        val category = DistanceCategory.fromKey(goal.targetDistanceCategory)
        dao.insertGoal(
            LocalGoalEntity(
                id = goal.id,
                title = goal.title.trim().take(120).ifBlank { "Race Goal" },
                targetDistanceCategory = category.key,
                targetTimeSeconds = goal.targetTimeSeconds.coerceIn(1L, 864000L),
                weeklyMileageTargetKm = goal.weeklyMileageTargetKm.coerceIn(1L, 400L),
                reminderEnabled = goal.reminderEnabled,
                reminderHour = goal.reminderHour.coerceIn(0L, 23L),
                reminderMinute = goal.reminderMinute.coerceIn(0L, 59L),
                completed = goal.completed
            )
        )
    }

    suspend fun deleteGoal(goalId: String) {
        dao.deleteGoalById(goalId)
    }

    suspend fun createLocalBackupSnapshot(
        label: String,
        workouts: List<WorkoutLog>,
        prs: List<PersonalRecord>,
        goals: List<TrainingGoal>
    ): String {
        val id = "local_bkp_${UUID.randomUUID()}"
        val totalKm = workouts.sumOf { it.distanceKm }
        val summaryDigest = buildString {
            append("SQLite Snapshot: ${"%.1f".format(totalKm)} km across ${workouts.size} workouts. ")
            if (prs.isNotEmpty()) {
                append("PRs: ")
                append(prs.joinToString { "${it.distanceCategory}=${it.bestTimeSeconds}s" })
                append(". ")
            }
            if (goals.isNotEmpty()) {
                append("Active Goals: ${goals.count { !it.completed }}.")
            }
        }.take(4000)

        dao.insertBackup(
            LocalBackupEntity(
                id = id,
                backupLabel = label.trim().take(120).ifBlank { "Local SQLite Snapshot" },
                workoutCount = workouts.size.toLong(),
                prCount = prs.size.toLong(),
                goalCount = goals.size.toLong(),
                summaryNotes = summaryDigest
            )
        )
        return id
    }

    suspend fun seedInitialStarterDataIfEmpty() {
        if (dao.getWorkoutCount() > 0) return

        val starterWorkouts = listOf(
            WorkoutLog(
                id = "local_wk_1",
                title = "Strava Track Threshold 10K",
                distanceKm = 10.0,
                durationSeconds = 2380L,
                avgPaceSecondsPerKm = 238L,
                avgHeartRate = 171L,
                cadenceSpm = 184L,
                elevationGainM = 42L,
                splitsSummary = "KM 1-3: 4:02/km | KM 4-7: 3:58/km | KM 8-10: 3:53/km",
                aiCoachingInsight = "High mechanical efficiency at 184 SPM. Negative 9s/km split indicates strong lactate clearance.",
                sourceType = "STRAVA_OCR"
            ),
            WorkoutLog(
                id = "local_wk_2",
                title = "Sunday Aerobic Long Progression",
                distanceKm = 21.1,
                durationSeconds = 5486L,
                avgPaceSecondsPerKm = 260L,
                avgHeartRate = 158L,
                cadenceSpm = 178L,
                elevationGainM = 165L,
                splitsSummary = "KM 1-10: 4:26/km | KM 11-18: 4:16/km | KM 19-21.1: 4:08/km",
                aiCoachingInsight = "Minimal cardiac drift (<3%) over 21.1km. Ready for sub-1:30 Half Marathon pacing.",
                sourceType = "STRAVA_OCR"
            ),
            WorkoutLog(
                id = "local_wk_3",
                title = "VO2 Max 5K Time Trial",
                distanceKm = 5.0,
                durationSeconds = 1125L,
                avgPaceSecondsPerKm = 225L,
                avgHeartRate = 179L,
                cadenceSpm = 188L,
                elevationGainM = 18L,
                splitsSummary = "KM 1: 3:44 | KM 2: 3:46 | KM 3: 3:47 | KM 4: 3:45 | KM 5: 3:43",
                aiCoachingInsight = "Elite pacing discipline with 4-second spread across all 5 splits and crisp midfoot turnover.",
                sourceType = "STRAVA_OCR"
            )
        )
        starterWorkouts.forEach { addWorkout(it) }

        val starterPrs = listOf(
            PersonalRecord(
                distanceCategory = "5K",
                distanceMeters = 5000L,
                bestTimeSeconds = 1125L,
                previousTimeSeconds = 1162L,
                paceSecondsPerKm = 225L,
                workoutTitle = "VO2 Max 5K Time Trial"
            ),
            PersonalRecord(
                distanceCategory = "10K",
                distanceMeters = 10000L,
                bestTimeSeconds = 2380L,
                previousTimeSeconds = 2445L,
                paceSecondsPerKm = 238L,
                workoutTitle = "Strava Track Threshold 10K"
            ),
            PersonalRecord(
                distanceCategory = "HALF_MARATHON",
                distanceMeters = 21097L,
                bestTimeSeconds = 5486L,
                previousTimeSeconds = 5610L,
                paceSecondsPerKm = 260L,
                workoutTitle = "Sunday Aerobic Long Progression"
            )
        )
        starterPrs.forEach { upsertPersonalRecord(it) }

        val starterGoal = TrainingGoal(
            id = "local_goal_1",
            title = "Sub-39:00 10K Road Championship",
            targetDistanceCategory = "10K",
            targetTimeSeconds = 2340L,
            weeklyMileageTargetKm = 65L,
            reminderEnabled = true,
            reminderHour = 7L,
            reminderMinute = 30L,
            completed = false
        )
        addGoal(starterGoal)

        createLocalBackupSnapshot(
            label = "Local SQLite Baseline Snapshot",
            workouts = starterWorkouts,
            prs = starterPrs,
            goals = listOf(starterGoal)
        )
    }
}
