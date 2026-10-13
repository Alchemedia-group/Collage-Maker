package com.alchemedia.collage.render

import android.content.Context
import android.content.res.AssetManager
import android.graphics.Typeface
import com.alchemedia.collage.model.FontCatalog
import com.alchemedia.collage.model.FontDef

/** Loads the bundled fonts (assets/fonts) once. */
object Fonts {
    private var assets: AssetManager? = null
    private val cache = HashMap<String, Typeface>()

    fun init(context: Context) {
        assets = context.applicationContext.assets
    }

    fun weightOf(def: FontDef, bold: Boolean): Int = if (bold) 700 else def.weight

    /** Whether a bold request on this font has to be faked because its file only has one weight. */
    fun needsFakeBold(def: FontDef, bold: Boolean): Boolean = bold && !def.variable && def.weight < 600

    @Synchronized
    fun typeface(def: FontDef, bold: Boolean = false): Typeface {
        val weight = if (def.variable) weightOf(def, bold) else def.weight
        val key = "${def.id}@$weight"
        cache[key]?.let { return it }
        val loaded = runCatching {
            val am = assets ?: return@runCatching null
            val builder = Typeface.Builder(am, "fonts/${def.file}")
            if (def.variable) builder.setFontVariationSettings("'wght' $weight")
            builder.build()
        }.getOrNull() ?: Typeface.create(Typeface.SANS_SERIF, if (weight >= 600) Typeface.BOLD else Typeface.NORMAL)
        cache[key] = loaded
        return loaded
    }

    fun typeface(id: String, bold: Boolean = false): Typeface = typeface(FontCatalog.byId(id), bold)
}
