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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ankio.bluetooth.R
import net.ankio.bluetooth.model.WebdavMode
import net.ankio.bluetooth.utils.PrefKeys
import net.ankio.bluetooth.utils.SpUtils
import net.ankio.bluetooth.webdav.WebdavServiceManager
import net.ankio.theme.toast.ThemeToast

/** 发送到 WebDAV：前台通知 + 周期扫描上传。 */
class WebdavPushService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var pushJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopPushLoop()
                isRunning = false
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
        }

        val showToast = intent?.getBooleanExtra(EXTRA_SHOW_TOAST, false) == true
        isRunning = true
        enterForeground(buildNotification())
        startPushLoop(showToast)
        return START_STICKY
    }

    override fun onDestroy() {
        stopPushLoop()
        isRunning = false
        serviceScope.cancel()
        super.onDestroy()
        Log.i(TAG, "Webdav push service stopped")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startPushLoop(showFirstToast: Boolean) {
        pushJob?.cancel()
        pushJob = serviceScope.launch {
            var showToast = showFirstToast
            loop@ while (isActive && WebdavMode.current() == WebdavMode.Sender2Webdav) {
                val result = withContext(Dispatchers.IO) {
                    WebdavServiceManager.pushOnce(applicationContext)
                }
                refreshNotification()
                if (showToast) {
                    toastPushResult(result)
                    showToast = false
                }
                when (result) {
                    WebdavServiceManager.PushResult.NotConfigured -> return@launch
                    WebdavServiceManager.PushResult.NoPermission -> {
                        delay(30_000L)
                        continue@loop
                    }
                    else -> delay(WebdavServiceManager.INTERVAL_MS)
                }
            }
        }
    }

    private fun stopPushLoop() {
        pushJob?.cancel()
        pushJob = null
    }

    private fun enterForeground(notification: Notification) {
        startForeground(
            NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE or
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION,
        )
    }

    private fun refreshNotification() {
        if (!isRunning) return
        val notification = buildNotification()
        enterForeground(notification)
    }

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

    private fun toastPushResult(result: WebdavServiceManager.PushResult) {
        val (message, style) = when (result) {
            WebdavServiceManager.PushResult.Ok -> getString(R.string.sync_push_success) to ThemeToast.Style.Success
            WebdavServiceManager.PushResult.NotFound -> getString(R.string.webdav_push_not_found) to ThemeToast.Style.Error
            WebdavServiceManager.PushResult.Failed -> getString(R.string.webdav_error) to ThemeToast.Style.Error
            WebdavServiceManager.PushResult.NotConfigured -> getString(R.string.webdav_not_configured) to ThemeToast.Style.Error
            WebdavServiceManager.PushResult.NoPermission -> getString(R.string.no_permission) to ThemeToast.Style.Error
        }
        ThemeToast.show(message, style)
    }

    private fun createNotificationChannel() {
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
        private const val EXTRA_SHOW_TOAST = "show_toast"

        const val ACTION_START = "net.ankio.bluetooth.webdav.PUSH_START"
        const val ACTION_STOP = "net.ankio.bluetooth.webdav.PUSH_STOP"

        @Volatile
        var isRunning = false
            private set

        fun start(context: Context, showToast: Boolean = false) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, WebdavPushService::class.java).apply {
                    action = ACTION_START
                    putExtra(EXTRA_SHOW_TOAST, showToast)
                },
            )
        }

        fun stop(context: Context) {
            context.startService(
                Intent(context, WebdavPushService::class.java).apply {
                    action = ACTION_STOP
                },
            )
        }
    }
}
