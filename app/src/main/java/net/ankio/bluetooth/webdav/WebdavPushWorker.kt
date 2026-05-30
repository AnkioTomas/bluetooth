package net.ankio.bluetooth.webdav

import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import net.ankio.bluetooth.model.WebdavMode
import net.ankio.bluetooth.service.WebdavPushService

class WebdavPushWorker(
    context: android.content.Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (WebdavMode.current() != WebdavMode.Sender2Webdav) return Result.success()
        if (!WebdavServiceManager.isConfigured(applicationContext)) return Result.success()
        if (!WebdavServiceManager.hasTargetMac()) return Result.success()

        return when (WebdavServiceManager.pushOnce(applicationContext)) {
            WebdavServiceManager.PushResult.Ok -> {
                WebdavPushService.updateNotification(applicationContext)
                Result.success()
            }
            WebdavServiceManager.PushResult.Failed -> {
                Log.w(TAG, "Periodic push failed")
                Result.retry()
            }
            else -> Result.success()
        }
    }

    companion object {
        private const val TAG = "WebdavPushWorker"
    }
}
