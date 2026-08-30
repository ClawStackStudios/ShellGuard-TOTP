package com.clawstack.shellguard.totp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.clawstack.shellguard.totp.ui.theme.LocalShellGuardColors

/**
 * Swipeable TOTP Card component wrapping TotpCard with Material 3 SwipeToDismissBox.
 * Swiping end-to-start (right to left) reveals a danger background with a trash bin icon
 * and invokes onDelete().
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableTotpCard(
    title: String,
    username: String?,
    category: String?,
    code: String,
    remainingSeconds: Int,
    progress: Float,
    isLocalOnly: Boolean,
    onCopy: (String) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shellColors = LocalShellGuardColors.current
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                false
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(shellColors.danger)
                    .padding(horizontal = 20.dp)
                    .testTag("swipe_delete_background_${title.lowercase().replace(" ", "_")}"),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete 2FA Code",
                    tint = Color.White
                )
            }
        },
        modifier = modifier
    ) {
        TotpCard(
            title = title,
            username = username,
            category = category,
            code = code,
            remainingSeconds = remainingSeconds,
            progress = progress,
            isLocalOnly = isLocalOnly,
            onCopy = onCopy,
            onEdit = onEdit
        )
    }
}
