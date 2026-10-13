package com.alchemedia.collage.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alchemedia.collage.data.ProjectRepository
import com.alchemedia.collage.model.CollageProject
import com.alchemedia.collage.model.PhotoRef
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

class HomeViewModel(private val repository: ProjectRepository) : ViewModel() {
    val projects: StateFlow<List<CollageProject>> = repository.projects

    init {
        viewModelScope.launch { repository.refresh() }
    }

    fun photoFile(project: CollageProject, photo: PhotoRef): File = repository.photoFile(project, photo)

    fun delete(id: String) {
        viewModelScope.launch { repository.delete(id) }
    }
}
