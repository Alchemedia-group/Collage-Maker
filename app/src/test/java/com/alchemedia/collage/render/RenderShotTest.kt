package com.alchemedia.collage.render

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.test.core.app.ApplicationProvider
import com.alchemedia.collage.TestAssets
import com.alchemedia.collage.model.AspectPreset
import com.alchemedia.collage.model.CanvasItem
import com.alchemedia.collage.model.CollageProject
import com.alchemedia.collage.model.LayoutCatalog
import com.alchemedia.collage.model.PhotoRef
import com.alchemedia.collage.model.ShadowSpec
import com.alchemedia.collage.model.TextPresets
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/** Renders collages with the real renderer and writes PNGs to app/build/shots for a visual check. */
@RunWith(RobolectricTestRunner::class)
class RenderShotTest {
    private val names = listOf("moraine", "beach", "milky", "wheat", "forest", "boat")
    private lateinit var context: Context
    private lateinit var bitmaps: Map<String, Bitmap>
    private val out = File("build/shots").apply { mkdirs() }

    @Before fun setUp() {
        TestAssets.loadCatalog()
        context = ApplicationProvider.getApplicationContext()
        Fonts.init(context)
        bitmaps = names.associate { n ->
            val id = context.resources.getIdentifier("photo_$n", "drawable", context.packageName)
            "p_$n" to BitmapFactory.decodeResource(context.resources, id)
        }
    }

    private val source = PhotoSource { bitmaps[it] }

    private fun project(layout: String, count: Int = 6, aspect: AspectPreset = AspectPreset.Square) = CollageProject.create(
        "t", 0, names.take(count).map { PhotoRef("p_$it", "res:photo_$it", 640, 427) }, LayoutCatalog.byId(layout), aspect,
    )

    private fun shot(name: String, p: CollageProject, w: Int = 900, watermark: Boolean = false): Bitmap {
        val h = (w / p.aspect.ratio).toInt()
        val bmp = CollageRenderer.render(p, w, h, source, watermark)
        File(out, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return bmp
    }

    @Test fun hearts() {
        val bmp = shot("hearts4", project("hearts4", 4))
        assertTrue(bmp.getPixel(450, 450) != 0)
    }

    @Test fun letters() { shot("love", project("w-love", 4).copy(background = 0xFFF3F4F6)) }

    @Test fun grid() { shot("hero", project("hero", 6).copy(background = 0xFF111827)) }

    @Test fun clover() { shot("clover", project("clover", 4)) }

    @Test fun textures() {
        Textures.all.filter { it.id != "none" }.forEach { t ->
            val p = project("g2x2", 4).copy(texture = t.id, background = t.base ?: 0xFFF1E7DC, gapPercent = 6f, marginPercent = 6f)
            shot("tex_${t.id}", p, 500)
        }
    }

    @Test fun filters() {
        listOf("noir", "vivid", "sepia", "sunset").forEach { f -> shot("filter_$f", project("g2x2", 4).copy(filter = f), 400) }
        shot("filter_half", project("g2x2", 4).copy(filter = "noir", filterAmount = .5f), 400)
    }

    @Test fun textAndStickers() {
        val items = listOf(
            CanvasItem.text("Birthday", font = "pacifico", size = .17f, y = .3f, rot = -8f, strokeColor = 0xFFFF4F8B, strokeW = .12f),
            CanvasItem.text("SUMMER", font = "bebas", size = .17f, y = .78f, strokeW = 0f, spacing = .08f, shadow = ShadowSpec(0x59000000, .12f, 0f, .05f)),
            CanvasItem.text("retro", font = "bungee", size = .1f, y = .5f, fill = 0xFFFFFFFF, strokeW = 0f, shadow = ShadowSpec(0xFF2557D6, 0f, .07f, .07f)),
            CanvasItem.text("curved text", font = "righteous", size = .08f, y = .9f).copy(curve = 60f, strokeW = 0f),
            CanvasItem.text("Enjoy", font = "pixel", size = .06f, y = .62f, fill = 0xFF111827, strokeW = 0f, bg = 0xFFFFFFFF),
            CanvasItem.emoji("🎉").copy(x = .15f, y = .15f),
            CanvasItem.shape("heart").copy(x = .85f, y = .2f, stickerFill = 0xFFFF4F8B),
            CanvasItem.shape("star").copy(x = .8f, y = .6f, rot = 15f),
        )
        shot("items", project("g2x2", 4).copy(items = items), 900, watermark = true)
    }

    @Test fun presets() {
        val base = CanvasItem.text("Aa Bb", font = "bric", size = .14f)
        val items = TextPresets.all.mapIndexed { i, preset -> preset.apply.invoke(base).copy(y = .08f + i * .1f) }
        shot("presets", project("g1x1", 1).copy(items = items, filter = "dramatic"), 700)
    }

    @Test fun portraitAndStory() {
        shot("story", project("hero", 6, AspectPreset.Story916), 540)
        shot("wide", project("g3x1", 3, AspectPreset.Wide169), 800)
    }

    @Test fun exportSizeKeepsRatio() {
        val (w, h) = CollageRenderer.exportSize(AspectPreset.Story916.ratio, 2048)
        assertTrue(h == 2048 && w in 1150..1153)
    }
}
