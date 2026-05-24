package net.ankio.bluetooth.hook

import de.robv.android.xposed.XSharedPreferences
import net.ankio.bluetooth.utils.HookLogManager

object HookConfig {

    private const val PREF_PACKAGE = "net.ankio.bluetooth"
    private const val PREF_NAME = "config"

    private val pref = XSharedPreferences(PREF_PACKAGE, PREF_NAME)

    fun reload() {
        if (pref.hasFileChanged()) {
            pref.reload()
            HookLogManager.reloadConfig()
        }
    }

    fun getString(key: String, def: String): String {
        reload()
        return pref.getString(key, def) ?: def
    }

    fun getBoolean(key: String, def: Boolean): Boolean {
        reload()
        return pref.getBoolean(key, def)
    }
}
