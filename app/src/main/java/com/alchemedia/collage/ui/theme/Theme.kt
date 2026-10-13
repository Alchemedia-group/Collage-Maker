package com.alchemedia.collage.ui.theme

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight

/** The design's colour tokens. Cool slate neutrals, graphite chrome, one deep blue for action, gold only for Pro. */
@Immutable
class CColors(
    val dark: Boolean,
    val paper: Color,
    val card: Color,
    val ink: Color,
    val muted: Color,
    val line: Color,
    val accent: Color,
    val onAccent: Color,
    val tint: Color,
    val onTint: Color,
    val hero: Color,
    val pro: Color,
    val onPro: Color,
    val desk: Color,
    val dot: Color,
    val dockBg: Color,
    val dockFg: Color,
    val danger: Color,
    val toastAccent: Color,
)

val LightColors = CColors(
    dark = false,
    paper = Color(0xFFF2F4F7), card = Color(0xFFFFFFFF), ink = Color(0xFF101828), muted = Color(0xFF5B6475), line = Color(0xFFDDE2EA),
    accent = Color(0xFF2557D6), onAccent = Color(0xFFFFFFFF), tint = Color(0xFFE3EBFB), onTint = Color(0xFF1D46AD), hero = Color(0xFF12213F),
    pro = Color(0xFFE3B341), onPro = Color(0xFF2B1D00), desk = Color(0xFFE4E8EF), dot = Color(0xFFC9D0DC), dockBg = Color(0xFF111827),
    dockFg = Color(0xFFEEF1F6), danger = Color(0xFFC0264B), toastAccent = Color(0xFF9DB8FF),
)

val DarkColors = CColors(
    dark = true,
    paper = Color(0xFF0D1117), card = Color(0xFF161B22), ink = Color(0xFFE6EAF0), muted = Color(0xFF98A2B3), line = Color(0xFF2A313C),
    accent = Color(0xFF7BA5FF), onAccent = Color(0xFF0B1B40), tint = Color(0xFF1D2A45), onTint = Color(0xFFA9C3FF), hero = Color(0xFF1B2C57),
    pro = Color(0xFFE3B341), onPro = Color(0xFF2B1D00), desk = Color(0xFF10151C), dot = Color(0xFF232B36), dockBg = Color(0xFF232A36),
    dockFg = Color(0xFFE6EAF0), danger = Color(0xFFFF8AA5), toastAccent = Color(0xFF1D46AD),
)

@Immutable
class CFonts(private val context: Context?, val display: FontFamily, val body: FontFamily) {
    private val sized = HashMap<Int, FontFamily>()

    /**
     * The display face with its optical size set to the text size, as a browser does automatically.
     * Titles get tighter, finer letters the larger they are.
     */
    fun display(sizeSp: Float): FontFamily {
        val ctx = context ?: return display
        val key = (sizeSp * 2).toInt()
        return sized.getOrPut(key) {
            variable(ctx, "BricolageGrotesque-var.ttf", listOf(500, 600, 700, 800), opticalSize = sizeSp.coerceIn(12f, 96f))
        }
    }
}

private fun variable(context: Context, file: String, weights: List<Int>, opticalSize: Float? = null): FontFamily = FontFamily(
    weights.map { w ->
        val settings = if (opticalSize != null) FontVariation.Settings(FontVariation.weight(w), FontVariation.Setting("opsz", opticalSize))
        else FontVariation.Settings(FontVariation.weight(w))
        Font(path = "fonts/$file", assetManager = context.assets, weight = FontWeight(w), variationSettings = settings)
    },
)

val LocalColors = compositionLocalOf { LightColors }
val LocalFonts = compositionLocalOf { CFonts(null, FontFamily.SansSerif, FontFamily.SansSerif) }

object CTheme {
    val colors: CColors @Composable @ReadOnlyComposable get() = LocalColors.current
    val fonts: CFonts @Composable @ReadOnlyComposable get() = LocalFonts.current
}

@Composable
fun CollageTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val c = if (dark) DarkColors else LightColors
    val context = LocalContext.current
    val fonts = remember(context) {
        CFonts(
            context = context.applicationContext,
            display = variable(context, "BricolageGrotesque-var.ttf", listOf(500, 600, 700, 800)),
            body = variable(context, "Figtree-var.ttf", listOf(400, 500, 600, 700, 800)),
        )
    }
    val scheme = if (dark) {
        darkColorScheme(primary = c.accent, onPrimary = c.onAccent, background = c.paper, onBackground = c.ink, surface = c.card, onSurface = c.ink, error = c.danger)
    } else {
        lightColorScheme(primary = c.accent, onPrimary = c.onAccent, background = c.paper, onBackground = c.ink, surface = c.card, onSurface = c.ink, error = c.danger)
    }
    CompositionLocalProvider(LocalColors provides c, LocalFonts provides fonts) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
