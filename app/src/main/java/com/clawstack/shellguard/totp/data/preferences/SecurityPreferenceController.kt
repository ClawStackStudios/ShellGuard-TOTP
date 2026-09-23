package com.clawstack.shellguard.totp.data.preferences

import android.content.Context
import android.content.SharedPreferences
import com.clawstack.shellguard.totp.data.local.dao.AuditLogDao
import com.clawstack.shellguard.totp.data.local.entities.AuditLogEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Phase 12 / Task 23: Security Preference Controller.
 * Manages runtime screen protection, tap-to-reveal timeout, panic purge configuration,
 * and records security audit trail events into Room.
 */
class SecurityPreferenceController(
    private val context: Context,
    private val auditLogDao: AuditLogDao
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("shellguard_auth_prefs", Context.MODE_PRIVATE)
    private val ioScope = CoroutineScope(Dispatchers.IO)

    private val _allowScreenshots = MutableStateFlow(prefs.getBoolean(PREF_ALLOW_SCREENSHOTS, false))
    val allowScreenshots: StateFlow<Boolean> = _allowScreenshots.asStateFlow()

    private val _tapRevealTimeoutSeconds = MutableStateFlow(prefs.getInt(PREF_TAP_REVEAL_TIMEOUT, DEFAULT_TAP_REVEAL_TIMEOUT))
    val tapRevealTimeoutSeconds: StateFlow<Int> = _tapRevealTimeoutSeconds.asStateFlow()

    private val _panicTriggerEnabled = MutableStateFlow(prefs.getBoolean(PREF_PANIC_TRIGGER_ENABLED, false))
    val panicTriggerEnabled: StateFlow<Boolean> = _panicTriggerEnabled.asStateFlow()

    fun setAllowScreenshots(enabled: Boolean) {
        val previous = _allowScreenshots.value
        _allowScreenshots.value = enabled
        prefs.edit().putBoolean(PREF_ALLOW_SCREENSHOTS, enabled).apply()
        recordAuditEvent(
            eventType = EVENT_SCREEN_SECURITY_CHANGED,
            detail = "allowScreenshots changed from $previous to $enabled"
        )
    }

    fun setTapRevealTimeout(seconds: Int) {
        _tapRevealTimeoutSeconds.value = seconds
        prefs.edit().putInt(PREF_TAP_REVEAL_TIMEOUT, seconds).apply()
    }

    fun setPanicTriggerEnabled(enabled: Boolean) {
        val previous = _panicTriggerEnabled.value
        _panicTriggerEnabled.value = enabled
        prefs.edit().putBoolean(PREF_PANIC_TRIGGER_ENABLED, enabled).apply()
        recordAuditEvent(
            eventType = "PANIC_SETTING_CHANGED",
            detail = "panicTriggerEnabled changed from $previous to $enabled"
        )
    }

    fun recordAuditEvent(eventType: String, detail: String? = null) {
        ioScope.launch {
            try {
                auditLogDao.insertEvent(
                    AuditLogEntity(
                        eventType = eventType,
                        detail = detail,
                        timestampMs = System.currentTimeMillis()
                    )
                )
            } catch (ignored: Throwable) {
                // Room may be unavailable or being wiped
            }
        }
    }

    companion object {
        const val PREF_ALLOW_SCREENSHOTS = "pref_security_allow_screenshots"
        const val PREF_TAP_REVEAL_TIMEOUT = "pref_security_tap_reveal_timeout"
        const val PREF_PANIC_TRIGGER_ENABLED = "pref_security_panic_trigger"

        const val DEFAULT_TAP_REVEAL_TIMEOUT = 30

        const val EVENT_VAULT_UNLOCKED = "VAULT_UNLOCKED"
        const val EVENT_BIOMETRIC_FAILED = "BIOMETRIC_FAILED"
        const val EVENT_BACKUP_CREATED = "BACKUP_CREATED"
        const val EVENT_SECRET_ADDED = "SECRET_ADDED"
        const val EVENT_PANIC_TRIGGERED = "PANIC_TRIGGERED"
        const val EVENT_SCREEN_SECURITY_CHANGED = "SCREEN_SECURITY_CHANGED"
        const val EVENT_SYNC_COMPLETED = "SYNC_COMPLETED"
        const val EVENT_SYNC_FAILED = "SYNC_FAILED"
    }
}
