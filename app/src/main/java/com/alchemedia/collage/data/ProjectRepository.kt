package com.alchemedia.collage.data

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.exifinterface.media.ExifInterface
import com.alchemedia.collage.model.CollageProject
import com.alchemedia.collage.model.GridLayouts
import com.alchemedia.collage.model.PhotoRef
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * Projects live as one JSON file each under `files/projects`, and imported photos under
 * `files/photos/<projectId>`. Photos are copied in because picker URIs are only guaranteed to
 * stay readable for a short while.
 */
class ProjectRepository(context: Context) {
    private val appContext = context.applicationContext
    private val projectsDir = File(appContext.filesDir, "projects")
    private val photosDir = File(appContext.filesDir, "photos")
    private val lock = Mutex()

    private val _projects = MutableStateFlow<List<CollageProject>>(emptyList())
    val projects: StateFlow<List<CollageProject>> = _projects.asStateFlow()

    suspend fun refresh() = withContext(Dispatchers.IO) {
        lock.withLock {
            val loaded = projectsDir.listFiles { f -> f.extension == "json" }.orEmpty().mapNotNull { file ->
                runCatching { ProjectJson.decode(file.readText()) }.getOrNull()
            }
            _projects.value = loaded.sortedByDescending { it.updatedAt }
        }
    }

    suspend fun get(id: String): CollageProject? = withContext(Dispatchers.IO) {
        _projects.value.firstOrNull { it.id == id }
            ?: runCatching { ProjectJson.decode(projectFile(id).readText()) }.getOrNull()
    }

    suspend fun save(project: CollageProject) = withContext(Dispatchers.IO) {
        lock.withLock {
            val stamped = project.copy(updatedAt = System.currentTimeMillis())
            projectsDir.mkdirs()
            // Write to a temp file and rename so a crash can't leave a half-written project.
            val tmp = File(projectsDir, "${project.id}.json.tmp")
            tmp.writeText(ProjectJson.encode(stamped))
            check(tmp.renameTo(projectFile(project.id))) { "Could not save project ${project.id}" }
            _projects.value = (listOf(stamped) + _projects.value.filterNot { it.id == project.id })
                .sortedByDescending { it.updatedAt }
        }
    }

    suspend fun delete(id: String) = withContext(Dispatchers.IO) {
        lock.withLock {
            projectFile(id).delete()
            File(photosDir, id).deleteRecursively()
            _projects.value = _projects.value.filterNot { it.id == id }
        }
    }

    /** Copies [uris] into private storage and creates a project. Returns null if nothing could be imported. */
    suspend fun createFromUris(uris: List<Uri>, layoutId: String? = null): CollageProject? =
        withContext(Dispatchers.IO) {
            val projectId = UUID.randomUUID().toString()
            val dir = File(photosDir, projectId).apply { mkdirs() }
            val photos = uris.mapNotNull { importPhoto(it, dir) }
            if (photos.isEmpty()) {
                dir.deleteRecursively()
                return@withContext null
            }
            val layout = layoutId ?: GridLayouts.suggestFor(photos.size).id
            val project = CollageProject.create(projectId, System.currentTimeMillis(), photos, layout)
            save(project)
            project
        }

    fun photoFile(project: CollageProject, photo: PhotoRef): File = File(File(photosDir, project.id), photo.fileName)

    private fun projectFile(id: String) = File(projectsDir, "$id.json")

    private fun importPhoto(uri: Uri, dir: File): PhotoRef? = runCatching {
        val resolver = appContext.contentResolver
        val ext = resolver.getType(uri)?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) } ?: "img"
        val id = UUID.randomUUID().toString()
        val file = File(dir, "$id.$ext")
        resolver.openInputStream(uri)!!.use { input -> file.outputStream().use { input.copyTo(it) } }
        val (w, h) = displaySize(file) ?: run { file.delete(); return@runCatching null }
        PhotoRef(id = id, fileName = file.name, width = w, height = h)
    }.getOrNull()

    /** Pixel size as the photo is meant to be seen, i.e. with EXIF rotation applied. */
    private fun displaySize(file: File): Pair<Int, Int>? {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, opts)
        if (opts.outWidth <= 0 || opts.outHeight <= 0) return null
        val orientation = runCatching {
            ExifInterface(file.path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
        val swap = orientation in setOf(
            ExifInterface.ORIENTATION_ROTATE_90,
            ExifInterface.ORIENTATION_ROTATE_270,
            ExifInterface.ORIENTATION_TRANSPOSE,
            ExifInterface.ORIENTATION_TRANSVERSE,
        )
        return if (swap) opts.outHeight to opts.outWidth else opts.outWidth to opts.outHeight
    }
}
