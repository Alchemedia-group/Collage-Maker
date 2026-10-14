package com.alchemedia.collage.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.alchemedia.collage.R
import com.alchemedia.collage.render.AppIcon
import kotlinx.coroutines.delay

/** The tiles of the launcher icon are this much of its 108 unit square wide (24.21 to 83.79, drawn at [AppIcon.ART_SCALE]). */
private const val ART_UNITS = (83.79f - 24.21f) * AppIcon.ART_SCALE

/**
 * The splash shown when the app starts: the icon's own blue gradient filling the screen, with the icon artwork large in the middle.
 * It fades in, holds for a moment, fades out and calls [onDone]. (The system's splash before it is plain icon-blue so the two join up.)
 */
@Composable
fun SplashOverlay(onDone: () -> Unit) {
    val appear = remember { Animatable(0f) }
    val leave = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        appear.animateTo(1f, tween(420, easing = FastOutSlowInEasing))
        delay(520)
        leave.animateTo(1f, tween(260))
        onDone()
    }
    // taps during the splash must not reach the Home screen underneath
    SplashContent(
        appear.value,
        Modifier.graphicsLayer { alpha = 1f - leave.value }.clickable(remember { MutableInteractionSource() }, indication = null) {},
    )
}

/** The splash picture itself; [reveal] goes from 0 (icon not yet shown) to 1 (settled). */
@Composable
fun SplashContent(reveal: Float = 1f, modifier: Modifier = Modifier) {
    BoxWithConstraints(
        modifier.fillMaxSize().drawBehind {
            // the icon's gradient (see ic_launcher_background), stretched over the screen
            val w = size.width
            val h = size.height
            drawRect(
                Brush.linearGradient(
                    0f to Color(0xFF588CFF), .5f to Color(0xFF2557D6), 1f to Color(0xFF132678),
                    start = Offset(w * 30f / 108f, 0f), end = Offset(w * 78f / 108f, h),
                ),
            )
            drawRect(Brush.radialGradient(listOf(Color(0x73A0C8FF), Color(0x00A0C8FF)), center = Offset(w * 37f / 108f, h * 22f / 108f), radius = w * 52f / 108f))
        },
        contentAlignment = Alignment.Center,
    ) {
        val art = (maxWidth * .56f).coerceAtMost(270.dp)
        Image(
            painterResource(R.drawable.ic_launcher_foreground), null,
            Modifier.requiredSize(art * (108f / ART_UNITS)).graphicsLayer {
                val k = .86f + .14f * reveal
                scaleX = k; scaleY = k; alpha = reveal
            },
        )
    }
}
