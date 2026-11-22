import {
  DISTANCE_CATEGORIES,
  dayNameId,
  formatDuration,
  formatPace,
  formatPaceShort,
  getDistanceCategoryInfo,
  parseSplitsSummary,
  stripScheduleSuffix,
} from "./formatters";
import type {
  BiomechanicsModule,
  ComprehensiveAnalyticsResponse,
  DailyFitnessPoint,
  ExtractedStravaMetrics,
  GoalScheduleWithSummary,
  HeatmapDay,
  MonthlyTrend,
  PersonalRecord,
  RacePrediction,
  ScheduledSession,
  ShoeTier,
  ShoeUnlockProgress,
  TrainingGoal,
  TrainingPaceZone,
  WeeklyAnalytics,
  WorkoutDeepMetrics,
  WorkoutLog,
} from "./types";

export const SHOE_TIERS_CATALOG: ShoeTier[] = [
  {
    id: 1,
    title: "Daily Cushion & Recovery Trainers",
    subtitle: "Aerobic Base, Easy Runs & Recovery",
    unlockPaceSecondsPerKm: 9999,
    unlockPaceDisplay: "Semua Pace",
    badgeLabel: "Tier 1 • Cushion",
    description:
      "Pilihan sepatu Daily Running / Easy terkurasi berbusa tebal untuk menyerap benturan, melindungi sendi, dan membangun fondasi aerobik.",
    biomechanicalRationale:
      "Pada fase membangun base atau recovery, kaki membutuhkan peredaman benturan maksimal tanpa pelat kaku agar adaptasi tulang, tendon, dan kapiler berjalan optimal.",
    shoes: [
      {
        name: "Vomero Plus",
        brand: "Nike",
        plateType: "Non-Plated · Full-Length ZoomX (Skor Lab 91)",
        stackHeight: "42.3 / 32.7 mm",
        dropMm: 9.6,
        weightG: 289,
        bestFor: "Daily Running, Recovery Days & Relaxed Runs",
        description:
          "Midsole full-length ZoomX berkonsep maksimalis dengan upper super nyaman dan konstruksi awet untuk menjaga kaki tetap segar saat pemulihan.",
        imageUrl: "https://cdn.runrepeat.com/storage/gallery/product_primary/40912/nike-vomero-plus-23479827-380.jpg",
        slug: "nike-vomero-plus",
      },
      {
        name: "Vomero 18",
        brand: "Nike",
        plateType: "Non-Plated · ZoomX Cushion (Skor Lab 90)",
        stackHeight: "42.5 / 28.6 mm",
        dropMm: 13.9,
        weightG: 298,
        bestFor: "Daily Running, Max Cushion & Heel Strikers",
        description:
          "Desain ulang total dengan tumpukan busa ZoomX lebih tebal, step-in comfort ekstra empuk, serta drop besar untuk meredam hentakan tumit.",
        imageUrl: "https://cdn.runrepeat.com/storage/gallery/product_primary/40615/nike-vomero-18-23005571-380.jpg",
        slug: "nike-vomero-18",
      },
      {
        name: "Clifton 10",
        brand: "HOKA",
        plateType: "Non-Plated · Supercritical EVA (Skor Lab 86)",
        stackHeight: "44.4 / 32.0 mm",
        dropMm: 12.4,
        weightG: 275,
        bestFor: "Daily Running, Stable Base Miles",
        description:
          "Generasi ke-10 berstack maksimalis 44.4 mm dengan toebox lebih lega dan platform stabil untuk lari harian jarak ringan hingga menengah.",
        imageUrl: "https://cdn.runrepeat.com/storage/gallery/product_primary/40691/hoka-clifton-10-22918659-380.jpg",
        slug: "hoka-clifton-10",
      },
      {
        name: "Bondi 9",
        brand: "HOKA",
        plateType: "Non-Plated · Supercritical EVA (Skor Lab 86)",
        stackHeight: "41.3 / 32.2 mm",
        dropMm: 9.1,
        weightG: 303,
        bestFor: "Daily Running, Recovery & Joint Protection",
        description:
          "Lebih ringan dan lebih awet dibanding v8 dengan busa supercritical lebih membal dan material upper premium.",
        imageUrl: "https://cdn.runrepeat.com/storage/gallery/product_primary/40639/hoka-bondi-9-22560581-380.jpg",
        slug: "hoka-bondi-9",
      },
    ],
  },
  {
    id: 2,
    title: "Lightweight Daily & Tempo Trainers",
    subtitle: "Cadence Turnover, Fartlek & Mixed Pace",
    unlockPaceSecondsPerKm: 420,
    unlockPaceDisplay: "Pace < 7:00/km",
    badgeLabel: "Tier 2 • Performance",
    description:
      "Sepatu Daily & Tempo ringan pilihan terbaik dengan busa responsif untuk meningkatkan pergantian langkah (cadence) saat pace mulai menanjak.",
    biomechanicalRationale:
      "Mengurangi bobot sepatu ke kisaran 207–254 gram meringankan beban rotasi ayunan kaki sehingga turnover langkah >170 spm terasa lebih natural.",
    shoes: [
      {
        name: "Adizero EVO SL",
        brand: "adidas",
        plateType: "Non-Plated · 100% Lightstrike Pro (Skor Lab 94)",
        stackHeight: "36.1 / 28.1 mm",
        dropMm: 8.0,
        weightG: 223,
        bestFor: "Daily Running, Tempo & Versatile Paces",
        description:
          "Membawa 100% busa balap Lightstrike Pro dan outsole karet Continental dalam bobot hanya 223 g dengan fleksibilitas pace luar biasa.",
        imageUrl: "https://cdn.runrepeat.com/storage/gallery/product_primary/40586/adidas-adizero-evo-sl-22256345-380.jpg",
        slug: "adidas-adizero-evo-sl",
      },
      {
        name: "Novablast 5",
        brand: "ASICS",
        plateType: "Non-Plated · FF Blast+ ECO (Skor Lab 93)",
        stackHeight: "40.9 / 33.5 mm",
        dropMm: 7.4,
        weightG: 254,
        bestFor: "Daily Running, Tempo & Long Steady",
        description:
          "Lebih ringan, empuk, dan membal dibanding pendahulunya dengan stabilitas mengejutkan untuk membangun kecepatan harian.",
        imageUrl: "https://cdn.runrepeat.com/storage/gallery/product_primary/40608/asics-novablast-5-22312736-380.jpg",
        slug: "asics-novablast-5",
      },
      {
        name: "Mach 7",
        brand: "HOKA",
        plateType: "Non-Plated · Supercritical EVA (Skor Lab 90)",
        stackHeight: "35.6 / 26.2 mm",
        dropMm: 9.4,
        weightG: 224,
        bestFor: "Daily Running, Tempo & Cadence Drills",
        description:
          "Mempertahankan DNA super lincah berbobot 224 g dengan peningkatan upper dan daya cengkeram outsole untuk latihan tempo cepat.",
        imageUrl: "https://cdn.runrepeat.com/storage/gallery/product_primary/41083/hoka-mach-7-24969866-380.jpg",
        slug: "hoka-mach-7",
      },
      {
        name: "SuperComp Rebel",
        brand: "New Balance",
        plateType: "Carbon Plate · FuelCell (Skor Lab 94)",
        stackHeight: "38.3 / 29.5 mm",
        dropMm: 8.8,
        weightG: 207,
        bestFor: "Tempo, Speed Play & Race Prep",
        description:
          "Ringan 207 g dengan busa FuelCell berpantulan tinggi dan traksi kuat yang membawa seri trainer ke level performa balap.",
        imageUrl: "https://cdn.runrepeat.com/storage/gallery/product_primary/41475/new-balance-super-comp-rebel-25437549-380.jpg",
        slug: "new-balance-supercomp-rebel",
      },
    ],
  },
  {
    id: 3,
    title: "Super Trainers & Tempo Workhorses",
    subtitle: "Lactate Threshold & Marathon Pace",
    unlockPaceSecondsPerKm: 360,
    unlockPaceDisplay: "Pace < 6:00/km",
    badgeLabel: "Tier 3 • Super Trainer",
    description:
      "Kumpulan Super Trainer skor tertinggi terbaik: tumpukan superfoam PEBA/LEAP dan/atau pelat nilon-karbon untuk menjaga pace threshold tetap efisien.",
    biomechanicalRationale:
      "Kombinasi stack 37–46 mm berdensitas rendah dan geometri propulsif menjaga ekonomi lari serta meminimalkan kelelahan otot betis pada pace tempo panjang.",
    shoes: [
      {
        name: "Megablast",
        brand: "ASICS",
        plateType: "Non-Plated · FF Turbo (Skor Lab 95)",
        stackHeight: "45.1 / 35.2 mm",
        dropMm: 9.9,
        weightG: 218,
        bestFor: "Tempo, Competition & Long Fast Runs",
        description:
          "Peraih skor performa tertinggi (95/100): energy return eksplosif, peredaman benturan superior, grip kelas satu, dan bobot sangat ringan 218 g.",
        imageUrl: "https://cdn.runrepeat.com/storage/gallery/product_primary/40948/asics-megablast-23653319-380.jpg",
        slug: "asics-megablast",
      },
      {
        name: "Superblast 3",
        brand: "ASICS",
        plateType: "Non-Plated · FF LEAP (Skor Lab 94)",
        stackHeight: "45.8 / 37.7 mm",
        dropMm: 8.1,
        weightG: 235,
        bestFor: "Daily Running, Threshold & Marathon Pace",
        description:
          "Super trainer berbusa FF LEAP setebal 45.8 mm dengan grip lebih menggigit dan bobot turun ke 235 g sambil tetap stabil.",
        imageUrl: "https://cdn.runrepeat.com/storage/gallery/product_primary/41200/asics-superblast-3-24636359-380.jpg",
        slug: "asics-superblast-3",
      },
      {
        name: "Endorphin Speed 5",
        brand: "Saucony",
        plateType: "Winged Nylon Plate · PWRRUN PB (Skor Lab 94)",
        stackHeight: "37.4 / 26.8 mm",
        dropMm: 10.6,
        weightG: 241,
        bestFor: "Tempo Runs, Threshold & Half Marathon",
        description:
          "Pelat nilon bersayap dan busa PWRRUN PB menghasilkan pantulan kelas dunia dengan sirkulasi udara upper yang sangat sejuk.",
        imageUrl: "https://cdn.runrepeat.com/storage/gallery/product_primary/40830/saucony-endorphin-speed-5-23261039-380.jpg",
        slug: "saucony-endorphin-speed-5",
      },
      {
        name: "Superblast 2",
        brand: "ASICS",
        plateType: "Non-Plated · FF Turbo+ / Blast+ (Skor Lab 91)",
        stackHeight: "42.8 / 34.6 mm",
        dropMm: 8.2,
        weightG: 252,
        bestFor: "Tempo, Marathon Pace & High Mileage",
        description:
          "Salah satu trainer paling serbaguna dengan midsole bertenaga dan outsole solid untuk menjaga konsistensi pace panjang.",
        imageUrl: "https://cdn.runrepeat.com/storage/gallery/product_primary/40450/asics-superblast-2-21749161-380.jpg",
        slug: "asics-superblast-2",
      },
      {
        name: "Zoom Fly 6",
        brand: "Nike",
        plateType: "Carbon Plate · ZoomX (Skor Lab 88)",
        stackHeight: "39.7 / 30.1 mm",
        dropMm: 9.6,
        weightG: 248,
        bestFor: "Tempo, Threshold Intervals & Long Fast",
        description:
          "Memadukan busa ZoomX responsif, pelat karbon, serta durabilitas outsole tinggi sebagai pasangan latihan super-shoe harian.",
        imageUrl: "https://cdn.runrepeat.com/storage/gallery/product_primary/40613/nike-zoom-fly-6-22464236-380.jpg",
        slug: "nike-zoom-fly-6",
      },
      {
        name: "FuelCell SuperComp Trainer v3",
        brand: "New Balance",
        plateType: "Carbon Plate · FuelCell (Skor Lab 89)",
        stackHeight: "36.8 / 29.5 mm",
        dropMm: 7.3,
        weightG: 278,
        bestFor: "Tempo Workouts & High-Stability Long Runs",
        description:
          "Versi paling stabil dan bertenaga di lini SC Trainer dengan grip sangat baik untuk volume latihan kecepatan mingguan.",
        imageUrl: "https://cdn.runrepeat.com/storage/gallery/product_primary/40405/new-balance-fuel-cell-super-comp-trainer-v-3-21926162-380.jpg",
        slug: "new-balance-fuelcell-supercomp-trainer-v3",
      },
    ],
  },
  {
    id: 4,
    title: "Carbon Plated Competition Racers",
    subtitle: "Race Day 5K, 10K, HM & Marathon PRs",
    unlockPaceSecondsPerKm: 300,
    unlockPaceDisplay: "Pace < 5:00/km",
    badgeLabel: "Tier 4 • Carbon Racer",
    description:
      "Sepatu balap Competition / Race teruji terbaik dengan pelat serat karbon penuh berbobot sub-215 gram untuk memecahkan rekor personal lomba.",
    biomechanicalRationale:
      "Pelat serat karbon kaku mengurangi tekukan berlebih pada sendi MTP telapak depan dan melontarkan kembali energi kompresi busa super di tiap langkah.",
    shoes: [
      {
        name: "Vaporfly 4",
        brand: "Nike",
        plateType: "Carbon Plate · ZoomX (Skor Lab 93)",
        stackHeight: "34.1 / 25.5 mm",
        dropMm: 8.6,
        weightG: 166,
        bestFor: "Competition 5K, 10K & Half Marathon PR",
        description:
          "Super-shoe super ringan 166 g dengan penyempurnaan lidah/fit upper dan transisi sangat natural untuk memecahkan PR jarak pendek-menengah.",
        imageUrl: "https://cdn.runrepeat.com/storage/gallery/product_primary/40754/nike-vaporfly-4-22860848-380.jpg",
        slug: "nike-vaporfly-4",
      },
      {
        name: "Adizero Adios Pro 4",
        brand: "adidas",
        plateType: "Carbon EnergyRods · Lightstrike Pro (Skor Lab 93)",
        stackHeight: "36.6 / 28.5 mm",
        dropMm: 8.1,
        weightG: 200,
        bestFor: "Competition Half & Full Marathon Race",
        description:
          "Menghadirkan batang karbon EnergyRods dengan busa Lightstrike Pro lebih empuk dan ledakan dorongan ke depan di bobot tepat 200 g.",
        imageUrl: "https://cdn.runrepeat.com/storage/gallery/product_primary/40584/adidas-adizero-adios-pro-4-22483965-380.jpg",
        slug: "adidas-adizero-adios-pro-4",
      },
      {
        name: "Endorphin Pro 5",
        brand: "Saucony",
        plateType: "Carbon Plate · PWRRUN PB (Skor Lab 91)",
        stackHeight: "36.5 / 27.8 mm",
        dropMm: 8.7,
        weightG: 215,
        bestFor: "Competition Marathon & Heel-Strike Stability",
        description:
          "Super shoe lomba berpelat karbon paling ramah kestabilan, nyaman untuk heel striker dan percaya diri pada tikungan maupun jalan basah.",
        imageUrl: "https://cdn.runrepeat.com/storage/gallery/product_primary/41067/saucony-endorphin-pro-5-24665925-380.jpg",
        slug: "saucony-endorphin-pro-5",
      },
    ],
  },
  {
    id: 5,
    title: "Elite Pinnacle Competition Super-Shoes",
    subtitle: "Sub-4:00 Pace, Podium & Record Tech",
    unlockPaceSecondsPerKm: 240,
    unlockPaceDisplay: "Pace < 4:00/km",
    badgeLabel: "Tier 5 • Alpha Weapon",
    description:
      "Kasta kompetisi tertinggi terkurasi: super-shoe ultra-ekstrem sub-190 g hingga sub-100 g untuk efisiensi biomekanika maksimal pada pace elit.",
    biomechanicalRationale:
      "Pada kecepatan >15 km/jam, gaya kontak kaki cukup besar untuk memeras kompresi penuh superfoam generasi baru sambil menjaga bobot serendah mungkin.",
    shoes: [
      {
        name: "Alphafly 4",
        brand: "Nike",
        plateType: "Carbon Plate · ZoomX (Skor Lab 94)",
        stackHeight: "39.0 / 32.8 mm",
        dropMm: 6.2,
        weightG: 190,
        bestFor: "Competition Marathon Record & Podium",
        description:
          "Alphafly terbaik di kelasnya: lebih ringan (190 g), pantulan lebih bertenaga, traksi membaik, dan lock-down Atomknit paling aman.",
        imageUrl: "https://cdn.runrepeat.com/storage/gallery/product_primary/41531/nike-alphafly-4-25929108-380.jpg",
        slug: "nike-alphafly-4",
      },
      {
        name: "Metaspeed Sky Tokyo",
        brand: "ASICS",
        plateType: "Carbon Plate · FF LEAP (Skor Lab 92)",
        stackHeight: "38.7 / 32.7 mm",
        dropMm: 6.0,
        weightG: 163,
        bestFor: "Competition Elite Road & Full Marathon",
        description:
          "Konstruksi super ringan 163 g dengan ventilasi maksimal, busa FF LEAP empuk, dan pelat karbon agresif untuk mempertahankan kecepatan puncak.",
        imageUrl: "https://cdn.runrepeat.com/storage/gallery/product_primary/40812/asics-metaspeed-sky-tokyo-23429911-380.jpg",
        slug: "asics-metaspeed-sky-tokyo",
      },
      {
        name: "Adizero Adios Pro Evo 3",
        brand: "adidas",
        plateType: "Carbon Plate · Lightstrike Pro Evo (Skor Lab 91)",
        stackHeight: "38.1 / 32.2 mm",
        dropMm: 5.9,
        weightG: 99,
        bestFor: "Elite Championship & Sub-2h Architecture",
        description:
          "Inovasi ultra-ekstrem berbobot 99 g di timbangan lab dengan stack 38.1 mm, menghadirkan energy return dan shock absorption level puncak dunia.",
        imageUrl: "https://cdn.runrepeat.com/storage/gallery/product_primary/41285/adidas-adizero-adios-pro-evo-3-24813691-380.jpg",
        slug: "adidas-adizero-adios-pro-evo-3",
      },
    ],
  },
];

