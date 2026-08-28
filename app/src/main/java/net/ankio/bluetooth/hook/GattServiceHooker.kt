package net.ankio.bluetooth.hook

import android.os.Handler
import android.os.Looper
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import net.ankio.bluetooth.model.SimulateMode
import net.ankio.bluetooth.utils.ByteUtils
import net.ankio.bluetooth.utils.HookLogManager
import net.ankio.bluetooth.utils.PrefKeys
import net.ankio.xposed.lib.hook.api.PartHooker
import net.ankio.xposed.lib.hook.hook.Hooker
import java.io.Serializable
import java.lang.reflect.Method

/**
 * 本机模拟：周期性向系统蓝牙的扫描回调注入伪造 BLE 扫描结果。
 * 仅在 [net.ankio.bluetooth.model.SimulateMode.Self] 下生效。
 *
 * 注入点随系统版本迁移，GattService 与 ScanController 两个入口都挂钩子，
 * 由 [resolveScanTarget] 判断哪个入口能走到真正持有 onScanResult 的对象：
 * - Android 15：GattService.start → getTransitionalScanHelper
 * - Android 16：GattService 构造完成 → getScanController
 * - Android 17：GattService 已无扫描逻辑，只有 ScanController 自身
 */
class GattServiceHooker : PartHooker() {

    override fun hook() {
        val mode = HookConfig.getString(PrefKeys.SIMULATE_MODE, "")
        if (mode != SimulateMode.Self.toString()) {
            HookLogManager.d(TAG, "Local BLE simulation disabled, mode=[$mode]")
            return
        }

        HookLogManager.d(TAG, "Local BLE simulation started")
        val gattClass = loadClassOrNull(GATT_SERVICE)
        val scanControllerClass = loadClassOrNull(SCAN_CONTROLLER)
        if (gattClass == null && scanControllerClass == null) {
            HookLogManager.e(
                TAG,
                "Unsupported device; export com.android.bluetooth and open a GitHub issue",
            )
            return
        }

        gattClass?.let { hookGattService(it) }
        scanControllerClass?.let { hookLifecycle(it, null, listOf("stop", "cleanup")) }
    }

    private fun hookGattService(gattClass: Class<*>) {
        when {
            hasMethod(gattClass, "start") -> hookLifecycle(gattClass, "start", listOf("stop"))
            hasMethod(gattClass, "initMiFeature") ->
                hookLifecycle(gattClass, "initMiFeature", listOf("cleanup"))

            else -> hookLifecycle(gattClass, null, listOf("cleanup"))
        }
    }

