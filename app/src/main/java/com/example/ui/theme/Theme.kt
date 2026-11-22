package com.example.ui.theme

import android.content.Context
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Global reactive Theme State for switching between Light & Dark themes.
 * State changes trigger immediate recomposition across all theme-aware composables.
 */
object ThemeState {
    var isDark by mutableStateOf(false)

    fun initialize(context: Context) {
        val prefs = context.getSharedPreferences("stride_apex_theme_pref", Context.MODE_PRIVATE)
        isDark = prefs.getBoolean("is_dark_mode", false)
    }

    fun toggle(context: Context) {
        isDark = !isDark
        val prefs = context.getSharedPreferences("stride_apex_theme_pref", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("is_dark_mode", isDark).apply()
    }
}

// =========================================================================
// Material 3 Color Schemes adhering to the CSS Theme Specifications
// =========================================================================

private val StravaDarkColorScheme = darkColorScheme(
    primary = ThemeDarkPrimary,                          // #FC5200 (Strava Blaze Orange)
    onPrimary = ThemeDarkPrimaryForeground,              // #FFFFFF
    primaryContainer = Color(0xFF2E1E16),
    onPrimaryContainer = Color(0xFFFF8A50),
    secondary = ThemeDarkSecondary,                      // #262629
    onSecondary = ThemeDarkSecondaryForeground,          // #D4D4D8
    secondaryContainer = ThemeDarkCard,                  // #1C1C1E
    onSecondaryContainer = ThemeDarkSecondaryForeground,
    tertiary = ThemeDarkChart2,                          // #38BDF8 (Telemetry Ice Blue)
    onTertiary = Color(0xFF121212),
    tertiaryContainer = Color(0xFF18222B),
    onTertiaryContainer = ThemeDarkChart2,
    background = ThemeDarkBackground,                    // #121212 (Strava Charcoal)
    onBackground = ThemeDarkForeground,                  // #F5F5F7
    surface = ThemeDarkCard,                              // #1C1C1E
    onSurface = ThemeDarkForeground,                      // #F5F5F7
    surfaceVariant = Color(0xFF262629),
    onSurfaceVariant = ThemeDarkMutedForeground,          // #A1A1AA
    outline = ThemeDarkBorder,                            // #2E2E32
    outlineVariant = ThemeDarkMuted,                      // #18181A
    error = ThemeDarkDestructive,                         // #EF4444
    onError = ThemeDarkDestructiveForeground              // #FFFFFF
)

private val StravaLightColorScheme = lightColorScheme(
    primary = ThemeLightPrimary,                         // #FC5200 (Strava Blaze Orange)
    onPrimary = ThemeLightPrimaryForeground,             // #FFFFFF
    primaryContainer = ThemeLightAccent,                 // #FFEDE5 (Soft Orange Tint)
    onPrimaryContainer = ThemeLightAccentForeground,     // #C2410C
    secondary = ThemeLightSecondary,                     // #F0F0F5
    onSecondary = ThemeLightSecondaryForeground,         // #3F3F46
    secondaryContainer = ThemeLightMuted,                 // #F0F0F5
    onSecondaryContainer = ThemeLightSecondaryForeground,
    tertiary = Color(0xFF0284C7),                        // Telemetry Sky Blue
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE0F2FE),
    onTertiaryContainer = Color(0xFF0369A1),
    background = ThemeLightBackground,                   // #F7F7FA (Strava Clean Light)
    onBackground = ThemeLightForeground,                 // #18181B
    surface = ThemeLightCard,                             // #FFFFFF (Pure White)
    onSurface = ThemeLightForeground,                     // #18181B
    surfaceVariant = ThemeLightMuted,                     // #F0F0F5
    onSurfaceVariant = ThemeLightMutedForeground,         // #6B7280
    outline = ThemeLightBorder,                           // #E5E5EA
    outlineVariant = ThemeLightInput,                     // #E5E5EA
    error = ThemeLightDestructive,                        // #EF4444
    onError = ThemeLightDestructiveForeground             // #FFFFFF
)

/**
 * Shape scale derived from CSS token `--radius: 0.5rem;` (8.dp).
 */
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(12.dp),
    extraLarge = RoundedCornerShape(16.dp)
)

@Composable
fun StrideApexTheme(
    darkTheme: Boolean = ThemeState.isDark,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) StravaDarkColorScheme else StravaLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
