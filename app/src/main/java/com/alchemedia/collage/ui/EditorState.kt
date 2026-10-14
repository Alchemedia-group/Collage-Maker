package com.alchemedia.collage.ui

import androidx.annotation.StringRes
import com.alchemedia.collage.R
import com.alchemedia.collage.Strings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.alchemedia.collage.model.AspectPreset
import com.alchemedia.collage.model.CanvasItem
import com.alchemedia.collage.model.CellContent
import com.alchemedia.collage.model.CollageGeometry
import com.alchemedia.collage.model.CollageProject
import com.alchemedia.collage.model.ItemType
import com.alchemedia.collage.model.LayoutCatalog
import com.alchemedia.collage.model.PhotoRef
import com.alchemedia.collage.model.ShadowSpec
import com.alchemedia.collage.render.CollageRenderer
import com.alchemedia.collage.render.ItemRenderer
import com.alchemedia.collage.render.PhotoSource
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

enum class EditorTab(@StringRes val labelRes: Int) {
    Layout(R.string.tab_layout), Shape(R.string.tab_shape), Style(R.string.tab_style), Photo(R.string.tab_photo), Text(R.string.tab_text), Sticker(R.string.tab_stickers);

    val label: String get() = Strings.get(labelRes)
}
enum class StyleSub(@StringRes val labelRes: Int) {
    Color(R.string.sub_color), Texture(R.string.sub_texture), Spacing(R.string.sub_spacing), Filter(R.string.sub_filter), Image(R.string.sub_image), Frame(R.string.sub_frame);

    val label: String get() = Strings.get(labelRes)
}
enum class PhotoSub(@StringRes val labelRes: Int) {
    Photo(R.string.tab_photo), Rotate(R.string.sub_rotate), Light(R.string.sub_light);

    val label: String get() = Strings.get(labelRes)
}

/** One of the three light sliders for the selected photo (-100..100 on screen, -1..1 stored). */
enum class LightSlider(@StringRes val labelRes: Int) {
    Brightness(R.string.light_brightness), Contrast(R.string.light_contrast), Saturation(R.string.light_saturation);

    val label: String get() = Strings.get(labelRes)

    fun get(c: CellContent): Float = when (this) { Brightness -> c.brightness; Contrast -> c.contrast; Saturation -> c.saturation } * 100f
    fun set(c: CellContent, v: Float): CellContent {
        val x = (v / 100f).coerceIn(-1f, 1f)
        return when (this) { Brightness -> c.copy(brightness = x); Contrast -> c.copy(contrast = x); Saturation -> c.copy(saturation = x) }
    }
}
enum class TextSub(@StringRes val labelRes: Int) {
    Text(R.string.tab_text), Font(R.string.sub_font), Color(R.string.sub_color), Style(R.string.sub_style), Adjust(R.string.sub_size);

    val label: String get() = Strings.get(labelRes)
}

/** One number the user can slide for the selected item. */
enum class ItemSlider(@StringRes val labelRes: Int, val min: Float, val max: Float, val step: Float, val unit: String = "%") {
    Stroke(R.string.slider_outline, 0f, 25f, 1f), Shadow(R.string.slider_shadow, 0f, 100f, 1f), Opacity(R.string.slider_opacity, 20f, 100f, 1f), Size(R.string.sub_size, 3f, 60f, .5f),
    Spacing(R.string.slider_spacing, -10f, 60f, 1f), Curve(R.string.slider_curve, -100f, 100f, 1f, ""), Rotate(R.string.sub_rotate, -180f, 180f, 1f, "°"),
    Gradient(R.string.slider_angle, 0f, 360f, 1f, "°");

    val label: String get() = Strings.get(labelRes)

    fun get(it: CanvasItem): Float = when (this) {
        Stroke -> (it.strokeW * 100).roundToInt().toFloat()
        Shadow -> it.shadow?.let { s -> ((s.blur / .5f).coerceIn(0f, 1f) * 100).roundToInt().let { v -> if (v == 0) 40 else v }.toFloat() } ?: 0f
        Opacity -> (it.opacity * 100).roundToInt().toFloat()
        Size -> (it.size * 1000).roundToInt() / 10f
        Spacing -> (it.spacing * 100).roundToInt().toFloat()
        Curve -> it.curve.roundToInt().toFloat()
        Rotate -> it.rot.roundToInt().toFloat()
        Gradient -> it.gradientAngle.roundToInt().toFloat()
    }

