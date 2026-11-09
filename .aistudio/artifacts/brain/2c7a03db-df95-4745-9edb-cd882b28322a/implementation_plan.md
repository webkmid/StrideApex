# StrideApex — Athletic Performance & Biomechanics Analyzer

StrideApex is a dark-mode running performance analyzer and training intelligence platform that turns Strava workout screenshots and run logs into algorithmic race predictions (5K, 10K, Half Marathon, Full Marathon), automated Personal Record (PR) tracking, goal push reminders, and interactive biomechanical form tutorials—synced seamlessly across devices in the cloud.

### User Review & Critical Decisions

> [!IMPORTANT]
> Please review the confirmed product decisions below before we begin provisioning Firebase and building the application.

- **Confirmed Decision — Cloud Sync & Backup Architecture**: Training workouts, PRs, and race goals will sync in real time across devices using **Firebase Firestore** (with on-device offline persistence caching enabled) gated by **Google Sign-In** (`CredentialManager`), paired with a dedicated Cloud Backup & Snapshot Export/Restore center so users have complete ownership and durability of their training history.
- **Confirmed Decision — Running Discipline Focus**: Algorithmic race predictions, VDOT training zones, PR tracking, and biomechanical tutorials are tailored specifically for road and distance runners targeting **5K, 10K, Half Marathon (21.1K), and Full Marathon (42.2K)** performance.
- **Confirmed Decision — AI Multimodal Strava Screenshot Logging**: Workout screenshot ingestion uses **Firebase AI Logic (`gemini-3.8-flash`)** with structured JSON schema extraction to read distance, moving time, pace, heart rate, cadence, elevation gain, split breakdowns, and coaching insights directly from screenshots, alongside a built-in synthetic Strava screenshot generator so you can test OCR extraction immediately on the emulator.
- **Platform Runtime Note**: Because this environment targets native Android, StrideApex is built directly in **Kotlin and Jetpack Compose** (Material 3) rather than Flutter, delivering true native GPU canvas rendering, zero-bridge camera/photo picker access, and native Android notification scheduling.

---

### 1. Overview & Core Concept

- **What It Does**: StrideApex centralizes a runner's training data by extracting structured telemetry from Strava workout screenshots or manual run logs, computing scientifically grounded race finish predictions and training pace zones, tracking distance PRs over time, triggering proactive goal push reminders, and guiding runners through interactive biomechanical form tutorials.
- **Target Audience / Persona**: Competitive and goal-oriented distance runners (5K to Marathon) who want deeper physiological insights, race-day pacing projections, and biomechanical form correction in a distraction-free, high-contrast dark telemetry interface.
- **Key Value**: Eliminates manual spreadsheet logging by combining instant AI screenshot OCR with Riegel/VDOT physiological race modeling, automated PR milestone detection, and interactive kinematic form coaching synced safely to the cloud.

---

### 2. User Experience & Visual Design

- **Key User Flows**:
  1. **Authentication & Cloud Sync Onboarding**: Users sign in with Google via the native Android Credential Manager bottom sheet; existing workouts, PRs, goals, and backup snapshots hydrate immediately in real time.
  2. **Dark Mode Telemetry Dashboard**: Displays weekly mileage progress, current VDOT fitness score, recent workouts, upcoming goal countdown, and quick-action launchers for Screenshot OCR Scan and Cloud Backup status.
  3. **Strava Screenshot AI Scanner & Logger**: Users select a workout screenshot via the zero-permission Android Photo Picker (or generate a realistic sample Strava run card on-device for instant testing). Gemini multimodal analysis extracts distance, duration, average pace, heart rate, cadence, elevation, splits, and a personalized coaching takeaway into an editable review sheet before saving to Firestore and checking for new PRs.
  4. **Algorithmic Race Predictor & Pace Calculator**: Dynamically computes predicted finish times and target paces for 5K, 10K, Half Marathon, and Full Marathon using Riegel's endurance power-law formula ($T_2 = T_1 \times (D_2 / D_1)^{1.06}$ adjusted by weekly mileage durability) and Jack Daniels' VDOT model, plus interactive fatigue-exponent sliders and training pace zones (Easy, Marathon, Threshold, Interval, Repetition).
  5. **PR Vault & Goal Push Reminders**: Tracks all-time personal bests across key distances with improvement deltas, lets runners set target race goals and weekly mileage targets, and dispatches native Android push notifications (`NotificationManager`) plus in-app motivational pacing nudges.
  6. **Interactive Biomechanical Video & Kinematic Tutorials**: A dedicated biomechanics lab featuring a synchronized, multi-angle kinematic runner animation & interactive tutorial player covering **Midfoot Strike Mechanics**, **180 SPM Cadence Optimization**, **Forward Lean from the Ankles**, **Pelvic Neutrality & Core Engagement**, and **Compact Arm Swing**. Users can scrub frame-by-frame, toggle playback speeds (`0.25x`, `0.5x`, `1.0x`), inspect joint-angle vector overlays, check off corrective drills, and ask the AI Biomechanics Coach specific form questions.

