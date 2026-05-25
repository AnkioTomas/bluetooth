package net.ankio.bluetooth.model

import androidx.annotation.StringRes
import net.ankio.bluetooth.R
import net.ankio.bluetooth.utils.PrefKeys
import net.ankio.bluetooth.utils.SpUtils

enum class WebdavMode {
    Sender2Webdav,
    SyncFromWebdav,
    None,
    ;

    @StringRes
    fun labelRes(): Int = when (this) {
        Sender2Webdav -> R.string.webdav_mode_sender2webdav
        SyncFromWebdav -> R.string.webdav_mode_sync
        None -> R.string.mode_none
    }

    companion object {
        fun load(): WebdavMode {
            val stored = SpUtils.getString(PrefKeys.WEBDAV_MODE, "")
            return entries.firstOrNull { it.name == stored } ?: None
        }
    }
}

enum class SimulateMode {
    SenderNearBy,
    Self,
    None,
    ;

    @StringRes
    fun labelRes(): Int = when (this) {
        SenderNearBy -> R.string.simulate_mode_nearby
        Self -> R.string.simulate_mode_self
        None -> R.string.mode_none
    }

    companion object {
        fun load(): SimulateMode {
            val stored = SpUtils.getString(PrefKeys.SIMULATE_MODE, "")
            return entries.firstOrNull { it.name == stored } ?: None
        }
    }
}

/** 将模式写入 SpUtils，并同步 Hook 仍读取的 legacy 布尔项 */
fun applyAppModes(webdavMode: WebdavMode, simulateMode: SimulateMode) {
    val webdavEnabled = webdavMode != WebdavMode.None
    val asSender = webdavMode == WebdavMode.Sender2Webdav || simulateMode == SimulateMode.SenderNearBy
    val mockEnable = simulateMode == SimulateMode.Self

    SpUtils.putString(PrefKeys.WEBDAV_MODE, webdavMode.name)
    SpUtils.putString(PrefKeys.SIMULATE_MODE, simulateMode.name)
    SpUtils.putBoolean(PrefKeys.PREF_ENABLE_WEBDAV, webdavEnabled)
    SpUtils.putBoolean(PrefKeys.PREF_AS_SENDER, asSender)
    SpUtils.putBoolean(PrefKeys.PREF_ENABLE, mockEnable)
}
