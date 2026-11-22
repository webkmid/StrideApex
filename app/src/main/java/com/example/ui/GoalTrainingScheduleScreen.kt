package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DistanceCategory
import com.example.data.model.TrainingGoal
import com.example.data.model.WorkoutLog
import com.example.domain.GoalScheduleMetadata
import com.example.domain.GoalTrainingPlanEngine
import com.example.domain.RacePredictionEngine
import com.example.domain.ScheduledSessionStatus
import com.example.domain.ScheduledTrainingSession
import com.example.domain.SessionWorkoutType
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
import com.example.ui.theme.WarningAmber
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class MonthGridInfo(
    val year: Int,
    val monthZeroBased: Int,
    val monthTitle: String, // e.g. "November 2026"
    val daysInMonth: Int,
    val firstDayOffsetIso: Int // 0 for Senin, 1 for Sel ... 6 for Min
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalTrainingScheduleScreen(
    goal: TrainingGoal?,
    workouts: List<WorkoutLog>,
    onBack: () -> Unit,
    onSaveGoalSchedule: (
        goalId: String?,
        cleanTitle: String,
        category: DistanceCategory,
        targetTimeSeconds: Long,
        weeklyMileageKm: Long,
        startDateKey: String,
        targetDateKey: String,
        trainingDaysIso: List<Int>
    ) -> Unit,
    onNavigateToOcr: () -> Unit,
    onAskGeminiAdvice: ((TrainingGoal) -> Unit)? = null,
    geminiAdviceText: String? = null,
    isGeminiLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val todayKey = remember { GoalTrainingPlanEngine.todayKey() }
    val initialMeta = remember(goal) {
        if (goal != null) {
            GoalTrainingPlanEngine.parseGoalMetadata(goal)
        } else {
            GoalScheduleMetadata(
                cleanTitle = "",
                startDateKey = todayKey,
                startDateDisplay = GoalTrainingPlanEngine.formatShortDate(todayKey),
                targetDateKey = GoalTrainingPlanEngine.defaultTargetDateKey(21),
                targetDateDisplay = GoalTrainingPlanEngine.formatShortDate(GoalTrainingPlanEngine.defaultTargetDateKey(21)),
                daysRemaining = 21,
                trainingDaysIso = listOf(2, 4, 7) // Default 3x: Sel, Kam, Min
            )
        }
    }

    // Editable Form States
    var cleanTitle by remember { mutableStateOf(initialMeta.cleanTitle) }
    var selectedCategory by remember {
        mutableStateOf(if (goal != null) DistanceCategory.fromKey(goal.targetDistanceCategory) else DistanceCategory.TEN_K)
    }
    var targetMinsText by remember {
        val totalSec = goal?.targetTimeSeconds ?: 3000L
        mutableStateOf((totalSec / 60).toString())
    }
    var targetSecsText by remember {
        val totalSec = goal?.targetTimeSeconds ?: 3000L
        mutableStateOf((totalSec % 60).toString())
    }
    var weeklyKmText by remember {
        mutableStateOf((goal?.weeklyMileageTargetKm ?: 30L).toString())
    }

    // Date Range: Start Date (Default: sysdate / today) to Target Date
    var startDateKey by remember { mutableStateOf(initialMeta.startDateKey.ifBlank { todayKey }) }
    var targetDateKey by remember { mutableStateOf(initialMeta.targetDateKey) }
    var selectedDays by remember { mutableStateOf(initialMeta.trainingDaysIso) }

    var isSettingsExpanded by remember { mutableStateOf(goal == null) }

    // Reconstruct temporary goal to preview the adaptive training plan dynamically
    val previewGoal = remember(
        cleanTitle,
        selectedCategory,
        targetMinsText,
        targetSecsText,
        weeklyKmText,
        startDateKey,
        targetDateKey,
        selectedDays,
        goal
    ) {
        val m = targetMinsText.toLongOrNull() ?: 50L
        val s = targetSecsText.toLongOrNull() ?: 0L
        val wk = weeklyKmText.toLongOrNull() ?: 30L
        val encodedTitle = GoalTrainingPlanEngine.encodeGoalTitleWithSchedule(
            cleanTitle = cleanTitle,
            targetDateKey = targetDateKey,
            trainingDaysIso = selectedDays,
            startDateKey = startDateKey
        )
        TrainingGoal(
            id = goal?.id ?: "new_goal_preview",
            userId = goal?.userId ?: "local_user",
            title = encodedTitle,
            targetDistanceCategory = selectedCategory.key,
            targetTimeSeconds = m * 60L + s,
            weeklyMileageTargetKm = wk,
            createdAt = goal?.createdAt
        )
    }

    // Generate Adaptive AI Training Sessions based on current settings and actual uploaded workouts!
    val sessions = remember(previewGoal, workouts) {
        GoalTrainingPlanEngine.generateScheduleForGoal(previewGoal, workouts)
    }

    val sessionsByDate = remember(sessions) {
        sessions.associateBy { it.dateKey }
    }

    // Default selected date: today if it has a session, or next upcoming session, or first session
    var selectedDateKey by remember {
        val todaySession = sessions.firstOrNull { it.status == ScheduledSessionStatus.TODAY_PENDING }
        val upcomingSession = sessions.firstOrNull { it.status == ScheduledSessionStatus.UPCOMING }
        val initial = todaySession?.dateKey ?: upcomingSession?.dateKey ?: sessions.firstOrNull()?.dateKey ?: todayKey
        mutableStateOf(initial)
    }

    val selectedSession = sessionsByDate[selectedDateKey] ?: sessions.firstOrNull()
    var sessionForQuickInfoDialog by remember { mutableStateOf<ScheduledTrainingSession?>(null) }

    // Build the list of months between startDate and targetDate
    val monthsInPlan = remember(startDateKey, targetDateKey) {
        computeMonthGrids(startDateKey, targetDateKey)
    }

    // Summary counts
    val completedCount = sessions.count { it.status == ScheduledSessionStatus.COMPLETED }
    val todayPendingCount = sessions.count { it.status == ScheduledSessionStatus.TODAY_PENDING }
    val skippedCount = sessions.count { it.status == ScheduledSessionStatus.SKIPPED }
    val upcomingCount = sessions.count { it.status == ScheduledSessionStatus.UPCOMING }
    val daysRemaining = remember(targetDateKey) {
        GoalTrainingPlanEngine.daysBetween(todayKey, targetDateKey).toInt()
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = VoltLime,
        unfocusedBorderColor = SlateBorder,
        focusedTextColor = TextPrimary,
        unfocusedTextColor = TextPrimary,
        cursorColor = VoltLime
    )

    val context = LocalContext.current
    val showDatePickerDialog = { initialKey: String, onDateSelected: (String) -> Unit ->
        val cal = Calendar.getInstance()
        try {
            val parts = initialKey.split("-")
            if (parts.size == 3) {
                cal.set(Calendar.YEAR, parts[0].toInt())
                cal.set(Calendar.MONTH, parts[1].toInt() - 1)
                cal.set(Calendar.DAY_OF_MONTH, parts[2].toInt())
            }
        } catch (_: Exception) {}

        android.app.DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val formatted = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth)
                onDateSelected(formatted)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBackground)
    ) {
        // Custom Integrated Top Bar (No nested Scaffold insets / double padding)
        Surface(
            color = SlateSurface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali ke Dashboard",
                            tint = TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = cleanTitle.ifBlank { "Jadwal Target Lari" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${selectedCategory.shortLabel} • Target ${targetMinsText}m ${targetSecsText}s • H-$daysRemaining",
                            style = MaterialTheme.typography.labelSmall,
                            color = AeroCyan,
                            maxLines = 1
                        )
                    }
                }
                IconButton(
                    onClick = { isSettingsExpanded = !isSettingsExpanded }
                ) {
                    Icon(
                        imageVector = if (isSettingsExpanded) Icons.Default.EditCalendar else Icons.Default.CalendarMonth,
                        contentDescription = "Pengaturan Jadwal",
                        tint = VoltLime
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Settings Card: DatePicker, Start Date (sysdate), Target Date, Frequency
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, if (isSettingsExpanded) VoltLime else SlateBorder, RoundedCornerShape(20.dp)),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SlateSurface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(
                                    role = Role.Button,
                                    onClickLabel = if (isSettingsExpanded) "Tutup Pengaturan Tanggal" else "Buka Pengaturan Tanggal"
                                ) { isSettingsExpanded = !isSettingsExpanded },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = null,
                                    tint = VoltLime,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "PENGATURAN TANGGAL & JADWAL LARI",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = VoltLime
                                )
                            }
                            Icon(
                                imageVector = if (isSettingsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = TextSecondary
                            )
                        }

                        AnimatedVisibility(visible = isSettingsExpanded) {
                            Column(
                                modifier = Modifier.padding(top = 14.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Nama Target Lari
                                OutlinedTextField(
                                    value = cleanTitle,
                                    onValueChange = { cleanTitle = it },
                                    label = { Text("Nama Target") },
                                    placeholder = { Text("Contoh: Target Lari 10K") },
                                    colors = fieldColors,
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                // Category Selection
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

                                // START DATE (Default: sysdate / hari ini) with Date Picker
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = "Tanggal Mulai",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary
                                    )
                                    Surface(
                                        color = ObsidianBackground,
                                        shape = RoundedCornerShape(12.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable(
                                                role = Role.Button,
                                                onClickLabel = "Pilih Tanggal Mulai Latihan"
                                            ) {
                                                showDatePickerDialog(startDateKey) { startDateKey = it }
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 14.dp, vertical = 12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = "TANGGAL MULAI",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = TextSecondary,
                                                    fontSize = 10.sp
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = GoalTrainingPlanEngine.formatShortDate(startDateKey),
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextPrimary
                                                )
                                            }
                                            Surface(
                                                color = VoltLime.copy(alpha = 0.2f),
                                                shape = CircleShape,
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Default.CalendarMonth,
                                                        contentDescription = "Pilih Tanggal Mulai",
                                                        tint = VoltLime,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // TARGET DATE (Hari Target Lomba) with Date Picker
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = "Tanggal Target",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary
                                    )
                                    Surface(
                                        color = ObsidianBackground,
                                        shape = RoundedCornerShape(12.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable(
                                                role = Role.Button,
                                                onClickLabel = "Pilih Tanggal Target Lomba"
                                            ) {
                                                showDatePickerDialog(targetDateKey) { targetDateKey = it }
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 14.dp, vertical = 12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = "TANGGAL TARGET",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = TextSecondary,
                                                    fontSize = 10.sp
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = GoalTrainingPlanEngine.formatShortDate(targetDateKey),
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextPrimary
                                                )
                                            }
                                            Surface(
                                                color = AeroCyan.copy(alpha = 0.2f),
                                                shape = CircleShape,
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Default.CalendarMonth,
                                                        contentDescription = "Pilih Tanggal Target",
                                                        tint = AeroCyan,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // Frekuensi & Hari Latihan per Minggu
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = "Hari Latihan (${selectedDays.size}x/minggu):",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary
                                    )

                                    // Toggle Hari Individu
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        (1..7).forEach { isoDay ->
                                            val isSelected = isoDay in selectedDays
                                            val dayName = GoalTrainingPlanEngine.dayNameId(isoDay)
                                            Surface(
                                                color = if (isSelected) VoltLime.copy(alpha = 0.25f) else SlateSurfaceElevated,
                                                shape = RoundedCornerShape(8.dp),
                                                border = androidx.compose.foundation.BorderStroke(
                                                    1.dp,
                                                    if (isSelected) VoltLime else SlateBorder
                                                ),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .defaultMinSize(minHeight = 44.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .clickable(
                                                        role = Role.Checkbox,
                                                        onClickLabel = "Pilih hari $dayName"
                                                    ) {
                                                        selectedDays = if (isSelected) {
                                                            if (selectedDays.size > 1) selectedDays - isoDay else selectedDays
                                                        } else {
                                                            (selectedDays + isoDay).sorted()
                                                        }
                                                    }
                                            ) {
                                                Box(
                                                    contentAlignment = Alignment.Center,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .defaultMinSize(minHeight = 44.dp)
                                                        .padding(vertical = 10.dp)
                                                ) {
                                                    Text(
                                                        text = GoalTrainingPlanEngine.dayShortNameId(isoDay),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontSize = 11.sp,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (isSelected) VoltLime else TextSecondary,
                                                        textAlign = TextAlign.Center,
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // Target Time & Weekly Km
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
                                    OutlinedTextField(
                                        value = weeklyKmText,
                                        onValueChange = { weeklyKmText = it },
                                        label = { Text("Target km/mgg") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        colors = fieldColors,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Button(
                                    onClick = {
                                        val m = targetMinsText.toLongOrNull()?.coerceAtLeast(1L) ?: 30L
                                        val s = targetSecsText.toLongOrNull()?.coerceIn(0L, 59L) ?: 0L
                                        val wk = weeklyKmText.toLongOrNull()?.coerceIn(1L, 350L) ?: 30L
                                        onSaveGoalSchedule(
                                            goal?.id,
                                            cleanTitle.ifBlank { "Target ${selectedCategory.shortLabel}" },
                                            selectedCategory,
                                            m * 60L + s,
                                            wk,
                                            startDateKey,
                                            targetDateKey,
                                            selectedDays
                                        )
                                        isSettingsExpanded = false
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = VoltLime, contentColor = Color.Black),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Simpan & Perbarui Kalender Latihan", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // 2. Periodization Summary Card
            item {
                Surface(
                    color = SlateSurfaceElevated,
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Insights,
                                    contentDescription = null,
                                    tint = VoltLime,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "PERIODISASI ADAPTIF",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = VoltLime
                                    )
                                    Text(
                                        text = "Variasi sesi adaptif berdasarkan riwayat lari Anda",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                            }
                            if (onAskGeminiAdvice != null && goal != null) {
                                OutlinedButton(
                                    onClick = { onAskGeminiAdvice(previewGoal) },
                                    enabled = !isGeminiLoading,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.defaultMinSize(minHeight = 44.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    if (isGeminiLoading) {
                                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = AeroCyan)
                                    } else {
                                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = AeroCyan, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Saran Sesi", style = MaterialTheme.typography.labelSmall, color = AeroCyan)
                                    }
                                }
                            }
                        }

                        if (!geminiAdviceText.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                color = ObsidianBackground,
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, AeroCyan.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = geminiAdviceText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextPrimary,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Stats Summary Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            MiniStatPill(label = "TOTAL SESI", value = "${sessions.size}", color = TextPrimary)
                            MiniStatPill(label = "SELESAI", value = "$completedCount", color = SuccessGreen)
                            MiniStatPill(label = "HARI INI", value = "$todayPendingCount", color = VoltLime)
                            MiniStatPill(label = "TERLEWAT", value = "$skippedCount", color = KineticCoral)
                            MiniStatPill(label = "DATANG", value = "$upcomingCount", color = AeroCyan)
                        }
                    }
                }
            }

            // 3. Selected Session Detail Card (Interactive on tap)
            item {
                selectedSession?.let { session ->
                    SessionDetailHighlightCard(
                        session = session,
                        onNavigateToOcr = onNavigateToOcr
                    )
                }
            }

            // 4. Legend of Session Types & Icons (Sleek 2-row container, Race Day never overflows)
            item {
                Surface(
                    color = SlateSurfaceElevated,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                                LegendItem(icon = Icons.Default.Bolt, label = "Interval", color = VoltLime)
                            }
                            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                                LegendItem(icon = Icons.Default.LocalFireDepartment, label = "Tempo", color = AeroCyan)
                            }
                            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                                LegendItem(icon = Icons.Default.Spa, label = "Recovery", color = SuccessGreen)
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                                LegendItem(icon = Icons.AutoMirrored.Filled.DirectionsRun, label = "Long Run", color = WarningAmber)
                            }
                            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                                LegendItem(icon = Icons.Default.EmojiEvents, label = "Race Day", color = KineticCoral)
                            }
                            Box(modifier = Modifier.weight(1f)) {}
                        }
                    }
                }
            }

            // 5. MONTH-BY-MONTH VERTICAL CALENDARS
            // "nanti akan ada tampilan tanggalan 1 - 30/31 bulan berjalan, jika target latihan lebih dari 1 bulan,
            // maka akan ada 2 bulan atas dan bawah dan seterusnya, nanti di tanggal jadwal latihan akan beergambar lain bukan hanya angka tanggal"
            items(monthsInPlan, key = { "${it.year}-${it.monthZeroBased}" }) { monthData ->
                MonthCalendarGridCard(
                    monthData = monthData,
                    sessionsByDate = sessionsByDate,
                    selectedDateKey = selectedDateKey,
                    todayKey = todayKey,
                    onSelectDate = { selectedDateKey = it },
                    onSessionClick = { sessionForQuickInfoDialog = it }
                )
            }
        }
    }

    // Modal Popup Informasi Singkat Sesi Latihan saat Logo Jadwal Diklik
    sessionForQuickInfoDialog?.let { session ->
        SessionQuickInfoDialog(
            session = session,
            onDismiss = { sessionForQuickInfoDialog = null },
            onNavigateToOcr = {
                sessionForQuickInfoDialog = null
                onNavigateToOcr()
            }
        )
    }
}

@Composable
private fun LegendItem(icon: ImageVector, label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextPrimary, fontSize = 11.sp, maxLines = 1)
    }
}

