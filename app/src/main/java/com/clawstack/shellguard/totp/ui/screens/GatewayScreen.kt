package com.clawstack.shellguard.totp.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clawstack.shellguard.totp.ui.theme.AbyssalDeep
import com.clawstack.shellguard.totp.ui.theme.ClawCyan
import com.clawstack.shellguard.totp.ui.theme.LobsterRed
import com.clawstack.shellguard.totp.ui.theme.ShellBorder
import com.clawstack.shellguard.totp.ui.theme.ShellSurfaceElevated
import com.clawstack.shellguard.totp.ui.theme.TextMuted
import com.clawstack.shellguard.totp.ui.theme.TextPearl
import com.clawstack.shellguard.totp.ui.theme.WarningBoxBg
import com.clawstack.shellguard.totp.ui.theme.WarningText
import com.clawstack.shellguard.totp.ui.viewmodels.GatewayUiState
import com.clawstack.shellguard.totp.ui.viewmodels.GatewayViewModel

@Composable
fun GatewayScreen(
    viewModel: GatewayViewModel,
    modifier: Modifier = Modifier,
    onLoginSuccess: () -> Unit = {},
    onBackClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val savedServerUrl by viewModel.savedServerUrl.collectAsStateWithLifecycle(initialValue = null)
    val savedKey by viewModel.savedKey.collectAsStateWithLifecycle(initialValue = null)

    var isUploadMode by remember { mutableStateOf(false) }
    var protocol by remember { mutableStateOf("https") }
    var serverHost by remember { mutableStateOf("") }
    var serverPort by remember { mutableStateOf("") }
    var isUrlValid by remember { mutableStateOf(true) }
    var urlErrorMessage by remember { mutableStateOf("") }
    var isProtocolDropdownExpanded by remember { mutableStateOf(false) }
    var isHostFocused by remember { mutableStateOf(false) }
    var isPortFocused by remember { mutableStateOf(false) }
    var keyText by remember { mutableStateOf("") }
    var uploadedKey by remember { mutableStateOf<String?>(null) }
    var uploadedFileName by remember { mutableStateOf<String?>(null) }

    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    fun parseAndSetUrl(input: String) {
        var raw = input.trim()
        if (raw.isEmpty()) {
            serverHost = ""
            return
        }
        if (raw.startsWith("https://", ignoreCase = true)) {
            protocol = "https"
            raw = raw.substring("https://".length)
        } else if (raw.startsWith("http://", ignoreCase = true)) {
            protocol = "http"
            raw = raw.substring("http://".length)
        }

        if (raw.contains(":")) {
            val parts = raw.split(":")
            serverHost = parts[0]
            if (parts.size > 1) {
                val portAndPath = parts[1]
                val slashIndex = portAndPath.indexOf('/')
                if (slashIndex != -1) {
                    serverPort = portAndPath.substring(0, slashIndex).filter { it.isDigit() }
                    serverHost += portAndPath.substring(slashIndex)
                } else {
                    serverPort = portAndPath.filter { it.isDigit() }
                }
            }
        } else {
            serverHost = raw
        }
    }

    val fileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                val text = context.contentResolver.openInputStream(it)?.bufferedReader()?.use { r -> r.readText() }
                if (text != null) {
                    var fileName = "shellguard_identity.json"
                    context.contentResolver.query(it, null, null, null, null)?.use { cursor ->
                        val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1 && cursor.moveToFirst()) {
                            fileName = cursor.getString(nameIndex)
                        }
                    }
                    val extractedKey = cleanAndExtractKey(text)
                    if (extractedKey.isNotEmpty()) {
                        uploadedKey = extractedKey
                        uploadedFileName = fileName
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    LaunchedEffect(savedServerUrl) {
        savedServerUrl?.let { url ->
            if (serverHost.isEmpty() && url.isNotEmpty()) parseAndSetUrl(url)
        }
    }

    LaunchedEffect(savedKey) {
        savedKey?.let { key ->
            if (keyText.isEmpty() && key.isNotEmpty()) {
                keyText = key
                isUploadMode = false
            }
        }
    }

    LaunchedEffect(uiState) {
        if (uiState is GatewayUiState.Success) {
            onLoginSuccess()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .imePadding()
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Navigation Bar with Back Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .size(44.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    .clip(CircleShape)
                    .testTag("gateway_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ShellGuard Brand Icon
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "🐚", fontSize = 36.sp)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Brand Title
        Text(
            text = buildAnnotatedString {
                withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary)) { append("Shell") }
                withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.secondary)) { append("Guard") }
                withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)) { append(" ©™") }
            },
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Authenticator Gateway",
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Login with your ShellKey©™ identity",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Segmented URL Bar
        val animatedPortWidth by animateDpAsState(
            targetValue = if (isPortFocused) 105.dp else 65.dp,
            label = "PortWidth"
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .zIndex(100f)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(
                            width = if (isHostFocused || isPortFocused || !isUrlValid) 1.5.dp else 1.dp,
                            color = if (!isUrlValid) MaterialTheme.colorScheme.error else if (isPortFocused || isHostFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            shape = RoundedCornerShape(12.dp)
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Protocol Button
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxHeight()
                            .background(if (!isUrlValid) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                            .clickable { isProtocolDropdownExpanded = !isProtocolDropdownExpanded }
                            .padding(horizontal = 14.dp)
                    ) {
                        Text(
                            text = if (protocol == "https") "https://" else "http://",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = if (isProtocolDropdownExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                            contentDescription = "Select protocol",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Host Input
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (serverHost.isEmpty()) {
                                Text(
                                    text = "vault.example.com",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    fontSize = 14.sp
                                )
                            }
                            BasicTextField(
                                value = serverHost,
                                onValueChange = {
                                    parseAndSetUrl(it)
                                    isUrlValid = true
                                },
                                textStyle = TextStyle(
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                cursorBrush = SolidColor(if (isHostFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                keyboardActions = KeyboardActions(onNext = { focusManager.clearFocus() }),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onFocusChanged { isHostFocused = it.isFocused }
                                    .testTag("gateway_host_input")
                            )
                        }
                    }

                    // Divider
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(24.dp)
                            .background(if (!isUrlValid) MaterialTheme.colorScheme.error else if (isPortFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                    )

                    // Port Section
                    Box(
                        modifier = Modifier
                            .width(animatedPortWidth)
                            .height(56.dp)
                            .background(if (!isUrlValid) MaterialTheme.colorScheme.error.copy(alpha = 0.12f) else if (isPortFocused) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (serverPort.isEmpty()) {
                                    Text(
                                        text = "Port",
                                        color = if (!isUrlValid) MaterialTheme.colorScheme.error else if (isPortFocused) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        fontSize = 14.sp
                                    )
                                }
                                BasicTextField(
                                    value = serverPort,
                                    onValueChange = { input ->
                                        serverPort = input.filter { it.isDigit() }
                                        isUrlValid = true
                                    },
                                    textStyle = TextStyle(
                                        color = if (!isUrlValid) MaterialTheme.colorScheme.error else if (isPortFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    cursorBrush = SolidColor(if (isPortFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .onFocusChanged { isPortFocused = it.isFocused }
                                        .testTag("gateway_port_input")
                                )
                            }
                        }
                    }
                }

                if (!isUrlValid) {
                    Text(
                        text = urlErrorMessage,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }
            }

            // Protocol Dropdown
            androidx.compose.animation.AnimatedVisibility(
                visible = isProtocolDropdownExpanded,
                enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(),
                modifier = Modifier
                    .zIndex(110f)
                    .padding(start = 0.dp, top = 60.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                    modifier = Modifier.width(110.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    protocol = "http"
                                    isProtocolDropdownExpanded = false
                                    isUrlValid = true
                                }
                                .background(if (protocol == "http") MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent)
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "http://",
                                color = if (protocol == "http") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (protocol == "http") FontWeight.Bold else FontWeight.Medium,
                                fontSize = 14.sp
                            )
                        }
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    protocol = "https"
                                    isProtocolDropdownExpanded = false
                                    isUrlValid = true
                                }
                                .background(if (protocol == "https") MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent)
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "https://",
                                color = if (protocol == "https") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (protocol == "https") FontWeight.Bold else FontWeight.Medium,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Mode Toggles: Upload File vs Paste Key
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp))
                    .background(if (isUploadMode) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { isUploadMode = true }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Upload File",
                    fontWeight = FontWeight.SemiBold,
                    color = if (isUploadMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp))
                    .background(if (!isUploadMode) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { isUploadMode = false }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Paste ShellKey©™",
                    fontWeight = FontWeight.SemiBold,
                    color = if (!isUploadMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (isUploadMode) {
            UploadFileView(
                uploadedFileName = uploadedFileName,
                onTap = { fileLauncher.launch("application/json") },
                onClear = {
                    uploadedKey = null
                    uploadedFileName = null
                }
            )
        } else {
            PasteKeyView(
                keyText = keyText,
                onKeyTextChanged = { keyText = it },
                hasSavedKey = savedKey != null,
                onClearSavedKey = {
                    viewModel.clearSavedKey()
                    keyText = ""
                }
            )
        }

        if (uiState is GatewayUiState.Error) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = (uiState as GatewayUiState.Error).message,
                color = MaterialTheme.colorScheme.error,
                fontSize = 13.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        val isFormValid = if (isUploadMode) {
            uploadedKey != null && serverHost.isNotBlank()
        } else {
            keyText.isNotBlank() && serverHost.isNotBlank()
        }

        Button(
            onClick = {
                val cleanHost = serverHost.trim()
                    .removePrefix("https://")
                    .removePrefix("http://")
                val finalUrl = buildString {
                    append(if (protocol == "https") "https://" else "http://")
                    append(cleanHost)
                    if (serverPort.isNotBlank()) {
                        append(":")
                        append(serverPort.trim())
                    }
                }
                val targetKey = cleanAndExtractKey(if (isUploadMode) uploadedKey.orEmpty() else keyText)
                viewModel.login(finalUrl, targetKey)
            },
            enabled = uiState !is GatewayUiState.Loading && isFormValid,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("gateway_login_button"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            if (uiState is GatewayUiState.Loading) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
            } else {
                Text("Login with Identity", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun UploadFileView(
    uploadedFileName: String?,
    onTap: () -> Unit,
    onClear: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .border(
                    width = 1.dp,
                    color = if (uploadedFileName != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(12.dp)
                )
                .clip(RoundedCornerShape(12.dp))
                .background(if (uploadedFileName != null) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                .clickable { onTap() }
                .testTag("gateway_file_dropzone"),
            contentAlignment = Alignment.Center
        ) {
            if (uploadedFileName != null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                    Text(text = "🐚 Identity Loaded Successfully!", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "File: $uploadedFileName", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Tap to change file", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "Tap to upload your identity file", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = ".json files only", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                }
            }
        }

        if (uploadedFileName != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onClear,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Remove File", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        WarningBox()
    }
}

@Composable
fun PasteKeyView(
    keyText: String,
    onKeyTextChanged: (String) -> Unit,
    hasSavedKey: Boolean,
    onClearSavedKey: () -> Unit
) {
    var passwordVisible by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = keyText,
            onValueChange = onKeyTextChanged,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("gateway_key_input"),
            placeholder = { Text("Paste your hu- or lb- key here...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            shape = RoundedCornerShape(12.dp),
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                        contentDescription = if (passwordVisible) "Hide key" else "Show key",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            )
        )

        if (hasSavedKey && keyText.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onClearSavedKey,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Clear Saved Key", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        WarningBox()
    }
}

@Composable
fun WarningBox() {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = WarningBoxBg),
        border = BorderStroke(1.dp, WarningBoxBg.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = "Warning",
                tint = WarningText,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text("Can't find your identity file?", color = WarningText, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    "Your identity file or hu- key is the only way to access your vault. If lost, vault secrets cannot be recovered.",
                    color = WarningText.copy(alpha = 0.85f),
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }
        }
    }
}

fun cleanAndExtractKey(rawInput: String): String {
    val trimmed = rawInput.trim()
    if (trimmed.isEmpty()) return ""

    val tokenJsonMatch = """"token"\s*:\s*"([^"]+)"""".toRegex().find(trimmed)
    if (tokenJsonMatch != null) {
        val extracted = tokenJsonMatch.groupValues[1].trim()
        if (extracted.isNotEmpty()) return extracted
    }

    val keyPatternMatch = """(hu-|lb-)[a-zA-Z0-9_-]+""".toRegex().find(trimmed)
    if (keyPatternMatch != null) {
        return keyPatternMatch.value.trim()
    }

    return trimmed.removeSurrounding("\"").removeSurrounding("'").trim()
}
