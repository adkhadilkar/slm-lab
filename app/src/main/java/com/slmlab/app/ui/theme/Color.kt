package com.slmlab.app.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * "Pop Paper" look: pale lavender paper, ink outlines, hard offset shadows and flat bright
 * colour blocks. Text on a bright block is always [PopPalette.onBlock] (dark ink).
 */
data class PopPalette(
    /** Outline and hard-shadow colour. */
    val ink: Color,
    /** Text and icons on a bright block (yellow, pink, green, orange). */
    val onBlock: Color,
    val yellow: Color,
    val pink: Color,
    val green: Color,
    val orange: Color,
    /** Strong block for the current item and primary actions; text on it is [onBlue]. */
    val blue: Color,
    val onBlue: Color,
    /** Block for stop and destructive actions; text on it is [onBlock]. */
    val red: Color,
)

private val Ink = Color(0xFF14121F)

val LightPop = PopPalette(
    ink = Ink,
    onBlock = Ink,
    yellow = Color(0xFFFFD23F),
    pink = Color(0xFFFF8FC7),
    green = Color(0xFF7BE495),
    orange = Color(0xFFFFB347),
    blue = Color(0xFF3D5AFE),
    onBlue = Color(0xFFFFFFFF),
    red = Color(0xFFFF8A80),
)

val DarkPop = PopPalette(
    ink = Color(0xFFF1EEFF),
    onBlock = Ink,
    yellow = Color(0xFFFFD23F),
    pink = Color(0xFFFF8FC7),
    green = Color(0xFF7BE495),
    orange = Color(0xFFFFB347),
    blue = Color(0xFF3D5AFE),
    onBlue = Color(0xFFFFFFFF),
    red = Color(0xFFFF8A80),
)

val LocalPop = staticCompositionLocalOf { LightPop }

// Material roles, light.
val LightBackground = Color(0xFFF1EEFF)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFE7E2FA)
val LightOutline = Ink
val LightOnBackground = Ink
val LightMuted = Color(0xFF55506B)
val LightPrimary = Color(0xFF3D5AFE)
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFFFFD23F)
val LightOnPrimaryContainer = Ink
val LightSecondary = Color(0xFFB4126C)
val LightOnSecondary = Color(0xFFFFFFFF)
val LightSecondaryContainer = Color(0xFFFFB347)
val LightOnSecondaryContainer = Ink
val LightTertiary = Color(0xFF0B7A3B)
val LightError = Color(0xFFC62828)
val LightOnError = Color(0xFFFFFFFF)

// Material roles, dark.
val DarkBackground = Color(0xFF14121F)
val DarkSurface = Color(0xFF1F1C2E)
val DarkSurfaceVariant = Color(0xFF2A2640)
val DarkOutline = Color(0xFFF1EEFF)
val DarkOnBackground = Color(0xFFF1EEFF)
val DarkMuted = Color(0xFFB9B3D6)
val DarkPrimary = Color(0xFF9DB0FF)
val DarkOnPrimary = Ink
val DarkPrimaryContainer = Color(0xFFFFD23F)
val DarkOnPrimaryContainer = Ink
val DarkSecondary = Color(0xFFFF8FC7)
val DarkOnSecondary = Ink
val DarkSecondaryContainer = Color(0xFFFFB347)
val DarkOnSecondaryContainer = Ink
val DarkTertiary = Color(0xFF7BE495)
val DarkError = Color(0xFFFF8A80)
val DarkOnError = Ink
