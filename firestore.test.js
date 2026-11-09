const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function validWorkoutPayload(userId) {
  const now = new Date(Date.now() - 1000);
  return {
    userId,
    title: "Morning Tempo 10K",
    distanceKm: 10.0,
    durationSeconds: 2460,
    avgPaceSecondsPerKm: 246,
    avgHeartRate: 168,
    cadenceSpm: 182,
    elevationGainM: 84,
    splitsSummary: "1km: 4:08 | 5km: 20:30 | 10km: 41:00",
    aiCoachingInsight: "Strong negative split with optimal 182 SPM cadence.",
    sourceType: "STRAVA_OCR",
    createdAt: now,
    updatedAt: now,
  };
}

function validPrPayload(userId) {
  const now = new Date(Date.now() - 1000);
  return {
    userId,
    distanceCategory: "10K",
    distanceMeters: 10000,
    bestTimeSeconds: 2460,
    previousTimeSeconds: 2520,
    paceSecondsPerKm: 246,
    workoutTitle: "Morning Tempo 10K",
    createdAt: now,
    updatedAt: now,
  };
}

function validGoalPayload(userId) {
  const now = new Date(Date.now() - 1000);
  return {
    userId,
    title: "Sub-40 10K Championship",
    targetDistanceCategory: "10K",
    targetTimeSeconds: 2399,
    weeklyMileageTargetKm: 65,
    reminderEnabled: true,
    reminderHour: 7,
    reminderMinute: 30,
    completed: false,
    createdAt: now,
    updatedAt: now,
  };
}

function validBackupPayload(userId) {
  const now = new Date(Date.now() - 1000);
  return {
    userId,
    backupLabel: "Pre-Marathon Block Checkpoint",
    workoutCount: 14,
    prCount: 4,
    goalCount: 2,
    summaryNotes: "Full cloud sync snapshot verified.",
    createdAt: now,
    updatedAt: now,
  };
}

test("Unauthenticated user: cannot read or create workouts", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("workouts").get()
  );
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("workouts")
      .doc("w1")
      .set(validWorkoutPayload(ALICE_UID))
  );
});

test("Authenticated owner: can create, read, list, update, and delete own workout", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const docRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("workouts")
    .doc("w_valid_1");

  await assertSucceeds(docRef.set(validWorkoutPayload(ALICE_UID)));
  await assertSucceeds(docRef.get());
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("workouts").get()
  );
  await assertSucceeds(
    docRef.update({
      title: "Updated Tempo 10K",
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertSucceeds(docRef.delete());
});

test("Cross-user isolation: Bob cannot read, list, or modify Alice's workouts", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context
      .firestore()
      .collection("users")
      .doc(ALICE_UID)
      .collection("workouts")
      .doc("w_alice")
      .set(validWorkoutPayload(ALICE_UID));
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const aliceWorkoutRef = bobDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("workouts")
    .doc("w_alice");

  await assertFails(aliceWorkoutRef.get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("workouts").get()
  );
  await assertFails(aliceWorkoutRef.delete());
});

test("Shadow update & schema validation: rejects ghost fields and invalid types", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const docRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("workouts")
    .doc("w_shadow");

  // Rejects extra ghost field on create
  await assertFails(
    docRef.set({
      ...validWorkoutPayload(ALICE_UID),
      isAdminOverride: true,
    })
  );

  // Create valid, then attempt value poisoning on update
  await assertSucceeds(docRef.set(validWorkoutPayload(ALICE_UID)));
  await assertFails(
    docRef.update({
      distanceKm: -15,
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("PersonalRecords, Goals, and Backups enforce owner access and schema rules", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  const prRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("personalRecords")
    .doc("pr_10k");
  await assertSucceeds(prRef.set(validPrPayload(ALICE_UID)));
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("personalRecords")
      .doc("pr_10k")
      .get()
  );

  const goalRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("goals")
    .doc("goal_1");
  await assertSucceeds(goalRef.set(validGoalPayload(ALICE_UID)));
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("goals")
      .doc("goal_1")
      .get()
  );

  const backupRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("backups")
    .doc("bkp_1");
  await assertSucceeds(backupRef.set(validBackupPayload(ALICE_UID)));
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("backups")
      .doc("bkp_1")
      .get()
  );
});
