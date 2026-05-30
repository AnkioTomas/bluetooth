package net.ankio.bluetooth.hook

import android.os.Handler
import android.os.Looper
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import net.ankio.bluetooth.model.SimulateMode
import net.ankio.bluetooth.utils.ByteUtils
import net.ankio.bluetooth.utils.HookLogManager
import net.ankio.bluetooth.utils.PrefKeys
import net.ankio.xposed.lib.hook.api.PartHooker
import net.ankio.xposed.lib.hook.hook.Hooker
import java.io.Serializable

/**
 * 本机模拟：Hook 系统 GattService，周期性注入伪造 BLE 扫描结果。
 * 仅在 [net.ankio.bluetooth.model.SimulateMode.Self] 下生效。
 */
class GattServiceHooker : PartHooker() {

    override fun hook() {
        if (HookConfig.getString(PrefKeys.SIMULATE_MODE, "") != SimulateMode.Self.toString()) {
            HookLogManager.d(TAG, "关闭蓝牙模拟功能")
            return
        }

        HookLogManager.d(TAG, "蓝牙模拟启动")
        val gattClass = Hooker.loader(GATT_SERVICE)

        when {
            hasMethod(gattClass, "start") -> hookStartStop(gattClass, "start", "stop")
            hasMethod(gattClass, "initMiFeature") -> hookStartStop(gattClass, "initMiFeature", "cleanup")
            else -> hookConstructorStop(gattClass)
        }
    }

    private fun hookStartStop(gattClass: Class<*>, start: String, stop: String) {
        Hooker.after(gattClass, start) { attachBroadcaster(it.thisObject) }
        Hooker.before(gattClass, stop) { detachBroadcaster(it.thisObject) }
    }

    /** Android 16+：GattService 在构造完成后就绪，停止仍走 cleanup。 */
    private fun hookConstructorStop(gattClass: Class<*>) {
        val adapterClass = Hooker.loader(ADAPTER_SERVICE)
        XposedHelpers.findAndHookConstructor(
            gattClass,
            adapterClass,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    attachBroadcaster(param.thisObject)
                }
            },
        )
        Hooker.before(gattClass, "cleanup") { detachBroadcaster(it.thisObject) }
    }

    private fun attachBroadcaster(gattService: Any) {
        var handler = XposedHelpers.getAdditionalInstanceField(gattService, HANDLER_KEY) as Handler?
        if (handler == null) {
            handler = Handler(Looper.getMainLooper())
        }
        XposedHelpers.setAdditionalInstanceField(gattService, HANDLER_KEY, handler)

        val broadcast = ScanBroadcaster(gattService, handler)
        XposedHelpers.setAdditionalInstanceField(gattService, RUNNABLE_KEY, broadcast)
        handler.postDelayed(broadcast, INTERVAL_MS)
    }

    private fun detachBroadcaster(gattService: Any) {
        val handler = XposedHelpers.getAdditionalInstanceField(gattService, HANDLER_KEY) as? Handler
            ?: return
        val runnable = XposedHelpers.getAdditionalInstanceField(gattService, RUNNABLE_KEY) as? Runnable ?: return
        handler.removeCallbacks(runnable)
    }

    private class ScanBroadcaster(
        private val gattService: Any,
        private val handler: Handler,
    ) : Runnable {

        /** 启动时解析一次，避免每 500ms 重复探测反射路径。 */
        private val scanPath = resolveScanPath(gattService)

        override fun run() {
            scanPath?.let { (getter, trailingMac) ->
                val mac = HookConfig.getString(PrefKeys.PREF_MAC, DEFAULT_MAC)
                val rssi = HookConfig.getString(PrefKeys.PREF_RSSI, DEFAULT_RSSI).toInt()
                val advData = ByteUtils.hexStringToBytes(
                    HookConfig.getString(PrefKeys.PREF_DATA, DEFAULT_ADV_DATA),
                )
                try {
                    invokeScanResult(getter(gattService), mac, rssi, advData, trailingMac)
                } catch (e: Throwable) {
                    HookLogManager.e(TAG, "mock 失败：${e.message}", e)
                }
            }
            handler.postDelayed(this, INTERVAL_MS)
        }
    }

    companion object {
        const val TAG = "AnkioのBluetooth"

        private const val GATT_SERVICE = "com.android.bluetooth.gatt.GattService"
        private const val ADAPTER_SERVICE = "com.android.bluetooth.btservice.AdapterService"
        private const val HANDLER_KEY = "handler"
        private const val RUNNABLE_KEY = "runnable"
        private const val INTERVAL_MS = 500L
        private const val DEFAULT_MAC = "76:A7:8A:67:66:C9"
        private const val DEFAULT_RSSI = "-50"
        private const val DEFAULT_ADV_DATA =
            "02010403033CFE17FF0001B500024271A7B6000000C983926CB1011000000000000000000000000000000000000000000000000000000000000000000000"

        private fun hasMethod(clazz: Class<*>, name: String): Boolean =
            clazz.declaredMethods.any { it.name == name }

        private fun resolveScanPath(gattService: Any): Pair<(Any) -> Any, Boolean>? {
            val gattClass = gattService.javaClass
            val candidates = buildList {
                if (hasMethod(gattClass, "getScanController")) {
                    add("getScanController" to { s: Any -> XposedHelpers.callMethod(s, "getScanController") })
                }
                if (hasMethod(gattClass, "getTransitionalScanHelper")) {
                    add(
                        "getTransitionalScanHelper" to { s: Any ->
                            XposedHelpers.callMethod(s, "getTransitionalScanHelper")
                        },
                    )
                }
                add("GattService" to { s: Any -> s })
            }

            for ((name, getter) in candidates) {
                val target = try {
                    getter(gattService)
                } catch (_: Throwable) {
                    continue
                }
                val trailingMac = when (onScanResultParamCount(target)) {
                    11 -> true
                    10 -> false
                    else -> continue
                }
                HookLogManager.d(TAG, "解析方法: $name")
                return getter to trailingMac
            }

            HookLogManager.e(TAG, "您的设备不支持，请提取com.android.bluetooth文件提交至github")
            return null
        }

        private fun onScanResultParamCount(target: Any): Int? =
            target.javaClass.declaredMethods
                .firstOrNull { it.name == "onScanResult" }
                ?.parameterTypes
                ?.size

        private fun invokeScanResult(
            target: Any,
            mac: String,
            rssi: Int,
            advData: ByteArray,
            trailingMac: Boolean,
        ) {
            val params: Array<Serializable> = arrayOf(
                0x1b,
                0x00,
                mac,
                0x01,
                0x00,
                0xff,
                0x7f,
                rssi,
                0x00,
                advData,
                mac,
            )
            val args = if (trailingMac) params else params.copyOf(params.size - 1)
            XposedHelpers.callMethod(target, "onScanResult", *args)
        }
    }
}