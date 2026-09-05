package com.clawstack.shellguard.totp.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clawstack.shellguard.totp.ui.theme.LocalShellGuardColors

/**
 * Master TOTP Flat Card Component conforming to Reef Modernist Mobile.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TotpCard(
    title: String,
    username: String?,
    category: String?,
    code: String,
    remainingSeconds: Int,
    progress: Float,
    isLocalOnly: Boolean = false,
    digitGrouping: Boolean = true,
    hapticsEnabled: Boolean = true,
    onCopy: (String) -> Unit,
    onEdit: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val shellColors = LocalShellGuardColors.current
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Smooth spring press bounce
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1.0f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f),
        label = "CardSpringScale"
    )

    // Formatted split display: "123456" -> "123 456", "12345678" -> "1234 5678"
    val formattedCode = when {
        !digitGrouping -> code
        code.length == 6 -> "${code.substring(0, 3)} ${code.substring(3)}"
        code.length == 8 -> "${code.substring(0, 4)} ${code.substring(4)}"
        else -> code
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .testTag("totp_card_${title.lowercase().replace(" ", "_")}")
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    if (hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onCopy(code)
                },
                onLongClick = {
                    if (hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onEdit()
                }
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = shellColors.bgSurface),
        border = BorderStroke(1.dp, shellColors.borderSubtle),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp) // Flat Material Aesthetic
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // ── Left: Issuer Badge & Account Metadata ───────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Issuer Avatar / Initial Badge
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(shellColors.bgElevated)
                        .border(1.dp, shellColors.borderSubtle, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title.firstOrNull()?.uppercase() ?: "🐚",
                        color = shellColors.primaryAccent,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = title,
                            color = shellColors.textMain,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        // Provenance Pill: 📱 Local vs ☁️ Synced (Read-Only)
                        Icon(
                            imageVector = if (isLocalOnly) Icons.Default.PhoneAndroid else Icons.Default.CloudDone,
                            contentDescription = if (isLocalOnly) "Local Only" else "Synced from Server (Read-Only)",
                            tint = if (isLocalOnly) shellColors.textMuted else shellColors.primaryAccent,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                    if (!username.isNullOrBlank()) {
                        Text(
                            text = username,
                            color = shellColors.textMuted,
                            fontSize = 13.sp,
                            maxLines = 1
                        )
                    }
                    if (!category.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(shellColors.bgElevated)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = category,
                                color = shellColors.primaryAccent,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // ── Right: Monospace Code & Animated Progress Ring ──────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = formattedCode,
                    color = when {
                        remainingSeconds <= 5 -> shellColors.danger
                        remainingSeconds <= 10 -> shellColors.warning
                        else -> shellColors.primaryAccent
                    },
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    letterSpacing = 1.5.sp
                )

                Spacer(modifier = Modifier.width(12.dp))

                TotpCountdownRing(
                    remainingSeconds = remainingSeconds,
                    progress = progress
                )
            }
        }
    }
}
