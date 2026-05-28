package net.ankio.bluetooth.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import net.ankio.bluetooth.R
import net.ankio.bluetooth.ui.compose.components.TabPageScaffold
import net.ankio.bluetooth.ui.compose.preview.PreviewSamples
import net.ankio.theme.PreviewAllScreen
import net.ankio.theme.PreviewAllThemes
import net.ankio.theme.ThemePreviewConfig
import net.ankio.theme.ThemePreviewParameterProvider

@PreviewAllScreen
@Composable
private fun ScanScreenPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) config: ThemePreviewConfig,
) {
    PreviewAllThemes(config) {
        TabPageScaffold(
            title = stringResource(R.string.nav_scan),
            scrollContent = false,
        ) {
            ScanScreenContent(
                devices = PreviewSamples.scanDevices,
                isScanning = true,
                onToggleScan = {},
                onFilter = {},
                onDeviceClick = {},
                onDeviceRemoveAt = {},
            )
        }
    }
}
