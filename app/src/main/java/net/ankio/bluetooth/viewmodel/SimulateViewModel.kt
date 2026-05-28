package net.ankio.bluetooth.viewmodel

import androidx.lifecycle.ViewModel
import net.ankio.bluetooth.utils.PrefKeys
import net.ankio.bluetooth.utils.persistedPrefState

class SimulateViewModel : ViewModel() {

    var prefMac: String by persistedPrefState(PrefKeys.PREF_MAC, "")

    var prefData: String by persistedPrefState(PrefKeys.PREF_DATA, "")

    var prefRssi: String by persistedPrefState(PrefKeys.PREF_RSSI, "-65")
}
