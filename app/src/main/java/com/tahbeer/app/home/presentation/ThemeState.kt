package com.tahbeer.app.home.presentation

import androidx.compose.runtime.Immutable

@Immutable
data class ThemeState(
    val theme: String = "system",
    val dynamicColorsEnabled: Boolean = false
)
