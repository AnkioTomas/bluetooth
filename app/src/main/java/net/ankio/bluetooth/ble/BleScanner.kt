package net.ankio.bluetooth.ble

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import net.ankio.bluetooth.data.BluetoothData
import net.ankio.bluetooth.utils.ByteUtils
import kotlin.coroutines.resume

/**
 * BLE 扫描器：负责启动/停止扫描，并按 [BleScanFilter] 过滤结果。
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
     * 启动扫描，使用扫描页保存的过滤条件。
     *
     * @param onDeviceFound 命中过滤条件后的设备回调
     * @return true 表示扫描已成功启动；false 表示权限或蓝牙状态不满足
     */
    fun start(onDeviceFound: (BleDevice) -> Unit): Boolean =
        start(BleScanFilter.fromScanPrefs(), onDeviceFound)

    /**
     * 启动扫描。
     *
     * @param filter 本次扫描使用的过滤条件
     * @param onDeviceFound 命中过滤条件后的设备回调
     * @return true 表示扫描已成功启动；false 表示权限或蓝牙状态不满足
     */
    fun start(filter: BleScanFilter, onDeviceFound: (BleDevice) -> Unit): Boolean {
        if (running) return true
        if (!hasScanPermissions()) return false

        val adapter = bluetoothAdapter ?: return false
        if (!adapter.isEnabled) return false

        val scanner = adapter.bluetoothLeScanner ?: return false
        scanCallback = object : ScanCallback() {
            @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                toBleDevice(result, filter)?.let(onDeviceFound)
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
     * 扫描直到命中 [filter] 或超时，命中后立即停止扫描。
     */
    suspend fun scanOnce(filter: BleScanFilter, timeoutMs: Long): BleDevice? {
        if (!hasScanPermissions()) return null
        val adapter = bluetoothAdapter ?: return null
        if (!adapter.isEnabled) return null
        val scanner = adapter.bluetoothLeScanner ?: return null

        return withTimeoutOrNull(timeoutMs) {
            suspendCancellableCoroutine { cont ->
                val callback = object : ScanCallback() {
                    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
                    override fun onScanResult(callbackType: Int, result: ScanResult) {
                        val device = toBleDevice(result, filter) ?: return
                        if (!cont.isActive) return
                        stopScan(scanner, this)
                        cont.resume(device)
                    }
                }
                cont.invokeOnCancellation { stopScan(scanner, callback) }
                try {
                    scanner.startScan(callback)
                } catch (e: SecurityException) {
                    Log.w(TAG, "startScan denied", e)
                    if (cont.isActive) cont.resume(null)
                }
            }
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

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun toBleDevice(result: ScanResult, filter: BleScanFilter): BleDevice? {
        if (!hasConnectPermission()) return null

        val scanRecord = result.scanRecord?.bytes ?: return null
        val companyName = bluetoothData.parseManufacturerData(scanRecord) ?: "None"
        val name = bluetoothData.parseLocalName(scanRecord) ?: result.device.name
        if (!filter.matches(companyName, name, result.rssi, result.device.address)) return null

        return BleDevice(
            data = ByteUtils.bytesToHexString(scanRecord) ?: "",
            company = companyName.ifBlank { "None" },
            rssi = Rssi.normalizeDbm(result.rssi, result.rssi),
            address = result.device.address,
            name = if (name.isNullOrBlank()) "None" else name,
        )
    }

    private fun stopScan(scanner: BluetoothLeScanner, callback: ScanCallback) {
        try {
            scanner.stopScan(callback)
        } catch (e: Exception) {
            Log.w(TAG, "stopScan failed", e)
        }
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

    companion object {
        private const val TAG = "BleScanner"
    }
}
