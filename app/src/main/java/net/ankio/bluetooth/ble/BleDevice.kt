package net.ankio.bluetooth.ble

/**
 *
 * @description BleDevice
 * @author llw
 * @date 2021/9/10 11:29
 */
data class BleDevice(
    var data: String,
    var company: String?,
    /** 信号强度（dBm），通常为 [-100, 0] 的负数 */
    var rssi: Int,
    var address: String,
    var name: String?
)