export const BIOMECHANICS_MODULES: BiomechanicsModule[] = [
  {
    id: "midfoot_strike",
    title: "Midfoot Strike & Tibial Alignment",
    subtitle: "Eliminate heel-striking braking forces by landing directly beneath your center of mass.",
    focusMetricLabel: "Tibial Inclination at Contact",
    optimalTargetRange: "0° to +4° (Vertical Shin)",
    commonFault: "Overstriding with extended knee (+14° tibial angle), creating high eccentric braking impulse.",
    biomechanicalCue:
      "Imagine pedaling a bicycle backward: paw the track down and back so your foot touches down directly under your hips.",
    drills: [
      "2 x 30m Barefoot Grass Cadence Paws",
      "3 x 40m A-Skip to Running Transition",
      "Metronome 182 SPM Short-Lever Strides",
    ],
    targetKneeFlexionDeg: 162,
    targetTibialAngleDeg: 2,
    targetTrunkLeanDeg: 8,
    targetCadenceSpm: 182,
  },
  {
    id: "cadence_recoil",
    title: "180+ SPM Cadence & Achilles Recoil",
    subtitle: "Shorten ground contact time (<210ms) to harness free elastic energy from the Achilles tendon.",
    focusMetricLabel: "Ground Contact Time (GCT)",
    optimalTargetRange: "175ms - 205ms @ 180-188 SPM",
    commonFault: "Low cadence (<166 SPM) with bounding vertical oscillation (>10.5 cm) and energy dissipation.",
    biomechanicalCue:
      "Run on hot coals: focus on rapid, light ankle stiffness and instant heel lift right after mid-stance.",
    drills: [
      "3 x 60s In-Place Ankling Hops (Stiff Ankle)",
      "4 x 100m +5% Cadence Metronome Pickups",
      "2 x 20 Single-Leg Pogo Jumps",
    ],
    targetKneeFlexionDeg: 158,
    targetTibialAngleDeg: 3,
    targetTrunkLeanDeg: 7,
    targetCadenceSpm: 186,
  },
  {
    id: "forward_lean",
    title: "Ankle-Driven Forward Lean",
    subtitle: "Use gravity for horizontal propulsion by leaning from the ankles rather than collapsing at the waist.",
    focusMetricLabel: "Whole-Body Forward Tilt",
    optimalTargetRange: "6° to 9° from Ankle Axis",
    commonFault: "Hinging at the lumbar spine ('sitting in the bucket'), restricting diaphragm expansion and hip extension.",
    biomechanicalCue:
      "Stand tall with a string pulling the crown of your head skyward, then press your hips slightly forward until gravity initiates your stride.",
    drills: [
      "4 x Falling Starts from Standing Tall",
      "3 x 50m Wall-Lean March & Acceleration",
      "Thoracic Tall-Posture Float Strides",
    ],
    targetKneeFlexionDeg: 160,
    targetTibialAngleDeg: 1,
    targetTrunkLeanDeg: 9,
    targetCadenceSpm: 180,
  },
  {
    id: "pelvic_neutrality",
    title: "Pelvic Neutrality & Triple Extension",
    subtitle: "Lock in core stability to transfer 100% of glute and hamstring torque into forward velocity.",
    focusMetricLabel: "Peak Hip Extension Angle",
    optimalTargetRange: "18° to 24° Behind Vertical",
    commonFault:
      "Anterior pelvic tilt (arched lower back), cutting off trailing-leg hip extension and overloading hip flexors.",
    biomechanicalCue:
      "Level your belt buckle like a bowl of water, squeeze your glute at toe-off to drive the ground behind you.",
    drills: [
      "2 x 15 Glute Bridge Marching Holds",
      "3 x 40m High-Knee B-Skips with Hip Lock",
      "4 x 60m Hill Bounds for Triple Extension",
    ],
    targetKneeFlexionDeg: 165,
    targetTibialAngleDeg: 2,
    targetTrunkLeanDeg: 8,
    targetCadenceSpm: 182,
  },
  {
    id: "compact_arm_swing",
    title: "Compact Arm Swing & Counter-Rotation",
    subtitle: "Balance lower-body angular momentum with compact 85° elbow drive back toward the hip pocket.",
    focusMetricLabel: "Elbow Flexion & Drive Arc",
    optimalTargetRange: "80°-90° Flexion (Zero Midline Cross)",
    commonFault: "Arms crossing the sternum midline, inducing torso torque and lateral side-to-side energy waste.",
    biomechanicalCue:
      "Brush your thumbs lightly past your hip pockets on the backswing, hands relaxed as if holding a potato chip without crushing it.",
    drills: [
      "3 x 45s Seated Rapid Arm-Drive Drill",
      "4 x 80m Relaxed-Jaw & Dropped-Shoulder Strides",
      "2 x 30m Band-Resisted Elbow Back-Drive",
    ],
    targetKneeFlexionDeg: 161,
    targetTibialAngleDeg: 2,
    targetTrunkLeanDeg: 8,
    targetCadenceSpm: 184,
  },
];

