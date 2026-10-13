package com.alchemedia.collage.data

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.alchemedia.collage.model.CollageProject
import com.alchemedia.collage.render.CollageRenderer
import com.alchemedia.collage.render.Textures
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Renders a finished collage and writes it to the device's Pictures folder. */
object Exporter {
    /** The longer side of an exported picture, in pixels. */
    const val LONG_SIDE = 2048

    fun sizeFor(project: CollageProject, longSide: Int = LONG_SIDE): Pair<Int, Int> = CollageRenderer.exportSize(project.aspect.ratio, longSide)

    suspend fun render(photos: PhotoStore, project: CollageProject, watermark: Boolean, longSide: Int = LONG_SIDE): Bitmap = withContext(Dispatchers.Default) {
        val (w, h) = sizeFor(project, longSide)
        CollageRenderer.render(project, w, h, photos.source(project, longSide, blocking = true), watermark)
    }

    /**
     * Renders at the full size, and on a device that runs out of memory tries smaller sizes rather than failing.
     * Returns null only if even the smallest size does not fit.
     */
    suspend fun renderWithFallback(photos: PhotoStore, project: CollageProject, watermark: Boolean): Bitmap? {
        for (side in intArrayOf(LONG_SIDE, 1536, 1080, 720)) {
            try {
                return render(photos, project, watermark, side)
            } catch (_: OutOfMemoryError) {
                Textures.clear()
            }
        }
        return null
    }

    /** Saves to Pictures/Photo Collage. On Android 9 and older this needs the storage permission. */
    suspend fun saveToGallery(context: Context, bitmap: Bitmap): Uri? = withContext(Dispatchers.IO) {
        runCatching {
            val resolver = context.contentResolver
            val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "PhotoCollage_$stamp.jpg")
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Photo Collage")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return@runCatching null
            try {
                resolver.openOutputStream(uri)?.use { check(bitmap.compress(Bitmap.CompressFormat.JPEG, 93, it)) } ?: error("no output stream")
            } catch (e: Exception) {
                resolver.delete(uri, null, null)
                throw e
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                resolver.update(uri, ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null)
            }
            uri
        }.getOrNull()
    }
}
