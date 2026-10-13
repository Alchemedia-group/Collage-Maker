package com.alchemedia.collage.model

import kotlin.math.max
import kotlin.math.min

data class CellRect(val x: Float, val y: Float, val width: Float, val height: Float, val cell: LayoutCell) {
    val right: Float get() = x + width
    val bottom: Float get() = y + height
}

object CollageGeometry {
    private const val EPS = 1e-6f

    /**
     * Pixel rectangles for [cells] on a [width] x [height] canvas. [gapPercent] and [marginPercent] are
     * percentages of the shorter side. The gap is only inserted between cells, never at the canvas edge.
     */
    fun cellRects(cells: List<LayoutCell>, width: Float, height: Float, gapPercent: Float, marginPercent: Float): List<CellRect> {
        val short = min(width, height)
        val margin = marginPercent / 100f * short
        val halfGap = gapPercent / 100f * short / 2f
        val contentW = width - 2 * margin
        val contentH = height - 2 * margin
        return cells.map { c ->
            val left = if (c.x < EPS) 0f else halfGap
            val top = if (c.y < EPS) 0f else halfGap
            val right = if (c.x + c.w > 1 - EPS) 0f else halfGap
            val bottom = if (c.y + c.h > 1 - EPS) 0f else halfGap
            CellRect(
                x = margin + c.x * contentW + left,
                y = margin + c.y * contentH + top,
                width = max(1f, c.w * contentW - left - right),
                height = max(1f, c.h * contentH - top - bottom),
                cell = c,
            )
        }
    }

    /** [cornerPercent] 100 makes the shorter side fully round. Shapes and letters ignore it. */
    fun cornerRadius(rect: CellRect, cornerPercent: Float): Float =
        if (rect.cell.isShape || rect.cell.isLetter) 0f else cornerPercent / 100f * (min(rect.width, rect.height) / 2f)
}
