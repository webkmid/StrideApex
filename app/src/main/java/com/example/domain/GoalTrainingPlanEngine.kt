package com.example.domain

import com.example.data.model.DistanceCategory
import com.example.data.model.TrainingGoal
import com.example.data.model.WorkoutLog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.roundToLong

enum class SessionWorkoutType(
    val code: String,
    val displayName: String,
    val shortBadge: String,
    val zoneLabel: String
) {
    INTERVAL("INTERVAL", "Interval / Speed Run", "INTERVAL", "Zona 4 (VO2Max)"),
    TEMPO("TEMPO", "Tempo / Threshold Run", "TEMPO", "Zona 3 (Threshold)"),
    RECOVERY("RECOVERY", "Recovery / Easy Run", "RECOVERY", "Zona 1-2 (Pemulihan)"),
    EASY_LONG("EASY_LONG", "Long Easy / Endurance Run", "LONG RUN", "Zona 2 (Endurance)"),
    RACE_DAY("RACE_DAY", "Race Day Target!", "RACE DAY", "Target Pace Lomba")
}

enum class ScheduledSessionStatus {
    COMPLETED,
    TODAY_PENDING,
    SKIPPED,
    UPCOMING
}

/** Who decided the session type for a given date. */
enum class PlanDecisionSource {
    GEMINI_AI,
    ADAPTIVE_AI
}

/** A Gemini-provided decision for one scheduled date. */
data class AiSessionOverride(
    val type: SessionWorkoutType,
    val reason: String
)

/** A cached AI training plan for a goal (persisted locally per goal). */
data class AiTrainingPlan(
    val goalId: String,
    val signature: String,
    val summary: String,
    val overrides: Map<String, AiSessionOverride>,
    val generatedAtMs: Long,
    val source: PlanDecisionSource = PlanDecisionSource.GEMINI_AI
)

data class ScheduledTrainingSession(
    val dateKey: String,          // yyyy-MM-dd
    val displayDate: String,      // e.g. "Selasa, 10 Nov 2026"
    val dayOfWeekIso: Int,        // 1=Senin .. 7=Minggu
    val dayName: String,          // Senin..Minggu
    val workoutType: SessionWorkoutType,
    val sessionTitle: String,
    val sessionDescription: String,
    val recommendedDistanceKm: Double,
    val targetPaceSecondsPerKm: Long,
    val status: ScheduledSessionStatus,
    val matchedWorkout: WorkoutLog? = null,
    val aiReason: String = "",
    val decisionSource: PlanDecisionSource = PlanDecisionSource.ADAPTIVE_AI
)

data class GoalScheduleMetadata(
    val cleanTitle: String,
    val startDateKey: String,      // yyyy-MM-dd (first training day, default = today)
    val startDateDisplay: String,
    val targetDateKey: String,     // yyyy-MM-dd (race day)
    val targetDateDisplay: String, // e.g. "29 Nov 2026"
    val daysRemaining: Int,
    val trainingDaysIso: List<Int> // e.g. [2, 4, 7] for Selasa, Kamis, Minggu
)

object GoalTrainingPlanEngine {

    /** Hard cap so a mistyped year cannot generate thousands of sessions. */
    const val MAX_PLAN_DAYS = 370

    private const val DAY_MS = 86_400_000L

