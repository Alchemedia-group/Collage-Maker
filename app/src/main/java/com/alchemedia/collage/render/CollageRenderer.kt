package com.alchemedia.collage.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import com.alchemedia.collage.model.CellContent
import com.alchemedia.collage.model.CellRect
import com.alchemedia.collage.model.CollageGeometry
import com.alchemedia.collage.model.CollageProject
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** Extras drawn only on screen (never exported). */
data class UiOptions(
    val selectedCell: Int = -1,
    val swapping: Boolean = false,
    val selectedItem: String? = null,
    val accent: Int = 0xFF2557D6.toInt(),
    val dpr: Float = 1f,
)

/**
 * Where a photo sits inside its cell: the centre of the whole bitmap, its size once scaled, how far it is turned (degrees,
 * clockwise) and how far it can slide along its own axes before the cell would show a gap ([slackX], [slackY]).
 */
data class PhotoPlacement(val cx: Float, val cy: Float, val width: Float, val height: Float, val rotation: Float, val slackX: Float, val slackY: Float)

/**
 * Draws a collage onto any Canvas. The editor, the previews and the exported picture all go through this one
 * function, so what you see is what you save.
 */
object CollageRenderer {
    private val clear = PorterDuffXfermode(PorterDuff.Mode.CLEAR)

    /**
     * Inside a layer: removes everything except [keep]. (A Porter-Duff mode only touches the pixels a shape
     * covers, so the mask is applied by clearing the inverse of the shape.)
     */
    private fun clearOutside(canvas: Canvas, keep: Path) {
        val inverse = Path(keep).apply { fillType = Path.FillType.INVERSE_WINDING }
        canvas.drawPath(inverse, Paint(Paint.ANTI_ALIAS_FLAG).apply { xfermode = clear; style = Paint.Style.FILL })
    }

    fun draw(canvas: Canvas, project: CollageProject, w: Float, h: Float, photos: PhotoSource, ui: UiOptions? = null, watermark: Boolean = false) {
        canvas.drawColor(project.background.toInt())
        project.backgroundImage?.let { ref ->
            photos.bitmap(ref.id)?.let { bmp ->
                // Fill the canvas, cropping the picture's edges rather than stretching it.
                val s = max(w / bmp.width, h / bmp.height)
                val dw = bmp.width * s
                val dh = bmp.height * s
                canvas.drawBitmap(bmp, null, RectF((w - dw) / 2, (h - dh) / 2, (w + dw) / 2, (h + dh) / 2), Paint(Paint.FILTER_BITMAP_FLAG))
            }
        }
        Textures.draw(canvas, project.texture, project.textureAmount, w.toInt(), h.toInt())
        val rects = CollageGeometry.cellRects(project.layout.cells, w, h, project.gapPercent, project.marginPercent)
        val dpr = ui?.dpr ?: 1f
        rects.forEachIndexed { i, r ->
            val content = project.cells.getOrNull(i) ?: CellContent()
            val bmp = content.photoId?.let { photos.bitmap(it) }
            if (bmp != null) drawPhotoCell(canvas, r, content, bmp, project, i)
            else if (ui != null) drawEmptyCell(canvas, r, project, dpr)
        }
        if (ui != null && ui.selectedCell in rects.indices) drawCellSelection(canvas, rects[ui.selectedCell], project, ui)
        project.items.forEach { ItemRenderer.draw(canvas, it, w, h) }
        ui?.selectedItem?.let { id -> project.items.firstOrNull { it.id == id } }?.let { ItemRenderer.drawSelection(canvas, it, w, h, dpr, ui.accent) }
        if (watermark) drawWatermark(canvas, w, h)
    }

    /** Renders the finished collage into a new bitmap. */
    fun render(project: CollageProject, width: Int, height: Int, photos: PhotoSource, watermark: Boolean): Bitmap {
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        draw(Canvas(bmp), project, width.toFloat(), height.toFloat(), photos, null, watermark)
        return bmp
    }

