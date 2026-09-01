package com.clawstack.shellguard.totp.ui.screens

import android.net.Uri
import android.widget.Toast
import com.clawstack.shellguard.totp.BuildConfig
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clawstack.shellguard.totp.ShellGuardTotpApp
import com.clawstack.shellguard.totp.data.repository.VaultProtectionMode
import com.clawstack.shellguard.totp.ui.components.SpotlightOverlay
import com.clawstack.shellguard.totp.ui.theme.AppThemeMode
import com.clawstack.shellguard.totp.ui.theme.ThemeAccent
import androidx.compose.ui.graphics.Brush
import com.clawstack.shellguard.totp.ui.viewmodels.TotpViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    onNavigateToGateway: () -> Unit,
    totpViewModel: TotpViewModel? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as ShellGuardTotpApp
    val authRepo = app.authRepository
    val totpRepo = app.totpRepository
    val backupManager = app.backupManager

    val currentSession by authRepo.currentSession.collectAsStateWithLifecycle()
    val isBiometricEnabled by authRepo.isBiometricEnabled.collectAsStateWithLifecycle()
    val themeMode by authRepo.themeMode.collectAsStateWithLifecycle()
    val themeAccent by authRepo.themeAccent.collectAsStateWithLifecycle()
    val vaultMode by authRepo.vaultMode.collectAsStateWithLifecycle()
    val tourStep by authRepo.tourStep.collectAsStateWithLifecycle()
    val isAutoClearClipboard by authRepo.isAutoClearClipboard.collectAsStateWithLifecycle()

    val offlineCodesCount = totpViewModel?.offlineCodesCount?.collectAsStateWithLifecycle()?.value ?: 0
    val syncedCodesCount = totpViewModel?.syncedCodesCount?.collectAsStateWithLifecycle()?.value ?: 0

    var connectButtonCenter by remember { mutableStateOf<Offset?>(null) }
    var isSyncing by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showChangeProtectionDialog by remember { mutableStateOf(false) }

    var selectedNewMode by remember { mutableStateOf(VaultProtectionMode.PIN) }
    var newSecretInput by remember { mutableStateOf("") }
    var confirmSecretInput by remember { mutableStateOf("") }
    var isNewSecretVisible by remember { mutableStateOf(false) }
    var changeSecretError by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()

    // Export Document Launcher
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri: Uri? ->
        uri?.let { destUri ->
            coroutineScope.launch {
                try {
                    context.contentResolver.openOutputStream(destUri)?.use { out ->
                        val rawKey = authRepo.getVaultSecret() ?: currentSession?.rawHuKey ?: "shellguard_default_master_key"
                        val ownerUuid = currentSession?.userUuid ?: "local"
                        val protMode = vaultMode.name
                        val result = backupManager.exportEncryptedBackup(
                            outputStream = out,
                            rawKey = rawKey,
                            ownerUuid = ownerUuid,
                            protectionMode = protMode,
                            isBiometricEnabled = isBiometricEnabled,
                            pinLength = if (vaultMode == VaultProtectionMode.PIN) rawKey.length else null
                        )
                        if (result.isSuccess) {
                            Toast.makeText(context, "Exported ${result.getOrNull()} items securely (.sgtotp.bak).", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "Export failed: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Export error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Import Document Launcher
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { srcUri ->
            coroutineScope.launch {
                try {
                    context.contentResolver.openInputStream(srcUri)?.use { input ->
                        val rawKey = authRepo.getVaultSecret() ?: currentSession?.rawHuKey ?: "shellguard_default_master_key"
                        val ownerUuid = currentSession?.userUuid ?: "local"
                        val result = backupManager.importEncryptedBackup(input, rawKey, ownerUuid)
                        if (result.isSuccess) {
                            Toast.makeText(context, "Restored ${result.getOrNull()} items successfully.", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "Restore failed: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Restore error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings & Security",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Appearance & Accessibility (Theme Switcher) ───────────
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("theme_settings_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "APPEARANCE & ACCESSIBILITY",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Choose your display theme. High-Contrast Light enhances daytime readability and accessibility.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 1. Dark (Abyssal)
                        ThemeOptionTile(
                            modifier = Modifier.weight(1f),
                            title = "Abyssal",
                            subtitle = "Dark Mode",
                            icon = Icons.Default.DarkMode,
                            isSelected = themeMode == AppThemeMode.DARK,
                            onClick = { authRepo.setThemeMode(AppThemeMode.DARK) },
                            testTag = "theme_switcher_dark"
                        )

                        // 2. High-Contrast (Ocean Mist)
                        ThemeOptionTile(
                            modifier = Modifier.weight(1f),
                            title = "Contrast",
                            subtitle = "Light Mode",
                            icon = Icons.Default.LightMode,
                            isSelected = themeMode == AppThemeMode.LIGHT,
                            onClick = { authRepo.setThemeMode(AppThemeMode.LIGHT) },
                            testTag = "theme_switcher_light"
                        )

                        // 3. System
                        ThemeOptionTile(
                            modifier = Modifier.weight(1f),
                            title = "System",
                            subtitle = "Default",
                            icon = Icons.Default.BrightnessAuto,
                            isSelected = themeMode == AppThemeMode.SYSTEM,
                            onClick = { authRepo.setThemeMode(AppThemeMode.SYSTEM) },
                            testTag = "theme_switcher_system"
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        text = "ACCENT PALETTES",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Choose an accent scheme inspired by the marine bioluminescent defense system.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // 2 rows of 3 accent palettes
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
                                onClick = { authRepo.setThemeAccent(accent) },
                                testTag = "accent_selector_${accent.name.lowercase()}"
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        secondRow.forEach { accent ->
                            AccentPaletteTile(
                                modifier = Modifier.weight(1f),
                                accent = accent,
                                isSelected = themeAccent == accent,
                                onClick = { authRepo.setThemeAccent(accent) },
                                testTag = "accent_selector_${accent.name.lowercase()}"
                            )
                        }
                    }
                }
            }

            // ── Vault Sync Info ─────────────────────────────────────
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(if (currentSession != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (currentSession != null) "Connected Vault" else "Offline Cache Mode",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            )
                        }

                        if (currentSession != null) {
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        isSyncing = true
                                        val session = currentSession!!
                                        totpRepo.syncRemoteVault(session.serverUrl, session.rawHuKey, session.userUuid)
                                        isSyncing = false
                                        Toast.makeText(context, "Vault synchronized.", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                enabled = !isSyncing,
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier.testTag("settings_sync_button")
                            ) {
                                if (isSyncing) {
                                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
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
                                onClick = onNavigateToGateway,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                                modifier = Modifier
                                    .onGloballyPositioned { coordinates ->
                                        val pos = coordinates.positionInRoot()
                                        val size = coordinates.size
                                        connectButtonCenter = Offset(pos.x + size.width / 2f, pos.y + size.height / 2f)
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
                }
            }

            // ── Local Storage & Offline Codes ──────────────────────
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_offline_codes_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "LOCAL STORAGE & OFFLINE CODES",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Offline-only codes remain securely encrypted on this device and are never pushed to the server unless enabled.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Offline Codes: $offlineCodesCount",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Synced Remote Codes: $syncedCodesCount",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                totpViewModel?.filterOfflineCodesOnly()
                                onBackClick()
                            },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                            modifier = Modifier.testTag("settings_display_offline_codes_button")
                        ) {
                            Text("Display on Dashboard", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // ── Biometric & Security Settings ───────────────────────
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Biometric Quick Unlock",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Hardware KeyStore backing",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        Switch(
                            checked = isBiometricEnabled,
                            onCheckedChange = { authRepo.setBiometricEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.background,
                                checkedTrackColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.testTag("biometric_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (vaultMode == VaultProtectionMode.PIN) Icons.Default.Dialpad else Icons.Default.Key,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Vault Protection Method",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = if (vaultMode == VaultProtectionMode.PIN) "4–8 Digit PIN Code" else "Master Password",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        OutlinedButton(
                            onClick = {
                                selectedNewMode = vaultMode
                                newSecretInput = ""
                                confirmSecretInput = ""
                                changeSecretError = null
                                showChangeProtectionDialog = true
                            },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier.testTag("change_protection_method_button")
                        ) {
                            Text("Change", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Auto-Scrub Clipboard",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Clears OTP from clipboard in 30s",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        Switch(
                            checked = isAutoClearClipboard,
                            onCheckedChange = { authRepo.setAutoClearClipboard(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.background,
                                checkedTrackColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.testTag("auto_clear_clipboard_switch")
                        )
                    }
                }
            }

            // ── Encrypted Backup & Restore ──────────────────────────
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "ENCRYPTED BACKUP & RESTORE",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { exportLauncher.launch("shellguard-totp-backup.sgtotp.bak") },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("export_backup_button")
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }

                        OutlinedButton(
                            onClick = { importLauncher.launch(arrayOf("*/*", "application/octet-stream", "application/json")) },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("import_backup_button")
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Restore", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                    }
                }
            }

            // ── Disconnect / Logout ─────────────────────────────────
            if (currentSession != null) {
                Button(
                    onClick = { showLogoutDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("settings_logout_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.15f))
                ) {
                    Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Disconnect Vault", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── Release & Integrity Footer ──────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "ShellGuard Authenticator • v${BuildConfig.VERSION_NAME}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Zero-Knowledge • Hardware KeyStore AES-256-GCM",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontSize = 11.sp
                )
            }
        }
    }

    if (showChangeProtectionDialog) {
        val isPinSelection = selectedNewMode == VaultProtectionMode.PIN
        AlertDialog(
            onDismissRequest = { showChangeProtectionDialog = false },
            title = {
                Text(
                    text = "Update Vault Protection",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "Choose your authentication method and enter a new secret.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Mode Selection Tabs
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                selectedNewMode = VaultProtectionMode.PIN
                                newSecretInput = ""
                                confirmSecretInput = ""
                                changeSecretError = null
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(
                                if (isPinSelection) 2.dp else 1.dp,
                                if (isPinSelection) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            ),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isPinSelection) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent
                            )
                        ) {
                            Icon(Icons.Default.Dialpad, contentDescription = null, tint = if (isPinSelection) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("PIN Code", color = if (isPinSelection) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                selectedNewMode = VaultProtectionMode.PASSWORD
                                newSecretInput = ""
                                confirmSecretInput = ""
                                changeSecretError = null
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(
                                if (!isPinSelection) 2.dp else 1.dp,
                                if (!isPinSelection) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            ),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (!isPinSelection) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent
                            )
                        ) {
                            Icon(Icons.Default.Key, contentDescription = null, tint = if (!isPinSelection) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Password", color = if (!isPinSelection) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = newSecretInput,
                        onValueChange = { input ->
                            if (isPinSelection) {
                                if (input.length <= 8 && input.all { it.isDigit() }) newSecretInput = input
                            } else {
                                newSecretInput = input
                            }
                        },
                        label = { Text(if (isPinSelection) "New PIN (4–8 digits)" else "New Master Password") },
                        placeholder = { Text(if (isPinSelection) "Enter digits" else "Enter password") },
                        visualTransformation = if (isNewSecretVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = if (isPinSelection) KeyboardType.NumberPassword else KeyboardType.Password
                        ),
                        trailingIcon = {
                            IconButton(onClick = { isNewSecretVisible = !isNewSecretVisible }) {
                                Icon(
                                    imageVector = if (isNewSecretVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle Visibility",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_new_secret_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = confirmSecretInput,
                        onValueChange = { input ->
                            if (isPinSelection) {
                                if (input.length <= 8 && input.all { it.isDigit() }) confirmSecretInput = input
                            } else {
                                confirmSecretInput = input
                            }
                        },
                        label = { Text(if (isPinSelection) "Confirm PIN" else "Confirm Password") },
                        visualTransformation = if (isNewSecretVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = if (isPinSelection) KeyboardType.NumberPassword else KeyboardType.Password
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_confirm_secret_input")
                    )

                    if (changeSecretError != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = changeSecretError!!,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (isPinSelection && newSecretInput.length < 4) {
                            changeSecretError = "PIN must be between 4 and 8 digits."
                            return@Button
                        }
                        if (!isPinSelection && newSecretInput.length < 6) {
                            changeSecretError = "Password must be at least 6 characters."
                            return@Button
                        }
                        if (newSecretInput != confirmSecretInput) {
                            changeSecretError = "Secrets do not match."
                            return@Button
                        }

                        authRepo.updateVaultSecret(newSecretInput, isPin = isPinSelection)
                        showChangeProtectionDialog = false
                        Toast.makeText(context, "Vault protection updated.", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.testTag("settings_save_secret_button")
                ) {
                    Text("Save", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangeProtectionDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurface)
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Disconnect Vault?", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
            text = { Text("Your local cached 2FA tokens will remain safely on this device. You will need to log in again to sync changes.", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        authRepo.logout()
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

    // ── Interactive Spotlight Guided Tour (Step 2: Connect to Server) ───
    SpotlightOverlay(
        visible = tourStep == 2,
        step = 2,
        targetCenter = connectButtonCenter,
        targetRadius = 85f,
        onNext = {
            authRepo.setGuidedTourCompleted(true)
            onNavigateToGateway()
        },
        onDismiss = {
            authRepo.setGuidedTourCompleted(true)
        }
    )
}

@Composable
private fun ThemeOptionTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val activeBorderColor = MaterialTheme.colorScheme.primary
    val inactiveBorderColor = MaterialTheme.colorScheme.outline
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) activeBorderColor else inactiveBorderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 13.sp
            )
            Text(
                text = subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
            if (isSelected) {
                Spacer(modifier = Modifier.height(4.dp))
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun AccentPaletteTile(
    accent: ThemeAccent,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val activeBorderColor = accent.primaryColor
    val inactiveBorderColor = MaterialTheme.colorScheme.outline
    val backgroundColor = if (isSelected) {
        accent.primaryColor.copy(alpha = 0.15f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) activeBorderColor else inactiveBorderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 6.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Gradient swatch pill
            Box(
                modifier = Modifier
                    .size(width = 36.dp, height = 18.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(accent.primaryColor, accent.secondaryColor)
                        )
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(9.dp))
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = accent.displayName.split(" ").firstOrNull() ?: accent.name,
                color = if (isSelected) accent.primaryColor else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 11.sp,
                maxLines = 1
            )
            if (isSelected) {
                Spacer(modifier = Modifier.height(2.dp))
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected Accent",
                    tint = accent.primaryColor,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}


