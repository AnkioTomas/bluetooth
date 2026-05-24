package net.ankio.bluetooth.ui.compose

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import net.ankio.theme.AnkioTheme
import net.ankio.theme.AutoTheme
import net.ankio.theme.LocalUiMode
import net.ankio.theme.ThemeSettings
import net.ankio.theme.UiMode
import net.ankio.theme.compat.ThemeSurface

@Composable
fun BluetoothAppTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalUiMode provides UiMode.fromValue(ThemeSettings.uiMode),
    ) {
        AutoTheme {
            ThemeSurface(
                modifier = Modifier.fillMaxSize(),
                color = AnkioTheme.colorScheme.surface,
            ) {
                content()
            }
        }
    }
}
