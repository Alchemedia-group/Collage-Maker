package com.alchemedia.collage.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.util.LruCache
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/** [base] is the background colour that suits the texture (null keeps whatever colour is already chosen). */
data class TextureDef(val id: String, val name: String, val base: Long? = null)

/** Seeded random numbers (mulberry32), so a texture looks the same every time and at every size. */
internal class Mulberry(seed: Int) {
    private var a = seed
    fun next(): Float {
        a += 0x6D2B79F5
        var t = (a xor (a ushr 15)) * (1 or a)
        t = (t + (t xor (t ushr 7)) * (61 or t)) xor t
        return ((t xor (t ushr 14)).toLong() and 0xFFFFFFFFL).toDouble().div(4294967296.0).toFloat()
    }
}

/**
 * Textured backgrounds. Every texture is two maps: dark marks (multiplied over the background) and light marks
 * (screened over it), so it works over any colour. Maps are cached by id and size.
 */
object Textures {
    val all = listOf(
        TextureDef("none", "None"), TextureDef("paper", "Paper"), TextureDef("linen", "Linen"), TextureDef("canvas", "Canvas"),
        TextureDef("kraft", "Kraft", 0xFFC8A578), TextureDef("concrete", "Concrete", 0xFFB9BCC2), TextureDef("marble", "Marble", 0xFFECEEF0),
        TextureDef("wood", "Wood", 0xFFB98A5C), TextureDef("dots", "Polka"), TextureDef("stripes", "Stripes"), TextureDef("grid", "Graph"),
        TextureDef("gingham", "Gingham"), TextureDef("watercolor", "Wash"), TextureDef("bokeh", "Bokeh"), TextureDef("stars", "Stars"),
        TextureDef("crumple", "Crumple"), TextureDef("denim", "Denim", 0xFF4A6F9C),
    )

    fun byId(id: String): TextureDef = all.firstOrNull { it.id == id } ?: all[0]

    /** Drops every cached texture map (low memory). They are rebuilt the next time they are drawn. */
    fun clear() = cache.evictAll()

    private class Maps(val dark: Bitmap, val light: Bitmap)