@Composable
private fun MiniStatPill(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 10.sp)
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
private fun MonthCalendarGridCard(
    monthData: MonthGridInfo,
    sessionsByDate: Map<String, ScheduledTrainingSession>,
    selectedDateKey: String,
    todayKey: String,
    onSelectDate: (String) -> Unit,
    onSessionClick: (ScheduledTrainingSession) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SlateBorder, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SlateSurface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Month Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = monthData.monthTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                val monthSessionsCount = (1..monthData.daysInMonth).count { day ->
                    val key = String.format(Locale.US, "%04d-%02d-%02d", monthData.year, monthData.monthZeroBased + 1, day)
                    sessionsByDate.containsKey(key)
                }
                Surface(
                    color = VoltLime.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "$monthSessionsCount Sesi Lari",
                        style = MaterialTheme.typography.labelSmall,
                        color = VoltLime,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Day of Week Header Row: Sen Sel Rab Kam Jum Sab Min
            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min").forEach { dayLabel ->
                    Text(
                        text = dayLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (dayLabel in listOf("Sab", "Min")) AeroCyan else TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Grid of days
            val totalCells = monthData.firstDayOffsetIso + monthData.daysInMonth
            val totalWeeks = (totalCells + 6) / 7

            for (week in 0 until totalWeeks) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (dayOfWeek in 0..6) {
                        val cellIndex = week * 7 + dayOfWeek
                        val dayNumber = cellIndex - monthData.firstDayOffsetIso + 1

                        if (dayNumber in 1..monthData.daysInMonth) {
                            val dateKey = String.format(
                                Locale.US,
                                "%04d-%02d-%02d",
                                monthData.year,
                                monthData.monthZeroBased + 1,
                                dayNumber
                            )
                            val session = sessionsByDate[dateKey]
                            val isSelected = dateKey == selectedDateKey
                            val isToday = dateKey == todayKey

                            CalendarDayCell(
                                dayNumber = dayNumber,
                                session = session,
                                isSelected = isSelected,
                                isToday = isToday,
                                onClick = {
                                    onSelectDate(dateKey)
                                    if (session != null) {
                                        onSessionClick(session)
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            // Empty spacer cell before day 1 or after last day
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

/**
 * Calendar cell:
 * - If not a scheduled day: simple day number.
 * - If a scheduled training day: BERGAMBAR LAIN bukan hanya angka tanggal!
 *   Displays custom icon, color container, and status badge.
 */
@Composable
private fun CalendarDayCell(
    dayNumber: Int,
    session: ScheduledTrainingSession?,
    isSelected: Boolean,
    isToday: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (session == null) {
        // Plain Non-Training Day Cell
        Box(
            modifier = modifier
                .aspectRatio(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(if (isSelected) SlateSurfaceElevated else Color.Transparent)
                .border(
                    width = if (isSelected) 1.5.dp else if (isToday) 1.dp else 0.dp,
                    color = if (isSelected) VoltLime else if (isToday) VoltLime.copy(alpha = 0.5f) else Color.Transparent,
                    shape = RoundedCornerShape(8.dp)
                )
                .clickable(
                    role = Role.Button,
                    onClickLabel = "Pilih tanggal $dayNumber"
                ) { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$dayNumber",
                style = MaterialTheme.typography.bodySmall,
                color = if (isToday) VoltLime else TextSecondary.copy(alpha = 0.7f),
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
            )
        }
    } else {
        // Scheduled Training Day: BERGAMBAR LAIN bukan hanya angka tanggal!
        val (icon, iconColor, containerColor) = when (session.workoutType) {
            SessionWorkoutType.INTERVAL -> Triple(
                Icons.Default.Bolt,
                VoltLime,
                VoltLime.copy(alpha = 0.22f)
            )
            SessionWorkoutType.TEMPO -> Triple(
                Icons.Default.LocalFireDepartment,
                AeroCyan,
                AeroCyan.copy(alpha = 0.22f)
            )
            SessionWorkoutType.RECOVERY -> Triple(
                Icons.Default.Spa,
                SuccessGreen,
                SuccessGreen.copy(alpha = 0.22f)
            )
            SessionWorkoutType.EASY_LONG -> Triple(
                Icons.AutoMirrored.Filled.DirectionsRun,
                WarningAmber,
                WarningAmber.copy(alpha = 0.22f)
            )
            SessionWorkoutType.RACE_DAY -> Triple(
                Icons.Default.EmojiEvents,
                KineticCoral,
                KineticCoral.copy(alpha = 0.35f)
            )
        }

        val statusBadgeColor = when (session.status) {
            ScheduledSessionStatus.COMPLETED -> SuccessGreen
            ScheduledSessionStatus.TODAY_PENDING -> VoltLime
            ScheduledSessionStatus.SKIPPED -> KineticCoral
            ScheduledSessionStatus.UPCOMING -> AeroCyan
        }

        Box(
            modifier = modifier
                .aspectRatio(1f)
                .clip(RoundedCornerShape(10.dp))
                .background(containerColor)
                .border(
                    width = if (isSelected) 2.dp else if (isToday) 1.5.dp else 1.dp,
                    color = if (isSelected) Color.White else if (isToday) VoltLime else iconColor.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(10.dp)
                )
                .clickable(
                    role = Role.Button,
                    onClickLabel = "Pilih sesi latihan tanggal $dayNumber, ${session.sessionTitle}"
                ) { onClick() }
                .padding(2.dp)
        ) {
            // Day Number (Top Left)
            Text(
                text = "$dayNumber",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 2.dp, top = 1.dp)
            )

            // Status Indicator Dot / Badge (Top Right)
            Surface(
                color = statusBadgeColor,
                shape = CircleShape,
                modifier = Modifier
                    .size(6.dp)
                    .align(Alignment.TopEnd)
                    .padding(end = 1.dp, top = 1.dp)
            ) {}

            // Prominent Graphic / Icon representing workout type (Center)
            Icon(
                imageVector = icon,
                contentDescription = session.sessionTitle,
                tint = iconColor,
                modifier = Modifier
                    .size(20.dp)
                    .align(Alignment.Center)
            )
        }
    }
}

/**
 * Interactive card highlighting the details and AI reasoning of the selected session.
 */
@Composable
private fun SessionDetailHighlightCard(
    session: ScheduledTrainingSession,
    onNavigateToOcr: () -> Unit
) {
    val (icon, typeColor) = when (session.workoutType) {
        SessionWorkoutType.INTERVAL -> Pair(Icons.Default.Bolt, VoltLime)
        SessionWorkoutType.TEMPO -> Pair(Icons.Default.LocalFireDepartment, AeroCyan)
        SessionWorkoutType.RECOVERY -> Pair(Icons.Default.Spa, SuccessGreen)
        SessionWorkoutType.EASY_LONG -> Pair(Icons.AutoMirrored.Filled.DirectionsRun, WarningAmber)
        SessionWorkoutType.RACE_DAY -> Pair(Icons.Default.EmojiEvents, KineticCoral)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, typeColor, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SlateSurfaceElevated)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header Row: Session Type, Date, Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = typeColor.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, typeColor.copy(alpha = 0.5f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(imageVector = icon, contentDescription = null, tint = typeColor, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = session.workoutType.displayName,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = typeColor
                            )
                        }
                    }
                }

                // Status Badge
                Surface(
                    color = when (session.status) {
                        ScheduledSessionStatus.COMPLETED -> SuccessGreen.copy(alpha = 0.2f)
                        ScheduledSessionStatus.TODAY_PENDING -> VoltLime.copy(alpha = 0.25f)
                        ScheduledSessionStatus.SKIPPED -> KineticCoral.copy(alpha = 0.25f)
                        ScheduledSessionStatus.UPCOMING -> SlateBorder
                    },
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = when (session.status) {
                            ScheduledSessionStatus.COMPLETED -> "✅ SELESAI"
                            ScheduledSessionStatus.TODAY_PENDING -> "🔔 HARI INI"
                            ScheduledSessionStatus.SKIPPED -> "⚠️ TERLEWAT"
                            ScheduledSessionStatus.UPCOMING -> "📅 AKAN DATANG"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = when (session.status) {
                            ScheduledSessionStatus.COMPLETED -> SuccessGreen
                            ScheduledSessionStatus.TODAY_PENDING -> VoltLime
                            ScheduledSessionStatus.SKIPPED -> KineticCoral
                            ScheduledSessionStatus.UPCOMING -> TextSecondary
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Title & Date
            Text(
                text = session.sessionTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = session.displayDate,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Distance, Target Pace, and Intensity Zone Row (Each has weight(1f) to ensure clean horizontal fit)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "JARAK",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        fontSize = 10.sp,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${"%.1f".format(session.recommendedDistanceKm)} km",
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = TelemetryMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "PACE",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        fontSize = 10.sp,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = RacePredictionEngine.formatPace(session.targetPaceSecondsPerKm),
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = TelemetryMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = VoltLime,
                        maxLines = 1
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "ZONA",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        fontSize = 10.sp,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = session.workoutType.zoneLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AeroCyan,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Session Description
            Text(
                text = session.sessionDescription,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            // DYNAMIC AI REASONING BOX
            // "selasa tidak pasti interval, bisa jadi tempo atau recovery tergantung history latihan yang di upload sebelumnya"
            if (session.aiReason.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = ObsidianBackground,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, VoltLime.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Insights,
                            contentDescription = null,
                            tint = VoltLime,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "ALASAN PENENTUAN SESI:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = VoltLime
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = session.aiReason,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }

            // Matched Strava History Info if completed
            if (session.matchedWorkout != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = SuccessGreen.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = SuccessGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Lari tercatat di Strava: ${session.matchedWorkout.distanceKm} km (${session.matchedWorkout.title}) • Waktu: ${RacePredictionEngine.formatDuration(session.matchedWorkout.durationSeconds)} (Pace ${RacePredictionEngine.formatPace(session.matchedWorkout.avgPaceSecondsPerKm)})",
                            style = MaterialTheme.typography.labelSmall,
                            color = SuccessGreen
                        )
                    }
                }
            }

            // Action Button for Today or Upcoming
            if (session.status == ScheduledSessionStatus.TODAY_PENDING || session.status == ScheduledSessionStatus.UPCOMING) {
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = onNavigateToOcr,
                    colors = ButtonDefaults.buttonColors(containerColor = VoltLime, contentColor = Color.Black),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.AddAPhoto,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (session.status == ScheduledSessionStatus.TODAY_PENDING) "Mulai Lari & Scan Strava Hari Ini" else "Scan Hasil Sesi Lari",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Computes the month grids from startDate to targetDate.
 * E.g. Nov 2026 to Dec 2026 -> 2 MonthGridInfo objects.
 */
private fun computeMonthGrids(startDateKey: String, targetDateKey: String): List<MonthGridInfo> {
    val startCal = Calendar.getInstance()
    GoalTrainingPlanEngine.parseDateKey(startDateKey)?.let { startCal.time = it }
    startCal.set(Calendar.DAY_OF_MONTH, 1)
    startCal.set(Calendar.HOUR_OF_DAY, 0)
    startCal.set(Calendar.MINUTE, 0)
    startCal.set(Calendar.SECOND, 0)

    val targetCal = Calendar.getInstance()
    GoalTrainingPlanEngine.parseDateKey(targetDateKey)?.let { targetCal.time = it }
    targetCal.set(Calendar.DAY_OF_MONTH, 1)

    val list = mutableListOf<MonthGridInfo>()
    val cursor = startCal.clone() as Calendar
    var guard = 0

    val monthNameFormat = SimpleDateFormat("MMMM yyyy", Locale("id", "ID"))

    while (guard < 12) {
        val y = cursor.get(Calendar.YEAR)
        val m = cursor.get(Calendar.MONTH)
        val title = monthNameFormat.format(cursor.time).replaceFirstChar { it.uppercase() }
        val daysInMonth = cursor.getActualMaximum(Calendar.DAY_OF_MONTH)

        // ISO Day of week of the 1st of this month: 1=Senin .. 7=Minggu
        val firstIsoDay = GoalTrainingPlanEngine.calendarToIsoDayOfWeek(cursor)
        val offset = firstIsoDay - 1 // 0 for Senin .. 6 for Minggu

        list.add(
            MonthGridInfo(
                year = y,
                monthZeroBased = m,
                monthTitle = title,
                daysInMonth = daysInMonth,
                firstDayOffsetIso = offset
            )
        )

        if (cursor.get(Calendar.YEAR) == targetCal.get(Calendar.YEAR) &&
            cursor.get(Calendar.MONTH) == targetCal.get(Calendar.MONTH)
        ) {
            break
        }

        cursor.add(Calendar.MONTH, 1)
        guard++
    }

    return list.ifEmpty {
        listOf(
            MonthGridInfo(
                year = Calendar.getInstance().get(Calendar.YEAR),
                monthZeroBased = Calendar.getInstance().get(Calendar.MONTH),
                monthTitle = monthNameFormat.format(Date()).replaceFirstChar { it.uppercase() },
                daysInMonth = 30,
                firstDayOffsetIso = 0
            )
        )
    }
}

/**
 * Modal dialog that pops up when a scheduled workout logo/cell is clicked in the calendar,
 * displaying a concise, actionable summary of the training session.
 */
@Composable
private fun SessionQuickInfoDialog(
    session: ScheduledTrainingSession,
    onDismiss: () -> Unit,
    onNavigateToOcr: () -> Unit
) {
    val (icon, typeColor) = when (session.workoutType) {
        SessionWorkoutType.INTERVAL -> Pair(Icons.Default.Bolt, VoltLime)
        SessionWorkoutType.TEMPO -> Pair(Icons.Default.LocalFireDepartment, AeroCyan)
        SessionWorkoutType.RECOVERY -> Pair(Icons.Default.Spa, SuccessGreen)
        SessionWorkoutType.EASY_LONG -> Pair(Icons.AutoMirrored.Filled.DirectionsRun, WarningAmber)
        SessionWorkoutType.RACE_DAY -> Pair(Icons.Default.EmojiEvents, KineticCoral)
    }

    val statusBadgeText = when (session.status) {
        ScheduledSessionStatus.COMPLETED -> "✅ Selesai"
        ScheduledSessionStatus.TODAY_PENDING -> "🔔 Hari Ini"
        ScheduledSessionStatus.SKIPPED -> "⚠️ Terlewat"
        ScheduledSessionStatus.UPCOMING -> "📅 Akan Datang"
    }

    val statusBadgeColor = when (session.status) {
        ScheduledSessionStatus.COMPLETED -> SuccessGreen
        ScheduledSessionStatus.TODAY_PENDING -> VoltLime
        ScheduledSessionStatus.SKIPPED -> KineticCoral
        ScheduledSessionStatus.UPCOMING -> AeroCyan
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SlateSurfaceElevated,
        icon = {
            Surface(
                color = typeColor.copy(alpha = 0.2f),
                shape = CircleShape,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = typeColor,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = session.sessionTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = session.displayDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Surface(
                        color = statusBadgeColor.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = statusBadgeText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = statusBadgeColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Key Metrics Row: Target Jarak, Pace, Zona
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = ObsidianBackground,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "JARAK", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 10.sp)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${"%.1f".format(session.recommendedDistanceKm)} km",
                                style = MaterialTheme.typography.bodyMedium,
                                fontFamily = TelemetryMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                    }
                    Surface(
                        color = ObsidianBackground,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "PACE", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 10.sp)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = RacePredictionEngine.formatPace(session.targetPaceSecondsPerKm),
                                style = MaterialTheme.typography.bodyMedium,
                                fontFamily = TelemetryMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = VoltLime
                            )
                        }
                    }
                    Surface(
                        color = ObsidianBackground,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "ZONA", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 10.sp)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = session.workoutType.zoneLabel,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = AeroCyan
                            )
                        }
                    }
                }

                // Description
                Text(
                    text = session.sessionDescription,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextPrimary.copy(alpha = 0.9f)
                )

                // Dynamic AI Reason if present
                if (session.aiReason.isNotBlank()) {
                    Surface(
                        color = ObsidianBackground,
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, VoltLime.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(imageVector = Icons.Default.Insights, contentDescription = null, tint = VoltLime, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = session.aiReason,
                                style = MaterialTheme.typography.labelSmall,
                                color = VoltLime
                            )
                        }
                    }
                }

                // Matched Workout from Strava
                if (session.matchedWorkout != null) {
                    Surface(
                        color = SuccessGreen.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = "Lari Tercatat di Strava:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = SuccessGreen
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${"%.2f".format(session.matchedWorkout.distanceKm)} km • ${session.matchedWorkout.title} pada pace ${RacePredictionEngine.formatPace(session.matchedWorkout.avgPaceSecondsPerKm)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (session.status == ScheduledSessionStatus.TODAY_PENDING) {
                Button(
                    onClick = onNavigateToOcr,
                    colors = ButtonDefaults.buttonColors(containerColor = VoltLime, contentColor = OnVoltLime)
                ) {
                    Icon(imageVector = Icons.Default.AddAPhoto, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Mulai Sesi / Scan")
                }
            } else {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = SlateSurface, contentColor = TextPrimary)
                ) {
                    Text("Tutup")
                }
            }
        },
        dismissButton = {
            if (session.status == ScheduledSessionStatus.TODAY_PENDING) {
                TextButton(onClick = onDismiss) {
                    Text("Tutup", color = TextSecondary)
                }
            }
        }
    )
}
