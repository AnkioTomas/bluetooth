package net.ankio.bluetooth.utils

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// 1. 核心委托类
class PersistedStateDelegate<T>(
    initialValue: T,
    private val scope: CoroutineScope,
    private val debounceMs: Long, // 新增：防抖时间参数
    private val onSave: suspend (T) -> Unit
) {
    // 真正的 UI 状态
    private var state by mutableStateOf(initialValue)

    // 核心黑魔法：用于记录当前的保存任务
    private var debounceJob: Job? = null

    operator fun getValue(thisRef: Any?, property: Any?): T = state

    operator fun setValue(thisRef: Any?, property: Any?, value: T) {
        update(value, persist = true)
    }

    /** 外部写入 pref 后刷新 UI，不再回写 pref。 */
    fun reload(value: T) {
        update(value, persist = false)
    }

    private fun update(value: T, persist: Boolean) {
        state = value
        debounceJob?.cancel()
        if (!persist) return
        debounceJob = scope.launch(Dispatchers.IO) {
            if (debounceMs > 0) {
                delay(debounceMs)
            }
            onSave(value)
        }
    }
}

// 2. 拓展函数（提供默认参数 debounceMs = 0L，默认不防抖）
fun <T> ViewModel.persistedState(
    initialValue: T,
    debounceMs: Long = 0L, // 允许用户自主选择是否防抖，默认 0 毫秒
    onSave: suspend (T) -> Unit
) = PersistedStateDelegate(initialValue, viewModelScope, debounceMs, onSave)

// 3. Pref 专用封装：字符串状态
fun ViewModel.persistedPrefState(
    key: String,
    defaultValue: String = "",
    debounceMs: Long = 0L,
) = persistedState(
    initialValue = SpUtils.getString(key, defaultValue),
    debounceMs = debounceMs,
) { value ->
    SpUtils.putString(key, value)
}