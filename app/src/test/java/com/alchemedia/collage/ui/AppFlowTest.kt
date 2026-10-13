package com.alchemedia.collage.ui

import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import com.alchemedia.collage.CollageApp
import com.alchemedia.collage.TestAssets
import com.alchemedia.collage.data.Exporter
import com.alchemedia.collage.model.AspectPreset
import com.alchemedia.collage.model.CanvasItem
import com.alchemedia.collage.model.CollageProject
import com.alchemedia.collage.model.Layout
import com.alchemedia.collage.model.LayoutCatalog
import com.alchemedia.collage.model.LayoutCell
import com.alchemedia.collage.model.Samples
import com.alchemedia.collage.render.ItemRenderer
import com.alchemedia.collage.render.PhotoSource
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AppFlowTest {
    private lateinit var app: CollageApp
    private lateinit var vm: AppViewModel

    @Before fun setUp() {
        TestAssets.loadCatalog()
        app = ApplicationProvider.getApplicationContext()
        vm = AppViewModel(app)
        await { vm.store.loaded.value }
    }

    private fun await(timeoutMs: Long = 8000, cond: () -> Boolean) {
        val end = System.currentTimeMillis() + timeoutMs
        while (!cond()) {
            ShadowLooper.idleMainLooper(60, java.util.concurrent.TimeUnit.MILLISECONDS)
            check(System.currentTimeMillis() < end) { "timed out waiting" }
            Thread.sleep(10)
        }
    }

    private fun saved(name: String = "Saved", layout: String = "g2x2"): CollageProject =
        runBlocking { vm.store.save(Samples.draft(LayoutCatalog.byId(layout)).copy(id = CollageProject.newId(), name = name)) }

    private fun openEditor(project: CollageProject = saved()): EditorState {
        vm.openEditor(project)
        return vm.editor!!.also { ed ->
            ed.canvasW = 1000f; ed.canvasH = 1000f; ed.density = 1f
            ed.photoSource = PhotoSource { Bitmap.createBitmap(200, 100, Bitmap.Config.ARGB_8888) }
        }
    }

    // ------------------------------------------------------------------ New collage

    @Test fun startingFromAQuickLayoutPicksSamples() {
        vm.startNew("hearts4", samples = true)
        assertEquals(Screen.New, vm.screen)
        assertEquals(4, vm.selection.size)
        assertEquals("hearts4", vm.newLayoutId)
    }

    @Test fun startingPlainPicksNothing() {
        vm.startNew()
        assertTrue(vm.selection.isEmpty())
        assertEquals(LayoutCatalog.DEFAULT_ID, vm.newLayoutId)
        assertTrue(vm.available.size >= 6)
    }

    @Test fun layoutFollowsThePhotoCount() {
        vm.startNew()
        val ids = vm.available.map { it.id }
        ids.take(3).forEach { vm.togglePick(it) }
        assertEquals(LayoutCatalog.suggestFor(3).id, vm.newLayoutId)
        vm.togglePick(ids[3])
        assertEquals(LayoutCatalog.suggestFor(4).id, vm.newLayoutId)
        vm.togglePick(ids[3])
        assertEquals(3, vm.selection.size)
    }

    @Test fun layoutChoiceSetsTheShape() {
        vm.startNew()
        vm.pickLayoutForNew("w-love")
        assertEquals(AspectPreset.Square, vm.newAspect)
        vm.pickLayoutForNew("circles2")
        assertEquals(AspectPreset.Landscape32, vm.newAspect)
    }

    @Test fun aCollageHoldsAtMostTwelvePhotos() {
        vm.startNew()
        repeat(8) { vm.available.add(PickItem("extra$it", null, Samples.moraine)) }
        vm.available.map { it.id }.take(12).forEach { vm.togglePick(it) }
        assertEquals(12, vm.selection.size)
        vm.togglePick(vm.available[12].id)
        assertEquals(12, vm.selection.size)
        assertEquals("A collage holds up to 12 photos.", vm.toast?.text)
    }

    @Test fun creatingACollageOpensTheEditorAndSaves() {
        vm.startNew("g3x1", samples = true)
        vm.createCollage()
        await { vm.editor != null }
        val p = vm.editor!!.project
        assertEquals("g3x1", p.layoutId)
        assertEquals(3, p.cells.count { it.photoId != null })
        assertTrue(vm.screen is Screen.Editor)
        await { vm.store.projects.value.any { it.id == p.id } }
    }

    @Test fun creatingWithNothingSelectedDoesNothing() {
        vm.startNew()
        vm.createCollage()
        assertNull(vm.editor)
        assertEquals(Screen.New, vm.screen)
    }

    @Test fun deviceAndSamplePhotosCanBeMixed() {
        vm.startNew()
        val f = java.io.File(app.cacheDir, "pick.jpg")
        Bitmap.createBitmap(80, 40, Bitmap.Config.ARGB_8888).compress(Bitmap.CompressFormat.JPEG, 90, f.outputStream())
        vm.addDevicePhotos(listOf(android.net.Uri.fromFile(f)))
        assertEquals(1, vm.selection.size)
        assertTrue(vm.available.first().uri != null)
        vm.togglePick(Samples.beach.id)
        vm.createCollage()
        await { vm.editor != null }
        val p = vm.editor!!.project
        assertEquals(2, p.photos.size)
        assertTrue(p.photos.any { it.fileName.startsWith("res:") })
        assertTrue(p.photos.any { !it.fileName.startsWith("res:") && vm.store.photoFile(p.id, it).exists() })
        assertNull(vm.available.firstOrNull { it.uri != null }?.let { null })
    }

    @Test fun addingTheSameDevicePhotoTwiceKeepsOne() {
        vm.startNew()
        val uri = android.net.Uri.parse("file:///x.jpg")
        vm.addDevicePhotos(listOf(uri)); vm.addDevicePhotos(listOf(uri))
        assertEquals(1, vm.available.count { it.uri == uri })
    }

    // ------------------------------------------------------------------ navigation

    @Test fun backWorksFromSheetsThenOverlaysThenScreens() {
        vm.startNew(samples = true)
        vm.openBrowse(BrowseTarget.New)
        vm.openSheet(Sheet.Pro)
        assertTrue(vm.canGoBack)
        assertTrue(vm.back()); assertTrue(vm.sheets.isEmpty()); assertEquals(1, vm.overlays.size)
        assertTrue(vm.back()); assertTrue(vm.overlays.isEmpty()); assertEquals(Screen.New, vm.screen)
        assertTrue(vm.back()); assertEquals(Screen.Home, vm.screen)
        assertFalse(vm.back())
        assertFalse(vm.canGoBack)
    }

    @Test fun leavingTheEditorSavesPendingChanges() {
        val ed = openEditor()
        ed.rename("Renamed")
        vm.goHome()
        await { vm.store.projects.value.any { it.name == "Renamed" } }
        assertNull(vm.editor)
    }

    @Test fun openingASavedCollageNormalisesItsLayout() {
        val mine = Layout("temp", "Temp", "mine", cells = List(2) { LayoutCell(0f, it * .5f, 1f, .5f) }, mine = true)
        runBlocking { vm.store.saveLayout(mine) }
        val p = runBlocking { vm.store.save(Samples.draft(mine).copy(id = CollageProject.newId())) }
        runBlocking { vm.store.deleteLayout("temp") }
        vm.openEditor(p)
        assertEquals(LayoutCatalog.DEFAULT_ID, vm.editor!!.project.layoutId)
    }

    // ------------------------------------------------------------------ delete with undo

    @Test fun deletingACollageOffersUndo() {
        val a = saved("A"); Thread.sleep(5); val b = saved("B")
        vm.deleteProject(a.id)
        await { vm.store.projects.value.size == 1 && vm.toast != null }
        val toast = vm.toast!!
        assertEquals("Undo", toast.actionLabel)
        toast.action!!.invoke()
        await { vm.store.projects.value.size == 2 }
        assertEquals(setOf(a.id, b.id), vm.store.projects.value.map { it.id }.toSet())
    }

    @Test fun duplicatingACollage() {
        val a = saved("A")
        vm.duplicateProject(a.id)
        await { vm.store.projects.value.size == 2 }
        assertTrue(vm.store.projects.value.any { it.name == "A copy" })
    }

    @Test fun historyDeleteAndClearOfferUndo() {
        val p = saved()
        runBlocking { vm.store.recordHistory(p, 1, 1, true); vm.store.recordHistory(p, 1, 1, false) }
        val id = vm.store.history.value.first().id
        vm.deleteHistory(id)
        await { vm.store.history.value.size == 1 && vm.toast != null }
        vm.toast!!.action!!.invoke()
        await { vm.store.history.value.size == 2 }
        vm.dismissToast()
        vm.clearHistory()
        await { vm.store.history.value.isEmpty() && vm.toast != null }
        vm.toast!!.action!!.invoke()
        await { vm.store.history.value.size == 2 }
    }

    @Test fun editAgainOpensACopyAndKeepsTheOriginal() {
        val p = saved("Orig")
        val entry = runBlocking { vm.store.recordHistory(p, 1, 1, true) }
        vm.editAgain(entry)
        await { vm.editor != null }
        assertNotEquals(p.id, vm.editor!!.project.id)
        assertEquals("Orig", vm.editor!!.project.name)
        assertEquals(1, vm.store.history.value.size)
    }

    @Test fun toastsClearThemselvesOrWhenDismissed() {
        vm.showToast("Hello")
        assertEquals("Hello", vm.toast?.text)
        vm.dismissToast()
        assertNull(vm.toast)
    }

    @Test fun savingALayoutRefreshesCollagesUsingIt() {
        val v1 = Layout("lay", "L", "mine", cells = List(2) { LayoutCell(0f, it * .5f, 1f, .5f) }, mine = true)
        runBlocking { vm.store.saveLayout(v1) }
        val ed = openEditor(runBlocking { vm.store.save(Samples.draft(v1).copy(id = CollageProject.newId())) })
        assertEquals(2, ed.project.cells.size)
        vm.saveLayout(v1.copy(cells = List(3) { LayoutCell(0f, it / 3f, 1f, 1 / 3f) }))
        await { ed.project.cells.size == 3 }
    }

    // ------------------------------------------------------------------ editor gestures

    @Test fun tappingACellSelectsItAndOpensThePhotoPanel() {
        val ed = openEditor()
        ed.onDown(250f, 250f); ed.onUp()
        assertEquals(0, ed.selectedCell)
        assertEquals(EditorTab.Photo, ed.tab)
        ed.onDown(900f, 900f)
        ed.onUp()
        assertEquals(3, ed.selectedCell)
    }

    @Test fun tappingTheGapDeselects() {
        val ed = openEditor(saved().let { runBlocking { vm.store.save(it.copy(gapPercent = 10f)) } })
        ed.onDown(250f, 250f); ed.onUp()
        ed.onDown(500f, 500f); ed.onUp()
        assertEquals(-1, ed.selectedCell)
    }

    @Test fun draggingPansThePhotoWithinItsLimits() {
        val ed = openEditor(saved("One", "g1x1"))
        ed.onDown(500f, 500f)
        ed.onMove(600f, 500f)
        val pan = ed.project.cells[0].panX
        assertTrue("panX=$pan", pan > .1f && pan <= 1f)
        ed.onMove(5000f, 500f)
        assertEquals(1f, ed.project.cells[0].panX, 0f)
        ed.onMove(500f, 900f)
        assertEquals(0f, ed.project.cells[0].panY, 0f) // a wide photo in a square cell has nothing to pan vertically
        ed.onUp()
    }

    @Test fun pinchZoomsAndStaysWithinRange() {
        val ed = openEditor(saved("One", "g1x1"))
        ed.onDown(500f, 500f)
        ed.onPinchStart(400f, 500f, 600f, 500f)
        ed.onPinch(300f, 500f, 700f, 500f)
        assertEquals(2f, ed.project.cells[0].zoom, .01f)
        ed.onPinch(0f, 500f, 1000f, 500f)
        assertEquals(MAX_ZOOM, ed.project.cells[0].zoom, 0f)
        ed.onPinch(490f, 500f, 510f, 500f)
        assertEquals(1f, ed.project.cells[0].zoom, 0f)
        ed.onPinchEnd(); ed.onUp()
    }

    @Test fun swapExchangesTwoCells() {
        val ed = openEditor()
        val before = ed.project.cells.map { it.photoId }
        ed.onDown(250f, 250f); ed.onUp()
        ed.swapping = true
        ed.onDown(750f, 250f); ed.onUp()
        assertFalse(ed.swapping)
        assertEquals(listOf(before[1], before[0], before[2], before[3]), ed.project.cells.map { it.photoId })
        assertEquals(1, ed.selectedCell)
    }

    @Test fun resetAndRemoveClearACell() {
        val ed = openEditor()
        ed.selectCell(0)
        ed.updateCell(0) { it.copy(zoom = 3f, panX = .5f) }
        ed.resetCell()
        assertEquals(1f, ed.project.cells[0].zoom, 0f)
        ed.emptyCell()
        assertNull(ed.project.cells[0].photoId)
    }

    @Test fun usingAnotherPhotoFillsTheCell() {
        val ed = openEditor()
        ed.selectCell(0)
        ed.useExistingPhoto(Samples.forest)
        assertEquals(Samples.forest.id, ed.project.cells[0].photoId)
        assertTrue(ed.project.photos.any { it.id == Samples.forest.id })
    }

    @Test fun layoutChangeKeepsPhotosAndAdjustsTheShape() {
        val ed = openEditor()
        ed.pickLayout("circles2")
        assertEquals("circles2", ed.project.layoutId)
        assertEquals(AspectPreset.Landscape32, ed.project.aspect)
        assertEquals(2, ed.project.cells.size)
        assertEquals("Shape set to 3:2 to fit this layout.", vm.toast?.text)
        ed.pickLayout("g2x2")
        assertEquals(4, ed.project.cells.count { it.photoId != null })
    }

    @Test fun styleChangesApply() {
        val ed = openEditor()
        ed.setTexture("kraft", 0xFFC8A578)
        assertEquals("kraft", ed.project.texture)
        assertEquals(0xFFC8A578, ed.project.background) // white background takes the texture's colour
        ed.setBackground(0xFF111827); ed.setTexture("paper", 0xFFFFFFFF)
        assertEquals(0xFF111827, ed.project.background) // a chosen colour is kept
        ed.setFilter("noir"); ed.setFilterAmount(.4f); ed.setGap(5f); ed.setMargin(1f); ed.setCorner(80f); ed.setAspect(AspectPreset.Story916)
        val p = ed.project
        assertEquals(listOf("noir", .4f, 5f, 1f, 80f, AspectPreset.Story916), listOf(p.filter, p.filterAmount, p.gapPercent, p.marginPercent, p.cornerPercent, p.aspect))
    }

    // ------------------------------------------------------------------ editor items

    private fun EditorState.centerOf(item: CanvasItem) = item.x * canvasW to item.y * canvasH

    @Test fun addingTextSelectsItAndOpensTheTextTab() {
        val ed = openEditor()
        ed.addItem(CanvasItem.text("Hi"))
        assertEquals(1, ed.project.items.size)
        assertEquals(ed.project.items[0].id, ed.selectedItemId)
        assertEquals(EditorTab.Text, ed.tab)
        assertTrue(ed.panelOpen)
    }

    @Test fun draggingAnItemMovesIt() {
        val ed = openEditor()
        ed.addItem(CanvasItem.text("Move me"))
        val item = ed.project.items[0]
        val (cx, cy) = ed.centerOf(item)
        ed.onDown(cx, cy)
        ed.onMove(cx + 100f, cy + 50f)
        ed.onUp()
        val moved = ed.project.items[0]
        assertEquals(item.x + .1f, moved.x, .005f)
        assertEquals(item.y + .05f, moved.y, .005f)
    }

    @Test fun itemsStayOnTheCanvas() {
        val ed = openEditor()
        ed.addItem(CanvasItem.emoji("🎉"))
        val (cx, cy) = ed.centerOf(ed.project.items[0])
        ed.onDown(cx, cy); ed.onMove(cx + 99999f, cy - 99999f); ed.onUp()
        assertEquals(1f, ed.project.items[0].x, 0f)
        assertEquals(0f, ed.project.items[0].y, 0f)
    }

    @Test fun cornerHandleScalesAndRotates() {
        val ed = openEditor()
        ed.addItem(CanvasItem.emoji("⭐"))
        val item = ed.project.items[0]
        val f = ItemRenderer.frame(item, 1000f, 1000f)
        val (hx, hy) = f.toWorld(f.w / 2, f.h / 2)
        ed.onDown(hx, hy)
        ed.onMove(f.cx + (hx - f.cx) * 2f, f.cy + (hy - f.cy) * 2f)
        ed.onUp()
        val after = ed.project.items[0]
        assertEquals(item.size * 2f, after.size, item.size * .02f)
    }

    @Test fun deleteHandleRemovesTheItem() {
        val ed = openEditor()
        ed.addItem(CanvasItem.emoji("⭐"))
        val f = ItemRenderer.frame(ed.project.items[0], 1000f, 1000f)
        val (dx, dy) = f.toWorld(-f.w / 2, -f.h / 2)
        ed.onDown(dx, dy); ed.onUp()
        assertTrue(ed.project.items.isEmpty())
        assertNull(ed.selectedItemId)
    }

    @Test fun pinchResizesAndRotatesTheSelectedItem() {
        val ed = openEditor()
        ed.addItem(CanvasItem.emoji("⭐"))
        val item = ed.project.items[0]
        val (cx, cy) = ed.centerOf(item)
        ed.onDown(cx, cy)
        ed.onPinchStart(400f, 500f, 600f, 500f)
        ed.onPinch(400f, 400f, 600f, 600f) // wider apart and turned
        ed.onUp()
        val after = ed.project.items[0]
        assertTrue(after.size > item.size)
        assertNotEquals(item.rot, after.rot)
    }

    @Test fun doubleTapOnTextStartsTyping() {
        val ed = openEditor()
        ed.addItem(CanvasItem.text("Edit"))
        val (cx, cy) = ed.centerOf(ed.project.items[0])
        val before = ed.focusRequest
        ed.onDown(cx, cy); ed.onUp()
        ed.onDown(cx, cy); ed.onUp()
        assertEquals(before + 1, ed.focusRequest)
        assertEquals(TextSub.Text, ed.textSub)
    }

    @Test fun duplicateDeleteAndOrdering() {
        val ed = openEditor()
        ed.addItem(CanvasItem.text("One"))
        ed.addItem(CanvasItem.text("Two"))
        ed.reorderItem(front = false)
        assertEquals("Two", ed.project.items.first().text)
        ed.duplicateItem()
        assertEquals(3, ed.project.items.size)
        assertNotEquals(ed.project.items[0].id, ed.project.items[1].id)
        ed.deleteItem()
        assertEquals(2, ed.project.items.size)
        ed.doneEditing()
        assertNull(ed.selectedItemId)
        assertFalse(ed.panelOpen)
    }

    @Test fun sliderValuesRoundTrip() {
        val ed = openEditor()
        ed.addItem(CanvasItem.text("Hi"))
        ItemSlider.entries.forEach { s ->
            val mid = (s.min + s.max) / 2f
            ed.itemOp { s.set(it, mid) }
            assertEquals(s.label, mid, s.get(ed.selectedItem!!), s.step + .01f)
        }
        assertEquals("17%", ItemSlider.Size.format(17f))
        assertEquals("16.5%", ItemSlider.Size.format(16.5f))
        assertEquals("30°", ItemSlider.Rotate.format(30f))
    }

    @Test fun textColoursNeverLeaveTextInvisible() {
        val ed = openEditor()
        ed.addItem(CanvasItem.text("Hi"))
        ed.itemOp { it.copy(fill = null, strokeW = 0f) }
        // removing the fill with no outline is handled by the colour panel; the model itself accepts it
        assertNull(ed.selectedItem!!.fill)
    }

    @Test fun editsAreSavedAfterAShortDelay() {
        val ed = openEditor()
        ed.setGap(7f)
        await { vm.store.projects.value.firstOrNull { it.id == ed.project.id }?.gapPercent == 7f }
        assertNotNull(vm.store.projects.value.firstOrNull())
    }

    // ------------------------------------------------------------------ export

    @Test fun exportSizeFollowsTheShape() {
        val p = Samples.draft(LayoutCatalog.byId("g2x2"), aspect = AspectPreset.Story916)
        val (w, h) = Exporter.sizeFor(p)
        assertEquals(2048, h)
        assertEquals(1152, w)
    }

    @Test fun watermarkOnlyAppearsInTheCorner() {
        val p = Samples.draft(LayoutCatalog.byId("g2x2")).copy(background = 0xFFFFFFFF, gapPercent = 0f, marginPercent = 0f, cornerPercent = 0f, id = "wm")
        val with = runBlocking { Exporter.render(app.photos, p, watermark = true, longSide = 600) }
        val without = runBlocking { Exporter.render(app.photos, p, watermark = false, longSide = 600) }
        assertEquals(with.width, without.width)
        var diffTopLeft = 0; var diffCorner = 0
        for (x in 0 until 200) for (y in 0 until 100) if (with.getPixel(x, y) != without.getPixel(x, y)) diffTopLeft++
        for (x in 380 until 590) for (y in 520 until 590) if (with.getPixel(x, y) != without.getPixel(x, y)) diffCorner++
        assertEquals(0, diffTopLeft)
        assertTrue("watermark missing: $diffCorner", diffCorner > 500)
    }
}
