package com.alchemedia.collage.ui

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
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.roundToInt

enum class EditorTab(val label: String) { Layout("Layout"), Shape("Shape"), Style("Style"), Photo("Photo"), Text("Text"), Sticker("Stickers") }
enum class StyleSub(val label: String) { Color("Color"), Texture("Texture"), Spacing("Spacing"), Filter("Filter"), Image("Image") }
enum class TextSub(val label: String) { Text("Text"), Font("Font"), Color("Color"), Style("Style"), Adjust("Size") }

/** One number the user can slide for the selected item. */
enum class ItemSlider(val label: String, val min: Float, val max: Float, val step: Float, val unit: String = "%") {
    Stroke("Outline", 0f, 25f, 1f), Shadow("Shadow", 0f, 100f, 1f), Opacity("Opacity", 20f, 100f, 1f), Size("Size", 3f, 60f, .5f),
    Spacing("Spacing", -10f, 60f, 1f), Curve("Curve", -100f, 100f, 1f, ""), Rotate("Rotate", -180f, 180f, 1f, "°");

    fun get(it: CanvasItem): Float = when (this) {
        Stroke -> (it.strokeW * 100).roundToInt().toFloat()
        Shadow -> it.shadow?.let { s -> ((s.blur / .5f).coerceIn(0f, 1f) * 100).roundToInt().let { v -> if (v == 0) 40 else v }.toFloat() } ?: 0f
        Opacity -> (it.opacity * 100).roundToInt().toFloat()
        Size -> (it.size * 1000).roundToInt() / 10f
        Spacing -> (it.spacing * 100).roundToInt().toFloat()
        Curve -> it.curve.roundToInt().toFloat()
        Rotate -> it.rot.roundToInt().toFloat()
    }

    fun set(it: CanvasItem, v: Float): CanvasItem = when (this) {
        Stroke -> it.copy(strokeW = v / 100f)
        Shadow -> it.copy(shadow = if (v > 0f) ShadowSpec(it.shadow?.color ?: 0x8C000000, v / 100f * .5f, 0f, v / 100f * .12f) else null)
        Opacity -> it.copy(opacity = v / 100f)
        Size -> it.copy(size = v / 100f)
        Spacing -> it.copy(spacing = v / 100f)
        Curve -> it.copy(curve = v)
        Rotate -> it.copy(rot = v)
    }

    fun format(v: Float): String {
        val rounded = (v * 10).roundToInt() / 10f
        val text = if (rounded == rounded.toInt().toFloat()) rounded.toInt().toString() else rounded.toString()
        return text + unit
    }
}

enum class ColorTarget(val label: String) { Fill("Text"), Stroke("Outline"), Highlight("Highlight") }

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
        if (after != before) vm.showToast("Shape set to ${after.label} to fit this layout.")
    }

    fun setAspect(a: AspectPreset) = update { it.copy(aspect = a) }
    fun clearBackgroundImage() = update { it.copy(backgroundImage = null) }

    /** Imports a picture from the device and uses it as the background. */
    fun setBackgroundFromDevice(uri: android.net.Uri) {
        vm.viewModelScope.launch {
            val ref = vm.store.importPhotos(project.id, listOf(uri)).firstOrNull()
            if (ref == null) vm.showToast("Couldn't read that picture.") else update { it.copy(backgroundImage = ref) }
        }
    }

    fun setBackground(argb: Long) = update { it.copy(background = argb) }
    fun setGap(v: Float) = update { it.copy(gapPercent = v) }
    fun setMargin(v: Float) = update { it.copy(marginPercent = v) }
    fun setCorner(v: Float) = update { it.copy(cornerPercent = v) }
    fun setFilter(id: String) = update { it.copy(filter = id) }
    fun setFilterAmount(v: Float) = update { it.copy(filterAmount = v) }
    fun setTextureAmount(v: Float) = update { it.copy(textureAmount = v) }

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

    fun resetCell() = updateCell(selectedCell) { it.copy(zoom = 1f, panX = 0f, panY = 0f) }
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
            if (refs.isEmpty()) vm.showToast("Couldn't read those photos.") else addPhotos(refs)
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
        vm.showToast(if (front) "Brought to front." else "Sent to back.")
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
        val pr = CollageRenderer.photoRect(r)
        val box = CollageRenderer.photoBox(pr.left, pr.top, pr.width(), pr.height(), cell, bmp.width, bmp.height)
        val ox = (box.width - pr.width()) / 2
        val oy = (box.height - pr.height()) / 2
        updateCell(drag.cell) {
            it.copy(
                panX = if (ox > .5f) (drag.panX + (x - drag.x) / ox).coerceIn(-1f, 1f) else it.panX,
                panY = if (oy > .5f) (drag.panY + (y - drag.y) / oy).coerceIn(-1f, 1f) else it.panY,
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
