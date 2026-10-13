package com.alchemedia.collage.model

import kotlin.math.max
import kotlin.math.min

data class CellRect(val x: Float, val y: Float, val width: Float, val height: Float)

object CollageGeometry {
    private const val EPS = 1e-6f

    /**
     * Pixel rectangles for every cell of [layout] on a [width] x [height] canvas.
     * [gapPercent] and [marginPercent] are percentages of the shorter canvas side. The gap is
     * only inserted between cells, never between a cell and the canvas edge.
     */
    fun cellRects(
        layout: GridLayout,
        width: Float,
        height: Float,
        gapPercent: Float,
        marginPercent: Float,
    ): List<CellRect> {
        val short = min(width, height)
        val margin = marginPercent / 100f * short
        val halfGap = gapPercent / 100f * short / 2f
        val contentW = width - 2 * margin
        val contentH = height - 2 * margin
        return layout.cells.map { (x, y, w, h) ->
            val left = if (x < EPS) 0f else halfGap
            val top = if (y < EPS) 0f else halfGap
            val right = if (x + w > 1 - EPS) 0f else halfGap
            val bottom = if (y + h > 1 - EPS) 0f else halfGap
            CellRect(
                x = margin + x * contentW + left,
                y = margin + y * contentH + top,
                width = max(1f, w * contentW - left - right),
                height = max(1f, h * contentH - top - bottom),
            )
        }
    }

    /** [cornerPercent] 100 makes the shorter side fully round. */
    fun cornerRadius(rect: CellRect, cornerPercent: Float): Float =
        cornerPercent / 100f * (min(rect.width, rect.height) / 2f)
}
