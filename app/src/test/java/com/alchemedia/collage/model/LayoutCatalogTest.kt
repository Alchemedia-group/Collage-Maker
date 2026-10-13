package com.alchemedia.collage.model

import com.alchemedia.collage.TestAssets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LayoutCatalogTest {
    @Before fun setUp() = TestAssets.loadCatalog()

    @Test fun hasAllBuiltInLayouts() {
        assertEquals(110, LayoutCatalog.builtInLayouts.size)
    }

    @Test fun idsAreUnique() {
        val ids = LayoutCatalog.all.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test fun everyCellIsInsideTheCanvas() {
        LayoutCatalog.all.forEach { l ->
            l.cells.forEach { c ->
                assertTrue("${l.id} has an empty cell", c.w > 0f && c.h > 0f)
                assertTrue("${l.id} leaves the canvas", c.x >= -1e-4f && c.y >= -1e-4f && c.x + c.w <= 1.0001f && c.y + c.h <= 1.0001f)
            }
        }
    }

    @Test fun everyShapeIsDefined() {
        LayoutCatalog.all.flatMap { it.cells }.mapNotNull { it.shape }.forEach { assertTrue("missing shape $it", LayoutCatalog.shapes.containsKey(it)) }
    }

    @Test fun gridLikeLayoutsTileTheCanvasExactly() {
        LayoutCatalog.all.filter { it.cat in setOf("grid", "feature", "mosaic") }.forEach { l ->
            val area = l.cells.sumOf { (it.w * it.h).toDouble() }
            assertEquals("${l.id} does not tile the canvas", 1.0, area, 0.002)
        }
    }

    @Test fun lettersCarryText() {
        val letters = LayoutCatalog.all.filter { it.cat == "letters" }
        assertTrue(letters.isNotEmpty())
        letters.forEach { l -> assertTrue("${l.id} has no letter", l.cells.any { it.isLetter }) }
    }

    @Test fun everyCategoryHasLayouts() {
        LayoutCategories.browsable.filter { it.first != "mine" }.forEach { (id, _) ->
            assertTrue("no layouts in $id", LayoutCatalog.all.any { it.cat == id })
        }
    }

    @Test fun suggestedLayoutMatchesPhotoCount() {
        (1..12).forEach { n ->
            val l = LayoutCatalog.suggestFor(n)
            assertTrue("suggestion for $n has ${l.cells.size} cells", l.cells.size in (n - 1)..(n + 3))
        }
    }

    @Test fun forYouListsExactMatchesFirst() {
        val list = LayoutCatalog.forCategory("fit", 4)
        assertTrue(list.isNotEmpty())
        assertEquals(4, list.first().cells.size)
    }

    @Test fun searchByNumberAndWord() {
        assertTrue(LayoutCatalog.search("4", CountFilter.Any, null).all { it.cells.size == 4 })
        assertTrue(LayoutCatalog.search("heart", CountFilter.Any, null).isNotEmpty())
        assertTrue(LayoutCatalog.search("", CountFilter.TenPlus, null).all { it.cells.size >= 10 })
        assertTrue(LayoutCatalog.search("zzzzqq", CountFilter.Any, null).isEmpty())
    }

    @Test fun myLayoutsJoinTheCatalogue() {
        val mine = Layout("m1", "Mine", "mine", cells = listOf(LayoutCell(0f, 0f, 1f, 1f)), mine = true)
        LayoutCatalog.setMine(listOf(mine))
        assertEquals("m1", LayoutCatalog.byId("m1").id)
        assertEquals(listOf("m1"), LayoutCatalog.forCategory("mine", 0).map { it.id })
        LayoutCatalog.setMine(emptyList())
        assertEquals(LayoutCatalog.DEFAULT_ID, LayoutCatalog.byId("m1").id)
    }
}
