package com.example.domain

import com.example.data.model.WorkoutLog
import kotlin.math.roundToLong

/**
 * Data model for a specific running shoe recommendation.
 */
data class ShoeModel(
    val name: String,
    val brand: String,
    val plateType: String,
    val stackHeight: String,
    val dropMm: Double,
    val weightG: Int? = null,
    val bestFor: String,
    val description: String,
    val imageUrl: String? = null,
    val slug: String = ShoeAffiliateLinks.slugify(brand, name),
    val shopeeAffiliateUrl: String? = null
)

/**
 * Tier definition for Shoe Unlock progression.
 */
data class ShoeTier(
    val id: Int,
    val title: String,
    val subtitle: String,
    val unlockPaceSecondsPerKm: Long,
    val unlockPaceDisplay: String,
    val badgeLabel: String,
    val description: String,
    val biomechanicalRationale: String,
    val shoes: List<ShoeModel>
)

/**
 * State of a tier for a specific runner.
 */
data class ShoeTierState(
    val tier: ShoeTier,
    val isUnlocked: Boolean,
    val isCurrentHighest: Boolean
)

/**
 * Aggregate progress of the runner in the shoe unlock progression system.
 */
data class ShoeUnlockProgress(
    val currentAvgPaceSec: Long,
    val totalWorkoutsEvaluated: Int,
    val currentTier: ShoeTier,
    val nextTier: ShoeTier?,
    val progressToNextTier: Float,
    val secondsNeededToNextTier: Long,
    val tiers: List<ShoeTierState>
)

/**
 * Domain engine that evaluates workout history and determines shoe tier unlocks.
 */
object ShoeUnlockEngine {

