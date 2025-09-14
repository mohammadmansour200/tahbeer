package com.tahbeer.app.di

import com.tahbeer.app.home.data.list.WhisperSpeechRecognition
import com.tahbeer.app.home.data.settings.LocalThemePreferencesDataSource
import com.tahbeer.app.home.data.settings.WhisperModelManager
import com.tahbeer.app.home.domain.list.SpeechRecognition
import com.tahbeer.app.home.domain.settings.ModelManager
import com.tahbeer.app.home.domain.settings.ThemePreferences
import com.tahbeer.app.home.presentation.settings.SettingsViewModel
import com.tahbeer.app.home.presentation.transcription_list.TranscriptionListViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single<ThemePreferences> {
        LocalThemePreferencesDataSource(context = androidContext())
    }

    single<ModelManager> {
        WhisperModelManager(context = androidContext())
    }

    single<SpeechRecognition> {
        WhisperSpeechRecognition(context = androidContext())
    }

    viewModel {
        SettingsViewModel(
            themePreferences = get(),
            whisperModelManager = get()
        )
    }

    viewModel {
        TranscriptionListViewModel(
            speechRecognition = get(),
            context = androidContext(),
        )
    }
}