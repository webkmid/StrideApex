"use client";

import React from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import {
  Activity,
  Award,
  Calendar,
  Camera,
  Cloud,
  CloudOff,
  Download,
  Flame,
  Footprints,
  History,
  LayoutDashboard,
  LogIn,
  LogOut,
  Moon,
  Sun,
  Target,
  X,
} from "lucide-react";
import { useStrideApex } from "@/lib/strideapex-context";

const NAV_ITEMS = [
  { href: "/", label: "Dashboard", icon: LayoutDashboard },
  { href: "/analytics", label: "Lab Analitik", icon: Activity },
  { href: "/shoes", label: "Katalog Sepatu", icon: Footprints },
  { href: "/progress", label: "Progress Mingguan", icon: Calendar },
  { href: "/history", label: "Riwayat Lari", icon: History },
  { href: "/predictor", label: "Prediksi & PR", icon: Award },
  { href: "/goals", label: "Target & Jadwal", icon: Target },
];

const MOBILE_NAV = [
  { href: "/progress", label: "Progress", icon: Calendar, isCenter: false },
  { href: "/shoes", label: "Sepatu", icon: Footprints, isCenter: false },
  { href: "/", label: "Dashboard", icon: LayoutDashboard, isCenter: true },
  { href: "/history", label: "History", icon: History, isCenter: false },
  { href: "/predictor", label: "Prediksi", icon: Award, isCenter: false },
];

