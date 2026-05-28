package net.ankio.bluetooth.ui

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import net.ankio.bluetooth.R
import net.ankio.bluetooth.ui.compose.HomeScreen
import net.ankio.bluetooth.ui.compose.MainShell
import net.ankio.bluetooth.ui.compose.ScanScreen
import net.ankio.bluetooth.ui.compose.SettingsScreen
import net.ankio.bluetooth.ui.compose.SimulateScreen
import net.ankio.bluetooth.ui.navigation.AppTab
import net.ankio.bluetooth.ui.navigation.navigateToTab
import net.ankio.theme.toast.ThemeToast

class MainActivity : BluetoothBaseComposeActivity() {

    private var pendingScanReady: (() -> Unit)? = null

    /**
     * 处理系统蓝牙开启请求的返回结果：
     * - 用户允许后提示可开始扫描
     * - 仍未开启时给出提示
     */
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

    /**
     * 申请扫描所需权限，授权成功后继续执行挂起的扫描前动作。
     */
    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
            if (granted.values.all { it }) {
                openBluetoothIfNeeded()
                pendingScanReady?.invoke()
            } else {
                ThemeToast.show(getString(R.string.no_permission), ThemeToast.Style.Error)
            }
            pendingScanReady = null
        }

    /**
     * 组合应用主布局：
     * - Activity 维护导航状态与 tab 选中态
     * - MainShell 只负责壳布局与底栏
     * - NavHost 作为 slot 注入壳中
     */
    @Composable
    override fun Content() {
        val navController = rememberNavController()
        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = backStackEntry?.destination?.route
        val selectedTab = AppTab.entries.firstOrNull { it.route == currentRoute } ?: AppTab.Home

        MainShell(
            navController = navController,
            selectedTab = selectedTab,
            onTabSelected = { tab ->
                if (tab == AppTab.Scan) {
                    ensureScanReady { navController.navigateToTab(tab) }
                } else {
                    navController.navigateToTab(tab)
                }
            },
        ) { contentModifier ->
            NavHost(
                navController = navController,
                startDestination = AppTab.Home.route,
                modifier = contentModifier,
            ) {
                composable(AppTab.Home.route) {
                    HomeScreen()
                }
                composable(AppTab.Simulate.route) {
                    SimulateScreen()
                }
                composable(AppTab.Scan.route) {
                    ScanScreen()
                }
                composable(AppTab.Settings.route) {
                    SettingsScreen(
                        onRecreateForLocale = ::recreateForLocaleChange,
                        onThemeChanged = ::recreateForThemeChange,
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
    }

    /**
     * 在进入扫描页前完成权限检查。
     *
     * @param onReady 当权限通过后执行的回调（通常用于导航到扫描页）
     */
    private fun ensureScanReady(onReady: () -> Unit) {
        pendingScanReady = onReady
        val permissions = buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(Manifest.permission.BLUETOOTH_SCAN)
                add(Manifest.permission.BLUETOOTH_CONNECT)
            }
        }.toTypedArray()
        permissionLauncher.launch(permissions)
    }

    /**
     * 权限通过后确保蓝牙已开启，未开启则拉起系统蓝牙开关请求。
     */
    private fun openBluetoothIfNeeded() {
        getSystemService(
            BluetoothManager::class.java
        ).adapter.let { adapter ->
            if (!adapter.isEnabled) {
                enableBluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            }
        }
    }
}
