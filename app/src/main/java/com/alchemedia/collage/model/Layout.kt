package com.alchemedia.collage.model

import com.alchemedia.collage.R
import com.alchemedia.collage.Strings
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** One cell of a layout, in the unit square. A cell can be cut to a shape or to a letter. */
@Serializable
data class LayoutCell(
    val x: Float,
    val y: Float,
    val w: Float,
    val h: Float,
    val shape: String? = null,
    val sc: Float? = null,
    val rot: Float? = null,
    val fill: Boolean = false,
    val text: String? = null,
) {
    val scale: Float get() = sc ?: 1f
    val rotation: Float get() = rot ?: 0f
    val isLetter: Boolean get() = !text.isNullOrEmpty()
    val isShape: Boolean get() = !shape.isNullOrEmpty() && !isLetter
}

@Serializable
data class Layout(
    val id: String,
    val name: String,
    val cat: String,
    val ar: String? = null,
    val kw: String = "",
    val cells: List<LayoutCell>,
    val mine: Boolean = false,
) {
    /** The name to show: translated for the built-in layouts, as typed for the user's own. */
    val title: String get() = if (mine || id == LayoutCatalog.DRAFT_ID) name else Strings.named("layout_$id", name)
}

@Serializable
data class LayoutFile(val shapes: Map<String, String>, val layouts: List<Layout>)

object LayoutCategories {
    /** "fit" is computed from the photo count; every other id is stored on the layouts. */
    val all = listOf("fit" to "For you", "grid" to "Grids", "feature" to "Feature", "mosaic" to "Mosaic", "shape" to "Shapes", "letters" to "Letters", "mine" to "Mine")
    val browsable = all.filter { it.first != "fit" }

    /** The category chip text, in the current language. */
    fun label(id: String): String = Strings.named("lc_$id", all.firstOrNull { it.first == id }?.second ?: id)

    fun title(id: String): String = if (id == "mine") Strings.named("lc_mine_title", "My layouts") else label(id)
}

enum class CountFilter(val label: String) {
    Any("Any"), One("1"), Two("2"), Three("3"), Four("4"), Five("5"), Six("6"), SevenToNine("7-9"), TenPlus("10+");

    /** "Any" is a word, so it is translated; the numbers are not. */
    val title: String get() = if (this == Any) Strings.get(R.string.any) else label

    fun matches(n: Int): Boolean = when (this) {
        Any -> true
        One -> n == 1
        Two -> n == 2
        Three -> n == 3
        Four -> n == 4
        Five -> n == 5
        Six -> n == 6
        SevenToNine -> n in 7..9
        TenPlus -> n >= 10
    }
}

/** The built-in layouts (from assets/layouts.json) plus the user's own. */
object LayoutCatalog {
    const val DEFAULT_ID = "g2x2"
    const val DRAFT_ID = "__draft"
    private const val WORD_PREFIX = "w-custom-"

    private val json = Json { ignoreUnknownKeys = true }
    private var builtIn: List<Layout> = emptyList()
    private var mine: List<Layout> = emptyList()

    var shapes: Map<String, String> = emptyMap()
        private set

    val popular = listOf("g2x2", "hero", "hearts4", "clover", "w-love", "honey7", "mos8", "petals4", "circles4", "l1r2", "bubbles", "frame7")
    val quickIds = listOf("hearts4", "clover", "w-love", "honey7", "hero", "mos8", "petals4", "bubbles")

    fun load(text: String) {
        val file = json.decodeFromString(LayoutFile.serializer(), text)
        builtIn = file.layouts
        shapes = file.shapes
    }

    fun setMine(layouts: List<Layout>) {
        mine = layouts
    }

    private var custom: List<Layout> = emptyList()

    /** The layout being drawn in the designer, so the renderer can draw it like any other. */
    var draft: Layout = Layout(DRAFT_ID, "Draft", "mine", cells = listOf(LayoutCell(0f, 0f, 1f, 1f)))

