import type { Metadata } from "next";
import Script from "next/script";
import { AppShell } from "@/components/AppShell";
import { StrideApexProvider } from "@/lib/strideapex-context";
import "./globals.css";

export const metadata: Metadata = {
  title: "StrideApex Web — Running Science & Performance Analytics",
  description:
    "Platform web analitik lari StrideApex: sinkronisasi Cloud Firestore dengan aplikasi Android, CTL/ATL/TSB, ACWR, polarisasi 80/20, prediksi VDOT, & katalog unlock sepatu.",
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  const adsenseClientId = process.env.NEXT_PUBLIC_ADSENSE_CLIENT_ID?.trim();

  return (
    <html lang="id" suppressHydrationWarning>
      <body>
        {adsenseClientId && (
          <Script
            id="google-adsense-script"
            async
            src={`https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js?client=${adsenseClientId}`}
            crossOrigin="anonymous"
            strategy="afterInteractive"
          />
        )}
        <StrideApexProvider>
          <AppShell>{children}</AppShell>
        </StrideApexProvider>
      </body>
    </html>
  );
}
