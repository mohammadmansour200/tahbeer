package com.tahbeer.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tahbeer.app.core.navigation.AdaptiveCoinListDetailPane
import com.tahbeer.app.home.presentation.SettingsViewModel
import com.tahbeer.app.ui.theme.AppTheme
import com.tahbeer.app.ui.theme.isDarkMode
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.KoinContext

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val receivedUri = if (intent?.action == Intent.ACTION_SEND && intent.type != null) {
            intent.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM) as? Uri
        } else null

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KoinContext {
                val settingsViewModel = koinViewModel<SettingsViewModel>()
                val themeState by settingsViewModel.state.collectAsStateWithLifecycle()

                val insetsController = WindowInsetsControllerCompat(window, window.decorView)
                insetsController.isAppearanceLightStatusBars = !isDarkMode(themeState.theme)
                AppTheme(
                    darkTheme = isDarkMode(themeState.theme),
                    dynamicColor = themeState.dynamicColorsEnabled
                ) {
                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        AdaptiveCoinListDetailPane(
                            modifier = Modifier.padding(innerPadding), receivedUri = receivedUri
                        )
                    }
                }
            }
        }
    }
}
