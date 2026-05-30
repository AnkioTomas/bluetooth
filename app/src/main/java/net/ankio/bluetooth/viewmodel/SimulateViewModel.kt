package net.ankio.bluetooth.viewmodel

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import net.ankio.bluetooth.utils.PersistedStateDelegate
import net.ankio.bluetooth.utils.PrefKeys
import net.ankio.bluetooth.utils.SpUtils
import net.ankio.bluetooth.utils.persistedPrefState

class SimulateViewModel : ViewModel() {

    private val macState = prefState(PrefKeys.PREF_MAC, "")
    private val dataState = prefState(PrefKeys.PREF_DATA, "")
    private val rssiState = prefState(PrefKeys.PREF_RSSI, "-65")

    var prefMac: String by macState
    var prefData: String by dataState
    var prefRssi: String by rssiState

    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        when (key) {
            PrefKeys.PREF_MAC -> macState.reload(SpUtils.getString(PrefKeys.PREF_MAC, ""))
            PrefKeys.PREF_DATA -> dataState.reload(SpUtils.getString(PrefKeys.PREF_DATA, ""))
            PrefKeys.PREF_RSSI -> rssiState.reload(SpUtils.getString(PrefKeys.PREF_RSSI, "-65"))
        }
    }

    init {
        SpUtils.registerChangeListener(prefListener)
    }

    override fun onCleared() {
        SpUtils.unregisterChangeListener(prefListener)
        super.onCleared()
    }

    private fun prefState(key: String, default: String): PersistedStateDelegate<String> =
        persistedPrefState(key, default)
}
