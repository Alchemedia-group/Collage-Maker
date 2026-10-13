package com.alchemedia.collagemaker.ui.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alchemedia.collagemaker.data.ProjectRepository
import com.alchemedia.collagemaker.model.AspectPreset
import com.alchemedia.collagemaker.model.CollageProject
import com.alchemedia.collagemaker.model.PhotoRef
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

sealed interface EditorState {
    data object Loading : EditorState
    data object Missing : EditorState
    data class Ready(val project: CollageProject) : EditorState
}

class EditorViewModel(
    private val repository: ProjectRepository,
    private val projectId: String,
) : ViewModel() {
    private val _state = MutableStateFlow<EditorState>(EditorState.Loading)
    val state: StateFlow<EditorState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val project = repository.get(projectId)
            _state.value = if (project == null) EditorState.Missing else EditorState.Ready(project)
        }
    }

    fun photoFile(project: CollageProject, photo: PhotoRef): File = repository.photoFile(project, photo)

    fun setLayout(layoutId: String) = update { it.withLayout(layoutId) }

    fun setAspect(aspect: AspectPreset) = update { it.copy(aspect = aspect) }

    private fun update(change: (CollageProject) -> CollageProject) {
        val current = (_state.value as? EditorState.Ready)?.project ?: return
        val next = change(current)
        _state.value = EditorState.Ready(next)
        viewModelScope.launch { repository.save(next) }
    }
}
