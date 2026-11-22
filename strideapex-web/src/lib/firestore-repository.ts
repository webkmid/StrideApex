import {
  collection,
  deleteDoc,
  doc,
  getDocs,
  onSnapshot,
  serverTimestamp,
  setDoc,
  Timestamp,
  type Firestore,
  type Unsubscribe,
} from "firebase/firestore";
import { DISTANCE_CATEGORIES, getDistanceCategoryInfo, normalizeCategoryKey } from "./formatters";
import type {
  CloudBackupSnapshot,
  PersonalRecord,
  TrainingGoal,
  WorkoutLog,
} from "./types";

function clampTimestamp(epochMs?: number) {
  if (epochMs && epochMs > 0) {
    // firestore.rules enforces createdAt <= request.time
    const safeMs = Math.min(epochMs, Date.now() - 1000);
    return Timestamp.fromMillis(safeMs);
  }
  return serverTimestamp();
}

function toEpochMs(val: unknown): number {
  if (val && typeof val === "object" && "toMillis" in val && typeof (val as Timestamp).toMillis === "function") {
    return (val as Timestamp).toMillis();
  }
  if (typeof val === "number") return val;
  return Date.now();
}

export function observeCloudWorkouts(
  db: Firestore,
  uid: string,
  onData: (items: WorkoutLog[]) => void,
  onError?: (err: Error) => void
): Unsubscribe {
  return onSnapshot(
    collection(db, "users", uid, "workouts"),
    (snapshot) => {
      const items: WorkoutLog[] = snapshot.docs.map((d) => {
        const data = d.data();
        return {
          id: d.id,
          userId: String(data.userId ?? uid),
          title: String(data.title ?? "Training Run"),
          distanceKm: Number(data.distanceKm ?? 0),
          durationSeconds: Number(data.durationSeconds ?? 0),
          avgPaceSecondsPerKm: Number(data.avgPaceSecondsPerKm ?? 0),
          avgHeartRate: Number(data.avgHeartRate ?? 0),
          cadenceSpm: Number(data.cadenceSpm ?? 0),
          elevationGainM: Number(data.elevationGainM ?? 0),
          splitsSummary: String(data.splitsSummary ?? ""),
          aiCoachingInsight: String(data.aiCoachingInsight ?? ""),
          sourceType: data.sourceType === "STRAVA_OCR" ? "STRAVA_OCR" : "MANUAL",
          createdAtEpochMs: toEpochMs(data.createdAt),
          updatedAtEpochMs: toEpochMs(data.updatedAt ?? data.createdAt),
        };
      });
      items.sort((a, b) => b.createdAtEpochMs - a.createdAtEpochMs);
      onData(items);
    },
    (err) => onError?.(err)
  );
}

export function observeCloudPersonalRecords(
  db: Firestore,
  uid: string,
  onData: (items: PersonalRecord[]) => void,
  onError?: (err: Error) => void
): Unsubscribe {
  return onSnapshot(
    collection(db, "users", uid, "personalRecords"),
    (snapshot) => {
      const items: PersonalRecord[] = snapshot.docs.map((d) => {
        const data = d.data();
        return {
          id: d.id,
          userId: String(data.userId ?? uid),
          distanceCategory: normalizeCategoryKey(String(data.distanceCategory ?? "10K")),
          distanceMeters: Number(data.distanceMeters ?? 10000),
          bestTimeSeconds: Number(data.bestTimeSeconds ?? 0),
          previousTimeSeconds: Number(data.previousTimeSeconds ?? 0),
          paceSecondsPerKm: Number(data.paceSecondsPerKm ?? 0),
          workoutTitle: String(data.workoutTitle ?? "PR Effort"),
          createdAtEpochMs: toEpochMs(data.createdAt),
        };
      });
      items.sort((a, b) => a.distanceMeters - b.distanceMeters);
      onData(items);
    },
    (err) => onError?.(err)
  );
}

