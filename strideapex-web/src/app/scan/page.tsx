"use client";

import React, { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { ArrowLeft, Camera, Check, FileText, Sparkles, Upload } from "lucide-react";
import { SplitsVisualChart } from "@/components/SplitsVisualChart";
import { formatPace, fromDateInputValue, toDateInputValue } from "@/lib/formatters";
import { useStrideApex } from "@/lib/strideapex-context";
import { runTesseractOcr, parseStravaOcrText } from "@/lib/tesseract-ocr";

export default function ScanRunPage() {
  const router = useRouter();
  const { extractStravaOcr, saveWorkout } = useStrideApex();

  const [previewUrl, setPreviewUrl] = useState<string | null>(null);
  const [imageBase64, setImageBase64] = useState<string | undefined>();
  const [mimeType, setMimeType] = useState<string | undefined>();
  const [rawText, setRawText] = useState("");
  const [extracting, setExtracting] = useState(false);
  const [sourceTag, setSourceTag] = useState<string | null>(null);

  // Editable form fields
  const [title, setTitle] = useState("");
  const [distanceKm, setDistanceKm] = useState("");
  const [durMins, setDurMins] = useState("");
  const [durSecs, setDurSecs] = useState("0");
  const [avgPaceMin, setAvgPaceMin] = useState("");
  const [avgPaceSec, setAvgPaceSec] = useState("0");
  const [avgHr, setAvgHr] = useState("");
  const [cadence, setCadence] = useState("");
  const [elevation, setElevation] = useState("");
  const [splitsSummary, setSplitsSummary] = useState("");
  const [aiInsight, setAiInsight] = useState("");
  const [dateStr, setDateStr] = useState(toDateInputValue(Date.now()));
  const [sourceType, setSourceType] = useState<"STRAVA_OCR" | "MANUAL">("MANUAL");
  const [saving, setSaving] = useState(false);

  const applyExtracted = (data: Awaited<ReturnType<typeof extractStravaOcr>>) => {
    setTitle(data.title);
    setDistanceKm(String(data.distanceKm));
    setDurMins(String(Math.floor(data.durationSeconds / 60)));
    setDurSecs(String(data.durationSeconds % 60));
    setAvgPaceMin(String(Math.floor(data.avgPaceSecondsPerKm / 60)));
    setAvgPaceSec(String(data.avgPaceSecondsPerKm % 60));
    setAvgHr(data.avgHeartRate ? String(data.avgHeartRate) : "");
    setCadence(data.cadenceSpm ? String(data.cadenceSpm) : "");
    setElevation(data.elevationGainM ? String(data.elevationGainM) : "");
    setSplitsSummary(data.splitsSummary);
    setAiInsight(data.aiCoachingInsight);
    setSourceType("STRAVA_OCR");
    setSourceTag(data.sourceEngine);
  };

  const handleFileChange = (file?: File) => {
    if (!file) return;
    setMimeType(file.type || "image/jpeg");
    const reader = new FileReader();
    reader.onload = async () => {
      const result = String(reader.result ?? "");
      setPreviewUrl(result);
      setImageBase64(result);
      setExtracting(true);
      try {
        const { rawText: ocrText, metrics } = await runTesseractOcr(result);
        setRawText(ocrText);
        applyExtracted(metrics);
      } finally {
        setExtracting(false);
      }
    };
    reader.readAsDataURL(file);
  };

  const handleRunExtract = async (presetIndex?: number) => {
    setExtracting(true);
    try {
      if (presetIndex !== undefined) {
        const extracted = await extractStravaOcr({ presetIndex });
        applyExtracted(extracted);
      } else if (rawText.trim()) {
        applyExtracted(parseStravaOcrText(rawText));
      } else if (imageBase64) {
        const { rawText: ocrText, metrics } = await runTesseractOcr(imageBase64);
        setRawText(ocrText);
        applyExtracted(metrics);
      }
    } finally {
      setExtracting(false);
    }
  };

  // Auto-calculate distance from duration / avg pace if distance left blank
  const parsedDur = (parseInt(durMins, 10) || 0) * 60 + (parseInt(durSecs, 10) || 0);
  const parsedPace = (parseInt(avgPaceMin, 10) || 0) * 60 + (parseInt(avgPaceSec, 10) || 0);
  const rawDistNum = parseFloat(distanceKm.replace(",", ".")) || 0;
  const effectiveDistanceKm =
    rawDistNum > 0 ? rawDistNum : parsedDur > 0 && parsedPace > 0 ? Number((parsedDur / parsedPace).toFixed(2)) : 0;
  const effectivePaceSec =
    parsedPace > 0 ? parsedPace : effectiveDistanceKm > 0 && parsedDur > 0 ? Math.round(parsedDur / effectiveDistanceKm) : 0;

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    if (effectiveDistanceKm <= 0 || parsedDur <= 0) return;
    setSaving(true);
    try {
      await saveWorkout({
        title: title.trim() || "Training Run",
        distanceKm: effectiveDistanceKm,
        durationSeconds: parsedDur,
        avgHeartRate: parseInt(avgHr, 10) || 0,
        cadenceSpm: parseInt(cadence, 10) || 0,
        elevationGainM: parseInt(elevation, 10) || 0,
        splitsSummary,
        aiCoachingInsight: aiInsight,
        sourceType,
        createdAtEpochMs: fromDateInputValue(dateStr),
      });
      router.push("/history");
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between gap-3">
        <Link
          href="/"
          className="inline-flex items-center gap-1.5 px-3 py-2 rounded-xl bg-card border border-border hover:border-primary/50 text-xs font-semibold text-text-primary transition-colors"
        >
          <ArrowLeft className="w-3.5 h-3.5" />
          <span>Kembali ke Dashboard</span>
        </Link>
        <span className="text-xs font-mono text-text-secondary">AI Vision OCR &amp; Manual Telemetry</span>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
      {/* Left: Screenshot Upload, Text Parser & Presets */}
      <div className="lg:col-span-5 space-y-5">
        <div className="p-5 rounded-2xl bg-card border border-border space-y-4">
          <div className="flex items-center gap-2">
            <Camera className="w-4 h-4 text-primary" />
            <h2 className="font-bold text-base">Scan Screenshot Strava</h2>
          </div>

          <label className="block border-2 border-dashed border-border hover:border-primary/60 rounded-2xl p-6 text-center cursor-pointer bg-elevated transition-colors">
            <input
              type="file"
              accept="image/*"
              className="hidden"
              onChange={(e) => handleFileChange(e.target.files?.[0])}
            />
            {previewUrl ? (
              // eslint-disable-next-line @next/next/no-img-element
              <img src={previewUrl} alt="Preview Screenshot" className="max-h-56 mx-auto rounded-xl object-contain" />
            ) : (
              <div className="space-y-2 py-4">
                <Upload className="w-8 h-8 mx-auto text-primary" />
                <div className="text-sm font-semibold">Unggah Screenshot Strava (Summary / Splits)</div>
                <div className="text-xs text-text-secondary">
                  Jika jarak tidak tertulis pada gambar Splits, sistem otomatis menghitung dari Moving Time / Avg Pace.
                </div>
              </div>
            )}
          </label>

          {/* Raw Text / Splits Paste */}
          <div className="space-y-2 pt-2 border-t border-border">
            <label className="flex items-center gap-1.5 text-xs font-semibold text-text-secondary">
              <FileText className="w-3.5 h-3.5" />
              Tempel Teks / Rincian Split Strava
            </label>
            <textarea
              rows={4}
              value={rawText}
              onChange={(e) => setRawText(e.target.value)}
              placeholder={"Contoh:\nMoving Time 25:00\nAvg Pace 5:00 /km\nKM 1: 5:04/km | KM 2: 5:00/km | KM 3: 4:58/km"}
              className="w-full p-3 rounded-xl bg-elevated border border-border text-xs font-mono text-text-primary focus:outline-none focus:border-primary"
            />
            <button
              type="button"
              disabled={extracting || (!rawText.trim() && !imageBase64)}
              onClick={() => void handleRunExtract()}
              className="w-full py-2.5 rounded-xl bg-primary text-white text-xs font-bold hover:bg-primary-dim disabled:opacity-50 inline-flex items-center justify-center gap-1.5"
            >
              <Sparkles className="w-3.5 h-3.5" />
              <span>{extracting ? "Mengekstrak Data..." : "Ekstrak Data Telemetri"}</span>
            </button>
          </div>
        </div>
      </div>

      {/* Right: Verification Form + Live Preview */}
      <div className="lg:col-span-7 space-y-5">
        <form onSubmit={(e) => void handleSave(e)} className="p-5 rounded-2xl bg-card border border-border space-y-4">
          <div className="flex items-center justify-between">
            <h2 className="font-bold text-base">Verifikasi & Simpan Sesi Lari</h2>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 text-sm">
            <div>
              <label className="block text-xs font-semibold text-text-secondary mb-1">Judul Sesi</label>
              <input
                type="text"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                placeholder="Morning Threshold Run"
                className="w-full px-3 py-2 rounded-xl bg-elevated border border-border text-text-primary focus:outline-none focus:border-primary"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-text-secondary mb-1">Tanggal Lari</label>
              <input
                type="date"
                value={dateStr}
                onChange={(e) => setDateStr(e.target.value)}
                className="w-full px-3 py-2 rounded-xl bg-elevated border border-border text-text-primary focus:outline-none focus:border-primary"
              />
            </div>
          </div>

          <div className="grid grid-cols-2 sm:grid-cols-3 gap-3 text-sm">
            <div>
              <label className="block text-xs font-semibold text-text-secondary mb-1">
                Jarak (km){!rawDistNum && effectiveDistanceKm > 0 ? " · Auto" : ""}
              </label>
              <input
                type="number"
                step="0.01"
                value={distanceKm}
                onChange={(e) => setDistanceKm(e.target.value)}
                placeholder={effectiveDistanceKm > 0 ? String(effectiveDistanceKm) : "10.0"}
                className="w-full px-3 py-2 rounded-xl bg-elevated border border-border text-text-primary font-mono focus:outline-none focus:border-primary"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-text-secondary mb-1">Durasi (Menit : Detik)</label>
              <div className="flex gap-1.5">
                <input
                  type="number"
                  value={durMins}
                  onChange={(e) => setDurMins(e.target.value)}
                  placeholder="45"
                  className="w-full px-2.5 py-2 rounded-xl bg-elevated border border-border text-text-primary font-mono focus:outline-none focus:border-primary"
                />
                <input
                  type="number"
                  min={0}
                  max={59}
                  value={durSecs}
                  onChange={(e) => setDurSecs(e.target.value)}
                  placeholder="00"
                  className="w-20 px-2.5 py-2 rounded-xl bg-elevated border border-border text-text-primary font-mono focus:outline-none focus:border-primary"
                />
              </div>
            </div>
            <div>
              <label className="block text-xs font-semibold text-text-secondary mb-1">
                Avg Pace ({formatPace(effectivePaceSec)})
              </label>
              <div className="flex gap-1.5">
                <input
                  type="number"
                  value={avgPaceMin}
                  onChange={(e) => setAvgPaceMin(e.target.value)}
                  placeholder="4"
                  className="w-full px-2.5 py-2 rounded-xl bg-elevated border border-border text-text-primary font-mono focus:outline-none focus:border-primary"
                />
                <input
                  type="number"
                  min={0}
                  max={59}
                  value={avgPaceSec}
                  onChange={(e) => setAvgPaceSec(e.target.value)}
                  placeholder="30"
                  className="w-20 px-2.5 py-2 rounded-xl bg-elevated border border-border text-text-primary font-mono focus:outline-none focus:border-primary"
                />
              </div>
            </div>
          </div>

          <div className="grid grid-cols-3 gap-3 text-sm">
            <div>
              <label className="block text-xs font-semibold text-text-secondary mb-1">Avg HR (bpm)</label>
              <input
                type="number"
                value={avgHr}
                onChange={(e) => setAvgHr(e.target.value)}
                placeholder="162"
                className="w-full px-3 py-2 rounded-xl bg-elevated border border-border text-text-primary font-mono focus:outline-none focus:border-primary"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-text-secondary mb-1">Cadence (spm)</label>
              <input
                type="number"
                value={cadence}
                onChange={(e) => setCadence(e.target.value)}
                placeholder="180"
                className="w-full px-3 py-2 rounded-xl bg-elevated border border-border text-text-primary font-mono focus:outline-none focus:border-primary"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-text-secondary mb-1">Elevasi (m)</label>
              <input
                type="number"
                value={elevation}
                onChange={(e) => setElevation(e.target.value)}
                placeholder="65"
                className="w-full px-3 py-2 rounded-xl bg-elevated border border-border text-text-primary font-mono focus:outline-none focus:border-primary"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-text-secondary mb-1">Rincian Split per KM</label>
            <input
              type="text"
              value={splitsSummary}
              onChange={(e) => setSplitsSummary(e.target.value)}
              placeholder="KM 1: 4:50/km | KM 2: 4:48/km"
              className="w-full px-3 py-2 rounded-xl bg-elevated border border-border text-text-primary font-mono text-xs focus:outline-none focus:border-primary"
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-text-secondary mb-1">Catatan AI Coach</label>
            <textarea
              rows={2}
              value={aiInsight}
              onChange={(e) => setAiInsight(e.target.value)}
              placeholder="Evaluasi otomatis saat ekstraksi atau simpan..."
              className="w-full px-3 py-2 rounded-xl bg-elevated border border-border text-text-primary text-xs focus:outline-none focus:border-primary"
            />
          </div>

          <button
            type="submit"
            disabled={saving || effectiveDistanceKm <= 0 || parsedDur <= 0}
            className="w-full py-2.5 rounded-xl bg-primary text-white text-sm font-bold hover:bg-primary-dim disabled:opacity-50 inline-flex items-center justify-center gap-2"
          >
            <Check className="w-4 h-4" />
            <span>{saving ? "Menyimpan..." : "Simpan Sesi & Perbarui PR"}</span>
          </button>
        </form>

        {splitsSummary && (
          <div className="p-5 rounded-2xl bg-card border border-border">
            <SplitsVisualChart splitsSummary={splitsSummary} avgPaceSecondsPerKm={effectivePaceSec} />
          </div>
        )}
        </div>
      </div>
    </div>
  );
}
