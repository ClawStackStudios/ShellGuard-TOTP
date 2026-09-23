package com.clawstack.shellguard.totp.security

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Process
import android.util.Log
import com.clawstack.shellguard.totp.ShellGuardTotpApp
import com.clawstack.shellguard.totp.crypto.AndroidKeyStoreHelper
import com.clawstack.shellguard.totp.crypto.EncryptedDeviceVault
import com.clawstack.shellguard.totp.data.preferences.SecurityPreferenceController

/**
 * Phase 12 / Task 23: Panic Trigger Receiver.
 * Receives emergency panic wipe broadcasts, validates panic trigger entitlement,
 * wipes all hardware KeyStore aliases, encrypted shared preferences, Room SQLite database tables,
 * records panic event if possible, and terminates the process.
 */
class PanicTriggerReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION_PANIC_WIPE) return

        val app = context.applicationContext as? ShellGuardTotpApp
        val panicEnabled = app?.securityPreferenceController?.panicTriggerEnabled?.value ?: true
        if (!panicEnabled) {
            Log.w(TAG, "Panic trigger received but panic wipe is disabled in settings.")
            return
        }

        Log.e(TAG, "EMERGENCY PANIC TRIGGER ACTIVATED - Purging all local cryptographic credentials and databases.")

        try {
            app?.securityPreferenceController?.recordAuditEvent(
                eventType = SecurityPreferenceController.EVENT_PANIC_TRIGGERED,
                detail = "Emergency panic purge executed"
            )
        } catch (ignored: Throwable) {}

        // 1. Wipe hardware KeyStore keys
        try {
            AndroidKeyStoreHelper.deleteAllKeys()
        } catch (e: Throwable) {
            Log.e(TAG, "Error wiping KeyStore keys: ${e.message}")
        }

        // 2. Wipe EncryptedDeviceVault preferences
        try {
            EncryptedDeviceVault.clearAll(context)
        } catch (e: Throwable) {
            Log.e(TAG, "Error clearing EncryptedDeviceVault: ${e.message}")
        }

        // 3. Purge Room SQLite database tables
        try {
            app?.database?.clearAllTables()
        } catch (e: Throwable) {
            Log.e(TAG, "Error purging database tables: ${e.message}")
        }

        // 4. Wipe regular SharedPreferences
        try {
            context.getSharedPreferences("shellguard_auth_prefs", Context.MODE_PRIVATE)
                .edit()
                .clear()
                .apply()
        } catch (e: Throwable) {
            Log.e(TAG, "Error clearing auth prefs: ${e.message}")
        }

        // 5. Terminate process immediately
        Process.killProcess(Process.myPid())
    }

    companion object {
        private const val TAG = "PanicTriggerReceiver"
        const val ACTION_PANIC_WIPE = "com.clawstack.shellguard.totp.ACTION_PANIC_WIPE"
    }
}
