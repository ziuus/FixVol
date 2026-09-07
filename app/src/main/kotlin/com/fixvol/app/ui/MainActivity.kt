package com.fixvol.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.fixvol.app.data.AppMetadata
import com.fixvol.app.ui.theme.FixVolTheme

sealed class Screen {
    object Main : Screen()
    data class AppConfig(val app: AppMetadata) : Screen()
    object Debug : Screen()
    object Settings : Screen()
}

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FixVolTheme {
                AppNavigation(viewModel)
            }
        }
    }
}

@Composable
fun AppNavigation(viewModel: MainViewModel) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Main) }

    when (val screen = currentScreen) {
        is Screen.Main -> MainScreen(
            viewModel = viewModel,
            onNavigateToAppConfig = { app -> currentScreen = Screen.AppConfig(app) },
            onNavigateToDebug = { currentScreen = Screen.Debug },
            onNavigateToSettings = { currentScreen = Screen.Settings }
        )
        is Screen.AppConfig -> AppConfigScreen(
            app = screen.app,
            viewModel = viewModel,
            onBack = { currentScreen = Screen.Main }
        )
        is Screen.Debug -> DebugScreen(
            viewModel = viewModel,
            onBack = { currentScreen = Screen.Main }
        )
        is Screen.Settings -> SettingsScreen(
            viewModel = viewModel,
            onBack = { currentScreen = Screen.Main }
        )
    }
}
