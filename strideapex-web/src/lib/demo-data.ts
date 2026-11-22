import type { CloudBackupSnapshot, PersonalRecord, TrainingGoal, WorkoutLog } from "./types";

/**
 * Starter dummy data removed so the web app starts completely clean.
 */
export function createDemoSeedData(): {
  workouts: WorkoutLog[];
  prs: PersonalRecord[];
  goals: TrainingGoal[];
  backups: CloudBackupSnapshot[];
} {
  return {
    workouts: [],
    prs: [],
    goals: [],
    backups: [],
  };
}
