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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.CloudBackupSnapshot
import com.example.data.model.DistanceCategory
import com.example.data.model.PersonalRecord
import com.example.data.model.TrainingGoal
import com.example.data.model.WorkoutLog
import com.example.domain.GoalScheduleMetadata
import com.example.domain.GoalTrainingPlanEngine
import com.example.domain.RacePredictionEngine
import com.example.domain.ScheduledSessionStatus
import com.example.domain.ScheduledTrainingSession
import com.example.domain.SessionWorkoutType
import com.example.ui.components.StravaSplitsBarChart
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
import java.text.SimpleDateFormat
import java.util.Calendar
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
    onUpdateWorkout: (id: String, title: String, distanceKm: Double, durationSeconds: Long, avgHeartRate: Long, cadenceSpm: Long, elevationGainM: Long, workoutDateEpochMs: Long?) -> Unit,
    onAddGoal: (String, DistanceCategory, Long, Long, Boolean, Long, Long) -> Unit = { _, _, _, _, _, _, _ -> },
    onSaveGoalSchedule: (
        goalId: String?,
        cleanTitle: String,
        category: DistanceCategory,
        targetTimeSeconds: Long,
        weeklyMileageKm: Long,
        startDateKey: String,
        targetDateKey: String,
        trainingDaysIso: List<Int>
    ) -> Unit = { _, _, _, _, _, _, _, _ -> },
    onToggleGoalCompleted: (TrainingGoal) -> Unit = {},
    onDeleteGoal: (String) -> Unit = {},
    onAskGeminiAdvice: ((TrainingGoal) -> Unit)? = null,
    geminiAdviceText: String? = null,
    isGeminiLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val workouts = (workoutsState as? UiState.Success)?.data.orEmpty()
    val prs = (prsState as? UiState.Success)?.data.orEmpty()
    val goals = (goalsState as? UiState.Success)?.data.orEmpty()

    val totalVolumeKm = workouts.sumOf { it.distanceKm }
    val hasTelemetry = workouts.isNotEmpty() || prs.isNotEmpty()
    val predictions = remember(workouts, prs) {
        if (hasTelemetry) RacePredictionEngine.computeRacePredictions(workouts, prs, 1.06) else emptyList()
    }
    val currentVdot = predictions.firstOrNull()?.vdotScore
    val activeGoal = goals.firstOrNull { !it.completed } ?: goals.firstOrNull()

    // 1. Check if today is a scheduled training day for any active goal
    val todayTrainingSessionInfo = remember(goals, workouts) {
        goals.firstNotNullOfOrNull { goal ->
            if (goal.completed) return@firstNotNullOfOrNull null
            val sessions = GoalTrainingPlanEngine.generateScheduleForGoal(goal, workouts)
            val today = sessions.firstOrNull { it.status == ScheduledSessionStatus.TODAY_PENDING }
            if (today != null) Pair(goal, today) else null
        }
    }

    LaunchedEffect(todayTrainingSessionInfo) {
        todayTrainingSessionInfo?.let { (goal, session) ->
            GoalReminderNotificationHelper.notifyIfTodayIsTrainingDay(context, goal, session)
        }
    }

    var workoutToEdit by remember { mutableStateOf<WorkoutLog?>(null) }
    var selectedWorkoutKey by remember { mutableStateOf<String?>(null) }
    var showVdotDialog by remember { mutableStateOf(false) }
    var showGoalDialog by remember { mutableStateOf(false) }

    if (showVdotDialog) {
        VdotInfoDialog(onDismiss = { showVdotDialog = false })
    }

    if (showGoalDialog) {
        DashboardAddGoalDialog(
            onDismiss = { showGoalDialog = false },
            onConfirm = { title, cat, targetSec, weeklyKm, hour, min ->
                onAddGoal(title, cat, targetSec, weeklyKm, true, hour, min)
                showGoalDialog = false
            }
        )
    }

    workoutToEdit?.let { workout ->
        EditWorkoutDialog(
            workout = workout,
            onDismiss = { workoutToEdit = null },
            onConfirm = { updatedTitle, updatedDist, updatedDur, updatedHr, updatedCad, updatedElev, dateEpochMs ->
                onUpdateWorkout(workout.id, updatedTitle, updatedDist, updatedDur, updatedHr, updatedCad, updatedElev, dateEpochMs)
                workoutToEdit = null
            }
        )
    }

    val selectedWorkout = workouts.firstOrNull {
        it.id.ifBlank { it.title + it.durationSeconds } == selectedWorkoutKey
    }

    if (selectedWorkout != null) {
        WorkoutDetailScreen(
            workout = selectedWorkout,
            allWorkouts = workouts,
            prs = prs,
            onBack = { selectedWorkoutKey = null },
            onEditWorkout = { workoutToEdit = it },
            onDeleteWorkout = { id ->
                onDeleteWorkout(id)
                selectedWorkoutKey = null
            },
            modifier = modifier
        )
        return
    }

    var selectedGoalForScheduleScreen by remember { mutableStateOf<TrainingGoal?>(null) }
    var isCreatingNewGoalScreen by remember { mutableStateOf(false) }

    if (selectedGoalForScheduleScreen != null || isCreatingNewGoalScreen) {
        GoalTrainingScheduleScreen(
            goal = selectedGoalForScheduleScreen,
            workouts = workouts,
            onBack = {
                selectedGoalForScheduleScreen = null
                isCreatingNewGoalScreen = false
            },
            onSaveGoalSchedule = { goalId, cleanTitle, cat, targetSec, weeklyKm, startKey, targetKey, daysIso ->
                onSaveGoalSchedule(goalId, cleanTitle, cat, targetSec, weeklyKm, startKey, targetKey, daysIso)
                selectedGoalForScheduleScreen = null
                isCreatingNewGoalScreen = false
            },
            onNavigateToOcr = onNavigateToOcr,
            onAskGeminiAdvice = onAskGeminiAdvice,
            geminiAdviceText = geminiAdviceText,
            isGeminiLoading = isGeminiLoading,
            modifier = modifier
        )
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBackground)
            .testTag("dashboard_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 0. Banner Jadwal Latihan Hari Ini (jika ada sesi latihan hari ini)
        if (todayTrainingSessionInfo != null) {
            item {
                val (todayGoal, todaySession) = todayTrainingSessionInfo
                val todayMeta = remember(todayGoal) { GoalTrainingPlanEngine.parseGoalMetadata(todayGoal) }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { selectedGoalForScheduleScreen = todayGoal }
                        .border(1.5.dp, VoltLime, RoundedCornerShape(20.dp)),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SlateSurfaceElevated)
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
                                Surface(
                                    color = VoltLime,
                                    shape = CircleShape,
                                    modifier = Modifier.size(26.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.NotificationsActive,
                                            contentDescription = null,
                                            tint = Color.Black,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "HARI INI JADWAL LATIHAN!",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = VoltLime,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "• ${todaySession.displayDate}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary,
                                        maxLines = 1
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = AeroCyan.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, AeroCyan.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = todaySession.workoutType.shortBadge,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = AeroCyan,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    maxLines = 1
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "${todayMeta.cleanTitle} • ${todaySession.sessionTitle}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = todaySession.sessionDescription,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "TARGET PACE:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary,
                                    maxLines = 1
                                )
                                Text(
                                    text = "${RacePredictionEngine.formatPace(todaySession.targetPaceSecondsPerKm)}/km",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontFamily = TelemetryMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = VoltLime,
                                    maxLines = 1
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { selectedGoalForScheduleScreen = todayGoal },
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = null, tint = AeroCyan, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Jadwal", style = MaterialTheme.typography.labelSmall, color = AeroCyan, maxLines = 1)
                                }
                                Button(
                                    onClick = onNavigateToOcr,
                                    colors = ButtonDefaults.buttonColors(containerColor = VoltLime, contentColor = Color.Black),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AddAPhoto,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Scan", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 1. Clean Summary Card
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
                        Column {
                            Text(
                                text = "TOTAL DISTANCE",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                            Text(
                                text = "${"%.1f".format(totalVolumeKm)} km",
                                style = MaterialTheme.typography.headlineLarge,
                                fontFamily = TelemetryMonoFontFamily,
                                color = VoltLime
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            SummaryStatBadge(
                                label = "RUNS",
                                value = "${workouts.size}",
                                color = Color.White
                            )
                            SummaryStatBadge(
                                label = "PRs",
                                value = "${prs.size}",
                                color = AeroCyan
                            )
                            SummaryStatBadge(
                                label = "VDOT",
                                value = currentVdot?.let { "%.1f".format(it) } ?: "--",
                                color = VoltLime,
                                onClick = { showVdotDialog = true }
                            )
                        }
                    }

                    if (activeGoal != null) {
                        val activeMeta = remember(activeGoal) { GoalTrainingPlanEngine.parseGoalMetadata(activeGoal) }
                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = SlateBorder)
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = activeMeta.cleanTitle,
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${"%.1f".format(totalVolumeKm)} / ${activeGoal.weeklyMileageTargetKm} km/wk",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = TelemetryMonoFontFamily,
                                color = AeroCyan,
                                maxLines = 1
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        val progress = (totalVolumeKm.toFloat() / activeGoal.weeklyMileageTargetKm.coerceAtLeast(1L).toFloat()).coerceIn(0f, 1f)
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape),
                            color = AeroCyan,
                            trackColor = SlateBorder
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onNavigateToOcr,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = VoltLime,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("hero_scan_strava_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddAPhoto,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Scan Run", style = MaterialTheme.typography.labelMedium, maxLines = 1)
                        }

                        OutlinedButton(
                            onClick = onNavigateToPredict,
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("hero_race_predictor_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                tint = AeroCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("PR & Target", style = MaterialTheme.typography.labelMedium, color = AeroCyan, maxLines = 1)
                        }
                    }
                }
            }
        }

        // 2. Training Goals Section (Target Lari)
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
                                tint = AeroCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "TARGET LARI (${goals.size})",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                        }

                        OutlinedButton(
                            onClick = { isCreatingNewGoalScreen = true },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = AeroCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("+ Target", style = MaterialTheme.typography.labelMedium, color = AeroCyan)
                        }
                    }

                    if (goals.isEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Belum ada target lari aktif. Tetapkan target jarak (5K/10K/HM/FM) dan target mingguan untuk memotivasi latihan Anda.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    } else {
                        Spacer(modifier = Modifier.height(12.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            goals.forEach { goal ->
                                val meta = remember(goal) { GoalTrainingPlanEngine.parseGoalMetadata(goal) }
                                val sessions = remember(goal, workouts) { GoalTrainingPlanEngine.generateScheduleForGoal(goal, workouts) }
                                val completedCount = sessions.count { it.status == ScheduledSessionStatus.COMPLETED }
                                val totalSessions = sessions.size
                                val progressRatio = if (totalSessions > 0) {
                                    (completedCount.toFloat() / totalSessions.toFloat()).coerceIn(0f, 1f)
                                } else 0f

                                Surface(
                                    color = ObsidianBackground,
                                    shape = RoundedCornerShape(16.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .clickable { selectedGoalForScheduleScreen = goal }
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        // 1. Header Row (Menyamping): Checkbox + Badge Jarak + Nama Target (1 line) + Delete
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                IconButton(
                                                    onClick = { onToggleGoalCompleted(goal) },
                                                    modifier = Modifier.size(26.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = if (goal.completed) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                                        contentDescription = "Toggle Complete",
                                                        tint = if (goal.completed) SuccessGreen else VoltLime,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    color = VoltLime.copy(alpha = 0.15f),
                                                    shape = RoundedCornerShape(6.dp),
                                                    border = androidx.compose.foundation.BorderStroke(1.dp, VoltLime.copy(alpha = 0.35f))
                                                ) {
                                                    Text(
                                                        text = goal.targetDistanceCategory,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = VoltLime,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                        maxLines = 1
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = meta.cleanTitle,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }

                                            IconButton(
                                                onClick = { onDeleteGoal(goal.id) },
                                                modifier = Modifier.size(26.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.DeleteOutline,
                                                    contentDescription = "Hapus Target",
                                                    tint = TextSecondary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }

                                        // 2. Info Row (Menyamping): Tanggal Target & Estimasi H-X di kiri, Target Waktu di kanan
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
                                                    imageVector = Icons.Default.CalendarToday,
                                                    contentDescription = null,
                                                    tint = AeroCyan,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "${meta.targetDateDisplay} (${if (meta.daysRemaining >= 0) "H-${meta.daysRemaining}" else "Selesai"})",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = AeroCyan,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            if (goal.targetTimeSeconds > 0) {
                                                Text(
                                                    text = "Target ${RacePredictionEngine.formatDuration(goal.targetTimeSeconds)}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontFamily = TelemetryMonoFontFamily,
                                                    color = TextSecondary,
                                                    maxLines = 1
                                                )
                                            }
                                        }

                                        // 3. Simple Counter Row (Menyamping): Informasi sudah berapa kali lari dari jadwal (misal: 2/12 Sesi Lari)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Progress Sesi Latihan",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextSecondary,
                                                maxLines = 1
                                            )
                                            Text(
                                                text = "$completedCount/$totalSessions Sesi Lari",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontFamily = TelemetryMonoFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                color = if (completedCount >= totalSessions && totalSessions > 0) SuccessGreen else VoltLime,
                                                maxLines = 1
                                            )
                                        }

                                        LinearProgressIndicator(
                                            progress = { progressRatio },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(6.dp)
                                                .clip(CircleShape),
                                            color = if (completedCount >= totalSessions && totalSessions > 0) SuccessGreen else VoltLime,
                                            trackColor = SlateBorder
                                        )

                                        // 4. Quick Action Row: Langsung Intent ke Halaman Baru Jadwal & Kalender
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(SlateSurfaceElevated)
                                                .padding(horizontal = 10.dp, vertical = 7.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.CalendarMonth,
                                                    contentDescription = null,
                                                    tint = VoltLime,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Buka Kalender & Jadwal Latihan",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color.White,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                                contentDescription = null,
                                                tint = VoltLime,
                                                modifier = Modifier.size(15.dp)
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

        // 3. Workouts Header + Quick Backup Button
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "WORKOUT HISTORY (${workouts.size})",
                    style = MaterialTheme.typography.labelLarge,
                    color = VoltLime
                )
                if (workouts.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                val timestampLabel = SimpleDateFormat("MMM d HH:mm", Locale.US).format(Date())
                                onCreateCloudBackup("Backup ($timestampLabel)")
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("create_backup_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = null,
                            tint = AeroCyan,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Backup",
                            style = MaterialTheme.typography.labelSmall,
                            color = AeroCyan
                        )
                    }
                }
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
                                    .padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.DirectionsRun,
                                    contentDescription = null,
                                    tint = VoltLime,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Belum ada data lari",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Catat lari manual atau scan screenshot Strava untuk mulai melacak progres Anda.",
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
                            onOpenDetail = {
                                selectedWorkoutKey = workout.id.ifBlank { workout.title + workout.durationSeconds }
                            },
                            onEdit = { workoutToEdit = workout },
                            onDelete = { if (workout.id.isNotBlank()) onDeleteWorkout(workout.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryStatBadge(
    label: String,
    value: String,
    color: Color,
    onClick: (() -> Unit)? = null
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = if (onClick != null) {
            Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable { onClick() }
                .padding(horizontal = 4.dp, vertical = 2.dp)
        } else {
            Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
            if (onClick != null) {
                Spacer(modifier = Modifier.width(3.dp))
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Info $label",
                    tint = AeroCyan,
                    modifier = Modifier.size(11.dp)
                )
            }
        }
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontFamily = TelemetryMonoFontFamily,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun WorkoutTelemetryCard(
    workout: WorkoutLog,
    onOpenDetail: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isStravaOcr = workout.sourceType == "STRAVA_OCR"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SlateBorder, RoundedCornerShape(18.dp))
            .clickable { onOpenDetail() }
            .testTag("workout_card_${workout.id}"),
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
                    Surface(
                        color = if (isStravaOcr) KineticCoral.copy(alpha = 0.2f) else AeroCyan.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (isStravaOcr) "OCR" else "MANUAL",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isStravaOcr) KineticCoral else AeroCyan,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = workout.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("edit_workout_${workout.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Workout",
                            tint = AeroCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("delete_workout_${workout.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete Workout",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

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

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = SlateBorder.copy(alpha = 0.6f))
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = VoltLime,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Lihat Detail, Grafik Pace, PR & Evaluasi AI",
                        style = MaterialTheme.typography.labelSmall,
                        color = AeroCyan
                    )
                }
                Text(
                    text = "Buka →",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = VoltLime
                )
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

@Composable
private fun EditWorkoutDialog(
    workout: WorkoutLog,
    onDismiss: () -> Unit,
    onConfirm: (String, Double, Long, Long, Long, Long, Long?) -> Unit
) {
    var title by remember { mutableStateOf(workout.title) }
    var distanceKmText by remember { mutableStateOf(workout.distanceKm.toString()) }
    var minutesText by remember { mutableStateOf((workout.durationSeconds / 60).toString()) }
    var secondsText by remember { mutableStateOf((workout.durationSeconds % 60).toString()) }
    var hrText by remember { mutableStateOf(if (workout.avgHeartRate > 0) workout.avgHeartRate.toString() else "") }
    var cadenceText by remember { mutableStateOf(if (workout.cadenceSpm > 0) workout.cadenceSpm.toString() else "") }
    var elevationText by remember { mutableStateOf(if (workout.elevationGainM > 0) workout.elevationGainM.toString() else "") }
    var dateStr by remember { mutableStateOf(GoalTrainingPlanEngine.workoutDateKey(workout)) }

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
            Text("Edit Riwayat Lari", color = Color.White, style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Nama Sesi Lari") },
                    colors = fieldColors,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Workout Date Field & Quick Chips
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = dateStr,
                        onValueChange = { dateStr = it },
                        label = { Text("Tanggal Sesi (YYYY-MM-DD)") },
                        colors = fieldColors,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
                        val yesterdayStr = remember {
                            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
                            SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
                        }
                        FilterChip(
                            selected = dateStr == todayStr,
                            onClick = { dateStr = todayStr },
                            label = { Text("Hari Ini", style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = VoltLime.copy(alpha = 0.2f),
                                selectedLabelColor = VoltLime
                            )
                        )
                        FilterChip(
                            selected = dateStr == yesterdayStr,
                            onClick = { dateStr = yesterdayStr },
                            label = { Text("Kemarin", style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AeroCyan.copy(alpha = 0.2f),
                                selectedLabelColor = AeroCyan
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = distanceKmText,
                    onValueChange = { distanceKmText = it },
                    label = { Text("Jarak (km)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = fieldColors,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = minutesText,
                        onValueChange = { minutesText = it },
                        label = { Text("Menit") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = fieldColors,
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = secondsText,
                        onValueChange = { secondsText = it },
                        label = { Text("Detik") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = fieldColors,
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = hrText,
                        onValueChange = { hrText = it },
                        label = { Text("Avg HR (bpm)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = fieldColors,
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = cadenceText,
                        onValueChange = { cadenceText = it },
                        label = { Text("Cadence (spm)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = fieldColors,
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = elevationText,
                    onValueChange = { elevationText = it },
                    label = { Text("Elevation Gain (m)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = fieldColors,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val dist = distanceKmText.replace(',', '.').toDoubleOrNull()?.coerceIn(0.1, 500.0) ?: workout.distanceKm
                    val mins = minutesText.toLongOrNull()?.coerceAtLeast(0L) ?: 0L
                    val secs = secondsText.toLongOrNull()?.coerceIn(0L, 59L) ?: 0L
                    val totalSecs = (mins * 60L + secs).coerceAtLeast(1L)
                    val hr = hrText.toLongOrNull()?.coerceIn(0L, 260L) ?: 0L
                    val cad = cadenceText.toLongOrNull()?.coerceIn(0L, 300L) ?: 0L
                    val elev = elevationText.toLongOrNull()?.coerceIn(0L, 20000L) ?: 0L
                    val parsedDateEpoch = try {
                        SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateStr.trim())?.time
                    } catch (_: Exception) {
                        workout.createdAt?.toDate()?.time
                    }

                    onConfirm(
                        title.ifBlank { "Training Run" },
                        dist,
                        totalSecs,
                        hr,
                        cad,
                        elev,
                        parsedDateEpoch
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = VoltLime, contentColor = Color.Black)
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal", color = TextSecondary)
            }
        }
    )
}

@Composable
private fun DashboardAddGoalDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, DistanceCategory, Long, Long, Long, Long) -> Unit
) {
    var selectedCategory by remember { mutableStateOf(DistanceCategory.TEN_K) }
    var titleText by remember { mutableStateOf("") }
    var targetMinsText by remember { mutableStateOf("50") }
    var targetSecsText by remember { mutableStateOf("0") }
    var weeklyKmText by remember { mutableStateOf("30") }

    // Date target state
    var targetDateKey by remember { mutableStateOf(GoalTrainingPlanEngine.defaultTargetDateKey(21)) }

    // Training frequency & day selection state (default: 3x per week - Selasa, Kamis, Minggu)
    var selectedDays by remember { mutableStateOf(listOf(2, 4, 7)) }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = VoltLime,
        unfocusedBorderColor = SlateBorder,
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White
    )

    val isValid = (targetMinsText.toLongOrNull() ?: 0L) > 0L && selectedDays.isNotEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SlateSurfaceElevated,
        title = {
            Text("Buat Target Lari & Jadwal Baru", color = Color.White, style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Category Chips
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DistanceCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = {
                                selectedCategory = cat
                                when (cat) {
                                    DistanceCategory.FIVE_K -> {
                                        targetMinsText = "24"
                                        weeklyKmText = "20"
                                    }
                                    DistanceCategory.TEN_K -> {
                                        targetMinsText = "50"
                                        weeklyKmText = "30"
                                    }
                                    DistanceCategory.HALF_MARATHON -> {
                                        targetMinsText = "115"
                                        weeklyKmText = "45"
                                    }
                                    DistanceCategory.MARATHON -> {
                                        targetMinsText = "240"
                                        weeklyKmText = "60"
                                    }
                                }
                            },
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
                    label = { Text("Nama Target (contoh: Astra Run 10K)") },
                    placeholder = { Text("Astra Run ${selectedCategory.shortLabel}") },
                    colors = fieldColors,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Target Event Date (YYYY-MM-DD)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = targetDateKey,
                        onValueChange = { targetDateKey = it },
                        label = { Text("Tanggal Target Lomba (YYYY-MM-DD)") },
                        colors = fieldColors,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Quick 29th chip
                        val date29Str = remember {
                            val cal = Calendar.getInstance()
                            if (cal.get(Calendar.DAY_OF_MONTH) > 29) {
                                cal.add(Calendar.MONTH, 1)
                            }
                            cal.set(Calendar.DAY_OF_MONTH, 29)
                            SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
                        }
                        FilterChip(
                            selected = targetDateKey == date29Str,
                            onClick = { targetDateKey = date29Str },
                            label = { Text("Tgl 29", style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = VoltLime.copy(alpha = 0.25f),
                                selectedLabelColor = VoltLime
                            )
                        )
                        FilterChip(
                            selected = targetDateKey == GoalTrainingPlanEngine.defaultTargetDateKey(14),
                            onClick = { targetDateKey = GoalTrainingPlanEngine.defaultTargetDateKey(14) },
                            label = { Text("+2 Mgg", style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AeroCyan.copy(alpha = 0.2f),
                                selectedLabelColor = AeroCyan
                            )
                        )
                        FilterChip(
                            selected = targetDateKey == GoalTrainingPlanEngine.defaultTargetDateKey(21),
                            onClick = { targetDateKey = GoalTrainingPlanEngine.defaultTargetDateKey(21) },
                            label = { Text("+3 Mgg", style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AeroCyan.copy(alpha = 0.2f),
                                selectedLabelColor = AeroCyan
                            )
                        )
                        FilterChip(
                            selected = targetDateKey == GoalTrainingPlanEngine.defaultTargetDateKey(28),
                            onClick = { targetDateKey = GoalTrainingPlanEngine.defaultTargetDateKey(28) },
                            label = { Text("+4 Mgg", style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AeroCyan.copy(alpha = 0.2f),
                                selectedLabelColor = AeroCyan
                            )
                        )
                    }
                }

                // Weekly Training Days Selection
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Jadwal Hari Latihan (${selectedDays.size}x per minggu):",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = VoltLime
                    )

                    // Presets Row
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = selectedDays == listOf(2, 4, 7),
                            onClick = { selectedDays = listOf(2, 4, 7) },
                            label = { Text("3x: Sel, Kam, Min (Rekomendasi)", style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = VoltLime.copy(alpha = 0.25f),
                                selectedLabelColor = VoltLime
                            )
                        )
                        FilterChip(
                            selected = selectedDays == listOf(2, 4, 6, 7),
                            onClick = { selectedDays = listOf(2, 4, 6, 7) },
                            label = { Text("4x: Sel, Kam, Sab, Min", style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AeroCyan.copy(alpha = 0.2f),
                                selectedLabelColor = AeroCyan
                            )
                        )
                    }

                    // Individual Days Toggle
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        (1..7).forEach { isoDay ->
                            val isSelected = isoDay in selectedDays
                            Surface(
                                color = if (isSelected) VoltLime.copy(alpha = 0.25f) else SlateSurface,
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) VoltLime else SlateBorder
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        selectedDays = if (isSelected) {
                                            if (selectedDays.size > 1) selectedDays - isoDay else selectedDays
                                        } else {
                                            (selectedDays + isoDay).sorted()
                                        }
                                    }
                            ) {
                                Text(
                                    text = GoalTrainingPlanEngine.dayShortNameId(isoDay),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) VoltLime else TextSecondary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    // Live Blueprint Preview
                    Surface(
                        color = ObsidianBackground,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = "Distribusi Sesi Latihan:",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                            selectedDays.forEachIndexed { idx, isoDay ->
                                val wType = GoalTrainingPlanEngine.assignWorkoutTypeForDayIndex(idx, selectedDays.size)
                                Text(
                                    text = "• ${GoalTrainingPlanEngine.dayNameId(isoDay)}: ${wType.displayName}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = when (wType) {
                                        SessionWorkoutType.INTERVAL -> VoltLime
                                        SessionWorkoutType.TEMPO -> AeroCyan
                                        SessionWorkoutType.RECOVERY -> SuccessGreen
                                        SessionWorkoutType.EASY_LONG -> Color.White
                                        SessionWorkoutType.RACE_DAY -> KineticCoral
                                        else -> Color.White
                                    }
                                )
                            }
                        }
                    }
                }

                // Target Finish Time (Menit & Detik)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = targetMinsText,
                        onValueChange = { targetMinsText = it },
                        label = { Text("Target Menit") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = fieldColors,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = targetSecsText,
                        onValueChange = { targetSecsText = it },
                        label = { Text("Detik") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = fieldColors,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = weeklyKmText,
                    onValueChange = { weeklyKmText = it },
                    label = { Text("Target Jarak Mingguan (km)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = fieldColors,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val mins = targetMinsText.toLongOrNull()?.coerceAtLeast(1L) ?: 30L
                    val secs = targetSecsText.toLongOrNull()?.coerceIn(0L, 59L) ?: 0L
                    val wkKm = weeklyKmText.toLongOrNull()?.coerceIn(1L, 350L) ?: 30L
                    val baseTitle = titleText.ifBlank { "Target ${selectedCategory.shortLabel}" }
                    val encodedTitle = GoalTrainingPlanEngine.encodeGoalTitleWithSchedule(
                        cleanTitle = baseTitle,
                        targetDateKey = targetDateKey,
                        trainingDaysIso = selectedDays
                    )
                    onConfirm(
                        encodedTitle,
                        selectedCategory,
                        mins * 60L + secs,
                        wkKm,
                        7L,
                        30L
                    )
                },
                enabled = isValid,
                colors = ButtonDefaults.buttonColors(containerColor = VoltLime, contentColor = Color.Black)
            ) {
                Text("Simpan Target & Jadwal")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal", color = TextSecondary)
            }
        }
    )
}
