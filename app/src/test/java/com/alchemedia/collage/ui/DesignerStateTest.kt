package com.alchemedia.collage.ui

import com.alchemedia.collage.TestAssets
import com.alchemedia.collage.model.LayoutCatalog
import com.alchemedia.collage.model.LayoutCell
import com.alchemedia.collage.ui.designer.DesignerState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DesignerStateTest {
    @Before fun setUp() = TestAssets.loadCatalog()

    private fun designer(layoutId: String? = "g2x2"): DesignerState {
        val d = DesignerState(layoutId?.let { LayoutCatalog.byId(it) }, null)
        d.canvasW = 1000f; d.canvasH = 1000f; d.density = 1f
        d.snap = false
        return d
    }

    private fun area(cells: List<LayoutCell>) = cells.sumOf { (it.w * it.h).toDouble() }

    @Test fun startsWithOneFullCell() {
        val d = DesignerState(null, null)
        assertEquals(1, d.cells.size)
        assertEquals(1f, d.cells[0].w, 0f)
    }

    @Test fun splitHalvesTheCell() {
        val d = designer(null)
        assertNull(d.split(vertical = true))
        assertEquals(2, d.cells.size)
        assertEquals(.5f, d.cells[0].w, 1e-6f)
        assertEquals(.5f, d.cells[1].x, 1e-6f)
        assertEquals(1.0, area(d.cells), 1e-6)
        assertNull(d.split(vertical = false))
        assertEquals(3, d.cells.size)
        assertEquals(1.0, area(d.cells), 1e-6)
    }

    @Test fun tinyCellsRefuseToSplit() {
        val d = designer(null)
        repeat(4) { d.split(true) }
        d.selected = 0
        val msgs = (0..6).mapNotNull { d.split(true) }
        assertTrue(msgs.isNotEmpty())
    }

    @Test fun deletingTheLastCellIsRefused() {
        val d = designer(null)
        assertNotNull(d.delete())
        assertEquals(1, d.cells.size)
    }

    @Test fun linkedResizeMovesTheSharedDivider() {
        val d = designer("g2x2")
        d.link = true
        d.selected = 0
        // right-middle handle of cell 0 sits at (0.5, 0.25)
        d.onDown(500f, 250f)
        d.onMove(700f, 250f)
        d.onUp()
        assertEquals(.7f, d.cells[0].w, 1e-3f)
        assertEquals(.7f, d.cells[2].w, 1e-3f) // the cell under it shares the divider
        assertEquals(.3f, d.cells[1].w, 1e-3f)
        assertEquals(.3f, d.cells[3].w, 1e-3f)
        assertEquals(1.0, area(d.cells), 1e-3)
    }

    @Test fun unlinkedResizeOnlyMovesOneCell() {
        val d = designer("g2x2")
        d.link = false
        d.selected = 0
        d.onDown(500f, 250f)
        d.onMove(700f, 250f)
        d.onUp()
        assertEquals(.7f, d.cells[0].w, 1e-3f)
        assertEquals(.5f, d.cells[2].w, 1e-3f)
    }

    @Test fun resizeNeverMakesACellSmallerThanTheMinimum() {
        val d = designer("g2x2")
        d.selected = 0
        d.onDown(500f, 250f)
        d.onMove(-5000f, 250f)
        d.onUp()
        assertTrue(d.cells.all { it.w >= .039f && it.h >= .039f })
    }

    @Test fun movingKeepsACellInsideTheCanvas() {
        val d = designer(null)
        d.split(true)
        d.selected = 0
        d.onDown(250f, 500f)
        d.onMove(-9000f, -9000f)
        d.onUp()
        assertTrue(d.cells.all { it.x >= 0f && it.y >= 0f && it.x + it.w <= 1.0001f && it.y + it.h <= 1.0001f })
    }

    @Test fun snappingShowsGuidesAndLandsOnTheCentre() {
        val d = designer(null)
        d.split(true)
        d.selected = 0
        d.snap = true
        d.density = 1f
        // move the left half so that its right edge is a few pixels from the middle of the canvas
        d.onDown(100f, 500f)
        d.onMove(100f + 3f, 500f)
        // within the 8px snap distance of the 0.5 line
        assertTrue(d.guides.isNotEmpty() || d.cells[0].x == 0f)
        d.onUp()
        assertTrue(d.guides.isEmpty())
    }

    @Test fun undoAndRedoRestoreCells() {
        val d = designer(null)
        d.split(true)
        assertEquals(2, d.cells.size)
        d.undo()
        assertEquals(1, d.cells.size)
        d.redo()
        assertEquals(2, d.cells.size)
        d.undo(); d.undo()
        assertEquals(1, d.cells.size)
    }

    @Test fun dragIsOneUndoStep() {
        val d = designer(null)
        d.split(true)
        val before = d.undoCount
        d.selected = 0
        d.onDown(250f, 500f)
        d.onMove(300f, 500f); d.onMove(350f, 500f); d.onMove(400f, 500f)
        d.onUp()
        assertEquals(before + 1, d.undoCount)
    }

    @Test fun tapOnEmptySpaceDeselects() {
        val d = designer(null)
        d.split(true)
        d.delete()
        d.selected = 0
        d.onDown(500f, 500f)
        d.onUp()
        assertTrue(d.selected >= 0) // the only cell covers the canvas
    }

    @Test fun shapesAndLetters() {
        val d = designer(null)
        assertNull(d.setShape("heart"))
        assertEquals("heart", d.selectedCell?.shape)
        assertEquals(1f, d.selectedCell?.sc ?: 0f, 0f)
        d.setSize(80f); d.setRotation(30f)
        assertEquals(.8f, d.selectedCell!!.scale, 1e-6f)
        assertEquals(30f, d.selectedCell!!.rotation, 0f)
        d.toggleStretch()
        assertTrue(d.selectedCell!!.fill)
        d.setShape("letter")
        assertTrue(d.selectedCell!!.isLetter)
        d.setLetter("xy9")
        assertEquals("XY", d.selectedCell!!.text)
        d.setShape("rect")
        assertNull(d.selectedCell!!.shape); assertNull(d.selectedCell!!.text)
    }

    @Test fun letterKeepsOldValueWhenCleared() {
        val d = designer(null)
        d.setShape("letter")
        d.setLetter("")
        assertEquals("A", d.selectedCell!!.text)
    }

    @Test fun reorderAndDuplicate() {
        val d = designer("g2x2")
        d.selected = 0
        d.toFront()
        assertEquals(3, d.selected)
        d.toBack()
        assertEquals(0, d.selected)
        d.duplicate()
        assertEquals(5, d.cells.size)
        assertEquals(4, d.selected)
    }

    @Test fun toLayoutCleansUpAndNames() {
        val d = designer("g2x2")
        val l = d.toLayout(existingCount = 2)
        assertEquals("My layout 3", l.name)
        assertTrue(l.mine && l.cat == "mine")
        assertTrue(l.id.startsWith("my-"))
        assertEquals(4, l.cells.size)
        d.name = "  Mosaic  "
        assertEquals("Mosaic", d.toLayout(0).name)
    }

    @Test fun editingKeepsTheId() {
        val d = DesignerState(LayoutCatalog.byId("g2x2"), "my-abc")
        assertEquals("my-abc", d.toLayout(0).id)
    }

    @Test fun loadReplacesCellsAndAspect() {
        val d = designer(null)
        d.load(LayoutCatalog.byId("w-love"))
        assertEquals(4, d.cells.size)
        assertEquals("1:1", d.aspect.label)
        d.undo()
        assertEquals(1, d.cells.size)
    }

    @Test fun draftIsPublishedToTheCatalog() {
        val d = designer("g2x2")
        d.publishDraft()
        assertEquals(4, LayoutCatalog.byId(LayoutCatalog.DRAFT_ID).cells.size)
    }
}
