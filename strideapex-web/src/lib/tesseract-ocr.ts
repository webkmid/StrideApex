import { createWorker, PSM, type Page } from "tesseract.js";
import type { ExtractedStravaMetrics } from "./types";

function parseTimeToSeconds(raw: string): number {
  const clean = raw.trim().replace(/[.;]/g, ":");
  const parts = clean.split(":").map((p) => parseInt(p.trim(), 10));
  if (parts.some((n) => Number.isNaN(n))) return 0;
  if (parts.length === 2) return parts[0] * 60 + parts[1];
  if (parts.length === 3) return parts[0] * 3600 + parts[1] * 60 + parts[2];
  return 0;
}

function formatPaceShort(sec: number): string {
  if (!sec || sec <= 0) return "--:--";
  const safe = Math.round(sec);
  const m = Math.floor(safe / 60);
  const s = safe % 60;
  return `${m}:${String(s).padStart(2, "0")}`;
}

function formatDurationShort(sec: number): string {
  if (!sec || sec <= 0) return "0:00";
  const safe = Math.round(sec);
  const h = Math.floor(safe / 3600);
  const m = Math.floor((safe % 3600) / 60);
  const s = safe % 60;
  if (h > 0) {
    return `${h}:${String(m).padStart(2, "0")}:${String(s).padStart(2, "0")}`;
  }
  return `${m}:${String(s).padStart(2, "0")}`;
}

function isChartAxisLine(line: string): boolean {
  const clean = line.trim().toLowerCase();
  const kmMatches = clean.match(/\bkm\b/g);
  if (kmMatches && kmMatches.length >= 2) return true;
  if (/^(?:0[.,]5|1(?:[.,]0)?|1[.,]5|2(?:[.,]0)?|2[.,]5|3(?:[.,]0)?|3[.,]5|4(?:[.,]0)?|5(?:[.,]0)?)\s*km$/i.test(clean)) {
    return true;
  }
  if (/^\/km$/i.test(clean)) return true;
  return false;
}

/**
 * Reconstructs visual horizontal lines from Tesseract word bounding boxes,
 * matching Android's StravaLocalOcrParser.reconstructTextByRows.
 */
function reconstructTextByRows(page: Page): string {
  const blocks = page.blocks ?? [];
  interface WordItem {
    text: string;
    x0: number;
    y0: number;
    x1: number;
    y1: number;
    centerY: number;
    height: number;
  }

  const items: WordItem[] = [];
  for (const block of blocks) {
    for (const para of block.paragraphs ?? []) {
      for (const line of para.lines ?? []) {
        for (const word of line.words ?? []) {
          const t = (word.text ?? "").trim();
          if (!t) continue;
          // Filter out isolated bar-artifact punctuation tokens
          if (/^[|—–_~=\[\]{}<>]+$/.test(t) && t.length <= 3) continue;
          const { x0, y0, x1, y1 } = word.bbox;
          const height = Math.max(10, y1 - y0);
          items.push({
            text: t,
            x0,
            y0,
            x1,
            y1,
            centerY: (y0 + y1) / 2,
            height,
          });
        }
      }
    }
  }

  if (items.length === 0) return page.text ?? "";

  const rows: WordItem[][] = [];
  for (const item of items) {
    let bestRow: WordItem[] | null = null;
    let bestDist = Infinity;
    for (const row of rows) {
      const avgCenterY = row.reduce((s, w) => s + w.centerY, 0) / row.length;
      const avgHeight = Math.max(14, row.reduce((s, w) => s + w.height, 0) / row.length);
      const dy = Math.abs(item.centerY - avgCenterY);
      if (dy < avgHeight * 0.55 && dy < bestDist) {
        bestDist = dy;
        bestRow = row;
      }
    }
    if (bestRow) {
      bestRow.push(item);
    } else {
      rows.push([item]);
    }
  }

  rows.sort((a, b) => {
    const minYa = Math.min(...a.map((w) => w.y0));
    const minYb = Math.min(...b.map((w) => w.y0));
    return minYa - minYb;
  });

  return rows
    .map((row) =>
      row
        .sort((a, b) => a.x0 - b.x0)
        .map((w) => w.text)
        .join(" ")
    )
    .join("\n");
}

