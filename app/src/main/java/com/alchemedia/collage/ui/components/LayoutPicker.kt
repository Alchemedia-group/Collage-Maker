package com.alchemedia.collage.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alchemedia.collage.model.Layout
import com.alchemedia.collage.model.LayoutCatalog
import com.alchemedia.collage.model.LayoutCategories
import com.alchemedia.collage.ui.theme.CTheme

/** The layout glyph on a rounded tile. Selected tiles turn accent blue. */
@Composable
fun LayoutTile(layout: Layout, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, size: androidx.compose.ui.unit.Dp = 56.dp, labelSize: Int = 11) {
    val c = CTheme.colors
    Column(
        modifier.width(size + 6.dp).pressable(onClick, role = Role.RadioButton, label = "${layout.name}, ${layout.cells.size} photos")
            .semantics { this.selected = selected },
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Box(
            Modifier.size(size).clip(RoundedCornerShape(14.dp)).background(if (selected) c.accent else lerpColor(c.card, c.ink, .07f)).padding(6.dp),
        ) {
            LayoutIcon(layout, if (selected) c.onAccent else lerpColor(c.card, c.ink, .52f), Modifier.size(size - 12.dp))
        }
        CText(layout.name, size = labelSize.sp, weight = 600, color = if (selected) c.ink else c.muted, maxLines = 1, align = TextAlign.Center)
    }
}

/** The dashed "Design" tile at the start of the My layouts row. */
@Composable
fun DesignTile(onClick: () -> Unit, modifier: Modifier = Modifier, size: androidx.compose.ui.unit.Dp = 56.dp) {
    val c = CTheme.colors
    Column(modifier.width(size + 6.dp).pressable(onClick, label = "Design a new layout"), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(
            Modifier.size(size).clip(RoundedCornerShape(14.dp)).drawBehind {
                drawRoundRect(c.line, Offset(1f, 1f), Size(this.size.width - 2f, this.size.height - 2f), CornerRadius(14.dp.toPx()), style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f))))
            },
            contentAlignment = Alignment.Center,
        ) { CIcon(Icon.Add, size = 26.dp, tint = c.accent, strokeWidth = 2.4f) }
        CText("Design", size = 11.sp, weight = 600, color = c.muted)
    }
}

/**
 * Category chips, an All button, and one scrolling row of layout tiles. Used on New collage and in the editor.
 */
@Composable
fun LayoutPicker(
    category: String,
    onCategory: (String) -> Unit,
    photoCount: Int,
    selectedId: String,
    onPick: (String) -> Unit,
    onBrowse: () -> Unit,
    onDesign: () -> Unit,
    modifier: Modifier = Modifier,
    mineVersion: Int = 0,
) {
    val c = CTheme.colors
    val layouts = remember(category, photoCount, mineVersion) { LayoutCatalog.forCategory(category, photoCount) }
    val chips = rememberLazyListState()
    val tiles = rememberLazyListState()
    LaunchedEffect(category) {
        val i = LayoutCategories.all.indexOfFirst { it.first == category }
        if (i >= 0) runCatching { chips.centerOn(i) }
    }
    LaunchedEffect(category, selectedId, layouts) {
        val i = layouts.indexOfFirst { it.id == selectedId }
        val offset = if (category == "mine") 1 else 0
        if (i >= 0) runCatching { tiles.centerOn(i + offset) } else runCatching { tiles.scrollToItem(0) }
    }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LazyRow(Modifier.weight(1f).edgeFade(chips), state = chips, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(LayoutCategories.all, key = { it.first }) { (id, name) -> TabChip(name, id == category, { onCategory(id) }) }
            }
            Row(
                Modifier.height(34.dp).clip(CircleShape).background(c.tint).pressable(onBrowse, label = "Browse all layouts").padding(start = 11.dp, end = 13.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                CIcon(Icon.AllLayouts, size = 16.dp, tint = c.onTint)
                CText("All", size = 13.sp, weight = 800, color = c.onTint)
            }
        }
        LazyRow(Modifier.fillMaxWidth().edgeFade(tiles), state = tiles, contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 14.dp, end = 14.dp, top = 2.dp, bottom = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (category == "mine") item(key = "design") { DesignTile(onDesign) }
            if (layouts.isEmpty() && category != "mine") item(key = "none") { CText("Nothing here yet.", Modifier.padding(vertical = 14.dp), size = 13.sp, color = c.muted) }
            items(layouts, key = { it.id }) { l -> LayoutTile(l, l.id == selectedId, { onPick(l.id) }) }
        }
    }
}


/** Scrolls so the item at [index] sits in the middle of the row, as far as the row allows. */
private suspend fun androidx.compose.foundation.lazy.LazyListState.centerOn(index: Int) {
    animateScrollToItem(index)
    val info = layoutInfo
    val item = info.visibleItemsInfo.firstOrNull { it.index == index } ?: return
    val center = info.viewportStartOffset + (info.viewportEndOffset - info.viewportStartOffset) / 2
    val delta = item.offset + item.size / 2 - center
    if (kotlin.math.abs(delta) > 1) animateScrollBy(delta.toFloat())
}
