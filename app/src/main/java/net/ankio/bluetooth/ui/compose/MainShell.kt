package net.ankio.bluetooth.ui.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import net.ankio.bluetooth.ui.compose.preview.PreviewSamples
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import net.ankio.bluetooth.ui.navigation.AppTab
import net.ankio.bluetooth.viewmodel.MainViewModel
import net.ankio.bluetooth.viewmodel.ScanViewModel
import net.ankio.theme.PreviewAll
import net.ankio.theme.PreviewAllThemes
import net.ankio.theme.ThemePreviewConfig
import net.ankio.theme.ThemePreviewParameterProvider
import net.ankio.theme.ThemeSettings
import net.ankio.theme.compat.ThemeNavigationBar
import net.ankio.theme.compat.ThemeNavigationBarItem
import net.ankio.theme.compat.ThemeTopAppBar
import net.ankio.theme.toast.ThemeToast

/**
 * 应用主壳：顶部栏 + 内容区 + [ThemeNavigationBar] 底部导航。
 * 预览见 [MainShellPreview]。
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
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: AppTab.Home.route
    val currentTab = AppTab.entries.firstOrNull { it.route == currentRoute } ?: AppTab.Home

    fun navigateTo(tab: AppTab) {
        navController.navigate(tab.route) {
            popUpTo(navController.graph.startDestinationId) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    MainShellScaffold(
        selectedTab = currentTab,
        onTabSelected = { tab ->
            if (tab == AppTab.Scan) {
                onEnsureScanReady { navigateTo(tab) }
            } else {
                navigateTo(tab)
            }
        },
    ) {
        NavHost(
            navController = navController,
            startDestination = AppTab.Home.route,
            modifier = Modifier.fillMaxSize(),
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
                val deviceSelectedMessage = stringResource(net.ankio.bluetooth.R.string.device_selected_to_simulate)
                ScanScreen(
                    viewModel = scanViewModel,
                    onBack = {},
                    showBackButton = false,
                    onDeviceSelected = {
                        mainViewModel.load()
                        navigateTo(AppTab.Simulate)
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
    }
}

@Composable
fun MainShellScaffold(
    selectedTab: AppTab,
    onTabSelected: (AppTab) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars),
    ) {
        ThemeTopAppBar(
            title = stringResource(selectedTab.titleRes),
            modifier = Modifier.fillMaxWidth(),
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            content()
        }

        ThemeNavigationBar(
            floating = false,
        ) {
            AppTab.entries.forEach { tab ->
                ThemeNavigationBarItem(
                    selected = selectedTab == tab,
                    onClick = { onTabSelected(tab) },
                    icon = tab.icon,
                    label = stringResource(tab.titleRes),
                )
            }
        }
    }
}

@Composable
fun MainShellPreviewContent() {
    var selectedTab by remember { mutableStateOf(AppTab.Home) }
    MainShellScaffold(
        selectedTab = selectedTab,
        onTabSelected = { selectedTab = it },
    ) {
        when (selectedTab) {
            AppTab.Home -> HomeScreenContent(uiState = PreviewSamples.mainUiState)
            AppTab.Simulate -> SimulateScreenContent(
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
            AppTab.Scan -> ScanScreenContent(
                uiState = PreviewSamples.scanUiState,
                onBack = {},
                showBackButton = false,
                onToggleScan = {},
                onOpenFilter = {},
                onShowHistory = {},
                onDeviceClick = {},
                onDeviceLongClick = {},
                onFilterDismiss = {},
            )
            AppTab.Settings -> SettingsScreenPreviewContent()
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
