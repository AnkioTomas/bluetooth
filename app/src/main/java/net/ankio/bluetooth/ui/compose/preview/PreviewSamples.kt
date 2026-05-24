package net.ankio.bluetooth.ui.compose.preview

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import net.ankio.bluetooth.R
import net.ankio.bluetooth.bluetooth.BleDevice
import net.ankio.bluetooth.viewmodel.MainUiState
import net.ankio.bluetooth.viewmodel.ScanUiState
import net.ankio.bluetooth.viewmodel.StatusBannerState
import net.ankio.bluetooth.viewmodel.StatusKind

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

    val mainUiState = MainUiState(
        status = StatusBannerState(
            messageRes = R.string.active_success,
            kind = StatusKind.Success,
            icon = Icons.Default.CheckCircle,
        ),
        prefMac = "AA:BB:CC:DD:EE:FF",
        prefData = "0201060AFF4C000215",
        prefRssi = "-65",
        prefEnable = true,
        prefEnableWebdav = true,
        prefAsSender = false,
        webdavServer = "https://dav.example.com/dav/",
        webdavUsername = "user",
        webdavPassword = "******",
        prefCompany = "Apple",
        prefMac2 = "11:22:33:44:55:66",
        webdavLast = "2026-05-24 12:00",
    )

    val scanUiState = ScanUiState(
        devices = previewDevices,
        isScanning = true,
        showFilterDialog = false,
    )

    val scanUiStateEmpty = ScanUiState()
}
