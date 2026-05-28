package net.ankio.bluetooth.viewmodel

import androidx.lifecycle.ViewModel
import net.ankio.bluetooth.model.SimulateMode
import net.ankio.bluetooth.model.WebdavMode
import net.ankio.bluetooth.utils.PrefKeys
import net.ankio.bluetooth.utils.SpUtils
import net.ankio.bluetooth.utils.persistedState

class HomeViewModel : ViewModel() {

    var webdavMode: WebdavMode by persistedState(
        initialValue = WebdavMode.current(),
        debounceMs = 0L,
    ) { value ->
        SpUtils.putString(PrefKeys.WEBDAV_MODE, value.name)
        // TODO 这里需要额外的操作（启动webdav服务）
    }

    var simulateMode: SimulateMode by persistedState(
        initialValue = SimulateMode.current(),
    ) { value ->
        SpUtils.putString(PrefKeys.SIMULATE_MODE, value.name)
        // TODO 这里需要额外的操作（启动外围模拟）
    }

}