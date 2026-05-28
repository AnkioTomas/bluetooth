package net.ankio.bluetooth.ui.compose.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import net.ankio.theme.compat.ThemeTopAppBar
import net.ankio.theme.compat.ThemeTopAppBarTitleAlignment
import net.ankio.theme.compat.rememberThemeTopAppBarScroll

/**
 * Tab 子页通用骨架：本页自管 [ThemeTopAppBar] 与内容区。
 *
 * @param scrollContent 为 true 时由本组件提供唯一纵向滚动（子页勿再套 verticalScroll / LazyColumn 外的 Column+scroll）。
 * 列表页（如扫描）或已自带滚动的页面应传 false。
 * @param collapseToolbarOnScroll 为 true 时大标题随内容滚动折叠到顶栏（与 [scrollContent] 独立；
 *   需在内容区使用可嵌套滚动容器，如 LazyColumn、`verticalScroll`）。
 */
@Composable
fun TabPageScaffold(
    title: String,
    modifier: Modifier = Modifier,
    scrollContent: Boolean = true,
    collapseToolbarOnScroll: Boolean = true,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable () -> Unit,
) {
    val scroll = rememberThemeTopAppBarScroll(collapseOnScroll = collapseToolbarOnScroll)
    val nestedScrollModifier = scroll?.let { Modifier.nestedScroll(it.nestedScrollConnection) } ?: Modifier
    val contentScroll = rememberScrollState()

    Column(modifier = modifier.fillMaxSize()) {
        ThemeTopAppBar(
            title = title,
            largeTitle = title,
            titleAlignment = ThemeTopAppBarTitleAlignment.Start,
            scroll = scroll,
            modifier = Modifier.fillMaxWidth(),
            navigationIcon = navigationIcon,
            actions = actions,
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .then(nestedScrollModifier),
        ) {
            if (scrollContent) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(contentScroll),
                ) {
                    content()
                }
            } else {
                content()
            }
        }
    }
}
