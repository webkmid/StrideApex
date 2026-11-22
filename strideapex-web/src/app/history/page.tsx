"use client";

import React, { useMemo, useState } from "react";
import Link from "next/link";
import { Download, Search, Trash2 } from "lucide-react";
import { AdBanner } from "@/components/AdBanner";
import { formatDateId, formatDuration, formatPace } from "@/lib/formatters";
import { useStrideApex } from "@/lib/strideapex-context";

type DistFilter = "ALL" | "SHORT" | "MID" | "LONG";
type SortMode = "NEWEST" | "FASTEST" | "LONGEST";

export default function HistoryPage() {
  const { workouts, analytics, deleteWorkout, exportData } = useStrideApex();
  const [query, setQuery] = useState("");
  const [distFilter, setDistFilter] = useState<DistFilter>("ALL");
  const [sortMode, setSortMode] = useState<SortMode>("NEWEST");

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();
    return workouts
      .filter((w) => {
        if (q && !w.title.toLowerCase().includes(q) && !w.splitsSummary.toLowerCase().includes(q)) {
          return false;
        }
        if (distFilter === "SHORT") return w.distanceKm < 5;
        if (distFilter === "MID") return w.distanceKm >= 5 && w.distanceKm <= 12;
        if (distFilter === "LONG") return w.distanceKm > 12;
        return true;
      })
      .sort((a, b) => {
        if (sortMode === "FASTEST") return a.avgPaceSecondsPerKm - b.avgPaceSecondsPerKm;
        if (sortMode === "LONGEST") return b.distanceKm - a.distanceKm;
        return b.createdAtEpochMs - a.createdAtEpochMs;
      });
  }, [workouts, query, distFilter, sortMode]);

  return (
    <div className="space-y-5">
      {/* Compact Filter Bar */}
      <div className="p-4 rounded-2xl bg-card border border-border flex flex-col md:flex-row md:items-center justify-between gap-3">
        <div className="relative flex-1 max-w-md">
          <Search className="w-4 h-4 text-text-secondary absolute left-3 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Cari judul lari..."
            className="w-full pl-9 pr-3 py-2 rounded-xl bg-elevated border border-border text-xs text-text-primary focus:outline-none focus:border-primary"
          />
        </div>

        <div className="flex flex-wrap items-center gap-2">
          {(
            [
              ["ALL", "Semua"],
              ["SHORT", "<5K"],
              ["MID", "5–12K"],
              ["LONG", ">12K"],
            ] as const
          ).map(([key, label]) => (
            <button
              key={key}
              type="button"
              onClick={() => setDistFilter(key)}
              className={`px-3 py-1.5 rounded-lg text-xs font-semibold border transition-colors ${
                distFilter === key
                  ? "bg-primary text-white border-primary"
                  : "bg-elevated text-text-secondary border-border hover:text-text-primary"
              }`}
            >
              {label}
            </button>
          ))}

          <select
            value={sortMode}
            onChange={(e) => setSortMode(e.target.value as SortMode)}
            className="px-3 py-1.5 rounded-lg bg-elevated border border-border text-xs font-semibold text-text-primary focus:outline-none"
          >
            <option value="NEWEST">Terbaru</option>
            <option value="FASTEST">Tercepat</option>
            <option value="LONGEST">Terjauh</option>
          </select>

          <button
            type="button"
            onClick={() => void exportData("csv")}
            className="inline-flex items-center gap-1 px-3 py-1.5 rounded-lg bg-elevated border border-border text-xs font-semibold hover:border-primary/50"
          >
            <Download className="w-3.5 h-3.5" /> CSV
          </button>
        </div>
      </div>

      {/* Workout List */}
      {filtered.length === 0 ? (
        <div className="p-10 rounded-2xl bg-card border border-border text-center text-sm text-text-secondary">
          Tidak ada sesi lari yang cocok dengan filter saat ini.
        </div>
      ) : (
        <div className="space-y-3">
          {filtered.map((w) => {
            const deep = analytics.workoutDeepMetrics[w.id];
            return (
              <div
                key={w.id}
                className="p-4 rounded-2xl bg-card border border-border hover:border-primary/50 transition-colors flex flex-col md:flex-row md:items-center justify-between gap-4"
              >
                <Link href={`/history/${encodeURIComponent(w.id)}`} className="min-w-0 flex-1 space-y-1.5">
                  <div className="flex flex-wrap items-center gap-2">
                    <span className="font-bold text-sm sm:text-base">{w.title}</span>
                    <span className="px-2 py-0.5 rounded bg-elevated border border-border text-[10px] font-mono text-text-secondary">
                      {w.sourceType === "STRAVA_OCR" ? "OCR" : "MANUAL"}
                    </span>
                    {deep && (
                      <>
                        <span className="px-2 py-0.5 rounded bg-primary/12 text-primary text-[10px] font-mono font-bold">
                          Zone {deep.zoneCode}
                        </span>
                        {deep.vdot > 0 && (
                          <span className="px-2 py-0.5 rounded bg-accent-blue/12 text-accent-blue text-[10px] font-mono font-bold">
                            VDOT {deep.vdot.toFixed(1)}
                          </span>
                        )}
                        {deep.splits.length >= 2 && (
                          <span className="px-2 py-0.5 rounded bg-elevated text-text-secondary text-[10px] font-mono">
                            {deep.splitStrategy}
                          </span>
                        )}
                      </>
                    )}
                  </div>
                  <div className="text-xs text-text-secondary">{formatDateId(w.createdAtEpochMs)}</div>
                </Link>

                <div className="flex items-center justify-between md:justify-end gap-5 shrink-0">
                  <div className="grid grid-cols-3 gap-4 font-mono text-xs tabular-nums">
                    <div>
                      <div className="text-[10px] text-text-secondary">JARAK</div>
                      <div className="font-bold">{w.distanceKm.toFixed(2)} km</div>
                    </div>
                    <div>
                      <div className="text-[10px] text-text-secondary">WAKTU</div>
                      <div className="font-bold">{formatDuration(w.durationSeconds)}</div>
                    </div>
                    <div>
                      <div className="text-[10px] text-text-secondary">PACE</div>
                      <div className="font-bold text-primary">{formatPace(w.avgPaceSecondsPerKm)}</div>
                    </div>
                  </div>

                  <button
                    type="button"
                    onClick={() => void deleteWorkout(w.id)}
                    title="Hapus sesi"
                    className="p-2 rounded-xl border border-border text-text-secondary hover:text-danger hover:border-danger/50 transition-colors"
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
                </div>
              </div>
            );
          })}
        </div>
      )}

      <AdBanner slotKey="footer" />
    </div>
  );
}
