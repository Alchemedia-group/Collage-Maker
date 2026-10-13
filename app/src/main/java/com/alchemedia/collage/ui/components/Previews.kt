package com.alchemedia.collage.ui.components

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.geometry.Size
import com.alchemedia.collage.CollageApp
import com.alchemedia.collage.model.CollageProject
import com.alchemedia.collage.model.Layout
import com.alchemedia.collage.render.CollageRenderer
import com.alchemedia.collage.render.Fonts
import com.alchemedia.collage.render.PhotoSource
import com.alchemedia.collage.render.Shapes
import com.alchemedia.collage.render.UiOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.min

val LocalApp = compositionLocalOf<CollageApp> { error("CollageApp not provided") }

/**
 * Photo bitmaps for [project] at about [maxSide] pixels, loaded off the main thread. The returned source starts empty
 * and fills in as photos arrive.
 */
@Composable
fun rememberPhotos(project: CollageProject, maxSide: Int): PhotoSource {
    val app = LocalApp.current
    var version by remember { mutableIntStateOf(0) }
    LaunchedEffect(project.id, project.photos, maxSide) {
        withContext(Dispatchers.IO) { app.photos.preload(project, maxSide) }
        version++
    }
    return remember(version, project.id, project.photos, maxSide) { app.photos.source(project, maxSide) }
}

/** A collage drawn with the shared renderer. [ui] adds the editing overlays. */
@Composable
fun CollagePreview(
    project: CollageProject,
    photos: PhotoSource,
    modifier: Modifier = Modifier,
    ui: UiOptions? = null,
    watermark: Boolean = false,
    fixedRatio: Boolean = true,
) {
    val m = if (fixedRatio) modifier.aspectRatio(project.aspect.ratio) else modifier
    Canvas(m) {
        drawIntoCanvas { c -> CollageRenderer.draw(c.nativeCanvas, project, size.width, size.height, photos, ui, watermark) }
    }
}

/** The small layout glyph: one rounded block per cell, shapes and letters included. */
@Composable
fun LayoutIcon(layout: Layout, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val box = min(size.width, size.height)
        val u = box / 40f
        val ratio = layout.ar?.let { com.alchemedia.collage.model.AspectPreset.fromLabel(it).ratio } ?: 1f
        var pw = 36f
        var ph = 36f
        if (ratio >= 1f) ph = 36f / ratio else pw = 36f * ratio
        val ox = 2f + (36f - pw) / 2
        val oy = 2f + (36f - ph) / 2
        val argb = color.toArgb()
        drawIntoCanvas { canvas ->
            val nc = canvas.nativeCanvas
            nc.save()
            nc.translate((size.width - box) / 2, (size.height - box) / 2)
            nc.scale(u, u)
            val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = argb }
            layout.cells.forEach { d ->
                val x = ox + d.x * pw
                val y = oy + d.y * ph
                val w = d.w * pw
                val h = d.h * ph
                when {
                    d.isLetter -> {
                        val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            this.color = argb
                            typeface = Fonts.typeface("bric", true)
                            textAlign = Paint.Align.CENTER
                            textSize = min(h * .92f, w * 1.1f)
                        }
                        nc.drawText(d.text.orEmpty(), x + w / 2, y + h / 2 + tp.textSize * .36f, tp)
                    }
                    d.isShape && d.fill -> {
                        nc.drawPath(Shapes.place(d.shape, x + .8f, y + .8f, w - 1.6f, h - 1.6f, true, 1f, 0f), fill)
                    }
                    d.isShape -> {
                        val side = min(w, h) * d.scale * .96f
                        nc.save()
                        nc.translate(x + w / 2, y + h / 2)
                        nc.rotate(d.rotation)
                        nc.scale(side / 100f, side / 100f)
                        nc.translate(-50f, -50f)
                        nc.drawPath(Shapes.base(d.shape), fill)
                        nc.restore()
                    }
                    else -> nc.drawRoundRect(x + 1.1f, y + 1.1f, x + 1.1f + maxOf(1f, w - 2.2f), y + 1.1f + maxOf(1f, h - 2.2f), 2.4f, 2.4f, fill)
                }
            }
            nc.restore()
        }
    }
}

