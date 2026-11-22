"use client";

import React, { useState } from "react";
import { X } from "lucide-react";
import { DISTANCE_CATEGORIES, dayShortNameId, toDateInputValue } from "@/lib/formatters";
import type { DistanceCategoryKey, GoalScheduleWithSummary } from "@/lib/types";

interface GoalFormModalProps {
  existing?: GoalScheduleWithSummary | null;
  onClose: () => void;
  onSave: (params: {
    goalId?: string | null;
    cleanTitle: string;
    category: DistanceCategoryKey;
    targetTimeSeconds: number;
    weeklyMileageTargetKm: number;
    startDateKey: string;
    targetDateKey: string;
    trainingDaysIso: number[];
  }) => Promise<void>;
}

export function GoalFormModal({ existing, onClose, onSave }: GoalFormModalProps) {
  const todayKey = toDateInputValue(Date.now());
  const defaultEndKey = toDateInputValue(Date.now() + 21 * 86_400_000);

  const [cleanTitle, setCleanTitle] = useState(existing?.metadata.cleanTitle ?? "");
  const [category, setCategory] = useState<DistanceCategoryKey>(
    existing?.goal.targetDistanceCategory ?? "10K"
  );
  const [targetMins, setTargetMins] = useState(
    String(Math.floor((existing?.goal.targetTimeSeconds ?? 3000) / 60))
  );
  const [weeklyKm, setWeeklyKm] = useState(String(existing?.goal.weeklyMileageTargetKm ?? 30));
  const [startDateKey, setStartDateKey] = useState(existing?.metadata.startDateKey ?? todayKey);
  const [targetDateKey, setTargetDateKey] = useState(existing?.metadata.targetDateKey ?? defaultEndKey);
  const [daysIso, setDaysIso] = useState<number[]>(existing?.metadata.trainingDaysIso ?? [2, 4, 7]);
  const [saving, setSaving] = useState(false);

  const toggleDay = (d: number) => {
    setDaysIso((prev) => {
      if (prev.includes(d)) {
        return prev.length > 1 ? prev.filter((x) => x !== d) : prev;
      }
      return [...prev, d].sort((a, b) => a - b);
    });
  };

  const handleCategoryChange = (catKey: DistanceCategoryKey) => {
    setCategory(catKey);
    if (!existing) {
      if (catKey === "5K") {
        setTargetMins("24");
        setWeeklyKm("20");
      } else if (catKey === "10K") {
        setTargetMins("50");
        setWeeklyKm("30");
      } else if (catKey === "HALF_MARATHON") {
        setTargetMins("115");
        setWeeklyKm("45");
      } else {
        setTargetMins("245");
        setWeeklyKm("65");
      }
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setSaving(true);
    try {
      const mins = Math.max(10, parseInt(targetMins, 10) || 50);
      const wk = Math.max(5, Math.min(400, parseInt(weeklyKm, 10) || 30));
      await onSave({
        goalId: existing?.goalId ?? null,
        cleanTitle: cleanTitle.trim(),
        category,
        targetTimeSeconds: mins * 60,
        weeklyMileageTargetKm: wk,
        startDateKey,
        targetDateKey,
        trainingDaysIso: daysIso,
      });
      onClose();
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 bg-black/50 backdrop-blur-sm flex items-center justify-center p-4">
      <div className="bg-card border border-border rounded-2xl w-full max-w-lg max-h-[90vh] overflow-y-auto p-5 shadow-xl">
        <div className="flex items-center justify-between pb-3 border-b border-border">
          <h3 className="font-bold text-base">
            {existing ? "Edit Target & Jadwal Latihan" : "Buat Target Lari Baru"}
          </h3>
          <button type="button" onClick={onClose} className="p-1.5 rounded-lg hover:bg-elevated">
            <X className="w-4 h-4" />
          </button>
        </div>

        <form onSubmit={(e) => void handleSubmit(e)} className="mt-4 space-y-4 text-sm">
          <div>
            <label className="block text-xs font-semibold text-text-secondary mb-1">Nama Target</label>
            <input
              type="text"
              value={cleanTitle}
              onChange={(e) => setCleanTitle(e.target.value)}
              placeholder="Contoh: Target Lari 10K"
              className="w-full px-3 py-2 rounded-xl bg-elevated border border-border text-text-primary focus:outline-none focus:border-primary"
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-text-secondary mb-1.5">Kategori Jarak</label>
            <div className="grid grid-cols-4 gap-2">
              {DISTANCE_CATEGORIES.map((c) => (
                <button
                  key={c.key}
                  type="button"
                  onClick={() => handleCategoryChange(c.key)}
                  className={`py-2 rounded-xl text-xs font-bold border transition-colors ${
                    category === c.key
                      ? "bg-primary text-white border-primary"
                      : "bg-elevated text-text-primary border-border hover:border-primary/50"
                  }`}
                >
                  {c.shortLabel}
                </button>
              ))}
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-text-secondary mb-1">
                Target Waktu (Menit)
              </label>
              <input
                type="number"
                min={10}
                max={600}
                value={targetMins}
                onChange={(e) => setTargetMins(e.target.value)}
                className="w-full px-3 py-2 rounded-xl bg-elevated border border-border text-text-primary font-mono focus:outline-none focus:border-primary"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-text-secondary mb-1">
                Target Jarak Mingguan (km)
              </label>
              <input
                type="number"
                min={5}
                max={300}
                value={weeklyKm}
                onChange={(e) => setWeeklyKm(e.target.value)}
                className="w-full px-3 py-2 rounded-xl bg-elevated border border-border text-text-primary font-mono focus:outline-none focus:border-primary"
              />
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-text-secondary mb-1">Tanggal Mulai</label>
              <input
                type="date"
                value={startDateKey}
                onChange={(e) => setStartDateKey(e.target.value)}
                className="w-full px-3 py-2 rounded-xl bg-elevated border border-border text-text-primary focus:outline-none focus:border-primary"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-text-secondary mb-1">Hari Perlombaan (Target)</label>
              <input
                type="date"
                value={targetDateKey}
                onChange={(e) => setTargetDateKey(e.target.value)}
                className="w-full px-3 py-2 rounded-xl bg-elevated border border-border text-text-primary focus:outline-none focus:border-primary"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-text-secondary mb-1.5">
              Hari Latihan per Minggu
            </label>
            <div className="grid grid-cols-7 gap-1.5">
              {[1, 2, 3, 4, 5, 6, 7].map((d) => {
                const selected = daysIso.includes(d);
                return (
                  <button
                    key={d}
                    type="button"
                    onClick={() => toggleDay(d)}
                    className={`py-2 rounded-lg text-xs font-semibold border transition-colors ${
                      selected
                        ? "bg-primary/15 text-primary border-primary"
                        : "bg-elevated text-text-secondary border-border"
                    }`}
                  >
                    {dayShortNameId(d)}
                  </button>
                );
              })}
            </div>
          </div>

          <div className="flex justify-end gap-2 pt-2">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 rounded-xl border border-border text-xs font-semibold hover:bg-elevated"
            >
              Batal
            </button>
            <button
              type="submit"
              disabled={saving}
              className="px-4 py-2 rounded-xl bg-primary text-white text-xs font-semibold hover:bg-primary-dim disabled:opacity-60"
            >
              {saving ? "Menyimpan..." : "Simpan Jadwal"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
