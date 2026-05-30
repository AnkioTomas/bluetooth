package net.ankio.bluetooth.ui.compose.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewParameter
import net.ankio.theme.PreviewAll
import net.ankio.theme.PreviewAllThemes
import net.ankio.theme.ThemePreviewConfig
import net.ankio.theme.ThemePreviewParameterProvider

@PreviewAll
@Composable
private fun StatusBarPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) config: ThemePreviewConfig,
) {
    PreviewAllThemes(config) {
        StatusBanner(
            message = "Plugin activated",
            kind = StatusKind.Error,
            icon = Icons.Default.Error,
        )
    }
}