/**
 * Preprocesses a Strava screenshot on an offscreen HTML5 Canvas:
 * - Upscales 2x for crisp small digit recognition
 * - Erases Strava blue split bars / pace chart fill and orange bars to white
 * - Converts dark-mode white/gray text into high-contrast dark text on pure white background
 */
async function preprocessStravaImage(imageDataUrl: string): Promise<string> {
  if (typeof document === "undefined" || typeof Image === "undefined") {
    return imageDataUrl;
  }

  return new Promise((resolve) => {
    const img = new Image();
    img.crossOrigin = "anonymous";
    img.onload = () => {
      try {
        const scale = img.width < 1400 ? 2 : 1;
        const w = Math.max(1, img.width * scale);
        const h = Math.max(1, img.height * scale);
        const canvas = document.createElement("canvas");
        canvas.width = w;
        canvas.height = h;
        const ctx = canvas.getContext("2d");
        if (!ctx) {
          resolve(imageDataUrl);
          return;
        }

        ctx.imageSmoothingEnabled = true;
        ctx.imageSmoothingQuality = "high";
        ctx.drawImage(img, 0, 0, w, h);

        const imgData = ctx.getImageData(0, 0, w, h);
        const data = imgData.data;

        // Detect Dark Mode vs Light Mode by sampling background luminance
        let lumSum = 0;
        let sampleCount = 0;
        const step = Math.max(4, Math.floor(data.length / 4 / 2000)) * 4;
        for (let i = 0; i < data.length; i += step) {
          const r = data[i];
          const g = data[i + 1];
          const b = data[i + 2];
          lumSum += 0.299 * r + 0.587 * g + 0.114 * b;
          sampleCount++;
        }
        const isDarkMode = sampleCount > 0 && lumSum / sampleCount < 128;

        for (let i = 0; i < data.length; i += 4) {
          const r = data[i];
          const g = data[i + 1];
          const b = data[i + 2];

          // Strava blue split bars (#2B78E4) & pace chart fill have strong blue dominance (b - r > 28)
          // Strava orange elements have strong red dominance (r - b > 45)
          const isColoredBarOrChart = b - r > 28 || (r - b > 45 && g < 160);
          const lum = 0.299 * r + 0.587 * g + 0.114 * b;

          let out = 255; // default pure white background
          if (!isColoredBarOrChart) {
            if (isDarkMode) {
              // In dark mode, neutral bright pixels (white/light-gray text) become dark ink
              if (lum > 155) {
                out = 0;
              } else if (lum > 95) {
                out = Math.max(0, Math.min(255, Math.round(255 - ((lum - 95) / 60) * 255)));
              }
            } else {
              // In light mode, neutral dark pixels become dark ink
              if (lum < 110) {
                out = 0;
              } else if (lum < 175) {
                out = Math.max(0, Math.min(255, Math.round(((lum - 110) / 65) * 255)));
              }
            }
          }

          data[i] = out;
          data[i + 1] = out;
          data[i + 2] = out;
          data[i + 3] = 255;
        }

        ctx.putImageData(imgData, 0, 0);
        resolve(canvas.toDataURL("image/png"));
      } catch {
        resolve(imageDataUrl);
      }
    };
    img.onerror = () => resolve(imageDataUrl);
    img.src = imageDataUrl;
  });
}

interface ParsedSplitRow {
  label: string;
  distKm: number;
  paceSec: number;
}

