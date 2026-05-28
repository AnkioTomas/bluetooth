package net.ankio.bluetooth.ble

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import net.ankio.bluetooth.data.BluetoothData
import net.ankio.bluetooth.utils.ByteUtils
import net.ankio.bluetooth.utils.SpUtils

/**
 * BLE 扫描器：负责启动/停止扫描，并按本地筛选配置过滤结果。
 *
 * 权限：调用方（如 [net.ankio.bluetooth.ui.MainActivity]）须在进扫描页前申请
 * 定位 +（Android 12+）BLUETOOTH_SCAN / BLUETOOTH_CONNECT。
 */
class BleScanner(context: Context) {
    private val appContext = context.applicationContext
    private val bluetoothData by lazy { BluetoothData(appContext) }
    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        appContext.getSystemService(BluetoothManager::class.java)?.adapter
    }

    private var scanCallback: ScanCallback? = null
    private var running = false

    /**
     * 启动扫描。
     *
     * @param onDeviceFound 命中过滤条件后的设备回调
     * @return true 表示扫描已成功启动；false 表示权限或蓝牙状态不满足
     */
    fun start(onDeviceFound: (BleDevice) -> Unit): Boolean {
        if (running) return true
        if (!hasScanPermissions()) return false

        val adapter = bluetoothAdapter ?: return false
        if (!adapter.isEnabled) return false

        val scanner = adapter.bluetoothLeScanner ?: return false
        scanCallback = object : ScanCallback() {
            @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                emitScanResult(result, onDeviceFound)
            }
        }

        return try {
            scanner.startScan(scanCallback)
            running = true
            true
        } catch (_: SecurityException) {
            running = false
            scanCallback = null
            false
        }
    }

    /**
     * 停止扫描。即使系统拒绝或状态异常，也会清理本地运行态。
     */
    fun stop() {
        if (!running) return
        if (!hasScanPermissions()) {
            clearRunningState()
            return
        }

        val adapter = bluetoothAdapter
        val callback = scanCallback
        if (adapter == null || callback == null) {
            clearRunningState()
            return
        }

        try {
            adapter.bluetoothLeScanner?.stopScan(callback)
        } catch (_: SecurityException) {
            // 权限被收回时仍须释放本地状态
        } finally {
            clearRunningState()
        }
    }

    fun isRunning(): Boolean = running

    /**
     * 停止扫描并关闭系统蓝牙。
     *
     * @return true 表示已发起关闭；false 表示权限不足或适配器不可用
     */
    fun disableBluetooth(): Boolean {
        stop()
        if (!hasConnectPermission()) return false
        val adapter = bluetoothAdapter ?: return false
        return try {
            adapter.disable()
        } catch (_: SecurityException) {
            false
        }
    }

    /**
     * 解析单条扫描结果并回调。须在已通过 [hasConnectPermission] 的路径内调用。
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun emitScanResult(result: ScanResult, onDeviceFound: (BleDevice) -> Unit) {
        if (!hasConnectPermission()) return

        val scanRecord = result.scanRecord?.bytes ?: return
        val companyName = bluetoothData.parseManufacturerData(scanRecord) ?: "None"
        val name = bluetoothData.parseLocalName(scanRecord) ?: result.device.name
        if (!matchesFilter(companyName, name, result.rssi)) return

        onDeviceFound(
            BleDevice(
                data = ByteUtils.bytesToHexString(scanRecord) ?: "",
                company = companyName.ifBlank { "None" },
                rssi = Rssi.normalizeDbm(result.rssi, result.rssi),
                address = result.device.address,
                name = if (name.isNullOrBlank()) "None" else name,
            ),
        )
    }

    /**
     * 按本地设置过滤扫描结果：
     * - 过滤空设备名
     * - 厂商关键字匹配
     * - 最低 RSSI 阈值
     */
    private fun matchesFilter(companyName: String, name: String?, rssi: Int): Boolean {
        val filterEmptyName = SpUtils.getBoolean(BleConstant.NULL_NAME)
        if (filterEmptyName && name.isNullOrBlank()) return false

        val company = SpUtils.getString(BleConstant.COMPANY, "")
        if (company.isNotEmpty() && !companyName.contains(company, ignoreCase = true)) return false

        val minRssi = Rssi.normalizeDbm(SpUtils.getInt(BleConstant.RSSI, Rssi.DEFAULT_FILTER_DBM))
        return rssi >= minRssi
    }

    private fun clearRunningState() {
        running = false
        scanCallback = null
    }

    /**
     * 启动/停止扫描所需权限（内联 [ContextCompat.checkSelfPermission]，供 Lint 识别）。
     */
    private fun hasScanPermissions(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(
                    appContext,
                    Manifest.permission.BLUETOOTH_SCAN,
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
            if (ContextCompat.checkSelfPermission(
                    appContext,
                    Manifest.permission.BLUETOOTH_CONNECT,
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
            return true
        }
        return ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * 读取 [android.bluetooth.BluetoothDevice] 名称/地址所需权限（内联检查）。
     */
    private fun hasConnectPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.BLUETOOTH_CONNECT,
            ) == PackageManager.PERMISSION_GRANTED
        }
        return ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
    }
}
