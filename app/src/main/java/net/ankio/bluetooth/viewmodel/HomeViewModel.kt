package net.ankio.bluetooth.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import net.ankio.bluetooth.model.SimulateMode
import net.ankio.bluetooth.model.WebdavMode

class HomeViewModel(
    initialWebdavMode: WebdavMode = WebdavMode.current(),
    initialSimulateMode: SimulateMode = SimulateMode.current(),
) : ViewModel() {

    var webdavMode by mutableStateOf(initialWebdavMode)
        private set

    var simulateMode by mutableStateOf(initialSimulateMode)
        private set

    fun selectWebdavMode(index: Int) {
        val mode = WebdavMode.entries.getOrElse(index.coerceIn(WebdavMode.entries.indices)) {
            WebdavMode.None
        }
        // 内部可以直接赋值修改
        webdavMode = mode
        mode.save()
    }

    fun selectSimulateMode(index: Int) {
        val mode = SimulateMode.entries.getOrElse(index.coerceIn(SimulateMode.entries.indices)) {
            SimulateMode.None
        }
        simulateMode = mode
        mode.save()
    }
}