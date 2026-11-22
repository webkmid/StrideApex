package com.example.util

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.util.Log
import com.example.data.model.ExtractedStravaMetrics
import com.example.ui.components.parseSplitsSummary
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import java.text.SimpleDateFormat
import java.util.Calendar
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

    /**
     * Runs Dual-Pass ML Kit OCR (preprocessed high-contrast image + original image)
     * and merges the strongest split table + duration + avg pace detections.
     */
    suspend fun parseStravaImage(bitmap: Bitmap): ExtractedStravaMetrics? {
        val candidates = mutableListOf<ExtractedStravaMetrics>()
        val preprocessed = try {
            preprocessBitmapForOcr(bitmap)
        } catch (e: Exception) {
            Log.w(TAG, "Image preprocessing failed, falling back to original bitmap", e)
            null
        }

        try {
            // Pass 1: Preprocessed high-contrast image (blue Strava bars & dark background normalized)
            if (preprocessed != null) {
                val visionClean = recognizeVisionText(preprocessed)
                if (visionClean != null) {
                    val spatialClean = reconstructTextByRows(visionClean)
                    Log.d(TAG, "Spatially reconstructed preprocessed OCR:\n$spatialClean")
                    parseStravaText(spatialClean)?.let { candidates.add(it) }
                    parseStravaText(visionClean.text)?.let { candidates.add(it) }
                }
            }

            // Pass 2: Original bitmap pass as backup / enrichment
            val visionOrig = recognizeVisionText(bitmap)
            if (visionOrig != null) {
                val spatialOrig = reconstructTextByRows(visionOrig)
                Log.d(TAG, "Spatially reconstructed original OCR:\n$spatialOrig")
                parseStravaText(spatialOrig)?.let { candidates.add(it) }
                parseStravaText(visionOrig.text)?.let { candidates.add(it) }
            }
        } finally {
            if (preprocessed != null && preprocessed !== bitmap && !preprocessed.isRecycled) {
                preprocessed.recycle()
            }
        }

        if (candidates.isEmpty()) return null

        val bestCandidate = candidates.maxByOrNull { scoreMetrics(it) } ?: return null
        val bestSplitsMetrics = candidates
            .filter { it.splitsSummary.isNotBlank() }
            .maxByOrNull { parseSplitsSummary(it.splitsSummary, it.avgPaceSecondsPerKm).size }

        val bestDuration = bestCandidate.durationSeconds.takeIf { it > 0L }
            ?: candidates.firstOrNull { it.durationSeconds > 0L }?.durationSeconds
            ?: 0L
        val bestAvgPace = bestCandidate.avgPaceSecondsPerKm.takeIf { it > 0L }
            ?: candidates.firstOrNull { it.avgPaceSecondsPerKm > 0L }?.avgPaceSecondsPerKm
            ?: 0L
        val bestSplits = bestSplitsMetrics?.splitsSummary ?: bestCandidate.splitsSummary

        // Reconcile if splits came from a different pass than Moving Time / Avg Pace
        if (bestSplits.isNotBlank() && bestSplitsMetrics != null && bestSplitsMetrics !== bestCandidate) {
            val syntheticText = buildString {
                if (bestCandidate.title.isNotBlank() && bestCandidate.title != "Strava Run") {
                    appendLine(bestCandidate.title)
                }
                if (bestDuration > 0L) {
                    appendLine("Moving Time ${formatSecondsToClock(bestDuration)}")
                }
                if (bestAvgPace > 0L) {
                    appendLine("Avg Pace ${formatSecondsToClock(bestAvgPace)} /km")
                }
                appendLine(bestSplits)
            }
            val reconciled = parseStravaText(syntheticText)
            if (reconciled != null) {
                val hr = candidates.maxOf { it.avgHeartRate }
                val cad = candidates.maxOf { it.cadenceSpm }
                val elev = candidates.maxOf { it.elevationGainM }
                val dateCand = candidates.firstOrNull { it.workoutDateEpochMs != null }
                return reconciled.copy(
                    avgHeartRate = if (hr > 0L) hr else reconciled.avgHeartRate,
                    cadenceSpm = if (cad > 0L) cad else reconciled.cadenceSpm,
                    elevationGainM = if (elev > 0L) elev else reconciled.elevationGainM,
                    workoutDateEpochMs = dateCand?.workoutDateEpochMs ?: reconciled.workoutDateEpochMs,
                    detectedDateStr = dateCand?.detectedDateStr ?: reconciled.detectedDateStr
                )
            }
        }

        return bestCandidate.copy(
            durationSeconds = bestDuration,
            avgPaceSecondsPerKm = bestAvgPace,
            splitsSummary = bestSplits
        )
    }

    private fun scoreMetrics(m: ExtractedStravaMetrics): Int {
        val splitCount = parseSplitsSummary(m.splitsSummary, m.avgPaceSecondsPerKm).size
        var score = splitCount * 20
        if (m.durationSeconds > 0L) score += 15
        if (m.avgPaceSecondsPerKm > 0L) score += 15
        if (m.distanceKm > 0.0) score += 10
        if (m.title.isNotBlank() && m.title != "Strava Run") score += 3
        return score
    }

    /**
     * Upscales small screenshots, strips Strava blue bars & pace chart fills to white,
     * and converts dark-mode white/gray typography into crisp dark ink on pure white.
     */
    internal fun preprocessBitmapForOcr(source: Bitmap): Bitmap {
        val maxDim = 2400
        val targetScale = if (source.width < 1200) 2.0f else 1.0f
        val maxSource = maxOf(source.width, source.height).coerceAtLeast(1)
        val clampedScale = minOf(targetScale, maxDim.toFloat() / maxSource.toFloat()).coerceAtLeast(1.0f)

        val scaled = if (clampedScale > 1.05f) {
            Bitmap.createScaledBitmap(
                source,
                (source.width * clampedScale).toInt().coerceAtLeast(1),
                (source.height * clampedScale).toInt().coerceAtLeast(1),
                true
            )
        } else {
            source
        }

        val w = scaled.width
        val h = scaled.height
        val pixels = IntArray(w * h)
        scaled.getPixels(pixels, 0, w, 0, 0, w, h)

        // Sample average luminance to detect Dark Mode vs Light Mode
        var lumSum = 0.0
        var sampleCount = 0
        val step = (pixels.size / 2000).coerceAtLeast(1)
        var i = 0
        while (i < pixels.size) {
            val px = pixels[i]
            val r = Color.red(px)
            val g = Color.green(px)
            val b = Color.blue(px)
            lumSum += 0.299 * r + 0.587 * g + 0.114 * b
            sampleCount++
            i += step
        }
        val isDarkMode = sampleCount > 0 && (lumSum / sampleCount) < 128.0

        for (idx in pixels.indices) {
            val px = pixels[idx]
            val r = Color.red(px)
            val g = Color.green(px)
            val b = Color.blue(px)

            // Strava blue split bars (#2B78E4) & pace chart fill: strong blue dominance (b - r > 28)
            // Strava orange accent fills: strong red dominance (r - b > 45 && g < 160)
            val isColoredBarOrChart = (b - r) > 28 || ((r - b) > 45 && g < 160)
            val lum = 0.299 * r + 0.587 * g + 0.114 * b

            val out = if (isColoredBarOrChart) {
                255
            } else if (isDarkMode) {
                when {
                    lum > 155.0 -> 0
                    lum > 95.0 -> (255.0 - ((lum - 95.0) / 60.0) * 255.0).toInt().coerceIn(0, 255)
                    else -> 255
                }
            } else {
                when {
                    lum < 110.0 -> 0
                    lum < 175.0 -> (((lum - 110.0) / 65.0) * 255.0).toInt().coerceIn(0, 255)
                    else -> 255
                }
            }
            pixels[idx] = Color.argb(255, out, out, out)
        }

        val outBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        outBitmap.setPixels(pixels, 0, w, 0, 0, w, h)
        if (scaled !== source && !scaled.isRecycled) {
            scaled.recycle()
        }
        return outBitmap
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

    private data class WordBox(val text: String, val rect: Rect)

    /**
     * Reconstructs horizontal text rows by vertical center proximity using element-level bounding boxes
     * so split indices, split paces, and bar artifacts do not scramble or merge across adjacent rows.
     */
    fun reconstructTextByRows(visionText: Text): String {
        val items = mutableListOf<WordBox>()
        val artifactOnlyRegex = Regex("""^[|—–_~=\[\]{}<>]+$""")

        for (block in visionText.textBlocks) {
            for (line in block.lines) {
                val elements = line.elements
                if (elements.isNotEmpty()) {
                    for (el in elements) {
                        val t = el.text.trim()
                        val box = el.boundingBox
                        if (box != null && t.isNotEmpty() && !(t.length <= 3 && artifactOnlyRegex.matches(t))) {
                            items.add(WordBox(t, box))
                        }
                    }
                } else {
                    val t = line.text.trim()
                    val box = line.boundingBox
                    if (box != null && t.isNotEmpty()) {
                        items.add(WordBox(t, box))
                    }
                }
            }
        }

        if (items.isEmpty()) return visionText.text

        val rows = mutableListOf<MutableList<WordBox>>()
        for (item in items) {
            var bestRow: MutableList<WordBox>? = null
            var bestDist = Double.MAX_VALUE
            for (row in rows) {
                val rowAvgCenterY = row.map { it.rect.centerY() }.average()
                val rowAvgHeight = row.map { it.rect.height() }.average().coerceAtLeast(14.0)
                val dy = abs(item.rect.centerY() - rowAvgCenterY)
                if (dy < rowAvgHeight * 0.55 && dy < bestDist) {
                    bestDist = dy
                    bestRow = row
                }
            }
            if (bestRow != null) {
                bestRow.add(item)
            } else {
                rows.add(mutableListOf(item))
            }
        }

        rows.sortBy { row -> row.minOfOrNull { it.rect.top } ?: 0 }

        return rows.joinToString("\n") { row ->
            row.sortedBy { it.rect.left }.joinToString(" ") { it.text }
        }
    }

    private data class ParsedSplitRow(
        var label: String,
        var distKm: Double,
        val paceSec: Long
    )

    fun parseStravaText(text: String): ExtractedStravaMetrics? {
        val normalizedText = text.replace('\u2014', '-').replace('\u2013', '-')
        val lines = normalizedText.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (lines.isEmpty()) return null

        // ==========================================
        // A. Average Pace (e.g. "6:59 /km")
        // ==========================================
        var avgPaceSec = 0L

        val avgPaceRegex = Pattern.compile(
            "(?:Avg\\s*Pace|Average\\s*Pace|Kec(?:epatan)?\\s*Rata[- ]?Rata|Pace\\s*Rata[- ]?Rata)[^\\d\\n]{0,12}([0-9]{1,2}[:.;][0-5][0-9])(?:\\s*/(?:km|mi))?",
            Pattern.CASE_INSENSITIVE
        )
        val avgPaceMatcher = avgPaceRegex.matcher(normalizedText)
        if (avgPaceMatcher.find()) {
            val candidate = parseTimeStringToSeconds(avgPaceMatcher.group(1).orEmpty())
            if (candidate in 120L..900L) avgPaceSec = candidate
        }

        if (avgPaceSec <= 0L) {
            val paceP = Pattern.compile("([0-9]{1,2}[:.;][0-5][0-9])")
            for (i in lines.indices) {
                val line = lines[i]
                if (line.contains("Avg Pace", ignoreCase = true) ||
                    line.contains("Average Pace", ignoreCase = true) ||
                    line.contains("Kecepatan", ignoreCase = true)
                ) {
                    if (i + 1 < lines.size) {
                        val mNext = paceP.matcher(lines[i + 1])
                        if (mNext.find()) {
                            val s = parseTimeStringToSeconds(mNext.group(1).orEmpty())
                            if (s in 120L..900L) {
                                avgPaceSec = s
                                break
                            }
                        }
                    }
                }
            }
        }

        if (avgPaceSec <= 0L) {
            val paceWithUnitRegex = Pattern.compile("\\b([0-9]{1,2}[:.;][0-5][0-9])\\s*/(?:km|mi)\\b", Pattern.CASE_INSENSITIVE)
            val paceUnitMatcher = paceWithUnitRegex.matcher(normalizedText)
            if (paceUnitMatcher.find()) {
                val candidate = parseTimeStringToSeconds(paceUnitMatcher.group(1).orEmpty())
                if (candidate in 120L..900L) avgPaceSec = candidate
            }
        }

        // ==========================================
        // B. Moving / Elapsed Time (prefer Moving Time)
        // ==========================================
        var movingTimeSec = 0L

        val movingTimeDirect = Regex(
            """(?:Moving\s*Time|Waktu\s*Bergerak)[^\d\n]*(\d{1,2}[:.;][0-5]\d(?:[:.;][0-5]\d)?)""",
            RegexOption.IGNORE_CASE
        ).find(normalizedText)
        if (movingTimeDirect != null) {
            val candidate = parseTimeStringToSeconds(movingTimeDirect.groupValues[1])
            if (candidate >= 60L && candidate != avgPaceSec) movingTimeSec = candidate
        }

        if (movingTimeSec <= 0L) {
            val elapsedOrTotalRegex = Pattern.compile(
                "(?:Elapsed\\s*Time|Total\\s*Time|Duration|Waktu\\s*Berlalu|Waktu\\s*Tempuh|Waktu\\s*Total|Waktu|\\bTime\\b)[^\\d\\n]{0,12}([0-9]{1,2}[:.;][0-5][0-9](?:[:.;][0-5][0-9])?)",
                Pattern.CASE_INSENSITIVE
            )
            val matcher = elapsedOrTotalRegex.matcher(normalizedText)
            while (matcher.find()) {
                val candidate = parseTimeStringToSeconds(matcher.group(1).orEmpty())
                if (candidate >= 60L && candidate != avgPaceSec) {
                    movingTimeSec = candidate
                    break
                }
            }
        }

        if (movingTimeSec <= 0L) {
            val timeP = Pattern.compile("(?<!/)\\b([0-9]{1,2}[:.;][0-5][0-9](?:[:.;][0-5][0-9])?)\\b(?!/)")
            for (i in lines.indices) {
                val line = lines[i]
                if (line.contains("Moving Time", ignoreCase = true) ||
                    line.contains("Elapsed Time", ignoreCase = true) ||
                    line.contains("Total Time", ignoreCase = true) ||
                    line.contains("Duration", ignoreCase = true) ||
                    line.contains("Waktu", ignoreCase = true) ||
                    line.equals("Time", ignoreCase = true)
                ) {
                    if (i + 1 < lines.size) {
                        val mNext = timeP.matcher(lines[i + 1])
                        if (mNext.find()) {
                            val s = parseTimeStringToSeconds(mNext.group(1).orEmpty())
                            if (s >= 60L && s != avgPaceSec) {
                                movingTimeSec = s
                                break
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // C. Focused Per-Kilometer Splits Extraction
        // ==========================================
        val splitRows = mutableListOf<ParsedSplitRow>()

        // Layer 1: Pre-formatted "KM 1: 7:54/km" tokens if present
        val explicitKmRegex = Regex(
            """KM[ \t]*(0[.,]\d+|[1-9]\d*(?:[.,]\d+)?)[ \t:.-]+(\d{1,2}[:.;][0-5]\d)""",
            RegexOption.IGNORE_CASE
        )
        val explicitMatches = explicitKmRegex.findAll(normalizedText).toList()
        if (explicitMatches.size >= 2) {
            for (m in explicitMatches) {
                val rawLabel = m.groupValues[1].replace(',', '.')
                val distVal = rawLabel.toDoubleOrNull() ?: continue
                val paceSec = parseTimeStringToSeconds(m.groupValues[2])
                if (distVal in 0.05..99.0 && paceSec in 120L..900L) {
                    val isFrac = distVal < 1.0
                    splitRows.add(
                        ParsedSplitRow(
                            label = if (isFrac) rawLabel else distVal.toInt().toString(),
                            distKm = if (isFrac) distVal else 1.0,
                            paceSec = paceSec
                        )
                    )
                }
            }
        }

        // Identify the Splits Table boundary above the Pace chart section (excludes 6:00..12:00 chart ticks)
        val paceHeaderIdx = lines.indexOfFirst { l ->
            lines.indexOf(l) > 0 &&
                Regex("""^(?:Pace|Grafik\s*Pace)\b""", RegexOption.IGNORE_CASE).containsMatchIn(l) &&
                !l.contains("Avg", ignoreCase = true)
        }
        val avgPaceLineIdx = lines.indexOfFirst { l ->
            l.contains("Avg Pace", ignoreCase = true) ||
                l.contains("Moving Time", ignoreCase = true) ||
                l.contains("Elapsed Time", ignoreCase = true)
        }
        val splitZoneEnd = when {
            paceHeaderIdx > 0 -> paceHeaderIdx
            avgPaceLineIdx > 0 -> avgPaceLineIdx
            else -> lines.size
        }
        val candidateSplitLines = lines.subList(0, splitZoneEnd)

        // Layer 2: Line-by-line Splits Table parsing (uses [ \t], never matches across '\n')
        if (splitRows.size < 2) {
            splitRows.clear()

            val headerSkipRegex = Regex("""^(?:Run|Splits?|Km|Pace|Elev(?:ation)?|Avg|Moving|Elapsed|Fastest)\b""", RegexOption.IGNORE_CASE)
            val statusClockRegex = Regex("""^\s*(?:1[3-9]|2[0-3])[:.;][0-5]\d\b""")
            val pairOnLineRegex = Regex(
                """(?:^|[ \t])(?:KM[ \t]*)?([0-9]+(?:[.,][0-9]+)?|[.,][0-9]+)[ \t:.\-]+([2-9]|1[0-4])[:.;]([0-5]\d)(?![0-9])""",
                RegexOption.IGNORE_CASE
            )
            val barePaceRegex = Regex("""(?:^|[ \t])([3-9]|1[0-4])[:.;]([0-5]\d)(?:[ \t]+[-+]?\d+)?\s*$""")
            val bareChartTickRegex = Regex("""^\s*\d{1,2}:00\s*$""")

            for (idx in candidateSplitLines.indices) {
                val rawLine = candidateSplitLines[idx]
                if (headerSkipRegex.containsMatchIn(rawLine)) continue
                if (isChartAxisLine(rawLine)) continue
                if (idx == 0 && statusClockRegex.containsMatchIn(rawLine) && !Regex("""[ \t][-+]?\d+\s*$""").containsMatchIn(rawLine)) {
                    continue
                }

                // Normalize common leading OCR character misreads before pace (e.g. I/l -> 1, A -> 4, S -> 5, O.9 -> 0.9)
                val cleanedLine = rawLine
                    .replace(Regex("""^\s*[Il|!](?=[ \t]+\d{1,2}[:.;][0-5]\d)"""), "1")
                    .replace(Regex("""^\s*A(?=[ \t]+\d{1,2}[:.;][0-5]\d)"""), "4")
                    .replace(Regex("""^\s*S(?=[ \t]+\d{1,2}[:.;][0-5]\d)"""), "5")
                    .replace(Regex("""^\s*[Oo]([.,]\d+)"""), "0$1")

                val pairs = pairOnLineRegex.findAll(cleanedLine).toList()
                if (pairs.isNotEmpty()) {
                    for (pm in pairs) {
                        var rawLabel = pm.groupValues[1].replace(',', '.')
                        if (rawLabel.startsWith(".")) {
                            rawLabel = "0$rawLabel"
                        } else if (Regex("""^0[1-9]$""").matches(rawLabel)) {
                            rawLabel = "0.${rawLabel.substring(1)}"
                        }
                        val paceSec = pm.groupValues[2].toLong() * 60L + pm.groupValues[3].toLong()
                        val numVal = rawLabel.toDoubleOrNull() ?: continue
                        if (numVal > 0.0 && numVal <= 99.0 && paceSec in 120L..900L) {
                            val isFrac = numVal < 1.0 || rawLabel.contains('.')
                            splitRows.add(
                                ParsedSplitRow(
                                    label = if (isFrac) String.format(Locale.US, "%.1f", numVal) else numVal.toInt().toString(),
                                    distKm = if (isFrac) numVal else 1.0,
                                    paceSec = paceSec
                                )
                            )
                        }
                    }
                    continue
                }

                // Inside the top Splits Table zone, if leading split index was missed (e.g. "6:45 0"), capture pace
                if ((paceHeaderIdx > 0 || avgPaceLineIdx > 0) &&
                    !cleanedLine.contains("/km", ignoreCase = true) &&
                    !cleanedLine.contains("/mi", ignoreCase = true) &&
                    !bareChartTickRegex.matches(cleanedLine)
                ) {
                    val bareMatch = barePaceRegex.find(cleanedLine)
                    if (bareMatch != null) {
                        val paceSec = bareMatch.groupValues[1].toLong() * 60L + bareMatch.groupValues[2].toLong()
                        if (paceSec in 150L..900L) {
                            splitRows.add(
                                ParsedSplitRow(
                                    label = (splitRows.size + 1).toString(),
                                    distKm = 1.0,
                                    paceSec = paceSec
                                )
                            )
                        }
                    }
                }
            }
        }

        // Layer 3: Column-separated ML Kit blocks ("1\n2\n3\n4\n0.9" and "7:54\n6:58\n6:35\n6:45\n6:45")
        if (splitRows.size < 2) {
            splitRows.clear()
            val labelTokens = mutableListOf<Double>()
            val paceTokens = mutableListOf<Long>()
            val standaloneLabelRegex = Regex("""^(0[.,]\d+|[.,]\d+|0[1-9]|[1-9]\d?)$""")
            val standalonePaceRegex = Regex("""^([2-9]|1[0-4])[:.;]([0-5]\d)$""")
            val bareTickRegex = Regex("""^\d{1,2}:00$""")

            for (idx in candidateSplitLines.indices) {
                val token = candidateSplitLines[idx].trim()
                if (idx == 0 && Regex("""^(?:1[3-9]|2[0-3])[:.;][0-5]\d$""").matches(token)) continue
                when {
                    standaloneLabelRegex.matches(token) -> {
                        val fixed = when {
                            token.startsWith(".") -> "0${token.replace(',', '.')}"
                            Regex("""^0[1-9]$""").matches(token) -> "0.${token.substring(1)}"
                            else -> token.replace(',', '.')
                        }
                        fixed.toDoubleOrNull()?.let { if (it in 0.1..99.0) labelTokens.add(it) }
                    }
                    standalonePaceRegex.matches(token) && !bareTickRegex.matches(token) -> {
                        val sec = parseTimeStringToSeconds(token)
                        if (sec in 120L..900L) paceTokens.add(sec)
                    }
                }
            }

            if (paceTokens.size >= 2 && labelTokens.size == paceTokens.size) {
                for (i in paceTokens.indices) {
                    val v = labelTokens[i]
                    val isFrac = v < 1.0
                    splitRows.add(
                        ParsedSplitRow(
                            label = if (isFrac) String.format(Locale.US, "%.1f", v) else v.toInt().toString(),
                            distKm = if (isFrac) v else 1.0,
                            paceSec = paceTokens[i]
                        )
                    )
                }
            } else if (paceTokens.size >= 2 && labelTokens.isEmpty()) {
                for (i in paceTokens.indices) {
                    splitRows.add(ParsedSplitRow(label = (i + 1).toString(), distKm = 1.0, paceSec = paceTokens[i]))
                }
            }
        }

        // Layer 4: Finalize Average Pace / Moving Time defaults before fractional reconciliation
        if (avgPaceSec <= 0L && splitRows.isNotEmpty()) {
            val totalWeightedSec = splitRows.sumOf { it.distKm * it.paceSec }
            val totalDist = splitRows.sumOf { it.distKm }.coerceAtLeast(0.1)
            avgPaceSec = (totalWeightedSec / totalDist).roundToLong()
        }

        if (movingTimeSec <= 0L) {
            val standaloneTimeRegex = Pattern.compile("\\b([1-9][0-9]{1,2}[:.;][0-5][0-9](?:[:.;][0-5][0-9])?)\\b(?!\\s*/(?:km|mi))")
            val standaloneMatcher = standaloneTimeRegex.matcher(normalizedText)
            val splitPaceSet = splitRows.map { it.paceSec }.toSet()
            while (standaloneMatcher.find()) {
                val candidate = parseTimeStringToSeconds(standaloneMatcher.group(1).orEmpty())
                if (candidate >= 180L && candidate !in splitPaceSet && candidate != avgPaceSec) {
                    movingTimeSec = candidate
                    break
                }
            }
        }

        // ==========================================
        // D. Reconcile Dropped Splits & Fractional Final KM
        // ==========================================
        if (splitRows.size >= 2) {
            for (i in 0 until splitRows.size - 1) {
                splitRows[i].label = (i + 1).toString()
                splitRows[i].distKm = 1.0
            }

            val lastIdx = splitRows.size - 1
            val lastRow = splitRows[lastIdx]
            val sumPrevSec = splitRows.take(lastIdx).sumOf { it.paceSec }

            // Recover 1 dropped full-km split right before a fractional last split (e.g. 1,2,3,_,0.9 with 34:16 @ 6:59)
            if (lastRow.distKm in 0.05..0.95 && movingTimeSec > 0L && avgPaceSec > 0L) {
                val expectedTotalKm = movingTimeSec.toDouble() / avgPaceSec.toDouble()
                val currentTotalKm = (splitRows.size - 1) + lastRow.distKm
                if (abs(expectedTotalKm - (currentTotalKm + 1.0)) <= 0.25) {
                    val missingSplitSec = (movingTimeSec.toDouble() - (sumPrevSec.toDouble() + lastRow.distKm * lastRow.paceSec)).roundToLong()
                    if (missingSplitSec in 150L..900L) {
                        splitRows.add(
                            lastIdx,
                            ParsedSplitRow(
                                label = (lastIdx + 1).toString(),
                                distKm = 1.0,
                                paceSec = missingSplitSec
                            )
                        )
                    }
                }
            }

            val updatedLastIdx = splitRows.size - 1
            val updatedLastRow = splitRows[updatedLastIdx]
            val updatedSumPrevSec = splitRows.take(updatedLastIdx).sumOf { it.paceSec }

            if (movingTimeSec > updatedSumPrevSec && updatedLastRow.paceSec > 0L) {
                val remSec = (movingTimeSec - updatedSumPrevSec).toDouble()
                val inferredLastDist = remSec / updatedLastRow.paceSec.toDouble()
                if (inferredLastDist in 0.08..0.95) {
                    val roundedFrac = (inferredLastDist * 10.0).roundToLong() / 10.0
                    updatedLastRow.distKm = roundedFrac
                    updatedLastRow.label = String.format(Locale.US, "%.1f", roundedFrac)
                } else if (updatedLastRow.distKm < 1.0) {
                    val roundedFrac = (updatedLastRow.distKm * 10.0).roundToLong() / 10.0
                    updatedLastRow.distKm = roundedFrac
                    updatedLastRow.label = String.format(Locale.US, "%.1f", roundedFrac)
                } else {
                    updatedLastRow.label = splitRows.size.toString()
                    updatedLastRow.distKm = 1.0
                }
            } else if (updatedLastRow.distKm < 1.0) {
                val roundedFrac = (updatedLastRow.distKm * 10.0).roundToLong() / 10.0
                updatedLastRow.distKm = roundedFrac
                updatedLastRow.label = String.format(Locale.US, "%.1f", roundedFrac)
            } else {
                val rawLastNum = updatedLastRow.label.toDoubleOrNull() ?: splitRows.size.toDouble()
                if (rawLastNum > splitRows.size && rawLastNum <= 9.0) {
                    val frac = rawLastNum / 10.0
                    updatedLastRow.distKm = frac
                    updatedLastRow.label = String.format(Locale.US, "%.1f", frac)
                } else {
                    updatedLastRow.label = splitRows.size.toString()
                    updatedLastRow.distKm = 1.0
                }
            }
        }

        if (movingTimeSec <= 0L && splitRows.isNotEmpty()) {
            movingTimeSec = splitRows.sumOf { (it.distKm * it.paceSec).roundToLong() }
        }

        val splitsFound = splitRows.map { "KM ${it.label}: ${formatSecondsToClock(it.paceSec)}/km" }
        val splitsPaces = splitRows.map { it.paceSec }

        // ==========================================
        // E. Total Distance (Explicit Label -> Reconciled Splits Sum / MovingTime÷AvgPace)
        // ==========================================
        var distanceKm = 0.0

        val hasExplicitDistanceLabel = lines.any { line ->
            val clean = line.trim()
            (clean.contains("Distance", ignoreCase = true) || clean.contains("Jarak", ignoreCase = true)) &&
                !clean.contains("Distance / Pace", ignoreCase = true) &&
                !clean.contains("Split", ignoreCase = true)
        }

        if (hasExplicitDistanceLabel) {
            val numWithKmRegex = Pattern.compile("(?<!/)\\b([0-9]+[.,][0-9]+)\\s*(?:km)?\\b(?!/)", Pattern.CASE_INSENSITIVE)
            for (i in lines.indices) {
                val line = lines[i]
                if (line.contains("Distance", ignoreCase = true) || line.contains("Jarak", ignoreCase = true)) {
                    if (i + 1 < lines.size && !isChartAxisLine(lines[i + 1])) {
                        val mNext = numWithKmRegex.matcher(lines[i + 1])
                        if (mNext.find()) {
                            val candidate = mNext.group(1)?.replace(",", ".")?.toDoubleOrNull() ?: 0.0
                            if (candidate in 0.1..500.0) {
                                distanceKm = candidate
                                break
                            }
                        }
                    }
                    val mSame = numWithKmRegex.matcher(line)
                    while (mSame.find()) {
                        val candidate = mSame.group(1)?.replace(",", ".")?.toDoubleOrNull() ?: 0.0
                        if (candidate in 0.1..500.0 && !isChartAxisLine(line)) {
                            distanceKm = candidate
                            break
                        }
                    }
                    if (distanceKm > 0.0) break
                }
            }
        }

        if (distanceKm <= 0.0 && hasExplicitDistanceLabel) {
            for (line in lines) {
                if (isChartAxisLine(line)) continue
                val kmRegex = Pattern.compile("(?<!/)\\b([0-9]+[.,][0-9]+)\\s*km\\b(?!/)", Pattern.CASE_INSENSITIVE)
                val kmMatcher = kmRegex.matcher(line)
                if (kmMatcher.find()) {
                    val candidate = kmMatcher.group(1)?.replace(",", ".")?.toDoubleOrNull() ?: 0.0
                    val minExpected = if (splitRows.size >= 2) (splitRows.sumOf { it.distKm } - 0.5) else 0.5
                    if (candidate >= minExpected) {
                        distanceKm = candidate
                        break
                    }
                }
            }
        }

        val splitsTotalKm = if (splitRows.isNotEmpty()) {
            (splitRows.sumOf { it.distKm } * 100.0).roundToLong() / 100.0
        } else 0.0

        val paceDerivedKm = if (movingTimeSec > 0L && avgPaceSec > 0L) {
            ((movingTimeSec.toDouble() / avgPaceSec.toDouble()) * 100.0).roundToLong() / 100.0
        } else 0.0

        val minExpectedSplits = if (splitRows.size >= 2) (splitRows.sumOf { it.distKm } - 0.5) else 0.5
        val isDistanceSuspiciouslySmall = distanceKm < minExpectedSplits && (splitRows.size >= 2 || movingTimeSec >= 300L)
        if (!hasExplicitDistanceLabel || distanceKm <= 0.0 || isDistanceSuspiciouslySmall) {
            distanceKm = when {
                splitsTotalKm > 0.0 && paceDerivedKm > 0.0 ->
                    if (abs(splitsTotalKm - paceDerivedKm) <= 0.15) splitsTotalKm else paceDerivedKm
                paceDerivedKm > 0.0 -> paceDerivedKm
                splitsTotalKm > 0.0 -> splitsTotalKm
                else -> 0.0
            }
        }

        // ==========================================
        // F. Elevation, Heart Rate, Cadence & Steps
        // ==========================================
        var elevation = 0L
        val elevRegex = Pattern.compile("(?:Elevation Gain|Elev Gain|Gain|Elevasi|Ketinggian)\\s*[:]?\\s*\\+?([0-9]+)\\s*m\\b", Pattern.CASE_INSENSITIVE)
        val elevMatcher = elevRegex.matcher(normalizedText)
        if (elevMatcher.find()) {
            elevation = elevMatcher.group(1)?.toLongOrNull() ?: 0L
        } else {
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
        val hrMatcher = Pattern.compile("([0-9]{2,3})\\s*(?:bpm|BPM)").matcher(normalizedText)
        if (hrMatcher.find()) hr = hrMatcher.group(1)?.toLongOrNull() ?: 0L

        var cadence = 0L
        val cadMatcher = Pattern.compile("([0-9]{2,3})\\s*(?:spm|SPM)").matcher(normalizedText)
        if (cadMatcher.find()) cadence = cadMatcher.group(1)?.toLongOrNull() ?: 0L

        var steps = 0L
        val stepsMatcher = Pattern.compile("(?:Steps|Langkah)\\s*[:]?\\s*([0-9,.]+)", Pattern.CASE_INSENSITIVE).matcher(normalizedText)
        if (stepsMatcher.find()) {
            steps = stepsMatcher.group(1)?.replace(",", "")?.replace(".", "")?.toLongOrNull() ?: 0L
        } else {
            for (i in lines.indices) {
                val l = lines[i]
                if (l.contains("Steps", ignoreCase = true) || l.contains("Langkah", ignoreCase = true)) {
                    if (i + 1 < lines.size) {
                        val sm = Pattern.compile("([0-9,.]+)").matcher(lines[i + 1])
                        if (sm.find()) {
                            steps = sm.group(1)?.replace(",", "")?.replace(".", "")?.toLongOrNull() ?: 0L
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
        // G. Title Detection
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
                !clean.startsWith("KM ", ignoreCase = true) &&
                clean.length in 4..35
        } ?: "Strava Run"

        if (distanceKm <= 0.0 && movingTimeSec <= 0L && avgPaceSec <= 0L && splitsFound.isEmpty()) {
            return null
        }

        // ==========================================
        // H. Splits Summary & Coaching Insight
        // ==========================================
        val splitsSummary = if (splitsFound.isNotEmpty()) splitsFound.joinToString(" | ") else ""

        val paceMin = if (avgPaceSec > 0) avgPaceSec / 60 else 0
        val paceSecRem = if (avgPaceSec > 0) avgPaceSec % 60 else 0
        val timeMin = movingTimeSec / 60
        val timeSecRem = movingTimeSec % 60

        val coachingInsight = if (splitsFound.size >= 2) {
            val firstSplit = splitsFound.first()
            val lastSplit = splitsFound.last()
            val paceDiff = splitsPaces.last() - splitsPaces.first()
            val pacingTrend = when {
                paceDiff < -5 -> "Pacing Anda menunjukkan negative split yang sangat baik (km akhir lebih cepat)."
                paceDiff > 15 -> "Pacing cenderung menurun di kilometer akhir. Disarankan mulai lebih santai agar stamina merata."
                else -> "Distribusi pacing sangat stabil dan konsisten sepanjang sesi lari."
            }
            "Analisis Performa ($titleCandidate):\n" +
                "1. Pacing & Ritme: $pacingTrend Rentang split tercatat dari $firstSplit hingga $lastSplit (rata-rata $paceMin:%02d/km).\n" +
                "2. Fisiologi & Mekanika: Waktu tempuh total ${timeMin}m ${timeSecRem}s.${if (cadence > 0) " Cadence rata-rata $cadence spm mendukung turnover langkah efisien." else ""}${if (hr > 0) " Denyut jantung rata-rata $hr bpm." else ""}\n" +
                "3. Rekomendasi Latihan Selanjutnya: Lakukan recovery run ringan 20-30 menit atau sesi peregangan hamstring dan betis sebelum sesi tempo/interval berikutnya."
                    .format(paceSecRem)
        } else if (distanceKm > 0.0 && movingTimeSec > 0L) {
            "Analisis Performa ($titleCandidate):\n" +
                "1. Capaian Sesi: Jarak tempuh ${String.format(Locale.US, "%.2f", distanceKm)} km selesai dalam ${timeMin}m ${timeSecRem}s dengan pace rata-rata $paceMin:%02d/km.\n" +
                "2. Adaptasi Aerobik: Latihan ini berhasil memicu adaptasi kapasitas aerobik (VDOT).${if (elevation > 0) " Kenaikan elevasi +$elevation m melatih ketahanan otot gluteus & quad." else ""}\n" +
                "3. Rekomendasi Latihan Selanjutnya: Jaga hidrasi serta tambahkan sesi interval 4x400m di hari lari berikutnya untuk melatih ambang batas laktat."
                    .format(paceSecRem)
        } else {
            "Analisis AI: Aktivitas lari berhasil terekam. Pertahankan konsistensi latihan mingguan dan kombinasikan dengan variasi easy run serta interval untuk peningkatan performa yang optimal."
        }

        val (detectedDateEpochMs, detectedDateStr) = parseWorkoutDateFromText(normalizedText)

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

    private fun formatSecondsToClock(seconds: Long): String {
        val safe = seconds.coerceAtLeast(0L)
        val mins = safe / 60L
        val secs = safe % 60L
        return String.format(Locale.US, "%d:%02d", mins, secs)
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
        val clean = timeStr.trim().replace(".", ":").replace(";", ":")
        val parts = clean.split(":").mapNotNull { it.trim().toLongOrNull() }
        return when (parts.size) {
            2 -> parts[0] * 60L + parts[1]
            3 -> parts[0] * 3600L + parts[1] * 60L + parts[2]
            else -> 0L
        }
    }
}
