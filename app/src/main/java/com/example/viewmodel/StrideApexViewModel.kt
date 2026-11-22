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
import com.example.domain.GoalTrainingPlanEngine
import com.example.domain.RacePredictionEngine
import com.example.domain.WorkoutRefreshEngine
import com.example.ui.components.parseSplitsSummary
import com.example.util.StravaLocalOcrParser
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.RequestOptions
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
            modelName = "gemini-2.5-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = stravaOcrSchema
            },
            requestOptions = RequestOptions(timeoutInMillis = 30_000L)
        )
    }

    private val coachGenerativeModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-2.5-flash",
            requestOptions = RequestOptions(timeoutInMillis = 30_000L)
        )
    }

    private val syncMutex = Mutex()
    private val mirrorMutex = Mutex()
    private val initialCloudSyncDone = MutableStateFlow(isOfflineMode)

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val workoutsFlow: Flow<List<WorkoutLog>> =
        if (!isOfflineMode && cloudRepository != null && currentUserId != null) {
            cloudRepository.observeWorkouts(currentUserId).onEach { cloudList ->
                mirrorCloudSnapshotToLocal(
                    cloudItems = cloudList,
                    localItemsProvider = { localRepository.getAllWorkoutsList() },
                    idOf = { it.id },
                    upsertLocal = { localRepository.updateWorkout(it) },
                    deleteLocal = { localRepository.deleteWorkout(it) }
                )
            }
        } else {
            localRepository.observeWorkouts()
        }

    private val prsFlow: Flow<List<PersonalRecord>> =
        if (!isOfflineMode && cloudRepository != null && currentUserId != null) {
            cloudRepository.observePersonalRecords(currentUserId).onEach { cloudPrs ->
                mirrorPrsToLocal(cloudPrs)
            }
        } else {
            localRepository.observePersonalRecords()
        }

    private val goalsFlow: Flow<List<TrainingGoal>> =
        if (!isOfflineMode && cloudRepository != null && currentUserId != null) {
            cloudRepository.observeGoals(currentUserId).onEach { cloudGoals ->
                mirrorCloudSnapshotToLocal(
                    cloudItems = cloudGoals,
                    localItemsProvider = { localRepository.getAllGoalsList() },
                    idOf = { it.id },
                    upsertLocal = { localRepository.updateGoal(it) },
                    deleteLocal = { localRepository.deleteGoal(it) }
                )
            }
        } else {
            localRepository.observeGoals()
        }

    private val backupsFlow: Flow<List<CloudBackupSnapshot>> =
        if (!isOfflineMode && cloudRepository != null && currentUserId != null) {
            cloudRepository.observeBackups(currentUserId).onEach { cloudBackups ->
                mirrorCloudSnapshotToLocal(
                    cloudItems = cloudBackups,
                    localItemsProvider = { localRepository.getAllBackupsList() },
                    idOf = { it.id },
                    upsertLocal = { localRepository.upsertBackupSnapshot(it) },
                    deleteLocal = { localRepository.deleteBackup(it) }
                )
            }
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

    val prsState: StateFlow<UiState<List<PersonalRecord>>> = combine(
        prsFlow,
        workoutsFlow
    ) { prsList, workoutsList ->
        if (workoutsList.isEmpty()) {
            emptyList()
        } else {
            // Group by category, deduplicate, and pick the best record per category
            prsList
                .groupBy { it.distanceCategory }
                .mapNotNull { (_, records) ->
                    records.minByOrNull { it.bestTimeSeconds }
                }
                .filter { pr ->
                    val category = DistanceCategory.fromKey(pr.distanceCategory)
                    // A PR category is validated if any workout has distance >= category.kilometers * 0.85
                    // e.g. A 10K workout validates both 10K and 5K
                    workoutsList.any { w ->
                        w.distanceKm >= (category.kilometers * 0.85)
                    }
                }
                .sortedBy { it.distanceMeters }
        }
    }
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

    init {
        viewModelScope.launch {
            if (!isOfflineMode && cloudRepository != null && currentUserId != null) {
                syncLocalAndCloud(showBannerOnEmpty = false)
            } else {
                try {
                    val remaining = localRepository.getAllWorkoutsList()
                    if (remaining.isEmpty()) {
                        localRepository.deleteAllPersonalRecords()
                    } else {
                        recalculatePrsFromRemainingWorkouts()
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "Error synchronizing local PRs on startup", e)
                }
            }
        }
    }

    fun syncLocalAndCloudNow() {
        viewModelScope.launch {
            syncLocalAndCloud(showBannerOnEmpty = true)
        }
    }

    private suspend fun syncLocalAndCloud(showBannerOnEmpty: Boolean) {
        val repo = cloudRepository ?: return
        val uid = currentUserId ?: return
        if (isOfflineMode) return

        syncMutex.withLock {
            _isSyncing.value = true
            try {
                val cloudWorkouts = repo.getWorkouts().toMutableList()
                val localWorkouts = localRepository.getAllWorkoutsList()
                val uploadedWorkouts = syncEntityCollection(
                    cloudItems = cloudWorkouts,
                    localItems = localWorkouts,
                    idOf = { it.id },
                    isEquivalent = { cloud, local ->
                        abs(cloud.distanceKm - local.distanceKm) < 0.01 &&
                            abs(cloud.durationSeconds - local.durationSeconds) <= 2L &&
                            cloud.title.equals(local.title, ignoreCase = true) &&
                            abs((cloud.createdAt?.toDate()?.time ?: 0L) - (local.createdAt?.toDate()?.time ?: 0L)) < 60_000L
                    },
                    uploadToCloud = { localItem ->
                        val cleanItem = localItem.copy(id = "", userId = uid)
                        val cloudId = repo.addWorkout(cleanItem)
                        cleanItem.copy(id = cloudId)
                    },
                    upsertLocal = { item -> localRepository.updateWorkout(item) },
                    deleteLocal = { id -> localRepository.deleteWorkout(id) }
                )

                val cloudGoals = repo.getGoals().toMutableList()
                val localGoals = localRepository.getAllGoalsList()
                val uploadedGoals = syncEntityCollection(
                    cloudItems = cloudGoals,
                    localItems = localGoals,
                    idOf = { it.id },
                    isEquivalent = { cloud, local ->
                        GoalTrainingPlanEngine.stripScheduleSuffix(cloud.title)
                            .equals(GoalTrainingPlanEngine.stripScheduleSuffix(local.title), ignoreCase = true) &&
                            cloud.targetDistanceCategory == local.targetDistanceCategory &&
                            cloud.targetTimeSeconds == local.targetTimeSeconds
                    },
                    uploadToCloud = { localItem ->
                        val cleanItem = localItem.copy(id = "", userId = uid)
                        val cloudId = repo.addGoal(cleanItem)
                        cleanItem.copy(id = cloudId)
                    },
                    upsertLocal = { item -> localRepository.updateGoal(item) },
                    deleteLocal = { id -> localRepository.deleteGoal(id) }
                )

                val cloudBackups = repo.getBackups().toMutableList()
                val localBackups = localRepository.getAllBackupsList()
                val uploadedBackups = syncEntityCollection(
                    cloudItems = cloudBackups,
                    localItems = localBackups,
                    idOf = { it.id },
                    isEquivalent = { cloud, local ->
                        cloud.backupLabel == local.backupLabel &&
                            cloud.workoutCount == local.workoutCount &&
                            abs((cloud.createdAt?.toDate()?.time ?: 0L) - (local.createdAt?.toDate()?.time ?: 0L)) < 60_000L
                    },
                    uploadToCloud = { localItem ->
                        val cleanItem = localItem.copy(id = "", userId = uid)
                        val cloudId = repo.addBackupSnapshot(cleanItem)
                        cleanItem.copy(id = cloudId)
                    },
                    upsertLocal = { item -> localRepository.upsertBackupSnapshot(item) },
                    deleteLocal = { id -> localRepository.deleteBackup(id) }
                )

                val cloudPrs = repo.getPersonalRecords()
                if (cloudWorkouts.isEmpty()) {
                    localRepository.deleteAllPersonalRecords()
                    if (cloudPrs.isNotEmpty()) {
                        repo.deleteAllPersonalRecords()
                    }
                } else if (uploadedWorkouts > 0 || cloudPrs.isEmpty()) {
                    recalculatePrsFromRemainingWorkouts(cloudWorkouts)
                } else {
                    syncPrsListToLocal(cloudPrs)
                }

                initialCloudSyncDone.value = true
                val totalUploaded = uploadedWorkouts + uploadedGoals + uploadedBackups
                if (totalUploaded > 0) {
                    _statusBanner.value = buildString {
                        append("Sinkronisasi Cloud selesai: ")
                        val parts = buildList {
                            if (uploadedWorkouts > 0) add("$uploadedWorkouts sesi lari")
                            if (uploadedGoals > 0) add("$uploadedGoals target")
                            if (uploadedBackups > 0) add("$uploadedBackups backup")
                        }
                        append(parts.joinToString(", "))
                        append(" diunggah & disinkronkan.")
                    }
                } else if (showBannerOnEmpty) {
                    _statusBanner.value = "Data lokal SQLite & Cloud Firestore sudah tersinkron (${cloudWorkouts.size} sesi)."
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Bidirectional cloud sync failed or deferred offline", e)
            } finally {
                _isSyncing.value = false
            }
        }
    }

    private suspend fun <T> syncEntityCollection(
        cloudItems: MutableList<T>,
        localItems: List<T>,
        idOf: (T) -> String,
        isEquivalent: (cloud: T, local: T) -> Boolean,
        uploadToCloud: suspend (T) -> T,
        upsertLocal: suspend (T) -> Unit,
        deleteLocal: suspend (String) -> Unit
    ): Int {
        var uploaded = 0
        for (localItem in localItems) {
            val localId = idOf(localItem)
            if (!localId.startsWith("local_")) continue
            val existingMatch = cloudItems.firstOrNull { isEquivalent(it, localItem) }
            if (existingMatch != null) {
                deleteLocal(localId)
                upsertLocal(existingMatch)
            } else {
                val uploadedItem = uploadToCloud(localItem)
                deleteLocal(localId)
                upsertLocal(uploadedItem)
                cloudItems.add(uploadedItem)
                uploaded++
            }
        }
        val cloudIds = cloudItems.mapTo(HashSet()) { idOf(it) }
        cloudItems.forEach { upsertLocal(it) }
        localItems.forEach { localItem ->
            val localId = idOf(localItem)
            if (localId.isNotBlank() && !localId.startsWith("local_") && localId !in cloudIds) {
                deleteLocal(localId)
            }
        }
        return uploaded
    }

    private suspend fun <T> mirrorCloudSnapshotToLocal(
        cloudItems: List<T>,
        localItemsProvider: suspend () -> List<T>,
        idOf: (T) -> String,
        upsertLocal: suspend (T) -> Unit,
        deleteLocal: suspend (String) -> Unit
    ) {
        if (!initialCloudSyncDone.value) return
        mirrorMutex.withLock {
            try {
                val localItems = localItemsProvider()
                val cloudIds = cloudItems.mapTo(HashSet()) { idOf(it) }
                cloudItems.forEach { upsertLocal(it) }
                localItems.forEach { localItem ->
                    val localId = idOf(localItem)
                    if (localId.isNotBlank() && !localId.startsWith("local_") && localId !in cloudIds) {
                        deleteLocal(localId)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Error mirroring cloud snapshot to Room SQLite", e)
            }
        }
    }

    private suspend fun mirrorPrsToLocal(cloudPrs: List<PersonalRecord>) {
        if (!initialCloudSyncDone.value) return
        mirrorMutex.withLock {
            try {
                syncPrsListToLocal(cloudPrs)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Error mirroring cloud PRs to Room SQLite", e)
            }
        }
    }

    private suspend fun syncPrsListToLocal(cloudPrs: List<PersonalRecord>) {
        val localPrs = localRepository.getAllPersonalRecordsList()
        val cloudCategories = cloudPrs.mapTo(HashSet()) { it.distanceCategory }
        localPrs.forEach { localPr ->
            if (localPr.distanceCategory !in cloudCategories) {
                localRepository.deletePersonalRecordByCategory(localPr.distanceCategory)
            }
        }
        cloudPrs.forEach { cloudPr ->
            localRepository.upsertPersonalRecord(cloudPr)
        }
    }

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

    private val _aiPlanLoading = MutableStateFlow(false)
    val aiPlanLoading: StateFlow<Boolean> = _aiPlanLoading.asStateFlow()

    private val _aiPlanCoachingInsight = MutableStateFlow<String?>(null)
    val aiPlanCoachingInsight: StateFlow<String?> = _aiPlanCoachingInsight.asStateFlow()

    private val _refreshingWorkoutId = MutableStateFlow<String?>(null)
    val refreshingWorkoutId: StateFlow<String?> = _refreshingWorkoutId.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    fun clearStatusBanner() {
        _statusBanner.value = null
    }

    fun updateFatigueExponent(newExponent: Double) {
        _fatigueExponent.value = newExponent.coerceIn(1.03, 1.10)
    }

    fun analyzeStravaScreenshot(bitmap: Bitmap) {
        viewModelScope.launch {
            _ocrState.value = OcrScanUiState.Scanning

            // 1. Fast on-device ML Kit OCR fallback/primary
            var localExtracted: ExtractedStravaMetrics? = null
            try {
                localExtracted = StravaLocalOcrParser.parseStravaImage(bitmap)
                if (localExtracted != null && (localExtracted.distanceKm > 0 || localExtracted.durationSeconds > 0)) {
                    _ocrState.value = OcrScanUiState.Extracted(localExtracted)
                    Log.d(TAG, "Local ML Kit OCR extracted successfully: $localExtracted")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Local ML Kit OCR parsing error", e)
            }

            // 2. In parallel/subsequently, attempt Gemini AI Vision to enrich insights or as fallback
            try {
                val maxDim = 1080
                val scaledBitmap = if (bitmap.width > maxDim || bitmap.height > maxDim) {
                    val ratio = maxDim.toFloat() / maxOf(bitmap.width, bitmap.height)
                    val newW = (bitmap.width * ratio).toInt().coerceAtLeast(1)
                    val newH = (bitmap.height * ratio).toInt().coerceAtLeast(1)
                    Bitmap.createScaledBitmap(bitmap, newW, newH, true)
                } else {
                    bitmap
                }

                val response = withTimeout(30_000L) {
                    ocrGenerativeModel.generateContent(
                        content {
                            image(scaledBitmap)
                            text(
                                "Extract running workout telemetry from this Strava screenshot into the required JSON schema. " +
                                    "CRITICAL RULES: " +
                                    "1) For durationSeconds, prefer 'Moving Time' over 'Elapsed Time' if both are shown. " +
                                    "2) If total distance is not explicitly labeled or shown on the image (e.g. a Splits screenshot), " +
                                    "sum the split distances in the table (e.g. 1 + 1 + 1 + 1 + 0.9 = 4.9) OR calculate distanceKm = durationSeconds / avgPaceSecondsPerKm. Never pick just split 1. " +
                                    "3) For 'splitsSummary', extract EVERY split row visible in the Splits table (including fractional last split like 0.9), " +
                                    "formatted strictly as 'KM 1: 7:54/km | KM 2: 6:58/km | KM 0.9: 6:45/km', ignoring the rightmost elevation delta column (-3, 2, 1, 0, -1) and ignoring Pace chart axis ticks (6:00..12:00, 1 km..4 km). " +
                                    "4) For 'aiCoachingInsight', provide an in-depth, structured, and actionable evaluation in Indonesian (with pacing analysis, biomechanical/cadence observation, and specific recommendations for the next workout)."
                            )
                        }
                    )
                }
                val jsonText = response.text
                if (!jsonText.isNullOrBlank()) {
                    val json = JSONObject(jsonText)
                    val rawDistKm = json.optDouble("distanceKm", 0.0).coerceIn(0.0, 500.0)
                    val durSec = json.optLong("durationSeconds", 0L).coerceIn(0L, 864000L)
                    val paceSec = json.optLong(
                        "avgPaceSecondsPerKm",
                        if (rawDistKm > 0.0 && durSec > 0L) (durSec / rawDistKm).roundToLong() else 0L
                    ).coerceIn(0L, 3600L)

                    val effectiveDurSec = if (durSec > 0L) durSec else (localExtracted?.durationSeconds ?: 0L)
                    val effectivePaceSec = if (paceSec > 0L) {
                        paceSec
                    } else if (localExtracted?.avgPaceSecondsPerKm != null && localExtracted.avgPaceSecondsPerKm > 0L) {
                        localExtracted.avgPaceSecondsPerKm
                    } else if (rawDistKm > 0.0 && effectiveDurSec > 0L) {
                        (effectiveDurSec / rawDistKm).roundToLong()
                    } else {
                        0L
                    }

                    val calcFromDurationAndPace = if (effectiveDurSec > 0L && effectivePaceSec > 0L) {
                        ((effectiveDurSec.toDouble() / effectivePaceSec.toDouble()) * 100.0).roundToLong() / 100.0
                    } else {
                        0.0
                    }

                    val localDist = localExtracted?.distanceKm ?: 0.0
                    val finalDistKm = if (calcFromDurationAndPace > 0.0 && (rawDistKm <= 0.0 || (rawDistKm <= 1.05 && calcFromDurationAndPace >= 1.5))) {
                        calcFromDurationAndPace
                    } else if (localDist > 1.0 && rawDistKm <= 1.0) {
                        localDist
                    } else if (rawDistKm > 0.0) {
                        rawDistKm
                    } else if (calcFromDurationAndPace > 0.0) {
                        calcFromDurationAndPace
                    } else if (localDist > 0.0) {
                        localDist
                    } else {
                        4.0
                    }

                    val resolvedPace = if (paceSec > 0) paceSec else (localExtracted?.avgPaceSecondsPerKm ?: 450L)
                    val geminiSplitsRaw = json.optString("splitsSummary", "")
                    val localSplitsRaw = localExtracted?.splitsSummary.orEmpty()
                    val parsedGeminiSplits = parseSplitsSummary(geminiSplitsRaw, resolvedPace)
                    val parsedLocalSplits = parseSplitsSummary(localSplitsRaw, resolvedPace)
                    val mergedSplitsSummary = when {
                        parsedGeminiSplits.size > parsedLocalSplits.size ->
                            parsedGeminiSplits.joinToString(" | ") { "KM ${it.kmLabel}: ${it.paceStr}/km" }
                        parsedLocalSplits.isNotEmpty() ->
                            parsedLocalSplits.joinToString(" | ") { "KM ${it.kmLabel}: ${it.paceStr}/km" }
                        else -> geminiSplitsRaw.ifBlank { localSplitsRaw }
                    }

                    val enriched = ExtractedStravaMetrics(
                        title = json.optString("title", localExtracted?.title ?: "Strava Run").ifBlank { localExtracted?.title ?: "Strava Run" },
                        distanceKm = if (finalDistKm > 0.0) finalDistKm else (localExtracted?.distanceKm ?: 4.0),
                        durationSeconds = if (durSec > 0) durSec else (localExtracted?.durationSeconds ?: 1800L),
                        avgPaceSecondsPerKm = resolvedPace,
                        avgHeartRate = maxOf(json.optLong("avgHeartRate", 0L), localExtracted?.avgHeartRate ?: 0L).coerceIn(0L, 260L),
                        cadenceSpm = maxOf(json.optLong("cadenceSpm", 0L), localExtracted?.cadenceSpm ?: 0L).coerceIn(0L, 300L),
                        elevationGainM = maxOf(json.optLong("elevationGainM", 0L), localExtracted?.elevationGainM ?: 0L).coerceIn(0L, 20000L),
                        splitsSummary = mergedSplitsSummary,
                        aiCoachingInsight = json.optString("aiCoachingInsight", localExtracted?.aiCoachingInsight ?: "Pacing konsisten."),
                        workoutDateEpochMs = localExtracted?.workoutDateEpochMs,
                        detectedDateStr = localExtracted?.detectedDateStr.orEmpty()
                    )
                    _ocrState.value = OcrScanUiState.Extracted(enriched)
                }
            } catch (e: Exception) {
                if (localExtracted != null && (localExtracted.distanceKm > 0 || localExtracted.durationSeconds > 0)) {
                    Log.i(TAG, "Gemini call failed (${e.localizedMessage}), but local ML Kit OCR result is preserved.")
                } else {
                    Log.e(TAG, "Both ML Kit and Gemini OCR failed", e)
                    val message = when (e) {
                        is TimeoutCancellationException -> "Ekstraksi AI melebihi batas waktu. Data lokal tidak terdeteksi. Silakan isi form di bawah atau kirim foto layar utama Strava (Distance, Time, Pace)."
                        else -> e.localizedMessage ?: "Gagal mengekstrak data screenshot."
                    }
                    _ocrState.value = OcrScanUiState.Error(message)
                }
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
        workoutDateEpochMs: Long? = null,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val safeDist = distanceKm.coerceIn(0.1, 500.0)
                val safeDur = durationSeconds.coerceIn(1L, 864000L)
                val avgPace = (safeDur / safeDist).roundToLong().coerceIn(60L, 3600L)
                val createdTs = if (workoutDateEpochMs != null && workoutDateEpochMs > 0L) {
                    com.google.firebase.Timestamp(java.util.Date(workoutDateEpochMs))
                } else {
                    com.google.firebase.Timestamp.now()
                }
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
                    sourceType = sourceType,
                    createdAt = createdTs
                )

                // Write to Cloud first when online so Cloud Firestore and Room SQLite share identical document IDs
                val currentList = (workoutsState.value as? UiState.Success)?.data.orEmpty()
                val savedOnline: Boolean = if (!isOfflineMode && cloudRepository != null) {
                    try {
                        val cloudId = cloudRepository.addWorkout(workout)
                        val syncedWorkout = workout.copy(id = cloudId)
                        localRepository.addWorkout(syncedWorkout)
                        recalculatePrsFromRemainingWorkouts(currentList + syncedWorkout)
                        true
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.w(TAG, "Cloud save unavailable, storing offline in Room SQLite for later sync", e)
                        localRepository.addWorkout(workout)
                        recalculatePrsFromRemainingWorkouts()
                        false
                    }
                } else {
                    localRepository.addWorkout(workout)
                    recalculatePrsFromRemainingWorkouts()
                    false
                }

                val storageTarget = if (isOfflineMode) {
                    "Database SQLite Lokal"
                } else if (savedOnline) {
                    "Cloud Firestore & SQLite"
                } else {
                    "SQLite Lokal (akan tersinkron saat online)"
                }
                _statusBanner.value = "Sesi '${workout.title}' disimpan ke $storageTarget."
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
                val currentList = (workoutsState.value as? UiState.Success)?.data.orEmpty()
                localRepository.deleteWorkout(workoutId)
                if (!isOfflineMode && cloudRepository != null && !workoutId.startsWith("local_")) {
                    try {
                        cloudRepository.deleteWorkout(workoutId)
                    } catch (e: Exception) {
                        Log.w(TAG, "Error deleting cloud workout", e)
                    }
                    recalculatePrsFromRemainingWorkouts(currentList.filter { it.id != workoutId })
                } else {
                    recalculatePrsFromRemainingWorkouts()
                }

                _statusBanner.value = "Workout deleted."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _statusBanner.value = "Error deleting workout: ${e.localizedMessage}"
            }
        }
    }

    fun updateWorkout(
        workoutId: String,
        title: String,
        distanceKm: Double,
        durationSeconds: Long,
        avgHeartRate: Long,
        cadenceSpm: Long,
        elevationGainM: Long,
        splitsSummary: String = "",
        aiCoachingInsight: String = "",
        workoutDateEpochMs: Long? = null
    ) {
        viewModelScope.launch {
            try {
                val currentList = (workoutsState.value as? UiState.Success)?.data.orEmpty()
                val existing = currentList.firstOrNull { it.id == workoutId }
                val safeDist = distanceKm.coerceIn(0.1, 500.0)
                val safeDur = durationSeconds.coerceIn(1L, 864000L)
                val avgPace = (safeDur / safeDist).roundToLong().coerceIn(60L, 3600L)
                val createdTs = if (workoutDateEpochMs != null && workoutDateEpochMs > 0L) {
                    com.google.firebase.Timestamp(java.util.Date(workoutDateEpochMs))
                } else {
                    existing?.createdAt ?: com.google.firebase.Timestamp.now()
                }

                val safeTitle = title.ifBlank { "Training Run" }
                val distChanged = existing != null && abs(safeDist - existing.distanceKm) >= 0.05
                val durChanged = existing != null && abs(safeDur - existing.durationSeconds) >= 5L
                val splitsManuallyEdited = existing != null && splitsSummary.trim() != existing.splitsSummary.trim()

                val resolvedSplits = when {
                    splitsManuallyEdited -> splitsSummary.trim()
                    existing == null || distChanged || durChanged -> {
                        WorkoutRefreshEngine.recalibrateSplits(
                            oldSplitsSummary = existing?.splitsSummary.orEmpty(),
                            newDistanceKm = safeDist,
                            newAvgPaceSec = avgPace,
                            generateIfEmpty = true
                        )
                    }
                    existing.splitsSummary.isNotBlank() && WorkoutRefreshEngine.needsSplitRecalibration(existing.splitsSummary, safeDist, avgPace) -> {
                        WorkoutRefreshEngine.recalibrateSplits(
                            oldSplitsSummary = existing.splitsSummary,
                            newDistanceKm = safeDist,
                            newAvgPaceSec = avgPace,
                            generateIfEmpty = true
                        )
                    }
                    else -> existing.splitsSummary
                }

                val telemetryChanged = existing == null ||
                    distChanged ||
                    durChanged ||
                    avgHeartRate != existing.avgHeartRate ||
                    cadenceSpm != existing.cadenceSpm ||
                    elevationGainM != existing.elevationGainM ||
                    safeTitle != existing.title

                val aiManuallyEdited = existing != null &&
                    aiCoachingInsight.trim() != existing.aiCoachingInsight.trim() &&
                    aiCoachingInsight.isNotBlank()

                val instantAiInsight = if (aiManuallyEdited && !telemetryChanged && !splitsManuallyEdited) {
                    aiCoachingInsight.trim()
                } else {
                    WorkoutRefreshEngine.buildLocalRefreshedAiInsight(
                        title = safeTitle,
                        distanceKm = safeDist,
                        durationSeconds = safeDur,
                        avgPaceSec = avgPace,
                        avgHeartRate = avgHeartRate,
                        cadenceSpm = cadenceSpm,
                        elevationGainM = elevationGainM,
                        splitsSummary = resolvedSplits
                    )
                }

                val updated = (existing ?: WorkoutLog(id = workoutId, userId = currentUserId ?: "offline_local_user")).copy(
                    title = safeTitle,
                    distanceKm = safeDist,
                    durationSeconds = safeDur,
                    avgPaceSecondsPerKm = avgPace,
                    avgHeartRate = avgHeartRate,
                    cadenceSpm = cadenceSpm,
                    elevationGainM = elevationGainM,
                    splitsSummary = resolvedSplits,
                    aiCoachingInsight = instantAiInsight,
                    createdAt = createdTs
                )

                persistWorkoutUpdate(updated)
                if (!isOfflineMode && cloudRepository != null && !updated.id.startsWith("local_")) {
                    val updatedList = if (currentList.any { it.id == updated.id }) {
                        currentList.map { if (it.id == updated.id) updated else it }
                    } else {
                        currentList + updated
                    }
                    recalculatePrsFromRemainingWorkouts(updatedList)
                } else {
                    recalculatePrsFromRemainingWorkouts()
                }

                if (!aiManuallyEdited || telemetryChanged || splitsManuallyEdited) {
                    _statusBanner.value = "Riwayat '${updated.title}' diperbarui. Memuat ulang analisis AI..."
                    fetchAndSaveGeminiInsightForWorkout(updated)
                } else {
                    _statusBanner.value = "Riwayat '${updated.title}' berhasil diperbarui."
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update workout", e)
                _statusBanner.value = "Gagal memperbarui riwayat: ${e.localizedMessage}"
            }
        }
    }

    private suspend fun persistWorkoutUpdate(workout: WorkoutLog) {
        localRepository.updateWorkout(workout)
        if (!isOfflineMode && cloudRepository != null && !workout.id.startsWith("local_")) {
            try {
                cloudRepository.updateWorkout(workout)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Error updating cloud workout", e)
            }
        }
    }

    /**
     * Re-evaluates splits consistency and fetches fresh Gemini AI coaching insight for a workout.
     */
    fun refreshWorkoutAiAndSplits(workout: WorkoutLog) {
        viewModelScope.launch {
            try {
                val safePace = if (workout.avgPaceSecondsPerKm in 60L..3600L) {
                    workout.avgPaceSecondsPerKm
                } else {
                    (workout.durationSeconds / workout.distanceKm.coerceAtLeast(0.1)).roundToLong().coerceIn(60L, 3600L)
                }
                val refreshedSplits = if (WorkoutRefreshEngine.needsSplitRecalibration(workout.splitsSummary, workout.distanceKm, safePace)) {
                    WorkoutRefreshEngine.recalibrateSplits(
                        oldSplitsSummary = workout.splitsSummary,
                        newDistanceKm = workout.distanceKm,
                        newAvgPaceSec = safePace,
                        generateIfEmpty = true
                    )
                } else {
                    workout.splitsSummary
                }
                val localRefreshedInsight = WorkoutRefreshEngine.buildLocalRefreshedAiInsight(
                    title = workout.title,
                    distanceKm = workout.distanceKm,
                    durationSeconds = workout.durationSeconds,
                    avgPaceSec = safePace,
                    avgHeartRate = workout.avgHeartRate,
                    cadenceSpm = workout.cadenceSpm,
                    elevationGainM = workout.elevationGainM,
                    splitsSummary = refreshedSplits
                )
                val updated = workout.copy(
                    avgPaceSecondsPerKm = safePace,
                    splitsSummary = refreshedSplits,
                    aiCoachingInsight = localRefreshedInsight
                )
                persistWorkoutUpdate(updated)
                _statusBanner.value = "Mengambil analisis AI terbaru untuk '${updated.title}'..."
                fetchAndSaveGeminiInsightForWorkout(updated)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Failed to refresh AI & splits", e)
                _statusBanner.value = "Gagal me-refresh AI: ${e.localizedMessage}"
            }
        }
    }

    private suspend fun fetchAndSaveGeminiInsightForWorkout(workout: WorkoutLog) {
        _refreshingWorkoutId.value = workout.id
        try {
            val paceFormatted = RacePredictionEngine.formatPace(workout.avgPaceSecondsPerKm)
            val durationFormatted = RacePredictionEngine.formatDuration(workout.durationSeconds)
            val prs = (prsState.value as? UiState.Success)?.data.orEmpty()
            val prSummary = if (prs.isNotEmpty()) {
                prs.joinToString(", ") { "${it.distanceCategory}: ${RacePredictionEngine.formatDuration(it.bestTimeSeconds)}" }
            } else {
                "Belum ada catatan PR khusus"
            }

            val prompt = buildString {
                append("Anda adalah Pelatih Lari Profesional & Ilmuwan Olahraga. ")
                append("Berikan evaluasi terstruktur dan mendalam dalam Bahasa Indonesia (maksimal 3-4 poin ringkas & actionable) untuk sesi lari berikut yang baru saja diperbarui:\n")
                append("- Judul: ${workout.title}\n")
                append("- Jarak Total: ${"%.2f".format(workout.distanceKm)} km\n")
                append("- Durasi Bergerak: $durationFormatted\n")
                append("- Pace Rata-Rata: $paceFormatted\n")
                if (workout.avgHeartRate > 0) append("- Heart Rate Rata-Rata: ${workout.avgHeartRate} bpm\n")
                if (workout.cadenceSpm > 0) append("- Cadence: ${workout.cadenceSpm} spm\n")
                if (workout.elevationGainM > 0) append("- Elevasi: +${workout.elevationGainM} m\n")
                if (workout.splitsSummary.isNotBlank()) append("- Rincian Split per KM: ${workout.splitsSummary}\n")
                append("- Rekor Pribadi (PR) Saat Ini: $prSummary\n\n")
                append("Analisis harus mencakup:\n")
                append("1. Evaluasi Pacing & Konsistensi Split (apakah negative split / positive split).\n")
                append("2. Observasi Biomekanika / Kardiovaskular & Zona Intensitas VDOT.\n")
                append("3. Rekomendasi konkret sesi lari dan pemulihan berikutnya.")
            }

            val response = withTimeout(30_000L) {
                coachGenerativeModel.generateContent(prompt)
            }
            val freshText = response.text?.trim()
            if (!freshText.isNullOrBlank()) {
                val aiUpdated = workout.copy(aiCoachingInsight = freshText)
                persistWorkoutUpdate(aiUpdated)
                _statusBanner.value = "Data, Split & Analisis AI '${workout.title}' berhasil diperbarui."
            } else {
                _statusBanner.value = "Data & Split '${workout.title}' berhasil diperbarui."
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Gemini insight fetch failed, using recalculated local insight", e)
            _statusBanner.value = "Data & Split '${workout.title}' diperbarui (analisis lokal)."
        } finally {
            _refreshingWorkoutId.value = null
        }
    }

    private suspend fun recalculatePrsFromRemainingWorkouts(overrideWorkouts: List<WorkoutLog>? = null) {
        val remainingWorkouts = overrideWorkouts ?: localRepository.getAllWorkoutsList()

        if (remainingWorkouts.isEmpty()) {
            localRepository.deleteAllPersonalRecords()
            if (!isOfflineMode && cloudRepository != null) {
                try {
                    cloudRepository.deleteAllPersonalRecords()
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to clean PRs from cloud", e)
                }
            }
            return
        }

        for (category in DistanceCategory.entries) {
            val docId = "pr_${category.key.lowercase()}"
            // Workouts qualifying for this category (distance >= 85% of category distance)
            // e.g. A 10K workout qualifies for BOTH 10K (>= 8.5) and 5K (>= 4.25)!
            val qualifying = remainingWorkouts.filter { w ->
                w.distanceKm >= (category.kilometers * 0.85)
            }

            if (qualifying.isEmpty()) {
                // If no remaining workout satisfies this distance, remove the PR
                localRepository.deletePersonalRecord(docId)
                localRepository.deletePersonalRecordByCategory(category.key)
                if (!isOfflineMode && cloudRepository != null) {
                    try {
                        cloudRepository.deletePersonalRecordByCategory(category.key)
                        cloudRepository.deletePersonalRecord(docId)
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to delete PR from cloud", e)
                    }
                }
            } else {
                // Find the best time among qualifying workouts
                val best = qualifying.minByOrNull { w ->
                    val safePace = if (w.avgPaceSecondsPerKm in 60..3600) {
                        w.avgPaceSecondsPerKm
                    } else if (w.distanceKm > 0.0) {
                        (w.durationSeconds / w.distanceKm).roundToLong().coerceIn(60L, 3600L)
                    } else {
                        3600L
                    }
                    (safePace * category.kilometers).roundToLong()
                }
                if (best != null) {
                    val safePace = if (best.avgPaceSecondsPerKm in 60..3600) {
                        best.avgPaceSecondsPerKm
                    } else if (best.distanceKm > 0.0) {
                        (best.durationSeconds / best.distanceKm).roundToLong().coerceIn(60L, 3600L)
                    } else {
                        300L
                    }
                    val normalizedBestTime = (safePace * category.kilometers).roundToLong().coerceAtLeast(1L)
                    val pr = PersonalRecord(
                        id = docId,
                        userId = currentUserId ?: "offline_local_user",
                        distanceCategory = category.key,
                        distanceMeters = category.meters,
                        bestTimeSeconds = normalizedBestTime,
                        previousTimeSeconds = 0L,
                        paceSecondsPerKm = safePace,
                        workoutTitle = best.title
                    )
                    localRepository.upsertPersonalRecord(pr)
                    if (!isOfflineMode && cloudRepository != null) {
                        try {
                            cloudRepository.upsertPersonalRecord(pr)
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to sync PR to cloud", e)
                        }
                    }
                }
            }
        }
    }

    fun deletePersonalRecord(categoryKey: String) {
        viewModelScope.launch {
            try {
                val docId = "pr_${categoryKey.lowercase()}"
                localRepository.deletePersonalRecord(docId)
                localRepository.deletePersonalRecordByCategory(categoryKey)
                if (!isOfflineMode && cloudRepository != null) {
                    try {
                        cloudRepository.deletePersonalRecordByCategory(categoryKey)
                        cloudRepository.deletePersonalRecord(docId)
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to delete PR from cloud", e)
                    }
                }
                _statusBanner.value = "Personal Record $categoryKey berhasil dihapus."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _statusBanner.value = "Failed to remove PR: ${e.localizedMessage}"
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

    private suspend fun persistNewGoalWithSync(goal: TrainingGoal): Boolean {
        if (!isOfflineMode && cloudRepository != null) {
            try {
                val cloudId = cloudRepository.addGoal(goal)
                localRepository.addGoal(goal.copy(id = cloudId))
                return true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Cloud goal write unavailable, storing locally for later sync", e)
            }
        }
        localRepository.addGoal(goal)
        return false
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
                    completed = false,
                    createdAt = com.google.firebase.Timestamp.now()
                )
                persistNewGoalWithSync(goal)
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
                if (!isOfflineMode && cloudRepository != null && !updated.id.startsWith("local_")) {
                    try {
                        cloudRepository.updateGoal(updated)
                    } catch (e: Exception) {
                        Log.w(TAG, "Cloud goal toggle failed", e)
                    }
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
                if (!isOfflineMode && cloudRepository != null && !goalId.startsWith("local_")) {
                    try {
                        cloudRepository.deleteGoal(goalId)
                    } catch (e: Exception) {
                        Log.w(TAG, "Cloud goal delete failed", e)
                    }
                }
                _statusBanner.value = "Goal removed."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _statusBanner.value = "Failed to delete goal: ${e.localizedMessage}"
            }
        }
    }

    fun saveOrUpdateGoalSchedule(
        goalId: String?,
        cleanTitle: String,
        category: DistanceCategory,
        targetTimeSeconds: Long,
        weeklyMileageKm: Long,
        startDateKey: String,
        targetDateKey: String,
        trainingDaysIso: List<Int>
    ) {
        viewModelScope.launch {
            try {
                val encodedTitle = GoalTrainingPlanEngine.encodeGoalTitleWithSchedule(
                    cleanTitle = cleanTitle,
                    targetDateKey = targetDateKey,
                    trainingDaysIso = trainingDaysIso,
                    startDateKey = startDateKey
                )
                if (!goalId.isNullOrBlank()) {
                    val currentGoals = (goalsState.value as? UiState.Success)?.data.orEmpty()
                    val existing = currentGoals.firstOrNull { it.id == goalId }
                    if (existing != null) {
                        val updated = existing.copy(
                            title = encodedTitle,
                            targetDistanceCategory = category.key,
                            targetTimeSeconds = targetTimeSeconds,
                            weeklyMileageTargetKm = weeklyMileageKm
                        )
                        localRepository.updateGoal(updated)
                        if (!isOfflineMode && cloudRepository != null && !updated.id.startsWith("local_")) {
                            try {
                                cloudRepository.updateGoal(updated)
                            } catch (e: Exception) {
                                Log.w(TAG, "Cloud goal schedule update failed", e)
                            }
                        }
                        _statusBanner.value = "Jadwal target '$cleanTitle' berhasil diperbarui."
                        return@launch
                    }
                }

                // If new goal
                val newGoal = TrainingGoal(
                    userId = currentUserId ?: "offline_local_user",
                    title = encodedTitle,
                    targetDistanceCategory = category.key,
                    targetTimeSeconds = targetTimeSeconds,
                    weeklyMileageTargetKm = weeklyMileageKm,
                    reminderEnabled = true,
                    reminderHour = 7L,
                    reminderMinute = 0L,
                    completed = false,
                    createdAt = com.google.firebase.Timestamp.now()
                )
                persistNewGoalWithSync(newGoal)
                _statusBanner.value = "Target '$cleanTitle' & jadwal adaptif AI berhasil disimpan."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save or update goal schedule", e)
                _statusBanner.value = "Gagal menyimpan jadwal target: ${e.localizedMessage}"
            }
        }
    }

    fun askGeminiForTrainingPlanAdvice(goal: TrainingGoal) {
        viewModelScope.launch {
            _aiPlanLoading.value = true
            try {
                val workouts = (workoutsState.value as? UiState.Success)?.data.orEmpty()
                val meta = GoalTrainingPlanEngine.parseGoalMetadata(goal)
                val targetDuration = RacePredictionEngine.formatDuration(goal.targetTimeSeconds)
                val category = DistanceCategory.fromKey(goal.targetDistanceCategory)
                val paceSec = (goal.targetTimeSeconds / category.kilometers).toLong()
                val targetPace = RacePredictionEngine.formatPace(paceSec)

                val prompt = buildString {
                    append("Anda adalah Pelatih Lari Maraton Profesional bersertifikasi IAFF (AI Running Coach). ")
                    append("Anda memiliki target: '${meta.cleanTitle}' (${category.displayName}) dengan target waktu $targetDuration (Pace $targetPace). ")
                    append("Periode latihan: ${meta.startDateDisplay} s/d hari perlombaan ${meta.targetDateDisplay} (Sisa ${meta.daysRemaining} hari). ")
                    append("Frekuensi latihan mingguan: ${meta.trainingDaysIso.size}x per minggu pada hari: ")
                    append(meta.trainingDaysIso.joinToString(", ") { GoalTrainingPlanEngine.dayNameId(it) })
                    append(".\n")
                    if (workouts.isNotEmpty()) {
                        append("Riwayat latihan aktual atlet (${workouts.size} sesi tercatat di Strava): ")
                        workouts.take(4).forEach { w ->
                            append("- ${w.title}: ${"%.2f".format(w.distanceKm)} km, Pace ${RacePredictionEngine.formatPace(w.avgPaceSecondsPerKm)}, HR ${w.avgHeartRate} bpm. ")
                        }
                    } else {
                        append("Belum ada riwayat lari yang di-upload sebelumnya. Mulai dari pembangunan aerobik dasar.")
                    }
                    append("\nBerikan analisis periodisasi adaptif yang ringkas dan padat (maksimal 3 paragraf singkat dalam Bahasa Indonesia): ")
                    append("1. Strategi pembagian sesi (kapan harus Recovery Run agar terhindar dari cedera, dan kapan aman Interval/Tempo). ")
                    append("2. Penyesuaian tapering pada 7 hari sebelum race day. ")
                    append("3. Rekomendasi hidrasi dan pemulihan glikogen.")
                }

                val response = withTimeout(30_000L) {
                    coachGenerativeModel.generateContent(prompt)
                }
                val text = response.text
                if (!text.isNullOrBlank()) {
                    _aiPlanCoachingInsight.value = text
                }
            } catch (e: Exception) {
                Log.w(TAG, "Gemini plan coaching advice failed", e)
                _aiPlanCoachingInsight.value = "AI Adaptive Engine: Jadwal latihan harian Anda telah dihitung secara adaptif berdasarkan riwayat lari. Sesi intensif (Interval/Tempo) akan otomatis beradaptasi menjadi Recovery jika sistem mendeteksi beban kelelahan tinggi."
            } finally {
                _aiPlanLoading.value = false
            }
        }
    }

    fun createCloudBackupCheckpoint(label: String) {
        viewModelScope.launch {
            try {
                val workouts = (workoutsState.value as? UiState.Success)?.data.orEmpty()
                val prs = (prsState.value as? UiState.Success)?.data.orEmpty()
                val goals = (goalsState.value as? UiState.Success)?.data.orEmpty()
                if (!isOfflineMode && cloudRepository != null) {
                    try {
                        val cloudId = cloudRepository.createCloudBackupSnapshot(
                            label = label.ifBlank { "Cloud Backup Checkpoint" },
                            workouts = workouts,
                            prs = prs,
                            goals = goals
                        )
                        localRepository.createLocalBackupSnapshot(
                            label = label.ifBlank { "Training Backup Checkpoint" },
                            workouts = workouts,
                            prs = prs,
                            goals = goals,
                            explicitId = cloudId
                        )
                        _statusBanner.value = "Cloud & Local SQLite backup checkpoint created (${workouts.size} workouts)."
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.w(TAG, "Cloud backup failed, saving offline snapshot", e)
                        localRepository.createLocalBackupSnapshot(
                            label = label.ifBlank { "Training Backup Checkpoint" },
                            workouts = workouts,
                            prs = prs,
                            goals = goals
                        )
                        _statusBanner.value = "Offline SQLite backup checkpoint saved (${workouts.size} workouts). Sign in anytime to sync to Cloud."
                    }
                } else {
                    localRepository.createLocalBackupSnapshot(
                        label = label.ifBlank { "Training Backup Checkpoint" },
                        workouts = workouts,
                        prs = prs,
                        goals = goals
                    )
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
