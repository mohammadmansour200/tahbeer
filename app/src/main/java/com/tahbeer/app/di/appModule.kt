package com.tahbeer.app.di

import com.tahbeer.app.home.data.preferences.LocalThemePreferencesDataSource
import com.tahbeer.app.home.domain.ThemePreferences
import com.tahbeer.app.home.presentation.ThemeViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single<ThemePreferences> {
        LocalThemePreferencesDataSource(context = androidContext())
    }

    viewModel {
        ThemeViewModel(
            themePreferences = get()
        )
    }
}