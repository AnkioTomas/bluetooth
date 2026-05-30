package net.ankio.bluetooth.webdav

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ankio.bluetooth.R
import net.ankio.bluetooth.ble.BleScanFilter
import net.ankio.bluetooth.ble.BleScanner
import net.ankio.bluetooth.ble.BluetoothData
import net.ankio.bluetooth.ble.Rssi
import net.ankio.bluetooth.model.WebdavMode
import net.ankio.bluetooth.utils.PrefKeys
import net.ankio.bluetooth.utils.SpUtils
import net.ankio.bluetooth.utils.WebdavUtils
import net.ankio.webdav.lib.WebDavConfigStore
import net.ankio.theme.toast.ThemeToast
import java.util.concurrent.TimeUnit

/** 按 WebDAV 模式启停周期任务：发送端扫描+上传，接收端拉取+写 pref。 */
object WebdavServiceManager {

    private const val TAG = "WebdavServiceManager"
    private const val UNIQUE_PUSH = "webdav_push_periodic"
    private const val UNIQUE_PULL = "webdav_pull_periodic"

    const val INTERVAL_MINUTES = 15L
    const val SCAN_TIMEOUT_MS = 90_000L

    fun isConfigured(context: Context): Boolean =
        WebDavConfigStore.load(context).isValid()

    fun hasTargetMac(): Boolean =
        SpUtils.getString(PrefKeys.PREF_MAC, "").trim().isNotEmpty()

    suspend fun applyMode(context: Context, mode: WebdavMode, showToast: Boolean = true) {
        val app = context.applicationContext
        when (mode) {
            WebdavMode.None -> cancelAll(app)
            WebdavMode.Sender2Webdav -> {
                cancelPull(app)
                when {
                    !isConfigured(app) -> if (showToast) toastError(app, R.string.webdav_not_configured)
                    !hasTargetMac() -> if (showToast) toastError(app, R.string.webdav_no_target)
                    else -> schedulePush(app)
                }
            }
            WebdavMode.SyncFromWebdav -> {
                cancelPush(app)
                if (!isConfigured(app)) {
                    cancelPull(app)
                    if (showToast) toastError(app, R.string.webdav_not_configured)
                } else {
                    schedulePull(app)
                    if (showToast) toastPullResult(app, pullOnce(app))
                }
            }
        }
    }

    suspend fun pullOnce(context: Context): PullResult = withContext(Dispatchers.IO) {
        if (!isConfigured(context)) return@withContext PullResult.NotConfigured
        try {
            val data = WebdavUtils(context.applicationContext).getFromServer()
                ?: return@withContext PullResult.Empty
            applyRemoteToPref(data)
            PullResult.Ok
        } catch (e: Exception) {
            Log.e(TAG, "pullOnce failed", e)
            PullResult.Failed
        }
    }

    suspend fun pushOnce(context: Context): PushResult = withContext(Dispatchers.IO) {
        if (!isConfigured(context)) return@withContext PushResult.NotConfigured
        val mac = SpUtils.getString(PrefKeys.PREF_MAC, "").trim()
        if (mac.isEmpty()) return@withContext PushResult.NoMac
        val device = BleScanner(context.applicationContext).scanOnce(
            BleScanFilter.forMac(mac),
            SCAN_TIMEOUT_MS,
        ) ?: return@withContext PushResult.NotFound
        val data = BluetoothData(device.data, device.address, device.rssi.toString())
        try {
            WebdavUtils(context.applicationContext).sendToServer(data)
            PushResult.Ok
        } catch (e: Exception) {
            Log.e(TAG, "pushOnce failed", e)
            PushResult.Failed
        }
    }

    private fun applyRemoteToPref(data: BluetoothData) {
        SpUtils.putString(PrefKeys.PREF_MAC, data.mac)
        SpUtils.putString(PrefKeys.PREF_DATA, data.data)
        SpUtils.putString(PrefKeys.PREF_RSSI, Rssi.normalizeDbmString(data.rssi).toString())
    }

    private fun schedulePush(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val periodic = PeriodicWorkRequestBuilder<WebdavPushWorker>(
            INTERVAL_MINUTES,
            TimeUnit.MINUTES,
        ).setConstraints(constraints).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_PUSH,
            ExistingPeriodicWorkPolicy.UPDATE,
            periodic,
        )
        WorkManager.getInstance(context).enqueue(
            OneTimeWorkRequestBuilder<WebdavPushWorker>().setConstraints(constraints).build(),
        )
    }

    private fun schedulePull(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val periodic = PeriodicWorkRequestBuilder<WebdavPullWorker>(
            INTERVAL_MINUTES,
            TimeUnit.MINUTES,
        ).setConstraints(constraints).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_PULL,
            ExistingPeriodicWorkPolicy.UPDATE,
            periodic,
        )
        WorkManager.getInstance(context).enqueue(
            OneTimeWorkRequestBuilder<WebdavPullWorker>().setConstraints(constraints).build(),
        )
    }

    private fun cancelPush(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_PUSH)
    }

    private fun cancelPull(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_PULL)
    }

    private fun cancelAll(context: Context) {
        cancelPush(context)
        cancelPull(context)
    }

    private suspend fun toastPullResult(context: Context, result: PullResult) {
        withContext(Dispatchers.Main) {
            when (result) {
                PullResult.Ok -> ThemeToast.show(
                    context.getString(R.string.sync_pull_success),
                    ThemeToast.Style.Success,
                )
                PullResult.Empty -> ThemeToast.show(
                    context.getString(R.string.get_bluetooth_error),
                    ThemeToast.Style.Error,
                )
                PullResult.Failed -> ThemeToast.show(
                    context.getString(R.string.webdav_error),
                    ThemeToast.Style.Error,
                )
                PullResult.NotConfigured -> ThemeToast.show(
                    context.getString(R.string.webdav_not_configured),
                    ThemeToast.Style.Error,
                )
            }
        }
    }

    private suspend fun toastError(context: Context, messageRes: Int) {
        withContext(Dispatchers.Main) {
            ThemeToast.show(context.getString(messageRes), ThemeToast.Style.Error)
        }
    }

    enum class PullResult { Ok, Empty, Failed, NotConfigured }

    enum class PushResult { Ok, NotFound, Failed, NotConfigured, NoMac }
}
