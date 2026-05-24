package net.ankio.bluetooth.ui

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.flow.collectLatest
import net.ankio.bluetooth.R
import net.ankio.bluetooth.ui.compose.MainShell
import net.ankio.bluetooth.viewmodel.MainViewModel
import net.ankio.bluetooth.viewmodel.ScanViewModel
import net.ankio.theme.toast.ThemeToast

class MainActivity : BluetoothBaseComposeActivity() {

    private val mainViewModel: MainViewModel by viewModels()
    private val scanViewModel: ScanViewModel by viewModels()

    private var pendingScanReady: (() -> Unit)? = null

    private val enableBluetoothLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
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
                pendingScanReady?.invoke()
            } else {
                ThemeToast.show(getString(R.string.no_permission), ThemeToast.Style.Error)
            }
            pendingScanReady = null
        }

    @Composable
    override fun Content() {
        LaunchedEffect(scanViewModel) {
            scanViewModel.messages.collectLatest { message ->
                ThemeToast.show(message, ThemeToast.Style.Info)
            }
        }
        LaunchedEffect(mainViewModel) {
            mainViewModel.messages.collectLatest { messageRes ->
                val style = when (messageRes) {
                    R.string.sync_pull_success -> ThemeToast.Style.Success
                    R.string.webdav_not_configured -> ThemeToast.Style.Info
                    else -> ThemeToast.Style.Error
                }
                ThemeToast.show(getString(messageRes), style)
            }
        }
        MainShell(
            mainViewModel = mainViewModel,
            scanViewModel = scanViewModel,
            versionName = packageManager.getPackageInfo(packageName, 0).versionName ?: "",
            onRecreateForLocale = ::recreateForLocaleChange,
            onThemeChanged = ::recreateForThemeChange,
            onEnsureScanReady = ::ensureScanReady,
        )
    }

    override fun onResume() {
        super.onResume()
        mainViewModel.load()
        mainViewModel.refresh()
    }

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

    private fun openBluetoothIfNeeded() {
        BluetoothAdapter.getDefaultAdapter()?.let { adapter ->
            if (!adapter.isEnabled) {
                enableBluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            }
        }
    }
}
