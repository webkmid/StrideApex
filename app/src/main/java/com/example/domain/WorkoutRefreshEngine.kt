package com.example.domain

import com.example.ui.components.parseSplitsSummary
import java.util.Locale
import kotlin.math.floor
import kotlin.math.roundToLong

/**
 * Domain helper to keep per-kilometer splits and AI coaching evaluations
 * synchronized whenever a workout entry is edited or refreshed.
 */
object WorkoutRefreshEngine {

    /**
     * Recalculates or rescales KM splits to stay consistent with updated distance and average pace.
     * - Preserves the pacing shape (fast/slow relative ratios) when existing splits are present.
     * - Adjusts split count to match [newDistanceKm] and shifts values so average matches [newAvgPaceSec].
     */
    fun recalibrateSplits(
        oldSplitsSummary: String,
        newDistanceKm: Double,
        newAvgPaceSec: Long,
        generateIfEmpty: Boolean = true
    ): String {
        val safePace = newAvgPaceSec.coerceIn(120L, 1200L)
        val safeDist = newDistanceKm.coerceIn(0.1, 500.0)
        val targetCount = floor(safeDist + 0.05).toInt()
            .coerceAtLeast(if (safeDist >= 0.5) 1 else 0)
            .coerceAtMost(100)

        if (targetCount <= 0) return ""

        val oldParsed = parseSplitsSummary(oldSplitsSummary, safePace)
        if (oldParsed.isEmpty() && !generateIfEmpty) {
            return ""
        }

        val generatedPaces = mutableListOf<Long>()
        if (oldParsed.isNotEmpty()) {
            val oldMean = oldParsed.map { it.paceSeconds }.average().coerceAtLeast(1.0)
            for (idx in 0 until targetCount) {
                val basePace = if (idx < oldParsed.size) {
                    (oldParsed[idx].paceSeconds.toDouble() * safePace.toDouble() / oldMean)
                        .roundToLong()
                        .coerceIn(120L, 1200L)
                } else {
                    // Extend extra kilometers smoothly around new average pace
                    val slightOffset = when {
                        idx == targetCount - 1 && targetCount > 2 -> -3L
                        idx % 2 == 0 -> 2L
                        else -> -1L
                    }
                    (safePace + slightOffset).coerceIn(120L, 1200L)
                }
                generatedPaces.add(basePace)
            }
        } else {
            // Build deterministic split variation profile anchored around safePace
            for (idx in 0 until targetCount) {
                val delta = when {
                    targetCount == 1 -> 0L
                    idx == 0 -> 4L
                    idx == targetCount - 1 -> -4L
                    idx % 2 == 1 -> 1L
                    else -> -1L
                }
                generatedPaces.add((safePace + delta).coerceIn(120L, 1200L))
            }
        }

        // Balance cumulative rounding error so split average aligns closely with safePace
        val currentMeanDiff = safePace - generatedPaces.average().roundToLong()
        if (currentMeanDiff != 0L) {
            for (i in generatedPaces.indices) {
                generatedPaces[i] = (generatedPaces[i] + currentMeanDiff).coerceIn(120L, 1200L)
            }
        }

        return generatedPaces.mapIndexed { idx, paceSec ->
            val mins = paceSec / 60L
            val secs = paceSec % 60L
            String.format(Locale.US, "KM %d: %d:%02d/km", idx + 1, mins, secs)
        }.joinToString(" | ")
    }

    /**
     * Checks whether existing splits are out of sync with the workout's distance or average pace.
     */
    fun needsSplitRecalibration(
        splitsSummary: String,
        distanceKm: Double,
        avgPaceSec: Long
    ): Boolean {
        val parsed = parseSplitsSummary(splitsSummary, avgPaceSec)
        if (parsed.isEmpty()) return distanceKm >= 1.0
        val floorCount = floor(distanceKm).toInt().coerceAtLeast(1)
        val ceilCount = kotlin.math.ceil(distanceKm - 0.05).toInt().coerceAtLeast(floorCount)
        val countMismatch = distanceKm >= 1.0 && parsed.size !in floorCount..ceilCount
        val splitsAvg = parsed.map { it.paceSeconds }.average()
        val paceMismatch = kotlin.math.abs(splitsAvg - avgPaceSec) >= 12.0
        return countMismatch || paceMismatch
    }

    /**
     * Generates an immediate structured coaching insight reflecting newly updated telemetry
     * while remote Gemini AI refresh runs asynchronously.
     */
    fun buildLocalRefreshedAiInsight(
        title: String,
        distanceKm: Double,
        durationSeconds: Long,
        avgPaceSec: Long,
        avgHeartRate: Long,
        cadenceSpm: Long,
        elevationGainM: Long,
        splitsSummary: String
    ): String {
        val safeTitle = title.ifBlank { "Sesi Lari" }
        val parsedSplits = parseSplitsSummary(splitsSummary, avgPaceSec)
        val durStr = RacePredictionEngine.formatDuration(durationSeconds)
        val paceStr = RacePredictionEngine.formatPace(avgPaceSec)

        return buildString {
            appendLine("Analisis Diperbarui ($safeTitle):")
            appendLine(
                "1. Ringkasan Data Baru: Jarak ${String.format(Locale.US, "%.2f", distanceKm)} km selesai dalam waktu $durStr (Pace rata-rata $paceStr)."
            )
            if (parsedSplits.size >= 2) {
                val first = parsedSplits.first()
                val last = parsedSplits.last()
                val trend = when {
                    last.paceSeconds - first.paceSeconds < -5 -> "negative split (akhir lebih cepat)"
                    last.paceSeconds - first.paceSeconds > 15 -> "positive split (melambat di akhir)"
                    else -> "pacing merata dan konsisten"
                }
                appendLine(
                    "2. Profil Split (${parsedSplits.size} KM): Pola lari menunjukkan $trend, dari KM 1 (${first.paceStr}/km) hingga KM ${last.kmLabel} (${last.paceStr}/km)."
                )
            } else {
                appendLine(
                    "2. Konsistensi Ritme: Pace terkunci rata-rata di $paceStr sepanjang rute${if (elevationGainM > 0) " dengan elevasi +${elevationGainM} m" else ""}."
                )
            }
            val bioParts = mutableListOf<String>()
            if (avgHeartRate > 0) bioParts.add("HR rata-rata $avgHeartRate bpm")
            if (cadenceSpm > 0) bioParts.add("cadence $cadenceSpm spm")
            if (bioParts.isNotEmpty()) {
                append("3. Beban Biomekanik: ${bioParts.joinToString(" & ")}. Jaga hidrasi serta sesi easy/recovery di hari berikutnya.")
            } else {
                append("3. Rekomendasi: Pertahankan ritme aerobik konsisten dan jadwalkan recovery run ringan setelah sesi intens.")
            }
        }.trim()
    }
}