export function calculateVdot(distanceMeters: number, durationSeconds: number): number {
  if (distanceMeters <= 0 || durationSeconds <= 0) return 48.0;
  const timeMin = durationSeconds / 60;
  const v = distanceMeters / timeMin;
  const vo2 = -4.6 + 0.182258 * v + 0.000104 * v * v;
  const pct = 0.8 + 0.1894393 * Math.exp(-0.012778 * timeMin) + 0.2989558 * Math.exp(-0.1932605 * timeMin);
  if (pct <= 0.05) return 48.0;
  return Math.min(85, Math.max(28, vo2 / pct));
}

function dateKeyFromMs(ms: number): string {
  const d = new Date(ms > 0 ? ms : Date.now());
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
}

export function computeLocalAnalyticsFallback(
  workouts: WorkoutLog[],
  prs: PersonalRecord[],
  goals: TrainingGoal[],
  fatigueExponent = 1.06
): ComprehensiveAnalyticsResponse {
  const now = Date.now();
  const sorted = [...workouts].sort((a, b) => b.createdAtEpochMs - a.createdAtEpochMs);

  const bestPr = [...prs].sort(
    (a, b) => calculateVdot(b.distanceMeters, b.bestTimeSeconds) - calculateVdot(a.distanceMeters, a.bestTimeSeconds)
  )[0];
  const bestWk = sorted
    .filter((w) => w.distanceKm >= 3 && w.durationSeconds > 0)
    .sort((a, b) => calculateVdot(b.distanceKm * 1000, b.durationSeconds) - calculateVdot(a.distanceKm * 1000, a.durationSeconds))[0];
  const fallbackWk =
    !bestPr && !bestWk
      ? sorted
          .filter((w) => w.distanceKm >= 0.5 && w.durationSeconds >= 60)
          .sort((a, b) => calculateVdot(b.distanceKm * 1000, b.durationSeconds) - calculateVdot(a.distanceKm * 1000, a.durationSeconds))[0]
      : undefined;

  const hasBaseline = sorted.length > 0 && Boolean(bestPr || bestWk || fallbackWk);
  let baseDistM = 0;
  let baseDurSec = 0;
  if (bestPr && bestWk) {
    const pV = calculateVdot(bestPr.distanceMeters, bestPr.bestTimeSeconds);
    const wV = calculateVdot(bestWk.distanceKm * 1000, bestWk.durationSeconds);
    if (pV >= wV) {
      baseDistM = bestPr.distanceMeters;
      baseDurSec = bestPr.bestTimeSeconds;
    } else {
      baseDistM = bestWk.distanceKm * 1000;
      baseDurSec = bestWk.durationSeconds;
    }
  } else if (bestPr) {
    baseDistM = bestPr.distanceMeters;
    baseDurSec = bestPr.bestTimeSeconds;
  } else if (bestWk) {
    baseDistM = bestWk.distanceKm * 1000;
    baseDurSec = bestWk.durationSeconds;
  } else if (fallbackWk) {
    baseDistM = fallbackWk.distanceKm * 1000;
    baseDurSec = fallbackWk.durationSeconds;
  }

  const currentVdot = hasBaseline ? Number(calculateVdot(baseDistM, baseDurSec).toFixed(1)) : 0;
  const totalDist = sorted.reduce((s, w) => s + w.distanceKm, 0);
  const totalDur = sorted.reduce((s, w) => s + w.durationSeconds, 0);
  const totalElev = sorted.reduce((s, w) => s + w.elevationGainM, 0);
  const avgPace = totalDist > 0 ? Math.round(totalDur / totalDist) : 0;

  const racePredictions: RacePrediction[] = hasBaseline
    ? DISTANCE_CATEGORIES.map((cat) => {
        const ratio = cat.meters / baseDistM;
        const penalty =
          cat.key === "MARATHON" && totalDist < 45 ? 1.022 : cat.key === "HALF_MARATHON" && totalDist < 30 ? 1.01 : 1.0;
        const predSec = Math.round(baseDurSec * Math.pow(ratio, fatigueExponent) * penalty);
        const paceSec = Math.round(predSec / cat.kilometers);
        return {
          categoryKey: cat.key,
          displayName: cat.displayName,
          shortLabel: cat.shortLabel,
          predictedTimeSeconds: predSec,
          predictedTimeFormatted: formatDuration(predSec),
          targetPaceSecondsPerKm: paceSec,
          targetPaceFormatted: formatPace(paceSec),
          vdotScore: currentVdot,
          confidenceNote:
            cat.key === "MARATHON"
              ? `Fatigue exponent ${fatigueExponent.toFixed(2)} + volume durability`
              : "Kalkulasi VDOT & daya tahan aerobik",
        };
      })
    : [];

  const tenKPace = racePredictions.find((p) => p.categoryKey === "10K")?.targetPaceSecondsPerKm ?? 0;
  const baseTenK = tenKPace > 0 ? Math.min(540, Math.max(150, tenKPace)) : 320;
  const tPace = baseTenK * 1.03;

  const paceZones: TrainingPaceZone[] = hasBaseline
    ? [
        {
          zoneName: "Zone 1-2: Easy / Aerobic Base",
          code: "E",
          paceRangePerKm: `${formatPaceShort(tPace * 1.14)} - ${formatPaceShort(tPace * 1.3)} /km`,
          physiologicalBenefit: "Mitochondrial density, capillary growth & active recovery",
          heartRatePctRange: "65% - 78% HRmax",
        },
        {
          zoneName: "Zone 3: Marathon / Aerobic Tempo",
          code: "M",
          paceRangePerKm: `${formatPaceShort(tPace * 1.04)} - ${formatPaceShort(tPace * 1.13)} /km`,
          physiologicalBenefit: "Lipid oxidation efficiency & race-pace biomechanical economy",
          heartRatePctRange: "79% - 84% HRmax",
        },
        {
          zoneName: "Zone 4: Lactate Threshold",
          code: "T",
          paceRangePerKm: `${formatPaceShort(tPace * 0.97)} - ${formatPaceShort(tPace * 1.03)} /km`,
          physiologicalBenefit: "Raises lactate clearance shuttle ceiling (60-min race effort)",
          heartRatePctRange: "85% - 90% HRmax",
        },
        {
          zoneName: "Zone 5A: VO2 Max Intervals",
          code: "I",
          paceRangePerKm: `${formatPaceShort(Math.max(135, tPace * 0.9))} - ${formatPaceShort(Math.max(145, tPace * 0.95))} /km`,
          physiologicalBenefit: "Maximizes cardiac stroke volume & central oxygen uptake",
          heartRatePctRange: "93% - 98% HRmax",
        },
        {
          zoneName: "Zone 5B: Neuromuscular Repetition",
          code: "R",
          paceRangePerKm: `${formatPaceShort(Math.max(120, tPace * 0.83))} - ${formatPaceShort(Math.max(130, tPace * 0.89))} /km`,
          physiologicalBenefit: "Fast-twitch fiber recruitment, tendon stiffness & running economy",
          heartRatePctRange: "98% - 100% Effort",
        },
      ]
    : [];

  // Top 3 fastest >= 3 km in last 30 days
  const validRuns = sorted.filter((w) => w.distanceKm >= 0.5 && w.durationSeconds >= 60);
  const cutoff30 = now - 30 * 86_400_000;
  const recent30 = validRuns.filter((w) => w.createdAtEpochMs >= cutoff30);
  const pool = recent30.length > 0 ? recent30 : validRuns;
  const qualifying = pool.filter((w) => w.distanceKm >= 3.0);
  const finalPool = qualifying.length > 0 ? qualifying : pool;
  const top3 = [...finalPool]
    .sort((a, b) => a.durationSeconds / a.distanceKm - b.durationSeconds / b.distanceKm)
    .slice(0, 3);
  const shoePace =
    top3.length > 0
      ? Math.round(top3.reduce((s, w) => s + w.durationSeconds / w.distanceKm, 0) / top3.length)
      : 0;

  const unlockedFlags = SHOE_TIERS_CATALOG.map((tier) => tier.id === 1 || (shoePace > 0 && shoePace <= tier.unlockPaceSecondsPerKm));
  const highestIdx = Math.max(0, unlockedFlags.lastIndexOf(true));
  const shoeUnlock: ShoeUnlockProgress = {
    currentAvgPaceSec: shoePace,
    totalWorkoutsEvaluated: top3.length,
    currentTier: SHOE_TIERS_CATALOG[highestIdx],
    nextTier: highestIdx < SHOE_TIERS_CATALOG.length - 1 ? SHOE_TIERS_CATALOG[highestIdx + 1] : null,
    progressToNextTier: highestIdx === SHOE_TIERS_CATALOG.length - 1 ? 1 : shoePace > 0 ? 0.55 : 0,
    secondsNeededToNextTier:
      highestIdx < SHOE_TIERS_CATALOG.length - 1
        ? Math.max(0, shoePace - SHOE_TIERS_CATALOG[highestIdx + 1].unlockPaceSecondsPerKm)
        : 0,
    tiers: SHOE_TIERS_CATALOG.map((tier, idx) => ({
      tier,
      isUnlocked: unlockedFlags[idx],
      isCurrentHighest: idx === highestIdx,
    })),
  };

  // Weekly progress
  const activeGoal = goals.find((g) => !g.completed) ?? goals[0];
  const targetKm = Math.max(10, activeGoal?.weeklyMileageTargetKm ?? 25);
  const today = new Date();
  const dow = today.getDay() === 0 ? 7 : today.getDay();
  const monday = new Date(today);
  monday.setDate(today.getDate() - (dow - 1));
  monday.setHours(0, 0, 0, 0);
  const dayNames = ["Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min"];
  const todayKey = dateKeyFromMs(now);

  const dailyStats = Array.from({ length: 7 }, (_, i) => {
    const d = new Date(monday);
    d.setDate(monday.getDate() + i);
    const key = dateKeyFromMs(d.getTime());
    const dayRuns = sorted.filter((w) => dateKeyFromMs(w.createdAtEpochMs) === key);
    const dist = dayRuns.reduce((s, w) => s + w.distanceKm, 0);
    const dur = dayRuns.reduce((s, w) => s + w.durationSeconds, 0);
    return {
      dayIndex: i,
      dayName: dayNames[i],
      dateKey: key,
      dateDisplay: `${d.getDate()}`,
      distanceKm: Number(dist.toFixed(2)),
      durationSec: dur,
      avgPaceSec: dist > 0 ? Math.round(dur / dist) : 0,
      hasRun: dayRuns.length > 0,
      isToday: key === todayKey,
    };
  });

  const wkDist = dailyStats.reduce((s, d) => s + d.distanceKm, 0);
  const wkDur = dailyStats.reduce((s, d) => s + d.durationSec, 0);
  const wkRuns = sorted.filter((w) => w.createdAtEpochMs >= monday.getTime()).length;
  const weeklyProgress: WeeklyAnalytics = {
    weekRangeDisplay: "Minggu Berjalan",
    totalDistanceKm: Number(wkDist.toFixed(2)),
    targetDistanceKm: targetKm,
    runsCount: wkRuns,
    totalDurationSec: wkDur,
    avgPaceSec: wkDist > 0 ? Math.round(wkDur / wkDist) : 0,
    prevWeekDistanceKm: 0,
    distanceDiffKm: Number(wkDist.toFixed(2)),
    prevWeekAvgPaceSec: 0,
    paceDiffSec: 0,
    dailyStats,
    coachingTip:
      wkDist >= targetKm
        ? `Target mingguan ${targetKm} km tercapai! Prioritaskan recovery.`
        : "Teruskan konsistensi latihan aerobik Anda minggu ini.",
  };

  const workoutDeepMetrics: Record<string, WorkoutDeepMetrics> = {};
  for (const w of sorted) {
    const splits = parseSplitsSummary(w.splitsSummary, w.avgPaceSecondsPerKm);
    const mean = splits.length > 0 ? splits.reduce((s, p) => s + p.paceSeconds, 0) / splits.length : 0;
    const variance =
      splits.length > 0 ? splits.reduce((s, p) => s + Math.pow(p.paceSeconds - mean, 2), 0) / splits.length : 0;
    const cv = mean > 0 ? Number(((Math.sqrt(variance) / mean) * 100).toFixed(2)) : 0;
    const half = Math.max(1, Math.floor(splits.length / 2));
    const h1 = splits.length >= 2 ? Math.round(splits.slice(0, half).reduce((s, p) => s + p.paceSeconds, 0) / half) : w.avgPaceSecondsPerKm;
    const h2 =
      splits.length >= 2
        ? Math.round(splits.slice(half).reduce((s, p) => s + p.paceSeconds, 0) / Math.max(1, splits.length - half))
        : w.avgPaceSecondsPerKm;
    const delta = h2 - h1;
    const zoneCode =
      w.avgPaceSecondsPerKm >= tPace * 1.135
        ? "E"
        : w.avgPaceSecondsPerKm >= tPace * 1.035
        ? "M"
        : w.avgPaceSecondsPerKm >= tPace * 0.96
        ? "T"
        : w.avgPaceSecondsPerKm >= tPace * 0.895
        ? "I"
        : "R";

    workoutDeepMetrics[w.id] = {
      workoutId: w.id,
      splits,
      splitCvPct: cv,
      firstHalfAvgPaceSec: h1,
      secondHalfAvgPaceSec: h2,
      splitDeltaSec: delta,
      splitStrategy: splits.length < 2 ? "Single Split" : delta <= -5 ? "Negative Split" : delta >= 10 ? "Positive Split" : "Even Split",
      fastestSplit: splits.length > 0 ? [...splits].sort((a, b) => a.paceSeconds - b.paceSeconds)[0] : null,
      slowestSplit: splits.length > 0 ? [...splits].sort((a, b) => b.paceSeconds - a.paceSeconds)[0] : null,
      estimatedRtss: Number((((w.durationSeconds / 3600) * Math.pow(Math.min(1.35, Math.max(0.55, tPace / Math.max(120, w.avgPaceSecondsPerKm))), 2) * 100) + w.elevationGainM * 0.04).toFixed(1)),
      aerobicEfficiency: w.avgHeartRate > 0 && w.durationSeconds > 0 ? Number((((w.distanceKm * 1000) / (w.durationSeconds / 60)) / w.avgHeartRate).toFixed(2)) : 0,
      strideLengthM: w.cadenceSpm > 0 && w.durationSeconds > 0 ? Number((((w.distanceKm * 1000) / (w.durationSeconds / 60)) / w.cadenceSpm).toFixed(2)) : 0,
      vdot: w.distanceKm >= 0.8 ? Number(calculateVdot(w.distanceKm * 1000, w.durationSeconds).toFixed(1)) : 0,
      zoneCode,
      zoneName: zoneCode,
      paceSpreadSec: splits.length > 0 ? Math.max(...splits.map((s) => s.paceSeconds)) - Math.min(...splits.map((s) => s.paceSeconds)) : 0,
    };
  }

  // Simple 90d load series + 112d heatmap
  const fitnessLoadSeries: DailyFitnessPoint[] = [];
  let ctl = 0;
  let atl = 0;
  for (let i = 89; i >= 0; i--) {
    const key = dateKeyFromMs(now - i * 86_400_000);
    const dayWks = sorted.filter((w) => dateKeyFromMs(w.createdAtEpochMs) === key);
    const load = dayWks.reduce((s, w) => s + (workoutDeepMetrics[w.id]?.estimatedRtss ?? 0), 0);
    const dist = dayWks.reduce((s, w) => s + w.distanceKm, 0);
    const tsb = ctl - atl;
    ctl += (load - ctl) * (2 / 43);
    atl += (load - atl) * (2 / 8);
    fitnessLoadSeries.push({
      dateKey: key,
      dailyLoad: Number(load.toFixed(1)),
      ctlFitness: Number(ctl.toFixed(1)),
      atlFatigue: Number(atl.toFixed(1)),
      tsbForm: Number(tsb.toFixed(1)),
      distanceKm: Number(dist.toFixed(2)),
    });
  }

  const activityHeatmap: HeatmapDay[] = Array.from({ length: 112 }, (_, idx) => {
    const i = 111 - idx;
    const key = dateKeyFromMs(now - i * 86_400_000);
    const dayWks = sorted.filter((w) => dateKeyFromMs(w.createdAtEpochMs) === key);
    const dist = dayWks.reduce((s, w) => s + w.distanceKm, 0);
    return {
      dateKey: key,
      distanceKm: Number(dist.toFixed(2)),
      runsCount: dayWks.length,
      intensityLevel: dist <= 0 ? 0 : dist < 5 ? 1 : dist < 10 ? 2 : dist < 16 ? 3 : 4,
    };
  });

  const goalSchedules: GoalScheduleWithSummary[] = goals.map((goal) => {
    const clean = stripScheduleSuffix(goal.title) || "Target Lari";
    const match = goal.title.match(/\[(?:(\d{4}-\d{2}-\d{2})~)?(\d{4}-\d{2}-\d{2})\|([0-9,]+)]$/);
    const targetKey = match?.[2] ?? dateKeyFromMs(now + 21 * 86_400_000);
    const startKey = match?.[1] ?? dateKeyFromMs(goal.createdAtEpochMs || now);
    const daysIso = match?.[3] ? match[3].split(",").map(Number).filter((d) => d >= 1 && d <= 7) : [2, 4, 7];
    const sessions: ScheduledSession[] = [];
    const cat = getDistanceCategoryInfo(goal.targetDistanceCategory);
    const goalPace = Math.round(goal.targetTimeSeconds / cat.kilometers);
    const curDate = new Date(`${startKey}T00:00:00`);
    const endDate = new Date(`${targetKey}T00:00:00`);
    let count = 0;
    while (curDate <= endDate && count < 60) {
      const k = dateKeyFromMs(curDate.getTime());
      const iso = curDate.getDay() === 0 ? 7 : curDate.getDay();
      if (k === targetKey || daysIso.includes(iso)) {
        const matched = sorted.find((w) => dateKeyFromMs(w.createdAtEpochMs) === k);
        sessions.push({
          dateKey: k,
          displayDate: `${dayNameId(iso)}, ${k}`,
          dayOfWeekIso: iso,
          dayName: dayNameId(iso),
          workoutTypeCode: k === targetKey ? "RACE_DAY" : iso >= 6 ? "EASY_LONG" : "TEMPO",
          workoutTypeDisplay: k === targetKey ? "Race Day Target!" : iso >= 6 ? "Long Run" : "Tempo Run",
          shortBadge: k === targetKey ? "RACE DAY" : iso >= 6 ? "LONG RUN" : "TEMPO",
          zoneLabel: "Zona Aerobik",
          sessionTitle: k === targetKey ? `RACE DAY: ${clean}` : iso >= 6 ? "Long Run" : "Tempo / Easy",
          sessionDescription: `Target pace ${formatPace(goalPace)}`,
          recommendedDistanceKm: Math.round(goal.weeklyMileageTargetKm / Math.max(1, daysIso.length)),
          targetPaceSecondsPerKm: goalPace,
          status: matched ? "COMPLETED" : k === todayKey ? "TODAY_PENDING" : k < todayKey ? "SKIPPED" : "UPCOMING",
          matchedWorkoutId: matched?.id ?? null,
          matchedWorkoutTitle: matched?.title ?? null,
          aiReason: "Sesi disesuaikan secara otomatis dari riwayat lari terbaru.",
          decisionSource: "LOCAL_FALLBACK",
        });
      }
      curDate.setDate(curDate.getDate() + 1);
      count++;
    }
    const completedSessions = sessions.filter((s) => s.status === "COMPLETED").length;
    return {
      goalId: goal.id,
      goal,
      metadata: {
        cleanTitle: clean,
        startDateKey: startKey,
        startDateDisplay: startKey,
        targetDateKey: targetKey,
        targetDateDisplay: targetKey,
        daysRemaining: Math.round((endDate.getTime() - now) / 86_400_000),
        trainingDaysIso: daysIso,
      },
      completedSessions,
      todayPendingSessions: sessions.filter((s) => s.status === "TODAY_PENDING").length,
      skippedSessions: sessions.filter((s) => s.status === "SKIPPED").length,
      upcomingSessions: sessions.filter((s) => s.status === "UPCOMING").length,
      compliancePct: sessions.length > 0 ? Math.round((completedSessions / sessions.length) * 100) : 100,
      sessions,
    };
  });

  const hrRuns = sorted.filter((w) => w.avgHeartRate > 0);
  const cadRuns = sorted.filter((w) => w.cadenceSpm > 0);
  const monthlyTrends: MonthlyTrend[] = [];
  const baseEasyKm = sorted.filter((w) => workoutDeepMetrics[w.id]?.zoneCode === "E").reduce((s, w) => s + w.distanceKm, 0);
  const baseModKm = sorted.filter((w) => workoutDeepMetrics[w.id]?.zoneCode === "M").reduce((s, w) => s + w.distanceKm, 0);
  const baseHardKm = Math.max(0, totalDist - baseEasyKm - baseModKm);

  return {
    overview: {
      totalWorkouts: sorted.length,
      totalDistanceKm: Number(totalDist.toFixed(2)),
      totalDurationSeconds: totalDur,
      totalElevationM: totalElev,
      avgPaceSec: avgPace,
      avgHeartRate: hrRuns.length > 0 ? Math.round(hrRuns.reduce((s, w) => s + w.avgHeartRate, 0) / hrRuns.length) : 0,
      avgCadenceSpm: cadRuns.length > 0 ? Math.round(cadRuns.reduce((s, w) => s + w.cadenceSpm, 0) / cadRuns.length) : 0,
      currentVdot,
      vdotDelta30d: 0,
      currentStreakDays: wkRuns > 0 ? 1 : 0,
      longestStreakDays: Math.min(sorted.length, 3),
      estimatedCaloriesKcal: Math.round(totalDist * 65),
      longestRunKm: sorted.length > 0 ? Math.max(...sorted.map((w) => w.distanceKm)) : 0,
      fastestPaceSec: validRuns.length > 0 ? Math.min(...validRuns.map((w) => w.avgPaceSecondsPerKm)) : 0,
    },
    racePredictions,
    paceZones,
    shoeUnlock,
    weeklyProgress,
    goalSchedules,
    fitnessLoadSeries,
    workloadStatus: {
      acwr: 1.05,
      acute7dLoad: fitnessLoadSeries.slice(-7).reduce((s, p) => s + p.dailyLoad, 0),
      chronic28dWeeklyAvgLoad: Number((fitnessLoadSeries.slice(-28).reduce((s, p) => s + p.dailyLoad, 0) / 4).toFixed(1)),
      zone: "SWEET_SPOT",
      label: "Zona Optimal (Fallback)",
      recommendation: "Jalankan StrideApex.Api (.NET 8) di localhost:5180 untuk metrik ACWR & fisiologi presisi penuh.",
    },
    polarizedDistribution: {
      easyPct: totalDist > 0 ? Number(((baseEasyKm * 100) / totalDist).toFixed(1)) : 0,
      moderatePct: totalDist > 0 ? Number(((baseModKm * 100) / totalDist).toFixed(1)) : 0,
      hardPct: totalDist > 0 ? Number(((baseHardKm * 100) / totalDist).toFixed(1)) : 0,
      easyKm: Number(baseEasyKm.toFixed(1)),
      moderateKm: Number(baseModKm.toFixed(1)),
      hardKm: Number(baseHardKm.toFixed(1)),
      alignmentScore: 80,
      coachingNote: "Distribusi intensitas dihitung dari profil pace latihan Anda.",
    },
    efficiencyTrend: [...sorted]
      .reverse()
      .map((w) => ({
        workoutId: w.id,
        dateKey: dateKeyFromMs(w.createdAtEpochMs),
        title: w.title,
        distanceKm: w.distanceKm,
        avgPaceSec: w.avgPaceSecondsPerKm,
        avgHeartRate: w.avgHeartRate,
        efficiencyFactor: workoutDeepMetrics[w.id]?.aerobicEfficiency ?? 0,
        vdot: workoutDeepMetrics[w.id]?.vdot ?? 0,
        cadenceSpm: w.cadenceSpm,
        strideLengthM: workoutDeepMetrics[w.id]?.strideLengthM ?? 0,
      })),
    pacingConsistency: {
      avgSplitCvPct: 2.8,
      negativeSplitCount: Object.values(workoutDeepMetrics).filter((m) => m.splitStrategy === "Negative Split").length,
      evenSplitCount: Object.values(workoutDeepMetrics).filter((m) => m.splitStrategy === "Even Split").length,
      positiveSplitCount: Object.values(workoutDeepMetrics).filter((m) => m.splitStrategy === "Positive Split").length,
      grade: sorted.length > 0 ? "A" : "N/A",
      bestPacedWorkoutTitle: sorted[0]?.title ?? "-",
      summary: "Ringkasan konsistensi split antar kilometer.",
    },
    raceReadiness: {
      score: sorted.length > 0 ? 78 : 0,
      grade: sorted.length > 0 ? "Siap Kompetitif" : "-",
      goalTitle: activeGoal ? stripScheduleSuffix(activeGoal.title) : "Belum Ada Target",
      targetDistanceLabel: activeGoal?.targetDistanceCategory ?? "-",
      volumeReadinessPct: Math.min(100, Math.round((wkDist / targetKm) * 100)),
      vdotGapSecPerKm: 0,
      acwrSafe: true,
      longRunCoveragePct: 85,
      bottleneck: "Aktifkan backend .NET untuk evaluasi readiness lengkap.",
      actionItems: ["Jaga konsistensi latihan sesuai jadwal."],
    },
    monthlyTrends,
    activityHeatmap,
    cadenceDiagnostics: {
      avgCadenceSpm: cadRuns.length > 0 ? Math.round(cadRuns.reduce((s, w) => s + w.cadenceSpm, 0) / cadRuns.length) : 0,
      estimatedAvgStrideLengthM: 1.05,
      optimalRange: "176 - 186 spm",
      status: "OPTIMAL",
      recommendedModuleId: "cadence_recoil",
      coachingNote: "Pertahankan cadence cepat untuk meredam gaya pengereman sendi lutut.",
    },
    workoutDeepMetrics,
  };
}

