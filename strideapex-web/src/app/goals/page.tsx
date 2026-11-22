"use client";

import React, { useState } from "react";
import Link from "next/link";
import { Calendar, CheckCircle2, Edit3, Plus, Sparkles, Target, Trash2 } from "lucide-react";
import { GoalFormModal } from "@/components/GoalFormModal";
import { formatDuration, formatPace } from "@/lib/formatters";
import { useStrideApex } from "@/lib/strideapex-context";
import type { GoalScheduleWithSummary, ScheduledSession } from "@/lib/types";

export default function GoalsPage() {
  const {
    analytics,
    saveGoalSchedule,
    toggleGoalCompleted,
    deleteGoal,
    getPlanAdvice,
  } = useStrideApex();

  const { goalSchedules, raceReadiness } = analytics;
  const [selectedGoalId, setSelectedGoalId] = useState<string | null>(null);
  const [selectedSession, setSelectedSession] = useState<ScheduledSession | null>(null);
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [editingGoal, setEditingGoal] = useState<GoalScheduleWithSummary | null>(null);
  const [adviceText, setAdviceText] = useState<string | null>(null);
  const [adviceEngine, setAdviceEngine] = useState<string | null>(null);
  const [adviceLoading, setAdviceLoading] = useState(false);

  const activeSummary =
    goalSchedules.find((g) => g.goalId === selectedGoalId) ??
    goalSchedules.find((g) => !g.goal.completed) ??
    goalSchedules[0] ??
    null;

  const displayedSession =
    selectedSession ??
    activeSummary?.sessions.find((s) => s.status === "TODAY_PENDING") ??
    activeSummary?.sessions.find((s) => s.status === "UPCOMING") ??
    activeSummary?.sessions[0] ??
    null;

  const handleAskAdvice = async () => {
    if (!activeSummary) return;
    setAdviceLoading(true);
    try {
      const res = await getPlanAdvice(activeSummary.goal);
      setAdviceText(res.advice);
      setAdviceEngine(res.sourceEngine);
    } finally {
      setAdviceLoading(false);
    }
  };

  return (
    <div className="space-y-6">
      {/* Header & Single Add Target Button */}
      <div className="p-5 rounded-2xl bg-card border border-border flex flex-wrap items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-2 text-primary text-xs font-bold uppercase tracking-wider">
            <Target className="w-4 h-4" /> TARGET LARI & KALENDER LATIHAN ADAPTIF
          </div>
          <h2 className="text-xl font-bold mt-1">
            Kesiapan Lomba Saat Ini: {raceReadiness.score}/100 ({raceReadiness.grade})
          </h2>
          <p className="text-xs text-text-secondary mt-0.5">{raceReadiness.bottleneck}</p>
        </div>

        <button
          type="button"
          onClick={() => setShowCreateModal(true)}
          className="inline-flex items-center gap-1.5 px-4 py-2.5 rounded-xl bg-primary text-white text-xs font-bold hover:bg-primary-dim transition-colors"
        >
          <Plus className="w-4 h-4" />
          <span>Target</span>
        </button>
      </div>

      {/* Goals Cards */}
      {goalSchedules.length === 0 ? (
        <div className="p-8 rounded-2xl bg-card border border-border text-center space-y-3">
          <div className="text-sm font-semibold">Belum ada target lari.</div>
          <button
            type="button"
            onClick={() => setShowCreateModal(true)}
            className="inline-flex items-center gap-1.5 px-4 py-2 rounded-xl bg-primary text-white text-xs font-bold"
          >
            <Plus className="w-4 h-4" /> Target Pertama
          </button>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {goalSchedules.map((item) => {
            const selected = activeSummary?.goalId === item.goalId;
            return (
              <div
                key={item.goalId}
                onClick={() => {
                  setSelectedGoalId(item.goalId);
                  setSelectedSession(null);
                  setAdviceText(null);
                }}
                className={`p-4 rounded-2xl bg-card border cursor-pointer transition-colors space-y-3 ${
                  selected ? "border-primary ring-1 ring-primary/30" : "border-border hover:border-primary/40"
                }`}
              >
                <div className="flex items-start justify-between gap-2">
                  <div className="flex items-center gap-2 min-w-0">
                    <button
                      type="button"
                      onClick={(e) => {
                        e.stopPropagation();
                        void toggleGoalCompleted(item.goal);
                      }}
                      className={`p-1 rounded-full border ${
                        item.goal.completed
                          ? "bg-success text-white border-success"
                          : "border-border text-text-secondary hover:border-primary"
                      }`}
                    >
                      <CheckCircle2 className="w-4 h-4" />
                    </button>
                    <div className="min-w-0">
                      <span className="px-2 py-0.5 rounded bg-elevated text-[10px] font-mono font-bold text-accent-blue">
                        {item.goal.targetDistanceCategory.replace("_", " ")}
                      </span>
                      <div className="font-bold text-sm truncate mt-1">{item.metadata.cleanTitle}</div>
                    </div>
                  </div>

                  <div className="flex items-center gap-1 shrink-0">
                    <button
                      type="button"
                      onClick={(e) => {
                        e.stopPropagation();
                        setEditingGoal(item);
                      }}
                      className="p-1.5 rounded-lg hover:bg-elevated text-text-secondary hover:text-primary"
                    >
                      <Edit3 className="w-3.5 h-3.5" />
                    </button>
                    <button
                      type="button"
                      onClick={(e) => {
                        e.stopPropagation();
                        void deleteGoal(item.goalId);
                      }}
                      className="p-1.5 rounded-lg hover:bg-elevated text-text-secondary hover:text-danger"
                    >
                      <Trash2 className="w-3.5 h-3.5" />
                    </button>
                  </div>
                </div>

                <div className="grid grid-cols-3 gap-2 text-xs font-mono">
                  <div className="p-2 rounded-lg bg-elevated">
                    <div className="text-[10px] text-text-secondary">TARGET</div>
                    <div className="font-bold">{formatDuration(item.goal.targetTimeSeconds)}</div>
                  </div>
                  <div className="p-2 rounded-lg bg-elevated">
                    <div className="text-[10px] text-text-secondary">MINGGUAN</div>
                    <div className="font-bold">{item.goal.weeklyMileageTargetKm} km</div>
                  </div>
                  <div className="p-2 rounded-lg bg-elevated">
                    <div className="text-[10px] text-text-secondary">SISA</div>
                    <div className="font-bold text-primary">H-{Math.max(0, item.metadata.daysRemaining)}</div>
                  </div>
                </div>

                <div className="space-y-1">
                  <div className="flex justify-between text-[11px] text-text-secondary">
                    <span>Kepatuhan Jadwal ({item.completedSessions}/{item.sessions.length})</span>
                    <span className="font-mono font-bold">{item.compliancePct}%</span>
                  </div>
                  <div className="h-2 rounded-full bg-elevated overflow-hidden">
                    <div
                      className="h-full rounded-full bg-primary"
                      style={{ width: `${Math.min(100, item.compliancePct)}%` }}
                    />
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Selected Goal Schedule & AI Advice */}
      {activeSummary && (
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-5">
          {/* Session List */}
          <div className="lg:col-span-7 p-5 rounded-2xl bg-card border border-border space-y-4">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <Calendar className="w-4 h-4 text-primary" />
                <h3 className="font-bold text-base">
                  Jadwal Adaptif: {activeSummary.metadata.cleanTitle}
                </h3>
              </div>
              <span className="text-xs font-mono text-text-secondary">
                {activeSummary.metadata.startDateDisplay} – {activeSummary.metadata.targetDateDisplay}
              </span>
            </div>

            <div className="space-y-2 max-h-[420px] overflow-y-auto pr-1">
              {activeSummary.sessions.map((s) => {
                const isSelected = displayedSession?.dateKey === s.dateKey;
                const statusBadge =
                  s.status === "COMPLETED"
                    ? "bg-success/15 text-success"
                    : s.status === "TODAY_PENDING"
                    ? "bg-primary text-white"
                    : s.status === "SKIPPED"
                    ? "bg-danger/15 text-danger"
                    : "bg-elevated text-text-secondary";
                return (
                  <button
                    key={s.dateKey}
                    type="button"
                    onClick={() => setSelectedSession(s)}
                    className={`w-full text-left p-3 rounded-xl border transition-colors flex items-center justify-between gap-3 ${
                      isSelected ? "bg-elevated border-primary" : "bg-elevated/60 border-border hover:border-primary/40"
                    }`}
                  >
                    <div className="min-w-0">
                      <div className="flex items-center gap-2">
                        <span className="px-2 py-0.5 rounded bg-card border border-border text-[10px] font-mono font-bold text-accent-blue">
                          {s.shortBadge}
                        </span>
                        <span className="font-bold text-xs sm:text-sm truncate">{s.sessionTitle}</span>
                      </div>
                      <div className="text-[11px] text-text-secondary mt-0.5">{s.displayDate}</div>
                    </div>

                    <span className={`px-2.5 py-1 rounded-lg text-[10px] font-bold shrink-0 ${statusBadge}`}>
                      {s.status.replace("_", " ")}
                    </span>
                  </button>
                );
              })}
            </div>
          </div>

          {/* Detail Inspector + Gemini Advice */}
          <div className="lg:col-span-5 space-y-4">
            {displayedSession && (
              <div className="p-5 rounded-2xl bg-card border border-border space-y-3">
                <div className="flex items-center justify-between">
                  <span className="px-2.5 py-1 rounded-lg bg-primary/15 text-primary text-xs font-bold font-mono">
                    {displayedSession.shortBadge} · {displayedSession.zoneLabel}
                  </span>
                  <span className="text-xs font-mono text-text-secondary">{displayedSession.displayDate}</span>
                </div>
                <h4 className="font-bold text-base">{displayedSession.sessionTitle}</h4>
                <p className="text-xs text-text-primary leading-relaxed">{displayedSession.sessionDescription}</p>
                <div className="grid grid-cols-2 gap-2 text-xs font-mono">
                  <div className="p-2.5 rounded-xl bg-elevated">
                    <div className="text-text-secondary text-[10px]">REKOMENDASI JARAK</div>
                    <div className="font-bold">{displayedSession.recommendedDistanceKm.toFixed(1)} km</div>
                  </div>
                  <div className="p-2.5 rounded-xl bg-elevated">
                    <div className="text-text-secondary text-[10px]">TARGET PACE</div>
                    <div className="font-bold text-primary">{formatPace(displayedSession.targetPaceSecondsPerKm)}</div>
                  </div>
                </div>
                <div className="p-3 rounded-xl bg-elevated border border-border text-xs text-text-secondary">
                  <span className="font-bold text-accent-blue block mb-1">Alasan AI Adaptive:</span>
                  {displayedSession.aiReason}
                </div>
                {displayedSession.matchedWorkoutId && (
                  <Link
                    href={`/history/${encodeURIComponent(displayedSession.matchedWorkoutId)}`}
                    className="block text-center py-2 rounded-xl bg-success/15 text-success text-xs font-bold"
                  >
                    Terkait Sesi Selesai: {displayedSession.matchedWorkoutTitle} →
                  </Link>
                )}
              </div>
            )}

            <div className="p-5 rounded-2xl bg-card border border-border space-y-3">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2 font-bold text-sm">
                  <Sparkles className="w-4 h-4 text-primary" /> Periodisasi AI Coach
                </div>
                {adviceEngine && (
                  <span className="text-[10px] font-mono text-accent-blue">{adviceEngine}</span>
                )}
              </div>

              <button
                type="button"
                disabled={adviceLoading}
                onClick={() => void handleAskAdvice()}
                className="w-full py-2.5 rounded-xl bg-primary text-white text-xs font-bold hover:bg-primary-dim disabled:opacity-60"
              >
                {adviceLoading ? "Menyusun Rekomendasi..." : "Minta Saran Periodisasi AI"}
              </button>

              {adviceText && (
                <p className="text-xs text-text-primary whitespace-pre-line leading-relaxed bg-elevated p-3.5 rounded-xl border border-border">
                  {adviceText}
                </p>
              )}
            </div>
          </div>
        </div>
      )}

      {showCreateModal && (
        <GoalFormModal onClose={() => setShowCreateModal(false)} onSave={saveGoalSchedule} />
      )}
      {editingGoal && (
        <GoalFormModal
          existing={editingGoal}
          onClose={() => setEditingGoal(null)}
          onSave={saveGoalSchedule}
        />
      )}
    </div>
  );
}
