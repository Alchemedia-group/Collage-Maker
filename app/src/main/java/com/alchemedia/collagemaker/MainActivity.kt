package com.alchemedia.collagemaker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.alchemedia.collagemaker.ui.AppNavigation
import com.alchemedia.collagemaker.ui.theme.CollageTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repository = (application as CollageApp).repository
        setContent {
            CollageTheme {
                AppNavigation(repository)
            }
        }
    }
}
