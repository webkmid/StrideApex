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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.WorkoutLog
import com.example.domain.GoalTrainingPlanEngine
import com.example.domain.RacePredictionEngine
import com.example.domain.WorkoutRefreshEngine
import com.example.ui.components.StravaSplitsBarChart
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * Dedicated Fullscreen Workout Editor, matching the modern structure, styling,
 * DatePicker, and athletic telemetry components of the Scan Run creation flow.
 */
@Composable
fun EditWorkoutScreen(
    workout: WorkoutLog,
    onBack: () -> Unit,
    onSaveWorkout: (
        id: String,
        title: String,
        distanceKm: Double,
        durationSeconds: Long,
        avgHeartRate: Long,
        cadenceSpm: Long,
        elevationGainM: Long,
        splitsSummary: String,
        aiCoachingInsight: String,
        workoutDateEpochMs: Long?
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val context = LocalContext.current

    var titleText by remember { mutableStateOf(workout.title) }
    var distanceKmText by remember {
        mutableStateOf(if (workout.distanceKm > 0.0) "%.2f".format(Locale.US, workout.distanceKm) else "")
    }
    var durationMinText by remember {
        mutableStateOf((workout.durationSeconds / 60).toString())
    }
    var durationSecText by remember {
        mutableStateOf((workout.durationSeconds % 60).toString())
    }
    var heartRateText by remember {
        mutableStateOf(if (workout.avgHeartRate > 0) workout.avgHeartRate.toString() else "")
    }
    var cadenceText by remember {
        mutableStateOf(if (workout.cadenceSpm > 0) workout.cadenceSpm.toString() else "")
    }
    var elevationText by remember {
        mutableStateOf(if (workout.elevationGainM > 0) workout.elevationGainM.toString() else "")
    }
    var splitsText by remember { mutableStateOf(workout.splitsSummary) }
    var coachingInsightText by remember { mutableStateOf(workout.aiCoachingInsight) }

    var workoutDateStr by remember {
        mutableStateOf(GoalTrainingPlanEngine.workoutDateKey(workout))
    }
    var workoutDateEpochMs by remember {
        mutableStateOf<Long?>(workout.createdAt?.toDate()?.time)
    }

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

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = VoltLime,
        unfocusedBorderColor = SlateBorder,
        focusedLabelColor = VoltLime,
        unfocusedLabelColor = TextSecondary,
        focusedTextColor = TextPrimary,
        unfocusedTextColor = TextPrimary,
        cursorColor = VoltLime
    )

    val distVal = distanceKmText.replace(',', '.').toDoubleOrNull() ?: 0.0
    val minsVal = durationMinText.toLongOrNull() ?: 0L
    val secsVal = durationSecText.toLongOrNull() ?: 0L
    val totalSecs = minsVal * 60L + secsVal
    val liveAvgPaceSec = if (distVal > 0.0 && totalSecs > 0L) (totalSecs.toDouble() / distVal).roundToLong() else 0L
    val metricsChanged = abs(distVal - workout.distanceKm) >= 0.05 || abs(totalSecs - workout.durationSeconds) >= 5L

    val refreshSplitsAndLocalInsight = {
        if (distVal > 0.0 && totalSecs > 0L) {
            val safePace = (totalSecs.toDouble() / distVal).roundToLong().coerceIn(60L, 3600L)
            val newSplits = WorkoutRefreshEngine.recalibrateSplits(
                oldSplitsSummary = splitsText,
                newDistanceKm = distVal,
                newAvgPaceSec = safePace,
                generateIfEmpty = true
            )
            splitsText = newSplits
            val hr = heartRateText.toLongOrNull()?.coerceIn(0L, 260L) ?: 0L
            val cad = cadenceText.toLongOrNull()?.coerceIn(0L, 300L) ?: 0L
            val elev = elevationText.toLongOrNull()?.coerceIn(0L, 20000L) ?: 0L
            coachingInsightText = WorkoutRefreshEngine.buildLocalRefreshedAiInsight(
                title = titleText.ifBlank { "Running Workout" },
                distanceKm = distVal,
                durationSeconds = totalSecs,
                avgPaceSec = safePace,
                avgHeartRate = hr,
                cadenceSpm = cad,
                elevationGainM = elev,
                splitsSummary = newSplits
            )
        }
    }

    val isFormValid = distVal > 0.0 && totalSecs > 0L

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Top Bar Header
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
                    onClick = onBack,
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("edit_workout_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali ke Riwayat",
                        tint = VoltLime
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = "Edit Riwayat Lari",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Perbarui metrik dan data lari sesi Anda",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }

            Surface(
                color = if (workout.sourceType == "STRAVA_OCR") KineticCoral.copy(alpha = 0.18f) else AeroCyan.copy(alpha = 0.18f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = if (workout.sourceType == "STRAVA_OCR") "Strava OCR" else "Manual",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (workout.sourceType == "STRAVA_OCR") KineticCoral else AeroCyan,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        // 2. Extracted Metrics Editable Form Card (Identical to StravaOcrScreen)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, SlateBorder, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SlateSurface)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "DATA LARI",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = VoltLime
                    )

                    if (liveAvgPaceSec > 0L) {
                        Surface(
                            color = SlateSurfaceElevated,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, SlateBorder.copy(alpha = 0.7f))
                        ) {
                            Text(
                                text = "Avg ${RacePredictionEngine.formatPace(liveAvgPaceSec)}",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = TelemetryMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = VoltLime,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // Judul Sesi Lari
                OutlinedTextField(
                    value = titleText,
                    onValueChange = { titleText = it },
                    label = { Text("Judul Sesi Lari") },
                    singleLine = true,
                    colors = fieldColors,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_workout_title_input")
                )

                // Tanggal Sesi Field with Date Picker
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Tanggal Sesi",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    Surface(
                        color = ObsidianBackground,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, SlateBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(
                                role = Role.Button,
                                onClickLabel = "Pilih Tanggal Sesi Lari"
                            ) {
                                showDatePickerDialog(workoutDateStr) { newDate ->
                                    workoutDateStr = newDate
                                    workoutDateEpochMs = try {
                                        SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(newDate)?.time
                                    } catch (_: Exception) {
                                        null
                                    }
                                }
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
                                    text = "TANGGAL SESI",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary,
                                    fontSize = 10.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = GoalTrainingPlanEngine.formatShortDate(workoutDateStr),
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
                                        contentDescription = "Pilih Tanggal Sesi",
                                        tint = VoltLime,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Row 1: Jarak (km) & Elevasi (m)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = distanceKmText,
                        onValueChange = { distanceKmText = it },
                        label = { Text("Jarak (km)", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = fieldColors,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("edit_workout_distance_input")
                    )
                    OutlinedTextField(
                        value = elevationText,
                        onValueChange = { elevationText = it },
                        label = { Text("Elevasi (m)", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = fieldColors,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Row 2: Waktu (Menit & Detik)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = durationMinText,
                        onValueChange = { durationMinText = it },
                        label = { Text("Waktu: Menit", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = fieldColors,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = durationSecText,
                        onValueChange = { durationSecText = it },
                        label = { Text("Detik", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = fieldColors,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Row 3: HR & Cadence (opsional)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = heartRateText,
                        onValueChange = { heartRateText = it },
                        label = { Text("HR (bpm)", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = fieldColors,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = cadenceText,
                        onValueChange = { cadenceText = it },
                        label = { Text("Cadence (spm)", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = fieldColors,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Sync Notice & Refresh Split/AI Control
                if (metricsChanged) {
                    Surface(
                        color = AeroCyan.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, AeroCyan.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Jarak/waktu berubah. Split & AI otomatis diperbarui saat disimpan.",
                                style = MaterialTheme.typography.labelSmall,
                                color = AeroCyan,
                                modifier = Modifier.weight(1f).padding(end = 8.dp)
                            )
                            OutlinedButton(
                                onClick = refreshSplitsAndLocalInsight,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, AeroCyan),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.defaultMinSize(minHeight = 36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Refresh Split & AI",
                                    tint = AeroCyan,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Sync",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = AeroCyan,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                // Catatan Splits
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SPLITS PER KM",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                    if (!metricsChanged && isFormValid) {
                        OutlinedButton(
                            onClick = refreshSplitsAndLocalInsight,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, SlateBorder),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.defaultMinSize(minHeight = 32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Hitung Ulang Split & AI",
                                tint = AeroCyan,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Refresh Split & AI",
                                style = MaterialTheme.typography.labelSmall,
                                color = AeroCyan,
                                maxLines = 1
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = splitsText,
                    onValueChange = { splitsText = it },
                    label = { Text("Catatan Splits (KM 1: 7:28/km | KM 2: 7:39/km)") },
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth()
                )

                // Live Splits Bar Chart Preview
                if (splitsText.isNotBlank()) {
                    StravaSplitsBarChart(
                        splitsSummary = splitsText,
                        avgPaceSecondsPerKm = liveAvgPaceSec
                    )
                }

                // Evaluasi & Rekomendasi Latihan Card
                Surface(
                    color = ObsidianBackground,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, VoltLime.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Insights,
                                contentDescription = null,
                                tint = VoltLime,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "EVALUASI & REKOMENDASI LATIHAN",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = VoltLime
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = coachingInsightText,
                            onValueChange = { coachingInsightText = it },
                            placeholder = { Text("Evaluasi analitik performa dan catatan evaluasi lari...") },
                            colors = fieldColors,
                            minLines = 3,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Tombol Simpan Perubahan
                Button(
                    onClick = {
                        val dist = distVal.coerceIn(0.1, 500.0)
                        val totalDuration = totalSecs.coerceAtLeast(1L)
                        val hr = heartRateText.toLongOrNull()?.coerceIn(0L, 260L) ?: 0L
                        val cad = cadenceText.toLongOrNull()?.coerceIn(0L, 300L) ?: 0L
                        val elev = elevationText.toLongOrNull()?.coerceIn(0L, 20000L) ?: 0L
                        val resolvedEpochMs = workoutDateEpochMs ?: try {
                            SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(workoutDateStr.trim())?.time
                        } catch (_: Exception) {
                            workout.createdAt?.toDate()?.time
                        }

                        onSaveWorkout(
                            workout.id,
                            titleText.ifBlank { "Running Workout" },
                            dist,
                            totalDuration,
                            hr,
                            cad,
                            elev,
                            splitsText,
                            coachingInsightText,
                            resolvedEpochMs
                        )
                    },
                    enabled = isFormValid,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VoltLime,
                        contentColor = OnVoltLime
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(vertical = 14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 44.dp)
                        .testTag("save_edited_workout_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudUpload,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Simpan Perubahan Lari",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