export function observeCloudGoals(
  db: Firestore,
  uid: string,
  onData: (items: TrainingGoal[]) => void,
  onError?: (err: Error) => void
): Unsubscribe {
  return onSnapshot(
    collection(db, "users", uid, "goals"),
    (snapshot) => {
      const items: TrainingGoal[] = snapshot.docs.map((d) => {
        const data = d.data();
        return {
          id: d.id,
          userId: String(data.userId ?? uid),
          title: String(data.title ?? "Target Lari"),
          targetDistanceCategory: normalizeCategoryKey(String(data.targetDistanceCategory ?? "10K")),
          targetTimeSeconds: Number(data.targetTimeSeconds ?? 2400),
          weeklyMileageTargetKm: Number(data.weeklyMileageTargetKm ?? 30),
          reminderEnabled: Boolean(data.reminderEnabled ?? true),
          reminderHour: Number(data.reminderHour ?? 7),
          reminderMinute: Number(data.reminderMinute ?? 0),
          completed: Boolean(data.completed ?? false),
          createdAtEpochMs: toEpochMs(data.createdAt),
        };
      });
      items.sort((a, b) => b.createdAtEpochMs - a.createdAtEpochMs);
      onData(items);
    },
    (err) => onError?.(err)
  );
}

export function observeCloudBackups(
  db: Firestore,
  uid: string,
  onData: (items: CloudBackupSnapshot[]) => void,
  onError?: (err: Error) => void
): Unsubscribe {
  return onSnapshot(
    collection(db, "users", uid, "backups"),
    (snapshot) => {
      const items: CloudBackupSnapshot[] = snapshot.docs.map((d) => {
        const data = d.data();
        return {
          id: d.id,
          userId: String(data.userId ?? uid),
          backupLabel: String(data.backupLabel ?? "Cloud Snapshot"),
          workoutCount: Number(data.workoutCount ?? 0),
          prCount: Number(data.prCount ?? 0),
          goalCount: Number(data.goalCount ?? 0),
          summaryNotes: String(data.summaryNotes ?? ""),
          createdAtEpochMs: toEpochMs(data.createdAt),
        };
      });
      items.sort((a, b) => b.createdAtEpochMs - a.createdAtEpochMs);
      onData(items);
    },
    (err) => onError?.(err)
  );
}

export async function saveCloudWorkout(db: Firestore, uid: string, workout: WorkoutLog): Promise<string> {
  const col = collection(db, "users", uid, "workouts");
  const docRef = workout.id && !workout.id.startsWith("local_") ? doc(col, workout.id) : doc(col);
  const safeDist = Math.min(500, Math.max(0.1, Number(workout.distanceKm) || 0.1));
  const safeDur = Math.min(864000, Math.max(1, Math.round(workout.durationSeconds) || 1));
  const safePace = Math.min(3600, Math.max(60, Math.round(workout.avgPaceSecondsPerKm || safeDur / safeDist)));

  const payload = {
    userId: uid,
    title: (workout.title || "Training Run").trim().slice(0, 120) || "Training Run",
    distanceKm: Number(safeDist.toFixed(2)),
    durationSeconds: safeDur,
    avgPaceSecondsPerKm: safePace,
    avgHeartRate: Math.min(260, Math.max(0, Math.round(workout.avgHeartRate || 0))),
    cadenceSpm: Math.min(300, Math.max(0, Math.round(workout.cadenceSpm || 0))),
    elevationGainM: Math.min(20000, Math.max(0, Math.round(workout.elevationGainM || 0))),
    splitsSummary: (workout.splitsSummary || "").slice(0, 1000),
    aiCoachingInsight: (workout.aiCoachingInsight || "").slice(0, 2000),
    sourceType: workout.sourceType === "STRAVA_OCR" ? "STRAVA_OCR" : "MANUAL",
    createdAt: clampTimestamp(workout.createdAtEpochMs),
    updatedAt: serverTimestamp(),
  };

  await setDoc(docRef, payload);
  return docRef.id;
}