    val TIERS = listOf(
        // Tier 1: Daily Cushion & Recovery
        ShoeTier(
            id = 1,
            title = "Daily Cushion & Recovery Trainers",
            subtitle = "Aerobic Base, Easy Runs & Recovery",
            unlockPaceSecondsPerKm = 9999L,
            unlockPaceDisplay = "Semua Pace",
            badgeLabel = "Tier 1 • Cushion",
            description = "Pilihan sepatu Daily Running / Easy terkurasi terkurasi berbusa tebal untuk menyerap benturan, melindungi sendi, dan membangun fondasi aerobik.",
            biomechanicalRationale = "Pada fase membangun base atau recovery, kaki membutuhkan peredaman benturan maksimal tanpa pelat kaku agar adaptasi tulang, tendon, dan kapiler berjalan optimal.",
            shoes = listOf(
                ShoeModel(
                    name = "Vomero Plus",
                    brand = "Nike",
                    plateType = "Non-Plated · Full-Length ZoomX (Skor Lab 91)",
                    stackHeight = "42.3 / 32.7 mm",
                    dropMm = 9.6,
                    weightG = 289,
                    bestFor = "Daily Running, Recovery Days & Relaxed Runs",
                    description = "Midsole full-length ZoomX berkonsep maksimalis dengan upper super nyaman dan konstruksi awet untuk menjaga kaki tetap segar saat pemulihan.",
                    imageUrl = "https://cdn.runrepeat.com/storage/gallery/product_primary/40912/nike-vomero-plus-23479827-380.jpg"
                ),
                ShoeModel(
                    name = "Vomero 18",
                    brand = "Nike",
                    plateType = "Non-Plated · ZoomX Cushion (Skor Lab 90)",
                    stackHeight = "42.5 / 28.6 mm",
                    dropMm = 13.9,
                    weightG = 298,
                    bestFor = "Daily Running, Max Cushion & Heel Strikers",
                    description = "Desain ulang total dengan tumpukan busa ZoomX lebih tebal, step-in comfort ekstra empuk, serta drop besar untuk meredam hentakan tumit.",
                    imageUrl = "https://cdn.runrepeat.com/storage/gallery/product_primary/40615/nike-vomero-18-23005571-380.jpg"
                ),
                ShoeModel(
                    name = "Clifton 10",
                    brand = "HOKA",
                    plateType = "Non-Plated · Supercritical EVA (Skor Lab 86)",
                    stackHeight = "44.4 / 32.0 mm",
                    dropMm = 12.4,
                    weightG = 275,
                    bestFor = "Daily Running, Stable Base Miles",
                    description = "Generasi ke-10 berstack maksimalis 44.4 mm dengan toebox lebih lega dan platform stabil untuk lari harian jarak ringan hingga menengah.",
                    imageUrl = "https://cdn.runrepeat.com/storage/gallery/product_primary/40691/hoka-clifton-10-22918659-380.jpg"
                ),
                ShoeModel(
                    name = "Bondi 9",
                    brand = "HOKA",
                    plateType = "Non-Plated · Supercritical EVA (Skor Lab 86)",
                    stackHeight = "41.3 / 32.2 mm",
                    dropMm = 9.1,
                    weightG = 303,
                    bestFor = "Daily Running, Recovery & Joint Protection",
                    description = "Lebih ringan dan lebih awet dibanding v8 dengan busa supercritical lebih membal dan material upper premium.",
                    imageUrl = "https://cdn.runrepeat.com/storage/gallery/product_primary/40639/hoka-bondi-9-22560581-380.jpg"
                )
            )
        ),

        // Tier 2: Lightweight Performance & Versatile Tempo
        ShoeTier(
            id = 2,
            title = "Lightweight Daily & Tempo Trainers",
            subtitle = "Cadence Turnover, Fartlek & Mixed Pace",
            unlockPaceSecondsPerKm = 420L, // 7:00 /km
            unlockPaceDisplay = "Pace < 7:00/km",
            badgeLabel = "Tier 2 • Performance",
            description = "Sepatu Daily & Tempo ringan pilihan terbaik dengan busa responsif untuk meningkatkan pergantian langkah (cadence) saat pace mulai menanjak.",
            biomechanicalRationale = "Mengurangi bobot sepatu ke kisaran 207–254 gram meringankan beban rotasi ayunan kaki sehingga turnover langkah >170 spm terasa lebih natural.",
            shoes = listOf(
                ShoeModel(
                    name = "Adizero EVO SL",
                    brand = "adidas",
                    plateType = "Non-Plated · 100% Lightstrike Pro (Skor Lab 94)",
                    stackHeight = "36.1 / 28.1 mm",
                    dropMm = 8.0,
                    weightG = 223,
                    bestFor = "Daily Running, Tempo & Versatile Paces",
                    description = "Membawa 100% busa balap Lightstrike Pro dan outsole karet Continental dalam bobot hanya 223 g dengan fleksibilitas pace luar biasa.",
                    imageUrl = "https://cdn.runrepeat.com/storage/gallery/product_primary/40586/adidas-adizero-evo-sl-22256345-380.jpg"
                ),
                ShoeModel(
                    name = "Novablast 5",
                    brand = "ASICS",
                    plateType = "Non-Plated · FF Blast+ ECO (Skor Lab 93)",
                    stackHeight = "40.9 / 33.5 mm",
                    dropMm = 7.4,
                    weightG = 254,
                    bestFor = "Daily Running, Tempo & Long Steady",
                    description = "Lebih ringan, empuk, dan membal dibanding pendahulunya dengan stabilitas mengejutkan untuk membangun kecepatan harian.",
                    imageUrl = "https://cdn.runrepeat.com/storage/gallery/product_primary/40608/asics-novablast-5-22312736-380.jpg"
                ),
                ShoeModel(
                    name = "Mach 7",
                    brand = "HOKA",
                    plateType = "Non-Plated · Supercritical EVA (Skor Lab 90)",
                    stackHeight = "35.6 / 26.2 mm",
                    dropMm = 9.4,
                    weightG = 224,
                    bestFor = "Daily Running, Tempo & Cadence Drills",
                    description = "Mempertahankan DNA super lincah berbobot 224 g dengan peningkatan upper dan daya cengkeram outsole untuk latihan tempo cepat.",
                    imageUrl = "https://cdn.runrepeat.com/storage/gallery/product_primary/41083/hoka-mach-7-24969866-380.jpg"
                ),
                ShoeModel(
                    name = "SuperComp Rebel",
                    brand = "New Balance",
                    plateType = "Carbon Plate · FuelCell (Skor Lab 94)",
                    stackHeight = "38.3 / 29.5 mm",
                    dropMm = 8.8,
                    weightG = 207,
                    bestFor = "Tempo, Speed Play & Race Prep",
                    description = "Ringan 207 g dengan busa FuelCell berpantulan tinggi dan traksi kuat yang membawa seri trainer ke level performa balap.",
                    imageUrl = "https://cdn.runrepeat.com/storage/gallery/product_primary/41475/new-balance-super-comp-rebel-25437549-380.jpg"
                )
            )
        ),

        // Tier 3: Super Trainers
        ShoeTier(
            id = 3,
            title = "Super Trainers & Tempo Workhorses",
            subtitle = "Lactate Threshold & Marathon Pace",
            unlockPaceSecondsPerKm = 360L, // 6:00 /km
            unlockPaceDisplay = "Pace < 6:00/km",
            badgeLabel = "Tier 3 • Super Trainer",
            description = "Kumpulan Super Trainer skor tertinggi terbaik: tumpukan superfoam PEBA/LEAP dan/atau pelat nilon-karbon untuk menjaga pace threshold tetap efisien.",
            biomechanicalRationale = "Kombinasi stack 37–46 mm berdensitas rendah dan geometri propulsif menjaga ekonomi lari serta meminimalkan kelelahan otot betis pada pace tempo panjang.",
            shoes = listOf(
                ShoeModel(
                    name = "Megablast",
                    brand = "ASICS",
                    plateType = "Non-Plated · FF Turbo (Skor Lab 95)",
                    stackHeight = "45.1 / 35.2 mm",
                    dropMm = 9.9,
                    weightG = 218,
                    bestFor = "Tempo, Competition & Long Fast Runs",
                    description = "Peraih skor performa tertinggi (95/100): energy return eksplosif, peredaman benturan superior, grip kelas satu, dan bobot sangat ringan 218 g.",
                    imageUrl = "https://cdn.runrepeat.com/storage/gallery/product_primary/40948/asics-megablast-23653319-380.jpg"
                ),
                ShoeModel(
                    name = "Superblast 3",
                    brand = "ASICS",
                    plateType = "Non-Plated · FF LEAP (Skor Lab 94)",
                    stackHeight = "45.8 / 37.7 mm",
                    dropMm = 8.1,
                    weightG = 235,
                    bestFor = "Daily Running, Threshold & Marathon Pace",
                    description = "Super trainer berbusa FF LEAP setebal 45.8 mm dengan grip lebih menggigit dan bobot turun ke 235 g sambil tetap stabil.",
                    imageUrl = "https://cdn.runrepeat.com/storage/gallery/product_primary/41200/asics-superblast-3-24636359-380.jpg"
                ),
                ShoeModel(
                    name = "Endorphin Speed 5",
                    brand = "Saucony",
                    plateType = "Winged Nylon Plate · PWRRUN PB (Skor Lab 94)",
                    stackHeight = "37.4 / 26.8 mm",
                    dropMm = 10.6,
                    weightG = 241,
                    bestFor = "Tempo Runs, Threshold & Half Marathon",
                    description = "Pelat nilon bersayap dan busa PWRRUN PB menghasilkan pantulan kelas dunia dengan sirkulasi udara upper yang sangat sejuk.",
                    imageUrl = "https://cdn.runrepeat.com/storage/gallery/product_primary/40830/saucony-endorphin-speed-5-23261039-380.jpg"
                ),
                ShoeModel(
                    name = "Superblast 2",
                    brand = "ASICS",
                    plateType = "Non-Plated · FF Turbo+ / Blast+ (Skor Lab 91)",
                    stackHeight = "42.8 / 34.6 mm",
                    dropMm = 8.2,
                    weightG = 252,
                    bestFor = "Tempo, Marathon Pace & High Mileage",
                    description = "Salah satu trainer paling serbaguna dengan midsole bertenaga dan outsole solid untuk menjaga konsistensi pace panjang.",
                    imageUrl = "https://cdn.runrepeat.com/storage/gallery/product_primary/40450/asics-superblast-2-21749161-380.jpg"
                ),
                ShoeModel(
                    name = "Zoom Fly 6",
                    brand = "Nike",
                    plateType = "Carbon Plate · ZoomX (Skor Lab 88)",
                    stackHeight = "39.7 / 30.1 mm",
                    dropMm = 9.6,
                    weightG = 248,
                    bestFor = "Tempo, Threshold Intervals & Long Fast",
                    description = "Memadukan busa ZoomX responsif, pelat karbon, serta durabilitas outsole tinggi sebagai pasangan latihan super-shoe harian.",
                    imageUrl = "https://cdn.runrepeat.com/storage/gallery/product_primary/40613/nike-zoom-fly-6-22464236-380.jpg"
                ),
                ShoeModel(
                    name = "FuelCell SuperComp Trainer v3",
                    brand = "New Balance",
                    plateType = "Carbon Plate · FuelCell (Skor Lab 89)",
                    stackHeight = "36.8 / 29.5 mm",
                    dropMm = 7.3,
                    weightG = 278,
                    bestFor = "Tempo Workouts & High-Stability Long Runs",
                    description = "Versi paling stabil dan bertenaga di lini SC Trainer dengan grip sangat baik untuk volume latihan kecepatan mingguan.",
                    imageUrl = "https://cdn.runrepeat.com/storage/gallery/product_primary/40405/new-balance-fuel-cell-super-comp-trainer-v-3-21926162-380.jpg"
                )
            )
        ),

        // Tier 4: Carbon Plated Racers
        ShoeTier(
            id = 4,
            title = "Carbon Plated Competition Racers",
            subtitle = "Race Day 5K, 10K, HM & Marathon PRs",
            unlockPaceSecondsPerKm = 300L, // 5:00 /km
            unlockPaceDisplay = "Pace < 5:00/km",
            badgeLabel = "Tier 4 • Carbon Racer",
            description = "Sepatu balap Competition / Race teruji terbaik dengan pelat serat karbon penuh berbobot sub-215 gram untuk memecahkan rekor personal lomba.",
            biomechanicalRationale = "Pelat serat karbon kaku mengurangi tekukan berlebih pada sendi MTP telapak depan dan melontarkan kembali energi kompresi busa super di tiap langkah.",
            shoes = listOf(
                ShoeModel(
                    name = "Vaporfly 4",
                    brand = "Nike",
                    plateType = "Carbon Plate · ZoomX (Skor Lab 93)",
                    stackHeight = "34.1 / 25.5 mm",
                    dropMm = 8.6,
                    weightG = 166,
                    bestFor = "Competition 5K, 10K & Half Marathon PR",
                    description = "Super-shoe super ringan 166 g dengan penyempurnaan lidah/fit upper dan transisi sangat natural untuk memecahkan PR jarak pendek-menengah.",
                    imageUrl = "https://cdn.runrepeat.com/storage/gallery/product_primary/40754/nike-vaporfly-4-22860848-380.jpg"
                ),
                ShoeModel(
                    name = "Adizero Adios Pro 4",
                    brand = "adidas",
                    plateType = "Carbon EnergyRods · Lightstrike Pro (Skor Lab 93)",
                    stackHeight = "36.6 / 28.5 mm",
                    dropMm = 8.1,
                    weightG = 200,
                    bestFor = "Competition Half & Full Marathon Race",
                    description = "Menghadirkan batang karbon EnergyRods dengan busa Lightstrike Pro lebih empuk dan ledakan dorongan ke depan di bobot tepat 200 g.",
                    imageUrl = "https://cdn.runrepeat.com/storage/gallery/product_primary/40584/adidas-adizero-adios-pro-4-22483965-380.jpg"
                ),
                ShoeModel(
                    name = "Endorphin Pro 5",
                    brand = "Saucony",
                    plateType = "Carbon Plate · PWRRUN PB (Skor Lab 91)",
                    stackHeight = "36.5 / 27.8 mm",
                    dropMm = 8.7,
                    weightG = 215,
                    bestFor = "Competition Marathon & Heel-Strike Stability",
                    description = "Super shoe lomba berpelat karbon paling ramah kestabilan, nyaman untuk heel striker dan percaya diri pada tikungan maupun jalan basah.",
                    imageUrl = "https://cdn.runrepeat.com/storage/gallery/product_primary/41067/saucony-endorphin-pro-5-24665925-380.jpg"
                )
            )
        ),

        // Tier 5: Elite Pinnacle Alpha Super-Weapons
        ShoeTier(
            id = 5,
            title = "Elite Pinnacle Competition Super-Shoes",
            subtitle = "Sub-4:00 Pace, Podium & Record Tech",
            unlockPaceSecondsPerKm = 240L, // 4:00 /km
            unlockPaceDisplay = "Pace < 4:00/km",
            badgeLabel = "Tier 5 • Alpha Weapon",
            description = "Kasta kompetisi tertinggi terkurasi: super-shoe ultra-ekstrem sub-190 g hingga sub-100 g untuk efisiensi biomekanika maksimal pada pace elit.",
            biomechanicalRationale = "Pada kecepatan >15 km/jam, gaya kontak kaki cukup besar untuk memeras kompresi penuh superfoam generasi baru sambil menjaga bobot serendah mungkin.",
            shoes = listOf(
                ShoeModel(
                    name = "Alphafly 4",
                    brand = "Nike",
                    plateType = "Carbon Plate · ZoomX (Skor Lab 94)",
                    stackHeight = "39.0 / 32.8 mm",
                    dropMm = 6.2,
                    weightG = 190,
                    bestFor = "Competition Marathon Record & Podium",
                    description = "Alphafly terbaik di kelasnya: lebih ringan (190 g), pantulan lebih bertenaga, traksi membaik, dan lock-down Atomknit paling aman.",
                    imageUrl = "https://cdn.runrepeat.com/storage/gallery/product_primary/41531/nike-alphafly-4-25929108-380.jpg"
                ),
                ShoeModel(
                    name = "Metaspeed Sky Tokyo",
                    brand = "ASICS",
                    plateType = "Carbon Plate · FF LEAP (Skor Lab 92)",
                    stackHeight = "38.7 / 32.7 mm",
                    dropMm = 6.0,
                    weightG = 163,
                    bestFor = "Competition Elite Road & Full Marathon",
                    description = "Konstruksi super ringan 163 g dengan ventilasi maksimal, busa FF LEAP empuk, dan pelat karbon agresif untuk mempertahankan kecepatan puncak.",
                    imageUrl = "https://cdn.runrepeat.com/storage/gallery/product_primary/40812/asics-metaspeed-sky-tokyo-23429911-380.jpg"
                ),
                ShoeModel(
                    name = "Adizero Adios Pro Evo 3",
                    brand = "adidas",
                    plateType = "Carbon Plate · Lightstrike Pro Evo (Skor Lab 91)",
                    stackHeight = "38.1 / 32.2 mm",
                    dropMm = 5.9,
                    weightG = 99,
                    bestFor = "Elite Championship & Sub-2h Architecture",
                    description = "Inovasi ultra-ekstrem berbobot 99 g di timbangan lab dengan stack 38.1 mm, menghadirkan energy return dan shock absorption level puncak dunia.",
                    imageUrl = "https://cdn.runrepeat.com/storage/gallery/product_primary/41285/adidas-adizero-adios-pro-evo-3-24813691-380.jpg"
                )
            )
        )
    )

