package com.clawstack.shellguard.totp.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.clawstack.shellguard.totp.data.migration.BackupSchemaType
import com.clawstack.shellguard.totp.data.migration.ConflictPolicy
import com.clawstack.shellguard.totp.data.migration.ImportDestination
import com.clawstack.shellguard.totp.data.migration.PreValidationResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BitwardenImportPreviewDialog(
    preValidationResult: PreValidationResult,
    hasRemoteGateway: Boolean,
    onConfirm: (ConflictPolicy, ImportDestination) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedPolicy by remember { mutableStateOf(ConflictPolicy.SKIP_DUPLICATES) }
    var selectedDestination by remember { mutableStateOf(ImportDestination.LOCAL_VAULT) }

    val schemaBadgeText = when (preValidationResult.schemaType) {
        BackupSchemaType.BITWARDEN_VAULT -> "BITWARDEN VAULT"
        BackupSchemaType.BITWARDEN_AUTHENTICATOR -> "BITWARDEN AUTHENTICATOR"
        BackupSchemaType.AEGIS -> "AEGIS"
        BackupSchemaType.TWO_FAS -> "2FAS"
        else -> "UNKNOWN SCHEMA"
    }

    val categoryBreakdown = preValidationResult.parsedTokens
        .groupBy { it.category ?: "General" }
        .mapValues { it.value.size }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        title = { Text(text = "Import Preview", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Schema Badge
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = schemaBadgeText,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                // Tokens summary
                Text(
                    text = "${preValidationResult.tokenCount} 2FA accounts found. Passwords and personal notes are strictly excluded.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (categoryBreakdown.isNotEmpty()) {
                    Text("Pod Breakdown:", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                    categoryBreakdown.forEach { (cat, count) ->
                        Text(text = "• $cat: $count items", style = MaterialTheme.typography.bodySmall)
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                // Conflict Resolution
                Text("Conflict Resolution Policy", fontWeight = FontWeight.SemiBold)
                ConflictPolicy.values().forEach { policy ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RadioButton(
                            selected = selectedPolicy == policy,
                            onClick = { selectedPolicy = policy }
                        )
                        Text(
                            text = policy.name.replace("_", " "),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                // Destination Selection
                Text("Destination", fontWeight = FontWeight.SemiBold)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RadioButton(
                        selected = selectedDestination == ImportDestination.LOCAL_VAULT,
                        onClick = { selectedDestination = ImportDestination.LOCAL_VAULT }
                    )
                    Text(
                        text = "📱 Save to Local Vault Only",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RadioButton(
                        selected = selectedDestination == ImportDestination.REMOTE_GATEWAY,
                        onClick = { if (hasRemoteGateway) selectedDestination = ImportDestination.REMOTE_GATEWAY },
                        enabled = hasRemoteGateway
                    )
                    Text(
                        text = "☁️ Save & Sync with Remote Gateway",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (hasRemoteGateway) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                if (!hasRemoteGateway) {
                    Text(
                        text = "(Remote gateway is not configured)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(start = 48.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(selectedPolicy, selectedDestination) }) {
                Text("Import")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