export async function updateCloudWorkout(db: Firestore, uid: string, workout: WorkoutLog): Promise<void> {
  const docRef = doc(db, "users", uid, "workouts", workout.id);
  const safeDist = Math.min(500, Math.max(0.1, Number(workout.distanceKm) || 0.1));
  const safeDur = Math.min(864000, Math.max(1, Math.round(workout.durationSeconds) || 1));
  const safePace = Math.min(3600, Math.max(60, Math.round(workout.avgPaceSecondsPerKm || safeDur / safeDist)));

  await setDoc(
    docRef,
    {
      title: (workout.title || "Training Run").trim().slice(0, 120) || "Training Run",
      distanceKm: Number(safeDist.toFixed(2)),
      durationSeconds: safeDur,
      avgPaceSecondsPerKm: safePace,
      avgHeartRate: Math.min(260, Math.max(0, Math.round(workout.avgHeartRate || 0))),
      cadenceSpm: Math.min(300, Math.max(0, Math.round(workout.cadenceSpm || 0))),
      elevationGainM: Math.min(20000, Math.max(0, Math.round(workout.elevationGainM || 0))),
      splitsSummary: (workout.splitsSummary || "").slice(0, 1000),
      aiCoachingInsight: (workout.aiCoachingInsight || "").slice(0, 2000),
      sourceType: workout.sourceType === "STRAVA_OCR" ? "STRAVA_OCR" : "MANUAL",
      createdAt: clampTimestamp(workout.createdAtEpochMs),
      updatedAt: serverTimestamp(),
    },
    { merge: true }
  );
}

export async function deleteCloudWorkout(db: Firestore, uid: string, workoutId: string): Promise<void> {
  await deleteDoc(doc(db, "users", uid, "workouts", workoutId));
}

export async function saveCloudGoal(db: Firestore, uid: string, goal: TrainingGoal): Promise<string> {
  const col = collection(db, "users", uid, "goals");
  const docRef = goal.id && !goal.id.startsWith("local_") ? doc(col, goal.id) : doc(col);
  const cat = getDistanceCategoryInfo(goal.targetDistanceCategory);

  await setDoc(docRef, {
    userId: uid,
    title: (goal.title || "Target Lari").trim().slice(0, 120) || "Target Lari",
    targetDistanceCategory: cat.key,
    targetTimeSeconds: Math.min(864000, Math.max(1, Math.round(goal.targetTimeSeconds))),
    weeklyMileageTargetKm: Math.min(400, Math.max(1, Math.round(goal.weeklyMileageTargetKm))),
    reminderEnabled: Boolean(goal.reminderEnabled),
    reminderHour: Math.min(23, Math.max(0, Math.round(goal.reminderHour))),
    reminderMinute: Math.min(59, Math.max(0, Math.round(goal.reminderMinute))),
    completed: Boolean(goal.completed),
    createdAt: clampTimestamp(goal.createdAtEpochMs),
    updatedAt: serverTimestamp(),
  });

  return docRef.id;
}

export async function updateCloudGoal(db: Firestore, uid: string, goal: TrainingGoal): Promise<void> {
  const docRef = doc(db, "users", uid, "goals", goal.id);
  const cat = getDistanceCategoryInfo(goal.targetDistanceCategory);

  await setDoc(
    docRef,
    {
      title: (goal.title || "Target Lari").trim().slice(0, 120) || "Target Lari",
      targetDistanceCategory: cat.key,
      targetTimeSeconds: Math.min(864000, Math.max(1, Math.round(goal.targetTimeSeconds))),
      weeklyMileageTargetKm: Math.min(400, Math.max(1, Math.round(goal.weeklyMileageTargetKm))),
      reminderEnabled: Boolean(goal.reminderEnabled),
      reminderHour: Math.min(23, Math.max(0, Math.round(goal.reminderHour))),
      reminderMinute: Math.min(59, Math.max(0, Math.round(goal.reminderMinute))),
      completed: Boolean(goal.completed),
      updatedAt: serverTimestamp(),
    },
    { merge: true }
  );
}

export async function deleteCloudGoal(db: Firestore, uid: string, goalId: string): Promise<void> {
  await deleteDoc(doc(db, "users", uid, "goals", goalId));
}

export async function deleteCloudPersonalRecord(db: Firestore, uid: string, categoryKey: string): Promise<void> {
  const docId = `pr_${categoryKey.toLowerCase()}`;
  await deleteDoc(doc(db, "users", uid, "personalRecords", docId));
}

export async function createCloudBackup(
  db: Firestore,
  uid: string,
  backup: Omit<CloudBackupSnapshot, "id" | "userId">
): Promise<string> {
  const col = collection(db, "users", uid, "backups");
  const docRef = doc(col);
  await setDoc(docRef, {
    userId: uid,
    backupLabel: (backup.backupLabel || "Web Cloud Checkpoint").trim().slice(0, 120),
    workoutCount: Math.min(10000, Math.max(0, Math.round(backup.workoutCount))),
    prCount: Math.min(1000, Math.max(0, Math.round(backup.prCount))),
    goalCount: Math.min(1000, Math.max(0, Math.round(backup.goalCount))),
    summaryNotes: (backup.summaryNotes || "").slice(0, 4000),
    createdAt: clampTimestamp(backup.createdAtEpochMs),
    updatedAt: serverTimestamp(),
  });
  return docRef.id;
}

