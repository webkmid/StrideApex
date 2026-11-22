"use client";

import React, { useState } from "react";
import { X } from "lucide-react";
import { fromDateInputValue, toDateInputValue } from "@/lib/formatters";
import type { WorkoutLog } from "@/lib/types";

interface WorkoutEditModalProps {
  workout: WorkoutLog;
  onClose: () => void;
  onSave: (updated: WorkoutLog) => Promise<void>;
}

export function WorkoutEditModal({ workout, onClose, onSave }: WorkoutEditModalProps) {
  const [title, setTitle] = useState(workout.title);
  const [dateStr, setDateStr] = useState(toDateInputValue(workout.createdAtEpochMs));
  const [distanceKm, setDistanceKm] = useState(String(workout.distanceKm));
  const [durationMins, setDurationMins] = useState(String(Math.floor(workout.durationSeconds / 60)));
  const [durationSecs, setDurationSecs] = useState(String(workout.durationSeconds % 60));
  const [avgHr, setAvgHr] = useState(String(workout.avgHeartRate || ""));
  const [cadence, setCadence] = useState(String(workout.cadenceSpm || ""));
  const [elevation, setElevation] = useState(String(workout.elevationGainM || ""));
  const [splitsSummary, setSplitsSummary] = useState(workout.splitsSummary);
  const [saving, setSaving] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const dist = Math.max(0.1, parseFloat(distanceKm.replace(",", ".")) || workout.distanceKm);
    const dur = Math.max(60, (parseInt(durationMins, 10) || 0) * 60 + (parseInt(durationSecs, 10) || 0));
    setSaving(true);
    try {
      await onSave({
        ...workout,
        title: title.trim() || "Training Run",
        distanceKm: dist,
        durationSeconds: dur,
        avgPaceSecondsPerKm: Math.round(dur / dist),
        avgHeartRate: parseInt(avgHr, 10) || 0,
        cadenceSpm: parseInt(cadence, 10) || 0,
        elevationGainM: parseInt(elevation, 10) || 0,
        splitsSummary: splitsSummary.trim(),
        createdAtEpochMs: fromDateInputValue(dateStr),
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
          <h3 className="font-bold text-base">Edit Sesi Lari & Kalibrasi Split</h3>
          <button type="button" onClick={onClose} className="p-1.5 rounded-lg hover:bg-elevated">
            <X className="w-4 h-4" />
          </button>
        </div>

        <form onSubmit={(e) => void handleSubmit(e)} className="mt-4 space-y-3.5 text-sm">
          <div>
            <label className="block text-xs font-semibold text-text-secondary mb-1">Judul Sesi</label>
            <input
              type="text"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              className="w-full px-3 py-2 rounded-xl bg-elevated border border-border text-text-primary focus:outline-none focus:border-primary"
              required
            />
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-text-secondary mb-1">Tanggal</label>
              <input
                type="date"
                value={dateStr}
                onChange={(e) => setDateStr(e.target.value)}
                className="w-full px-3 py-2 rounded-xl bg-elevated border border-border text-text-primary focus:outline-none focus:border-primary"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-text-secondary mb-1">Jarak (km)</label>
              <input
                type="number"
                step="0.01"
                value={distanceKm}
                onChange={(e) => setDistanceKm(e.target.value)}
                className="w-full px-3 py-2 rounded-xl bg-elevated border border-border text-text-primary font-mono focus:outline-none focus:border-primary"
                required
              />
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-text-secondary mb-1">Durasi (Menit)</label>
              <input
                type="number"
                value={durationMins}
                onChange={(e) => setDurationMins(e.target.value)}
                className="w-full px-3 py-2 rounded-xl bg-elevated border border-border text-text-primary font-mono focus:outline-none focus:border-primary"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-text-secondary mb-1">Detik Sisa</label>
              <input
                type="number"
                min={0}
                max={59}
                value={durationSecs}
                onChange={(e) => setDurationSecs(e.target.value)}
                className="w-full px-3 py-2 rounded-xl bg-elevated border border-border text-text-primary font-mono focus:outline-none focus:border-primary"
              />
            </div>
          </div>

          <div className="grid grid-cols-3 gap-3">
            <div>
              <label className="block text-xs font-semibold text-text-secondary mb-1">Avg HR (bpm)</label>
              <input
                type="number"
                value={avgHr}
                onChange={(e) => setAvgHr(e.target.value)}
                className="w-full px-3 py-2 rounded-xl bg-elevated border border-border text-text-primary font-mono focus:outline-none focus:border-primary"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-text-secondary mb-1">Cadence (spm)</label>
              <input
                type="number"
                value={cadence}
                onChange={(e) => setCadence(e.target.value)}
                className="w-full px-3 py-2 rounded-xl bg-elevated border border-border text-text-primary font-mono focus:outline-none focus:border-primary"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-text-secondary mb-1">Elevasi (m)</label>
              <input
                type="number"
                value={elevation}
                onChange={(e) => setElevation(e.target.value)}
                className="w-full px-3 py-2 rounded-xl bg-elevated border border-border text-text-primary font-mono focus:outline-none focus:border-primary"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-text-secondary mb-1">
              Split Summary (otomatis dikalibrasi ulang saat jarak/durasi berubah)
            </label>
            <textarea
              rows={3}
              value={splitsSummary}
              onChange={(e) => setSplitsSummary(e.target.value)}
              placeholder="KM 1: 4:50/km | KM 2: 4:48/km"
              className="w-full px-3 py-2 rounded-xl bg-elevated border border-border text-text-primary font-mono text-xs focus:outline-none focus:border-primary"
            />
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
              {saving ? "Menyimpan..." : "Simpan & Refresh AI"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