    fun set(it: CanvasItem, v: Float): CanvasItem = when (this) {
        Stroke -> it.copy(strokeW = v / 100f)
        Shadow -> it.copy(shadow = if (v > 0f) ShadowSpec(it.shadow?.color ?: 0x8C000000, v / 100f * .5f, 0f, v / 100f * .12f) else null)
        Opacity -> it.copy(opacity = v / 100f)
        Size -> it.copy(size = v / 100f)
        Spacing -> it.copy(spacing = v / 100f)
        Curve -> it.copy(curve = v)
        Rotate -> it.copy(rot = v)
        Gradient -> it.copy(gradientAngle = v)
    }

    fun format(v: Float): String {
        val rounded = (v * 10).roundToInt() / 10f
        val text = if (rounded == rounded.toInt().toFloat()) rounded.toInt().toString() else rounded.toString()
        return text + unit
    }
}

enum class ColorTarget(@StringRes val labelRes: Int) {
    Fill(R.string.tab_text), Gradient(R.string.target_gradient), Stroke(R.string.slider_outline), Highlight(R.string.target_highlight);

    val label: String get() = Strings.get(labelRes)
}

const val MAX_ZOOM = 20f
private const val MIN_ITEM = .02f
private const val MAX_ITEM = .7f

/**
 * Everything about the collage being edited: the project itself, what is selected, which panel is open, and the
 * gestures on the canvas. Changes are saved a moment after the last edit.
 */
class EditorState(private val vm: AppViewModel, initial: CollageProject) {
    var project by mutableStateOf(initial.normalized())
        private set
    var selectedCell by mutableIntStateOf(-1)
    var selectedItemId by mutableStateOf<String?>(null)
    var swapping by mutableStateOf(false)
    var tab by mutableStateOf(EditorTab.Layout)
    var photoSub by mutableStateOf(PhotoSub.Photo)
    var lightSlider by mutableStateOf(LightSlider.Brightness)
    var panelOpen by mutableStateOf(true)
    var styleSub by mutableStateOf(StyleSub.Color)
    var textSub by mutableStateOf(TextSub.Text)
    var colorTarget by mutableStateOf(ColorTarget.Fill)
    var styleSlider by mutableStateOf(ItemSlider.Stroke)
    var adjustSlider by mutableStateOf(ItemSlider.Size)
    var stickerSlider by mutableStateOf(ItemSlider.Size)
    var fontCategory by mutableStateOf("pop")
    var emojiCategory by mutableStateOf("smile")
    var typing by mutableStateOf(false)
    /** Bumped when the text box should take the keyboard (double tap on text, or after adding some). */
    var focusRequest by mutableIntStateOf(0)

    /** Canvas size in pixels, set by the canvas composable. */
    var canvasW by mutableFloatStateOf(1f)
    var canvasH by mutableFloatStateOf(1f)
    var density by mutableFloatStateOf(1f)
    var photoSource: PhotoSource? = null

    private var saveJob: Job? = null
    private var dirty = false

    val selectedItem: CanvasItem? get() = selectedItemId?.let { id -> project.items.firstOrNull { it.id == id } }
    val hasPhotos: Boolean get() = project.cells.any { it.photoId != null }

    init {
        // A project whose layout was edited or removed is brought back in line when it opens.
        if (project != initial) markDirty()
    }

    // ------------------------------------------------------------ saving

    private fun markDirty() {
        dirty = true
        saveJob?.cancel()
        saveJob = vm.viewModelScope.launch {
            delay(450)
            flushNow()
        }
    }

    private suspend fun flushNow() {
        if (!dirty) return
        dirty = false
        vm.store.save(project)
    }

