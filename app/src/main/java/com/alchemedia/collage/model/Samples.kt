package com.alchemedia.collage.model

/** Built-in photos that ship with the app: used for the home screen previews and for trying layouts. */
object Samples {
    private fun ref(name: String) = PhotoRef(id = "sample_$name", fileName = "res:photo_$name", width = 640, height = 427)

    val moraine = ref("moraine")
    val beach = ref("beach")
    val milky = ref("milky")
    val wheat = ref("wheat")
    val forest = ref("forest")
    val boat = ref("boat")

    val all = listOf(moraine, beach, milky, wheat, forest, boat)

    /** A white, lightly rounded collage of sample photos, for previews. */
    fun draft(layout: Layout, offset: Int = 0, aspect: AspectPreset? = null, corner: Float = 14f, gap: Float = 2.5f, margin: Float = 2.5f): CollageProject {
        val photos = List(layout.cells.size) { all[(it + offset) % all.size] }
        val unique = photos.distinctBy { it.id }
        val project = CollageProject.create("sample", 0, unique, layout, aspect ?: AspectPreset.fromLabel(layout.ar))
            .copy(gapPercent = gap, marginPercent = margin, cornerPercent = corner)
        // Layouts with more cells than sample photos reuse photos, so fill every cell explicitly.
        return project.copy(cells = photos.map { CellContent(photoId = it.id) })
    }
}
