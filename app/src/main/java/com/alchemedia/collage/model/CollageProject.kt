package com.alchemedia.collage.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class AspectPreset(val label: String, val width: Int, val height: Int) {
    Square("1:1", 1, 1),
    Portrait45("4:5", 4, 5),
    Landscape32("3:2", 3, 2),
    Portrait23("2:3", 2, 3),
    Wide169("16:9", 16, 9),
    Story916("9:16", 9, 16);

    val ratio: Float get() = width.toFloat() / height

    companion object {
        fun fromLabel(label: String?): AspectPreset = entries.firstOrNull { it.label == label } ?: Square
    }
}

/** A photo copied into the app's private storage. [fileName] is relative to the project's photo folder, or `res:<drawable>` for built-in samples. */
@Serializable
data class PhotoRef(val id: String, val fileName: String, val width: Int, val height: Int)

/** What sits in one grid cell. [panX]/[panY] are -1..1 and [zoom] is >= 1. */
@Serializable
data class CellContent(
    val photoId: String? = null,
    val zoom: Float = 1f,
    val panX: Float = 0f,
    val panY: Float = 0f,
)

@Serializable
data class CollageProject(
    val id: String,
    val name: String = "",
    val createdAt: Long,
    val updatedAt: Long,
    val layoutId: String = LayoutCatalog.DEFAULT_ID,
    val aspect: AspectPreset = AspectPreset.Square,
    val background: Long = 0xFFFFFFFF,
    val gapPercent: Float = 2f,
    val marginPercent: Float = 2f,
    val cornerPercent: Float = 0f,
    val filter: String = "none",
    val filterAmount: Float = 1f,
    val texture: String = "none",
    val textureAmount: Float = 1f,
    val photos: List<PhotoRef> = emptyList(),
    val cells: List<CellContent> = emptyList(),
    val items: List<CanvasItem> = emptyList(),
) {
    val layout: Layout get() = LayoutCatalog.byId(layoutId)

    fun photo(id: String?): PhotoRef? = id?.let { wanted -> photos.firstOrNull { it.id == wanted } }

    /**
     * Switches layout. Photos keep their cell index where possible; photos that lost their cell
     * (or were never placed) fill whatever cells are empty.
     */
    fun withLayout(target: Layout): CollageProject {
        val next = MutableList(target.cells.size) { cells.getOrNull(it) ?: CellContent() }
        val placed = next.mapNotNull { it.photoId }.toSet()
        photos.filter { it.id !in placed }.forEach { photo ->
            val free = next.indexOfFirst { it.photoId == null }
            if (free >= 0) next[free] = CellContent(photoId = photo.id)
        }
        return copy(layoutId = target.id, cells = next)
    }

    /**
     * Brings a project back to a consistent shape when its layout was edited or deleted since it was saved. A project that
     * already matches its layout is left alone, so a cell the user emptied stays empty.
     */
    fun normalized(): CollageProject {
        val l = layout
        return if (l.id == layoutId && cells.size == l.cells.size) this else withLayout(l)
    }

    companion object {
        fun newId(): String = UUID.randomUUID().toString()

        fun create(id: String, now: Long, photos: List<PhotoRef>, layout: Layout, aspect: AspectPreset = AspectPreset.Square): CollageProject =
            CollageProject(
                id = id, createdAt = now, updatedAt = now, photos = photos, aspect = aspect,
                gapPercent = 2.5f, marginPercent = 2.5f, cornerPercent = 12f,
            ).withLayout(layout)
    }
}
