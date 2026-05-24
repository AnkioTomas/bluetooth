package net.ankio.bluetooth.viewmodel

import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.ankio.bluetooth.bluetooth.BleDevice
import net.ankio.bluetooth.data.BluetoothData
import net.ankio.bluetooth.utils.BleConstant.BleConstant
import net.ankio.bluetooth.utils.ByteUtils
import net.ankio.bluetooth.utils.SpUtils

data class ScanUiState(
    val devices: List<BleDevice> = emptyList(),
    val isScanning: Boolean = false,
    val showFilterDialog: Boolean = false,
)

class ScanViewModel(application: Application) : AndroidViewModel(application) {

    private val tag = "ScanViewModel"
    private val context get() = getApplication<Application>()

    private val _uiState = MutableStateFlow(ScanUiState())
    val uiState = _uiState.asStateFlow()

    private val _messages = MutableSharedFlow<String>()
    val messages = _messages.asSharedFlow()

    private var defaultAdapter: BluetoothAdapter? = null
    private val addressList = mutableSetOf<String>()
    private var historyList = mutableListOf<BleDevice>()
    private val handler = Handler(Looper.getMainLooper())
    private var stopScanRunnable: Runnable? = null

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val scanRecord = result.scanRecord?.bytes ?: return
            viewModelScope.launch {
                val companyName = BluetoothData(context).parseManufacturerData(scanRecord)
                val name = result.device.name
                addDevice(
                    BleDevice(
                        ByteUtils.bytesToHexString(scanRecord) ?: "",
                        if (TextUtils.isEmpty(companyName)) "None" else companyName,
                        result.rssi,
                        result.device.address,
                        if (TextUtils.isEmpty(name)) "None" else name,
                    ),
                )
            }
        }
    }

    fun initialize(): Boolean {
        return try {
            defaultAdapter = (context.getSystemService(Application.BLUETOOTH_SERVICE) as BluetoothManager).adapter
            val historyJson = SpUtils.getString("history", "")
            historyList = Gson().fromJson<List<BleDevice>>(
                historyJson,
                object : TypeToken<List<BleDevice>>() {}.type,
            )?.toMutableList() ?: mutableListOf()
            true
        } catch (e: NullPointerException) {
            e.message?.let { Log.e(tag, it) }
            false
        }
    }

    fun toggleScan() {
        if (_uiState.value.isScanning) stopScan() else startScan()
    }

    fun showHistory() {
        stopScan()
        addressList.clear()
        val devices = historyList.toList()
        _uiState.update { it.copy(devices = devices, isScanning = false) }
        addressList.addAll(devices.map { it.address })
    }

    fun showFilterDialog(show: Boolean) {
        _uiState.update { it.copy(showFilterDialog = show) }
    }

    fun onFilterDismiss(wasScanning: Boolean) {
        _uiState.update { it.copy(showFilterDialog = false) }
        if (wasScanning) startScan()
    }

    fun selectDevice(device: BleDevice) {
        SpUtils.putString("pref_mac", device.address)
        SpUtils.putString("pref_data", device.data)
        SpUtils.putString("pref_rssi", device.rssi.toString())
        if (!historyList.any { it.address == device.address }) {
            historyList.add(device)
            SpUtils.putString("history", Gson().toJson(historyList))
        }
        stopScan()
    }

    fun removeDeviceAt(index: Int) {
        val current = _uiState.value.devices.toMutableList()
        if (index !in current.indices) return
        val removed = current.removeAt(index)
        historyList.removeAll { it.address == removed.address }
        SpUtils.putString("history", Gson().toJson(historyList))
        addressList.remove(removed.address)
        _uiState.update { it.copy(devices = current) }
    }

    fun stopScan() {
        stopScanRunnable?.let { handler.removeCallbacks(it) }
        val adapter = defaultAdapter ?: return
        if (!adapter.isEnabled) return
        if (!_uiState.value.isScanning) return
        try {
            adapter.bluetoothLeScanner.stopScan(scanCallback)
        } catch (_: SecurityException) {
            viewModelScope.launch { _messages.emit(context.getString(net.ankio.bluetooth.R.string.no_permission)) }
            return
        }
        addressList.clear()
        _uiState.update { it.copy(isScanning = false, devices = emptyList()) }
    }

    private fun startScan() {
        val adapter = defaultAdapter ?: return
        if (!adapter.isEnabled) {
            viewModelScope.launch { _messages.emit(context.getString(net.ankio.bluetooth.R.string.not_open_bluetooth)) }
            return
        }
        if (_uiState.value.isScanning) return

        addressList.clear()
        _uiState.update { it.copy(isScanning = true, devices = emptyList()) }

        try {
            adapter.bluetoothLeScanner.startScan(scanCallback)
        } catch (_: SecurityException) {
            viewModelScope.launch { _messages.emit(context.getString(net.ankio.bluetooth.R.string.no_permission)) }
            _uiState.update { it.copy(isScanning = false) }
            return
        }

        stopScanRunnable = Runnable { stopScan() }
        handler.postDelayed(stopScanRunnable!!, 5 * 60 * 1000L)
    }

    private fun addDevice(bleDevice: BleDevice) {
        val company = SpUtils.getString(BleConstant.COMPANY, "")
        if (!TextUtils.isEmpty(company) && bleDevice.company?.contains(company) == false) {
            return
        }
        val rssiFilter = -SpUtils.getInt(BleConstant.RSSI, 100)
        if (bleDevice.rssi < rssiFilter) return

        if (addressList.add(bleDevice.address)) {
            _uiState.update { it.copy(devices = it.devices + bleDevice) }
        }
    }

    override fun onCleared() {
        stopScan()
        super.onCleared()
    }
}