export function parseStravaOcrText(rawText: string): ExtractedStravaMetrics {
  const normalizedText = rawText.replace(/\u2014|\u2013/g, "-");
  const lines = normalizedText
    .split(/\r?\n/)
    .map((l) => l.trim())
    .filter(Boolean);

  // ==========================================
  // 1. Moving Time & Elapsed Time
  // ==========================================
  let movingTimeSec = 0;
  let elapsedTimeSec = 0;

  const movingInline = normalizedText.match(
    /(?:Moving\s*Time|Waktu\s*Bergerak)[^\d\n]*(\d{1,2}[:.;][0-5]\d(?:[:.;][0-5]\d)?)/i
  );
  if (movingInline) {
    movingTimeSec = parseTimeToSeconds(movingInline[1]);
  }

  const elapsedInline = normalizedText.match(
    /(?:Elapsed\s*Time|Waktu\s*Berlalu|Total\s*Time)[^\d\n]*(\d{1,2}[:.;][0-5]\d(?:[:.;][0-5]\d)?)/i
  );
  if (elapsedInline) {
    elapsedTimeSec = parseTimeToSeconds(elapsedInline[1]);
  }

  // Next-line proximity check for Moving Time / Elapsed Time
  if (movingTimeSec <= 0) {
    for (let i = 0; i < lines.length; i++) {
      if (/Moving\s*Time|Waktu\s*Bergerak/i.test(lines[i])) {
        for (let j = i; j <= Math.min(lines.length - 1, i + 2); j++) {
          const m = lines[j].match(/\b(\d{1,2}[:.;][0-5]\d(?:[:.;][0-5]\d)?)\b(?!\s*\/)/);
          if (m) {
            const s = parseTimeToSeconds(m[1]);
            if (s >= 60) {
              movingTimeSec = s;
              break;
            }
          }
        }
        if (movingTimeSec > 0) break;
      }
    }
  }

  // Fallback when labels and values are in two separate column blocks at the bottom
  if (movingTimeSec <= 0 && elapsedTimeSec <= 0) {
    const hasBottomTimeLabels = /Moving\s*Time|Elapsed\s*Time/i.test(normalizedText);
    if (hasBottomTimeLabels) {
      const paceSectionIdx = lines.findIndex((l) => /^(?:Pace|Avg\s*Pace)\b/i.test(l));
      const startIdx = paceSectionIdx >= 0 ? paceSectionIdx : 0;
      const bottomDurations: number[] = [];
      for (let i = startIdx; i < lines.length; i++) {
        const l = lines[i];
        if (/\/km|\/mi|Avg\s*Pace|Fastest\s*Split/i.test(l)) continue;
        // Skip chart y-axis whole minutes (6:00..12:00)
        if (/^\d{1,2}:00$/.test(l)) continue;
        const m = l.match(/\b(\d{1,2}[:.;][0-5]\d(?::[0-5]\d)?)\b/);
        if (m) {
          const sec = parseTimeToSeconds(m[1]);
          // Duration is typically >= 15 mins (900s) or larger than normal 1km pace
          if (sec >= 900) {
            bottomDurations.push(sec);
          }
        }
      }
      if (bottomDurations.length > 0) {
        movingTimeSec = Math.min(...bottomDurations);
      }
    }
  }

  let durationSeconds = movingTimeSec > 0 ? movingTimeSec : elapsedTimeSec;

  // ==========================================
  // 2. Average Pace (e.g. "6:59 /km")
  // ==========================================
  let avgPaceSecondsPerKm = 0;
  const avgPaceInline = normalizedText.match(
    /(?:Avg\s*Pace|Average\s*Pace|Pace\s*Rata)[^\d\n]*(\d{1,2}[:.;][0-5]\d)/i
  );
  if (avgPaceInline) {
    const candidate = parseTimeToSeconds(avgPaceInline[1]);
    if (candidate >= 120 && candidate <= 900) {
      avgPaceSecondsPerKm = candidate;
    }
  }

  // Fallback: look for "<M:SS> /km" tokens (Avg Pace appears before Fastest Split and is >= Fastest Split)
  if (avgPaceSecondsPerKm <= 0) {
    const slashKmMatches = Array.from(
      normalizedText.matchAll(/\b(\d{1,2}[:.;][0-5]\d)\s*\/?\s*km\b/gi)
    )
      .map((m) => parseTimeToSeconds(m[1]))
      .filter((s) => s >= 120 && s <= 900);
    if (slashKmMatches.length > 0) {
      avgPaceSecondsPerKm = slashKmMatches[0];
    }
  }

  // ==========================================
  // 3. Per-KM Splits Extraction
  // ==========================================
  const splitRows: ParsedSplitRow[] = [];

  // 3A. First check if text already contains formatted "KM 1: 7:54/km" tokens
  const explicitKmMatches = Array.from(
    normalizedText.matchAll(/KM\s*(0[.,]\d+|[1-9]\d*(?:[.,]\d+)?)\s*[:.\-]?\s*(\d{1,2}[:.;][0-5]\d)/gi)
  );
  if (explicitKmMatches.length >= 2) {
    for (const m of explicitKmMatches) {
      const label = m[1].replace(",", ".");
      const distVal = parseFloat(label);
      const paceSec = parseTimeToSeconds(m[2]);
      if (distVal > 0 && paceSec >= 120 && paceSec <= 900) {
        splitRows.push({
          label,
          distKm: distVal < 1 ? distVal : 1,
          paceSec,
        });
      }
    }
  }

  // 3B. Parse Strava Splits Table rows (above the "Pace" chart header or across lines)
  if (splitRows.length < 2) {
    splitRows.length = 0;

    // Identify where the Pace chart section begins so chart y-axis labels (6:00..12:00) are excluded
    const paceHeaderIdx = lines.findIndex(
      (l, idx) => idx > 0 && /^(?:Pace|Grafik\s*Pace)\b/i.test(l) && !/Avg/i.test(l)
    );
    const avgPaceLineIdx = lines.findIndex((l) => /Avg\s*Pace|Moving\s*Time/i.test(l));
    const splitZoneEnd =
      paceHeaderIdx > 0
        ? paceHeaderIdx
        : avgPaceLineIdx > 0
        ? avgPaceLineIdx
        : lines.length;

    const candidateSplitLines = lines.slice(0, splitZoneEnd);

    for (let idx = 0; idx < candidateSplitLines.length; idx++) {
      const line = candidateSplitLines[idx];

      // Skip phone status bar or header "Run"
      if (/^(?:Run|Splits|Pace|Avg|Moving|Elapsed|Fastest)\b/i.test(line)) continue;
      if (isChartAxisLine(line)) continue;
      // Skip top status bar line if it starts with a 24-hour clock (e.g. "22:09")
      if (idx === 0 && /^\s*(?:1[5-9]|2[0-3])[:.;][0-5]\d\b/.test(line)) continue;

      // Normalize common OCR single-digit misreads at the start of a split line (e.g. 'A' -> '4', 'I'/'l' -> '1', 'O' -> '0')
      const cleanedLine = line
        .replace(/^\s*[Il|!](?=\s+\d{1,2}[:.;][0-5]\d)/, "1")
        .replace(/^\s*A(?=\s+\d{1,2}[:.;][0-5]\d)/, "4")
        .replace(/^\s*S(?=\s+\d{1,2}[:.;][0-5]\d)/, "5")
        .replace(/^\s*[Oo]([.,]\d+)/, "0$1");

      // If two splits were accidentally merged onto one line, find all "<label> <M:SS>" pairs on the line
      const pairMatches = Array.from(
        cleanedLine.matchAll(
          /(?:^|\s)(?:KM\s*)?([0-9]+(?:[.,][0-9]+)?|[.,][0-9]+)\s+([2-9]|1[0-4])[:.;]([0-5]\d)\b/gi
        )
      );

      if (pairMatches.length > 0) {
        for (const pm of pairMatches) {
          let rawLabel = pm[1].replace(",", ".");
          if (rawLabel.startsWith(".")) {
            rawLabel = `0${rawLabel}`;
          } else if (/^0[1-9]$/.test(rawLabel)) {
            rawLabel = `0.${rawLabel.slice(1)}`;
          }
          const paceSec = parseInt(pm[2], 10) * 60 + parseInt(pm[3], 10);
          const numVal = parseFloat(rawLabel);
          if (!Number.isNaN(numVal) && numVal > 0 && numVal <= 99 && paceSec >= 120 && paceSec <= 900) {
            const isFractional = numVal < 1 || rawLabel.includes(".");
            splitRows.push({
              label: isFractional ? Number(numVal.toFixed(2)).toString() : String(Math.round(numVal)),
              distKm: isFractional ? numVal : 1,
              paceSec,
            });
          }
        }
        continue;
      }

      // If we are inside the top Splits Table Zone and OCR missed the leading split digit on a row (e.g. "6:45 0"), capture the pace!
      if ((paceHeaderIdx > 0 || avgPaceLineIdx > 0) && !/\/km|\/mi/i.test(cleanedLine)) {
        const barePaceMatch = cleanedLine.match(/(?:^|\s)([3-9]|1[0-4])[:.;]([0-5]\d)(?:\s|$)/);
        if (barePaceMatch) {
          const paceSec = parseInt(barePaceMatch[1], 10) * 60 + parseInt(barePaceMatch[2], 10);
          // Exclude pure chart y-axis whole-minute lines ("6:00", "7:00") unless accompanied by an elevation delta token
          const isBareChartTick = /^\s*\d{1,2}:00\s*$/.test(cleanedLine);
          if (!isBareChartTick && paceSec >= 150 && paceSec <= 900) {
            const nextKm = splitRows.length + 1;
            splitRows.push({
              label: String(nextKm),
              distKm: 1,
              paceSec,
            });
          }
        }
      }
    }
  }

  // ==========================================
  // 4. Reconcile Fractional Final Split & Total Distance
  // ==========================================
  // In Strava Splits screens (e.g. 1, 2, 3, 4, 0.9 with Moving Time 34:16 and Avg Pace 6:59/km):
  // MovingTime = sum(full_split_paces) + last_split_dist * last_split_pace.
  if (splitRows.length >= 2) {
    // Re-number sequential full splits (1, 2, ..., N-1) cleanly
    for (let i = 0; i < splitRows.length - 1; i++) {
      splitRows[i].label = String(i + 1);
      splitRows[i].distKm = 1;
    }

    const lastIdx = splitRows.length - 1;
    const lastRow = splitRows[lastIdx];
    const sumPrevSeconds = splitRows.slice(0, lastIdx).reduce((acc, r) => acc + r.paceSec, 0);

    // If the last row is fractional (e.g. 0.9) and 1 full split row right before it was dropped by OCR:
    // MovingTime - (sumPrevSeconds + lastRow.distKm * lastRow.paceSec) will equal the missing 1 km split's pace!
    if (lastRow.distKm > 0 && lastRow.distKm < 1 && durationSeconds > 0 && avgPaceSecondsPerKm > 0) {
      const expectedTotalKm = durationSeconds / avgPaceSecondsPerKm;
      const currentTotalKm = (splitRows.length - 1) + lastRow.distKm;
      if (Math.abs(expectedTotalKm - (currentTotalKm + 1)) <= 0.25) {
        const missingSplitSec = Math.round(durationSeconds - (sumPrevSeconds + lastRow.distKm * lastRow.paceSec));
        if (missingSplitSec >= 150 && missingSplitSec <= 900) {
          splitRows.splice(lastIdx, 0, {
            label: String(lastIdx + 1),
            distKm: 1,
            paceSec: missingSplitSec,
          });
        }
      }
    }

    const updatedLastIdx = splitRows.length - 1;
    const updatedLastRow = splitRows[updatedLastIdx];
    const updatedSumPrevSec = splitRows.slice(0, updatedLastIdx).reduce((acc, r) => acc + r.paceSec, 0);

    if (durationSeconds > updatedSumPrevSec && updatedLastRow.paceSec > 0) {
      const remSeconds = durationSeconds - updatedSumPrevSec;
      const inferredLastDist = remSeconds / updatedLastRow.paceSec;
      // If the remaining time clearly indicates a fractional final kilometer (e.g. 0.1 to 0.95 km)
      if (inferredLastDist >= 0.08 && inferredLastDist < 0.96) {
        const roundedFrac = Math.round(inferredLastDist * 10) / 10;
        updatedLastRow.distKm = roundedFrac;
        updatedLastRow.label = roundedFrac.toFixed(1);
      } else if (updatedLastRow.distKm >= 1) {
        updatedLastRow.label = String(splitRows.length);
        updatedLastRow.distKm = 1;
      }
    } else if (updatedLastRow.distKm < 1) {
      const roundedFrac = Math.round(updatedLastRow.distKm * 10) / 10;
      updatedLastRow.distKm = roundedFrac;
      updatedLastRow.label = roundedFrac.toFixed(1);
    } else {
      // Check if lastRow.label was misread as "9" after "4" (from "0.9")
      const rawLastNum = parseFloat(updatedLastRow.label);
      if (rawLastNum > splitRows.length && rawLastNum <= 9) {
        const frac = rawLastNum / 10;
        updatedLastRow.distKm = frac;
        updatedLastRow.label = frac.toFixed(1);
      } else {
        updatedLastRow.label = String(splitRows.length);
        updatedLastRow.distKm = 1;
      }
    }
  }

  const splitsSummary = splitRows
    .map((s) => `KM ${s.label}: ${formatPaceShort(s.paceSec)}/km`)
    .join(" | ");

  // ==========================================
  // 5. Total Distance (Explicit Label -> Splits Sum -> Duration / AvgPace)
  // ==========================================
  let distanceKm = 0;
  const hasExplicitDistanceLabel = lines.some(
    (l) =>
      /\b(?:Distance|Jarak)\b/i.test(l) &&
      !/Distance\s*\/\s*Pace/i.test(l) &&
      !/Split/i.test(l)
  );

  if (hasExplicitDistanceLabel) {
    for (let i = 0; i < lines.length; i++) {
      if (/\b(?:Distance|Jarak)\b/i.test(lines[i])) {
        const windowText = [lines[i], lines[i + 1] ?? ""].join(" ");
        const dm = windowText.match(/\b(\d+[.,]\d+)\s*(?:km)?\b/i);
        if (dm) {
          const val = parseFloat(dm[1].replace(",", "."));
          if (val >= 0.1 && val <= 300) {
            distanceKm = Number(val.toFixed(2));
            break;
          }
        }
      }
    }
  }

  const splitsTotalKm =
    splitRows.length > 0
      ? Number(splitRows.reduce((acc, r) => acc + r.distKm, 0).toFixed(2))
      : 0;

  const paceDerivedKm =
    durationSeconds > 0 && avgPaceSecondsPerKm > 0
      ? Number((durationSeconds / avgPaceSecondsPerKm).toFixed(2))
      : 0;

  if (distanceKm <= 0) {
    if (splitsTotalKm > 0 && paceDerivedKm > 0) {
      // If splits sum (e.g. 4.9 km) is close to duration/pace (e.g. 4.91 km), prefer the clean split sum (4.9)
      distanceKm = Math.abs(splitsTotalKm - paceDerivedKm) <= 0.15 ? splitsTotalKm : paceDerivedKm;
    } else if (splitsTotalKm > 0) {
      distanceKm = splitsTotalKm;
    } else if (paceDerivedKm > 0) {
      distanceKm = paceDerivedKm;
    }
  }

  // Fill in missing duration or avgPace from splits if needed
  if (durationSeconds <= 0 && splitRows.length > 0) {
    durationSeconds = Math.round(splitRows.reduce((acc, r) => acc + r.distKm * r.paceSec, 0));
  }
  if (avgPaceSecondsPerKm <= 0 && distanceKm > 0 && durationSeconds > 0) {
    avgPaceSecondsPerKm = Math.round(durationSeconds / distanceKm);
  }

  // ==========================================
  // 6. Optional HR, Cadence, Elevation
  // ==========================================
  const hrMatch = normalizedText.match(/(?:Avg\s*HR|Heart\s*Rate|\bHR\b)[^\d\n]*(\d{2,3})\s*(?:bpm)?/i);
  const cadMatch = normalizedText.match(/(?:Cadence|\bSPM\b)[^\d\n]*(\d{2,3})/i);
  const elevMatch = normalizedText.match(/(?:Elev(?:ation)?\s*Gain|Elevasi)[^\d\n]*\+?(\d{1,5})\s*m\b/i);

  const avgHeartRate = hrMatch ? Math.min(240, parseInt(hrMatch[1], 10)) : 0;
  const cadenceSpm = cadMatch ? Math.min(260, parseInt(cadMatch[1], 10)) : 0;
  const elevationGainM = elevMatch ? Math.min(10000, parseInt(elevMatch[1], 10)) : 0;

  // ==========================================
  // 7. Coaching Insight
  // ==========================================
  let aiCoachingInsight =
    "Teks screenshot berhasil dibaca, silakan periksa kembali rincian sebelum menyimpan.";
  if (splitRows.length >= 2 && distanceKm > 0 && durationSeconds > 0) {
    const first = splitRows[0];
    const last = splitRows[splitRows.length - 1];
    const diff = last.paceSec - first.paceSec;
    const strategy =
      diff <= -5
        ? `Negative split yang solid (${Math.abs(diff)} detik/km lebih tajam di split akhir dibanding KM 1).`
        : diff >= 15
        ? `Positive split (+${diff} detik/km di split akhir vs KM 1); jaga kontrol pace awal agar daya tahan lebih merata.`
        : "Distribusi pace antar split terjaga stabil.";
    aiCoachingInsight = `Terekstrak ${distanceKm} km dalam ${formatDurationShort(durationSeconds)} (Avg Pace ${formatPaceShort(avgPaceSecondsPerKm)}/km, ${splitRows.length} split). ${strategy}`;
  } else if (distanceKm > 0 && durationSeconds > 0) {
    aiCoachingInsight = `Terekstrak otomatis dari screenshot (${distanceKm} km, durasi ${formatDurationShort(durationSeconds)}, avg pace ${formatPaceShort(avgPaceSecondsPerKm)}/km).`;
  }

  return {
    title: "Strava Run",
    distanceKm,
    durationSeconds,
    avgPaceSecondsPerKm,
    avgHeartRate,
    cadenceSpm,
    elevationGainM,
    splitsSummary,
    aiCoachingInsight,
    workoutDateEpochMs: null,
    detectedDateStr: "",
    sourceEngine: "TESSERACT_OCR",
  };
}

