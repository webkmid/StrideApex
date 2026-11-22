export type DistanceCategoryKey = "5K" | "10K" | "HALF_MARATHON" | "MARATHON";

export interface DistanceCategoryInfo {
  key: DistanceCategoryKey;
  displayName: string;
  shortLabel: string;
  meters: number;
  kilometers: number;
}

export interface WorkoutLog {
  id: string;
  userId: string;
  title: string;
  distanceKm: number;
  durationSeconds: number;
  avgPaceSecondsPerKm: number;
  avgHeartRate: number;
  cadenceSpm: number;
  elevationGainM: number;
  splitsSummary: string;
  aiCoachingInsight: string;
  sourceType: "STRAVA_OCR" | "MANUAL";
  createdAtEpochMs: number;
  updatedAtEpochMs: number;
}

export interface PersonalRecord {
  id: string;
  userId: string;
  distanceCategory: DistanceCategoryKey;
  distanceMeters: number;
  bestTimeSeconds: number;
  previousTimeSeconds: number;
  paceSecondsPerKm: number;
  workoutTitle: string;
  createdAtEpochMs: number;
}

export interface TrainingGoal {
  id: string;
  userId: string;
  title: string;
  targetDistanceCategory: DistanceCategoryKey;
  targetTimeSeconds: number;
  weeklyMileageTargetKm: number;
  reminderEnabled: boolean;
  reminderHour: number;
  reminderMinute: number;
  completed: boolean;
  createdAtEpochMs: number;
}

export interface CloudBackupSnapshot {
  id: string;
  userId: string;
  backupLabel: string;
  workoutCount: number;
  prCount: number;
  goalCount: number;
  summaryNotes: string;
  createdAtEpochMs: number;
}

export interface SplitItem {
  km: number;
  paceStr: string;
  paceSeconds: number;
  deltaFromAvgSeconds: number;
}

export interface RacePrediction {
  categoryKey: DistanceCategoryKey;
  displayName: string;
  shortLabel: string;
  predictedTimeSeconds: number;
  predictedTimeFormatted: string;
  targetPaceSecondsPerKm: number;
  targetPaceFormatted: string;
  vdotScore: number;
  confidenceNote: string;
}

export interface TrainingPaceZone {
  zoneName: string;
  code: string;
  paceRangePerKm: string;
  physiologicalBenefit: string;
  heartRatePctRange: string;
}

export interface ShoeModel {
  name: string;
  brand: string;
  plateType: string;
  stackHeight: string;
  dropMm: number;
  weightG: number | null;
  bestFor: string;
  description: string;
  imageUrl: string | null;
  slug?: string;
  shopeeAffiliateUrl?: string | null;
}

export interface ShoeTier {
  id: number;
  title: string;
  subtitle: string;
  unlockPaceSecondsPerKm: number;
  unlockPaceDisplay: string;
  badgeLabel: string;
  description: string;
  biomechanicalRationale: string;
  shoes: ShoeModel[];
}

export interface ShoeTierState {
  tier: ShoeTier;
  isUnlocked: boolean;
  isCurrentHighest: boolean;
}

export interface ShoeUnlockProgress {
  currentAvgPaceSec: number;
  totalWorkoutsEvaluated: number;
  currentTier: ShoeTier;
  nextTier: ShoeTier | null;
  progressToNextTier: number;
  secondsNeededToNextTier: number;
  tiers: ShoeTierState[];
}

export interface DayStat {
  dayIndex: number;
  dayName: string;
  dateKey: string;
  dateDisplay: string;
  distanceKm: number;
  durationSec: number;
  avgPaceSec: number;
  hasRun: boolean;
  isToday: boolean;
}

export interface WeeklyAnalytics {
  weekRangeDisplay: string;
  totalDistanceKm: number;
  targetDistanceKm: number;
  runsCount: number;
  totalDurationSec: number;
  avgPaceSec: number;
  prevWeekDistanceKm: number;
  distanceDiffKm: number;
  prevWeekAvgPaceSec: number;
  paceDiffSec: number;
  dailyStats: DayStat[];
  coachingTip: string;
}

export interface GoalScheduleMetadata {
  cleanTitle: string;
  startDateKey: string;
  startDateDisplay: string;
  targetDateKey: string;
  targetDateDisplay: string;
  daysRemaining: number;
  trainingDaysIso: number[];
}

export interface ScheduledSession {
  dateKey: string;
  displayDate: string;
  dayOfWeekIso: number;
  dayName: string;
  workoutTypeCode: "INTERVAL" | "TEMPO" | "RECOVERY" | "EASY_LONG" | "RACE_DAY";
  workoutTypeDisplay: string;
  shortBadge: string;
  zoneLabel: string;
  sessionTitle: string;
  sessionDescription: string;
  recommendedDistanceKm: number;
  targetPaceSecondsPerKm: number;
  status: "COMPLETED" | "TODAY_PENDING" | "SKIPPED" | "UPCOMING";
  matchedWorkoutId?: string | null;
  matchedWorkoutTitle?: string | null;
  aiReason: string;
  decisionSource: string;
}