    /** Writes any pending change right away (leaving the editor, closing the app). */
    fun flush() {
        saveJob?.cancel()
        if (!dirty) return
        dirty = false
        val snapshot = project
        vm.viewModelScope.launch { vm.store.save(snapshot) }
    }

    fun update(fn: (CollageProject) -> CollageProject) {
        project = fn(project)
        markDirty()
    }

    fun rename(name: String) = update { it.copy(name = name.take(40)) }

    // ------------------------------------------------------------ navigation helpers

    /** Returns true when back was used up closing something inside the editor. */
    fun handleBack(): Boolean {
        if (swapping) { swapping = false; return true }
        return false
    }

    fun layoutsChanged() {
        val fixed = project.normalized()
        if (fixed != project) { project = fixed; selectedCell = -1; markDirty() }
    }

    fun selectTab(t: EditorTab) {
        panelOpen = !(tab == t && panelOpen)
        tab = t
    }

    // ------------------------------------------------------------ layout, shape, style

    fun pickLayout(id: String) {
        val layout = LayoutCatalog.byId(id)
        val before = project.aspect
        val after = layout.ar?.let { AspectPreset.fromLabel(it) } ?: before
        update { it.withLayout(layout).copy(aspect = after) }
        selectedCell = -1
        swapping = false
        if (after != before) vm.showToast(Strings.get(R.string.shape_set_to, after.label))
    }

    fun setAspect(a: AspectPreset) = update { it.copy(aspect = a) }
    fun clearBackgroundImage() = update { it.copy(backgroundImage = null) }

    /** Imports a picture from the device and uses it as the background. */
    fun setBackgroundFromDevice(uri: android.net.Uri) {
        vm.viewModelScope.launch {
            val ref = vm.store.importPhotos(project.id, listOf(uri)).firstOrNull()
            if (ref == null) vm.showToast(Strings.get(R.string.err_read_picture)) else update { it.copy(backgroundImage = ref) }
        }
    }

    fun setBackground(argb: Long) = update { it.copy(background = argb) }
    fun setGap(v: Float) = update { it.copy(gapPercent = v) }
    fun setMargin(v: Float) = update { it.copy(marginPercent = v) }
    fun setCorner(v: Float) = update { it.copy(cornerPercent = v) }
    fun setFilter(id: String) = update { it.copy(filter = id) }
    fun setFilterAmount(v: Float) = update { it.copy(filterAmount = v) }
    fun setTextureAmount(v: Float) = update { it.copy(textureAmount = v) }

    /** Picking a frame also sets its usual colour and width; both can be changed afterwards. */
    fun setFrame(id: String) {
        val def = com.alchemedia.collage.render.Frames.byId(id)
        update { it.copy(frame = def.id, frameColor = def.color, frameAmount = def.amount) }
    }
    fun setFrameColor(argb: Long) = update { it.copy(frameColor = argb) }
    fun setFrameAmount(v: Float) = update { it.copy(frameAmount = v.coerceIn(0f, com.alchemedia.collage.render.Frames.MAX_AMOUNT)) }

    fun setTexture(id: String, base: Long?) = update {
        val bg = if (base != null && (it.background and 0xFFFFFFL) == 0xFFFFFFL) base else it.background
        it.copy(texture = id, background = bg, textureAmount = if (it.textureAmount > 0f) it.textureAmount else 1f)
    }

    // ------------------------------------------------------------ cells and photos

    private fun cellRects() = CollageGeometry.cellRects(project.layout.cells, canvasW, canvasH, project.gapPercent, project.marginPercent)

    fun cellAt(x: Float, y: Float): Int {
        val rects = cellRects()
        for (i in rects.indices) {
            val r = rects[i]
            if (x >= r.x && x <= r.right && y >= r.y && y <= r.bottom) return i
        }
        return -1
    }

    fun selectCell(i: Int) {
        selectedCell = i
        selectedItemId = null
        if (i >= 0) { tab = EditorTab.Photo; panelOpen = true }
    }

    fun updateCell(index: Int, fn: (CellContent) -> CellContent) = update { p ->
        p.copy(cells = p.cells.mapIndexed { i, c -> if (i == index) fn(c) else c })
    }