export function extractLocalFallbackOcr(rawText: string, presetIndex = 0): ExtractedStravaMetrics {
  if (rawText.trim()) {
    const durMatch = rawText.match(/(?:Moving Time|Time|Waktu|Durasi)\s*[:=]?\s*(?:(\d{1,2}):)?(\d{1,2}):(\d{2})/i);
    const paceMatch = rawText.match(/(?:Avg Pace|Average Pace|Pace)\s*[:=]?\s*(\d{1,2}):([0-5]\d)/i);
    const distMatch = rawText.match(/(?:Distance|Jarak)\s*[:=]?\s*([0-9]+(?:[.,][0-9]+)?)\s*km/i);
    const dur = durMatch
      ? (durMatch[1] ? Number(durMatch[1]) * 3600 : 0) + Number(durMatch[2]) * 60 + Number(durMatch[3])
      : 1800;
    const splits = parseSplitsSummary(rawText);
    const pace = paceMatch
      ? Number(paceMatch[1]) * 60 + Number(paceMatch[2])
      : splits.length > 0
      ? Math.round(splits.reduce((s, i) => s + i.paceSeconds, 0) / splits.length)
      : 300;
    const rawDist = distMatch ? Number(distMatch[1].replace(",", ".")) : 0;
    const calcDist = dur > 0 && pace > 0 ? Number((dur / pace).toFixed(2)) : 5.0;
    const dist = rawDist > 1.05 ? rawDist : calcDist;
    return {
      title: "Strava Run Import",
      distanceKm: dist,
      durationSeconds: dur,
      avgPaceSecondsPerKm: pace,
      avgHeartRate: 0,
      cadenceSpm: 0,
      elevationGainM: 0,
      splitsSummary: splits.map((s) => `KM ${s.km}: ${s.paceStr}/km`).join(" | "),
      aiCoachingInsight: `Sesi ${dist.toFixed(2)} km selesai dalam ${formatDuration(dur)} (pace ${formatPace(pace)}).`,
      detectedDateStr: "",
      sourceEngine: "CLIENT_FALLBACK",
    };
  }
  const idx = Math.abs(presetIndex) % 3;
  if (idx === 0) {
    return {
      title: "Morning Carbon Tempo 10K",
      distanceKm: 10.0,
      durationSeconds: 2360,
      avgPaceSecondsPerKm: 236,
      avgHeartRate: 172,
      cadenceSpm: 184,
      elevationGainM: 64,
      splitsSummary: "KM 1: 4:02/km | KM 2: 3:59/km | KM 4: 3:56/km | KM 6: 3:55/km | KM 8: 3:53/km | KM 10: 3:50/km",
      aiCoachingInsight: "Pacing negative split sangat matang (+12 detik lebih cepat di KM 10 vs KM 1).",
      detectedDateStr: "",
      sourceEngine: "SAMPLE_PRESET",
    };
  }
  if (idx === 1) {
    return {
      title: "Half Marathon Progressive Long Run",
      distanceKm: 21.1,
      durationSeconds: 5380,
      avgPaceSecondsPerKm: 255,
      avgHeartRate: 161,
      cadenceSpm: 180,
      elevationGainM: 142,
      splitsSummary: "KM 1: 4:22/km | KM 5: 4:20/km | KM 10: 4:18/km | KM 15: 4:14/km | KM 21: 4:03/km",
      aiCoachingInsight: "Daya tahan aerobik solid pada 21.1 km dengan HR rata-rata 161 bpm.",
      detectedDateStr: "",
      sourceEngine: "SAMPLE_PRESET",
    };
  }
  return {
    title: "Evening Track 5K Speed Trial",
    distanceKm: 5.0,
    durationSeconds: 1105,
    avgPaceSecondsPerKm: 221,
    avgHeartRate: 181,
    cadenceSpm: 190,
    elevationGainM: 12,
    splitsSummary: "KM 1: 3:43/km | KM 2: 3:42/km | KM 3: 3:42/km | KM 4: 3:40/km | KM 5: 3:38/km",
    aiCoachingInsight: "Effort VO2Max puncak pada cadence 190 spm.",
    detectedDateStr: "",
    sourceEngine: "SAMPLE_PRESET",
  };
}