    /**
     * Where the photo goes for a cell: it is scaled to cover [rw] x [rh] at (rx, ry) however it is turned, zoomed by
     * the cell's zoom, then slid by the pan. Pan runs along the photo's own axes, so a turned photo still never shows a gap.
     */
    fun photoPlacement(rx: Float, ry: Float, rw: Float, rh: Float, cell: CellContent, imgW: Int, imgH: Int): PhotoPlacement {
        val deg = cell.rotation
        val a = Math.toRadians(deg.toDouble())
        val cosA = cos(a).toFloat()
        val sinA = sin(a).toFloat()
        // The cell as seen in the photo's own turned frame: the box that must stay covered.
        val needW = rw * abs(cosA) + rh * abs(sinA)
        val needH = rw * abs(sinA) + rh * abs(cosA)
        val s = max(needW / imgW, needH / imgH) * cell.zoom
        val dw = imgW * s
        val dh = imgH * s
        val slackX = (dw - needW) / 2
        val slackY = (dh - needH) / 2
        val lx = slackX * cell.panX
        val ly = slackY * cell.panY
        return PhotoPlacement(rx + rw / 2 + lx * cosA - ly * sinA, ry + rh / 2 + lx * sinA + ly * cosA, dw, dh, deg, slackX, slackY)
    }

    /** The square a shape's photo has to cover (shapes may be scaled and rotated inside the cell). */
    fun photoRect(r: CellRect, project: CollageProject? = null): RectF {
        val d = r.cell
        if (!d.isShape || d.fill) {
            val rect = RectF(r.x, r.y, r.right, r.bottom)
            // Inside a frame the photo only fills what the frame leaves free.
            return if (project != null && !d.isShape && !d.isLetter && project.frame != Frames.NONE) Frames.inner(project.frame, rect, project.frameAmount) else rect
        }
        val side = min(r.width, r.height) * d.scale
        val a = Math.toRadians(d.rotation.toDouble())
        val s = (side * (abs(cos(a)) + abs(sin(a)))).toFloat()
        val cx = r.x + r.width / 2
        val cy = r.y + r.height / 2
        return RectF(cx - s / 2, cy - s / 2, cx + s / 2, cy + s / 2)
    }

    /** The outline of a cell: shape, rounded rectangle or plain rectangle. */
    fun cellOutline(r: CellRect, project: CollageProject): Path {
        val d = r.cell
        if (d.isShape) return Shapes.place(d.shape, r.x, r.y, r.width, r.height, d.fill, d.scale, d.rotation)
        val rad = min(CollageGeometry.cornerRadius(r, project.cornerPercent), min(r.width, r.height) / 2)
        return Path().apply { addRoundRect(RectF(r.x, r.y, r.right, r.bottom), rad, rad, Path.Direction.CW) }
    }

    private fun drawBitmapIn(canvas: Canvas, bmp: Bitmap, pl: PhotoPlacement, content: CellContent, project: CollageProject) {
        val dst = RectF(-pl.width / 2, -pl.height / 2, pl.width / 2, pl.height / 2)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
        val look = Filters.adjust(content.brightness, content.contrast, content.saturation)
        val filter = Filters.matrix(project.filter)
        val amount = project.filterAmount.coerceIn(0f, 1f)
        canvas.save()
        canvas.translate(pl.cx, pl.cy)
        canvas.rotate(pl.rotation)
        if (content.flipH || content.flipV) canvas.scale(if (content.flipH) -1f else 1f, if (content.flipV) -1f else 1f)
        if (filter == null || amount <= 0f) {
            paint.colorFilter = look?.let { ColorMatrixColorFilter(it) }
            canvas.drawBitmap(bmp, null, dst, paint)
        } else {
            // Below full strength the filtered copy is blended over the original.
            if (amount < .999f) {
                paint.colorFilter = look?.let { ColorMatrixColorFilter(it) }
                canvas.drawBitmap(bmp, null, dst, paint)
            }
            val both = if (look == null) filter else ColorMatrix(look).also { it.postConcat(filter) }
            paint.colorFilter = ColorMatrixColorFilter(both)
            if (amount < .999f) paint.alpha = (amount * 255).toInt()
            canvas.drawBitmap(bmp, null, dst, paint)
        }
        canvas.restore()
    }