    fun resetCell() = updateCell(selectedCell) { it.resetPosition() }
    fun resetLook() = updateCell(selectedCell) { it.resetLook() }
    fun setLook(slider: LightSlider, v: Float) = updateCell(selectedCell) { slider.set(it, v) }
    fun setAngle(v: Float) = updateCell(selectedCell) { it.copy(angle = v.coerceIn(-45f, 45f)) }
    fun rotateCell() = updateCell(selectedCell) { it.copy(turns = (it.turns + 1) % 4) }

    /** Mirrors the photo as it looks on screen: after a quarter turn the photo's own axes are swapped. */
    fun mirrorCell(vertical: Boolean) = updateCell(selectedCell) { c ->
        if (vertical xor (c.turns % 2 != 0)) c.copy(flipV = !c.flipV) else c.copy(flipH = !c.flipH)
    }
    fun emptyCell() = updateCell(selectedCell) { CellContent() }
    fun setZoom(v: Float) = updateCell(selectedCell) { it.copy(zoom = v.coerceIn(1f, MAX_ZOOM)) }

    fun useExistingPhoto(ref: PhotoRef) {
        val i = selectedCell
        if (i < 0) return
        update { p ->
            val photos = if (p.photos.any { it.id == ref.id }) p.photos else p.photos + ref
            p.copy(photos = photos, cells = p.cells.mapIndexed { n, c -> if (n == i) CellContent(photoId = ref.id) else c })
        }
    }

    /** Photos picked from the device join the collage; the first fills the selected cell when there is one. */
    fun addPhotos(refs: List<PhotoRef>) {
        if (refs.isEmpty()) return
        update { p ->
            var cells = p.cells
            val i = selectedCell
            val first = refs.first()
            if (i in cells.indices) cells = cells.mapIndexed { n, c -> if (n == i) CellContent(photoId = first.id) else c }
            p.copy(photos = p.photos + refs, cells = cells)
        }
    }

    /** Imports photos chosen with the system picker and adds them to this collage. */
    fun addFromDevice(uris: List<android.net.Uri>) {
        vm.viewModelScope.launch {
            val refs = vm.store.importPhotos(project.id, uris)
            if (refs.isEmpty()) vm.showToast(Strings.get(R.string.err_read_photos)) else addPhotos(refs)
        }
    }

    fun swapCells(a: Int, b: Int) = update { p ->
        val cells = p.cells.toMutableList()
        val t = cells[a]; cells[a] = cells[b]; cells[b] = t
        p.copy(cells = cells)
    }

    // ------------------------------------------------------------ items

    fun addItem(item: CanvasItem, select: Boolean = true) {
        val n = project.items.size
        val placed = item.copy(x = (item.x + (n % 4) * .02f).coerceIn(.1f, .9f), y = (item.y + (n % 4) * .02f).coerceIn(.1f, .9f))
        update { it.copy(items = it.items + placed) }
        if (select) selectItem(placed.id)
    }

    fun addTemplate(items: List<CanvasItem>) {
        items.forEach { addItem(it, select = false) }
        selectItem(project.items.last().id)
    }

    fun selectItem(id: String?) {
        selectedItemId = id
        selectedCell = -1
        swapping = false
        val it = selectedItem ?: return
        tab = if (it.isText) EditorTab.Text else EditorTab.Sticker
        panelOpen = true
        if (it.isText && textSub !in TextSub.entries) textSub = TextSub.Text
    }

    fun itemOp(fn: (CanvasItem) -> CanvasItem) {
        val id = selectedItemId ?: return
        update { p -> p.copy(items = p.items.map { if (it.id == id) fn(it) else it }) }
    }

    private fun itemById(id: String, fn: (CanvasItem) -> CanvasItem) = update { p -> p.copy(items = p.items.map { if (it.id == id) fn(it) else it }) }

    fun duplicateItem() {
        val it = selectedItem ?: return
        val copy = it.copy(id = CanvasItem.newId(if (it.isText) "t" else "s"), x = (it.x + .05f).coerceIn(0f, 1f), y = (it.y + .05f).coerceIn(0f, 1f))
        update { p -> p.copy(items = p.items + copy) }
        selectItem(copy.id)
    }

