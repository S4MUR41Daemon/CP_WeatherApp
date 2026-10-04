package com.atmos.weather.work

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.atmos.weather.AtmosApp
import java.util.concurrent.TimeUnit

class RefreshWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        return try {
            val app = applicationContext as AtmosApp
            val cached = app.container.repository.loadCached()
            val loc = cached?.location ?: return Result.success()
            val b = app.container.repository.refresh(loc)
            com.atmos.weather.widget.WidgetUpdater.update(applicationContext)
            if (b.fromMock) Result.retry() else Result.success()
        } catch (_: Throwable) {
            Result.retry()
        }
    }
}

object RefreshScheduler {
    const val WORK_NAME = "atmos_refresh"
    fun schedule(ctx: Context, minutes: Int) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()
        val req = PeriodicWorkRequestBuilder<RefreshWorker>(minutes.toLong(), TimeUnit.MINUTES)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, req)
    }
}
