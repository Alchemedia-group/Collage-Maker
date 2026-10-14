package com.alchemedia.collage.ui.designer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alchemedia.collage.model.AspectPreset
import com.alchemedia.collage.model.CellContent
import com.alchemedia.collage.model.CollageProject
import com.alchemedia.collage.model.LayoutCatalog
import com.alchemedia.collage.model.Samples
import com.alchemedia.collage.render.CollageRenderer
import com.alchemedia.collage.render.Shapes
import com.alchemedia.collage.ui.AppViewModel
import com.alchemedia.collage.ui.BrowseTarget
import com.alchemedia.collage.ui.components.AppBar
import com.alchemedia.collage.ui.components.BtnStyle
import com.alchemedia.collage.ui.components.CButton
import com.alchemedia.collage.ui.components.CIcon
import com.alchemedia.collage.ui.components.CText
import com.alchemedia.collage.ui.components.CTextField
import com.alchemedia.collage.ui.components.Icon
import com.alchemedia.collage.ui.components.IconBtn
import com.alchemedia.collage.ui.components.Pill
import com.alchemedia.collage.ui.components.ScrollRow
import com.alchemedia.collage.ui.components.SliderOne
import com.alchemedia.collage.ui.components.SquareIconBtn
import com.alchemedia.collage.ui.components.TabChip
import com.alchemedia.collage.ui.components.lerpColor
import com.alchemedia.collage.ui.components.pressable
import com.alchemedia.collage.ui.components.rememberPhotos
import com.alchemedia.collage.ui.theme.CTheme
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun DesignerScreen(vm: AppViewModel, state: DesignerState, target: BrowseTarget) {
    val c = CTheme.colors
    val focus = LocalFocusManager.current

    // A layout chosen in "Start from…" arrives through the view model.
    LaunchedEffect(vm.designerBase) {
        val id = vm.designerBase ?: return@LaunchedEffect
        vm.designerBase = null
        val l = LayoutCatalog.byId(id)
        state.load(l)
        vm.showToast("Started from “${l.name}”. Change anything you like.")
    }
    LaunchedEffect(state.armDelete) { if (state.armDelete) { delay(3000); state.armDelete = false } }

    fun save() {
        focus.clearFocus()
        val layout = state.toLayout(vm.store.myLayouts.value.size)
        vm.saveLayout(layout) {
            vm.category = "mine"
            vm.closeOverlay()
            when (target) {
                BrowseTarget.Editor -> vm.editor?.pickLayout(layout.id)
                BrowseTarget.New -> vm.pickLayoutForNew(layout.id)
                BrowseTarget.Home -> vm.startNew(layout.id, samples = true)
                BrowseTarget.Designer -> {}
            }
            vm.showToast("Saved “${layout.name}” to My layouts.")
        }
    }

    Column(Modifier.fillMaxSize().background(c.paper).statusBarsPadding().imePadding()) {
        AppBar(onBack = { vm.closeOverlay() }, backLabel = "Close designer") {
            Box(Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).padding(horizontal = 4.dp, vertical = 6.dp)) {
                CTextField(
                    state.name, { state.name = it }, Modifier.fillMaxWidth(), "Name your layout",
                    textStyle = TextStyle(fontFamily = CTheme.fonts.display(22f), fontSize = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = (-.4).sp),
                    placeholderColor = c.ink.copy(alpha = .9f), maxLength = 24, onDone = { focus.clearFocus() },
                )
            }
            if (state.editId != null) {
                IconBtn(Icon.Trash, "Delete this layout", {
                    if (!state.armDelete) { state.armDelete = true; vm.showToast("Tap the bin again to delete this layout.") }
                    else vm.deleteLayout(state.editId) { vm.closeOverlay(); vm.showToast("Layout deleted.") }
                }, tint = if (state.armDelete) c.danger else c.ink)
            }
            Pill("Save layout", ::save, icon = Icon.Check)
        }
        BoxWithConstraints(
            Modifier.weight(1f).padding(horizontal = 10.dp).fillMaxWidth().clip(RoundedCornerShape(26.dp))
                .drawBehind {
                    drawRect(c.desk)
                    val step = 16.dp.toPx()
                    var y = step / 2
                    while (y < size.height) { var x = step / 2; while (x < size.width) { drawCircle(c.dot, 1.3.dp.toPx(), Offset(x, y)); x += step }; y += step }
                }.padding(horizontal = 14.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            val ratio = state.aspect.ratio
            var w = maxWidth
            var h = w / ratio
            if (h > maxHeight) { h = maxHeight; w = h * ratio }
            DesignerCanvas(state, Modifier.size(w.coerceAtLeast(60.dp), h.coerceAtLeast(60.dp)))
        }
        Column(
            Modifier.padding(start = 10.dp, end = 10.dp, top = 8.dp).navigationBarsPadding().fillMaxWidth().heightIn(max = 300.dp)
                .shadow(8.dp, RoundedCornerShape(22.dp), spotColor = Color(0x24101828)).clip(RoundedCornerShape(22.dp)).background(c.card)
                .verticalScroll(rememberScrollState()).padding(top = 10.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(Modifier.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ScrollRow(Modifier.weight(1f), padding = 0.dp, spacing = 6.dp) {
                    DesignerTab.entries.forEach { t -> TabChip(t.label, state.tab == t, { state.tab = t }) }
                }
                CText("${state.cells.size} cell${if (state.cells.size == 1) "" else "s"}", size = 12.sp, color = c.muted)
            }
            when (state.tab) {
                DesignerTab.Cells -> CellsTab(vm, state)
                DesignerTab.Shape -> ShapeTab(vm, state)
                DesignerTab.Canvas -> CanvasTab(vm, state, target)
            }
        }
    }
}

@Composable
private fun DesignerCanvas(state: DesignerState, modifier: Modifier) {
    val density = LocalDensity.current
    val accent = CTheme.colors.accent.toArgb()
    val base = remember(state.aspect) {
        CollageProject(
            id = "designer", createdAt = 0, updatedAt = 0, layoutId = LayoutCatalog.DRAFT_ID, aspect = state.aspect, background = 0xFFFFFFFF,
            gapPercent = 0f, marginPercent = 0f, cornerPercent = 0f, photos = Samples.all,
        )
    }
    val photos = rememberPhotos(base, 640)
    SideEffect { state.density = density.density }
    Canvas(
        modifier.shadow(10.dp, spotColor = Color(0x4D101828)).background(Color.White)
            .semantics { this.contentDescription = "Layout canvas. Tap a cell, drag it to move, drag the dots to resize." }
            .onSizeChanged { state.canvasW = it.width.toFloat(); state.canvasH = it.height.toFloat() }
            .pointerInput(state) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    state.onDown(down.position.x, down.position.y)
                    while (true) {
                        val e = awaitPointerEvent()
                        val ch = e.changes.firstOrNull() ?: break
                        if (!ch.pressed) break
                        state.onMove(ch.position.x, ch.position.y)
                        e.changes.forEach { it.consume() }
                    }
                    state.onUp()
                }
            },
    ) {
        val cells = state.cells
        val d = density.density
        state.publishDraft()
        val project = base.copy(cells = List(cells.size) { CellContent(photoId = Samples.all[(it * 5 + 2) % Samples.all.size].id) })
        drawIntoCanvas { canvas ->
            val nc = canvas.nativeCanvas
            // The picture is cropped at the canvas edge; outlines and handles are drawn past it so you can see what overhangs.
            nc.save()
            nc.clipRect(0f, 0f, size.width, size.height)
            CollageRenderer.draw(nc, project, size.width, size.height, photos)
            nc.restore()
            val rects = com.alchemedia.collage.model.CollageGeometry.cellRects(cells, size.width, size.height, 0f, 0f)
            val white = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { style = android.graphics.Paint.Style.STROKE; strokeWidth = 3.5f * d; color = 0xF2FFFFFF.toInt() }
            val dark = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { style = android.graphics.Paint.Style.STROKE; strokeWidth = 1.2f * d; color = 0xCC101828.toInt() }
            rects.forEach { r -> val p = CollageRenderer.cellOutline(r, project); nc.drawPath(p, white); nc.drawPath(p, dark) }
            val dash = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                style = android.graphics.Paint.Style.STROKE; strokeWidth = 1.5f * d; color = accent
                pathEffect = android.graphics.DashPathEffect(floatArrayOf(6 * d, 5 * d), 0f)
            }
            state.guides.forEach { g -> if (g.axis == 'x') nc.drawLine(g.v * size.width, 0f, g.v * size.width, size.height, dash) else nc.drawLine(0f, g.v * size.height, size.width, g.v * size.height, dash) }
            rects.getOrNull(state.selected)?.let { sel ->
                val p = CollageRenderer.cellOutline(sel, project)
                nc.drawPath(p, android.graphics.Paint(white).apply { strokeWidth = 6 * d; color = android.graphics.Color.WHITE })
                nc.drawPath(p, android.graphics.Paint(white).apply { strokeWidth = 3 * d; color = accent })
                nc.drawRect(sel.x, sel.y, sel.right, sel.bottom, android.graphics.Paint(dash).apply { pathEffect = android.graphics.DashPathEffect(floatArrayOf(5 * d, 4 * d), 0f); strokeWidth = 1.5f * d })
                val fill = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.WHITE }
                val ring = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { style = android.graphics.Paint.Style.STROKE; strokeWidth = 2.5f * d; color = accent }
                state.selectedCell?.let { cell ->
                    state.handlePoints(cell).values.forEach { (ux, uy) ->
                        nc.drawCircle(ux * size.width, uy * size.height, 6.5f * d, fill)
                        nc.drawCircle(ux * size.width, uy * size.height, 6.5f * d, ring)
                    }
                }
            }
        }
    }
}