    /** Draws a photo into [rect] with rounded corners of [radius] (a straight clip when there is no rounding). */
    private fun drawPhotoClipped(canvas: Canvas, bmp: Bitmap, content: CellContent, project: CollageProject, rect: RectF, radius: Float) {
        val pl = photoPlacement(rect.left, rect.top, rect.width(), rect.height(), content, bmp.width, bmp.height)
        if (radius <= .5f) {
            canvas.save()
            canvas.clipRect(rect)
            drawBitmapIn(canvas, bmp, pl, content, project)
            canvas.restore()
            return
        }
        // Masking through a layer gives smooth (anti-aliased) edges for rounded corners.
        val layer = canvas.saveLayer(RectF(rect).apply { inset(-2f, -2f) }, null)
        drawBitmapIn(canvas, bmp, pl, content, project)
        clearOutside(canvas, Path().apply { addRoundRect(rect, radius, radius, Path.Direction.CW) })
        canvas.restoreToCount(layer)
    }

    private fun drawPhotoCell(canvas: Canvas, r: CellRect, content: CellContent, bmp: Bitmap, project: CollageProject, index: Int) {
        val d = r.cell
        if (d.isLetter) { drawLetterCell(canvas, r, content, bmp, project); return }
        if (!d.isShape && project.frame != Frames.NONE) {
            val rect = RectF(r.x, r.y, r.right, r.bottom)
            val radius = min(CollageGeometry.cornerRadius(r, project.cornerPercent), min(r.width, r.height) / 2)
            Frames.draw(canvas, project.frame, rect, radius, project.frameColor.toInt(), project.frameAmount, index) { inner, innerRadius ->
                drawPhotoClipped(canvas, bmp, content, project, inner, innerRadius)
            }
            return
        }
        val pr = photoRect(r)
        val pl = photoPlacement(pr.left, pr.top, pr.width(), pr.height(), content, bmp.width, bmp.height)
        val radius = CollageGeometry.cornerRadius(r, project.cornerPercent)
        if (!d.isShape && radius <= 0.5f) {
            canvas.save()
            canvas.clipRect(r.x, r.y, r.right, r.bottom)
            drawBitmapIn(canvas, bmp, pl, content, project)
            canvas.restore()
            return
        }
        // Masking through a layer gives smooth (anti-aliased) edges for rounded corners and shapes.
        val bounds = RectF(r.x - 2, r.y - 2, r.right + 2, r.bottom + 2).also { if (d.isShape) it.union(pr) }
        val layer = canvas.saveLayer(bounds, null)
        drawBitmapIn(canvas, bmp, pl, content, project)
        clearOutside(canvas, cellOutline(r, project))
        canvas.restoreToCount(layer)
    }

