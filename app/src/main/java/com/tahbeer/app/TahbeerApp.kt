package com.tahbeer.app

import android.app.Application
import com.tahbeer.app.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class TahbeerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@TahbeerApp)
            androidLogger()

            modules(appModule)
        }
    }
}