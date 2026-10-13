package com.alchemedia.collage.ui.picker

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alchemedia.collage.data.ProjectRepository
import com.alchemedia.collage.model.GridLayouts
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NewCollageState(
    val photos: List<Uri> = emptyList(),
    val layoutId: String = GridLayouts.DEFAULT_ID,
    val creating: Boolean = false,
    val importFailed: Boolean = false,
)

class NewCollageViewModel(private val repository: ProjectRepository) : ViewModel() {
    private val _state = MutableStateFlow(NewCollageState())
    val state: StateFlow<NewCollageState> = _state.asStateFlow()

    fun onPhotosPicked(uris: List<Uri>) {
        if (uris.isEmpty()) return
        val photos = uris.take(MAX_PHOTOS)
        _state.update {
            it.copy(photos = photos, layoutId = GridLayouts.suggestFor(photos.size).id, importFailed = false)
        }
    }

    fun onLayoutSelected(id: String) = _state.update { it.copy(layoutId = id) }

    fun create(onCreated: (String) -> Unit) {
        val current = _state.value
        if (current.photos.isEmpty() || current.creating) return
        _state.update { it.copy(creating = true, importFailed = false) }
        viewModelScope.launch {
            val project = repository.createFromUris(current.photos, current.layoutId)
            if (project == null) {
                _state.update { it.copy(creating = false, importFailed = true) }
            } else {
                _state.update { it.copy(creating = false) }
                onCreated(project.id)
            }
        }
    }

    companion object {
        const val MAX_PHOTOS = 50
    }
}
