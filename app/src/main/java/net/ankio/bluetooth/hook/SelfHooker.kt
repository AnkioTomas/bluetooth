package net.ankio.bluetooth.hook

import de.robv.android.xposed.XposedBridge
import net.ankio.bluetooth.BuildConfig
import net.ankio.xposed.lib.dex.model.Clazz
import net.ankio.xposed.lib.hook.api.HookerManifest
import net.ankio.xposed.lib.hook.api.PartHooker
import net.ankio.xposed.lib.hook.hook.Hooker

class SelfHooker : HookerManifest() {

    override val packageName: String = BuildConfig.APPLICATION_ID

    override val appName: String = "Bluetooth"

    override fun hookLoadPackage() {
        Hooker.replaceReturn(
            "net.ankio.bluetooth.utils.HookUtils",
            "getActiveAndSupportFramework",
            true,
        )
        Hooker.after("net.ankio.bluetooth.utils.HookUtils", "getAppVersion") { param ->
            param.result = BuildConfig.VERSION_CODE
        }
        Hooker.after("net.ankio.bluetooth.utils.HookUtils", "getXposedVersion") { param ->
            param.result = XposedBridge.getXposedVersion()
        }
    }

    override var partHookers: MutableList<PartHooker> = mutableListOf()

    override var rules: MutableList<Clazz> = mutableListOf()
}
