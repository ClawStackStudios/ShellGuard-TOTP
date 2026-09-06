package com.clawstack.shellguard.totp.ui.viewmodels

import android.app.Application
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.clawstack.shellguard.totp.ShellGuardTotpApp
import com.clawstack.shellguard.totp.crypto.AndroidKeyStoreHelper
import com.clawstack.shellguard.totp.data.repository.AuthRepository
import com.clawstack.shellguard.totp.data.repository.UserSession
import com.clawstack.shellguard.totp.data.repository.VaultProtectionMode
import com.clawstack.shellguard.totp.ui.theme.AppThemeMode
import com.clawstack.shellguard.totp.ui.theme.ThemeAccent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.crypto.Cipher

sealed interface AuthState {
    data object Idle : AuthState
    data object Loading : AuthState
    data object Authenticated : AuthState
    data class Error(val message: String) : AuthState
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as ShellGuardTotpApp
    private val authRepository: AuthRepository = app.authRepository

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    val themeMode: StateFlow<AppThemeMode> = authRepository.themeMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, authRepository.themeMode.value)

    val themeAccent: StateFlow<ThemeAccent> = authRepository.themeAccent
        .stateIn(viewModelScope, SharingStarted.Eagerly, authRepository.themeAccent.value)

    val vaultMode: StateFlow<VaultProtectionMode> = authRepository.vaultMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, authRepository.vaultMode.value)

    // ── Phase 11 / Task 21: structured preference streams ───────────────────
    val appearancePrefs = authRepository.appearancePrefs
    val behaviorPrefs = authRepository.behaviorPrefs

    fun setViewMode(mode: com.clawstack.shellguard.totp.data.preferences.EntryViewMode) = authRepository.setViewMode(mode)
    fun setShowIcons(enabled: Boolean) = authRepository.setShowIcons(enabled)
    fun setShowNextCode(enabled: Boolean) = authRepository.setShowNextCode(enabled)
    fun setExpireBlinkIndicator(enabled: Boolean) = authRepository.setExpireBlinkIndicator(enabled)
    fun setDigitGrouping(enabled: Boolean) = authRepository.setDigitGrouping(enabled)
    fun setIssuerDisplayMode(mode: com.clawstack.shellguard.totp.data.preferences.IssuerDisplayMode) = authRepository.setIssuerDisplayMode(mode)
    fun setFocusSearchOnStart(enabled: Boolean) = authRepository.setFocusSearchOnStart(enabled)
    fun setSearchScope(scope: com.clawstack.shellguard.totp.data.preferences.SearchScope) = authRepository.setSearchScope(scope)
    fun setMinimizeOnCopy(enabled: Boolean) = authRepository.setMinimizeOnCopy(enabled)
    fun setCopyOnTap(enabled: Boolean) = authRepository.setCopyOnTap(enabled)
    fun setHapticFeedback(enabled: Boolean) = authRepository.setHapticFeedback(enabled)
    fun setMultiselectGroups(enabled: Boolean) = authRepository.setMultiselectGroups(enabled)
    fun setHighlightTokensOnTap(enabled: Boolean) = authRepository.setHighlightTokensOnTap(enabled)
    fun setFreezeTokensOnTap(enabled: Boolean) = authRepository.setFreezeTokensOnTap(enabled)

    /** Phase 11.5 / Task 22c — session state & disconnect for SettingsServerSyncScreen (ViewModel-first wiring). */
    val currentSession: StateFlow<UserSession?> = authRepository.currentSession

    fun logout() = authRepository.logout()

    /** Phase 11.5 / Task 22c — Spotlight Tour step 2 host (migrated from legacy SettingsScreen). */
    val tourStep: StateFlow<Int> = authRepository.tourStep

    fun setGuidedTourCompleted() = authRepository.setGuidedTourCompleted(true)


    val isVaultHatched: StateFlow<Boolean> = authRepository.isVaultHatched
        .stateIn(viewModelScope, SharingStarted.Eagerly, authRepository.isVaultHatched.value)

    val isBiometricEnabled: StateFlow<Boolean> = authRepository.isBiometricEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, authRepository.isBiometricEnabled.value)

    val isLocked: StateFlow<Boolean> = authRepository.isLocked
        .stateIn(viewModelScope, SharingStarted.Eagerly, authRepository.isLocked.value)

    private val _isBiometricAvailable = MutableStateFlow(checkBiometricAvailability())
    val isBiometricAvailable: StateFlow<Boolean> = _isBiometricAvailable.asStateFlow()

    private fun checkBiometricAvailability(): Boolean {
        val biometricManager = BiometricManager.from(getApplication())
        val canAuthenticate = biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )
        return canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun promptBiometrics(
        activity: FragmentActivity,
        onResult: ((Boolean) -> Unit)? = null
    ) {
        val executor = ContextCompat.getMainExecutor(activity)

        val biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    authRepository.unlockWithBiometrics()
                    _authState.value = AuthState.Authenticated
                    onResult?.invoke(true)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    if (errorCode != BiometricPrompt.ERROR_USER_CANCELED && errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                        _authState.value = AuthState.Error(errString.toString())
                    }
                    onResult?.invoke(false)
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    _authState.value = AuthState.Error("Biometric verification failed. Please try again.")
                    onResult?.invoke(false)
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock ShellGuard Vault")
            .setSubtitle("Authenticate using your biometric credentials")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        try {
            _authState.value = AuthState.Loading
            // Attempt hardware-backed CryptoObject authentication when available
            try {
                val cipher = AndroidKeyStoreHelper.getBiometricCipher(Cipher.ENCRYPT_MODE)
                biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
            } catch (cryptoEx: Throwable) {
                // Fallback to standard biometric/device credential prompt without crypto object
                biometricPrompt.authenticate(promptInfo)
            }
        } catch (e: Exception) {
            _authState.value = AuthState.Error(e.message ?: "Biometric prompt initialization failed")
            onResult?.invoke(false)
        }
    }

    fun unlockWithMasterKey(secretOrKey: String): Boolean = unlockWithSecret(secretOrKey)

    fun unlockWithSecret(secretOrKey: String): Boolean {
        _authState.value = AuthState.Loading
        val success = authRepository.unlockWithSecret(secretOrKey)
        if (success) {
            _authState.value = AuthState.Authenticated
        } else {
            val mode = authRepository.vaultMode.value
            val modeName = if (mode == VaultProtectionMode.PIN) "PIN code" else "master password"
            _authState.value = AuthState.Error("Incorrect $modeName. Please try again.")
        }
        return success
    }

    fun updateVaultSecret(newSecret: String, isPin: Boolean) {
        authRepository.updateVaultSecret(newSecret, isPin)
    }

    fun unlockWithBiometrics() {
        authRepository.unlockWithBiometrics()
        _authState.value = AuthState.Authenticated
    }

    fun hatchVault(masterSecret: String, isPin: Boolean, enableBiometrics: Boolean) {
        viewModelScope.launch {
            authRepository.hatchVault(masterSecret, isPin, enableBiometrics)
            _authState.value = AuthState.Authenticated
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        authRepository.setBiometricEnabled(enabled)
    }

    fun setThemeMode(mode: AppThemeMode) {
        authRepository.setThemeMode(mode)
    }

    fun setThemeAccent(accent: ThemeAccent) {
        authRepository.setThemeAccent(accent)
    }

    fun lock() {
        authRepository.lockVault()
        _authState.value = AuthState.Idle
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }
}
