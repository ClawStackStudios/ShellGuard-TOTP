package com.clawstack.shellguard.totp.ui.onboarding

import android.net.Uri
import com.clawstack.shellguard.totp.data.backup.PreValidationResult
import com.clawstack.shellguard.totp.data.local.entities.TotpItemEntity

/**
 * Onboarding steps for the first-run intake wizard.
 */
enum class IntakeStep {
    WELCOME,
    VALIDATING,
    PASSWORD_PROMPT,
    SUMMARY_CONFIRM,
    SECURITY_SETUP,
    COMPLETED
}

/**
 * Reactive UI state model representing the first-run intake & import state machine.
 */
data class IntakeUiState(
    val step: IntakeStep = IntakeStep.WELCOME,
    val selectedFileUri: Uri? = null,
    val fileName: String? = null,
    val preValidationResult: PreValidationResult? = null,
    val isDecrypting: Boolean = false,
    val isImporting: Boolean = false,
    val errorMessage: String? = null,
    val passwordInput: String = "",
    val confirmSecretInput: String = "",
    val isPinMode: Boolean = true,
    val enableBiometrics: Boolean = true,
    val isSecretVisible: Boolean = false,
    val decryptionSecret: String = "",
    val isReusingDecryptionSecret: Boolean = true,
    val importedCount: Int = 0,
    val decryptedItems: List<TotpItemEntity> = emptyList()
) {
    val isSecretValid: Boolean
        get() = if (decryptionSecret.isNotBlank() && isReusingDecryptionSecret) {
            passwordInput.isNotBlank() && passwordInput == confirmSecretInput
        } else if (isPinMode) {
            passwordInput.length in 4..8 && passwordInput.all { it.isDigit() } && passwordInput == confirmSecretInput
        } else {
            passwordInput.isNotBlank() && passwordInput == confirmSecretInput
        }

    val isSecretMismatch: Boolean
        get() = confirmSecretInput.isNotEmpty() && confirmSecretInput != passwordInput

    val canSubmitDecryptionPassword: Boolean
        get() = passwordInput.isNotBlank() && !isDecrypting

    val isBottomSheetVisible: Boolean
        get() = step == IntakeStep.PASSWORD_PROMPT || step == IntakeStep.SUMMARY_CONFIRM
}
