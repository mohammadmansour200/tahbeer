package com.tahbeer.app.home.presentation.components

import androidx.compose.runtime.Composable
import com.tahbeer.app.home.presentation.ThemeAction
import com.tahbeer.app.home.presentation.ThemeState

enum class BottomSheetType {
    ABOUT, SETTINGS
}

@Composable
fun SheetContent(
    bottomSheetType: BottomSheetType,
    settingsState: ThemeState,
    settingsOnAction: (ThemeAction) -> Unit,
) {
    when (bottomSheetType) {
        BottomSheetType.ABOUT -> AboutBottomSheet()
        BottomSheetType.SETTINGS -> SettingsBottomSheet(
            state = settingsState,
            onAction = settingsOnAction
        )
    }
}
