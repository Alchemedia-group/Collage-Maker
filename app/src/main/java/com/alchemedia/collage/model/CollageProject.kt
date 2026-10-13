package com.alchemedia.collage.model

import kotlinx.serialization.Serializable

@Serializable
enum class AspectPreset(val label: String, val width: Int, val height: Int) {
    Square("1:1", 1, 1),
    Portrait45("4:5", 4, 5),
    Landscape32("3:2", 3, 2),
    Portrait23("2:3", 2, 3),
    Wide169("16:9", 16, 9),
    Story916("9:16", 9, 16);

    val ratio: Float get() = width.toFloat() / height
}

/** A photo copied into the app's private storage. [fileName] is relative to the project's photo folder. */
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
    val layoutId: String = GridLayouts.DEFAULT_ID,
    val aspect: AspectPreset = AspectPreset.Square,
    val background: Long = 0xFFFFFFFF,
    val gapPercent: Float = 2f,
    val marginPercent: Float = 2f,
    val cornerPercent: Float = 0f,
    val photos: List<PhotoRef> = emptyList(),
    val cells: List<CellContent> = emptyList(),
) {
    val layout: GridLayout get() = GridLayouts.byId(layoutId)

    fun photo(id: String?): PhotoRef? = id?.let { wanted -> photos.firstOrNull { it.id == wanted } }

    /**
     * Switches layout. Photos keep their cell index where possible; photos that lost their cell
     * (or were never placed) fill whatever cells are empty.
     */
    fun withLayout(newLayoutId: String): CollageProject {
        val target = GridLayouts.byId(newLayoutId)
        val cells = MutableList(target.cells.size) { this.cells.getOrNull(it) ?: CellContent() }
        val placed = cells.mapNotNull { it.photoId }.toSet()
        photos.filter { it.id !in placed }.forEach { photo ->
            val free = cells.indexOfFirst { it.photoId == null }
            if (free >= 0) cells[free] = CellContent(photoId = photo.id)
        }
        return copy(layoutId = target.id, cells = cells)
    }

    companion object {
        fun create(id: String, now: Long, photos: List<PhotoRef>, layoutId: String): CollageProject =
            CollageProject(id = id, createdAt = now, updatedAt = now, photos = photos).withLayout(layoutId)
    }
}
