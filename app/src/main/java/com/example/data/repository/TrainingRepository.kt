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
        val workoutsCol = db.collection("users").document(uid).collection("workouts")
        val docRef = if (workout.id.isNotBlank() && !workout.id.startsWith("local_")) {
            workoutsCol.document(workout.id)
        } else {
            workoutsCol.document()
        }
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
            "createdAt" to (workout.createdAt ?: FieldValue.serverTimestamp()),
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
        val category = DistanceCategory.fromKey(pr.distanceCategory)
        val docId = "pr_${category.key.lowercase()}"
        val colRef = db.collection("users").document(uid).collection("personalRecords")
        val docRef = colRef.document(docId)

        // Delete any other duplicate documents with this distanceCategory to ensure strict 1-to-1 mapping
        try {
            val snap = colRef.whereEqualTo("distanceCategory", category.key).get().await()
            for (doc in snap.documents) {
                if (doc.id != docId) {
                    doc.reference.delete().await()
                }
            }
        } catch (e: Exception) {
            // Best effort cleanup
        }

        val existingSnap = try {
            docRef.get().await()
        } catch (e: Exception) {
            null
        }

        val prevTime = if (existingSnap != null && existingSnap.exists()) {
            val oldBest = existingSnap.getLong("bestTimeSeconds") ?: 0L
            if (oldBest > 0L && oldBest != pr.bestTimeSeconds) oldBest else pr.previousTimeSeconds
        } else {
            pr.previousTimeSeconds
        }

        val payload = mapOf(
            "userId" to uid,
            "distanceCategory" to category.key,
            "distanceMeters" to category.meters,
            "bestTimeSeconds" to pr.bestTimeSeconds.coerceIn(1L, 864000L),
            "previousTimeSeconds" to prevTime.coerceIn(0L, 864000L),
            "paceSecondsPerKm" to pr.paceSecondsPerKm.coerceIn(60L, 3600L),
            "workoutTitle" to pr.workoutTitle.trim().take(120).ifBlank { "PR Effort" },
            "createdAt" to (existingSnap?.getTimestamp("createdAt") ?: FieldValue.serverTimestamp()),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            return docId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deletePersonalRecord(docId: String) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("personalRecords").document(docId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    suspend fun deletePersonalRecordByCategory(categoryKey: String) {
        val uid = requireUserId()
        val colRef = db.collection("users").document(uid).collection("personalRecords")
        try {
            val snap = colRef.whereEqualTo("distanceCategory", categoryKey).get().await()
            for (doc in snap.documents) {
                doc.reference.delete().await()
            }
            colRef.document("pr_${categoryKey.lowercase()}").delete().await()
        } catch (e: Exception) {
            // Best effort
        }
    }

    suspend fun deleteAllPersonalRecords() {
        val uid = requireUserId()
        val colRef = db.collection("users").document(uid).collection("personalRecords")
        try {
            val snap = colRef.get().await()
            for (doc in snap.documents) {
                doc.reference.delete().await()
            }
        } catch (e: Exception) {
            // Best effort
        }
    }

    suspend fun addGoal(goal: TrainingGoal): String {
        val uid = requireUserId()
        val goalsCol = db.collection("users").document(uid).collection("goals")
        val docRef = if (goal.id.isNotBlank() && !goal.id.startsWith("local_")) {
            goalsCol.document(goal.id)
        } else {
            goalsCol.document()
        }
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
            "createdAt" to (goal.createdAt ?: FieldValue.serverTimestamp()),
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

    suspend fun addBackupSnapshot(backup: CloudBackupSnapshot): String {
        val uid = requireUserId()
        val backupsCol = db.collection("users").document(uid).collection("backups")
        val docRef = if (backup.id.isNotBlank() && !backup.id.startsWith("local_")) {
            backupsCol.document(backup.id)
        } else {
            backupsCol.document()
        }
        val payload = mapOf(
            "userId" to uid,
            "backupLabel" to backup.backupLabel.trim().take(120).ifBlank { "Cloud Training Checkpoint" },
            "workoutCount" to backup.workoutCount.coerceIn(0L, 10000L),
            "prCount" to backup.prCount.coerceIn(0L, 1000L),
            "goalCount" to backup.goalCount.coerceIn(0L, 1000L),
            "summaryNotes" to backup.summaryNotes.take(4000),
            "createdAt" to (backup.createdAt ?: FieldValue.serverTimestamp()),
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

    private suspend fun <T : Any> fetchCollection(
        collectionName: String,
        clazz: Class<T>,
        mapper: (DocumentSnapshot, T) -> T
    ): List<T> {
        val uid = requireUserId()
        val snapshot = db.collection("users")
            .document(uid)
            .collection(collectionName)
            .get()
            .await()
        return snapshot.documents.mapNotNull { doc ->
            doc.toObject(clazz, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)?.let { mapper(doc, it) }
        }
    }

    suspend fun getWorkouts(): List<WorkoutLog> =
        fetchCollection("workouts", WorkoutLog::class.java) { doc, item -> item.copy(id = doc.id) }

    suspend fun getPersonalRecords(): List<PersonalRecord> =
        fetchCollection("personalRecords", PersonalRecord::class.java) { doc, item -> item.copy(id = doc.id) }

    suspend fun getGoals(): List<TrainingGoal> =
        fetchCollection("goals", TrainingGoal::class.java) { doc, item -> item.copy(id = doc.id) }

    suspend fun getBackups(): List<CloudBackupSnapshot> =
        fetchCollection("backups", CloudBackupSnapshot::class.java) { doc, item -> item.copy(id = doc.id) }

    suspend fun seedInitialStarterData() {
        // Starter dummy data removed so the app starts completely clean
    }
}
