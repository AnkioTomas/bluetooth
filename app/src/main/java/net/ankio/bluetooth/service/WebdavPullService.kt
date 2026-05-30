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

/** 从 WebDAV 拉取：前台通知 + 周期同步到 pref。 */
class WebdavPullService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var pullJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopPullLoop()
                isRunning = false
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
        }

        val showToast = intent?.getBooleanExtra(EXTRA_SHOW_TOAST, false) == true
        isRunning = true
        enterForeground(buildNotification())
        startPullLoop(showToast)
        return START_STICKY
    }

    override fun onDestroy() {
        stopPullLoop()
        isRunning = false
        serviceScope.cancel()
        super.onDestroy()
        Log.i(TAG, "Webdav pull service stopped")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startPullLoop(showFirstToast: Boolean) {
        pullJob?.cancel()
        pullJob = serviceScope.launch {
            var showToast = showFirstToast
            while (isActive && WebdavMode.current() == WebdavMode.SyncFromWebdav) {
                val result = withContext(Dispatchers.IO) {
                    WebdavServiceManager.pullOnce(applicationContext)
                }
                refreshNotification()
                if (showToast) {
                    toastPullResult(result)
                    showToast = false
                }
                if (result == WebdavServiceManager.PullResult.NotConfigured) return@launch
                delay(WebdavServiceManager.INTERVAL_MS)
            }
        }
    }

    private fun stopPullLoop() {
        pullJob?.cancel()
        pullJob = null
    }

    private fun enterForeground(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun refreshNotification() {
        if (!isRunning) return
        enterForeground(buildNotification())
    }

    private fun buildNotification(): Notification {
        val last = SpUtils.getString(PrefKeys.WEBDAV_LAST, "").trim()
        val detail = if (last.isEmpty()) {
            getString(R.string.sync_from_webdav)
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

    private fun toastPullResult(result: WebdavServiceManager.PullResult) {
        val (message, style) = when (result) {
            WebdavServiceManager.PullResult.Ok -> getString(R.string.sync_pull_success) to ThemeToast.Style.Success
            WebdavServiceManager.PullResult.Empty -> getString(R.string.get_bluetooth_error) to ThemeToast.Style.Error
            WebdavServiceManager.PullResult.Failed -> getString(R.string.webdav_error) to ThemeToast.Style.Error
            WebdavServiceManager.PullResult.NotConfigured -> getString(R.string.webdav_not_configured) to ThemeToast.Style.Error
        }
        ThemeToast.show(message, style)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.sync_from_webdav),
            NotificationManager.IMPORTANCE_LOW,
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        private const val TAG = "WebdavPullService"
        private const val CHANNEL_ID = "webdav_pull_channel"
        private const val NOTIFICATION_ID = 1002
        private const val EXTRA_SHOW_TOAST = "show_toast"

        const val ACTION_START = "net.ankio.bluetooth.webdav.PULL_START"
        const val ACTION_STOP = "net.ankio.bluetooth.webdav.PULL_STOP"

        @Volatile
        var isRunning = false
            private set

        fun start(context: Context, showToast: Boolean = false) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, WebdavPullService::class.java).apply {
                    action = ACTION_START
                    putExtra(EXTRA_SHOW_TOAST, showToast)
                },
            )
        }

        fun stop(context: Context) {
            context.startService(
                Intent(context, WebdavPullService::class.java).apply {
                    action = ACTION_STOP
                },
            )
        }
    }
}
