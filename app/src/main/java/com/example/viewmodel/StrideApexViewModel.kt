package com.example.viewmodel

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.LocalRoomRepository
import com.example.data.model.BiomechanicsModule
import com.example.data.model.CloudBackupSnapshot
import com.example.data.model.DistanceCategory
import com.example.data.model.ExtractedStravaMetrics
import com.example.data.model.PersonalRecord
import com.example.data.model.TrainingGoal
import com.example.data.model.WorkoutLog
import com.example.data.repository.TrainingRepository
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.abs
import kotlin.math.roundToLong

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

sealed interface OcrScanUiState {
    data object Idle : OcrScanUiState
    data object Scanning : OcrScanUiState
    data class Extracted(val metrics: ExtractedStravaMetrics) : OcrScanUiState
    data class Error(val message: String) : OcrScanUiState
}

sealed interface CoachUiState {
    data object Idle : CoachUiState
    data object Loading : CoachUiState
    data class Streaming(val text: String) : CoachUiState
    data class Success(val text: String) : CoachUiState
    data class Error(val message: String) : CoachUiState
}

class StrideApexViewModel(
    private val localRepository: LocalRoomRepository,
    private val cloudRepository: TrainingRepository?,
    private val currentUserId: String?
) : ViewModel() {

    val isOfflineMode: Boolean = (currentUserId == null || cloudRepository == null)

    private val stravaOcrSchema = Schema.obj(
        properties = mapOf(
            "title" to Schema.string(description = "Workout title from the Strava screenshot"),
            "distanceKm" to Schema.double(description = "Total run distance in kilometers (e.g. 10.0)"),
            "durationSeconds" to Schema.integer(description = "Total moving duration in seconds"),
            "avgPaceSecondsPerKm" to Schema.integer(description = "Average pace in seconds per kilometer"),
            "avgHeartRate" to Schema.integer(description = "Average heart rate in BPM (use 160 if not shown)"),
            "cadenceSpm" to Schema.integer(description = "Average running cadence in steps per minute (use 180 if not shown)"),
            "elevationGainM" to Schema.integer(description = "Total elevation gain in meters (use 45 if not shown)"),
            "splitsSummary" to Schema.string(description = "Concise summary of kilometer/mile splits shown"),
            "aiCoachingInsight" to Schema.string(description = "Actionable physiological and biomechanical coaching insight based on the workout metrics")
        )
    )

    private val ocrGenerativeModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = stravaOcrSchema
            }
        )
    }

    private val coachGenerativeModel by lazy {
        Firebase.ai.generativeModel("gemini-3.8-flash")
    }

    private val workoutsFlow: Flow<List<WorkoutLog>> =
        if (!isOfflineMode && cloudRepository != null && currentUserId != null) {
            cloudRepository.observeWorkouts(currentUserId)
        } else {
            localRepository.observeWorkouts()
        }

    private val prsFlow: Flow<List<PersonalRecord>> =
        if (!isOfflineMode && cloudRepository != null && currentUserId != null) {
            cloudRepository.observePersonalRecords(currentUserId)
        } else {
            localRepository.observePersonalRecords()
        }

    private val goalsFlow: Flow<List<TrainingGoal>> =
        if (!isOfflineMode && cloudRepository != null && currentUserId != null) {
            cloudRepository.observeGoals(currentUserId)
        } else {
            localRepository.observeGoals()
        }

    private val backupsFlow: Flow<List<CloudBackupSnapshot>> =
        if (!isOfflineMode && cloudRepository != null && currentUserId != null) {
            cloudRepository.observeBackups(currentUserId)
        } else {
            localRepository.observeBackups()
        }

    val workoutsState: StateFlow<UiState<List<WorkoutLog>>> = workoutsFlow
        .map<List<WorkoutLog>, UiState<List<WorkoutLog>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing workouts", rootError)
            emit(UiState.Error(rootError.message ?: "Failed to observe workouts"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val prsState: StateFlow<UiState<List<PersonalRecord>>> = prsFlow
        .map<List<PersonalRecord>, UiState<List<PersonalRecord>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing PRs", rootError)
            emit(UiState.Error(rootError.message ?: "Failed to observe Personal Records"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val goalsState: StateFlow<UiState<List<TrainingGoal>>> = goalsFlow
        .map<List<TrainingGoal>, UiState<List<TrainingGoal>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing goals", rootError)
            emit(UiState.Error(rootError.message ?: "Failed to observe goals"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val backupsState: StateFlow<UiState<List<CloudBackupSnapshot>>> = backupsFlow
        .map<List<CloudBackupSnapshot>, UiState<List<CloudBackupSnapshot>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing backups", rootError)
            emit(UiState.Error(rootError.message ?: "Failed to observe backups"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    private val _ocrState = MutableStateFlow<OcrScanUiState>(OcrScanUiState.Idle)
    val ocrState: StateFlow<OcrScanUiState> = _ocrState.asStateFlow()

    private val _coachState = MutableStateFlow<CoachUiState>(CoachUiState.Idle)
    val coachState: StateFlow<CoachUiState> = _coachState.asStateFlow()

    private val _fatigueExponent = MutableStateFlow(1.06)
    val fatigueExponent: StateFlow<Double> = _fatigueExponent.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                localRepository.seedInitialStarterDataIfEmpty()
                if (!isOfflineMode && cloudRepository != null) {
                    cloudRepository.seedInitialStarterData()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Starter data seeding skipped or failed: ${e.message}")
            }
        }
    }

    fun clearStatusBanner() {
        _statusBanner.value = null
    }

    fun updateFatigueExponent(newExponent: Double) {
        _fatigueExponent.value = newExponent.coerceIn(1.03, 1.10)
    }

    fun analyzeStravaScreenshot(bitmap: Bitmap) {
        viewModelScope.launch {
            _ocrState.value = OcrScanUiState.Scanning
            try {
                val response = withTimeout(60_000L) {
                    ocrGenerativeModel.generateContent(
                        content {
                            image(bitmap)
                            text(
                                "Extract all running workout telemetry from this Strava activity screenshot into the required JSON schema. " +
                                    "Convert distance to kilometers, duration to total seconds, and pace to seconds per kilometer. " +
                                    "Provide a specific, physiological coaching insight based on the splits, pace, heart rate, and cadence."
                            )
                        }
                    )
                }
                val jsonText = response.text
                if (jsonText.isNullOrBlank()) {
                    _ocrState.value = OcrScanUiState.Error("Gemini returned an empty OCR response.")
                    return@launch
                }
                val json = JSONObject(jsonText)
                val distKm = json.optDouble("distanceKm", 10.0).coerceIn(0.1, 500.0)
                val durSec = json.optLong("durationSeconds", 2400L).coerceIn(1L, 864000L)
                val paceSec = json.optLong(
                    "avgPaceSecondsPerKm",
                    (durSec / distKm).roundToLong()
                ).coerceIn(60L, 3600L)

                val extracted = ExtractedStravaMetrics(
                    title = json.optString("title", "Strava OCR Workout").ifBlank { "Strava OCR Workout" },
                    distanceKm = distKm,
                    durationSeconds = durSec,
                    avgPaceSecondsPerKm = paceSec,
                    avgHeartRate = json.optLong("avgHeartRate", 168L).coerceIn(0L, 260L),
                    cadenceSpm = json.optLong("cadenceSpm", 182L).coerceIn(0L, 300L),
                    elevationGainM = json.optLong("elevationGainM", 55L).coerceIn(0L, 20000L),
                    splitsSummary = json.optString("splitsSummary", "Splits extracted via Gemini Vision"),
                    aiCoachingInsight = json.optString(
                        "aiCoachingInsight",
                        "Solid aerobic execution with consistent pacing."
                    )
                )
                _ocrState.value = OcrScanUiState.Extracted(extracted)
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Strava OCR timed out", e)
                _ocrState.value = OcrScanUiState.Error("AI screenshot extraction timed out after 60 seconds. Please try again.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Strava OCR extraction failed", e)
                _ocrState.value = OcrScanUiState.Error(e.localizedMessage ?: "Failed to extract Strava screenshot metrics.")
            }
        }
    }

    fun resetOcrState() {
        _ocrState.value = OcrScanUiState.Idle
    }

    fun saveWorkoutAndCheckPr(
        title: String,
        distanceKm: Double,
        durationSeconds: Long,
        avgHeartRate: Long,
        cadenceSpm: Long,
        elevationGainM: Long,
        splitsSummary: String,
        aiCoachingInsight: String,
        sourceType: String,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val safeDist = distanceKm.coerceIn(0.1, 500.0)
                val safeDur = durationSeconds.coerceIn(1L, 864000L)
                val avgPace = (safeDur / safeDist).roundToLong().coerceIn(60L, 3600L)
                val workout = WorkoutLog(
                    userId = currentUserId ?: "offline_local_user",
                    title = title.ifBlank { "Training Run" },
                    distanceKm = safeDist,
                    durationSeconds = safeDur,
                    avgPaceSecondsPerKm = avgPace,
                    avgHeartRate = avgHeartRate,
                    cadenceSpm = cadenceSpm,
                    elevationGainM = elevationGainM,
                    splitsSummary = splitsSummary,
                    aiCoachingInsight = aiCoachingInsight,
                    sourceType = sourceType
                )

                // Always persist locally in Room SQLite first for instant offline availability
                localRepository.addWorkout(workout)
                if (!isOfflineMode && cloudRepository != null) {
                    cloudRepository.addWorkout(workout)
                }

                // Check if this workout matches a standard PR distance bucket (within 8% tolerance)
                val matchedCategory = DistanceCategory.entries.firstOrNull { cat ->
                    abs(safeDist - cat.kilometers) / cat.kilometers <= 0.08
                }
                var prNewBanner: String? = null
                if (matchedCategory != null) {
                    val normalizedTimeSec = (avgPace * matchedCategory.kilometers).roundToLong()
                    val existingPrs = (prsState.value as? UiState.Success)?.data.orEmpty()
                    val existingForCat = existingPrs.firstOrNull { it.distanceCategory == matchedCategory.key }
                    if (existingForCat == null || normalizedTimeSec < existingForCat.bestTimeSeconds) {
                        val newPr = PersonalRecord(
                            userId = currentUserId ?: "offline_local_user",
                            distanceCategory = matchedCategory.key,
                            distanceMeters = matchedCategory.meters,
                            bestTimeSeconds = normalizedTimeSec,
                            previousTimeSeconds = existingForCat?.bestTimeSeconds ?: 0L,
                            paceSecondsPerKm = avgPace,
                            workoutTitle = workout.title
                        )
                        localRepository.upsertPersonalRecord(newPr)
                        if (!isOfflineMode && cloudRepository != null) {
                            cloudRepository.upsertPersonalRecord(newPr)
                        }
                        prNewBanner = "New ${matchedCategory.shortLabel} Personal Record logged!"
                    }
                }

                val storageTarget = if (isOfflineMode) "Local SQLite Database" else "Cloud Firestore & Local SQLite"
                _statusBanner.value = prNewBanner ?: "Workout '${workout.title}' saved to $storageTarget."
                _ocrState.value = OcrScanUiState.Idle
                onComplete()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save workout", e)
                _statusBanner.value = "Failed to save workout: ${e.localizedMessage ?: "Unknown error"}"
            }
        }
    }

    fun deleteWorkout(workoutId: String) {
        viewModelScope.launch {
            try {
                localRepository.deleteWorkout(workoutId)
                if (!isOfflineMode && cloudRepository != null) {
                    cloudRepository.deleteWorkout(workoutId)
                }
                _statusBanner.value = "Workout deleted."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _statusBanner.value = "Error deleting workout: ${e.localizedMessage}"
            }
        }
    }

    fun upsertPersonalRecordManual(
        category: DistanceCategory,
        bestTimeSeconds: Long,
        workoutTitle: String
    ) {
        viewModelScope.launch {
            try {
                val existingPrs = (prsState.value as? UiState.Success)?.data.orEmpty()
                val existing = existingPrs.firstOrNull { it.distanceCategory == category.key }
                val pace = (bestTimeSeconds / category.kilometers).roundToLong().coerceIn(60L, 3600L)
                val pr = PersonalRecord(
                    userId = currentUserId ?: "offline_local_user",
                    distanceCategory = category.key,
                    distanceMeters = category.meters,
                    bestTimeSeconds = bestTimeSeconds,
                    previousTimeSeconds = existing?.bestTimeSeconds ?: 0L,
                    paceSecondsPerKm = pace,
                    workoutTitle = workoutTitle.ifBlank { "${category.shortLabel} Time Trial" }
                )
                localRepository.upsertPersonalRecord(pr)
                if (!isOfflineMode && cloudRepository != null) {
                    cloudRepository.upsertPersonalRecord(pr)
                }
                _statusBanner.value = "Updated ${category.shortLabel} Personal Record."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _statusBanner.value = "Failed to update PR: ${e.localizedMessage}"
            }
        }
    }

    fun addTrainingGoal(
        title: String,
        category: DistanceCategory,
        targetTimeSeconds: Long,
        weeklyMileageTargetKm: Long,
        reminderEnabled: Boolean,
        reminderHour: Long,
        reminderMinute: Long
    ) {
        viewModelScope.launch {
            try {
                val goal = TrainingGoal(
                    userId = currentUserId ?: "offline_local_user",
                    title = title.ifBlank { "${category.shortLabel} Race Target" },
                    targetDistanceCategory = category.key,
                    targetTimeSeconds = targetTimeSeconds,
                    weeklyMileageTargetKm = weeklyMileageTargetKm,
                    reminderEnabled = reminderEnabled,
                    reminderHour = reminderHour,
                    reminderMinute = reminderMinute,
                    completed = false
                )
                localRepository.addGoal(goal)
                if (!isOfflineMode && cloudRepository != null) {
                    cloudRepository.addGoal(goal)
                }
                _statusBanner.value = "Race goal saved & reminders configured."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _statusBanner.value = "Failed to add goal: ${e.localizedMessage}"
            }
        }
    }

    fun toggleGoalCompleted(goal: TrainingGoal) {
        viewModelScope.launch {
            try {
                val updated = goal.copy(completed = !goal.completed)
                localRepository.updateGoal(updated)
                if (!isOfflineMode && cloudRepository != null) {
                    cloudRepository.updateGoal(updated)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _statusBanner.value = "Failed to update goal: ${e.localizedMessage}"
            }
        }
    }

    fun deleteGoal(goalId: String) {
        viewModelScope.launch {
            try {
                localRepository.deleteGoal(goalId)
                if (!isOfflineMode && cloudRepository != null) {
                    cloudRepository.deleteGoal(goalId)
                }
                _statusBanner.value = "Goal removed."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _statusBanner.value = "Failed to delete goal: ${e.localizedMessage}"
            }
        }
    }

    fun createCloudBackupCheckpoint(label: String) {
        viewModelScope.launch {
            try {
                val workouts = (workoutsState.value as? UiState.Success)?.data.orEmpty()
                val prs = (prsState.value as? UiState.Success)?.data.orEmpty()
                val goals = (goalsState.value as? UiState.Success)?.data.orEmpty()
                localRepository.createLocalBackupSnapshot(
                    label = label.ifBlank { "Training Backup Checkpoint" },
                    workouts = workouts,
                    prs = prs,
                    goals = goals
                )
                if (!isOfflineMode && cloudRepository != null) {
                    cloudRepository.createCloudBackupSnapshot(
                        label = label.ifBlank { "Cloud Backup Checkpoint" },
                        workouts = workouts,
                        prs = prs,
                        goals = goals
                    )
                    _statusBanner.value = "Cloud & Local SQLite backup checkpoint created (${workouts.size} workouts)."
                } else {
                    _statusBanner.value = "Offline SQLite backup checkpoint saved (${workouts.size} workouts). Sign in anytime to sync to Cloud."
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _statusBanner.value = "Backup checkpoint failed: ${e.localizedMessage}"
            }
        }
    }

    fun exportFullTrainingBackupJson(): String {
        val workouts = (workoutsState.value as? UiState.Success)?.data.orEmpty()
        val prs = (prsState.value as? UiState.Success)?.data.orEmpty()
        val goals = (goalsState.value as? UiState.Success)?.data.orEmpty()

        val root = JSONObject().apply {
            put("app", "StrideApex")
            put("mode", if (isOfflineMode) "OFFLINE_SQLITE" else "CLOUD_FIRESTORE_SYNC")
            put("exportedAtEpochMs", System.currentTimeMillis())
            put("workoutCount", workouts.size)
            put("prCount", prs.size)
            put("goalCount", goals.size)
            put(
                "workouts",
                JSONArray(
                    workouts.map { w ->
                        JSONObject().apply {
                            put("title", w.title)
                            put("distanceKm", w.distanceKm)
                            put("durationSeconds", w.durationSeconds)
                            put("avgPaceSecondsPerKm", w.avgPaceSecondsPerKm)
                            put("avgHeartRate", w.avgHeartRate)
                            put("cadenceSpm", w.cadenceSpm)
                            put("elevationGainM", w.elevationGainM)
                            put("splitsSummary", w.splitsSummary)
                        }
                    }
                )
            )
            put(
                "personalRecords",
                JSONArray(
                    prs.map { pr ->
                        JSONObject().apply {
                            put("distanceCategory", pr.distanceCategory)
                            put("bestTimeSeconds", pr.bestTimeSeconds)
                            put("paceSecondsPerKm", pr.paceSecondsPerKm)
                        }
                    }
                )
            )
        }
        return root.toString(2)
    }

    fun askBiomechanicsCoach(question: String, module: BiomechanicsModule) {
        if (question.isBlank()) return
        viewModelScope.launch {
            _coachState.value = CoachUiState.Loading
            val accumulated = StringBuilder()
            val prompt = buildString {
                append("You are an elite Olympic running biomechanics scientist and marathon coach. ")
                append("The athlete is currently studying '${module.title}' (Target: ${module.optimalTargetRange}; Common fault: ${module.commonFault}). ")
                append("Answer their question concisely in 3-4 actionable bullet points with specific joint angles, cadence cues, and corrective feel: ")
                append(question)
            }
            try {
                withTimeout(60_000L) {
                    coachGenerativeModel.generateContentStream(prompt).collect { chunk ->
                        val part = chunk.text.orEmpty()
                        accumulated.append(part)
                        _coachState.value = CoachUiState.Streaming(accumulated.toString())
                    }
                }
                if (accumulated.isNotBlank()) {
                    _coachState.value = CoachUiState.Success(accumulated.toString())
                } else {
                    _coachState.value = CoachUiState.Error("Biomechanics AI Coach returned an empty response.")
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Biomechanics AI Coach timed out", e)
                _coachState.value = CoachUiState.Error("Coaching analysis timed out after 60 seconds.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Biomechanics AI Coach failed", e)
                _coachState.value = CoachUiState.Error(e.localizedMessage ?: "Failed to generate coaching response.")
            }
        }
    }

    private companion object {
        const val TAG = "StrideApexVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
