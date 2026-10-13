package com.alchemedia.collagemaker.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = Color(0xFF5B4BE0),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE4DFFF),
    onPrimaryContainer = Color(0xFF16005F),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFC7BFFF),
    onPrimary = Color(0xFF2A0C94),
    primaryContainer = Color(0xFF4231C7),
    onPrimaryContainer = Color(0xFFE4DFFF),
)

@Composable
fun CollageTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colors, content = content)
}
