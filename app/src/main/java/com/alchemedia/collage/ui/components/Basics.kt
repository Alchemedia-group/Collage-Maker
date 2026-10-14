package com.alchemedia.collage.ui.components

import com.alchemedia.collage.Strings
import com.alchemedia.collage.R
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale as drawScale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alchemedia.collage.ui.theme.CTheme
import kotlin.math.roundToInt

// ------------------------------------------------------------------ text and icons

@Composable
fun CText(
    text: String,
    modifier: Modifier = Modifier,
    size: TextUnit = 14.sp,
    weight: Int = 500,
    display: Boolean = false,
    color: Color = CTheme.colors.ink,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    align: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = size,
        fontWeight = FontWeight(weight),
        fontFamily = if (display) CTheme.fonts.display(size.value) else CTheme.fonts.body,
        letterSpacing = letterSpacing,
        maxLines = maxLines,
        overflow = if (maxLines == Int.MAX_VALUE) TextOverflow.Clip else TextOverflow.Ellipsis,
        textAlign = align,
        // CSS "normal" line height: about 1.2 times the size, with the extra space shared above and below.
        lineHeight = if (lineHeight != TextUnit.Unspecified) lineHeight else (size.value * (if (display) 1.25f else 1.2f)).sp,
        style = TextStyle(lineHeightStyle = androidx.compose.ui.text.style.LineHeightStyle(androidx.compose.ui.text.style.LineHeightStyle.Alignment.Center, androidx.compose.ui.text.style.LineHeightStyle.Trim.None)),
    )
}

private val parsedIcons = HashMap<String, Path>()

private fun iconPath(d: String): Path = parsedIcons.getOrPut(d) { PathParser().parsePathString(d).toPath() }

@Composable
fun CIcon(icon: Icon, modifier: Modifier = Modifier, size: Dp = 22.dp, tint: Color = CTheme.colors.ink, strokeWidth: Float = 2f) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension / 24f
        icon.parts.forEach { part ->
            val path = iconPath(part.path)
            drawScale(s, s, pivot = Offset.Zero) {
                if (part.filled) drawPath(path, tint.copy(alpha = tint.alpha * part.alpha), style = Fill)
                drawPath(
                    path, tint.copy(alpha = tint.alpha * part.alpha),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round),
                )
            }
        }
    }
}

// ------------------------------------------------------------------ interaction helpers

/** Buttons squash slightly while pressed, like the design. */
@Composable
fun Modifier.pressable(
    onClick: () -> Unit,
    enabled: Boolean = true,
    role: Role = Role.Button,
    label: String? = null,
    onLongClick: (() -> Unit)? = null,
): Modifier {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed && enabled) .97f else 1f, label = "press")
    return this
        .scale(scale)
        .semantics { this.role = role; if (label != null) contentDescription = label }
        .clickable(interactionSource = source, indication = null, enabled = enabled, onClick = onClick)
}

// ------------------------------------------------------------------ buttons

@Composable
fun IconBtn(icon: Icon, label: String, onClick: () -> Unit, modifier: Modifier = Modifier, tint: Color = CTheme.colors.ink, enabled: Boolean = true, size: Dp = 44.dp, iconSize: Dp = 22.dp) {
    Box(
        modifier.size(size).clip(CircleShape).alpha(if (enabled) 1f else .35f).pressable(onClick, enabled, label = label),
        contentAlignment = Alignment.Center,
    ) { CIcon(icon, size = iconSize, tint = tint) }
}

@Composable
fun Pill(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: Icon? = null, ink: Boolean = false) {
    val c = CTheme.colors
    Row(
        modifier.height(36.dp).clip(CircleShape).background(if (ink) c.ink else c.accent).pressable(onClick).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (icon != null) CIcon(icon, size = 18.dp, tint = if (ink) c.paper else c.onAccent)
        CText(text, size = 13.sp, weight = 700, color = if (ink) c.paper else c.onAccent, maxLines = 1)
    }
}

@Composable
fun ProTag(modifier: Modifier = Modifier) {
    val c = CTheme.colors
    Box(modifier.clip(CircleShape).background(c.pro).padding(horizontal = 9.dp, vertical = 4.dp)) {
        CText(Strings.get(R.string.pro_tag), size = 11.sp, weight = 800, color = c.onPro, letterSpacing = .9.sp)
    }
}

enum class BtnStyle { Primary, Tonal, Ghost, GhostDanger, Danger }

