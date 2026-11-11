package com.example.domain

import com.example.data.model.DistanceCategory
import com.example.data.model.WorkoutLog
import java.util.Calendar
import java.util.Date
import kotlin.math.roundToLong

/**
 * On-device intelligent training engine that dynamically decides session types
 * based on the athlete's actual logged workout history, acute fatigue,
 * recovery status, and periodization leading up to race day.
 *
 * "Selasa tidak pasti interval, bisa jadi tempo atau recovery
 * tergantung history latihan yang di-upload sebelumnya."
 */
object AdaptiveTrainingAi {

    data class PlanSlot(
        val dateKey: String,
        val epochDay: Long,
        val isoDay: Int,
        val isRaceDay: Boolean
    )

    data class Decision(
        val type: SessionWorkoutType,
        val reason: String
    )

    /**
     * Estimates the runner's current race pace based on recent workouts and PRs.
     * If no history exists, falls back to goal race pace + 15%.
     */
    fun estimateCurrentRacePace(
        workouts: List<WorkoutLog>,
        category: DistanceCategory,
        goalRacePaceSec: Long,
        nowEpochMs: Long = System.currentTimeMillis()
    ): Long {
        if (workouts.isEmpty()) {
            return (goalRacePaceSec * 1.15).roundToLong().coerceIn(150L, 1200L)
        }

        // Prefer workouts within the last 60 days
        val cutoffMs = nowEpochMs - 60L * 86_400_000L
        val recent = workouts.filter { (it.createdAt?.toDate()?.time ?: 0L) >= cutoffMs }
            .ifEmpty { workouts }

        // Find workout with comparable distance (within 50% - 150% of category distance)
        val comparable = recent.filter { it.distanceKm >= category.kilometers * 0.4 && it.avgPaceSecondsPerKm > 60 }
        if (comparable.isNotEmpty()) {
            val bestPace = comparable.minOf { it.avgPaceSecondsPerKm }
            return bestPace.coerceIn(150L, 1200L)
        }

        // Otherwise use overall average pace
        val avgPace = recent.map { it.avgPaceSecondsPerKm }.filter { it in 120..1200 }
        if (avgPace.isNotEmpty()) {
            return (avgPace.average()).roundToLong().coerceIn(150L, 1200L)
        }

        return (goalRacePaceSec * 1.15).roundToLong().coerceIn(150L, 1200L)
    }

