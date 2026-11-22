import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  transpilePackages: ["recharts"],
  images: {
    unoptimized: true,
  },
};

export default nextConfig;
