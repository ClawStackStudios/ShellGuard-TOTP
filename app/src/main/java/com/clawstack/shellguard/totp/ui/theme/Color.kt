package com.clawstack.shellguard.totp.ui.theme

import androidx.compose.ui.graphics.Color

// ── Canonical ShellGuard Brand Colors ───────────────────────────
val BrandLobsterRed = Color(0xFFE4048A)      // Primary Action / Brand Gradient
val BrandClawCyan = Color(0xFF06B6D4)        // Secondary Action / Active Vents
val BrandPurple = Color(0xFF3B0764)          // ShellGuard Dark Purple
val BrandCoralOrange = Color(0xFFF97316)      // Countdown Warning (< 10s)
val BrandEmerald = Color(0xFF10B981)          // Success / Validated State

// ── Dark Mode Tokens (Abyssal Dark — Default) ───────────────────
val DarkBgBase = Color(0xFF0F1419)           // Canvas Viewport Floor
val DarkBgSurface = Color(0xFF171C21)        // Card / Container Surface
val DarkBgElevated = Color(0xFF1E252C)       // Sheets / Modals / Toolbars
val DarkTextMain = Color(0xFFDEE3EA)         // Luminous Shell Headlines & Codes
val DarkTextMuted = Color(0xFF879298)        // Secondary Subtitles & Timestamps
val DarkBorderSubtle = Color(0xFF3D484E)     // Carapace Ridge 1dp Outlines

// ── Light Mode Tokens (Ocean Mist) ──────────────────────────────
val LightBgBase = Color(0xFFF1F5F9)          // Ocean Mist Canvas
val LightBgSurface = Color(0xFFFFFFFF)       // Crisp White Card Surface
val LightBgElevated = Color(0xFFF8FAFC)      // Elevated Surfaces
val LightTextMain = Color(0xFF0F172A)        // Slate 900 Typography
val LightTextMuted = Color(0xFF64748B)       // Slate 500 Subtitles
val LightBorderSubtle = Color(0xFFCBD5E1)    // Slate 300 Outlines

// ── Warning & Notice Badges ─────────────────────────────────────
val WarningBoxBg = Color(0x26F97316)         // Amber Warning Container (15% Alpha)
val WarningText = Color(0xFFFDBA74)          // Amber Warning Text

// ── Backward-Compatible Aliases ─────────────────────────────────
val AbyssalDeep = DarkBgBase
val ShellSurface = DarkBgSurface
val ShellSurfaceElevated = DarkBgElevated
val ShellBorder = DarkBorderSubtle
val ShellBorderActive = BrandClawCyan
val ClawCyan = BrandClawCyan
val ClawCyanGlow = Color(0x3306B6D4)
val LobsterRed = BrandLobsterRed
val CoralOrange = BrandCoralOrange
val TextPearl = DarkTextMain
val TextMuted = DarkTextMuted
val LightCanvas = LightBgBase
val LightClawCyan = Color(0xFF0284C7)
val LightClawCyanGlow = Color(0x260284C7)
val LightLobsterRed = Color(0xFFDC2626)
val LightCoralOrange = Color(0xFFEA580C)
val LightTextPearl = LightTextMain


