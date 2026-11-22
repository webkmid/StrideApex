"use client";

import React, { useState } from "react";
import { Activity, AlertTriangle, Flame, HeartPulse, PieChart as PieIcon, ShieldCheck } from "lucide-react";
import {
  Area,
  Bar,
  CartesianGrid,
  Cell,
  ComposedChart,
  Line,
  LineChart,
  Pie,
  PieChart,
  ReferenceLine,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import { formatPace, formatPaceShort } from "@/lib/formatters";
import { useStrideApex } from "@/lib/strideapex-context";

export default function AnalyticsLabPage() {
  const { analytics, backendOnline } = useStrideApex();
  const [rangeDays, setRangeDays] = useState<30 | 60 | 90>(90);

  const {
    fitnessLoadSeries,
    workloadStatus,
    polarizedDistribution,
    efficiencyTrend,
    pacingConsistency,
    monthlyTrends,
    activityHeatmap,
    cadenceDiagnostics,
    raceReadiness,
  } = analytics;

  const visibleFitness = fitnessLoadSeries.slice(-rangeDays);
  const latestFitness = visibleFitness[visibleFitness.length - 1];

  const pieData = [
    { name: "Easy (Z1-2)", value: polarizedDistribution.easyPct, color: "var(--success)" },
    { name: "Moderate (Z3)", value: polarizedDistribution.moderatePct, color: "var(--warning)" },
    { name: "Hard (Z4-5)", value: polarizedDistribution.hardPct, color: "var(--primary)" },
  ];

  const efPoints = efficiencyTrend.filter((p) => p.efficiencyFactor > 0 || p.vdot > 0);
  const heatmapWeeks: typeof activityHeatmap[] = [];
  for (let i = 0; i < activityHeatmap.length; i += 7) {
    heatmapWeeks.push(activityHeatmap.slice(i, i + 7));
  }

  return (
    <div className="space-y-6">
      {/* Header Banner */}
      <div className="p-5 rounded-2xl bg-card border border-border flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <span className="px-2 py-0.5 rounded-md bg-primary/15 text-primary text-[11px] font-bold font-mono">
              SPORTS SCIENCE ENGINE
            </span>
          </div>
          <h2 className="text-xl font-bold mt-1">Deep Performance, Beban Latihan & Fisiologi Lari</h2>
          <p className="text-xs text-text-secondary mt-0.5">
            Analisis CTL/ATL/TSB (Fitness-Fatigue-Form), risiko cedera ACWR, polarisasi 80/20, Aerobic Efficiency Factor, dan konsistensi split.
          </p>
        </div>

        <div className="flex items-center gap-1.5 bg-elevated p-1 rounded-xl border border-border shrink-0">
          {([30, 60, 90] as const).map((d) => (
            <button
              key={d}
              type="button"
              onClick={() => setRangeDays(d)}
              className={`px-3 py-1.5 rounded-lg text-xs font-mono font-bold transition-colors ${
                rangeDays === d ? "bg-primary text-white" : "text-text-secondary hover:text-text-primary"
              }`}
            >
              {d}H
            </button>
          ))}
        </div>
      </div>

      {/* 1. Fitness (CTL), Fatigue (ATL), Form (TSB) */}
      <div className="p-5 rounded-2xl bg-card border border-border space-y-4">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
          <div>
            <h3 className="font-bold text-base flex items-center gap-2">
              <Activity className="w-4 h-4 text-primary" />
              Kurva Kebugaran vs Kelelahan (CTL / ATL / TSB · {rangeDays} Hari)
            </h3>
            <p className="text-xs text-text-secondary">
              CTL (42d Fitness) · ATL (7d Kelelahan Akut) · TSB (Form = CTL − ATL; zona positif ideal untuk hari lomba)
            </p>
          </div>
          {latestFitness && (
            <div className="flex items-center gap-4 font-mono text-xs">
              <div>
                <span className="text-text-secondary">CTL: </span>
                <span className="font-bold text-accent-blue">{latestFitness.ctlFitness.toFixed(1)}</span>
              </div>
              <div>
                <span className="text-text-secondary">ATL: </span>
                <span className="font-bold text-primary">{latestFitness.atlFatigue.toFixed(1)}</span>
              </div>
              <div>
                <span className="text-text-secondary">TSB: </span>
                <span className={`font-bold ${latestFitness.tsbForm >= 0 ? "text-success" : "text-warning"}`}>
                  {latestFitness.tsbForm >= 0 ? `+${latestFitness.tsbForm.toFixed(1)}` : latestFitness.tsbForm.toFixed(1)}
                </span>
              </div>
            </div>
          )}
        </div>

        <div className="h-72 w-full">
          <ResponsiveContainer width="100%" height="100%">
            <ComposedChart data={visibleFitness} margin={{ top: 10, right: 12, bottom: 0, left: -12 }}>
              <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" />
              <XAxis
                dataKey="dateKey"
                tickFormatter={(v) => String(v).slice(5)}
                tick={{ fontSize: 11, fill: "var(--text-secondary)" }}
                minTickGap={22}
              />
              <YAxis tick={{ fontSize: 11, fill: "var(--text-secondary)" }} />
              <Tooltip
                contentStyle={{
                  backgroundColor: "var(--card)",
                  borderColor: "var(--border)",
                  borderRadius: "0.75rem",
                  fontSize: "12px",
                }}
              />
              <ReferenceLine y={0} stroke="var(--border)" strokeDasharray="3 3" />
              <Area
                type="monotone"
                dataKey="tsbForm"
                name="Form (TSB)"
                fill="var(--success)"
                fillOpacity={0.14}
                stroke="var(--success)"
                strokeWidth={1.8}
              />
              <Line
                type="monotone"
                dataKey="ctlFitness"
                name="Fitness (CTL 42d)"
                stroke="var(--accent-blue)"
                strokeWidth={2.4}
                dot={false}
              />
              <Line
                type="monotone"
                dataKey="atlFatigue"
                name="Fatigue (ATL 7d)"
                stroke="var(--primary)"
                strokeWidth={2.2}
                dot={false}
              />
            </ComposedChart>
          </ResponsiveContainer>
        </div>
      </div>

      {/* 2. ACWR Risk Gauge + 80/20 Polarized + Pacing Grade */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-5">
        {/* ACWR Injury Risk */}
        <div className="p-5 rounded-2xl bg-card border border-border space-y-4">
          <div className="flex items-center justify-between">
            <h3 className="font-bold text-sm flex items-center gap-2">
              {workloadStatus.zone === "HIGH_RISK" ? (
                <AlertTriangle className="w-4 h-4 text-danger" />
              ) : (
                <ShieldCheck className="w-4 h-4 text-success" />
              )}
              ACWR Injury Risk Ratio
            </h3>
            <span className="font-mono font-bold text-lg text-primary">{workloadStatus.acwr.toFixed(2)}</span>
          </div>

          <div className="space-y-1.5">
            <div className="h-3 w-full rounded-full overflow-hidden flex border border-border">
              <div className="w-[35%] bg-accent-blue/60" title="Undertrained <0.8" />
              <div className="w-[30%] bg-success" title="Sweet Spot 0.8-1.3" />
              <div className="w-[15%] bg-warning" title="Caution 1.3-1.5" />
              <div className="w-[20%] bg-danger" title="High Risk >1.5" />
            </div>
            <div className="flex justify-between text-[10px] font-mono text-text-secondary">
              <span>0.0</span>
              <span>0.8 (Sweet Spot)</span>
              <span>1.3</span>
              <span>1.5+</span>
            </div>
          </div>

          <div className="grid grid-cols-2 gap-2 text-xs font-mono">
            <div className="p-2.5 rounded-xl bg-elevated">
              <div className="text-text-secondary text-[10px]">BEBAN 7 HARI</div>
              <div className="font-bold">{workloadStatus.acute7dLoad.toFixed(1)} rTSS</div>
            </div>
            <div className="p-2.5 rounded-xl bg-elevated">
              <div className="text-text-secondary text-[10px]">RATA-RATA 28 HARI</div>
              <div className="font-bold">{workloadStatus.chronic28dWeeklyAvgLoad.toFixed(1)} /mgg</div>
            </div>
          </div>

          <div className="p-3 rounded-xl bg-elevated border border-border text-xs text-text-secondary leading-relaxed">
            <span className="font-bold text-text-primary block mb-0.5">{workloadStatus.label}</span>
            {workloadStatus.recommendation}
          </div>
        </div>

        {/* 80/20 Polarized Intensity Distribution */}
        <div className="p-5 rounded-2xl bg-card border border-border space-y-3">
          <div className="flex items-center justify-between">
            <h3 className="font-bold text-sm flex items-center gap-2">
              <PieIcon className="w-4 h-4 text-accent-blue" />
              Distribusi Intensitas 80/20
            </h3>
            <span className="font-mono text-xs font-bold px-2 py-0.5 rounded-md bg-elevated border border-border">
              Skor: {polarizedDistribution.alignmentScore}/100
            </span>
          </div>

          <div className="h-44 w-full flex items-center">
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie
                  data={pieData}
                  dataKey="value"
                  nameKey="name"
                  innerRadius={42}
                  outerRadius={68}
                  paddingAngle={3}
                >
                  {pieData.map((entry) => (
                    <Cell key={entry.name} fill={entry.color} />
                  ))}
                </Pie>
                <Tooltip formatter={(val) => `${Number(val).toFixed(1)}%`} />
              </PieChart>
            </ResponsiveContainer>
          </div>

          <div className="grid grid-cols-3 gap-2 text-center text-xs font-mono">
            <div className="p-2 rounded-lg bg-elevated">
              <div className="text-success font-bold">{polarizedDistribution.easyPct}%</div>
              <div className="text-[10px] text-text-secondary">Easy ({polarizedDistribution.easyKm} km)</div>
            </div>
            <div className="p-2 rounded-lg bg-elevated">
              <div className="text-warning font-bold">{polarizedDistribution.moderatePct}%</div>
              <div className="text-[10px] text-text-secondary">Mod ({polarizedDistribution.moderateKm} km)</div>
            </div>
            <div className="p-2 rounded-lg bg-elevated">
              <div className="text-primary font-bold">{polarizedDistribution.hardPct}%</div>
              <div className="text-[10px] text-text-secondary">Hard ({polarizedDistribution.hardKm} km)</div>
            </div>
          </div>

          <p className="text-xs text-text-secondary leading-relaxed">{polarizedDistribution.coachingNote}</p>
        </div>

        {/* Pacing Grade & Race Readiness */}
        <div className="p-5 rounded-2xl bg-card border border-border space-y-4">
          <div className="flex items-center justify-between">
            <h3 className="font-bold text-sm">Konsistensi Split & Readiness</h3>
            <span className="px-2.5 py-1 rounded-lg bg-primary/15 text-primary font-mono font-bold text-sm">
              Grade {pacingConsistency.grade}
            </span>
          </div>

          <div className="grid grid-cols-3 gap-2 text-center text-xs">
            <div className="p-2.5 rounded-xl bg-elevated">
              <div className="font-bold font-mono text-success text-sm">{pacingConsistency.negativeSplitCount}</div>
              <div className="text-[10px] text-text-secondary">Negative</div>
            </div>
            <div className="p-2.5 rounded-xl bg-elevated">
              <div className="font-bold font-mono text-accent-blue text-sm">{pacingConsistency.evenSplitCount}</div>
              <div className="text-[10px] text-text-secondary">Even</div>
            </div>
            <div className="p-2.5 rounded-xl bg-elevated">
              <div className="font-bold font-mono text-danger text-sm">{pacingConsistency.positiveSplitCount}</div>
              <div className="text-[10px] text-text-secondary">Positive</div>
            </div>
          </div>

          <div className="p-3 rounded-xl bg-elevated border border-border space-y-1 text-xs">
            <div className="flex justify-between font-semibold">
              <span>Race Readiness ({raceReadiness.targetDistanceLabel})</span>
              <span className="font-mono text-primary">{raceReadiness.score}/100</span>
            </div>
            <div className="text-text-secondary">{raceReadiness.bottleneck}</div>
            {raceReadiness.actionItems.map((item, i) => (
              <div key={i} className="text-accent-blue font-medium">• {item}</div>
            ))}
          </div>

          <div className="p-3 rounded-xl bg-elevated border border-border text-xs">
            <div className="font-semibold">Cadence & Stride Length</div>
            <div className="text-text-secondary font-mono mt-0.5">
              {cadenceDiagnostics.avgCadenceSpm} spm · {cadenceDiagnostics.estimatedAvgStrideLengthM.toFixed(2)} m/langkah
            </div>
          </div>
        </div>
      </div>

      {/* 3. Aerobic Efficiency Factor & Monthly Trend */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-5">
        <div className="p-5 rounded-2xl bg-card border border-border space-y-3">
          <h3 className="font-bold text-base flex items-center gap-2">
            <HeartPulse className="w-4 h-4 text-primary" />
            Tren Aerobic Efficiency Factor (EF) & VDOT
          </h3>
          <p className="text-xs text-text-secondary">
            EF = kecepatan (m/menit) dibagi detak jantung rata-rata. Nilai EF meningkat menunjukkan jantung lebih efisien di pace yang sama.
          </p>
          <div className="h-60 w-full">
            <ResponsiveContainer width="100%" height="100%">
              <LineChart data={efPoints} margin={{ top: 8, right: 12, bottom: 0, left: -12 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" />
                <XAxis dataKey="dateKey" tickFormatter={(v) => String(v).slice(5)} tick={{ fontSize: 11, fill: "var(--text-secondary)" }} />
                <YAxis yAxisId="left" tick={{ fontSize: 11, fill: "var(--text-secondary)" }} />
                <YAxis yAxisId="right" orientation="right" tick={{ fontSize: 11, fill: "var(--text-secondary)" }} />
                <Tooltip
                  contentStyle={{
                    backgroundColor: "var(--card)",
                    borderColor: "var(--border)",
                    borderRadius: "0.75rem",
                    fontSize: "12px",
                  }}
                />
                <Line
                  yAxisId="left"
                  type="monotone"
                  dataKey="efficiencyFactor"
                  name="Efficiency Factor"
                  stroke="var(--primary)"
                  strokeWidth={2.5}
                  dot={{ r: 3 }}
                />
                <Line
                  yAxisId="right"
                  type="monotone"
                  dataKey="vdot"
                  name="Session VDOT"
                  stroke="var(--accent-blue)"
                  strokeWidth={2}
                  dot={{ r: 2.5 }}
                />
              </LineChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* 6-Month Volume & Pace Trend */}
        <div className="p-5 rounded-2xl bg-card border border-border space-y-3">
          <h3 className="font-bold text-base">Tren Volume & Pace 6 Bulan Terakhir</h3>
          <p className="text-xs text-text-secondary">
            Perbandingan total kilometer bulanan dengan rata-rata pace bulanan.
          </p>
          <div className="h-60 w-full">
            <ResponsiveContainer width="100%" height="100%">
              <ComposedChart data={monthlyTrends} margin={{ top: 8, right: 12, bottom: 0, left: -12 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" />
                <XAxis dataKey="monthLabel" tick={{ fontSize: 11, fill: "var(--text-secondary)" }} />
                <YAxis yAxisId="km" tick={{ fontSize: 11, fill: "var(--text-secondary)" }} />
                <YAxis
                  yAxisId="pace"
                  orientation="right"
                  reversed
                  tickFormatter={(v) => formatPaceShort(Number(v))}
                  tick={{ fontSize: 11, fill: "var(--text-secondary)" }}
                />
                <Tooltip
                  contentStyle={{
                    backgroundColor: "var(--card)",
                    borderColor: "var(--border)",
                    borderRadius: "0.75rem",
                    fontSize: "12px",
                  }}
                  formatter={(value, name) =>
                    name === "Avg Pace" ? [formatPace(Number(value)), name] : [`${Number(value).toFixed(1)} km`, name]
                  }
                />
                <Bar yAxisId="km" dataKey="totalDistanceKm" name="Total km" fill="var(--accent-blue)" radius={[6, 6, 0, 0]} />
                <Line
                  yAxisId="pace"
                  type="monotone"
                  dataKey="avgPaceSec"
                  name="Avg Pace"
                  stroke="var(--primary)"
                  strokeWidth={2.5}
                />
              </ComposedChart>
            </ResponsiveContainer>
          </div>
        </div>
      </div>

      {/* 4. 16-Week Activity Heatmap */}
      <div className="p-5 rounded-2xl bg-card border border-border space-y-3">
        <div className="flex items-center justify-between">
          <div>
            <h3 className="font-bold text-base flex items-center gap-2">
              <Flame className="w-4 h-4 text-primary" />
              Heatmap Konsistensi 16 Minggu (112 Hari)
            </h3>
            <p className="text-xs text-text-secondary">
              Intensitas warna menunjukkan volume jarak harian Anda dalam 16 minggu terakhir.
            </p>
          </div>
          <div className="flex items-center gap-1.5 text-[10px] text-text-secondary">
            <span>Santai</span>
            <span className="w-3 h-3 rounded bg-elevated border border-border" />
            <span className="w-3 h-3 rounded bg-primary/25" />
            <span className="w-3 h-3 rounded bg-primary/50" />
            <span className="w-3 h-3 rounded bg-primary/75" />
            <span className="w-3 h-3 rounded bg-primary" />
            <span>Tinggi</span>
          </div>
        </div>

        <div className="overflow-x-auto pb-1">
          <div className="inline-flex gap-1.5">
            {heatmapWeeks.map((week, wIdx) => (
              <div key={wIdx} className="grid grid-rows-7 gap-1.5">
                {week.map((day) => {
                  const bg =
                    day.intensityLevel === 0
                      ? "bg-elevated border-border"
                      : day.intensityLevel === 1
                      ? "bg-primary/25 border-primary/30"
                      : day.intensityLevel === 2
                      ? "bg-primary/50 border-primary/50"
                      : day.intensityLevel === 3
                      ? "bg-primary/75 border-primary/80"
                      : "bg-primary border-primary";
                  return (
                    <div
                      key={day.dateKey}
                      title={`${day.dateKey}: ${day.distanceKm.toFixed(1)} km (${day.runsCount} sesi)`}
                      className={`w-4 h-4 rounded border ${bg}`}
                    />
                  );
                })}
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}
