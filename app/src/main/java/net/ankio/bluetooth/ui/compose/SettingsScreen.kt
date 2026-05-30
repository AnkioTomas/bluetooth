package net.ankio.bluetooth.ui.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import net.ankio.bluetooth.R
import net.ankio.bluetooth.viewmodel.SettingsViewModel
import net.ankio.theme.AnkioTheme
import net.ankio.theme.settings.SettingCardPosition
import net.ankio.theme.settings.ThemeSectionHeader
import net.ankio.theme.settings.ThemeSettingDropdown
import net.ankio.theme.settings.UiSettingsScreen
import net.ankio.utils.LangList
import net.ankio.webdav.lib.ui.WebDavSettingsScreen
import java.util.Locale

@Composable
fun SettingsScreen(
    onRecreateForLocale: () -> Unit,
    onThemeChanged: () -> Unit,
    viewModel: SettingsViewModel = viewModel(),
) {
    SettingsScreenContent(
        languageTag = viewModel.languageTag,
        onRecreateForLocale = {
            viewModel.languageTag = it
            onRecreateForLocale()
        },
        onThemeChanged = onThemeChanged,
    )
}

@Composable
fun SettingsScreenContent(
    languageTag: String,
    onRecreateForLocale: (String) -> Unit,
    onThemeChanged: () -> Unit,
) {
    val languageOptions = LangList.LOCALES.map { tag ->
        if (tag == "SYSTEM") {
            stringResource(R.string.lang_follow_system)
        } else {
            val locale = Locale.forLanguageTag(tag)
            locale.getDisplayName(locale)
        }
    }
    val languageIndex = LangList.LOCALES.toList().indexOf(languageTag).coerceAtLeast(0)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        WebDavSettingsScreen()

        ThemeSectionHeader(stringResource(R.string.setting_general))
        ThemeSettingDropdown(
            items = languageOptions,
            selectedIndex = languageIndex,
            onSelectedIndexChange = { index ->
                val tag = LangList.LOCALES.getOrElse(index.coerceIn(LangList.LOCALES.indices)) { "SYSTEM" }
                onRecreateForLocale(tag)
            },
            title = stringResource(R.string.setting_lang),
            startAction = {
                Icon(
                    imageVector = Icons.Filled.Language,
                    contentDescription = null,
                    tint = AnkioTheme.colorScheme.primary,
                )
            },
            position = SettingCardPosition.Single,
        )

        UiSettingsScreen(
            onThemeChanged = onThemeChanged,
        )
    }
}
