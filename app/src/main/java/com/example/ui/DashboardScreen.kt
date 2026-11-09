package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.CloudBackupSnapshot
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
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VoltLime
import com.example.viewmodel.UiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    workoutsState: UiState<List<WorkoutLog>>,
    prsState: UiState<List<PersonalRecord>>,
    goalsState: UiState<List<TrainingGoal>>,
    backupsState: UiState<List<CloudBackupSnapshot>>,
    isOfflineMode: Boolean = false,
    onOpenSignIn: () -> Unit = {},
    onNavigateToOcr: () -> Unit,
    onNavigateToPredict: () -> Unit,
    onCreateCloudBackup: (String) -> Unit,
    onExportBackupJson: () -> String,
    onDeleteWorkout: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val workouts = (workoutsState as? UiState.Success)?.data.orEmpty()
    val prs = (prsState as? UiState.Success)?.data.orEmpty()
    val goals = (goalsState as? UiState.Success)?.data.orEmpty()
    val backups = (backupsState as? UiState.Success)?.data.orEmpty()

    val totalVolumeKm = workouts.sumOf { it.distanceKm }
    val avgCadence = if (workouts.isNotEmpty()) workouts.map { it.cadenceSpm }.average().toInt() else 182
    val predictions = remember(workouts, prs) {
        RacePredictionEngine.computeRacePredictions(workouts, prs, 1.06)
    }
    val currentVdot = predictions.firstOrNull()?.vdotScore ?: 52.4
    val activeGoal = goals.firstOrNull { !it.completed } ?: goals.firstOrNull()
    val weeklyTargetKm = activeGoal?.weeklyMileageTargetKm ?: 65L

    var showExportDialog by remember { mutableStateOf(false) }
    var exportedJsonText by remember { mutableStateOf("") }

    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            containerColor = SlateSurfaceElevated,
            title = {
                Text(
                    text = if (isOfflineMode) "Local SQLite Backup JSON" else "Cloud Training Backup JSON",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )
            },
            text = {
                Column {
                    Text(
                        text = if (isOfflineMode) {
                            "Your training history is stored locally on-device in Room SQLite. Sign in anytime to enable Cloud Firestore sync."
                        } else {
                            "Your training history is continuously synced with Cloud Firestore and mirrored to local Room SQLite for offline resilience."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        color = ObsidianBackground,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .border(1.dp, SlateBorder, RoundedCornerShape(12.dp))
                    ) {
                        Text(
                            text = exportedJsonText,
                            fontFamily = TelemetryMonoFontFamily,
                            style = MaterialTheme.typography.labelSmall,
                            color = VoltLime,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("Done", color = VoltLime)
                }
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBackground)
            .testTag("dashboard_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Hero Telemetry Banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(195.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .border(1.dp, SlateBorder, RoundedCornerShape(24.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_hero_runner_1794240109820),
                    contentDescription = "Runner Telemetry Banner",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    ObsidianBackground.copy(alpha = 0.94f),
                                    ObsidianBackground.copy(alpha = 0.65f),
                                    Color.Transparent
                                )
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val badgeColor = if (isOfflineMode) AeroCyan else SuccessGreen
                        Surface(
                            color = badgeColor.copy(alpha = 0.2f),
                            shape = CircleShape,
                            modifier = Modifier
                                .border(1.dp, badgeColor, CircleShape)
                                .clickable(enabled = isOfflineMode) { onOpenSignIn() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudDone,
                                    contentDescription = if (isOfflineMode) "Offline SQLite Mode" else "Cloud Synced",
                                    tint = badgeColor,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isOfflineMode) "OFFLINE SQLITE MODE" else "CLOUD SYNC ACTIVE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = badgeColor
                                )
                            }
                        }
                        Surface(
                            color = AeroCyan.copy(alpha = 0.15f),
                            shape = CircleShape
                        ) {
                            Text(
                                text = "VDOT ${"%.1f".format(currentVdot)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = AeroCyan,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Column {
                        Text(
                            text = "PERFORMANCE COCKPIT",
                            style = MaterialTheme.typography.labelMedium,
                            color = VoltLime
                        )
                        Text(
                            text = "${"%.1f".format(totalVolumeKm)} km Logged",
                            style = MaterialTheme.typography.headlineLarge,
                            color = Color.White
                        )
                        Text(
                            text = "Avg Cadence $avgCadence SPM • ${prs.size} Verified Distance PRs",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = onNavigateToOcr,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = KineticCoral,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                            modifier = Modifier.testTag("hero_scan_strava_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddAPhoto,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Strava AI OCR", style = MaterialTheme.typography.labelLarge)
                        }

                        OutlinedButton(
                            onClick = onNavigateToPredict,
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                            modifier = Modifier.testTag("hero_race_predictor_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                tint = VoltLime,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Race Predictor", style = MaterialTheme.typography.labelLarge, color = VoltLime)
                        }
                    }
                }
            }
        }

        // 2. Telemetry Gauges Row (VDOT Radial + Weekly Volume Bar)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // VDOT Card
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, SlateBorder, RoundedCornerShape(20.dp)),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SlateSurface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "AEROBIC VDOT",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(96.dp)
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val strokeWidth = 10.dp.toPx()
                                drawArc(
                                    color = SlateBorder,
                                    startAngle = 135f,
                                    sweepAngle = 270f,
                                    useCenter = false,
                                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                )
                                val progress = ((currentVdot - 30.0) / 50.0).toFloat().coerceIn(0.1f, 1f)
                                drawArc(
                                    color = VoltLime,
                                    startAngle = 135f,
                                    sweepAngle = 270f * progress,
                                    useCenter = false,
                                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "%.1f".format(currentVdot),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontFamily = TelemetryMonoFontFamily,
                                    color = VoltLime
                                )
                                Text(
                                    text = "ml/kg/min",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        val tenKPred = predictions.getOrNull(1)
                        Text(
                            text = "10K Est: ${tenKPred?.let { RacePredictionEngine.formatDuration(it.predictedTimeSeconds) } ?: "39:40"}",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White
                        )
                    }
                }

                // Weekly Mileage & Goal Progress Card
                Card(
                    modifier = Modifier
                        .weight(1.25f)
                        .border(1.dp, SlateBorder, RoundedCornerShape(20.dp)),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SlateSurface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "WEEKLY VOLUME",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = "Goal Reminder Active",
                                tint = AeroCyan,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${"%.1f".format(totalVolumeKm)} / $weeklyTargetKm km",
                            style = MaterialTheme.typography.titleLarge,
                            fontFamily = TelemetryMonoFontFamily,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        val progress = (totalVolumeKm.toFloat() / weeklyTargetKm.toFloat()).coerceIn(0f, 1f)
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(CircleShape),
                            color = AeroCyan,
                            trackColor = SlateBorder
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        // Mini Bar Sparkline of recent runs
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                        ) {
                            val recent = workouts.take(6).reversed()
                            val barCount = 6
                            val spacing = 8.dp.toPx()
                            val barWidth = (size.width - spacing * (barCount - 1)) / barCount
                            val maxKm = (recent.maxOfOrNull { it.distanceKm } ?: 21.1).coerceAtLeast(10.0)
                            for (i in 0 until barCount) {
                                val w = recent.getOrNull(i)
                                val ratio = if (w != null) (w.distanceKm / maxKm).toFloat().coerceIn(0.15f, 1f) else 0.12f
                                val barHeight = size.height * ratio
                                val x = i * (barWidth + spacing)
                                drawRoundRect(
                                    color = if (w != null) VoltLime else SlateBorder,
                                    topLeft = Offset(x, size.height - barHeight),
                                    size = Size(barWidth, barHeight),
                                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = activeGoal?.title ?: "Target: Sub-39:00 10K",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // 3. Comprehensive Cloud Backups & Multi-Device Sync Card
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
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = null,
                                tint = AeroCyan,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Cloud Backups & Multi-Device Sync",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White
                                )
                                Text(
                                    text = "${backups.size} cloud recovery checkpoints • Offline disk cache enabled",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val latestBackup = backups.firstOrNull()
                    if (latestBackup != null) {
                        Surface(
                            color = ObsidianBackground,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, SlateBorder, RoundedCornerShape(12.dp))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = latestBackup.backupLabel,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = VoltLime
                                    )
                                    val dateStr = latestBackup.createdAt?.toDate()?.let {
                                        SimpleDateFormat("MMM d, HH:mm", Locale.US).format(it)
                                    } ?: "Just now"
                                    Text(
                                        text = dateStr,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = latestBackup.summaryNotes,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                val timestampLabel = SimpleDateFormat("MMM d HH:mm", Locale.US).format(Date())
                                onCreateCloudBackup("Cloud Snapshot ($timestampLabel)")
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AeroCyan,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("create_backup_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Backup Now", style = MaterialTheme.typography.labelLarge)
                        }

                        OutlinedButton(
                            onClick = {
                                exportedJsonText = onExportBackupJson()
                                showExportDialog = true
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("export_backup_json_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Code,
                                contentDescription = null,
                                tint = VoltLime,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Inspect JSON", style = MaterialTheme.typography.labelLarge, color = VoltLime)
                        }
                    }
                }
            }
        }

        // 4. Logged Workouts Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RECENT TELEMETRY LOGS (${workouts.size})",
                    style = MaterialTheme.typography.labelLarge,
                    color = VoltLime
                )
                Text(
                    text = "Tap + OCR to scan Strava",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }

        when (workoutsState) {
            is UiState.Loading -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = VoltLime)
                    }
                }
            }
            is UiState.Error -> {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SlateSurface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = workoutsState.message,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
            is UiState.Success -> {
                if (workouts.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, SlateBorder, RoundedCornerShape(20.dp)),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = SlateSurface)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DirectionsRun,
                                    contentDescription = null,
                                    tint = VoltLime,
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No workouts logged yet",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White
                                )
                                Text(
                                    text = "Scan a Strava screenshot with Gemini AI to populate your telemetry dashboard.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                } else {
                    items(workouts, key = { it.id.ifBlank { it.title + it.durationSeconds } }) { workout ->
                        WorkoutTelemetryCard(
                            workout = workout,
                            onDelete = { if (workout.id.isNotBlank()) onDeleteWorkout(workout.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkoutTelemetryCard(
    workout: WorkoutLog,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(true) }
    val isStravaOcr = workout.sourceType == "STRAVA_OCR"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SlateBorder, RoundedCornerShape(20.dp))
            .clickable { expanded = !expanded }
            .testTag("workout_card_${workout.id}"),
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
                    Surface(
                        color = if (isStravaOcr) KineticCoral.copy(alpha = 0.2f) else AeroCyan.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (isStravaOcr) "STRAVA AI OCR" else "MANUAL LOG",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isStravaOcr) KineticCoral else AeroCyan,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = workout.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete Workout",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Primary Telemetry Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TelemetryMetricItem(
                    label = "DISTANCE",
                    value = "${"%.2f".format(workout.distanceKm)} km",
                    valueColor = VoltLime
                )
                TelemetryMetricItem(
                    label = "DURATION",
                    value = RacePredictionEngine.formatDuration(workout.durationSeconds),
                    valueColor = Color.White
                )
                TelemetryMetricItem(
                    label = "AVG PACE",
                    value = RacePredictionEngine.formatPace(workout.avgPaceSecondsPerKm),
                    valueColor = AeroCyan
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Secondary Physiological Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = KineticCoral,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${workout.avgHeartRate} bpm",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DirectionsRun,
                        contentDescription = null,
                        tint = VoltLime,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${workout.cadenceSpm} spm",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Terrain,
                        contentDescription = null,
                        tint = AeroCyan,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "+${workout.elevationGainM} m",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                }
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = SlateBorder)
                Spacer(modifier = Modifier.height(10.dp))

                if (workout.splitsSummary.isNotBlank()) {
                    Text(
                        text = "SPLITS: ${workout.splitsSummary}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }

                if (workout.aiCoachingInsight.isNotBlank()) {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = VoltLime,
                            modifier = Modifier
                                .size(16.dp)
                                .padding(top = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = workout.aiCoachingInsight,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TelemetryMetricItem(
    label: String,
    value: String,
    valueColor: Color
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontFamily = TelemetryMonoFontFamily,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
    }
}
