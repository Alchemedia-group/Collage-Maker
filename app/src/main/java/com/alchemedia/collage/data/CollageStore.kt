package com.alchemedia.collage.data

import com.alchemedia.collage.Strings
import com.alchemedia.collage.R
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.exifinterface.media.ExifInterface
import com.alchemedia.collage.model.AspectPreset
import com.alchemedia.collage.model.CollageProject
import com.alchemedia.collage.model.HistoryEntry
import com.alchemedia.collage.model.Layout
import com.alchemedia.collage.model.LayoutCatalog
import com.alchemedia.collage.model.PhotoRef
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import java.io.File
import java.util.UUID

/**
 * Everything the user has made, kept in app-private storage: projects (one JSON file each), the history of saved
 * collages, their own layouts, and the photos they imported. Anything read back is validated so one bad record
 * can never stop the app from starting.
 */
class CollageStore(context: Context) {
    private val appContext = context.applicationContext
    private val projectsDir = File(appContext.filesDir, "projects")
    private val photosDir = File(appContext.filesDir, "photos")
    private val historyFile = File(appContext.filesDir, "history.json")
    private val layoutsFile = File(appContext.filesDir, "my_layouts.json")
    private val lock = Mutex()
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private val _projects = MutableStateFlow<List<CollageProject>>(emptyList())
    val projects: StateFlow<List<CollageProject>> = _projects.asStateFlow()
    private val _history = MutableStateFlow<List<HistoryEntry>>(emptyList())
    val history: StateFlow<List<HistoryEntry>> = _history.asStateFlow()
    private val _myLayouts = MutableStateFlow<List<Layout>>(emptyList())
    val myLayouts: StateFlow<List<Layout>> = _myLayouts.asStateFlow()
    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    suspend fun load() = withContext(Dispatchers.IO) {
        lock.withLock {
            val layouts = decodeList(layoutsFile, Layout.serializer()).mapNotNull { sanitize(it) }
            LayoutCatalog.setMine(layouts)
            _myLayouts.value = layouts
            _projects.value = projectsDir.listFiles { f -> f.extension == "json" }.orEmpty()
                .mapNotNull { runCatching { json.decodeFromString(CollageProject.serializer(), it.readText()).normalized() }.getOrNull() }
                .sortedByDescending { it.updatedAt }
            _history.value = decodeList(historyFile, HistoryEntry.serializer())
                .map { it.copy(project = it.project.normalized()) }
                .sortedByDescending { it.time }
            purgeOrphanPhotos()
            _loaded.value = true
        }
    }

    // ---------------------------------------------------------------- projects

    suspend fun get(id: String): CollageProject? = _projects.value.firstOrNull { it.id == id }

    suspend fun save(project: CollageProject): CollageProject = withContext(Dispatchers.IO) {
        lock.withLock {
            val stamped = project.copy(updatedAt = System.currentTimeMillis())
            projectsDir.mkdirs()
            val tmp = File(projectsDir, "${project.id}.json.tmp")
            tmp.writeText(json.encodeToString(CollageProject.serializer(), stamped))
            check(tmp.renameTo(projectFile(project.id))) { "Could not save project ${project.id}" }
            _projects.value = (listOf(stamped) + _projects.value.filterNot { it.id == project.id }).sortedByDescending { it.updatedAt }
            stamped
        }
    }

    /** Removes the project from the list. Its photos stay until the next start so Undo can bring it back. */
    suspend fun delete(id: String): Pair<Int, CollageProject>? = withContext(Dispatchers.IO) {
        lock.withLock {
            val index = _projects.value.indexOfFirst { it.id == id }
            if (index < 0) return@withLock null
            val gone = _projects.value[index]
            projectFile(id).delete()
            _projects.value = _projects.value.filterNot { it.id == id }
            index to gone
        }
    }

