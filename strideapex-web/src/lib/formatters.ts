import type { DistanceCategoryInfo, DistanceCategoryKey, SplitItem } from "./types";

export const DISTANCE_CATEGORIES: DistanceCategoryInfo[] = [
  { key: "5K", displayName: "5K Run", shortLabel: "5K", meters: 5000, kilometers: 5.0 },
  { key: "10K", displayName: "10K Run", shortLabel: "10K", meters: 10000, kilometers: 10.0 },
  { key: "HALF_MARATHON", displayName: "Half Marathon", shortLabel: "21.1K", meters: 21097, kilometers: 21.0975 },
  { key: "MARATHON", displayName: "Full Marathon", shortLabel: "42.2K", meters: 42195, kilometers: 42.195 },
];

export function getDistanceCategoryInfo(key?: string): DistanceCategoryInfo {
  return (
    DISTANCE_CATEGORIES.find((c) => c.key.toUpperCase() === (key ?? "").toUpperCase()) ??
    DISTANCE_CATEGORIES[1]
  );
}

export function formatDuration(seconds: number): string {
  const safe = Math.max(0, Math.round(seconds));
  const hrs = Math.floor(safe / 3600);
  const mins = Math.floor((safe % 3600) / 60);
  const secs = safe % 60;
  if (hrs > 0) {
    return `${hrs}:${String(mins).padStart(2, "0")}:${String(secs).padStart(2, "0")}`;
  }
  return `${String(mins).padStart(2, "0")}:${String(secs).padStart(2, "0")}`;
}

export function formatPace(secondsPerKm: number): string {
  if (!secondsPerKm || secondsPerKm <= 0) return "--:-- /km";
  const safe = Math.round(secondsPerKm);
  return `${Math.floor(safe / 60)}:${String(safe % 60).padStart(2, "0")} /km`;
}

export function formatPaceShort(secondsPerKm: number): string {
  if (!secondsPerKm || secondsPerKm <= 0) return "--:--";
  const safe = Math.round(secondsPerKm);
  return `${Math.floor(safe / 60)}:${String(safe % 60).padStart(2, "0")}`;
}

export function formatDateId(epochMs: number): string {
  if (!epochMs) return "-";
  return new Intl.DateTimeFormat("id-ID", {
    day: "2-digit",
    month: "short",
    year: "numeric",
  }).format(new Date(epochMs));
}

export function formatDateTimeId(epochMs: number): string {
  if (!epochMs) return "-";
  return new Intl.DateTimeFormat("id-ID", {
    day: "2-digit",
    month: "short",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(epochMs));
}

export function toDateInputValue(epochMs: number): string {
  const d = new Date(epochMs > 0 ? epochMs : Date.now());
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  return `${y}-${m}-${day}`;
}

export function fromDateInputValue(dateKey: string): number {
  const parts = dateKey.split("-").map(Number);
  if (parts.length !== 3 || parts.some(Number.isNaN)) return Date.now();
  const dt = new Date(parts[0], parts[1] - 1, parts[2], 7, 0, 0, 0);
  return Math.min(dt.getTime(), Date.now() - 1000);
}

const SCHEDULE_SUFFIX_REGEX = /\s*\[(?:(\d{4}-\d{2}-\d{2})~)?(\d{4}-\d{2}-\d{2})\|([0-9,]+)\]$/;

export function stripScheduleSuffix(title: string): string {
  return (title ?? "").replace(SCHEDULE_SUFFIX_REGEX, "").trim();
}

export function encodeGoalTitleWithSchedule(
  cleanTitle: string,
  targetDateKey: string,
  trainingDaysIso: number[],
  startDateKey?: string
): string {
  const sortedDays = Array.from(new Set(trainingDaysIso.filter((d) => d >= 1 && d <= 7))).sort((a, b) => a - b);
  const days = sortedDays.length > 0 ? sortedDays : [2, 4, 7];
  const datePart = startDateKey && startDateKey <= targetDateKey ? `${startDateKey}~${targetDateKey}` : targetDateKey;
  const suffix = ` [${datePart}|${days.join(",")}]`;
  const maxTitleLen = Math.max(10, 120 - suffix.length);
  const clean = stripScheduleSuffix(cleanTitle).slice(0, maxTitleLen).trim() || "Target Lari";
  return `${clean}${suffix}`;
}

export function dayNameId(isoDay: number): string {
  return ["Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu", "Minggu"][isoDay - 1] ?? "Minggu";
}

export function dayShortNameId(isoDay: number): string {
  return ["Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min"][isoDay - 1] ?? "Min";
}

export function parseSplitsSummary(splitsSummary: string, avgPaceSec = 0): SplitItem[] {
  if (!splitsSummary.trim()) return [];
  const regex = /(?:KM\s*)?(0[.,]\d+|[1-9]\d*(?:[.,]\d+)?)\s*[:.\-]?\s*([0-9]{1,2}:[0-5][0-9])(?:\/km)?/gi;
  const raw: { km: number; paceStr: string; paceSeconds: number }[] = [];
  let match: RegExpExecArray | null;
  while ((match = regex.exec(splitsSummary)) !== null) {
    const km = parseFloat(match[1].replace(",", "."));
    const paceStr = match[2];
    const [m, s] = paceStr.split(":").map(Number);
    const sec = m * 60 + s;
    if (!Number.isNaN(km) && km > 0 && km <= 100 && sec >= 120 && sec <= 900) {
      raw.push({ km, paceStr, paceSeconds: sec });
    }
  }
  if (raw.length === 0) return [];
  const avg = avgPaceSec > 0 ? avgPaceSec : Math.round(raw.reduce((a, b) => a + b.paceSeconds, 0) / raw.length);
  return raw.map((item) => ({
    ...item,
    deltaFromAvgSeconds: item.paceSeconds - avg,
  }));
}

export function normalizeCategoryKey(raw: string): DistanceCategoryKey {
  const upper = raw.toUpperCase();
  if (upper === "5K" || upper === "10K" || upper === "HALF_MARATHON" || upper === "MARATHON") {
    return upper;
  }
  return "10K";
}
