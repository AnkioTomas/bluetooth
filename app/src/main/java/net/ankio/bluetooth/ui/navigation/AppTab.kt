package net.ankio.bluetooth.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.ui.graphics.vector.ImageVector
import net.ankio.bluetooth.R

enum class AppTab(
    val route: String,
    @StringRes val titleRes: Int,
    val icon: ImageVector,
) {
    Home("home", R.string.nav_home, Icons.Default.Home),
    Simulate("simulate", R.string.nav_simulate, Icons.Default.Tune),
    Scan("scan", R.string.nav_scan, Icons.Default.Bluetooth),
    Settings("settings", R.string.nav_settings, Icons.Default.Settings),
}
