package com.alchemedia.collage.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import com.alchemedia.collage.ui.theme.CColors
import com.alchemedia.collage.ui.theme.DarkColors
import com.alchemedia.collage.ui.theme.LightColors
import org.junit.Assert.assertTrue
import org.junit.Test

/** WCAG contrast of the text colours on the backgrounds they are used on: 4.5 to 1 for normal text. */
class ContrastTest {
    private fun ratio(fg: Color, bg: Color): Double {
        val a = fg.compositeOver(bg).luminance() + 0.05
        val b = bg.luminance() + 0.05
        return maxOf(a, b).toDouble() / minOf(a, b)
    }

    private fun pairs(c: CColors) = listOf(
        "ink on paper" to ratio(c.ink, c.paper),
        "ink on card" to ratio(c.ink, c.card),
        "muted on paper" to ratio(c.muted, c.paper),
        "muted on card" to ratio(c.muted, c.card),
        "muted on desk" to ratio(c.muted, c.desk),
        "accent on paper" to ratio(c.accent, c.paper),
        "accent on card" to ratio(c.accent, c.card),
        "onAccent on accent" to ratio(c.onAccent, c.accent),
        "onTint on tint" to ratio(c.onTint, c.tint),
        "onPro on pro" to ratio(c.onPro, c.pro),
        "danger on paper" to ratio(c.danger, c.paper),
        "dock label on dock" to ratio(c.dockFg.copy(alpha = .72f), c.dockBg),
    )

    private fun check(name: String, c: CColors) {
        val failing = pairs(c).filter { it.second < 4.5 }
        pairs(c).forEach { println("CONTRAST $name ${it.first}: ${"%.2f".format(it.second)}") }
        assertTrue("$name below 4.5:1: ${failing.joinToString { it.first + " " + "%.2f".format(it.second) }}", failing.isEmpty())
    }

    @Test fun lightTheme() = check("light", LightColors)
    @Test fun darkTheme() = check("dark", DarkColors)
}
