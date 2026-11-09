package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.CloudBackupSnapshot
import com.example.data.model.DistanceCategory
import com.example.data.model.PersonalRecord
import com.example.data.model.TrainingGoal
import com.example.data.model.WorkoutLog
import com.example.util.OperationType
import com.example.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlin.math.roundToLong

class TrainingRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeWorkouts(userId: String = requireUserId()): Flow<List<WorkoutLog>> = flow {
        val path = "users/$userId/workouts"
        emitAll(
            db.collection("users").document(userId).collection("workouts")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        WorkoutLog::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observePersonalRecords(userId: String = requireUserId()): Flow<List<PersonalRecord>> = flow {
        val path = "users/$userId/personalRecords"
        emitAll(
            db.collection("users").document(userId).collection("personalRecords")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        PersonalRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.distanceMeters }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeGoals(userId: String = requireUserId()): Flow<List<TrainingGoal>> = flow {
        val path = "users/$userId/goals"
        emitAll(
            db.collection("users").document(userId).collection("goals")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        TrainingGoal::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeBackups(userId: String = requireUserId()): Flow<List<CloudBackupSnapshot>> = flow {
        val path = "users/$userId/backups"
        emitAll(
            db.collection("users").document(userId).collection("backups")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        CloudBackupSnapshot::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun getWorkoutById(ownerUserId: String, workoutId: String): WorkoutLog? {
        val docRef = db.collection("users").document(ownerUserId)
            .collection("workouts").document(workoutId)
        return try {
            docRef.get().await().toObject(
                WorkoutLog::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
    }

    suspend fun addWorkout(workout: WorkoutLog): String {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("workouts").document()
        val safePace = if (workout.avgPaceSecondsPerKm in 60..3600) {
            workout.avgPaceSecondsPerKm
        } else if (workout.distanceKm > 0.0) {
            (workout.durationSeconds / workout.distanceKm).roundToLong().coerceIn(60L, 3600L)
        } else {
            300L
        }
        val payload = mapOf(
            "userId" to uid,
            "title" to workout.title.trim().take(120).ifBlank { "Training Run" },
            "distanceKm" to workout.distanceKm.coerceIn(0.1, 500.0),
            "durationSeconds" to workout.durationSeconds.coerceIn(1L, 864000L),
            "avgPaceSecondsPerKm" to safePace,
            "avgHeartRate" to workout.avgHeartRate.coerceIn(0L, 260L),
            "cadenceSpm" to workout.cadenceSpm.coerceIn(0L, 300L),
            "elevationGainM" to workout.elevationGainM.coerceIn(0L, 20000L),
            "splitsSummary" to workout.splitsSummary.take(1000),
            "aiCoachingInsight" to workout.aiCoachingInsight.take(2000),
            "sourceType" to if (workout.sourceType in listOf("STRAVA_OCR", "MANUAL")) workout.sourceType else "MANUAL",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            return docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updateWorkout(workout: WorkoutLog) {
        val uid = requireUserId()
        require(workout.id.isNotBlank()) { "Workout ID must not be blank for update" }
        requireNotNull(workout.createdAt) { "Workout createdAt must not be null for update" }
        val docRef = db.collection("users").document(uid).collection("workouts").document(workout.id)
        try {
            docRef.set(workout.copy(userId = uid, updatedAt = null)).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteWorkout(workoutId: String) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("workouts").document(workoutId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    suspend fun upsertPersonalRecord(pr: PersonalRecord): String {
        val uid = requireUserId()
        val docId = "pr_${pr.distanceCategory.lowercase()}"
        val docRef = db.collection("users").document(uid).collection("personalRecords").document(docId)
        val existingSnap = try {
            docRef.get().await()
        } catch (e: Exception) {
            null
        }
        val category = DistanceCategory.fromKey(pr.distanceCategory)
        if (existingSnap != null && existingSnap.exists()) {
            val existingPr = existingSnap.toObject(
                PersonalRecord::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
            if (existingPr != null && existingPr.createdAt != null) {
                val updatedModel = existingPr.copy(
                    id = docId,
                    userId = uid,
                    distanceCategory = category.key,
                    distanceMeters = category.meters,
                    bestTimeSeconds = pr.bestTimeSeconds.coerceIn(1L, 864000L),
                    previousTimeSeconds = existingPr.bestTimeSeconds.coerceIn(0L, 864000L),
                    paceSecondsPerKm = pr.paceSecondsPerKm.coerceIn(60L, 3600L),
                    workoutTitle = pr.workoutTitle.trim().take(120).ifBlank { "PR Effort" },
                    updatedAt = null
                )
                try {
                    docRef.set(updatedModel).await()
                    return docId
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    handleFirestoreError(e, OperationType.UPDATE, docRef.path)
                    throw e
                }
            }
        }

        val createPayload = mapOf(
            "userId" to uid,
            "distanceCategory" to category.key,
            "distanceMeters" to category.meters,
            "bestTimeSeconds" to pr.bestTimeSeconds.coerceIn(1L, 864000L),
            "previousTimeSeconds" to pr.previousTimeSeconds.coerceIn(0L, 864000L),
            "paceSecondsPerKm" to pr.paceSecondsPerKm.coerceIn(60L, 3600L),
            "workoutTitle" to pr.workoutTitle.trim().take(120).ifBlank { "PR Effort" },
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(createPayload).await()
            return docId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun addGoal(goal: TrainingGoal): String {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("goals").document()
        val category = DistanceCategory.fromKey(goal.targetDistanceCategory)
        val payload = mapOf(
            "userId" to uid,
            "title" to goal.title.trim().take(120).ifBlank { "Race Goal" },
            "targetDistanceCategory" to category.key,
            "targetTimeSeconds" to goal.targetTimeSeconds.coerceIn(1L, 864000L),
            "weeklyMileageTargetKm" to goal.weeklyMileageTargetKm.coerceIn(1L, 400L),
            "reminderEnabled" to goal.reminderEnabled,
            "reminderHour" to goal.reminderHour.coerceIn(0L, 23L),
            "reminderMinute" to goal.reminderMinute.coerceIn(0L, 59L),
            "completed" to goal.completed,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            return docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updateGoal(goal: TrainingGoal) {
        val uid = requireUserId()
        require(goal.id.isNotBlank()) { "Goal ID must not be blank for update" }
        requireNotNull(goal.createdAt) { "Goal createdAt must not be null for update" }
        val docRef = db.collection("users").document(uid).collection("goals").document(goal.id)
        try {
            docRef.set(goal.copy(userId = uid, updatedAt = null)).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteGoal(goalId: String) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("goals").document(goalId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    suspend fun createCloudBackupSnapshot(
        label: String,
        workouts: List<WorkoutLog>,
        prs: List<PersonalRecord>,
        goals: List<TrainingGoal>
    ): String {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("backups").document()
        val totalKm = workouts.sumOf { it.distanceKm }
        val summaryDigest = buildString {
            append("Total Volume: ${"%.1f".format(totalKm)} km across ${workouts.size} workouts. ")
            if (prs.isNotEmpty()) {
                append("PRs: ")
                append(prs.joinToString { "${it.distanceCategory}=${it.bestTimeSeconds}s" })
                append(". ")
            }
            if (goals.isNotEmpty()) {
                append("Active Goals: ${goals.count { !it.completed }}.")
            }
        }.take(4000)

        val payload = mapOf(
            "userId" to uid,
            "backupLabel" to label.trim().take(120).ifBlank { "Cloud Training Checkpoint" },
            "workoutCount" to workouts.size.toLong().coerceIn(0L, 10000L),
            "prCount" to prs.size.toLong().coerceIn(0L, 1000L),
            "goalCount" to goals.size.toLong().coerceIn(0L, 1000L),
            "summaryNotes" to summaryDigest,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            return docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun seedInitialStarterData() {
        val uid = requireUserId()
        val existingWorkouts = try {
            db.collection("users").document(uid).collection("workouts").limit(1).get().await()
        } catch (e: Exception) {
            null
        }
        if (existingWorkouts != null && !existingWorkouts.isEmpty) return

        val starterWorkouts = listOf(
            WorkoutLog(
                userId = uid,
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
                userId = uid,
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
                userId = uid,
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
                userId = uid,
                distanceCategory = "5K",
                distanceMeters = 5000L,
                bestTimeSeconds = 1125L,
                previousTimeSeconds = 1162L,
                paceSecondsPerKm = 225L,
                workoutTitle = "VO2 Max 5K Time Trial"
            ),
            PersonalRecord(
                userId = uid,
                distanceCategory = "10K",
                distanceMeters = 10000L,
                bestTimeSeconds = 2380L,
                previousTimeSeconds = 2445L,
                paceSecondsPerKm = 238L,
                workoutTitle = "Strava Track Threshold 10K"
            ),
            PersonalRecord(
                userId = uid,
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
            userId = uid,
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

        createCloudBackupSnapshot(
            label = "Initial Cloud Sync Baseline",
            workouts = starterWorkouts,
            prs = starterPrs,
            goals = listOf(starterGoal)
        )
    }
}
