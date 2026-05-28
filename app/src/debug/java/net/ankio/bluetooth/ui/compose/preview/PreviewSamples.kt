package net.ankio.bluetooth.ui.compose.preview

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import net.ankio.bluetooth.R
import net.ankio.bluetooth.ble.BleDevice
import net.ankio.bluetooth.model.SimulateMode
import net.ankio.bluetooth.model.WebdavMode
import net.ankio.bluetooth.ui.compose.components.StatusKind
import net.ankio.webdav.lib.ui.WebDavTestUiState as ConnTestOutcome

object PreviewSamples {
    private val previewDevices = listOf(
        BleDevice(
            data = "0201060AFF4C000215E2C41BE",
            company = "Apple",
            rssi = -58,
            address = "AA:BB:CC:DD:EE:01",
            name = "iBeacon",
        ),
        BleDevice(
            data = "0201060303AAFE",
            company = "Xiaomi",
            rssi = -72,
            address = "AA:BB:CC:DD:EE:02",
            name = "Mi Band",
        ),
    )

    val statusMessageRes = R.string.active_success
    val statusKind = StatusKind.Success
    val statusIcon = Icons.Default.CheckCircle
    val webdavMode = WebdavMode.SyncFromWebdav
    val simulateMode = SimulateMode.Self

    val prefMac = "AA:BB:CC:DD:EE:FF"
    val prefData = "0201060AFF4C000215"
    val prefRssi = "-65"

    val webDavServer = "https://dav.example.com/dav/"
    val webDavUsername = "user"
    val webDavPassword = "******"
    val webDavProbeOutcome = ConnTestOutcome.Idle
    val hookLogEnabled = false
    val hideIcon = false

    val scanDevices = previewDevices
    val scanDevicesEmpty = emptyList<BleDevice>()
}
