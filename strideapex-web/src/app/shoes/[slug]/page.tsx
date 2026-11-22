"use client";

import React from "react";
import Link from "next/link";
import { useParams } from "next/navigation";
import {
  ArrowLeft,
  CheckCircle2,
  ExternalLink,
  Footprints,
  Lock,
  LockOpen,
  ShoppingBag,
  Sparkles,
} from "lucide-react";
import { ShoeThumbnail } from "@/components/ShoeThumbnail";
import { formatPace } from "@/lib/formatters";
import {
  findShoeBySlug,
  getShoeDetailExtra,
  getShoeShopeeUrl,
  getShoeSlug,
} from "@/lib/shoe-affiliates";
import { useStrideApex } from "@/lib/strideapex-context";

export default function ShoeDetailPage() {
  const params = useParams<{ slug: string }>();
  const slug = decodeURIComponent(params?.slug ?? "");

  const { analytics } = useStrideApex();
  const { shoeUnlock } = analytics;
  const found = findShoeBySlug(shoeUnlock.tiers, slug);

  if (!found) {
    return (
      <div className="space-y-6">
        <div className="p-8 rounded-2xl bg-card border border-border text-center space-y-3">
          <Footprints className="w-10 h-10 mx-auto text-text-secondary opacity-70" />
          <h2 className="text-lg font-bold">Sepatu tidak ditemukan</h2>
          <p className="text-xs text-text-secondary">
            Model sepatu yang Anda cari tidak tersedia di katalog StrideApex saat ini.
          </p>
          <Link
            href="/shoes"
            className="inline-flex items-center gap-1.5 px-4 py-2 rounded-xl bg-primary text-white text-xs font-bold hover:bg-primary-dim transition-colors"
          >
            <ArrowLeft className="w-3.5 h-3.5" />
            <span>Kembali ke Katalog Sepatu</span>
          </Link>
        </div>
      </div>
    );
  }

  const { shoe, tierState } = found;
  const { tier } = tierState;
  const extra = getShoeDetailExtra(shoe);
  const shopeeUrl = getShoeShopeeUrl(shoe);
  const currentSlug = getShoeSlug(shoe);
  const relatedShoes = tier.shoes.filter((s) => getShoeSlug(s) !== currentSlug);

  const paceGapSec =
    !tierState.isUnlocked && shoeUnlock.currentAvgPaceSec > 0 && tier.unlockPaceSecondsPerKm < 9000
      ? Math.max(0, shoeUnlock.currentAvgPaceSec - tier.unlockPaceSecondsPerKm)
      : 0;

  return (
    <div className="space-y-6">
      {/* Back Breadcrumb */}
      <div className="flex items-center justify-between gap-3">
        <Link
          href="/shoes"
          className="inline-flex items-center gap-1.5 px-3 py-2 rounded-xl bg-card border border-border hover:border-primary/50 text-xs font-semibold text-text-primary transition-colors"
        >
          <ArrowLeft className="w-3.5 h-3.5" />
          <span>Kembali ke Katalog Sepatu</span>
        </Link>

        <span className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-elevated border border-border text-xs font-mono font-bold text-primary">
          {tierState.isUnlocked ? <LockOpen className="w-3.5 h-3.5" /> : <Lock className="w-3.5 h-3.5" />}
          {tier.badgeLabel}
        </span>
      </div>

      {/* Hero Shoe Showcase + Shopee Affiliate CTA */}
      <div className="p-5 sm:p-6 rounded-2xl bg-card border border-border grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
        <div className="lg:col-span-5 space-y-3">
          <ShoeThumbnail
            size="lg"
            imageUrl={shoe.imageUrl}
            alt={`${shoe.brand} ${shoe.name}`}
            isUnlocked={tierState.isUnlocked}
          />

          <div className="flex items-center justify-between gap-2 px-3 py-2 rounded-xl bg-elevated border border-border text-xs">
            <span className="font-semibold text-text-secondary">Status Kasta Pace Anda</span>
            <span
              className={`px-2.5 py-0.5 rounded-md font-bold text-[11px] ${
                tierState.isCurrentHighest
                  ? "bg-primary text-white"
                  : tierState.isUnlocked
                  ? "bg-accent-blue/15 text-accent-blue"
                  : "bg-card text-text-secondary border border-border"
              }`}
            >
              {tierState.isUnlocked
                ? tierState.isCurrentHighest
                  ? "REKOMENDASI UTAMA"
                  : "TERBUKA"
                : `TERKUNCI · ${tier.unlockPaceDisplay}`}
            </span>
          </div>
        </div>

        <div className="lg:col-span-7 space-y-4">
          <div className="flex flex-wrap items-center gap-2">
            <span className="px-2.5 py-0.5 rounded-lg bg-accent-blue/15 text-accent-blue text-xs font-bold">
              {shoe.brand}
            </span>
            <span className="px-2.5 py-0.5 rounded-lg bg-elevated border border-border text-xs font-mono text-text-secondary">
              {tier.unlockPaceDisplay}
            </span>
          </div>

          <div>
            <h1 className="text-2xl sm:text-3xl font-bold tracking-tight">
              {shoe.brand} {shoe.name}
            </h1>
            <div className="mt-2 inline-block px-3 py-1 rounded-lg bg-elevated border border-border text-xs font-mono text-primary">
              {shoe.plateType}
            </div>
          </div>

          <div className="p-3.5 rounded-xl bg-elevated border border-border flex flex-wrap items-center justify-between gap-2">
            <span className="text-xs text-text-secondary font-medium">Estimasi Harga Pasaran</span>
            <span className="text-lg font-bold font-mono text-text-primary">{extra.estimatedPriceIdr}</span>
          </div>

          <p className="text-sm text-text-secondary leading-relaxed">{shoe.description}</p>

          <div className="text-xs font-semibold text-accent-blue">
            Cocok Untuk: <span className="text-text-primary font-normal">{shoe.bestFor}</span>
          </div>

          {!tierState.isUnlocked && (
            <div className="p-3 rounded-xl bg-elevated border border-border text-xs text-text-secondary leading-relaxed">
              {shoeUnlock.currentAvgPaceSec > 0 ? (
                <>
                  Pace Top 3 Anda saat ini{" "}
                  <span className="font-mono font-bold text-text-primary">
                    {formatPace(shoeUnlock.currentAvgPaceSec)}
                  </span>
                  . Tingkatkan sekitar{" "}
                  <span className="font-mono font-bold text-primary">-{paceGapSec}s/km</span> untuk membuka tier{" "}
                  <span className="font-semibold text-text-primary">{tier.title}</span>.
                </>
              ) : (
                <>
                  Catat minimal 1 sesi lari (≥ 3 km) agar StrideApex dapat memverifikasi kesiapan pace Anda untuk{" "}
                  <span className="font-semibold text-text-primary">{tier.title}</span>.
                </>
              )}
            </div>
          )}

          {/* Shopee Affiliate CTA */}
          <div className="pt-2 space-y-2">
            <a
              href={shopeeUrl}
              target="_blank"
              rel="noopener noreferrer sponsored"
              className="w-full py-3.5 px-5 rounded-xl bg-[#EE4D2D] hover:bg-[#D73211] text-white text-sm font-bold transition-colors inline-flex items-center justify-center gap-2 shadow-sm"
            >
              <ShoppingBag className="w-4 h-4" />
              <span>Cek Harga &amp; Beli di Shopee</span>
              <ExternalLink className="w-4 h-4 opacity-90" />
            </a>
            <p className="text-[11px] text-text-secondary text-center">
              Tautan afiliasi Shopee · Pembelian melalui link ini mendukung pengembangan StrideApex tanpa biaya tambahan bagi Anda.
            </p>
          </div>
        </div>
      </div>

      {/* 6-Card Technical Specifications */}
      <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-3">
        <div className="p-3.5 rounded-2xl bg-card border border-border">
          <div className="text-[10px] font-bold uppercase tracking-wider text-text-secondary">STACK HEIGHT</div>
          <div className="mt-1 text-sm font-bold font-mono">{shoe.stackHeight}</div>
        </div>
        <div className="p-3.5 rounded-2xl bg-card border border-border">
          <div className="text-[10px] font-bold uppercase tracking-wider text-text-secondary">HEEL DROP</div>
          <div className="mt-1 text-sm font-bold font-mono">{shoe.dropMm} mm</div>
        </div>
        <div className="p-3.5 rounded-2xl bg-card border border-border">
          <div className="text-[10px] font-bold uppercase tracking-wider text-text-secondary">BOBOT LAB</div>
          <div className="mt-1 text-sm font-bold font-mono">{shoe.weightG ? `~${shoe.weightG} g` : "--"}</div>
        </div>
        <div className="p-3.5 rounded-2xl bg-card border border-border">
          <div className="text-[10px] font-bold uppercase tracking-wider text-text-secondary">BANTALAN</div>
          <div className="mt-1 text-xs font-bold leading-snug">{extra.cushionLevel}</div>
        </div>
        <div className="p-3.5 rounded-2xl bg-card border border-border">
          <div className="text-[10px] font-bold uppercase tracking-wider text-text-secondary">PERMUKAAN</div>
          <div className="mt-1 text-xs font-bold leading-snug">{extra.surfaceType}</div>
        </div>
        <div className="p-3.5 rounded-2xl bg-card border border-border">
          <div className="text-[10px] font-bold uppercase tracking-wider text-text-secondary">TARGET TIER</div>
          <div className="mt-1 text-xs font-bold text-primary leading-snug">{tier.unlockPaceDisplay}</div>
        </div>
      </div>

      {/* Pros & Biomechanical Rationale */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
        <div className="p-5 rounded-2xl bg-card border border-border space-y-4">
          <div className="flex items-center gap-2 text-sm font-bold">
            <CheckCircle2 className="w-4 h-4 text-success" />
            <span>Keunggulan Utama &amp; Tips Ukuran</span>
          </div>
          <ul className="space-y-2.5">
            {extra.pros.map((pro) => (
              <li key={pro} className="flex items-start gap-2.5 text-xs text-text-secondary leading-relaxed">
                <CheckCircle2 className="w-4 h-4 text-success shrink-0 mt-0.5" />
                <span>{pro}</span>
              </li>
            ))}
          </ul>
          <div className="p-3 rounded-xl bg-elevated border border-border text-xs">
            <span className="font-bold text-text-primary">Tips Ukuran (Sizing): </span>
            <span className="text-text-secondary">{extra.sizingTip}</span>
          </div>
        </div>

        <div className="p-5 rounded-2xl bg-card border border-border space-y-3">
          <div className="flex items-center gap-2 text-sm font-bold">
            <Sparkles className="w-4 h-4 text-primary" />
            <span>Mengapa Cocok di {tier.badgeLabel}?</span>
          </div>
          <p className="text-xs text-text-secondary leading-relaxed">{tier.description}</p>
          <div className="p-3.5 rounded-xl bg-elevated border border-border text-xs text-text-secondary leading-relaxed">
            {tier.biomechanicalRationale}
          </div>
        </div>
      </div>

      {/* Related Shoes in Same Tier */}
      {relatedShoes.length > 0 && (
        <div className="p-5 rounded-2xl bg-card border border-border space-y-3">
          <div className="flex items-center justify-between">
            <h2 className="text-sm font-bold">Alternatif Lain di {tier.badgeLabel}</h2>
            <Link href="/shoes" className="text-xs font-semibold text-primary hover:underline">
              Semua Sepatu →
            </Link>
          </div>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
            {relatedShoes.map((other) => {
              const otherSlug = getShoeSlug(other);
              return (
                <Link
                  key={otherSlug}
                  href={`/shoes/${encodeURIComponent(otherSlug)}`}
                  className="p-3 rounded-xl bg-elevated border border-border hover:border-primary/50 transition-colors flex items-center gap-3"
                >
                  <ShoeThumbnail
                    size="sm"
                    imageUrl={other.imageUrl}
                    alt={`${other.brand} ${other.name}`}
                    isUnlocked={tierState.isUnlocked}
                  />
                  <div className="min-w-0 flex-1">
                    <div className="text-[10px] font-bold text-accent-blue">{other.brand}</div>
                    <div className="font-bold text-xs truncate">{other.name}</div>
                    <div className="text-[10px] font-mono text-text-secondary truncate">
                      {other.weightG ? `~${other.weightG} g · ` : ""}Drop {other.dropMm} mm
                    </div>
                  </div>
                </Link>
              );
            })}
          </div>
        </div>
      )}
    </div>
  );
}
