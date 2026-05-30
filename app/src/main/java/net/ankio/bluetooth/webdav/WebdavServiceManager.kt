package net.ankio.bluetooth.webdav

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ankio.bluetooth.R
import net.ankio.bluetooth.ble.BlePermissions
import net.ankio.bluetooth.ble.BleScanFilter
import net.ankio.bluetooth.ble.BleScanner
import net.ankio.bluetooth.ble.BluetoothData
import net.ankio.bluetooth.ble.Rssi
import net.ankio.bluetooth.model.WebdavMode
import net.ankio.bluetooth.service.WebdavPullService
import net.ankio.bluetooth.service.WebdavPushService
import net.ankio.bluetooth.utils.PrefKeys
import net.ankio.bluetooth.utils.SpUtils
import net.ankio.bluetooth.utils.WebdavUtils
import net.ankio.webdav.lib.WebDavConfigStore
import net.ankio.theme.toast.ThemeToast

/** 按 WebDAV 模式启停前台服务：发送端扫描+上传，接收端拉取+写 pref。 */
object WebdavServiceManager {

    private const val TAG = "WebdavServiceManager"

    const val INTERVAL_MS = 5 * 60_000L
    const val SCAN_TIMEOUT_MS = 90_000L

    fun isConfigured(context: Context): Boolean =
        WebDavConfigStore.load(context).isValid()

    fun hasTargetMac(): Boolean =
        SpUtils.getString(PrefKeys.PREF_MAC, "").trim().isNotEmpty()

    suspend fun applyMode(context: Context, mode: WebdavMode, showToast: Boolean = true) {
        val app = context.applicationContext
        when (mode) {
            WebdavMode.None -> stopAll(app)
            WebdavMode.Sender2Webdav -> {
                stopPull(app)
                when {
                    !isConfigured(app) -> {
                        stopPush(app)
                        if (showToast) toastError(app, R.string.webdav_not_configured)
                    }
                    !hasTargetMac() -> {
                        stopPush(app)
                        if (showToast) toastError(app, R.string.webdav_no_target)
                    }
                    else -> startPush(app, showToast)
                }
            }
            WebdavMode.SyncFromWebdav -> {
                stopPush(app)
                if (!isConfigured(app)) {
                    stopPull(app)
                    if (showToast) toastError(app, R.string.webdav_not_configured)
                } else {
                    startPull(app, showToast)
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
        Log.d(TAG,"pushOnce: start")
        if (!isConfigured(context)) return@withContext PushResult.NotConfigured
        if (!BlePermissions.hasScan(context.applicationContext)) {
            Log.w(TAG, "pushOnce: missing BLE permissions")
            return@withContext PushResult.NoPermission
        }
        val mac = SpUtils.getString(PrefKeys.PREF_MAC, "").trim()
        Log.d(TAG,"pushOnce: filter mac $mac")
        if (mac.isEmpty()) return@withContext PushResult.NoMac

        val scanned = BleScanner(context.applicationContext).scanOnce(
            BleScanFilter.forMac(mac),
            SCAN_TIMEOUT_MS,
        )
        val data = when {
            scanned != null -> BluetoothData(
                scanned.data,
                scanned.address,
                scanned.rssi.toString(),
            )
            else -> {
                Log.w(TAG, "pushOnce: scan miss for $mac, fallback to pref data")
                readPrefBluetoothData(mac) ?: return@withContext PushResult.NotFound
            }
        }

        try {
            WebdavUtils(context.applicationContext).sendToServer(data)
            Log.i(TAG, "pushOnce: uploaded ${data.mac}")
            PushResult.Ok
        } catch (e: Exception) {
            Log.e(TAG, "pushOnce failed", e)
            PushResult.Failed
        }
    }

    private fun readPrefBluetoothData(mac: String): BluetoothData? {
        val prefData = SpUtils.getString(PrefKeys.PREF_DATA, "").trim()
        if (prefData.isEmpty()) return null
        return BluetoothData(
            prefData,
            mac,
            SpUtils.getString(PrefKeys.PREF_RSSI, "-65"),
        )
    }

    private fun applyRemoteToPref(data: BluetoothData) {
        SpUtils.putString(PrefKeys.PREF_MAC, data.mac)
        SpUtils.putString(PrefKeys.PREF_DATA, data.data)
        SpUtils.putString(PrefKeys.PREF_RSSI, Rssi.normalizeDbmString(data.rssi).toString())
    }

    private fun startPush(context: Context, showToast: Boolean) {
        WebdavPushService.start(context, showToast = showToast)
    }

    private fun stopPush(context: Context) {
        WebdavPushService.stop(context)
    }

    private fun startPull(context: Context, showToast: Boolean) {
        WebdavPullService.start(context, showToast = showToast)
    }

    private fun stopPull(context: Context) {
        WebdavPullService.stop(context)
    }

    private fun stopAll(context: Context) {
        stopPush(context)
        stopPull(context)
    }

    private suspend fun toastError(context: Context, messageRes: Int) {
        withContext(Dispatchers.Main) {
            ThemeToast.show(context.getString(messageRes), ThemeToast.Style.Error)
        }
    }

    enum class PullResult { Ok, Empty, Failed, NotConfigured }

    enum class PushResult { Ok, NotFound, Failed, NotConfigured, NoMac, NoPermission }
}
