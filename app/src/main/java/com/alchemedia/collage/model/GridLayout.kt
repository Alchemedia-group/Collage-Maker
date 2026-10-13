package com.alchemedia.collage.model

/** A rectangle in the unit square. Layouts are stretched to whatever aspect ratio the canvas has. */
data class UnitRect(val x: Float, val y: Float, val w: Float, val h: Float)

data class GridLayout(val id: String, val cells: List<UnitRect>)

object GridLayouts {
    private fun grid(cols: Int, rows: Int): List<UnitRect> = buildList {
        for (j in 0 until rows) for (i in 0 until cols) {
            add(UnitRect(i / cols.toFloat(), j / rows.toFloat(), 1f / cols, 1f / rows))
        }
    }

    /** Cells given as (x, y, w, h) in a cols x rows grid, where w and h default to 1. */
    private fun spans(cols: Int, rows: Int, vararg cells: IntArray): List<UnitRect> = cells.map {
        val w = it.getOrElse(2) { 1 }
        val h = it.getOrElse(3) { 1 }
        UnitRect(it[0] / cols.toFloat(), it[1] / rows.toFloat(), w / cols.toFloat(), h / rows.toFloat())
    }

    private fun c(vararg v: Int) = v

    val all: List<GridLayout> = listOf(
        GridLayout("1", grid(1, 1)),
        GridLayout("2v", grid(2, 1)),
        GridLayout("2h", grid(1, 2)),
        GridLayout("3v", grid(3, 1)),
        GridLayout("3h", grid(1, 3)),
        GridLayout("4", grid(2, 2)),
        GridLayout("l1r2", spans(2, 2, c(0, 0, 1, 2), c(1, 0), c(1, 1))),
        GridLayout("l2r1", spans(2, 2, c(0, 0), c(0, 1), c(1, 0, 1, 2))),
        GridLayout("t1b2", spans(2, 2, c(0, 0, 2, 1), c(0, 1), c(1, 1))),
        GridLayout("t2b1", spans(2, 2, c(0, 0), c(1, 0), c(0, 1, 2, 1))),
        GridLayout("l1r3", spans(3, 3, c(0, 0, 2, 3), c(2, 0), c(2, 1), c(2, 2))),
        GridLayout("t1b3", spans(3, 2, c(0, 0, 3, 1), c(0, 1), c(1, 1), c(2, 1))),
        GridLayout("3x2", grid(3, 2)),
        GridLayout("2x3", grid(2, 3)),
        GridLayout("hero", spans(3, 3, c(0, 0, 2, 2), c(2, 0), c(2, 1), c(0, 2), c(1, 2), c(2, 2))),
        GridLayout(
            "pinwheel",
            listOf(
                UnitRect(0f, 0f, .6f, .4f),
                UnitRect(.6f, 0f, .4f, .6f),
                UnitRect(.4f, .6f, .6f, .4f),
                UnitRect(0f, .4f, .4f, .6f),
                UnitRect(.4f, .4f, .2f, .2f),
            ),
        ),
        GridLayout("3x3", grid(3, 3)),
        GridLayout("4x3", grid(4, 3)),
    )

    const val DEFAULT_ID = "4"

    fun byId(id: String): GridLayout = all.firstOrNull { it.id == id } ?: all.first { it.id == DEFAULT_ID }

    /** A sensible starting layout for [photoCount] photos. */
    fun suggestFor(photoCount: Int): GridLayout {
        val exact = when (photoCount) {
            1 -> "1"
            2 -> "2v"
            3 -> "l1r2"
            4 -> "4"
            5 -> "pinwheel"
            6 -> "hero"
            in 7..9 -> "3x3"
            else -> "4x3"
        }
        return byId(exact)
    }
}
