package com.tahbeer.app.home.presentation.components

import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import com.tahbeer.app.R
import com.tahbeer.app.core.domain.CoreConstants.SUPPORTED_LANGUAGES
import com.tahbeer.app.core.presentation.components.LanguagePickerDialog
import com.tahbeer.app.core.utils.isSubtitle
import com.tahbeer.app.home.domain.model.WhisperModel
import com.tahbeer.app.home.presentation.settings.SettingsAction
import com.tahbeer.app.home.presentation.transcription_list.TranscriptionListAction
import kotlinx.coroutines.launch

@Composable
fun StartTranscriptionBottomSheet(
    settingsOnAction: (SettingsAction) -> Unit,
    transcriptionListOnAction: (TranscriptionListAction) -> Unit,
    pickedUri: Uri,
    whisperModels: List<WhisperModel>,
    snackbarHostState: SnackbarHostState
) {
    val context = LocalContext.current
    val snackScope = rememberCoroutineScope()
    val type = context.contentResolver.getType(pickedUri)
    val isMedia = type?.isSubtitle() == false

    AnimatedContent(whisperModels.none { it.isDownloaded }) { noDownloadedModel ->
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // No models downloaded AND we need one (it's media)
            if (noDownloadedModel && isMedia) {
                Text(
                    text = stringResource(R.string.download_model_title),
                    style = MaterialTheme.typography.titleLarge
                )
                Column {
                    whisperModels.forEach { model ->
                        WhisperModelItem(model, { settingsOnAction(it) })
                    }
                }
            } else {
                var selectedModel by remember {
                    mutableStateOf(
                        if (isMedia && !noDownloadedModel) {
                            whisperModels.first { it.isDownloaded }
                        } else {
                            null
                        }
                    )
                }

                var selectedLanguage by rememberSaveable { mutableStateOf("") }
                var showLanguageDialog by remember { mutableStateOf(false) }

                // Model Picker (Only show if it's media and we have models)
                if (isMedia && selectedModel != null) {
                    Column(modifier = Modifier.selectableGroup()) {
                        Text(
                            text = stringResource(R.string.select_a_model),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .padding(top = 16.dp, bottom = 8.dp),
                        )
                        whisperModels.filter { it.isDownloaded }.forEach { model ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .height(56.dp)
                                    .selectable(
                                        selected = (model.name == selectedModel!!.name),
                                        onClick = { selectedModel = model },
                                        role = Role.RadioButton
                                    ),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ListItem(
                                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                    leadingContent = {
                                        RadioButton(
                                            selected = (model == selectedModel),
                                            onClick = { selectedModel = model },
                                        )
                                    },
                                    headlineContent = {
                                        Text(
                                            text = stringResource(
                                                if (model.enOnly) R.string.settings_model_en_title else R.string.settings_model_multilingual_title
                                            ),
                                            style = MaterialTheme.typography.bodyLarge,
                                        )
                                    },
                                    supportingContent = {
                                        Text(
                                            text = stringResource(model.description),
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                    },
                                )
                            }
                        }
                    }
                }

                // Language Picker
                // Shows if:
                // - It's a subtitle (!isMedia)
                // - It's media AND the selected model is not English-only
                AnimatedVisibility(selectedModel?.enOnly != true || !isMedia) {
                    HorizontalDivider()
                    ListItem(
                        colors = ListItemDefaults.colors(
                            containerColor = Color.Transparent
                        ),
                        modifier = Modifier.clickable { showLanguageDialog = true },
                        headlineContent = { Text(stringResource(R.string.language)) },
                        supportingContent = {
                            Text(
                                if (selectedLanguage.isEmpty()) stringResource(R.string.language_desc) else Locale(
                                    selectedLanguage
                                ).platformLocale.displayLanguage
                            )
                        },
                        trailingContent = {
                            Icon(
                                ImageVector.vectorResource(R.drawable.chevron_right),
                                contentDescription = null
                            )
                        }
                    )
                    if (showLanguageDialog) {
                        LanguagePickerDialog(
                            onLanguageSelected = { language ->
                                selectedLanguage = language
                                showLanguageDialog = false
                            },
                            languages = SUPPORTED_LANGUAGES,
                            onDismissRequest = { showLanguageDialog = false }
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp, start = 16.dp, end = 16.dp),
                    onClick = {
                        // Check if language is required and missing
                        val model = selectedModel
                        val needsLanguage = (model != null && !model.enOnly) || !isMedia

                        if (needsLanguage && selectedLanguage.isEmpty()) {
                            snackScope.launch {
                                snackbarHostState.showSnackbar(context.getString(R.string.select_lang_err))
                            }
                            return@Button
                        }

                        transcriptionListOnAction(
                            TranscriptionListAction.OnTranscriptFile(
                                // Use model name if it exists, otherwise empty for subtitles
                                model?.name ?: "",
                                // Use selected lang if needed, otherwise default to "en"
                                if (needsLanguage) selectedLanguage else "en",
                                pickedUri,
                            )
                        )
                    }) {
                    Text(stringResource(R.string.start))
                }
            }
        }
    }
}
