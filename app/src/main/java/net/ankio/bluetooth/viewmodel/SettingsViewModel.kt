package net.ankio.bluetooth.viewmodel

import androidx.lifecycle.ViewModel
import net.ankio.bluetooth.App
import net.ankio.bluetooth.utils.LocaleDelegate
import net.ankio.bluetooth.utils.SpUtils
import net.ankio.bluetooth.utils.persistedState

/**
 * 设置页状态：负责语言配置的读取、持久化与运行时 Locale 切换。
 */
class SettingsViewModel : ViewModel() {
    /**
     * 当前语言标记（如 SYSTEM / zh-CN / en-US）。
     * 赋值后会立即落盘，并更新 [LocaleDelegate.defaultLocale]。
     */
    var languageTag: String by persistedState(
        initialValue = SpUtils.getString("setting_language", "SYSTEM").ifEmpty { "SYSTEM" },
        debounceMs = 0L,
    ) { value ->
        SpUtils.putString("setting_language", value)
        LocaleDelegate.defaultLocale = App.getLocale(value)
    }
}