- **Visual Identity & Theme**:
  - *Aesthetic Direction*: **Obsidian Athletic Telemetry** — a high-contrast, precision dark-mode cockpit inspired by carbon-plated racing gear and physiological lab instrumentation.
  - *Color Palette & Mood*:
    - `--background`: Deep Carbon Obsidian (`#0A0D12`)
    - `--surface`: Elevated Slate Telemetry Card (`#121721`) with subtle `1dp` border strokes (`#1F2937`)
    - `--primary`: Electric Volt Lime (`#CCFF00`) for primary actions, active pace zones, and PR badges
    - `--secondary`: Strava-Inspired Kinetic Coral (`#FF5526`) for heart-rate zones, threshold effort, and OCR scan highlights
    - `--accent`: Aero Cyan (`#00E5FF`) for biomechanical joint vectors, cadence metrics, and cloud sync indicators
  - *Typography & Hierarchy*:
    - **Display / Headers**: `Space Grotesk` (bundled locally via `font-util`) for bold, geometric athletic headlines and section titles.
    - **Body / UI Labels**: `Inter` for crisp legibility across coaching insights and drill instructions.
    - **Monospace Telemetry**: `JetBrains Mono` for tabular alignment of pace splits (`min/km`), finish times (`HH:MM:SS`), heart rate (`BPM`), and VDOT indices.
  - *Component Styling & Layout*: Edge-to-edge layout respecting system status and navigation bar insets; asymmetric hero telemetry cards with custom `Canvas` sparklines, radial gauge arcs, and glowing pill indicators.

- **Interactive Feedback & Motion**:
  - Smooth spring-physics transitions between tabs and detail sheets.
  - Scanning laser-line shimmer animation over Strava screenshots while Gemini multimodal extraction is in progress.
  - Interactive touch scrubbing on biomechanical kinematic timelines and race predictor fatigue curves.

---

### 3. Key Product Decisions & Trade-Offs

- **Decision 1: Cloud-First Persistence with Local Offline Caching & Snapshot Backups**
  - *Chosen Approach*: Use **Firebase Firestore** (with local disk persistence enabled) scoped per authenticated Google user (`/users/{userId}/...`), supplemented by a dedicated **Cloud Backup & JSON Export/Import Manager** that captures full training state snapshots in a `backups` collection and allows local JSON export/restore.
  - *Why*: Satisfies both instant offline read/write responsiveness on the device and seamless multi-device cloud synchronization without dual-database synchronization conflicts.
  - *Alternatives Considered*: Separate local SQLite + manual sync scripts, which risk split-brain merge conflicts when switching devices.

- **Decision 2: Structured Multimodal AI Extraction (`gemini-3.8-flash` + `Schema.obj`)**
  - *Chosen Approach*: Route Strava screenshot bitmaps through Firebase AI Logic using `gemini-3.8-flash` configured with `responseMimeType = "application/json"` and a strict `responseSchema` (`Schema.obj`), paired with an editable verification step and an on-device sample Strava screenshot generator.
  - *Why*: Raw OCR struggles with multi-column Strava layouts (pace vs. elevation vs. heart rate in varying orders). Multimodal Gemini understands visual hierarchy AND simultaneously generates actionable physiological coaching analysis, while the editable confirmation form guarantees 100% data accuracy before saving.
  - *Alternatives Considered*: Raw regex over OCR lines, which breaks when screenshots come from different Strava activity views (overview vs. splits tab).

- **Decision 3: Interactive Kinematic Canvas + Video Player for Biomechanical Tutorials**
  - *Chosen Approach*: Combine a real-time, frame-scrubbable **Compose Kinematic Biomechanics Visualizer** (rendering articulated joint angles, ground-reaction force vectors, tibial inclination, and center-of-mass trajectory at `0.25x`–`1.0x` speeds) with structured coaching cues, interactive drill checklists, and AI form analysis.
  - *Why*: Gives runners interactive control over slow-motion phase inspection (Initial Contact, Mid-Stance, Toe-Off, Float Phase) and angle measurements that work reliably offline and on streaming emulators without external video CDN buffering failures.

---

### 4. Technical Architecture & Data Strategy *(Technical Reference)*

- **Architecture & Component Diagram**:

