package com.clawstack.shellguard.totp.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.clawstack.shellguard.totp.data.migration.BackupSchemaType
import com.clawstack.shellguard.totp.data.migration.ParsedTotpItem
import com.clawstack.shellguard.totp.data.migration.PreValidationResult
import com.clawstack.shellguard.totp.ui.dialogs.BitwardenImportPreviewDialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import com.clawstack.shellguard.totp.data.migration.MultiFormatMigrationEngine
import com.clawstack.shellguard.totp.ui.dialogs.GoogleAuthExportViewerDialog
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBackupsScreen(
    navController: NavController,
    hasRemoteGateway: Boolean = false // Passed in from ViewModel realistically
) {
    val context = LocalContext.current
    var showImportDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var requireStepUpAuth by remember { mutableStateOf(false) }

    var preValidationData by remember { mutableStateOf<PreValidationResult?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val parseResult = MultiFormatMigrationEngine.parseStream(inputStream)
                    preValidationData = PreValidationResult(
                        isValid = true,
                        schemaType = parseResult.schemaType,
                        tokenCount = parseResult.items.size,
                        parsedTokens = parseResult.items
                    )
                    showImportDialog = true
                }
            } catch (e: Exception) {
                // handle error appropriately
            }
        }
    }

    // Mock data for dialog preview testing
    val mockExportUris = remember {
        listOf(
            "otpauth-migration://offline?data=mock1",
            "otpauth-migration://offline?data=mock2"
        )
    }

    if (showImportDialog && preValidationData != null) {
        BitwardenImportPreviewDialog(
            preValidationResult = preValidationData!!,
            hasRemoteGateway = hasRemoteGateway,
            onConfirm = { policy, dest ->
                // Handle import logic
                showImportDialog = false
            },
            onDismiss = { showImportDialog = false }
        )
    }

    if (requireStepUpAuth) {
        // Implement biometric/PIN step-up logic here (simplified for this task)
        // Upon successful auth: requireStepUpAuth = false; showExportDialog = true
        AlertDialog(
            onDismissRequest = { requireStepUpAuth = false },
            title = { Text("Authentication Required") },
            text = { Text("Please verify your identity to export 2FA accounts.") },
            confirmButton = {
                TextButton(onClick = {
                    requireStepUpAuth = false
                    showExportDialog = true
                }) { Text("Verify (Mocked)") }
            },
            dismissButton = {
                TextButton(onClick = { requireStepUpAuth = false }) { Text("Cancel") }
            }
        )
    }

    if (showExportDialog) {
        GoogleAuthExportViewerDialog(
            exportUris = mockExportUris,
            onDismiss = { showExportDialog = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Backups") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Import", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

            SettingsActionCard(
                title = "Import from File",
                description = "Import Bitwarden, Aegis, or 2FAS exports.",
                onClick = { filePickerLauncher.launch(arrayOf("*/*")) }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text("Export", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

            SettingsActionCard(
                title = "Export to Google Authenticator",
                description = "Generate QR codes to migrate your vault.",
                onClick = { requireStepUpAuth = true }
            )

            SettingsActionCard(
                title = "Export Encrypted Backup",
                description = "Generate a password-protected .sgtotp.bak file.",
                onClick = { /* Handle sgtotp.bak export */ }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsActionCard(title: String, description: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
