package net.ankio.bluetooth.utils

import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.XposedBridge

/**
 * Xposed Hook日志管理工具类
 * 提供可控制的日志输出功能，支持通过设置开关控制日志输出
 *
 * @author SmallBaby
 * @date 2025-11-13
 */
object HookLogManager {

    private const val PREF_NAME = "net.ankio.bluetooth"
    private const val CONFIG_NAME = "config"
    private const val LOG_ENABLED_KEY = "pref_hook_log_enabled"

    private val preferences: XSharedPreferences by lazy {
        val prefs = XSharedPreferences(PREF_NAME, CONFIG_NAME)
        // 尝试设置可读写权限
        try {
            prefs.makeWorldReadable()
        } catch (e: Exception) {
            // 忽略权限设置失败
        }
        prefs
    }

    /**
     * 默认的日志标签
     */
    private const val DEFAULT_TAG = "AnkioのBluetooth"

    
    /**
     * 检查是否启用日志输出
     * @return true 如果启用日志输出，false 如果禁用
     */
    private fun isLogEnabled(): Boolean {
        return try {
            // 每次都强制重新加载配置，避免缓存问题
            preferences.reload()
            preferences.getBoolean(LOG_ENABLED_KEY, false) // 默认关闭日志
        } catch (e: Exception) {
            // 如果读取配置失败，默认关闭日志避免影响性能
            false
        }
    }

    /**
     * 输出调试日志
     * @param message 日志消息
     */
    fun d(message: String) {
        log(DEFAULT_TAG, message)
    }

    /**
     * 输出带标签的调试日志
     * @param tag 日志标签
     * @param message 日志消息
     */
    fun d(tag: String, message: String) {
        log(tag, message)
    }

    /**
     * 输出错误日志
     * @param message 日志消息
     */
    fun e(message: String) {
        log(DEFAULT_TAG, message, isError = true)
    }

    /**
     * 输出带标签的错误日志
     * @param tag 日志标签
     * @param message 日志消息
     */
    fun e(tag: String, message: String) {
        log(tag, message, isError = true)
    }

    /**
     * 输出异常日志
     * @param message 日志消息
     * @param throwable 异常对象
     */
    fun e(message: String, throwable: Throwable) {
        log(DEFAULT_TAG, "$message: ${throwable.message}", isError = true)
    }

    /**
     * 输出带标签的异常日志
     * @param tag 日志标签
     * @param message 日志消息
     * @param throwable 异常对象
     */
    fun e(tag: String, message: String, throwable: Throwable) {
        log(tag, "$message: ${throwable.message}", isError = true)
    }

    /**
     * 核心日志输出方法
     * @param tag 日志标签
     * @param message 日志消息
     * @param isError 是否为错误日志
     */
    private fun log(tag: String, message: String, isError: Boolean = false) {
        // 只有启用日志时才输出
        if (isLogEnabled()) {
            try {
                val formattedMessage = if (isError) {
                    "[ERROR] $tag : $message"
                } else {
                    "[DEBUG] $tag : $message"
                }
                XposedBridge.log(formattedMessage)
            } catch (e: Exception) {
                // 日志输出失败时不影响主功能
            }
        }
    }

    /**
     * 检查日志开关状态（用于设置界面显示）
     * @return 当前日志开关状态
     */
    fun getLogStatus(): Boolean {
        return isLogEnabled()
    }

    /**
     * 强制重新加载配置
     * 用于配置更新后刷新状态
     */
    fun reloadConfig() {
        try {
            preferences.reload()
        } catch (e: Exception) {
            // 忽略重载失败
        }
    }
}