/**
 * Mirrors StrideApexViewModel.recalculatePrsFromRemainingWorkouts:
 * Qualifies runs with distance >= category.kilometers * 0.85 and updates /personalRecords 1:1.
 */
export async function recalculateCloudPrsFromWorkouts(
  db: Firestore,
  uid: string,
  workouts: WorkoutLog[]
): Promise<void> {
  const prCol = collection(db, "users", uid, "personalRecords");
  const existingSnap = await getDocs(prCol);
  const existingById = new Map(existingSnap.docs.map((d) => [d.id, d.data()]));

  for (const cat of DISTANCE_CATEGORIES) {
    const docId = `pr_${cat.key.toLowerCase()}`;
    const qualifying = workouts.filter((w) => w.distanceKm >= cat.kilometers * 0.85);

    if (qualifying.length === 0) {
      if (existingById.has(docId)) {
        await deleteDoc(doc(prCol, docId));
      }
      continue;
    }

    const best = qualifying.reduce((prev, curr) => {
      const prevPace = prev.avgPaceSecondsPerKm > 0 ? prev.avgPaceSecondsPerKm : prev.durationSeconds / prev.distanceKm;
      const currPace = curr.avgPaceSecondsPerKm > 0 ? curr.avgPaceSecondsPerKm : curr.durationSeconds / curr.distanceKm;
      return currPace < prevPace ? curr : prev;
    });

    const safePace = Math.min(
      3600,
      Math.max(60, Math.round(best.avgPaceSecondsPerKm || best.durationSeconds / Math.max(0.1, best.distanceKm)))
    );
    const normalizedBest = Math.max(1, Math.round(safePace * cat.kilometers));
    const prevRecord = existingById.get(docId);
    const prevBest = prevRecord ? Number(prevRecord.bestTimeSeconds ?? 0) : 0;
    const previousTime = prevBest > 0 && prevBest !== normalizedBest ? prevBest : 0;

    if (prevRecord) {
      await setDoc(
        doc(prCol, docId),
        {
          distanceCategory: cat.key,
          distanceMeters: cat.meters,
          bestTimeSeconds: normalizedBest,
          previousTimeSeconds: previousTime,
          paceSecondsPerKm: safePace,
          workoutTitle: (best.title || "PR Effort").slice(0, 120),
          updatedAt: serverTimestamp(),
        },
        { merge: true }
      );
    } else {
      await setDoc(doc(prCol, docId), {
        userId: uid,
        distanceCategory: cat.key,
        distanceMeters: cat.meters,
        bestTimeSeconds: normalizedBest,
        previousTimeSeconds: 0,
        paceSecondsPerKm: safePace,
        workoutTitle: (best.title || "PR Effort").slice(0, 120),
        createdAt: serverTimestamp(),
        updatedAt: serverTimestamp(),
      });
    }
  }
}

export function derivePrsLocally(workouts: WorkoutLog[], userId = "demo_runner"): PersonalRecord[] {
  const prs: PersonalRecord[] = [];
  for (const cat of DISTANCE_CATEGORIES) {
    const qualifying = workouts.filter((w) => w.distanceKm >= cat.kilometers * 0.85);
    if (qualifying.length === 0) continue;
    const best = qualifying.reduce((prev, curr) =>
      curr.avgPaceSecondsPerKm < prev.avgPaceSecondsPerKm ? curr : prev
    );
    const pace = Math.max(60, Math.round(best.avgPaceSecondsPerKm || best.durationSeconds / best.distanceKm));
    prs.push({
      id: `pr_${cat.key.toLowerCase()}`,
      userId,
      distanceCategory: cat.key,
      distanceMeters: cat.meters,
      bestTimeSeconds: Math.round(pace * cat.kilometers),
      previousTimeSeconds: 0,
      paceSecondsPerKm: pace,
      workoutTitle: best.title,
      createdAtEpochMs: best.createdAtEpochMs,
    });
  }
  return prs;
}
