package com.alchemedia.collage.render

import com.alchemedia.collage.Strings
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** A photo frame. [color] and [amount] are what picking it sets (the user can change both afterwards). */
data class FrameDef(val id: String, val name: String, val color: Long, val amount: Float) {
    val title: String get() = Strings.named("frame_$id", name)
}

/**
 * Frames drawn around each rectangular photo: a plain border, a polaroid card, a film strip, a postage stamp, torn paper,
 * taped corners and a lifted shadow. [amount] is a percentage of the cell's shorter side.
 */
object Frames {
    const val NONE = "none"
    const val MAX_AMOUNT = 12f

    val all = listOf(
        FrameDef(NONE, "None", 0xFFFFFFFF, 5f),
        FrameDef("border", "Border", 0xFFFFFFFF, 3f),
        FrameDef("polaroid", "Polaroid", 0xFFFFFFFF, 4.5f),
        FrameDef("film", "Film", 0xFF15181F, 6f),
        FrameDef("stamp", "Stamp", 0xFFFFFFFF, 6f),
        FrameDef("torn", "Torn", 0xFFFFFBF2, 5f),
        FrameDef("tape", "Tape", 0xFFF6D97A, 6f),
        FrameDef("lift", "Lift", 0xFF000000, 4f),
    )

    fun byId(id: String): FrameDef = all.firstOrNull { it.id == id } ?: all[0]

    private val clear = PorterDuffXfermode(PorterDuff.Mode.CLEAR)

    private fun unit(r: RectF, amount: Float): Float = min(r.width(), r.height()) * (amount / 100f).coerceIn(0f, MAX_AMOUNT / 100f)

    private fun inset(r: RectF, l: Float, t: Float, rt: Float, b: Float) = RectF(r.left + l, r.top + t, r.right - rt, r.bottom - b)

    /** The rectangle the photo fills inside a frame around [r]. */
    fun inner(id: String, r: RectF, amount: Float): RectF {
        val t = unit(r, amount)
        return when (id) {
            "border", "stamp", "torn" -> inset(r, t, t, t, t)
            "polaroid" -> inset(r, t, t, t, t * 3f)
            "film" -> inset(r, t * 2.2f, t * .55f, t * 2.2f, t * .55f)
            else -> RectF(r)
        }
    }

