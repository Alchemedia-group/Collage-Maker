package com.alchemedia.collagemaker

import com.alchemedia.collagemaker.model.CellRect
import com.alchemedia.collagemaker.model.CollageGeometry
import com.alchemedia.collagemaker.model.GridLayouts
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CollageGeometryTest {
    private val eps = 1e-3f

    private fun assertRect(expected: CellRect, actual: CellRect) {
        assertEquals(expected.x, actual.x, eps)
        assertEquals(expected.y, actual.y, eps)
        assertEquals(expected.width, actual.width, eps)
        assertEquals(expected.height, actual.height, eps)
    }

    @Test
    fun `2x2 grid without gap or margin splits the canvas in four`() {
        val rects = CollageGeometry.cellRects(GridLayouts.byId("4"), 100f, 100f, 0f, 0f)
        assertRect(CellRect(0f, 0f, 50f, 50f), rects[0])
        assertRect(CellRect(50f, 0f, 50f, 50f), rects[1])
        assertRect(CellRect(0f, 50f, 50f, 50f), rects[2])
        assertRect(CellRect(50f, 50f, 50f, 50f), rects[3])
    }

    @Test
    fun `gap only appears between cells, not at the canvas edge`() {
        val rects = CollageGeometry.cellRects(GridLayouts.byId("2v"), 100f, 100f, gapPercent = 10f, marginPercent = 0f)
        assertRect(CellRect(0f, 0f, 45f, 100f), rects[0])
        assertRect(CellRect(55f, 0f, 45f, 100f), rects[1])
    }

    @Test
    fun `margin insets the whole collage`() {
        val rects = CollageGeometry.cellRects(GridLayouts.byId("2v"), 100f, 100f, gapPercent = 0f, marginPercent = 5f)
        assertRect(CellRect(5f, 5f, 45f, 90f), rects[0])
        assertRect(CellRect(50f, 5f, 45f, 90f), rects[1])
    }

    @Test
    fun `gap and margin scale with the shorter side`() {
        val rects = CollageGeometry.cellRects(GridLayouts.byId("2v"), 200f, 100f, gapPercent = 10f, marginPercent = 0f)
        // 10% of the shorter side (100) = 10 px, so each cell loses 5 px next to the seam.
        assertEquals(10f, rects[1].x - (rects[0].x + rects[0].width), eps)
    }

    @Test
    fun `corner radius of 100 percent makes the short side fully round`() {
        val rect = CellRect(0f, 0f, 80f, 40f)
        assertEquals(20f, CollageGeometry.cornerRadius(rect, 100f), eps)
        assertEquals(0f, CollageGeometry.cornerRadius(rect, 0f), eps)
    }

    @Test
    fun `every layout tiles the unit square exactly with no overlaps`() {
        GridLayouts.all.forEach { layout ->
            val area = layout.cells.sumOf { (it.w * it.h).toDouble() }
            assertEquals("area of ${layout.id}", 1.0, area, 1e-4)
            layout.cells.forEach {
                assertTrue("${layout.id} cell inside square", it.x >= -1e-6f && it.y >= -1e-6f && it.x + it.w <= 1 + 1e-6f && it.y + it.h <= 1 + 1e-6f)
            }
            for (i in layout.cells.indices) for (j in i + 1 until layout.cells.size) {
                val a = layout.cells[i]
                val b = layout.cells[j]
                val overlapW = minOf(a.x + a.w, b.x + b.w) - maxOf(a.x, b.x)
                val overlapH = minOf(a.y + a.h, b.y + b.h) - maxOf(a.y, b.y)
                assertTrue("${layout.id}: cells $i and $j overlap", overlapW <= 1e-5f || overlapH <= 1e-5f)
            }
        }
    }

    @Test
    fun `layout ids are unique`() {
        assertEquals(GridLayouts.all.size, GridLayouts.all.map { it.id }.toSet().size)
    }

    @Test
    fun `suggested layout has room for the photos up to the largest layout`() {
        for (n in 1..12) {
            assertTrue("layout for $n photos", GridLayouts.suggestFor(n).cells.size >= minOf(n, 12) || n > 9)
        }
        assertEquals(1, GridLayouts.suggestFor(1).cells.size)
        assertEquals(4, GridLayouts.suggestFor(4).cells.size)
        assertEquals(12, GridLayouts.suggestFor(40).cells.size)
    }

    @Test
    fun `unknown layout id falls back to the default`() {
        assertEquals(GridLayouts.DEFAULT_ID, GridLayouts.byId("nope").id)
    }
}
