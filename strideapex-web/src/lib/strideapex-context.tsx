"use client";

import React, { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from "react";
import { onAuthStateChanged, signInWithPopup, signOut, type User } from "firebase/auth";
import {
  askBiomechanicsViaApi,
  downloadExportFile,
  extractStravaViaApi,
  fetchApiHealth,
  fetchComprehensiveAnalytics,
  fetchPlanAdviceViaApi,
  refreshWorkoutAiViaApi,
} from "./api-client";
import { createDemoSeedData } from "./demo-data";
import {
  getFirebaseAuth,
  getFirebaseDb,
  googleAuthProvider,
  isFirebaseConfigured,
} from "./firebase";
import {
  createCloudBackup,
  deleteCloudGoal,
  deleteCloudPersonalRecord,
  deleteCloudWorkout,
  derivePrsLocally,
  observeCloudBackups,
  observeCloudGoals,
  observeCloudPersonalRecords,
  observeCloudWorkouts,
  recalculateCloudPrsFromWorkouts,
  saveCloudGoal,
  saveCloudWorkout,
  updateCloudGoal,
  updateCloudWorkout,
} from "./firestore-repository";
import { encodeGoalTitleWithSchedule, formatPace } from "./formatters";
import { computeLocalAnalyticsFallback, extractLocalFallbackOcr } from "./local-analytics-fallback";
import type {
  BiomechanicsModule,
  CloudBackupSnapshot,
  ComprehensiveAnalyticsResponse,
  DistanceCategoryKey,
  ExtractedStravaMetrics,
  PersonalRecord,
  TrainingGoal,
  WorkoutLog,
} from "./types";

const LOCAL_STORAGE_KEY = "strideapex_web_local_state_v2";
const LEGACY_LOCAL_STORAGE_KEY = "strideapex_web_local_state_v1";
const THEME_STORAGE_KEY = "strideapex_web_theme_dark";

interface LocalStorageSnapshot {
  workouts: WorkoutLog[];
  prs: PersonalRecord[];
  goals: TrainingGoal[];
  backups: CloudBackupSnapshot[];
}

interface StrideApexContextValue {
  user: User | null;
  authLoading: boolean;
  isCloudMode: boolean;
  firebaseReady: boolean;
  isDark: boolean;
  toggleTheme: () => void;
  workouts: WorkoutLog[];
  prs: PersonalRecord[];
  goals: TrainingGoal[];
  backups: CloudBackupSnapshot[];
  analytics: ComprehensiveAnalyticsResponse;
  analyticsLoading: boolean;
  backendOnline: boolean;
  geminiConfigured: boolean;
  fatigueExponent: number;
  setFatigueExponent: (v: number) => void;
  statusBanner: string | null;
  clearStatusBanner: () => void;
  signInWithGoogle: () => Promise<void>;
  signOutToDemo: () => Promise<void>;
  resetDemoDataset: () => void;
  extractStravaOcr: (params: {
    imageBase64?: string;
    mimeType?: string;
    rawText?: string;
    presetIndex?: number;
  }) => Promise<ExtractedStravaMetrics>;
  saveWorkout: (input: {
    title: string;
    distanceKm: number;
    durationSeconds: number;
    avgHeartRate: number;
    cadenceSpm: number;
    elevationGainM: number;
    splitsSummary: string;
    aiCoachingInsight: string;
    sourceType: "STRAVA_OCR" | "MANUAL";
    createdAtEpochMs?: number;
  }) => Promise<string>;
  updateWorkout: (workout: WorkoutLog) => Promise<void>;
  deleteWorkout: (workoutId: string) => Promise<void>;
  refreshWorkoutAi: (workout: WorkoutLog) => Promise<void>;
  saveGoalSchedule: (params: {
    goalId?: string | null;
    cleanTitle: string;
    category: DistanceCategoryKey;
    targetTimeSeconds: number;
    weeklyMileageTargetKm: number;
    startDateKey: string;
    targetDateKey: string;
    trainingDaysIso: number[];
  }) => Promise<void>;
  toggleGoalCompleted: (goal: TrainingGoal) => Promise<void>;
  deleteGoal: (goalId: string) => Promise<void>;
  deletePr: (categoryKey: string) => Promise<void>;
  getPlanAdvice: (goal: TrainingGoal) => Promise<{ advice: string; sourceEngine: string }>;
  askBiomechanics: (question: string, module: BiomechanicsModule) => Promise<{ answer: string; sourceEngine: string }>;
  createBackupCheckpoint: (label: string) => Promise<void>;
  exportData: (format: "csv" | "json") => Promise<void>;
}

const StrideApexContext = createContext<StrideApexContextValue | null>(null);

export function StrideApexProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [authLoading, setAuthLoading] = useState(true);
  const [isDark, setIsDark] = useState(false); // Default Strava Light

  const [workouts, setWorkouts] = useState<WorkoutLog[]>([]);
  const [prs, setPrs] = useState<PersonalRecord[]>([]);
  const [goals, setGoals] = useState<TrainingGoal[]>([]);
  const [backups, setBackups] = useState<CloudBackupSnapshot[]>([]);

  const [fatigueExponent, setFatigueExponent] = useState(1.06);
  const [analytics, setAnalytics] = useState<ComprehensiveAnalyticsResponse>(() =>
    computeLocalAnalyticsFallback([], [], [], 1.06)
  );
  const [analyticsLoading, setAnalyticsLoading] = useState(false);
  const [backendOnline, setBackendOnline] = useState(false);
  const [geminiConfigured, setGeminiConfigured] = useState(false);
  const [statusBanner, setStatusBanner] = useState<string | null>(null);

  const syncedOfflineForUid = useRef<string | null>(null);
  const isCloudMode = Boolean(user && getFirebaseDb());

  const showBanner = useCallback((msg: string) => {
    setStatusBanner(msg);
  }, []);

  // Load theme preference
  useEffect(() => {
    const saved = window.localStorage.getItem(THEME_STORAGE_KEY);
    const dark = saved === "true";
    setIsDark(dark);
    document.documentElement.classList.toggle("dark", dark);
  }, []);

  const toggleTheme = useCallback(() => {
    setIsDark((prev) => {
      const next = !prev;
      document.documentElement.classList.toggle("dark", next);
      window.localStorage.setItem(THEME_STORAGE_KEY, String(next));
      return next;
    });
  }, []);

  // Helper to read/write local storage snapshot
  const loadLocalSnapshot = useCallback((): LocalStorageSnapshot => {
    try {
      window.localStorage.removeItem(LEGACY_LOCAL_STORAGE_KEY);
      const raw = window.localStorage.getItem(LOCAL_STORAGE_KEY);
      if (raw) {
        const parsed = JSON.parse(raw) as LocalStorageSnapshot;
        if (Array.isArray(parsed.workouts)) {
          return {
            workouts: parsed.workouts.filter((w) => !w.id.startsWith("demo_")),
            prs: (parsed.prs ?? []).filter((p) => p.userId !== "demo_runner"),
            goals: (parsed.goals ?? []).filter((g) => !g.id.startsWith("demo_")),
            backups: (parsed.backups ?? []).filter((b) => !b.id.startsWith("demo_")),
          };
        }
      }
    } catch {
      // ignore corrupted storage
    }
    const seed = createDemoSeedData();
    window.localStorage.setItem(LOCAL_STORAGE_KEY, JSON.stringify(seed));
    return seed;
  }, []);

  const persistLocalSnapshot = useCallback((next: LocalStorageSnapshot) => {
    try {
      window.localStorage.setItem(LOCAL_STORAGE_KEY, JSON.stringify(next));
    } catch {
      // ignore quota error
    }
  }, []);

  // Observe Firebase Auth
  useEffect(() => {
    const auth = getFirebaseAuth();
    if (!auth) {
      setWorkouts([]);
      setPrs([]);
      setGoals([]);
      setBackups([]);
      setAuthLoading(false);
      return;
    }

    const unsub = onAuthStateChanged(auth, (firebaseUser) => {
      setUser(firebaseUser);
      setAuthLoading(false);
    });
    return () => unsub();
  }, [loadLocalSnapshot]);

  // Subscribe to Firestore when signed in, or LocalStorage Demo data when signed out
  useEffect(() => {
    if (authLoading) return;
    const db = getFirebaseDb();

    if (!user || !db) {
      setWorkouts([]);
      setPrs([]);
      setGoals([]);
      setBackups([]);
      return;
    }

    let isMounted = true;

    // Auto-upload any locally created `local_*` records to Cloud when user connects
    const syncLocalToCloudOnce = async () => {
      if (syncedOfflineForUid.current === user.uid) return;
      syncedOfflineForUid.current = user.uid;
      try {
        const local = loadLocalSnapshot();
        const unsyncedWorkouts = local.workouts.filter((w) => w.id.startsWith("local_"));
        const unsyncedGoals = local.goals.filter((g) => g.id.startsWith("local_"));

        for (const w of unsyncedWorkouts) {
          await saveCloudWorkout(db, user.uid, { ...w, id: "" });
        }
        for (const g of unsyncedGoals) {
          await saveCloudGoal(db, user.uid, { ...g, id: "" });
        }

        if (unsyncedWorkouts.length > 0 || unsyncedGoals.length > 0) {
          const remaining: LocalStorageSnapshot = {
            ...local,
            workouts: local.workouts.filter((w) => !w.id.startsWith("local_")),
            goals: local.goals.filter((g) => !g.id.startsWith("local_")),
          };
          persistLocalSnapshot(remaining);
          if (isMounted) {
            showBanner(
              `Sinkronisasi selesai: ${unsyncedWorkouts.length} sesi & ${unsyncedGoals.length} target lokal diunggah ke Cloud.`
            );
          }
        }
      } catch (err) {
        console.warn("Cloud upload sync error:", err);
      }
    };

    void syncLocalToCloudOnce();

    const unsubW = observeCloudWorkouts(
      db,
      user.uid,
      (items) => {
        if (!isMounted) return;
        setWorkouts(items);
        // Mirror Cloud snapshot locally for seamless offline fallback
        const cur = loadLocalSnapshot();
        const unsyncedLocal = cur.workouts.filter((w) => w.id.startsWith("local_"));
        persistLocalSnapshot({ ...cur, workouts: [...unsyncedLocal, ...items] });
      },
      (err) => showBanner(`Firestore workouts error: ${err.message}`)
    );
    const unsubP = observeCloudPersonalRecords(db, user.uid, (items) => {
      if (!isMounted) return;
      setPrs(items);
    });
    const unsubG = observeCloudGoals(db, user.uid, (items) => {
      if (!isMounted) return;
      setGoals(items);
    });
    const unsubB = observeCloudBackups(db, user.uid, (items) => {
      if (!isMounted) return;
      setBackups(items);
    });

    return () => {
      isMounted = false;
      unsubW();
      unsubP();
      unsubG();
      unsubB();
    };
  }, [authLoading, user, loadLocalSnapshot, persistLocalSnapshot, showBanner]);

  // Health ping to .NET API on mount
  useEffect(() => {
    fetchApiHealth()
      .then((h) => {
        setBackendOnline(h.status === "ok");
        setGeminiConfigured(Boolean(h.geminiConfigured));
      })
      .catch(() => {
        setBackendOnline(false);
        setGeminiConfigured(false);
      });
  }, []);

  // Recompute Comprehensive Analytics via .NET 8 API whenever telemetry changes
  useEffect(() => {
    let active = true;
    const localComputed = computeLocalAnalyticsFallback(workouts, prs, goals, fatigueExponent);
    setAnalytics(localComputed);

    const runBackendAnalytics = async () => {
      setAnalyticsLoading(true);
      try {
        const res = await fetchComprehensiveAnalytics({
          workouts,
          prs,
          goals,
          fatigueExponent,
        });
        if (active) {
          setAnalytics(res);
          setBackendOnline(true);
        }
      } catch {
        if (active) {
          setBackendOnline(false);
        }
      } finally {
        if (active) setAnalyticsLoading(false);
      }
    };

    void runBackendAnalytics();
    return () => {
      active = false;
    };
  }, [workouts, prs, goals, fatigueExponent]);

  const signInWithGoogle = useCallback(async () => {
    const auth = getFirebaseAuth();
    if (!auth) {
      showBanner("Konfigurasi Firebase belum lengkap.");
      return;
    }
    try {
      await signInWithPopup(auth, googleAuthProvider);
      showBanner("Berhasil masuk dengan akun Google & tersinkron ke Firestore.");
    } catch (err) {
      const msg = err instanceof Error ? err.message : "Gagal masuk Google";
      if (msg.includes("auth/unauthorized-domain")) {
        const host = typeof window !== "undefined" ? window.location.hostname : "localhost";
        showBanner(
          `Domain '${host}' belum diizinkan di Firebase Authentication. Tambahkan '${host}' di Firebase Console → Authentication → Settings → Authorized domains.`
        );
      } else {
        showBanner(`Login Google dibatalkan/gagal: ${msg}`);
      }
    }
  }, [showBanner]);

  const signOutToDemo = useCallback(async () => {
    const auth = getFirebaseAuth();
    if (auth) {
      await signOut(auth);
      syncedOfflineForUid.current = null;
      setWorkouts([]);
      setPrs([]);
      setGoals([]);
      setBackups([]);
      showBanner("Anda telah keluar. Silakan masuk dengan Google untuk mengakses data.");
    }
  }, [showBanner]);

  const resetDemoDataset = useCallback(() => {
    const seed = createDemoSeedData();
    persistLocalSnapshot(seed);
    if (!isCloudMode) {
      setWorkouts(seed.workouts);
      setPrs(seed.prs);
      setGoals(seed.goals);
      setBackups(seed.backups);
      showBanner("Dataset Demo dikembalikan ke kondisi awal.");
    }
  }, [isCloudMode, persistLocalSnapshot, showBanner]);

  const extractStravaOcr = useCallback(
    async (params: {
      imageBase64?: string;
      mimeType?: string;
      rawText?: string;
      presetIndex?: number;
    }): Promise<ExtractedStravaMetrics> => {
      try {
        const res = await extractStravaViaApi(params);
        setBackendOnline(true);
        return res;
      } catch {
        setBackendOnline(false);
        return extractLocalFallbackOcr(params.rawText ?? "", params.presetIndex ?? 0);
      }
    },
    []
  );

  const saveWorkout = useCallback(
    async (input: {
      title: string;
      distanceKm: number;
      durationSeconds: number;
      avgHeartRate: number;
      cadenceSpm: number;
      elevationGainM: number;
      splitsSummary: string;
      aiCoachingInsight: string;
      sourceType: "STRAVA_OCR" | "MANUAL";
      createdAtEpochMs?: number;
    }): Promise<string> => {
      const safeDist = Math.min(500, Math.max(0.1, input.distanceKm));
      const safeDur = Math.min(864000, Math.max(1, Math.round(input.durationSeconds)));
      const avgPace = Math.min(3600, Math.max(60, Math.round(safeDur / safeDist)));
      const nowMs = input.createdAtEpochMs && input.createdAtEpochMs > 0 ? input.createdAtEpochMs : Date.now();
      const db = getFirebaseDb();

      const workout: WorkoutLog = {
        id: `local_wk_${crypto.randomUUID()}`,
        userId: user?.uid ?? "demo_runner",
        title: input.title.trim() || "Training Run",
        distanceKm: Number(safeDist.toFixed(2)),
        durationSeconds: safeDur,
        avgPaceSecondsPerKm: avgPace,
        avgHeartRate: Math.round(input.avgHeartRate),
        cadenceSpm: Math.round(input.cadenceSpm),
        elevationGainM: Math.round(input.elevationGainM),
        splitsSummary: input.splitsSummary.trim(),
        aiCoachingInsight: input.aiCoachingInsight.trim() || `Pace rata-rata ${formatPace(avgPace)}.`,
        sourceType: input.sourceType,
        createdAtEpochMs: nowMs,
        updatedAtEpochMs: nowMs,
      };

      if (user && db) {
        const cloudId = await saveCloudWorkout(db, user.uid, { ...workout, id: "" });
        const nextList = [{ ...workout, id: cloudId }, ...workouts];
        await recalculateCloudPrsFromWorkouts(db, user.uid, nextList);
        showBanner(`Sesi '${workout.title}' tersimpan ke Cloud Firestore.`);
        return cloudId;
      }

      const nextWorkouts = [workout, ...workouts];
      const nextPrs = derivePrsLocally(nextWorkouts);
      setWorkouts(nextWorkouts);
      setPrs(nextPrs);
      persistLocalSnapshot({ workouts: nextWorkouts, prs: nextPrs, goals, backups });
      showBanner(`Sesi '${workout.title}' disimpan ke penyimpanan lokal.`);
      return workout.id;
    },
    [user, workouts, goals, backups, persistLocalSnapshot, showBanner]
  );

  const updateWorkout = useCallback(
    async (updated: WorkoutLog) => {
      const safeDist = Math.min(500, Math.max(0.1, updated.distanceKm));
      const safeDur = Math.min(864000, Math.max(1, Math.round(updated.durationSeconds)));
      const avgPace = Math.min(3600, Math.max(60, Math.round(safeDur / safeDist)));

      let splitsSummary = updated.splitsSummary;
      let aiInsight = updated.aiCoachingInsight;
      try {
        const refreshed = await refreshWorkoutAiViaApi({ ...updated, avgPaceSecondsPerKm: avgPace }, prs);
        splitsSummary = refreshed.recalibratedSplits;
        aiInsight = refreshed.aiCoachingInsight;
      } catch {
        // retain provided values if API unavailable
      }

      const finalWk: WorkoutLog = {
        ...updated,
        distanceKm: Number(safeDist.toFixed(2)),
        durationSeconds: safeDur,
        avgPaceSecondsPerKm: avgPace,
        splitsSummary,
        aiCoachingInsight: aiInsight,
        updatedAtEpochMs: Date.now(),
      };

      const db = getFirebaseDb();
      const nextList = workouts.map((w) => (w.id === finalWk.id ? finalWk : w));

      if (user && db && !finalWk.id.startsWith("local_")) {
        await updateCloudWorkout(db, user.uid, finalWk);
        await recalculateCloudPrsFromWorkouts(db, user.uid, nextList);
        showBanner(`Riwayat '${finalWk.title}' & Split AI diperbarui di Cloud.`);
        return;
      }

      const nextPrs = derivePrsLocally(nextList);
      setWorkouts(nextList);
      setPrs(nextPrs);
      persistLocalSnapshot({ workouts: nextList, prs: nextPrs, goals, backups });
      showBanner(`Riwayat '${finalWk.title}' & Split diperbarui.`);
    },
    [user, workouts, prs, goals, backups, persistLocalSnapshot, showBanner]
  );

  const deleteWorkout = useCallback(
    async (workoutId: string) => {
      const db = getFirebaseDb();
      const nextList = workouts.filter((w) => w.id !== workoutId);

      if (user && db && !workoutId.startsWith("local_")) {
        await deleteCloudWorkout(db, user.uid, workoutId);
        await recalculateCloudPrsFromWorkouts(db, user.uid, nextList);
        showBanner("Sesi lari dihapus dari Cloud & PR diperbarui.");
        return;
      }

      const nextPrs = derivePrsLocally(nextList);
      setWorkouts(nextList);
      setPrs(nextPrs);
      persistLocalSnapshot({ workouts: nextList, prs: nextPrs, goals, backups });
      showBanner("Sesi lari dihapus.");
    },
    [user, workouts, goals, backups, persistLocalSnapshot, showBanner]
  );

  const refreshWorkoutAi = useCallback(
    async (workout: WorkoutLog) => {
      try {
        const refreshed = await refreshWorkoutAiViaApi(workout, prs);
        const updated: WorkoutLog = {
          ...workout,
          splitsSummary: refreshed.recalibratedSplits,
          aiCoachingInsight: refreshed.aiCoachingInsight,
          updatedAtEpochMs: Date.now(),
        };
        const db = getFirebaseDb();
        if (user && db && !updated.id.startsWith("local_")) {
          await updateCloudWorkout(db, user.uid, updated);
        } else {
          const next = workouts.map((w) => (w.id === updated.id ? updated : w));
          setWorkouts(next);
          persistLocalSnapshot({ workouts: next, prs, goals, backups });
        }
        showBanner(`Analisis AI & Split '${workout.title}' diperbarui (${refreshed.sourceEngine}).`);
      } catch {
        showBanner("Gagal menghubungi .NET AI Engine. Pastikan strideapex-api berjalan.");
      }
    },
    [user, workouts, prs, goals, backups, persistLocalSnapshot, showBanner]
  );

  const saveGoalSchedule = useCallback(
    async (params: {
      goalId?: string | null;
      cleanTitle: string;
      category: DistanceCategoryKey;
      targetTimeSeconds: number;
      weeklyMileageTargetKm: number;
      startDateKey: string;
      targetDateKey: string;
      trainingDaysIso: number[];
    }) => {
      const encodedTitle = encodeGoalTitleWithSchedule(
        params.cleanTitle,
        params.targetDateKey,
        params.trainingDaysIso,
        params.startDateKey
      );
      const db = getFirebaseDb();

      if (params.goalId) {
        const existing = goals.find((g) => g.id === params.goalId);
        if (existing) {
          const updated: TrainingGoal = {
            ...existing,
            title: encodedTitle,
            targetDistanceCategory: params.category,
            targetTimeSeconds: params.targetTimeSeconds,
            weeklyMileageTargetKm: params.weeklyMileageTargetKm,
          };
          if (user && db && !updated.id.startsWith("local_")) {
            await updateCloudGoal(db, user.uid, updated);
          } else {
            const nextGoals = goals.map((g) => (g.id === updated.id ? updated : g));
            setGoals(nextGoals);
            persistLocalSnapshot({ workouts, prs, goals: nextGoals, backups });
          }
          showBanner(`Target '${params.cleanTitle || "Target Lari"}' berhasil diperbarui.`);
          return;
        }
      }

      const newGoal: TrainingGoal = {
        id: `local_goal_${crypto.randomUUID()}`,
        userId: user?.uid ?? "demo_runner",
        title: encodedTitle,
        targetDistanceCategory: params.category,
        targetTimeSeconds: params.targetTimeSeconds,
        weeklyMileageTargetKm: params.weeklyMileageTargetKm,
        reminderEnabled: true,
        reminderHour: 7,
        reminderMinute: 0,
        completed: false,
        createdAtEpochMs: Date.now(),
      };

      if (user && db) {
        await saveCloudGoal(db, user.uid, { ...newGoal, id: "" });
        showBanner(`Target baru tersimpan ke Cloud Firestore.`);
      } else {
        const nextGoals = [newGoal, ...goals];
        setGoals(nextGoals);
        persistLocalSnapshot({ workouts, prs, goals: nextGoals, backups });
        showBanner("Target lari baru disimpan secara lokal.");
      }
    },
    [user, workouts, prs, goals, backups, persistLocalSnapshot, showBanner]
  );

  const toggleGoalCompleted = useCallback(
    async (goal: TrainingGoal) => {
      const updated = { ...goal, completed: !goal.completed };
      const db = getFirebaseDb();
      if (user && db && !updated.id.startsWith("local_")) {
        await updateCloudGoal(db, user.uid, updated);
      } else {
        const nextGoals = goals.map((g) => (g.id === updated.id ? updated : g));
        setGoals(nextGoals);
        persistLocalSnapshot({ workouts, prs, goals: nextGoals, backups });
      }
    },
    [user, workouts, prs, goals, backups, persistLocalSnapshot]
  );

  const deleteGoal = useCallback(
    async (goalId: string) => {
      const db = getFirebaseDb();
      if (user && db && !goalId.startsWith("local_")) {
        await deleteCloudGoal(db, user.uid, goalId);
      } else {
        const nextGoals = goals.filter((g) => g.id !== goalId);
        setGoals(nextGoals);
        persistLocalSnapshot({ workouts, prs, goals: nextGoals, backups });
      }
      showBanner("Target lari dihapus.");
    },
    [user, workouts, prs, goals, backups, persistLocalSnapshot, showBanner]
  );

  const deletePr = useCallback(
    async (categoryKey: string) => {
      const db = getFirebaseDb();
      if (user && db) {
        await deleteCloudPersonalRecord(db, user.uid, categoryKey);
      } else {
        const nextPrs = prs.filter((p) => p.distanceCategory !== categoryKey);
        setPrs(nextPrs);
        persistLocalSnapshot({ workouts, prs: nextPrs, goals, backups });
      }
      showBanner(`Rekor ${categoryKey} dihapus.`);
    },
    [user, workouts, prs, goals, backups, persistLocalSnapshot, showBanner]
  );

  const getPlanAdvice = useCallback(
    async (goal: TrainingGoal) => {
      try {
        return await fetchPlanAdviceViaApi(goal, workouts);
      } catch {
        return {
          advice:
            "AI Adaptive Engine: Jaga jarak minimal 48 jam antara sesi Interval/Tempo dan Long Run, serta terapkan tapering volume 60% pada H-7 menuju hari lomba.",
          sourceEngine: "LOCAL_FALLBACK",
        };
      }
    },
    [workouts]
  );

  const askBiomechanics = useCallback(async (question: string, module: BiomechanicsModule) => {
    try {
      return await askBiomechanicsViaApi(question, module);
    } catch {
      return {
        answer: `Fokus ${module.title}: Targetkan ${module.optimalTargetRange} dengan latihan ${module.drills.join(", ")}.`,
        sourceEngine: "LOCAL_FALLBACK",
      };
    }
  }, []);

  const createBackupCheckpoint = useCallback(
    async (label: string) => {
      const totalKm = workouts.reduce((s, w) => s + w.distanceKm, 0);
      const summaryNotes = `Total Volume: ${totalKm.toFixed(1)} km across ${workouts.length} workouts. Active Goals: ${goals.filter((g) => !g.completed).length}.`;
      const db = getFirebaseDb();
      if (user && db) {
        await createCloudBackup(db, user.uid, {
          backupLabel: label || "Web Cloud Checkpoint",
          workoutCount: workouts.length,
          prCount: prs.length,
          goalCount: goals.length,
          summaryNotes,
          createdAtEpochMs: Date.now(),
        });
        showBanner("Cloud backup checkpoint berhasil disimpan.");
      } else {
        const bkp: CloudBackupSnapshot = {
          id: `local_bkp_${crypto.randomUUID()}`,
          userId: "demo_runner",
          backupLabel: label || "Local Checkpoint",
          workoutCount: workouts.length,
          prCount: prs.length,
          goalCount: goals.length,
          summaryNotes,
          createdAtEpochMs: Date.now(),
        };
        const nextBackups = [bkp, ...backups];
        setBackups(nextBackups);
        persistLocalSnapshot({ workouts, prs, goals, backups: nextBackups });
        showBanner("Backup lokal disimpan.");
      }
    },
    [user, workouts, prs, goals, backups, persistLocalSnapshot, showBanner]
  );

  const exportData = useCallback(
    async (format: "csv" | "json") => {
      try {
        await downloadExportFile(format, { workouts, prs, goals });
        showBanner(`Berhasil mengunduh file ${format.toUpperCase()}.`);
      } catch {
        // Fallback client-side download
        const content =
          format === "json"
            ? JSON.stringify({ exportedAtUtc: new Date().toISOString(), workouts, prs, goals }, null, 2)
            : [
                "Id,Title,DistanceKm,DurationSec,AvgPaceSec,AvgHR,Cadence,ElevationM",
                ...workouts.map(
                  (w) =>
                    `"${w.id}","${w.title.replace(/"/g, '""')}",${w.distanceKm},${w.durationSeconds},${w.avgPaceSecondsPerKm},${w.avgHeartRate},${w.cadenceSpm},${w.elevationGainM}`
                ),
              ].join("\n");
        const blob = new Blob([content], { type: format === "csv" ? "text/csv" : "application/json" });
        const url = URL.createObjectURL(blob);
        const a = document.createElement("a");
        a.href = url;
        a.download = format === "csv" ? "strideapex-workouts.csv" : "strideapex-backup.json";
        a.click();
        URL.revokeObjectURL(url);
        showBanner(`Ekspor ${format.toUpperCase()} selesai (mode lokal).`);
      }
    },
    [workouts, prs, goals, showBanner]
  );

  const value = useMemo<StrideApexContextValue>(
    () => ({
      user,
      authLoading,
      isCloudMode,
      firebaseReady: isFirebaseConfigured,
      isDark,
      toggleTheme,
      workouts,
      prs,
      goals,
      backups,
      analytics,
      analyticsLoading,
      backendOnline,
      geminiConfigured,
      fatigueExponent,
      setFatigueExponent,
      statusBanner,
      clearStatusBanner: () => setStatusBanner(null),
      signInWithGoogle,
      signOutToDemo,
      resetDemoDataset,
      extractStravaOcr,
      saveWorkout,
      updateWorkout,
      deleteWorkout,
      refreshWorkoutAi,
      saveGoalSchedule,
      toggleGoalCompleted,
      deleteGoal,
      deletePr,
      getPlanAdvice,
      askBiomechanics,
      createBackupCheckpoint,
      exportData,
    }),
    [
      user,
      authLoading,
      isCloudMode,
      isDark,
      toggleTheme,
      workouts,
      prs,
      goals,
      backups,
      analytics,
      analyticsLoading,
      backendOnline,
      geminiConfigured,
      fatigueExponent,
      statusBanner,
      signInWithGoogle,
      signOutToDemo,
      resetDemoDataset,
      extractStravaOcr,
      saveWorkout,
      updateWorkout,
      deleteWorkout,
      refreshWorkoutAi,
      saveGoalSchedule,
      toggleGoalCompleted,
      deleteGoal,
      deletePr,
      getPlanAdvice,
      askBiomechanics,
      createBackupCheckpoint,
      exportData,
    ]
  );

  return <StrideApexContext.Provider value={value}>{children}</StrideApexContext.Provider>;
}

export function useStrideApex(): StrideApexContextValue {
  const ctx = useContext(StrideApexContext);
  if (!ctx) throw new Error("useStrideApex must be used inside StrideApexProvider");
  return ctx;
}
