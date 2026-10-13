package com.alchemedia.collage

import com.alchemedia.collage.model.LayoutCatalog
import java.io.File

object TestAssets {
    private val assets = File("src/main/assets")

    fun loadCatalog() {
        LayoutCatalog.load(File(assets, "layouts.json").readText())
        LayoutCatalog.setMine(emptyList())
    }
}
