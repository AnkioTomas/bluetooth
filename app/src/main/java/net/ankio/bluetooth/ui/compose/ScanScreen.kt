package net.ankio.bluetooth.ui.compose

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.ankio.bluetooth.R
import net.ankio.bluetooth.bluetooth.BleDevice
import net.ankio.bluetooth.ui.compose.components.TabPageScaffold
import net.ankio.bluetooth.ui.compose.preview.PreviewSamples
import net.ankio.bluetooth.utils.BleConstant.BleConstant
import net.ankio.bluetooth.utils.SpUtils
import net.ankio.bluetooth.viewmodel.ScanUiState
import net.ankio.bluetooth.viewmodel.ScanViewModel
import net.ankio.theme.AnkioTheme
import net.ankio.theme.PreviewAll
import net.ankio.theme.PreviewAllThemes
import net.ankio.theme.ThemePreviewConfig
import net.ankio.theme.ThemePreviewParameterProvider
import net.ankio.theme.compat.ThemeCard
import net.ankio.theme.compat.ThemeFloatingActionButton
import net.ankio.theme.compat.ThemeIcon
import net.ankio.theme.compat.ThemeLinearProgressIndicator
import net.ankio.theme.compat.ThemePrimaryButton
import net.ankio.theme.compat.ThemeSlider
import net.ankio.theme.compat.ThemeSwitch
import net.ankio.theme.compat.ThemeText
import net.ankio.theme.compat.ThemeTextField

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ScanScreen(
    viewModel: ScanViewModel,
    onDeviceSelected: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var wasScanningBeforeFilter by remember { mutableStateOf(false) }

    TabPageScaffold(title = stringResource(R.string.nav_scan)) {
        Box(modifier = Modifier.fillMaxSize()) {
            ScanScreenContent(
                uiState = uiState,
                onToggleScan = viewModel::toggleScan,
                onDeviceClick = { device ->
                    viewModel.selectDevice(device)
                    onDeviceSelected()
                },
                onDeviceLongClick = viewModel::removeDeviceAt,
            )

            ScanFabActions(
                isScanning = uiState.isScanning,
                onToggleScan = viewModel::toggleScan,
                onSearch = viewModel::showHistory,
                onFilter = {
                    wasScanningBeforeFilter = uiState.isScanning
                    viewModel.stopScan()
                    viewModel.showFilterDialog(true)
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 16.dp),
            )
        }
    }

    if (uiState.showFilterDialog) {
        FilterDialog(
            onDismiss = {
                viewModel.onFilterDismiss(wasScanningBeforeFilter)
            },
        )
    }
}

