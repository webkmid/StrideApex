package com.example.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.GoalTrainingPlanEngine
import com.example.ui.components.StravaSplitsBarChart
import com.example.ui.theme.AeroCyan
import com.example.ui.theme.KineticCoral
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.OnVoltLime
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.TelemetryMonoFontFamily
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VoltLime
import com.example.util.getAppCheckDebugToken
import com.example.viewmodel.OcrScanUiState
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun StravaOcrScreen(
    ocrState: OcrScanUiState,
    onAnalyzeBitmap: (Bitmap) -> Unit,
    onSaveWorkout: (
        title: String,
        distanceKm: Double,
        durationSeconds: Long,
        avgHeartRate: Long,
        cadenceSpm: Long,
        elevationGainM: Long,
        splitsSummary: String,
        aiCoachingInsight: String,
        sourceType: String,
        workoutDateEpochMs: Long?
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Clean, Empty Workout Fields by Default
    var titleText by remember { mutableStateOf("") }
    var distanceKmText by remember { mutableStateOf("") }
    var durationMinText by remember { mutableStateOf("") }
    var durationSecText by remember { mutableStateOf("") }
    var heartRateText by remember { mutableStateOf("") }
    var cadenceText by remember { mutableStateOf("") }
    var elevationText by remember { mutableStateOf("") }
    var splitsText by remember { mutableStateOf("") }
    var coachingInsightText by remember { mutableStateOf("") }
    var sourceType by remember { mutableStateOf("MANUAL") }
    var workoutDateEpochMs by remember { mutableStateOf<Long?>(null) }
    var workoutDateStr by remember {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        mutableStateOf(sdf.format(Date()))
    }

    val showDatePickerDialog = { initialKey: String, onDateSelected: (String) -> Unit ->
        val cal = java.util.Calendar.getInstance()
        try {
            val parts = initialKey.split("-")
            if (parts.size == 3) {
                cal.set(java.util.Calendar.YEAR, parts[0].toInt())
                cal.set(java.util.Calendar.MONTH, parts[1].toInt() - 1)
                cal.set(java.util.Calendar.DAY_OF_MONTH, parts[2].toInt())
            }
        } catch (_: Exception) {}

        android.app.DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val formatted = String.format(java.util.Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth)
                onDateSelected(formatted)
            },
            cal.get(java.util.Calendar.YEAR),
            cal.get(java.util.Calendar.MONTH),
            cal.get(java.util.Calendar.DAY_OF_MONTH)
        ).show()
    }

    // Populate fields automatically whenever Gemini AI OCR completes
    LaunchedEffect(ocrState) {
        if (ocrState is OcrScanUiState.Extracted) {
            val m = ocrState.metrics
            titleText = m.title
            distanceKmText = "%.2f".format(m.distanceKm)
            durationMinText = (m.durationSeconds / 60).toString()
            durationSecText = (m.durationSeconds % 60).toString()
            heartRateText = if (m.avgHeartRate > 0) m.avgHeartRate.toString() else ""
            cadenceText = if (m.cadenceSpm > 0) m.cadenceSpm.toString() else ""
            elevationText = if (m.elevationGainM > 0) m.elevationGainM.toString() else ""
            splitsText = m.splitsSummary
            coachingInsightText = m.aiCoachingInsight
            sourceType = "STRAVA_OCR"
            if (m.workoutDateEpochMs != null && m.workoutDateEpochMs > 0L) {
                workoutDateEpochMs = m.workoutDateEpochMs
                workoutDateStr = m.detectedDateStr.ifBlank {
                    SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(m.workoutDateEpochMs))
                }
            }
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val decoded = BitmapFactory.decodeStream(stream)
                    if (decoded != null) {
                        currentBitmap = decoded
                        onAnalyzeBitmap(decoded)
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Screenshot Upload Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, SlateBorder, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SlateSurface)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Scan Strava Screenshot",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Pilih screenshot aktivitas lari dari galeri untuk diekstrak otomatis dengan AI, atau isi manual di bawah.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )

                val bmp = currentBitmap
                if (bmp != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, SlateBorder, RoundedCornerShape(16.dp))
                            .background(ObsidianBackground)
                    ) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Strava Activity Screenshot Preview",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )

                        if (ocrState is OcrScanUiState.Scanning) {
                            val infiniteTransition = rememberInfiniteTransition(label = "ocr_laser")
                            val scanProgress by infiniteTransition.animateFloat(
                                initialValue = 0.05f,
                                targetValue = 0.95f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1400, easing = LinearEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "laser_y"
                            )
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val y = size.height * scanProgress
                                drawLine(
                                    color = VoltLime,
                                    start = Offset(0f, y),
                                    end = Offset(size.width, y),
                                    strokeWidth = 4.dp.toPx()
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(ObsidianBackground.copy(alpha = 0.55f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .background(SlateSurface, RoundedCornerShape(12.dp))
                                        .padding(horizontal = 16.dp, vertical = 10.dp)
                                ) {
                                    CircularProgressIndicator(
                                        color = VoltLime,
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.5.dp
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Mengekstrak data...",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = VoltLime
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (bmp == null) {
                    OutlinedButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pick_strava_photo_button"),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = null,
                            tint = AeroCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pilih Screenshot Strava", style = MaterialTheme.typography.labelLarge, color = AeroCyan)
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("pick_strava_photo_button"),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoLibrary,
                                contentDescription = null,
                                tint = AeroCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Ganti Foto", style = MaterialTheme.typography.labelMedium, color = AeroCyan, maxLines = 1)
                        }

                        Button(
                            onClick = { onAnalyzeBitmap(bmp) },
                            enabled = ocrState !is OcrScanUiState.Scanning,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = KineticCoral,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("run_ai_ocr_button"),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DocumentScanner,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Scan Ulang", style = MaterialTheme.typography.labelMedium, maxLines = 1)
                        }
                    }
                }

                if (ocrState is OcrScanUiState.Error) {
                    val clipboardManager = LocalClipboardManager.current
                    val debugToken = remember { getAppCheckDebugToken(context) }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = ocrState.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.testTag("ocr_error_message")
                    )
                    if (!debugToken.isNullOrBlank() && ocrState.message.contains("App Check", ignoreCase = true)) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = ObsidianBackground),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, AeroCyan, RoundedCornerShape(12.dp))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Daftarkan Debug Token ini di Firebase Console -> App Check -> Apps -> Manage debug tokens:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = debugToken,
                                    fontFamily = TelemetryMonoFontFamily,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = VoltLime
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = { clipboardManager.setText(AnnotatedString(debugToken)) },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Text("Salin Debug Token", style = MaterialTheme.typography.labelSmall, color = AeroCyan)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Clean Workout Entry Form
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, SlateBorder, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SlateSurface)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "DATA LARI",
                    style = MaterialTheme.typography.titleMedium,
                    color = VoltLime
                )

                val fieldColors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = VoltLime,
                    unfocusedBorderColor = SlateBorder,
                    focusedLabelColor = VoltLime,
                    unfocusedLabelColor = TextSecondary,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )

                OutlinedTextField(
                    value = titleText,
                    onValueChange = { titleText = it },
                    label = { Text("Judul Sesi Lari") },
                    singleLine = true,
                    colors = fieldColors,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("workout_title_input")
                )

                // Tanggal Lari Field with Date Picker
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
                                    color = Color.White
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
                            .testTag("workout_distance_input")
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

                OutlinedTextField(
                    value = splitsText,
                    onValueChange = { splitsText = it },
                    label = { Text("Catatan Splits (KM 1: 7:28/km | KM 2: 7:39/km)") },
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth()
                )

                if (splitsText.isNotBlank()) {
                    val distForAvg = distanceKmText.replace(',', '.').toDoubleOrNull() ?: 0.0
                    val minsForAvg = durationMinText.toLongOrNull() ?: 0L
                    val secsForAvg = durationSecText.toLongOrNull() ?: 0L
                    val totalSecsForAvg = minsForAvg * 60L + secsForAvg
                    val avgPaceForSplits = if (distForAvg > 0.0 && totalSecsForAvg > 0L) {
                        (totalSecsForAvg / distForAvg).toLong()
                    } else {
                        0L
                    }
                    StravaSplitsBarChart(
                        splitsSummary = splitsText,
                        avgPaceSecondsPerKm = avgPaceForSplits
                    )
                }

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
                            placeholder = { Text("Evaluasi analitik performa dan rekomendasi perbaikan lari...") },
                            colors = fieldColors,
                            minLines = 3,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                val isFormValid = (distanceKmText.toDoubleOrNull() ?: 0.0) > 0.0 &&
                    ((durationMinText.toLongOrNull() ?: 0L) > 0L || (durationSecText.toLongOrNull() ?: 0L) > 0L)

                Button(
                    onClick = {
                        val dist = distanceKmText.toDoubleOrNull()?.coerceIn(0.1, 500.0) ?: return@Button
                        val mins = durationMinText.toLongOrNull()?.coerceAtLeast(0L) ?: 0L
                        val secs = durationSecText.toLongOrNull()?.coerceIn(0L, 59L) ?: 0L
                        val totalDuration = (mins * 60L + secs).coerceAtLeast(1L)
                        val hr = heartRateText.toLongOrNull()?.coerceIn(0L, 260L) ?: 0L
                        val cad = cadenceText.toLongOrNull()?.coerceIn(0L, 300L) ?: 0L
                        val elev = elevationText.toLongOrNull()?.coerceIn(0L, 20000L) ?: 0L
                        val resolvedEpochMs = workoutDateEpochMs ?: try {
                            SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(workoutDateStr.trim())?.time
                        } catch (_: Exception) {
                            System.currentTimeMillis()
                        }

                        onSaveWorkout(
                            titleText.ifBlank { "Running Workout" },
                            dist,
                            totalDuration,
                            hr,
                            cad,
                            elev,
                            splitsText,
                            coachingInsightText,
                            sourceType,
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
                        .testTag("save_workout_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudUpload,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Simpan Data Lari",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
