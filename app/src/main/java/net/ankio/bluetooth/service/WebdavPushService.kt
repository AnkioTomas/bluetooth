package net.ankio.bluetooth.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import net.ankio.bluetooth.R
import net.ankio.bluetooth.utils.PrefKeys
import net.ankio.bluetooth.utils.SpUtils

/** 发送到 WebDAV 模式下的常驻前台通知。 */
class WebdavPushService : Service() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                isRunning = false
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_UPDATE -> {
                if (isRunning) {
                    getSystemService(NotificationManager::class.java)
                        .notify(NOTIFICATION_ID, buildNotification())
                }
                return START_STICKY
            }
        }
        isRunning = true
        startForeground(
            NOTIFICATION_ID,
            buildNotification(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE,
        )
        return START_STICKY
    }

    override fun onDestroy() {
        isRunning = false
        super.onDestroy()
        Log.i(TAG, "Webdav push service stopped")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(): Notification {
        val last = SpUtils.getString(PrefKeys.WEBDAV_LAST, "").trim()
        val detail = if (last.isEmpty()) {
            getString(R.string.server_name)
        } else {
            getString(R.string.webdav_last_sync, last)
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(detail)
            .setSmallIcon(R.drawable.ic_bluetooth_scan)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.server_name),
            NotificationManager.IMPORTANCE_LOW,
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        private const val TAG = "WebdavPushService"
        private const val CHANNEL_ID = "webdav_push_channel"
        private const val NOTIFICATION_ID = 1001

        const val ACTION_START = "net.ankio.bluetooth.webdav.PUSH_START"
        const val ACTION_STOP = "net.ankio.bluetooth.webdav.PUSH_STOP"
        const val ACTION_UPDATE = "net.ankio.bluetooth.webdav.PUSH_UPDATE"

        @Volatile
        var isRunning = false
            private set

        fun start(context: Context) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, WebdavPushService::class.java).apply {
                    action = ACTION_START
                },
            )
        }

        fun stop(context: Context) {
            if (!isRunning) return
            context.startService(
                Intent(context, WebdavPushService::class.java).apply {
                    action = ACTION_STOP
                },
            )
        }

        fun updateNotification(context: Context) {
            if (!isRunning) return
            context.startService(
                Intent(context, WebdavPushService::class.java).apply {
                    action = ACTION_UPDATE
                },
            )
        }
    }
}
