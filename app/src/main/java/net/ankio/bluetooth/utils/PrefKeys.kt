package net.ankio.bluetooth.utils

/** SharedPreferences `config` 键名，读写统一走 [SpUtils] */
object PrefKeys {
    const val PREF_MAC = "pref_mac"
    const val PREF_DATA = "pref_data"
    const val PREF_RSSI = "pref_rssi"
    const val PREF_ENABLE = "pref_enable"
    const val PREF_ENABLE_WEBDAV = "pref_enable_webdav"
    const val PREF_AS_SENDER = "pref_as_sender"
    const val PREF_COMPANY = "pref_company"
    const val PREF_MAC2 = "pref_mac2"

    const val WEBDAV_MODE = "webdav_mode"
    const val SIMULATE_MODE = "simulate_mode"
    const val WEBDAV_SERVER = "webdav_server"
    const val WEBDAV_USERNAME = "webdav_username"
    const val WEBDAV_PASSWORD = "webdav_password"
    const val WEBDAV_LAST = "webdav_last"

    const val HISTORY = "history"
    const val APP_VERSION = "app_version"
}
