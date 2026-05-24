package net.ankio.bluetooth.ui.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.ankio.bluetooth.R
import net.ankio.bluetooth.ui.compose.components.StatusBanner
import net.ankio.bluetooth.ui.compose.preview.PreviewSamples
import net.ankio.bluetooth.viewmodel.MainUiState
import net.ankio.bluetooth.viewmodel.MainViewModel
import net.ankio.theme.AnkioTheme
import net.ankio.theme.PreviewAll
import net.ankio.theme.PreviewAllThemes
import net.ankio.theme.ThemePreviewConfig
import net.ankio.theme.ThemePreviewParameterProvider
import net.ankio.theme.compat.ThemeCard
import net.ankio.theme.compat.ThemeText

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    versionName: String,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    Column(modifier = Modifier.fillMaxSize()) {
        HomeScreenContent(uiState = uiState)
    }
}

@Composable
fun HomeScreenContent(
    uiState: MainUiState,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        StatusBanner(
            message = stringResource(uiState.status.messageRes),
            kind = uiState.status.kind,
            icon = uiState.status.icon,
        )

        ThemeCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeText(
                    text = if (uiState.prefEnable) {
                        stringResource(R.string.status_mock_on)
                    } else {
                        stringResource(R.string.status_mock_off)
                    },
                    style = AnkioTheme.textStyles.main,
                    color = AnkioTheme.colorScheme.onSurface,
                )
                ThemeText(
                    text = if (uiState.prefAsSender) {
                        stringResource(R.string.status_sender_on)
                    } else {
                        stringResource(R.string.status_sender_off)
                    },
                    style = AnkioTheme.textStyles.footnote1,
                    color = AnkioTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

    }
}

@PreviewAll
@Composable
private fun HomeScreenPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) config: ThemePreviewConfig,
) {
    PreviewAllThemes(config) {
        HomeScreenContent(uiState = PreviewSamples.mainUiState)
    }
}
