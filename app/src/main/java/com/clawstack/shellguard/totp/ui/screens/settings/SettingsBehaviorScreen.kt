package com.clawstack.shellguard.totp.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import com.clawstack.shellguard.totp.data.preferences.SearchScope
import com.clawstack.shellguard.totp.ui.viewmodels.AuthViewModel

/** Phase 11 / Task 22 — Behavior settings sub-screen (live StateFlow-backed). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBehaviorScreen(
    authViewModel: AuthViewModel,
    onBackClick: () -> Unit
) {
    val prefs by authViewModel.behaviorPrefs.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("⚡ Behavior", fontSize = 18.sp, fontWeight = FontWeight.SemiBold) },
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
                title = "Search scope",
                options = listOf(SearchScope.ALL, SearchScope.LOCAL_ONLY, SearchScope.REMOTE_ONLY),
                selected = prefs.searchScope,
                label = {
                    when (it) {
                        SearchScope.ALL -> "All"
                        SearchScope.LOCAL_ONLY -> "Local"
                        SearchScope.REMOTE_ONLY -> "Synced"
                    }
                },
                onSelect = authViewModel::setSearchScope
            )
            SettingsSwitchRow("Focus search on start", "Open the dashboard with search focused", prefs.focusSearchOnStart, authViewModel::setFocusSearchOnStart)
            SettingsSwitchRow("Minimize on copy", "Minimize the entry after copying its code", prefs.minimizeOnCopy, authViewModel::setMinimizeOnCopy)
            SettingsSwitchRow("Copy tokens on tap", "Tap an entry to copy its code", prefs.copyOnTap, authViewModel::setCopyOnTap)
            SettingsSwitchRow("Haptic feedback", "Vibrate on copy and interactions", prefs.hapticFeedback, authViewModel::setHapticFeedback)
            SettingsSwitchRow("Multiselect groups", "Filter multiple categories at once", prefs.multiselectGroups, authViewModel::setMultiselectGroups)
            SettingsSwitchRow("Highlight tokens on tap", "Emphasize the tapped code", prefs.highlightTokensOnTap, authViewModel::setHighlightTokensOnTap)
            SettingsSwitchRow("Freeze tokens on tap", "Pause the countdown while viewing a code", prefs.freezeTokensOnTap, authViewModel::setFreezeTokensOnTap)
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
