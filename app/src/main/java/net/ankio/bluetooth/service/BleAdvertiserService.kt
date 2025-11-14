package net.ankio.bluetooth.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.os.ParcelUuid
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID
import net.ankio.bluetooth.R
import net.ankio.bluetooth.ui.MainActivity
import net.ankio.bluetooth.utils.BleAdvertiserManager
import net.ankio.bluetooth.utils.ByteUtils
import net.ankio.bluetooth.utils.SpUtils

/**
 * BLE外围设备广播服务
 */
class BleAdvertiserService : Service() {

    companion object {
        private const val TAG = "BleAdvertiserService"
        private const val NOTIFICATION_ID = 2001
        private const val CHANNEL_ID = "ble_advertiser_channel"
        private const val CHANNEL_NAME = "BLE广播服务"

        // 动作常量
        const val ACTION_START_ADVERTISING = "net.ankio.bluetooth.START_ADVERTISING"
        const val ACTION_STOP_ADVERTISING = "net.ankio.bluetooth.STOP_ADVERTISING"
        const val ACTION_UPDATE_CONFIG = "net.ankio.bluetooth.UPDATE_CONFIG"
    }

    /**
     * 广播事件类型
     */
    enum class AdvertisingEvent {
        STARTED,
        STOPPED,
        FAILED,
        BLUETOOTH_DISABLED,
        DEVICE_NOT_SUPPORTED,
        PERMISSION_DENIED,
        EXCEPTION
    }

    private var bluetoothManager: BluetoothManager? = null
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothLeAdvertiser: BluetoothLeAdvertiser? = null
    private var advertiseCallback: AdvertiseCallback? = null
    private var isAdvertising = false
    private var advertisingJob: Job? = null

    private val serviceScope = CoroutineScope(Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        initBluetooth()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_ADVERTISING -> startAdvertising()
            ACTION_STOP_ADVERTISING -> stopAdvertising()
            ACTION_UPDATE_CONFIG -> updateAdvertisingConfig()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        stopAdvertising()
    }

    /**
     * 初始化蓝牙适配器
     */
    private fun initBluetooth() {
        bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager?.adapter
        bluetoothLeAdvertiser = bluetoothAdapter?.bluetoothLeAdvertiser
    }

