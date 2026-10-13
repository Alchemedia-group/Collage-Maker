package com.alchemedia.collage.ui.designer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.alchemedia.collage.model.AspectPreset
import com.alchemedia.collage.model.Layout
import com.alchemedia.collage.model.LayoutCatalog
import com.alchemedia.collage.model.LayoutCell
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

enum class DesignerTab(val label: String) { Cells("Cells"), Shape("Shape"), Canvas("Canvas") }
enum class ShapeAdjust(val label: String) { Size("Size"), Rotate("Rotate") }

/** A dashed guide line shown while a cell snaps (axis 'x' is a vertical line). */
data class Guide(val axis: Char, val v: Float)

/** The kinds of cell the designer offers. */
val DesignerShapes = listOf(
    "rect" to "Rectangle", "heart" to "Heart", "circle" to "Circle", "star" to "Star", "diamond" to "Diamond", "hexagon" to "Hexagon",
    "triangle" to "Triangle", "leaf" to "Leaf", "leafR" to "Leaf 2", "arch" to "Arch", "letter" to "Letter",
)

private const val MIN_CELL = .04f
private val FULL = LayoutCell(0f, 0f, 1f, 1f)

/**
 * Draw-your-own layout: cells in the unit square that can be moved, resized (neighbours follow when edges are
 * linked), split, shaped and saved to My layouts. The geometry is plain Kotlin so it can be tested without a screen.
 */
class DesignerState(base: Layout?, val editId: String?) {
    var cells by mutableStateOf(base?.cells ?: listOf(FULL))
        private set
    var selected by mutableIntStateOf(0)
    var aspect by mutableStateOf(AspectPreset.fromLabel(base?.ar))
    var name by mutableStateOf(if (editId != null) base?.name.orEmpty() else "")
    var tab by mutableStateOf(DesignerTab.Cells)
    var adjust by mutableStateOf(ShapeAdjust.Size)
    var snap by mutableStateOf(true)
    var link by mutableStateOf(true)
    var guides by mutableStateOf<List<Guide>>(emptyList())
        private set
    var armDelete by mutableStateOf(false)

    private val past = ArrayDeque<List<LayoutCell>>()
    private val future = ArrayDeque<List<LayoutCell>>()
    var undoCount by mutableIntStateOf(0)
        private set
    var redoCount by mutableIntStateOf(0)
        private set

    /** Canvas size in pixels and screen density (for touch slop and handle size), set by the canvas composable. */
    var canvasW by mutableFloatStateOf(1f)
    var canvasH by mutableFloatStateOf(1f)
    var density by mutableFloatStateOf(1f)

    val selectedCell: LayoutCell? get() = cells.getOrNull(selected)

    /** The shape key of the selected cell as the shape row shows it. */
    val selectedKey: String? get() = selectedCell?.let { if (it.isLetter) "letter" else it.shape ?: "rect" }

    // ------------------------------------------------------------ history

    private fun push() {
        past.addLast(cells)
        while (past.size > 60) past.removeFirst()
        future.clear()
        syncCounts()
    }

    private fun syncCounts() { undoCount = past.size; redoCount = future.size }

    fun undo() {
        if (past.isEmpty()) return
        future.addLast(cells)
        cells = past.removeLast()
        selected = min(selected, cells.lastIndex)
        syncCounts()
    }

    fun redo() {
        if (future.isEmpty()) return
        past.addLast(cells)
        cells = future.removeLast()
        selected = min(selected, cells.lastIndex)
        syncCounts()
    }

    // ------------------------------------------------------------ whole layout

    /** Replaces the cells with those of another layout ("Start from…"). */
    fun load(layout: Layout) {
        push()
        cells = layout.cells
        layout.ar?.let { aspect = AspectPreset.fromLabel(it) }
        selected = 0
    }

    fun clear() { push(); cells = listOf(FULL); selected = 0 }

    // ------------------------------------------------------------ cell operations (return a message when they cannot be done)

