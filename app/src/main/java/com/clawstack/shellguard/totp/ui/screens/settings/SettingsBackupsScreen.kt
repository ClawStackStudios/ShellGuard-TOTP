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
import com.clawstack.shellguard.totp.ui.dialogs.GoogleAuthExportViewerDialog
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBackupsScreen(
    navController: NavController,
    hasRemoteGateway: Boolean = false // Passed in from ViewModel realistically
) {
    var showImportDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }

    // Mock data for dialog preview testing
    val mockPreValidation = remember {
        PreValidationResult(
            isValid = true,
            schemaType = BackupSchemaType.BITWARDEN_VAULT,
            tokenCount = 5,
            parsedTokens = listOf(
                ParsedTotpItem(id = UUID.randomUUID().toString(), title = "Github", secret = "JBSWY3DPEHPK3PXP", category = "Dev"),
                ParsedTotpItem(id = UUID.randomUUID().toString(), title = "Twitter", secret = "JBSWY3DPEHPK3PXP", category = "Social")
            )
        )
    }

    val mockExportUris = remember {
        listOf(
            "otpauth-migration://offline?data=mock1",
            "otpauth-migration://offline?data=mock2"
        )
    }

    if (showImportDialog) {
        BitwardenImportPreviewDialog(
            preValidationResult = mockPreValidation,
            hasRemoteGateway = hasRemoteGateway,
            onConfirm = { policy, dest ->
                // Handle import logic
                showImportDialog = false
            },
            onDismiss = { showImportDialog = false }
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
                onClick = { showImportDialog = true }
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            Text("Export", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

            SettingsActionCard(
                title = "Export to Google Authenticator",
                description = "Generate QR codes to migrate your vault.",
                onClick = { showExportDialog = true }
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
