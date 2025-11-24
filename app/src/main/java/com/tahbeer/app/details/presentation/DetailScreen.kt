package com.tahbeer.app.details.presentation

import android.Manifest
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.mlkit.nl.translate.TranslateLanguage
import com.tahbeer.app.R
import com.tahbeer.app.core.domain.CoreConstants.AUDIO_MIME_TYPES
import com.tahbeer.app.core.domain.CoreConstants.VIDEO_MIME_TYPES
import com.tahbeer.app.core.domain.model.MediaType
import com.tahbeer.app.core.domain.model.TranscriptionItem
import com.tahbeer.app.core.domain.model.TranscriptionStatus
import com.tahbeer.app.core.presentation.components.AppSnackbarHost
import com.tahbeer.app.core.presentation.components.IconWithTooltip
import com.tahbeer.app.core.presentation.utils.ObserveAsEvents
import com.tahbeer.app.details.presentation.components.BottomSheetType
import com.tahbeer.app.details.presentation.components.MediaPlayer
import com.tahbeer.app.details.presentation.components.SheetContent
import com.tahbeer.app.details.utils.longToTimestamp
import com.tahbeer.app.home.presentation.settings.SettingsAction
import com.tahbeer.app.home.presentation.settings.SettingsState
import com.tahbeer.app.home.presentation.transcription_list.TranscriptionListAction
import com.tahbeer.app.home.presentation.transcription_list.TranscriptionListEvent
import com.tahbeer.app.home.presentation.transcription_list.TranscriptionListState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    modifier: Modifier = Modifier,
    transcriptionListState: TranscriptionListState,
    settingsState: SettingsState,
    transcriptionListOnAction: (TranscriptionListAction) -> Unit,
    transcriptionListEvents: Flow<TranscriptionListEvent> = emptyFlow(),
    settingsOnAction: (SettingsAction) -> Unit,
    onGoBack: () -> Unit
) {
    val transcriptionItem =
        transcriptionListState.transcriptions.find { it.id == transcriptionListState.selectedTranscriptionId }
    transcriptionItem?.let { item ->
        val context = LocalContext.current
        val scope = rememberCoroutineScope()

        val viewModel = koinViewModel<DetailScreenViewModel>()
        val state by viewModel.state.collectAsStateWithLifecycle()

        DisposableEffect(Unit) {
            viewModel.onAction(
                DetailScreenAction.OnLoadMedia(
                    item.mediaUri?.toUri()
                )
            )
            onDispose {
                viewModel.mediaPlaybackManager.releaseMedia()
            }
        }

        var currentBottomSheet by remember { mutableStateOf<BottomSheetType?>(null) }

        var lastDeferred by remember { mutableStateOf<CompletableDeferred<Boolean>?>(null) }
        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { granted ->
            lastDeferred?.complete(granted)
        }

        val videoPickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument()
        ) {
            if (it != null) {
                transcriptionListOnAction(
                    TranscriptionListAction.OnLinkVideoWithTranscript(
                        item.id,
                        it
                    )
                )
                viewModel.onAction(
                    DetailScreenAction.OnLoadMedia(
                        it
                    )
                )
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
        }

        ObserveAsEvents(events = viewModel.events) {
            when (it) {
                DetailScreenEvent.Error -> {
                    currentBottomSheet = BottomSheetType.ERROR
                }

                DetailScreenEvent.Success -> {
                    currentBottomSheet = BottomSheetType.SUCCESS
                }

                is DetailScreenEvent.PermissionRequired -> {
                    lastDeferred = it.deferred
                    permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }
        }

        val snackbarHostState = remember { SnackbarHostState() }
        ObserveAsEvents(events = transcriptionListEvents) {
            when (it) {
                TranscriptionListEvent.SplitError -> scope.launch {
                    snackbarHostState.showSnackbar(
                        context.getString(R.string.split_subtitle_err)
                    )
                }
            }
        }

        val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
        Scaffold(
            snackbarHost = { AppSnackbarHost(snackbarHostState = snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = item.title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { onGoBack() }) {
                            IconWithTooltip(
                                icon = Icons.AutoMirrored.Filled.ArrowBack,
                                text = stringResource(R.string.back_btn),
                            )
                        }
                    },
                    actions = {
                        var menuExpanded by remember { mutableStateOf(false) }

                        IconButton(onClick = { menuExpanded = !menuExpanded }) {
                            IconWithTooltip(
                                icon = Icons.Filled.MoreVert,
                                text = stringResource(R.string.more_options),
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                        ) {
                            DropdownMenuItem(
                                enabled = item.status == TranscriptionStatus.SUCCESS,
                                text = { Text(stringResource(R.string.share_menu_item)) },
                                onClick = {
                                    menuExpanded = false
                                    val sendIntent: Intent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, buildString {
                                            transcriptionItem.result?.forEach {
                                                append("${it.text}\n")
                                            }
                                        })
                                        type = "text/plain"
                                    }
                                    val shareIntent = Intent.createChooser(sendIntent, null)
                                    context.startActivity(shareIntent)
                                },
                            )
                            DropdownMenuItem(
                                enabled = item.status == TranscriptionStatus.SUCCESS,
                                text = { Text(stringResource(R.string.translate_menu_item)) },
                                onClick = {
                                    val sourceLang = TranslateLanguage.fromLanguageTag(item.lang)
                                    if (sourceLang == null) {
                                        scope.launch {
                                            snackbarHostState.showSnackbar(context.getString(R.string.translate_error))
                                        }
                                        return@DropdownMenuItem
                                    }

                                    menuExpanded = false
                                    currentBottomSheet = BottomSheetType.TRANSLATE
                                },
                            )
                            DropdownMenuItem(
                                enabled = item.status == TranscriptionStatus.SUCCESS,
                                text = { Text(stringResource(R.string.export_menu_item)) },
                                onClick = {
                                    menuExpanded = false
                                    currentBottomSheet = BottomSheetType.EXPORT
                                },
                            )

                            if (item.mediaUri == null && item.mediaType == MediaType.SUBTITLE) {
                                DropdownMenuItem(
                                    enabled = item.status == TranscriptionStatus.SUCCESS,
                                    text = { Text(stringResource(R.string.link_menu_item)) },
                                    onClick = {
                                        menuExpanded = false
                                        videoPickerLauncher.launch(
                                            arrayOf(
                                                *AUDIO_MIME_TYPES,
                                                *VIDEO_MIME_TYPES,
                                            )
                                        )
                                    },
                                )
                            }

                            if (item.mediaUri != null && item.mediaType != MediaType.AUDIO)
                                DropdownMenuItem(
                                    enabled = item.status == TranscriptionStatus.SUCCESS,
                                    text = { Text(stringResource(R.string.burn_menu_item)) },
                                    onClick = {
                                        menuExpanded = false
                                        currentBottomSheet = BottomSheetType.BURN
                                    },
                                )
                        }
                    },
                    scrollBehavior = scrollBehavior
                )
            }
        ) { innerPadding ->
            if (currentBottomSheet != null) {
                ModalBottomSheet(
                    onDismissRequest = {
                        currentBottomSheet = null
                    }) {
                    currentBottomSheet?.let { type ->
                        SheetContent(
                            snackbarHostState = snackbarHostState,
                            bottomSheetType = type,
                            settingsState = settingsState,
                            transcriptionListState = transcriptionListState,
                            transcriptionItem = transcriptionItem,
                            transcriptionListOnAction = { transcriptionListOnAction(it) },
                            detailScreenAction = { viewModel.onAction(it) },
                            settingsOnAction = { settingsOnAction(it) },
                            detailScreenState = state
                        )
                    }
                }
            }

            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (item.status) {
                    TranscriptionStatus.PROCESSING -> Box {
                        when (transcriptionItem.progress) {
                            null -> LinearProgressIndicator(Modifier.fillMaxWidth())

                            else -> LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth(),
                                progress = { transcriptionItem.progress },
                            )
                        }
                    }

                    TranscriptionStatus.SUCCESS -> Crossfade(
                        targetState = state.mediaStatus,
                    ) { status ->
                        when (status) {
                            MediaStatus.LOADING -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            }

                            MediaStatus.READY -> {
                                val sheetState = rememberBottomSheetScaffoldState()
                                BottomSheetScaffold(
                                    scaffoldState = sheetState,
                                    sheetPeekHeight = 56.dp,
                                    sheetContent = {
                                        MediaPlayer(
                                            player = viewModel.player,
                                            onAction = { viewModel.onAction(it) },
                                            state = state,
                                            mediaType = item.mediaType
                                        )
                                    },
                                ) { innerPadding ->
                                    SubtitleCues(
                                        modifier = Modifier.padding(innerPadding),
                                        scrollBehavior = scrollBehavior,
                                        transcriptionItem = item,
                                        transcriptionListOnAction = { transcriptionListOnAction(it) },
                                        mediaCurrentPosition = state.mediaPosition,
                                        onSeek = { viewModel.onAction(DetailScreenAction.OnSeek(it)) }
                                    )
                                }
                            }

                            else -> SubtitleCues(
                                scrollBehavior = scrollBehavior,
                                transcriptionItem = item,
                                transcriptionListOnAction = { transcriptionListOnAction(it) },
                                mediaCurrentPosition = null,
                                onSeek = {}
                            )
                        }
                    }

                    else -> Box(Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            val errorTitleResId = when (item.status) {
                                TranscriptionStatus.ERROR_PROCESSING -> R.string.status_error_processing
                                TranscriptionStatus.ERROR_EMPTY -> R.string.status_error_empty
                                TranscriptionStatus.ERROR_FORMAT -> R.string.status_error_format
                                else -> R.string.status_error_model
                            }
                            Text(
                                text = stringResource(errorTitleResId),
                                style = MaterialTheme.typography.titleLarge
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            val descriptionResId = when (item.status) {
                                TranscriptionStatus.ERROR_FORMAT if item.mediaType == MediaType.SUBTITLE -> R.string.status_error_format_subtitle_desc
                                TranscriptionStatus.ERROR_FORMAT -> R.string.status_error_format_desc
                                TranscriptionStatus.ERROR_MODEL -> R.string.status_error_model_desc
                                else -> null
                            }
                            descriptionResId?.let { resId ->
                                Text(
                                    text = stringResource(resId),
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun SubtitleCues(
    modifier: Modifier = Modifier,
    scrollBehavior: TopAppBarScrollBehavior,
    transcriptionItem: TranscriptionItem,
    transcriptionListOnAction: (TranscriptionListAction) -> Unit,
    mediaCurrentPosition: Long?,
    onSeek: (Long) -> Unit
) {
    val appLayoutDirection = LocalLayoutDirection.current

    val listState = rememberLazyListState()

    val currentCueIndex = transcriptionItem.result
        ?.indexOfFirst { result ->
            mediaCurrentPosition != null &&
                    mediaCurrentPosition >= result.startTime &&
                    mediaCurrentPosition < result.endTime
        }

    LaunchedEffect(currentCueIndex) {
        if (currentCueIndex != null && currentCueIndex >= 0) {
            if (!listState.layoutInfo.visibleItemsInfo.map { it.index }.contains(currentCueIndex))
                listState.animateScrollToItem(
                    index = currentCueIndex,
                )
        }
    }

    LazyColumn(
        modifier = modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        state = listState
    ) {
        transcriptionItem.result?.forEachIndexed { index, result ->
            item(result.startTime) {
                val isCurrentCue = index == currentCueIndex

                val targetBackgroundColor = if (isCurrentCue) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    Color.Transparent
                }

                val backgroundColor by animateColorAsState(
                    targetValue = targetBackgroundColor,
                )

                val targetTextColor = if (isCurrentCue) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                }

                val textColor by animateColorAsState(
                    targetValue = targetTextColor,
                )

                val rtlLanguages = listOf("ar", "he", "ur", "fa", "yi", "sd")
                CompositionLocalProvider(
                    LocalLayoutDirection provides if (rtlLanguages.contains(
                            transcriptionItem.lang
                        )
                    ) LayoutDirection.Rtl else LayoutDirection.Ltr
                ) {
                    Row(
                        modifier = if (mediaCurrentPosition != null) Modifier
                            .fillMaxWidth()
                            .background(color = backgroundColor)
                            .clickable(onClick = { onSeek(result.startTime) })
                            .padding(16.dp)
                            .animateItem() else Modifier
                            .fillMaxWidth()
                            .background(color = backgroundColor)
                            .padding(16.dp)
                            .animateItem(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Subtitle cue
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "${longToTimestamp(result.startTime)} - ${
                                    longToTimestamp(
                                        result.endTime
                                    )
                                }",
                                style = MaterialTheme.typography.labelSmall,
                                color = textColor
                            )
                            Text(
                                text = result.text,
                                style = MaterialTheme.typography.bodyLarge,
                                color = textColor
                            )
                        }
                        Spacer(modifier = Modifier.padding(8.dp))

                        Box {
                            // More options button
                            var menuExpanded by remember { mutableStateOf(false) }
                            IconButton(onClick = { menuExpanded = !menuExpanded }) {
                                IconWithTooltip(
                                    icon = Icons.Filled.MoreVert,
                                    text = stringResource(R.string.more_options),
                                    tint = textColor
                                )
                            }

                            var showEditDialog by remember { mutableStateOf(false) }
                            CompositionLocalProvider(LocalLayoutDirection provides appLayoutDirection) {
                                if (showEditDialog) {
                                    var newSubtitle by remember { mutableStateOf(result.text) }
                                    AlertDialog(
                                        onDismissRequest = {
                                            showEditDialog = false
                                        },
                                        title = {
                                            Text(text = stringResource(R.string.edit_subtitle_btn))
                                        },
                                        text = {
                                            CompositionLocalProvider(
                                                LocalLayoutDirection provides if (rtlLanguages.contains(
                                                        transcriptionItem.lang
                                                    )
                                                ) LayoutDirection.Rtl else LayoutDirection.Ltr
                                            ) {
                                                TextField(
                                                    value = newSubtitle,
                                                    onValueChange = { newSubtitle = it },
                                                )
                                            }
                                        },
                                        confirmButton = {
                                            Button(
                                                onClick = {
                                                    val subtitles = transcriptionItem.result
                                                    transcriptionListOnAction(
                                                        TranscriptionListAction.OnSubtitleEntryEdit(
                                                            transcriptionId = transcriptionItem.id,
                                                            editedResults = subtitles.toMutableList()
                                                                .apply {
                                                                    this[index] =
                                                                        this[index].copy(
                                                                            text = newSubtitle
                                                                        )
                                                                }
                                                        )
                                                    )
                                                    showEditDialog = false
                                                }
                                            ) {
                                                Text(stringResource(R.string.confirm_btn))
                                            }
                                        },
                                    )
                                }


                                DropdownMenu(
                                    expanded = menuExpanded,
                                    onDismissRequest = { menuExpanded = false },
                                ) {
                                    // Copy text
                                    val clipboardManager = LocalClipboardManager.current
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.copy_subtitle_btn)) },
                                        onClick = {
                                            menuExpanded = false
                                            clipboardManager.setText(AnnotatedString(result.text))
                                        }
                                    )

                                    // Edit
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.edit_subtitle_btn)) },
                                        onClick = {
                                            showEditDialog = true
                                            menuExpanded = false
                                        }
                                    )

                                    // Split
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.split_subtitle_btn)) },
                                        onClick = {
                                            menuExpanded = false
                                            transcriptionListOnAction(
                                                TranscriptionListAction.OnSubtitleEntrySplit(
                                                    transcriptionId = transcriptionItem.id,
                                                    index = index
                                                )
                                            )
                                        }
                                    )

                                    // Delete
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.delete_subtitle_btn)) },
                                        onClick = {
                                            menuExpanded = false
                                            transcriptionListOnAction(
                                                TranscriptionListAction.OnSubtitleEntryDelete(
                                                    transcriptionId = transcriptionItem.id,
                                                    index = index
                                                )
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


