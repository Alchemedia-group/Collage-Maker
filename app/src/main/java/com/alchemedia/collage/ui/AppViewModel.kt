package com.alchemedia.collage.ui

import com.alchemedia.collage.Strings
import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.alchemedia.collage.CollageApp
import com.alchemedia.collage.R
import com.alchemedia.collage.model.AspectPreset
import com.alchemedia.collage.model.CollageProject
import com.alchemedia.collage.model.HistoryEntry
import com.alchemedia.collage.model.Layout
import com.alchemedia.collage.model.LayoutCatalog
import com.alchemedia.collage.model.PhotoRef
import com.alchemedia.collage.model.Samples
import com.alchemedia.collage.ui.components.ToastMessage
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Where "pick a layout" was started from, which decides what choosing one does. */
enum class BrowseTarget { Home, New, Editor, Designer }

sealed interface Screen {
    data object Home : Screen
    data object New : Screen
    data class Editor(val projectId: String) : Screen
}

sealed interface Overlay {
    data class Browse(val target: BrowseTarget) : Overlay
    data class Designer(val editId: String?, val baseId: String?, val target: BrowseTarget) : Overlay
    data object History : Overlay
}

sealed interface Sheet {
    data object Pro : Sheet
    data object Language : Sheet
    data object About : Sheet
    data object Export : Sheet
    data class Card(val projectId: String) : Sheet
    data class HistoryItem(val entryId: String) : Sheet
}

/** A photo that can be chosen on the New collage screen: one from the device or one of the built-in samples. */
class PickItem(val id: String, val uri: Uri?, val sample: PhotoRef?) {
    val model: Any get() = uri ?: sampleDrawable(sample!!)
}

fun sampleDrawable(ref: PhotoRef): Int = when (ref.fileName.removePrefix("res:")) {
    "photo_moraine" -> R.drawable.photo_moraine
    "photo_beach" -> R.drawable.photo_beach
    "photo_milky" -> R.drawable.photo_milky
    "photo_wheat" -> R.drawable.photo_wheat
    "photo_forest" -> R.drawable.photo_forest
    else -> R.drawable.photo_boat
}

