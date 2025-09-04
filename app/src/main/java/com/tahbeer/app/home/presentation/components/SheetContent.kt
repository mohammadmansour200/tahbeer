package com.tahbeer.app.home.presentation.components

import androidx.compose.runtime.Composable
import com.tahbeer.app.home.presentation.SettingsAction
import com.tahbeer.app.home.presentation.SettingsState

enum class BottomSheetType {
    ABOUT, SETTINGS
}

@Composable
fun SheetContent(
    bottomSheetType: BottomSheetType,
    settingsState: SettingsState,
    settingsOnAction: (SettingsAction) -> Unit,
) {
    when (bottomSheetType) {
        BottomSheetType.ABOUT -> AboutBottomSheet()
        BottomSheetType.SETTINGS -> SettingsBottomSheet(
            state = settingsState,
            onAction = settingsOnAction
        )
    }
}