function buildCleanRawTextDisplay(metrics: ExtractedStravaMetrics, fallbackText: string): string {
  const parts: string[] = [];
  if (metrics.distanceKm > 0) {
    parts.push(`Distance: ${metrics.distanceKm} km`);
  }
  if (metrics.durationSeconds > 0) {
    parts.push(`Moving Time: ${formatDurationShort(metrics.durationSeconds)}`);
  }
  if (metrics.avgPaceSecondsPerKm > 0) {
    parts.push(`Avg Pace: ${formatPaceShort(metrics.avgPaceSecondsPerKm)} /km`);
  }
  if (metrics.splitsSummary) {
    parts.push(metrics.splitsSummary);
  }
  return parts.length >= 2 ? parts.join("\n") : fallbackText.trim();
}

export async function runTesseractOcr(imageDataUrl: string): Promise<{
  rawText: string;
  metrics: ExtractedStravaMetrics;
}> {
  const cleanedImageUrl = await preprocessStravaImage(imageDataUrl);
  const worker = await createWorker("eng");
  try {
    await worker.setParameters({
      tessedit_pageseg_mode: PSM.SINGLE_BLOCK,
      preserve_interword_spaces: "1",
    });

    // Pass 1: Preprocessed high-contrast image (blue bars & chart removed)
    const resClean = await worker.recognize(cleanedImageUrl, {}, { text: true, blocks: true });
    const spatialClean = reconstructTextByRows(resClean.data);
    const metricsClean = parseStravaOcrText(spatialClean);

    // If Pass 1 already extracted splits + duration + pace, return immediately
    if (
      metricsClean.splitsSummary &&
      metricsClean.durationSeconds > 0 &&
      metricsClean.avgPaceSecondsPerKm > 0
    ) {
      return {
        rawText: buildCleanRawTextDisplay(metricsClean, spatialClean),
        metrics: metricsClean,
      };
    }

    // Pass 2 fallback: also recognize original image and merge any missing fields
    const resOrig = await worker.recognize(imageDataUrl, {}, { text: true, blocks: true });
    const spatialOrig = reconstructTextByRows(resOrig.data);
    const metricsOrig = parseStravaOcrText(spatialOrig);

    const mergedSplits =
      (metricsClean.splitsSummary?.length ?? 0) >= (metricsOrig.splitsSummary?.length ?? 0)
        ? metricsClean.splitsSummary
        : metricsOrig.splitsSummary;
    const mergedDuration = metricsClean.durationSeconds || metricsOrig.durationSeconds;
    const mergedPace = metricsClean.avgPaceSecondsPerKm || metricsOrig.avgPaceSecondsPerKm;

    // Re-parse combined summary so fractional split and total distance reconcile with merged duration/pace
    const combinedInput = [
      mergedDuration > 0 ? `Moving Time ${formatDurationShort(mergedDuration)}` : "",
      mergedPace > 0 ? `Avg Pace ${formatPaceShort(mergedPace)} /km` : "",
      mergedSplits || spatialClean,
      spatialOrig,
    ]
      .filter(Boolean)
      .join("\n");

    const finalMetrics = parseStravaOcrText(combinedInput);
    if (!finalMetrics.splitsSummary && mergedSplits) {
      finalMetrics.splitsSummary = mergedSplits;
    }

    return {
      rawText: buildCleanRawTextDisplay(finalMetrics, spatialClean || spatialOrig),
      metrics: finalMetrics,
    };
  } finally {
    await worker.terminate();
  }
}
