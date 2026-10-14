package com.alchemedia.collage.render

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FiltersTest {
    private fun apply(css: String, r: Float, g: Float, b: Float): FloatArray {
        val a = Filters.fromCss(css)!!.array
        return floatArrayOf(
            a[0] * r + a[1] * g + a[2] * b + a[4] / 255f,
            a[5] * r + a[6] * g + a[7] * b + a[9] / 255f,
            a[10] * r + a[11] * g + a[12] * b + a[14] / 255f,
        )
    }

    @Test fun noneHasNoMatrix() = assertNull(Filters.matrix("none"))

    @Test fun everyOtherFilterParses() {
        Filters.all.filter { it.id != "none" }.forEach { assertNotNull(it.id, Filters.matrix(it.id)) }
    }

    @Test fun twentyFilters() = assertEquals(20, Filters.all.size)

    @Test fun grayscaleMakesGrey() {
        val (r, g, b) = apply("grayscale(1)", 1f, .2f, .4f).toList()
        assertEquals(r, g, .001f)
        assertEquals(g, b, .001f)
    }

    @Test fun brightnessScales() {
        val out = apply("brightness(.5)", .8f, .4f, .2f)
        assertEquals(.4f, out[0], .001f)
        assertEquals(.1f, out[2], .001f)
    }

    @Test fun contrastKeepsMidGrey() {
        val out = apply("contrast(1.5)", .5f, .5f, .5f)
        assertEquals(.5f, out[0], .001f)
    }

    @Test fun hueRotateZeroIsIdentity() {
        val out = apply("hue-rotate(0deg)", .3f, .6f, .9f)
        assertEquals(.3f, out[0], .001f)
        assertEquals(.9f, out[2], .001f)
    }

    @Test fun photoSlidersAtZeroDoNothing() = assertNull(Filters.adjust(0f, 0f, 0f))

    @Test fun brightnessSliderBrightensAndDarkens() {
        assertEquals(1.3f, Filters.adjust(.5f, 0f, 0f)!!.array[0], .001f)
        assertEquals(.7f, Filters.adjust(-.5f, 0f, 0f)!!.array[6], .001f)
    }

    @Test fun saturationSliderAllTheWayDownIsGrey() {
        val a = Filters.adjust(0f, 0f, -1f)!!.array
        // every output channel is the same mix of the input channels
        assertEquals(a[0], a[5], .001f)
        assertEquals(a[0], a[10], .001f)
        assertEquals(a[1], a[6], .001f)
    }

    @Test fun contrastSliderSpreadsTheTones() {
        val a = Filters.adjust(0f, .5f, 0f)!!.array
        assertEquals(1.3f, a[0], .001f)
        assertEquals((.5f - .5f * 1.3f) * 255f, a[4], .01f)
    }
}
