package com.example.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * StrideApex Theme Palette
 * - Dark Theme: Authentic Strava-inspired Charcoal & Blaze Orange (#FC5200) on #121212 / #1C1C1E
 * - Light Theme: Authentic Strava-inspired Clean Light (#F7F7FA / #FFFFFF) & Blaze Orange (#FC5200)
 */

// =========================================================================
// 1. STRAVA-INSPIRED DARK THEME SPECIFICATION (.dark)
// =========================================================================
val ThemeDarkBackground = Color(0xFF121212)
val ThemeDarkCard = Color(0xFF1C1C1E)
val ThemeDarkCardForeground = Color(0xFFF5F5F7)
val ThemeDarkPopover = Color(0xFF232326)
val ThemeDarkPopoverForeground = Color(0xFFF5F5F7)
val ThemeDarkSidebar = Color(0xFF1C1C1E)
val ThemeDarkSidebarForeground = Color(0xFFF5F5F7)
val ThemeDarkMuted = Color(0xFF18181A)
val ThemeDarkMutedForeground = Color(0xFFA1A1AA)
val ThemeDarkInput = Color(0xFF2E2E32)
val ThemeDarkBorder = Color(0xFF2E2E32)
val ThemeDarkPrimary = Color(0xFFFC5200)
val ThemeDarkPrimaryForeground = Color(0xFFFFFFFF)
val ThemeDarkSecondary = Color(0xFF262629)
val ThemeDarkSecondaryForeground = Color(0xFFD4D4D8)
val ThemeDarkAccent = Color(0xFF2A211C)
val ThemeDarkAccentForeground = Color(0xFFFB923C)
val ThemeDarkForeground = Color(0xFFF5F5F7)
val ThemeDarkDestructive = Color(0xFFEF4444)
val ThemeDarkDestructiveForeground = Color(0xFFFFFFFF)
val ThemeDarkRing = Color(0xFFFC5200)

// Dark Theme Data Visualization Charts
val ThemeDarkChart1 = Color(0xFFFC5200)
val ThemeDarkChart2 = Color(0xFF38BDF8)
val ThemeDarkChart3 = Color(0xFF22C55E)
val ThemeDarkChart4 = Color(0xFFFB923C)
val ThemeDarkChart5 = Color(0xFFF59E0B)

// =========================================================================
// 2. STRAVA-INSPIRED LIGHT THEME SPECIFICATION (:root)
// =========================================================================
val ThemeLightBackground = Color(0xFFF7F7FA)
val ThemeLightCard = Color(0xFFFFFFFF)
val ThemeLightCardForeground = Color(0xFF18181B)
val ThemeLightPopover = Color(0xFFFFFFFF)
val ThemeLightPopoverForeground = Color(0xFF18181B)
val ThemeLightSidebar = Color(0xFFFFFFFF)
val ThemeLightSidebarForeground = Color(0xFF18181B)
val ThemeLightMuted = Color(0xFFF0F0F5)
val ThemeLightMutedForeground = Color(0xFF6B7280)
val ThemeLightInput = Color(0xFFE5E5EA)
val ThemeLightBorder = Color(0xFFE5E5EA)
val ThemeLightPrimary = Color(0xFFFC5200)
val ThemeLightPrimaryForeground = Color(0xFFFFFFFF)
val ThemeLightSecondary = Color(0xFFF0F0F5)
val ThemeLightSecondaryForeground = Color(0xFF3F3F46)
val ThemeLightAccent = Color(0xFFFFEDE5)
val ThemeLightAccentForeground = Color(0xFFC2410C)
val ThemeLightForeground = Color(0xFF18181B)
val ThemeLightDestructive = Color(0xFFEF4444)
val ThemeLightDestructiveForeground = Color(0xFFFFFFFF)
val ThemeLightRing = Color(0xFFFC5200)

// Light Theme Data Visualization Charts
val ThemeLightChart1 = Color(0xFFFC5200)
val ThemeLightChart2 = Color(0xFF0284C7)
val ThemeLightChart3 = Color(0xFF16A34A)
val ThemeLightChart4 = Color(0xFFEA580C)
val ThemeLightChart5 = Color(0xFFD97706)

// =========================================================================
// 3. APPLICATION COLOR ROLES (Dynamic based on ThemeState.isDark)
// =========================================================================

// Backgrounds & Card Surfaces
val ObsidianBackground: Color
    get() = if (ThemeState.isDark) ThemeDarkBackground else ThemeLightBackground

val SlateSurface: Color
    get() = if (ThemeState.isDark) ThemeDarkCard else ThemeLightCard

val SlateSurfaceElevated: Color
    get() = if (ThemeState.isDark) Color(0xFF262629) else ThemeLightMuted

val SlateBorder: Color
    get() = if (ThemeState.isDark) ThemeDarkBorder else ThemeLightBorder

// Primary Brand Action (Strava Blaze Orange #FC5200)
val VoltLime: Color
    get() = if (ThemeState.isDark) ThemeDarkPrimary else ThemeLightPrimary

val VoltLimeDim: Color
    get() = if (ThemeState.isDark) Color(0xFFE04800) else Color(0xFFC2410C)

val OnVoltLime: Color
    get() = if (ThemeState.isDark) ThemeDarkPrimaryForeground else ThemeLightPrimaryForeground

// Energetic Coral / Highlight Red
val KineticCoral: Color
    get() = if (ThemeState.isDark) ThemeDarkDestructive else ThemeLightDestructive

val OnKineticCoral: Color
    get() = Color(0xFFFFFFFF)

// High-Tech Telemetry Accent (Ice Blue in Dark #38BDF8, Sky Cyan in Light #0284C7)
val AeroCyan: Color
    get() = if (ThemeState.isDark) ThemeDarkChart2 else Color(0xFF0284C7)

val OnAeroCyan: Color
    get() = if (ThemeState.isDark) Color(0xFF121212) else Color(0xFFFFFFFF)

// Neutral Minimalist Typography Scale
val TextPrimary: Color
    get() = if (ThemeState.isDark) ThemeDarkForeground else ThemeLightForeground

val TextSecondary: Color
    get() = if (ThemeState.isDark) ThemeDarkMutedForeground else ThemeLightMutedForeground

val TextMuted: Color
    get() = if (ThemeState.isDark) Color(0xFF71717A) else Color(0xFF9CA3AF)

// Semantic Sport Status Indicators
val SuccessGreen: Color
    get() = if (ThemeState.isDark) ThemeDarkChart3 else ThemeLightChart3

val WarningAmber: Color
    get() = if (ThemeState.isDark) Color(0xFFF59E0B) else Color(0xFFD97706)

val ErrorCrimson: Color
    get() = if (ThemeState.isDark) ThemeDarkDestructive else ThemeLightDestructive
