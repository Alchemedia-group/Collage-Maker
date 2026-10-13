package com.alchemedia.collagemaker.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.alchemedia.collagemaker.R
import com.alchemedia.collagemaker.model.CollageGeometry
import com.alchemedia.collagemaker.model.CollageProject
import com.alchemedia.collagemaker.model.PhotoRef
import java.io.File

/**
 * Read-only rendering of a grid collage. Pan and zoom per cell arrive with the grid editor milestone,
 * so for now every photo is simply centre-cropped into its cell.
 */
@Composable
fun CollagePreview(
    project: CollageProject,
    photoFile: (PhotoRef) -> File,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(R.string.cd_collage_preview)
    BoxWithConstraints(
        modifier = modifier
            .aspectRatio(project.aspect.ratio)
            .background(Color(project.background))
            .semantics { contentDescription = description },
    ) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        val rects = CollageGeometry.cellRects(
            layout = project.layout,
            width = widthPx,
            height = heightPx,
            gapPercent = project.gapPercent,
            marginPercent = project.marginPercent,
        )
        rects.forEachIndexed { index, rect ->
            val radius = with(density) { CollageGeometry.cornerRadius(rect, project.cornerPercent).toDp() }
            val cellModifier = Modifier
                .offset(with(density) { rect.x.toDp() }, with(density) { rect.y.toDp() })
                .size(with(density) { rect.width.toDp() }, with(density) { rect.height.toDp() })
                .clip(RoundedCornerShape(radius))
            val photo = project.photo(project.cells.getOrNull(index)?.photoId)
            if (photo != null) {
                AsyncImage(
                    model = photoFile(photo),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = cellModifier,
                )
            } else {
                Box(cellModifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)))
            }
        }
    }
}