    private val cache = object : LruCache<String, Maps>(48 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Maps) = value.dark.byteCount + value.light.byteCount
    }

    private val multiply = Paint(Paint.FILTER_BITMAP_FLAG).apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.MULTIPLY) }
    private val screen = Paint(Paint.FILTER_BITMAP_FLAG).apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN) }

    /** Lays the texture over whatever is already on [canvas]; [amount] 0..2 (above 1 the texture is applied twice). */
    fun draw(canvas: Canvas, id: String, amount: Float, width: Int, height: Int) {
        if (id == "none" || amount <= 0f || width < 2 || height < 2) return
        val maps = mapsFor(id, width, height) ?: return
        var left = amount
        var pass = 0
        while (left > 0f && pass < 2) {
            val alpha = (min(1f, left) * 255).roundToInt()
            multiply.alpha = alpha
            screen.alpha = alpha
            canvas.drawBitmap(maps.dark, 0f, 0f, multiply)
            canvas.drawBitmap(maps.light, 0f, 0f, screen)
            pass++
            left -= 1f
        }
    }

    @Synchronized
    private fun mapsFor(id: String, width: Int, height: Int): Maps? {
        val key = "$id|${width}x$height"
        cache.get(key)?.let { return it }
        val draw = painters[id] ?: return null
        val dark = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.WHITE) }
        val light = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.BLACK) }
        val seed = id.fold(7L) { acc, ch -> acc * 31 + ch.code }.toInt()
        val t = Tex(Canvas(dark), Canvas(light), dark, light, width.toFloat(), height.toFloat(), min(width, height) / 400f, Mulberry(seed))
        draw(t)
        return Maps(dark, light).also { cache.put(key, it) }
    }

    private class Tex(val d: Canvas, val l: Canvas, val dBmp: Bitmap, val lBmp: Bitmap, val w: Float, val h: Float, val k: Float, val rnd: Mulberry)

    private fun stroke(width: Float) = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = max(1f, width); strokeCap = Paint.Cap.BUTT; strokeJoin = Paint.Join.ROUND }
    private fun fill() = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private fun ink(dark: Boolean, alpha: Float): Int = Color.argb((alpha.coerceIn(0f, 1f) * 255).roundToInt(), if (dark) 0 else 255, if (dark) 0 else 255, if (dark) 0 else 255)
    private fun Paint.ink(dark: Boolean, alpha: Float) = apply { color = this@Textures.ink(dark, alpha) }

    /** Fine per-pixel noise: negative values darken (dark map), positive values lighten (light map). */
    private fun grain(t: Tex, amp: Float, block: Int = 1) {
        val w = max(1, ceil(t.w / block).toInt())
        val h = max(1, ceil(t.h / block).toInt())
        val a = IntArray(w * h)
        val b = IntArray(w * h)
        for (i in 0 until w * h) {
            val v = (t.rnd.next() - .5f) * 2f
            if (v < 0) {
                val g = (255 + v * amp).roundToInt().coerceIn(0, 255)
                a[i] = Color.rgb(g, g, g); b[i] = Color.BLACK
            } else {
                val g = (v * amp).roundToInt().coerceIn(0, 255)
                a[i] = Color.WHITE; b[i] = Color.rgb(g, g, g)
            }
        }
        val dst = RectF(0f, 0f, t.w, t.h)
        val smooth = Paint(Paint.FILTER_BITMAP_FLAG)
        val ba = Bitmap.createBitmap(a, w, h, Bitmap.Config.ARGB_8888)
        val bb = Bitmap.createBitmap(b, w, h, Bitmap.Config.ARGB_8888)
        smooth.xfermode = PorterDuffXfermode(PorterDuff.Mode.MULTIPLY)
        t.d.drawBitmap(ba, null, dst, smooth)
        smooth.xfermode = PorterDuffXfermode(PorterDuff.Mode.ADD)
        t.l.drawBitmap(bb, null, dst, smooth)
        ba.recycle(); bb.recycle()
    }

    /** Soft round patches, half dark and half light. */
    private fun blots(t: Tex, n: Int, rMin: Float, rMax: Float, aMax: Float) {
        for (i in 0 until n) {
            val x = t.rnd.next() * t.w
            val y = t.rnd.next() * t.h
            val r = (rMin + t.rnd.next() * (rMax - rMin)) * t.k
            val dark = t.rnd.next() < .5f
            val al = (.3f + t.rnd.next() * .7f) * aMax
            val c = if (dark) Color.argb((al * 255).roundToInt(), 0, 0, 0) else Color.argb((al * 255).roundToInt(), 255, 255, 255)
            val p = fill().apply { shader = RadialGradient(x, y, r, c, c and 0x00FFFFFF, Shader.TileMode.CLAMP) }
            (if (dark) t.d else t.l).drawRect(x - r, y - r, x + r, y + r, p)
        }
    }

    private fun Canvas.line(p: Paint, x1: Float, y1: Float, x2: Float, y2: Float) = drawLine(x1, y1, x2, y2, p)

    private val painters: Map<String, (Tex) -> Unit> = mapOf(
        "paper" to { t -> grain(t, 24f, max(1, t.k.roundToInt())); blots(t, 40, 40f, 160f, .07f) },
        "linen" to { t ->
            val k = t.k
            grain(t, 14f)
            val pd = stroke(k * .9f)
            val pl = stroke(k * .9f)
            var x = 0f
            while (x < t.w) {
                t.d.line(pd.ink(true, .04f + t.rnd.next() * .1f), x, 0f, x, t.h)
                t.l.line(pl.ink(false, .03f + t.rnd.next() * .07f), x + k, 0f, x + k, t.h)
                x += 3 * k
            }
            var y = 0f
            while (y < t.h) {
                t.d.line(pd.ink(true, .04f + t.rnd.next() * .1f), 0f, y, t.w, y)
                t.l.line(pl.ink(false, .03f + t.rnd.next() * .07f), 0f, y + k, t.w, y + k)
                y += 3 * k
            }
        },
        "canvas" to { t ->
            val k = t.k
            val c = 5 * k
            grain(t, 12f)
            val pd = stroke(k * .9f).ink(true, .09f)
            val pl = stroke(k * .9f).ink(false, .08f)
            var j = 0
            while (j * c < t.h) {
                var i = 0
                while (i * c < t.w) {
                    if ((i + j) % 2 == 0) {
                        t.d.line(pd, i * c, j * c + c * .5f, i * c + c, j * c + c * .5f)
                        t.l.line(pl, i * c, j * c + c * .5f + k, i * c + c, j * c + c * .5f + k)
                    } else {
                        t.d.line(pd, i * c + c * .5f, j * c, i * c + c * .5f, j * c + c)
                        t.l.line(pl, i * c + c * .5f + k, j * c, i * c + c * .5f + k, j * c + c)
                    }
                    i++
                }
                j++
            }
        },
        "kraft" to { t ->
            val k = t.k
            grain(t, 28f)
            val n = (1400 * (t.w * t.h) / 160000f).toInt()
            for (i in 0 until n) {
                val x = t.rnd.next() * t.w
                val y = t.rnd.next() * t.h
                val len = (6 + t.rnd.next() * 18) * k
                val a = t.rnd.next() * PI.toFloat()
                val dark = t.rnd.next() < .6f
                val p = stroke(k * .8f).ink(dark, .1f + t.rnd.next() * .16f)
                val path = Path().apply {
                    moveTo(x, y)
                    quadTo(x + cos(a) * len * .5f + k * 2, y + sin(a) * len * .5f, x + cos(a) * len, y + sin(a) * len)
                }
                (if (dark) t.d else t.l).drawPath(path, p)
            }
            blots(t, 24, 50f, 160f, .09f)
        },
        "concrete" to { t ->
            val k = t.k
            grain(t, 36f, max(1, k.roundToInt()))
            blots(t, 120, 20f, 110f, .11f)
            val n = (260 * (t.w * t.h) / 160000f).toInt()
            for (i in 0 until n) {
                val p = fill().ink(true, .25f + t.rnd.next() * .3f)
                t.d.drawCircle(t.rnd.next() * t.w, t.rnd.next() * t.h, (.6f + t.rnd.next() * 1.4f) * k, p)
            }
        },
        "marble" to { t ->
            val k = t.k
            blots(t, 50, 60f, 220f, .08f)
            grain(t, 8f)
            repeat(7) {
                var x = t.rnd.next() * t.w
                var y = t.rnd.next() * t.h * .3f
                val ang = (PI / 2).toFloat() + (t.rnd.next() - .5f) * 1.2f
                val w0 = (.5f + t.rnd.next() * 1.6f) * k
                val pts = mutableListOf(x to y)
                repeat(26) {
                    x += cos(ang + (t.rnd.next() - .5f) * .9f) * t.h / 20
                    y += sin(ang + (t.rnd.next() - .5f) * .9f) * t.h / 20
                    pts += x to y
                }
                listOf(Triple(t.d, true, .32f) to 0f, Triple(t.l, false, .55f) to k).forEach { (spec, off) ->
                    val (canvas, dark, alpha) = spec
                    val path = Path()
                    pts.forEachIndexed { i, (px, py) -> if (i == 0) path.moveTo(px + off, py) else path.lineTo(px + off, py) }
                    val p = stroke(w0).ink(dark, alpha)
                    canvas.drawPath(path, p)
                }
            }
        },
        "wood" to { t ->
            val k = t.k
            grain(t, 10f)
            var y = 0f
            while (y < t.h) {
                val dark = t.rnd.next() < .7f
                val canvas = if (dark) t.d else t.l
                val ph = t.rnd.next() * 6
                val amp = (2 + t.rnd.next() * 5) * k
                val p = stroke((.6f + t.rnd.next() * 1.4f) * k).ink(dark, .05f + t.rnd.next() * .2f)
                val path = Path()
                var x = 0f
                var first = true
                while (x <= t.w) {
                    val yy = y + sin(x / (60 * k) + ph) * amp + sin(x / (17 * k) + ph * 2) * k
                    if (first) { path.moveTo(x, yy); first = false } else path.lineTo(x, yy)
                    x += 8f
                }
                canvas.drawPath(path, p)
                y += 2.2f * k
            }
            repeat(2) {
                val cx = t.rnd.next() * t.w
                val cy = t.rnd.next() * t.h
                var r = 4
                while (r < 34) {
                    val p = stroke(k).ink(true, .16f - r / 300f)
                    t.d.drawOval(RectF(cx - r * 2.4f * k, cy - r * k, cx + r * 2.4f * k, cy + r * k), p)
                    r += 4
                }
            }
        },
        "dots" to { t ->
            val k = t.k
            val gap = 24 * k
            var j = 0
            var y = gap / 2
            while (y < t.h + gap) {
                var x = if (j % 2 == 1) gap else gap / 2
                while (x < t.w + gap) {
                    t.d.drawCircle(x, y, 5 * k, fill().ink(true, .13f))
                    t.l.drawCircle(x - k, y - k, 4.5f * k, fill().ink(false, .2f))
                    x += gap
                }
                y += gap; j++
            }
        },
        "stripes" to { t ->
            val bw = 14 * t.k
            val pd = fill().ink(true, .11f)
            val pl = fill().ink(false, .1f)
            var o = -t.h
            while (o < t.w) {
                t.d.drawPath(Path().apply { moveTo(o, 0f); lineTo(o + bw, 0f); lineTo(o + bw + t.h, t.h); lineTo(o + t.h, t.h); close() }, pd)
                t.l.drawPath(Path().apply { moveTo(o + bw, 0f); lineTo(o + bw * 2, 0f); lineTo(o + bw * 2 + t.h, t.h); lineTo(o + bw + t.h, t.h); close() }, pl)
                o += bw * 2
            }
        },
        "grid" to { t ->
            val k = t.k
            val g = 18 * k
            var i = 0
            var x = 0f
            while (x <= t.w) {
                val major = i % 5 == 0
                t.d.line(stroke(k * if (major) 1.1f else .7f).ink(true, if (major) .26f else .12f), x, 0f, x, t.h)
                t.l.line(stroke(k).ink(false, .12f), x + k, 0f, x + k, t.h)
                x += g; i++
            }
            i = 0
            var y = 0f
            while (y <= t.h) {
                val major = i % 5 == 0
                t.d.line(stroke(k * if (major) 1.1f else .7f).ink(true, if (major) .26f else .12f), 0f, y, t.w, y)
                t.l.line(stroke(k).ink(false, .12f), 0f, y + k, t.w, y + k)
                y += g; i++
            }
        },
        "gingham" to { t ->
            val c = 16 * t.k
            var j = 0
            while (j * c < t.h) {
                t.d.drawRect(0f, j * c, t.w, j * c + c, fill().ink(true, .1f))
                t.l.drawRect(0f, (j + 1) * c, t.w, (j + 2) * c, fill().ink(false, .05f))
                j += 2
            }
            var i = 0
            while (i * c < t.w) {
                t.d.drawRect(i * c, 0f, i * c + c, t.h, fill().ink(true, .1f))
                t.l.drawRect((i + 1) * c, 0f, (i + 2) * c, t.h, fill().ink(false, .05f))
                i += 2
            }
            grain(t, 8f)
        },
        "watercolor" to { t -> grain(t, 14f, max(1, t.k.roundToInt())); blots(t, 46, 70f, 260f, .2f); blots(t, 30, 30f, 120f, .12f) },
        "bokeh" to { t ->
            val k = t.k
            for (i in 0 until 44) {
                val x = t.rnd.next() * t.w
                val y = t.rnd.next() * t.h
                val r = (10 + t.rnd.next() * 38) * k
                t.l.drawCircle(x, y, r, fill().ink(false, .05f + t.rnd.next() * .12f))
                t.l.drawCircle(x, y, r, stroke(k).ink(false, .1f + t.rnd.next() * .12f))
                t.d.drawCircle(x + 3 * k, y + 3 * k, r, fill().ink(true, .025f))
            }
        },
        "stars" to { t ->
            val k = t.k
            val n = (150 * (t.w * t.h) / 160000f).toInt()
            for (i in 0 until n) {
                val x = t.rnd.next() * t.w
                val y = t.rnd.next() * t.h
                val r = (1 + t.rnd.next() * 3.4f) * k
                val a = .3f + t.rnd.next() * .6f
                val path = Path().apply {
                    moveTo(x, y - r * 2); quadTo(x, y, x + r * 2, y); quadTo(x, y, x, y + r * 2); quadTo(x, y, x - r * 2, y); quadTo(x, y, x, y - r * 2); close()
                }
                t.l.drawPath(path, fill().ink(false, a))
            }
        },
        "crumple" to { t ->
            val k = t.k
            grain(t, 10f)
            for (i in 0 until 70) {
                val cx = t.rnd.next() * t.w
                val cy = t.rnd.next() * t.h
                val r = (40 + t.rnd.next() * 130) * k
                val a = t.rnd.next() * 6
                val dark = t.rnd.next() < .5f
                val p = fill().ink(dark, .04f + t.rnd.next() * .1f)
                val sides = 3 + (t.rnd.next() * 3).toInt()
                val path = Path()
                for (s in 0 until sides) {
                    val ang = a + s * (6.28f / 4) + (t.rnd.next() - .5f)
                    val px = cx + cos(ang) * r
                    val py = cy + sin(ang) * r
                    if (s == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                path.close()
                (if (dark) t.d else t.l).drawPath(path, p)
            }
        },
        "denim" to { t ->
            val k = t.k
            grain(t, 26f)
            val g = 3 * k
            var o = -t.h
            while (o < t.w) {
                t.d.line(stroke(k).ink(true, .13f), o, 0f, o + t.h, t.h)
                t.l.line(stroke(k).ink(false, .09f), o + k * 1.5f, 0f, o + t.h + k * 1.5f, t.h)
                o += g
            }
        },
    )

}
