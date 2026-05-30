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
    private var showToastOnError = false

    override fun onCreate() {
        super.onCreate()
        val manager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothLeAdvertiser = manager.adapter?.bluetoothLeAdvertiser
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                showToastOnError = intent.getBooleanExtra(EXTRA_SHOW_TOAST, false)
                stopBleAdvertise()
                startBleAdvertise()
            }
            ACTION_STOP -> stopAndQuit()
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopBleAdvertise()
        isRunning = false
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

        enterForeground()

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
        if (showToastOnError) {
            ThemeToast.show(message, ThemeToast.Style.Error)
        }
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
        var index = 0
        while (index < data.size) {
            val length = data[index++].toInt() and 0xFF
            if (length == 0 || index + length > data.size) break
            val type = data[index].toInt() and 0xFF
            val field = if (length > 1) data.copyOfRange(index + 1, index + length) else byteArrayOf()
            when (type) {
                0x03 -> {
                    if (field.size >= 2 && field.size % 2 == 0) {
                        for (i in field.indices step 2) {
                            val id = ((field[i + 1].toInt() and 0xFF) shl 8) or (field[i].toInt() and 0xFF)
                            builder.addServiceUuid(
                                ParcelUuid(UUID.fromString(String.format("%04X-0000-1000-8000-00805F9B34FB", id))),
                            )
                        }
                    }
                }
                0xFF -> if (field.size >= 2) {
                    val companyId = ((field[1].toInt() and 0xFF) shl 8) or (field[0].toInt() and 0xFF)
                    val payload = if (field.size > 2) field.copyOfRange(2, field.size) else byteArrayOf()
                    builder.addManufacturerData(companyId, payload)
                }
            }
            index += length
        }
        return builder.build()
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
        private const val EXTRA_SHOW_TOAST = "show_toast"

        const val ACTION_START = "net.ankio.bluetooth.ADVERTISE_START"
        const val ACTION_STOP = "net.ankio.bluetooth.ADVERTISE_STOP"

        @Volatile
        var isRunning = false
            private set

        fun start(context: Context, showToast: Boolean = false) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, BleAdvertiserService::class.java).apply {
                    action = ACTION_START
                    putExtra(EXTRA_SHOW_TOAST, showToast)
                },
            )
        }

        fun stop(context: Context) {
            context.startService(
                Intent(context, BleAdvertiserService::class.java).apply {
                    action = ACTION_STOP
                },
            )
        }
    }
}
