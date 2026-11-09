package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val ObsidianDarkColorScheme = darkColorScheme(
    primary = VoltLime,
    onPrimary = OnVoltLime,
    primaryContainer = SlateSurfaceElevated,
    onPrimaryContainer = VoltLime,
    secondary = KineticCoral,
    onSecondary = OnKineticCoral,
    secondaryContainer = SlateSurfaceElevated,
    onSecondaryContainer = KineticCoral,
    tertiary = AeroCyan,
    onTertiary = OnAeroCyan,
    tertiaryContainer = SlateSurfaceElevated,
    onTertiaryContainer = AeroCyan,
    background = ObsidianBackground,
    onBackground = TextPrimary,
    surface = SlateSurface,
    onSurface = TextPrimary,
    surfaceVariant = SlateSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = SlateBorder,
    error = ErrorCrimson
)

@Composable
fun StrideApexTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ObsidianDarkColorScheme,
        typography = Typography,
        content = content
    )
}
