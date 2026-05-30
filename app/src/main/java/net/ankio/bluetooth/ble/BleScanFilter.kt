package net.ankio.bluetooth.ble

import net.ankio.bluetooth.utils.SpUtils

/** BLE 扫描过滤条件；扫描页与 WebDAV 推送共用。 */
data class BleScanFilter(
    val targetMac: String = "",
    val companyKeyword: String = "",
    val filterEmptyName: Boolean = false,
    val minRssiDbm: Int? = null,
) {
    fun matches(companyName: String, name: String?, rssi: Int, address: String): Boolean {
        if (targetMac.isNotEmpty() && !address.equals(targetMac, ignoreCase = true)) return false
        if (filterEmptyName && name.isNullOrBlank()) return false
        if (companyKeyword.isNotEmpty() && !companyName.contains(companyKeyword, ignoreCase = true)) {
            return false
        }
        if (minRssiDbm != null && rssi < minRssiDbm) return false
        return true
    }

    companion object {
        fun fromScanPrefs(): BleScanFilter = BleScanFilter(
            targetMac = SpUtils.getString(BleConstant.FILTER_MAC, "").trim(),
            companyKeyword = SpUtils.getString(BleConstant.COMPANY, ""),
            filterEmptyName = SpUtils.getBoolean(BleConstant.NULL_NAME),
            minRssiDbm = Rssi.normalizeDbm(SpUtils.getInt(BleConstant.RSSI, Rssi.DEFAULT_FILTER_DBM)),
        )
    }
}
