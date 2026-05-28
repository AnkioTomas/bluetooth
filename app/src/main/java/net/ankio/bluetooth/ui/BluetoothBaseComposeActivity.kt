package net.ankio.bluetooth.ui

import android.content.Context
import net.ankio.bluetooth.App
import net.ankio.bluetooth.utils.ContextWrapper
import net.ankio.bluetooth.utils.LocaleDelegate
import net.ankio.theme.BaseComposeActivity

abstract class BluetoothBaseComposeActivity : BaseComposeActivity() {
    private var appliedLocaleVersion: Int = -1

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase?.let { ContextWrapper.wrap(it, App.getLocale()) })
        appliedLocaleVersion = LocaleDelegate.localeVersion
    }

    override fun onResume() {
        super.onResume()
        if (appliedLocaleVersion != LocaleDelegate.localeVersion) {
            appliedLocaleVersion = LocaleDelegate.localeVersion
            recreate()
        }
    }

    protected fun recreateForLocaleChange() {
        appliedLocaleVersion = LocaleDelegate.localeVersion
        recreate()
    }
}
