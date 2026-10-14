package com.alchemedia.collage.ui

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w390dp-h844dp-xhdpi")
class SplashTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    private fun luma(argb: Int) = (0.299 * ((argb shr 16) and 255) + 0.587 * ((argb shr 8) and 255) + 0.114 * (argb and 255))

    @Test fun theSplashIsTheIconGradientWithTheIconInTheMiddle() {
        rule.setContent { SplashContent() }
        rule.mainClock.advanceTimeBy(100)
        val bmp = rule.onRoot().captureToImage().asAndroidBitmap()
        val out = File("build/shots/ui").apply { mkdirs() }
        File(out, "splash.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val w = bmp.width
        val h = bmp.height
        // the gradient runs from a lighter blue at the top to a darker blue at the bottom
        val top = bmp.getPixel(w / 2, h / 20)
        val bottom = bmp.getPixel(w / 2, h - h / 20)
        assertTrue("top is lighter than bottom", luma(top) > luma(bottom) + 20)
        assertTrue("both are blue", (top and 255) > ((top shr 16) and 255) && (bottom and 255) > ((bottom shr 16) and 255))
        // and the middle holds the artwork, which is much lighter than the blue around it
        var bright = 0
        for (y in h / 2 - h / 10 until h / 2 + h / 10) for (x in w / 2 - w / 4 until w / 2 + w / 4) if (luma(bmp.getPixel(x, y)) > 170) bright++
        assertTrue("the icon is drawn in the middle ($bright light pixels)", bright > 2000)
    }

    @Test fun theSplashFinishesByItself() {
        var done by mutableStateOf(false)
        rule.setContent { SplashOverlay(onDone = { done = true }) }
        rule.mainClock.advanceTimeBy(500)
        assertFalse("still showing while the icon settles", done)
        rule.mainClock.advanceTimeBy(1500)
        assertTrue("gone after about a second and a half", done)
    }

    @Test fun tappingDuringTheSplashDoesNotReachTheScreenBelow() {
        var taps by mutableStateOf(0)
        rule.setContent {
            Box {
                Box(Modifier.fillMaxSize().clickable { taps++ })
                SplashOverlay(onDone = {})
            }
        }
        rule.onRoot().performClick()
        assertEquals(0, taps)
    }
}
