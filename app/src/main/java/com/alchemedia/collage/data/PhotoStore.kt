package com.alchemedia.collage.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.LruCache
import androidx.exifinterface.media.ExifInterface
import com.alchemedia.collage.model.CollageProject
import com.alchemedia.collage.model.PhotoRef
import com.alchemedia.collage.render.PhotoSource
import java.io.File

/**
 * Decodes photos at the size the screen needs (or full size for export), turns them upright using EXIF, and keeps
 * recent ones in memory. Built-in sample photos are drawables named `res:photo_xyz`.
 */
class PhotoStore(context: Context, private val store: CollageStore) {
    private val appContext = context.applicationContext
    private val cache = object : LruCache<String, Bitmap>(((Runtime.getRuntime().maxMemory() / 6).coerceAtMost(160L * 1024 * 1024)).toInt()) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }

    /** Frees the in-memory photo cache (low memory). */
    fun trim() = cache.evictAll()

    private fun key(projectId: String, photo: PhotoRef, maxSide: Int) = "$projectId/${photo.fileName}@$maxSide"

    /** Loads (and caches) a photo. Call from a background thread. */
    fun load(projectId: String, photo: PhotoRef, maxSide: Int): Bitmap? {
        val k = key(projectId, photo, maxSide)
        cache.get(k)?.let { return it }
        val bmp = if (photo.fileName.startsWith("res:")) decodeResource(photo.fileName.removePrefix("res:"), maxSide)
        else decodeFile(store.photoFile(projectId, photo), maxSide)
        if (bmp != null) cache.put(k, bmp)
        return bmp
    }

    fun preload(project: CollageProject, maxSide: Int) {
        project.photos.forEach { load(project.id, it, maxSide) }
        project.backgroundImage?.let { load(project.id, it, maxSide) }
    }

    /** A source for the renderer. With [blocking] it decodes on demand (export); otherwise it only returns what is cached. */
    fun source(project: CollageProject, maxSide: Int, blocking: Boolean = false): PhotoSource = object : PhotoSource {
        override fun bitmap(photoId: String): Bitmap? {
            val photo = project.photo(photoId) ?: return null
            return if (blocking) load(project.id, photo, maxSide) else cache.get(key(project.id, photo, maxSide))
        }
    }

    /** A small, upright copy of a picked photo that has not been imported yet (for previews). */
    fun decodeUri(uri: android.net.Uri, maxSide: Int): Bitmap? = runCatching {
        val resolver = appContext.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0) return null
        val opts = BitmapFactory.Options().apply { inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, maxSide) }
        val raw = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) } ?: return null
        val orientation = resolver.openInputStream(uri)?.use {
            ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        } ?: ExifInterface.ORIENTATION_NORMAL
        rotate(raw, orientation)
    }.getOrNull()

    private fun decodeResource(name: String, maxSide: Int): Bitmap? {
        val id = appContext.resources.getIdentifier(name, "drawable", appContext.packageName)
        if (id == 0) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeResource(appContext.resources, id, bounds)
        val opts = BitmapFactory.Options().apply { inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, maxSide) }
        return BitmapFactory.decodeResource(appContext.resources, id, opts)
    }

    private fun decodeFile(file: File, maxSide: Int): Bitmap? {
        if (!file.exists()) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        if (bounds.outWidth <= 0) return null
        val opts = BitmapFactory.Options().apply { inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, maxSide) }
        val raw = BitmapFactory.decodeFile(file.path, opts) ?: return null
        return rotate(raw, exifOrientation(file))
    }

    private fun rotate(raw: Bitmap, orientation: Int): Bitmap {
        val m = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> m.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> m.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> m.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> m.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> m.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> { m.postRotate(90f); m.postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_TRANSVERSE -> { m.postRotate(270f); m.postScale(-1f, 1f) }
            else -> return raw
        }
        return runCatching { Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, m, true) }.getOrDefault(raw)
    }

    /** Largest power of two that still leaves the longer side at least [maxSide] pixels. */
    private fun sampleSize(w: Int, h: Int, maxSide: Int): Int {
        var sample = 1
        while (maxOf(w, h) / (sample * 2) >= maxSide) sample *= 2
        return sample
    }
}