    suspend fun restore(index: Int, project: CollageProject) = withContext(Dispatchers.IO) {
        lock.withLock {
            projectsDir.mkdirs()
            projectFile(project.id).writeText(json.encodeToString(CollageProject.serializer(), project))
            val list = _projects.value.filterNot { it.id == project.id }.toMutableList()
            list.add(index.coerceIn(0, list.size), project)
            _projects.value = list
        }
    }

    /** A copy with its own id and its own copy of the photos. */
    suspend fun duplicate(source: CollageProject, nameSuffix: String? = Strings.get(R.string.copy_suffix)): CollageProject = withContext(Dispatchers.IO) {
        val copy = source.normalized().copy(
            id = CollageProject.newId(),
            name = if (nameSuffix != null) (source.name.ifBlank { Strings.get(R.string.untitled) }) + nameSuffix else source.name,
            createdAt = System.currentTimeMillis(),
        )
        File(photosDir, source.id).takeIf { it.exists() }?.copyRecursively(File(photosDir, copy.id), overwrite = true)
        save(copy)
    }

    /** Creates a project from picked photos. Returns null if none of them could be imported. */
    suspend fun createFromUris(uris: List<Uri>, layout: Layout?, aspect: AspectPreset = AspectPreset.Square): CollageProject? =
        withContext(Dispatchers.IO) {
            val id = CollageProject.newId()
            val photos = importPhotos(id, uris)
            if (photos.isEmpty()) {
                File(photosDir, id).deleteRecursively()
                return@withContext null
            }
            save(CollageProject.create(id, System.currentTimeMillis(), photos, layout ?: LayoutCatalog.suggestFor(photos.size), aspect))
        }

    suspend fun importPhotos(projectId: String, uris: List<Uri>): List<PhotoRef> = withContext(Dispatchers.IO) {
        val dir = File(photosDir, projectId).apply { mkdirs() }
        uris.mapNotNull { importPhoto(it, dir) }
    }

    fun photoFile(projectId: String, photo: PhotoRef): File = File(File(photosDir, projectId), photo.fileName)

    // ---------------------------------------------------------------- history

    suspend fun recordHistory(project: CollageProject, width: Int, height: Int, watermark: Boolean): HistoryEntry = withContext(Dispatchers.IO) {
        lock.withLock {
            val entry = HistoryEntry(UUID.randomUUID().toString(), System.currentTimeMillis(), width, height, watermark, project.normalized())
            _history.value = (listOf(entry) + _history.value).take(60)
            persistHistory()
            entry
        }
    }

    suspend fun deleteHistory(id: String): Pair<Int, HistoryEntry>? = withContext(Dispatchers.IO) {
        lock.withLock {
            val index = _history.value.indexOfFirst { it.id == id }
            if (index < 0) return@withLock null
            val gone = _history.value[index]
            _history.value = _history.value.filterNot { it.id == id }
            persistHistory()
            index to gone
        }
    }

    suspend fun restoreHistory(index: Int, entry: HistoryEntry) = withContext(Dispatchers.IO) {
        lock.withLock {
            val list = _history.value.toMutableList()
            list.add(index.coerceIn(0, list.size), entry)
            _history.value = list
            persistHistory()
        }
    }

    suspend fun clearHistory(): List<HistoryEntry> = withContext(Dispatchers.IO) {
        lock.withLock {
            val all = _history.value
            _history.value = emptyList()
            persistHistory()
            all
        }
    }

    suspend fun restoreAllHistory(entries: List<HistoryEntry>) = withContext(Dispatchers.IO) {
        lock.withLock {
            _history.value = entries
            persistHistory()
        }
    }

    // ---------------------------------------------------------------- my layouts

    suspend fun saveLayout(layout: Layout) = withContext(Dispatchers.IO) {
        lock.withLock {
            val clean = sanitize(layout.copy(mine = true, cat = "mine")) ?: return@withLock
            val list = _myLayouts.value.toMutableList()
            val i = list.indexOfFirst { it.id == clean.id }
            if (i >= 0) list[i] = clean else list.add(clean)
            persistLayouts(list)
        }
    }

