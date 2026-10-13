package com.alchemedia.collage.model

import kotlinx.serialization.Serializable

/** One saved collage: a snapshot of the project at the moment it was saved, and how it was exported. */
@Serializable
data class HistoryEntry(
    val id: String,
    val time: Long,
    val width: Int,
    val height: Int,
    val watermark: Boolean,
    val project: CollageProject,
)
