package com.alchemedia.collage

import com.alchemedia.collage.data.ProjectJson
import com.alchemedia.collage.model.AspectPreset
import com.alchemedia.collage.model.CellContent
import com.alchemedia.collage.model.CollageProject
import com.alchemedia.collage.model.PhotoRef
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CollageProjectTest {
    private fun photos(n: Int) = List(n) { PhotoRef("p$it", "p$it.jpg", 4000, 3000) }

    private fun project(n: Int, layout: String) = CollageProject.create("id", 1L, photos(n), layout)

    @Test
    fun `new project fills cells in order`() {
        val p = project(3, "4")
        assertEquals(listOf("p0", "p1", "p2", null), p.cells.map { it.photoId })
    }

    @Test
    fun `extra photos stay in the library and unused cells stay empty`() {
        val p = project(5, "2v")
        assertEquals(listOf("p0", "p1"), p.cells.map { it.photoId })
        assertEquals(5, p.photos.size)
    }

    @Test
    fun `switching to a bigger layout pulls unplaced photos into new cells`() {
        val p = project(5, "2v").withLayout("3x3")
        assertEquals(listOf("p0", "p1", "p2", "p3", "p4"), p.cells.take(5).map { it.photoId })
        assertEquals(9, p.cells.size)
    }

    @Test
    fun `switching layout keeps pan and zoom for cells that stay`() {
        val base = project(2, "2v")
        val tweaked = base.copy(cells = base.cells.mapIndexed { i, c -> if (i == 1) c.copy(zoom = 2f, panX = .5f) else c })
        val moved = tweaked.withLayout("2h")
        assertEquals(CellContent("p1", zoom = 2f, panX = .5f), moved.cells[1])
    }

    @Test
    fun `switching to a smaller layout and back restores unplaced photos without duplicates`() {
        val p = project(4, "4").withLayout("1").withLayout("4")
        val ids = p.cells.mapNotNull { it.photoId }
        assertEquals(ids.size, ids.toSet().size)
        assertEquals(4, ids.size)
    }

    @Test
    fun `photo lookup`() {
        val p = project(2, "2v")
        assertEquals("p1", p.photo("p1")?.id)
        assertNull(p.photo("missing"))
        assertNull(p.photo(null))
    }

    @Test
    fun `json round trip keeps everything`() {
        val p = project(3, "l1r2").copy(
            name = "Summer",
            aspect = AspectPreset.Portrait45,
            background = 0xFFF6EFE4,
            gapPercent = 3.5f,
            cornerPercent = 20f,
        )
        assertEquals(p, ProjectJson.decode(ProjectJson.encode(p)))
    }

    @Test
    fun `json ignores unknown keys and fills defaults`() {
        val p = ProjectJson.decode("""{"id":"x","createdAt":1,"updatedAt":2,"futureField":true}""")
        assertEquals("x", p.id)
        assertEquals(AspectPreset.Square, p.aspect)
        assertEquals(emptyList<CellContent>(), p.cells)
    }
}
