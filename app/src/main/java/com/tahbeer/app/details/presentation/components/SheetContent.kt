package com.tahbeer.app.details.presentation.components

import androidx.compose.runtime.Composable
import com.tahbeer.app.core.domain.model.TranscriptionItem
import com.tahbeer.app.details.presentation.DetailScreenAction
import com.tahbeer.app.home.presentation.settings.SettingsAction
import com.tahbeer.app.home.presentation.settings.SettingsState
import com.tahbeer.app.home.presentation.transcription_list.TranscriptionListAction
import com.tahbeer.app.home.presentation.transcription_list.TranscriptionListState

enum class BottomSheetType {
    TRANSLATE, EXPORT, SUCCESS, ERROR
}

@Composable
fun SheetContent(
    bottomSheetType: BottomSheetType,
    settingsState: SettingsState,
    transcriptionListState: TranscriptionListState,
    transcriptionItem: TranscriptionItem,
    transcriptionListOnAction: (TranscriptionListAction) -> Unit,
    settingsOnAction: (SettingsAction) -> Unit,
    detailScreenAction: (DetailScreenAction) -> Unit,
) {
    when (bottomSheetType) {
        BottomSheetType.TRANSLATE -> TranslateBottomSheet(
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

        BottomSheetType.SUCCESS -> TODO()
        BottomSheetType.ERROR -> TODO()
    }
}