    fun deleteItem() {
        val id = selectedItemId ?: return
        update { p -> p.copy(items = p.items.filterNot { it.id == id }) }
        selectedItemId = null
    }

    fun reorderItem(front: Boolean) {
        val it = selectedItem ?: return
        update { p ->
            val rest = p.items.filterNot { x -> x.id == it.id }
            p.copy(items = if (front) rest + it else listOf(it) + rest)
        }
        vm.showToast(Strings.get(if (front) R.string.brought_front else R.string.sent_back))
    }

    fun doneEditing() {
        selectedItemId = null
        panelOpen = false
        typing = false
    }

    fun startEditingText(id: String) {
        selectItem(id)
        textSub = TextSub.Text
        focusRequest++
    }

    private var lastDownAt = 0L
    private var lastDownItem: String? = null

    // ------------------------------------------------------------ canvas gestures (coordinates are canvas pixels)

    private class CellDrag(val cell: Int, val panX: Float, val panY: Float, val x: Float, val y: Float)
    private class ItemDrag(val id: String, val scale: Boolean, val x0: Float, val y0: Float, val ix: Float, val iy: Float, val d0: Float, val a0: Float, val size: Float, val rot: Float)
    private class CellPinch(val cell: Int, val dist: Float, val zoom: Float)
    private class ItemPinch(val id: String, val dist: Float, val ang: Float, val size: Float, val rot: Float)

    private var cellDrag: CellDrag? = null
    private var itemDrag: ItemDrag? = null
    private var cellPinch: CellPinch? = null
    private var itemPinch: ItemPinch? = null

    /** A first finger touched the canvas. */
    fun onDown(x: Float, y: Float) {
        val p = project
        val sel = selectedItem
        if (sel != null) {
            when (ItemRenderer.handleAt(sel, x, y, canvasW, canvasH, density)) {
                ItemRenderer.Handle.Delete -> { deleteItem(); return }
                ItemRenderer.Handle.Scale -> {
                    val f = ItemRenderer.frame(sel, canvasW, canvasH)
                    itemDrag = ItemDrag(sel.id, true, 0f, 0f, 0f, 0f, hypot(x - f.cx, y - f.cy), atan2(y - f.cy, x - f.cx), sel.size, sel.rot)
                    return
                }
                null -> {}
            }
        }
        ItemRenderer.hit(p.items, x, y, canvasW, canvasH)?.let { hit ->
            val now = System.currentTimeMillis()
            val doubleTap = hit.isText && lastDownItem == hit.id && now - lastDownAt < 320
            lastDownAt = now
            lastDownItem = hit.id
            if (doubleTap) { startEditingText(hit.id); return }
            if (selectedItemId != hit.id) selectItem(hit.id)
            itemDrag = ItemDrag(hit.id, false, x, y, hit.x, hit.y, 0f, 0f, hit.size, hit.rot)
            return
        }
        val i = cellAt(x, y)
        if (i < 0) { swapping = false; selectedItemId = null; selectCell(-1); return }
        if (swapping && selectedCell >= 0 && selectedCell != i) {
            swapCells(selectedCell, i)
            swapping = false
            selectCell(i)
            return
        }
        swapping = false
        selectCell(i)
        val cell = project.cells[i]
        if (cell.photoId != null) cellDrag = CellDrag(i, cell.panX, cell.panY, x, y)
    }

    /** A second finger arrived: scale and rotate the selected item, or zoom the dragged photo. */
    fun onPinchStart(ax: Float, ay: Float, bx: Float, by: Float) {
        val dist = hypot(ax - bx, ay - by).coerceAtLeast(1f)
        val sel = selectedItem
        if (sel != null) {
            itemPinch = ItemPinch(sel.id, dist, atan2(by - ay, bx - ax), sel.size, sel.rot)
            itemDrag = null; cellDrag = null
            return
        }
        val drag = cellDrag
        if (drag != null) {
            cellPinch = CellPinch(drag.cell, dist, project.cells[drag.cell].zoom)
            cellDrag = null
        }
    }

