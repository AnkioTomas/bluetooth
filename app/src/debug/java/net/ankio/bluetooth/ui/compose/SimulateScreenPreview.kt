package net.ankio.bluetooth.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import net.ankio.bluetooth.R
import net.ankio.bluetooth.ui.compose.components.TabPageScaffold
import net.ankio.theme.PreviewAllScreen
import net.ankio.theme.PreviewAllThemes
import net.ankio.theme.ThemePreviewConfig
import net.ankio.theme.ThemePreviewParameterProvider

@PreviewAllScreen
@Composable
private fun SimulateScreenPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) config: ThemePreviewConfig,
) {
    PreviewAllThemes(config) {
        TabPageScaffold(title = stringResource(R.string.nav_simulate)) {
            SimulateScreenContent(
                prefMac = "AA:BB:CC:DD:EE:FF",
                prefData = "0201060AFF4C000215",
                prefRssi = "-65",
                onPrefMacChange = {},
                onPrefDataChange = {},
                onPrefRssiChange = {},
            )
        }
    }
}
