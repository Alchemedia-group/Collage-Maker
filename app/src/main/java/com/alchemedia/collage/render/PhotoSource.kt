package com.alchemedia.collage.render

import android.graphics.Bitmap

/** Where the renderer gets photos. Returns null while a photo is still loading. */
fun interface PhotoSource {
    fun bitmap(photoId: String): Bitmap?
}
