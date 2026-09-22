package com.lvlaanu.mediastarremote

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.lvlaanu.mediastarremote.ui.LearnScreen
import com.lvlaanu.mediastarremote.ui.RemoteScreen
import com.lvlaanu.mediastarremote.ui.RemoteViewModel
import com.lvlaanu.mediastarremote.ui.SettingsScreen
import com.lvlaanu.mediastarremote.ui.theme.MediaStarTheme

/**
 * Single activity hosting the three screens.
 *
 * The ViewModel is created at the NavHost level rather than per screen so the
 * IR transmitter and its single send thread survive navigation, and so a
 * Discovery sweep is not silently killed by opening Settings.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { MediaStarApp() }
    }
}

private object Routes {
    const val REMOTE = "remote"
    const val SETTINGS = "settings"
    const val LEARN = "learn"
}

@Composable
fun MediaStarApp() {
    MediaStarTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            val navController = rememberNavController()
            val viewModel: RemoteViewModel = viewModel(factory = RemoteViewModel.Factory)

            NavHost(
                navController = navController,
                startDestination = Routes.REMOTE,
                modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing),
            ) {
                composable(Routes.REMOTE) {
                    RemoteScreen(
                        viewModel = viewModel,
                        onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                        onOpenLearn = { navController.navigate(Routes.LEARN) },
                    )
                }
                composable(Routes.SETTINGS) {
                    SettingsScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
                }
                composable(Routes.LEARN) {
                    LearnScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
                }
            }
        }
    }
}
