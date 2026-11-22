package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.SlowMotionVideo
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.BiomechanicsModule
import com.example.domain.BiomechanicsCatalog
import com.example.ui.theme.AeroCyan
import com.example.ui.theme.KineticCoral
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TelemetryMonoFontFamily
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VoltLime
import com.example.viewmodel.CoachUiState
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BiomechanicsLabScreen(
    coachState: CoachUiState,
    onAskCoach: (String, BiomechanicsModule) -> Unit,
    modifier: Modifier = Modifier
) {
    val modules = BiomechanicsCatalog.modules
    var selectedModuleIndex by remember { mutableIntStateOf(0) }
    val activeModule = modules[selectedModuleIndex]

    var isPlaying by remember { mutableStateOf(true) }
    var playbackSpeed by remember { mutableFloatStateOf(0.5f) }
    var gaitPhase by remember { mutableFloatStateOf(0.15f) }

    var showJointAngles by remember { mutableStateOf(true) }
    var showGrfVectors by remember { mutableStateOf(true) }
    var showPlumbLine by remember { mutableStateOf(true) }

    val completedDrills = remember { mutableStateListOf<String>() }
    var coachQuestion by remember { mutableStateOf("") }

    // Continuous 60fps kinematic animation loop when playing
    LaunchedEffect(isPlaying, playbackSpeed) {
        var lastFrameNanos = 0L
        while (isPlaying) {
            withFrameNanos { nanos ->
                if (lastFrameNanos != 0L) {
                    val deltaSec = (nanos - lastFrameNanos) / 1_000_000_000f
                    gaitPhase = (gaitPhase + deltaSec * playbackSpeed * 0.9f) % 1f
                }
                lastFrameNanos = nanos
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
        // 1. Module Selector Carousel
        Column {
            Text(
                text = "INTERACTIVE BIOMECHANICAL VIDEO & KINEMATIC LAB",
                style = MaterialTheme.typography.labelLarge,
                color = VoltLime
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                modules.forEachIndexed { idx, mod ->
                    FilterChip(
                        selected = selectedModuleIndex == idx,
                        onClick = { selectedModuleIndex = idx },
                        label = {
                            Text(
                                text = mod.title,
                                style = MaterialTheme.typography.labelLarge
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = VoltLime.copy(alpha = 0.22f),
                            selectedLabelColor = VoltLime
                        ),
                        modifier = Modifier.testTag("biomechanics_module_chip_$idx")
                    )
                }
            }
        }

        // 2. Interactive Kinematic Video Player Canvas Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = activeModule.title,
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary
                        )
                        Text(
                            text = BiomechanicsCatalog.phaseLabelForProgress(gaitPhase),
                            style = MaterialTheme.typography.labelMedium,
                            color = AeroCyan
                        )
                    }
                    Surface(
                        color = VoltLime.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "${activeModule.targetCadenceSpm} SPM",
                            style = MaterialTheme.typography.labelMedium,
                            color = VoltLime,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Kinematic Skeletal Video Viewport
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(265.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(ObsidianBackground)
                        .border(1.dp, SlateBorder, RoundedCornerShape(16.dp))
                ) {
                    KinematicRunnerCanvas(
                        gaitPhase = gaitPhase,
                        module = activeModule,
                        showJointAngles = showJointAngles,
                        showGrfVectors = showGrfVectors,
                        showPlumbLine = showPlumbLine,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Live Telemetry HUD Overlay in top-left & top-right corners
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "KNEE FLEXION: ${activeModule.targetKneeFlexionDeg}°",
                            style = MaterialTheme.typography.labelSmall,
                            color = VoltLime
                        )
                        Text(
                            text = "TIBIAL ANGLE: +${activeModule.targetTibialAngleDeg}°",
                            style = MaterialTheme.typography.labelSmall,
                            color = AeroCyan
                        )
                        Text(
                            text = "FORWARD LEAN: ${activeModule.targetTrunkLeanDeg}°",
                            style = MaterialTheme.typography.labelSmall,
                            color = KineticCoral
                        )
                    }

                    Surface(
                        color = SlateSurface.copy(alpha = 0.85f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "GAIT CYCLE ${(gaitPhase * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Video Transport Scrubber & Speed Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    IconButton(
                        onClick = { isPlaying = !isPlaying },
                        modifier = Modifier
                            .size(44.dp)
                            .background(VoltLime, CircleShape)
                            .testTag("biomechanics_play_pause_button")
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause Tutorial" else "Play Tutorial",
                            tint = Color.Black
                        )
                    }

                    Slider(
                        value = gaitPhase,
                        onValueChange = {
                            isPlaying = false
                            gaitPhase = it
                        },
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(
                            thumbColor = VoltLime,
                            activeTrackColor = AeroCyan,
                            inactiveTrackColor = SlateBorder
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("biomechanics_phase_scrubber")
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Speed Selector & Vector Overlay Toggles
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(0.25f to "0.25x Ultra Slow", 0.5f to "0.5x Analysis", 1.0f to "1.0x Real-Time").forEach { (spd, label) ->
                        FilterChip(
                            selected = playbackSpeed == spd,
                            onClick = { playbackSpeed = spd },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.SlowMotionVideo,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AeroCyan.copy(alpha = 0.22f),
                                selectedLabelColor = AeroCyan
                            )
                        )
                    }

                    FilterChip(
                        selected = showJointAngles,
                        onClick = { showJointAngles = !showJointAngles },
                        label = { Text("Joint Angles", style = MaterialTheme.typography.labelSmall) }
                    )
                    FilterChip(
                        selected = showGrfVectors,
                        onClick = { showGrfVectors = !showGrfVectors },
                        label = { Text("GRF Force Vector", style = MaterialTheme.typography.labelSmall) }
                    )
                    FilterChip(
                        selected = showPlumbLine,
                        onClick = { showPlumbLine = !showPlumbLine },
                        label = { Text("COM Plumb Line", style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
        }

        // 3. Biomechanical Breakdown & Corrective Drills Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, SlateBorder, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SlateSurface)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = activeModule.focusMetricLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = TextSecondary
                    )
                    Text(
                        text = activeModule.optimalTargetRange,
                        style = MaterialTheme.typography.labelLarge,
                        fontFamily = TelemetryMonoFontFamily,
                        color = VoltLime
                    )
                }

                Surface(
                    color = ObsidianBackground,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SlateBorder, RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "COACHING CUE:",
                            style = MaterialTheme.typography.labelSmall,
                            color = VoltLime
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = activeModule.biomechanicalCue,
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "COMMON FAULT TO AVOID: ${activeModule.commonFault}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = KineticCoral
                        )
                    }
                }

                Text(
                    text = "CORRECTIVE NEUROMUSCULAR DRILLS (TAP TO CHECK OFF):",
                    style = MaterialTheme.typography.labelSmall,
                    color = AeroCyan
                )

                activeModule.drills.forEach { drill ->
                    val isDone = completedDrills.contains(drill)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(ObsidianBackground)
                            .clickable(
                                role = androidx.compose.ui.semantics.Role.Checkbox,
                                onClickLabel = "Tandai drill selesai"
                            ) {
                                if (isDone) completedDrills.remove(drill) else completedDrills.add(drill)
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isDone) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                            contentDescription = null,
                            tint = if (isDone) SuccessGreen else VoltLime,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = drill,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isDone) SuccessGreen else Color.White
                        )
                    }
                }
            }
        }

        // 4. Biomechanics Coach Interactive Q&A
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = null,
                        tint = VoltLime,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Pelatih & Analisis Biomekanika",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = coachQuestion,
                        onValueChange = { coachQuestion = it },
                        label = { Text("Tanyakan form lari, kadens, atau pencegahan cedera") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = VoltLime,
                            unfocusedBorderColor = SlateBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = VoltLime
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("biomechanics_coach_input")
                    )

                    Button(
                        onClick = { onAskCoach(coachQuestion, activeModule) },
                        enabled = coachQuestion.isNotBlank() && coachState !is CoachUiState.Loading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VoltLime,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .height(54.dp)
                            .testTag("ask_biomechanics_coach_button")
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = "Ask Coach")
                    }
                }

                when (coachState) {
                    is CoachUiState.Idle -> {}
                    is CoachUiState.Loading -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                color = VoltLime,
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Analyzing kinematic mechanics...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                    is CoachUiState.Streaming -> {
                        Surface(
                            color = ObsidianBackground,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = coachState.text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }
                    is CoachUiState.Success -> {
                        Surface(
                            color = ObsidianBackground,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, VoltLime.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        ) {
                            Text(
                                text = coachState.text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }
                    is CoachUiState.Error -> {
                        Text(
                            text = coachState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KinematicRunnerCanvas(
    gaitPhase: Float,
    module: BiomechanicsModule,
    showJointAngles: Boolean,
    showGrfVectors: Boolean,
    showPlumbLine: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val trackY = h * 0.84f

        // 1. Background Grid & Tartan Track
        val gridSpacing = 28.dp.toPx()
        var gx = 0f
        while (gx < w) {
            drawLine(
                color = SlateBorder.copy(alpha = 0.35f),
                start = Offset(gx, 0f),
                end = Offset(gx, trackY),
                strokeWidth = 1f
            )
            gx += gridSpacing
        }
        drawLine(
            color = AeroCyan.copy(alpha = 0.8f),
            start = Offset(0f, trackY),
            end = Offset(w, trackY),
            strokeWidth = 3.dp.toPx()
        )

        // Moving Track Tick Marks
        val tickSpacing = 44.dp.toPx()
        val tickOffset = (gaitPhase * tickSpacing * 2f) % tickSpacing
        var tx = -tickOffset
        while (tx < w) {
            drawLine(
                color = VoltLime.copy(alpha = 0.5f),
                start = Offset(tx, trackY + 4.dp.toPx()),
                end = Offset(tx - 10.dp.toPx(), trackY + 14.dp.toPx()),
                strokeWidth = 2.dp.toPx()
            )
            tx += tickSpacing
        }

        // 2. Compute Articulated Kinematic Joints from gaitPhase [0..1]
        val theta = (gaitPhase * 2.0 * PI).toFloat()
        val bounce = sin(theta * 2f) * 6.dp.toPx()
        val hipCenter = Offset(w * 0.48f, h * 0.47f - bounce)

        // Forward lean angle in radians
        val leanRad = (module.targetTrunkLeanDeg * PI / 180.0).toFloat()
        val torsoLen = h * 0.23f
        val shoulder = Offset(
            hipCenter.x + sin(leanRad) * torsoLen,
            hipCenter.y - cos(leanRad) * torsoLen
        )
        val headCenter = Offset(
            shoulder.x + sin(leanRad) * (h * 0.085f),
            shoulder.y - cos(leanRad) * (h * 0.085f)
        )

        // Center-of-Mass Plumb Line
        if (showPlumbLine) {
            drawLine(
                color = AeroCyan.copy(alpha = 0.55f),
                start = Offset(hipCenter.x, 20f),
                end = Offset(hipCenter.x, trackY),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
            )
        }

        val thighLen = h * 0.18f
        val shinLen = h * 0.18f

        fun computeLegJoints(phaseShift: Float): Triple<Offset, Offset, Offset> {
            val t = theta + phaseShift
            val hipSwingAngle = sin(t) * 0.62f + 0.12f
            val kneeBend = ((cos(t - 0.6f) + 1f) * 0.5f) * 1.15f + 0.15f
            val knee = Offset(
                hipCenter.x + sin(hipSwingAngle) * thighLen,
                hipCenter.y + cos(hipSwingAngle) * thighLen
            )
            val shinAngle = hipSwingAngle - kneeBend
            val ankle = Offset(
                knee.x + sin(shinAngle) * shinLen,
                (knee.y + cos(shinAngle) * shinLen).coerceAtMost(trackY - 4.dp.toPx())
            )
            val toe = Offset(
                ankle.x + 16.dp.toPx(),
                (ankle.y + 3.dp.toPx()).coerceAtMost(trackY)
            )
            return Triple(knee, ankle, toe)
        }

        // Far Leg (Dimmed Cyan) & Near Leg (Electric Volt Lime)
        val (farKnee, farAnkle, farToe) = computeLegJoints(PI.toFloat())
        val (nearKnee, nearAnkle, nearToe) = computeLegJoints(0f)

        val boneStroke = 5.dp.toPx()

        // Draw Far Leg
        drawLine(AeroCyan.copy(alpha = 0.45f), hipCenter, farKnee, boneStroke, StrokeCap.Round)
        drawLine(AeroCyan.copy(alpha = 0.45f), farKnee, farAnkle, boneStroke, StrokeCap.Round)
        drawLine(AeroCyan.copy(alpha = 0.45f), farAnkle, farToe, boneStroke, StrokeCap.Round)

        // Draw Torso & Head
        drawLine(Color.White, hipCenter, shoulder, boneStroke * 1.15f, StrokeCap.Round)
        drawCircle(VoltLime, radius = 11.dp.toPx(), center = headCenter)

        // Draw Near Leg
        drawLine(VoltLime, hipCenter, nearKnee, boneStroke, StrokeCap.Round)
        drawLine(VoltLime, nearKnee, nearAnkle, boneStroke, StrokeCap.Round)
        drawLine(KineticCoral, nearAnkle, nearToe, boneStroke, StrokeCap.Round)

        // Draw Compact Arm Swing
        val upperArmLen = h * 0.13f
        val forearmLen = h * 0.12f
        val armSwing = -sin(theta) * 0.65f
        val elbow = Offset(
            shoulder.x + sin(armSwing) * upperArmLen,
            shoulder.y + cos(armSwing) * upperArmLen
        )
        val forearmAngle = armSwing + 1.35f // ~85 degree compact elbow flexion
        val wrist = Offset(
            elbow.x + sin(forearmAngle) * forearmLen,
            elbow.y - cos(forearmAngle) * forearmLen * 0.35f
        )
        drawLine(AeroCyan, shoulder, elbow, boneStroke * 0.9f, StrokeCap.Round)
        drawLine(AeroCyan, elbow, wrist, boneStroke * 0.9f, StrokeCap.Round)

        // Joint Nodes
        if (showJointAngles) {
            listOf(hipCenter, shoulder, nearKnee, nearAnkle, elbow).forEach { joint ->
                drawCircle(Color.Black, radius = 6.dp.toPx(), center = joint)
                drawCircle(VoltLime, radius = 4.dp.toPx(), center = joint)
            }
        }

        // Ground Reaction Force (GRF) Vector when foot is near track contact
        if (showGrfVectors) {
            val contactFoot = if (nearAnkle.y >= farAnkle.y) nearAnkle else farAnkle
            if (contactFoot.y >= trackY - 22.dp.toPx()) {
                val forceStart = Offset(contactFoot.x + 6.dp.toPx(), trackY)
                val forceEnd = Offset(contactFoot.x - 24.dp.toPx(), trackY - 56.dp.toPx())
                drawLine(
                    color = KineticCoral,
                    start = forceStart,
                    end = forceEnd,
                    strokeWidth = 3.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawCircle(
                    color = KineticCoral,
                    radius = 5.dp.toPx(),
                    center = forceEnd
                )
            }
        }
    }
}
