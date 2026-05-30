package net.ankio.bluetooth.hook

import net.ankio.bluetooth.hook.GattServiceHooker
import net.ankio.xposed.lib.dex.model.Clazz
import net.ankio.xposed.lib.hook.api.HookerManifest
import net.ankio.xposed.lib.hook.api.PartHooker

class BluetoothHooker : HookerManifest() {

    override val packageName: String = "com.android.bluetooth"

    override val appName: String = "蓝牙服务"

    override val systemApp: Boolean = true

    override fun hookLoadPackage() {
        HookConfig.reload()
    }

    override var partHookers: MutableList<PartHooker> = mutableListOf(
        GattServiceHooker(),
    )

    override var rules: MutableList<Clazz> = mutableListOf()
}
