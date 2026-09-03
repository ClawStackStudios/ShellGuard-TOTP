package com.clawstack.shellguard.totp.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clawstack.shellguard.totp.R
import com.clawstack.shellguard.totp.ShellGuardTotpApp
import com.clawstack.shellguard.totp.data.local.entities.TotpItemEntity
import com.clawstack.shellguard.totp.engine.HashAlgorithm
import com.clawstack.shellguard.totp.engine.TotpEngine
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.clawstack.shellguard.totp.ui.components.ClipboardToastPill
import com.clawstack.shellguard.totp.ui.components.ExpandableSpeedDialFab
import com.clawstack.shellguard.totp.ui.components.PodFilterChips
import com.clawstack.shellguard.totp.ui.components.SpotlightOverlay
import com.clawstack.shellguard.totp.ui.components.SpeedDialState
import com.clawstack.shellguard.totp.ui.components.SwipeableTotpCard
import com.clawstack.shellguard.totp.ui.components.TotpEmptyState
import com.clawstack.shellguard.totp.ui.components.rememberSpeedDialState
import com.clawstack.shellguard.totp.ui.viewmodels.TotpViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TotpListScreen(
    viewModel: TotpViewModel,
    onAddSecretClick: () -> Unit,
    onScanQrClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val localItems by viewModel.localItems.collectAsStateWithLifecycle()
    val remoteItems by viewModel.remoteItems.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val tickerState by viewModel.tickerState.collectAsStateWithLifecycle()
    val syncMeta by viewModel.syncMetadata.collectAsStateWithLifecycle()
    val isServerConnected by viewModel.isServerConnected.collectAsStateWithLifecycle()
    val clipboardFeedback by viewModel.clipboardFeedback.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val app = context.applicationContext as ShellGuardTotpApp
    val authRepo = app.authRepository
    val tourStep by authRepo.tourStep.collectAsStateWithLifecycle()
    val isBackupPromptDismissed by authRepo.isBackupPromptDismissed.collectAsStateWithLifecycle()

    var settingsIconCenter by remember { mutableStateOf<Offset?>(null) }
    var editingItem by remember { mutableStateOf<TotpItemEntity?>(null) }
    var itemPendingDeletion by remember { mutableStateOf<TotpItemEntity?>(null) }
    val isOnlineSynced = syncMeta?.lastSyncStatus == "SUCCESS"

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            val speedDialState = rememberSpeedDialState()
            ExpandableSpeedDialFab(
                speedDialState = speedDialState,
                onScanQrClick = onScanQrClick,
                onAddSecretClick = onAddSecretClick,
                onImageQrDecoded = { rawUri ->
                    if (viewModel.importScannedUri(rawUri)) {
                        Toast.makeText(context, "2FA code added to Local Vault.", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Invalid 2FA QR code.", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.testTag("speed_dial_host")
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // ── Header Bar ──────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Logo + App Name + Tagline
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = "ShellGuard Shield",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = stringResource(R.string.app_name),
                                color = MaterialTheme.colorScheme.onBackground,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            // Connectivity Status Pill
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (!isServerConnected) MaterialTheme.colorScheme.primary
                                            else if (isOnlineSynced) Color(0xFF10B981)
                                            else Color(0xFFF59E0B)
                                        )
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = if (!isServerConnected) "🔒 Local Mode"
                                    else if (isOnlineSynced) "🟢 Synced"
                                    else "🟡 Offline Cache",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Settings Action Button
                    IconButton(
                        onClick = onSettingsClick,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .onGloballyPositioned { coordinates ->
                                val pos = coordinates.positionInRoot()
                                val size = coordinates.size
                                settingsIconCenter = Offset(pos.x + size.width / 2f, pos.y + size.height / 2f)
                            }
                            .testTag("settings_icon_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // ── Search Bar ──────────────────────────────────────────
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.onSearchQueryChanged(it) },
                    placeholder = {
                        Text(
                            text = "Search 2FA tokens or accounts...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear search",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp)
                        .testTag("totp_search_bar")
                )

                

                // ── Main Content: List or Empty State ────────────────────
                if (localItems.isEmpty() && remoteItems.isEmpty() && searchQuery.isBlank() && selectedCategory == null) {
                    TotpEmptyState(
                        onScanQrClick = onScanQrClick,
                        onManualAddClick = onAddSecretClick
                    )
                } else if (localItems.isEmpty() && remoteItems.isEmpty()) {
                    // No search results
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No matching 2FA tokens found.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp),
                        contentPadding = PaddingValues(top = 4.dp, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // ── Proactive Encrypted Backup Prompt Card ───────────────
                        if (!isBackupPromptDismissed && searchQuery.isBlank() && selectedCategory == null) {
                            item(key = "backup_prompt_card") {
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                                    ),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .animateItem()
                                        .testTag("backup_prompt_card")
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(32.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Security,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = "Protect Against Lockout",
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                        }
                                        IconButton(
                                            onClick = { authRepo.setBackupPromptDismissed(true) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Dismiss backup prompt",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(
                                        text = "Create an encrypted export of your vault to ensure you never lose access to your accounts if you switch or lose devices.",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                authRepo.setBackupPromptDismissed(true)
                                                onSettingsClick()
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                            modifier = Modifier.testTag("backup_prompt_action_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CloudUpload,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Create Encrypted Backup",
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                        
                        if (localItems.isNotEmpty()) {
                            item {
                                Text("📱 Local Vault", modifier = Modifier.padding(top = 16.dp, bottom = 8.dp), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                            itemsIndexed(localItems, key = { _, item -> item.id }) { index, item ->
                            val code = TotpEngine.generateTotp(
                                secretBase32 = item.secret,
                                timestampMillis = tickerState.timestampMillis,
                                timeStepSeconds = item.period.toLong(),
                                digits = item.digits,
                                algorithm = HashAlgorithm.fromString(item.algorithm)
                            )

                            StaggeredAnimatedItem(
                                index = index,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .animateItem()
                            ) {
                                SwipeableTotpCard(
                                    title = item.title,
                                    username = item.username,
                                    category = item.category,
                                    code = code,
                                    remainingSeconds = tickerState.remainingSeconds,
                                    progress = tickerState.progress,
                                    isLocalOnly = item.isLocalOnly,
                                    onCopy = { rawCode ->
                                        viewModel.copyToClipboard(item.title, rawCode)
                                    },
                                    onEdit = {
                                        editingItem = item
                                    },
                                    onDelete = {
                                        itemPendingDeletion = item
                                    }
                                )
                            }
                            }
                        }
                        if (remoteItems.isNotEmpty()) {
                            item {
                                Text("☁️ Synced from ShellGuard", modifier = Modifier.padding(top = 16.dp, bottom = 8.dp), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                            itemsIndexed(remoteItems, key = { _, item -> item.id }) { index, item ->
                            val code = TotpEngine.generateTotp(
                                secretBase32 = item.secret,
                                timestampMillis = tickerState.timestampMillis,
                                timeStepSeconds = item.period.toLong(),
                                digits = item.digits,
                                algorithm = HashAlgorithm.fromString(item.algorithm)
                            )

                            StaggeredAnimatedItem(
                                index = index,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .animateItem()
                            ) {
                                SwipeableTotpCard(
                                    title = item.title,
                                    username = item.username,
                                    category = item.category,
                                    code = code,
                                    remainingSeconds = tickerState.remainingSeconds,
                                    progress = tickerState.progress,
                                    isLocalOnly = item.isLocalOnly,
                                    onCopy = { rawCode ->
                                        viewModel.copyToClipboard(item.title, rawCode)
                                    },
                                    onEdit = {
                                        editingItem = item
                                    },
                                    onDelete = {
                                        itemPendingDeletion = item
                                    }
                                )
                            }
                            }
                        }
                    }
                }
            }

            // ── Floating Clipboard Toast Notification ───────────────────
            ClipboardToastPill(
                visible = clipboardFeedback.isVisible,
                message = clipboardFeedback.message,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }

    // ── Edit Account Modal Dialog ───────────────────────────────
    editingItem?.let { item ->
        var editTitle by remember(item) { mutableStateOf(item.title) }
        var editUsername by remember(item) { mutableStateOf(item.username ?: "") }
        var editCategory by remember(item) { mutableStateOf(item.category ?: "") }

        AlertDialog(
            onDismissRequest = { editingItem = null },
            title = {
                Text(
                    text = "Edit 2FA Account",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text("Account / Service Name") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editUsername,
                        onValueChange = { editUsername = it },
                        label = { Text("Username or Email (Optional)") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editCategory,
                        onValueChange = { editCategory = it },
                        label = { Text("Pod / Category (Optional)") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateItemDetails(
                            id = item.id,
                            title = editTitle,
                            username = editUsername,
                            category = editCategory
                        )
                        editingItem = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Save Changes", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            val toDelete = item
                            editingItem = null
                            itemPendingDeletion = toDelete
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }

                    OutlinedButton(
                        onClick = { editingItem = null },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // ── Delete Confirmation Modal Dialog ────────────────────────
    itemPendingDeletion?.let { item ->
        AlertDialog(
            onDismissRequest = { itemPendingDeletion = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = "Remove 2FA Account?",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to remove \"${item.title}\"? You will no longer be able to generate 2FA verification codes for this service.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteItem(item.id)
                        itemPendingDeletion = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("confirm_delete_button")
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.onError, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { itemPendingDeletion = null },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("cancel_delete_button")
                ) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurface)
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.testTag("delete_confirmation_dialog")
        )
    }

    // ── Interactive Spotlight Guided Tour (Step 1: Settings) ─────
    SpotlightOverlay(
        visible = tourStep == 1,
        step = 1,
        targetCenter = settingsIconCenter,
        targetRadius = 70f,
        onNext = {
            authRepo.setTourStep(2)
            onSettingsClick()
        },
        onDismiss = {
            authRepo.setGuidedTourCompleted(true)
        }
    )
}

/**
 * Applies a subtle staggered slide-up and fade-in entrance animation to list items.
 */
@Composable
private fun StaggeredAnimatedItem(
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val alphaAnim = remember { Animatable(0f) }
    val translationYAnim = remember { Animatable(20f) }

    LaunchedEffect(Unit) {
        val delayMs = (index * 35L).coerceAtMost(280L)
        delay(delayMs)
        launch {
            alphaAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
            )
        }
        launch {
            translationYAnim.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
    }

    Box(
        modifier = modifier.graphicsLayer {
            alpha = alphaAnim.value
            translationY = translationYAnim.value * density
        }
    ) {
        content()
    }
}


