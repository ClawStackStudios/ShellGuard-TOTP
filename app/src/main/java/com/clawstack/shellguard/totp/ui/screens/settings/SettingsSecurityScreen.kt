package com.clawstack.shellguard.totp.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clawstack.shellguard.totp.data.repository.VaultProtectionMode
import com.clawstack.shellguard.totp.ui.viewmodels.AuthViewModel

/**
 * Phase 12 / Task 24: Security sub-screen.
 * Controls encryption status display, runtime screen security (FLAG_SECURE),
 * tap-to-reveal timeout configuration, and emergency panic purge settings.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSecurityScreen(
    authViewModel: AuthViewModel,
    onBackClick: () -> Unit
) {
    val vaultMode by authViewModel.vaultMode.collectAsStateWithLifecycle()
    val isBiometricEnabled by authViewModel.isBiometricEnabled.collectAsStateWithLifecycle()
    val allowScreenshots by authViewModel.allowScreenshots.collectAsStateWithLifecycle()
    val tapRevealTimeout by authViewModel.tapRevealTimeoutSeconds.collectAsStateWithLifecycle()
    val panicTriggerEnabled by authViewModel.panicTriggerEnabled.collectAsStateWithLifecycle()

    var showScreenshotRiskDialog by remember { mutableStateOf(false) }
    var showPanicConfirmDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("🔐 Security", fontSize = 18.sp, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Spacer(modifier = Modifier.height(6.dp))

            // 1. Encryption Status Tile
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Vault Hardware Encryption",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text = "• Protection Mode: ${if (vaultMode == VaultProtectionMode.PIN) "Hardware PIN" else "Master Password"}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "• Biometric Authentication: ${if (isBiometricEnabled) "Active (Hardware-backed)" else "Disabled"}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "• Storage at Rest: SQLCipher AES-256 (Page-aligned)",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 2. Screen Security Toggle (FLAG_SECURE)
            SettingsSwitchRow(
                title = "Allow screenshots",
                subtitle = "Disables anti-snoop protection for this device.",
                checked = allowScreenshots,
                onCheckedChange = { desired ->
                    if (desired) {
                        showScreenshotRiskDialog = true
                    } else {
                        authViewModel.setAllowScreenshots(false)
                    }
                }
            )

            // 3. Tap-to-Reveal Timeout Selector
            SettingsSelectorRow(
                title = "Tap-to-reveal timeout",
                options = listOf(10, 30, 60),
                selected = tapRevealTimeout,
                label = { "${it}s" },
                onSelect = { authViewModel.setTapRevealTimeout(it) }
            )

            // 4. Panic Purge Toggle
            SettingsSwitchRow(
                title = "Delete vault on panic trigger",
                subtitle = "Purges encryption keys and local database upon receiving emergency broadcast.",
                checked = panicTriggerEnabled,
                onCheckedChange = { desired ->
                    if (desired) {
                        showPanicConfirmDialog = true
                    } else {
                        authViewModel.setPanicTriggerEnabled(false)
                    }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Confirmation Dialog: Screen Security Opt-In Risk
    if (showScreenshotRiskDialog) {
        AlertDialog(
            onDismissRequest = { showScreenshotRiskDialog = false },
            title = { Text("Allow screenshots?") },
            text = {
                Text(
                    "Other apps, screen recorders, and the task-switcher may capture your 2FA codes while this is enabled."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        authViewModel.setAllowScreenshots(true)
                        showScreenshotRiskDialog = false
                    }
                ) {
                    Text("Allow", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showScreenshotRiskDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Confirmation Dialog: Emergency Panic Purge
    if (showPanicConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showPanicConfirmDialog = false },
            title = { Text("Enable Emergency Panic Purge?") },
            text = {
                Text(
                    "All local encryption keys and database tables will be irreversibly erased if an authorized panic broadcast is received."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        authViewModel.setPanicTriggerEnabled(true)
                        showPanicConfirmDialog = false
                    }
                ) {
                    Text("Enable", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPanicConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
