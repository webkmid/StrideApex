package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.PersonalRecord
import com.example.data.model.TrainingGoal
import com.example.data.model.WorkoutLog
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class TrainingRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun addAndObserveWorkout_authenticatedOwner_succeeds() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = TrainingRepository(firestore)

        val workoutId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.addWorkout(
                WorkoutLog(
                    userId = aliceUid,
                    title = "Track Interval 5K",
                    distanceKm = 5.0,
                    durationSeconds = 1140L,
                    avgPaceSecondsPerKm = 228L,
                    avgHeartRate = 176L,
                    cadenceSpm = 186L,
                    elevationGainM = 15L,
                    splitsSummary = "3:48/km average",
                    aiCoachingInsight = "Consistent VO2 max effort.",
                    sourceType = "STRAVA_OCR"
                )
            )
        }
        assertTrue(workoutId.isNotBlank())

        val workouts = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeWorkouts(aliceUid).first { list -> list.any { it.id == workoutId } }
        }
        assertTrue(workouts.any { it.id == workoutId && it.title == "Track Interval 5K" })
    }

    @Test
    fun upsertPrAndAddGoalAndBackup_authenticatedOwner_succeeds() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = TrainingRepository(firestore)

        val prId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.upsertPersonalRecord(
                PersonalRecord(
                    userId = aliceUid,
                    distanceCategory = "5K",
                    distanceMeters = 5000L,
                    bestTimeSeconds = 1140L,
                    previousTimeSeconds = 1180L,
                    paceSecondsPerKm = 228L,
                    workoutTitle = "Track Interval 5K"
                )
            )
        }
        assertTrue(prId.isNotBlank())

        val goalId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.addGoal(
                TrainingGoal(
                    userId = aliceUid,
                    title = "Sub-18:30 5K",
                    targetDistanceCategory = "5K",
                    targetTimeSeconds = 1110L,
                    weeklyMileageTargetKm = 60L,
                    reminderEnabled = true,
                    reminderHour = 7L,
                    reminderMinute = 0L,
                    completed = false
                )
            )
        }
        assertTrue(goalId.isNotBlank())

        val backupId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createCloudBackupSnapshot(
                label = "Automated Rule Test Backup",
                workouts = emptyList(),
                prs = emptyList(),
                goals = emptyList()
            )
        }
        assertTrue(backupId.isNotBlank())
    }

    @Test
    fun getWorkoutById_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = TrainingRepository(firestore)
        val workoutId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.addWorkout(
                WorkoutLog(
                    userId = aliceUid,
                    title = "Alice Private Run",
                    distanceKm = 8.0,
                    durationSeconds = 2160L,
                    avgPaceSecondsPerKm = 270L,
                    avgHeartRate = 152L,
                    cadenceSpm = 176L,
                    elevationGainM = 50L,
                    splitsSummary = "4:30/km",
                    aiCoachingInsight = "Aerobic base run.",
                    sourceType = "MANUAL"
                )
            )
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = TrainingRepository(firestore)
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                bobRepo.getWorkoutById(aliceUid, workoutId)
            }
            fail("Expected FirebaseFirestoreException PERMISSION_DENIED for cross-user access")
        } catch (e: Exception) {
            val firestoreEx = e.unwrapFirestoreException()
            assertNotNull("Expected FirebaseFirestoreException cause, got $e", firestoreEx)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
        }
    }

    @Test
    fun observeWorkouts_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        auth.signOut()
        val repository = TrainingRepository(firestore)

        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeWorkouts(aliceUid).first()
            }
            fail("Expected PERMISSION_DENIED when unauthenticated")
        } catch (e: Exception) {
            val firestoreEx = e.unwrapFirestoreException()
            assertNotNull("Expected nested FirebaseFirestoreException, got $e", firestoreEx)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
        }
    }

    private companion object {
        const val ALICE_EMAIL = "alice@strideapex.test"
        const val BOB_EMAIL = "bob@strideapex.test"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
