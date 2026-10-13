package com.alchemedia.collage

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.alchemedia.collage.ui.AppRoot
import com.alchemedia.collage.ui.AppViewModel
import com.alchemedia.collage.ui.components.LocalApp
import com.alchemedia.collage.ui.theme.CollageTheme

class MainActivity : ComponentActivity() {
    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        hideNavigationBar()
        val app = application as CollageApp
        setContent {
            CompositionLocalProvider(LocalApp provides app) {
                CollageTheme { AppRoot(viewModel) }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        (application as CollageApp).billing.connect()
    }

    override fun onStop() {
        viewModel.editor?.flush()
        super.onStop()
    }

    // The system brings the bars back after the photo picker or a dialog closes, so hide them
    // again whenever the window regains focus.
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideNavigationBar()
    }

    /**
     * Hides the navigation bar. Swiping from the screen edge reveals it briefly and it hides
     * itself again. The status bar stays visible.
     */
    private fun hideNavigationBar() {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.navigationBars())
    }
}
