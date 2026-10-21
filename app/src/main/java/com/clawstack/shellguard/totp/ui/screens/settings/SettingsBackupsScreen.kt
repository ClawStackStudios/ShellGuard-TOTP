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
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.widget.Toast

// TODO(jules-sec): Verify MASVS-AUTH-1 step-up challenge covers any future unencrypted JSON backup export additions.
// TODO(jules-phase-14): Wire automatic backup WorkManager schedule interval selector into SettingsBackupsScreen.

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBackupsScreen(
    navController: NavController,
    hasRemoteGateway: Boolean = false,
    migrationPersister: com.clawstack.shellguard.totp.data.migration.MigrationPersister? = null,
    totpItemDao: com.clawstack.shellguard.totp.data.local.dao.TotpItemDao? = null,
    authViewModel: com.clawstack.shellguard.totp.ui.viewmodels.AuthViewModel? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
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
                Toast.makeText(context, "Failed to parse import file: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    var realExportUris by remember { mutableStateOf<List<String>>(emptyList()) }

    fun loadAndLaunchExport() {
        coroutineScope.launch(Dispatchers.IO) {
            val items = totpItemDao?.getAllTotpItems() ?: emptyList()
            if (items.isNotEmpty()) {
                val uris = com.clawstack.shellguard.totp.data.migration.GoogleAuthMigrationEncoder.encodeToMigrationUris(items)
                withContext(Dispatchers.Main) {
                    realExportUris = uris
                    showExportDialog = true
                }
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "No accounts available in vault to export", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    if (showImportDialog && preValidationData != null) {
        BitwardenImportPreviewDialog(
            preValidationResult = preValidationData!!,
            hasRemoteGateway = hasRemoteGateway,
            onConfirm = { policy, dest ->
                coroutineScope.launch(Dispatchers.IO) {
                    val session = authViewModel?.currentSession?.value
                    migrationPersister?.persistImportedTokens(
                        tokens = preValidationData!!.parsedTokens,
                        policy = policy,
                        destination = dest,
                        huKey = session?.rawHuKey,
                        ownerUuid = session?.userUuid ?: "local"
                    )
                    withContext(Dispatchers.Main) {
                        showImportDialog = false
                    }
                }
            },
            onDismiss = { showImportDialog = false }
        )
    }

    var showPinDialog by remember { mutableStateOf(false) }

    if (requireStepUpAuth) {
        val activity = context as? androidx.fragment.app.FragmentActivity
        LaunchedEffect(Unit) {
            if (activity != null) {
                if (authViewModel?.isBiometricEnabled?.value == true) {
                    authViewModel.promptBiometrics(activity) { success ->
                        requireStepUpAuth = false
                        if (success) {
                            loadAndLaunchExport()
                        }
                    }
                } else {
                    requireStepUpAuth = false
                    showPinDialog = true
                }
            } else {
                requireStepUpAuth = false
                showPinDialog = true
            }
        }
    }

    if (showPinDialog) {
        var inputPin by remember { mutableStateOf("") }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text("Authentication Required") },
            text = {
                Column {
                    Text("Please enter your Master PIN/Password to export 2FA accounts.")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = inputPin,
                        onValueChange = {
                            inputPin = it
                            errorMessage = null
                        },
                        singleLine = true,
                        isError = errorMessage != null
                    )
                    if (errorMessage != null) {
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (authViewModel?.unlockWithSecret(inputPin) == true) {
                        showPinDialog = false
                        loadAndLaunchExport()
                    } else {
                        errorMessage = "Incorrect PIN/Password."
                    }
                }) { Text("Verify") }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showExportDialog) {
        GoogleAuthExportViewerDialog(
            exportUris = realExportUris,
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
                onClick = { navController.navigate(com.clawstack.shellguard.totp.ui.navigation.Screen.SettingsImportExport.route) }
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
