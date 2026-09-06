package com.clawstack.shellguard.totp.ui.components

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clawstack.shellguard.totp.scanner.ImageQrDecoder
import kotlinx.coroutines.launch

/**
 * Task 20 — Animated Speed Dial FAB & Elevated Action Pills.
 *
 * Replaces the previous dual-FAB arrangement on the vault dashboard:
 *  - Main FAB: smooth 45-degree rotation morphing from `+` to `✕`.
 *  - Background scrim: subtle dark alpha dimming, dismissible by tapping outside.
 *  - Elevated action pills (staggered slide-and-fade entrance, bottom → top):
 *      1. [ 📷 Scan QR code ]  → live CameraX preview scanner
 *      2. [ 🖼️ Scan image ]    → SAF gallery picker decoded via [ImageQrDecoder]
 *      3. [ ✏️ Enter manually ] → manual secret entry form
 *
 * All colors bind strictly to MaterialTheme tokens — no hardcoded brand hexes.
 */
@Composable
fun ExpandableSpeedDialFab(
    speedDialState: SpeedDialState,
    onScanQrClick: () -> Unit,
    onAddSecretClick: () -> Unit,
    onImageQrDecoded: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // ── SAF Image Gallery Picker (Pill 2) ───────────────────────────
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            ImageQrDecoder.decode(context, uri) { result ->
                when (result) {
                    is ImageQrDecoder.DecodeResult.Success -> onImageQrDecoded(result.rawValue)
                    is ImageQrDecoder.DecodeResult.NoBarcodeFound ->
                        Toast.makeText(context, "No 2FA QR code found in selected image.", Toast.LENGTH_SHORT).show()
                    is ImageQrDecoder.DecodeResult.Failure ->
                        Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // ── Back-handler interception while expanded ────────────────────
    BackHandler(enabled = speedDialState.isExpanded) {
        speedDialState.toggle(scope)
    }

    Box(modifier = modifier) {
        // ── Elevated Action Pills (bottom → top staggered) ──────────────
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 84.dp)
        ) {
            SpeedDialPill(
                label = "📷 Scan QR code",
                icon = Icons.Default.QrCodeScanner,
                visible = speedDialState.fraction,
                index = 0,
                onClick = {
                    speedDialState.toggle(scope)
                    onScanQrClick()
                }
            )
            SpeedDialPill(
                label = "🖼️ Scan image",
                icon = Icons.Default.Image,
                visible = speedDialState.fraction,
                index = 1,
                onClick = {
                    speedDialState.toggle(scope)
                    imagePickerLauncher.launch("image/*")
                }
            )
            SpeedDialPill(
                label = "✏️ Enter manually",
                icon = Icons.Default.Edit,
                visible = speedDialState.fraction,
                index = 2,
                onClick = {
                    speedDialState.toggle(scope)
                    onAddSecretClick()
                }
            )
        }

        // ── Main FAB: 45-degree rotation morphing `+` → `✕` ────────────
        FloatingActionButton(
            onClick = { speedDialState.toggle(scope) },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = CircleShape,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 6.dp,
                pressedElevation = 10.dp
            ),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .testTag("speed_dial_fab")
        ) {
            val iconRotation by animateFloatAsState(
                targetValue = if (speedDialState.isExpanded) 45f else 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "speedDialIconRotation"
            )
            Icon(
                imageVector = if (speedDialState.isExpanded) Icons.Default.Close else Icons.Default.Add,
                contentDescription = if (speedDialState.isExpanded) "Close speed dial" else "Open speed dial",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .size(26.dp)
                    .rotate(iconRotation)
            )
        }
    }
}

/**
 * Full-screen dimming scrim for the Speed Dial.
 *
 * IMPORTANT: render this as a sibling ABOVE the dashboard content but BELOW the
 * [ExpandableSpeedDialFab] host (e.g. last child of the Scaffold content root Box).
 * It must NOT live inside the Scaffold's `floatingActionButton` slot — that slot is
 * wrap-content anchored bottom-end, so a fillMaxSize scrim there only covers a small
 * off-center patch, leaving white gaps at the screen edges (light-mode regression).
 */
@Composable
fun SpeedDialScrim(
    speedDialState: SpeedDialState,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    if (speedDialState.fraction > 0f) {
        Box(
            modifier = modifier
                .graphicsLayer { alpha = speedDialState.fraction }
                .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.45f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { speedDialState.dismissOnOutsideTouch(scope) }
                .testTag("speed_dial_scrim")
        )
    }
}

/**
 * A single elevated action pill with a staggered slide-and-fade entrance
 * driven by the dial's shared progress fraction (bottom → top by index).
 */
@Composable
private fun SpeedDialPill(
    label: String,
    icon: ImageVector,
    visible: Float,
    index: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Stagger: lower-index pills appear first as the dial expands.
    val staggeredAlpha = ((visible - index * 0.18f) / (1f - index * 0.18f)).coerceIn(0f, 1f)
    if (staggeredAlpha <= 0f) return

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shadowElevation = 8.dp,
        modifier = modifier
            .graphicsLayer {
                alpha = staggeredAlpha
                translationY = (1f - staggeredAlpha) * 24f
            }
            .clickable(onClick = onClick)
            .testTag("speed_dial_pill_$index")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(10.dp))
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}


