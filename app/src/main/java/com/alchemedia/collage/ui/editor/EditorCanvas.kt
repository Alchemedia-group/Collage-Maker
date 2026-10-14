package com.alchemedia.collage.ui.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.alchemedia.collage.render.CollageRenderer
import com.alchemedia.collage.render.UiOptions
import com.alchemedia.collage.ui.EditorState
import com.alchemedia.collage.ui.components.rememberPhotos
import com.alchemedia.collage.ui.theme.CTheme
import kotlin.math.ceil

/** The collage on the desk. One finger selects and drags, two fingers pinch (zoom a photo, or resize and rotate an item). */
@Composable
fun EditorCanvas(ed: EditorState, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val accent = CTheme.colors.accent.toArgb()
    BoxWithConstraints(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val ratio = ed.project.aspect.ratio
        var w = maxWidth
        var h = w / ratio
        if (h > maxHeight) { h = maxHeight; w = h * ratio }
        w = w.coerceAtLeast(60.dp)
        h = h.coerceAtLeast(60.dp)
        val longSidePx = with(density) { maxOf(w, h).toPx() }
        // Zoomed-in photos need more pixels than the canvas has, or they go soft.
        val zoomed = ed.project.cells.maxOfOrNull { it.zoom }?.coerceAtMost(4f) ?: 1f
        val bucket = (ceil(longSidePx * zoomed / 256f) * 256f).toInt().coerceIn(512, 3072)
        val photos = rememberPhotos(ed.project, bucket)
        SideEffect {
            ed.photoSource = photos
            ed.density = density.density
        }
        Canvas(
            Modifier.size(w, h).shadow(10.dp, spotColor = androidx.compose.ui.graphics.Color(0x52101828)).clipToBounds()
                .semantics { contentDescription = "Collage canvas. Tap a cell, drag to reposition its photo, pinch to zoom." }
                .onSizeChanged { ed.canvasW = it.width.toFloat(); ed.canvasH = it.height.toFloat() }
                .pointerInput(ed) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        down.consume()
                        ed.onDown(down.position.x, down.position.y)
                        var pinching = false
                        while (true) {
                            val event = awaitPointerEvent()
                            val pressed = event.changes.filter { it.pressed }
                            if (pressed.isEmpty()) break
                            if (pressed.size >= 2) {
                                val a = pressed[0].position
                                val b = pressed[1].position
                                if (!pinching) { pinching = true; ed.onPinchStart(a.x, a.y, b.x, b.y) } else ed.onPinch(a.x, a.y, b.x, b.y)
                            } else if (!pinching) {
                                val p = pressed[0].position
                                ed.onMove(p.x, p.y)
                            }
                            event.changes.forEach { it.consume() }
                        }
                        ed.onUp()
                    }
                },
        ) {
            val project = ed.project
            val ui = UiOptions(ed.selectedCell, ed.swapping, ed.selectedItemId, accent, density.density)
            drawIntoCanvas { c -> CollageRenderer.draw(c.nativeCanvas, project, size.width, size.height, photos, ui) }
        }
    }
}