    fun onPinch(ax: Float, ay: Float, bx: Float, by: Float) {
        val dist = hypot(ax - bx, ay - by)
        itemPinch?.let { pinch ->
            val ratio = dist / pinch.dist
            val da = ((atan2(by - ay, bx - ax) - pinch.ang) * 180 / Math.PI).toFloat()
            itemById(pinch.id) { it.copy(size = (pinch.size * ratio).coerceIn(MIN_ITEM, MAX_ITEM), rot = ((pinch.rot + da) * 10).roundToInt() / 10f) }
            return
        }
        cellPinch?.let { pinch ->
            updateCell(pinch.cell) { it.copy(zoom = (pinch.zoom * dist / pinch.dist).coerceIn(1f, MAX_ZOOM)) }
        }
    }

    fun onMove(x: Float, y: Float) {
        itemDrag?.let { d ->
            if (!d.scale) {
                itemById(d.id) { it.copy(x = (d.ix + (x - d.x0) / canvasW).coerceIn(0f, 1f), y = (d.iy + (y - d.y0) / canvasH).coerceIn(0f, 1f)) }
            } else {
                val it = selectedItem ?: return
                val f = ItemRenderer.frame(it, canvasW, canvasH)
                val dist = hypot(x - f.cx, y - f.cy)
                val a = atan2(y - f.cy, x - f.cx)
                var rot = d.rot + ((a - d.a0) * 180 / Math.PI).toFloat()
                val snap = (rot / 90f).roundToInt() * 90f
                if (abs(rot - snap) < 4f) rot = snap
                itemById(d.id) { o -> o.copy(size = (d.size * dist / d.d0.coerceAtLeast(1f)).coerceIn(MIN_ITEM, MAX_ITEM), rot = (rot * 10).roundToInt() / 10f) }
            }
            return
        }
        val drag = cellDrag ?: return
        val p = project
        val cell = p.cells.getOrNull(drag.cell) ?: return
        val photoId = cell.photoId ?: return
        val bmp = photoSource?.bitmap(photoId) ?: return
        val r = cellRects().getOrNull(drag.cell) ?: return
        val pr = CollageRenderer.photoRect(r, p)
        val pl = CollageRenderer.photoPlacement(pr.left, pr.top, pr.width(), pr.height(), cell, bmp.width, bmp.height)
        // The finger moves in screen space; pan runs along the photo's own (turned) axes.
        val a = Math.toRadians(cell.rotation.toDouble())
        val dx = x - drag.x
        val dy = y - drag.y
        val lx = (dx * cos(a) + dy * sin(a)).toFloat()
        val ly = (-dx * sin(a) + dy * cos(a)).toFloat()
        updateCell(drag.cell) {
            it.copy(
                panX = if (pl.slackX > .5f) (drag.panX + lx / pl.slackX).coerceIn(-1f, 1f) else it.panX,
                panY = if (pl.slackY > .5f) (drag.panY + ly / pl.slackY).coerceIn(-1f, 1f) else it.panY,
            )
        }
    }

    fun onPinchEnd() { cellPinch = null; itemPinch = null }

    fun onUp() { cellDrag = null; itemDrag = null; cellPinch = null; itemPinch = null }

    /** Scroll-wheel / keyboard style zoom: resizes the item under [x], [y] or zooms the cell there. */
    fun zoomAt(x: Float, y: Float, factor: Float) {
        ItemRenderer.hit(project.items, x, y, canvasW, canvasH)?.let { hit ->
            if (selectedItemId != hit.id) selectItem(hit.id)
            itemById(hit.id) { it.copy(size = (it.size * factor).coerceIn(MIN_ITEM, MAX_ITEM)) }
            return
        }
        val i = cellAt(x, y)
        if (i < 0 || project.cells.getOrNull(i)?.photoId == null) return
        updateCell(i) { it.copy(zoom = (it.zoom * factor).coerceIn(1f, MAX_ZOOM)) }
        if (selectedCell != i) selectCell(i)
    }

}
