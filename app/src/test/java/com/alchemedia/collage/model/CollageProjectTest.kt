package com.alchemedia.collage.model

import com.alchemedia.collage.TestAssets
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CollageProjectTest {
    @Before fun setUp() = TestAssets.loadCatalog()

    private fun photos(n: Int) = List(n) { PhotoRef("p$it", "p$it.jpg", 100, 100) }

    @Test fun newProjectFillsCellsInOrder() {
        val p = CollageProject.create("a", 1, photos(3), LayoutCatalog.byId("g2x2"))
        assertEquals(listOf("p0", "p1", "p2", null), p.cells.map { it.photoId })
    }

    @Test fun switchingLayoutKeepsPhotosAndPlacesLeftovers() {
        val p = CollageProject.create("a", 1, photos(4), LayoutCatalog.byId("g2x2"))
        val three = p.withLayout(LayoutCatalog.byId("g3x1"))
        assertEquals(3, three.cells.size)
        assertEquals(listOf("p0", "p1", "p2"), three.cells.map { it.photoId })
        val back = three.withLayout(LayoutCatalog.byId("g2x2"))
        assertEquals("p3", back.cells[3].photoId)
    }

    @Test fun deletedLayoutFallsBackToTheDefault() {
        val mine = Layout("gone", "Gone", "mine", cells = List(2) { LayoutCell(0f, it * .5f, 1f, .5f) }, mine = true)
        LayoutCatalog.setMine(listOf(mine))
        val p = CollageProject.create("a", 1, photos(2), mine)
        LayoutCatalog.setMine(emptyList())
        val fixed = p.normalized()
        assertEquals(LayoutCatalog.DEFAULT_ID, fixed.layoutId)
        assertEquals(LayoutCatalog.byId(LayoutCatalog.DEFAULT_ID).cells.size, fixed.cells.size)
        assertEquals(setOf("p0", "p1"), fixed.cells.mapNotNull { it.photoId }.toSet())
    }

    @Test fun editedLayoutResizesTheCells() {
        val v1 = Layout("m", "M", "mine", cells = List(2) { LayoutCell(0f, it * .5f, 1f, .5f) }, mine = true)
        LayoutCatalog.setMine(listOf(v1))
        val p = CollageProject.create("a", 1, photos(2), v1)
        LayoutCatalog.setMine(listOf(v1.copy(cells = List(3) { LayoutCell(0f, it / 3f, 1f, 1 / 3f) })))
        assertEquals(3, p.normalized().cells.size)
    }

    @Test fun jsonRoundTrip() {
        val items = listOf(
            CanvasItem.text("Hi", shadow = ShadowSpec(0x66000000, .1f, 0f, .05f), bg = 0xFFFFFFFF),
            CanvasItem.emoji("😀"),
            CanvasItem.shape("heart"),
        )
        val p = CollageProject.create("a", 1, photos(2), LayoutCatalog.byId("hero")).copy(filter = "noir", texture = "kraft", items = items)
        val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }
        val back = json.decodeFromString(CollageProject.serializer(), json.encodeToString(CollageProject.serializer(), p))
        assertEquals(p, back)
    }

    @Test fun unknownFieldsAreIgnored() {
        val json = Json { ignoreUnknownKeys = true }
        val p = json.decodeFromString(CollageProject.serializer(), """{"id":"x","createdAt":1,"updatedAt":2,"future":true}""")
        assertEquals("x", p.id)
        assertNull(p.photo("nope"))
    }

    @Test fun aspectLookup() {
        assertEquals(AspectPreset.Story916, AspectPreset.fromLabel("9:16"))
        assertEquals(AspectPreset.Square, AspectPreset.fromLabel("?"))
    }

    @Test fun geometryKeepsGapBetweenCellsOnly() {
        val cells = LayoutCatalog.byId("g2x2").cells
        val r = CollageGeometry.cellRects(cells, 1000f, 1000f, 4f, 0f)
        assertEquals(0f, r[0].x, .01f)
        assertEquals(0f, r[0].y, .01f)
        assertEquals(500f - 20f, r[0].width, .01f)
        assertTrue(r[1].x - r[0].right >= 39.9f)
        assertEquals(1000f, r[3].right, .01f)
    }

    @Test fun negativeMarginLetsCellsBleedPastTheEdge() {
        val cells = LayoutCatalog.byId("g2x2").cells
        val r = CollageGeometry.cellRects(cells, 1000f, 1000f, 0f, -5f)
        assertEquals(-50f, r[0].x, .01f)
        assertEquals(1050f, r[3].right, .01f)
    }

    @Test fun newCollagesStartWithASlimMargin() {
        assertTrue(CollageProject.create("a", 1, photos(2), LayoutCatalog.byId("g2x2")).marginPercent <= 1f)
    }
}
