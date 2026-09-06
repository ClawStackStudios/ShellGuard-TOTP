package com.clawstack.shellguard.totp.ui.screens.settings

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clawstack.shellguard.totp.data.repository.UserSession
import com.clawstack.shellguard.totp.ui.components.SpotlightOverlay
import com.clawstack.shellguard.totp.ui.viewmodels.AuthViewModel
import com.clawstack.shellguard.totp.ui.viewmodels.TotpViewModel

/**
 * Phase 11.5 / Task 22c — "☁️ Server & Sync" hub sub-screen.
 *
 * Re-homes the v0.0.1.3 legacy Server Sync controls (Connect to Server →
 * Gateway login form, Sync Now, connection status, Disconnect Vault) with a
 * semantic home in the Categorized Settings Hub, and hosts Spotlight Tour
 * step 2 (the "Connect to Server" spotlight) migrated from the legacy screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsServerSyncScreen(
    authViewModel: AuthViewModel,
    totpViewModel: TotpViewModel,
    onBackClick: () -> Unit,
    onNavigateToGateway: () -> Unit
) {
    val context = LocalContext.current
    val currentSession by authViewModel.currentSession.collectAsStateWithLifecycle()
    val isSyncing by totpViewModel.isSyncing.collectAsStateWithLifecycle()
    val tourStep by authViewModel.tourStep.collectAsStateWithLifecycle()

    var connectButtonCenter by remember { mutableStateOf<Offset?>(null) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("☁️ Server & Sync", fontSize = 18.sp, fontWeight = FontWeight.SemiBold) },
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
            ServerSyncStatusCard(
                currentSession = currentSession,
                isSyncing = isSyncing,
                onSyncNow = {
                    totpViewModel.refreshRemoteVault { success ->
                        Toast.makeText(
                            context,
                            if (success) "Vault synchronized." else "Sync failed: check your connection.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                onConnectToServer = onNavigateToGateway,
                onConnectButtonPositioned = { center -> connectButtonCenter = center },
                onDisconnectRequested = { showLogoutDialog = true }
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // ── Disconnect confirmation (legacy parity) ──
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Disconnect Vault?") },
            text = { Text("Your local codes remain on this device. You can reconnect to the server at any time.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        authViewModel.logout()
                        onNavigateToGateway()
                    }
                ) {
                    Text("Disconnect", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurface)
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }

    // ── Interactive Spotlight Guided Tour (Step 2: Connect to Server) — migrated from legacy screen ──
    SpotlightOverlay(
        visible = tourStep == 2,
        step = 2,
        targetCenter = connectButtonCenter,
        targetRadius = 85f,
        onNext = {
            authViewModel.setGuidedTourCompleted()
            onNavigateToGateway()
        },
        onDismiss = {
            authViewModel.setGuidedTourCompleted()
        }
    )
}

/** Phase 11.5 / Task 22c — status card extracted for readability; legacy copy parity preserved. */
@Composable
private fun ServerSyncStatusCard(
    currentSession: UserSession?,
    isSyncing: Boolean,
    onSyncNow: () -> Unit,
    onConnectToServer: () -> Unit,
    onConnectButtonPositioned: (Offset) -> Unit,
    onDisconnectRequested: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CloudSync,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("Vault Synchronization", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    Text(
                        if (currentSession != null) "Connected Vault" else "Offline Cache Mode",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (currentSession != null) {
                    Button(
                        onClick = onSyncNow,
                        enabled = !isSyncing,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.testTag("settings_sync_button")
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = "Sync",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Sync Now", color = MaterialTheme.colorScheme.onPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = onConnectToServer,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .onGloballyPositioned { coordinates ->
                                val pos = coordinates.positionInRoot()
                                val size = coordinates.size
                                onConnectButtonPositioned(Offset(pos.x + size.width / 2f, pos.y + size.height / 2f))
                            }
                            .testTag("settings_connect_button")
                    ) {
                        Text("Connect to Server", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Server: ${currentSession?.serverUrl ?: "None (Standalone)"}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )
            Text(
                text = "User: ${currentSession?.username ?: "Local User"}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )

            if (currentSession != null) {
                Spacer(modifier = Modifier.height(12.dp))
                TextButton(
                    onClick = onDisconnectRequested,
                    modifier = Modifier.testTag("settings_disconnect_button")
                ) {
                    Text("Disconnect Vault", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}