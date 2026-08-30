package com.clawstack.shellguard.totp.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clawstack.shellguard.totp.ui.theme.LocalShellGuardColors

/**
 * Dynamic Canvas Circular Countdown Ring conforming to Reef Modernist design.
 * Depletes counter-clockwise over the TOTP period and smoothly animates color based on remaining time:
 * Secondary Accent (30s–11s) -> Warning (10s–6s) -> Danger (< 5s).
 */
@Composable
fun TotpCountdownRing(
    remainingSeconds: Int,
    progress: Float,
    modifier: Modifier = Modifier
) {
    val shellColors = LocalShellGuardColors.current
    val targetColor = when {
        remainingSeconds <= 5 -> shellColors.danger
        remainingSeconds <= 10 -> shellColors.warning
        else -> shellColors.secondaryAccent
    }
    val ringColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(400),
        label = "RingColorInterpolation"
    )
    val trackColor = shellColors.borderSubtle.copy(alpha = 0.5f)

    Box(
        modifier = modifier.size(38.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(34.dp)) {
            // Track circle
            drawCircle(
                color = trackColor,
                style = Stroke(width = 3.dp.toPx())
            )
            // Depleting progress arc
            drawArc(
                color = ringColor,
                startAngle = -90f,
                sweepAngle = progress * 360f,
                useCenter = false,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )
        }
        Text(
            text = "$remainingSeconds",
            color = ringColor,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}


