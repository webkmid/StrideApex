"use client";

import React, { useState } from "react";
import Link from "next/link";
import {
  Activity,
  ArrowRight,
  Award,
  Calendar,
  Camera,
  Cloud,
  Download,
  Flame,
  Footprints,
  Gauge,
  Plus,
  ShieldCheck,
  TrendingUp,
} from "lucide-react";
import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import { AdBanner } from "@/components/AdBanner";
import { GoalFormModal } from "@/components/GoalFormModal";
import { formatDateId, formatDuration, formatPace, stripScheduleSuffix } from "@/lib/formatters";
import { useStrideApex } from "@/lib/strideapex-context";

export default function DashboardPage() {
  const {
    workouts,
    analytics,
    backups,
    createBackupCheckpoint,
    exportData,
    saveGoalSchedule,
  } = useStrideApex();

  const [backupLabel, setBackupLabel] = useState("");
  const [showGoalModal, setShowGoalModal] = useState(false);

  const { overview, weeklyProgress, shoeUnlock, workloadStatus, raceReadiness, goalSchedules } = analytics;
  const activeGoalSummary = goalSchedules.find((g) => !g.goal.completed) ?? goalSchedules[0] ?? null;
  const todaySession =
    activeGoalSummary?.sessions.find((s) => s.status === "TODAY_PENDING") ??
    activeGoalSummary?.sessions.find((s) => s.status === "UPCOMING") ??
    null;

  const recentRuns = workouts.slice(0, 3);
  const weeklyRatioPct = Math.min(
    100,
    Math.round((weeklyProgress.totalDistanceKm / Math.max(1, weeklyProgress.targetDistanceKm)) * 100)
  );
  const shoePct = Math.round(shoeUnlock.progressToNextTier * 100);

  const acwrColor =
    workloadStatus.zone === "SWEET_SPOT"
      ? "text-success border-success/40 bg-success/10"
      : workloadStatus.zone === "CAUTION"
      ? "text-warning border-warning/40 bg-warning/10"
      : workloadStatus.zone === "HIGH_RISK"
      ? "text-danger border-danger/40 bg-danger/10"
      : "text-accent-blue border-accent-blue/40 bg-accent-blue/10";

  return (
    <div className="space-y-6">
      {/* Quick Action Bar: Scan Run */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 rounded-2xl border border-primary/35 bg-card p-4 shadow-sm">
        <div>
          <p className="text-xs font-bold uppercase tracking-wider text-primary">
            AI Vision OCR &amp; Telemetry
          </p>
          <p className="text-xs text-text-secondary mt-0.5">
            Upload screenshot Strava atau catat sesi lari baru secara instan.
          </p>
        </div>
        <Link
          href="/scan"
          className="inline-flex items-center justify-center gap-2 rounded-xl bg-primary px-4 py-2.5 text-xs font-bold text-white hover:bg-primary-dim transition-colors shrink-0"
        >
          <Camera className="w-4 h-4" />
          <span>Scan Run</span>
        </Link>
      </div>

      {/* Top 5 KPI Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-5 gap-4">
        <div className="p-4 rounded-2xl bg-card border border-border">
          <div className="flex items-center justify-between text-xs text-text-secondary font-semibold">
            <span>ESTIMASI VDOT</span>
            <TrendingUp className="w-4 h-4 text-primary" />
          </div>
          <div className="mt-2 flex items-baseline gap-2">
            <span className="text-2xl font-bold font-mono tabular-nums text-primary">
              {workouts.length > 0 && overview.currentVdot > 0 ? overview.currentVdot.toFixed(1) : "--"}
            </span>
            {workouts.length > 0 && overview.currentVdot > 0 && overview.vdotDelta30d !== 0 && (
              <span
                className={`text-xs font-mono font-semibold ${
                  overview.vdotDelta30d > 0 ? "text-success" : "text-danger"
                }`}
              >
                {overview.vdotDelta30d > 0 ? `+${overview.vdotDelta30d}` : overview.vdotDelta30d} (30h)
              </span>
            )}
          </div>
          <div className="mt-1 text-xs text-text-secondary">
            {overview.totalWorkouts} sesi tercatat
          </div>
        </div>

        <div className="p-4 rounded-2xl bg-card border border-border">
          <div className="flex items-center justify-between text-xs text-text-secondary font-semibold">
            <span>TOTAL VOLUME</span>
            <Activity className="w-4 h-4 text-accent-blue" />
          </div>
          <div className="mt-2 text-2xl font-bold font-mono tabular-nums">
            {overview.totalDistanceKm.toFixed(1)} <span className="text-sm font-normal text-text-secondary">km</span>
          </div>
          <div className="mt-1 text-xs text-text-secondary font-mono">
            Avg Pace {formatPace(overview.avgPaceSec)}
          </div>
        </div>

        <Link
          href="/analytics"
          className="p-4 rounded-2xl bg-card border border-border hover:border-primary/50 transition-colors block"
        >
          <div className="flex items-center justify-between text-xs text-text-secondary font-semibold">
            <span>BEBAN ACWR</span>
            <ShieldCheck className="w-4 h-4 text-success" />
          </div>
          <div className="mt-2 flex items-center gap-2">
            <span className="text-2xl font-bold font-mono tabular-nums">
              {workloadStatus.acwr.toFixed(2)}
            </span>
            <span className={`px-2 py-0.5 rounded-full text-[10px] font-bold border ${acwrColor}`}>
              {workloadStatus.zone.replace("_", " ")}
            </span>
          </div>
          <div className="mt-1 text-xs text-text-secondary truncate">{workloadStatus.label}</div>
        </Link>

        <Link
          href="/goals"
          className="p-4 rounded-2xl bg-card border border-border hover:border-primary/50 transition-colors block"
        >
          <div className="flex items-center justify-between text-xs text-text-secondary font-semibold">
            <span>RACE READINESS</span>
            <Gauge className="w-4 h-4 text-primary" />
          </div>
          <div className="mt-2 flex items-baseline gap-2">
            <span className="text-2xl font-bold font-mono tabular-nums text-text-primary">
              {raceReadiness.score}
            </span>
            <span className="text-xs font-semibold text-primary">/ 100 · {raceReadiness.grade}</span>
          </div>
          <div className="mt-1 text-xs text-text-secondary truncate">{raceReadiness.goalTitle}</div>
        </Link>

        <Link
          href="/shoes"
          className="p-4 rounded-2xl bg-card border border-border hover:border-primary/50 transition-colors block"
        >
          <div className="flex items-center justify-between text-xs text-text-secondary font-semibold">
            <span>TIER SEPATU</span>
            <Footprints className="w-4 h-4 text-accent-blue" />
          </div>
          <div className="mt-2 text-base font-bold text-text-primary truncate">
            {shoeUnlock.currentTier.badgeLabel}
          </div>
          <div className="mt-1 text-xs text-text-secondary font-mono">
            Top 3 Pace: {formatPace(shoeUnlock.currentAvgPaceSec)}
          </div>
        </Link>
      </div>

      {/* Clickable Dual Progress Strip: Goals + Shoe Tier */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
        <Link
          href="/goals"
          className="p-5 rounded-2xl bg-card border border-border hover:border-primary/50 transition-colors block space-y-3"
        >
          <div className="flex items-center justify-between gap-2">
            <div>
              <div className="text-xs font-bold uppercase tracking-wider text-primary">
                Progress Target Lari Mingguan
              </div>
              <div className="text-base font-bold mt-0.5">
                {activeGoalSummary
                  ? stripScheduleSuffix(activeGoalSummary.goal.title)
                  : "Target Mingguan Standar"}
              </div>
            </div>
            <span className="text-sm font-mono font-bold tabular-nums text-primary">
              {weeklyProgress.totalDistanceKm.toFixed(1)} / {weeklyProgress.targetDistanceKm} km
            </span>
          </div>

          <div className="w-full h-2.5 rounded-full bg-elevated overflow-hidden">
            <div
              className="h-full rounded-full bg-primary transition-all duration-300"
              style={{ width: `${weeklyRatioPct}%` }}
            />
          </div>

          <div className="flex items-center justify-between text-xs text-text-secondary">
            <span>{weeklyProgress.runsCount} sesi minggu ini ({weeklyRatioPct}%)</span>
            <span className="inline-flex items-center gap-1 text-primary font-semibold">
              Buka Jadwal & Target <ArrowRight className="w-3.5 h-3.5" />
            </span>
          </div>
        </Link>

        <Link
          href="/shoes"
          className="p-5 rounded-2xl bg-card border border-border hover:border-primary/50 transition-colors block space-y-3"
        >
          <div className="flex items-center justify-between gap-2">
            <div>
              <div className="text-xs font-bold uppercase tracking-wider text-accent-blue">
                Progress Unlock Sepatu (Top 3 · 30 Hari)
              </div>
              <div className="text-base font-bold mt-0.5">
                {shoeUnlock.nextTier ? `Menuju ${shoeUnlock.nextTier.badgeLabel}` : "Semua Kasta Terbuka"}
              </div>
            </div>
            <span className="text-sm font-mono font-bold tabular-nums text-accent-blue">
              {shoePct}%
            </span>
          </div>

          <div className="w-full h-2.5 rounded-full bg-elevated overflow-hidden">
            <div
              className="h-full rounded-full bg-accent-blue transition-all duration-300"
              style={{ width: `${shoePct}%` }}
            />
          </div>

          <div className="flex items-center justify-between text-xs text-text-secondary">
            <span>
              {shoeUnlock.nextTier
                ? `Butuh -${shoeUnlock.secondsNeededToNextTier}s/km (${shoeUnlock.nextTier.unlockPaceDisplay})`
                : "Puncak Tier 5 terbuka!"}
            </span>
            <span className="inline-flex items-center gap-1 text-accent-blue font-semibold">
              Lihat Katalog Sepatu <ArrowRight className="w-3.5 h-3.5" />
            </span>
          </div>
        </Link>
      </div>

      {/* Weekly Bar Chart + Today's Session & Target Action */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-5">
        <div className="lg:col-span-2 p-5 rounded-2xl bg-card border border-border space-y-4">
          <div className="flex items-center justify-between">
            <div>
              <h2 className="font-bold text-base">Volume Lari Minggu Ini</h2>
              <p className="text-xs text-text-secondary">{weeklyProgress.weekRangeDisplay}</p>
            </div>
            <div className="text-right font-mono text-xs">
              <div className="font-bold text-text-primary">{weeklyProgress.totalDistanceKm.toFixed(1)} km</div>
              <div className="text-text-secondary">
                {weeklyProgress.distanceDiffKm >= 0
                  ? `+${weeklyProgress.distanceDiffKm.toFixed(1)} km vs mgg lalu`
                  : `${weeklyProgress.distanceDiffKm.toFixed(1)} km vs mgg lalu`}
              </div>
            </div>
          </div>

          <div className="h-56 w-full">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={weeklyProgress.dailyStats} margin={{ top: 8, right: 8, bottom: 0, left: -16 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" vertical={false} />
                <XAxis dataKey="dayName" tick={{ fontSize: 12, fill: "var(--text-secondary)" }} />
                <YAxis tick={{ fontSize: 11, fill: "var(--text-secondary)" }} unit=" km" />
                <Tooltip
                  contentStyle={{
                    backgroundColor: "var(--card)",
                    borderColor: "var(--border)",
                    borderRadius: "0.75rem",
                    fontSize: "12px",
                  }}
                  formatter={(val) => [`${Number(val).toFixed(2)} km`, "Jarak"]}
                />
                <Bar dataKey="distanceKm" radius={[6, 6, 0, 0]}>
                  {weeklyProgress.dailyStats.map((entry) => (
                    <Cell
                      key={entry.dateKey}
                      fill={entry.isToday ? "var(--primary)" : entry.hasRun ? "var(--accent-blue)" : "var(--border)"}
                    />
                  ))}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          </div>

          <p className="text-xs text-text-secondary bg-elevated p-3 rounded-xl border border-border">
            {weeklyProgress.coachingTip}
          </p>
        </div>

        {/* Today's Reminder & Goal Quick Card */}
        <div className="p-5 rounded-2xl bg-card border border-border flex flex-col justify-between space-y-4">
          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <Calendar className="w-4 h-4 text-primary" />
                <h2 className="font-bold text-base">Jadwal Latihan</h2>
              </div>
              <button
                type="button"
                onClick={() => setShowGoalModal(true)}
                className="inline-flex items-center gap-1 px-2.5 py-1.5 rounded-lg border border-border bg-elevated text-xs font-semibold text-accent-blue hover:border-primary/50"
              >
                <Plus className="w-3.5 h-3.5" />
                <span>Target</span>
              </button>
            </div>

            {todaySession ? (
              <div className="p-3.5 rounded-xl bg-elevated border border-border space-y-2">
                <div className="flex items-center justify-between">
                  <span className="px-2 py-0.5 rounded-md bg-primary/15 text-primary text-[11px] font-bold">
                    {todaySession.shortBadge}
                  </span>
                  <span className="text-[11px] font-mono text-text-secondary">{todaySession.displayDate}</span>
                </div>
                <div className="font-bold text-sm">{todaySession.sessionTitle}</div>
                <p className="text-xs text-text-secondary leading-relaxed">{todaySession.sessionDescription}</p>
                <div className="pt-1 text-[11px] text-accent-blue font-medium">
                  AI Coach: {todaySession.aiReason}
                </div>
              </div>
            ) : (
              <div className="p-4 rounded-xl bg-elevated border border-border text-xs text-text-secondary">
                Belum ada target lomba aktif. Buat target untuk menghasilkan kalender latihan adaptif AI.
              </div>
            )}
          </div>

          <div className="grid grid-cols-2 gap-2 pt-2 border-t border-border text-xs">
            <div className="p-2.5 rounded-xl bg-elevated">
              <div className="text-text-secondary">Streak Aktif</div>
              <div className="font-bold font-mono text-sm flex items-center gap-1 mt-0.5">
                <Flame className="w-3.5 h-3.5 text-primary" />
                {overview.currentStreakDays} hari
              </div>
            </div>
            <div className="p-2.5 rounded-xl bg-elevated">
              <div className="text-text-secondary">Tercepat</div>
              <div className="font-bold font-mono text-sm flex items-center gap-1 mt-0.5">
                <Award className="w-3.5 h-3.5 text-success" />
                {formatPace(overview.fastestPaceSec)}
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Recent Runs + Backup & Export */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-5">
        <div className="lg:col-span-2 p-5 rounded-2xl bg-card border border-border space-y-3">
          <div className="flex items-center justify-between">
            <h2 className="font-bold text-base">Sesi Lari Terbaru</h2>
            <Link href="/history" className="text-xs font-semibold text-primary hover:underline">
              Lihat Semua →
            </Link>
          </div>

          {recentRuns.length === 0 ? (
            <div className="p-6 rounded-xl bg-elevated text-center text-xs text-text-secondary space-y-2">
              <p>Belum ada sesi lari. Gunakan tombol Scan Run di atas untuk memulai!</p>
              <Link
                href="/scan"
                className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-primary text-white text-xs font-semibold hover:bg-primary-dim transition-colors"
              >
                <Camera className="w-3.5 h-3.5" />
                <span>Scan Run</span>
              </Link>
            </div>
          ) : (
            <div className="space-y-2.5">
              {recentRuns.map((w) => (
                <Link
                  key={w.id}
                  href={`/history/${encodeURIComponent(w.id)}`}
                  className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 p-3.5 rounded-xl bg-elevated border border-border hover:border-primary/50 transition-colors"
                >
                  <div className="min-w-0">
                    <div className="flex items-center gap-2">
                      <span className="font-bold text-sm truncate">{w.title}</span>
                      <span className="px-1.5 py-0.5 rounded bg-card border border-border text-[10px] font-mono text-text-secondary">
                        {w.sourceType === "STRAVA_OCR" ? "OCR" : "MANUAL"}
                      </span>
                    </div>
                    <div className="text-xs text-text-secondary mt-0.5">{formatDateId(w.createdAtEpochMs)}</div>
                  </div>
                  <div className="flex items-center gap-4 font-mono text-xs tabular-nums shrink-0">
                    <div>
                      <div className="text-text-secondary text-[10px]">JARAK</div>
                      <div className="font-bold">{w.distanceKm.toFixed(2)} km</div>
                    </div>
                    <div>
                      <div className="text-text-secondary text-[10px]">DURASI</div>
                      <div className="font-bold">{formatDuration(w.durationSeconds)}</div>
                    </div>
                    <div>
                      <div className="text-text-secondary text-[10px]">PACE</div>
                      <div className="font-bold text-primary">{formatPace(w.avgPaceSecondsPerKm)}</div>
                    </div>
                  </div>
                </Link>
              ))}
            </div>
          )}
        </div>

        {/* Cloud Backup & Export Box */}
        <div className="p-5 rounded-2xl bg-card border border-border space-y-4">
          <div className="flex items-center gap-2">
            <Cloud className="w-4 h-4 text-accent-blue" />
            <h2 className="font-bold text-base">Backup & Ekspor Data</h2>
          </div>

          <div className="flex gap-2">
            <input
              type="text"
              value={backupLabel}
              onChange={(e) => setBackupLabel(e.target.value)}
              placeholder="Label checkpoint backup..."
              className="flex-1 min-w-0 px-3 py-2 rounded-xl bg-elevated border border-border text-xs focus:outline-none focus:border-primary"
            />
            <button
              type="button"
              onClick={() => {
                void createBackupCheckpoint(backupLabel);
                setBackupLabel("");
              }}
              className="px-3 py-2 rounded-xl bg-primary text-white text-xs font-semibold hover:bg-primary-dim shrink-0"
            >
              Simpan
            </button>
          </div>

          <div className="grid grid-cols-2 gap-2">
            <button
              type="button"
              onClick={() => void exportData("csv")}
              className="flex items-center justify-center gap-1.5 py-2 rounded-xl border border-border bg-elevated text-xs font-semibold hover:border-primary/50"
            >
              <Download className="w-3.5 h-3.5" /> Ekspor CSV
            </button>
            <button
              type="button"
              onClick={() => void exportData("json")}
              className="flex items-center justify-center gap-1.5 py-2 rounded-xl border border-border bg-elevated text-xs font-semibold hover:border-primary/50"
            >
              <Download className="w-3.5 h-3.5" /> Ekspor JSON
            </button>
          </div>

          <div className="space-y-1.5 max-h-36 overflow-y-auto">
            {backups.slice(0, 3).map((b) => (
              <div key={b.id} className="p-2.5 rounded-xl bg-elevated border border-border text-xs">
                <div className="font-semibold truncate">{b.backupLabel}</div>
                <div className="text-[11px] text-text-secondary">
                  {b.workoutCount} sesi · {b.prCount} PR · {formatDateId(b.createdAtEpochMs)}
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>

      {showGoalModal && (
        <GoalFormModal onClose={() => setShowGoalModal(false)} onSave={saveGoalSchedule} />
      )}

      <AdBanner slotKey="footer" />
    </div>
  );
}
