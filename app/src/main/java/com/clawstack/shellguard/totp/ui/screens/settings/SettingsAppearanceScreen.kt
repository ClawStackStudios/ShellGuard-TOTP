package com.clawstack.shellguard.totp.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clawstack.shellguard.totp.data.preferences.EntryViewMode
import com.clawstack.shellguard.totp.data.preferences.IssuerDisplayMode
import com.clawstack.shellguard.totp.ui.theme.AppThemeMode
import com.clawstack.shellguard.totp.ui.theme.ThemeAccent
import com.clawstack.shellguard.totp.ui.viewmodels.AuthViewModel

/** Phase 11 / Task 22 — Appearance settings sub-screen (live StateFlow-backed). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsAppearanceScreen(
    authViewModel: AuthViewModel,
    onBackClick: () -> Unit
) {
    val prefs by authViewModel.appearancePrefs.collectAsStateWithLifecycle()
    val themeMode by authViewModel.themeMode.collectAsStateWithLifecycle()
    val themeAccent by authViewModel.themeAccent.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("🎨 Appearance", fontSize = 18.sp, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // ── Theme (Phase 11.5 / Task 22c — completes Task 22's original spec) ──
            Text(
                "Theme",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeOptionTile(
                    modifier = Modifier.weight(1f),
                    title = "Abyssal",
                    subtitle = "Dark Mode",
                    icon = Icons.Default.DarkMode,
                    isSelected = themeMode == AppThemeMode.DARK,
                    onClick = { authViewModel.setThemeMode(AppThemeMode.DARK) },
                    testTag = "theme_switcher_dark"
                )
                ThemeOptionTile(
                    modifier = Modifier.weight(1f),
                    title = "Contrast",
                    subtitle = "Light Mode",
                    icon = Icons.Default.LightMode,
                    isSelected = themeMode == AppThemeMode.LIGHT,
                    onClick = { authViewModel.setThemeMode(AppThemeMode.LIGHT) },
                    testTag = "theme_switcher_light"
                )
                ThemeOptionTile(
                    modifier = Modifier.weight(1f),
                    title = "System",
                    subtitle = "Default",
                    icon = Icons.Default.BrightnessAuto,
                    isSelected = themeMode == AppThemeMode.SYSTEM,
                    onClick = { authViewModel.setThemeMode(AppThemeMode.SYSTEM) },
                    testTag = "theme_switcher_system"
                )
            }
            Text(
                "Choose an accent scheme inspired by the marine bioluminescent defense system.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val accents = ThemeAccent.values()
            val firstRow = accents.take(3)
            val secondRow = accents.drop(3)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                firstRow.forEach { accent ->
                    AccentPaletteTile(
                        modifier = Modifier.weight(1f),
                        accent = accent,
                        isSelected = themeAccent == accent,
                        onClick = { authViewModel.setThemeAccent(accent) },
                        testTag = "accent_selector_${accent.name.lowercase()}"
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                secondRow.forEach { accent ->
                    AccentPaletteTile(
                        modifier = Modifier.weight(1f),
                        accent = accent,
                        isSelected = themeAccent == accent,
                        onClick = { authViewModel.setThemeAccent(accent) },
                        testTag = "accent_selector_${accent.name.lowercase()}"
                    )
                }
            }

            // ── Entries ──
            Text(
                "Entries",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            SettingsSelectorRow(
                title = "View mode",
                options = listOf(EntryViewMode.NORMAL, EntryViewMode.COMPACT),
                selected = prefs.viewMode,
                label = { if (it == EntryViewMode.NORMAL) "Normal" else "Compact" },
                onSelect = authViewModel::setViewMode
            )
            SettingsSwitchRow("Show issuer icons", "Render brand icons on account cards", prefs.showIcons, authViewModel::setShowIcons)
            SettingsSwitchRow("Show next code", "Preview the upcoming TOTP code", prefs.showNextCode, authViewModel::setShowNextCode)
            SettingsSwitchRow("Expiration blink indicator", "Blink the countdown when expiring", prefs.expireBlinkIndicator, authViewModel::setExpireBlinkIndicator)
            SettingsSelectorRow(
                title = "Issuer / account display",
                options = listOf(IssuerDisplayMode.ISSUER_AND_ACCOUNT, IssuerDisplayMode.ISSUER_ONLY, IssuerDisplayMode.ACCOUNT_ONLY),
                selected = prefs.issuerDisplayMode,
                label = {
                    when (it) {
                        IssuerDisplayMode.ISSUER_AND_ACCOUNT -> "Both"
                        IssuerDisplayMode.ISSUER_ONLY -> "Issuer"
                        IssuerDisplayMode.ACCOUNT_ONLY -> "Account"
                    }
                },
                onSelect = authViewModel::setIssuerDisplayMode
            )
            SettingsSwitchRow("Digit grouping", "Split codes into groups (123 456)", prefs.digitGrouping, authViewModel::setDigitGrouping)
            if (prefs.hiddenGroups.isNotEmpty()) {
                Text(
                    "Hidden groups: ${prefs.hiddenGroups.sorted().joinToString(", ")}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
