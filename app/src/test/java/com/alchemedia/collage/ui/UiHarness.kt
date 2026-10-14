package com.alchemedia.collage.ui

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import com.alchemedia.collage.CollageApp
import com.alchemedia.collage.model.AspectPreset
import com.alchemedia.collage.model.CanvasItem
import com.alchemedia.collage.model.CollageProject
import com.alchemedia.collage.model.LayoutCatalog
import com.alchemedia.collage.model.Samples
import com.alchemedia.collage.ui.components.LocalApp
import com.alchemedia.collage.ui.theme.CollageTheme
import kotlinx.coroutines.runBlocking
import java.io.File

/** Shared set-up for screenshot tests: the real app root on a Robolectric phone, plus helpers to wait and capture. */
class UiHarness(private val rule: AndroidComposeTestRule<*, ComponentActivity>, private val dark: Boolean = false) {
    val app: CollageApp = ApplicationProvider.getApplicationContext()
    val vm = AppViewModel(app)
    private val out = File("build/shots/ui").apply { mkdirs() }

    fun start() {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            CompositionLocalProvider(LocalApp provides app) {
                CollageTheme(dark) { AppRoot(vm) }
            }
        }
        settle()
    }

    /** Lets background work (photo decoding, saving) finish and the animations play out. */
    fun settle(ms: Long = 900) {
        // tests change state from outside Compose; make sure those writes reach the composition before the clock moves
        androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications()
        repeat(6) {
            Thread.sleep(120)
            rule.mainClock.advanceTimeBy(ms / 6)
            org.robolectric.shadows.ShadowLooper.idleMainLooper()
        }
        rule.mainClock.advanceTimeBy(300)
    }

    fun shot(name: String): Bitmap {
        settle(400)
        repeat(12) { rule.mainClock.advanceTimeBy(32); org.robolectric.shadows.ShadowLooper.idleMainLooper(); Thread.sleep(15) }
        val bmp = rule.onRoot().captureToImage().asAndroidBitmap()
        File(out, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return bmp
    }

    fun run(block: suspend () -> Unit) = runBlocking { block() }

    /** A saved collage of sample photos, ready for Home and the editor. */
    fun makeProject(layoutId: String, name: String, items: List<CanvasItem> = emptyList(), mutate: (CollageProject) -> CollageProject = { it }): CollageProject {
        val layout = LayoutCatalog.byId(layoutId)
        val base = Samples.draft(layout, offset = name.length).copy(id = CollageProject.newId(), name = name, cornerPercent = 12f, items = items)
        val project = mutate(base)
        return runBlocking { app.store.save(project) }
    }
}