@Composable
private fun ScanFabActions(
    isScanning: Boolean,
    onToggleScan: () -> Unit,
    onSearch: () -> Unit,
    onFilter: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.End,
    ) {
        // 扫描按钮 FAB
        ThemeFloatingActionButton(
            onClick = onToggleScan
        ) {
            if (isScanning) {
                ThemeText(
                    text = "STOP",
                    style = AnkioTheme.textStyles.main,
                    color = AnkioTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            } else {
                ThemeIcon(
                    imageVector = Icons.Default.Bluetooth,
                    contentDescription = stringResource(R.string.toggle_scan),
                    tint = AnkioTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        ThemeFloatingActionButton(onClick = onSearch) {
            ThemeIcon(
                imageVector = Icons.Default.Search,
                contentDescription = stringResource(R.string.scan_search),
                tint = AnkioTheme.colorScheme.onPrimaryContainer,
            )
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ScanScreenContent(
    uiState: ScanUiState,
    onToggleScan: () -> Unit,
    onDeviceClick: (BleDevice) -> Unit,
    onDeviceLongClick: (Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        if (uiState.devices.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 32.dp, bottom = 88.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                ThemeText(
                    text = stringResource(R.string.ic_empty),
                    style = AnkioTheme.textStyles.main,
                    color = AnkioTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 12.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                itemsIndexed(uiState.devices, key = { _, item -> item.address }) { index, device ->
                    BleDeviceItem(
                        device = device,
                        onClick = { onDeviceClick(device) },
                        onLongClick = { onDeviceLongClick(index) },
                    )
                }
            }
        }
    }
}

@PreviewAll
@Composable
private fun ScanScreenPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) config: ThemePreviewConfig,
) {
    PreviewAllThemes(config) {
        TabPageScaffold(title = stringResource(R.string.nav_scan)) {
            Box(Modifier.fillMaxSize()) {
                ScanScreenContent(
                    uiState = PreviewSamples.scanUiState,
                    onToggleScan = {},
                    onDeviceClick = {},
                    onDeviceLongClick = {},
                )
                ScanFabActions(
                    isScanning = false,
                    onToggleScan = {},
                    onSearch = {},
                    onFilter = {},
                    modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                )
            }
        }
    }
}

@PreviewAll
@Composable
private fun ScanScreenEmptyPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) config: ThemePreviewConfig,
) {
    PreviewAllThemes(config) {
        TabPageScaffold(title = stringResource(R.string.nav_scan)) {
            Box(Modifier.fillMaxSize()) {
                ScanScreenContent(
                    uiState = PreviewSamples.scanUiStateEmpty,
                    onToggleScan = {},
                    onDeviceClick = {},
                    onDeviceLongClick = {},
                )
                ScanFabActions(
                    isScanning = false,
                    onToggleScan = {},
                    onSearch = {},
                    onFilter = {},
                    modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                )
            }
        }
    }
}

@PreviewAll
@Composable
private fun FilterDialogPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) config: ThemePreviewConfig,
) {
    PreviewAllThemes(config) {
        val context = LocalContext.current
        SpUtils.init(context)
        FilterDialog(onDismiss = {})
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BleDeviceItem(
    device: BleDevice,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    ThemeCard(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ThemeIcon(
                Icons.Default.Bluetooth,
                contentDescription = null,
                tint = AnkioTheme.colorScheme.primary,
            )
            Column(Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = device.company ?: "None",
                        style = AnkioTheme.textStyles.title4,
                        color = AnkioTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    ThemeText(
                        text = "${device.rssi} dBm",
                        style = AnkioTheme.textStyles.footnote1,
                        color = AnkioTheme.colorScheme.onSurfaceVariant,
                    )
                }
                ThemeText(
                    text = device.address,
                    style = AnkioTheme.textStyles.footnote1,
                    color = AnkioTheme.colorScheme.onSurfaceVariant,
                )
                ThemeText(
                    text = device.data,
                    style = AnkioTheme.textStyles.footnote2,
                    color = AnkioTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun FilterDialog(onDismiss: () -> Unit) {
    var filterEmptyName by remember { mutableStateOf(SpUtils.getBoolean(BleConstant.NULL_NAME)) }
    var company by remember { mutableStateOf(SpUtils.getString(BleConstant.COMPANY, "")) }
    var rssi by remember { mutableStateOf(SpUtils.getInt(BleConstant.RSSI, 100).toFloat()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.filter)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ThemeText(
                        text = stringResource(R.string.filter_empty),
                        style = AnkioTheme.textStyles.main,
                        color = AnkioTheme.colorScheme.onSurface,
                    )
                    ThemeSwitch(
                        checked = filterEmptyName,
                        onCheckedChange = {
                            filterEmptyName = it
                            SpUtils.putBoolean(BleConstant.NULL_NAME, it)
                        },
                    )
                }
                ThemeText(
                    text = stringResource(R.string.signal_filter),
                    style = AnkioTheme.textStyles.main,
                    color = AnkioTheme.colorScheme.onSurface,
                )
                ThemeText(
                    text = "-${rssi.toInt()} dBm",
                    style = AnkioTheme.textStyles.footnote1,
                    color = AnkioTheme.colorScheme.onSurfaceVariant,
                )
                ThemeSlider(
                    value = rssi,
                    onValueChange = {
                        rssi = it
                        SpUtils.putInt(BleConstant.RSSI, it.toInt())
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
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.save_webdav))
            }
        },
    )
}
