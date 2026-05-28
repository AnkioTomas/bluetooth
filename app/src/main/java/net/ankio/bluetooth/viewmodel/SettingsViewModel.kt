package net.ankio.bluetooth.viewmodel

import androidx.lifecycle.ViewModel
import net.ankio.bluetooth.App
import net.ankio.bluetooth.utils.LocaleDelegate
import net.ankio.bluetooth.utils.SpUtils
import net.ankio.bluetooth.utils.persistedState

class SettingsViewModel : ViewModel() {
    var languageTag: String by persistedState(
        initialValue = SpUtils.getString("setting_language", "SYSTEM").ifEmpty { "SYSTEM" },
        debounceMs = 0L,
    ) { value ->
        SpUtils.putString("setting_language", value)
        LocaleDelegate.defaultLocale = App.getLocale(value)
    }
}
