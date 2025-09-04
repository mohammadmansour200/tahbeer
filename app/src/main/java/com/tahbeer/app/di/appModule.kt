package com.tahbeer.app.di

import com.tahbeer.app.home.data.settings.LocalThemePreferencesDataSource
import com.tahbeer.app.home.data.settings.VoskModelManager
import com.tahbeer.app.home.domain.settings.ModelManager
import com.tahbeer.app.home.domain.settings.ThemePreferences
import com.tahbeer.app.home.presentation.settings.SettingsViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single<ThemePreferences> {
        LocalThemePreferencesDataSource(context = androidContext())
    }

    single<ModelManager> {
        VoskModelManager(context = androidContext())
    }

    viewModel {
        SettingsViewModel(
            themePreferences = get(),
            voskModelManager = get()
        )
    }
}