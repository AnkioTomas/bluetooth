package net.ankio.bluetooth.ui

import android.content.Context
import net.ankio.bluetooth.App
import net.ankio.bluetooth.utils.ContextWrapper
import net.ankio.bluetooth.utils.LocaleDelegate
import net.ankio.theme.BaseComposeActivity

abstract class BluetoothBaseComposeActivity : BaseComposeActivity() {

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase?.let { ContextWrapper.wrap(it, App.getLocale()) })
        LocaleDelegate.changedList[javaClass] = true
    }

    override fun onResume() {
        super.onResume()
        if (!LocaleDelegate.changedList.containsKey(javaClass) ||
            LocaleDelegate.changedList[javaClass] == false
        ) {
            LocaleDelegate.changedList[javaClass] = true
            recreate()
        }
    }

    protected fun recreateForLocaleChange() {
        LocaleDelegate.changedList.clear()
        LocaleDelegate.changedList[javaClass] = true
        recreate()
    }
}
