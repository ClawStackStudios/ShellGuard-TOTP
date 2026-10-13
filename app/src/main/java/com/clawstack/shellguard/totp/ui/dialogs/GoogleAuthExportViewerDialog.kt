package com.clawstack.shellguard.totp.ui.dialogs

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun GoogleAuthExportViewerDialog(
    exportUris: List<String>,
    onDismiss: () -> Unit
) {
    if (exportUris.isEmpty()) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Export Empty") },
            text = { Text("No accounts available for export.") },
            confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
        )
        return
    }

    var currentIndex by remember { mutableStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Google Auth Export", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Page Indicator
                Text(
                    text = "Page ${currentIndex + 1} of ${exportUris.size}",
                    style = MaterialTheme.typography.labelMedium
                )

                // Account Count Badge (Max 10 per page as per encoder)
                Badge(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                    Text(
                        text = "Chunk ${currentIndex + 1}",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                // Temporary QR Code Display Placeholder since ZXing isn't available
                Box(
                    modifier = Modifier
                        .size(250.dp)
                        .background(androidx.compose.ui.graphics.Color.LightGray),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "QR Code Placeholder\n${exportUris[currentIndex].take(20)}...",
                        color = androidx.compose.ui.graphics.Color.Black,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { if (currentIndex > 0) currentIndex-- },
                        enabled = currentIndex > 0
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous")
                    }

                    IconButton(
                        onClick = { if (currentIndex < exportUris.size - 1) currentIndex++ },
                        enabled = currentIndex < exportUris.size - 1
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}
