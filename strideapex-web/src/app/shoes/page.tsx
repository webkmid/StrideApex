"use client";

import React, { useMemo, useState } from "react";
import Link from "next/link";
import { ArrowRight, CheckCircle2, Footprints, Lock, LockOpen, Search, ShoppingBag } from "lucide-react";
import { ShoeThumbnail } from "@/components/ShoeThumbnail";
import { formatPace } from "@/lib/formatters";
import { getShoeDetailExtra, getShoeSlug } from "@/lib/shoe-affiliates";
import { useStrideApex } from "@/lib/strideapex-context";

export default function ShoesCatalogPage() {
  const { analytics } = useStrideApex();
  const { shoeUnlock } = analytics;

  const [query, setQuery] = useState("");
  const [selectedTierId, setSelectedTierId] = useState<number | "ALL">("ALL");
  const [selectedBrand, setSelectedBrand] = useState<string>("ALL");

  const brands = useMemo(() => {
    const set = new Set<string>();
    shoeUnlock.tiers.forEach((t) => t.tier.shoes.forEach((s) => set.add(s.brand)));
    return ["ALL", ...Array.from(set)];
  }, [shoeUnlock.tiers]);

  const filteredTiers = useMemo(() => {
    const q = query.trim().toLowerCase();
    return shoeUnlock.tiers
      .filter((ts) => selectedTierId === "ALL" || ts.tier.id === selectedTierId)
      .map((ts) => {
        const filteredShoes = ts.tier.shoes.filter((shoe) => {
          if (selectedBrand !== "ALL" && shoe.brand.toLowerCase() !== selectedBrand.toLowerCase()) {
            return false;
          }
          if (!q) return true;
          return (
            shoe.name.toLowerCase().includes(q) ||
            shoe.brand.toLowerCase().includes(q) ||
            shoe.plateType.toLowerCase().includes(q) ||
            shoe.bestFor.toLowerCase().includes(q) ||
            shoe.description.toLowerCase().includes(q)
          );
        });
        return { ...ts, shoes: filteredShoes };
      })
      .filter((item) => item.shoes.length > 0);
  }, [shoeUnlock.tiers, selectedTierId, selectedBrand, query]);

  const totalMatchingShoes = useMemo(
    () => filteredTiers.reduce((sum, item) => sum + item.shoes.length, 0),
    [filteredTiers]
  );

  return (
    <div className="space-y-6">
      {/* Pace Top 3 & Tier Unlock Hero Card */}
      <div className="p-5 rounded-2xl bg-card border border-border space-y-4">
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div>
            <div className="text-xs font-bold tracking-wider uppercase text-text-secondary">
              PACE TOP 3 (30 HARI)
            </div>
            <div className="text-3xl font-bold font-mono tabular-nums text-primary mt-1">
              {shoeUnlock.currentAvgPaceSec > 0 ? formatPace(shoeUnlock.currentAvgPaceSec) : "--:-- /km"}
            </div>
          </div>
          <span className="px-3 py-1.5 rounded-xl bg-primary/15 border border-primary/40 text-primary text-xs font-bold">
            {shoeUnlock.currentTier.badgeLabel}
          </span>
        </div>

        {shoeUnlock.totalWorkoutsEvaluated > 0 ? (
          <p className="text-xs text-text-secondary leading-relaxed">
            Dihitung dari {shoeUnlock.totalWorkoutsEvaluated} sesi tercepat (≥3 km) dalam 30 hari · Klik sepatu manapun untuk melihat spesifikasi detail &amp; tautan Shopee resmi.
          </p>
        ) : (
          <p className="text-xs text-text-secondary leading-relaxed">
            Klik sepatu di bawah untuk membuka detail spesifikasi lab, analisis bantalan, tips ukuran, dan tautan belanja Shopee.
          </p>
        )}

        <div className="pt-3 border-t border-border space-y-2">
          {shoeUnlock.nextTier ? (
            <>
              <div className="flex items-center justify-between text-xs">
                <span className="font-semibold">Target: {shoeUnlock.nextTier.title}</span>
                <span className="font-mono font-bold text-accent-blue">{shoeUnlock.nextTier.unlockPaceDisplay}</span>
              </div>
              <div className="w-full h-2.5 rounded-full bg-elevated overflow-hidden">
                <div
                  className="h-full rounded-full bg-primary transition-all duration-300"
                  style={{ width: `${Math.round(shoeUnlock.progressToNextTier * 100)}%` }}
                />
              </div>
              {shoeUnlock.secondsNeededToNextTier > 0 && shoeUnlock.currentAvgPaceSec > 0 && (
                <p className="text-xs text-accent-blue">
                  Tingkatkan pace {shoeUnlock.secondsNeededToNextTier}s/km lebih cepat untuk membuka {shoeUnlock.nextTier.title}!
                </p>
              )}
            </>
          ) : (
            <div className="flex items-center gap-2 text-sm font-semibold text-success">
              <CheckCircle2 className="w-4 h-4" />
              <span>Luar biasa! Seluruh kasta sepatu tertinggi hingga Elite Pinnacle Alpha telah terbuka!</span>
            </div>
          )}
        </div>
      </div>

      {/* Filter Bar: Search, Tier Pills & Brand Pills */}
      <div className="p-4 rounded-2xl bg-card border border-border space-y-3">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
          <div className="relative flex-1">
            <Search className="w-4 h-4 text-text-secondary absolute left-3.5 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder="Cari nama sepatu, brand, atau teknologi busa..."
              className="w-full pl-10 pr-4 py-2.5 rounded-xl bg-elevated border border-border text-xs text-text-primary placeholder:text-text-secondary focus:outline-none focus:border-primary"
            />
          </div>
          <div className="text-xs font-mono text-text-secondary shrink-0">
            Menampilkan <span className="font-bold text-text-primary">{totalMatchingShoes}</span> sepatu ·{" "}
            {shoeUnlock.tiers.filter((t) => t.isUnlocked).length}/{shoeUnlock.tiers.length} Tier Terbuka
          </div>
        </div>

        {/* Tier Filter Pills */}
        <div className="flex gap-2 overflow-x-auto pb-1">
          <button
            type="button"
            onClick={() => setSelectedTierId("ALL")}
            className={`px-3 py-1.5 rounded-xl text-xs font-bold border shrink-0 transition-colors ${
              selectedTierId === "ALL"
                ? "bg-primary text-white border-primary"
                : "bg-elevated text-text-secondary border-border hover:border-primary/40"
            }`}
          >
            Semua Tier
          </button>
          {shoeUnlock.tiers.map((t) => {
            const paceShort =
              t.tier.unlockPaceSecondsPerKm >= 9000
                ? "Semua Pace"
                : `<${Math.floor(t.tier.unlockPaceSecondsPerKm / 60)}:00`;
            const active = selectedTierId === t.tier.id;
            return (
              <button
                key={t.tier.id}
                type="button"
                onClick={() => setSelectedTierId(t.tier.id)}
                className={`inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-xs font-mono font-bold border shrink-0 transition-colors ${
                  active
                    ? "bg-primary text-white border-primary"
                    : t.isUnlocked
                    ? "bg-accent-blue/12 text-accent-blue border-accent-blue/40 hover:border-primary/50"
                    : "bg-elevated text-text-secondary border-border hover:border-primary/40"
                }`}
              >
                {t.isUnlocked ? <LockOpen className="w-3.5 h-3.5" /> : <Lock className="w-3.5 h-3.5" />}
                <span>Tier {t.tier.id} · {paceShort}</span>
              </button>
            );
          })}
        </div>

        {/* Brand Filter Pills */}
        <div className="flex gap-1.5 overflow-x-auto pb-1">
          {brands.map((brand) => {
            const active = selectedBrand === brand;
            return (
              <button
                key={brand}
                type="button"
                onClick={() => setSelectedBrand(brand)}
                className={`px-2.5 py-1 rounded-lg text-[11px] font-semibold border shrink-0 transition-colors ${
                  active
                    ? "bg-accent-blue text-white border-accent-blue"
                    : "bg-card text-text-secondary border-border hover:text-text-primary"
                }`}
              >
                {brand === "ALL" ? "Semua Brand" : brand}
              </button>
            );
          })}
        </div>
      </div>

      {/* Filtered Tiers & Clickable Shoe Grid */}
      {filteredTiers.length === 0 ? (
        <div className="p-8 rounded-2xl bg-card border border-border text-center space-y-2">
          <Footprints className="w-8 h-8 mx-auto text-text-secondary opacity-70" />
          <div className="text-sm font-bold">Tidak ada sepatu yang cocok dengan filter Anda</div>
          <button
            type="button"
            onClick={() => {
              setQuery("");
              setSelectedTierId("ALL");
              setSelectedBrand("ALL");
            }}
            className="text-xs font-semibold text-primary hover:underline"
          >
            Reset Filter Pencarian
          </button>
        </div>
      ) : (
        <div className="space-y-6">
          {filteredTiers.map((tierState) => {
            const { tier } = tierState;
            return (
              <section
                key={tier.id}
                className={`p-5 rounded-2xl bg-card border space-y-4 ${
                  tierState.isCurrentHighest ? "border-primary ring-1 ring-primary/25" : "border-border"
                }`}
              >
                <div className="flex flex-wrap items-start justify-between gap-3">
                  <div className="space-y-1 max-w-3xl">
                    <div className="flex flex-wrap items-center gap-2">
                      <span className="inline-flex items-center gap-1.5 text-xs font-mono font-bold text-primary">
                        {tierState.isUnlocked ? (
                          <LockOpen className="w-3.5 h-3.5" />
                        ) : (
                          <Lock className="w-3.5 h-3.5 text-text-secondary" />
                        )}
                        {tier.badgeLabel}
                      </span>
                      <span
                        className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                          tierState.isCurrentHighest
                            ? "bg-primary text-white"
                            : tierState.isUnlocked
                            ? "bg-accent-blue/15 text-accent-blue"
                            : "bg-elevated text-text-secondary"
                        }`}
                      >
                        {tierState.isUnlocked
                          ? tierState.isCurrentHighest
                            ? "REKOMENDASI"
                            : "TERBUKA"
                          : tier.unlockPaceDisplay}
                      </span>
                    </div>
                    <h2 className="font-bold text-lg leading-snug">{tier.title}</h2>
                    <p className="text-xs text-text-secondary leading-relaxed">{tier.biomechanicalRationale}</p>
                  </div>
                  <span className="text-xs font-mono text-text-secondary shrink-0">
                    {tierState.shoes.length} Sepatu
                  </span>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
                  {tierState.shoes.map((shoe) => {
                    const slug = getShoeSlug(shoe);
                    const extra = getShoeDetailExtra(shoe);
                    return (
                      <Link
                        key={slug}
                        href={`/shoes/${encodeURIComponent(slug)}`}
                        className="group p-4 rounded-2xl bg-elevated border border-border hover:border-primary/60 transition-all flex flex-col justify-between gap-3"
                      >
                        <div className="space-y-3">
                          <ShoeThumbnail
                            size="md"
                            imageUrl={shoe.imageUrl}
                            alt={`${shoe.brand} ${shoe.name}`}
                            isUnlocked={tierState.isUnlocked}
                          />

                          <div className="flex items-center justify-between gap-2">
                            <span className="px-2 py-0.5 rounded bg-accent-blue/15 text-accent-blue text-[11px] font-bold">
                              {shoe.brand}
                            </span>
                            <span className="text-xs font-mono font-bold text-text-primary">
                              {extra.estimatedPriceIdr}
                            </span>
                          </div>

                          <div>
                            <h3 className="font-bold text-base group-hover:text-primary transition-colors">
                              {shoe.name}
                            </h3>
                            <div className="mt-1 px-2 py-1 rounded bg-card border border-border text-[11px] font-mono text-primary truncate">
                              {shoe.plateType}
                            </div>
                          </div>

                          <p className="text-xs text-text-secondary leading-relaxed line-clamp-2">
                            {shoe.description}
                          </p>
                        </div>

                        <div className="space-y-2.5 pt-1">
                          <div className="grid grid-cols-3 gap-2 p-2 rounded-xl bg-card border border-border text-[10px]">
                            <div>
                              <div className="text-text-secondary">STACK</div>
                              <div className="font-mono font-bold truncate">{shoe.stackHeight}</div>
                            </div>
                            <div>
                              <div className="text-text-secondary">DROP</div>
                              <div className="font-mono font-bold">{shoe.dropMm} mm</div>
                            </div>
                            <div>
                              <div className="text-text-secondary">BOBOT</div>
                              <div className="font-mono font-bold">{shoe.weightG ? `~${shoe.weightG} g` : "--"}</div>
                            </div>
                          </div>

                          <div className="flex items-center justify-between text-xs font-semibold pt-1 border-t border-border/70">
                            <span className="inline-flex items-center gap-1.5 text-[#EE4D2D]">
                              <ShoppingBag className="w-3.5 h-3.5" />
                              Tersedia di Shopee
                            </span>
                            <span className="inline-flex items-center gap-1 text-primary group-hover:translate-x-0.5 transition-transform">
                              Lihat Detail <ArrowRight className="w-3.5 h-3.5" />
                            </span>
                          </div>
                        </div>
                      </Link>
                    );
                  })}
                </div>
              </section>
            );
          })}
        </div>
      )}
    </div>
  );
}
