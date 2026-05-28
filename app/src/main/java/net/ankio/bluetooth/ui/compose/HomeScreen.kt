package net.ankio.bluetooth.ui.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import net.ankio.bluetooth.R
import net.ankio.bluetooth.model.SimulateMode
import net.ankio.bluetooth.model.WebdavMode
import net.ankio.bluetooth.ui.compose.components.StatusBanner
import net.ankio.bluetooth.ui.compose.components.StatusKind
import net.ankio.bluetooth.ui.compose.components.TabPageScaffold
import net.ankio.bluetooth.viewmodel.HomeViewModel
import net.ankio.theme.AnkioTheme
import net.ankio.theme.settings.SettingCardPosition
import net.ankio.theme.settings.ThemeSectionHeader
import net.ankio.theme.settings.ThemeSettingDropdown

// 👑 老大：负责和外部世界（ViewModel、Navigation）打交道，收集状态
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(),
) {
    TabPageScaffold(title = stringResource(R.string.nav_home)) {
        // 2. 把纯数据和方法传给小弟
        HomeScreenContent(
            webdavMode = viewModel.webdavMode,
            simulateMode = viewModel.simulateMode,
            onWebdavModeChange = viewModel::selectWebdavMode,
            onSimulateModeChange = viewModel::selectSimulateMode
        )
    }
}

// 👷 小弟：纯粹的 UI，只认数据，不认识 ViewModel。这就是“无状态 (Stateless)”
@Composable
fun HomeScreenContent(
    webdavMode: WebdavMode,
    simulateMode: SimulateMode,
    onWebdavModeChange: (Int) -> Unit,
    onSimulateModeChange: (Int) -> Unit,
    modifier: Modifier = Modifier // 提供 modifier 默认参数是 Compose 的好习惯
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
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        StatusBanner(
            message = stringResource(R.string.active_success),
            kind = StatusKind.Success,
            icon = Icons.Default.CloudSync,
        )

        ThemeSectionHeader(stringResource(R.string.home_section_modes))

        ThemeSettingDropdown(
            items = webdavLabels,
            selectedIndex = webdavIndex,
            onSelectedIndexChange = onWebdavModeChange, // 直接调用传进来的 Lambda
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
            onSelectedIndexChange = onSimulateModeChange, // 直接调用传进来的 Lambda
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
