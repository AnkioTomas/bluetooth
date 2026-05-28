package net.ankio.bluetooth.ui.compose

import androidx.compose.runtime.compositionLocalOf
import androidx.navigation.NavHostController

/** 主页 Tab NavHostController；预览未提供时为 null（导航与 Toast 不执行）。 */
val LocalBluetoothNavHost = compositionLocalOf<NavHostController?> { null }