    suspend fun deleteLayout(id: String) = withContext(Dispatchers.IO) {
        lock.withLock { persistLayouts(_myLayouts.value.filterNot { it.id == id }) }
    }

    // ---------------------------------------------------------------- internals

    private fun persistLayouts(list: List<Layout>) {
        layoutsFile.writeText(json.encodeToString(ListSerializer(Layout.serializer()), list))
        _myLayouts.value = list
        LayoutCatalog.setMine(list)
    }

    private fun persistHistory() {
        historyFile.writeText(json.encodeToString(ListSerializer(HistoryEntry.serializer()), _history.value))
    }

    private fun projectFile(id: String) = File(projectsDir, "$id.json")

    private fun <T> decodeList(file: File, serializer: KSerializer<T>): List<T> {
        if (!file.exists()) return emptyList()
        return runCatching {
            json.parseToJsonElement(file.readText()).jsonArray.mapNotNull { el ->
                runCatching { json.decodeFromJsonElement(serializer, el) }.getOrNull()
            }
        }.getOrDefault(emptyList())
    }

    /** Accepts only layouts whose numbers make sense; cells may run past the canvas edge but not absurdly far. */
    private fun sanitize(layout: Layout): Layout? {
        if (layout.id.isBlank() || layout.cells.isEmpty() || layout.cells.size > 40) return null
        val cells = layout.cells.map { c ->
            if (!c.x.isFinite() || !c.y.isFinite() || !c.w.isFinite() || !c.h.isFinite() || c.w <= 0f || c.h <= 0f) return null
            val shape = c.shape?.takeIf { LayoutCatalog.shapes.containsKey(it) }
            c.copy(x = c.x.coerceIn(-1f, 2f), y = c.y.coerceIn(-1f, 2f), w = c.w.coerceAtMost(3f), h = c.h.coerceAtMost(3f), shape = shape, text = c.text?.take(2)?.takeIf { it.isNotEmpty() })
        }
        return layout.copy(name = layout.name.ifBlank { Strings.get(R.string.my_layout) }.take(40), cat = "mine", mine = true, cells = cells)
    }

    /** Photo folders nobody refers to any more (deleted collages that are not in History). */
    private fun purgeOrphanPhotos() {
        val used = (_projects.value.map { it.id } + _history.value.map { it.project.id }).toSet()
        photosDir.listFiles()?.filter { it.isDirectory && it.name !in used }?.forEach { it.deleteRecursively() }
    }

    private fun importPhoto(uri: Uri, dir: File): PhotoRef? = runCatching {
        val resolver = appContext.contentResolver
        val ext = resolver.getType(uri)?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) } ?: "img"
        val id = UUID.randomUUID().toString()
        val file = File(dir, "$id.$ext")
        resolver.openInputStream(uri)!!.use { input -> file.outputStream().use { input.copyTo(it) } }
        val size = displaySize(file)
        if (size == null) {
            file.delete()
            null
        } else {
            PhotoRef(id = id, fileName = file.name, width = size.first, height = size.second)
        }
    }.getOrNull()

    /** Pixel size as the photo is meant to be seen, i.e. with EXIF rotation applied. */
    private fun displaySize(file: File): Pair<Int, Int>? {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, opts)
        if (opts.outWidth <= 0 || opts.outHeight <= 0) return null
        val swap = exifOrientation(file) in setOf(
            ExifInterface.ORIENTATION_ROTATE_90, ExifInterface.ORIENTATION_ROTATE_270,
            ExifInterface.ORIENTATION_TRANSPOSE, ExifInterface.ORIENTATION_TRANSVERSE,
        )
        return if (swap) opts.outHeight to opts.outWidth else opts.outWidth to opts.outHeight
    }
}

internal fun exifOrientation(file: File): Int = runCatching {
    ExifInterface(file.path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
}.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