@Composable
private fun CellsTab(vm: AppViewModel, state: DesignerState) {
    val c = CTheme.colors
    fun run(msg: String?) { if (msg != null) vm.showToast(msg) }
    if (state.selectedCell == null) CText("Tap a cell to select it.", Modifier.padding(horizontal = 18.dp), size = 13.sp, color = c.muted)
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        SquareIconBtn(Icon.SplitV, "Split into left and right", { run(state.split(true)) })
        SquareIconBtn(Icon.SplitH, "Split into top and bottom", { run(state.split(false)) })
        SquareIconBtn(Icon.Duplicate, "Duplicate cell", { run(state.duplicate()) })
        SquareIconBtn(Icon.Trash, "Delete cell", { run(state.delete()) }, danger = true)
        SquareIconBtn(Icon.ToFront, "Bring to front", { run(state.toFront()) })
        SquareIconBtn(Icon.ToBack, "Send to back", { run(state.toBack()) })
        SquareIconBtn(Icon.Undo, "Undo", state::undo, enabled = state.undoCount > 0)
        SquareIconBtn(Icon.Redo, "Redo", state::redo, enabled = state.redoCount > 0)
    }
}

@Composable
private fun ShapeTab(vm: AppViewModel, state: DesignerState) {
    val c = CTheme.colors
    val cell = state.selectedCell
    val key = state.selectedKey
    ScrollRow(padding = 14.dp, spacing = 8.dp, state = rememberScrollState(), tiles = true) {
        DesignerShapes.forEach { (k, label) ->
            val on = key == k
            Box(
                Modifier.size(46.dp).clip(RoundedCornerShape(13.dp)).background(if (on) c.accent else lerpColor(c.card, c.ink, .08f)).padding(9.dp)
                    .pressable({ state.setShape(k)?.let(vm::showToast) }, role = Role.RadioButton, label = label).semantics { selected = on },
            ) {
                ShapeGlyph(k, if (on) c.onAccent else lerpColor(c.card, c.ink, .6f), Modifier.fillMaxSize())
            }
        }
    }
    if (cell != null && cell.isLetter) {
        Row(Modifier.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CText("LETTER", size = 11.sp, weight = 700, color = c.muted, letterSpacing = 1.sp)
            var text by remember(state.selected) { mutableStateOf(cell.text.orEmpty()) }
            Box(Modifier.width(80.dp).clip(RoundedCornerShape(12.dp)).background(c.paper).border(1.dp, c.line, RoundedCornerShape(12.dp)).padding(horizontal = 10.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
                CTextField(
                    text, { text = it.uppercase().filter { ch -> !ch.isWhitespace() }.take(2); state.beginEdit(); state.setLetter(text) }, Modifier.fillMaxWidth(), "A",
                    textStyle = TextStyle(fontFamily = CTheme.fonts.body, fontSize = 16.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                    maxLength = 2, capitalization = KeyboardCapitalization.Characters,
                )
            }
        }
    } else if (cell != null && cell.shape != null) {
        Column(Modifier.padding(horizontal = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ScrollRow(Modifier.weight(1f), padding = 0.dp, spacing = 6.dp) { ShapeAdjust.entries.forEach { a -> TabChip(a.label, state.adjust == a, { state.adjust = a }) } }
                SquareIconBtn(Icon.Stretch, "Stretch the shape to fill the cell", state::toggleStretch, pressed = cell.fill)
            }
            if (state.adjust == ShapeAdjust.Size) {
                SliderOne((cell.scale * 100f), 30f..300f, { "${it.roundToInt()}%" }, state::setSize, step = 1f, label = "Shape size", onStart = state::beginEdit)
            } else {
                SliderOne(cell.rotation, -180f..180f, { "${it.roundToInt()}°" }, state::setRotation, step = 1f, label = "Shape rotation", onStart = state::beginEdit)
            }
        }
    } else {
        CText(
            if (cell != null) "Rectangles are plain photo cells. Pick a shape or a letter to cut the photo into it." else "Pick a cell, then choose a shape for it.",
            Modifier.padding(horizontal = 18.dp), size = 13.sp, color = c.muted, lineHeight = 19.sp,
        )
    }
}

@Composable
private fun CanvasTab(vm: AppViewModel, state: DesignerState, target: BrowseTarget) {
    val c = CTheme.colors
    ScrollRow(padding = 14.dp, spacing = 10.dp, tiles = true) {
        AspectPreset.entries.forEach { a ->
            val on = state.aspect == a
            val k = 30f / maxOf(a.width, a.height)
            Column(
                Modifier.width(64.dp).pressable({ state.aspect = a }, role = Role.RadioButton, label = "Aspect ${a.label}").semantics { selected = on },
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                    Box(
                        Modifier.size((a.width * k).dp, (a.height * k).dp).clip(RoundedCornerShape(5.dp)).background(if (on) c.accent.copy(alpha = .18f) else Color.Transparent)
                            .border(2.5.dp, if (on) c.accent else c.muted, RoundedCornerShape(5.dp)),
                    )
                }
                CText(a.label, size = 12.sp, weight = 700, color = if (on) c.accent else c.muted)
            }
        }
    }
    Row(Modifier.padding(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        TabChip("Snap", state.snap, { state.snap = !state.snap })
        TabChip("Link edges", state.link, { state.link = !state.link })
        CButton("Start from…", { vm.openBrowse(BrowseTarget.Designer) }, small = true, style = BtnStyle.Tonal)
        CButton("Clear", state::clear, small = true, style = BtnStyle.Ghost)
    }
}

@Composable
private fun ShapeGlyph(key: String, color: Color, modifier: Modifier) {
    Canvas(modifier) {
        drawIntoCanvas { canvas ->
            val nc = canvas.nativeCanvas
            val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { this.color = color.toArgb() }
            nc.save()
            nc.scale(size.width / 100f, size.height / 100f)
            when (key) {
                "rect" -> nc.drawRoundRect(12f, 12f, 88f, 88f, 10f, 10f, paint)
                "letter" -> {
                    paint.typeface = com.alchemedia.collage.render.Fonts.typeface("bric", true)
                    paint.textSize = 70f; paint.textAlign = android.graphics.Paint.Align.CENTER
                    nc.drawText("A", 50f, 50f + 70f * .36f, paint)
                }
                else -> nc.drawPath(Shapes.base(key), paint)
            }
            nc.restore()
        }
    }
}
