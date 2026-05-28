package net.ankio.bluetooth.ui.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import net.ankio.bluetooth.ui.navigation.AppTab
import net.ankio.theme.compat.ThemeNavigationBar
import net.ankio.theme.compat.ThemeNavigationBarItem

/**
 * 应用主壳：仅负责系统栏适配、内容区容器与底部 [ThemeNavigationBar]。
 *
 * 该组件不持有业务路由，只通过 [content] 插槽承载页面内容，从而保持壳与业务解耦。
 *
 * @param navController 通过 CompositionLocal 下发给子页面使用（如跨页 tab 跳转）
 * @param selectedTab 当前底栏选中项
 * @param onTabSelected 底栏点击回调
 * @param content 主内容区域插槽，参数为建议的 fillMaxSize 修饰符
 */
@Composable
fun MainShell(
    navController: NavHostController,
    selectedTab: AppTab,
    onTabSelected: (AppTab) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (Modifier) -> Unit,
) {
    CompositionLocalProvider(LocalBluetoothNavHost provides navController) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars),
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                content(Modifier.fillMaxSize())
            }

            ThemeNavigationBar {
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
}
