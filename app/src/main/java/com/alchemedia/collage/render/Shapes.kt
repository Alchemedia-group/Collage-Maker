package com.alchemedia.collage.render

import android.graphics.Matrix
import android.graphics.Path
import androidx.core.graphics.PathParser
import com.alchemedia.collage.model.LayoutCatalog

/** Shape outlines, drawn in a 100 x 100 box. */
object Shapes {
    private val cache = HashMap<String, Path>()
    private val fallback = Path().apply { addRect(0f, 0f, 100f, 100f, Path.Direction.CW) }

    @Synchronized
    fun base(name: String?): Path {
        if (name == null) return fallback
        cache[name]?.let { return it }
        val data = LayoutCatalog.shapes[name]
        val path = data?.let { runCatching { PathParser.createPathFromPathData(it) }.getOrNull() } ?: fallback
        cache[name] = path
        return path
    }

    /** The outline placed on the canvas: stretched over the cell, or scaled and rotated about its centre. */
    fun place(name: String?, x: Float, y: Float, w: Float, h: Float, fill: Boolean, scale: Float, rotation: Float): Path {
        val m = Matrix()
        if (fill) {
            m.postScale(w / 100f, h / 100f)
            m.postTranslate(x, y)
        } else {
            val side = minOf(w, h) * scale
            m.postTranslate(-50f, -50f)
            m.postScale(side / 100f, side / 100f)
            m.postRotate(rotation)
            m.postTranslate(x + w / 2f, y + h / 2f)
        }
        return Path().also { base(name).transform(m, it) }
    }
}
