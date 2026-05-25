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
import net.ankio.bluetooth.model.SimulateMode
import net.ankio.bluetooth.model.WebdavMode
import net.ankio.bluetooth.model.applyAppModes
import net.ankio.bluetooth.service.SendWebdavServer
import net.ankio.bluetooth.utils.HookUtils
import net.ankio.bluetooth.utils.PrefKeys
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
    val webdavMode: WebdavMode = WebdavMode.None,
    val simulateMode: SimulateMode = SimulateMode.None,
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val tag = "MainViewModel"
    private val context get() = getApplication<Application>()

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState = _uiState.asStateFlow()

    private val _messages = MutableSharedFlow<Int>()
    val messages = _messages.asSharedFlow()

    init {
        load()
    }

    fun load() {
        val webdavMode = WebdavMode.load()
        val simulateMode = SimulateMode.load()
        _uiState.update {
            it.copy(
                webdavMode = webdavMode,
                simulateMode = simulateMode,
                prefMac = SpUtils.getString(PrefKeys.PREF_MAC, ""),
                prefData = SpUtils.getString(PrefKeys.PREF_DATA, ""),
                prefRssi = SpUtils.getString(PrefKeys.PREF_RSSI, "-50").ifEmpty { "-50" },
                prefEnableWebdav = webdavMode != WebdavMode.None,
                prefAsSender = webdavMode == WebdavMode.Sender2Webdav || simulateMode == SimulateMode.SenderNearBy,
                prefEnable = simulateMode == SimulateMode.Self,
                webdavServer = SpUtils.getString(PrefKeys.WEBDAV_SERVER, "https://dav.jianguoyun.com/dav/"),
                webdavUsername = SpUtils.getString(PrefKeys.WEBDAV_USERNAME, ""),
                webdavPassword = SpUtils.getString(PrefKeys.WEBDAV_PASSWORD, ""),
                prefCompany = SpUtils.getString(PrefKeys.PREF_COMPANY, ""),
                prefMac2 = SpUtils.getString(PrefKeys.PREF_MAC2, ""),
                webdavLast = SpUtils.getString(PrefKeys.WEBDAV_LAST, context.getString(R.string.webdav_no_sync)),
            )
        }
        applyAppModes(webdavMode, simulateMode)
        serverConnect()
    }

    fun updateWebdavMode(mode: WebdavMode) {
        val simulateMode = _uiState.value.simulateMode
        commitModes(mode, simulateMode)
    }

    fun updateSimulateMode(mode: SimulateMode) {
        val webdavMode = _uiState.value.webdavMode
        commitModes(webdavMode, mode)
    }

    private fun commitModes(webdavMode: WebdavMode, simulateMode: SimulateMode) {
        val webdavEnabled = webdavMode != WebdavMode.None
        val asSender = webdavMode == WebdavMode.Sender2Webdav || simulateMode == SimulateMode.SenderNearBy
        val mockEnable = simulateMode == SimulateMode.Self

        applyAppModes(webdavMode, simulateMode)
        _uiState.update {
            it.copy(
                webdavMode = webdavMode,
                simulateMode = simulateMode,
                prefEnableWebdav = webdavEnabled,
                prefAsSender = asSender,
                prefEnable = mockEnable,
            )
        }
        serverConnect()
    }

    fun updatePrefMac(value: String) {
        _uiState.update { it.copy(prefMac = value) }
        SpUtils.putString(PrefKeys.PREF_MAC, value)
        saveToLocal()
    }

    fun updatePrefData(value: String) {
        _uiState.update { it.copy(prefData = value) }
        SpUtils.putString(PrefKeys.PREF_DATA, value)
        saveToLocal()
    }

    fun updatePrefRssi(value: String) {
        _uiState.update { it.copy(prefRssi = value) }
        SpUtils.putString(PrefKeys.PREF_RSSI, value)
        saveToLocal()
    }

    fun updateWebdavServer(value: String) {
        _uiState.update { it.copy(webdavServer = value) }
        SpUtils.putString(PrefKeys.WEBDAV_SERVER, value)
    }

    fun updateWebdavUsername(value: String) {
        _uiState.update { it.copy(webdavUsername = value) }
        SpUtils.putString(PrefKeys.WEBDAV_USERNAME, value)
    }

    fun updateWebdavPassword(value: String) {
        _uiState.update { it.copy(webdavPassword = value) }
        SpUtils.putString(PrefKeys.WEBDAV_PASSWORD, value)
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
                SpUtils.putInt(PrefKeys.APP_VERSION, BuildConfig.VERSION_CODE)
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
                    SpUtils.putString(PrefKeys.PREF_DATA, bluetoothData.data)
                    SpUtils.putString(PrefKeys.PREF_MAC, bluetoothData.mac)
                    _uiState.update {
                        it.copy(
                            prefData = bluetoothData.data,
                            prefMac = bluetoothData.mac,
                            webdavLast = SpUtils.getString(PrefKeys.WEBDAV_LAST, context.getString(R.string.webdav_no_sync)),
                        )
                    }
                    saveToLocal()
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
        val historyJson = SpUtils.getString(PrefKeys.HISTORY, "")
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
        SpUtils.putString(PrefKeys.HISTORY, Gson().toJson(saveHistory))
    }
}
