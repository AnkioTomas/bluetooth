package net.ankio.bluetooth.viewmodel

import android.app.Application
import android.content.Intent
import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.thegrizzlylabs.sardineandroid.impl.SardineException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.ankio.bluetooth.BuildConfig
import net.ankio.bluetooth.R
import net.ankio.bluetooth.bluetooth.BleDevice
import net.ankio.bluetooth.service.SendWebdavServer
import net.ankio.bluetooth.utils.HookUtils
import net.ankio.bluetooth.utils.SpUtils
import net.ankio.bluetooth.utils.WebdavUtils

enum class StatusKind {
    Error, Success, Warning,
}

data class StatusBannerState(
    val messageRes: Int,
    val kind: StatusKind,
    val icon: ImageVector,
)

data class MainUiState(
    val status: StatusBannerState = StatusBannerState(
        R.string.active_error,
        StatusKind.Error,
        Icons.Default.Error,
    ),
    val prefMac: String = "",
    val prefData: String = "",
    val prefRssi: String = "-50",
    val prefEnable: Boolean = false,
    val prefEnableWebdav: Boolean = false,
    val prefAsSender: Boolean = false,
    val webdavServer: String = "https://dav.jianguoyun.com/dav/",
    val webdavUsername: String = "",
    val webdavPassword: String = "",
    val prefCompany: String = "",
    val prefMac2: String = "",
    val webdavLast: String = "",
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val tag = "MainViewModel"
    private val context get() = getApplication<Application>()

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState = _uiState.asStateFlow()

    private val _messages = MutableSharedFlow<Int>()
    val messages = _messages.asSharedFlow()

    fun load() {
        _uiState.update {
            it.copy(
                prefMac = SpUtils.getString("pref_mac", ""),
                prefData = SpUtils.getString("pref_data", ""),
                prefRssi = SpUtils.getString("pref_rssi", "-50").ifEmpty { "-50" },
                prefEnable = SpUtils.getBoolean("pref_enable", false),
                prefEnableWebdav = SpUtils.getBoolean("pref_enable_webdav", false),
                prefAsSender = SpUtils.getBoolean("pref_as_sender", false),
                webdavServer = SpUtils.getString("webdav_server", "https://dav.jianguoyun.com/dav/"),
                webdavUsername = SpUtils.getString("webdav_username", ""),
                webdavPassword = SpUtils.getString("webdav_password", ""),
                prefCompany = SpUtils.getString("pref_company", ""),
                prefMac2 = SpUtils.getString("pref_mac2", ""),
                webdavLast = SpUtils.getString("webdav_last", context.getString(R.string.webdav_no_sync)),
            )
        }
        serverConnect()
    }

    fun updatePrefMac(value: String) {
        _uiState.update { it.copy(prefMac = value) }
        SpUtils.putString("pref_mac", value)
        saveToLocal()
    }

    fun updatePrefData(value: String) {
        _uiState.update { it.copy(prefData = value) }
        SpUtils.putString("pref_data", value)
        saveToLocal()
    }

    fun updatePrefRssi(value: String) {
        _uiState.update { it.copy(prefRssi = value) }
        SpUtils.putString("pref_rssi", value)
        saveToLocal()
    }

    fun updatePrefEnable(value: Boolean) {
        _uiState.update { it.copy(prefEnable = value) }
        SpUtils.putBoolean("pref_enable", value)
    }

    fun updatePrefEnableWebdav(value: Boolean) {
        _uiState.update { it.copy(prefEnableWebdav = value) }
        SpUtils.putBoolean("pref_enable_webdav", value)
        serverConnect()
    }

    fun updatePrefAsSender(value: Boolean) {
        _uiState.update {
            it.copy(
                prefAsSender = value,
                prefEnable = if (value) false else it.prefEnable,
            )
        }
        SpUtils.putBoolean("pref_as_sender", value)
        if (value) {
            SpUtils.putBoolean("pref_enable", false)
        }
        serverConnect()
    }

    fun updateWebdavServer(value: String) {
        _uiState.update { it.copy(webdavServer = value) }
        SpUtils.putString("webdav_server", value)
    }

    fun updateWebdavUsername(value: String) {
        _uiState.update { it.copy(webdavUsername = value) }
        SpUtils.putString("webdav_username", value)
    }

    fun updateWebdavPassword(value: String) {
        _uiState.update { it.copy(webdavPassword = value) }
        SpUtils.putString("webdav_password", value)
    }

    fun updatePrefCompany(value: String) {
        _uiState.update { it.copy(prefCompany = value) }
        SpUtils.putString("pref_company", value)
    }

    fun updatePrefMac2(value: String) {
        _uiState.update { it.copy(prefMac2 = value) }
        SpUtils.putString("pref_mac2", value)
    }

    private fun isSender(): Boolean {
        val state = _uiState.value
        return state.prefEnableWebdav && state.prefAsSender
    }

    private fun serverConnect() {
        if (isSender()) {
            startServer()
        } else {
            stopServer()
        }
        refreshStatus()
    }

    fun refresh() {
        refreshStatus()
    }

    fun pullFromWebdav() {
        if (isSender()) return
        val state = _uiState.value
        if (!state.prefEnableWebdav) {
            viewModelScope.launch { _messages.emit(R.string.webdav_not_configured) }
            return
        }
        syncFromServer()
    }

    private fun refreshStatus() {
        val status = if (isSender()) {
            Log.i(tag, "isServerRunning => ${SendWebdavServer.isRunning}")
            if (!SendWebdavServer.isRunning) {
                StatusBannerState(
                    R.string.server_error,
                    StatusKind.Error,
                    Icons.Default.Error,
                )
            } else {
                StatusBannerState(
                    R.string.server_working,
                    StatusKind.Success,
                    Icons.Default.CheckCircle,
                )
            }
        } else if (HookUtils.getActiveAndSupportFramework()) {
            if (HookUtils.getXposedVersion() < 93) {
                StatusBannerState(
                    R.string.active_version,
                    StatusKind.Warning,
                    Icons.Default.Error,
                )
            } else {
                SpUtils.putInt("app_version", BuildConfig.VERSION_CODE)
                StatusBannerState(
                    R.string.active_success,
                    StatusKind.Success,
                    Icons.Default.CheckCircle,
                )
            }
        } else {
            StatusBannerState(
                R.string.active_error,
                StatusKind.Error,
                Icons.Default.Error,
            )
        }
        _uiState.update { it.copy(status = status) }
    }

    private fun startServer() {
        if (!SendWebdavServer.isRunning) {
            SendWebdavServer.isRunning = true
            Log.i(tag, "bluetooth server start!")
            val intent = Intent(context, SendWebdavServer::class.java)
            ContextCompat.startForegroundService(context, intent)
            refreshStatus()
        }
    }

    private fun stopServer() {
        if (SendWebdavServer.isRunning) {
            Log.i(tag, "bluetooth server stop!")
            val intent = Intent(context, SendWebdavServer::class.java)
            context.stopService(intent)
            refreshStatus()
        }
    }

    private fun syncFromServer() {
        viewModelScope.launch {
            try {
                val state = _uiState.value
                val bluetoothData = WebdavUtils(state.webdavUsername, state.webdavPassword).getFromServer()
                if (bluetoothData != null) {
                    SpUtils.putString("pref_data", bluetoothData.data)
                    SpUtils.putString("pref_mac", bluetoothData.mac)
                    _uiState.update {
                        it.copy(
                            prefData = bluetoothData.data,
                            prefMac = bluetoothData.mac,
                            webdavLast = SpUtils.getString("webdav_last", context.getString(R.string.webdav_no_sync)),
                        )
                    }
                    _messages.emit(R.string.sync_pull_success)
                }
            } catch (e: SardineException) {
                e.message?.let { Log.e(tag, it) }
                _messages.emit(R.string.webdav_error)
            } catch (e: Exception) {
                e.message?.let { Log.e(tag, it) }
            }
        }
    }

    private fun saveToLocal() {
        val state = _uiState.value
        val historyJson = SpUtils.getString("history", "")
        var localHistoryList = Gson().fromJson<List<BleDevice>>(
            historyJson,
            object : TypeToken<List<BleDevice>>() {}.type,
        )?.toMutableList() ?: mutableListOf()

        val rssi = state.prefRssi.ifEmpty { "-50" }
        var insert = false
        val saveHistory = ArrayList<BleDevice>()
        localHistoryList.forEach { device ->
            if (device.address == state.prefMac) {
                saveHistory.add(
                    device.copy(
                        data = state.prefData,
                        rssi = rssi.toIntOrNull() ?: -50,
                    ),
                )
                insert = true
            } else {
                saveHistory.add(device)
            }
        }
        if (!insert) {
            saveHistory.add(
                BleDevice(
                    state.prefData,
                    context.getString(R.string.manual_increase),
                    rssi.toIntOrNull() ?: -50,
                    state.prefMac,
                    "",
                ),
            )
        }
        SpUtils.putString("history", Gson().toJson(saveHistory))
    }
}
