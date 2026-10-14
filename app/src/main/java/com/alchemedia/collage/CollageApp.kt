package com.alchemedia.collage

import android.app.Application
import com.alchemedia.collage.data.BillingManager
import com.alchemedia.collage.data.CollageStore
import com.alchemedia.collage.data.PhotoStore
import com.alchemedia.collage.data.SettingsStore
import com.alchemedia.collage.model.LayoutCatalog
import com.alchemedia.collage.render.Fonts
import com.alchemedia.collage.render.Textures

class CollageApp : Application() {
    val store: CollageStore by lazy { CollageStore(this) }
    val photos: PhotoStore by lazy { PhotoStore(this, store) }
    val settings: SettingsStore by lazy { SettingsStore(this) }
    val billing: BillingManager by lazy { BillingManager(this, settings) }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= TRIM_MEMORY_RUNNING_LOW) {
            Textures.clear()
            photos.trim()
        }
    }

    override fun onCreate() {
        super.onCreate()
        Strings.setLanguage(settings.language.value)
        Strings.init(this)
        Fonts.init(this)
        LayoutCatalog.load(assets.open("layouts.json").bufferedReader().use { it.readText() })
    }
}
