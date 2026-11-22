package com.example.domain

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import com.example.data.model.DistanceCategory
import com.example.data.model.PersonalRecord
import com.example.data.model.RacePredictionResult
import com.example.data.model.TrainingPaceZone
import com.example.data.model.WorkoutLog
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.roundToLong

object RacePredictionEngine {

    /**
     * Computes Jack Daniels VDOT estimate from distance (meters) and time (seconds).
     */
    fun calculateVdot(distanceMeters: Double, durationSeconds: Long): Double {
        if (distanceMeters <= 0.0 || durationSeconds <= 0L) return 48.0
        val timeMinutes = durationSeconds / 60.0
        val velocityMetersPerMin = distanceMeters / timeMinutes
        // Oxygen cost of running (ml/kg/min)
        val vo2 = -4.60 + 0.182258 * velocityMetersPerMin + 0.000104 * velocityMetersPerMin.pow(2)
        // Fraction of VO2max sustained for duration
        val pctMax = 0.8 + 0.1894393 * exp(-0.012778 * timeMinutes) + 0.2989558 * exp(-0.1932605 * timeMinutes)
        return (vo2 / pctMax).coerceIn(28.0, 85.0)
    }

    /**
     * Computes algorithmic race predictions across 5K, 10K, Half Marathon, and Marathon
     * using Riegel's endurance power-law equation adjusted by weekly mileage durability.
     */
    fun computeRacePredictions(
        workouts: List<WorkoutLog>,
        prs: List<PersonalRecord>,
        fatigueExponent: Double = 1.06
    ): List<RacePredictionResult> {
        // Select strongest physiological baseline from PRs or workouts
        val bestPrCandidate = prs.maxByOrNull {
            calculateVdot(it.distanceMeters.toDouble(), it.bestTimeSeconds)
        }
        val bestWorkoutCandidate = workouts.filter { it.distanceKm >= 3.0 }.maxByOrNull {
            calculateVdot(it.distanceKm * 1000.0, it.durationSeconds)
        }

        val (baseDistanceM, baseTimeSec) = when {
            bestPrCandidate != null && bestWorkoutCandidate != null -> {
                val prVdot = calculateVdot(bestPrCandidate.distanceMeters.toDouble(), bestPrCandidate.bestTimeSeconds)
                val wkVdot = calculateVdot(bestWorkoutCandidate.distanceKm * 1000.0, bestWorkoutCandidate.durationSeconds)
                if (prVdot >= wkVdot) {
                    bestPrCandidate.distanceMeters.toDouble() to bestPrCandidate.bestTimeSeconds
                } else {
                    (bestWorkoutCandidate.distanceKm * 1000.0) to bestWorkoutCandidate.durationSeconds
                }
            }
            bestPrCandidate != null -> bestPrCandidate.distanceMeters.toDouble() to bestPrCandidate.bestTimeSeconds
            bestWorkoutCandidate != null -> (bestWorkoutCandidate.distanceKm * 1000.0) to bestWorkoutCandidate.durationSeconds
            else -> 10000.0 to 2460L // Default 41:00 10K baseline
        }

        val vdot = calculateVdot(baseDistanceM, baseTimeSec)
        val totalVolumeKm = workouts.sumOf { it.distanceKm }

        return DistanceCategory.entries.map { category ->
            val ratio = category.meters.toDouble() / baseDistanceM
            // Apply subtle volume durability modifier for Half & Full Marathon
            val distancePenaltyFactor = when (category) {
                DistanceCategory.MARATHON -> if (totalVolumeKm < 45.0) 1.022 else 1.0
                DistanceCategory.HALF_MARATHON -> if (totalVolumeKm < 30.0) 1.01 else 1.0
                else -> 1.0
            }
            val predictedSeconds = (baseTimeSec * ratio.pow(fatigueExponent) * distancePenaltyFactor).roundToLong()
            val pacePerKm = (predictedSeconds / category.kilometers).roundToLong()
            val note = when (category) {
                DistanceCategory.FIVE_K -> "VO2 Max ceiling & neuromuscular turnover"
                DistanceCategory.TEN_K -> "Lactate threshold sustainability benchmark"
                DistanceCategory.HALF_MARATHON -> "Aerobic threshold & glycogen sparing index"
                DistanceCategory.MARATHON -> "Fatigue exponent ${"%.2f".format(fatigueExponent)} + volume durability"
            }
            RacePredictionResult(
                category = category,
                predictedTimeSeconds = predictedSeconds,
                targetPaceSecondsPerKm = pacePerKm,
                vdotScore = vdot,
                confidenceNote = note
            )
        }
    }

