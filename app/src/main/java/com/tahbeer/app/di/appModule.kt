package com.tahbeer.app.di

import com.tahbeer.app.details.data.media.LocalMediaStoreManager
import com.tahbeer.app.details.domain.MediaStoreManager
import com.tahbeer.app.details.presentation.DetailScreenViewModel
import com.tahbeer.app.home.data.list.WhisperSpeechRecognition
import com.tahbeer.app.home.data.settings.LocalThemePreferencesDataSource
import com.tahbeer.app.home.data.settings.MlkitModelManager
import com.tahbeer.app.home.data.settings.WhisperModelManager
import com.tahbeer.app.home.domain.list.SpeechRecognition
import com.tahbeer.app.home.domain.model.MlkitModel
import com.tahbeer.app.home.domain.model.WhisperModel
import com.tahbeer.app.home.domain.settings.ModelManager
import com.tahbeer.app.home.domain.settings.ThemePreferences
import com.tahbeer.app.home.presentation.settings.SettingsViewModel
import com.tahbeer.app.home.presentation.transcription_list.TranscriptionListViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

val appModule = module {
    single<ThemePreferences> {
        LocalThemePreferencesDataSource(context = androidContext())
    }

    single<ModelManager<WhisperModel>>(named("whisper")) {
        WhisperModelManager(androidContext())
    }

    single<ModelManager<MlkitModel>>(named("mlkit")) {
        MlkitModelManager()
    }

    single<SpeechRecognition> {
        WhisperSpeechRecognition(context = androidContext())
    }

    single<MediaStoreManager> {
        LocalMediaStoreManager(context = androidContext())
    }

    viewModel {
        SettingsViewModel(
            themePreferences = get(),
            whisperModelManager = get(named("whisper")),
            mlkitModelManager = get(named("mlkit"))
        )
    }

    viewModel {
        TranscriptionListViewModel(
            speechRecognition = get(),
            context = androidContext(),
        )
    }

    viewModel {
        DetailScreenViewModel(
            context = androidContext(),
            mediaStoreManager = get()
        )
    }
}