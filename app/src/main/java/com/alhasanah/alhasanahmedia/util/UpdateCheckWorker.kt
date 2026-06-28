package com.alhasanah.alhasanahmedia.util

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

class UpdateCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "Checking for updates in background...")
            val result = UpdateChecker.checkUpdateAsync()

            when (result) {
                is UpdateResult.Available -> {
                    Log.d(TAG, "Update available: ${result.info.versionName}")
                    // Show notification only — do NOT auto-download to save bandwidth
                    UpdateNotificationHelper.showUpdateAvailable(
                        context = applicationContext,
                        versionName = result.info.versionName,
                        changelog = result.info.changelog
                    )
                    Result.success()
                }
                is UpdateResult.UpToDate -> {
                    Log.d(TAG, "App is up to date")
                    Result.success()
                }
                is UpdateResult.Error -> {
                    Log.w(TAG, "Update check failed: ${result.message}")
                    Result.retry()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking update", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "UpdateCheckWorker"
        private const val WORK_NAME = "alhasanah_update_check"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<UpdateCheckWorker>(
                repeatInterval = 6,
                repeatIntervalTimeUnit = TimeUnit.HOURS
            )
                .setInitialDelay(1, TimeUnit.HOURS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
            Log.d(TAG, "Periodic update check scheduled (every 6 hours)")
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            Log.d(TAG, "Periodic update check cancelled")
        }
    }
}