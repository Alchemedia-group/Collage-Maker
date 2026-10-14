package com.alchemedia.collage.render

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader

/**
 * The launcher icon, drawn straight onto a canvas so exported pictures can carry it without a Context.
 * The shapes and colours are the same as res/drawable/ic_launcher_background.xml and ic_launcher_foreground.xml.
 */
object AppIcon {
    /** The artwork sits in the middle of the 108 unit launcher square; this is the part that is shown (as the Home logo does). */
    private const val FROM = 19.16f
    private const val SPAN = 69.68f

    /** The tiles are drawn a little smaller than the launcher square, about its centre, like the launcher icon's foreground. */
    const val ART_SCALE = 0.88f

    private class Tile(val l: Float, val t: Float, val r: Float, val b: Float, val x0: Float, val y0: Float, val x1: Float, val y1: Float, val colors: IntArray, val stops: FloatArray?)

    private val tiles = listOf(
        Tile(24.21f, 24.21f, 61.12f, 44.24f, 33.43f, 24.21f, 51.89f, 44.24f, intArrayOf(0xFFFFCD60.toInt(), 0xFFFF786E.toInt(), 0xFFC44AA0.toInt()), floatArrayOf(0f, .5f, 1f)),
        Tile(63.76f, 24.21f, 83.79f, 61.12f, 68.77f, 24.21f, 78.79f, 61.12f, intArrayOf(0xFFFFFFFF.toInt(), 0xFFD8E5FF.toInt()), null),
        Tile(46.88f, 63.76f, 83.79f, 83.79f, 56.11f, 63.76f, 74.57f, 83.79f, intArrayOf(0xFF6EE8D6.toInt(), 0xFF226ED6.toInt()), null),
        Tile(24.21f, 46.88f, 44.24f, 83.79f, 29.21f, 46.88f, 39.23f, 83.79f, intArrayOf(0xFFFFEC96.toInt(), 0xFFFFAA46.toInt()), null),
    )

    /** Draws the icon as a rounded square filling [box]. */
    fun draw(canvas: Canvas, box: RectF) {
        val k = box.width() / SPAN
        val save = canvas.save()
        val corner = box.width() * .286f
        canvas.clipPath(Path().apply { addRoundRect(box, corner, corner, Path.Direction.CW) })
        canvas.translate(box.left - FROM * k, box.top - FROM * k)
        canvas.scale(k, k)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.shader = LinearGradient(30f, 0f, 78f, 108f, intArrayOf(0xFF588CFF.toInt(), 0xFF2557D6.toInt(), 0xFF132678.toInt()), floatArrayOf(0f, .5f, 1f), Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, 108f, 108f, p)
        p.shader = RadialGradient(37f, 22f, 52f, 0x73A0C8FF, 0x00A0C8FF, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, 108f, 108f, p)
        p.shader = null
        canvas.scale(ART_SCALE, ART_SCALE, 54f, 54f)
        p.color = 0x40050C28
        tiles.forEach { canvas.drawRoundRect(it.l, it.t + 1.1f, it.r, it.b + 1.1f, 4.85f, 4.85f, p) }
        p.color = 0x38050C28
        canvas.drawCircle(54f, 54.7f, 6.82f, p)
        p.color = 0xFF000000.toInt() // a shader is multiplied by the paint's alpha, so the shadow tint must not be left on it
        tiles.forEach {
            p.shader = LinearGradient(it.x0, it.y0, it.x1, it.y1, it.colors, it.stops, Shader.TileMode.CLAMP)
            canvas.drawRoundRect(it.l, it.t, it.r, it.b, 4.85f, 4.85f, p)
        }
        p.shader = LinearGradient(50.59f, 47.18f, 57.41f, 60.82f, 0xFFFFFFFF.toInt(), 0xFFFFE1A0.toInt(), Shader.TileMode.CLAMP)
        canvas.drawCircle(54f, 54f, 6.82f, p)
        canvas.restoreToCount(save)
    }
}