    /**
     * 创建通知渠道
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "BLE外围设备广播服务通知"
                setShowBadge(false)
                setSound(null, null)
                enableVibration(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * 创建前台服务通知
     */
    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("BLE广播服务")
            .setContentText("正在广播模拟蓝牙设备数据")
            .setSmallIcon(R.drawable.ic_bluetooth)
            .setContentIntent(pendingIntent)
            .setOngoing(false) // 不设为常驻通知
            .setSilent(true)
            .setAutoCancel(true) // 点击后自动取消
            .build()
    }

    /**
     * 开始BLE广播
     */
    @SuppressLint("MissingPermission")
    private fun startAdvertising() {
        if (isAdvertising) {
            Log.w(TAG, "广播已在运行中")
            return
        }

        if (bluetoothAdapter?.isEnabled != true) {
            Log.e(TAG, "蓝牙未开启")
            notifyManager(AdvertisingEvent.BLUETOOTH_DISABLED)
            return
        }

        if (bluetoothLeAdvertiser == null) {
            Log.e(TAG, "设备不支持BLE广播")
            notifyManager(AdvertisingEvent.DEVICE_NOT_SUPPORTED)
            return
        }

        if (!hasRequiredPermissions()) {
            Log.e(TAG, "缺少必要权限")
            notifyManager(AdvertisingEvent.PERMISSION_DENIED)
            return
        }

        try {
            Log.i(TAG, "启动BLE广播...")
            startForeground(NOTIFICATION_ID, createNotification())

            // 获取配置数据
            val broadcastDataHex = SpUtils.getString("pref_data", BleAdvertiserManager.BroadcastConfig.DEFAULT_DATA)
            val dataBytes = ByteUtils.hexStringToBytes(broadcastDataHex)

            if (dataBytes.isEmpty()) {
                Log.e(TAG, "广播数据格式错误")
                notifyManager(AdvertisingEvent.FAILED, "广播数据格式错误")
                return
            }

            val settings = createAdvertiseSettings()
            val advertiseData = createAdvertiseData(dataBytes)
            val scanResponseData = createScanResponseData()

            Log.d(TAG, "广播数据: $broadcastDataHex")

            advertiseCallback = object : AdvertiseCallback() {
                override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
                    Log.i(TAG, "BLE广播启动成功")
                    isAdvertising = true
                    // 设置服务状态为启用
                    BleAdvertiserManager.ServiceState.setEnabled(this@BleAdvertiserService, true)
                    updateNotificationContent("BLE广播运行中")
                    startPeriodicAdvertising()
                    notifyManager(AdvertisingEvent.STARTED)
                }

                override fun onStartFailure(errorCode: Int) {
                    val errorMsg = getAdvertiseErrorMessage(errorCode)
                    Log.e(TAG, "BLE广播启动失败: $errorMsg")
                    // 设置服务状态为禁用
                    BleAdvertiserManager.ServiceState.setEnabled(this@BleAdvertiserService, false)
                    cleanupAdvertising()
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                    notifyManager(AdvertisingEvent.FAILED, errorMsg)
                }
            }

            bluetoothLeAdvertiser?.startAdvertising(settings, advertiseData, scanResponseData, advertiseCallback)

        } catch (e: Exception) {
            Log.e(TAG, "启动广播异常", e)
            cleanupAdvertising()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            notifyManager(AdvertisingEvent.EXCEPTION, e.message)
        }
    }

    /**
     * 创建广播设置
     */
    private fun createAdvertiseSettings(): AdvertiseSettings {
        return AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
            .setConnectable(true)
            .setTimeout(0)
            .build()
    }

    
    /**
     * 停止BLE广播
     */
    @SuppressLint("MissingPermission")
    private fun stopAdvertising() {
        try {
            Log.i(TAG, "停止BLE广播...")

            advertiseCallback?.let { callback ->
                if (hasPermission(Manifest.permission.BLUETOOTH_ADVERTISE)) {
                    bluetoothLeAdvertiser?.stopAdvertising(callback)
                    Log.d(TAG, "已发送停止广播指令")
                } else {
                    Log.w(TAG, "缺少BLUETOOTH_ADVERTISE权限")
                }
            }

            cleanupAdvertising()
            // 设置服务状态为禁用
            BleAdvertiserManager.ServiceState.setEnabled(this, false)
            updateNotificationContent("BLE广播已停止")
            notifyManager(AdvertisingEvent.STOPPED)

        } catch (e: Exception) {
            Log.e(TAG, "停止广播异常", e)
        } finally {
            // 取消通知
            cancelNotification()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    /**
     * 通知Manager事件
     */
    private fun notifyManager(event: AdvertisingEvent, message: String? = null) {
        // 发送广播通知Manager
        val intent = Intent("net.ankio.bluetooth.ADVERTISING_EVENT").apply {
            putExtra("event", event.name)
            message?.let { putExtra("message", it) }
        }
        sendBroadcast(intent)
    }

    /**
     * 取消通知
     */
    private fun cancelNotification() {
        try {
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.cancel(NOTIFICATION_ID)
            Log.d(TAG, "已取消BLE广播通知")
        } catch (e: Exception) {
            Log.e(TAG, "取消通知失败", e)
        }
    }

    /**
     * 更新通知内容
     */
    private fun updateNotificationContent(content: String) {
        try {
            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("BLE广播服务")
                .setContentText(content)
                .setSmallIcon(R.drawable.ic_bluetooth)
                .setOngoing(false) // 不设为常驻通知
                .setSilent(true)
                .setAutoCancel(true) // 点击后自动取消
                .build()

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.notify(NOTIFICATION_ID, notification)

        } catch (e: Exception) {
            Log.e(TAG, "更新通知失败", e)
        }
    }

    
    /**
     * 更新广播配置
     */
    private fun updateAdvertisingConfig() {
        if (isAdvertising) {
            stopAdvertising()
            advertisingJob = serviceScope.launch {
                delay(1000)
                startAdvertising()
            }
        }
    }

    /**
     * 创建广播数据包
     */
    private fun createAdvertiseData(data: ByteArray): AdvertiseData {
        val builder = AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            .setIncludeTxPowerLevel(false)

        // 解析广播数据包中的各个字段
        parseAdvertisingData(builder, data)

        return builder.build()
    }

    /**
     * 解析广播数据包
     */
    private fun parseAdvertisingData(builder: AdvertiseData.Builder, data: ByteArray) {
        var index = 0

        while (index < data.size) {
            val length = (data[index++].toInt() and 0xFF)
            if (length == 0 || index + length > data.size) break

            val type = (data[index].toInt() and 0xFF)
            val fieldData = if (index + 1 < data.size && length > 1) {
                data.copyOfRange(index + 1, index + length)
            } else {
                byteArrayOf()
            }

            when (type) {
                0x01 -> {
                    // 标志位
                    Log.d(TAG, "标志位: ${fieldData.joinToString("") { String.format("%02X", it) }}")
                }
                0x03 -> {
                    // 16位服务UUID完整列表
                    if (fieldData.size >= 2 && fieldData.size % 2 == 0) {
                        val serviceUuids = mutableListOf<ParcelUuid>()
                        for (i in fieldData.indices step 2) {
                            val uuidBytes = fieldData.sliceArray(i until i + 2)
                            val uuid = UUID.fromString(String.format("%04X-0000-1000-8000-00805F9B34FB",
                                (uuidBytes[1].toInt() and 0xFF shl 8) or (uuidBytes[0].toInt() and 0xFF)))
                            serviceUuids.add(ParcelUuid(uuid))
                            Log.d(TAG, "添加16位服务UUID: 0x${String.format("%04X",
                                (uuidBytes[1].toInt() and 0xFF shl 8) or (uuidBytes[0].toInt() and 0xFF))}")
                        }
                        // 使用正确的方法添加服务UUID
                        for (serviceUuid in serviceUuids) {
                            builder.addServiceUuid(serviceUuid)
                        }
                    }
                }
                0xFF -> {
                    // 厂商特定数据
                    if (fieldData.size >= 2) {
                        val companyIdLow = (fieldData[0].toInt() and 0xFF)
                        val companyIdHigh = (fieldData[1].toInt() and 0xFF)
                        val companyId = (companyIdHigh shl 8) or companyIdLow

                        val manufacturerData = if (fieldData.size > 2) {
                            fieldData.sliceArray(2 until fieldData.size)
                        } else {
                            byteArrayOf()
                        }

                        builder.addManufacturerData(companyId, manufacturerData)
                        Log.d(TAG, "添加厂商数据: ID=0x${String.format("%04X", companyId)}, 数据长度=${manufacturerData.size}")
                        Log.d(TAG, "厂商数据内容: ${manufacturerData.joinToString("") { String.format("%02X", it) }}")
                    }
                }
                else -> {
                    Log.d(TAG, "未知数据类型: 0x${String.format("%02X", type)}, 长度: ${fieldData.size}")
                }
            }
            index += length
        }
    }

    /**
     * 创建扫描响应数据
     */
    private fun createScanResponseData(): AdvertiseData {
        return AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            .setIncludeTxPowerLevel(false)
            .build()
    }

    /**
     * 开始周期性广播
     */
    private fun startPeriodicAdvertising() {
        advertisingJob = serviceScope.launch {
            while (isActive && isAdvertising) {
                try {
                    delay(1000)
                } catch (e: Exception) {
                    Log.e(TAG, "周期性广播更新异常：${e.message}")
                }
            }
        }
    }

    /**
     * 检查权限
     */
    private fun hasPermission(permission: String): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * 检查是否有必要权限
     */
    private fun hasRequiredPermissions(): Boolean {
        val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN
            )
        }

        return requiredPermissions.all { hasPermission(it) }
    }

    /**
     * 清理广播状态
     */
    private fun cleanupAdvertising() {
        advertiseCallback = null
        isAdvertising = false
        advertisingJob?.cancel()
        advertisingJob = null
    }

    /**
     * 获取BLE广播错误消息
     */
    private fun getAdvertiseErrorMessage(errorCode: Int): String {
        return when (errorCode) {
            AdvertiseCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED -> "设备不支持BLE广播"
            AdvertiseCallback.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS -> "广播实例数量已达上限"
            AdvertiseCallback.ADVERTISE_FAILED_ALREADY_STARTED -> "广播已经在运行"
            AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE -> "广播数据过大"
            AdvertiseCallback.ADVERTISE_FAILED_INTERNAL_ERROR -> "内部错误"
            else -> "未知错误代码: $errorCode"
        }
    }
}