package net.ankio.bluetooth.ui.compose

import android.content.ComponentName
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.core.text.HtmlCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.ankio.bluetooth.App
import net.ankio.bluetooth.BuildConfig
import net.ankio.bluetooth.R
import net.ankio.bluetooth.ui.compose.components.SwitchRow
import net.ankio.bluetooth.utils.CustomTabsHelper
import net.ankio.bluetooth.utils.LocaleDelegate
import net.ankio.bluetooth.utils.SpUtils
import net.ankio.bluetooth.viewmodel.MainViewModel
import net.ankio.theme.AnkioTheme
import net.ankio.theme.PreviewAll
import net.ankio.theme.PreviewAllThemes
import net.ankio.theme.ThemePreviewConfig
import net.ankio.theme.ThemePreviewParameterProvider
import net.ankio.theme.compat.ThemeCard
import net.ankio.theme.compat.ThemeIcon
import net.ankio.theme.compat.ThemeSmallTitle
import net.ankio.theme.compat.ThemeSwitch
import net.ankio.theme.compat.ThemeText
import net.ankio.theme.compat.ThemeTextField
import net.ankio.theme.settings.UiSettingsScreen
import net.ankio.utils.LangList
import java.util.Locale

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onRecreateForLocale: () -> Unit,
    onThemeChanged: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var hookLogEnabled by remember { mutableStateOf(SpUtils.getBoolean("pref_hook_log_enabled", false)) }
    var hideIcon by remember { mutableStateOf(SpUtils.getBoolean("hide_icon", false)) }
    val languageTag = remember { mutableStateOf(SpUtils.getString("setting_language", "SYSTEM")) }
    val languageOptions = remember {
        LangList.LOCALES.map { tag ->
            if (tag == "SYSTEM") {
                context.getString(R.string.lang_follow_system)
            } else {
                val locale = Locale.forLanguageTag(tag)
                HtmlCompat.fromHtml(locale.getDisplayName(locale), HtmlCompat.FROM_HTML_MODE_LEGACY).toString()
            }
        }
    }
    val languageLabels = remember { LangList.LOCALES.zip(languageOptions).toMap() }
    var showLanguageDialog by remember { mutableStateOf(false) }

    fun setHideIcon(checked: Boolean) {
        hideIcon = checked
        SpUtils.putBoolean("hide_icon", checked)
        val component = ComponentName(context, BuildConfig.APPLICATION_ID + ".MainActivityLauncher")
        val status = if (checked) {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        }
        context.packageManager.setComponentEnabledSetting(component, status, PackageManager.DONT_KILL_APP)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ThemeSmallTitle(stringResource(R.string.setting_webdav))
        ThemeCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SwitchRow(
                    title = stringResource(R.string.webdav_enable),
                    checked = uiState.prefEnableWebdav,
                    onCheckedChange = viewModel::updatePrefEnableWebdav,
                )
                ThemeTextField(
                    value = uiState.webdavServer,
                    onValueChange = viewModel::updateWebdavServer,
                    label = stringResource(R.string.webdav_data),
                    modifier = Modifier.fillMaxWidth(),
                )
                ThemeTextField(
                    value = uiState.webdavUsername,
                    onValueChange = viewModel::updateWebdavUsername,
                    label = stringResource(R.string.webdav_username),
                    modifier = Modifier.fillMaxWidth(),
                )
                ThemeTextField(
                    value = uiState.webdavPassword,
                    onValueChange = viewModel::updateWebdavPassword,
                    label = stringResource(R.string.webdav_password),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        ThemeSmallTitle(stringResource(R.string.setting_general))
        ThemeCard(
            modifier = Modifier.fillMaxWidth(),
            onClick = { showLanguageDialog = true },
        ) {
            SettingItemRow(
                icon = {
                    ThemeIcon(Icons.Default.Language, contentDescription = null, tint = AnkioTheme.colorScheme.primary)
                },
                title = stringResource(R.string.setting_lang),
                subtitle = languageLabels[languageTag.value] ?: languageTag.value,
            )
        }
        ThemeCard(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                CustomTabsHelper.launchUrlOrCopy(context, context.getString(R.string.translation_url))
            },
        ) {
            SettingItemRow(
                icon = {
                    ThemeIcon(Icons.Default.Translate, contentDescription = null, tint = AnkioTheme.colorScheme.primary)
                },
                title = stringResource(R.string.setting_translation),
                subtitle = stringResource(R.string.setting_help_translation),
            )
        }
        ThemeCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                SettingSwitchRow(
                    icon = {
                        ThemeIcon(Icons.Default.VisibilityOff, contentDescription = null, tint = AnkioTheme.colorScheme.primary)
                    },
                    title = stringResource(R.string.hook_log_title),
                    subtitle = stringResource(R.string.hook_log_desc),
                    checked = hookLogEnabled,
                    onCheckedChange = {
                        hookLogEnabled = it
                        SpUtils.putBoolean("pref_hook_log_enabled", it)
                    },
                )
                SettingSwitchRow(
                    icon = {
                        ThemeIcon(Icons.Default.VisibilityOff, contentDescription = null, tint = AnkioTheme.colorScheme.primary)
                    },
                    title = stringResource(R.string.hide_icon),
                    checked = hideIcon,
                    onCheckedChange = ::setHideIcon,
                )
            }
        }

        ThemeSmallTitle(stringResource(R.string.setting_skin))
        UiSettingsScreen(
            modifier = Modifier.fillMaxWidth(),
            onThemeChanged = onThemeChanged,
        )
    }

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(stringResource(R.string.setting_lang)) },
            text = {
                Column {
                    LangList.LOCALES.forEachIndexed { index, tag ->
                        TextButton(
                            onClick = {
                                languageTag.value = tag
                                SpUtils.putString("setting_language", tag)
                                LocaleDelegate.defaultLocale = App.getLocale()
                                showLanguageDialog = false
                                onRecreateForLocale()
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(languageOptions[index], modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            },
            confirmButton = {},
        )
    }
}

@Composable
private fun SettingItemRow(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.padding(end = 12.dp)) { icon() }
        Column(modifier = Modifier.weight(1f)) {
            ThemeText(text = title, style = AnkioTheme.textStyles.title4, color = AnkioTheme.colorScheme.onSurface)
            if (subtitle != null) {
                ThemeText(
                    text = subtitle,
                    style = AnkioTheme.textStyles.footnote1,
                    color = AnkioTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SettingSwitchRow(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.padding(end = 12.dp)) { icon() }
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            ThemeText(text = title, style = AnkioTheme.textStyles.title4, color = AnkioTheme.colorScheme.onSurface)
            if (subtitle != null) {
                ThemeText(
                    text = subtitle,
                    style = AnkioTheme.textStyles.footnote1,
                    color = AnkioTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        ThemeSwitch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun SettingsScreenPreviewContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ThemeSmallTitle(stringResource(R.string.setting_webdav))
        ThemeCard(modifier = Modifier.fillMaxWidth()) {
            ThemeText(
                text = "https://dav.example.com/dav/",
                style = AnkioTheme.textStyles.main,
                color = AnkioTheme.colorScheme.onSurface,
                modifier = Modifier.padding(16.dp),
            )
        }
        ThemeSmallTitle(stringResource(R.string.setting_general))
        ThemeCard(modifier = Modifier.fillMaxWidth()) {
            ThemeText(
                text = stringResource(R.string.setting_lang),
                style = AnkioTheme.textStyles.main,
                color = AnkioTheme.colorScheme.onSurface,
                modifier = Modifier.padding(16.dp),
            )
        }
        ThemeSmallTitle(stringResource(R.string.setting_skin))
    }
}

@PreviewAll
@Composable
private fun SettingsScreenPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) config: ThemePreviewConfig,
) {
    PreviewAllThemes(config) {
        SettingsScreenPreviewContent()
    }
}
