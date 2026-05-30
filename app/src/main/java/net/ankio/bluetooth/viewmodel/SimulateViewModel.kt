package net.ankio.bluetooth.viewmodel

import androidx.lifecycle.ViewModel
import net.ankio.bluetooth.App
import net.ankio.bluetooth.model.SimulateMode
import net.ankio.bluetooth.service.BleAdvertiserService
import net.ankio.bluetooth.utils.PersistedStateDelegate
import net.ankio.bluetooth.utils.PrefKeys
import net.ankio.bluetooth.utils.SpUtils
import net.ankio.bluetooth.utils.persistedState

class SimulateViewModel : ViewModel() {

    private val macState = prefState(PrefKeys.PREF_MAC, "")
    private val dataState = prefState(PrefKeys.PREF_DATA, "")
    private val rssiState = prefState(PrefKeys.PREF_RSSI, "-65")

    var prefMac: String by macState
    var prefData: String by dataState
    var prefRssi: String by rssiState

    fun reloadFromPrefs() {
        macState.reload(SpUtils.getString(PrefKeys.PREF_MAC, ""))
        dataState.reload(SpUtils.getString(PrefKeys.PREF_DATA, ""))
        rssiState.reload(SpUtils.getString(PrefKeys.PREF_RSSI, "-65"))
    }

    private fun prefState(key: String, default: String): PersistedStateDelegate<String> =
        persistedState(
            initialValue = SpUtils.getString(key, default),
            debounceMs = 500,
        ) { value ->
            SpUtils.putString(key, value)
            if (SimulateMode.current() == SimulateMode.SenderNearBy) {
                BleAdvertiserService.start(App.context)
            }
        }
}
