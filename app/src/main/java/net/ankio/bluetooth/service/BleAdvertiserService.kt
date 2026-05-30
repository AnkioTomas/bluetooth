package net.ankio.bluetooth.service

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.os.ParcelUuid
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.ankio.bluetooth.R
import net.ankio.bluetooth.ble.BlePermissions
import net.ankio.bluetooth.utils.ByteUtils
import net.ankio.bluetooth.utils.PrefKeys
import net.ankio.bluetooth.utils.SpUtils
import net.ankio.theme.toast.ThemeToast
import java.util.UUID

/** 附近发送端：前台服务 + BLE 广播，配置来自 pref。 */
class BleAdvertiserService : Service() {

    private var bluetoothLeAdvertiser: BluetoothLeAdvertiser? = null
    private var advertiseCallback: AdvertiseCallback? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var timeoutJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        val manager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothLeAdvertiser = manager.adapter?.bluetoothLeAdvertiser
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // startForegroundService() 要求尽快调用 startForeground()，校验失败也要先进入前台再退出
        enterForeground()
        stopBleAdvertise()
        startBleAdvertise()
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        cancelTimeout()
        stopBleAdvertise()
        isRunning = false
        serviceScope.cancel()
        super.onDestroy()
    }

    @SuppressLint("MissingPermission")
    private fun startBleAdvertise() {
        val adapter = (getSystemService(BLUETOOTH_SERVICE) as BluetoothManager).adapter
        if (adapter?.isEnabled != true) {
            fail(R.string.not_open_bluetooth)
            return
        }
        if (bluetoothLeAdvertiser == null) {
            fail(R.string.ble_advertiser_not_supported)
            return
        }
        if (!BlePermissions.hasAdvertise(this)) {
            fail(R.string.no_permission)
            return
        }

        val dataBytes = ByteUtils.hexStringToBytes(
            SpUtils.getString(PrefKeys.PREF_DATA, "").trim(),
        )
        if (dataBytes.isEmpty()) {
            fail(R.string.simulate_no_config)
            return
        }

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
            .setConnectable(true)
            .setTimeout(0)
            .build()
        val advertiseData = buildAdvertiseData(dataBytes)
        val scanResponse = AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            .setIncludeTxPowerLevel(false)
            .build()

        advertiseCallback = object : AdvertiseCallback() {
            override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
                isRunning = true
                scheduleTimeout()
            }

            override fun onStartFailure(errorCode: Int) {
                isRunning = false
                failMessage(advertiseErrorMessage(errorCode))
                stopAndQuit()
            }
        }

        try {
            bluetoothLeAdvertiser?.startAdvertising(settings, advertiseData, scanResponse, advertiseCallback)
        } catch (e: Exception) {
            Log.e(TAG, "startAdvertising failed", e)
            isRunning = false
            failMessage(e.message ?: getString(R.string.ble_advertiser_start_failed))
            stopAndQuit()
        }
    }

    @SuppressLint("MissingPermission")
    private fun stopBleAdvertise() {
        cancelTimeout()
        advertiseCallback?.let { callback ->
            if (BlePermissions.hasAdvertise(this)) {
                runCatching { bluetoothLeAdvertiser?.stopAdvertising(callback) }
            }
        }
        advertiseCallback = null
        isRunning = false
    }

    private fun stopAndQuit() {
        stopBleAdvertise()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun fail(messageRes: Int) {
        failMessage(getString(messageRes))
        stopAndQuit()
    }

    private fun failMessage(message: String) {
        ThemeToast.show(message, ThemeToast.Style.Error)
    }

    private fun scheduleTimeout() {
        cancelTimeout()
        timeoutJob = serviceScope.launch {
            delay(ADVERTISE_TIMEOUT_MS)
            Log.i(TAG, "advertising timeout after ${ADVERTISE_TIMEOUT_MS / 60_000} minutes")
            ThemeToast.show(getString(R.string.ble_advertiser_timeout), ThemeToast.Style.Info)
            stopAndQuit()
        }
    }

    private fun cancelTimeout() {
        timeoutJob?.cancel()
        timeoutJob = null
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.ble_advertiser_title),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.ble_advertiser_desc)
            setShowBadge(false)
            setSound(null, null)
            enableVibration(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun enterForeground() {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.ble_advertiser_title))
            .setContentText(getString(R.string.ble_advertiser_active))
            .setSmallIcon(R.drawable.ic_bluetooth_scan)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .build()
        startForeground(
            NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE or
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION,
        )
    }

    private fun buildAdvertiseData(data: ByteArray): AdvertiseData {
        val builder = AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            .setIncludeTxPowerLevel(false)

        val elements = parseRawAdvertisingData(data)

        // 1. 标志位 (Flags) 处理
        // 注意：Android 系统通常会自动添加 Flags。这里我们仅记录，暂不尝试强制修改，
        // 因为强制修改可能需要反射调用隐藏 API 或在某些 ROM 上无效。
        elements.find { it.type == 0x01 }?.let { flagsElement ->
            Log.d(TAG, "Flags from raw adv data: ${ByteUtils.byteArrayToHexString(flagsElement.data)}")
        }

        // 2. 优先添加服务 UUID (Type 0x03)，以保持与原始设备一致的物理顺序
        elements.filter { it.type == 0x03 }.forEach { element ->
            val fieldData = element.data
            if (fieldData.size >= 2 && fieldData.size % 2 == 0) {
                for (i in fieldData.indices step 2) {
                    val uuidBytes = fieldData.sliceArray(i until i + 2)
                    val uuid = UUID.fromString(
                        String.format(
                            "%04X-0000-1000-8000-00805F9B34FB",
                            (uuidBytes[1].toInt() and 0xFF shl 8) or (uuidBytes[0].toInt() and 0xFF)
                        )
                    )
                    builder.addServiceUuid(ParcelUuid(uuid))
                    Log.d(TAG, "Added 16-bit service UUID: 0x${String.format("%04X", (uuidBytes[1].toInt() and 0xFF shl 8) or (uuidBytes[0].toInt() and 0xFF))}")
                }
            }
        }

        // 3. 添加厂商自定义数据 (Type 0xFF)
        elements.filter { it.type == 0xFF }.forEach { element ->
            val fieldData = element.data
            if (fieldData.size >= 2) {
                val companyId = ((fieldData[1].toInt() and 0xFF) shl 8) or (fieldData[0].toInt() and 0xFF)
                val manufacturerData = if (fieldData.size > 2) fieldData.sliceArray(2 until fieldData.size) else byteArrayOf()

                builder.addManufacturerData(companyId, manufacturerData)
                Log.d(TAG, "Added manufacturer data: id=0x${String.format("%04X", companyId)}, length=${manufacturerData.size}")
            }
        }

        return builder.build()
    }

    /**
     * 将原始字节数组解析为 AD 元素列表
     */
    private fun parseRawAdvertisingData(data: ByteArray): List<AdElement> {
        val elements = mutableListOf<AdElement>()
        var index = 0
        while (index < data.size) {
            val length = (data[index++].toInt() and 0xFF)
            if (length == 0 || index + length > data.size) break

            val type = (data[index].toInt() and 0xFF)
            val fieldData = if (length > 1) {
                data.copyOfRange(index + 1, index + length)
            } else {
                byteArrayOf()
            }
            elements.add(AdElement(type, fieldData))
            index += length
        }
        return elements
    }

    private data class AdElement(val type: Int, val data: ByteArray) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as AdElement
            if (type != other.type) return false
            if (!data.contentEquals(other.data)) return false
            return true
        }

        override fun hashCode(): Int {
            var result = type
            result = 31 * result + data.contentHashCode()
            return result
        }
    }

    private fun advertiseErrorMessage(errorCode: Int): String = when (errorCode) {
        AdvertiseCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED ->
            getString(R.string.ble_advertiser_not_supported)
        AdvertiseCallback.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS ->
            getString(R.string.ble_advertiser_failed_too_many)
        AdvertiseCallback.ADVERTISE_FAILED_ALREADY_STARTED ->
            getString(R.string.ble_advertiser_failed_already_started)
        AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE ->
            getString(R.string.ble_advertiser_failed_data_too_large)
        AdvertiseCallback.ADVERTISE_FAILED_INTERNAL_ERROR ->
            getString(R.string.ble_advertiser_failed_internal)
        else -> getString(R.string.ble_advertiser_unknown_error, errorCode)
    }

    companion object {
        private const val TAG = "BleAdvertiserService"
        private const val CHANNEL_ID = "ble_advertiser_channel"
        private const val NOTIFICATION_ID = 2001
        private const val ADVERTISE_TIMEOUT_MS = 10 * 60 * 1000L

        @Volatile
        var isRunning = false
            private set

        fun start(context: Context) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, BleAdvertiserService::class.java),
            )
        }

        fun stop(context: Context) {
            // 直接调用原生的 stopService，优雅结束 Service 生命周期
            context.stopService(Intent(context, BleAdvertiserService::class.java))
        }
    }
}