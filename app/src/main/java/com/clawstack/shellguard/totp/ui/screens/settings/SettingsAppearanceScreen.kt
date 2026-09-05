package com.clawstack.shellguard.totp.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.clawstack.shellguard.totp.ui.viewmodels.AuthViewModel

/** Phase 11 / Task 22 — Appearance settings sub-screen (live StateFlow-backed). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsAppearanceScreen(
    authViewModel: AuthViewModel,
    onBackClick: () -> Unit
) {
    val prefs by authViewModel.appearancePrefs.collectAsStateWithLifecycle()

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