    private val dateKeyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
    private val utcKeyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        isLenient = false
        timeZone = TimeZone.getTimeZone("UTC")
    }
    private val displayShortFormat = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))

    // Supports both the legacy "[target|days]" and the new "[start~target|days]" suffix.
    private val scheduleSuffixRegex =
        Regex("\\s*\\[(?:(\\d{4}-\\d{2}-\\d{2})~)?(\\d{4}-\\d{2}-\\d{2})\\|([0-9,]+)]$")
    private val dateKeyRegex = Regex("\\d{4}-\\d{2}-\\d{2}")

    // ISO Day of Week: 1 = Senin, 2 = Selasa, 3 = Rabu, 4 = Kamis, 5 = Jumat, 6 = Sabtu, 7 = Minggu
    fun dayNameId(isoDay: Int): String = when (isoDay) {
        1 -> "Senin"
        2 -> "Selasa"
        3 -> "Rabu"
        4 -> "Kamis"
        5 -> "Jumat"
        6 -> "Sabtu"
        else -> "Minggu"
    }

    fun dayShortNameId(isoDay: Int): String = when (isoDay) {
        1 -> "Sen"
        2 -> "Sel"
        3 -> "Rab"
        4 -> "Kam"
        5 -> "Jum"
        6 -> "Sab"
        else -> "Min"
    }

    /**
     * Default static blueprint assignment based on training days count,
     * used as fallback or template preview before dynamic AI evaluation.
     */
    fun assignWorkoutTypeForDayIndex(dayIndexInWeek: Int, totalDaysInWeek: Int): SessionWorkoutType {
        return when (totalDaysInWeek) {
            1 -> SessionWorkoutType.EASY_LONG
            2 -> if (dayIndexInWeek == 0) SessionWorkoutType.TEMPO else SessionWorkoutType.EASY_LONG
            3 -> when (dayIndexInWeek) {
                0 -> SessionWorkoutType.INTERVAL
                1 -> SessionWorkoutType.TEMPO
                else -> SessionWorkoutType.EASY_LONG
            }
            4 -> when (dayIndexInWeek) {
                0 -> SessionWorkoutType.INTERVAL
                1 -> SessionWorkoutType.TEMPO
                2 -> SessionWorkoutType.RECOVERY
                else -> SessionWorkoutType.EASY_LONG
            }
            else -> when (dayIndexInWeek) {
                0 -> SessionWorkoutType.INTERVAL
                1 -> SessionWorkoutType.RECOVERY
                2 -> SessionWorkoutType.TEMPO
                3 -> SessionWorkoutType.RECOVERY
                else -> SessionWorkoutType.EASY_LONG
            }
        }
    }

    /**
     * Encodes cleanTitle, optional startDate, targetDate (yyyy-MM-dd), and trainingDays (e.g. 2,4,7)
     * into the 120-char Firestore-safe title field:
     * - "Astra Run 10K [2026-11-10~2026-11-29|2,4,7]" (with start date)
     * - "Astra Run 10K [2026-11-29|2,4,7]" (legacy, without start date)
     */
    fun encodeGoalTitleWithSchedule(
        cleanTitle: String,
        targetDateKey: String,
        trainingDaysIso: List<Int>,
        startDateKey: String? = null
    ): String {
        val sortedDays = trainingDaysIso.filter { it in 1..7 }.distinct().sorted().ifEmpty { listOf(2, 4, 7) }
        val safeTarget = if (isValidDateKey(targetDateKey)) targetDateKey else defaultTargetDateKey(21)
        val safeStart = startDateKey?.takeIf { isValidDateKey(it) && it <= safeTarget }
        val datePart = if (safeStart != null) "$safeStart~$safeTarget" else safeTarget
        val suffix = " [$datePart|${sortedDays.joinToString(",")}]"
        val maxTitleLen = (120 - suffix.length).coerceAtLeast(10)
        val trimmedTitle = stripScheduleSuffix(cleanTitle)
            .take(maxTitleLen)
            .trim()
            .ifBlank { "Target Lari" }
        return "$trimmedTitle$suffix"
    }

    fun stripScheduleSuffix(title: String): String = title.replace(scheduleSuffixRegex, "").trim()

    fun isValidDateKey(key: String): Boolean =
        key.matches(dateKeyRegex) && parseDateKey(key) != null

    fun parseGoalMetadata(goal: TrainingGoal, nowEpochMs: Long = System.currentTimeMillis()): GoalScheduleMetadata {
        val raw = goal.title.trim()
        val match = scheduleSuffixRegex.find(raw)
        val todayKey = formatCalendarDateKey(startOfDayCalendar(nowEpochMs))
        val createdKey = goal.createdAt?.toDate()?.let { formatDateKey(it) } ?: todayKey

        val cleanTitle: String
        val startKey: String
        val targetKey: String
        val daysIso: List<Int>

        if (match != null) {
            cleanTitle = raw.substring(0, match.range.first).trim().ifBlank { "Target Lari" }
            targetKey = match.groupValues[2]
            startKey = match.groupValues[1].ifBlank { minOf(createdKey, targetKey) }
            daysIso = match.groupValues[3]
                .split(",")
                .mapNotNull { it.trim().toIntOrNull() }
                .filter { it in 1..7 }
                .distinct()
                .sorted()
                .ifEmpty { listOf(2, 4, 7) }
        } else {
            cleanTitle = raw.ifBlank { "Target Lari" }
            startKey = createdKey
            targetKey = addDaysToKey(createdKey, 21)
            daysIso = listOf(2, 4, 7) // Default recommendation: 3x/week
        }

        val diffDays = daysBetween(todayKey, targetKey).toInt()

        return GoalScheduleMetadata(
            cleanTitle = cleanTitle,
            startDateKey = startKey,
            startDateDisplay = formatShortDate(startKey),
            targetDateKey = targetKey,
            targetDateDisplay = formatShortDate(targetKey),
            daysRemaining = diffDays,
            trainingDaysIso = daysIso
        )
    }

    /**
     * Generates every scheduled session from the goal's start date up to race day.
     *
     * The session TYPE for each date is NOT fixed per weekday. It is decided by:
     * 1. [aiOverrides] from Gemini (if available for that date), otherwise
     * 2. [AdaptiveTrainingAi] — an on-device adaptive model that reads the uploaded
     *    workout history (fatigue, training load, last hard session, long run, taper).
     *
     * Status per session:
     * - COMPLETED if a workout exists on that dateKey
     * - TODAY_PENDING if dateKey == today and no workout logged yet
     * - SKIPPED if dateKey < today and no workout logged on that date
     * - UPCOMING if dateKey > today
     */
    fun generateScheduleForGoal(
        goal: TrainingGoal,
        workouts: List<WorkoutLog>,
        nowEpochMs: Long = System.currentTimeMillis(),
        aiOverrides: Map<String, AiSessionOverride> = emptyMap()
    ): List<ScheduledTrainingSession> {
        val meta = parseGoalMetadata(goal, nowEpochMs)
        val category = DistanceCategory.fromKey(goal.targetDistanceCategory)
        val goalRacePaceSec = (goal.targetTimeSeconds / category.kilometers).roundToLong().coerceIn(150L, 1200L)
        val todayKey = formatCalendarDateKey(startOfDayCalendar(nowEpochMs))
        val startKey = meta.startDateKey
        var endKey = if (meta.targetDateKey < startKey) startKey else meta.targetDateKey
        if (daysBetween(startKey, endKey) > MAX_PLAN_DAYS) {
            endKey = addDaysToKey(startKey, MAX_PLAN_DAYS)
        }

        // If several runs were logged on the same date, the longest one counts for the session.
        val workoutsByDate: Map<String, WorkoutLog> = workouts
            .groupBy { workoutDateKey(it, nowEpochMs) }
            .mapValues { (_, list) -> list.maxBy { it.distanceKm } }

        val slots = mutableListOf<AdaptiveTrainingAi.PlanSlot>()
        val cursor = startOfDayCalendar(parseDateKey(startKey)?.time ?: nowEpochMs)
        var guard = 0
        while (guard <= MAX_PLAN_DAYS + 1) {
            val key = formatCalendarDateKey(cursor)
            if (key > endKey) break
            val isoDay = calendarToIsoDayOfWeek(cursor)
            val isRaceDay = key == meta.targetDateKey
            if (isRaceDay || isoDay in meta.trainingDaysIso) {
                slots += AdaptiveTrainingAi.PlanSlot(
                    dateKey = key,
                    epochDay = epochDayOf(key),
                    isoDay = isoDay,
                    isRaceDay = isRaceDay
                )
            }
            cursor.add(Calendar.DAY_OF_YEAR, 1)
            guard++
        }
        if (slots.isEmpty()) return emptyList()

        val avgSessionKm = (goal.weeklyMileageTargetKm.toDouble() / meta.trainingDaysIso.size.coerceAtLeast(1))
            .coerceIn(3.0, 25.0)
        val currentRacePace = AdaptiveTrainingAi.estimateCurrentRacePace(workouts, category, goalRacePaceSec, nowEpochMs)
        val decisions = AdaptiveTrainingAi.planSessionTypes(
            slots = slots,
            workouts = workouts,
            goalRacePaceSec = goalRacePaceSec,
            currentRacePaceSec = currentRacePace,
            category = category,
            todayEpochDay = epochDayOf(todayKey),
            trainingDaysIso = meta.trainingDaysIso,
            avgSessionKm = avgSessionKm
        )

        val startEpoch = epochDayOf(startKey)
        val raceEpoch = epochDayOf(meta.targetDateKey)

        return slots.mapIndexed { index, slot ->
            val adaptive = decisions[index]
            val override = if (slot.isRaceDay) null else aiOverrides[slot.dateKey]
            val type = override?.type ?: adaptive.type
            val reason = override?.reason?.takeIf { it.isNotBlank() } ?: adaptive.reason
            val source = if (override != null) PlanDecisionSource.GEMINI_AI else PlanDecisionSource.ADAPTIVE_AI

            // Pace targets progress from current fitness towards goal pace as race day approaches.
            val progress = if (raceEpoch > startEpoch) {
                ((slot.epochDay - startEpoch).toDouble() / (raceEpoch - startEpoch)).coerceIn(0.0, 1.0)
            } else {
                1.0
            }
            val basePace = if (currentRacePace <= goalRacePaceSec) {
                currentRacePace
            } else {
                (currentRacePace + (goalRacePaceSec - currentRacePace) * progress).roundToLong()
            }
            val daysToRace = (raceEpoch - slot.epochDay).toInt()

            val prescription = buildSessionPrescription(
                wType = type,
                category = category,
                basePaceSec = basePace,
                goalRacePaceSec = goalRacePaceSec,
                avgSessionKm = avgSessionKm,
                daysToRace = daysToRace,
                cleanGoalTitle = meta.cleanTitle
            )

            val matched = workoutsByDate[slot.dateKey]
            val status = when {
                matched != null -> ScheduledSessionStatus.COMPLETED
                slot.dateKey == todayKey -> ScheduledSessionStatus.TODAY_PENDING
                slot.dateKey < todayKey -> ScheduledSessionStatus.SKIPPED
                else -> ScheduledSessionStatus.UPCOMING
            }

            val dayNameStr = dayNameId(slot.isoDay)
            ScheduledTrainingSession(
                dateKey = slot.dateKey,
                displayDate = "$dayNameStr, ${formatShortDate(slot.dateKey)}",
                dayOfWeekIso = slot.isoDay,
                dayName = dayNameStr,
                workoutType = type,
                sessionTitle = prescription.title,
                sessionDescription = prescription.desc,
                recommendedDistanceKm = prescription.distKm,
                targetPaceSecondsPerKm = prescription.paceSec,
                status = status,
                matchedWorkout = matched,
                aiReason = reason,
                decisionSource = source
            )
        }
    }

    /**
     * Signature used to decide whether a cached Gemini plan is stale:
     * changes whenever the goal schedule, the workout history, or the current day changes.
     */
    fun planSignature(goal: TrainingGoal, workouts: List<WorkoutLog>, todayKey: String = todayKey()): String {
        val workoutPart = workouts
            .map { "${it.id}|${workoutDateKey(it)}|${it.distanceKm}|${it.durationSeconds}" }
            .sorted()
            .joinToString(";")
        val raw = "${goal.title}|${goal.targetDistanceCategory}|${goal.targetTimeSeconds}|" +
            "${goal.weeklyMileageTargetKm}|$todayKey|$workoutPart"
        return raw.hashCode().toString(16)
    }

    private data class Prescription(
        val distKm: Double,
        val paceSec: Long,
        val title: String,
        val desc: String
    )

    private fun buildSessionPrescription(
        wType: SessionWorkoutType,
        category: DistanceCategory,
        basePaceSec: Long,
        goalRacePaceSec: Long,
        avgSessionKm: Double,
        daysToRace: Int,
        cleanGoalTitle: String
    ): Prescription {
        val taperFactor = if (daysToRace in 1..7) 0.7 else 1.0
        return when (wType) {
            SessionWorkoutType.INTERVAL -> {
                val p = (basePaceSec * 0.93).roundToLong().coerceAtLeast(150L)
                val d = (avgSessionKm * 0.8 * taperFactor).coerceIn(3.0, 10.0)
                val reps = when (category) {
                    DistanceCategory.FIVE_K -> "6x400m"
                    DistanceCategory.TEN_K -> "5x800m"
                    DistanceCategory.HALF_MARATHON -> "4x1600m"
                    DistanceCategory.MARATHON -> "3x2km"
                }
                Prescription(
                    distKm = d,
                    paceSec = p,
                    title = "Interval Speed (${"%.1f".format(d)} km)",
                    desc = "Pemanasan 1 km + $reps pada pace ${RacePredictionEngine.formatPace(p)} (jeda jalan/jog 90 detik) untuk mendongkrak VO2Max."
                )
            }
            SessionWorkoutType.TEMPO -> {
                val p = basePaceSec.coerceAtLeast(160L)
                val d = (avgSessionKm * 0.95 * taperFactor).coerceIn(3.0, 14.0)
                Prescription(
                    distKm = d,
                    paceSec = p,
                    title = "Tempo Run (${"%.1f".format(d)} km)",
                    desc = "Lari ritme stabil di sekitar ${RacePredictionEngine.formatPace(p)} untuk melatih ambang batas asam laktat."
                )
            }
            SessionWorkoutType.RECOVERY -> {
                val p = (basePaceSec * 1.18).roundToLong()
                val d = (avgSessionKm * 0.65 * taperFactor).coerceIn(3.0, 8.0)
                Prescription(
                    distKm = d,
                    paceSec = p,
                    title = "Recovery / Easy Run (${"%.1f".format(d)} km)",
                    desc = "Lari ringan Zona 1-2 pada pace santai ${RacePredictionEngine.formatPace(p)} — harus bisa sambil mengobrol."
                )
            }
            SessionWorkoutType.EASY_LONG -> {
                val p = (basePaceSec * 1.12).roundToLong()
                val d = (avgSessionKm * 1.25 * taperFactor).coerceIn(5.0, (category.kilometers * 1.1).coerceAtLeast(6.0))
                Prescription(
                    distKm = d,
                    paceSec = p,
                    title = "Long Run (${"%.1f".format(d)} km)",
                    desc = "Lari daya tahan aerobik Zona 2 pada pace ${RacePredictionEngine.formatPace(p)} guna memperkuat fondasi jarak ${category.shortLabel}."
                )
            }
            SessionWorkoutType.RACE_DAY -> Prescription(
                distKm = category.kilometers,
                paceSec = goalRacePaceSec,
                title = "RACE DAY: $cleanGoalTitle (${category.shortLabel})",
                desc = "Hari Lomba! Jaga target pace ${RacePredictionEngine.formatPace(goalRacePaceSec)} untuk mencapai target waktu " +
                    "${RacePredictionEngine.formatDuration((goalRacePaceSec * category.kilometers).roundToLong())}."
            )
        }
    }

    fun workoutDateKey(workout: WorkoutLog, fallbackMs: Long = System.currentTimeMillis()): String {
        val dt = workout.createdAt?.toDate() ?: Date(fallbackMs)
        return formatDateKey(dt)
    }

    fun todayKey(nowEpochMs: Long = System.currentTimeMillis()): String =
        formatCalendarDateKey(startOfDayCalendar(nowEpochMs))

    fun defaultTargetDateKey(daysFromNow: Int = 21): String {
        val cal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, daysFromNow)
        }
        return formatDateKey(cal.time)
    }

    fun addDaysToKey(dateKey: String, days: Int): String {
        val base = parseDateKey(dateKey) ?: Date()
        val cal = Calendar.getInstance().apply {
            time = base
            add(Calendar.DAY_OF_YEAR, days)
        }
        return formatDateKey(cal.time)
    }

    /** Whole days from [fromKey] to [toKey] (negative if toKey is earlier). */
    fun daysBetween(fromKey: String, toKey: String): Long = epochDayOf(toKey) - epochDayOf(fromKey)

    /** Timezone-independent day number for a yyyy-MM-dd key. */
    fun epochDayOf(dateKey: String): Long {
        val ms = try {
            synchronized(utcKeyFormat) { utcKeyFormat.parse(dateKey.trim())?.time }
        } catch (e: Exception) {
            null
        } ?: return 0L
        return Math.floorDiv(ms, DAY_MS)
    }

    fun parseDateKey(dateKey: String): Date? {
        return try {
            synchronized(dateKeyFormat) { dateKeyFormat.parse(dateKey.trim()) }
        } catch (e: Exception) {
            null
        }
    }

    /** "10 Nov 2026" */
    fun formatShortDate(dateKey: String): String {
        val d = parseDateKey(dateKey) ?: return dateKey
        return synchronized(displayShortFormat) { displayShortFormat.format(d) }
    }

    /** "Sel, 10 Nov 2026" */
    fun formatDisplayDate(dateKey: String): String {
        val d = parseDateKey(dateKey) ?: return dateKey
        val cal = Calendar.getInstance().apply { time = d }
        return "${dayShortNameId(calendarToIsoDayOfWeek(cal))}, ${formatShortDate(dateKey)}"
    }

    fun formatDateKey(date: Date): String = synchronized(dateKeyFormat) { dateKeyFormat.format(date) }

    fun startOfDayCalendar(epochMs: Long): Calendar {
        return Calendar.getInstance().apply {
            timeInMillis = epochMs
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    fun formatCalendarDateKey(cal: Calendar): String = formatDateKey(cal.time)

    fun calendarToIsoDayOfWeek(cal: Calendar): Int {
        return when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.FRIDAY -> 5
            Calendar.SATURDAY -> 6
            else -> 7
        }
    }
}
