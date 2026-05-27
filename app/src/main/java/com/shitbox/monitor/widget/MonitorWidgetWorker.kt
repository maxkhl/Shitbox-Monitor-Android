package com.shitbox.monitor.widget

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class MonitorWidgetWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        return try {
            MonitorWidget.updateAll(applicationContext)
            Result.success()
        } catch (t: Throwable) {
            Result.retry()
        }
    }
}
