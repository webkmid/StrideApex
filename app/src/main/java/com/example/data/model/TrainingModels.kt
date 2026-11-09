package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class WorkoutLog(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val distanceKm: Double = 0.0,
    val durationSeconds: Long = 0L,
    val avgPaceSecondsPerKm: Long = 0L,
    val avgHeartRate: Long = 0L,
    val cadenceSpm: Long = 0L,
    val elevationGainM: Long = 0L,
    val splitsSummary: String = "",
    val aiCoachingInsight: String = "",
    val sourceType: String = "STRAVA_OCR",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class PersonalRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val distanceCategory: String = "5K",
    val distanceMeters: Long = 5000L,
    val bestTimeSeconds: Long = 0L,
    val previousTimeSeconds: Long = 0L,
    val paceSecondsPerKm: Long = 0L,
    val workoutTitle: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class TrainingGoal(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val targetDistanceCategory: String = "10K",
    val targetTimeSeconds: Long = 2400L,
    val weeklyMileageTargetKm: Long = 50L,
    val reminderEnabled: Boolean = true,
    val reminderHour: Long = 7L,
    val reminderMinute: Long = 0L,
    val completed: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CloudBackupSnapshot(
    @DocumentId val id: String = "",
    val userId: String = "",
    val backupLabel: String = "",
    val workoutCount: Long = 0L,
    val prCount: Long = 0L,
    val goalCount: Long = 0L,
    val summaryNotes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

enum class DistanceCategory(
    val key: String,
    val displayName: String,
    val shortLabel: String,
    val meters: Long,
    val kilometers: Double
) {
    FIVE_K("5K", "5K Road Race", "5K", 5000L, 5.0),
    TEN_K("10K", "10K Championship", "10K", 10000L, 10.0),
    HALF_MARATHON("HALF_MARATHON", "Half Marathon", "21.1K", 21097L, 21.0975),
    MARATHON("MARATHON", "Full Marathon", "42.2K", 42195L, 42.195);

    companion object {
        fun fromKey(key: String): DistanceCategory =
            entries.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: TEN_K
    }
}

data class ExtractedStravaMetrics(
    val title: String = "Morning Threshold Run",
    val distanceKm: Double = 10.0,
    val durationSeconds: Long = 2460L,
    val avgPaceSecondsPerKm: Long = 246L,
    val avgHeartRate: Long = 166L,
    val cadenceSpm: Long = 182L,
    val elevationGainM: Long = 85L,
    val splitsSummary: String = "KM 1-3: 4:08/km | KM 4-7: 4:05/km | KM 8-10: 4:03/km",
    val aiCoachingInsight: String = "Strong negative split progression with controlled aerobic heart rate drift."
)

data class RacePredictionResult(
    val category: DistanceCategory,
    val predictedTimeSeconds: Long,
    val targetPaceSecondsPerKm: Long,
    val vdotScore: Double,
    val confidenceNote: String
)

data class TrainingPaceZone(
    val zoneName: String,
    val code: String,
    val paceRangePerKm: String,
    val physiologicalBenefit: String,
    val heartRatePctRange: String
)

data class BiomechanicsModule(
    val id: String,
    val title: String,
    val subtitle: String,
    val focusMetricLabel: String,
    val optimalTargetRange: String,
    val commonFault: String,
    val biomechanicalCue: String,
    val drills: List<String>,
    val targetKneeFlexionDeg: Int,
    val targetTibialAngleDeg: Int,
    val targetTrunkLeanDeg: Int,
    val targetCadenceSpm: Int
)
