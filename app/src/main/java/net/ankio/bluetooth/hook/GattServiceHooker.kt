package net.ankio.bluetooth.hook

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import net.ankio.bluetooth.model.SimulateMode
import net.ankio.bluetooth.utils.ByteUtils
import net.ankio.bluetooth.utils.PrefKeys
import net.ankio.bluetooth.utils.HookLogManager
import net.ankio.xposed.lib.hook.api.PartHooker
import net.ankio.xposed.lib.hook.hook.Hooker
import java.io.Serializable

class GattServiceHooker : PartHooker() {

    private val tag = "AnkioのBluetooth"
    private val gattServiceClass = "com.android.bluetooth.gatt.GattService"

    override fun hook() {
        HookConfig.reload()
        if (HookConfig.getString(PrefKeys.SIMULATE_MODE, "") !== SimulateMode.Self.toString()) {
            HookLogManager.d(tag, "关闭蓝牙模拟功能")
            return
        }

        HookLogManager.d(tag, "蓝牙模拟启动")
        val clazz = Hooker.loader(gattServiceClass)
        if (hasMethod(clazz, "start")) {
            hookLifecycleMethods(clazz, "start", "stop")
        } else {
            hookLifecycleMethods(clazz, "initMiFeature", "cleanup")
        }
    }

    private fun hasMethod(clazz: Class<*>, methodName: String): Boolean {
        return clazz.declaredMethods.any { it.name == methodName }
    }

    private fun hookLifecycleMethods(clazz: Class<*>, startMethodName: String, stopMethodName: String) {
        Hooker.after(clazz, startMethodName) { param ->
            var handler = XposedHelpers.getAdditionalInstanceField(param.thisObject, HANDLER_KEY) as Handler?
            if (handler == null) {
                handler = Handler(Looper.getMainLooper())
            }
            XposedHelpers.setAdditionalInstanceField(param.thisObject, HANDLER_KEY, handler)

            val broadcast = BroadcastBluetooth(param, handler)
            XposedHelpers.setAdditionalInstanceField(param.thisObject, RUNNABLE_KEY, broadcast)
            handler.postDelayed(broadcast, BROADCAST_INTERVAL_MS)
        }

        Hooker.before(clazz, stopMethodName) { param ->
            val handler = XposedHelpers.getAdditionalInstanceField(param.thisObject, HANDLER_KEY)
            if (handler !is Handler) {
                return@before
            }
            val runnable = XposedHelpers.getAdditionalInstanceField(param.thisObject, RUNNABLE_KEY)
            if (runnable !is Runnable) {
                return@before
            }
            handler.removeCallbacks(runnable)
        }
    }

    private class BroadcastBluetooth(
        private val param: XC_MethodHook.MethodHookParam,
        private val handler: Handler,
    ) : Runnable {

        private val tag = "AnkioのBluetooth"

        @SuppressLint("SuspiciousIndentation")
        override fun run() {
            val mac = HookConfig.getString(PrefKeys.PREF_MAC, "76:A7:8A:67:66:C9")
            val params = arrayOf<Serializable>(
                0x1b,
                0x00,
                mac,
                0x01,
                0x00,
                0xff,
                0x7f,
                HookConfig.getString(PrefKeys.PREF_RSSI, "-50").toInt(),
                0x00,
                ByteUtils.hexStringToBytes(
                    HookConfig.getString(
                        PrefKeys.PREF_DATA,
                        "02010403033CFE17FF0001B500024271A7B6000000C983926CB1011000000000000000000000000000000000000000000000000000000000000000000000",
                    ),
                ),
                mac,
            )

            try {
                HookLogManager.d(tag, "解析方法: getScanController")
                val scanHelper = XposedHelpers.callMethod(param.thisObject, "getScanController")
                callMethodOnScanResult(scanHelper, params)
            } catch (e: NoSuchMethodError) {
                try {
                    HookLogManager.d(tag, "解析方法: getTransitionalScanHelper")
                    val scanHelper = XposedHelpers.callMethod(param.thisObject, "getTransitionalScanHelper")
                    callMethodOnScanResult(scanHelper, params)
                } catch (e: NoSuchMethodError) {
                    HookLogManager.d(tag, "解析方法: getTransitionalScanHelper 失败, 回退解析 GattService")
                    callMethodOnScanResult(param.thisObject, params)
                }
            }

            HookLogManager.d(tag, "mock => $mac")
            handler.postDelayed(this, BROADCAST_INTERVAL_MS)
        }

        private fun callMethodOnScanResult(thisObject: Any, params: Array<Serializable>) {
            try {
                XposedHelpers.callMethod(thisObject, "onScanResult", *params)
            } catch (e: NoSuchMethodError) {
                try {
                    XposedHelpers.callMethod(
                        thisObject,
                        "onScanResult",
                        *params.copyOf(params.size - 1),
                    )
                } catch (e: NoSuchMethodError) {
                    HookLogManager.e(tag, "您的设备不支持，请提取com.android.bluetooth文件提交至github")
                    HookLogManager.e(tag, "异常：${e.message}", e)
                }
            }
        }
    }

    private companion object {
        const val HANDLER_KEY = "handler"
        const val RUNNABLE_KEY = "runnable"
        const val BROADCAST_INTERVAL_MS = 500L
    }
}
