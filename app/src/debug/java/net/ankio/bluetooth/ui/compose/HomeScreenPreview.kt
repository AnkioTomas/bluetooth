package net.ankio.bluetooth.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import net.ankio.bluetooth.R
import net.ankio.bluetooth.model.SimulateMode
import net.ankio.bluetooth.model.WebdavMode
import net.ankio.bluetooth.ui.compose.components.TabPageScaffold
import net.ankio.theme.PreviewAllScreen
import net.ankio.theme.PreviewAllThemes
import net.ankio.theme.ThemePreviewConfig
import net.ankio.theme.ThemePreviewParameterProvider

@PreviewAllScreen
@Composable
private fun HomeScreenPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) config: ThemePreviewConfig,
) {
    PreviewAllThemes(config) {
        HomeScreenContent(
            webdavMode = WebdavMode.None,
            simulateMode = SimulateMode.None,
            onWebdavModeChange = {},
            onSimulateModeChange = {}
        )
    }
}
