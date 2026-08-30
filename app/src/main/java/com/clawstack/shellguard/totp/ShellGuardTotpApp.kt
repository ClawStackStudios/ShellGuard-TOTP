package com.clawstack.shellguard.totp

import android.app.Application
import android.util.Log
import com.clawstack.shellguard.totp.data.backup.BackupManager
import com.clawstack.shellguard.totp.data.local.ShellGuardTotpDatabase
import com.clawstack.shellguard.totp.data.repository.AuthRepository
import com.clawstack.shellguard.totp.data.repository.TotpRepository
import com.clawstack.shellguard.totp.data.sync.TotpSyncWorker
import com.clawstack.shellguard.totp.lifecycle.AppLifecycleObserver

/**
 * ShellGuard-TOTP Application Class
 * Responsible for core security initialization, dependencies, and SQLCipher native binary loading.
 */
class ShellGuardTotpApp : Application() {

    val database: ShellGuardTotpDatabase by lazy {
        ShellGuardTotpDatabase.getInstance(this)
    }

    val authRepository: AuthRepository by lazy {
        AuthRepository(this)
    }

    val totpRepository: TotpRepository by lazy {
        TotpRepository(
            totpItemDao = database.totpItemDao(),
            syncMetadataDao = database.syncMetadataDao()
        )
    }

    val backupManager: BackupManager by lazy {
        BackupManager(database.totpItemDao())
    }

    override fun onCreate() {
        super.onCreate()
        initializeSecurityFoundation()
        registerActivityLifecycleCallbacks(AppLifecycleObserver(authRepository))
        if (!isRobolectric()) {
            try {
                TotpSyncWorker.schedulePeriodicSync(this)
            } catch (ignored: Throwable) {
                // WorkManager might not be initialized in test environments
            }
        }
    }

    private fun isRobolectric(): Boolean {
        return android.os.Build.FINGERPRINT == "robolectric"
    }

    private fun initializeSecurityFoundation() {
        try {
            System.loadLibrary("sqlcipher")
            Log.i(TAG, "SQLCipher native libraries initialized successfully.")
        } catch (e: Throwable) {
            Log.i(TAG, "SQLCipher native library note: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "ShellGuardTotpApp"
    }
}

