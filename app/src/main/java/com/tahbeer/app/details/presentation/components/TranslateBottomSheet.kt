package com.tahbeer.app.details.presentation.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.intl.Locale
import com.tahbeer.app.R
import com.tahbeer.app.core.domain.model.TranscriptionItem
import com.tahbeer.app.core.presentation.components.LanguagePickerDialog
import com.tahbeer.app.home.presentation.components.MlkitDownloaderDialog
import com.tahbeer.app.home.presentation.settings.SettingsAction
import com.tahbeer.app.home.presentation.settings.SettingsState
import com.tahbeer.app.home.presentation.transcription_list.TranscriptionListAction
import com.tahbeer.app.home.presentation.transcription_list.TranscriptionListState

@Composable
fun TranslateBottomSheet(
    settingsState: SettingsState,
    settingsOnAction: (SettingsAction) -> Unit,
    transcriptionListState: TranscriptionListState,
    onAction: (TranscriptionListAction) -> Unit,
    transcriptionItem: TranscriptionItem
) {
    var showLanguageDialog by remember { mutableStateOf(false) }
    var selectedLanguage by remember { mutableStateOf("") }

    val availableLanguages =
        settingsState.mlkitModels.filter { it.isDownloaded && transcriptionItem.lang != it.lang }
    AnimatedContent(availableLanguages.none { it.isDownloaded }) { noDownloadedModel ->
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (noDownloadedModel) {
                Text(
                    text = stringResource(R.string.download_model_title),
                    style = MaterialTheme.typography.titleLarge
                )
                var showMlkitDialog by remember { mutableStateOf(false) }
                TextButton(
                    onClick = { showMlkitDialog = true },
                    shape = MaterialTheme.shapes.extraSmall,
                    contentPadding = PaddingValues()
                ) {
                    ListItem(
                        headlineContent = { Text(text = stringResource(R.string.settings_translate_label)) },
                        leadingContent = {
                            Icon(
                                ImageVector.vectorResource(R.drawable.translate),
                                contentDescription = null,
                            )
                        },
                        trailingContent = {
                            Icon(
                                ImageVector.vectorResource(R.drawable.chevron_right),
                                contentDescription = null
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }

                if (showMlkitDialog) {
                    MlkitDownloaderDialog(
                        languages = settingsState.mlkitModels.filterNot { it.lang == transcriptionItem.lang },
                        onAction = { settingsOnAction(it) },
                        onDismissRequest = {
                            showMlkitDialog = false
                        },
                    )
                }
            } else {
                ListItem(
                    modifier = Modifier.clickable { showLanguageDialog = true },
                    headlineContent = { Text(text = stringResource(R.string.translate_to_label)) },
                    supportingContent = {
                        Text(
                            text = Locale(selectedLanguage).platformLocale.displayLanguage,
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
                            selectedLanguage = language
                            showLanguageDialog = false
                        },
                        languages = availableLanguages.map { it.lang },
                        onDismissRequest = { showLanguageDialog = false }
                    )
                }
                AnimatedContent(transcriptionListState.translationProgress != null) { isTranslating ->
                    if (isTranslating) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            when (transcriptionListState.translationProgress != null) {
                                true -> LinearProgressIndicator(
                                    modifier = Modifier.fillMaxWidth(),
                                    progress = { transcriptionListState.translationProgress })

                                false -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            }

                            OutlinedButton(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    onAction(TranscriptionListAction.cancelTranslation)
                                })
                            { Text(stringResource(R.string.cancel_btn)) }
                        }
                    } else {
                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                if (selectedLanguage.isEmpty())
                                    return@Button

                                onAction(
                                    TranscriptionListAction.OnTranscriptTranslate(
                                        transcriptionId = transcriptionItem.id,
                                        outputLang = selectedLanguage
                                    )
                                )
                            }) { Text(stringResource(R.string.translate_menu_item)) }
                    }
                }
            }
        }
    }
}