package com.tahbeer.app.home.domain

interface ThemePreferences {
    fun setTheme(
        theme: String
    )

    fun getTheme(): String?

    fun setDynamicColorsEnabled(
        enabled: Boolean
    )

    fun getDynamicColorsEnabled(): Boolean
}