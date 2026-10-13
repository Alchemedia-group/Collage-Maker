package com.alchemedia.collage.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alchemedia.collage.ui.theme.CTheme

fun Long.toColor(): Color = Color(this.toInt())
fun Color.toLong(): Long = this.toArgb().toLong() and 0xFFFFFFFFL
fun hexOf(argb: Long): String = "#%06X".format(argb and 0xFFFFFFL)

/** The rainbow swatch that opens the custom colour picker. */
@Composable
fun CustomSwatch(selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, size: androidx.compose.ui.unit.Dp = 28.dp) {
    val c = CTheme.colors
    Box(
        modifier.size(size).then(if (selected) Modifier.border(2.dp, c.accent, CircleShape).padding(3.dp) else Modifier).clip(CircleShape)
            .background(Brush.sweepGradient(listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)))
            .pressable(onClick, label = "Custom colour"),
    )
}

/** One scrolling row of colour dots, optionally starting with "none". */
@Composable
fun ColorRow(colors: List<Long>, selected: Long?, onPick: (Long?) -> Unit, onCustom: () -> Unit, modifier: Modifier = Modifier, allowNone: Boolean = false) {
    val isPreset = selected == null || colors.any { it == selected }
    ScrollRow(modifier, state = rememberScrollState(), padding = 16.dp, spacing = 11.dp) {
        Box(Modifier.padding(vertical = 6.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(11.dp), verticalAlignment = Alignment.CenterVertically) {
                if (allowNone) Swatch(null, selected == null, "None", { onPick(null) })
                colors.forEach { col -> Swatch(col.toColor(), selected == col, hexOf(col), { onPick(col) }) }
                CustomSwatch(!isPreset, onCustom)
            }
        }
    }
}

/** The background colour grid: eight to a row. */
@Composable
fun ColorGrid(colors: List<Long>, selected: Long, onPick: (Long) -> Unit, onCustom: () -> Unit, modifier: Modifier = Modifier) {
    val isPreset = colors.any { it == selected }
    val cells = colors.map { it as Long? } + listOf(null)
    Column(modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        cells.chunked(8).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { col ->
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        if (col != null) Swatch(col.toColor(), selected == col, hexOf(col), { onPick(col) }, size = 32.dp)
                        else CustomSwatch(!isPreset, onCustom, size = 32.dp)
                    }
                }
                repeat(8 - row.size) { Box(Modifier.weight(1f)) }
            }
        }
    }
}

/** A small colour picker: saturation/brightness square, hue strip and a hex box. */
@Composable
fun CustomColorSheet(initial: Long, onPick: (Long) -> Unit, onDismiss: () -> Unit) {
    val c = CTheme.colors
    val hsv = remember {
        val out = FloatArray(3)
        android.graphics.Color.colorToHSV(initial.toInt(), out)
        out
    }
    var hue by remember { mutableFloatStateOf(hsv[0]) }
    var sat by remember { mutableFloatStateOf(hsv[1]) }
    var value by remember { mutableFloatStateOf(hsv[2].coerceAtLeast(.05f)) }
    var hex by remember { mutableStateOf(hexOf(initial)) }
    val current = Color.hsv(hue, sat, value)

    fun syncHex() { hex = hexOf(current.toLong()) }

    SheetHost(onDismiss) {
        SheetTitle("Custom colour")
        Canvas(
            Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(16.dp))
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        fun at(p: Offset) { sat = (p.x / size.width).coerceIn(0f, 1f); value = 1f - (p.y / size.height).coerceIn(0f, 1f); syncHex() }
                        at(down.position)
                        while (true) {
                            val e = awaitPointerEvent()
                            val ch = e.changes.firstOrNull() ?: break
                            if (!ch.pressed) break
                            at(ch.position); ch.consume()
                        }
                    }
                },
        ) {
            drawRect(Brush.horizontalGradient(listOf(Color.White, Color.hsv(hue, 1f, 1f))))
            drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
            val p = Offset(sat * size.width, (1f - value) * size.height)
            drawCircle(Color.White, 11.dp.toPx(), p, style = androidx.compose.ui.graphics.drawscope.Stroke(3.dp.toPx()))
            drawCircle(Color.Black.copy(alpha = .35f), 12.5.dp.toPx(), p, style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx()))
        }
        Canvas(
            Modifier.fillMaxWidth().height(28.dp).clip(RoundedCornerShape(14.dp))
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        fun at(p: Offset) { hue = (p.x / size.width).coerceIn(0f, 1f) * 360f; syncHex() }
                        at(down.position)
                        while (true) {
                            val e = awaitPointerEvent()
                            val ch = e.changes.firstOrNull() ?: break
                            if (!ch.pressed) break
                            at(ch.position); ch.consume()
                        }
                    }
                },
        ) {
            drawRect(Brush.horizontalGradient((0..6).map { Color.hsv(it * 60f, 1f, 1f) }))
            val x = hue / 360f * size.width
            drawCircle(Color.White, 12.dp.toPx(), Offset(x, size.height / 2), style = androidx.compose.ui.graphics.drawscope.Stroke(3.dp.toPx()))
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(44.dp).clip(CircleShape).background(current).border(1.dp, c.line, CircleShape))
            Box(Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(12.dp)).background(c.card).border(1.dp, c.line, RoundedCornerShape(12.dp)).padding(horizontal = 12.dp), contentAlignment = Alignment.CenterStart) {
                CTextField(
                    hex,
                    { text ->
                        hex = text.uppercase()
                        val clean = text.trim().removePrefix("#")
                        if (clean.length == 6 && clean.all { it.isDigit() || it.uppercaseChar() in 'A'..'F' }) {
                            val argb = (0xFF000000L or clean.toLong(16)).toInt()
                            val out = FloatArray(3)
                            android.graphics.Color.colorToHSV(argb, out)
                            hue = out[0]; sat = out[1]; value = out[2]
                        }
                    },
                    Modifier.fillMaxWidth(), "#RRGGBB",
                    textStyle = TextStyle(fontFamily = CTheme.fonts.body, fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                    maxLength = 7, capitalization = KeyboardCapitalization.Characters,
                )
            }
        }
        CButton("Use this colour", { onPick(current.toLong()); onDismiss() })
        CButton("Cancel", onDismiss, style = BtnStyle.Ghost)
    }
}
