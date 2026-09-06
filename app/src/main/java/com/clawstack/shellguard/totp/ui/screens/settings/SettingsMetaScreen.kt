package com.clawstack.shellguard.totp.ui.screens.settings

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.ImportExport
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Phase 11 / Task 22 — Categorized Settings Hub (master category list). */
data class SettingsCategory(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val destination: SettingsDestination
)

enum class SettingsDestination { APPEARANCE, BEHAVIOR, SERVER_SYNC, PLACEHOLDER }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsMetaScreen(
    onBackClick: () -> Unit,
    onNavigateToAppearance: () -> Unit,
    onNavigateToBehavior: () -> Unit,
    onNavigateToServerSync: () -> Unit,
    onNavigateToPlaceholder: (String) -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontSize = 20.sp, fontWeight = FontWeight.SemiBold) },
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            categories.forEach { category ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            when (category.destination) {
                                SettingsDestination.APPEARANCE -> onNavigateToAppearance()
                                SettingsDestination.BEHAVIOR -> onNavigateToBehavior()
                                SettingsDestination.SERVER_SYNC -> onNavigateToServerSync()
                                SettingsDestination.PLACEHOLDER -> onNavigateToPlaceholder(category.title)
                            }
                        },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = category.icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = category.title, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                            Text(text = category.subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

    val categories = listOf(
        SettingsCategory("🎨 Appearance", "Adjust theme, language, and other appearance settings", Icons.Default.Palette, SettingsDestination.APPEARANCE),
        SettingsCategory("⚡ Behavior", "Customize behavior when interacting with entry list", Icons.Default.Speed, SettingsDestination.BEHAVIOR),
        SettingsCategory("☁️ Server & Sync", "Gateway connection, sync status, and vault link", Icons.Default.CloudSync, SettingsDestination.SERVER_SYNC),
        SettingsCategory("📦 Icon packs", "Manage and import icon packs", Icons.Default.Image, SettingsDestination.PLACEHOLDER),
        SettingsCategory("🔐 Security", "Configure encryption, biometric unlock, auto lock", Icons.Default.Security, SettingsDestination.PLACEHOLDER),
        SettingsCategory("☁️ Backups", "Automatic backups & Android cloud backup system", Icons.Default.Backup, SettingsDestination.PLACEHOLDER),
        SettingsCategory("🛠️ Import & Export", "Import from Aegis/Bitwarden/Google, export vault", Icons.Default.ImportExport, SettingsDestination.PLACEHOLDER),
        SettingsCategory("📈 Audit log", "Security event audit trail", Icons.Default.History, SettingsDestination.PLACEHOLDER)
    )
