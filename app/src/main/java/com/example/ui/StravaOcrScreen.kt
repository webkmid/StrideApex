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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.domain.RacePredictionEngine
import com.example.ui.theme.AeroCyan
import com.example.ui.theme.KineticCoral
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VoltLime
import com.example.viewmodel.OcrScanUiState

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
        sourceType: String
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedPresetIndex by remember { mutableIntStateOf(0) }
    var currentBitmap by remember {
        mutableStateOf(RacePredictionEngine.generateSampleStravaScreenshotBitmap(0))
    }

    // Editable Workout Fields
    var titleText by remember { mutableStateOf("Morning Carbon Tempo 10K") }
    var distanceKmText by remember { mutableStateOf("10.0") }
    var durationMinText by remember { mutableStateOf("39") }
    var durationSecText by remember { mutableStateOf("20") }
    var heartRateText by remember { mutableStateOf("172") }
    var cadenceText by remember { mutableStateOf("184") }
    var elevationText by remember { mutableStateOf("64") }
    var splitsText by remember { mutableStateOf("KM 1: 4:02 | KM 4: 3:56 | KM 8: 3:53 | KM 10: 3:50") }
    var coachingInsightText by remember {
        mutableStateOf("Progressive negative split with 184 SPM cadence shows strong lactate threshold control.")
    }
    var sourceType by remember { mutableStateOf("STRAVA_OCR") }

    // Populate fields automatically whenever Gemini AI OCR completes
    LaunchedEffect(ocrState) {
        if (ocrState is OcrScanUiState.Extracted) {
            val m = ocrState.metrics
            titleText = m.title
            distanceKmText = "%.2f".format(m.distanceKm)
            durationMinText = (m.durationSeconds / 60).toString()
            durationSecText = (m.durationSeconds % 60).toString()
            heartRateText = m.avgHeartRate.toString()
            cadenceText = m.cadenceSpm.toString()
            elevationText = m.elevationGainM.toString()
            splitsText = m.splitsSummary
            coachingInsightText = m.aiCoachingInsight
            sourceType = "STRAVA_OCR"
        }
    }

    // Zero-permission Android Photo Picker (Google Play Policy Compliant)
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
        // Header Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, SlateBorder, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SlateSurface)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = KineticCoral.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "GEMINI MULTIMODAL OCR",
                            style = MaterialTheme.typography.labelSmall,
                            color = KineticCoral,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Strava Screenshot Scanner",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Pick a Strava screenshot from your gallery or load one of the synthetic Strava activity cards below to extract splits, pace, heart rate, and AI coaching analysis.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Sample Strava Card Presets for Instant Emulator Testing
                Text(
                    text = "INSTANT EMULATOR STRAVA SAMPLES:",
                    style = MaterialTheme.typography.labelSmall,
                    color = VoltLime
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val labels = listOf("10K Tempo", "21.1K Long", "5K Track PR")
                    labels.forEachIndexed { index, label ->
                        FilterChip(
                            selected = selectedPresetIndex == index,
                            onClick = {
                                selectedPresetIndex = index
                                currentBitmap = RacePredictionEngine.generateSampleStravaScreenshotBitmap(index)
                            },
                            label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = VoltLime.copy(alpha = 0.2f),
                                selectedLabelColor = VoltLime
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Screenshot Preview Box with Laser Scan Line Animation
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, SlateBorder, RoundedCornerShape(16.dp))
                        .background(ObsidianBackground)
                ) {
                    Image(
                        bitmap = currentBitmap.asImageBitmap(),
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
                                    text = "Gemini 3.8 Flash extracting telemetry...",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = VoltLime
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

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
                            .testTag("pick_strava_photo_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = null,
                            tint = AeroCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pick Screenshot", style = MaterialTheme.typography.labelLarge, color = AeroCyan)
                    }

                    Button(
                        onClick = { onAnalyzeBitmap(currentBitmap) },
                        enabled = ocrState !is OcrScanUiState.Scanning,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = KineticCoral,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1.2f)
                            .testTag("run_ai_ocr_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Extract with AI", style = MaterialTheme.typography.labelLarge)
                    }
                }

                if (ocrState is OcrScanUiState.Error) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = ocrState.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.testTag("ocr_error_message")
                    )
                }
            }
        }

        // Editable Telemetry Verification & Manual Entry Card
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "VERIFY & LOG WORKOUT TELEMETRY",
                        style = MaterialTheme.typography.titleMedium,
                        color = VoltLime
                    )
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

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
                    label = { Text("Workout Title") },
                    singleLine = true,
                    colors = fieldColors,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("workout_title_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = distanceKmText,
                        onValueChange = { distanceKmText = it },
                        label = { Text("Distance (km)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = fieldColors,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("workout_distance_input")
                    )
                    OutlinedTextField(
                        value = durationMinText,
                        onValueChange = { durationMinText = it },
                        label = { Text("Minutes") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = fieldColors,
                        modifier = Modifier.weight(0.85f)
                    )
                    OutlinedTextField(
                        value = durationSecText,
                        onValueChange = { durationSecText = it },
                        label = { Text("Seconds") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = fieldColors,
                        modifier = Modifier.weight(0.85f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = heartRateText,
                        onValueChange = { heartRateText = it },
                        label = { Text("Avg HR (bpm)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = fieldColors,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = cadenceText,
                        onValueChange = { cadenceText = it },
                        label = { Text("Cadence (spm)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = fieldColors,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = elevationText,
                        onValueChange = { elevationText = it },
                        label = { Text("Elev Gain (m)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = fieldColors,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = splitsText,
                    onValueChange = { splitsText = it },
                    label = { Text("Kilometer Splits Breakdown") },
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = coachingInsightText,
                    onValueChange = { coachingInsightText = it },
                    label = { Text("AI Physiological Coaching Takeaway") },
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        val dist = distanceKmText.toDoubleOrNull()?.coerceIn(0.1, 500.0) ?: 10.0
                        val mins = durationMinText.toLongOrNull()?.coerceAtLeast(0L) ?: 40L
                        val secs = durationSecText.toLongOrNull()?.coerceIn(0L, 59L) ?: 0L
                        val totalDuration = (mins * 60L + secs).coerceAtLeast(30L)
                        val hr = heartRateText.toLongOrNull()?.coerceIn(0L, 260L) ?: 165L
                        val cad = cadenceText.toLongOrNull()?.coerceIn(0L, 300L) ?: 180L
                        val elev = elevationText.toLongOrNull()?.coerceIn(0L, 20000L) ?: 40L

                        onSaveWorkout(
                            titleText,
                            dist,
                            totalDuration,
                            hr,
                            cad,
                            elev,
                            splitsText,
                            coachingInsightText,
                            sourceType
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VoltLime,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(vertical = 14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("save_workout_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudUpload,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Save Workout & Check PRs",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
