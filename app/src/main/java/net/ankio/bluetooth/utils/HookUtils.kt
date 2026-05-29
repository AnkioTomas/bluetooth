package net.ankio.bluetooth.utils

import net.ankio.bluetooth.BuildConfig

object HookUtils {

    const val MIN_XPOSED_VERSION = 93

    /** 模块是否已在 Xposed 环境中激活（未 Hook 时恒为 false） */
    fun getActiveAndSupportFramework(): Boolean = false

    /** 蓝牙侧已加载的模块版本（未 Hook 时返回当前构建号） */
    fun getAppVersion(): Int = BuildConfig.VERSION_CODE

    /** Xposed 框架 API 版本（未 Hook 时返回占位值） */
    fun getXposedVersion(): Int = 82
}
