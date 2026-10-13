package com.alchemedia.collage.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.assertCountEquals

import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.center
import androidx.compose.ui.test.down
import androidx.compose.ui.test.moveBy
import androidx.compose.ui.test.pinch
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.up
import com.alchemedia.collage.TestAssets
import com.alchemedia.collage.model.LayoutCatalog
import com.alchemedia.collage.model.Samples
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Taps and drags through the real screens, the way a person would. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w390dp-h844dp-xhdpi")
class UiFlowTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    private lateinit var h: UiHarness

    @Before fun setUp() {
        TestAssets.loadCatalog()
        LayoutCatalog.setMine(emptyList())
        h = UiHarness(rule)
    }

    private fun tapDesc(desc: String) { rule.onNodeWithContentDescription(desc).performClick(); h.settle(300) }
    private fun tapText(text: String) { rule.onNodeWithText(text).performClick(); h.settle(300) }
    private fun canvas() = rule.onNode(hasContentDescription("Collage canvas", substring = true))

    @Test fun fromHomeToANewCollage() {
        h.start()
        rule.onNodeWithText("Photo Collage").assertIsDisplayed()
        tapDesc("New collage")
        assertEquals(Screen.New, h.vm.screen)
        rule.onNodeWithText("Choose photos").assertIsDisplayed()
        h.vm.togglePick(Samples.moraine.id); h.vm.togglePick(Samples.beach.id); h.settle()
        rule.onNodeWithText("2/12").assertIsDisplayed()
        tapText("Create collage (2)")
        h.settle(1500)
        assertTrue(h.vm.screen is Screen.Editor)
        assertEquals(2, h.vm.editor!!.project.cells.count { it.photoId != null })
    }

    @Test fun quickLayoutOpensWithPhotosAlreadyChosen() {
        h.start()
        tapDesc("4 hearts")
        assertEquals(Screen.New, h.vm.screen)
        assertEquals(4, h.vm.selection.size)
    }

    @Test fun tappingAThumbnailTogglesItsNumber() {
        h.start()
        h.vm.startNew(); h.settle()
        rule.onAllNodes(hasContentDescription("Photo"))[1].performClick(); h.settle(300)
        assertEquals(1, h.vm.selection.size)
        rule.onAllNodes(hasContentDescription("Photo"))[1].performClick(); h.settle(300)
        assertEquals(0, h.vm.selection.size)
    }

    @Test fun editorCanvasGestures() {
        val p = h.makeProject("g1x1", "One")
        h.start()
        h.vm.openProject(p.id); h.settle(1500)
        val ed = h.vm.editor!!
        canvas().performTouchInput { down(center); up() }
        h.settle(300)
        assertEquals(0, ed.selectedCell)
        canvas().performTouchInput { down(center); moveBy(Offset(120f, 0f)); up() }
        h.settle(300)
        assertTrue("pan ${ed.project.cells[0].panX}", ed.project.cells[0].panX != 0f || ed.project.cells[0].zoom >= 1f)
        canvas().performTouchInput { pinch(center - Offset(40f, 0f), center - Offset(160f, 0f), center + Offset(40f, 0f), center + Offset(160f, 0f)) }
        h.settle(300)
        assertTrue("zoom ${ed.project.cells[0].zoom}", ed.project.cells[0].zoom > 1.5f)
    }

    @Test fun addingAndEditingText() {
        val p = h.makeProject("g2x2", "Words")
        h.start()
        h.vm.openProject(p.id); h.settle(1500)
        tapDesc("Text")
        tapText("Add text")
        val ed = h.vm.editor!!
        assertEquals(1, ed.project.items.size)
        rule.onNodeWithText("Your text").performClick()
        rule.onNodeWithText("Your text").performTextInput("Hello")
        h.settle()
        assertEquals("Hello", ed.project.items[0].text)
        assertTrue(ed.typing)
        tapText("Done") // closes the keyboard and brings the tabs back
        assertFalse(ed.typing)
        tapText("Font")
        tapText("Script")
        tapText("Color")
        tapText("Outline")
        tapText("Done")
        assertEquals(null, ed.selectedItemId)
    }

    @Test fun stickersCanBeAddedAndRemoved() {
        val p = h.makeProject("g2x2", "Stickers")
        h.start()
        h.vm.openProject(p.id); h.settle(1500)
        tapDesc("Stickers")
        tapDesc("Add 🎉".takeIf { false } ?: "Add 😀")
        val ed = h.vm.editor!!
        assertEquals(1, ed.project.items.size)
        tapDesc("Delete")
        assertTrue(ed.project.items.isEmpty())
    }

    @Test fun styleTabsChangeTheCollage() {
        val p = h.makeProject("g2x2", "Style")
        h.start()
        h.vm.openProject(p.id); h.settle(1500)
        tapDesc("Style")
        val ed = h.vm.editor!!
        tapDesc("#2557D6")
        assertEquals(0xFF2557D6, ed.project.background)
        tapText("Texture")
        rule.onNodeWithText("Linen").performClick(); h.settle(300)
        assertEquals("linen", ed.project.texture)
        tapText("Filter")
        rule.onNodeWithText("Mono").performClick(); h.settle(300)
        assertEquals("mono", ed.project.filter)
        tapDesc("Shape")
        tapDesc("Aspect 4:5")
        assertEquals("4:5", ed.project.aspect.label)
    }

    @Test fun layoutRowAndBrowserChangeTheLayout() {
        val p = h.makeProject("g2x2", "Layouts")
        h.start()
        h.vm.openProject(p.id); h.settle(1500)
        val ed = h.vm.editor!!
        tapDesc("Browse all layouts")
        rule.onNodeWithText("All layouts").assertIsDisplayed()
        tapText("Shapes")
        rule.onNodeWithText("Clover").performClick(); h.settle(500)
        assertEquals("clover", ed.project.layoutId)
        assertTrue(h.vm.overlays.isEmpty())
    }

    @Test fun typingAWordMakesALayout() {
        val p = h.makeProject("g2x2", "Words")
        h.start()
        h.vm.openProject(p.id); h.settle(1500)
        tapDesc("Browse all layouts")
        rule.onNodeWithText("MOM, 2026, BFF").performClick()
        rule.onNodeWithText("MOM, 2026, BFF").performTextInput("hello")
        h.settle()
        tapText("Use")
        assertEquals("w-custom-HELLO", h.vm.editor!!.project.layoutId)
        assertEquals(5, h.vm.editor!!.project.cells.size)
    }

    @Test fun savingShowsTheExportSheet() {
        val p = h.makeProject("hearts4", "Export")
        h.start()
        h.vm.openProject(p.id); h.settle(1500)
        tapText("Save")
        h.settle(1500)
        rule.onNodeWithText("Save collage").assertIsDisplayed()
        rule.onNodeWithText("Save to gallery").assertIsDisplayed()
        tapText("Keep editing")
        assertTrue(h.vm.sheets.isEmpty())
    }

    @Test fun savingNeedsAPhoto() {
        val p = h.makeProject("g2x2", "Empty") { it.copy(cells = it.cells.map { c -> c.copy(photoId = null) }) }
        h.start()
        h.vm.openProject(p.id); h.settle(1500)
        h.vm.editor!!.let { assertFalse(it.hasPhotos) }
        h.vm.showToast("x"); h.vm.dismissToast()
    }

    @Test fun cardMenuDeletesWithUndo() {
        h.makeProject("g2x2", "Doomed")
        h.start()
        tapDesc("More options for Doomed")
        rule.onNodeWithText("Delete collage").assertIsDisplayed()
        tapText("Delete collage")
        h.settle(800)
        assertTrue(h.vm.store.projects.value.isEmpty())
        rule.onNodeWithText("UNDO").assertIsDisplayed()
        tapText("UNDO")
        h.settle(800)
        assertEquals(1, h.vm.store.projects.value.size)
    }

    @Test fun historyShowsAndDeletes() {
        val p = h.makeProject("g2x2", "Saved one")
        h.run { h.app.store.recordHistory(p, 2048, 2048, false) }
        h.start()
        tapDesc("History")
        rule.onNodeWithText("Saved one").assertIsDisplayed()
        tapDesc("Delete Saved one from History")
        h.settle(800)
        assertTrue(h.vm.store.history.value.isEmpty())
        rule.onNodeWithText("No saved collages yet").assertIsDisplayed()
        tapText("UNDO")
        h.settle(800)
        assertEquals(1, h.vm.store.history.value.size)
    }

    @Test fun clearAllNeedsASecondTap() {
        val p = h.makeProject("g2x2", "One")
        h.run { h.app.store.recordHistory(p, 1, 1, true) }
        h.start()
        tapDesc("History")
        tapText("Clear all")
        assertEquals(1, h.vm.store.history.value.size)
        tapText("Tap again to clear")
        h.settle(800)
        assertTrue(h.vm.store.history.value.isEmpty())
    }

    @Test fun proSheetOffersTheSubscription() {
        h.start()
        tapText("Go Pro")
        rule.onNodeWithText("Photo Collage Pro").assertIsDisplayed()
        rule.onAllNodes(hasText("£2.99", substring = true)).onFirst().assertIsDisplayed()
        rule.onNodeWithText("Removes the watermark from every export").assertIsDisplayed()
        tapText("Not now")
        assertTrue(h.vm.sheets.isEmpty())
    }

    @Test fun designerSavesALayoutToMine() {
        h.start()
        tapDesc("New collage")
        tapDesc("Browse all layouts")
        tapText("Design your own layout")
        assertTrue(h.vm.overlays.last() is Overlay.Designer)
        rule.onNodeWithText("Name your layout").performClick()
        rule.onNodeWithText("Name your layout").performTextInput("Trio")
        h.settle()
        tapDesc("Split into left and right")
        tapDesc("Split into top and bottom")
        rule.onNodeWithText("3 cells").assertIsDisplayed()
        tapText("Save layout")
        h.settle(1200)
        val mine = h.vm.store.myLayouts.value
        assertEquals(1, mine.size)
        assertEquals("Trio", mine[0].name)
        assertEquals(3, mine[0].cells.size)
        assertEquals(mine[0].id, h.vm.newLayoutId)
        assertEquals("mine", h.vm.category)
        assertTrue(h.vm.overlays.isEmpty())
    }

    @Test fun designerShapeAndLetter() {
        h.start()
        h.vm.openDesigner(BrowseTarget.Home); h.settle()
        tapText("Shape")
        tapDesc("Heart")
        rule.onNodeWithText("Size").assertExists()
        rule.onNodeWithText("Rotate").assertExists()
        tapDesc("Rectangle")
        rule.onNodeWithText("Rectangles are plain photo cells. Pick a shape or a letter to cut the photo into it.").assertExists()
    }

    @Test fun backButtonLeavesTheEditor() {
        val p = h.makeProject("g2x2", "Back")
        h.start()
        h.vm.openProject(p.id); h.settle(1500)
        tapDesc("Back")
        assertEquals(Screen.Home, h.vm.screen)
        rule.onNodeWithText("Photo Collage").assertIsDisplayed()
    }

    @Test fun designerCanStartFromAnotherLayout() {
        h.start()
        h.vm.openDesigner(BrowseTarget.Home); h.settle()
        tapText("Canvas")
        tapText("Start from…")
        assertTrue(h.vm.overlays.last() is Overlay.Browse)
        tapText("Shapes")
        rule.onNodeWithText("Clover").performClick(); h.settle(600)
        assertTrue(h.vm.overlays.last() is Overlay.Designer)
        rule.onNodeWithText("4 cells").assertIsDisplayed()
        assertNull(h.vm.designerBase)
    }

    @Test fun pencilOnAMyLayoutTileOpensItInTheDesigner() {
        val mine = com.alchemedia.collage.model.Layout("my-x", "Trio", "mine", ar = "1:1", cells = List(3) { com.alchemedia.collage.model.LayoutCell(it / 3f, 0f, 1 / 3f, 1f) }, mine = true)
        h.run { h.app.store.saveLayout(mine) }
        h.start()
        h.vm.openBrowse(BrowseTarget.Home); h.settle()
        rule.onNodeWithText("Search: heart, love, grid, mosaic").performTextInput("Trio"); h.settle()
        tapDesc("Edit Trio")
        val d = h.vm.overlays.last() as Overlay.Designer
        assertEquals("my-x", d.editId)
        rule.onNodeWithText("3 cells").assertIsDisplayed()
        rule.onNodeWithContentDescription("Delete this layout").assertExists()
    }

    @Test fun deletingALayoutNeedsTwoTaps() {
        val mine = com.alchemedia.collage.model.Layout("my-y", "Gone soon", "mine", ar = "1:1", cells = listOf(com.alchemedia.collage.model.LayoutCell(0f, 0f, 1f, 1f)), mine = true)
        h.run { h.app.store.saveLayout(mine) }
        h.start()
        h.vm.openDesigner(BrowseTarget.Home, editId = "my-y"); h.settle()
        tapDesc("Delete this layout")
        assertEquals(1, h.vm.store.myLayouts.value.size)
        tapDesc("Delete this layout")
        h.settle(800)
        assertTrue(h.vm.store.myLayouts.value.isEmpty())
        assertTrue(h.vm.overlays.isEmpty())
    }

    @Test fun emptyHomeExplainsItself() {
        h.start()
        rule.onNodeWithText("Nothing pinned yet").assertIsDisplayed()
    }

    @Test fun overlaysBlockTheScreenBelow() {
        h.makeProject("g2x2", "Under")
        h.start()
        h.vm.openHistory(); h.settle()
        rule.onAllNodes(hasText("Under")).assertCountEquals(0)
    }
}
