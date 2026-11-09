package com.example.domain

import com.example.data.model.BiomechanicsModule

object BiomechanicsCatalog {

    val modules: List<BiomechanicsModule> = listOf(
        BiomechanicsModule(
            id = "midfoot_strike",
            title = "Midfoot Strike & Tibial Alignment",
            subtitle = "Eliminate heel-striking braking forces by landing directly beneath your center of mass.",
            focusMetricLabel = "Tibial Inclination at Contact",
            optimalTargetRange = "0° to +4° (Vertical Shin)",
            commonFault = "Overstriding with extended knee (+14° tibial angle), creating high eccentric braking impulse.",
            biomechanicalCue = "Imagine pedaling a bicycle backward—paw the track down and back so your foot touches down directly under your hips.",
            drills = listOf(
                "2 x 30m Barefoot Grass Cadence Paws",
                "3 x 40m A-Skip to Running Transition",
                "Metronome 182 SPM Short-Lever Strides"
            ),
            targetKneeFlexionDeg = 162,
            targetTibialAngleDeg = 2,
            targetTrunkLeanDeg = 8,
            targetCadenceSpm = 182
        ),
        BiomechanicsModule(
            id = "cadence_recoil",
            title = "180+ SPM Cadence & Achilles Recoil",
            subtitle = "Shorten ground contact time (<210ms) to harness free elastic energy from the Achilles tendon.",
            focusMetricLabel = "Ground Contact Time (GCT)",
            optimalTargetRange = "175ms – 205ms @ 180-188 SPM",
            commonFault = "Low cadence (<166 SPM) with bounding vertical oscillation (>10.5 cm) and energy dissipation.",
            biomechanicalCue = "Run on hot coals—focus on rapid, light ankle stiffness and instant heel lift right after mid-stance.",
            drills = listOf(
                "3 x 60s In-Place Ankling Hops (Stiff Ankle)",
                "4 x 100m +5% Cadence Metronome Pickups",
                "2 x 20 Single-Leg Pogo Jumps"
            ),
            targetKneeFlexionDeg = 158,
            targetTibialAngleDeg = 3,
            targetTrunkLeanDeg = 7,
            targetCadenceSpm = 186
        ),
        BiomechanicsModule(
            id = "forward_lean",
            title = "Ankle-Driven Forward Lean",
            subtitle = "Use gravity for horizontal propulsion by leaning from the ankles rather than collapsing at the waist.",
            focusMetricLabel = "Whole-Body Forward Tilt",
            optimalTargetRange = "6° to 9° from Ankle Axis",
            commonFault = "Hinging at the lumbar spine ('sitting in the bucket'), restricting diaphragm expansion and hip extension.",
            biomechanicalCue = "Stand tall with a string pulling the crown of your head skyward, then press your hips slightly forward until gravity initiates your stride.",
            drills = listOf(
                "4 x Falling Starts from Standing Tall",
                "3 x 50m Wall-Lean March & Acceleration",
                "Thoracic Tall-Posture Float Strides"
            ),
            targetKneeFlexionDeg = 160,
            targetTibialAngleDeg = 1,
            targetTrunkLeanDeg = 9,
            targetCadenceSpm = 180
        ),
        BiomechanicsModule(
            id = "pelvic_neutrality",
            title = "Pelvic Neutrality & Triple Extension",
            subtitle = "Lock in core stability to transfer 100% of glute and hamstring torque into forward velocity.",
            focusMetricLabel = "Peak Hip Extension Angle",
            optimalTargetRange = "18° to 24° Behind Vertical",
            commonFault = "Anterior pelvic tilt (arched lower back), cutting off trailing-leg hip extension and overloading hip flexors.",
            biomechanicalCue = "Level your belt buckle like a bowl of water—squeeze your glute at toe-off to drive the ground behind you.",
            drills = listOf(
                "2 x 15 Glute Bridge Marching Holds",
                "3 x 40m High-Knee B-Skips with Hip Lock",
                "4 x 60m Hill Bounds for Triple Extension"
            ),
            targetKneeFlexionDeg = 165,
            targetTibialAngleDeg = 2,
            targetTrunkLeanDeg = 8,
            targetCadenceSpm = 182
        ),
        BiomechanicsModule(
            id = "compact_arm_swing",
            title = "Compact Arm Swing & Counter-Rotation",
            subtitle = "Balance lower-body angular momentum with compact 85° elbow drive back toward the hip pocket.",
            focusMetricLabel = "Elbow Flexion & Drive Arc",
            optimalTargetRange = "80°–90° Flexion (Zero Midline Cross)",
            commonFault = "Arms crossing the sternum midline, inducing torso torque and lateral side-to-side energy waste.",
            biomechanicalCue = "Brush your thumbs lightly past your hip pockets on the backswing—hands relaxed as if holding a potato chip without crushing it.",
            drills = listOf(
                "3 x 45s Seated Rapid Arm-Drive Drill",
                "4 x 80m Relaxed-Jaw & Dropped-Shoulder Strides",
                "2 x 30m Band-Resisted Elbow Back-Drive"
            ),
            targetKneeFlexionDeg = 161,
            targetTibialAngleDeg = 2,
            targetTrunkLeanDeg = 8,
            targetCadenceSpm = 184
        )
    )

    fun phaseLabelForProgress(phase: Float): String {
        val normalized = (phase % 1f + 1f) % 1f
        return when {
            normalized < 0.20f -> "Phase 1: Initial Midfoot Contact (GCT Entry)"
            normalized < 0.45f -> "Phase 2: Mid-Stance Elastic Loading"
            normalized < 0.70f -> "Phase 3: Triple Extension & Toe-Off Propulsion"
            else -> "Phase 4: Aerial Float & High Knee Recovery"
        }
    }
}
