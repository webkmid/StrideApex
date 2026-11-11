package com.example.util

import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Log
import com.example.data.model.ExtractedStravaMetrics
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern
import kotlin.coroutines.resume
import kotlin.math.abs
import kotlin.math.roundToLong

object StravaLocalOcrParser {
    private const val TAG = "StravaLocalOcrParser"

    private val textRecognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    suspend fun parseStravaImage(bitmap: Bitmap): ExtractedStravaMetrics? {
        val visionText = recognizeVisionText(bitmap) ?: return null

        // 1. Reconstruct text by visual rows based on bounding box coordinates
        val spatiallySortedText = reconstructTextByRows(visionText)
        Log.d(TAG, "Spatially reconstructed OCR text:\n$spatiallySortedText")

        val spatialMetrics = parseStravaText(spatiallySortedText)
        if (spatialMetrics != null && (spatialMetrics.durationSeconds > 0 || spatialMetrics.avgPaceSecondsPerKm > 0 || spatialMetrics.distanceKm > 0.5)) {
            return spatialMetrics
        }

        // 2. Fallback to raw text from ML Kit
        Log.d(TAG, "Fallback to raw ML Kit text:\n${visionText.text}")
        return parseStravaText(visionText.text)
    }

    private suspend fun recognizeVisionText(bitmap: Bitmap): Text? = suspendCancellableCoroutine { cont ->
        try {
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            textRecognizer.process(inputImage)
                .addOnSuccessListener { visionText ->
                    cont.resume(visionText)
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "ML Kit text recognition failed", e)
                    cont.resume(null)
                }
        } catch (e: Exception) {
            Log.w(TAG, "Error executing text recognition", e)
            cont.resume(null)
        }
    }

    private data class LineItem(val text: String, val rect: Rect)

    fun reconstructTextByRows(visionText: Text): String {
        val items = mutableListOf<LineItem>()
        for (block in visionText.textBlocks) {
            for (line in block.lines) {
                val box = line.boundingBox
                if (box != null && line.text.isNotBlank()) {
                    items.add(LineItem(line.text.trim(), box))
                }
            }
        }

        if (items.isEmpty()) return visionText.text

        // Group lines into rows if their vertical centers are within 75% of line height
        val rows = mutableListOf<MutableList<LineItem>>()
        for (item in items) {
            val matchingRow = rows.firstOrNull { row ->
                val rowAvgCenterY = row.map { it.rect.centerY() }.average()
                val rowAvgHeight = row.map { it.rect.height() }.average().coerceAtLeast(15.0)
                abs(item.rect.centerY() - rowAvgCenterY) < (rowAvgHeight * 0.75)
            }

            if (matchingRow != null) {
                matchingRow.add(item)
            } else {
                rows.add(mutableListOf(item))
            }
        }

        // Sort rows top-to-bottom
        rows.sortBy { row -> row.map { it.rect.top }.minOrNull() ?: 0 }

        // Inside each row, sort left-to-right
        val lines = rows.map { row ->
            row.sortedBy { it.rect.left }.joinToString(" ") { it.text }
        }

        return lines.joinToString("\n")
    }

    fun parseStravaText(text: String): ExtractedStravaMetrics? {
        val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }

        // ==========================================
        // A. Kilometer Splits (e.g. "1 7:28", "2 7:39")
        // ==========================================
        val splitsFound = mutableListOf<String>()
        val splitsPaces = mutableListOf<Long>()

        val splitRegex = Pattern.compile("(?:^|\\b)([1-9][0-9]?)[.:\\s]+([0-9]{1,2}:[0-5][0-9])\\b", Pattern.MULTILINE)
        val splitMatcher = splitRegex.matcher(text)
        while (splitMatcher.find()) {
            val kmNum = splitMatcher.group(1)?.toIntOrNull()
            val kmPaceStr = splitMatcher.group(2).orEmpty()
            val paceSec = parseTimeStringToSeconds(kmPaceStr)

            // Valid pace between 2:00/km (120s) and 15:00/km (900s)
            if (kmNum != null && kmNum in 1..99 && paceSec in 120L..900L) {
                val expected = splitsFound.size + 1
                if (kmNum == expected || splitsFound.isEmpty()) {
                    splitsFound.add("KM $kmNum: $kmPaceStr/km")
                    splitsPaces.add(paceSec)
                }
            }
        }

        // If split numbers and paces were on adjacent lines
        if (splitsFound.size < 2) {
            val pacePattern = Pattern.compile("^[0-9]{1,2}:[0-5][0-9]$")
            for (i in 0 until lines.size - 1) {
                val lineCurrent = lines[i].trim()
                val lineNext = lines[i + 1].trim()
                val kmNum = lineCurrent.toIntOrNull()
                if (kmNum != null && kmNum in 1..99 && pacePattern.matcher(lineNext).matches()) {
                    val pSec = parseTimeStringToSeconds(lineNext)
                    if (pSec in 120L..900L) {
                        val expected = splitsFound.size + 1
                        if (kmNum == expected || splitsFound.isEmpty()) {
                            splitsFound.add("KM $kmNum: $lineNext/km")
                            splitsPaces.add(pSec)
                        }
                    }
                }
            }
        }

        // ==========================================
        // B. Average Pace (e.g., "6:59 /km" or "7:32 /km")
        // ==========================================
        var avgPaceSec = 0L

        // Priority 1: Pace value explicitly followed by /km or /mi (e.g. "6:59 /km")
        val paceWithUnitRegex = Pattern.compile("\\b([0-9]{1,2}:[0-5][0-9])\\s*/(?:km|mi)\\b", Pattern.CASE_INSENSITIVE)
        val paceUnitMatcher = paceWithUnitRegex.matcher(text)
        if (paceUnitMatcher.find()) {
            avgPaceSec = parseTimeStringToSeconds(paceUnitMatcher.group(1).orEmpty())
        }

        // Priority 2: "Avg Pace" / "Average Pace" / "Kecepatan Rata-Rata" label followed by mm:ss
        if (avgPaceSec <= 0L) {
            val avgPaceRegex = Pattern.compile("(?:Avg Pace|Average Pace|Kec(?:epatan)?\\s*Rata[- ]?Rata|Pace Rata[- ]?Rata)\\s*[:]?\\s*([0-9]{1,2}:[0-5][0-9])", Pattern.CASE_INSENSITIVE)
            val avgPaceMatcher = avgPaceRegex.matcher(text)
            if (avgPaceMatcher.find()) {
                avgPaceSec = parseTimeStringToSeconds(avgPaceMatcher.group(1).orEmpty())
            }
        }

        // Priority 3: Line proximity check for Pace / Kecepatan
        if (avgPaceSec <= 0L) {
            val paceP = Pattern.compile("([0-9]{1,2}:[0-5][0-9])")
            for (i in lines.indices) {
                val line = lines[i]
                if (line.contains("Avg Pace", ignoreCase = true) || line.contains("Kecepatan", ignoreCase = true)) {
                    if (i + 1 < lines.size) {
                        val mNext = paceP.matcher(lines[i + 1])
                        if (mNext.find()) {
                            val s = parseTimeStringToSeconds(mNext.group(1))
                            if (s in 120L..900L) { avgPaceSec = s; break }
                        }
                    }
                }
            }
        }

        // Priority 4: Average of detected splits
        if (avgPaceSec <= 0L && splitsPaces.isNotEmpty()) {
            avgPaceSec = (splitsPaces.average()).roundToLong()
        }

        // ==========================================
        // C. Moving / Elapsed Time (e.g. "34:26" or "30:14")
        // ==========================================
        var movingTimeSec = 0L

        // Priority 1: Direct label matching for Moving Time or Elapsed Time (supports race activities)
        val timeLabelRegex = Pattern.compile("(?:Moving Time|Elapsed Time|Waktu Bergerak|Waktu Berlalu|Waktu Tempuh)\\s*[:]?\\s*([0-9]{1,2}:[0-5][0-9](?::[0-5][0-9])?)", Pattern.CASE_INSENSITIVE)
        val timeLabelMatcher = timeLabelRegex.matcher(text)
        if (timeLabelMatcher.find()) {
            movingTimeSec = parseTimeStringToSeconds(timeLabelMatcher.group(1).orEmpty())
        }

        // Priority 2: Line proximity check for Time / Elapsed Time (value on line below label)
        if (movingTimeSec <= 0L) {
            val timeP = Pattern.compile("(?<!/)\\b([0-9]{1,2}:[0-5][0-9](?::[0-5][0-9])?)\\b(?!/)")
            for (i in lines.indices) {
                val line = lines[i]
                if (line.contains("Moving Time", ignoreCase = true) ||
                    line.contains("Elapsed Time", ignoreCase = true) ||
                    line.contains("Waktu", ignoreCase = true) ||
                    line.equals("Time", ignoreCase = true)
                ) {
                    // Check next line
                    if (i + 1 < lines.size) {
                        val mNext = timeP.matcher(lines[i + 1])
                        if (mNext.find()) {
                            val s = parseTimeStringToSeconds(mNext.group(1))
                            if (s >= 60L && s != avgPaceSec) {
                                movingTimeSec = s
                                break
                            }
                        }
                    }
                }
            }
        }

        // Priority 3: Standalone duration token mm:ss (that is not a pace with /km and not in splits)
        if (movingTimeSec <= 0L) {
            val standaloneTimeRegex = Pattern.compile("\\b([1-9][0-9]{1,2}:[0-5][0-9](?::[0-5][0-9])?)\\b(?!\\s*/(?:km|mi))")
            val standaloneMatcher = standaloneTimeRegex.matcher(text)
            while (standaloneMatcher.find()) {
                val candidate = parseTimeStringToSeconds(standaloneMatcher.group(1).orEmpty())
                if (candidate >= 180L && candidate !in splitsPaces && candidate != avgPaceSec) {
                    movingTimeSec = candidate
                    break
                }
            }
        }

        // ==========================================
        // D. Total Distance (in kilometers)
        // ==========================================
        var distanceKm = 0.0

        // Priority 1: Explicit "Distance" or "Jarak" label with adjacent/subsequent value (e.g. "4.92 km")
        val numWithKmRegex = Pattern.compile("(?<!/)\\b([0-9]+[.,][0-9]+)\\s*(?:km)?\\b(?!/)", Pattern.CASE_INSENSITIVE)
        for (i in lines.indices) {
            val line = lines[i]
            if (line.contains("Distance", ignoreCase = true) || line.contains("Jarak", ignoreCase = true)) {
                // Check line immediately below label
                if (i + 1 < lines.size && !isChartAxisLine(lines[i + 1])) {
                    val mNext = numWithKmRegex.matcher(lines[i + 1])
                    if (mNext.find()) {
                        val candidate = mNext.group(1).replace(",", ".").toDoubleOrNull() ?: 0.0
                        if (candidate in 0.1..500.0) {
                            distanceKm = candidate
                            break
                        }
                    }
                }
                // Check same line after label
                val mSame = numWithKmRegex.matcher(line)
                while (mSame.find()) {
                    val candidate = mSame.group(1).replace(",", ".").toDoubleOrNull() ?: 0.0
                    if (candidate in 0.1..500.0 && !isChartAxisLine(line)) {
                        distanceKm = candidate
                        break
                    }
                }
                if (distanceKm > 0.0) break
            }
        }

        // Priority 2: Standalone distance explicitly printed with "km" (e.g. "4.92 km", "10.00 km")
        // but strictly exclude chart axis ticks
        if (distanceKm <= 0.0) {
            for (line in lines) {
                if (isChartAxisLine(line)) continue
                val kmRegex = Pattern.compile("(?<!/)\\b([0-9]+[.,][0-9]+)\\s*km\\b(?!/)", Pattern.CASE_INSENSITIVE)
                val kmMatcher = kmRegex.matcher(line)
                if (kmMatcher.find()) {
                    val candidate = kmMatcher.group(1)?.replace(",", ".")?.toDoubleOrNull() ?: 0.0
                    // If splits exist, distance must be at least near the split count
                    val minExpected = if (splitsFound.size >= 2) (splitsFound.size - 0.5) else 0.5
                    if (candidate >= minExpected) {
                        distanceKm = candidate
                        break
                    }
                }
            }
        }

        // Priority 3: Fallback calculation from Duration and Pace (e.g. for split graphs where total km isn't printed)
        val minExpectedSplits = if (splitsFound.size >= 2) (splitsFound.size - 0.5) else 0.5
        val isDistanceSuspiciouslySmall = distanceKm < minExpectedSplits && (splitsFound.size >= 2 || movingTimeSec >= 300L)
        if (distanceKm <= 0.0 || isDistanceSuspiciouslySmall) {
            if (movingTimeSec > 0 && avgPaceSec > 0) {
                val calcDist = movingTimeSec.toDouble() / avgPaceSec.toDouble()
                distanceKm = ((calcDist * 100).roundToLong()) / 100.0
            } else if (splitsFound.isNotEmpty()) {
                distanceKm = splitsFound.size.toDouble()
            }
        }

        // ==========================================
        // E. Elevation, Heart Rate, Cadence & Steps
        // ==========================================
        var elevation = 0L
        val elevRegex = Pattern.compile("(?:Elevation Gain|Elev Gain|Gain|Elevasi|Ketinggian)\\s*[:]?\\s*\\+?([0-9]+)\\s*m\\b", Pattern.CASE_INSENSITIVE)
        val elevMatcher = elevRegex.matcher(text)
        if (elevMatcher.find()) {
            elevation = elevMatcher.group(1)?.toLongOrNull() ?: 0L
        } else {
            // Proximity check for elevation
            for (i in lines.indices) {
                val l = lines[i]
                if (l.contains("Elevation Gain", ignoreCase = true) || l.contains("Elevasi", ignoreCase = true)) {
                    if (i + 1 < lines.size) {
                        val em = Pattern.compile("\\b([0-9]+)\\s*m\\b", Pattern.CASE_INSENSITIVE).matcher(lines[i + 1])
                        if (em.find()) {
                            elevation = em.group(1)?.toLongOrNull() ?: 0L
                            break
                        }
                    }
                }
            }
        }

        var hr = 0L
        val hrRegex = Pattern.compile("([0-9]{2,3})\\s*(?:bpm|BPM)")
        val hrMatcher = hrRegex.matcher(text)
        if (hrMatcher.find()) {
            hr = hrMatcher.group(1)?.toLongOrNull() ?: 0L
        }

        var cadence = 0L
        val cadRegex = Pattern.compile("([0-9]{2,3})\\s*(?:spm|SPM)")
        val cadMatcher = cadRegex.matcher(text)
        if (cadMatcher.find()) {
            cadence = cadMatcher.group(1)?.toLongOrNull() ?: 0L
        }

        // Steps detection (e.g. "Steps 4,806") -> infer cadence if cadence was not directly present
        var steps = 0L
        val stepsRegex = Pattern.compile("(?:Steps|Langkah)\\s*[:]?\\s*([0-9,.]+)", Pattern.CASE_INSENSITIVE)
        val stepsMatcher = stepsRegex.matcher(text)
        if (stepsMatcher.find()) {
            steps = stepsMatcher.group(1)?.replace(",", "")?.replace(".", "")?.toLongOrNull() ?: 0L
        } else {
            for (i in lines.indices) {
                val l = lines[i]
                if (l.contains("Steps", ignoreCase = true) || l.contains("Langkah", ignoreCase = true)) {
                    if (i + 1 < lines.size) {
                        val sm = Pattern.compile("([0-9,.]+)").matcher(lines[i + 1])
                        if (sm.find()) {
                            steps = sm.group(1).replace(",", "").replace(".", "").toLongOrNull() ?: 0L
                            break
                        }
                    }
                }
            }
        }
        if (cadence <= 0L && steps > 0L && movingTimeSec >= 60L) {
            val minutes = movingTimeSec.toDouble() / 60.0
            cadence = (steps.toDouble() / minutes).roundToLong().coerceIn(100L, 240L)
        }

        // ==========================================
        // F. Title Detection (e.g. "Anlene Run")
        // ==========================================
        val titleCandidate = lines.firstOrNull { l ->
            val clean = l.trim()
            !clean.contains(":") &&
            !clean.contains("<") &&
            !clean.contains("·") &&
            !clean.contains("http") &&
            !clean.contains("with ", ignoreCase = true) &&
            !clean.contains("fastest", ignoreCase = true) &&
            !clean.contains("miles", ignoreCase = true) &&
            !clean.contains("ever", ignoreCase = true) &&
            !clean.contains("PR", ignoreCase = true) &&
            !clean.contains("Manage", ignoreCase = true) &&
            !clean.contains("Group", ignoreCase = true) &&
            !clean.contains("Remove", ignoreCase = true) &&
            !clean.contains("Festival", ignoreCase = true) &&
            !clean.contains("Pace", ignoreCase = true) &&
            !clean.contains("Time", ignoreCase = true) &&
            !clean.contains("Distance", ignoreCase = true) &&
            !clean.contains("Jarak", ignoreCase = true) &&
            !clean.contains("Elevation", ignoreCase = true) &&
            !clean.contains("Steps", ignoreCase = true) &&
            !clean.contains("Langkah", ignoreCase = true) &&
            !clean.equals("Run", ignoreCase = true) &&
            !clean.equals("Hard", ignoreCase = true) &&
            !clean.equals("Race", ignoreCase = true) &&
            clean.length in 4..35
        } ?: "Strava Run"

        // If nothing valid was extracted, return null
        if (distanceKm <= 0.0 && movingTimeSec <= 0L && avgPaceSec <= 0L && splitsFound.isEmpty()) {
            return null
        }

        // ==========================================
        // G. Splits Summary & Coaching Insight
        // ==========================================
        // If no per-km splits are present on this screen, leave splitsSummary EMPTY
        // so the user is not confused by placeholder text.
        val splitsSummary = if (splitsFound.isNotEmpty()) {
            splitsFound.joinToString(" | ")
        } else {
            ""
        }

        val paceMin = if (avgPaceSec > 0) avgPaceSec / 60 else 0
        val paceSecRem = if (avgPaceSec > 0) avgPaceSec % 60 else 0
        val timeMin = movingTimeSec / 60
        val timeSecRem = movingTimeSec % 60

        val coachingInsight = if (splitsFound.size >= 2) {
            val firstSplit = splitsFound.first()
            val lastSplit = splitsFound.last()
            val paceDiff = splitsPaces.last() - splitsPaces.first()
            val pacingTrend = if (paceDiff < -5) {
                "Pacing Anda menunjukkan negative split yang sangat baik (km akhir lebih cepat)."
            } else if (paceDiff > 15) {
                "Pacing cenderung menurun di kilometer akhir. Disarankan mulai lebih santai agar stamina merata."
            } else {
                "Distribusi pacing sangat stabil dan konsisten sepanjang sesi lari."
            }
            "Analisis Performa ($titleCandidate):\n" +
                "1. Pacing & Ritme: $pacingTrend Rentang split tercatat dari $firstSplit hingga $lastSplit (rata-rata $paceMin:%02d/km).\n" +
                "2. Fisiologi & Mekanika: Waktu tempuh total ${timeMin}m ${timeSecRem}s.${if (cadence > 0) " Cadence rata-rata $cadence spm mendukung turnover langkah efisien." else ""}${if (hr > 0) " Denyut jantung rata-rata $hr bpm." else ""}\n" +
                "3. Rekomendasi Latihan Selanjutnya: Lakukan recovery run ringan 20-30 menit atau sesi peregangan hamstring dan betis sebelum sesi tempo/interval berikutnya."
                    .format(paceSecRem)
        } else if (distanceKm > 0.0 && movingTimeSec > 0L) {
            "Analisis Performa ($titleCandidate):\n" +
                "1. Capaian Sesi: Jarak tempuh ${"%.2f".format(distanceKm)} km selesai dalam ${timeMin}m ${timeSecRem}s dengan pace rata-rata $paceMin:%02d/km.\n" +
                "2. Adaptasi Aerobik: Latihan ini berhasil memicu adaptasi kapasitas aerobik (VDOT).${if (elevation > 0) " Kenaikan elevasi +$elevation m melatih ketahanan otot gluteus & quad." else ""}\n" +
                "3. Rekomendasi Latihan Selanjutnya: Jaga hidrasi serta tambahkan sesi interval 4x400m di hari lari berikutnya untuk melatih ambang batas laktat."
                    .format(paceSecRem)
        } else {
            "Analisis AI: Aktivitas lari berhasil terekam. Pertahankan konsistensi latihan mingguan dan kombinasikan dengan variasi easy run serta interval untuk peningkatan performa yang optimal."
        }

        val (detectedDateEpochMs, detectedDateStr) = parseWorkoutDateFromText(text)

        return ExtractedStravaMetrics(
            title = titleCandidate,
            distanceKm = distanceKm,
            durationSeconds = movingTimeSec,
            avgPaceSecondsPerKm = avgPaceSec,
            avgHeartRate = hr,
            cadenceSpm = cadence,
            elevationGainM = elevation,
            splitsSummary = splitsSummary,
            aiCoachingInsight = coachingInsight,
            workoutDateEpochMs = detectedDateEpochMs,
            detectedDateStr = detectedDateStr
        )
    }

    private val MONTH_MAP = mapOf(
        "jan" to 0, "januari" to 0, "january" to 0,
        "feb" to 1, "februari" to 1, "february" to 1,
        "mar" to 2, "maret" to 2, "march" to 2,
        "apr" to 3, "april" to 3,
        "mei" to 4, "may" to 4,
        "jun" to 5, "juni" to 5, "june" to 5,
        "jul" to 6, "juli" to 6, "july" to 6,
        "agu" to 7, "agustus" to 7, "aug" to 7, "august" to 7,
        "sep" to 8, "september" to 8,
        "okt" to 9, "oktober" to 9, "oct" to 9, "october" to 9,
        "nov" to 10, "november" to 10,
        "des" to 11, "desember" to 11, "dec" to 11, "december" to 11
    )

    fun parseWorkoutDateFromText(text: String): Pair<Long?, String> {
        val nowCal = Calendar.getInstance()

        // 1. "Today" / "Hari Ini" or "Yesterday" / "Kemarin"
        val relativePattern = Pattern.compile("(?:^|\\b)(today|hari\\s*ini|yesterday|kemarin)(?:\\s+at\\s+([0-2]?[0-9]:[0-5][0-9]))?", Pattern.CASE_INSENSITIVE)
        val relMatcher = relativePattern.matcher(text)
        if (relMatcher.find()) {
            val relWord = relMatcher.group(1)?.lowercase() ?: ""
            val timePart = relMatcher.group(2)
            val cal = Calendar.getInstance()
            if (relWord.contains("yesterday") || relWord.contains("kemarin")) {
                cal.add(Calendar.DAY_OF_YEAR, -1)
            }
            applyTimePartIfAny(cal, timePart)
            val dateStr = String.format(Locale.US, "%04d-%02d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
            return Pair(cal.timeInMillis, dateStr)
        }

        // 2. Pattern: "1 November 2026 at 05:31" or "29 Oct 2026"
        val dmyPattern = Pattern.compile("(?:^|\\b)([0-3]?[0-9])\\s+([A-Za-z]{3,9})\\s+(20[2-3][0-9])(?:\\s+at\\s+([0-2]?[0-9]:[0-5][0-9]))?", Pattern.CASE_INSENSITIVE)
        val dmyMatcher = dmyPattern.matcher(text)
        if (dmyMatcher.find()) {
            val day = dmyMatcher.group(1)?.toIntOrNull()
            val monthName = dmyMatcher.group(2)?.lowercase() ?: ""
            val year = dmyMatcher.group(3)?.toIntOrNull()
            val timePart = dmyMatcher.group(4)
            val month = MONTH_MAP[monthName] ?: MONTH_MAP[monthName.take(3)]

            if (day != null && day in 1..31 && year != null && month != null) {
                val cal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, day)
                }
                applyTimePartIfAny(cal, timePart)
                val dateStr = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day)
                return Pair(cal.timeInMillis, dateStr)
            }
        }

        // 3. Pattern: "November 1, 2026" or "Oct 29, 2026 at 06:15"
        val mdyPattern = Pattern.compile("(?:^|\\b)([A-Za-z]{3,9})\\s+([0-3]?[0-9]),?\\s+(20[2-3][0-9])(?:\\s+at\\s+([0-2]?[0-9]:[0-5][0-9]))?", Pattern.CASE_INSENSITIVE)
        val mdyMatcher = mdyPattern.matcher(text)
        if (mdyMatcher.find()) {
            val monthName = mdyMatcher.group(1)?.lowercase() ?: ""
            val day = mdyMatcher.group(2)?.toIntOrNull()
            val year = mdyMatcher.group(3)?.toIntOrNull()
            val timePart = mdyMatcher.group(4)
            val month = MONTH_MAP[monthName] ?: MONTH_MAP[monthName.take(3)]

            if (day != null && day in 1..31 && year != null && month != null) {
                val cal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, day)
                }
                applyTimePartIfAny(cal, timePart)
                val dateStr = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day)
                return Pair(cal.timeInMillis, dateStr)
            }
        }

        // 4. ISO or standard format: "2026-11-01" or "2026/11/01"
        val isoPattern = Pattern.compile("\\b(20[2-3][0-9])[-/.](0?[1-9]|1[0-2])[-/.](0?[1-9]|[12][0-9]|3[01])\\b")
        val isoMatcher = isoPattern.matcher(text)
        if (isoMatcher.find()) {
            val year = isoMatcher.group(1)?.toIntOrNull()
            val month = isoMatcher.group(2)?.toIntOrNull()?.minus(1)
            val day = isoMatcher.group(3)?.toIntOrNull()
            if (year != null && month != null && day != null) {
                val cal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, day)
                    set(Calendar.HOUR_OF_DAY, 6)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                }
                val dateStr = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day)
                return Pair(cal.timeInMillis, dateStr)
            }
        }

        return Pair(null, "")
    }

    private fun applyTimePartIfAny(cal: Calendar, timePart: String?) {
        if (!timePart.isNullOrBlank()) {
            val parts = timePart.split(":")
            if (parts.size >= 2) {
                val h = parts[0].toIntOrNull() ?: 6
                val m = parts[1].toIntOrNull() ?: 0
                cal.set(Calendar.HOUR_OF_DAY, h)
                cal.set(Calendar.MINUTE, m)
                cal.set(Calendar.SECOND, 0)
                return
            }
        }
        cal.set(Calendar.HOUR_OF_DAY, 6)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
    }

    private fun isChartAxisLine(line: String): Boolean {
        val clean = line.trim().lowercase()
        val kmCount = "km".toRegex().findAll(clean).count()
        if (kmCount >= 2) return true
        if (clean.matches(Regex("^(?:0\\.5|1\\.0|1\\.5|2\\.0|2\\.5|3\\.0|3\\.5|4\\.0|4\\.5|5\\.0)\\s*km$"))) return true
        if (clean.contains("0.5") && clean.contains("1.0")) return true
        return false
    }

    private fun parseTimeStringToSeconds(timeStr: String): Long {
        val clean = timeStr.trim().replace(".", ":")
        val parts = clean.split(":").mapNotNull { it.trim().toLongOrNull() }
        return when (parts.size) {
            2 -> parts[0] * 60L + parts[1]
            3 -> parts[0] * 3600L + parts[1] * 60L + parts[2]
            else -> 0L
        }
    }
}