const val MAX_PHOTOS = 12

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as CollageApp
    val store = app.store
    val photos = app.photos
    val settings = app.settings

    var screen by mutableStateOf<Screen>(Screen.Home)
        private set
    val overlays: SnapshotStateList<Overlay> = mutableStateListOf()
    val sheets: SnapshotStateList<Sheet> = mutableStateListOf()
    var toast by mutableStateOf<ToastMessage?>(null)
        private set
    /** The layout category chip that is selected in every picker. */
    var category by mutableStateOf("fit")
    /** Bumped when My layouts change, so pickers refresh. */
    var mineVersion by mutableIntStateOf(0)
        private set

    // ------------------------------------------------------------ New collage
    val available: SnapshotStateList<PickItem> = mutableStateListOf()
    val selection: SnapshotStateList<String> = mutableStateListOf()
    var newLayoutId by mutableStateOf(LayoutCatalog.DEFAULT_ID)
    var newAspect by mutableStateOf(AspectPreset.Square)
    var creating by mutableStateOf(false)
        private set

    /** Small previews of every photo on the New collage screen, keyed by [PickItem.id]. */
    val thumbs = mutableStateMapOf<String, Bitmap>()

    var editor: EditorState? by mutableStateOf(null)
        private set

    private var toastJob: Job? = null

    init {
        viewModelScope.launch { store.load(); mineVersion++ }
    }

    // ------------------------------------------------------------ navigation

    val canGoBack: Boolean get() = sheets.isNotEmpty() || overlays.isNotEmpty() || screen != Screen.Home

    /** Handles the system back button / gesture. Returns false when nothing was left to close. */
    fun back(): Boolean {
        if (sheets.isNotEmpty()) { sheets.removeAt(sheets.lastIndex); return true }
        if (overlays.isNotEmpty()) { overlays.removeAt(overlays.lastIndex); return true }
        if (editor?.handleBack() == true) return true
        if (screen != Screen.Home) { goHome(); return true }
        return false
    }

    fun goHome() {
        editor?.flush()
        editor = null
        screen = Screen.Home
        overlays.clear()
        sheets.clear()
    }

    fun openBrowse(target: BrowseTarget) { overlays.add(Overlay.Browse(target)) }
    fun closeOverlay() { if (overlays.isNotEmpty()) overlays.removeAt(overlays.lastIndex) }
    fun openHistory() { overlays.add(Overlay.History) }
    fun openDesigner(target: BrowseTarget, editId: String? = null, baseId: String? = null) { overlays.add(Overlay.Designer(editId, baseId, target)) }
    fun openSheet(sheet: Sheet) { sheets.add(sheet) }
    fun closeSheet() { if (sheets.isNotEmpty()) sheets.removeAt(sheets.lastIndex) }
    fun closeSheets() { sheets.clear() }

    fun showToast(text: String, actionLabel: String? = null, action: (() -> Unit)? = null) {
        val message = ToastMessage(text, actionLabel, action)
        toast = message
        toastJob?.cancel()
        toastJob = viewModelScope.launch {
            delay(if (action != null) 6500 else 3200)
            if (toast?.id == message.id) toast = null
        }
    }

    fun dismissToast() { toast = null; toastJob?.cancel() }

    // ------------------------------------------------------------ New collage flow

    private fun ensureSamples() {
        Samples.all.forEach { s ->
            if (available.none { it.id == s.id }) {
                val item = PickItem(s.id, null, s)
                available.add(item)
                loadThumb(item)
            }
        }
    }

    private fun loadThumb(item: PickItem) {
        viewModelScope.launch {
            val bmp = withContext(Dispatchers.IO) {
                if (item.sample != null) photos.load("pick", item.sample, 360) else photos.decodeUri(item.uri!!, 360)
            }
            if (bmp != null) thumbs[item.id] = bmp
        }
    }

    /** Opens New collage with [layoutId] chosen and, when [samples] is true, as many sample photos picked as it has cells. */
    fun startNew(layoutId: String = LayoutCatalog.DEFAULT_ID, samples: Boolean = false) {
        ensureSamples()
        selection.clear()
        val layout = LayoutCatalog.byId(layoutId)
        if (samples) Samples.all.take(minOf(layout.cells.size, MAX_PHOTOS)).forEach { selection.add(it.id) }
        newLayoutId = layout.id
        newAspect = AspectPreset.fromLabel(layout.ar)
        overlays.clear(); sheets.clear()
        screen = Screen.New
    }

    fun togglePick(id: String) {
        if (selection.remove(id)) { suggestForSelection(); return }
        if (selection.size >= MAX_PHOTOS) { showToast(Strings.get(R.string.max_photos, MAX_PHOTOS)); return }
        selection.add(id)
        suggestForSelection()
    }

    private fun suggestForSelection() {
        val l = LayoutCatalog.suggestFor(selection.size)
        newLayoutId = l.id
        newAspect = AspectPreset.fromLabel(l.ar)
    }

    fun pickLayoutForNew(id: String) {
        val l = LayoutCatalog.byId(id)
        newLayoutId = l.id
        newAspect = AspectPreset.fromLabel(l.ar)
    }

    /** Photos chosen with the system picker join the grid and are selected. */
    fun addDevicePhotos(uris: List<Uri>) {
        var added = 0
        uris.forEach { uri ->
            if (available.none { it.uri == uri }) {
                val item = PickItem("u_" + java.util.UUID.randomUUID().toString().take(8), uri, null)
                available.add(0, item)
                loadThumb(item)
                if (selection.size < MAX_PHOTOS) { selection.add(item.id); added++ }
            }
        }
        if (added > 0) {
            suggestForSelection()
            showToast(Strings.plural(R.plurals.p_added_photos, added))
        }
    }

    fun createCollage() {
        if (selection.isEmpty() || creating) return
        creating = true
        val chosen = selection.mapNotNull { id -> available.firstOrNull { it.id == id } }
        val layout = LayoutCatalog.byId(newLayoutId)
        val aspect = newAspect
        viewModelScope.launch {
            val id = CollageProject.newId()
            val refs = mutableListOf<PhotoRef>()
            chosen.forEach { item ->
                if (item.sample != null) refs.add(item.sample)
                else refs.addAll(store.importPhotos(id, listOf(item.uri!!)))
            }
            creating = false
            if (refs.isEmpty()) {
                showToast(Strings.get(R.string.added_photos_fail))
                return@launch
            }
            val project = store.save(CollageProject.create(id, System.currentTimeMillis(), refs, layout, aspect))
            openEditor(project)
        }
    }

    // ------------------------------------------------------------ editor

    fun openEditor(project: CollageProject) {
        editor?.flush()
        editor = EditorState(this, project)
        overlays.clear(); sheets.clear()
        screen = Screen.Editor(project.id)
    }

    fun openProject(id: String) {
        val p = store.projects.value.firstOrNull { it.id == id } ?: return
        openEditor(p)
    }

    // ------------------------------------------------------------ saved collages

    fun duplicateProject(id: String) {
        val p = store.projects.value.firstOrNull { it.id == id } ?: return
        viewModelScope.launch { store.duplicate(p); showToast(Strings.get(R.string.duplicated)) }
    }

    fun deleteProject(id: String) {
        viewModelScope.launch {
            val (index, gone) = store.delete(id) ?: return@launch
            showToast(Strings.get(R.string.deleted_collage, gone.name.ifBlank { Strings.get(R.string.untitled) }), Strings.get(R.string.undo)) {
                viewModelScope.launch { store.restore(index, gone) }
            }
        }
    }

    // ------------------------------------------------------------ history

    fun deleteHistory(entryId: String) {
        viewModelScope.launch {
            val (index, entry) = store.deleteHistory(entryId) ?: return@launch
            sheets.removeAll { it is Sheet.HistoryItem }
            showToast(Strings.get(R.string.removed_history), Strings.get(R.string.undo)) { viewModelScope.launch { store.restoreHistory(index, entry) } }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            val all = store.clearHistory()
            if (all.isEmpty()) return@launch
            showToast(Strings.get(R.string.history_cleared), Strings.get(R.string.undo)) { viewModelScope.launch { store.restoreAllHistory(all) } }
        }
    }

    /** Opens a copy of a saved collage so the version in History stays as it was. */
    fun editAgain(entry: HistoryEntry) {
        viewModelScope.launch {
            val copy = store.duplicate(entry.project, nameSuffix = null)
            openEditor(copy)
            showToast(Strings.get(R.string.opened_copy))
        }
    }

    // ------------------------------------------------------------ my layouts

    /** Layout chosen in the browser while the designer is open ("Start from…"). The designer picks it up. */
    var designerBase by mutableStateOf<String?>(null)

    fun saveLayout(layout: Layout, then: () -> Unit = {}) {
        viewModelScope.launch {
            store.saveLayout(layout)
            mineVersion++
            // Collages using an edited layout need their cells brought back in line.
            editor?.layoutsChanged()
            then()
        }
    }

    fun deleteLayout(id: String, then: () -> Unit = {}) {
        viewModelScope.launch {
            store.deleteLayout(id)
            mineVersion++
            editor?.layoutsChanged()
            then()
        }
    }

    override fun onCleared() {
        editor?.flush()
    }
}
