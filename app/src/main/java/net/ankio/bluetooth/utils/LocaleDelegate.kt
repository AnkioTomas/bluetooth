package net.ankio.bluetooth.utils

import java.util.Locale

class LocaleDelegate {


    companion object {

        /** current locale  */
        @JvmStatic
        var defaultLocale: Locale? = Locale.getDefault()
            private set

        /** system locale  */
        @JvmStatic
        var systemLocale: Locale? = Locale.getDefault()

        /**
         * 语言配置版本号。每次语言切换成功后递增一次，
         * Activity 通过比较本地已应用版本判断是否需要 recreate。
         */
        @JvmStatic
        var localeVersion: Int = 0
            private set

        @JvmStatic
        fun updateDefaultLocale(locale: Locale?, increaseVersion: Boolean = true) {
            val changed = defaultLocale != locale
            defaultLocale = locale
            if (changed && increaseVersion) {
                localeVersion++
            }
        }
    }
}