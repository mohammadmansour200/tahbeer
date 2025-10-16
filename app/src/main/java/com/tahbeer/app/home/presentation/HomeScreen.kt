package com.tahbeer.app.home.presentation

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.tahbeer.app.R
import com.tahbeer.app.core.domain.CoreConstants.AUDIO_MIME_TYPES
import com.tahbeer.app.core.domain.CoreConstants.SUBTITLE_MIME_TYPES
import com.tahbeer.app.core.domain.CoreConstants.VIDEO_MIME_TYPES
import com.tahbeer.app.core.domain.model.MediaType
import com.tahbeer.app.core.domain.model.SubtitleEntry
import com.tahbeer.app.core.domain.model.TranscriptionItem
import com.tahbeer.app.core.domain.model.TranscriptionStatus
import com.tahbeer.app.core.presentation.components.AppSnackbarHost
import com.tahbeer.app.core.presentation.components.IconWithTooltip
import com.tahbeer.app.core.presentation.utils.ObserveAsEvents
import com.tahbeer.app.home.domain.settings.DownloadError
import com.tahbeer.app.home.presentation.components.BottomSheetType
import com.tahbeer.app.home.presentation.components.SheetContent
import com.tahbeer.app.home.presentation.components.StartTranscriptionBottomSheet
import com.tahbeer.app.home.presentation.settings.SettingsAction
import com.tahbeer.app.home.presentation.settings.SettingsEvent
import com.tahbeer.app.home.presentation.settings.SettingsState
import com.tahbeer.app.home.presentation.transcription_list.TranscriptionList
import com.tahbeer.app.home.presentation.transcription_list.TranscriptionListAction
import com.tahbeer.app.home.presentation.transcription_list.TranscriptionListState
import com.tahbeer.app.ui.theme.AppTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    settingsEvents: Flow<SettingsEvent> = emptyFlow(),
    settingsState: SettingsState,
    transcriptionListState: TranscriptionListState,
    settingsOnAction: (SettingsAction) -> Unit,
    transcriptionListOnAction: (TranscriptionListAction) -> Unit,
    receivedUri: Uri? = null,
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var currentBottomSheet: BottomSheetType? by remember { mutableStateOf(null) }
    var pickedUri by remember { mutableStateOf(receivedUri) }

    ObserveAsEvents(events = settingsEvents) {
        when (it) {
            SettingsEvent.ModelDeleteSuccess -> {
                snackbarHostState.showSnackbar(context.getString(R.string.settings_model_delete_success))
            }

            SettingsEvent.ModelDownloadSuccess -> {
                snackbarHostState.showSnackbar(context.getString(R.string.settings_model_download_success))
            }

            is SettingsEvent.ModelDownloadError -> {
                val messageResId = when (it.error) {
                    DownloadError.NETWORK_ERROR -> R.string.settings_model_error_network
                    DownloadError.DOWNLOAD_FAILED -> R.string.settings_model_error_download_failed
                    DownloadError.INSUFFICIENT_SPACE -> R.string.settings_model_error_insufficient_space
                }
                snackbarHostState.showSnackbar(context.getString(messageResId))
            }
        }
    }

    Scaffold(
        snackbarHost = { AppSnackbarHost(snackbarHostState = snackbarHostState) },
        bottomBar = {
            BottomAppBar(
                actions = {
                    IconButton(onClick = { currentBottomSheet = BottomSheetType.SETTINGS }) {
                        IconWithTooltip(
                            text = stringResource(R.string.settings_menu_item),
                            icon = Icons.Filled.Settings
                        )
                    }
                    IconButton(onClick = { currentBottomSheet = BottomSheetType.ABOUT }) {
                        IconWithTooltip(
                            text = stringResource(R.string.about_menu_item),
                            icon = Icons.Filled.Info
                        )
                    }
                },

                floatingActionButton = {
                    val launcher =
                        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
                            if (it != null) {
                                pickedUri = it

                                context.contentResolver.takePersistableUriPermission(
                                    it,
                                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                                )
                            }
                        }
                    FloatingActionButton(
                        onClick = {
                            launcher.launch(
                                arrayOf(
                                    *AUDIO_MIME_TYPES,
                                    *VIDEO_MIME_TYPES,
                                    *SUBTITLE_MIME_TYPES
                                )
                            )
                        }
                    ) {
                        IconWithTooltip(
                            icon = ImageVector.vectorResource(R.drawable.rounded_add),
                            text = stringResource(R.string.add_transcript_fab),
                            iconModifier = Modifier.size(28.dp),
                        )
                    }
                }
            )
        }) { innerPadding ->
        TranscriptionList(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding),
            transcriptionListState = transcriptionListState,
            onItemClick = {
                transcriptionListOnAction(TranscriptionListAction.OnTranscriptClick(it))
            },
            onItemDelete = {
                transcriptionListOnAction(TranscriptionListAction.OnTranscriptDelete(it))
            }
        )

        pickedUri?.let { uri ->
            ModalBottomSheet(
                onDismissRequest = {
                    pickedUri = null
                },
            ) {
                StartTranscriptionBottomSheet(
                    whisperModels = settingsState.whisperModels,
                    settingsOnAction = { settingsOnAction(it) },
                    transcriptionListOnAction = {
                        transcriptionListOnAction(it)
                        pickedUri = null
                    },
                    pickedUri = uri,
                    snackbarHostState = snackbarHostState
                )
            }
        }
    }

    if (currentBottomSheet != null) {
        ModalBottomSheet(
            onDismissRequest = {
                currentBottomSheet = null
            },
        ) {
            currentBottomSheet?.let { type ->
                SheetContent(
                    bottomSheetType = type,
                    settingsState = settingsState,
                    settingsOnAction = { settingsOnAction(it) }
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun HomeScreenPreview() {
    AppTheme(dynamicColor = false) {
        HomeScreen(
            settingsState = SettingsState(),
            transcriptionListState = TranscriptionListState(
                listOf(
                    TranscriptionItem(
                        id = "2",
                        mediaUri = "https://example.com/video2.mp4",
                        mediaType = MediaType.VIDEO,
                        lang = "fr",
                        title = "Transcription 2",
                        status = TranscriptionStatus.SUCCESS,
                        progress = 1f,
                        result = listOf(
                            SubtitleEntry(startTime = 0, endTime = 50, text = "Hello, world!"),
                            SubtitleEntry(
                                startTime = 50,
                                endTime = 10,
                                text = "This is a subtitle."
                            )
                        )
                    ),
                    TranscriptionItem(
                        id = "1",
                        mediaUri = "https://example.com/video2.mp3",
                        mediaType = MediaType.AUDIO,
                        lang = "fr",
                        title = "Transcription 2",
                        status = TranscriptionStatus.PROCESSING,
                        progress = 0.5f,
                        result = listOf(
                            SubtitleEntry(startTime = 0, endTime = 50, text = "Hello, world!"),
                            SubtitleEntry(
                                startTime = 50,
                                endTime = 100,
                                text = "This is a subtitle."
                            )
                        )
                    )
                )
            ),
            settingsOnAction = {},
            transcriptionListOnAction = {},
        )
    }
}