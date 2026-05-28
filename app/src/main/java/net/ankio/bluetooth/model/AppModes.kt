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

    fun save() {
        SpUtils.putString(PrefKeys.WEBDAV_MODE, name)
    }

    companion object {
        fun current(): WebdavMode = readPref(PrefKeys.WEBDAV_MODE, None)
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

    fun save() {
        SpUtils.putString(PrefKeys.SIMULATE_MODE, name)
    }

    companion object {
        fun current(): SimulateMode = readPref(PrefKeys.SIMULATE_MODE, None)
    }
}

private inline fun <reified T : Enum<T>> readPref(key: String, default: T): T =
    enumValues<T>().firstOrNull { it.name == SpUtils.getString(key, "") } ?: default