@Composable
fun CButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: BtnStyle = BtnStyle.Primary,
    enabled: Boolean = true,
    small: Boolean = false,
    icon: Icon? = null,
) {
    val c = CTheme.colors
    val (bg, fg, border) = when (style) {
        BtnStyle.Primary -> Triple(c.accent, c.onAccent, null)
        BtnStyle.Tonal -> Triple(lerpColor(c.card, c.accent, .16f), c.accent, null)
        BtnStyle.Ghost -> Triple(c.card, c.ink, c.line)
        BtnStyle.GhostDanger -> Triple(c.card, c.danger, c.line)
        BtnStyle.Danger -> Triple(c.danger, Color.White, null)
    }
    val shape = RoundedCornerShape(if (small) 12.dp else 16.dp)
    var m = modifier.then(if (small) Modifier.height(34.dp) else Modifier.fillMaxWidth().height(52.dp))
        .alpha(if (enabled) 1f else .35f).clip(shape).background(bg)
    if (border != null) m = m.border(BorderStroke(1.dp, border), shape)
    Row(
        m.pressable(onClick, enabled).padding(horizontal = if (small) 14.dp else 22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
    ) {
        if (icon != null) CIcon(icon, size = 18.dp, tint = fg)
        CText(text, size = if (small) 13.sp else 15.sp, weight = 700, color = fg, maxLines = 1)
    }
}

fun lerpColor(a: Color, b: Color, t: Float): Color = androidx.compose.ui.graphics.lerp(a, b, t)

// ------------------------------------------------------------------ chips

/** Category / sub-tab chip (dark when selected). */
@Composable
fun TabChip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = CTheme.colors
    val shape = CircleShape
    Box(
        modifier.height(30.dp).clip(shape)
            .background(if (selected) c.ink else Color.Transparent)
            .border(1.dp, if (selected) c.ink else c.line, shape)
            .pressable(onClick, role = Role.Tab)
            .semantics { this.selected = selected }
            .padding(horizontal = 13.dp),
        contentAlignment = Alignment.Center,
    ) { CText(text, size = 13.sp, weight = 600, color = if (selected) c.paper else c.muted, maxLines = 1) }
}

/** Filter chip (accent when selected), used in the layout browser. */
@Composable
fun FilterChip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = CTheme.colors
    Box(
        modifier.height(34.dp).widthIn(min = 38.dp).clip(CircleShape)
            .background(if (selected) c.accent else c.card)
            .border(1.dp, if (selected) c.accent else c.line, CircleShape)
            .pressable(onClick, role = Role.RadioButton)
            .semantics { this.selected = selected }
            .padding(horizontal = 13.dp),
        contentAlignment = Alignment.Center,
    ) { CText(text, size = 13.sp, weight = 700, color = if (selected) c.onAccent else c.muted, maxLines = 1) }
}

@Composable
fun CountBadge(text: String, modifier: Modifier = Modifier) {
    val c = CTheme.colors
    Box(modifier.clip(CircleShape).background(c.tint).padding(horizontal = 11.dp, vertical = 5.dp)) {
        CText(text, size = 13.sp, weight = 800, color = c.onTint)
    }
}

