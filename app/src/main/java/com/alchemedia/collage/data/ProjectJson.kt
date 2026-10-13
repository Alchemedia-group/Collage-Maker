package com.alchemedia.collage.data

import com.alchemedia.collage.model.CollageProject
import kotlinx.serialization.json.Json

/** Shared JSON codec for project files. Unknown keys are ignored so newer files still open. */
object ProjectJson {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(project: CollageProject): String = json.encodeToString(CollageProject.serializer(), project)

    fun decode(text: String): CollageProject = json.decodeFromString(CollageProject.serializer(), text)
}
