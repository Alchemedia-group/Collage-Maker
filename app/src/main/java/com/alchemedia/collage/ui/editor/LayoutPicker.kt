package com.alchemedia.collage.ui.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.alchemedia.collage.R
import com.alchemedia.collage.model.GridLayout
import com.alchemedia.collage.model.GridLayouts

@Composable
fun LayoutPicker(
    selectedId: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp),
) {
    LazyRow(
        modifier = modifier,
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(GridLayouts.all, key = { it.id }) { layout ->
            LayoutOption(layout, selected = layout.id == selectedId, onClick = { onSelect(layout.id) })
        }
    }
}

@Composable
private fun LayoutOption(layout: GridLayout, selected: Boolean, onClick: () -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val shape = RoundedCornerShape(10.dp)
    val description = stringResource(R.string.cd_layout_option, layout.cells.size)
    Canvas(
        modifier = Modifier
            .size(56.dp)
            .clip(shape)
            .border(if (selected) 2.dp else 1.dp, if (selected) primary else muted.copy(alpha = 0.4f), shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(6.dp)
            .semantics { contentDescription = description },
    ) {
        val inset = 1.5.dp.toPx()
        layout.cells.forEach { cell ->
            drawRoundRect(
                color = if (selected) primary else muted,
                topLeft = Offset(cell.x * size.width + inset, cell.y * size.height + inset),
                size = Size(cell.w * size.width - 2 * inset, cell.h * size.height - 2 * inset),
                cornerRadius = CornerRadius(2.dp.toPx()),
            )
        }
    }
}
