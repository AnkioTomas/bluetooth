package net.ankio.bluetooth.ble

/**
 * RSSI（dBm）统一约定：业务层一律使用 [-100, 0] 的负数值。
 *
 * 历史版本曾把筛选阈值存为 0..100 正数，读取时会自动迁移为负数。
 */
object Rssi {
    const val MIN_DBM = -100
    const val MAX_DBM = 0
    const val DEFAULT_FILTER_DBM = -100
    const val DEFAULT_SIMULATE_DBM = -65

    /** 将存储值规范为 dBm 负数（兼容历史正数阈值） */
    fun normalizeDbm(value: Int, default: Int = DEFAULT_FILTER_DBM): Int {
        val dbm = when {
            value in MIN_DBM..MAX_DBM -> value
            value in 1..100 -> -value
            else -> default
        }
        return dbm.coerceIn(MIN_DBM, MAX_DBM)
    }

    fun normalizeDbmString(raw: String?, default: Int = DEFAULT_SIMULATE_DBM): Int {
        val parsed = raw?.trim()?.toIntOrNull() ?: return default
        return normalizeDbm(parsed, default)
    }

    /** 滑块 0..100 映射到 dBm：0 -> -100，100 -> 0 */
    fun sliderToDbm(slider: Float): Int = (slider.toInt() - 100).coerceIn(MIN_DBM, MAX_DBM)

    fun dbmToSlider(dbm: Int): Float = (100 + normalizeDbm(dbm)).toFloat()
}
