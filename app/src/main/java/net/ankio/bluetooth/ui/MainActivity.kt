package net.ankio.bluetooth.ui

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.os.Build
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.core.content.ContextCompat
import net.ankio.bluetooth.R
import net.ankio.bluetooth.ble.BlePermissions
import net.ankio.bluetooth.model.SimulateMode
import net.ankio.bluetooth.model.WebdavMode
import net.ankio.bluetooth.service.BleAdvertiserService
import net.ankio.bluetooth.service.WebdavPushService
import net.ankio.bluetooth.ui.compose.HomeScreen
import net.ankio.bluetooth.ui.compose.ScanScreen
import net.ankio.bluetooth.ui.compose.SettingsScreen
import net.ankio.bluetooth.ui.compose.SimulateScreen
import net.ankio.bluetooth.ui.navigation.AppTab
import net.ankio.theme.compat.ThemeTopAppBarTitleAlignment
import net.ankio.theme.layout.ThemeApp
import net.ankio.theme.toast.ThemeToast

class MainActivity : BluetoothBaseComposeActivity() {

    private var pendingScanReady: (() -> Unit)? = null

    private val enableBluetoothLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                val message = if (BluetoothAdapter.getDefaultAdapter()?.isEnabled == true) {
                    getString(R.string.start_scan)
                } else {
                    getString(R.string.not_open_bluetooth)
                }
                ThemeToast.show(message, ThemeToast.Style.Info)
            }
        }

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
            if (granted.values.all { it }) {
                openBluetoothIfNeeded()
                if (WebdavMode.current() == WebdavMode.Sender2Webdav) {
                    WebdavPushService.start(this, showToast = false)
                }
                if (SimulateMode.current() == SimulateMode.SenderNearBy) {
                    BleAdvertiserService.start(this)
                }
                pendingScanReady?.invoke()
            } else {
                ThemeToast.show(getString(R.string.no_permission), ThemeToast.Style.Error)
            }
            pendingScanReady = null
        }

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* optional */ }

    @Composable
    override fun Content() {
        LaunchedEffect(Unit) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(
                    this@MainActivity,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            if (WebdavMode.current() == WebdavMode.Sender2Webdav &&
                !BlePermissions.hasScan(this@MainActivity)
            ) {
                permissionLauncher.launch(BlePermissions.required())
            }
        }

        ThemeApp(
            start = AppTab.Home.route,
            titleAlignment = ThemeTopAppBarTitleAlignment.Start,
        ) {
            screen(
                route = AppTab.Home.route,
                title = getString(R.string.nav_home),
                tab = AppTab.Home.icon to getString(AppTab.Home.titleRes),
            ) {
                scrollColumn {
                    HomeScreen(
                        onRequestBlePermissions = ::requestBlePermissions,
                        onRequestBleAdvertisePermissions = ::requestBleAdvertisePermissions,
                    )
                }
            }
            screen(
                route = AppTab.Simulate.route,
                title = getString(R.string.nav_simulate),
                tab = AppTab.Simulate.icon to getString(AppTab.Simulate.titleRes),
            ) {
                scrollColumn {
                    SimulateScreen()
                }
            }
            screen(
                route = AppTab.Scan.route,
                title = getString(R.string.nav_scan),
                tab = AppTab.Scan.icon to getString(AppTab.Scan.titleRes),
            ) {
                LaunchedEffect(Unit) {
                    ensureScanReady {}
                }
                ScanScreen(
                    list = lazyList(),
                    onNavigateToSimulate = {
                        go(AppTab.Simulate.route) {
                            popUpTo(AppTab.Home.route) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
            screen(
                route = AppTab.Settings.route,
                title = getString(R.string.nav_settings),
                tab = AppTab.Settings.icon to getString(AppTab.Settings.titleRes),
                collapse = true,
            ) {
                scrollColumn {
                    SettingsScreen(
                        onRecreateForLocale = ::recreateForLocaleChange,
                        onThemeChanged = ::recreateForThemeChange,
                    )
                }
            }
        }
    }

    private fun requestBlePermissions() {
        if (BlePermissions.hasScan(this)) return
        permissionLauncher.launch(BlePermissions.required())
    }

    private fun requestBleAdvertisePermissions() {
        if (BlePermissions.hasAdvertise(this)) return
        permissionLauncher.launch(BlePermissions.requiredForAdvertise())
    }

    private fun ensureScanReady(onReady: () -> Unit) {
        pendingScanReady = onReady
        permissionLauncher.launch(BlePermissions.required())
    }

    private fun openBluetoothIfNeeded() {
        getSystemService(
            BluetoothManager::class.java,
        ).adapter.let { adapter ->
            if (!adapter.isEnabled) {
                enableBluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            }
        }
    }
}
