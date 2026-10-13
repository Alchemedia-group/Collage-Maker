package com.alchemedia.collage

import android.app.Application
import com.alchemedia.collage.data.ProjectRepository

class CollageApp : Application() {
    val repository: ProjectRepository by lazy { ProjectRepository(this) }
}