    /**
     * Computes 5 scientific training pace zones anchored proportionally to Lactate Threshold Pace
     * (T-Pace ~ 1.03 x 10K equivalent pace) across all runner speeds.
     */
    fun computeTrainingPaceZones(tenKPaceSecondsPerKm: Long): List<TrainingPaceZone> {
        val base = tenKPaceSecondsPerKm.coerceIn(150L, 540L)
        val tPace = (base * 1.03)
        return listOf(
            TrainingPaceZone(
                zoneName = "Zone 1-2: Easy / Aerobic Base",
                code = "E",
                paceRangePerKm = "${formatPaceShort((tPace * 1.14).roundToLong())} - ${formatPaceShort((tPace * 1.30).roundToLong())} /km",
                physiologicalBenefit = "Mitochondrial density, capillary growth & active recovery",
                heartRatePctRange = "65% - 78% HRmax"
            ),
            TrainingPaceZone(
                zoneName = "Zone 3: Marathon / Aerobic Tempo",
                code = "M",
                paceRangePerKm = "${formatPaceShort((tPace * 1.04).roundToLong())} - ${formatPaceShort((tPace * 1.13).roundToLong())} /km",
                physiologicalBenefit = "Lipid oxidation efficiency & race-pace biomechanical economy",
                heartRatePctRange = "79% - 84% HRmax"
            ),
            TrainingPaceZone(
                zoneName = "Zone 4: Lactate Threshold",
                code = "T",
                paceRangePerKm = "${formatPaceShort((tPace * 0.97).roundToLong())} - ${formatPaceShort((tPace * 1.03).roundToLong())} /km",
                physiologicalBenefit = "Raises lactate clearance shuttle ceiling (60-min race effort)",
                heartRatePctRange = "85% - 90% HRmax"
            ),
            TrainingPaceZone(
                zoneName = "Zone 5A: VO2 Max Intervals",
                code = "I",
                paceRangePerKm = "${formatPaceShort((tPace * 0.90).roundToLong().coerceAtLeast(135L))} - ${formatPaceShort((tPace * 0.95).roundToLong().coerceAtLeast(145L))} /km",
                physiologicalBenefit = "Maximizes cardiac stroke volume & central oxygen uptake",
                heartRatePctRange = "93% - 98% HRmax"
            ),
            TrainingPaceZone(
                zoneName = "Zone 5B: Neuromuscular Repetition",
                code = "R",
                paceRangePerKm = "${formatPaceShort((tPace * 0.83).roundToLong().coerceAtLeast(120L))} - ${formatPaceShort((tPace * 0.89).roundToLong().coerceAtLeast(130L))} /km",
                physiologicalBenefit = "Fast-twitch fiber recruitment, tendon stiffness & running economy",
                heartRatePctRange = "98% - 100% Effort"
            )
        )
    }

    fun formatDuration(seconds: Long): String {
        val safe = seconds.coerceAtLeast(0L)
        val hrs = safe / 3600
        val mins = (safe % 3600) / 60
        val secs = safe % 60
        return if (hrs > 0) {
            "%d:%02d:%02d".format(hrs, mins, secs)
        } else {
            "%02d:%02d".format(mins, secs)
        }
    }

    fun formatPace(secondsPerKm: Long): String {
        val safe = secondsPerKm.coerceAtLeast(0L)
        val mins = safe / 60
        val secs = safe % 60
        return "%d:%02d /km".format(mins, secs)
    }

    private fun formatPaceShort(secondsPerKm: Long): String {
        val safe = secondsPerKm.coerceAtLeast(0L)
        val mins = safe / 60
        val secs = safe % 60
        return "%d:%02d".format(mins, secs)
    }

