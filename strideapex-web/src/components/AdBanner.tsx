"use client";

import React, { useEffect, useRef } from "react";

declare global {
  interface Window {
    adsbygoogle?: Record<string, unknown>[];
  }
}

interface AdBannerProps {
  slotKey: "footer" | "shoes" | "shoe_detail";
  className?: string;
}

export function AdBanner({ slotKey, className = "" }: AdBannerProps) {
  const clientId = process.env.NEXT_PUBLIC_ADSENSE_CLIENT_ID?.trim() ?? "";

  // Read NEXT_PUBLIC_* statically so Next.js inlines values into client bundles
  const slotFooter = process.env.NEXT_PUBLIC_ADSENSE_SLOT_FOOTER?.trim() ?? "";
  const slotShoes = process.env.NEXT_PUBLIC_ADSENSE_SLOT_SHOES?.trim() ?? "";
  const slotShoeDetail = process.env.NEXT_PUBLIC_ADSENSE_SLOT_SHOE_DETAIL?.trim() ?? "";

  const slotId =
    slotKey === "shoes"
      ? slotShoes || slotFooter
      : slotKey === "shoe_detail"
      ? slotShoeDetail || slotFooter
      : slotFooter;

  const pushedRef = useRef(false);

  useEffect(() => {
    if (!clientId || !slotId || pushedRef.current) return;
    pushedRef.current = true;
    try {
      (window.adsbygoogle = window.adsbygoogle || []).push({});
    } catch {
      // Ignore AdSense push failures when blocked by extensions or offline
    }
  }, [clientId, slotId]);

  if (clientId && slotId) {
    return (
      <div
        className={`rounded-2xl bg-card/70 border border-border/70 px-3 py-2.5 text-center overflow-hidden ${className}`}
      >
        <div className="text-[10px] font-mono uppercase tracking-wider text-text-secondary mb-1">
          Iklan
        </div>
        <ins
          className="adsbygoogle"
          style={{ display: "block", minHeight: "90px" }}
          data-ad-client={clientId}
          data-ad-slot={slotId}
          data-ad-format="horizontal"
          data-full-width-responsive="true"
        />
      </div>
    );
  }

  return (
    <div
      className={`rounded-2xl bg-card border border-dashed border-border px-4 py-3 flex flex-wrap items-center justify-between gap-2 text-xs text-text-secondary ${className}`}
    >
      <span className="font-semibold text-text-primary/80">Slot Banner Iklan · Non-Intrusif</span>
      <span className="text-[11px] font-mono">
        Tambahkan NEXT_PUBLIC_ADSENSE_CLIENT_ID &amp; Slot ID di .env.local untuk mengaktifkan Google AdSense
      </span>
    </div>
  );
}
