package net.ankio.bluetooth.ui.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.ankio.bluetooth.R
import net.ankio.bluetooth.ui.compose.components.TabPageScaffold
import net.ankio.bluetooth.ui.compose.preview.PreviewSamples
import net.ankio.bluetooth.viewmodel.MainUiState
import net.ankio.bluetooth.viewmodel.MainViewModel
import net.ankio.theme.AnkioTheme
import net.ankio.theme.PreviewAll
import net.ankio.theme.PreviewAllThemes
import net.ankio.theme.ThemePreviewConfig
import net.ankio.theme.ThemePreviewParameterProvider
import net.ankio.theme.compat.ThemeCard
import net.ankio.theme.compat.ThemeSlider
import net.ankio.theme.compat.ThemeText
import net.ankio.theme.compat.ThemeTextField
import net.ankio.theme.settings.SettingCardPosition
import net.ankio.theme.settings.ThemeSectionHeader
import net.ankio.theme.settings.toShape
import net.ankio.theme.settings.toVerticalPadding

@Composable
fun SimulateScreen(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    TabPageScaffold(title = stringResource(R.string.nav_simulate)) {
        SimulateScreenContent(
            uiState = uiState,
            onPrefMacChange = viewModel::updatePrefMac,
            onPrefDataChange = viewModel::updatePrefData,
            onPrefRssiChange = viewModel::updatePrefRssi,
        )
    }
}

@Composable
fun SimulateScreenContent(
    uiState: MainUiState,
    onPrefMacChange: (String) -> Unit,
    onPrefDataChange: (String) -> Unit,
    onPrefRssiChange: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ThemeSectionHeader(stringResource(R.string.bluetooth_data))

        SimulateSettingBlock(
            title = stringResource(R.string.mac_data),
            icon = {
                Icon(
                    imageVector = Icons.Filled.Router,
                    contentDescription = null,
                    tint = AnkioTheme.colorScheme.primary,
                )
            },
            position = SettingCardPosition.First,
        ) {
            ThemeTextField(
                value = uiState.prefMac,
                onValueChange = onPrefMacChange,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        SimulateSettingBlock(
            title = stringResource(R.string.broadcast_data),
            icon = {
                Icon(
                    imageVector = Icons.Filled.Campaign,
                    contentDescription = null,
                    tint = AnkioTheme.colorScheme.primary,
                )
            },
            position = SettingCardPosition.Middle,
        ) {
            ThemeTextField(
                value = uiState.prefData,
                onValueChange = onPrefDataChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = false,
                maxLines = 4,
            )
        }

        SimulateSettingBlock(
            title = stringResource(R.string.signal),
            icon = {
                Icon(
                    imageVector = Icons.Filled.SignalCellularAlt,
                    contentDescription = null,
                    tint = AnkioTheme.colorScheme.primary,
                )
            },
            position = SettingCardPosition.Last,
        ) {
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

@Composable
private fun SimulateSettingBlock(
    title: String,
    icon: @Composable () -> Unit,
    position: SettingCardPosition,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val (topPad, bottomPad) = position.toVerticalPadding()
    ThemeCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = topPad, bottom = bottomPad),
        shape = position.toShape(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier.size(40.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    icon()
                }
                ThemeText(
                    text = title,
                    style = AnkioTheme.textStyles.title4,
                    color = AnkioTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
            }
            content()
        }
    }
}

@PreviewAll
@Composable
private fun SimulateScreenPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) config: ThemePreviewConfig,
) {
    PreviewAllThemes(config) {
        TabPageScaffold(title = stringResource(R.string.nav_simulate)) {
            SimulateScreenContent(
                uiState = PreviewSamples.mainUiState,
                onPrefMacChange = {},
                onPrefDataChange = {},
                onPrefRssiChange = {},
            )
        }
    }
}
