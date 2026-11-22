package com.example.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PersonalRecord
import com.example.data.model.WorkoutLog
import com.example.domain.RacePredictionEngine
import com.example.ui.theme.AeroCyan
import com.example.ui.theme.KineticCoral
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.OnVoltLime
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.SlateSurfaceElevated
import com.example.ui.theme.TelemetryMonoFontFamily
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VoltLime
import com.example.util.AdBannerBar
import com.example.viewmodel.UiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToLong

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    workoutsState: UiState<List<WorkoutLog>>,
    prsState: UiState<List<PersonalRecord>> = UiState.Success(emptyList()),
    onDeleteWorkout: (String) -> Unit,
    onUpdateWorkout: (
        workoutId: String,
        title: String,
        distKm: Double,
        durationSec: Long,
        avgHr: Long,
        cadence: Long,
        elevationM: Long,
        splits: String,
        insight: String,
        dateEpochMs: Long?
    ) -> Unit,
    onCreateCloudBackup: (String) -> Unit,
    refreshingWorkoutId: String? = null,
    onRefreshWorkoutAi: ((WorkoutLog) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val workouts = (workoutsState as? UiState.Success)?.data ?: emptyList()
    val prs = (prsState as? UiState.Success)?.data ?: emptyList()

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedFilterIndex by rememberSaveable { mutableIntStateOf(0) } // 0: Semua, 1: 5K+, 2: 10K+, 3: 21K+, 4: Strava OCR, 5: Manual

    var selectedWorkoutKey by rememberSaveable { mutableStateOf<String?>(null) }
    var workoutToEdit by remember { mutableStateOf<WorkoutLog?>(null) }
    var workoutToDelete by remember { mutableStateOf<WorkoutLog?>(null) }

    // Detail Screen Overlay
    if (selectedWorkoutKey != null) {
        val selectedWorkout = workouts.find {
            it.id == selectedWorkoutKey || (it.title + it.durationSeconds) == selectedWorkoutKey
        }
        if (selectedWorkout != null) {
            BackHandler { selectedWorkoutKey = null }
            WorkoutDetailScreen(
                workout = selectedWorkout,
                allWorkouts = workouts,
                prs = prs,
                onBack = { selectedWorkoutKey = null },
                onEditWorkout = {
                    workoutToEdit = it
                    selectedWorkoutKey = null
                },
                onDeleteWorkout = { id ->
                    onDeleteWorkout(id)
                    selectedWorkoutKey = null
                },
                isRefreshingAi = refreshingWorkoutId == selectedWorkout.id && selectedWorkout.id.isNotBlank(),
                onRefreshAi = onRefreshWorkoutAi
            )
            return
        }
    }

    // Edit Screen Overlay
    if (workoutToEdit != null) {
        val editingWorkout = workoutToEdit!!
        BackHandler { workoutToEdit = null }
        EditWorkoutScreen(
            workout = editingWorkout,
            onBack = { workoutToEdit = null },
            onSaveWorkout = { id, title, dist, dur, hr, cad, elev, splits, insight, dateEpochMs ->
                onUpdateWorkout(
                    id,
                    title,
                    dist,
                    dur,
                    hr,
                    cad,
                    elev,
                    splits,
                    insight,
                    dateEpochMs
                )
                workoutToEdit = null
            }
        )
        return
    }

    // Delete Confirmation Dialog
    if (workoutToDelete != null) {
        val toDel = workoutToDelete!!
        AlertDialog(
            onDismissRequest = { workoutToDelete = null },
            containerColor = SlateSurfaceElevated,
            title = {
                Text(
                    text = "Hapus Sesi Lari?",
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Text(
                    text = "Apakah Anda yakin ingin menghapus '${toDel.title}' (${String.format(Locale.US, "%.2f", toDel.distanceKm)} km)? Data yang terhapus tidak dapat dikembalikan.",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (toDel.id.isNotBlank()) {
                            onDeleteWorkout(toDel.id)
                        }
                        workoutToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = KineticCoral,
                        contentColor = Color.White
                    )
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = { workoutToDelete = null }) {
                    Text("Batal", color = TextSecondary)
                }
            }
        )
    }

    // Filter Logic
    val filteredWorkouts = remember(workouts, searchQuery, selectedFilterIndex) {
        workouts.filter { w ->
            val matchSearch = searchQuery.isBlank() ||
                w.title.contains(searchQuery, ignoreCase = true) ||
                w.splitsSummary.contains(searchQuery, ignoreCase = true) ||
                w.aiCoachingInsight.contains(searchQuery, ignoreCase = true)

            val matchFilter = when (selectedFilterIndex) {
                1 -> w.distanceKm >= 5.0
                2 -> w.distanceKm >= 10.0
                3 -> w.distanceKm >= 21.0
                4 -> w.sourceType == "STRAVA_OCR"
                5 -> w.sourceType != "STRAVA_OCR"
                else -> true
            }

            matchSearch && matchFilter
        }
    }

    val totalDistanceAll = remember(workouts) { workouts.sumOf { it.distanceKm } }
    val totalDurationAll = remember(workouts) { workouts.sumOf { it.durationSeconds } }
    val avgPaceAll = remember(totalDistanceAll, totalDurationAll) {
        if (totalDistanceAll > 0.0) (totalDurationAll.toDouble() / totalDistanceAll).roundToLong() else 0L
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBackground)
            .padding(horizontal = 16.dp)
    ) {
        // Banner Iklan Non-Intrusif di paling atas History
        AdBannerBar(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 4.dp)
        )

        // 1. Compact Header Row with Inline Metrics & Backup Action (Export removed)
        Spacer(modifier = Modifier.height(8.dp))
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
                    text = "Riwayat Sesi Lari",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val paceDisplay = if (avgPaceAll > 0L) RacePredictionEngine.formatPace(avgPaceAll) else "--:--"
                Text(
                    text = "${workouts.size} sesi • ${String.format(Locale.US, "%.1f", totalDistanceAll)} km • Avg $paceDisplay",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = TelemetryMonoFontFamily,
                    color = AeroCyan,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            OutlinedButton(
                onClick = {
                    val timestampLabel = SimpleDateFormat("MMM d HH:mm", Locale.US).format(Date())
                    onCreateCloudBackup("Backup ($timestampLabel)")
                },
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, SlateBorder),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                modifier = Modifier.defaultMinSize(minHeight = 44.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CloudUpload,
                    contentDescription = null,
                    tint = AeroCyan,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text("Backup", style = MaterialTheme.typography.labelSmall, color = AeroCyan)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 2. Compact 44dp Search Pill
        Surface(
            color = SlateSurface,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, if (searchQuery.isNotEmpty()) VoltLime.copy(alpha = 0.7f) else SlateBorder),
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = "Cari sesi, jarak, atau split...",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodySmall.copy(color = TextPrimary),
                        cursorBrush = SolidColor(VoltLime),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("history_search_input")
                    )
                }
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { searchQuery = "" },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Hapus pencarian",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 3. Compact Filter Pills (44dp touch area)
        val filterLabels = listOf("Semua", "5K+", "10K+", "21K+", "Strava OCR", "Manual")
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(filterLabels.size) { idx ->
                val isSelected = selectedFilterIndex == idx
                Box(
                    modifier = Modifier
                        .defaultMinSize(minHeight = 44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(role = Role.Tab) { selectedFilterIndex = idx },
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        color = if (isSelected) VoltLime else SlateSurface,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, if (isSelected) VoltLime else SlateBorder)
                    ) {
                        Text(
                            text = filterLabels[idx],
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) OnVoltLime else TextSecondary,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 5. Workout List
        when (workoutsState) {
            is UiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = VoltLime)
                }
            }
            is UiState.Error -> {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SlateSurface),
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                ) {
                    Text(
                        text = workoutsState.message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            is UiState.Success -> {
                if (filteredWorkouts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Surface(
                                color = SlateSurface,
                                shape = CircleShape,
                                modifier = Modifier.size(64.dp),
                                border = BorderStroke(1.dp, SlateBorder)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.DirectionsRun,
                                        contentDescription = null,
                                        tint = VoltLime,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = if (searchQuery.isNotBlank() || selectedFilterIndex != 0) {
                                    "Tidak ada sesi lari yang cocok"
                                } else {
                                    "Belum ada riwayat lari"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (searchQuery.isNotBlank() || selectedFilterIndex != 0) {
                                    "Coba reset kata kunci pencarian atau ubah filter kategori."
                                } else {
                                    "Gunakan tombol Scan Run atau Catat Lari di Dashboard untuk mulai mencatat riwayat!"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredWorkouts, key = { it.id.ifBlank { it.title + it.durationSeconds } }) { workout ->
                            HistoryWorkoutCard(
                                workout = workout,
                                onOpenDetail = {
                                    selectedWorkoutKey = workout.id.ifBlank { workout.title + workout.durationSeconds }
                                },
                                onEdit = { workoutToEdit = workout },
                                onDelete = { workoutToDelete = workout }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Dedicated Card in History tab.
 */
@Composable
private fun HistoryWorkoutCard(
    workout: WorkoutLog,
    onOpenDetail: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isStravaOcr = workout.sourceType == "STRAVA_OCR"
    val dateDisplay = workout.createdAt?.toDate()?.let {
        SimpleDateFormat("d MMM yyyy, HH:mm", Locale("id", "ID")).format(it)
    } ?: "Sesi Lari"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SlateBorder, RoundedCornerShape(16.dp))
            .clickable(
                role = Role.Button,
                onClickLabel = "Buka Detail Sesi ${workout.title}"
            ) { onOpenDetail() }
            .testTag("history_card_${workout.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SlateSurface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Source, Title & Action buttons
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
                        color = if (isStravaOcr) KineticCoral.copy(alpha = 0.18f) else AeroCyan.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (isStravaOcr) "Strava OCR" else "Manual",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isStravaOcr) KineticCoral else AeroCyan,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = workout.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = dateDisplay,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            fontSize = 10.sp
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(36.dp).testTag("edit_workout_${workout.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Sesi",
                            tint = AeroCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(36.dp).testTag("delete_workout_${workout.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Hapus Sesi",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Main Metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("DISTANCE", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 9.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${String.format(Locale.US, "%.2f", workout.distanceKm)} km",
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = TelemetryMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = VoltLime
                    )
                }

                Column {
                    Text("DURATION", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 9.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = RacePredictionEngine.formatDuration(workout.durationSeconds),
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = TelemetryMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Column {
                    Text("AVG PACE", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 9.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = RacePredictionEngine.formatPace(workout.avgPaceSecondsPerKm),
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = TelemetryMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = AeroCyan
                    )
                }

                if (workout.avgHeartRate > 0) {
                    Column {
                        Text("HR", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 9.sp)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${workout.avgHeartRate} bpm",
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = TelemetryMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary
                        )
                    }
                }
            }

            if (workout.splitsSummary.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = SlateSurfaceElevated,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = workout.splitsSummary,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = TelemetryMonoFontFamily,
                        color = TextSecondary,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = SlateBorder.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(6.dp))

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
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Lihat Analisis Detail & Biomekanika",
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
