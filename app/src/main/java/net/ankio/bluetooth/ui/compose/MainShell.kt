package net.ankio.bluetooth.ui.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import net.ankio.bluetooth.R
import net.ankio.bluetooth.ui.compose.components.TabPageScaffold
import net.ankio.bluetooth.ui.compose.preview.PreviewSamples
import net.ankio.bluetooth.ui.navigation.AppTab
import net.ankio.bluetooth.viewmodel.MainViewModel
import net.ankio.bluetooth.viewmodel.ScanViewModel
import net.ankio.theme.PreviewAll
import net.ankio.theme.PreviewAllThemes
import net.ankio.theme.ThemePreviewConfig
import net.ankio.theme.ThemePreviewParameterProvider
import net.ankio.theme.compat.ThemeNavigationBar
import net.ankio.theme.compat.ThemeNavigationBarItem
import net.ankio.theme.toast.ThemeToast

/**
 * 应用主壳：仅负责底部 [ThemeNavigationBar] 与 Tab 路由，顶栏与内容由各子页面自行处理。
 */
@Composable
fun MainShell(
    mainViewModel: MainViewModel,
    scanViewModel: ScanViewModel,
    versionName: String,
    onRecreateForLocale: () -> Unit,
    onThemeChanged: () -> Unit,
    onEnsureScanReady: (onReady: () -> Unit) -> Unit,
) {
    val navController = rememberNavController()
    val selectedTab = navController.currentTab()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars),
    ) {
        Box(modifier = Modifier.weight(1f)) {
            MainTabNavHost(
                navController = navController,
                mainViewModel = mainViewModel,
                scanViewModel = scanViewModel,
                versionName = versionName,
                onRecreateForLocale = onRecreateForLocale,
                onThemeChanged = onThemeChanged,
            )
        }

        ThemeNavigationBar {
            AppTab.entries.forEach { tab ->
                ThemeNavigationBarItem(
                    selected = selectedTab == tab,
                    onClick = {
                        if (tab == AppTab.Scan) {
                            onEnsureScanReady { navController.navigateToTab(tab) }
                        } else {
                            navController.navigateToTab(tab)
                        }
                    },
                    icon = tab.icon,
                    label = stringResource(tab.titleRes),
                )
            }
        }
    }
}

@Composable
private fun MainTabNavHost(
    navController: NavHostController,
    mainViewModel: MainViewModel,
    scanViewModel: ScanViewModel,
    versionName: String,
    onRecreateForLocale: () -> Unit,
    onThemeChanged: () -> Unit,
) {
    NavHost(
        navController = navController,
        startDestination = AppTab.Home.route,
        modifier = Modifier.fillMaxSize(),
    ) {
        mainTabRoutes(
            mainViewModel = mainViewModel,
            scanViewModel = scanViewModel,
            versionName = versionName,
            onRecreateForLocale = onRecreateForLocale,
            onThemeChanged = onThemeChanged,
            onNavigateToTab = navController::navigateToTab,
        )
    }
}

private fun NavGraphBuilder.mainTabRoutes(
    mainViewModel: MainViewModel,
    scanViewModel: ScanViewModel,
    versionName: String,
    onRecreateForLocale: () -> Unit,
    onThemeChanged: () -> Unit,
    onNavigateToTab: (AppTab) -> Unit,
) {
    composable(AppTab.Home.route) {
        HomeScreen(
            viewModel = mainViewModel,
            versionName = versionName,
        )
    }
    composable(AppTab.Simulate.route) {
        SimulateScreen(viewModel = mainViewModel)
    }
    composable(AppTab.Scan.route) {
        val deviceSelectedMessage = stringResource(R.string.device_selected_to_simulate)
        ScanScreen(
            viewModel = scanViewModel,
            onDeviceSelected = {
                mainViewModel.load()
                onNavigateToTab(AppTab.Simulate)
                ThemeToast.show(deviceSelectedMessage, ThemeToast.Style.Success)
            },
        )
    }
    composable(AppTab.Settings.route) {
        SettingsScreen(
            viewModel = mainViewModel,
            onRecreateForLocale = onRecreateForLocale,
            onThemeChanged = onThemeChanged,
        )
    }
}

@Composable
private fun NavHostController.currentTab(): AppTab {
    val backStackEntry by currentBackStackEntryAsState()
    val route = backStackEntry?.destination?.route
    return AppTab.entries.firstOrNull { it.route == route } ?: AppTab.Home
}

private fun NavHostController.navigateToTab(tab: AppTab) {
    navigate(tab.route) {
        popUpTo(graph.startDestinationId) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun MainShellPreviewContent() {
    var selectedTab by remember { mutableStateOf(AppTab.Home) }
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) {
            when (selectedTab) {
                AppTab.Home -> TabPageScaffold(title = stringResource(R.string.nav_home)) {
                    HomeScreenContent(
                        uiState = PreviewSamples.mainUiState,
                        onWebdavModeChange = {},
                        onSimulateModeChange = {},
                    )
                }
                AppTab.Simulate -> TabPageScaffold(title = stringResource(R.string.nav_simulate)) {
                    SimulateScreenContent(
                        uiState = PreviewSamples.mainUiState,
                        onPrefMacChange = {},
                        onPrefDataChange = {},
                        onPrefRssiChange = {},
                    )
                }
                AppTab.Scan -> ScanScreenContent(
                    uiState = PreviewSamples.scanUiState,
                    onToggleScan = {},
                    onDeviceClick = {},
                    onDeviceLongClick = {},
                )
                AppTab.Settings -> TabPageScaffold(title = stringResource(R.string.nav_settings)) {
                    SettingsScreenPreviewContent()
                }
            }
        }
        ThemeNavigationBar {
            AppTab.entries.forEach { tab ->
                ThemeNavigationBarItem(
                    selected = selectedTab == tab,
                    onClick = { selectedTab = tab },
                    icon = tab.icon,
                    label = stringResource(tab.titleRes),
                )
            }
        }
    }
}

@PreviewAll
@Composable
private fun MainShellPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) config: ThemePreviewConfig,
) {
    PreviewAllThemes(config) {
        MainShellPreviewContent()
    }
}
