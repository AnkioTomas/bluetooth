package net.ankio.bluetooth.ui.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import net.ankio.bluetooth.R
import net.ankio.bluetooth.model.SimulateMode
import net.ankio.bluetooth.model.WebdavMode
import net.ankio.bluetooth.ui.compose.components.StatusBanner
import net.ankio.bluetooth.ui.compose.components.StatusKind
import net.ankio.bluetooth.viewmodel.HomeViewModel
import net.ankio.theme.AnkioTheme
import net.ankio.theme.settings.SettingCardPosition
import net.ankio.theme.settings.ThemeSectionHeader
import net.ankio.theme.settings.ThemeSettingDropdown

@Composable
fun HomeScreen(viewModel: HomeViewModel = viewModel()) {

    HomeScreenContent(
        webdavMode = viewModel.webdavMode,
        simulateMode = viewModel.simulateMode,
        pluginStatusMessage = viewModel.pluginStatusMessage,
        pluginStatusKind = viewModel.pluginStatusKind,
        onWebdavModeChange = { viewModel.webdavMode = it },
        onSimulateModeChange = { viewModel.simulateMode = it },
    )
}

@Composable
fun HomeScreenContent(
    webdavMode: WebdavMode,
    simulateMode: SimulateMode,
    pluginStatusMessage: String,
    pluginStatusKind: StatusKind,
    onWebdavModeChange: (WebdavMode) -> Unit,
    onSimulateModeChange: (SimulateMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val webdavOptions = WebdavMode.entries
    val simulateOptions = SimulateMode.entries
    val webdavLabels = webdavOptions.map { stringResource(it.labelRes()) }
    val simulateLabels = simulateOptions.map { stringResource(it.labelRes()) }
    val webdavIndex = webdavOptions.indexOf(webdavMode).coerceAtLeast(0)
    val simulateIndex = simulateOptions.indexOf(simulateMode).coerceAtLeast(0)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        StatusBanner(
            message = pluginStatusMessage.ifEmpty { stringResource(R.string.active_error) },
            kind = pluginStatusKind,
            icon = pluginStatusIcon(pluginStatusKind),
        )

        ThemeSectionHeader(stringResource(R.string.home_section_modes))

        ThemeSettingDropdown(
            items = webdavLabels,
            selectedIndex = webdavIndex,
            onSelectedIndexChange = { onWebdavModeChange(webdavOptions[it]) },
            title = stringResource(R.string.home_webdav_mode),
            startAction = {
                Icon(
                    imageVector = Icons.Filled.CloudSync,
                    contentDescription = null,
                    tint = AnkioTheme.colorScheme.primary,
                )
            },
            position = SettingCardPosition.First,
        )

        ThemeSettingDropdown(
            items = simulateLabels,
            selectedIndex = simulateIndex,
            onSelectedIndexChange = { onSimulateModeChange(simulateOptions[it]) },
            title = stringResource(R.string.home_simulate_mode),
            startAction = {
                Icon(
                    imageVector = Icons.Filled.Bluetooth,
                    contentDescription = null,
                    tint = AnkioTheme.colorScheme.primary,
                )
            },
            position = SettingCardPosition.Last,
        )
    }
}

private fun pluginStatusIcon(kind: StatusKind): ImageVector = when (kind) {
    StatusKind.Error -> Icons.Default.Error
    StatusKind.Warning -> Icons.Default.Warning
    StatusKind.Success -> Icons.Default.CheckCircle
}