/** Square icon/text button used in tool rows. */
@Composable
fun SquareBtn(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, pressed: Boolean = false, danger: Boolean = false, enabled: Boolean = true, size: Dp = 36.dp, content: @Composable () -> Unit) {
    val c = CTheme.colors
    Box(
        modifier.size(size).alpha(if (enabled) 1f else .35f).clip(RoundedCornerShape(11.dp))
            .background(if (pressed) c.accent else lerpColor(c.card, c.ink, .07f))
            .pressable(onClick, enabled, label = label)
            .semantics { selected = pressed },
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
fun SquareIconBtn(icon: Icon, label: String, onClick: () -> Unit, modifier: Modifier = Modifier, pressed: Boolean = false, danger: Boolean = false, enabled: Boolean = true) {
    val c = CTheme.colors
    SquareBtn(label, onClick, modifier, pressed, danger, enabled) {
        CIcon(icon, size = 20.dp, tint = if (pressed) c.onAccent else if (danger) c.danger else c.ink)
    }
}

@Composable
fun DoneBtn(onClick: () -> Unit, modifier: Modifier = Modifier, text: String = Strings.get(R.string.done)) {
    val c = CTheme.colors
    Box(
        modifier.height(30.dp).clip(CircleShape).background(c.accent).pressable(onClick).padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) { CText(text, size = 13.sp, weight = 700, color = c.onAccent) }
}

@Composable
fun LinkBtn(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, color: Color = CTheme.colors.accent, vertical: Dp = 8.dp) {
    Box(modifier.pressable(onClick).padding(vertical = vertical, horizontal = 2.dp)) { CText(text, size = 13.sp, weight = 700, color = color) }
}

// ------------------------------------------------------------------ swatches

@Composable
fun Swatch(color: Color?, selected: Boolean, label: String, onClick: () -> Unit, modifier: Modifier = Modifier, size: Dp = 28.dp) {
    val c = CTheme.colors
    Box(
        modifier.size(size).then(
            if (selected) Modifier.border(2.dp, c.accent, CircleShape).padding(3.dp) else Modifier,
        ).clip(CircleShape).pressable(onClick, role = Role.RadioButton, label = label)
            .semantics { this.selected = selected },
    ) {
        Canvas(Modifier.size(size)) {
            if (color != null) drawCircle(color) else {
                drawCircle(c.card)
                drawLine(c.danger, Offset(this.size.width * .22f, this.size.height * .78f), Offset(this.size.width * .78f, this.size.height * .22f), strokeWidth = 3f, cap = StrokeCap.Round)
            }
            drawCircle(c.line, radius = this.size.minDimension / 2 - .5f, style = Stroke(1f))
        }
    }
}

// ------------------------------------------------------------------ scrolling rows with edge fades

/** Fades the edges of a scrolling row, but only where more content is hidden. */
fun Modifier.edgeFade(state: ScrollableState, fade: Dp = 28.dp): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        val f = fade.toPx()
        if (state.canScrollBackward) {
            drawRect(Brush.horizontalGradient(0f to Color.Transparent, 1f to Color.Black, startX = 0f, endX = f), size = Size(f, size.height), blendMode = BlendMode.DstIn)
        }
        if (state.canScrollForward) {
            drawRect(
                Brush.horizontalGradient(0f to Color.Black, 1f to Color.Transparent, startX = size.width - f, endX = size.width),
                topLeft = Offset(size.width - f, 0f), size = Size(f, size.height), blendMode = BlendMode.DstIn,
            )
        }
    }

@Composable
fun ScrollRow(
    modifier: Modifier = Modifier,
    state: ScrollState = rememberScrollState(),
    padding: Dp = 14.dp,
    spacing: Dp = 10.dp,
    tiles: Boolean = false,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier.fillMaxWidth().edgeFade(state).horizontalScroll(state).padding(start = padding, end = padding, top = if (tiles) 2.dp else 0.dp, bottom = if (tiles) 4.dp else 0.dp),
        horizontalArrangement = Arrangement.spacedBy(spacing), verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

// ------------------------------------------------------------------ big slider

/** A thumb-sized slider: 8dp track, 28dp thumb, 36dp touch height. */
@Composable
fun BigSlider(
    value: Float,
    onChange: (Float) -> Unit,
    range: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    step: Float = 0f,
    label: String? = null,
    onStart: (() -> Unit)? = null,
) {
    val c = CTheme.colors
    val density = LocalDensity.current
    val thumb = with(density) { 28.dp.toPx() }
    fun snap(v: Float): Float {
        val clamped = v.coerceIn(range.start, range.endInclusive)
        return if (step > 0f) (Math.round((clamped - range.start) / step) * step + range.start).coerceIn(range.start, range.endInclusive) else clamped
    }
    Box(
        modifier.height(24.dp).semantics { if (label != null) contentDescription = label }
            .pointerInput(range, step) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    onStart?.invoke()
                    fun at(x: Float) = snap(range.start + ((x - thumb / 2) / (size.width - thumb)).coerceIn(0f, 1f) * (range.endInclusive - range.start))
                    onChange(at(down.position.x))
                    down.consume()
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break
                        if (!change.pressed) break
                        if (change.positionChange() != Offset.Zero) {
                            onChange(at(change.position.x))
                            change.consume()
                        }
                    }
                }
            },
    ) {
        Canvas(Modifier.fillMaxWidth().height(24.dp)) {
            val frac = if (range.endInclusive > range.start) ((value - range.start) / (range.endInclusive - range.start)).coerceIn(0f, 1f) else 0f
            val cy = size.height / 2
            val trackH = 8.dp.toPx()
            val left = thumb / 2
            val right = size.width - thumb / 2
            val x = left + (right - left) * frac
            drawRoundRect(c.ink.copy(alpha = .18f), Offset(left - 4.dp.toPx(), cy - trackH / 2), Size(right - left + 8.dp.toPx(), trackH), androidx.compose.ui.geometry.CornerRadius(trackH / 2))
            drawRoundRect(c.accent, Offset(left - 4.dp.toPx(), cy - trackH / 2), Size(x - left + 4.dp.toPx(), trackH), androidx.compose.ui.geometry.CornerRadius(trackH / 2))
            drawCircle(Color.Black.copy(alpha = .22f), radius = thumb / 2, center = Offset(x, cy + 2.dp.toPx()))
            drawCircle(Color.White, radius = thumb / 2, center = Offset(x, cy))
            drawCircle(c.accent, radius = thumb / 2 - 1.5.dp.toPx(), center = Offset(x, cy), style = Stroke(3.dp.toPx()))
        }
    }
}

/** Label, slider and value on one row (the design's `.slider`). */
@Composable
fun SliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    format: (Float) -> String,
    onChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    step: Float = 0f,
) {
    Row(modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        CText(label, Modifier.width(62.dp), size = 14.sp, weight = 600)
        BigSlider(value, onChange, range, Modifier.weight(1f), step, label)
        CText(format(value), Modifier.width(48.dp), size = 13.sp, weight = 600, align = TextAlign.End)
    }
}

/** Slider without a label (the design's `.slider.one`). */
@Composable
fun SliderOne(value: Float, range: ClosedFloatingPointRange<Float>, format: (Float) -> String, onChange: (Float) -> Unit, modifier: Modifier = Modifier, step: Float = 0f, label: String? = null, onStart: (() -> Unit)? = null) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        BigSlider(value, onChange, range, Modifier.weight(1f), step, label, onStart)
        CText(format(value), Modifier.width(52.dp), size = 13.sp, weight = 600, align = TextAlign.End)
    }
}

fun percentText(v: Float) = "${v.roundToInt()}%"
