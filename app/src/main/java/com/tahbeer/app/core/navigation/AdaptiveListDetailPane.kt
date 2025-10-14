package com.tahbeer.app.core.navigation

import android.net.Uri
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.NavigableListDetailPaneScaffold
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tahbeer.app.details.presentation.DetailScreen
import com.tahbeer.app.home.presentation.HomeScreen
import com.tahbeer.app.home.presentation.settings.SettingsViewModel
import com.tahbeer.app.home.presentation.transcription_list.TranscriptionListAction
import com.tahbeer.app.home.presentation.transcription_list.TranscriptionListViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun AdaptiveListDetailPane(
    transcriptionListViewModel: TranscriptionListViewModel = koinViewModel(),
    settingsViewModel: SettingsViewModel = koinViewModel(),
    receivedUri: Uri?
) {
    val coroutineScope = rememberCoroutineScope()

    val settingsState by settingsViewModel.state.collectAsStateWithLifecycle()
    val transcriptionListState by transcriptionListViewModel.state.collectAsStateWithLifecycle()

    val navigator = rememberListDetailPaneScaffoldNavigator<Any>()
    NavigableListDetailPaneScaffold(
        navigator = navigator,
        listPane = {
            AnimatedPane {
                HomeScreen(
                    receivedUri = receivedUri,
                    settingsEvents = settingsViewModel.events,
                    settingsState = settingsState,
                    transcriptionListState = transcriptionListState,
                    settingsOnAction = { action ->
                        settingsViewModel.onAction(action)
                    },
                    transcriptionListOnAction = { action ->
                        when (action) {
                            is TranscriptionListAction.OnTranscriptClick -> {
                                transcriptionListViewModel.onAction(action)
                                coroutineScope.launch {
                                    navigator.navigateTo(
                                        pane = ListDetailPaneScaffoldRole.Detail
                                    )
                                }
                            }

                            is TranscriptionListAction.OnTranscriptDelete -> {
                                transcriptionListViewModel.onAction(action)
                                if (action.transcriptionId == transcriptionListState.selectedTranscriptionId) {
                                    coroutineScope.launch {
                                        navigator.navigateTo(
                                            pane = ListDetailPaneScaffoldRole.List
                                        )
                                    }
                                }
                            }

                            else -> transcriptionListViewModel.onAction(action)
                        }
                    },
                )
            }
        },
        detailPane = {
            AnimatedPane {
                DetailScreen(
                    transcriptionListState = transcriptionListState,
                    settingsState = settingsState,
                    onGoBack = {
                        coroutineScope.launch {
                            navigator.navigateTo(
                                pane = ListDetailPaneScaffoldRole.List
                            )
                            transcriptionListViewModel.onAction(
                                TranscriptionListAction.OnTranscriptClick(
                                    null
                                )
                            )
                        }
                    },
                    transcriptionListOnAction = { transcriptionListViewModel.onAction(it) },
                    settingsOnAction = { settingsViewModel.onAction(it) },
                )
            }
        },
    )
}