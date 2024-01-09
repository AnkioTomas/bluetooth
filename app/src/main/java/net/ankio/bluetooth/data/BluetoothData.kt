package net.ankio.bluetooth.data

import android.content.Context

class BluetoothData(context: Context) {
    private val bluetoothCompanyParser = BluetoothCompanyParser(context)
    fun parseManufacturerData(advertisingData: ByteArray): String? {
        var index = 0
        while (index < advertisingData.size - 2) {
            val length = advertisingData[index++].toInt() and 0xFF
            if (length == 0 || index + length > advertisingData.size) {
                // 长度为0或数据超出数组范围
                break
            }

            val type = advertisingData[index].toInt() and 0xFF
            if (type == 0xFF) { // 厂商特定数据类型
                // 假设厂商ID占据接下来的两个字节
                if (length >= 3) { // 确保有足够的数据
                    val companyId = ((advertisingData[index + 2].toInt() and 0xFF) shl 8) or
                            (advertisingData[index + 1].toInt() and 0xFF)
                    return bluetoothCompanyParser.getCompanyName(companyId)
                }
            }
            index += length
        }
        return "None"
    }

}