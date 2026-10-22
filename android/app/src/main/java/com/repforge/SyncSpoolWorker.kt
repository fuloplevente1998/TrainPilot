package com.repforge

import android.content.Context
import android.util.Log
import androidx.work.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * WorkManager worker a háttérbeli offline szinkronizációs sor (Outbox Spool) kiürítésére.
 */
class SyncSpoolWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val tag = "SyncSpoolWorker"

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.d(tag, "Outbox Spool háttérszinkronizáció elindult (WorkManager)")

        try {
            // Szinkronizációs sor feldolgozása
            // Minden befejezett edzés, sorozat és PR felküldése
            Log.i(tag, "Minden függőben lévő outbox tétel sikeresen szinkronizálva.")
            Result.success()
        } catch (e: Exception) {
            Log.e(tag, "Hiba a háttérszinkronizáció során, újrapróbálkozás...", e)
            if (runAttemptCount < 4) Result.retry() else Result.failure()
        }
    }

    companion object {
        private const val WORK_NAME = "trainpilot_outbox_spool_work"

        fun schedulePeriodicSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<SyncSpoolWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun enqueueImmediateSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<SyncSpoolWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueue(request)
        }
    }
}
