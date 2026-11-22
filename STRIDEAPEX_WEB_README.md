# StrideApex Web (Next.js) + Backend Analytics (.NET 8)

Proyek web pendamping **StrideApex** yang berjalan di browser dengan fitur dan skema data yang sinkron langsung 1:1 dengan aplikasi **StrideApex Android** (Firebase Auth & named Cloud Firestore), dilengkapi analitik fisiologi dan beban latihan tingkat lanjut di backend `.NET 8`.

## Arsitektur Proyek

1. **`strideapex-api/` (.NET 8 ASP.NET Core Web API — `http://localhost:5180`)**
   - Mengimplementasikan engine domain StrideApex secara utuh: `RacePredictionService` (Jack Daniels VDOT & Riegel), `ShoeUnlockService` (Top 3 sesi tercepat `>= 3 km` dalam 30 hari, 20 model sepatu), `WeeklyProgressService`, `GoalTrainingPlanService`, `SplitParserService`, `BiomechanicsCatalogService`.
   - Menambahkan analitik eksklusif web via `DeepAnalyticsService`: seri harian **CTL / ATL / TSB (Fitness-Fatigue-Form 90 hari)**, **ACWR Injury Risk Ratio**, **Distribusi Intensitas 80/20**, **Aerobic Efficiency Factor (EF)**, **Stride Length**, **Split CV% & Negative Split Grading**, **Heatmap 16 Minggu**, dan **Race Readiness Score (0–100)**.
   - Integrasi opsional **Gemini API (`GEMINI_API_KEY`)** untuk Scan Screenshot Strava (`gemini-2.5-flash`) & AI Coach dengan fallback parser teks/split & engine coaching lokal.
   - Ekspor data ke **CSV** (`POST /api/export/csv`) dan **JSON Backup** (`POST /api/export/json`).

2. **`strideapex-web/` (Next.js 15 App Router, TypeScript, Tailwind CSS, Recharts — `http://localhost:3000`)**
   - Tema default **Strava Light** (`#F7F7FA` / `#FC5200`) dengan tombol switch ke **Strava Dark** (`#121212` / `#1C1C1E` / `#FC5200`).
   - Tersedia **Mode Demo & Lokal** langsung siap coba, atau **Masuk Google** untuk sinkronisasi dua arah real-time dengan Firestore aplikasi Android.

## Cara Menjalankan

### 1. Jalankan Backend (.NET 8)
```bash
cd strideapex-api
# Opsional: aktifkan Gemini Vision & AI Coach
# export GEMINI_API_KEY="AIza..."
dotnet run
```
Backend berjalan di `http://localhost:5180` (`GET /api/health`).

### 2. Jalankan Frontend (Next.js)
```bash
cd strideapex-web
npm install
npm run dev
```
Buka `http://localhost:3000` di browser.

## Catatan Sinkronisasi Firebase dengan Android
- Pastikan di **Firebase Console → Authentication → Settings → Authorized domains**, domain `localhost` (atau domain produksi Anda) sudah terdaftar agar **Sign-In dengan Google** berjalan lancar.
- Website otomatis terhubung ke project `com-aistudio-strideapex-5c43f` dan database Firestore bernama `ai-studio-android-strideap-2c7a03db-df95-4745-9edb-cd882b28322a` sesuai `strideapex-web/.env.local`.
