package com.alchemedia.collagemaker

import android.app.Application
import com.alchemedia.collagemaker.data.ProjectRepository

class CollageApp : Application() {
    val repository: ProjectRepository by lazy { ProjectRepository(this) }
}