```
┌───────────────────────────────────────────────────────────────────────────┐
│                   MainActivity (Edge-to-Edge + App Check)                 │
│     configureAppCheck(this, intent) -> AuthGate (Google Sign-In Flow)     │
└─────────────────────────────────────┬─────────────────────────────────────┘
                                      │ Authenticated UID
                                      ▼
┌───────────────────────────────────────────────────────────────────────────┐
│                 StrideApexViewModel (MVVM State Coordinator)              │
│  ┌─────────────────┬──────────────────┬─────────────────┬──────────────┐  │
│  │ Dashboard &     │ Strava AI OCR    │ Race Predictor  │ Biomechanics │  │
│  │ Cloud Backup UI │ Scanner & Logger │ & PR / Goals UI │ Video Lab UI │  │
│  └────────┬────────┴────────┬─────────┴────────┬────────┴──────┬───────┘  │
└───────────┼─────────────────┼──────────────────┼───────────────┼──────────┘
            │                 │                  │               │
            ▼                 ▼                  ▼               ▼
┌────────────────────┐ ┌────────────────┐ ┌───────────────┐ ┌───────────────┐
│ TrainingRepository │ │ AiOcrService   │ │ RacePredictor │ │ Notification  │
│ (Firestore Custom  │ │ (Firebase.ai   │ │ Engine (VDOT  │ │ Scheduler     │
│  DB ID + Snapshots │ │  gemini-3.8-   │ │ & Riegel      │ │ (Goal Push    │
│  + Cloud Backups)  │ │  flash Schema) │ │ Algorithms)   │ │  Reminders)   │
└────────────────────┘ └────────────────┘ └───────────────┘ └───────────────┘
```

- **Data Model & State**:
  - `WorkoutLog` (`/users/{userId}/workouts/{workoutId}`): `userId`, `title`, `distanceKm`, `durationSeconds`, `avgPaceSecondsPerKm`, `avgHeartRate`, `cadenceSpm`, `elevationGainM`, `splitsSummary`, `aiCoachingInsight`, `sourceType` (`STRAVA_OCR` or `MANUAL`), `recordedAt`, `createdAt`, `updatedAt`.
  - `PersonalRecord` (`/users/{userId}/personalRecords/{prId}`): `userId`, `distanceCategory` (`5K`, `10K`, `HALF_MARATHON`, `MARATHON`), `distanceMeters`, `bestTimeSeconds`, `previousTimeSeconds`, `paceSecondsPerKm`, `workoutTitle`, `achievedAt`, `createdAt`, `updatedAt`.
  - `TrainingGoal` (`/users/{userId}/goals/{goalId}`): `userId`, `title`, `targetDistanceCategory`, `targetTimeSeconds`, `targetDateEpochMs`, `weeklyMileageTargetKm`, `reminderEnabled`, `reminderHour`, `reminderMinute`, `isCompleted`, `createdAt`, `updatedAt`.
  - `CloudBackupSnapshot` (`/users/{userId}/backups/{backupId}`): `userId`, `backupLabel`, `workoutCount`, `prCount`, `goalCount`, `payloadJson`, `createdAt`, `updatedAt`.

- **Interactive Component & State Mapping**:
  - **Auth Gate (`AuthScreen`)**: Executes silent `GetGoogleIdOption` auto-sign-in on launch and renders an interactive "Sign in with Google" button (`GetSignInWithGoogleOption`). Unauthenticated state blocks Firestore queries cleanly.
  - **Dashboard Tab**: Observes `workouts`, `personalRecords`, `goals`, and `backups` via real-time `snapshots()` Flows; displays weekly volume bar chart, VDOT readiness gauge, quick-scan button, and one-tap Cloud Backup creation/restoration.
  - **Strava OCR Scanner Modal / Tab**: Launches `ActivityResultContracts.PickVisualMedia` (or generates a synthetic high-DPI Strava summary bitmap on demand), sends the bitmap to `gemini-3.8-flash` with `Schema.obj`, populates editable fields (`distanceKm`, `duration`, `heartRate`, `cadence`, `elevation`, `splits`, `aiCoachingInsight`), saves the workout to Firestore, and automatically updates or creates a `PersonalRecord` if the time beats the existing PR for that distance bucket.
  - **Predict & PRs Tab**: Interactive race distance selector (`5K`, `10K`, `Half Marathon`, `Full Marathon`), Riegel fatigue factor slider (`1.03` speed-endurance to `1.10` novice endurance), live VDOT pace zone breakdown table, PR trophy cards with delta badges, and Goal creation/reminder toggle (`ActivityResultContracts.RequestPermission(Manifest.permission.POST_NOTIFICATIONS)` + instant test push notification button).
  - **Biomechanics Lab Tab**: Module selector across 5 running biomechanics lessons; interactive kinematic runner canvas with Play/Pause, speed selector (`0.25x`, `0.5x`, `1.0x`), gait-phase scrubber (`0%`–`100%` gait cycle), toggleable vector overlays (Joint Angles, Ground Reaction Force, Center of Mass), interactive drill completion toggles, and AI Form Coach Q&A drawer.

- **Key Sequences & Integrations**:
  - **Firebase Provisioning & Zero-Trust Rules**: Provision `"firestore"` and `"ai_logic"` via `firebase-integration-android-rpc`, wire `configureAppCheck(this, intent)` in `MainActivity.onCreate()` before `setContent`, author `firebase-blueprint.json` and hardened `firestore.rules` validating ownership (`request.auth.uid == userId`) and schema constraints for all 4 subcollections (`workouts`, `personalRecords`, `goals`, `backups`), validate rules against local emulators via `firestore.test.js` and Robolectric `*RuleTest.kt`, and deploy rules.
