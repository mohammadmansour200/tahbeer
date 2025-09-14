package com.tahbeer.app.core.presentation.components

import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider

@Composable
fun AppSnackbarHost(
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    snackbarHostState.currentSnackbarData
        ?.takeIf { it.visuals.message.isNotEmpty() }
        ?.let { data ->
            Dialog(
                onDismissRequest = {
                    data.dismiss()
                },
                properties = DialogProperties(
                    dismissOnClickOutside = true,
                    dismissOnBackPress = true,
                    usePlatformDefaultWidth = false
                )
            ) {
                // Position the dialog at the bottom of the screen
                (LocalView.current.parent as DialogWindowProvider).window.apply {
                    setGravity(Gravity.BOTTOM)
                    clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)  // Remove the dim background
                    // Don't add FLAG_NOT_TOUCHABLE so swipe gestures work
                    // Don't add FLAG_NOT_FOCUSABLE so clicks outside can dismiss

                    // Set layout parameters to position at bottom with proper width
                    attributes = attributes.apply {
                        width = WindowManager.LayoutParams.MATCH_PARENT
                        // No need to set height as it will wrap content
                    }
                }

                val density = LocalDensity.current
                val navigationBarPadding = with(density) {
                    WindowInsets.navigationBars.getBottom(this).toDp()
                }

                Box(
                    modifier = modifier.padding(bottom = navigationBarPadding),
                ) {
                    SnackbarHost(hostState = snackbarHostState) {
                        Snackbar(
                            snackbarData = it,
                            actionOnNewLine = true,
                        )
                    }
                }
            }
        }
}