    fun split(vertical: Boolean): String? {
        val c = selectedCell ?: return "Tap a cell first."
        val half = (if (vertical) c.w else c.h) / 2f
        if (half < MIN_CELL) return "That cell is too small to split."
        push()
        val a = if (vertical) c.copy(w = half) else c.copy(h = half)
        val b = if (vertical) c.copy(x = c.x + half, w = half) else c.copy(y = c.y + half, h = half)
        cells = cells.toMutableList().also { it[selected] = a; it.add(selected + 1, b) }
        return null
    }

    fun duplicate(): String? {
        val c = selectedCell ?: return "Tap a cell first."
        if (cells.size >= 40) return "A layout can have up to 40 cells."
        push()
        cells = cells + c.copy(x = (c.x + .03f).coerceIn(0f, 1f - c.w), y = (c.y + .03f).coerceIn(0f, 1f - c.h))
        selected = cells.lastIndex
        return null
    }

    fun delete(): String? {
        if (selectedCell == null) return "Tap a cell first."
        if (cells.size <= 1) return "A layout needs at least one cell."
        push()
        cells = cells.toMutableList().also { it.removeAt(selected) }
        selected = min(selected, cells.lastIndex)
        return null
    }

    fun toFront(): String? {
        val c = selectedCell ?: return "Tap a cell first."
        push()
        cells = cells.toMutableList().also { it.removeAt(selected); it.add(c) }
        selected = cells.lastIndex
        return null
    }

    fun toBack(): String? {
        val c = selectedCell ?: return "Tap a cell first."
        push()
        cells = cells.toMutableList().also { it.removeAt(selected); it.add(0, c) }
        selected = 0
        return null
    }

    private fun replaceSelected(fn: (LayoutCell) -> LayoutCell) {
        val c = selectedCell ?: return
        cells = cells.toMutableList().also { it[selected] = fn(c) }
    }

    fun setShape(key: String): String? {
        if (selectedCell == null) return "Tap a cell first."
        push()
        replaceSelected { c ->
            val plain = c.copy(shape = null, text = null, fill = false, rot = null, sc = null)
            when (key) {
                "letter" -> plain.copy(text = "A")
                "rect" -> plain
                else -> plain.copy(shape = key, sc = 1f)
            }
        }
        return null
    }

    /** Begin a slider drag: one undo step for the whole drag. */
    fun beginEdit() { if (selectedCell != null) push() }

    fun setSize(percent: Float) = replaceSelected { it.copy(sc = percent / 100f) }
    fun setRotation(deg: Float) = replaceSelected { it.copy(rot = deg) }

    fun toggleStretch() {
        val c = selectedCell ?: return
        if (c.shape == null) return
        push()
        replaceSelected { it.copy(fill = !it.fill) }
    }

    fun setLetter(text: String) {
        val v = text.uppercase().filter { !it.isWhitespace() }.take(2)
        if (v.isEmpty()) return
        replaceSelected { if (it.isLetter) it.copy(text = v) else it }
    }

    fun nudge(dx: Float, dy: Float) {
        val c = selectedCell ?: return
        push()
        replaceSelected { it.copy(x = (c.x + dx).coerceIn(0f, 1f - c.w), y = (c.y + dy).coerceIn(0f, 1f - c.h)) }
    }

    // ------------------------------------------------------------ touch

    private enum class Mode { Move, Resize }
    private class Drag(val mode: Mode, val handle: String?, val idx: Int, val start: List<LayoutCell>, val x0: Float, val y0: Float, var pushed: Boolean = false)
    private var drag: Drag? = null

    fun handlePoints(c: LayoutCell): Map<String, Pair<Float, Float>> = mapOf(
        "tl" to (c.x to c.y), "t" to (c.x + c.w / 2 to c.y), "tr" to (c.x + c.w to c.y), "l" to (c.x to c.y + c.h / 2),
        "r" to (c.x + c.w to c.y + c.h / 2), "bl" to (c.x to c.y + c.h), "b" to (c.x + c.w / 2 to c.y + c.h), "br" to (c.x + c.w to c.y + c.h),
    )

    private class Hit(val handle: String? = null, val cell: Int = -1)

