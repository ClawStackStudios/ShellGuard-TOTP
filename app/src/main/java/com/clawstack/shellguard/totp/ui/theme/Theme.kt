package com.clawstack.shellguard.totp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ── Theme Accents Enum ──────────────────────────────────────────
enum class ThemeAccent(
    val displayName: String,
    val primaryColor: Color,
    val secondaryColor: Color
) {
    REEF_DEFAULT("Reef Pink", BrandLobsterRed, BrandClawCyan),
    CYAN_VENT("Electric Cyan", BrandClawCyan, BrandLobsterRed),
    PURPLE_SHELL("Imperial Purple", Color(0xFFA855F7), BrandClawCyan),
    EMERALD_TRENCH("Emerald Bio-Flora", BrandEmerald, BrandClawCyan),
    AMBER_FLARE("Solar Vent", Color(0xFFF59E0B), BrandLobsterRed),
    MONOCHROME("Minimalist Pearl", Color(0xFFF8FAFC), Color(0xFF879298))
}

// ── Theme Mode ──────────────────────────────────────────────────
enum class AppThemeMode(val title: String, val description: String) {
    DARK("Abyssal Dark", "Trench noir dark palette"),
    LIGHT("Ocean Mist", "Luminous light mode for accessibility"),
    SYSTEM("System Default", "Follows Android OS appearance")
}

typealias ThemeMode = AppThemeMode

// ── Dynamic Color Scheme Carrier ─────────────────────────────────
data class ShellGuardCustomColors(
    val bgBase: Color,
    val bgSurface: Color,
    val bgElevated: Color,
    val textMain: Color,
    val textMuted: Color,
    val borderSubtle: Color,
    val primaryAccent: Color,
    val secondaryAccent: Color,
    val warning: Color = BrandCoralOrange,
    val danger: BrandLobsterRedColor = BrandLobsterRed
)

typealias BrandLobsterRedColor = Color

val LocalShellGuardColors = staticCompositionLocalOf {
    ShellGuardCustomColors(
        bgBase = DarkBgBase,
        bgSurface = DarkBgSurface,
        bgElevated = DarkBgElevated,
        textMain = DarkTextMain,
        textMuted = DarkTextMuted,
        borderSubtle = DarkBorderSubtle,
        primaryAccent = ThemeAccent.REEF_DEFAULT.primaryColor,
        secondaryAccent = ThemeAccent.REEF_DEFAULT.secondaryColor,
        warning = BrandCoralOrange,
        danger = BrandLobsterRed
    )
}

@Composable
fun ShellGuardTheme(
    themeMode: AppThemeMode = AppThemeMode.DARK,
    themeAccent: ThemeAccent = ThemeAccent.REEF_DEFAULT,
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        AppThemeMode.DARK -> true
        AppThemeMode.LIGHT -> false
        AppThemeMode.SYSTEM -> isSystemDark
    }

    val customColors = if (isDark) {
        ShellGuardCustomColors(
            bgBase = DarkBgBase,
            bgSurface = DarkBgSurface,
            bgElevated = DarkBgElevated,
            textMain = DarkTextMain,
            textMuted = DarkTextMuted,
            borderSubtle = DarkBorderSubtle,
            primaryAccent = themeAccent.primaryColor,
            secondaryAccent = themeAccent.secondaryColor,
            warning = BrandCoralOrange,
            danger = BrandLobsterRed
        )
    } else {
        val lightPrimary = if (themeAccent == ThemeAccent.MONOCHROME) Color(0xFF0F172A) else themeAccent.primaryColor
        val lightSecondary = if (themeAccent == ThemeAccent.MONOCHROME) Color(0xFF64748B) else themeAccent.secondaryColor
        ShellGuardCustomColors(
            bgBase = LightBgBase,
            bgSurface = LightBgSurface,
            bgElevated = LightBgElevated,
            textMain = LightTextMain,
            textMuted = LightTextMuted,
            borderSubtle = LightBorderSubtle,
            primaryAccent = lightPrimary,
            secondaryAccent = lightSecondary,
            warning = BrandCoralOrange,
            danger = BrandLobsterRed
        )
    }

    val materialColors = if (isDark) {
        darkColorScheme(
            primary = themeAccent.primaryColor,
            onPrimary = if (themeAccent == ThemeAccent.MONOCHROME) DarkBgBase else Color.White,
            primaryContainer = themeAccent.primaryColor.copy(alpha = 0.2f),
            onPrimaryContainer = themeAccent.primaryColor,
            secondary = themeAccent.secondaryColor,
            onSecondary = Color.White,
            tertiary = BrandCoralOrange,
            background = DarkBgBase,
            onBackground = DarkTextMain,
            surface = DarkBgSurface,
            onSurface = DarkTextMain,
            surfaceVariant = DarkBgElevated,
            onSurfaceVariant = DarkTextMuted,
            outline = DarkBorderSubtle,
            outlineVariant = themeAccent.primaryColor,
            error = BrandLobsterRed,
            onError = Color.White
        )
    } else {
        val lightPrimary = if (themeAccent == ThemeAccent.MONOCHROME) Color(0xFF0F172A) else themeAccent.primaryColor
        val lightSecondary = if (themeAccent == ThemeAccent.MONOCHROME) Color(0xFF64748B) else themeAccent.secondaryColor
        lightColorScheme(
            primary = lightPrimary,
            onPrimary = Color.White,
            primaryContainer = lightPrimary.copy(alpha = 0.15f),
            onPrimaryContainer = lightPrimary,
            secondary = lightSecondary,
            onSecondary = Color.White,
            tertiary = BrandCoralOrange,
            background = LightBgBase,
            onBackground = LightTextMain,
            surface = LightBgSurface,
            onSurface = LightTextMain,
            surfaceVariant = LightBgElevated,
            onSurfaceVariant = LightTextMuted,
            outline = LightBorderSubtle,
            outlineVariant = lightPrimary,
            error = BrandLobsterRed,
            onError = Color.White
        )
    }

    CompositionLocalProvider(LocalShellGuardColors provides customColors) {
        MaterialTheme(
            colorScheme = materialColors,
            typography = shellGuardTypography(customColors),
            content = content
        )
    }
}

// Legacy alias
val ShellGuardTotpColorScheme = darkColorScheme(
    primary = BrandLobsterRed,
    onPrimary = Color.White,
    secondary = BrandClawCyan,
    background = DarkBgBase,
    surface = DarkBgSurface
)


