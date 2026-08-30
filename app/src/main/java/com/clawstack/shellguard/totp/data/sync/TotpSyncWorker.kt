package com.clawstack.shellguard.totp.data.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.clawstack.shellguard.totp.ShellGuardTotpApp
import java.util.concurrent.TimeUnit

class TotpSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? ShellGuardTotpApp ?: return Result.failure()
        val authRepo = app.authRepository
        val session = authRepo.currentSession.value ?: return Result.success()

        val totpRepo = app.totpRepository
        val syncResult = authRepo.withSyncLock {
            totpRepo.syncRemoteVault(
                serverUrl = session.serverUrl,
                rawHuKey = session.rawHuKey,
                userUuid = session.userUuid
            )
        }

        return if (syncResult.isSuccess) {
            Result.success()
        } else {
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "shellguard_totp_periodic_sync"

        fun schedulePeriodicSync(context: Context, intervalMinutes: Long = 15) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncRequest = PeriodicWorkRequestBuilder<TotpSyncWorker>(intervalMinutes, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
            )
        }

        fun cancelPeriodicSync(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
