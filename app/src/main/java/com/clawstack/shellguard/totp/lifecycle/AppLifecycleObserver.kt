package com.clawstack.shellguard.totp.lifecycle

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.clawstack.shellguard.totp.data.repository.AuthRepository

/**
 * Observes application backgrounding / foregrounding events to enforce security auto-lock timeouts.
 */
class AppLifecycleObserver(
    private val authRepository: AuthRepository,
    private val autoLockTimeoutMs: Long = 30_000L // 30 seconds background timeout
) : Application.ActivityLifecycleCallbacks {

    private var activityReferences = 0
    private var isActivityChangingConfigurations = false
    private var lastBackgroundTimestamp = 0L

    override fun onActivityStarted(activity: Activity) {
        if (++activityReferences == 1 && !isActivityChangingConfigurations) {
            // App entered foreground
            if (lastBackgroundTimestamp > 0L) {
                val elapsed = System.currentTimeMillis() - lastBackgroundTimestamp
                if (elapsed >= autoLockTimeoutMs) {
                    authRepository.lockVault()
                }
            }
        }
    }

    override fun onActivityStopped(activity: Activity) {
        isActivityChangingConfigurations = activity.isChangingConfigurations
        if (--activityReferences == 0 && !isActivityChangingConfigurations) {
            // App entered background
            lastBackgroundTimestamp = System.currentTimeMillis()
            if (autoLockTimeoutMs == 0L) {
                authRepository.lockVault()
            }
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityResumed(activity: Activity) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}
}