    /**
     * Dynamically determines session types and AI reasoning for every slot in the schedule.
     */
    fun planSessionTypes(
        slots: List<PlanSlot>,
        workouts: List<WorkoutLog>,
        goalRacePaceSec: Long,
        currentRacePaceSec: Long,
        category: DistanceCategory,
        todayEpochDay: Long,
        trainingDaysIso: List<Int>,
        avgSessionKm: Double
    ): List<Decision> {
        val totalSlots = slots.size
        val raceSlot = slots.lastOrNull { it.isRaceDay } ?: slots.lastOrNull()
        val raceEpochDay = raceSlot?.epochDay ?: (todayEpochDay + 21)

        // Map past workouts by epoch day
        val workoutDayMap = mutableMapOf<Long, WorkoutLog>()
        for (w in workouts) {
            val d = w.createdAt?.toDate() ?: continue
            val eDay = epochDayFromDate(d)
            val existing = workoutDayMap[eDay]
            if (existing == null || w.distanceKm > existing.distanceKm) {
                workoutDayMap[eDay] = w
            }
        }

        val decisions = mutableListOf<Decision>()
        var lastQualitySlotEpochDay = -999L // Last day assigned an Interval or Tempo
        var lastLongRunEpochDay = -999L

        for (i in slots.indices) {
            val slot = slots[i]

            // 1. Race Day is always RACE_DAY
            if (slot.isRaceDay) {
                decisions.add(
                    Decision(
                        type = SessionWorkoutType.RACE_DAY,
                        reason = "Hari Perlombaan! Eksekusi strategi race pace target dengan disiplin energi."
                    )
                )
                continue
            }

            val daysUntilRace = (raceEpochDay - slot.epochDay).toInt()

            // 2. Taper week (terakhir 7 hari sebelum race day)
            if (daysUntilRace in 1..6) {
                if (daysUntilRace <= 3) {
                    decisions.add(
                        Decision(
                            type = SessionWorkoutType.RECOVERY,
                            reason = "Fase Tapering Akhir (H-$daysUntilRace): Lari pemulihan sangat santai untuk menyimpan simpanan glikogen dan menyegarkan otot sebelum race."
                        )
                    )
                } else {
                    decisions.add(
                        Decision(
                            type = SessionWorkoutType.TEMPO,
                            reason = "Fase Tapering (H-$daysUntilRace): Sesi tempo singkat penyesuaian ritme race pace tanpa menimbulkan kelelahan otot mendalam."
                        )
                    )
                }
                continue
            }

            // 3. Check recent past workouts around this slot
            // If there's an actual workout logged 1 or 2 days prior to this slot
            val yesterdayWorkout = workoutDayMap[slot.epochDay - 1]
            val twoDaysAgoWorkout = workoutDayMap[slot.epochDay - 2]
            val recentHardWorkout = when {
                yesterdayWorkout != null && (yesterdayWorkout.distanceKm >= avgSessionKm * 1.1 || yesterdayWorkout.avgHeartRate >= 165 || yesterdayWorkout.elevationGainM >= 60) -> yesterdayWorkout
                twoDaysAgoWorkout != null && (twoDaysAgoWorkout.distanceKm >= avgSessionKm * 1.3 || twoDaysAgoWorkout.avgHeartRate >= 170) -> twoDaysAgoWorkout
                else -> null
            }

            // Days since the last high-intensity slot in our plan
            val daysSinceQuality = (slot.epochDay - lastQualitySlotEpochDay).toInt()
            val daysSinceLong = (slot.epochDay - lastLongRunEpochDay).toInt()

            // 4. Adaptive rule: If recent workout was heavy or intensive, force RECOVERY!
            if (recentHardWorkout != null) {
                decisions.add(
                    Decision(
                        type = SessionWorkoutType.RECOVERY,
                        reason = "Adaptasi AI: Riwayat lari ${recentHardWorkout.distanceKm} km (${recentHardWorkout.title}) baru saja diselesaikan. Sesi dialihkan ke Recovery Run Zona 1-2 untuk mencegah cedera & overtraining."
                    )
                )
                continue
            }

            // 5. If yesterday was a rest day and athlete has had >= 2 days rest from hard efforts
            // Determine if this day fits Long Run, Interval, or Tempo
            val isWeekend = slot.isoDay in listOf(6, 7) // Sabtu atau Minggu
            val isFirstTrainingDayOfWeek = trainingDaysIso.indexOf(slot.isoDay) == 0
            val isLastTrainingDayOfWeek = trainingDaysIso.indexOf(slot.isoDay) == trainingDaysIso.lastIndex

            val decision = when {
                // If it's the weekend and long run hasn't occurred in 5+ days -> LONG RUN
                (isWeekend || isLastTrainingDayOfWeek) && daysSinceLong >= 5 -> {
                    lastLongRunEpochDay = slot.epochDay
                    Decision(
                        type = SessionWorkoutType.EASY_LONG,
                        reason = "Sesi Aerobik Jarak Jauh (Long Run): Membangun efisiensi mitokondria dan kapasitas endurance menyongsong target ${category.shortLabel}."
                    )
                }

                // If well-rested from quality sessions (at least 3 days) -> INTERVAL
                daysSinceQuality >= 3 && !isWeekend -> {
                    lastQualitySlotEpochDay = slot.epochDay
                    Decision(
                        type = SessionWorkoutType.INTERVAL,
                        reason = "Kondisi fisik optimal setelah masa pemulihan. AI menjadwalkan Interval Speed VO2Max guna menaikkan ambang batas kecepatan puncak."
                    )
                }

                // Otherwise, if 2 days since quality and not weekend -> TEMPO
                daysSinceQuality >= 2 && !isWeekend -> {
                    lastQualitySlotEpochDay = slot.epochDay
                    Decision(
                        type = SessionWorkoutType.TEMPO,
                        reason = "Sesi Laktat Threshold (Tempo): Melatih tubuh membuang asam laktat secara efisien pada kecepatan mendekati target race pace."
                    )
                }

                // Default fallback -> RECOVERY
                else -> {
                    Decision(
                        type = SessionWorkoutType.RECOVERY,
                        reason = "Sesi Regenerasi Aktif: Menjaga turnover langkah kaki tanpa menambah beban kardiovaskular berat."
                    )
                }
            }

            decisions.add(decision)
        }

        return decisions
    }

    private fun epochDayFromDate(date: Date): Long {
        val cal = Calendar.getInstance().apply {
            time = date
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis / 86_400_000L
    }
}