    private fun hit(nx: Float, ny: Float): Hit? {
        selectedCell?.let { c ->
            handlePoints(c).forEach { (name, p) ->
                if (hypot(nx * canvasW - p.first * canvasW, ny * canvasH - p.second * canvasH) <= 18f * density) return Hit(handle = name)
            }
        }
        for (i in cells.indices.reversed()) {
            val q = cells[i]
            if (nx >= q.x && nx <= q.x + q.w && ny >= q.y && ny <= q.y + q.h) return Hit(cell = i)
        }
        return null
    }

    fun onDown(px: Float, py: Float) {
        val nx = px / canvasW
        val ny = py / canvasH
        val h = hit(nx, ny)
        if (h == null) { selected = -1; return }
        if (h.handle == null) selected = h.cell
        drag = Drag(if (h.handle != null) Mode.Resize else Mode.Move, h.handle, selected, cells, nx, ny)
    }

    fun onMove(px: Float, py: Float) {
        val d = drag ?: return
        val nx = px / canvasW
        val ny = py / canvasH
        val dx = nx - d.x0
        val dy = ny - d.y0
        if (!d.pushed) {
            if (hypot(dx * canvasW, dy * canvasH) < 3f * density) return
            past.addLast(d.start); while (past.size > 60) past.removeFirst(); future.clear(); syncCounts()
            d.pushed = true
        }
        val g = ArrayList<Guide>()
        cells = if (d.mode == Mode.Move) move(d.start, d.idx, dx, dy, g) else resize(d.start, d.idx, d.handle!!, dx, dy, g)
        guides = g
    }

    fun onUp() { drag = null; guides = emptyList() }

    // ------------------------------------------------------------ snapping and resizing

    private fun candidates(axis: Char, start: List<LayoutCell>, skip: Set<Int>): List<Float> {
        val out = mutableListOf(0f, 1f, .5f)
        if (snap) for (g in 1 until 20) out.add(g / 20f)
        start.forEachIndexed { i, c ->
            if (i in skip) return@forEachIndexed
            if (axis == 'x') { out.add(c.x); out.add(c.x + c.w) } else { out.add(c.y); out.add(c.y + c.h) }
        }
        return out
    }

    private fun threshold(axis: Char): Float = 8f * density / (if (axis == 'x') canvasW else canvasH)

    private fun snapValue(axis: Char, v: Float, start: List<LayoutCell>, skip: Set<Int>, lo: Float, hi: Float, guides: MutableList<Guide>): Float {
        if (!snap) return v.coerceIn(lo, hi)
        var best: Float? = null
        var bestD = Float.MAX_VALUE
        candidates(axis, start, skip).forEach { cd ->
            val d = abs(cd - v)
            if (d < threshold(axis) && d < bestD) { best = cd; bestD = d }
        }
        val b = best
        if (b != null) {
            val sv = b.coerceIn(lo, hi)
            if (abs(sv - b) < 1e-6f) guides.add(Guide(axis, sv))
            return sv
        }
        return v.coerceIn(lo, hi)
    }

    private fun move(start: List<LayoutCell>, idx: Int, dx: Float, dy: Float, guides: MutableList<Guide>): List<LayoutCell> {
        val s = start[idx]
        var x = (s.x + dx).coerceIn(0f, 1f - s.w)
        var y = (s.y + dy).coerceIn(0f, 1f - s.h)
        if (snap) {
            fun fit(axis: Char, pos: Float, size: Float): Float {
                var best: Triple<Float, Float, Float>? = null // distance, position, guide
                listOf(0f, size / 2f, size).forEach { off ->
                    candidates(axis, start, setOf(idx)).forEach { cd ->
                        val d = abs(pos + off - cd)
                        if (d < threshold(axis) && (best == null || d < best!!.first)) best = Triple(d, cd - off, cd)
                    }
                }
                return best?.let { guides.add(Guide(axis, it.third)); it.second } ?: pos
            }
            x = fit('x', x, s.w).coerceIn(0f, 1f - s.w)
            y = fit('y', y, s.h).coerceIn(0f, 1f - s.h)
        }
        return start.toMutableList().also { it[idx] = s.copy(x = x, y = y) }
    }

    private class EdgeCell(val i: Int, val lo: Boolean, val hi: Boolean)

