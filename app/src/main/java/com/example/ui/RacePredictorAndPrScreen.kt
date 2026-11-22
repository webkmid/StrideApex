package com.example.ui

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
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.DistanceCategory
import com.example.data.model.PersonalRecord
import com.example.data.model.TrainingGoal
import com.example.data.model.WorkoutLog
import com.example.domain.RacePredictionEngine
import com.example.ui.theme.AeroCyan
import com.example.ui.theme.KineticCoral
import com.example.ui.theme.ObsidianBackground
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

@Composable
fun RacePredictorAndPrScreen(
    workoutsState: UiState<List<WorkoutLog>>,
    prsState: UiState<List<PersonalRecord>>,
    goalsState: UiState<List<TrainingGoal>>? = null,
    fatigueExponent: Double,
    onFatigueExponentChange: (Double) -> Unit,
    onAddGoal: ((String, DistanceCategory, Long, Long, Boolean, Long, Long) -> Unit)? = null,
    onToggleGoalCompleted: ((TrainingGoal) -> Unit)? = null,
    onDeleteGoal: ((String) -> Unit)? = null,
    onDeletePr: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val workouts = (workoutsState as? UiState.Success)?.data.orEmpty()
    val prs = (prsState as? UiState.Success)?.data.orEmpty()

    val hasData = workouts.isNotEmpty() || prs.isNotEmpty()
    val predictions = remember(workouts, prs, fatigueExponent) {
        if (hasData) {
            RacePredictionEngine.computeRacePredictions(workouts, prs, fatigueExponent)
        } else {
            emptyList()
        }
    }

    var showVdotDialog by remember { mutableStateOf(false) }

    if (showVdotDialog) {
        VdotInfoDialog(onDismiss = { showVdotDialog = false })
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBackground)
            .testTag("predict_prs_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Banner Iklan Non-Intrusif di paling atas Prediksi
        item {
            AdBannerBar(modifier = Modifier.fillMaxWidth())
        }

        // 1. Personal Record (PR) Vault
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = VoltLime,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Personal Records",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                maxLines = 1
                            )
                        }
                        Surface(
                            color = VoltLime.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Auto dari Riwayat",
                                style = MaterialTheme.typography.labelSmall,
                                color = VoltLime,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    DistanceCategory.entries.forEach { category ->
                        val record = prs.firstOrNull { it.distanceCategory == category.key }
                        Surface(
                            color = ObsidianBackground,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .border(1.dp, SlateBorder, RoundedCornerShape(12.dp))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${category.shortLabel} • ${category.displayName}",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = if (record != null) "Dari: ${record.workoutTitle}" else "Belum ada rekor di riwayat",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (record != null) AeroCyan else TextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                if (record != null) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = RacePredictionEngine.formatDuration(record.bestTimeSeconds),
                                                style = MaterialTheme.typography.titleLarge,
                                                fontFamily = TelemetryMonoFontFamily,
                                                color = VoltLime
                                            )
                                            val delta = if (record.previousTimeSeconds > record.bestTimeSeconds) {
                                                record.previousTimeSeconds - record.bestTimeSeconds
                                            } else {
                                                0L
                                            }
                                            if (delta > 0L) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = Icons.AutoMirrored.Filled.TrendingDown,
                                                        contentDescription = null,
                                                        tint = SuccessGreen,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(3.dp))
                                                    Text(
                                                        text = "-${delta}s PR",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = SuccessGreen
                                                    )
                                                }
                                            } else {
                                                Text(
                                                    text = RacePredictionEngine.formatPace(record.paceSecondsPerKm),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = AeroCyan
                                                )
                                            }
                                        }
                                        if (onDeletePr != null) {
                                            IconButton(
                                                onClick = { onDeletePr(category.key) },
                                                modifier = Modifier.size(44.dp).padding(start = 4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.DeleteOutline,
                                                    contentDescription = "Hapus PR",
                                                    tint = TextSecondary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    Text(
                                        text = "--:--",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontFamily = TelemetryMonoFontFamily,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Race Predictions (Only shown when user has logged at least 1 workout or PR)
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
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                tint = VoltLime,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Race Predictions",
                                style = MaterialTheme.typography.titleLarge,
                                color = TextPrimary
                            )
                        }
                        if (predictions.isNotEmpty()) {
                            Surface(
                                color = VoltLime.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.clickable { showVdotDialog = true }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    val vdot = predictions.first().vdotScore
                                    Text(
                                        text = "VDOT ${"%.1f".format(vdot)}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = VoltLime
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "Info VDOT",
                                        tint = VoltLime,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (predictions.isEmpty()) {
                        Text(
                            text = "Tambahkan data lari di Scan Run atau Riwayat terlebih dahulu untuk melihat kalkulasi prediksi waktu lomba dan zona latihan Anda.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    } else {
                        predictions.forEach { pred ->
                            Surface(
                                color = ObsidianBackground,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .border(1.dp, SlateBorder, RoundedCornerShape(12.dp))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Surface(
                                            color = VoltLime.copy(alpha = 0.18f),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = pred.category.shortLabel,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = VoltLime,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = pred.category.displayName,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = RacePredictionEngine.formatDuration(pred.predictedTimeSeconds),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontFamily = TelemetryMonoFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            color = VoltLime,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                        Text(
                                            text = RacePredictionEngine.formatPace(pred.targetPaceSecondsPerKm),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = AeroCyan,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VdotInfoDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SlateSurfaceElevated,
        icon = {
            Icon(
                imageVector = Icons.Default.Speed,
                contentDescription = null,
                tint = VoltLime,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Text(
                text = "Apa itu VDOT?",
                color = TextPrimary,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "VDOT adalah indikator kapasitas aerobik lari (running fitness) yang dirumuskan oleh Dr. Jack Daniels dalam 'Daniels' Running Formula'.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
                Text(
                    text = "• Dihitung otomatis dari waktu dan jarak tempuh terbaik di riwayat lari Anda (tanpa perlu input manual).",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Text(
                    text = "• Memprediksi potensi waktu finish realistis di nomor 5K, 10K, Half Marathon, dan Full Marathon.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Text(
                    text = "• Mengkalibrasi zona target pace latihan harian (Easy, Marathon, Threshold, Interval, Repetition) agar latihan efektif dan bebas cedera.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = VoltLime, contentColor = Color.Black)
            ) {
                Text("Mengerti")
            }
        }
    )
}