export function AppShell({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const {
    user,
    authLoading,
    isCloudMode,
    isDark,
    toggleTheme,
    backendOnline,
    geminiConfigured,
    statusBanner,
    clearStatusBanner,
    signInWithGoogle,
    signOutToDemo,
    exportData,
  } = useStrideApex();

  const activeNav = pathname.startsWith("/scan")
    ? { href: "/scan", label: "Scan / Input Run", icon: Camera }
    : NAV_ITEMS.find((n) => (n.href === "/" ? pathname === "/" : pathname.startsWith(n.href))) ?? NAV_ITEMS[0];

  if (authLoading) {
    return (
      <div className="min-h-screen bg-bg text-text-primary flex items-center justify-center p-6">
        <div className="flex flex-col items-center gap-3">
          <div className="w-11 h-11 rounded-2xl bg-primary flex items-center justify-center text-white animate-pulse">
            <Flame className="w-6 h-6" />
          </div>
          <p className="text-xs font-mono text-text-secondary">Memeriksa sesi akun Google...</p>
        </div>
      </div>
    );
  }

  if (!user) {
    return (
      <div className="min-h-screen bg-bg text-text-primary flex flex-col items-center justify-center p-6 relative">
        <button
          type="button"
          onClick={toggleTheme}
          title={isDark ? "Ubah ke Strava Light" : "Ubah ke Strava Dark"}
          className="absolute top-5 right-5 p-2.5 rounded-xl border border-border bg-card text-text-primary hover:border-primary/50 transition-colors"
        >
          {isDark ? <Sun className="w-4 h-4 text-warning" /> : <Moon className="w-4 h-4 text-primary" />}
        </button>

        <div className="w-full max-w-md p-7 rounded-3xl bg-card border border-border shadow-lg space-y-6 text-center">
          <div className="w-14 h-14 mx-auto rounded-2xl bg-primary flex items-center justify-center text-white shadow-md">
            <Flame className="w-8 h-8" />
          </div>

          <div className="space-y-2">
            <h1 className="text-2xl font-bold tracking-tight">StrideApex Web</h1>
            <p className="text-xs text-text-secondary leading-relaxed">
              Masuk dengan akun Google yang sama dengan aplikasi Android StrideApex untuk mengakses dashboard analitik, riwayat lari, VDOT, dan rekomendasi sepatu Anda.
            </p>
          </div>

          {statusBanner && (
            <div className="p-3 rounded-xl bg-elevated border border-primary/40 text-xs font-medium text-primary text-left">
              {statusBanner}
            </div>
          )}

          <button
            type="button"
            onClick={() => void signInWithGoogle()}
            className="w-full py-3 px-4 rounded-xl bg-primary text-white text-sm font-bold hover:bg-primary-dim transition-colors inline-flex items-center justify-center gap-2"
          >
            <LogIn className="w-4 h-4" />
            <span>Masuk dengan Google</span>
          </button>

          <div className="pt-2 border-t border-border text-[11px] font-mono text-text-secondary flex items-center justify-end">
            <span>Firestore Sync Required</span>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-bg text-text-primary flex flex-col lg:flex-row">
      {/* Desktop Left Sidebar */}
      <aside className="hidden lg:flex lg:w-64 lg:flex-col lg:fixed lg:inset-y-0 bg-card border-r border-border z-30">
        <div className="p-5 border-b border-border flex items-center gap-3">
          <div className="w-9 h-9 rounded-xl bg-primary flex items-center justify-center text-white shadow-sm">
            <Flame className="w-5 h-5" />
          </div>
          <div>
            <div className="font-bold tracking-tight text-base leading-tight">StrideApex</div>
            <div className="text-[11px] font-mono text-text-secondary">WEB ANALYTICS</div>
          </div>
        </div>

        <nav className="flex-1 p-3 space-y-1 overflow-y-auto">
          {NAV_ITEMS.map((item) => {
            const Icon = item.icon;
            const active = item.href === "/" ? pathname === "/" : pathname.startsWith(item.href);
            return (
              <Link
                key={item.href}
                href={item.href}
                className={`flex items-center gap-3 px-3.5 py-2.5 rounded-xl text-sm font-medium transition-colors ${
                  active
                    ? "bg-primary/12 text-primary font-semibold border border-primary/25"
                    : "text-text-secondary hover:bg-elevated hover:text-text-primary"
                }`}
              >
                <Icon className="w-4 h-4 shrink-0" />
                <span>{item.label}</span>
              </Link>
            );
          })}
        </nav>

        {/* System Status Footer inside Sidebar */}
        <div className="p-4 border-t border-border space-y-2.5 text-xs">
          <div className="p-3 rounded-xl bg-elevated border border-border space-y-1.5">
            <div className="flex items-center justify-between">
              <span className="text-text-secondary font-medium">Mode Data</span>
              <span
                className={`inline-flex items-center gap-1 font-semibold ${
                  isCloudMode ? "text-success" : "text-accent-blue"
                }`}
              >
                {isCloudMode ? <Cloud className="w-3.5 h-3.5" /> : <CloudOff className="w-3.5 h-3.5" />}
                {isCloudMode ? "Cloud Sync" : "Lokal (Offline)"}
              </span>
            </div>
          </div>
        </div>
      </aside>

      {/* Main Viewport */}
      <div className="flex-1 lg:pl-64 flex flex-col min-h-screen pb-20 lg:pb-8">
        {/* Sticky Top Header */}
        <header className="sticky top-0 z-20 bg-card/90 backdrop-blur-md border-b border-border px-4 sm:px-6 py-3 flex items-center justify-between gap-3">
          <div className="flex items-center gap-3 min-w-0">
            <div className="lg:hidden w-8 h-8 rounded-lg bg-primary flex items-center justify-center text-white shrink-0">
              <Flame className="w-4 h-4" />
            </div>
            <div className="min-w-0">
              <h1 className="text-base sm:text-lg font-bold tracking-tight truncate">{activeNav.label}</h1>
              <p className="hidden sm:block text-xs text-text-secondary truncate">
                {isCloudMode
                  ? `Tersinkron real-time ke Cloud (${user?.email ?? "Akun Google"})`
                  : "Penyimpanan Lokal · Masuk Google untuk sinkron data Android"}
              </p>
            </div>
          </div>

          <div className="flex items-center gap-2 shrink-0">
            <button
              type="button"
              onClick={() => void exportData("csv")}
              title="Ekspor CSV"
              className="hidden md:inline-flex items-center gap-1.5 px-3 py-2 rounded-xl border border-border bg-elevated text-xs font-medium text-text-primary hover:border-primary/50 transition-colors"
            >
              <Download className="w-3.5 h-3.5" />
              <span>CSV</span>
            </button>

            <button
              type="button"
              onClick={toggleTheme}
              title={isDark ? "Ubah ke Strava Light" : "Ubah ke Strava Dark"}
              className="p-2 rounded-xl border border-border bg-elevated text-text-primary hover:border-primary/50 transition-colors"
            >
              {isDark ? <Sun className="w-4 h-4 text-warning" /> : <Moon className="w-4 h-4 text-primary" />}
            </button>

            {isCloudMode ? (
              <button
                type="button"
                onClick={() => void signOutToDemo()}
                className="inline-flex items-center gap-1.5 px-3 py-2 rounded-xl border border-border bg-elevated text-xs font-medium text-text-primary hover:border-danger/60 transition-colors"
              >
                <LogOut className="w-3.5 h-3.5 text-danger" />
                <span className="hidden sm:inline">Keluar Cloud</span>
              </button>
            ) : (
              <button
                type="button"
                onClick={() => void signInWithGoogle()}
                className="inline-flex items-center gap-1.5 px-3 py-2 rounded-xl border border-primary/40 bg-primary/10 text-primary text-xs font-semibold hover:bg-primary/20 transition-colors"
              >
                <LogIn className="w-3.5 h-3.5" />
                <span>Masuk Google</span>
              </button>
            )}
          </div>
        </header>

        {/* Status Toast Banner */}
        {statusBanner && (
          <div className="mx-4 sm:mx-6 mt-4 p-3 rounded-xl bg-card border border-primary/40 flex items-center justify-between gap-3 shadow-sm">
            <p className="text-xs sm:text-sm font-medium text-primary">{statusBanner}</p>
            <button
              type="button"
              onClick={clearStatusBanner}
              className="p-1 rounded-lg hover:bg-elevated text-text-secondary"
              aria-label="Tutup notifikasi"
            >
              <X className="w-4 h-4" />
            </button>
          </div>
        )}

        <main className="flex-1 px-4 sm:px-6 py-5 max-w-7xl w-full mx-auto">
          {children}
        </main>
      </div>

      {/* Mobile Bottom Navigation */}
      <nav className="lg:hidden fixed bottom-0 inset-x-0 bg-card/95 backdrop-blur-md border-t border-border z-30 flex items-center justify-around py-1.5 px-1">
        {MOBILE_NAV.map((item) => {
          const Icon = item.icon;
          const active = item.href === "/" ? pathname === "/" : pathname.startsWith(item.href);
          if (item.isCenter) {
            return (
              <Link
                key={item.href}
                href={item.href}
                className="flex flex-col items-center gap-0.5 px-2.5 py-0.5 rounded-xl text-[11px] font-semibold"
              >
                <span
                  className={`w-9 h-9 rounded-full flex items-center justify-center transition-colors shadow-sm ${
                    active
                      ? "bg-primary text-white"
                      : "bg-accent-blue/15 text-accent-blue border border-accent-blue/40"
                  }`}
                >
                  <Icon className="w-4 h-4" />
                </span>
                <span className={active ? "text-primary font-bold" : "text-text-secondary"}>{item.label}</span>
              </Link>
            );
          }
          return (
            <Link
              key={item.href}
              href={item.href}
              className={`flex flex-col items-center gap-0.5 px-2.5 py-1.5 rounded-xl text-[11px] font-medium ${
                active ? "text-primary font-semibold" : "text-text-secondary"
              }`}
            >
              <Icon className="w-4 h-4" />
              <span>{item.label}</span>
            </Link>
          );
        })}
      </nav>
    </div>
  );
}
