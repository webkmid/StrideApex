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
                sourceType = if (workout.sourceType in listOf("STRAVA_OCR", "MANUAL")) workout.sourceType else "MANUAL",
                createdAtEpochMs = workout.createdAt?.toDate()?.time ?: System.currentTimeMillis()
            )
        )
        return id
    }

    suspend fun updateWorkout(workout: WorkoutLog) {
        val safePace = if (workout.avgPaceSecondsPerKm in 60..3600) {
            workout.avgPaceSecondsPerKm
        } else if (workout.distanceKm > 0.0) {
            (workout.durationSeconds / workout.distanceKm).roundToLong().coerceIn(60L, 3600L)
        } else {
            300L
        }
        val creationEpoch = workout.createdAt?.toDate()?.time ?: System.currentTimeMillis()
        dao.insertWorkout(
            LocalWorkoutEntity(
                id = workout.id,
                title = workout.title.trim().take(120).ifBlank { "Training Run" },
                distanceKm = workout.distanceKm.coerceIn(0.1, 500.0),
                durationSeconds = workout.durationSeconds.coerceIn(1L, 864000L),
                avgPaceSecondsPerKm = safePace,
                avgHeartRate = workout.avgHeartRate.coerceIn(0L, 260L),
                cadenceSpm = workout.cadenceSpm.coerceIn(0L, 300L),
                elevationGainM = workout.elevationGainM.coerceIn(0L, 20000L),
                splitsSummary = workout.splitsSummary.take(1000),
                aiCoachingInsight = workout.aiCoachingInsight.take(2000),
                sourceType = if (workout.sourceType in listOf("STRAVA_OCR", "MANUAL")) workout.sourceType else "MANUAL",
                createdAtEpochMs = creationEpoch
            )
        )
    }

    suspend fun deleteWorkout(workoutId: String) {
        dao.deleteWorkoutById(workoutId)
    }

    suspend fun upsertPersonalRecord(pr: PersonalRecord): String {
        val category = DistanceCategory.fromKey(pr.distanceCategory)
        val docId = "pr_${category.key.lowercase()}"
        val existing = dao.getPersonalRecordById(docId)
        val prevTime = if (existing != null && existing.bestTimeSeconds > 0L && existing.bestTimeSeconds != pr.bestTimeSeconds) {
            existing.bestTimeSeconds
        } else {
            pr.previousTimeSeconds
        }
        // Ensure no duplicate records exist for this category
        dao.deletePersonalRecordByCategory(category.key)
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

    suspend fun deletePersonalRecord(docId: String) {
        dao.deletePersonalRecordById(docId)
    }

    suspend fun deletePersonalRecordByCategory(category: String) {
        dao.deletePersonalRecordByCategory(category)
    }

    suspend fun deleteAllPersonalRecords() {
        dao.deleteAllPersonalRecords()
    }

    suspend fun getAllWorkoutsList(): List<WorkoutLog> {
        return dao.getAllWorkouts().map { it.toDomainModel() }
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
        // Starter dummy data removed so the app starts completely clean
    }
}