    private fun letterPaint(): Paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
        typeface = Fonts.typeface("bric", true)
        textAlign = Paint.Align.LEFT
    }

    /** Text size and origin that make [text] fill a [w] x [h] box. */
    private fun letterFit(text: String, w: Float, h: Float): Triple<Paint, Float, Float> {
        val p = letterPaint()
        p.textSize = h * .95f
        val b = android.graphics.Rect()
        p.getTextBounds(text, 0, text.length, b)
        val bw = max(1, b.width())
        val bh = max(1, b.height())
        p.textSize = p.textSize * min(w * .97f / bw, h * .97f / bh)
        p.getTextBounds(text, 0, text.length, b)
        val x = (w - b.width()) / 2f - b.left
        val y = h / 2f - (b.top + b.bottom) / 2f
        return Triple(p, x, y)
    }

    private fun drawLetterCell(canvas: Canvas, r: CellRect, content: CellContent, bmp: Bitmap, project: CollageProject) {
        val text = r.cell.text ?: return
        val bounds = RectF(r.x, r.y, r.right, r.bottom)
        val layer = canvas.saveLayer(bounds, null)
        val pl = photoPlacement(r.x, r.y, r.width, r.height, content, bmp.width, bmp.height)
        drawBitmapIn(canvas, bmp, pl, content, project)
        val (p, x, y) = letterFit(text, r.width, r.height)
        val glyphs = Path()
        p.getTextPath(text, 0, text.length, r.x + x, r.y + y, glyphs)
        clearOutside(canvas, glyphs)
        canvas.restoreToCount(layer)
    }

    private fun drawEmptyCell(canvas: Canvas, r: CellRect, project: CollageProject, dpr: Float) {
        if (r.cell.isLetter) {
            val (p, x, y) = letterFit(r.cell.text.orEmpty(), r.width, r.height)
            p.color = (0x66 shl 24) or 0x8084A0
            canvas.drawText(r.cell.text.orEmpty(), r.x + x, r.y + y, p)
        } else {
            val path = cellOutline(r, project)
            canvas.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = (0x33 shl 24) or 0x8084A0 })
            canvas.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE; strokeWidth = 1.5f * dpr; color = (0xCC shl 24) or 0x8084A0
                pathEffect = DashPathEffect(floatArrayOf(6 * dpr, 5 * dpr), 0f)
            })
        }
        val cx = r.x + r.width / 2
        val cy = r.y + r.height / 2
        val arm = min(12 * dpr, min(r.width, r.height) / 5)
        val plus = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 2 * dpr; strokeCap = Paint.Cap.ROUND; color = (0xF2 shl 24) or 0x8084A0 }
        canvas.drawLine(cx - arm, cy, cx + arm, cy, plus)
        canvas.drawLine(cx, cy - arm, cx, cy + arm, plus)
    }

    private fun drawCellSelection(canvas: Canvas, r: CellRect, project: CollageProject, ui: UiOptions) {
        val dpr = ui.dpr
        val path = cellOutline(r, project)
        val white = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = 6 * dpr; color = Color.WHITE
            if (ui.swapping) pathEffect = DashPathEffect(floatArrayOf(9 * dpr, 7 * dpr), 0f)
        }
        canvas.drawPath(path, white)
        canvas.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 3 * dpr; color = ui.accent })
        val box = if (r.cell.isShape && !r.cell.fill) photoRect(r) else RectF(r.x, r.y, r.right, r.bottom)
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 2.5f * dpr; color = ui.accent }
        listOf(box.left to box.top, box.right to box.top, box.left to box.bottom, box.right to box.bottom).forEach { (x, y) ->
            canvas.drawCircle(x, y, 6.5f * dpr, fill)
            canvas.drawCircle(x, y, 6.5f * dpr, ring)
        }
    }

    /** The "Photo Collage" pill in the bottom right corner of free exports. */
    fun drawWatermark(canvas: Canvas, w: Float, h: Float) {
        val s = max(14f, min(w, h) * .034f)
        val label = "Photo Collage"
        val text = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
            typeface = Fonts.typeface("figtree", true)
            textSize = s
            color = Color.WHITE
        }
        val tw = text.measureText(label)
        val padX = s * .75f
        val icon = s * 1.1f
        val pw = tw + padX * 2 + icon + s * .5f
        val ph = s * 2.1f
        val x = w - pw - s
        val y = h - ph - s
        canvas.drawRoundRect(RectF(x, y, x + pw, y + ph), ph / 2, ph / 2, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb((.9f * .8f * 255).toInt(), 16, 24, 40) })
        val ix = x + padX
        val iy = y + (ph - icon) / 2
        val u = icon / 2 - 1
        val light = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF9DB8FF.toInt() }
        val white = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        canvas.drawRoundRect(RectF(ix, iy, ix + u, iy + icon), 2f, 2f, light)
        canvas.drawRoundRect(RectF(ix + icon / 2 + 1, iy + icon * .55f, ix + icon / 2 + 1 + u, iy + icon), 2f, 2f, light)
        canvas.drawRoundRect(RectF(ix + icon / 2 + 1, iy, ix + icon / 2 + 1 + u, iy + icon * .45f), 2f, 2f, white)
        val fm = text.fontMetrics
        canvas.drawText(label, ix + icon + s * .5f, y + ph / 2 + 1 - (fm.ascent + fm.descent) / 2, text)
    }

    /** Export size: the longer side is [longSide] pixels. */
    fun exportSize(ratio: Float, longSide: Int): Pair<Int, Int> =
        if (ratio >= 1f) longSide to Math.round(longSide / ratio) else Math.round(longSide * ratio) to longSide
}
