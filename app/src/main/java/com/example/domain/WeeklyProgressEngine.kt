package com.example.domain

import com.example.data.model.TrainingGoal
import com.example.data.model.WorkoutLog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToLong

/**
 * Statistics for a single day of the week.
 */
data class DayStat(
    val dayIndex: Int, // 0 for Monday .. 6 for Sunday
    val dayName: String, // "Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min"
    val dateKey: String, // "yyyy-MM-dd"
    val dateDisplay: String, // "12 Nov"
    val distanceKm: Double,
    val durationSec: Long,
    val avgPaceSec: Long,
    val hasRun: Boolean,
    val isToday: Boolean
)

/**
 * Summary analytics for weekly running progress.
 */
data class WeeklyAnalytics(
    val weekRangeDisplay: String,
    val totalDistanceKm: Double,
    val targetDistanceKm: Long,
    val runsCount: Int,
    val totalDurationSec: Long,
    val avgPaceSec: Long,
    val prevWeekDistanceKm: Double,
    val distanceDiffKm: Double,
    val prevWeekAvgPaceSec: Long,
    val paceDiffSec: Long, // negative means faster (improved)
    val dailyStats: List<DayStat>,
    val coachingTip: String
)

object WeeklyProgressEngine {

    /**
     * Computes weekly progress analytics from workout logs and optional training goals.
     */
    fun computeWeeklyProgress(
        workouts: List<WorkoutLog>,
        activeGoal: TrainingGoal? = null,
        nowEpochMs: Long = System.currentTimeMillis()
    ): WeeklyAnalytics {
        val nowCal = Calendar.getInstance().apply { timeInMillis = nowEpochMs }
        val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(nowCal.time)

        // Find current week's Monday (00:00:00.000) and Sunday (23:59:59.999) locale-independently
        val currentWeekCal = Calendar.getInstance().apply {
            timeInMillis = nowEpochMs
            val daysSinceMonday = (get(Calendar.DAY_OF_WEEK) + 5) % 7
            add(Calendar.DAY_OF_YEAR, -daysSinceMonday)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val currentWeekStartMs = currentWeekCal.timeInMillis

        val currentWeekEndCal = (currentWeekCal.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, 6)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        val currentWeekEndMs = currentWeekEndCal.timeInMillis

        // Previous week Monday (00:00:00) to Sunday (23:59:59)
        val prevWeekCal = (currentWeekCal.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, -7)
        }
        val prevWeekStartMs = prevWeekCal.timeInMillis
        val prevWeekEndMs = currentWeekStartMs - 1L

        val sdfDisplay = SimpleDateFormat("d MMM", Locale("id", "ID"))
        val weekRangeDisplay = "${sdfDisplay.format(Date(currentWeekStartMs))} - ${sdfDisplay.format(Date(currentWeekEndMs))}"

        val sdfKey = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        // Filter workouts for current week
        val currentWeekWorkouts = workouts.filter { w ->
            val epoch = w.createdAt?.toDate()?.time ?: 0L
            epoch in currentWeekStartMs..currentWeekEndMs
        }

        // Filter workouts for previous week
        val prevWeekWorkouts = workouts.filter { w ->
            val epoch = w.createdAt?.toDate()?.time ?: 0L
            epoch in prevWeekStartMs..prevWeekEndMs
        }

        // Current week aggregates
        val totalDistanceKm = currentWeekWorkouts.sumOf { it.distanceKm }
        val totalDurationSec = currentWeekWorkouts.sumOf { it.durationSeconds }
        val runsCount = currentWeekWorkouts.size
        val avgPaceSec = if (totalDistanceKm > 0.0) {
            (totalDurationSec.toDouble() / totalDistanceKm).roundToLong()
        } else {
            0L
        }

        // Previous week aggregates
        val prevWeekDistanceKm = prevWeekWorkouts.sumOf { it.distanceKm }
        val prevWeekDurationSec = prevWeekWorkouts.sumOf { it.durationSeconds }
        val prevWeekAvgPaceSec = if (prevWeekDistanceKm > 0.0) {
            (prevWeekDurationSec.toDouble() / prevWeekDistanceKm).roundToLong()
        } else {
            0L
        }

        val distanceDiffKm = totalDistanceKm - prevWeekDistanceKm
        val paceDiffSec = if (avgPaceSec > 0L && prevWeekAvgPaceSec > 0L) {
            avgPaceSec - prevWeekAvgPaceSec // negative = faster
        } else {
            0L
        }

        val targetDistanceKm = activeGoal?.weeklyMileageTargetKm?.coerceAtLeast(10L) ?: 25L

        // Generate 7-day breakdown (Monday = 0 .. Sunday = 6)
        val dayNames = listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min")
        val dailyStats = (0..6).map { dayIndex ->
            val dayCal = (currentWeekCal.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, dayIndex)
            }
            val dateKey = sdfKey.format(dayCal.time)
            val dateDisplay = sdfDisplay.format(dayCal.time)
            val isToday = dateKey == todayKey

            val dayWorkouts = currentWeekWorkouts.filter { w ->
                val wDate = w.createdAt?.toDate()
                wDate != null && sdfKey.format(wDate) == dateKey
            }

            val dayDist = dayWorkouts.sumOf { it.distanceKm }
            val dayDur = dayWorkouts.sumOf { it.durationSeconds }
            val dayPace = if (dayDist > 0.0) (dayDur.toDouble() / dayDist).roundToLong() else 0L

            DayStat(
                dayIndex = dayIndex,
                dayName = dayNames[dayIndex],
                dateKey = dateKey,
                dateDisplay = dateDisplay,
                distanceKm = dayDist,
                durationSec = dayDur,
                avgPaceSec = dayPace,
                hasRun = dayWorkouts.isNotEmpty(),
                isToday = isToday
            )
        }

        // Formulate actionable coaching insight
        val coachingTip = when {
            totalDistanceKm >= targetDistanceKm -> {
                "Luar biasa! Target mingguan ${targetDistanceKm} km telah tercapai. Jaga fase recovery agar otot siap menghadapi minggu berikutnya."
            }
            totalDistanceKm > 0.0 && runsCount >= 3 -> {
                val rem = targetDistanceKm - totalDistanceKm
                "Konsistensi sangat baik dengan $runsCount sesi lari. Butuh ${String.format(Locale.US, "%.1f", rem.coerceAtLeast(0.0))} km lagi untuk menuntaskan target minggu ini."
            }
            totalDistanceKm > 0.0 -> {
                "Bagus! Sesi awal minggu ini sudah dimulai. Pertahankan easy run santai untuk membangun akumulasi volume tanpa overtraining."
            }
            else -> {
                "Minggu baru telah dimulai. Jadwalkan 3-4 sesi lari mingguan Anda untuk menjaga performa aerobik dan membuka tier sepatu baru!"
            }
        }

        return WeeklyAnalytics(
            weekRangeDisplay = weekRangeDisplay,
            totalDistanceKm = totalDistanceKm,
            targetDistanceKm = targetDistanceKm,
            runsCount = runsCount,
            totalDurationSec = totalDurationSec,
            avgPaceSec = avgPaceSec,
            prevWeekDistanceKm = prevWeekDistanceKm,
            distanceDiffKm = distanceDiffKm,
            prevWeekAvgPaceSec = prevWeekAvgPaceSec,
            paceDiffSec = paceDiffSec,
            dailyStats = dailyStats,
            coachingTip = coachingTip
        )
    }
}
