"use client";

import React from "react";
import {
  CartesianGrid,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import { formatPaceShort, parseSplitsSummary } from "@/lib/formatters";
import type { WorkoutDeepMetrics } from "@/lib/types";

interface SplitsVisualChartProps {
  splitsSummary: string;
  avgPaceSecondsPerKm: number;
  deepMetrics?: WorkoutDeepMetrics;
}

export function SplitsVisualChart({
  splitsSummary,
  avgPaceSecondsPerKm,
  deepMetrics,
}: SplitsVisualChartProps) {
  const splits = deepMetrics?.splits?.length
    ? deepMetrics.splits
    : parseSplitsSummary(splitsSummary, avgPaceSecondsPerKm);

  if (splits.length === 0) {
    return (
      <div className="p-4 rounded-xl bg-elevated border border-border text-xs text-text-secondary">
        Belum ada rincian split per kilometer pada sesi ini. Edit sesi atau klik Refresh AI untuk membentuk split dari jarak & pace.
      </div>
    );
  }

  const minSec = Math.max(60, Math.min(...splits.map((s) => s.paceSeconds)));
  const maxSec = Math.max(minSec + 1, Math.max(...splits.map((s) => s.paceSeconds)));
  const spread = Math.max(10, maxSec - minSec);

  const chartData = splits.map((s) => ({
    kmLabel: `KM ${s.km}`,
    paceSeconds: s.paceSeconds,
    paceStr: s.paceStr,
  }));

  return (
    <div className="space-y-4">
      {/* Telemetry Strip */}
      {deepMetrics && (
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
          <div className="p-3 rounded-xl bg-elevated border border-border">
            <div className="text-[10px] font-semibold uppercase tracking-wider text-text-secondary">
              Strategi Split
            </div>
            <div className="mt-1 text-sm font-bold text-text-primary">{deepMetrics.splitStrategy}</div>
            <div className="text-[11px] font-mono text-text-secondary">
              Delta Paruh: {deepMetrics.splitDeltaSec > 0 ? `+${deepMetrics.splitDeltaSec}s` : `${deepMetrics.splitDeltaSec}s`}
            </div>
          </div>
          <div className="p-3 rounded-xl bg-elevated border border-border">
            <div className="text-[10px] font-semibold uppercase tracking-wider text-text-secondary">
              Variasi Pace (CV)
            </div>
            <div className="mt-1 text-sm font-bold font-mono tabular-nums text-accent-blue">
              {deepMetrics.splitCvPct.toFixed(1)}%
            </div>
            <div className="text-[11px] text-text-secondary">
              Spread {deepMetrics.paceSpreadSec}s/km
            </div>
          </div>
          <div className="p-3 rounded-xl bg-elevated border border-border">
            <div className="text-[10px] font-semibold uppercase tracking-wider text-text-secondary">
              KM Tercepat
            </div>
            <div className="mt-1 text-sm font-bold font-mono tabular-nums text-success">
              {deepMetrics.fastestSplit ? `KM ${deepMetrics.fastestSplit.km} · ${deepMetrics.fastestSplit.paceStr}` : "-"}
            </div>
            <div className="text-[11px] text-text-secondary">Paruh 1: {formatPaceShort(deepMetrics.firstHalfAvgPaceSec)}</div>
          </div>
          <div className="p-3 rounded-xl bg-elevated border border-border">
            <div className="text-[10px] font-semibold uppercase tracking-wider text-text-secondary">
              KM Terlambat
            </div>
            <div className="mt-1 text-sm font-bold font-mono tabular-nums text-danger">
              {deepMetrics.slowestSplit ? `KM ${deepMetrics.slowestSplit.km} · ${deepMetrics.slowestSplit.paceStr}` : "-"}
            </div>
            <div className="text-[11px] text-text-secondary">Paruh 2: {formatPaceShort(deepMetrics.secondHalfAvgPaceSec)}</div>
          </div>
        </div>
      )}

      {/* Strava Horizontal Bar Breakdown */}
      <div className="p-4 rounded-xl bg-elevated border border-border space-y-2.5">
        <div className="flex items-center justify-between">
          <span className="text-xs font-bold tracking-wider uppercase text-accent-blue">
            Grafik Split Per Kilometer
          </span>
          <span className="text-xs font-mono text-text-secondary">{splits.length} KM</span>
        </div>

        <div className="space-y-2">
          {splits.map((s, idx) => {
            const normalized = 1 - (s.paceSeconds - minSec) / spread;
            const widthPct = Math.min(100, Math.max(22, Math.round((0.35 + normalized * 0.65) * 100)));
            const deltaText =
              s.deltaFromAvgSeconds === 0
                ? "0s"
                : s.deltaFromAvgSeconds < 0
                ? `${s.deltaFromAvgSeconds}s`
                : `+${s.deltaFromAvgSeconds}s`;
            const deltaColor =
              s.deltaFromAvgSeconds < 0
                ? "text-success"
                : s.deltaFromAvgSeconds === 0
                ? "text-text-secondary"
                : "text-danger";

            return (
              <div key={`${s.km}-${idx}`} className="flex items-center gap-3 text-xs">
                <span className="w-14 shrink-0 font-mono text-text-secondary tabular-nums">KM {s.km}</span>
                <span className="w-12 font-mono font-bold tabular-nums text-text-primary">{s.paceStr}</span>
                <div className="flex-1 h-3.5 bg-card rounded overflow-hidden border border-border/50">
                  <div
                    className="h-full rounded bg-primary transition-all duration-300"
                    style={{ width: `${widthPct}%` }}
                  />
                </div>
                <span className={`w-11 text-right font-mono tabular-nums font-semibold ${deltaColor}`}>
                  {deltaText}
                </span>
              </div>
            );
          })}
        </div>
      </div>

      {/* Pace Trajectory Curve */}
      {chartData.length >= 2 && (
        <div className="p-4 rounded-xl bg-elevated border border-border">
          <div className="text-xs font-bold uppercase tracking-wider text-text-secondary mb-3">
            Kurva Tren Pace Antar KM (Lebih Tinggi = Lebih Cepat)
          </div>
          <div className="h-52 w-full">
            <ResponsiveContainer width="100%" height="100%">
              <LineChart data={chartData} margin={{ top: 8, right: 12, bottom: 4, left: 0 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" />
                <XAxis dataKey="kmLabel" tick={{ fontSize: 11, fill: "var(--text-secondary)" }} />
                <YAxis
                  reversed
                  domain={[minSec - 8, maxSec + 8]}
                  tickFormatter={(v) => formatPaceShort(Number(v))}
                  tick={{ fontSize: 11, fill: "var(--text-secondary)" }}
                  width={48}
                />
                <Tooltip
                  contentStyle={{
                    backgroundColor: "var(--card)",
                    borderColor: "var(--border)",
                    borderRadius: "0.75rem",
                    fontSize: "12px",
                  }}
                  formatter={(value) => [formatPaceShort(Number(value)) + " /km", "Pace"]}
                />
                <Line
                  type="monotone"
                  dataKey="paceSeconds"
                  stroke="var(--primary)"
                  strokeWidth={2.5}
                  dot={{ r: 3.5, fill: "var(--primary)" }}
                  activeDot={{ r: 5 }}
                />
              </LineChart>
            </ResponsiveContainer>
          </div>
        </div>
      )}
    </div>
  );
}
