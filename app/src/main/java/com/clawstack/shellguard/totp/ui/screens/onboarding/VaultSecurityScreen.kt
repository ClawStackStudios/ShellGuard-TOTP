package com.clawstack.shellguard.totp.ui.screens.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clawstack.shellguard.totp.data.repository.VaultProtectionMode

@Composable
fun VaultSecurityScreen(
    onVaultHatched: (masterSecret: String, isPin: Boolean, useBiometrics: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var step by remember { mutableIntStateOf(1) } // 1: Education & Mode Selector, 2: Secret Setup & Biometrics
    var protectionMode by remember { mutableStateOf(VaultProtectionMode.PIN) }
    var pinOrPassword by remember { mutableStateOf("") }
    var confirmSecret by remember { mutableStateOf("") }
    var enableBiometrics by remember { mutableStateOf(true) }
    var isSecretVisible by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when (step) {
                1 -> {
                    // Education & Mode Selector
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Security",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(56.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Secure Your Local Vault",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                        fontSize = 26.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Educational Cards
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.VpnKey, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text("Zero-Knowledge Encryption", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Your vault is encrypted with AES-256-GCM. We never see your keys.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text("Hardware Isolation", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Protected by Android KeyStore and StrongBox for maximum hardware security.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.WifiOff, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text("Offline Autonomy", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Fully functional offline. Your secrets never leave your device unencrypted.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Text("Choose Protection Mode", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(16.dp))

                    // Protection Mode Selector (PIN vs Password)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp))
                                .background(if (protectionMode == VaultProtectionMode.PIN) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable { protectionMode = VaultProtectionMode.PIN }
                                .padding(vertical = 14.dp)
                                .testTag("select_pin_mode"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "🔢 PIN Code",
                                color = if (protectionMode == VaultProtectionMode.PIN) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(topEnd = 14.dp, bottomEnd = 14.dp))
                                .background(if (protectionMode == VaultProtectionMode.PASSWORD) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable { protectionMode = VaultProtectionMode.PASSWORD }
                                .padding(vertical = 14.dp)
                                .testTag("select_password_mode"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "🔑 Master Password",
                                color = if (protectionMode == VaultProtectionMode.PASSWORD) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = { step = 2 },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("continue_to_setup_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text(
                            text = "Continue",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                2 -> {
                    // Secret Setup & Biometrics
                    Text(
                        text = if (protectionMode == VaultProtectionMode.PIN) "Set Your Vault PIN" else "Set Master Password",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (protectionMode == VaultProtectionMode.PIN)
                            "Enter a 4 to 8 digit numeric PIN code to unlock your vault."
                        else
                            "Enter a strong master passphrase to secure your vault secrets.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    OutlinedTextField(
                        value = pinOrPassword,
                        onValueChange = { input ->
                            if (protectionMode == VaultProtectionMode.PIN) {
                                if (input.length <= 8 && input.all { it.isDigit() }) pinOrPassword = input
                            } else {
                                pinOrPassword = input
                            }
                        },
                        label = { Text(if (protectionMode == VaultProtectionMode.PIN) "Enter PIN (4–8 digits)" else "Master Password") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = if (protectionMode == VaultProtectionMode.PIN) KeyboardType.NumberPassword else KeyboardType.Password
                        ),
                        visualTransformation = if (isSecretVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { isSecretVisible = !isSecretVisible }) {
                                Icon(
                                    imageVector = if (isSecretVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle Secret Visibility",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("vault_secret_input"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                    
                    // Simple Strength Meter for Password Mode
                    if (protectionMode == VaultProtectionMode.PASSWORD) {
                        Spacer(modifier = Modifier.height(8.dp))
                        val strength = when {
                            pinOrPassword.length < 6 -> 0.2f
                            pinOrPassword.length < 10 -> 0.6f
                            else -> 1.0f
                        }
                        val color = when {
                            strength < 0.5f -> MaterialTheme.colorScheme.error
                            strength < 0.8f -> Color(0xFFFFA000)
                            else -> Color(0xFF4CAF50)
                        }
                        LinearProgressIndicator(
                            progress = { strength },
                            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                            color = color,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    val isMismatch = confirmSecret.isNotEmpty() && confirmSecret != pinOrPassword
                    OutlinedTextField(
                        value = confirmSecret,
                        onValueChange = { input ->
                            if (protectionMode == VaultProtectionMode.PIN) {
                                if (input.length <= 8 && input.all { it.isDigit() }) confirmSecret = input
                            } else {
                                confirmSecret = input
                            }
                        },
                        label = { Text("Confirm Secret") },
                        singleLine = true,
                        isError = isMismatch,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = if (protectionMode == VaultProtectionMode.PIN) KeyboardType.NumberPassword else KeyboardType.Password
                        ),
                        visualTransformation = if (isSecretVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("vault_confirm_secret_input"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (isMismatch) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    if (isMismatch) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Secrets do not match",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            modifier = Modifier.align(Alignment.Start)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Biometric Switch Card
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Fingerprint,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Enable Biometric Unlock",
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "Unlock with fingerprint or face on cold boot",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                            Switch(
                                checked = enableBiometrics,
                                onCheckedChange = { enableBiometrics = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.background,
                                    checkedTrackColor = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.testTag("biometric_toggle_switch")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    val isSecretValid = if (protectionMode == VaultProtectionMode.PIN) {
                        pinOrPassword.length in 4..8 && pinOrPassword == confirmSecret
                    } else {
                        pinOrPassword.isNotBlank() && pinOrPassword == confirmSecret
                    }

                    Button(
                        onClick = {
                            onVaultHatched(
                                pinOrPassword,
                                protectionMode == VaultProtectionMode.PIN,
                                enableBiometrics
                            )
                        },
                        enabled = isSecretValid,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("hatch_vault_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text(
                            text = "Hatch My Vault",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }
}
