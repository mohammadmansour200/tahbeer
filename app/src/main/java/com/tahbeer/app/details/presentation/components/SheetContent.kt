package com.tahbeer.app.details.presentation.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import com.tahbeer.app.core.domain.model.TranscriptionItem
import com.tahbeer.app.details.presentation.DetailScreenAction
import com.tahbeer.app.details.presentation.DetailScreenState
import com.tahbeer.app.home.presentation.settings.SettingsAction
import com.tahbeer.app.home.presentation.settings.SettingsState
import com.tahbeer.app.home.presentation.transcription_list.TranscriptionListAction
import com.tahbeer.app.home.presentation.transcription_list.TranscriptionListState

enum class BottomSheetType {
    TRANSLATE, EXPORT, BURN, SUCCESS, ERROR
}

@Composable
fun SheetContent(
    snackbarHostState: SnackbarHostState,
    bottomSheetType: BottomSheetType,
    settingsState: SettingsState,
    transcriptionListState: TranscriptionListState,
    transcriptionItem: TranscriptionItem,
    transcriptionListOnAction: (TranscriptionListAction) -> Unit,
    settingsOnAction: (SettingsAction) -> Unit,
    detailScreenAction: (DetailScreenAction) -> Unit,
    detailScreenState: DetailScreenState,
) {
    AnimatedContent(bottomSheetType) { type ->
        when (type) {
            BottomSheetType.TRANSLATE -> TranslateBottomSheet(
                snackbarHostState = snackbarHostState,
                settingsState = settingsState,
                settingsOnAction = { settingsOnAction(it) },
                transcriptionListState = transcriptionListState,
                onAction = { transcriptionListOnAction(it) },
                transcriptionItem = transcriptionItem
            )

            BottomSheetType.EXPORT -> ExportBottomSheet(
                onAction = { detailScreenAction(it) },
                transcriptionItem = transcriptionItem
            )

            BottomSheetType.SUCCESS -> SuccessBottomSheet(
                outputedFile = detailScreenState.outputedFile,
            )

            BottomSheetType.ERROR -> ErrorBottomSheet(
                detailedErrorMessage = detailScreenState.detailedErrorMessage,
                error = detailScreenState.error
            )

            BottomSheetType.BURN -> BurnSubtitleSheet(
                snackbarHostState = snackbarHostState,
                state = detailScreenState,
                onAction = { detailScreenAction(it) },
                transcriptionItem = transcriptionItem,
            )
        }
    }
}