"use client";

import React from "react";
import { Award, Flame, Gauge, Trash2 } from "lucide-react";
import { AdBanner } from "@/components/AdBanner";
import { formatDuration, formatPace } from "@/lib/formatters";
import { useStrideApex } from "@/lib/strideapex-context";

export default function PredictorAndPrPage() {
  const { analytics, workouts, prs, fatigueExponent, setFatigueExponent, deletePr } = useStrideApex();
  const { racePredictions, paceZones, overview } = analytics;
  const hasWorkouts = workouts.length > 0;

  return (
    <div className="space-y-6">
      {/* VDOT Hero + Riegel Fatigue Slider */}
      <div className="p-5 rounded-2xl bg-card border border-border space-y-4">
        <div className="flex flex-wrap items-center justify-between gap-4">
          <div>
            <div className="text-xs font-bold uppercase tracking-wider text-primary">
              JACK DANIELS VDOT & RIEGEL ENDURANCE MODEL
            </div>
            <h2 className="text-xl font-bold mt-0.5">Prediksi Waktu Lomba & Kalibrasi Zona Latihan</h2>
          </div>
          <div className="px-4 py-2 rounded-xl bg-primary/15 border border-primary/40 text-right">
            <div className="text-[10px] font-bold text-text-secondary">ESTIMASI VDOT</div>
            <div className="text-2xl font-bold font-mono text-primary">
              {hasWorkouts && overview.currentVdot > 0 ? overview.currentVdot.toFixed(1) : "--"}
            </div>
          </div>
        </div>

        {!hasWorkouts || racePredictions.length === 0 ? (
          <div className="p-6 rounded-xl bg-elevated border border-border text-center text-xs text-text-secondary">
            Belum ada catatan lari atau Personal Record. Unggah sesi lari pertama Anda terlebih dahulu untuk membuka prediksi waktu lomba dan perhitungan VDOT.
          </div>
        ) : (
          <>
            <div className="p-4 rounded-xl bg-elevated border border-border space-y-2">
              <div className="flex items-center justify-between text-xs">
                <span className="font-semibold">Riegel Fatigue Exponent (Daya Tahan Jarak Jauh)</span>
                <span className="font-mono font-bold text-primary">{fatigueExponent.toFixed(2)}</span>
              </div>
              <input
                type="range"
                min={1.03}
                max={1.1}
                step={0.01}
                value={fatigueExponent}
                onChange={(e) => setFatigueExponent(parseFloat(e.target.value))}
                className="w-full accent-[var(--primary)]"
              />
              <div className="flex justify-between text-[11px] text-text-secondary">
                <span>1.03 (Endurance Tinggi)</span>
                <span>1.06 (Standar)</span>
                <span>1.10 (Konservatif)</span>
              </div>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3">
              {racePredictions.map((pred) => (
                <div key={pred.categoryKey} className="p-4 rounded-xl bg-elevated border border-border space-y-1.5">
                  <div className="flex items-center justify-between">
                    <span className="font-bold text-sm">{pred.displayName}</span>
                    <span className="px-2 py-0.5 rounded bg-card border border-border text-[10px] font-mono">
                      {pred.shortLabel}
                    </span>
                  </div>
                  <div className="text-2xl font-bold font-mono tabular-nums text-primary">
                    {pred.predictedTimeFormatted}
                  </div>
                  <div className="text-xs font-mono text-accent-blue font-semibold">
                    Target Pace: {pred.targetPaceFormatted}
                  </div>
                  <p className="text-[11px] text-text-secondary leading-snug">{pred.confidenceNote}</p>
                </div>
              ))}
            </div>
          </>
        )}
      </div>

      {/* 5 Training Pace Zones */}
      <div className="p-5 rounded-2xl bg-card border border-border space-y-4">
        <div className="flex items-center gap-2">
          <Gauge className="w-4 h-4 text-accent-blue" />
          <h3 className="font-bold text-base">5 Zona Pace Latihan Ilmiah (Anchored to T-Pace)</h3>
        </div>

        {!hasWorkouts || paceZones.length === 0 ? (
          <div className="p-6 rounded-xl bg-elevated border border-border text-center text-xs text-text-secondary">
            Zona latihan VDOT akan muncul otomatis setelah Anda mencatat minimal satu sesi lari atau PR.
          </div>
        ) : (
          <div className="space-y-2.5">
            {paceZones.map((zone) => (
              <div
                key={zone.code}
                className="p-4 rounded-xl bg-elevated border border-border flex flex-col sm:flex-row sm:items-center justify-between gap-3"
              >
                <div className="space-y-1">
                  <div className="flex items-center gap-2">
                    <span className="px-2 py-0.5 rounded bg-primary/15 text-primary font-mono text-xs font-bold">
                      {zone.code}
                    </span>
                    <span className="font-bold text-sm">{zone.zoneName}</span>
                  </div>
                  <p className="text-xs text-text-secondary">{zone.physiologicalBenefit}</p>
                </div>
                <div className="sm:text-right shrink-0 font-mono">
                  <div className="text-sm font-bold text-accent-blue">{zone.paceRangePerKm}</div>
                  <div className="text-[11px] text-text-secondary">{zone.heartRatePctRange}</div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Personal Records Vault */}
      <div className="p-5 rounded-2xl bg-card border border-border space-y-4">
        <div className="flex items-center gap-2">
          <Award className="w-4 h-4 text-primary" />
          <h3 className="font-bold text-base">Personal Records (PR Vault)</h3>
        </div>

        {prs.length === 0 ? (
          <div className="p-6 rounded-xl bg-elevated text-center text-xs text-text-secondary">
            Belum ada Personal Record. Catat lari jarak ≥ 4.25 km (5K), ≥ 8.5 km (10K), ≥ 17.9 km (HM), atau ≥ 35.8 km (FM).
          </div>
        ) : (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3">
            {prs.map((pr) => (
              <div key={pr.id} className="p-4 rounded-xl bg-elevated border border-border space-y-2">
                <div className="flex items-center justify-between">
                  <span className="px-2 py-0.5 rounded bg-primary text-white text-xs font-mono font-bold">
                    {pr.distanceCategory.replace("_", " ")}
                  </span>
                  <button
                    type="button"
                    onClick={() => void deletePr(pr.distanceCategory)}
                    className="p-1 text-text-secondary hover:text-danger"
                    title="Hapus PR"
                  >
                    <Trash2 className="w-3.5 h-3.5" />
                  </button>
                </div>
                <div className="text-2xl font-bold font-mono tabular-nums">{formatDuration(pr.bestTimeSeconds)}</div>
                <div className="text-xs font-mono text-accent-blue">{formatPace(pr.paceSecondsPerKm)}</div>
                <div className="text-xs text-text-secondary truncate flex items-center gap-1">
                  <Flame className="w-3 h-3 text-primary shrink-0" /> {pr.workoutTitle}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      <AdBanner slotKey="footer" />
    </div>
  );
}