    /** Every cell that touches the same divider line and is joined to the selected cell along it. */
    private fun edgeGroup(cells: List<LayoutCell>, idx: Int, axis: Char, coord: Float): List<EdgeCell> {
        val eps = .004f
        fun seg(c: LayoutCell) = if (axis == 'x') c.y to c.y + c.h else c.x to c.x + c.w
        fun near(c: LayoutCell) = if (axis == 'x') c.x else c.y
        fun far(c: LayoutCell) = if (axis == 'x') c.x + c.w else c.y + c.h
        val cand = cells.mapIndexed { i, c -> EdgeCell(i, abs(far(c) - coord) < eps, abs(near(c) - coord) < eps) }.filter { it.lo || it.hi }
        val group = mutableSetOf(idx)
        var (a, b) = seg(cells[idx])
        var changed = true
        while (changed) {
            changed = false
            cand.forEach { o ->
                if (o.i in group) return@forEach
                val (s0, s1) = seg(cells[o.i])
                if (s1 >= a - eps && s0 <= b + eps) { group.add(o.i); a = min(a, s0); b = max(b, s1); changed = true }
            }
        }
        return cand.filter { it.i in group }
    }

    private fun resize(start: List<LayoutCell>, idx: Int, handle: String, dx: Float, dy: Float, guides: MutableList<Guide>): List<LayoutCell> {
        val cells = start.toMutableList()
        val s = start[idx]
        val axes = mutableListOf<Pair<Char, Boolean>>() // axis, near edge?
        if ('l' in handle) axes.add('x' to true)
        if ('r' in handle) axes.add('x' to false)
        if ('t' in handle) axes.add('y' to true)
        if ('b' in handle) axes.add('y' to false)
        axes.forEach { (axis, nearEdge) ->
            val coord = if (axis == 'x') (if (nearEdge) s.x else s.x + s.w) else (if (nearEdge) s.y else s.y + s.h)
            val d = if (axis == 'x') dx else dy
            val group = if (link) edgeGroup(start, idx, axis, coord) else listOf(EdgeCell(idx, !nearEdge, nearEdge))
            var lo = 0f
            var hi = 1f
            group.forEach { o ->
                val c = start[o.i]
                if (o.lo) lo = max(lo, (if (axis == 'x') c.x else c.y) + MIN_CELL)
                if (o.hi) hi = min(hi, (if (axis == 'x') c.x + c.w else c.y + c.h) - MIN_CELL)
            }
            var e = (coord + d).coerceIn(lo, max(lo, hi))
            e = snapValue(axis, e, start, group.map { it.i }.toSet(), lo, max(lo, hi), guides)
            group.forEach { o ->
                val c = cells[o.i]
                cells[o.i] = if (axis == 'x') {
                    var n = c
                    if (o.lo) n = n.copy(w = e - n.x)
                    if (o.hi) { val right = n.x + n.w; n = n.copy(x = e, w = right - e) }
                    n
                } else {
                    var n = c
                    if (o.lo) n = n.copy(h = e - n.y)
                    if (o.hi) { val bottom = n.y + n.h; n = n.copy(y = e, h = bottom - e) }
                    n
                }
            }
        }
        return cells
    }

    // ------------------------------------------------------------ saving

    private fun r4(v: Float) = Math.round(v * 10000f) / 10000f

    /** The layout to save, with a generated name when the user left it blank. */
    fun toLayout(existingCount: Int): Layout {
        val label = name.trim().ifEmpty { "My layout ${existingCount + (if (editId != null) 0 else 1)}" }
        val id = editId ?: ("my-" + java.lang.Long.toString(System.currentTimeMillis(), 36))
        val out = cells.map { c ->
            c.copy(x = r4(c.x), y = r4(c.y), w = r4(c.w), h = r4(c.h), shape = c.shape?.takeIf { it.isNotEmpty() }, text = c.text?.takeIf { it.isNotEmpty() })
        }
        return Layout(id, label, "mine", ar = aspect.label, kw = "custom designed my layout ${label.lowercase()}", cells = out, mine = true)
    }

    /** Publishes the working cells so the renderer can draw them. */
    fun publishDraft() {
        LayoutCatalog.draft = Layout(LayoutCatalog.DRAFT_ID, "Draft", "mine", cells = cells)
    }
}
