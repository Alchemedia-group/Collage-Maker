package com.alchemedia.collage.render

import com.alchemedia.collage.Strings
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import kotlin.math.cos
import kotlin.math.sin

data class FilterDef(val id: String, val name: String, val css: String) {
    val title: String get() = Strings.named("filter_$id", name)
}

/** Photo filters. Each is written as a CSS filter string and turned into one colour matrix. */
object Filters {
    val all = listOf(
        FilterDef("none", "None", "none"),
        FilterDef("warm", "Warm", "sepia(.28) saturate(1.25) hue-rotate(-8deg)"),
        FilterDef("cool", "Cool", "saturate(1.1) hue-rotate(12deg) brightness(1.03)"),
        FilterDef("mono", "Mono", "grayscale(1) contrast(1.1)"),
        FilterDef("noir", "Noir", "grayscale(1) contrast(1.45) brightness(.92)"),
        FilterDef("fade", "Fade", "contrast(.85) brightness(1.08) saturate(.85)"),
        FilterDef("vivid", "Vivid", "saturate(1.5) contrast(1.08)"),
        FilterDef("sepia", "Sepia", "sepia(.85) contrast(1.05)"),
        FilterDef("vintage", "Vintage", "sepia(.45) contrast(.92) saturate(1.15) hue-rotate(-12deg) brightness(1.04)"),
        FilterDef("film", "Film", "contrast(1.12) saturate(.88) sepia(.12) brightness(1.02)"),
        FilterDef("matte", "Matte", "contrast(.88) brightness(1.06) saturate(.78)"),
        FilterDef("cinema", "Cinema", "contrast(1.22) saturate(.85) sepia(.15) hue-rotate(8deg)"),
        FilterDef("dramatic", "Dramatic", "contrast(1.38) saturate(1.1) brightness(.94)"),
        FilterDef("bright", "Bright", "brightness(1.16) saturate(1.1) contrast(1.02)"),
        FilterDef("sunset", "Sunset", "sepia(.35) saturate(1.6) hue-rotate(-16deg) brightness(1.03)"),
        FilterDef("ocean", "Ocean", "hue-rotate(16deg) saturate(1.3) brightness(1.02)"),
        FilterDef("forest", "Forest", "hue-rotate(28deg) saturate(1.2) contrast(1.05)"),
        FilterDef("rose", "Rose", "sepia(.22) hue-rotate(-30deg) saturate(1.4) brightness(1.04)"),
        FilterDef("lavender", "Lavender", "hue-rotate(42deg) saturate(1.15) brightness(1.04)"),
        FilterDef("punch", "Punch", "contrast(1.18) saturate(1.6)"),
    )

    fun byId(id: String): FilterDef = all.firstOrNull { it.id == id } ?: all[0]

    private val call = Regex("""([a-z-]+)\(\s*(-?[\d.]+)\s*(deg)?\s*\)""")
    private val cache = HashMap<String, ColorMatrix?>()

    /** The matrix for a filter id, or null for "none". */
    fun matrix(id: String): ColorMatrix? = cache.getOrPut(id) { fromCss(byId(id).css) }

    fun colorFilter(id: String): ColorMatrixColorFilter? = matrix(id)?.let { ColorMatrixColorFilter(it) }

    /**
     * The matrix for one photo's own brightness, contrast and saturation sliders (each -1..1), or null when all are
     * zero. Applied before the collage-wide filter.
     */
    fun adjust(brightness: Float, contrast: Float, saturation: Float): ColorMatrix? {
        if (brightness == 0f && contrast == 0f && saturation == 0f) return null
        val result = ColorMatrix()
        if (saturation != 0f) result.postConcat(saturate((1f + saturation).coerceIn(0f, 2f)))
        if (contrast != 0f) result.postConcat(contrast((1f + contrast * .6f).coerceAtLeast(0f)))
        if (brightness != 0f) result.postConcat(brightness((1f + brightness * .6f).coerceAtLeast(0f)))
        return result
    }

    internal fun fromCss(css: String): ColorMatrix? {
        if (css == "none") return null
        val result = ColorMatrix()
        call.findAll(css).forEach { m ->
            val v = m.groupValues[2].toFloat()
            result.postConcat(
                when (m.groupValues[1]) {
                    "sepia" -> sepia(v)
                    "grayscale" -> grayscale(v)
                    "saturate" -> saturate(v)
                    "hue-rotate" -> hueRotate(v)
                    "contrast" -> contrast(v)
                    "brightness" -> brightness(v)
                    else -> ColorMatrix()
                },
            )
        }
        return result
    }

    private fun m(vararg rgbRows: Float) = ColorMatrix(
        floatArrayOf(
            rgbRows[0], rgbRows[1], rgbRows[2], 0f, 0f,
            rgbRows[3], rgbRows[4], rgbRows[5], 0f, 0f,
            rgbRows[6], rgbRows[7], rgbRows[8], 0f, 0f,
            0f, 0f, 0f, 1f, 0f,
        ),
    )

    private fun sepia(a: Float): ColorMatrix {
        val s = 1 - a.coerceIn(0f, 1f)
        return m(
            .393f + .607f * s, .769f - .769f * s, .189f - .189f * s,
            .349f - .349f * s, .686f + .314f * s, .168f - .168f * s,
            .272f - .272f * s, .534f - .534f * s, .131f + .869f * s,
        )
    }

    private fun grayscale(a: Float): ColorMatrix {
        val s = 1 - a.coerceIn(0f, 1f)
        return m(
            .2126f + .7874f * s, .7152f - .7152f * s, .0722f - .0722f * s,
            .2126f - .2126f * s, .7152f + .2848f * s, .0722f - .0722f * s,
            .2126f - .2126f * s, .7152f - .7152f * s, .0722f + .9278f * s,
        )
    }

    private fun saturate(s: Float) = m(
        .213f + .787f * s, .715f - .715f * s, .072f - .072f * s,
        .213f - .213f * s, .715f + .285f * s, .072f - .072f * s,
        .213f - .213f * s, .715f - .715f * s, .072f + .928f * s,
    )

    private fun hueRotate(deg: Float): ColorMatrix {
        val a = Math.toRadians(deg.toDouble())
        val c = cos(a).toFloat()
        val s = sin(a).toFloat()
        return m(
            .213f + c * .787f - s * .213f, .715f - c * .715f - s * .715f, .072f - c * .072f + s * .928f,
            .213f - c * .213f + s * .143f, .715f + c * .285f + s * .140f, .072f - c * .072f - s * .283f,
            .213f - c * .213f - s * .787f, .715f - c * .715f + s * .715f, .072f + c * .928f + s * .072f,
        )
    }

    private fun contrast(c: Float): ColorMatrix {
        val t = (.5f - .5f * c) * 255f
        return ColorMatrix(floatArrayOf(c, 0f, 0f, 0f, t, 0f, c, 0f, 0f, t, 0f, 0f, c, 0f, t, 0f, 0f, 0f, 1f, 0f))
    }

    private fun brightness(b: Float) = ColorMatrix(floatArrayOf(b, 0f, 0f, 0f, 0f, 0f, b, 0f, 0f, 0f, 0f, 0f, b, 0f, 0f, 0f, 0f, 0f, 1f, 0f))
}
