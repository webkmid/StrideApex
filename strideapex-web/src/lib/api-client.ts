import type {
  BiomechanicsModule,
  ComprehensiveAnalyticsResponse,
  ExtractedStravaMetrics,
  PersonalRecord,
  TrainingGoal,
  WorkoutLog,
} from "./types";

const API_BASE = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:5180";

async function postJson<TResponse>(path: string, body: unknown): Promise<TResponse> {
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), 20_000);
  try {
    const res = await fetch(`${API_BASE}${path}`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body),
      signal: controller.signal,
    });
    if (!res.ok) {
      throw new Error(`API ${path} failed (${res.status})`);
    }
    return (await res.json()) as TResponse;
  } finally {
    clearTimeout(timeout);
  }
}

export async function fetchApiHealth(): Promise<{ status: string; geminiConfigured: boolean }> {
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), 4000);
  try {
    const res = await fetch(`${API_BASE}/api/health`, { signal: controller.signal });
    if (!res.ok) throw new Error("Health check failed");
    return (await res.json()) as { status: string; geminiConfigured: boolean };
  } finally {
    clearTimeout(timeout);
  }
}

export async function fetchComprehensiveAnalytics(params: {
  workouts: WorkoutLog[];
  prs: PersonalRecord[];
  goals: TrainingGoal[];
  fatigueExponent?: number;
}): Promise<ComprehensiveAnalyticsResponse> {
  return postJson<ComprehensiveAnalyticsResponse>("/api/analytics/comprehensive", {
    workouts: params.workouts,
    prs: params.prs,
    goals: params.goals,
    fatigueExponent: params.fatigueExponent ?? 1.06,
    nowEpochMs: Date.now(),
  });
}

export async function extractStravaViaApi(params: {
  imageBase64?: string;
  mimeType?: string;
  rawText?: string;
  presetIndex?: number;
}): Promise<ExtractedStravaMetrics> {
  return postJson<ExtractedStravaMetrics>("/api/ocr/extract", params);
}

export async function refreshWorkoutAiViaApi(
  workout: WorkoutLog,
  prs: PersonalRecord[]
): Promise<{ recalibratedSplits: string; aiCoachingInsight: string; sourceEngine: string }> {
  return postJson("/api/ai/refresh-workout", { workout, prs });
}

export async function fetchPlanAdviceViaApi(
  goal: TrainingGoal,
  workouts: WorkoutLog[]
): Promise<{ advice: string; sourceEngine: string }> {
  return postJson("/api/ai/plan-advice", { goal, workouts });
}

export async function askBiomechanicsViaApi(
  question: string,
  module: BiomechanicsModule
): Promise<{ answer: string; sourceEngine: string }> {
  return postJson("/api/ai/biomechanics-coach", { question, module });
}

export async function downloadExportFile(
  format: "csv" | "json",
  payload: { workouts: WorkoutLog[]; prs: PersonalRecord[]; goals: TrainingGoal[] }
): Promise<void> {
  const res = await fetch(`${API_BASE}/api/export/${format}`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  });
  if (!res.ok) throw new Error(`Export ${format} failed`);
  const blob = await res.blob();
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = format === "csv" ? "strideapex-workouts.csv" : "strideapex-backup.json";
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
}