    /**
     * Draws the frame around [r] and calls [photo] once with the rectangle (and corner radius) the picture has to fill, so
     * the caller decides how the photo is drawn. Parts that sit on top of the photo (tape) are drawn after it.
     */
    fun draw(canvas: Canvas, id: String, r: RectF, radius: Float, color: Int, amount: Float, seed: Int, photo: (RectF, Float) -> Unit) {
        val t = unit(r, amount)
        val inner = inner(id, r, amount)
        val fill = color
        val card = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = fill }
        when (id) {
            "border" -> {
                canvas.drawRoundRect(r, radius, radius, card)
                photo(inner, max(0f, radius - t))
            }
            "polaroid" -> {
                val path = Path().apply { addRoundRect(r, radius * .35f, radius * .35f, Path.Direction.CW) }
                shadow(canvas, path, t * .5f, t * .35f, 0, 12)
                canvas.drawPath(path, card)
                photo(inner, 0f)
            }
            "film" -> {
                val strip = RectF(r)
                val corner = min(radius, t * 1.2f)
                // Shadow, strip and sprocket holes share one layer, so the holes show the real background rather than the shadow.
                val layer = canvas.saveLayer(RectF(r).apply { inset(-t * 2f, -t * 2f) }, null)
                shadow(canvas, Path().apply { addRoundRect(strip, corner, corner, Path.Direction.CW) }, t * .5f, t * .35f, 0, 12)
                canvas.drawRoundRect(strip, corner, corner, card)
                val hole = Paint(Paint.ANTI_ALIAS_FLAG).apply { xfermode = clear }
                val holeW = t * .85f
                val holeH = t * 1.1f
                val count = max(1, (strip.height() / (t * 2.1f)).roundToInt())
                val pitch = strip.height() / count
                for (i in 0 until count) {
                    val cy = strip.top + pitch * (i + .5f)
                    for (cx in floatArrayOf(strip.left + t * 1.1f, strip.right - t * 1.1f)) {
                        canvas.drawRoundRect(RectF(cx - holeW / 2, cy - holeH / 2, cx + holeW / 2, cy + holeH / 2), holeW * .3f, holeW * .3f, hole)
                    }
                }
                canvas.restoreToCount(layer)
                photo(inner, 0f)
            }
            "stamp" -> {
                val path = stampPath(r, t)
                shadow(canvas, path, t * .5f, t * .35f, 0, 12)
                canvas.drawPath(path, card)
                photo(inner, 0f)
            }
            "torn" -> {
                val path = tornPath(r, t, seed)
                shadow(canvas, path, t * .45f, t * .3f, 0, 12)
                canvas.drawPath(path, card)
                photo(inner, 0f)
            }
            "tape" -> {
                photo(inner, radius)
                val w = t * 4.6f
                val h = t * 1.7f
                tape(canvas, r.left + t * .9f, r.top + t * .9f, w, h, -38f, color)
                tape(canvas, r.right - t * .9f, r.top + t * .9f, w, h, 38f, color)
            }
            "lift" -> {
                val path = Path().apply { addRoundRect(r, radius, radius, Path.Direction.CW) }
                shadow(canvas, path, t * 1.3f, t * .8f, color and 0xFFFFFF, 17)
                photo(inner, radius)
            }
            else -> photo(inner, radius)
        }
    }

    /** A soft shadow: the path drawn several times, widest and faintest first. */
    private fun shadow(canvas: Canvas, path: Path, spread: Float, dy: Float, rgb: Int, alpha: Int) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL_AND_STROKE
            strokeJoin = Paint.Join.ROUND
            color = Color.argb(alpha, Color.red(rgb), Color.green(rgb), Color.blue(rgb))
        }
        canvas.save()
        canvas.translate(0f, dy)
        for (i in 6 downTo 0) {
            p.strokeWidth = spread * 2f * i / 6f
            canvas.drawPath(path, p)
        }
        canvas.restore()
    }

    /** A rectangle whose edges are scalloped like a postage stamp. */
    private fun stampPath(r: RectF, t: Float): Path {
        val rho = max(1f, t * .3f)
        val path = Path().apply { addRect(r, Path.Direction.CW) }
        val holes = Path()
        fun edge(length: Float, from: Float, at: (Float) -> Pair<Float, Float>) {
            val n = max(2, (length / (rho * 2.9f)).roundToInt())
            val pitch = length / n
            for (i in 0 until n) at(from + pitch * (i + .5f)).let { (x, y) -> holes.addCircle(x, y, rho, Path.Direction.CW) }
        }
        edge(r.width(), r.left) { it to r.top }
        edge(r.width(), r.left) { it to r.bottom }
        edge(r.height(), r.top) { r.left to it }
        edge(r.height(), r.top) { r.right to it }
        path.op(holes, Path.Op.DIFFERENCE)
        return path
    }

    /** A rectangle with a ragged, torn-paper edge. The same [seed] always gives the same tear. */
    private fun tornPath(r: RectF, t: Float, seed: Int): Path {
        var s = seed * 2654435761L + 0x9E3779B9L
        fun rnd(): Float {
            s = (s * 6364136223846793005L + 1442695040888963407L)
            return ((s ushr 33) and 0xFFFF).toInt() / 65535f - .5f
        }
        val step = max(2f, t * .5f)
        val amp = t * .55f
        val path = Path()
        fun side(x0: Float, y0: Float, x1: Float, y1: Float, nx: Float, ny: Float, first: Boolean) {
            val len = abs(x1 - x0) + abs(y1 - y0)
            val n = max(2, (len / step).roundToInt())
            for (i in (if (first) 0 else 1)..n) {
                val f = i / n.toFloat()
                val jit = if (i == 0 || i == n) 0f else rnd() * amp
                val x = x0 + (x1 - x0) * f + nx * jit
                val y = y0 + (y1 - y0) * f + ny * jit
                if (first && i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
        }
        side(r.left, r.top, r.right, r.top, 0f, 1f, true)
        side(r.right, r.top, r.right, r.bottom, 1f, 0f, false)
        side(r.right, r.bottom, r.left, r.bottom, 0f, 1f, false)
        side(r.left, r.bottom, r.left, r.top, 1f, 0f, false)
        path.close()
        return path
    }

    private fun tape(canvas: Canvas, cx: Float, cy: Float, w: Float, h: Float, degrees: Float, color: Int) {
        canvas.save()
        canvas.translate(cx, cy)
        canvas.rotate(degrees)
        val rect = RectF(-w / 2, -h / 2, w / 2, h / 2)
        val translucent = (color and 0xFFFFFF) or (0xC8 shl 24)
        canvas.drawRect(rect, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = translucent })
        canvas.drawRect(rect, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = max(1f, h * .04f)
            this.color = 0x22000000
        })
        canvas.restore()
    }
}