    /**
     * Generates a high-contrast synthetic Strava workout screenshot Bitmap on-device
     * so users can test Gemini multimodal OCR extraction immediately on the emulator.
     */
    fun generateSampleStravaScreenshotBitmap(presetIndex: Int): Bitmap {
        val width = 900
        val height = 1120
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Background gradient
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, 0f, height.toFloat(),
                Color.parseColor("#111318"),
                Color.parseColor("#1A1F29"),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Header bar
        val orangePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FC4C02")
        }
        canvas.drawRect(0f, 0f, width.toFloat(), 14f, orangePaint)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 42f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#9BA8BE")
            textSize = 24f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        val metricValuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 48f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }
        val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FC4C02")
            textSize = 26f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }

        data class SamplePreset(
            val title: String,
            val subtitle: String,
            val dist: String,
            val pace: String,
            val time: String,
            val elev: String,
            val hr: String,
            val cadence: String,
            val splits: List<Pair<String, String>>
        )

        val preset = when (presetIndex % 3) {
            0 -> SamplePreset(
                title = "Morning Carbon Tempo 10K",
                subtitle = "Run • Today at 6:42 AM • Track & Road Loop",
                dist = "10.00 km",
                pace = "3:56 /km",
                time = "39:20",
                elev = "64 m",
                hr = "172 bpm",
                cadence = "184 spm",
                splits = listOf(
                    "KM 1" to "4:02",
                    "KM 2" to "3:59",
                    "KM 4" to "3:56",
                    "KM 6" to "3:55",
                    "KM 8" to "3:53",
                    "KM 10" to "3:50"
                )
            )
            1 -> SamplePreset(
                title = "Half Marathon Progressive Long Run",
                subtitle = "Run • Yesterday at 7:15 AM • Waterfront Path",
                dist = "21.10 km",
                pace = "4:15 /km",
                time = "1:29:40",
                elev = "142 m",
                hr = "161 bpm",
                cadence = "180 spm",
                splits = listOf(
                    "KM 1-5" to "4:22",
                    "KM 6-10" to "4:18",
                    "KM 11-15" to "4:14",
                    "KM 16-18" to "4:10",
                    "KM 19-20" to "4:07",
                    "KM 21.1" to "4:03"
                )
            )
            else -> SamplePreset(
                title = "Evening Track 5K Speed Trial",
                subtitle = "Run • Tuesday at 6:00 PM • Tartan Oval",
                dist = "5.00 km",
                pace = "3:41 /km",
                time = "18:25",
                elev = "12 m",
                hr = "181 bpm",
                cadence = "190 spm",
                splits = listOf(
                    "KM 1" to "3:43",
                    "KM 2" to "3:42",
                    "KM 3" to "3:42",
                    "KM 4" to "3:40",
                    "KM 5" to "3:38"
                )
            )
        }

        canvas.drawText("STRAVA ACTIVITY SUMMARY", 48f, 70f, accentPaint)
        canvas.drawText(preset.title, 48f, 130f, titlePaint)
        canvas.drawText(preset.subtitle, 48f, 172f, labelPaint)

        // Primary Metrics Card
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#222836")
        }
        canvas.drawRoundRect(RectF(40f, 210f, width - 40f, 520f), 24f, 24f, cardPaint)

        // Row 1: Distance, Moving Time, Avg Pace
        canvas.drawText("Distance", 80f, 265f, labelPaint)
        canvas.drawText(preset.dist, 80f, 325f, metricValuePaint)

        canvas.drawText("Moving Time", 360f, 265f, labelPaint)
        canvas.drawText(preset.time, 360f, 325f, metricValuePaint)

        canvas.drawText("Avg Pace", 630f, 265f, labelPaint)
        canvas.drawText(preset.pace, 630f, 325f, metricValuePaint)

        // Row 2: Elevation, Avg Heart Rate, Cadence
        canvas.drawText("Elevation Gain", 80f, 410f, labelPaint)
        canvas.drawText(preset.elev, 80f, 470f, metricValuePaint)

        canvas.drawText("Avg Heart Rate", 360f, 410f, labelPaint)
        canvas.drawText(preset.hr, 360f, 470f, metricValuePaint)

        canvas.drawText("Cadence", 630f, 410f, labelPaint)
        canvas.drawText(preset.cadence, 630f, 470f, metricValuePaint)

        // Splits Section
        canvas.drawText("KILOMETER SPLITS BREAKDOWN", 48f, 590f, accentPaint)
        val barBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#2A3244")
        }
        val barFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FC4C02")
        }
        val splitTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 28f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }

        preset.splits.forEachIndexed { idx, (kmLabel, splitPace) ->
            val top = 630f + idx * 72f
            canvas.drawText(kmLabel, 52f, top + 34f, splitTextPaint)
            canvas.drawRoundRect(RectF(230f, top + 6f, 680f, top + 44f), 10f, 10f, barBgPaint)
            val fillWidth = 340f + (idx * 18f)
            canvas.drawRoundRect(RectF(230f, top + 6f, 230f + fillWidth, top + 44f), 10f, 10f, barFillPaint)
            canvas.drawText("$splitPace /km", 705f, top + 34f, splitTextPaint)
        }

        return bitmap
    }
}
