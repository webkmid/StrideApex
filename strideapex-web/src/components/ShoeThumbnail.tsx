"use client";

import React, { useState } from "react";
import { Footprints } from "lucide-react";

interface ShoeThumbnailProps {
  imageUrl?: string | null;
  alt: string;
  isUnlocked: boolean;
  size?: "sm" | "md" | "lg";
}

export function ShoeThumbnail({ imageUrl, alt, isUnlocked, size = "sm" }: ShoeThumbnailProps) {
  const [failed, setFailed] = useState(false);

  const sizeClass =
    size === "lg"
      ? "w-full h-64 sm:h-72 rounded-2xl p-4"
      : size === "md"
      ? "w-full h-40 rounded-xl p-3"
      : "w-16 h-12 rounded-xl shrink-0";

  return (
    <div
      className={`${sizeClass} border border-border flex items-center justify-center overflow-hidden ${
        imageUrl && !failed ? "bg-[#F8FAFC]" : "bg-elevated"
      }`}
    >
      {imageUrl && !failed ? (
        // eslint-disable-next-line @next/next/no-img-element
        <img
          src={imageUrl}
          alt={alt}
          loading="lazy"
          onError={() => setFailed(true)}
          className={`w-full h-full object-contain ${size === "sm" ? "p-1" : ""} ${
            isUnlocked ? "opacity-100" : "opacity-55"
          }`}
        />
      ) : (
        <Footprints className={`${size === "sm" ? "w-5 h-5" : "w-10 h-10"} text-accent-blue opacity-70`} />
      )}
    </div>
  );
}
