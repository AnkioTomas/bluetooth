package net.ankio.bluetooth

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.text.TextUtils
import net.ankio.bluetooth.utils.LocaleDelegate
import net.ankio.bluetooth.utils.SpUtils
import net.ankio.theme.ThemeSettings
import net.ankio.theme.toast.ThemeToast
import java.util.Locale


open class App : Application() {

    companion object {
        fun getLocale(tag: String): Locale? {
            return if (TextUtils.isEmpty(tag) || "SYSTEM" == tag) {
                LocaleDelegate.systemLocale
            } else Locale.forLanguageTag(tag)
        }

        fun getLocale(): Locale? {
            val tag: String = SpUtils.getString("setting_language", "SYSTEM").ifEmpty { "SYSTEM" }
            return getLocale(tag)
        }


        @SuppressLint("StaticFieldLeak")
        lateinit var context: Context
    }



    override fun onCreate() {
        super.onCreate()
        context = applicationContext
        SpUtils.init(this)
        ThemeSettings.init(this)
        ThemeToast.init(this)
        LocaleDelegate.defaultLocale = getLocale()
    }



}