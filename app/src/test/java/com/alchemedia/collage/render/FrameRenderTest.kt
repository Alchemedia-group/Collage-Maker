package com.alchemedia.collage.render

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.alchemedia.collage.TestAssets
import com.alchemedia.collage.model.CanvasItem
import com.alchemedia.collage.model.CollageProject
import com.alchemedia.collage.model.LayoutCatalog
import com.alchemedia.collage.model.PhotoRef
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FrameRenderTest {
    private lateinit var context: Context
    private val blue = 0xFF2040C0.toInt()
    private val green = 0xFF00AA00.toInt()

    private val source = PhotoSource { Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888).apply { eraseColor(blue) } }

    @Before fun setUp() {
        TestAssets.loadCatalog()
        context = ApplicationProvider.getApplicationContext()
        Fonts.init(context)
    }

    /** Four blue photos on a green background, 400 x 400, so each cell is 200 x 200 and there are no gaps. */
    private fun project(frame: String = Frames.NONE): CollageProject {
        val photos = List(4) { PhotoRef("p$it", "p$it.jpg", 100, 100) }
        val base = CollageProject.create("t", 0, photos, LayoutCatalog.byId("g2x2"))
        val def = Frames.byId(frame)
        return base.copy(gapPercent = 0f, marginPercent = 0f, cornerPercent = 0f, background = green.toLong() and 0xFFFFFFFFL, frame = def.id, frameColor = def.color, frameAmount = def.amount)
    }

    private fun render(p: CollageProject) = CollageRenderer.render(p, 400, 400, source, false)

    @Test fun noFrameLeavesThePhotoEdgeToEdge() {
        val bmp = render(project())
        assertEquals(blue, bmp.getPixel(2, 2))
        assertEquals(blue, bmp.getPixel(100, 100))
    }

    @Test fun everyFrameDrawsWithoutLosingThePhoto() {
        val plain = render(project())
        Frames.all.filter { it.id != Frames.NONE }.forEach { f ->
            val bmp = render(project(f.id))
            assertEquals("${f.id}: centre of the first photo", blue, bmp.getPixel(100, 100))
            val same = (0 until 400 step 7).all { y -> (0 until 400 step 7).all { x -> bmp.getPixel(x, y) == plain.getPixel(x, y) } }
            assertTrue("${f.id} should change the picture", !same)
        }
    }

    @Test fun aBorderIsAFlatRingOfTheFrameColour() {
        val bmp = render(project("border"))
        assertEquals(0xFFFFFFFF.toInt(), bmp.getPixel(2, 2))
        assertEquals(blue, bmp.getPixel(100, 100))
    }

    @Test fun aPolaroidHasAThickerBottomEdge() {
        val bmp = render(project("polaroid"))
        // t = 4.5% of 200 = 9px; the bottom margin is 3t more.
        assertEquals(blue, bmp.getPixel(100, 5 + 9))
        assertEquals(0xFFFFFFFF.toInt(), bmp.getPixel(100, 185))
        assertEquals(blue, bmp.getPixel(100, 150))
    }

    @Test fun filmSprocketHolesShowTheBackground() {
        val bmp = render(project("film"))
        assertEquals(0xFF15181F.toInt(), bmp.getPixel(3, 100))
        assertEquals(green, bmp.getPixel(13, 12))
    }

    @Test fun thePhotoOnlyFillsWhatTheFrameLeaves() {
        val p = project("polaroid")
        val r = com.alchemedia.collage.model.CollageGeometry.cellRects(p.layout.cells, 400f, 400f, 0f, 0f)[0]
        val inner = CollageRenderer.photoRect(r, p)
        assertTrue(inner.width() < r.width && inner.bottom < r.bottom - 20f)
        val plain = CollageRenderer.photoRect(r, null)
        assertEquals(r.width, plain.width(), .01f)
    }

    @Test fun aTornEdgeIsTheSameEveryTimeItIsDrawn() {
        val a = render(project("torn"))
        val b = render(project("torn"))
        assertTrue(a.sameAs(b))
    }

    @Test fun lightSettingsChangeOnlyThatPhoto() {
        val p = project().let { it.copy(cells = it.cells.mapIndexed { i, c -> if (i == 0) c.copy(brightness = .8f) else c }) }
        val bmp = render(p)
        val lit = bmp.getPixel(100, 100)
        assertTrue("brightened pixel ${Integer.toHexString(lit)}", Color.blue(lit) > Color.blue(blue))
        assertEquals(blue, bmp.getPixel(300, 100))
    }

    @Test fun mirroringAPhotoFlipsItsContent() {
        // Left half red, right half blue.
        val two = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888).apply {
            for (y in 0 until 100) for (x in 0 until 100) setPixel(x, y, if (x < 50) 0xFFDD2222.toInt() else 0xFF2222DD.toInt())
        }
        val src = PhotoSource { two }
        val base = project().let { it.copy(cells = it.cells.mapIndexed { i, c -> if (i == 0) c.copy(flipH = true) else c }) }
        val flipped = CollageRenderer.render(base, 400, 400, src, false)
        val normal = CollageRenderer.render(project(), 400, 400, src, false)
        assertTrue(Color.red(normal.getPixel(20, 100)) > 150)
        assertTrue(Color.blue(flipped.getPixel(20, 100)) > 150)
    }

    @Test fun textFadesFromOneColourToTheOther() {
        val item = CanvasItem.text("MMMM", font = "bebas", size = .3f, fill = 0xFFFF0000, strokeW = 0f).copy(fill2 = 0xFF0000FF, gradientAngle = 0f)
        val bmp = Bitmap.createBitmap(600, 200, Bitmap.Config.ARGB_8888)
        ItemRenderer.draw(android.graphics.Canvas(bmp), item, 600f, 200f)
        var left = -1
        var right = -1
        for (x in 0 until 600) for (y in 0 until 200) if (Color.alpha(bmp.getPixel(x, y)) > 200) { if (left < 0) left = x; right = x }
        assertTrue("some text was drawn", left >= 0 && right > left)
        fun colourNear(x: Int): Int {
            for (dx in 0..20) for (y in 0 until 200) { val c = bmp.getPixel(x + dx, y); if (Color.alpha(c) > 250) return c }
            error("no glyph pixel near $x")
        }
        val l = colourNear(left)
        val r = colourNear(right - 20)
        assertTrue("left should be red: ${Integer.toHexString(l)}", Color.red(l) > Color.blue(l))
        assertTrue("right should be blue: ${Integer.toHexString(r)}", Color.blue(r) > Color.red(r))
        assertNotEquals(l, r)
    }

    @Test fun plainTextKeepsOneColour() {
        val item = CanvasItem.text("MMMM", font = "bebas", size = .3f, fill = 0xFFFF0000, strokeW = 0f)
        val bmp = Bitmap.createBitmap(600, 200, Bitmap.Config.ARGB_8888)
        ItemRenderer.draw(android.graphics.Canvas(bmp), item, 600f, 200f)
        var c = 0
        loop@ for (x in 0 until 600) for (y in 0 until 200) if (Color.alpha(bmp.getPixel(x, y)) > 250) { c = bmp.getPixel(x, y); break@loop }
        assertTrue("plain text is one colour: ${Integer.toHexString(c)}", Color.red(c) > 240 && Color.green(c) < 15 && Color.blue(c) < 15)
    }
}
