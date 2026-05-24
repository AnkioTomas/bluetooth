package net.ankio.bluetooth.ui.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.ankio.bluetooth.R
import net.ankio.bluetooth.ui.compose.components.SwitchRow
import net.ankio.bluetooth.ui.compose.preview.PreviewSamples
import net.ankio.bluetooth.viewmodel.MainUiState
import net.ankio.bluetooth.viewmodel.MainViewModel
import net.ankio.theme.AnkioTheme
import net.ankio.theme.PreviewAll
import net.ankio.theme.PreviewAllThemes
import net.ankio.theme.ThemePreviewConfig
import net.ankio.theme.ThemePreviewParameterProvider
import net.ankio.theme.compat.ThemeCard
import net.ankio.theme.compat.ThemeSecondaryButton
import net.ankio.theme.compat.ThemeSlider
import net.ankio.theme.compat.ThemeSmallTitle
import net.ankio.theme.compat.ThemeText
import net.ankio.theme.compat.ThemeTextField

@Composable
fun SimulateScreen(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SimulateScreenContent(
        uiState = uiState,
        onPrefEnableChange = viewModel::updatePrefEnable,
        onPrefMacChange = viewModel::updatePrefMac,
        onPrefDataChange = viewModel::updatePrefData,
        onPrefRssiChange = viewModel::updatePrefRssi,
        onPrefAsSenderChange = viewModel::updatePrefAsSender,
        onPrefMac2Change = viewModel::updatePrefMac2,
        onPrefCompanyChange = viewModel::updatePrefCompany,
        onPullFromWebdav = viewModel::pullFromWebdav,
    )
}

@Composable
fun SimulateScreenContent(
    uiState: MainUiState,
    onPrefEnableChange: (Boolean) -> Unit,
    onPrefMacChange: (String) -> Unit,
    onPrefDataChange: (String) -> Unit,
    onPrefRssiChange: (String) -> Unit,
    onPrefAsSenderChange: (Boolean) -> Unit,
    onPrefMac2Change: (String) -> Unit,
    onPrefCompanyChange: (String) -> Unit,
    onPullFromWebdav: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ThemeSmallTitle(stringResource(R.string.bluetooth_data))
        ThemeCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (!uiState.prefAsSender) {
                    SwitchRow(
                        title = stringResource(R.string.use_mock),
                        checked = uiState.prefEnable,
                        onCheckedChange = onPrefEnableChange,
                    )
                }

                ThemeTextField(
                    value = uiState.prefMac,
                    onValueChange = onPrefMacChange,
                    label = stringResource(R.string.mac_data),
                    modifier = Modifier.fillMaxWidth(),
                )
                ThemeTextField(
                    value = uiState.prefData,
                    onValueChange = onPrefDataChange,
                    label = stringResource(R.string.broadcast_data),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false,
                    maxLines = 4,
                )

                Column {
                    ThemeText(
                        text = stringResource(R.string.signal),
                        style = AnkioTheme.textStyles.title4,
                        color = AnkioTheme.colorScheme.onSurface,
                    )
                    val rssiValue = uiState.prefRssi.toIntOrNull() ?: -50
                    val sliderValue = when {
                        rssiValue >= 0 -> 100f
                        rssiValue <= -100 -> 0f
                        else -> (100 + rssiValue).toFloat()
                    }
                    ThemeSlider(
                        value = sliderValue,
                        onValueChange = { value ->
                            val newRssi = when {
                                value >= 100 -> 0
                                value <= 0 -> -100
                                else -> (value - 100).toInt()
                            }
                            onPrefRssiChange(newRssi.toString())
                        },
                        valueRange = 0f..100f,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    ThemeText(
                        text = "${uiState.prefRssi} dBm",
                        style = AnkioTheme.textStyles.footnote1,
                        color = AnkioTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        ThemeSmallTitle(stringResource(R.string.server_data))
        ThemeCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ThemeText(
                        text = stringResource(R.string.webdav_enable),
                        style = AnkioTheme.textStyles.title4,
                        color = AnkioTheme.colorScheme.onSurface,
                    )
                    ThemeText(
                        text = uiState.webdavLast,
                        style = AnkioTheme.textStyles.footnote1,
                        color = AnkioTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (!uiState.prefAsSender) {
                    ThemeSecondaryButton(
                        onClick = onPullFromWebdav,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = uiState.prefEnableWebdav,
                    ) {
                        ThemeText(
                            text = stringResource(R.string.sync_from_webdav),
                            style = AnkioTheme.textStyles.main,
                            color = AnkioTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }

                SwitchRow(
                    title = stringResource(R.string.as_sender),
                    checked = uiState.prefAsSender,
                    onCheckedChange = onPrefAsSenderChange,
                )

                if (uiState.prefAsSender) {
                    ThemeTextField(
                        value = uiState.prefMac2,
                        onValueChange = onPrefMac2Change,
                        label = stringResource(R.string.mac_data),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    ThemeTextField(
                        value = uiState.prefCompany,
                        onValueChange = onPrefCompanyChange,
                        label = stringResource(R.string.company_data),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@PreviewAll
@Composable
private fun SimulateScreenPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) config: ThemePreviewConfig,
) {
    PreviewAllThemes(config) {
        SimulateScreenContent(
            uiState = PreviewSamples.mainUiState,
            onPrefEnableChange = {},
            onPrefMacChange = {},
            onPrefDataChange = {},
            onPrefRssiChange = {},
            onPrefAsSenderChange = {},
            onPrefMac2Change = {},
            onPrefCompanyChange = {},
            onPullFromWebdav = {},
        )
    }
}
