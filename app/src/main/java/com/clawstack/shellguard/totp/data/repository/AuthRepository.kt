package com.clawstack.shellguard.totp.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.clawstack.shellguard.totp.crypto.ClawCrypto
import com.clawstack.shellguard.totp.crypto.EncryptedDeviceVault
import com.clawstack.shellguard.totp.data.remote.ApiClient
import com.clawstack.shellguard.totp.data.remote.models.SessionData
import com.clawstack.shellguard.totp.ui.theme.AppThemeMode
import com.clawstack.shellguard.totp.ui.theme.ThemeAccent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

enum class VaultProtectionMode { PIN, PASSWORD }

data class UserSession(
    val serverUrl: String,
    val userUuid: String,
    val username: String,
    val displayName: String?,
    val rawHuKey: String,
    val token: String,
    val expiresAt: String
)

class AuthRepository(
    private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("shellguard_auth_prefs", Context.MODE_PRIVATE)
    private val mutex = Mutex()

    private val _themeMode = MutableStateFlow(
        when (prefs.getString("pref_theme_mode", "DARK")) {
            "LIGHT" -> AppThemeMode.LIGHT
            "SYSTEM" -> AppThemeMode.SYSTEM
            else -> AppThemeMode.DARK
        }
    )
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    private val _themeAccent = MutableStateFlow(
        try {
            ThemeAccent.valueOf(prefs.getString("pref_theme_accent", ThemeAccent.REEF_DEFAULT.name) ?: ThemeAccent.REEF_DEFAULT.name)
        } catch (_: Exception) {
            ThemeAccent.REEF_DEFAULT
        }
    )
    val themeAccent: StateFlow<ThemeAccent> = _themeAccent.asStateFlow()

    private val _vaultMode = MutableStateFlow(
        if (prefs.getString("pref_vault_mode", "PIN") == "PASSWORD") VaultProtectionMode.PASSWORD else VaultProtectionMode.PIN
    )
    val vaultMode: StateFlow<VaultProtectionMode> = _vaultMode.asStateFlow()

    private val _currentSession = MutableStateFlow<UserSession?>(null)
    val currentSession: StateFlow<UserSession?> = _currentSession.asStateFlow()

    private val _isBiometricEnabled = MutableStateFlow(prefs.getBoolean("pref_biometric_enabled", false))
    val isBiometricEnabled: StateFlow<Boolean> = _isBiometricEnabled.asStateFlow()

    private val _isVaultHatched = MutableStateFlow(prefs.getBoolean("pref_vault_hatched", false))
    val isVaultHatched: StateFlow<Boolean> = _isVaultHatched.asStateFlow()

    private val _isLocked = MutableStateFlow(
        prefs.getBoolean("pref_vault_hatched", false) && (prefs.getBoolean("pref_biometric_enabled", false) || getVaultSecretHash() != null)
    )
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    private val _hasCompletedGuidedTour = MutableStateFlow(prefs.getBoolean("pref_guided_tour_completed", false))
    val hasCompletedGuidedTour: StateFlow<Boolean> = _hasCompletedGuidedTour.asStateFlow()

    private val _isBackupPromptDismissed = MutableStateFlow(prefs.getBoolean("pref_backup_prompt_dismissed", false))
    val isBackupPromptDismissed: StateFlow<Boolean> = _isBackupPromptDismissed.asStateFlow()

    private val _isAutoClearClipboard = MutableStateFlow(prefs.getBoolean("pref_auto_clear_clipboard", true))
    val isAutoClearClipboard: StateFlow<Boolean> = _isAutoClearClipboard.asStateFlow()

    private val _tourStep = MutableStateFlow(if (prefs.getBoolean("pref_guided_tour_completed", false)) 0 else 1)
    val tourStep: StateFlow<Int> = _tourStep.asStateFlow()

    suspend fun <T> withSyncLock(block: suspend () -> T): T = mutex.withLock {
        block()
    }

    init {
        // Restore saved server URL & username (non-sensitive)
        val savedServerUrl = prefs.getString("saved_server_url", null)
        val savedUserUuid = prefs.getString("saved_user_uuid", null)
        val savedUsername = prefs.getString("saved_username", null)
        val savedDisplayName = prefs.getString("saved_display_name", null)

        // Restore sensitive credentials from EncryptedDeviceVault with legacy migration
        val savedToken = getSecureStringWithMigration("saved_token")
        val savedKey = getSecureStringWithMigration("saved_raw_key")

        if (!savedServerUrl.isNullOrBlank() && !savedUserUuid.isNullOrBlank() && !savedToken.isNullOrBlank() && !savedKey.isNullOrBlank()) {
            _currentSession.value = UserSession(
                serverUrl = savedServerUrl,
                userUuid = savedUserUuid,
                username = savedUsername ?: "User",
                displayName = savedDisplayName,
                rawHuKey = savedKey,
                token = savedToken,
                expiresAt = prefs.getString("saved_expires_at", "") ?: ""
            )
            ApiClient.updateAuthToken(savedToken)
        }
    }

    private fun getSecureStringWithMigration(key: String): String? {
        val secured = EncryptedDeviceVault.getSecureString(context, key)
        if (!secured.isNullOrBlank()) return secured

        val legacyPlain = prefs.getString(key, null)
        if (!legacyPlain.isNullOrBlank()) {
            EncryptedDeviceVault.storeSecureString(context, key, legacyPlain)
            prefs.edit().remove(key).apply()
            return legacyPlain
        }
        return null
    }

    private fun getVaultSecretHash(): String? {
        return getSecureStringWithMigration("pref_vault_secret_hash")
    }

    suspend fun login(serverUrl: String, rawHuKey: String): Result<UserSession> = withContext(Dispatchers.IO) {
        mutex.withLock {
            runCatching {
                val cleanUrl = serverUrl.trim().removeSuffix("/")
                val cleanKey = rawHuKey.trim()
                val keyHash = ClawCrypto.hashHumanKey(cleanKey)

                val client = ApiClient.getClient(cleanUrl)
                val sessionData: SessionData = client.authenticate(keyHash).getOrThrow()

                val session = UserSession(
                    serverUrl = cleanUrl,
                    userUuid = sessionData.user.uuid,
                    username = sessionData.user.username,
                    displayName = sessionData.user.displayName,
                    rawHuKey = cleanKey,
                    token = sessionData.token,
                    expiresAt = sessionData.expiresAt
                )

                ApiClient.updateAuthToken(sessionData.token)
                _currentSession.value = session

                // Persist non-sensitive metadata in SharedPreferences
                prefs.edit()
                    .putString("saved_server_url", cleanUrl)
                    .putString("saved_user_uuid", session.userUuid)
                    .putString("saved_username", session.username)
                    .putString("saved_display_name", session.displayName)
                    .putString("saved_expires_at", session.expiresAt)
                    .remove("saved_token")
                    .remove("saved_raw_key")
                    .apply()

                // Persist sensitive token & master key securely in hardware-backed EncryptedDeviceVault
                EncryptedDeviceVault.storeSecureString(context, "saved_token", session.token)
                EncryptedDeviceVault.storeSecureString(context, "saved_raw_key", session.rawHuKey)

                session
            }
        }
    }

    suspend fun ensureAuthenticated(): Result<String> = withContext(Dispatchers.IO) {
        mutex.withLock {
            runCatching {
                val session = _currentSession.value ?: throw IllegalStateException("Not logged in")
                if (ApiClient.authToken != null) {
                    return@runCatching ApiClient.authToken!!
                }

                // Re-authenticate using key hash
                val client = ApiClient.getClient(session.serverUrl)
                val keyHash = ClawCrypto.hashHumanKey(session.rawHuKey)
                val refreshed = client.authenticate(keyHash).getOrThrow()
                
                ApiClient.updateAuthToken(refreshed.token)
                val updatedSession = session.copy(
                    token = refreshed.token,
                    expiresAt = refreshed.expiresAt
                )
                _currentSession.value = updatedSession
                EncryptedDeviceVault.storeSecureString(context, "saved_token", refreshed.token)

                refreshed.token
            }
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        _isBiometricEnabled.value = enabled
        prefs.edit().putBoolean("pref_biometric_enabled", enabled).apply()
    }

    fun hatchVault(masterSecret: String, isPin: Boolean, enableBiometrics: Boolean) {
        val trimmedSecret = masterSecret.trim()
        val secretHash = ClawCrypto.hashHumanKey(trimmedSecret)
        val mode = if (isPin) VaultProtectionMode.PIN else VaultProtectionMode.PASSWORD
        
        // Instant hardware key generation for the selected mode
        if (isPin) {
            com.clawstack.shellguard.totp.crypto.AndroidKeyStoreHelper.getOrCreatePinSecretKey()
        } else {
            com.clawstack.shellguard.totp.crypto.AndroidKeyStoreHelper.getOrCreatePasswordSecretKey()
        }
        
        if (enableBiometrics) {
            com.clawstack.shellguard.totp.crypto.AndroidKeyStoreHelper.getOrCreateBiometricSecretKey()
        } else {
            com.clawstack.shellguard.totp.crypto.AndroidKeyStoreHelper.deleteKey(com.clawstack.shellguard.totp.crypto.AndroidKeyStoreHelper.KEY_ALIAS_BIOMETRIC_WRAPPER)
        }

        EncryptedDeviceVault.storeSecureString(context, "pref_vault_secret", trimmedSecret)
        EncryptedDeviceVault.storeSecureString(context, "pref_vault_secret_hash", secretHash)
        prefs.edit()
            .putBoolean("pref_vault_hatched", true)
            .putString("pref_vault_mode", mode.name)
            .putBoolean("pref_biometric_enabled", enableBiometrics)
            .remove("pref_vault_secret_hash")
            .apply()

        _isVaultHatched.value = true
        _vaultMode.value = mode
        _isBiometricEnabled.value = enableBiometrics
        _isLocked.value = false
    }

    fun updateVaultSecret(newSecret: String, isPin: Boolean) {
        val trimmedSecret = newSecret.trim()
        val secretHash = ClawCrypto.hashHumanKey(trimmedSecret)
        val mode = if (isPin) VaultProtectionMode.PIN else VaultProtectionMode.PASSWORD
        
        // Instant hardware key generation for the selected mode
        if (isPin) {
            com.clawstack.shellguard.totp.crypto.AndroidKeyStoreHelper.getOrCreatePinSecretKey()
        } else {
            com.clawstack.shellguard.totp.crypto.AndroidKeyStoreHelper.getOrCreatePasswordSecretKey()
        }

        EncryptedDeviceVault.storeSecureString(context, "pref_vault_secret", trimmedSecret)
        EncryptedDeviceVault.storeSecureString(context, "pref_vault_secret_hash", secretHash)
        prefs.edit()
            .putString("pref_vault_mode", mode.name)
            .remove("pref_vault_secret_hash")
            .apply()
        _vaultMode.value = mode
    }

    fun getVaultSecret(): String? {
        return EncryptedDeviceVault.getSecureString(context, "pref_vault_secret")
    }

    fun unlockWithSecret(inputSecret: String): Boolean {
        val savedHash = getVaultSecretHash() ?: return true
        val inputHash = ClawCrypto.hashHumanKey(inputSecret.trim())
        val matches = savedHash.equals(inputHash, ignoreCase = true)
        if (matches) {
            _isLocked.value = false
        }
        return matches
    }

    fun unlockWithBiometrics(): Boolean {
        _isLocked.value = false
        return true
    }

    fun lockVault() {
        if (_isVaultHatched.value) {
            _isLocked.value = true
        }
    }

    fun setTourStep(step: Int) {
        _tourStep.value = step
    }

    fun setGuidedTourCompleted(completed: Boolean) {
        _hasCompletedGuidedTour.value = completed
        _tourStep.value = if (completed) 0 else 1
        prefs.edit().putBoolean("pref_guided_tour_completed", completed).apply()
    }

    fun setBackupPromptDismissed(dismissed: Boolean) {
        _isBackupPromptDismissed.value = dismissed
        prefs.edit().putBoolean("pref_backup_prompt_dismissed", dismissed).apply()
    }

    fun setAutoClearClipboard(enabled: Boolean) {
        _isAutoClearClipboard.value = enabled
        prefs.edit().putBoolean("pref_auto_clear_clipboard", enabled).apply()
    }

    fun setThemeMode(mode: AppThemeMode) {
        _themeMode.value = mode
        prefs.edit().putString("pref_theme_mode", mode.name).apply()
    }

    fun setThemeAccent(accent: ThemeAccent) {
        _themeAccent.value = accent
        prefs.edit().putString("pref_theme_accent", accent.name).apply()
    }

    fun logout() {
        _currentSession.value = null
        ApiClient.reset()
        prefs.edit()
            .remove("saved_token")
            .remove("saved_raw_key")
            .remove("saved_expires_at")
            .apply()
        EncryptedDeviceVault.removeSecureString(context, "saved_token")
        EncryptedDeviceVault.removeSecureString(context, "saved_raw_key")
    }

    fun getSavedServerUrl(): String? = prefs.getString("saved_server_url", null)
    fun getSavedKey(): String? = getSecureStringWithMigration("saved_raw_key")
    fun clearSavedKey() {
        prefs.edit().remove("saved_raw_key").apply()
        EncryptedDeviceVault.removeSecureString(context, "saved_raw_key")
    }
}
