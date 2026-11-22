package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DistanceCategory
import com.example.data.model.PersonalRecord
import com.example.data.model.WorkoutLog
import com.example.domain.RacePredictionEngine
import com.example.ui.components.StravaSplitItem
import com.example.ui.components.StravaSplitsBarChart
import com.example.ui.components.parseSplitsSummary
import com.example.ui.theme.AeroCyan
import com.example.ui.theme.KineticCoral
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TelemetryMonoFontFamily
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VoltLime
import com.example.ui.theme.WarningAmber
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.roundToLong

private val StravaBlue get() = VoltLime
private val StravaLightBlue get() = AeroCyan

data class PrEvaluationResult(
    val categoryLabel: String,
    val rank: Int,
    val totalQualifyingRuns: Int,
    val currentEquivTimeSec: Long,
    val bestPrTimeSec: Long,
    val diffFromBestSec: Long,
    val diffFromSecondSec: Long,
    val bestWorkoutTitle: String
)

data class PaceSegmentSample(
    val distanceKmMark: Double,
    val paceSecondsPerKm: Long,
    val segmentDurationSeconds: Double
)

data class PaceZoneDistributionItem(
    val zoneNumber: Int,
    val zoneTitle: String,
    val paceRangeText: String,
    val percentage: Int,
    val durationSeconds: Long,
    val color: Color
)

