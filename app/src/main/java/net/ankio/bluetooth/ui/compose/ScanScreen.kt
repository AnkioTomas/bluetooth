package net.ankio.bluetooth.ui.compose

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import net.ankio.bluetooth.R
import net.ankio.bluetooth.ble.BleConstant.BleConstant
import net.ankio.bluetooth.ble.BleDevice
import net.ankio.bluetooth.ble.Rssi
import net.ankio.bluetooth.utils.SpUtils
import net.ankio.bluetooth.viewmodel.ScanViewModel
import net.ankio.theme.AnkioTheme
import net.ankio.theme.compat.ThemeCard
import net.ankio.theme.compat.ThemeFloatingActionButton
import net.ankio.theme.compat.ThemeIcon
import net.ankio.theme.compat.ThemeLinearProgressIndicator
import net.ankio.theme.compat.ThemeSlider
import net.ankio.theme.compat.ThemeSwitch
import net.ankio.theme.compat.ThemeText
import net.ankio.theme.compat.ThemeTextField
import net.ankio.theme.layout.ThemeLazyListScroll
import net.ankio.theme.settings.ThemeSectionHeader
import net.ankio.theme.sheet.ThemeBottomSheet
import net.ankio.theme.toast.ThemeToast

@Composable
fun ScanScreen(
    list: ThemeLazyListScroll,
    onNavigateToSimulate: () -> Unit,
    viewModel: ScanViewModel = viewModel(),
) {
    val deviceSelectedToast = stringResource(R.string.device_selected_to_simulate)

    LaunchedEffect(viewModel.openSimulate) {
        if (!viewModel.openSimulate) return@LaunchedEffect
        onNavigateToSimulate()
        ThemeToast.show(deviceSelectedToast, ThemeToast.Style.Success)
        viewModel.clearOpenSimulate()
    }

    ScanScreenContent(
        list = list,
        devices = viewModel.devices,
        isScanning = viewModel.isScanning,
        onScanFabClick = viewModel::onScanFabClick,
        onFilter = viewModel::openFilterDialog,
        onDeviceClick = viewModel::selectDevice,
    )

    if (viewModel.showFilterDialog) {
        ScanFilterSheet(onDismiss = viewModel::dismissFilterDialog)
    }
}

@Composable
fun ScanScreenContent(
    list: ThemeLazyListScroll,
    devices: List<BleDevice>,
    isScanning: Boolean,
    onScanFabClick: () -> Unit,
    onFilter: () -> Unit,
    onDeviceClick: (BleDevice) -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = list.state,
            modifier = list.modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (isScanning) {
                item(key = "progress") {
                    ThemeLinearProgressIndicator(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .fillMaxWidth(),
                    )
                }
            }

            if (devices.isEmpty()) {
                item(key = "empty") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(320.dp)
                            .padding(top = 24.dp, bottom = 88.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        ThemeText(
                            text = stringResource(R.string.ic_empty),
                            style = AnkioTheme.textStyles.main,
                            color = AnkioTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                item(key = "header") {
                    ThemeSectionHeader(stringResource(R.string.scan_blue))
                }
                itemsIndexed(devices, key = { _, item -> item.address }) { _, device ->
                    ScanDeviceCard(
                        device = device,
                        onClick = { onDeviceClick(device) },
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.End,
        ) {
            ThemeFloatingActionButton(onClick = onScanFabClick) {
                if (isScanning) {
                    ThemeIcon(
                        imageVector = Icons.Default.BluetoothDisabled,
                        contentDescription = stringResource(R.string.close_bluetooth),
                        tint = AnkioTheme.colorScheme.onPrimaryContainer,
                    )
                } else {
                    ThemeIcon(
                        imageVector = Icons.Default.Bluetooth,
                        contentDescription = stringResource(R.string.toggle_scan),
                        tint = AnkioTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
            ThemeFloatingActionButton(onClick = onFilter) {
                ThemeIcon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = stringResource(R.string.scan_filter),
                    tint = AnkioTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ScanDeviceCard(
    device: BleDevice,
    onClick: () -> Unit,
) {
    ThemeCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            ThemeIcon(
                Icons.Default.Bluetooth,
                contentDescription = null,
                tint = AnkioTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ThemeText(
                        text = device.name ?: "None",
                        style = AnkioTheme.textStyles.title4,
                        color = AnkioTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    ThemeText(
                        text = "${Rssi.normalizeDbm(device.rssi)} dBm",
                        style = AnkioTheme.textStyles.footnote1,
                        color = AnkioTheme.colorScheme.primary,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ThemeText(
                        text = device.company ?: "None",
                        style = AnkioTheme.textStyles.footnote1,
                        color = AnkioTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    ThemeText(
                        text = device.address,
                        style = AnkioTheme.textStyles.footnote1,
                        color = AnkioTheme.colorScheme.onSurfaceVariant,
                    )
                }
                ThemeText(
                    text = device.data,
                    style = AnkioTheme.textStyles.footnote2,
                    color = AnkioTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScanFilterSheet(onDismiss: () -> Unit) {
    var filterEmptyName by remember { mutableStateOf(SpUtils.getBoolean(BleConstant.NULL_NAME)) }
    var company by remember { mutableStateOf(SpUtils.getString(BleConstant.COMPANY, "")) }
    var minRssi by remember {
        mutableIntStateOf(
            Rssi.normalizeDbm(SpUtils.getInt(BleConstant.RSSI, Rssi.DEFAULT_FILTER_DBM)),
        )
    }
    var slider by remember(minRssi) { mutableFloatStateOf(Rssi.dbmToSlider(minRssi)) }

    ThemeBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ThemeText(
                stringResource(R.string.filter),
                style = AnkioTheme.textStyles.title2,
                color = AnkioTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ThemeText(
                    text = stringResource(R.string.filter_empty),
                    style = AnkioTheme.textStyles.main,
                    color = AnkioTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp),
                )
                ThemeSwitch(checked = filterEmptyName, onCheckedChange = {
                    filterEmptyName = it
                    SpUtils.putBoolean(BleConstant.NULL_NAME, it)
                })
            }
            ThemeText(
                text = stringResource(R.string.signal_filter),
                style = AnkioTheme.textStyles.main,
                color = AnkioTheme.colorScheme.onSurface,
            )
            ThemeText(
                text = "$minRssi dBm",
                style = AnkioTheme.textStyles.footnote1,
                color = AnkioTheme.colorScheme.onSurfaceVariant,
            )
            ThemeSlider(
                value = slider,
                onValueChange = {
                    slider = it
                    minRssi = Rssi.sliderToDbm(it)
                    SpUtils.putInt(BleConstant.RSSI, minRssi)
                },
                valueRange = 0f..100f,
                modifier = Modifier.fillMaxWidth(),
            )
            ThemeTextField(
                value = company,
                onValueChange = {
                    company = it
                    SpUtils.putString(BleConstant.COMPANY, it)
                },
                label = stringResource(R.string.company_filter),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
