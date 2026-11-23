package com.auratv.player.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object AuraColors {
    val BgObsidian = Color(0xFF090C10)
    val BgSurface = Color(0xFF11161E)
    val BgSurfaceElevated = Color(0xFF18202C)
    val BgSurfaceHover = Color(0xFF202B3A)
    val BorderSubtle = Color(0xFF263242)
    val Emerald = Color(0xFF00E5A0)
    val EmeraldDark = Color(0xFF00B880)
    val Violet = Color(0xFF7C5CFF)
    val LiveRed = Color(0xFFFF4D67)
    val Amber = Color(0xFFFFB84D)
    val TextPrimary = Color(0xFFF3F6FA)
    val TextSecondary = Color(0xFF9AA8BA)
    val TextMuted = Color(0xFF66758A)
}

private val AuraDarkScheme = darkColorScheme(
    primary = AuraColors.Emerald,
    onPrimary = Color(0xFF041A12),
    secondary = AuraColors.Violet,
    onSecondary = Color.White,
    background = AuraColors.BgObsidian,
    onBackground = AuraColors.TextPrimary,
    surface = AuraColors.BgSurface,
    onSurface = AuraColors.TextPrimary,
    surfaceVariant = AuraColors.BgSurfaceElevated,
    onSurfaceVariant = AuraColors.TextSecondary,
    outline = AuraColors.BorderSubtle
)

@Composable
fun AuraTvTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AuraDarkScheme,
        content = content
    )
}
