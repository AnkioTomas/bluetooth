package net.ankio.bluetooth.hook

import net.ankio.bluetooth.BuildConfig
import net.ankio.bluetooth.utils.HookLogManager
import net.ankio.xposed.lib.hook.App
import net.ankio.xposed.lib.hook.api.HookerManifest
import net.ankio.xposed.lib.log.Logger

class BluetoothXposedEntry : App() {

    override val hookers: List<HookerManifest> = listOf(
        BluetoothHooker(),
        SelfHooker(),
    )

    override val debug: Boolean = BuildConfig.DEBUG

    override val showLoadSuccessToast: Boolean = false

    override val loadSuccessMessage: String = ""

    override val hostApplicationId: String = BuildConfig.APPLICATION_ID

    override fun setUpLogger() {
        Logger.install(object : Logger {
            override fun d(msg: String) = HookLogManager.d(msg)

            override fun i(msg: String) = HookLogManager.d(msg)

            override fun w(msg: String) = HookLogManager.e(msg)

            override fun e(msg: String, tr: Throwable?) {
                if (tr != null) {
                    HookLogManager.e(msg, tr)
                } else {
                    HookLogManager.e(msg)
                }
            }
        })
    }
}
