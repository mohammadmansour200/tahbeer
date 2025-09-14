package com.tahbeer.app.home.presentation.settings

import android.os.Build
import android.view.Gravity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.os.LocaleListCompat
import com.tahbeer.app.R
import com.tahbeer.app.core.presentation.components.LanguagePickerDialog
import com.tahbeer.app.core.presentation.utils.findActivity
import com.tahbeer.app.home.presentation.components.WhisperModelItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBottomSheet(
    state: SettingsState,
    onAction: (SettingsAction) -> Unit
) {
    val context = LocalContext.current

    // Whisper model managing
    var showModelBottomSheet by remember { mutableStateOf(false) }
    TextButton(
        onClick = { showModelBottomSheet = true },
        shape = MaterialTheme.shapes.extraSmall,
        contentPadding = PaddingValues()
    ) {
        ListItem(
            headlineContent = { Text(text = stringResource(R.string.settings_model_label)) },
            supportingContent = {
                Text(
                    text = stringResource(R.string.settings_model_desc),
                    modifier = Modifier.alpha(0.8f)
                )
            },
            leadingContent = {
                Icon(
                    ImageVector.vectorResource(R.drawable.tts),
                    contentDescription = null,
                )
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )
    }

    if (showModelBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                showModelBottomSheet = false
            },
        ) {
            Column {
                state.whisperModels.forEach { model ->
                    WhisperModelItem(model, { onAction(it) })
                }
            }
        }
    }

    HorizontalDivider(modifier = Modifier.alpha(0.8f))
    Column {
        //Theme settings
        var showDialog by remember { mutableStateOf(false) }
        val localizedRadioLabel = { label: String ->
            when (label) {
                "dark" -> R.string.settings_theme_dark_option
                "light" -> R.string.settings_theme_light_option
                else -> R.string.settings_theme_system_option
            }
        }
        TextButton(
            onClick = { showDialog = true },
            shape = MaterialTheme.shapes.extraSmall,
            contentPadding = PaddingValues()
        ) {
            ListItem(
                headlineContent = { Text(text = stringResource(R.string.settings_theme_label)) },
                supportingContent = {
                    Text(
                        text = stringResource(localizedRadioLabel(state.theme)),
                        modifier = Modifier.alpha(0.8f)
                    )
                },
                leadingContent = {
                    Icon(
                        ImageVector.vectorResource(R.drawable.sun),
                        null,
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
        }
        if (showDialog) {
            Dialog(onDismissRequest = { showDialog = false }) {
                val dialogWindowProvider = LocalView.current.parent as DialogWindowProvider
                dialogWindowProvider.window.setGravity(Gravity.BOTTOM)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .padding(start = 24.dp, top = 24.dp, end = 24.dp, bottom = 16.dp),
                ) {
                    Spacer(Modifier.size(16.dp))
                    Text(
                        text = stringResource(R.string.settings_theme_alert_title),
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    val radioOptions = listOf("system", "light", "dark")

                    Column(modifier = Modifier.selectableGroup()) {
                        radioOptions.forEach { text ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .height(56.dp)
                                    .selectable(
                                        selected = (text == state.theme),
                                        onClick = {
                                            onAction(
                                                SettingsAction.OnThemeChange(
                                                    text
                                                )
                                            )
                                        },
                                        role = Role.RadioButton
                                    ),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    modifier = Modifier.padding(start = 16.dp),
                                    selected = (text == state.theme),
                                    onClick = null
                                )
                                Text(
                                    text = stringResource(localizedRadioLabel(text)),
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.padding(start = 16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }


        //Dynamic color settings
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            TextButton(
                onClick = { onAction(SettingsAction.OnDynamicColorChange(!state.dynamicColorsEnabled)) },
                shape = MaterialTheme.shapes.extraSmall,
                contentPadding = PaddingValues()
            ) {
                ListItem(
                    headlineContent = { Text(text = stringResource(R.string.settings_dynamic_color_label)) },
                    trailingContent = {
                        Switch(
                            checked = state.dynamicColorsEnabled,
                            onCheckedChange = { onAction(SettingsAction.OnDynamicColorChange(!state.dynamicColorsEnabled)) },
                        )
                    },
                    leadingContent = {
                        Icon(
                            ImageVector.vectorResource(R.drawable.palette),
                            null,
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }

            HorizontalDivider(modifier = Modifier.alpha(0.8f))
        }

        //Language settings
        var showLanguageDialog by remember { mutableStateOf(false) }
        val appLocales = listOf("ar", "en")
        val onLocaleClick = { lang: String ->
            context.findActivity()?.runOnUiThread {
                val appLocale =
                    LocaleListCompat.forLanguageTags(lang)
                AppCompatDelegate.setApplicationLocales(appLocale)
            }
        }

        ListItem(
            modifier = Modifier.clickable { showLanguageDialog = true },
            headlineContent = { Text(text = stringResource(R.string.settings_lang_label)) },
            supportingContent = {
                Text(
                    text = Locale(Locale.current.language).platformLocale.displayLanguage,
                    modifier = Modifier.alpha(0.8f)
                )
            },
            trailingContent = {
                Icon(
                    ImageVector.vectorResource(R.drawable.chevron_right),
                    contentDescription = null
                )
            },
            leadingContent = {
                Icon(
                    ImageVector.vectorResource(R.drawable.language),
                    null,
                )
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )
        if (showLanguageDialog) {
            LanguagePickerDialog(
                onLanguageSelected = { language ->
                    onLocaleClick(language)
                    showLanguageDialog = false
                },
                languages = appLocales,
                onDismissRequest = { showLanguageDialog = false }
            )
        }
    }
}


