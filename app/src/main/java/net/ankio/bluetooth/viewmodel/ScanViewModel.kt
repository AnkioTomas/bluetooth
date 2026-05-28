package net.ankio.bluetooth.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import net.ankio.bluetooth.ble.BleScanner
import net.ankio.bluetooth.ble.BleDevice
import net.ankio.bluetooth.utils.PrefKeys
import net.ankio.bluetooth.utils.SpUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 扫描页状态与交互编排：
 * - 管理扫描开关和设备列表
 * - 管理筛选面板显示
 * - 处理选中设备后的配置落盘与页面跳转信号
 */
class ScanViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        private const val SCAN_TIMEOUT_MS = 60_000L
    }

    private val scanner = BleScanner(application)
    private var scanTimeoutJob: Job? = null

    var devices by mutableStateOf(emptyList<BleDevice>())

    var isScanning by mutableStateOf(false)

    var showFilterDialog by mutableStateOf(false)

    var openSimulate by mutableStateOf(false)

    /**
     * 扫描页主按钮：未扫描时开始扫描，扫描中关闭蓝牙。
     */
    fun onScanFabClick() {
        if (isScanning) {
            closeBluetooth()
        } else {
            startScanning()
        }
    }

    /**
     * 开始扫描，并清空当前列表重新收集结果。
     */
    fun startScanning() {
        devices = emptyList()
        val started = scanner.start { device ->
            if (devices.any { it.address == device.address }) return@start
            devices = devices + device
        }
        if (started) {
            isScanning = true
            scheduleScanTimeout()
        }
    }

    /**
     * 停止扫描并关闭系统蓝牙。
     */
    fun closeBluetooth() {
        stopScanning()
        scanner.disableBluetooth()
    }

    /**
     * 打开筛选面板前先停止扫描，避免边扫边改过滤条件造成状态抖动。
     */
    fun openFilterDialog() {
        stopScanning()
        showFilterDialog = true
    }

    fun dismissFilterDialog() {
        showFilterDialog = false
    }

    /**
     * 选中设备后保存模拟参数，并发送跳转到模拟页的信号。
     */
    fun selectDevice(device: BleDevice) {
        SpUtils.putString(PrefKeys.PREF_MAC, device.address)
        SpUtils.putString(PrefKeys.PREF_DATA, device.data)
        SpUtils.putString(PrefKeys.PREF_RSSI, device.rssi.toString())
        stopScanning()
        openSimulate = true
    }

    /**
     * 删除单条扫描结果，仅影响当前展示列表。
     */
    fun removeDeviceAt(index: Int) {
        if (index !in devices.indices) return
        devices = devices.toMutableList().also { it.removeAt(index) }
    }

    /**
     * 清理一次性导航信号，防止重复触发。
     */
    fun clearOpenSimulate() {
        openSimulate = false
    }

    private fun stopScanning() {
        scanTimeoutJob?.cancel()
        scanTimeoutJob = null
        scanner.stop()
        isScanning = false
    }

    private fun scheduleScanTimeout() {
        scanTimeoutJob?.cancel()
        scanTimeoutJob = viewModelScope.launch {
            delay(SCAN_TIMEOUT_MS)
            if (isScanning) {
                stopScanning()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        scanner.stop()
    }
}
