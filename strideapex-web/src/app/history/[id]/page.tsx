"use client";

import React, { useState } from "react";
import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { ArrowLeft, Edit3, RefreshCw, Sparkles, Trash2 } from "lucide-react";
import { SplitsVisualChart } from "@/components/SplitsVisualChart";
import { WorkoutEditModal } from "@/components/WorkoutEditModal";
import { formatDateTimeId, formatDuration, formatPace } from "@/lib/formatters";
import { useStrideApex } from "@/lib/strideapex-context";

export default function WorkoutDetailPage() {
  const params = useParams<{ id: string }>();
  const router = useRouter();
  const { workouts, analytics, updateWorkout, deleteWorkout, refreshWorkoutAi } = useStrideApex();

  const decodedId = decodeURIComponent(params?.id ?? "");
  const workout = workouts.find((w) => w.id === decodedId);
  const deep = workout ? analytics.workoutDeepMetrics[workout.id] : undefined;

  const [showEdit, setShowEdit] = useState(false);
  const [refreshing, setRefreshing] = useState(false);

  if (!workout) {
    return (
      <div className="p-8 rounded-2xl bg-card border border-border text-center space-y-3">
        <div className="font-bold text-base">Sesi lari tidak ditemukan</div>
        <Link href="/history" className="inline-flex items-center gap-1.5 text-sm font-semibold text-primary">
          <ArrowLeft className="w-4 h-4" /> Kembali ke Riwayat
        </Link>
      </div>
    );
  }

  const handleRefreshAi = async () => {
    setRefreshing(true);
    try {
      await refreshWorkoutAi(workout);
    } finally {
      setRefreshing(false);
    }
  };

  const handleDelete = async () => {
    await deleteWorkout(workout.id);
    router.push("/history");
  };

  return (
    <div className="space-y-6">
      {/* Header Actions */}
      <div className="flex flex-wrap items-center justify-between gap-3">
        <Link
          href="/history"
          className="inline-flex items-center gap-1.5 px-3 py-2 rounded-xl bg-card border border-border text-xs font-semibold hover:border-primary/50"
        >
          <ArrowLeft className="w-4 h-4" /> Riwayat Lari
        </Link>

        <div className="flex items-center gap-2">
          <button
            type="button"
            disabled={refreshing}
            onClick={() => void handleRefreshAi()}
            className="inline-flex items-center gap-1.5 px-3 py-2 rounded-xl bg-elevated border border-border text-xs font-semibold text-accent-blue hover:border-accent-blue/60 disabled:opacity-60"
          >
            <RefreshCw className={`w-3.5 h-3.5 ${refreshing ? "animate-spin" : ""}`} />
            <span>{refreshing ? "Menyegarkan..." : "Refresh Split & AI"}</span>
          </button>

          <button
            type="button"
            onClick={() => setShowEdit(true)}
            className="inline-flex items-center gap-1.5 px-3 py-2 rounded-xl bg-primary text-white text-xs font-semibold hover:bg-primary-dim"
          >
            <Edit3 className="w-3.5 h-3.5" /> Edit Sesi
          </button>

          <button
            type="button"
            onClick={() => void handleDelete()}
            className="inline-flex items-center gap-1.5 px-3 py-2 rounded-xl border border-border bg-card text-xs font-semibold text-danger hover:border-danger/60"
          >
            <Trash2 className="w-3.5 h-3.5" /> Hapus
          </button>
        </div>
      </div>

      {/* Main Summary Card */}
      <div className="p-6 rounded-2xl bg-card border border-border space-y-5">
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div>
            <div className="flex items-center gap-2">
              <span className="px-2 py-0.5 rounded bg-primary/15 text-primary text-[11px] font-mono font-bold">
                {workout.sourceType}
              </span>
              {deep && (
                <span className="px-2 py-0.5 rounded bg-accent-blue/15 text-accent-blue text-[11px] font-mono font-bold">
                  {deep.zoneName}
                </span>
              )}
            </div>
            <h2 className="text-2xl font-bold mt-2">{workout.title}</h2>
            <p className="text-xs text-text-secondary mt-0.5">{formatDateTimeId(workout.createdAtEpochMs)}</p>
          </div>
        </div>

        <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
          <div className="p-3.5 rounded-xl bg-elevated border border-border">
            <div className="text-[11px] text-text-secondary font-semibold">JARAK</div>
            <div className="text-xl font-bold font-mono tabular-nums mt-0.5">{workout.distanceKm.toFixed(2)} km</div>
          </div>
          <div className="p-3.5 rounded-xl bg-elevated border border-border">
            <div className="text-[11px] text-text-secondary font-semibold">DURASI</div>
            <div className="text-xl font-bold font-mono tabular-nums mt-0.5">{formatDuration(workout.durationSeconds)}</div>
          </div>
          <div className="p-3.5 rounded-xl bg-elevated border border-border">
            <div className="text-[11px] text-text-secondary font-semibold">PACE RATA-RATA</div>
            <div className="text-xl font-bold font-mono tabular-nums text-primary mt-0.5">
              {formatPace(workout.avgPaceSecondsPerKm)}
            </div>
          </div>
          <div className="p-3.5 rounded-xl bg-elevated border border-border">
            <div className="text-[11px] text-text-secondary font-semibold">HEART RATE</div>
            <div className="text-xl font-bold font-mono tabular-nums mt-0.5">
              {workout.avgHeartRate > 0 ? `${workout.avgHeartRate} bpm` : "--"}
            </div>
          </div>
        </div>

        <div className="grid grid-cols-2 sm:grid-cols-5 gap-3 text-xs font-mono">
          <div className="p-3 rounded-xl bg-elevated">
            <div className="text-text-secondary text-[10px]">CADENCE</div>
            <div className="font-bold mt-0.5">{workout.cadenceSpm > 0 ? `${workout.cadenceSpm} spm` : "--"}</div>
          </div>
          <div className="p-3 rounded-xl bg-elevated">
            <div className="text-text-secondary text-[10px]">ELEVASI</div>
            <div className="font-bold mt-0.5">+{workout.elevationGainM} m</div>
          </div>
          <div className="p-3 rounded-xl bg-elevated">
            <div className="text-text-secondary text-[10px]">SESSION VDOT</div>
            <div className="font-bold text-primary mt-0.5">{deep?.vdot ? deep.vdot.toFixed(1) : "--"}</div>
          </div>
          <div className="p-3 rounded-xl bg-elevated">
            <div className="text-text-secondary text-[10px]">BEBAN (rTSS)</div>
            <div className="font-bold text-accent-blue mt-0.5">{deep?.estimatedRtss ? `${deep.estimatedRtss.toFixed(1)}` : "--"}</div>
          </div>
          <div className="p-3 rounded-xl bg-elevated">
            <div className="text-text-secondary text-[10px]">EFFICIENCY / STRIDE</div>
            <div className="font-bold mt-0.5">
              {deep?.aerobicEfficiency ? `EF ${deep.aerobicEfficiency}` : "--"} ·{" "}
              {deep?.strideLengthM ? `${deep.strideLengthM}m` : "--"}
            </div>
          </div>
        </div>
      </div>

      {/* Splits & Curve */}
      <div className="p-6 rounded-2xl bg-card border border-border">
        <SplitsVisualChart
          splitsSummary={workout.splitsSummary}
          avgPaceSecondsPerKm={workout.avgPaceSecondsPerKm}
          deepMetrics={deep}
        />
      </div>

      {/* AI Coaching Insight */}
      <div className="p-6 rounded-2xl bg-card border border-border space-y-3">
        <div className="flex items-center gap-2 text-primary font-bold text-base">
          <Sparkles className="w-4 h-4" />
          <span>Analisis & Evaluasi AI Coach</span>
        </div>
        <p className="text-sm text-text-primary whitespace-pre-line leading-relaxed">
          {workout.aiCoachingInsight || "Belum ada analisis. Tekan Refresh Split & AI untuk menghasilkan evaluasi mendalam."}
        </p>
      </div>

      {showEdit && (
        <WorkoutEditModal workout={workout} onClose={() => setShowEdit(false)} onSave={updateWorkout} />
      )}
    </div>
  );
}
