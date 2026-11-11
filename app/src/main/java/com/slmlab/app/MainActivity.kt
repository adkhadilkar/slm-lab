package com.slmlab.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.slmlab.app.data.AppContainer
import com.slmlab.app.data.AppSettings
import com.slmlab.app.ui.nav.AppNav
import com.slmlab.app.ui.theme.SlmLabTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings by AppContainer.progress.settings
                .collectAsStateWithLifecycle(initialValue = AppSettings())
            SlmLabTheme(settings.themeMode) {
                AppNav()
            }
        }
    }
}
