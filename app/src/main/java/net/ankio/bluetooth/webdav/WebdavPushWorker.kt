package net.ankio.bluetooth.webdav

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import net.ankio.bluetooth.R
import net.ankio.bluetooth.model.WebdavMode

class WebdavPushWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (WebdavMode.current() != WebdavMode.Sender2Webdav) return Result.success()
        if (!WebdavServiceManager.isConfigured(applicationContext)) return Result.success()
        if (!WebdavServiceManager.hasTargetMac()) return Result.success()

        setForeground(createForegroundInfo())
        return when (WebdavServiceManager.pushOnce(applicationContext)) {
            WebdavServiceManager.PushResult.Failed -> {
                Log.w(TAG, "Periodic push failed")
                Result.retry()
            }
            else -> Result.success()
        }
    }

    private fun createForegroundInfo(): ForegroundInfo {
        createNotificationChannel()
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle(applicationContext.getString(R.string.app_name))
            .setContentText(applicationContext.getString(R.string.server_name))
            .setSmallIcon(R.drawable.ic_bluetooth_scan)
            .setOngoing(true)
            .build()
        return ForegroundInfo(
            NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE,
        )
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            applicationContext.getString(R.string.server_name),
            NotificationManager.IMPORTANCE_LOW,
        )
        applicationContext.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    companion object {
        private const val TAG = "WebdavPushWorker"
        private const val CHANNEL_ID = "webdav_push_channel"
        private const val NOTIFICATION_ID = 1001
    }
}
