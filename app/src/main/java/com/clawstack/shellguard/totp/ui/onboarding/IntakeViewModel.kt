package com.clawstack.shellguard.totp.ui.onboarding

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.clawstack.shellguard.totp.ShellGuardTotpApp
import com.clawstack.shellguard.totp.data.backup.MultiVaultBackupPreValidator
import com.clawstack.shellguard.totp.data.backup.PreValidationResult
import com.clawstack.shellguard.totp.data.local.entities.TotpItemEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class IntakeViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as ShellGuardTotpApp
    private val database = app.database
    private val totpItemDao = database.totpItemDao()
    private val authRepository = app.authRepository

    private val _uiState = MutableStateFlow(IntakeUiState())
    val uiState: StateFlow<IntakeUiState> = _uiState.asStateFlow()

    /**
     * Inspects and pre-validates selected SAF OpenDocument URI.
     */
    fun onFileSelected(uri: Uri) {
        val resolvedFileName = queryFileName(uri) ?: uri.lastPathSegment ?: "backup.json"
        _uiState.update {
            it.copy(
                step = IntakeStep.VALIDATING,
                selectedFileUri = uri,
                fileName = resolvedFileName,
                errorMessage = null,
                passwordInput = "",
                confirmSecretInput = "",
                decryptedItems = emptyList()
            )
        }

        viewModelScope.launch {
            val validationResult = withContext(Dispatchers.IO) {
                try {
                    val inputStream = getApplication<Application>().contentResolver.openInputStream(uri)
                    if (inputStream == null) {
                        PreValidationResult.Error("Unable to open file stream.")
                    } else {
                        MultiVaultBackupPreValidator.validate(inputStream, resolvedFileName)
                    }
                } catch (e: Exception) {
                    PreValidationResult.Error(e.message ?: "Failed to read file.")
                }
            }

            when (validationResult) {
                is PreValidationResult.Success -> {
                    val detectedPin = validationResult.protectionMode?.equals("PASSWORD", ignoreCase = true) != true
                    val bioEnabled = validationResult.isBiometricEnabled
                    if (validationResult.isEncrypted) {
                        _uiState.update {
                            it.copy(
                                step = IntakeStep.PASSWORD_PROMPT,
                                preValidationResult = validationResult,
                                isPinMode = detectedPin,
                                enableBiometrics = bioEnabled,
                                errorMessage = null
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                step = IntakeStep.SUMMARY_CONFIRM,
                                preValidationResult = validationResult,
                                decryptedItems = validationResult.itemsPreview,
                                isPinMode = detectedPin,
                                enableBiometrics = bioEnabled,
                                errorMessage = null
                            )
                        }
                    }
                }
                is PreValidationResult.Error -> {
                    _uiState.update {
                        it.copy(
                            step = IntakeStep.WELCOME,
                            preValidationResult = validationResult,
                            errorMessage = validationResult.message
                        )
                    }
                }
            }
        }
    }

    fun onPasswordChanged(password: String) {
        _uiState.update { it.copy(passwordInput = password, errorMessage = null) }
    }

    fun onConfirmSecretChanged(confirmSecret: String) {
        _uiState.update { it.copy(confirmSecretInput = confirmSecret, errorMessage = null) }
    }

    fun onProtectionModeChanged(isPin: Boolean) {
        _uiState.update {
            it.copy(
                isPinMode = isPin,
                passwordInput = "",
                confirmSecretInput = "",
                errorMessage = null
            )
        }
    }

    fun onBiometricToggleChanged(enabled: Boolean) {
        _uiState.update { it.copy(enableBiometrics = enabled) }
    }

    fun toggleSecretVisibility() {
        _uiState.update { it.copy(isSecretVisible = !it.isSecretVisible) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun dismissBottomSheet() {
        _uiState.update {
            it.copy(
                step = IntakeStep.WELCOME,
                errorMessage = null,
                passwordInput = "",
                confirmSecretInput = ""
            )
        }
    }

    /**
     * Decrypts encrypted ShellGuard habitat using the password entered in PASSWORD_PROMPT step.
     */
    fun decryptEncryptedHabitat() {
        val currentState = _uiState.value
        val result = currentState.preValidationResult as? PreValidationResult.Success ?: return
        val rawJson = result.rawJson
        val password = currentState.passwordInput.trim()

        if (password.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Please enter the vault password or key.") }
            return
        }

        _uiState.update { it.copy(isDecrypting = true, errorMessage = null) }

        viewModelScope.launch {
            val decryptResult = withContext(Dispatchers.IO) {
                MultiVaultBackupPreValidator.decryptShellGuardBackup(
                    rawJson = rawJson,
                    passwordOrKey = password,
                    targetOwnerUuid = "local"
                )
            }

            decryptResult.fold(
                onSuccess = { items ->
                    _uiState.update {
                        it.copy(
                            step = IntakeStep.SUMMARY_CONFIRM,
                            isDecrypting = false,
                            decryptedItems = items,
                            decryptionSecret = password,
                            isReusingDecryptionSecret = true,
                            passwordInput = password,
                            confirmSecretInput = password,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isDecrypting = false,
                            errorMessage = error.message ?: "Incorrect password or corrupted backup file."
                        )
                    }
                }
            )
        }
    }

    /**
     * Toggles whether to reuse the verified secret from the decrypted backup
     * or establish a new PIN/Password (Key Rotation).
     */
    fun onToggleReuseSecret(reuse: Boolean) {
        _uiState.update { state ->
            if (reuse) {
                state.copy(
                    isReusingDecryptionSecret = true,
                    passwordInput = state.decryptionSecret,
                    confirmSecretInput = state.decryptionSecret,
                    errorMessage = null
                )
            } else {
                state.copy(
                    isReusingDecryptionSecret = false,
                    passwordInput = "",
                    confirmSecretInput = "",
                    errorMessage = null
                )
            }
        }
    }

    /**
     * Hatches the local vault with the selected protection (PIN / Master Password & Biometrics)
     * and persists all decrypted / extracted 2FA tokens to the Room SQLCipher database.
     */
    fun hatchAndImportVault(onComplete: () -> Unit) {
        val currentState = _uiState.value
        if (!currentState.isSecretValid) {
            _uiState.update {
                it.copy(
                    errorMessage = if (currentState.isPinMode) {
                        "PIN must be 4 to 8 digits and match confirmation."
                    } else {
                        "Passwords must not be empty and must match confirmation."
                    }
                )
            }
            return
        }

        val itemsToImport = currentState.decryptedItems
        val masterSecret = currentState.passwordInput.trim()
        val isPin = currentState.isPinMode
        val enableBiometrics = currentState.enableBiometrics

        _uiState.update { it.copy(isImporting = true, errorMessage = null) }

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                // 1. Upsert items into Room DB
                if (itemsToImport.isNotEmpty()) {
                    val sanitized = itemsToImport.map { item ->
                        item.copy(
                            ownerUuid = "local",
                            isLocalOnly = true,
                            syncState = "LOCAL",
                            localUpdatedAt = System.currentTimeMillis()
                        )
                    }
                    totpItemDao.upsertItems(sanitized)
                }

                // 2. Hatch Vault in AuthRepository (hardware KeyStore / preferences)
                authRepository.hatchVault(
                    masterSecret = masterSecret,
                    isPin = isPin,
                    enableBiometrics = enableBiometrics
                )
            }

            _uiState.update {
                it.copy(
                    step = IntakeStep.COMPLETED,
                    isImporting = false,
                    importedCount = itemsToImport.size
                )
            }

            onComplete()
        }
    }

    private fun queryFileName(uri: Uri): String? {
        return try {
            val cursor = getApplication<Application>().contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        it.getString(nameIndex)
                    } else null
                } else null
            }
        } catch (_: Exception) {
            uri.lastPathSegment
        }
    }
}
