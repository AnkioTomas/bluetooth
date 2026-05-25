package net.ankio.bluetooth.ui.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.ankio.bluetooth.R
import net.ankio.bluetooth.model.SimulateMode
import net.ankio.bluetooth.model.WebdavMode
import net.ankio.bluetooth.ui.compose.components.StatusBanner
import net.ankio.bluetooth.ui.compose.components.TabPageScaffold
import net.ankio.bluetooth.ui.compose.preview.PreviewSamples
import net.ankio.bluetooth.viewmodel.MainUiState
import net.ankio.bluetooth.viewmodel.MainViewModel
import net.ankio.theme.AnkioTheme
import net.ankio.theme.PreviewAll
import net.ankio.theme.PreviewAllThemes
import net.ankio.theme.ThemePreviewConfig
import net.ankio.theme.ThemePreviewParameterProvider
import net.ankio.theme.settings.SettingCardPosition
import net.ankio.theme.settings.ThemeSectionHeader
import net.ankio.theme.settings.ThemeSettingDropdown

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    versionName: String,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    TabPageScaffold(title = stringResource(R.string.nav_home)) {
        HomeScreenContent(
            uiState = uiState,
            onWebdavModeChange = viewModel::updateWebdavMode,
            onSimulateModeChange = viewModel::updateSimulateMode,
        )
    }
}

@Composable
fun HomeScreenContent(
    uiState: MainUiState,
    onWebdavModeChange: (WebdavMode) -> Unit,
    onSimulateModeChange: (SimulateMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val webdavOptions = WebdavMode.entries
    val simulateOptions = SimulateMode.entries
    val webdavLabels = webdavOptions.map { stringResource(it.labelRes()) }
    val simulateLabels = simulateOptions.map { stringResource(it.labelRes()) }
    val webdavIndex = webdavOptions.indexOf(uiState.webdavMode).coerceAtLeast(0)
    val simulateIndex = simulateOptions.indexOf(uiState.simulateMode).coerceAtLeast(0)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        StatusBanner(
            message = stringResource(uiState.status.messageRes),
            kind = uiState.status.kind,
            icon = uiState.status.icon,
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

@PreviewAll
@Composable
private fun HomeScreenPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) config: ThemePreviewConfig,
) {
    PreviewAllThemes(config) {
        TabPageScaffold(title = stringResource(R.string.nav_home)) {
            HomeScreenContent(
                uiState = PreviewSamples.mainUiState,
                onWebdavModeChange = {},
                onSimulateModeChange = {},
            )
        }
    }
}
