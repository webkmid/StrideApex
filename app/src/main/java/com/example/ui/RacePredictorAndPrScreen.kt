package com.example.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VoltLime
import com.example.util.GoalReminderNotificationHelper
import com.example.viewmodel.UiState

@Composable
fun RacePredictorAndPrScreen(
    workoutsState: UiState<List<WorkoutLog>>,
    prsState: UiState<List<PersonalRecord>>,
    goalsState: UiState<List<TrainingGoal>>,
    fatigueExponent: Double,
    onFatigueExponentChange: (Double) -> Unit,
    onUpsertPrManual: (DistanceCategory, Long, String) -> Unit,
    onAddGoal: (String, DistanceCategory, Long, Long, Boolean, Long, Long) -> Unit,
    onToggleGoalCompleted: (TrainingGoal) -> Unit,
    onDeleteGoal: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val workouts = (workoutsState as? UiState.Success)?.data.orEmpty()
    val prs = (prsState as? UiState.Success)?.data.orEmpty()
    val goals = (goalsState as? UiState.Success)?.data.orEmpty()

    val predictions = remember(workouts, prs, fatigueExponent) {
        RacePredictionEngine.computeRacePredictions(workouts, prs, fatigueExponent)
    }
    val tenKPace = predictions.firstOrNull { it.category == DistanceCategory.TEN_K }?.targetPaceSecondsPerKm ?: 238L
    val paceZones = remember(tenKPace) {
        RacePredictionEngine.computeTrainingPaceZones(tenKPace)
    }
    val totalWeeklyKm = workouts.sumOf { it.distanceKm }

    var showPrDialog by remember { mutableStateOf(false) }
    var showGoalDialog by remember { mutableStateOf(false) }
    var pushFeedbackMessage by remember { mutableStateOf<String?>(null) }
    var pendingNotificationGoal by remember { mutableStateOf<TrainingGoal?>(null) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        val goal = pendingNotificationGoal
        if (isGranted && goal != null) {
            val sent = GoalReminderNotificationHelper.sendGoalPushReminder(context, goal, totalWeeklyKm)
            pushFeedbackMessage = if (sent) {
                "Goal push notification dispatched for '${goal.title}'!"
            } else {
                "Reminder logged in-app for '${goal.title}'."
            }
        } else {
            pushFeedbackMessage = "In-app Goal Push Reminder active (enable system notifications for status bar alerts)."
        }
    }

    if (showPrDialog) {
        AddOrUpdatePrDialog(
            onDismiss = { showPrDialog = false },
            onConfirm = { cat, totalSec, title ->
                onUpsertPrManual(cat, totalSec, title)
                showPrDialog = false
            }
        )
    }

    if (showGoalDialog) {
        AddGoalDialog(
            onDismiss = { showGoalDialog = false },
            onConfirm = { title, cat, targetSec, weeklyKm, hour, min ->
                onAddGoal(title, cat, targetSec, weeklyKm, true, hour, min)
                showGoalDialog = false
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBackground)
            .testTag("predict_prs_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Algorithmic Race Predictor Card
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
                            Column {
                                Text(
                                    text = "Algorithmic Race Predictions",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.White
                                )
                                Text(
                                    text = "Riegel Endurance Power-Law + VDOT Model",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                        }
                        Surface(
                            color = VoltLime.copy(alpha = 0.16f),
                            shape = CircleShape
                        ) {
                            val vdot = predictions.firstOrNull()?.vdotScore ?: 52.0
                            Text(
                                text = "VDOT ${"%.1f".format(vdot)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = VoltLime,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Interactive Fatigue Exponent Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Endurance Fatigue Exponent (c)",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White
                        )
                        Text(
                            text = "%.2f".format(fatigueExponent),
                            style = MaterialTheme.typography.labelLarge,
                            fontFamily = TelemetryMonoFontFamily,
                            color = AeroCyan
                        )
                    }
                    Slider(
                        value = fatigueExponent.toFloat(),
                        onValueChange = { onFatigueExponentChange(it.toDouble()) },
                        valueRange = 1.03f..1.10f,
                        colors = SliderDefaults.colors(
                            thumbColor = VoltLime,
                            activeTrackColor = VoltLime,
                            inactiveTrackColor = SlateBorder
                        ),
                        modifier = Modifier.testTag("fatigue_exponent_slider")
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("1.03 High Mileage Endurance", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Text("1.06 Standard", style = MaterialTheme.typography.labelSmall, color = VoltLime)
                        Text("1.10 Speed Bias", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 4 Race Distance Cards (5K, 10K, Half, Full Marathon)
                    predictions.forEach { pred ->
                        Surface(
                            color = ObsidianBackground,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .border(1.dp, SlateBorder, RoundedCornerShape(14.dp))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            color = VoltLime.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = pred.category.shortLabel,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = VoltLime,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = pred.category.displayName,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = Color.White
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = pred.confidenceNote,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = RacePredictionEngine.formatDuration(pred.predictedTimeSeconds),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontFamily = TelemetryMonoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        color = VoltLime
                                    )
                                    Text(
                                        text = RacePredictionEngine.formatPace(pred.targetPaceSecondsPerKm),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = AeroCyan
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. VDOT Physiological Training Pace Zones
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
                        text = "VDOT PHYSIOLOGICAL TRAINING ZONES",
                        style = MaterialTheme.typography.labelLarge,
                        color = AeroCyan
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    paceZones.forEachIndexed { idx, zone ->
                        if (idx > 0) {
                            HorizontalDivider(
                                color = SlateBorder,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
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
                                    color = AeroCyan.copy(alpha = 0.2f),
                                    shape = CircleShape,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = zone.code,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = AeroCyan
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = zone.zoneName,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White
                                    )
                                    Text(
                                        text = zone.physiologicalBenefit,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = zone.paceRangePerKm,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = VoltLime
                                )
                                Text(
                                    text = zone.heartRatePctRange,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = KineticCoral
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Personal Record (PR) Trophy Vault
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
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = VoltLime,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Personal Record (PR) Vault",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White
                            )
                        }
                        OutlinedButton(
                            onClick = { showPrDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("open_pr_dialog_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = VoltLime,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Log PR", style = MaterialTheme.typography.labelLarge, color = VoltLime)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    DistanceCategory.entries.forEach { category ->
                        val record = prs.firstOrNull { it.distanceCategory == category.key }
                        Surface(
                            color = ObsidianBackground,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .border(1.dp, SlateBorder, RoundedCornerShape(14.dp))
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
                                        color = Color.White
                                    )
                                    Text(
                                        text = record?.workoutTitle ?: "Not logged yet — scan a Strava run or add manually",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary
                                    )
                                }

                                if (record != null) {
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
                                                    imageVector = Icons.Default.TrendingDown,
                                                    contentDescription = null,
                                                    tint = SuccessGreen,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text(
                                                    text = "-${delta}s PR drop",
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

        // 4. Goal Push Reminders Section
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
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = KineticCoral,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Race Goals & Push Reminders",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White
                            )
                        }

                        OutlinedButton(
                            onClick = { showGoalDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("open_goal_dialog_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = AeroCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New Goal", style = MaterialTheme.typography.labelLarge, color = AeroCyan)
                        }
                    }

                    if (!pushFeedbackMessage.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            color = SuccessGreen.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = pushFeedbackMessage!!,
                                style = MaterialTheme.typography.bodyMedium,
                                color = SuccessGreen,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            }
        }

        items(goals, key = { it.id.ifBlank { it.title } }) { goal ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SlateBorder, RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = SlateSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            IconButton(onClick = { onToggleGoalCompleted(goal) }) {
                                Icon(
                                    imageVector = if (goal.completed) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                    contentDescription = "Toggle Goal Complete",
                                    tint = if (goal.completed) SuccessGreen else VoltLime
                                )
                            }
                            Column {
                                Text(
                                    text = goal.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White
                                )
                                Text(
                                    text = "${goal.targetDistanceCategory} Target: ${RacePredictionEngine.formatDuration(goal.targetTimeSeconds)} • ${goal.weeklyMileageTargetKm} km/wk",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = AeroCyan
                                )
                            }
                        }

                        if (goal.id.isNotBlank()) {
                            IconButton(onClick = { onDeleteGoal(goal.id) }) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Delete Goal",
                                    tint = TextSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Daily Push Reminder @ %02d:%02d".format(goal.reminderHour, goal.reminderMinute),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )

                        Button(
                            onClick = {
                                pendingNotificationGoal = goal
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                    !GoalReminderNotificationHelper.hasNotificationPermission(context)
                                ) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    val sent = GoalReminderNotificationHelper.sendGoalPushReminder(
                                        context = context,
                                        goal = goal,
                                        currentWeeklyKm = totalWeeklyKm
                                    )
                                    pushFeedbackMessage = if (sent) {
                                        "Push notification sent for '${goal.title}'!"
                                    } else {
                                        "Goal Push Reminder triggered for '${goal.title}'!"
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = KineticCoral,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("send_goal_push_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Send Push Alert", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddOrUpdatePrDialog(
    onDismiss: () -> Unit,
    onConfirm: (DistanceCategory, Long, String) -> Unit
) {
    var selectedCategory by remember { mutableStateOf(DistanceCategory.FIVE_K) }
    var minutesText by remember { mutableStateOf("18") }
    var secondsText by remember { mutableStateOf("15") }
    var raceTitleText by remember { mutableStateOf("City Championship PR") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SlateSurfaceElevated,
        title = {
            Text("Log Personal Record (PR)", color = Color.White)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DistanceCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat.shortLabel) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = VoltLime.copy(alpha = 0.25f),
                                selectedLabelColor = VoltLime
                            )
                        )
                    }
                }
                OutlinedTextField(
                    value = raceTitleText,
                    onValueChange = { raceTitleText = it },
                    label = { Text("Race / Workout Name") },
                    singleLine = true
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = minutesText,
                        onValueChange = { minutesText = it },
                        label = { Text("Total Mins") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = secondsText,
                        onValueChange = { secondsText = it },
                        label = { Text("Seconds") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val mins = minutesText.toLongOrNull()?.coerceAtLeast(1L) ?: 20L
                    val secs = secondsText.toLongOrNull()?.coerceIn(0L, 59L) ?: 0L
                    onConfirm(selectedCategory, mins * 60L + secs, raceTitleText)
                },
                colors = ButtonDefaults.buttonColors(containerColor = VoltLime, contentColor = Color.Black)
            ) {
                Text("Save PR")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

@Composable
private fun AddGoalDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, DistanceCategory, Long, Long, Long, Long) -> Unit
) {
    var selectedCategory by remember { mutableStateOf(DistanceCategory.TEN_K) }
    var titleText by remember { mutableStateOf("Autumn Sub-38:30 10K") }
    var targetMinsText by remember { mutableStateOf("38") }
    var targetSecsText by remember { mutableStateOf("30") }
    var weeklyKmText by remember { mutableStateOf("70") }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = VoltLime,
        unfocusedBorderColor = SlateBorder,
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SlateSurfaceElevated,
        title = {
            Text("Create Race & Volume Goal", color = Color.White)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DistanceCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat.shortLabel) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AeroCyan.copy(alpha = 0.25f),
                                selectedLabelColor = AeroCyan
                            )
                        )
                    }
                }
                OutlinedTextField(
                    value = titleText,
                    onValueChange = { titleText = it },
                    label = { Text("Goal Title") },
                    colors = fieldColors,
                    singleLine = true
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = targetMinsText,
                        onValueChange = { targetMinsText = it },
                        label = { Text("Target Mins") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = fieldColors,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = targetSecsText,
                        onValueChange = { targetSecsText = it },
                        label = { Text("Target Secs") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = fieldColors,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = weeklyKmText,
                    onValueChange = { weeklyKmText = it },
                    label = { Text("Weekly Mileage Target (km)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = fieldColors,
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val mins = targetMinsText.toLongOrNull()?.coerceAtLeast(1L) ?: 40L
                    val secs = targetSecsText.toLongOrNull()?.coerceIn(0L, 59L) ?: 0L
                    val wkKm = weeklyKmText.toLongOrNull()?.coerceIn(5L, 350L) ?: 60L
                    onConfirm(titleText, selectedCategory, mins * 60L + secs, wkKm, 7L, 30L)
                },
                colors = ButtonDefaults.buttonColors(containerColor = VoltLime, contentColor = Color.Black)
            ) {
                Text("Save Goal")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}
