package net.ankio.bluetooth.ui.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import net.ankio.bluetooth.R
import net.ankio.bluetooth.ui.compose.components.TabPageScaffold
import net.ankio.bluetooth.viewmodel.SimulateViewModel
import net.ankio.theme.AnkioTheme
import net.ankio.theme.compat.ThemeIcon
import net.ankio.theme.settings.SettingCardPosition
import net.ankio.theme.settings.ThemeSettingSlider
import net.ankio.theme.settings.ThemeSettingTextField

@Composable
fun SimulateScreen(viewModel: SimulateViewModel = viewModel()) {
    TabPageScaffold(title = stringResource(R.string.nav_simulate)) {
        SimulateScreenContent(
            prefMac = viewModel.prefMac,
            prefData = viewModel.prefData,
            prefRssi = viewModel.prefRssi,
            onPrefMacChange = { viewModel.prefMac = it },
            onPrefDataChange = { viewModel.prefData = it },
            onPrefRssiChange = { viewModel.prefRssi = it },
        )
    }
}

@Composable
fun SimulateScreenContent(
    prefMac: String,
    prefData: String,
    prefRssi: String,
    onPrefMacChange: (String) -> Unit,
    onPrefDataChange: (String) -> Unit,
    onPrefRssiChange: (String) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ThemeSettingTextField(
            value = prefMac,
            onValueChange = onPrefMacChange,
            title = stringResource(R.string.mac_data),
            startAction = {
                ThemeIcon(
                    imageVector = Icons.Filled.Router,
                    contentDescription = null,
                    tint = AnkioTheme.colorScheme.primary,
                )
            },
            position = SettingCardPosition.First,
        )

        ThemeSettingTextField(
            value = prefData,
            onValueChange = onPrefDataChange,
            title = stringResource(R.string.broadcast_data),
            startAction = {
                ThemeIcon(
                    imageVector = Icons.Filled.Campaign,
                    contentDescription = null,
                    tint = AnkioTheme.colorScheme.primary,
                )
            },
            position = SettingCardPosition.Middle,
        )

        var rssiValue by remember(prefRssi) { mutableIntStateOf(prefRssi.toIntOrNull() ?: -50) }
        var slider  = rssiValue.toFloat()

        ThemeSettingSlider(
            title = stringResource(R.string.signal),
            value = slider,
            onValueChange = {
                rssiValue = it.toInt()
                onPrefRssiChange(rssiValue.toString())
            },
            valueRange = 0f..100f,
            startAction = {
                ThemeIcon(
                    imageVector = Icons.Filled.SignalCellularAlt,
                    contentDescription = null,
                    tint = AnkioTheme.colorScheme.primary,
                )
            },
            valueLabel = "$rssiValue dBm",
            position = SettingCardPosition.Last,
        )
    }
}
