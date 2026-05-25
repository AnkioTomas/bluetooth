package net.ankio.bluetooth.ui.compose.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import net.ankio.theme.compat.ThemeTopAppBar
import net.ankio.theme.compat.ThemeTopAppBarTitleAlignment
import net.ankio.theme.compat.rememberThemeTopAppBarScroll

/**
 * Tab 子页通用骨架：本页自管 [ThemeTopAppBar] 与可折叠滚动内容区。
 */
@Composable
fun TabPageScaffold(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable () -> Unit,
) {
    val scroll = rememberThemeTopAppBarScroll(collapseOnScroll = true)
    val nestedScrollModifier = scroll?.let { Modifier.nestedScroll(it.nestedScrollConnection) } ?: Modifier

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
            content()
        }
    }
}
