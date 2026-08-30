package com.clawstack.shellguard.totp.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clawstack.shellguard.totp.R
import com.clawstack.shellguard.totp.engine.TotpEngine
import com.clawstack.shellguard.totp.engine.TotpTicker
import com.clawstack.shellguard.totp.ui.components.TotpCard
import com.clawstack.shellguard.totp.ui.theme.ClawCyan
import com.clawstack.shellguard.totp.ui.theme.ShellBorder
import com.clawstack.shellguard.totp.ui.theme.ShellSurface
import com.clawstack.shellguard.totp.ui.theme.TextMuted
import com.clawstack.shellguard.totp.ui.theme.TextPearl

data class SampleTotpAccount(
    val title: String,
    val username: String,
    val secretBase32: String,
    val category: String
)

@Composable
fun ShellGuardAppPlaceholder(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val tickerState by remember { TotpTicker.observeTicker() }.collectAsState(
        initial = com.clawstack.shellguard.totp.engine.TotpTickerState(
            System.currentTimeMillis(),
            TotpEngine.getRemainingSeconds(),
            TotpEngine.getProgressRatio()
        )
    )

    val sampleAccounts = remember {
        listOf(
            SampleTotpAccount(
                title = "GitHub",
                username = "octocat@github.com",
                secretBase32 = "JBSWY3DPEHPK3PXP",
                category = "Developer"
            ),
            SampleTotpAccount(
                title = "ClawChives Vault",
                username = "admin@clawstack.internal",
                secretBase32 = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ",
                category = "Infrastructure"
            ),
            SampleTotpAccount(
                title = "Google Workspace",
                username = "security@example.com",
                secretBase32 = "KVKFKRCPNZQUYMLX",
                category = "Personal"
            )
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Shield,
                        contentDescription = "ShellGuard Security Shield",
                        modifier = Modifier.size(28.dp),
                        tint = ClawCyan
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPearl
                    )
                    Text(
                        text = stringResource(R.string.tagline),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                }
            }
        }

        item {
            Text(
                text = "AUTHENTICATOR CODES",
                style = MaterialTheme.typography.labelSmall,
                color = ClawCyan,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        items(sampleAccounts.size) { index ->
            val account = sampleAccounts[index]
            val code = TotpEngine.generateTotp(
                secretBase32 = account.secretBase32,
                timestampMillis = tickerState.timestampMillis
            )

            TotpCard(
                title = account.title,
                username = account.username,
                category = account.category,
                code = code,
                remainingSeconds = tickerState.remainingSeconds,
                progress = tickerState.progress,
                onCopy = { copiedCode ->
                    Toast.makeText(context, "Copied code for ${account.title}", Toast.LENGTH_SHORT).show()
                }
            )
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            // Security Engine Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("security_status_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ShellSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, ShellBorder)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "CRYPTOGRAPHIC & SECURITY SUBSYSTEMS",
                        style = MaterialTheme.typography.labelSmall,
                        color = ClawCyan,
                        fontWeight = FontWeight.Bold
                    )

                    SecurityStatusItem(
                        title = "RFC 6238 TOTP Engine",
                        subtitle = "1s reactive countdown flow with Kotlin Time"
                    )

                    SecurityStatusItem(
                        title = "ShellCryption AES-GCM-256",
                        subtitle = "HKDF-SHA256 derivation & AAD integrity check"
                    )

                    SecurityStatusItem(
                        title = "AndroidKeyStore Biometric Layer",
                        subtitle = "Hardware-backed key authentication"
                    )

                    SecurityStatusItem(
                        title = "Anti-Screen Capture Enforced",
                        subtitle = "FLAG_SECURE active on window manager"
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SecurityStatusItem(
    title: String,
    subtitle: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = "Status Active",
            modifier = Modifier.size(18.dp),
            tint = ClawCyan
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 13.sp),
                color = TextPearl,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                color = TextMuted
            )
        }
    }
}
