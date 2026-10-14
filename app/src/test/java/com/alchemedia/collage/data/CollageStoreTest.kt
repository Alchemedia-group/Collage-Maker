package com.alchemedia.collage.data

import android.graphics.Bitmap
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import androidx.test.core.app.ApplicationProvider
import com.alchemedia.collage.CollageApp
import com.alchemedia.collage.TestAssets
import com.alchemedia.collage.model.CollageProject
import com.alchemedia.collage.model.Layout
import com.alchemedia.collage.model.LayoutCatalog
import com.alchemedia.collage.model.LayoutCell
import com.alchemedia.collage.model.PhotoRef
import com.alchemedia.collage.model.Samples
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
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CollageStoreTest {
    private lateinit var app: CollageApp
    private lateinit var store: CollageStore

    @Before fun setUp() {
        TestAssets.loadCatalog()
        app = ApplicationProvider.getApplicationContext()
        store = CollageStore(app)
        runBlocking { store.load() }
    }

    private fun project(name: String = "A", layout: String = "g2x2") =
        Samples.draft(LayoutCatalog.byId(layout)).copy(id = CollageProject.newId(), name = name)

    private fun reopen(): CollageStore = CollageStore(app).also { runBlocking { it.load() } }

    private fun photoFile(width: Int, height: Int, orientation: Int? = null): File {
        val f = File(app.cacheDir, "in-${System.nanoTime()}.jpg")
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply { eraseColor(0xFF3366CC.toInt()) }
        f.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        if (orientation != null) ExifInterface(f.path).apply { setAttribute(ExifInterface.TAG_ORIENTATION, orientation.toString()); saveAttributes() }
        return f
    }

    @Test fun startsEmpty() {
        assertTrue(store.projects.value.isEmpty())
        assertTrue(store.history.value.isEmpty())
        assertTrue(store.loaded.value)
    }

    @Test fun savedProjectsComeBackAfterARestart() {
        val p = runBlocking { store.save(project("Trip")) }
        val again = reopen()
        assertEquals(listOf(p.id), again.projects.value.map { it.id })
        assertEquals("Trip", again.projects.value.single().name)
    }

    @Test fun photoToolsFramesAndGradientsSurviveARestart() {
        val base = project("Tools")
        val p = base.copy(
            frame = "film", frameColor = 0xFF123456, frameAmount = 7f,
            cells = base.cells.mapIndexed { i, c ->
                if (i == 0) c.copy(turns = 1, angle = -3f, flipH = true, flipV = true, brightness = .2f, contrast = -.4f, saturation = .6f) else c
            },
            items = listOf(com.alchemedia.collage.model.CanvasItem.text("Hi").copy(fill2 = 0xFF0000FF, gradientAngle = 30f)),
        )
        runBlocking { store.save(p) }
        val back = reopen().projects.value.first { it.id == p.id }
        assertEquals(p.cells, back.cells)
        assertEquals(p.items, back.items)
        assertEquals(listOf("film", 0xFF123456, 7f), listOf(back.frame, back.frameColor, back.frameAmount))
    }

    @Test fun newestProjectIsFirst() {
        val a = runBlocking { store.save(project("A")) }
        Thread.sleep(5)
        val b = runBlocking { store.save(project("B")) }
        assertEquals(listOf(b.id, a.id), store.projects.value.map { it.id })
        Thread.sleep(5)
        runBlocking { store.save(a) }
        assertEquals(listOf(a.id, b.id), store.projects.value.map { it.id })
    }

    @Test fun deleteAndRestoreKeepTheSamePosition() {
        val a = runBlocking { store.save(project("A")) }
        Thread.sleep(5)
        val b = runBlocking { store.save(project("B")) }
        Thread.sleep(5)
        val c = runBlocking { store.save(project("C")) }
        val (index, gone) = runBlocking { store.delete(b.id) }!!
        assertEquals(1, index)
        assertEquals(listOf(c.id, a.id), store.projects.value.map { it.id })
        runBlocking { store.restore(index, gone) }
        assertEquals(listOf(c.id, b.id, a.id), store.projects.value.map { it.id })
        assertEquals(3, reopen().projects.value.size)
    }

    @Test fun deletingAnUnknownProjectDoesNothing() {
        assertNull(runBlocking { store.delete("nope") })
    }

    @Test fun duplicateGetsItsOwnIdAndPhotos() {
        val src = File(app.filesDir, "photos/src").apply { mkdirs() }
        File(src, "x.jpg").writeText("data")
        val p = runBlocking { store.save(project("Orig").copy(id = "src")) }
        val copy = runBlocking { store.duplicate(p) }
        assertNotEquals(p.id, copy.id)
        assertEquals("Orig copy", copy.name)
        assertTrue(File(app.filesDir, "photos/${copy.id}/x.jpg").exists())
        assertEquals(2, store.projects.value.size)
    }

    @Test fun brokenProjectFilesAreIgnored() {
        runBlocking { store.save(project("Good")) }
        File(app.filesDir, "projects/broken.json").writeText("{ not json")
        File(app.filesDir, "projects/empty.json").writeText("")
        val again = reopen()
        assertEquals(listOf("Good"), again.projects.value.map { it.name })
    }

    @Test fun projectWhoseLayoutVanishedIsBroughtBackInLine() {
        val mine = Layout("gone", "Gone", "mine", cells = List(2) { LayoutCell(0f, it * .5f, 1f, .5f) }, mine = true)
        runBlocking { store.saveLayout(mine) }
        val p = Samples.draft(mine).copy(id = CollageProject.newId())
        runBlocking { store.save(p) }
        runBlocking { store.deleteLayout("gone") }
        val again = reopen()
        val loaded = again.projects.value.single()
        assertEquals(LayoutCatalog.DEFAULT_ID, loaded.layoutId)
        assertEquals(LayoutCatalog.byId(LayoutCatalog.DEFAULT_ID).cells.size, loaded.cells.size)
    }

    // ------------------------------------------------------------------ history

    @Test fun historyKeepsEntriesNewestFirstAndTrimsToSixty() {
        val p = project()
        repeat(65) { runBlocking { store.recordHistory(p, 100, 100, it % 2 == 0) } }
        assertEquals(60, store.history.value.size)
        assertTrue(store.history.value.zipWithNext().all { (a, b) -> a.time >= b.time })
        assertEquals(60, reopen().history.value.size)
    }

    @Test fun historyDeleteAndUndo() {
        val p = project()
        val e1 = runBlocking { store.recordHistory(p, 1, 1, true) }
        Thread.sleep(3)
        val e2 = runBlocking { store.recordHistory(p, 1, 1, false) }
        val (index, entry) = runBlocking { store.deleteHistory(e2.id) }!!
        assertEquals(listOf(e1.id), store.history.value.map { it.id })
        runBlocking { store.restoreHistory(index, entry) }
        assertEquals(listOf(e2.id, e1.id), store.history.value.map { it.id })
        assertEquals(2, reopen().history.value.size)
    }

    @Test fun clearHistoryCanBeUndone() {
        val p = project()
        runBlocking { store.recordHistory(p, 1, 1, true); store.recordHistory(p, 1, 1, true) }
        val all = runBlocking { store.clearHistory() }
        assertEquals(2, all.size)
        assertTrue(store.history.value.isEmpty())
        assertTrue(reopen().history.value.isEmpty())
        runBlocking { store.restoreAllHistory(all) }
        assertEquals(2, reopen().history.value.size)
    }

    @Test fun historySnapshotIsNotChangedByLaterEdits() {
        val p = runBlocking { store.save(project("v1")) }
        val entry = runBlocking { store.recordHistory(p, 1, 1, true) }
        runBlocking { store.save(p.copy(name = "v2")) }
        assertEquals("v1", store.history.value.first { it.id == entry.id }.project.name)
    }

    // ------------------------------------------------------------------ photos

    @Test fun photosOfDeletedCollagesAreCleanedUpUnlessHistoryNeedsThem() {
        val keep = File(app.filesDir, "photos/keep").apply { mkdirs() }
        val drop = File(app.filesDir, "photos/drop").apply { mkdirs() }
        val inHistory = File(app.filesDir, "photos/hist").apply { mkdirs() }
        listOf(keep, drop, inHistory).forEach { File(it, "a.jpg").writeText("x") }
        runBlocking {
            store.save(project("K").copy(id = "keep"))
            store.recordHistory(project("H").copy(id = "hist"), 1, 1, true)
        }
        reopen()
        assertTrue(keep.exists())
        assertTrue(inHistory.exists())
        assertFalse(drop.exists())
    }

    @Test fun importedPhotosAreCopiedAndMeasured() {
        val f = photoFile(120, 60)
        val refs = runBlocking { store.importPhotos("p1", listOf(Uri.fromFile(f))) }
        assertEquals(1, refs.size)
        assertEquals(120, refs[0].width); assertEquals(60, refs[0].height)
        assertTrue(store.photoFile("p1", refs[0]).exists())
    }

    @Test fun exifRotationSwapsTheMeasuredSize() {
        val f = photoFile(120, 60, ExifInterface.ORIENTATION_ROTATE_90)
        val ref = runBlocking { store.importPhotos("p2", listOf(Uri.fromFile(f))) }.single()
        assertEquals(60, ref.width); assertEquals(120, ref.height)
    }

    @Test fun unreadableFilesAreSkipped() {
        val bad = File(app.cacheDir, "bad.jpg").apply { writeText("not a picture") }
        val good = photoFile(10, 10)
        val refs = runBlocking { store.importPhotos("p3", listOf(Uri.fromFile(bad), Uri.fromFile(good), Uri.parse("file:///no/such/file.jpg"))) }
        assertEquals(1, refs.size)
    }

    @Test fun photoStoreDecodesUprightAndCaches() {
        val f = photoFile(200, 100, ExifInterface.ORIENTATION_ROTATE_90)
        val ref = runBlocking { store.importPhotos("p4", listOf(Uri.fromFile(f))) }.single()
        val photos = PhotoStore(app, store)
        val bmp = photos.load("p4", ref, 512)!!
        assertEquals(100, bmp.width); assertEquals(200, bmp.height)
        assertTrue(photos.load("p4", ref, 512) === bmp)
        assertNotNull(photos.load("any", Samples.moraine, 300))
        assertNull(photos.load("p4", PhotoRef("x", "missing.jpg", 1, 1), 100))
    }

    @Test fun photoStoreDownsamplesLargePhotos() {
        val f = photoFile(2000, 1000)
        val ref = runBlocking { store.importPhotos("p5", listOf(Uri.fromFile(f))) }.single()
        val bmp = PhotoStore(app, store).load("p5", ref, 500)!!
        assertTrue(bmp.width in 500..1000)
    }

    // ------------------------------------------------------------------ my layouts

    @Test fun myLayoutsSurviveRestartAndJoinTheCatalogue() {
        val l = Layout("my-1", "Mine", "mine", cells = listOf(LayoutCell(0f, 0f, .5f, 1f), LayoutCell(.5f, 0f, .5f, 1f)), mine = true)
        runBlocking { store.saveLayout(l) }
        reopen()
        assertEquals(listOf("my-1"), LayoutCatalog.forCategory("mine", 0).map { it.id })
        assertEquals("Mine", LayoutCatalog.byId("my-1").name)
    }

    @Test fun badLayoutsAreRejected() {
        val ok = LayoutCell(0f, 0f, 1f, 1f)
        listOf(
            Layout("", "x", "mine", cells = listOf(ok)),
            Layout("a", "x", "mine", cells = emptyList()),
            Layout("b", "x", "mine", cells = List(41) { ok }),
            Layout("c", "x", "mine", cells = listOf(LayoutCell(0f, 0f, 0f, 1f))),
            Layout("d", "x", "mine", cells = listOf(LayoutCell(Float.NaN, 0f, 1f, 1f))),
        ).forEach { runBlocking { store.saveLayout(it) } }
        assertTrue(store.myLayouts.value.isEmpty())
    }

    @Test fun layoutCellsAreLimitedAndUnknownShapesDropped() {
        runBlocking { store.saveLayout(Layout("e", "  ", "x", cells = listOf(LayoutCell(-3f, 9f, 5f, 5f, shape = "nope", text = "ABCDE")), mine = false)) }
        val l = store.myLayouts.value.single()
        assertEquals("My layout", l.name)
        assertEquals("mine", l.cat)
        assertTrue(l.mine)
        val c = l.cells.single()
        assertEquals(-1f, c.x, 0f); assertEquals(2f, c.y, 0f); assertEquals(3f, c.w, 0f)
        assertNull(c.shape)
        assertEquals("AB", c.text)
    }

    @Test fun editingALayoutReplacesIt() {
        val a = Layout("m", "One", "mine", cells = listOf(LayoutCell(0f, 0f, 1f, 1f)), mine = true)
        runBlocking { store.saveLayout(a); store.saveLayout(a.copy(name = "Two")) }
        assertEquals(listOf("Two"), store.myLayouts.value.map { it.name })
    }

    @Test fun corruptLayoutFileIsIgnored() {
        File(app.filesDir, "my_layouts.json").writeText("[[[")
        File(app.filesDir, "history.json").writeText("{}")
        val again = reopen()
        assertTrue(again.myLayouts.value.isEmpty())
        assertTrue(again.history.value.isEmpty())
    }
}
