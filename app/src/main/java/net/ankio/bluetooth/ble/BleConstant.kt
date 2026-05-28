package net.ankio.bluetooth.ble

/**
 *
 * @description BleConstant
 * @author llw
 * @date 2021/9/13 14:12
 */
class BleConstant {

    companion object BleConstant {
        /**
         * 公司信息
         */
        const val COMPANY = "company"

        /**
         * 是否过滤设备名称为Null的设备
         */
        const val NULL_NAME = "nullName"

        /**
         * 扫描过滤最低 RSSI（dBm，负数，如 -70）
         */
        const val RSSI = "rssi"
    }

}