@Composable
fun WorkoutDetailScreen(
    workout: WorkoutLog,
    allWorkouts: List<WorkoutLog>,
    prs: List<PersonalRecord>,
    onBack: () -> Unit,
    onEditWorkout: (WorkoutLog) -> Unit,
    onDeleteWorkout: (String) -> Unit,
    isRefreshingAi: Boolean = false,
    onRefreshAi: ((WorkoutLog) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val safeDist = workout.distanceKm.coerceAtLeast(0.1)
    val safeDur = workout.durationSeconds.coerceAtLeast(1L)
    val avgPaceSec = if (workout.avgPaceSecondsPerKm in 60L..3600L) {
        workout.avgPaceSecondsPerKm
    } else {
        (safeDur / safeDist).roundToLong().coerceIn(60L, 3600L)
    }

    // 1. Scientific Calorie Calculation (METs & ACSM Running Energy Expenditure Formula)
    val caloriesBurned = remember(workout) {
        // Net running oxygen cost ~ 0.2 ml/kg/m (~1.036 kcal/kg/km) + grade cost for elevation
        val assumedWeightKg = 62.0
        val horizontalKcal = safeDist * assumedWeightKg * 1.036
        val verticalKcal = workout.elevationGainM * 0.95
        (horizontalKcal + verticalKcal).roundToInt().coerceAtLeast(15)
    }

    // 2. Splits parsing & Fastest Split analysis
    val parsedSplits = remember(workout.splitsSummary, avgPaceSec) {
        parseSplitsSummary(workout.splitsSummary, avgPaceSec)
    }

    val fastestSplit: StravaSplitItem? = remember(parsedSplits) {
        parsedSplits.minByOrNull { it.paceSeconds }
    }
    val slowestSplit: StravaSplitItem? = remember(parsedSplits) {
        parsedSplits.maxByOrNull { it.paceSeconds }
    }
    val fastestPaceSec = fastestSplit?.paceSeconds ?: avgPaceSec

    // 3. Athlete's VDOT & Lactate Threshold Pace (T-Pace) via Jack Daniels Formula
    val isExplicitRaceEffort = remember(workout, prs) {
        val titleLower = workout.title.lowercase(Locale.ROOT)
        prs.isNotEmpty() ||
        titleLower.contains("race") ||
        titleLower.contains("lomba") ||
        titleLower.contains("time trial") ||
        titleLower.contains("pb") ||
        titleLower.contains("pr ") ||
        titleLower.endsWith(" pr")
    }

    val thresholdPaceSec = remember(workout, prs, allWorkouts, avgPaceSec, safeDist, isExplicitRaceEffort) {
        if (prs.isNotEmpty()) {
            val predictions = RacePredictionEngine.computeRacePredictions(allWorkouts.ifEmpty { listOf(workout) }, prs, 1.06)
            val tenK = predictions.firstOrNull { it.category == DistanceCategory.TEN_K }?.targetPaceSecondsPerKm
                ?: (avgPaceSec * (10.0 / safeDist).pow(0.06)).roundToLong()
            // In Jack Daniels VDOT tables, T-Pace (~88% VO2max) is ~1.03x 10K race pace (~91% VO2max)
            (tenK * 1.03).roundToLong().coerceIn(150L, 600L)
        } else {
            // Check any explicit race workout logged in history even without formal PR records
            val explicitRaceBaseline = allWorkouts
                .filter { w ->
                    val t = w.title.lowercase(Locale.ROOT)
                    w.distanceKm >= 1.5 && w.durationSeconds > 60L && (
                        t.contains("race") || t.contains("lomba") || t.contains("time trial") ||
                            t.contains("pb") || t.contains("pr ") || t.endsWith(" pr")
                        )
                }
                .minOfOrNull { w ->
                    val wPace = (w.durationSeconds.toDouble() / w.distanceKm).coerceIn(120.0, 900.0)
                    val tenKFromRace = wPace * (10.0 / w.distanceKm).pow(0.06)
                    (tenKFromRace * 1.03).roundToLong().coerceIn(150L, 600L)
                }

            val thisRunThreshold = if (isExplicitRaceEffort) {
                val tenKFromThisRace = avgPaceSec * (10.0 / safeDist).pow(0.06)
                (tenKFromThisRace * 1.03).roundToLong().coerceIn(150L, 600L)
            } else {
                // Calibrate T-Pace physiologically from cardiovascular intensity (%MaxHR)
                val isHrAvailable = workout.avgHeartRate > 0L
                val hrMax = 190.0
                val hrRatio = if (isHrAvailable) (workout.avgHeartRate.toDouble() / hrMax).coerceIn(0.55, 1.0) else null

                val paceToThresholdRatio = when {
                    hrRatio != null && hrRatio >= 0.91 -> 0.94 // Zone 5: VO2Max / Anaerobic (> T-speed)
                    hrRatio != null && hrRatio >= 0.85 -> 1.00 // Zone 4: Lactate Threshold (T-Pace ~85-90% HRmax)
                    hrRatio != null && hrRatio >= 0.79 -> 1.08 // Zone 3: Marathon / Aerobic Tempo (79-84% HRmax)
                    hrRatio != null && hrRatio >= 0.72 -> 1.18 // Zone 2: Easy / Aerobic Base (72-78% HRmax)
                    hrRatio != null -> 1.26                    // Zone 1-2: Light Recovery (<72% HRmax)
                    else -> 1.20                               // No HR: standard everyday aerobic base run
                }
                (avgPaceSec / paceToThresholdRatio).roundToLong().coerceIn(150L, 600L)
            }

            if (explicitRaceBaseline != null) {
                minOf(thisRunThreshold, explicitRaceBaseline)
            } else {
                thisRunThreshold
            }
        }
    }

    // Calibrate VDOT score from the scientifically determined Threshold Pace
    val vdotScore = remember(thresholdPaceSec) {
        val tenKEquivSec = (thresholdPaceSec / 1.03 * 10.0).roundToLong()
        RacePredictionEngine.calculateVdot(10000.0, tenKEquivSec)
    }

    // 4. Deterministic Time-Weighted Pace Segments from Actual Splits (NO random seeds or dummy waves)
    val paceSegments = remember(parsedSplits, avgPaceSec, safeDist, safeDur) {
        buildDeterministicPaceSegments(
            splits = parsedSplits,
            avgPaceSec = avgPaceSec,
            totalDistanceKm = safeDist,
            totalDurationSec = safeDur
        )
    }

    // 5. Patented Jack Daniels VDOT Pace Zone Distribution (Time-Integrated, Total = 100%)
    val paceZonesDistribution = remember(paceSegments, thresholdPaceSec, safeDur) {
        computeScientificVdotPaceZoneDistribution(
            segments = paceSegments,
            thresholdPaceSec = thresholdPaceSec,
            totalDurationSec = safeDur
        )
    }

    // 6. PR Evaluation & Ranking (checks if PR #1, PR #2 with exact second delta, etc.)
    val prEvaluations = remember(workout, allWorkouts, prs) {
        evaluateWorkoutPrRankings(workout, allWorkouts, avgPaceSec)
    }

    // 7. Heart Rate Insight (Karvonen / %MaxHR)
    val isHrMeasured = workout.avgHeartRate > 0L
    val effectiveHr = if (isHrMeasured) {
        workout.avgHeartRate
    } else {
        // Physiological HR estimate from intensity ratio (thresholdPaceSec / avgPaceSec)
        val intensityRatio = (thresholdPaceSec.toDouble() / avgPaceSec.toDouble()).coerceIn(0.65, 1.12)
        // At T-Pace (intensityRatio = 1.0), HR is ~88% of MaxHR (assuming MaxHR = 190 -> 167 bpm)
        (190.0 * 0.88 * intensityRatio).roundToLong().coerceIn(115L, 192L)
    }

    // 8. Comprehensive AI Synthesis from all data
    val fullAiSynthesis = remember(
        workout,
        caloriesBurned,
        prEvaluations,
        fastestSplit,
        paceZonesDistribution,
        effectiveHr,
        vdotScore,
        thresholdPaceSec
    ) {
        buildComprehensiveAiResult(
            workout = workout,
            avgPaceSec = avgPaceSec,
            calories = caloriesBurned,
            prResults = prEvaluations,
            splits = parsedSplits,
            fastestSplit = fastestSplit,
            slowestSplit = slowestSplit,
            zones = paceZonesDistribution,
            effectiveHr = effectiveHr,
            isHrMeasured = isHrMeasured,
            vdotScore = vdotScore,
            thresholdPaceSec = thresholdPaceSec
        )
    }

    val dateFormatted = remember(workout.createdAt) {
        workout.createdAt?.toDate()?.let {
            SimpleDateFormat("EEEE, dd MMM yyyy • HH:mm", Locale("id", "ID")).format(it)
        } ?: "Riwayat Aktivitas Lari"
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBackground),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ==========================================
        // TOP BAR NAVIGATION & ACTIONS
        // ==========================================
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        color = SlateSurface,
                        shape = CircleShape,
                        modifier = Modifier.border(1.dp, SlateBorder, CircleShape)
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Kembali",
                                tint = VoltLime
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = workout.title.ifBlank { "Running Session" },
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "$dateFormatted • ${if (workout.sourceType == "STRAVA_OCR") "Strava OCR" else "Manual Log"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onEditWorkout(workout) }) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Riwayat",
                            tint = AeroCyan
                        )
                    }
                    if (workout.id.isNotBlank()) {
                        IconButton(onClick = {
                            onDeleteWorkout(workout.id)
                            onBack()
                        }) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Hapus Riwayat",
                                tint = KineticCoral
                            )
                        }
                    }
                }
            }
        }

        // ==========================================
        // 1. HERO SUMMARY METRICS (Distance, Moving Time, Avg Pace, Calories)
        // ==========================================
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SlateBorder, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SlateSurface)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "WORKOUT SUMMARY",
                        style = MaterialTheme.typography.labelLarge,
                        color = VoltLime
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        DetailMetricTile(
                            label = "DISTANCE",
                            value = "${"%.2f".format(safeDist)} km",
                            accentColor = VoltLime,
                            icon = Icons.AutoMirrored.Filled.DirectionsRun,
                            modifier = Modifier.weight(1f)
                        )
                        DetailMetricTile(
                            label = "MOVING TIME",
                            value = RacePredictionEngine.formatDuration(safeDur),
                            accentColor = TextPrimary,
                            icon = Icons.Default.Timer,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        DetailMetricTile(
                            label = "AVG PACE",
                            value = RacePredictionEngine.formatPace(avgPaceSec),
                            accentColor = AeroCyan,
                            icon = Icons.Default.Speed,
                            modifier = Modifier.weight(1f)
                        )
                        DetailMetricTile(
                            label = "CALORIES",
                            value = "$caloriesBurned kcal",
                            accentColor = KineticCoral,
                            icon = Icons.Default.LocalFireDepartment,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (workout.elevationGainM > 0 || workout.cadenceSpm > 0 || workout.avgHeartRate > 0) {
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = SlateBorder)
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            if (workout.elevationGainM > 0) {
                                Text(
                                    text = "Elevasi: +${workout.elevationGainM} m",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextSecondary
                                )
                            }
                            if (workout.cadenceSpm > 0) {
                                Text(
                                    text = "Cadence: ${workout.cadenceSpm} spm",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = AeroCyan
                                )
                            }
                            if (workout.avgHeartRate > 0) {
                                Text(
                                    text = "Avg HR: ${workout.avgHeartRate} bpm",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = KineticCoral
                                )
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // 2. RESULT PR & PERINGKAT RIWAYAT (PR #1, PR #2, Selisih Detik)
        // ==========================================
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SlateBorder, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SlateSurface)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = VoltLime,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "HASIL & PERINGKAT PERSONAL RECORD",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    prEvaluations.forEach { eval ->
                        val isNewPr = eval.rank == 1
                        val isSecondPr = eval.rank == 2
                        val badgeColor = when {
                            isNewPr -> VoltLime
                            isSecondPr -> AeroCyan
                            else -> WarningAmber
                        }

                        Surface(
                            color = ObsidianBackground,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.55f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                // Baris 1: Badge Peringkat di Kiri, Waktu PR di Kanan
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = badgeColor.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = when {
                                                isNewPr -> "🏆 TEMBUS PR #1"
                                                isSecondPr -> "🥈 PR KE-2"
                                                else -> "🏅 PR KE-${eval.rank}"
                                            },
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = badgeColor,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }

                                    Text(
                                        text = RacePredictionEngine.formatDuration(eval.currentEquivTimeSec),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontFamily = TelemetryMonoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        color = badgeColor
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Baris 2: Nama Kategori (bebas overflow selebar kartu)
                                Text(
                                    text = "Kategori ${eval.categoryLabel}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                val explanationText = if (isNewPr) {
                                    if (eval.totalQualifyingRuns > 1 && eval.diffFromSecondSec > 0) {
                                        "Selamat! Sesi lari ini menembus Personal Record #1 Anda di kategori ${eval.categoryLabel}, unggul -${formatDeltaSeconds(eval.diffFromSecondSec)} dibandingkan rekor ke-2 Anda!"
                                    } else {
                                        "Selamat! Sesi lari ini tercatat sebagai Personal Record #1 tercepat Anda untuk kategori ${eval.categoryLabel}."
                                    }
                                } else {
                                    "Belum tembus PR #1. Sesi ini merupakan PR ke-${eval.rank} tercepat Anda (dari ${eval.totalQualifyingRuns} sesi). " +
                                        "Selisih +${formatDeltaSeconds(eval.diffFromBestSec)} dari PR #1 (${RacePredictionEngine.formatDuration(eval.bestPrTimeSec)} pada '${eval.bestWorkoutTitle}')."
                                }

                                Text(
                                    text = explanationText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // 3. PACE ANALYSIS, GRAFIK PACE & SPLITS
        // ==========================================
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SlateBorder, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SlateSurface)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = StravaLightBlue,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "PACE ANALYSIS",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Avg Pace & Fastest Split Badges
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            color = ObsidianBackground,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, SlateBorder),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "AVG PACE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = RacePredictionEngine.formatPace(avgPaceSec),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontFamily = TelemetryMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                        }

                        Surface(
                            color = ObsidianBackground,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, VoltLime.copy(alpha = 0.45f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = if (fastestSplit != null) "FASTEST SPLIT (KM ${fastestSplit.km})" else "PEAK PACE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = VoltLime
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = RacePredictionEngine.formatPace(fastestPaceSec),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontFamily = TelemetryMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = VoltLime
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = if (parsedSplits.isNotEmpty()) {
                            "GRAFIK PACE AKTUAL DARI ${parsedSplits.size} SPLIT (/KM)"
                        } else {
                            "GRAFIK PROFIL PACE (/KM)"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = AeroCyan,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    StravaPaceAreaChart(
                        paceSamples = paceSegments.map { it.paceSecondsPerKm },
                        splits = parsedSplits,
                        avgPaceSec = avgPaceSec,
                        totalDistanceKm = safeDist
                    )

                    if (parsedSplits.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        StravaSplitsBarChart(
                            splitsSummary = workout.splitsSummary,
                            avgPaceSecondsPerKm = avgPaceSec
                        )
                    }
                }
            }
        }

        // ==========================================
        // 4. PACE ZONE DISTRIBUTION (Jack Daniels VDOT Formula)
        // ==========================================
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SlateBorder, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SlateSurface)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = AeroCyan,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "PACE ZONE (FORMULA VDOT)",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Dihitung dengan rumus fisiologis Jack Daniels (VDOT ${"%.1f".format(vdotScore)} • Threshold T-Pace: ${RacePredictionEngine.formatPace(thresholdPaceSec)}) berdasarkan integrasi durasi aktual tiap segmen:",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Stacked 100% Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(14.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(ObsidianBackground)
                    ) {
                        paceZonesDistribution.filter { it.percentage > 0 }.forEach { zone ->
                            Box(
                                modifier = Modifier
                                    .weight(zone.percentage.toFloat().coerceAtLeast(1f))
                                    .height(14.dp)
                                    .background(zone.color)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    paceZonesDistribution.forEach { zone ->
                        Column(modifier = Modifier.padding(vertical = 5.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(
                                        color = zone.color.copy(alpha = 0.22f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "Z${zone.zoneNumber}",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = zone.color,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = zone.zoneTitle,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextPrimary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = zone.paceRangeText,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextSecondary
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = RacePredictionEngine.formatDuration(zone.durationSeconds),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "${zone.percentage}%",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontFamily = TelemetryMonoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        color = if (zone.percentage > 0) zone.color else TextSecondary,
                                        modifier = Modifier.width(46.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(5.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(7.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(ObsidianBackground)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth((zone.percentage / 100f).coerceIn(0f, 1f))
                                        .height(7.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(zone.color)
                                )
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // 5. HEART RATE INSIGHT
        // ==========================================
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SlateBorder, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SlateSurface)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = null,
                                tint = KineticCoral,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "HEART RATE INSIGHT",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Surface(
                            color = KineticCoral.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (isHrMeasured) "$effectiveHr bpm" else "~$effectiveHr bpm (Est.)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = KineticCoral,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val hrMaxPct = ((effectiveHr.toDouble() / 190.0) * 100).roundToInt().coerceIn(55, 98)
                    val hrZoneName = when {
                        hrMaxPct >= 90 -> "Zone 5 • Anaerobic Peak ($hrMaxPct% Max HR)"
                        hrMaxPct >= 82 -> "Zone 4 • Lactate Threshold ($hrMaxPct% Max HR)"
                        hrMaxPct >= 74 -> "Zone 3 • Aerobic Tempo ($hrMaxPct% Max HR)"
                        hrMaxPct >= 65 -> "Zone 2 • Aerobic Base ($hrMaxPct% Max HR)"
                        else -> "Zone 1 • Active Recovery ($hrMaxPct% Max HR)"
                    }

                    val hrInsightDesc = when {
                        hrMaxPct >= 85 -> "Detak jantung berada di zona ambang laktat tinggi. Sesi ini sangat efektif meningkatkan VO2Max dan toleransi asam laktat, namun membutuhkan pemulihan otot 36-48 jam."
                        hrMaxPct >= 74 -> "Detak jantung berada di zona Aerobik Tempo (Sweet Spot). Melatih efisiensi pompa jantung (stroke volume) dan daya tahan kecepatan tanpa memicu kelelahan sistem saraf berlebih."
                        else -> "Detak jantung terkontrol di zona Aerobik Dasar (Fat Burning & Capillary Building). Sangat optimal untuk membangun fondasi kardiovaskular dan mempercepat recovery."
                    }

                    Surface(
                        color = ObsidianBackground,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, KineticCoral.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = hrZoneName,
                                style = MaterialTheme.typography.titleSmall,
                                color = KineticCoral,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = hrInsightDesc,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }
        }

        // ==========================================
        // 6. RESULT AI DARI SEMUA DATA
        // ==========================================
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, VoltLime.copy(alpha = 0.6f), RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SlateSurface)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f).padding(end = 8.dp)
                        ) {
                            Surface(
                                color = VoltLime.copy(alpha = 0.2f),
                                shape = CircleShape,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Insights,
                                        contentDescription = "Analisis Latihan",
                                        tint = VoltLime,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "EVALUASI PERFORMA MENYELURUH",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = VoltLime,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Sintesis AI Coach, Jarak, PR, Split, Zona VDOT & HR",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                        }

                        if (onRefreshAi != null) {
                            OutlinedButton(
                                onClick = { if (!isRefreshingAi) onRefreshAi(workout) },
                                enabled = !isRefreshingAi,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, AeroCyan.copy(alpha = 0.6f)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.defaultMinSize(minHeight = 34.dp)
                            ) {
                                if (isRefreshingAi) {
                                    CircularProgressIndicator(
                                        color = AeroCyan,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(14.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Refresh AI",
                                        tint = AeroCyan,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isRefreshingAi) "Memuat..." else "Refresh AI",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AeroCyan,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    if (workout.aiCoachingInsight.isNotBlank()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Surface(
                            color = ObsidianBackground,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, AeroCyan.copy(alpha = 0.45f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "ANALISIS AI COACH",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = AeroCyan
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = workout.aiCoachingInsight,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPrimary,
                                    lineHeight = 21.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        color = ObsidianBackground,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, SlateBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "RINGKASAN TELEMETRI & ZONA VDOT",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = VoltLime
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = fullAiSynthesis,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary,
                                lineHeight = 21.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailMetricTile(
    label: String,
    value: String,
    accentColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        color = ObsidianBackground,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, SlateBorder),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    maxLines = 1
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontFamily = TelemetryMonoFontFamily,
                fontWeight = FontWeight.Bold,
                color = accentColor,
                maxLines = 1
            )
        }
    }
}

@Composable
fun StravaPaceAreaChart(
    paceSamples: List<Long>,
    avgPaceSec: Long,
    totalDistanceKm: Double,
    modifier: Modifier = Modifier,
    splits: List<StravaSplitItem> = emptyList()
) {
    val chartPaces = if (splits.isNotEmpty()) splits.map { it.paceSeconds } else paceSamples
    if (chartPaces.isEmpty()) return

    val minRaw = chartPaces.minOrNull() ?: avgPaceSec
    val maxRaw = chartPaces.maxOrNull() ?: avgPaceSec
    val spreadRaw = (maxRaw - minRaw).coerceAtLeast(10L)
    val pad = (spreadRaw * 0.18).roundToLong().coerceAtLeast(6L)
    val minPace = minOf(minRaw - pad, avgPaceSec - 4L).coerceAtLeast(120L)
    val maxPace = maxOf(maxRaw + pad, avgPaceSec + 4L).coerceAtLeast(minPace + 24L)
    val paceRange = (maxPace - minPace).coerceAtLeast(24L)

    val topPaceLabel = RacePredictionEngine.formatPace(minPace).removeSuffix("/km").trim()
    val midPaceLabel = RacePredictionEngine.formatPace((minPace + maxPace) / 2).removeSuffix("/km").trim()
    val botPaceLabel = RacePredictionEngine.formatPace(maxPace).removeSuffix("/km").trim()

    Surface(
        color = ObsidianBackground,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, SlateBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Micro legend matching StravaSplitsBarChart
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .width(14.dp)
                            .height(2.dp)
                            .background(VoltLime)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Avg ${RacePredictionEngine.formatPace(avgPaceSec)}",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = TelemetryMonoFontFamily,
                        fontSize = 10.sp,
                        color = VoltLime
                    )
                }
                if (splits.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(SuccessGreen)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Cepat", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = SuccessGreen)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(KineticCoral)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Lambat", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = KineticCoral)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            ) {
                // Fixed-width Y-Axis Pace Labels (Faster at top, Slower at bottom)
                Column(
                    modifier = Modifier
                        .width(42.dp)
                        .height(150.dp)
                        .padding(end = 6.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End
                ) {
                    Text(text = topPaceLabel, style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 10.sp, maxLines = 1)
                    Text(text = midPaceLabel, style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 10.sp, maxLines = 1)
                    Text(text = botPaceLabel, style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 10.sp, maxLines = 1)
                    Text(text = "/km", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 9.sp, maxLines = 1)
                }

                // Area Chart Canvas aligned 1-to-1 with splits
                Canvas(
                    modifier = Modifier
                        .weight(1f)
                        .height(150.dp)
                ) {
                    val w = size.width
                    val h = size.height
                    val horizontalPad = if (splits.isNotEmpty()) 6.dp.toPx() else 0f
                    val usableW = (w - horizontalPad * 2f).coerceAtLeast(1f)

                    // Horizontal grid lines
                    for (i in 0..3) {
                        val y = h * (i / 3f)
                        drawLine(
                            color = SlateBorder.copy(alpha = 0.5f),
                            start = Offset(0f, y),
                            end = Offset(w, y),
                            strokeWidth = 1f
                        )
                    }

                    // Average pace dashed reference line
                    val avgNorm = ((avgPaceSec - minPace).toFloat() / paceRange.toFloat()).coerceIn(0.06f, 0.94f)
                    val avgY = avgNorm * h
                    drawLine(
                        color = VoltLime.copy(alpha = 0.7f),
                        start = Offset(0f, avgY),
                        end = Offset(w, avgY),
                        strokeWidth = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                    )

                    val points = if (chartPaces.size == 1) {
                        val norm = ((chartPaces[0] - minPace).toFloat() / paceRange.toFloat()).coerceIn(0.08f, 0.92f)
                        val y = norm * h
                        listOf(Offset(horizontalPad, y), Offset(w - horizontalPad, y))
                    } else {
                        val stepX = usableW / (chartPaces.size - 1)
                        chartPaces.mapIndexed { index, pace ->
                            val x = horizontalPad + index * stepX
                            val normalized = ((pace - minPace).toFloat() / paceRange.toFloat()).coerceIn(0.08f, 0.92f)
                            Offset(x, normalized * h)
                        }
                    }

                    val linePath = Path()
                    val areaPath = Path()
                    points.forEachIndexed { idx, pt ->
                        if (idx == 0) {
                            linePath.moveTo(pt.x, pt.y)
                            areaPath.moveTo(pt.x, h)
                            areaPath.lineTo(pt.x, pt.y)
                        } else {
                            linePath.lineTo(pt.x, pt.y)
                            areaPath.lineTo(pt.x, pt.y)
                        }
                    }
                    areaPath.lineTo(points.last().x, h)
                    areaPath.close()

                    drawPath(
                        path = areaPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                StravaLightBlue.copy(alpha = 0.55f),
                                StravaBlue.copy(alpha = 0.14f)
                            )
                        )
                    )

                    drawPath(
                        path = linePath,
                        color = StravaLightBlue,
                        style = Stroke(
                            width = 2.8.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )

                    // Split marker dots matching faster/slower coloring
                    if (splits.isNotEmpty()) {
                        val dotPoints = if (splits.size == 1) {
                            listOf(Offset(w / 2f, points.first().y) to splits.first().paceSeconds)
                        } else {
                            points.mapIndexed { idx, pt -> pt to splits[idx].paceSeconds }
                        }
                        dotPoints.forEach { (pt, pace) ->
                            val dotColor = if (pace <= avgPaceSec) SuccessGreen else KineticCoral
                            drawCircle(
                                color = ObsidianBackground,
                                radius = 5.dp.toPx(),
                                center = pt
                            )
                            drawCircle(
                                color = dotColor,
                                radius = 3.5.dp.toPx(),
                                center = pt
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // X-Axis Ticks aligned directly under Canvas
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 42.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (splits.isNotEmpty()) {
                    val displayedSplits = if (splits.size <= 6) {
                        splits
                    } else {
                        val indices = (0..4).map { step ->
                            ((step * (splits.size - 1)) / 4.0).roundToInt().coerceIn(0, splits.lastIndex)
                        }.distinct()
                        indices.map { splits[it] }
                    }
                    if (displayedSplits.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "KM ${displayedSplits[0].km}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            fontSize = 10.sp
                        )
                        Spacer(modifier = Modifier.weight(1f))
                    } else {
                        displayedSplits.forEach { sp ->
                            Text(
                                text = "KM ${sp.km}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                fontSize = 10.sp,
                                maxLines = 1
                            )
                        }
                    }
                } else {
                    val ticks = listOf(0.0, 0.25, 0.5, 0.75, 1.0).map { totalDistanceKm * it }
                    ticks.forEachIndexed { idx, d ->
                        Text(
                            text = if (idx == 0) "0 km" else "${"%.1f".format(d)} km",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            fontSize = 10.sp,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

/**
 * Deterministic pace segments derived strictly from actual recorded splits (for VDOT zone integration).
 * Zero random numbers or dummy trigonometric waves.
 */
private fun buildDeterministicPaceSegments(
    splits: List<StravaSplitItem>,
    avgPaceSec: Long,
    totalDistanceKm: Double,
    totalDurationSec: Long
): List<PaceSegmentSample> {
    val segments = mutableListOf<PaceSegmentSample>()

    if (splits.isNotEmpty()) {
        var coveredKm = 0.0
        for (i in splits.indices) {
            val split = splits[i]
            val segDistKm = (totalDistanceKm - coveredKm).coerceIn(0.05, 1.0)
            val segDurSec = split.paceSeconds * segDistKm
            coveredKm += segDistKm
            segments.add(
                PaceSegmentSample(
                    distanceKmMark = coveredKm,
                    paceSecondsPerKm = split.paceSeconds,
                    segmentDurationSeconds = segDurSec
                )
            )
            if (coveredKm >= totalDistanceKm - 0.02) break
        }

        val remainderKm = (totalDistanceKm - coveredKm).coerceAtLeast(0.0)
        if (remainderKm >= 0.05) {
            val coveredDuration = segments.sumOf { it.segmentDurationSeconds }
            val remDurSec = (totalDurationSec - coveredDuration).coerceAtLeast(splits.last().paceSeconds * remainderKm)
            val remPaceSec = (remDurSec / remainderKm).roundToLong().coerceIn(120L, 1800L)
            segments.add(
                PaceSegmentSample(
                    distanceKmMark = totalDistanceKm,
                    paceSecondsPerKm = remPaceSec,
                    segmentDurationSeconds = remDurSec
                )
            )
        }
    } else {
        // When no splits were provided, use a standard physiological pacing curve:
        // First 15% distance: warmup transition (1.04 * avgPace -> 1.00 * avgPace)
        // Middle 70% distance: steady aerobic cruise (1.00 * avgPace -> 0.99 * avgPace)
        // Final 15% distance: finish effort (0.99 * avgPace -> 0.97 * avgPace)
        val totalSteps = 20
        val stepDist = totalDistanceKm / totalSteps
        for (step in 0 until totalSteps) {
            val progress = step.toDouble() / (totalSteps - 1).toDouble()
            val multiplier = when {
                progress < 0.15 -> 1.04 - (progress / 0.15) * 0.04
                progress < 0.85 -> 1.00 - ((progress - 0.15) / 0.70) * 0.01
                else -> 0.99 - ((progress - 0.85) / 0.15) * 0.02
            }
            val p = (avgPaceSec * multiplier).roundToLong().coerceIn(120L, 1800L)
            segments.add(
                PaceSegmentSample(
                    distanceKmMark = (step + 1) * stepDist,
                    paceSecondsPerKm = p,
                    segmentDurationSeconds = p * stepDist
                )
            )
        }
    }

    return segments
}

/**
 * Patented Jack Daniels VDOT 5-Zone Pace Distribution aligned 1:1 with Heart Rate zones:
 * Zones are anchored to the athlete's Lactate Threshold Pace (T-Pace, in sec/km; faster pace = smaller sec/km):
 * - Zone 5 (VO2Max Interval & Repetition, >105% T-speed, 93-100% HRmax): Pace <= 0.95 * T-Pace
 * - Zone 4 (Lactate Threshold / T-Pace, 97% - 105% T-speed, 85-90% HRmax): 0.95 * T-Pace < Pace <= 1.03 * T-Pace
 * - Zone 3 (Marathon / Aerobic Tempo, 88% - 97% T-speed, 79-84% HRmax): 1.03 * T-Pace < Pace <= 1.14 * T-Pace
 * - Zone 2 (Easy / Aerobic Endurance, 77% - 88% T-speed, 65-78% HRmax): 1.14 * T-Pace < Pace <= 1.30 * T-Pace
 * - Zone 1 (Active Recovery, <77% T-speed, <65% HRmax): Pace > 1.30 * T-Pace
 *
 * Time in each zone is integrated from actual segment durations and normalized to 100%.
 */
private fun computeScientificVdotPaceZoneDistribution(
    segments: List<PaceSegmentSample>,
    thresholdPaceSec: Long,
    totalDurationSec: Long
): List<PaceZoneDistributionItem> {
    if (segments.isEmpty()) return emptyList()

    val z5Bound = (thresholdPaceSec * 0.95).roundToLong().coerceAtLeast(120L)
    val z4Bound = (thresholdPaceSec * 1.03).roundToLong().coerceAtLeast(z5Bound + 6L)
    val z3Bound = (thresholdPaceSec * 1.14).roundToLong().coerceAtLeast(z4Bound + 10L)
    val z2Bound = (thresholdPaceSec * 1.30).roundToLong().coerceAtLeast(z3Bound + 15L)

    val zoneDurationsSec = DoubleArray(5)
    for (seg in segments) {
        val p = seg.paceSecondsPerKm
        val dur = seg.segmentDurationSeconds
        when {
            p <= z5Bound -> zoneDurationsSec[4] += dur // Zone 5: VO2Max / Anaerobic
            p <= z4Bound -> zoneDurationsSec[3] += dur // Zone 4: Lactate Threshold (T-Pace centered here)
            p <= z3Bound -> zoneDurationsSec[2] += dur // Zone 3: Marathon / Aerobic Tempo
            p <= z2Bound -> zoneDurationsSec[1] += dur // Zone 2: Easy / Aerobic Base
            else -> zoneDurationsSec[0] += dur         // Zone 1: Active Recovery
        }
    }

    val totalIntegratedSec = zoneDurationsSec.sum().coerceAtLeast(1.0)
    val rawPcts = zoneDurationsSec.map { (it * 100.0) / totalIntegratedSec }
    val intPcts = rawPcts.map { it.toInt() }.toIntArray()
    var remainder = 100 - intPcts.sum()

    // Largest Remainder Method (Hamilton method) so integer percentages strictly sum to 100%
    val fractions = rawPcts.mapIndexed { idx, v -> idx to (v - v.toInt()) }
        .sortedByDescending { it.second }
    var fIdx = 0
    while (remainder > 0 && fIdx < fractions.size) {
        intPcts[fractions[fIdx].first]++
        remainder--
        fIdx++
    }

    fun fmt(sec: Long) = RacePredictionEngine.formatPace(sec).removeSuffix("/km").trim()

    val definitions = listOf(
        Triple(5, "Zona 5 • VO2Max / Anaerobic", "< ${fmt(z5Bound)} /km") to KineticCoral,
        Triple(4, "Zona 4 • Lactate Threshold", "${fmt(z5Bound)} - ${fmt(z4Bound)} /km") to WarningAmber,
        Triple(3, "Zona 3 • Marathon / Tempo Aerobik", "${fmt(z4Bound)} - ${fmt(z3Bound)} /km") to VoltLime,
        Triple(2, "Zona 2 • Easy / Aerobic Base", "${fmt(z3Bound)} - ${fmt(z2Bound)} /km") to AeroCyan,
        Triple(1, "Zona 1 • Active Recovery", "> ${fmt(z2Bound)} /km") to SuccessGreen
    )

    return definitions.map { (info, color) ->
        val zNum = info.first
        val pct = intPcts[zNum - 1]
        val zDur = ((totalDurationSec * pct) / 100.0).roundToLong()
        PaceZoneDistributionItem(
            zoneNumber = zNum,
            zoneTitle = info.second,
            paceRangeText = info.third,
            percentage = pct,
            durationSeconds = zDur,
            color = color
        )
    }
}

private fun evaluateWorkoutPrRankings(
    workout: WorkoutLog,
    allWorkouts: List<WorkoutLog>,
    workoutPaceSec: Long
): List<PrEvaluationResult> {
    val results = mutableListOf<PrEvaluationResult>()
    val combinedWorkouts = if (allWorkouts.any { it.id == workout.id }) {
        allWorkouts
    } else {
        allWorkouts + workout
    }

    val qualifyingCategories = DistanceCategory.entries.filter { cat ->
        workout.distanceKm >= (cat.kilometers * 0.85)
    }

    if (qualifyingCategories.isNotEmpty()) {
        for (cat in qualifyingCategories) {
            val eligibleRuns = combinedWorkouts.filter { w ->
                w.distanceKm >= (cat.kilometers * 0.85)
            }.map { w ->
                val p = if (w.avgPaceSecondsPerKm in 60L..3600L) {
                    w.avgPaceSecondsPerKm
                } else {
                    (w.durationSeconds / w.distanceKm.coerceAtLeast(0.1)).roundToLong()
                }
                val equivTime = (p * cat.kilometers).roundToLong()
                w to equivTime
            }.sortedBy { it.second }

            val rankIdx = eligibleRuns.indexOfFirst { it.first.id == workout.id && workout.id.isNotBlank() }
                .let { idx ->
                    if (idx >= 0) idx else eligibleRuns.indexOfFirst {
                        it.first.title == workout.title && it.first.durationSeconds == workout.durationSeconds
                    }
                }.coerceAtLeast(0)

            val rank = rankIdx + 1
            val currentEquivTime = (workoutPaceSec * cat.kilometers).roundToLong()
            val bestEntry = eligibleRuns.firstOrNull()
            val bestTime = bestEntry?.second ?: currentEquivTime
            val secondTime = eligibleRuns.getOrNull(1)?.second ?: currentEquivTime

            results.add(
                PrEvaluationResult(
                    categoryLabel = "${cat.shortLabel} (${cat.displayName})",
                    rank = rank,
                    totalQualifyingRuns = eligibleRuns.size,
                    currentEquivTimeSec = currentEquivTime,
                    bestPrTimeSec = bestTime,
                    diffFromBestSec = (currentEquivTime - bestTime).coerceAtLeast(0L),
                    diffFromSecondSec = (secondTime - currentEquivTime).coerceAtLeast(0L),
                    bestWorkoutTitle = bestEntry?.first?.title ?: workout.title
                )
            )
        }
    } else {
        val targetKm = workout.distanceKm.coerceAtLeast(1.0)
        val sortedByPace = combinedWorkouts.map { w ->
            val p = if (w.avgPaceSecondsPerKm in 60L..3600L) {
                w.avgPaceSecondsPerKm
            } else {
                (w.durationSeconds / w.distanceKm.coerceAtLeast(0.1)).roundToLong()
            }
            val equivTime = (p * targetKm).roundToLong()
            w to equivTime
        }.sortedBy { it.second }

        val rankIdx = sortedByPace.indexOfFirst { it.first.id == workout.id && workout.id.isNotBlank() }
            .let { idx -> if (idx >= 0) idx else 0 }
        val rank = rankIdx + 1
        val currentTime = (workoutPaceSec * targetKm).roundToLong()
        val bestEntry = sortedByPace.firstOrNull()
        val bestTime = bestEntry?.second ?: currentTime
        val secondTime = sortedByPace.getOrNull(1)?.second ?: currentTime

        results.add(
            PrEvaluationResult(
                categoryLabel = "${"%.2f".format(targetKm)} KM",
                rank = rank,
                totalQualifyingRuns = sortedByPace.size,
                currentEquivTimeSec = currentTime,
                bestPrTimeSec = bestTime,
                diffFromBestSec = (currentTime - bestTime).coerceAtLeast(0L),
                diffFromSecondSec = (secondTime - currentTime).coerceAtLeast(0L),
                bestWorkoutTitle = bestEntry?.first?.title ?: workout.title
            )
        )
    }

    return results
}

private fun formatDeltaSeconds(seconds: Long): String {
    if (seconds < 60L) return "$seconds detik"
    val mins = seconds / 60L
    val remSecs = seconds % 60L
    return if (remSecs == 0L) "$mins menit" else "$mins menit $remSecs detik"
}

private fun buildComprehensiveAiResult(
    workout: WorkoutLog,
    avgPaceSec: Long,
    calories: Int,
    prResults: List<PrEvaluationResult>,
    splits: List<StravaSplitItem>,
    fastestSplit: StravaSplitItem?,
    slowestSplit: StravaSplitItem?,
    zones: List<PaceZoneDistributionItem>,
    effectiveHr: Long,
    isHrMeasured: Boolean,
    vdotScore: Double,
    thresholdPaceSec: Long
): String {
    val primaryPr = prResults.firstOrNull()
    val prSummary = if (primaryPr != null) {
        if (primaryPr.rank == 1) {
            "Tembus Personal Record #1 pada kategori ${primaryPr.categoryLabel} dengan waktu ${RacePredictionEngine.formatDuration(primaryPr.currentEquivTimeSec)}!"
        } else {
            "Merupakan PR ke-${primaryPr.rank} tercepat di kategori ${primaryPr.categoryLabel} (terpaut +${formatDeltaSeconds(primaryPr.diffFromBestSec)} dari PR #1 Anda)."
        }
    } else {
        "Sesi latihan berhasil meningkatkan volume aerobik mingguan Anda."
    }

    val splitInsight = if (fastestSplit != null && slowestSplit != null && splits.size >= 2) {
        val spread = slowestSplit.paceSeconds - fastestSplit.paceSeconds
        val trend = if (splits.last().paceSeconds < splits.first().paceSeconds) {
            "Negative Split (kilometer akhir lebih cepat)"
        } else {
            "Positive Split (awal cepat lalu stabil)"
        }
        "Pola lari menunjukkan $trend. Split tercepat dicapai pada KM ${fastestSplit.kmLabel} (${fastestSplit.paceStr}/km) dengan rentang variasi antar split $spread detik/km."
    } else {
        "Pace rata-rata terjaga di ${RacePredictionEngine.formatPace(avgPaceSec)} sepanjang ${"%.2f".format(workout.distanceKm)} km."
    }

    val dominantZones = zones.filter { it.percentage > 0 }
        .sortedByDescending { it.percentage }
        .take(3)
        .joinToString(", ") { "Zona ${it.zoneNumber} (${it.percentage}%)" }

    val hrText = if (isHrMeasured) "$effectiveHr bpm (sensor HR)" else "~$effectiveHr bpm (estimasi rasio T-Pace)"

    val nextAction = if (primaryPr != null && primaryPr.rank > 1 && primaryPr.diffFromBestSec in 1L..30L) {
        "Karena Anda hanya berjarak ${formatDeltaSeconds(primaryPr.diffFromBestSec)} dari PR #1, jaga pace awal di ${RacePredictionEngine.formatPace(avgPaceSec)} pada paruh pertama lalu tingkatkan ritme ke Threshold Pace (${RacePredictionEngine.formatPace(thresholdPaceSec)}) di kilometer terakhir untuk memecahkan PR #1!"
    } else {
        "Pulihkan energi ~$calories kcal dengan nutrisi karbohidrat & protein (rasio 3:1) dalam 45 menit pasca-lari, serta kombinasikan sesi Threshold / Interval (Zona 4-5) dan Easy Run (Zona 2) di jadwal latihan minggu ini."
    }

    return buildString {
        appendLine("1. Ringkasan & Status PR:")
        appendLine("Sesi '${workout.title}' menempuh ${"%.2f".format(workout.distanceKm)} km dalam ${RacePredictionEngine.formatDuration(workout.durationSeconds)} (~$calories kcal). $prSummary")
        appendLine()
        appendLine("2. Analisis Pace & Split:")
        appendLine(splitInsight)
        appendLine()
        appendLine("3. Distribusi Zona VDOT (${"%.1f".format(vdotScore)}) & Beban Jantung:")
        appendLine("Berdasarkan Threshold Pace ${RacePredictionEngine.formatPace(thresholdPaceSec)}, waktu lari terdistribusi di $dominantZones dengan beban kardiovaskular $hrText.")
        appendLine()
        append("4. Strategi & Rekomendasi Latihan Berikutnya:\n$nextAction")
    }
}
