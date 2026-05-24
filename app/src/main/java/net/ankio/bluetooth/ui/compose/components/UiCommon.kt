package net.ankio.bluetooth.ui.compose.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import net.ankio.bluetooth.viewmodel.StatusKind
import net.ankio.theme.AnkioTheme
import net.ankio.theme.LocalColorMode
import net.ankio.theme.LocalUiMode
import net.ankio.theme.PreviewAll
import net.ankio.theme.PreviewAllThemes
import net.ankio.theme.ThemePreviewConfig
import net.ankio.theme.ThemePreviewParameterProvider
import net.ankio.theme.UiMode
import net.ankio.theme.compat.ThemeCard
import net.ankio.theme.compat.ThemeIcon
import net.ankio.theme.compat.ThemeSwitch
import net.ankio.theme.compat.ThemeText

@Composable
fun StatusBanner(
    message: String,
    kind: StatusKind,
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
    val colors = AnkioTheme.colorScheme
    val semantic = colors.semantic
    val usePrimaryForSuccess = LocalColorMode.current.isMonet || LocalUiMode.current == UiMode.Material
    val (contentColor, containerColor) = when (kind) {
        StatusKind.Error -> semantic.error.text to semantic.error.bg
        StatusKind.Success -> if (usePrimaryForSuccess) {
            colors.primary to colors.primaryContainer
        } else {
            semantic.success.text to semantic.success.bg
        }
        StatusKind.Warning -> semantic.warning.text to semantic.warning.bg
    }

    ThemeCard(
        modifier = modifier.fillMaxWidth(),
        containerColor = containerColor,
        contentColor = contentColor,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ThemeIcon(imageVector = icon, contentDescription = null, tint = contentColor)
            ThemeText(text = message, style = AnkioTheme.textStyles.main, color = contentColor)
        }
    }
}

@Composable
fun SwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ThemeText(
            text = title,
            style = AnkioTheme.textStyles.main,
            color = AnkioTheme.colorScheme.onSurface,
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp),
        )
        ThemeSwitch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@PreviewAll
@Composable
fun StatusBarPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) config: ThemePreviewConfig
) {
    // Preview different status types
    PreviewAllThemes(config) {
        StatusBanner(
            message = "激活",
            kind = StatusKind.Error,
            icon = androidx.compose.material.icons.Icons.Default.Error
        )
    }

}