export interface GoalScheduleWithSummary {
  goalId: string;
  goal: TrainingGoal;
  metadata: GoalScheduleMetadata;
  completedSessions: number;
  todayPendingSessions: number;
  skippedSessions: number;
  upcomingSessions: number;
  compliancePct: number;
  sessions: ScheduledSession[];
}

export interface OverviewSummary {
  totalWorkouts: number;
  totalDistanceKm: number;
  totalDurationSeconds: number;
  totalElevationM: number;
  avgPaceSec: number;
  avgHeartRate: number;
  avgCadenceSpm: number;
  currentVdot: number;
  vdotDelta30d: number;
  currentStreakDays: number;
  longestStreakDays: number;
  estimatedCaloriesKcal: number;
  longestRunKm: number;
  fastestPaceSec: number;
}

export interface DailyFitnessPoint {
  dateKey: string;
  dailyLoad: number;
  ctlFitness: number;
  atlFatigue: number;
  tsbForm: number;
  distanceKm: number;
}

export interface WorkloadStatus {
  acwr: number;
  acute7dLoad: number;
  chronic28dWeeklyAvgLoad: number;
  zone: "UNDERTRAINED" | "SWEET_SPOT" | "CAUTION" | "HIGH_RISK";
  label: string;
  recommendation: string;
}

export interface PolarizedDistribution {
  easyPct: number;
  moderatePct: number;
  hardPct: number;
  easyKm: number;
  moderateKm: number;
  hardKm: number;
  alignmentScore: number;
  coachingNote: string;
}

export interface EfficiencyPoint {
  workoutId: string;
  dateKey: string;
  title: string;
  distanceKm: number;
  avgPaceSec: number;
  avgHeartRate: number;
  efficiencyFactor: number;
  vdot: number;
  cadenceSpm: number;
  strideLengthM: number;
}

export interface PacingConsistency {
  avgSplitCvPct: number;
  negativeSplitCount: number;
  evenSplitCount: number;
  positiveSplitCount: number;
  grade: string;
  bestPacedWorkoutTitle: string;
  summary: string;
}

export interface RaceReadiness {
  score: number;
  grade: string;
  goalTitle: string;
  targetDistanceLabel: string;
  volumeReadinessPct: number;
  vdotGapSecPerKm: number;
  acwrSafe: boolean;
  longRunCoveragePct: number;
  bottleneck: string;
  actionItems: string[];
}

export interface MonthlyTrend {
  monthKey: string;
  monthLabel: string;
  runsCount: number;
  totalDistanceKm: number;
  avgPaceSec: number;
  elevationGainM: number;
  avgLoad: number;
}

export interface HeatmapDay {
  dateKey: string;
  distanceKm: number;
  runsCount: number;
  intensityLevel: number;
}

export interface CadenceDiagnostics {
  avgCadenceSpm: number;
  estimatedAvgStrideLengthM: number;
  optimalRange: string;
  status: string;
  recommendedModuleId: string;
  coachingNote: string;
}

export interface WorkoutDeepMetrics {
  workoutId: string;
  splits: SplitItem[];
  splitCvPct: number;
  firstHalfAvgPaceSec: number;
  secondHalfAvgPaceSec: number;
  splitDeltaSec: number;
  splitStrategy: string;
  fastestSplit: SplitItem | null;
  slowestSplit: SplitItem | null;
  estimatedRtss: number;
  aerobicEfficiency: number;
  strideLengthM: number;
  vdot: number;
  zoneCode: string;
  zoneName: string;
  paceSpreadSec: number;
}

export interface ComprehensiveAnalyticsResponse {
  overview: OverviewSummary;
  racePredictions: RacePrediction[];
  paceZones: TrainingPaceZone[];
  shoeUnlock: ShoeUnlockProgress;
  weeklyProgress: WeeklyAnalytics;
  goalSchedules: GoalScheduleWithSummary[];
  fitnessLoadSeries: DailyFitnessPoint[];
  workloadStatus: WorkloadStatus;
  polarizedDistribution: PolarizedDistribution;
  efficiencyTrend: EfficiencyPoint[];
  pacingConsistency: PacingConsistency;
  raceReadiness: RaceReadiness;
  monthlyTrends: MonthlyTrend[];
  activityHeatmap: HeatmapDay[];
  cadenceDiagnostics: CadenceDiagnostics;
  workoutDeepMetrics: Record<string, WorkoutDeepMetrics>;
}

export interface BiomechanicsModule {
  id: string;
  title: string;
  subtitle: string;
  focusMetricLabel: string;
  optimalTargetRange: string;
  commonFault: string;
  biomechanicalCue: string;
  drills: string[];
  targetKneeFlexionDeg: number;
  targetTibialAngleDeg: number;
  targetTrunkLeanDeg: number;
  targetCadenceSpm: number;
}

export interface ExtractedStravaMetrics {
  title: string;
  distanceKm: number;
  durationSeconds: number;
  avgPaceSecondsPerKm: number;
  avgHeartRate: number;
  cadenceSpm: number;
  elevationGainM: number;
  splitsSummary: string;
  aiCoachingInsight: string;
  workoutDateEpochMs?: number | null;
  detectedDateStr: string;
  sourceEngine: string;
}
