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
        // 1. UI 状态必须瞬间更新，绝不延迟，保证画面不卡顿
        state = value

        // 2. 取消上一次还没执行的写入任务（如果存在的话）
        debounceJob?.cancel()

        // 3. 开启一个新的写入任务
        debounceJob = scope.launch(Dispatchers.IO) {
            // 如果用户设置了防抖时间，就先等一会儿
            if (debounceMs > 0) {
                delay(debounceMs)
            }
            // 时间到了，用户没再输入，执行真正的保存操作
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