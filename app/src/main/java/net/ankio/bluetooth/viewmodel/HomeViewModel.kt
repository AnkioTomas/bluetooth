package net.ankio.bluetooth.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import net.ankio.bluetooth.BuildConfig
import net.ankio.bluetooth.R
import net.ankio.bluetooth.model.SimulateMode
import net.ankio.bluetooth.model.WebdavMode
import net.ankio.bluetooth.ui.compose.components.StatusKind
import net.ankio.bluetooth.service.BleAdvertiserService
import net.ankio.bluetooth.utils.HookUtils
import net.ankio.bluetooth.utils.PrefKeys
import net.ankio.bluetooth.utils.SpUtils
import net.ankio.bluetooth.utils.persistedState
import net.ankio.bluetooth.webdav.WebdavServiceManager

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    var webdavMode: WebdavMode by persistedState(
        initialValue = WebdavMode.current(),
    ) { value ->
        SpUtils.putString(PrefKeys.WEBDAV_MODE, value.name)
        WebdavServiceManager.applyMode(getApplication(), value)
    }

    var simulateMode: SimulateMode by persistedState(
        initialValue = SimulateMode.current(),
    ) { value ->
        SpUtils.putString(PrefKeys.SIMULATE_MODE, value.name)
        when (value) {
            SimulateMode.SenderNearBy ->
                BleAdvertiserService.start(getApplication(), showToast = true)
            else ->
                BleAdvertiserService.stop(getApplication())
        }
    }

    var pluginStatusMessage by mutableStateOf("")
        private set
    var pluginStatusKind by mutableStateOf(StatusKind.Success)
        private set

    init {
        refreshPluginStatus()
        viewModelScope.launch {
            WebdavServiceManager.applyMode(
                getApplication(),
                WebdavMode.current(),
                showToast = false,
            )
            if (SimulateMode.current() == SimulateMode.SenderNearBy) {
                BleAdvertiserService.start(getApplication(), showToast = false)
            }
        }
    }

    fun onHomeResume() {
        refreshPluginStatus()
    }

    fun refreshPluginStatus() {
        val ctx = getApplication<Application>()
        val (message, kind) = when {
            !HookUtils.getActiveAndSupportFramework() ->
                ctx.getString(R.string.active_error) to StatusKind.Error
            HookUtils.getXposedVersion() < HookUtils.MIN_XPOSED_VERSION ->
                ctx.getString(R.string.active_version) to StatusKind.Warning
            HookUtils.getAppVersion() != BuildConfig.VERSION_CODE ->
                ctx.getString(R.string.active_restart) to StatusKind.Warning
            else -> ctx.getString(R.string.active_success) to StatusKind.Success
        }
        pluginStatusMessage = message
        pluginStatusKind = kind
    }
}