    /** A layout whose cells are the letters of [word] (up to 8). The same word always gives the same layout. */
    fun wordLayout(word: String): Layout? {
        val text = word.uppercase().filter { it.isLetterOrDigit() || it == '*' }.take(8)
        if (text.isEmpty()) return null
        val id = "$WORD_PREFIX$text"
        custom.firstOrNull { it.id == id }?.let { return it }
        val layout = Layout(id, text, "letters", ar = wordAspect(text.length), kw = "custom word ${text.lowercase()}", cells = wordCells(text))
        custom = custom + layout
        return layout
    }

    private fun wordAspect(n: Int) = when (n) { 1, 4 -> "1:1"; 2, 3, 5, 6, 7 -> "3:2"; 8 -> "16:9"; else -> "1:1" }

    private fun wordCells(text: String): List<LayoutCell> {
        val n = text.length
        val rows = when (n) { 1 -> listOf(1); 2 -> listOf(2); 3 -> listOf(3); 4 -> listOf(2, 2); 5 -> listOf(3, 2); 6 -> listOf(3, 3); 7 -> listOf(4, 3); 8 -> listOf(4, 4); else -> listOf(n) }
        var k = 0
        return rows.flatMapIndexed { j, count ->
            List(count) { i ->
                val ch = text[k++]
                val x = i / count.toFloat()
                val y = j / rows.size.toFloat()
                if (ch == '*') LayoutCell(x, y, 1f / count, 1f / rows.size, shape = "heart", sc = .9f)
                else LayoutCell(x, y, 1f / count, 1f / rows.size, text = ch.toString())
            }
        }
    }

    val all: List<Layout> get() = builtIn + custom + mine
    val builtInLayouts: List<Layout> get() = builtIn

    /** Layouts typed in by the user are rebuilt from their id, so collages that use one still open after a restart. */
    fun find(id: String?): Layout? {
        if (id == null) return null
        if (id == DRAFT_ID) return draft
        all.firstOrNull { it.id == id }?.let { return it }
        return if (id.startsWith(WORD_PREFIX)) wordLayout(id.removePrefix(WORD_PREFIX)) else null
    }

    fun byId(id: String?): Layout = find(id) ?: find(DEFAULT_ID) ?: all.first()

    fun suggestFor(count: Int): Layout = byId(
        when {
            count <= 0 -> DEFAULT_ID
            count == 1 -> "g1x1"
            count == 2 -> "g2x1"
            count == 3 -> "l1r2"
            count == 4 -> "g2x2"
            count == 5 -> "pinwheel"
            count == 6 -> "hero"
            count <= 9 -> "g3x3"
            else -> "g4x3"
        },
    )

    /** The row shown under the category chips. */
    fun forCategory(category: String, photoCount: Int): List<Layout> {
        if (category != "fit") return all.filter { it.cat == category }
        if (photoCount <= 0) return popular.mapNotNull { find(it) }
        val exact = all.filter { it.cells.size == photoCount }
        val near = all.filter { it.cells.size == photoCount + 1 || (photoCount > 1 && it.cells.size == photoCount - 1) }
        return (exact + near).take(28)
    }

    /** Lower case with the accents taken off Latin letters, so "corazon" finds "corazón" and "cœur" finds "Coeur". */
    private fun plain(text: String): String =
        java.text.Normalizer.normalize(text.lowercase(), java.text.Normalizer.Form.NFD).replace(Regex("[\\u0300-\\u036f]"), "").replace("œ", "oe")

    /** Browse sheet: free text (a number means "this many photos"), photo-count filter and category. */
    fun search(query: String, count: CountFilter, category: String?): List<Layout> {
        val q = plain(query.trim())
        val byNumber = q.isNotEmpty() && q.all { it.isDigit() }
        return all.filter { l ->
            (category == null || l.cat == category) && count.matches(l.cells.size) &&
                when {
                    q.isEmpty() -> true
                    byNumber -> l.cells.size == q.toIntOrNull()
                    else -> plain("${l.title} ${l.name} ${l.kw} ${LayoutCategories.title(l.cat)}").contains(q)
                }
        }
    }
}