    /**
     * Evaluates recent quality workout logs (Top 3 fastest qualifying >= 3 km efforts in the last 30 days)
     * and computes the runner's shoe tier progression without easy/long runs dragging down the unlock pace.
     */
    fun evaluateProgress(
        workouts: List<WorkoutLog>,
        nowEpochMs: Long = System.currentTimeMillis()
    ): ShoeUnlockProgress {
        val validWorkouts = workouts.filter { it.distanceKm >= 0.5 && it.durationSeconds >= 60L }

        val thirtyDaysAgoMs = nowEpochMs - 30L * 24L * 60L * 60L * 1000L
        val recentRuns = validWorkouts.filter { w ->
            val epoch = w.createdAt?.toDate()?.time ?: nowEpochMs
            epoch >= thirtyDaysAgoMs
        }.ifEmpty { validWorkouts }

        val qualifyingRuns = recentRuns.filter { it.distanceKm >= 3.0 }.ifEmpty { recentRuns }
        val topRuns = qualifyingRuns
            .sortedBy { w -> w.durationSeconds.toDouble() / w.distanceKm }
            .take(3)

        val overallAvgPace = if (topRuns.isNotEmpty()) {
            topRuns.map { w -> w.durationSeconds.toDouble() / w.distanceKm }.average().roundToLong()
        } else {
            0L
        }

        // Determine unlocked tiers
        val tierStates = TIERS.map { tier ->
            val isUnlocked = when {
                tier.id == 1 -> true // Tier 1 always unlocked
                overallAvgPace > 0L && overallAvgPace <= tier.unlockPaceSecondsPerKm -> true
                else -> false
            }
            ShoeTierState(
                tier = tier,
                isUnlocked = isUnlocked,
                isCurrentHighest = false
            )
        }

        // Find current highest unlocked tier
        val highestUnlockedIndex = tierStates.indexOfLast { it.isUnlocked }.coerceAtLeast(0)
        val updatedTierStates = tierStates.mapIndexed { index, state ->
            state.copy(isCurrentHighest = index == highestUnlockedIndex)
        }

        val currentTier = TIERS[highestUnlockedIndex]
        val nextTier = if (highestUnlockedIndex < TIERS.size - 1) TIERS[highestUnlockedIndex + 1] else null

        // Compute progress to next tier
        val (progressRatio, secondsNeeded) = if (nextTier != null && overallAvgPace > 0L) {
            val targetPace = nextTier.unlockPaceSecondsPerKm
            val prevTargetPace = currentTier.unlockPaceSecondsPerKm.coerceAtMost(600L) // cap at 10:00/km for math
            val diffFromTarget = overallAvgPace - targetPace
            if (diffFromTarget <= 0L) {
                1.0f to 0L
            } else {
                val span = (prevTargetPace - targetPace).coerceAtLeast(30L)
                val done = (prevTargetPace - overallAvgPace).coerceAtLeast(0L)
                val ratio = (done.toFloat() / span.toFloat()).coerceIn(0.05f, 0.95f)
                ratio to diffFromTarget
            }
        } else if (nextTier == null) {
            1.0f to 0L
        } else {
            // No runs yet
            0.0f to (nextTier.unlockPaceSecondsPerKm)
        }

        return ShoeUnlockProgress(
            currentAvgPaceSec = overallAvgPace,
            totalWorkoutsEvaluated = topRuns.size,
            currentTier = currentTier,
            nextTier = nextTier,
            progressToNextTier = progressRatio,
            secondsNeededToNextTier = secondsNeeded,
            tiers = updatedTierStates
        )
    }

    fun findShoeBySlug(progress: ShoeUnlockProgress, slug: String): Pair<ShoeModel, ShoeTierState>? {
        val target = slug.trim().lowercase()
        for (tierState in progress.tiers) {
            for (shoe in tierState.tier.shoes) {
                if (shoe.slug.lowercase() == target) {
                    return shoe to tierState
                }
            }
        }
        return null
    }
}