    /**
     * [start] 为 null 时挂全部构造器：Android 16+ 的 GattService 与 ScanController
     * 都是构造完成即就绪，且构造器签名每个版本都在变，不能写死参数类型。
     */
    private fun hookLifecycle(clazz: Class<*>, start: String?, stops: List<String>) {
        if (start == null) {
            XposedBridge.hookAllConstructors(
                clazz,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        attachBroadcaster(param.thisObject)
                    }
                },
            )
        } else {
            Hooker.after(clazz, start) { attachBroadcaster(it.thisObject) }
        }

        for (stop in stops) {
            if (hasMethod(clazz, stop)) {
                Hooker.before(clazz, stop) { detachBroadcaster(it.thisObject) }
            }
        }
    }

    private fun attachBroadcaster(entry: Any) {
        val (target, method) = resolveScanTarget(entry) ?: return
        // 同一个 target 可能被两个入口先后发现，只保留一个定时器。
        if (XposedHelpers.getAdditionalInstanceField(target, RUNNABLE_KEY) != null) return

        val handler = scanThreadHandler(target)
            ?: scanThreadHandler(entry)
            ?: Handler(Looper.getMainLooper())
        val broadcast = ScanBroadcaster(target, method, handler)
        XposedHelpers.setAdditionalInstanceField(target, RUNNABLE_KEY, broadcast)
        XposedHelpers.setAdditionalInstanceField(entry, TARGET_KEY, target)
        handler.postDelayed(broadcast, INTERVAL_MS)
    }

    private fun detachBroadcaster(entry: Any) {
        val target = XposedHelpers.getAdditionalInstanceField(entry, TARGET_KEY) ?: return
        val broadcast = XposedHelpers.getAdditionalInstanceField(target, RUNNABLE_KEY)
            as? ScanBroadcaster ?: return
        broadcast.handler.removeCallbacks(broadcast)
        XposedHelpers.setAdditionalInstanceField(target, RUNNABLE_KEY, null)
    }

    private class ScanBroadcaster(
        private val target: Any,
        private val method: Method,
        val handler: Handler,
    ) : Runnable {

        override fun run() {
            val mac = HookConfig.getString(PrefKeys.PREF_MAC, DEFAULT_MAC)
            val rssi = HookConfig.getString(PrefKeys.PREF_RSSI, DEFAULT_RSSI).toInt()
            val advData = ByteUtils.hexStringToBytes(
                HookConfig.getString(PrefKeys.PREF_DATA, DEFAULT_ADV_DATA),
            )
            try {
                invokeScanResult(target, method, mac, rssi, advData)
            } catch (e: Throwable) {
                HookLogManager.e(TAG, "Mock scan injection failed: ${e.message}", e)
            }
            handler.postDelayed(this, INTERVAL_MS)
        }
    }

    companion object {
        const val TAG = "BluetoothDebug"

        private const val GATT_SERVICE = "com.android.bluetooth.gatt.GattService"
        private const val SCAN_CONTROLLER = "com.android.bluetooth.le_scan.ScanController"
        private const val RUNNABLE_KEY = "runnable"
        private const val TARGET_KEY = "scanTarget"
        private const val INTERVAL_MS = 500L
        private const val DEFAULT_MAC = "76:A7:8A:67:66:C9"
        private const val DEFAULT_RSSI = "-50"
        private const val DEFAULT_ADV_DATA =
            "02010403033CFE17FF0001B500024271A7B6000000C983926CB1011000000000000000000000000000000000000000000000000000000000000000000000"

        /**
         * declaredMethods 只有本类声明的方法，GattService.start 继承自 ProfileService，
         * 只看本类会把 Android 15 误判成"设备不支持"。
         */
        private fun allMethods(clazz: Class<*>): List<Method> {
            val result = mutableListOf<Method>()
            var current: Class<*>? = clazz
            while (current != null && current != Any::class.java) {
                result += current.declaredMethods
                current = current.superclass
            }
            return result
        }

        private fun hasMethod(clazz: Class<*>, name: String): Boolean =
            allMethods(clazz).any { it.name == name }

        /** 从入口对象出发，找到真正声明了 onScanResult 的那一层。 */
        private fun resolveScanTarget(entry: Any): Pair<Any, Method>? {
            val candidates = listOf("getScanController", "getTransitionalScanHelper")
            val targets = buildList {
                for (name in candidates) {
                    if (!hasMethod(entry.javaClass, name)) continue
                    val value = try {
                        XposedHelpers.callMethod(entry, name)
                    } catch (_: Throwable) {
                        continue
                    }
                    if (value != null) add(name to value)
                }
                add(entry.javaClass.simpleName to entry)
            }

            for ((name, target) in targets) {
                val method = findScanMethod(target.javaClass) ?: continue
                if (method.parameterTypes.size < 10) continue
                HookLogManager.d(TAG, "Resolved scan target: $name.${method.name}")
                return target to method
            }
            HookLogManager.d(TAG, "No scan target under ${entry.javaClass.name}")
            return null
        }

        /**
         * onScanResult 带 enforceScanThread()，从别的线程调必抛 IllegalStateException；
         * onScanResultInternal 没有该断言，是唯一可用的注入点。
         */
        private fun findScanMethod(clazz: Class<*>): Method? {
            val methods = allMethods(clazz)
            val internal = methods.filter { it.name == "onScanResultInternal" }
            if (internal.isNotEmpty()) return internal.maxByOrNull { it.parameterTypes.size }
            return methods.filter { it.name == "onScanResult" }.maxByOrNull { it.parameterTypes.size }
        }

        /** Android 17 的 ScanController 把扫描逻辑限制在自己的 mHandler 线程上。 */
        private fun scanThreadHandler(target: Any): Handler? = try {
            XposedHelpers.getObjectField(target, "mHandler") as? Handler
        } catch (_: NoSuchFieldError) {
            null
        }

        private fun loadClassOrNull(name: String): Class<*>? = try {
            Hooker.loader(name)
        } catch (_: Throwable) {
            null
        }

        private fun invokeScanResult(
            target: Any,
            method: Method,
            mac: String,
            rssi: Int,
            advData: ByteArray,
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
            val args = if (method.parameterTypes.size >= params.size) {
                params
            } else {
                params.copyOf(params.size - 1)
            }
            XposedHelpers.callMethod(target, method.name, *args)
        }
    }
}
