package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TrainingGoal
import com.example.data.model.WorkoutLog
import com.example.domain.RacePredictionEngine
import com.example.domain.ShoeUnlockEngine
import com.example.domain.WeeklyAnalytics
import com.example.domain.WeeklyProgressEngine
import com.example.ui.theme.AeroCyan
import com.example.ui.theme.KineticCoral
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.OnVoltLime
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.SlateSurfaceElevated
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TelemetryMonoFontFamily
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VoltLime
import com.example.util.AdBannerBar
import com.example.viewmodel.UiState
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(
    workoutsState: UiState<List<WorkoutLog>>,
    goalsState: UiState<List<TrainingGoal>>,
    onNavigateToShoes: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val workouts = (workoutsState as? UiState.Success)?.data ?: emptyList()
    val goals = (goalsState as? UiState.Success)?.data ?: emptyList()
    val activeGoal = goals.firstOrNull { !it.completed } ?: goals.firstOrNull()

    val shoeProgress = remember(workouts) {
        ShoeUnlockEngine.evaluateProgress(workouts)
    }

    val weeklyAnalytics = remember(workouts, activeGoal) {
        WeeklyProgressEngine.computeWeeklyProgress(workouts, activeGoal)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBackground)
    ) {
        // Banner Iklan Non-Intrusif di paling atas Progress
        AdBannerBar(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        )

        // Top Header + Dedicated Shoes Shortcut
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Progress Mingguan",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = "Pantau akumulasi jarak mingguan Anda dan buka halaman Katalog Sepatu khusus.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            Surface(
                color = SlateSurface,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, VoltLime.copy(alpha = 0.45f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onNavigateToShoes() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            imageVector = Icons.Default.ElectricBolt,
                            contentDescription = null,
                            tint = VoltLime,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Katalog Sepatu (${shoeProgress.currentTier.badgeLabel})",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                    Text(
                        text = "Buka →",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = VoltLime
                    )
                }
            }
        }

        // Weekly Progress Body
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            WeeklyProgressTabContent(
                analytics = weeklyAnalytics
            )
        }
    }
}

/**
 * Tab Content: Weekly Progress Analytics & Day-by-Day Volume Breakdown.
 */
@Composable
private fun WeeklyProgressTabContent(
    analytics: WeeklyAnalytics
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Weekly Mileage Hero Card
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
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        ) {
                            Text(
                                text = "TOTAL JARAK MINGGU INI",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = TextSecondary,
                                letterSpacing = 0.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${String.format(Locale.US, "%.1f", analytics.totalDistanceKm)} km",
                                style = MaterialTheme.typography.headlineLarge,
                                fontFamily = TelemetryMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = VoltLime,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Surface(
                            color = VoltLime.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = analytics.weekRangeDisplay,
                                style = MaterialTheme.typography.labelSmall,
                                color = VoltLime,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Progress vs target
                    val progressRatio = (analytics.totalDistanceKm.toFloat() / analytics.targetDistanceKm.toFloat()).coerceIn(0f, 1f)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Target Mingguan: ${analytics.targetDistanceKm} km",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )
                        Text(
                            text = "${(progressRatio * 100).toInt()}%",
                            style = MaterialTheme.typography.labelMedium,
                            fontFamily = TelemetryMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = if (progressRatio >= 1f) SuccessGreen else VoltLime
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { progressRatio },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape),
                        color = if (progressRatio >= 1f) SuccessGreen else VoltLime,
                        trackColor = SlateBorder
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = SlateBorder)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Three Metric Badges: Runs, Avg Pace, Moving Time
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = SlateSurfaceElevated,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, SlateBorder),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("SESI LARI", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 9.sp, maxLines = 1)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${analytics.runsCount} sesi",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Surface(
                            color = SlateSurfaceElevated,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, SlateBorder),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("AVG PACE", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 9.sp, maxLines = 1)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (analytics.avgPaceSec > 0L) RacePredictionEngine.formatPace(analytics.avgPaceSec) else "--:--",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontFamily = TelemetryMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = VoltLime,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Surface(
                            color = SlateSurfaceElevated,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, SlateBorder),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("WAKTU TEMPUH", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = RacePredictionEngine.formatDuration(analytics.totalDurationSec),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontFamily = TelemetryMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = AeroCyan,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Weekly Comparison vs Last Week
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SlateBorder, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SlateSurface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "KOMPARASI DENGAN MINGGU LALU",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val diffSign = if (analytics.distanceDiffKm >= 0.0) "+" else ""
                            Text(
                                text = "$diffSign${String.format(Locale.US, "%.1f", analytics.distanceDiffKm)} km",
                                style = MaterialTheme.typography.titleMedium,
                                fontFamily = TelemetryMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = if (analytics.distanceDiffKm >= 0.0) SuccessGreen else KineticCoral
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "vs minggu lalu (${String.format(Locale.US, "%.1f", analytics.prevWeekDistanceKm)} km)",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }

                    if (analytics.paceDiffSec != 0L && analytics.avgPaceSec > 0L && analytics.prevWeekAvgPaceSec > 0L) {
                        Surface(
                            color = if (analytics.paceDiffSec < 0) SuccessGreen.copy(alpha = 0.18f) else KineticCoral.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (analytics.paceDiffSec < 0) "${-analytics.paceDiffSec}s lebih cepat" else "+${analytics.paceDiffSec}s",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (analytics.paceDiffSec < 0) SuccessGreen else KineticCoral,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // 3. Day-by-Day Volume Breakdown (Mon - Sun)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SlateBorder, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SlateSurface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "DISTRIBUSI LARI HARIAN",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = VoltLime
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    val maxDayDist = analytics.dailyStats.maxOfOrNull { it.distanceKm }?.coerceAtLeast(5.0) ?: 5.0

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        analytics.dailyStats.forEach { day ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                if (day.distanceKm > 0.0) {
                                    Text(
                                        text = "${String.format(Locale.US, "%.1f", day.distanceKm)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = TelemetryMonoFontFamily,
                                        fontSize = 9.sp,
                                        color = VoltLime
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                }

                                val barHeightRatio = (day.distanceKm / maxDayDist).coerceIn(0.08, 1.0).toFloat()
                                Box(
                                    modifier = Modifier
                                        .width(22.dp)
                                        .height((100 * barHeightRatio).dp)
                                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                        .background(
                                            when {
                                                day.hasRun -> VoltLime
                                                day.isToday -> AeroCyan.copy(alpha = 0.4f)
                                                else -> SlateBorder.copy(alpha = 0.4f)
                                            }
                                        )
                                )

                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = day.dayName,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Normal,
                                    color = if (day.isToday) VoltLime else TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. Coaching Advice Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SlateBorder, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SlateSurface)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = AeroCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Rekomendasi Beban Latihan",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = analytics.coachingTip,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }
}
