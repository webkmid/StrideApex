"use client";

import React from "react";
import Link from "next/link";
import { ArrowRight, Calendar, Footprints } from "lucide-react";
import { Bar, BarChart, CartesianGrid, Cell, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import { AdBanner } from "@/components/AdBanner";
import { formatDuration, formatPace } from "@/lib/formatters";
import { useStrideApex } from "@/lib/strideapex-context";

export default function ProgressPage() {
  const { analytics } = useStrideApex();
  const { shoeUnlock, weeklyProgress } = analytics;

  return (
    <div className="space-y-6">
      {/* Header + Dedicated Shoes Shortcut */}
      <div className="p-5 rounded-2xl bg-card border border-border flex flex-wrap items-center justify-between gap-4">
        <div>
          <div className="inline-flex items-center gap-1.5 text-xs font-bold uppercase tracking-wider text-primary">
            <Calendar className="w-4 h-4" />
            <span>Progress Mingguan</span>
          </div>
          <h2 className="text-xl font-bold mt-1">Volume & Konsistensi Lari Mingguan</h2>
          <p className="text-xs text-text-secondary mt-0.5">
            Pantau pencapaian kilometer harian (Senin – Minggu) dibanding target mingguan Anda.
          </p>
        </div>

        <Link
          href="/shoes"
          className="inline-flex items-center gap-2 px-4 py-2.5 rounded-xl bg-elevated border border-border hover:border-primary/50 text-xs font-bold text-text-primary transition-colors"
        >
          <Footprints className="w-4 h-4 text-primary" />
          <span>Katalog Sepatu ({shoeUnlock.currentTier.badgeLabel})</span>
          <ArrowRight className="w-3.5 h-3.5 text-primary" />
        </Link>
      </div>

      {/* Weekly Progress Summary */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
        <div className="p-4 rounded-2xl bg-card border border-border">
          <div className="text-xs text-text-secondary font-semibold">VOLUME MINGGU INI</div>
          <div className="mt-1 text-2xl font-bold font-mono text-primary">
            {weeklyProgress.totalDistanceKm.toFixed(1)} km
          </div>
          <div className="text-xs text-text-secondary">Target {weeklyProgress.targetDistanceKm} km</div>
        </div>
        <div className="p-4 rounded-2xl bg-card border border-border">
          <div className="text-xs text-text-secondary font-semibold">JUMLAH SESI</div>
          <div className="mt-1 text-2xl font-bold font-mono">{weeklyProgress.runsCount}</div>
          <div className="text-xs text-text-secondary">{weeklyProgress.weekRangeDisplay}</div>
        </div>
        <div className="p-4 rounded-2xl bg-card border border-border">
          <div className="text-xs text-text-secondary font-semibold">TOTAL DURASI</div>
          <div className="mt-1 text-2xl font-bold font-mono">{formatDuration(weeklyProgress.totalDurationSec)}</div>
          <div className="text-xs text-text-secondary">Waktu aktif</div>
        </div>
        <div className="p-4 rounded-2xl bg-card border border-border">
          <div className="text-xs text-text-secondary font-semibold">PACE MINGGUAN</div>
          <div className="mt-1 text-2xl font-bold font-mono text-accent-blue">
            {formatPace(weeklyProgress.avgPaceSec)}
          </div>
          <div className="text-xs text-text-secondary">
            {weeklyProgress.paceDiffSec < 0
              ? `${Math.abs(weeklyProgress.paceDiffSec)}s lebih cepat`
              : weeklyProgress.paceDiffSec > 0
              ? `+${weeklyProgress.paceDiffSec}s vs mgg lalu`
              : "Minggu baseline"}
          </div>
        </div>
      </div>

      <div className="p-5 rounded-2xl bg-card border border-border space-y-4">
        <h3 className="font-bold text-base">Distribusi Harian (Senin – Minggu)</h3>
        <div className="h-60 w-full">
          <ResponsiveContainer width="100%" height="100%">
            <BarChart data={weeklyProgress.dailyStats}>
              <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" vertical={false} />
              <XAxis dataKey="dayName" tick={{ fontSize: 12, fill: "var(--text-secondary)" }} />
              <YAxis tick={{ fontSize: 11, fill: "var(--text-secondary)" }} unit=" km" />
              <Tooltip
                contentStyle={{
                  backgroundColor: "var(--card)",
                  borderColor: "var(--border)",
                  borderRadius: "0.75rem",
                }}
              />
              <Bar dataKey="distanceKm" radius={[6, 6, 0, 0]}>
                {weeklyProgress.dailyStats.map((d) => (
                  <Cell
                    key={d.dateKey}
                    fill={d.isToday ? "var(--primary)" : d.hasRun ? "var(--accent-blue)" : "var(--border)"}
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

      <AdBanner slotKey="footer" />
    </div>
  );
}
