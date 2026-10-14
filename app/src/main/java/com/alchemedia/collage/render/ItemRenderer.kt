package com.alchemedia.collage.render

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import com.alchemedia.collage.model.CanvasItem
import com.alchemedia.collage.model.StickerKind
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign
import kotlin.math.sin

/** One laid-out character: its centre, relative to the item centre, and its rotation in radians. */
internal class PlacedChar(val ch: String, val x: Float, val y: Float, val rot: Float)

internal class TextLayoutResult(val chars: List<PlacedChar>, val box: RectF)

/** Text and sticker drawing, plus the geometry the editor needs for hit testing and handles. */
object ItemRenderer {
    private const val EMOJI_PAD = 1.25f

    private fun paintFor(item: CanvasItem, px: Float): Paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
        val def = com.alchemedia.collage.model.FontCatalog.byId(item.font)
        typeface = Fonts.typeface(def, item.bold)
        isFakeBoldText = Fonts.needsFakeBold(def, item.bold)
        textSize = px
        textAlign = Paint.Align.CENTER
    }

    private fun baselineForMiddle(p: Paint): Float {
        val fm = p.fontMetrics
        return -(fm.ascent + fm.descent) / 2f
    }

    internal fun layout(item: CanvasItem, px: Float, measure: Paint = paintFor(item, px)): TextLayoutResult {
        val lines = item.text.split('\n')
        val sp = item.spacing * px
        val lh = px * 1.18f
        class LineGeo(val chars: List<String>, val widths: List<Float>, val total: Float)
        val geo = lines.map { line ->
            val chars = line.codePoints().toArray().map { String(Character.toChars(it)) }
            val ws = chars.map { measure.measureText(it) }
            LineGeo(chars, ws, ws.sum() + sp * max(0, chars.size - 1))
        }
        val maxW = max(1f, geo.maxOf { it.total })
        val curved = geo.size == 1 && abs(item.curve) > 1f
        val out = ArrayList<PlacedChar>()
        var minX = 1e9f; var maxX = -1e9f; var minY = 1e9f; var maxY = -1e9f
        geo.forEachIndexed { k, g ->
            val cy = (k - (geo.size - 1) / 2f) * lh
            val start = when (item.align) {
                "left" -> -maxW / 2
                "right" -> maxW / 2 - g.total
                else -> -g.total / 2
            }
            var cursor = start
            val sweep = abs(item.curve) / 100f * Math.PI.toFloat() * 1.15f
            val r0 = if (curved) g.total / sweep else 0f
            val sag = if (curved) r0 * (1 - cos(sweep / 2)) else 0f
            val sgn = sign(item.curve)
            g.chars.forEachIndexed { i, ch ->
                val cx = cursor + g.widths[i] / 2
                cursor += g.widths[i] + sp
                var x = cx; var y = cy; var rot = 0f
                if (curved) {
                    val phi = cx / r0
                    x = r0 * sin(phi)
                    y = if (sgn > 0) r0 * (1 - cos(phi)) - sag / 2 else -(r0 * (1 - cos(phi))) + sag / 2
                    rot = if (sgn > 0) phi else -phi
                }
                out += PlacedChar(ch, x, y, rot)
                minX = min(minX, x - g.widths[i] / 2 - px * .1f); maxX = max(maxX, x + g.widths[i] / 2 + px * .1f)
                minY = min(minY, y - lh / 2); maxY = max(maxY, y + lh / 2)
            }
        }
        if (out.isEmpty()) { minX = -px; maxX = px; minY = -lh / 2; maxY = lh / 2 }
        return TextLayoutResult(out, RectF(minX, minY, maxX, maxY))
    }

    fun textPx(item: CanvasItem, w: Float, h: Float) = max(4f, item.size * min(w, h))
    fun stickerSide(item: CanvasItem, w: Float, h: Float) = item.size * min(w, h) * EMOJI_PAD

    /** Width and height of the item before rotation, without selection padding. */
    fun box(item: CanvasItem, w: Float, h: Float): Pair<Float, Float> {
        if (!item.isText) return stickerSide(item, w, h).let { it to it }
        val px = textPx(item, w, h)
        val l = layout(item, px)
        val pad = if (item.bg != null) px * .28f else px * .08f
        return (l.box.width() + pad * 2) to (l.box.height() + pad * 2)
    }

    /** The item's frame on a canvas of [w] x [h], padded a little for easier touching. */
    class Frame(val cx: Float, val cy: Float, val w: Float, val h: Float, private val cos: Float, private val sin: Float) {
        fun toWorld(lx: Float, ly: Float) = (cx + lx * cos - ly * sin) to (cy + lx * sin + ly * cos)
        fun toLocal(px: Float, py: Float) = ((px - cx) * cos + (py - cy) * sin) to (-(px - cx) * sin + (py - cy) * cos)
        fun contains(px: Float, py: Float): Boolean = toLocal(px, py).let { (x, y) -> abs(x) <= w / 2 && abs(y) <= h / 2 }
    }

    fun frame(item: CanvasItem, w: Float, h: Float): Frame {
        val (bw, bh) = box(item, w, h)
        val a = Math.toRadians(item.rot.toDouble())
        val pad = min(w, h) * .012f
        return Frame(item.x * w, item.y * h, bw + pad * 2, bh + pad * 2, cos(a).toFloat(), sin(a).toFloat())
    }

    /** Topmost item under the point, if any. */
    fun hit(items: List<CanvasItem>, px: Float, py: Float, w: Float, h: Float): CanvasItem? =
        items.lastOrNull { frame(it, w, h).contains(px, py) }

    enum class Handle { Scale, Delete }

    fun handleAt(item: CanvasItem, px: Float, py: Float, w: Float, h: Float, dpr: Float): Handle? {
        val f = frame(item, w, h)
        val r = 20f * dpr
        val del = f.toWorld(-f.w / 2, -f.h / 2)
        val rs = f.toWorld(f.w / 2, f.h / 2)
        if (kotlin.math.hypot(px - rs.first, py - rs.second) <= r) return Handle.Scale
        if (kotlin.math.hypot(px - del.first, py - del.second) <= r) return Handle.Delete
        return null
    }

    fun draw(canvas: Canvas, item: CanvasItem, w: Float, h: Float) {
        if (item.isText) drawText(canvas, item, w, h) else drawSticker(canvas, item, w, h)
    }

    private fun alphaOf(color: Long, opacity: Float): Int {
        val a = ((color shr 24) and 0xFF) / 255f * opacity
        return ((a * 255).toInt() shl 24) or (color.toInt() and 0x00FFFFFF)
    }

    private fun drawText(canvas: Canvas, item: CanvasItem, w: Float, h: Float) {
        val px = textPx(item, w, h)
        val paint = paintFor(item, px)
        val lay = layout(item, px, paint)
        val base = baselineForMiddle(paint)
        canvas.save()
        canvas.translate(item.x * w, item.y * h)
        canvas.rotate(item.rot)
        val op = item.opacity

        item.bg?.let {
            val pad = px * .28f
            val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = alphaOf(it, op) }
            val r = RectF(lay.box.left - pad, lay.box.top - pad, lay.box.right + pad, lay.box.bottom + pad)
            canvas.drawRoundRect(r, min(px * .3f, r.width() / 2), min(px * .3f, r.height() / 2), bg)
        }

        val sh = item.shadow
        val hasStroke = item.strokeW > 0f
        fun each(p: Paint, dx: Float = 0f, dy: Float = 0f, shaderFor: ((PlacedChar) -> Shader)? = null) = lay.chars.forEach { q ->
            canvas.save()
            canvas.translate(q.x + dx, q.y + dy)
            canvas.rotate(Math.toDegrees(q.rot.toDouble()).toFloat())
            if (shaderFor != null) p.shader = shaderFor(q)
            canvas.drawText(q.ch, 0f, base, p)
            canvas.restore()
        }
        fun withShadow(p: Paint) {
            if (sh == null) return
            if (sh.blur > 0f) p.setShadowLayer(sh.blur * px * .9f + .01f, sh.dx * px, sh.dy * px, alphaOf(sh.color, op))
        }
        // A hard (blur-free) shadow is a second copy of the glyphs drawn offset underneath.
        val hardShadow = sh != null && sh.blur <= 0f
        if (sh != null && hardShadow) {
            val sp = Paint(paint).apply {
                color = alphaOf(sh.color, op)
                if (hasStroke) { style = Paint.Style.STROKE; strokeWidth = item.strokeW * px; strokeJoin = Paint.Join.ROUND; strokeMiter = 2f } else style = Paint.Style.FILL
            }
            if (hasStroke || item.fill != null) each(sp, sh.dx * px, sh.dy * px)
        }
        if (hasStroke) {
            val sp = Paint(paint).apply {
                style = Paint.Style.STROKE; strokeWidth = item.strokeW * px; strokeJoin = Paint.Join.ROUND; strokeMiter = 2f
                color = alphaOf(item.strokeColor, op)
                withShadow(this)
            }
            each(sp)
        }
        val fillColor = item.fill
        if (fillColor != null) {
            val fp = Paint(paint).apply {
                style = Paint.Style.FILL
                color = alphaOf(fillColor, op)
                if (!hasStroke) withShadow(this)
            }
            val end = item.fill2
            if (end == null) each(fp)
            else each(fp, shaderFor = { q -> gradientShader(item, lay.box, q, alphaOf(fillColor, op), alphaOf(end, op)) })
        } else if (sh != null && !hasStroke && sh.blur > 0f) {
            // Hollow text with a glow: the shadow of a hairline stroke.
            val gp = Paint(paint).apply {
                style = Paint.Style.STROKE; strokeWidth = 1f; color = alphaOf(sh.color, op) and 0x01FFFFFF
                withShadow(this)
            }
            each(gp)
        }
        canvas.restore()
    }

    /**
     * The text's colour fade, laid across the whole text box at the item's gradient angle. Each letter is drawn in its
     * own turned and shifted space, so the shader is mapped back into the item's space for that letter.
     */
    private fun gradientShader(item: CanvasItem, box: RectF, q: PlacedChar, from: Int, to: Int): Shader {
        val a = Math.toRadians(item.gradientAngle.toDouble())
        val dx = cos(a).toFloat()
        val dy = sin(a).toFloat()
        val half = max(1f, abs(box.width() / 2 * dx) + abs(box.height() / 2 * dy))
        val cx = box.centerX()
        val cy = box.centerY()
        val shader = LinearGradient(cx - dx * half, cy - dy * half, cx + dx * half, cy + dy * half, from, to, Shader.TileMode.CLAMP)
        shader.setLocalMatrix(Matrix().apply {
            setTranslate(-q.x, -q.y)
            postRotate(-Math.toDegrees(q.rot.toDouble()).toFloat())
        })
        return shader
    }

    private fun drawSticker(canvas: Canvas, item: CanvasItem, w: Float, h: Float) {
        val side = stickerSide(item, w, h)
        canvas.save()
        canvas.translate(item.x * w, item.y * h)
        canvas.rotate(item.rot)
        if (item.flip) canvas.scale(-1f, 1f)
        if (item.kind == StickerKind.SHAPE) {
            canvas.scale(side / 100f, side / 100f)
            canvas.translate(-50f, -50f)
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = alphaOf(item.stickerFill, item.opacity) }
            canvas.drawPath(Shapes.base(item.shape), p)
        } else {
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = side / EMOJI_PAD
                textAlign = Paint.Align.CENTER
                alpha = (item.opacity * 255).toInt()
            }
            canvas.drawText(item.emoji, 0f, baselineForMiddle(p) + side * .04f, p)
        }
        canvas.restore()
    }

    /** The selection frame with a delete knob (top left) and a scale/rotate knob (bottom right). */
    fun drawSelection(canvas: Canvas, item: CanvasItem, w: Float, h: Float, dpr: Float, accent: Int) {
        val f = frame(item, w, h)
        canvas.save()
        canvas.translate(f.cx, f.cy)
        canvas.rotate(item.rot)
        val r = RectF(-f.w / 2, -f.h / 2, f.w / 2, f.h / 2)
        val white = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 5 * dpr; color = Color.WHITE }
        canvas.drawRect(r, white)
        val dash = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = 2 * dpr; color = accent
            pathEffect = android.graphics.DashPathEffect(floatArrayOf(7 * dpr, 5 * dpr), 0f)
        }
        canvas.drawRect(r, dash)
        knob(canvas, -f.w / 2, -f.h / 2, dpr, accent) { c, p ->
            c.drawLine(-4.5f * dpr, -4.5f * dpr, 4.5f * dpr, 4.5f * dpr, p)
            c.drawLine(4.5f * dpr, -4.5f * dpr, -4.5f * dpr, 4.5f * dpr, p)
        }
        knob(canvas, f.w / 2, f.h / 2, dpr, accent) { c, p ->
            c.drawArc(RectF(-5.5f * dpr, -5.5f * dpr, 5.5f * dpr, 5.5f * dpr), 27f, 280f, false, p)
            c.drawLine(3 * dpr, -7.5f * dpr, 6.2f * dpr, -3.4f * dpr, p)
            c.drawLine(6.2f * dpr, -3.4f * dpr, 1.2f * dpr, -2.6f * dpr, p)
        }
        canvas.restore()
    }

    private fun knob(canvas: Canvas, x: Float, y: Float, dpr: Float, accent: Int, glyph: (Canvas, Paint) -> Unit) {
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 2.5f * dpr; color = accent }
        canvas.drawCircle(x, y, 12 * dpr, fill)
        canvas.drawCircle(x, y, 12 * dpr, ring)
        canvas.save()
        canvas.translate(x, y)
        glyph(canvas, Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 2.2f * dpr; strokeCap = Paint.Cap.ROUND; color = accent })
        canvas.restore()
    }
}
