package com.alchemedia.collage.ui.home

import com.alchemedia.collage.Strings
import com.alchemedia.collage.R
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alchemedia.collage.model.CountFilter
import com.alchemedia.collage.model.Layout
import com.alchemedia.collage.model.LayoutCatalog
import com.alchemedia.collage.model.LayoutCategories
import com.alchemedia.collage.ui.AppViewModel
import com.alchemedia.collage.ui.BrowseTarget
import com.alchemedia.collage.ui.components.AppBar
import com.alchemedia.collage.ui.components.BarTitle
import com.alchemedia.collage.ui.components.CButton
import com.alchemedia.collage.ui.components.CIcon
import com.alchemedia.collage.ui.components.CText
import com.alchemedia.collage.ui.components.CTextField
import com.alchemedia.collage.ui.components.CountBadge
import com.alchemedia.collage.ui.components.FilterChip
import com.alchemedia.collage.ui.components.Icon
import com.alchemedia.collage.ui.components.LayoutIcon
import com.alchemedia.collage.ui.components.SearchField
import com.alchemedia.collage.ui.components.BtnStyle
import com.alchemedia.collage.ui.components.edgeFade
import com.alchemedia.collage.ui.components.lerpColor
import com.alchemedia.collage.ui.components.pressable
import com.alchemedia.collage.ui.theme.CTheme

private sealed interface BrowseRow {
    data class Header(val title: String, val count: Int) : BrowseRow
    data class Tiles(val layouts: List<Layout>) : BrowseRow
}

/** Every layout in one place: search, filter by photo count and category, type your own word. */
@Composable
fun BrowseScreen(vm: AppViewModel, target: BrowseTarget, selectedId: String?, onChoose: (String) -> Unit) {
    val c = CTheme.colors
    var query by remember { mutableStateOf("") }
    var count by remember { mutableStateOf(CountFilter.Any) }
    var cat by remember { mutableStateOf<String?>(null) }
    var word by remember { mutableStateOf("") }

    val results = remember(query, count, cat, vm.mineVersion) { LayoutCatalog.search(query, count, cat) }
    val rows = remember(results) {
        buildList {
            LayoutCategories.browsable.forEach { (id, _) ->
                val items = results.filter { it.cat == id }
                if (items.isNotEmpty()) {
                    add(BrowseRow.Header(LayoutCategories.title(id), items.size))
                    items.chunked(3).forEach { add(BrowseRow.Tiles(it)) }
                }
            }
        }
    }
    val showWord = cat == null || cat == "letters"
    val countState = rememberLazyListState()
    val catState = rememberLazyListState()

    fun useWord() {
        val layout = LayoutCatalog.wordLayout(word)
        if (layout == null) { vm.showToast(Strings.get(R.string.word_hint)); return }
        word = ""
        onChoose(layout.id)
    }

    Column(Modifier.fillMaxSize().background(c.paper).statusBarsPadding().imePadding()) {
        AppBar(onBack = { vm.closeOverlay() }, backIcon = Icon.Close, backLabel = Strings.get(R.string.close)) {
            BarTitle(Strings.get(R.string.all_layouts), Modifier.weight(1f))
            CountBadge(results.size.toString())
        }
        Box(Modifier.padding(horizontal = 16.dp)) { SearchField(query, { query = it }, Strings.get(R.string.search_hint)) }
        FieldLabel(Strings.get(R.string.label_how_many))
        LazyRow(Modifier.fillMaxWidth().edgeFade(countState), state = countState, contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(CountFilter.entries) { f -> FilterChip(f.title, count == f, { count = f }) }
        }
        LazyRow(Modifier.fillMaxWidth().padding(top = 10.dp).edgeFade(catState), state = catState, contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            item { FilterChip(Strings.get(R.string.all), cat == null, { cat = null }) }
            items(LayoutCategories.browsable) { (id, _) -> FilterChip(LayoutCategories.label(id), cat == id, { cat = id }) }
        }
        if (showWord) {
            val shape = RoundedCornerShape(16.dp)
            Row(
                Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp).fillMaxWidth().clip(shape).background(c.card).dashedBorder(c.line, 16).padding(10.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CText("Your\nword", size = 12.sp, weight = 700, color = c.muted, lineHeight = 14.sp)
                Box(Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(11.dp)).background(c.paper).border(1.dp, c.line, RoundedCornerShape(11.dp)).padding(horizontal = 12.dp), contentAlignment = Alignment.CenterStart) {
                    CTextField(
                        word, { word = it.uppercase().filter { ch -> ch.isLetterOrDigit() || ch == '*' } }, Modifier.fillMaxWidth(), "MOM, 2026, BFF",
                        textStyle = TextStyle(fontFamily = CTheme.fonts.body, fontSize = 15.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp),
                        maxLength = 8, capitalization = KeyboardCapitalization.Characters, onDone = ::useWord,
                    )
                }
                CButton(Strings.get(R.string.use), ::useWord, small = true)
            }
        }
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 28.dp)) {
            if (target != BrowseTarget.Designer) {
                item(key = "design") {
                    Row(
                        Modifier.padding(top = 13.dp, bottom = 2.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(c.card).dashedBorder(c.line, 18)
                            .pressable({ vm.closeOverlay(); vm.openDesigner(target) }, label = Strings.get(R.string.design_own_layout)).padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(c.tint), contentAlignment = Alignment.Center) { CIcon(Icon.Pencil, tint = c.onTint) }
                        Column {
                            CText(Strings.get(R.string.design_own_layout), size = 16.sp, weight = 700, display = true)
                            CText(Strings.get(R.string.design_own_sub), size = 12.sp, color = c.muted)
                        }
                    }
                }
            }
            if (rows.isEmpty()) {
                item(key = "empty") {
                    Column(Modifier.fillMaxWidth().padding(vertical = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        CText(if (cat == "mine") Strings.get(R.string.no_saved_layouts) else Strings.get(R.string.no_layouts_match), size = 22.sp, weight = 700, display = true)
                        CText(
                            if (cat == "mine") Strings.get(R.string.tap_design_above) else Strings.get(R.string.try_another_word),
                            Modifier.padding(top = 6.dp), size = 14.sp, color = c.muted,
                        )
                    }
                }
            }
            items(rows.size, key = { i -> rows[i].hashCode() * 31 + i }) { i ->
                when (val row = rows[i]) {
                    is BrowseRow.Header -> Row(Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 8.dp, start = 2.dp, end = 2.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                        CText(row.title, size = 18.sp, weight = 700, display = true, letterSpacing = (-.4).sp)
                        CText(Strings.plural(R.plurals.p_layouts, row.count), size = 12.sp, color = c.muted)
                    }
                    is BrowseRow.Tiles -> Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.layouts.forEach { l ->
                            BrowseTile(l, l.id == selectedId, Modifier.weight(1f), onClick = { onChoose(l.id) }, onEdit = { vm.closeOverlay(); vm.openDesigner(target, editId = l.id) })
                        }
                        repeat(3 - row.layouts.size) { Box(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun FieldLabel(text: String) = CText(text, Modifier.padding(start = 18.dp, top = 12.dp, bottom = 10.dp), size = 11.sp, weight = 700, color = CTheme.colors.muted, letterSpacing = 1.sp)

@Composable
private fun BrowseTile(layout: Layout, selected: Boolean, modifier: Modifier, onClick: () -> Unit, onEdit: () -> Unit) {
    val c = CTheme.colors
    Box(modifier) {
        Column(
            Modifier.fillMaxWidth().pressable(onClick, role = Role.RadioButton, label = Strings.get(R.string.layout_photos_desc, layout.title, Strings.plural(R.plurals.p_photos, layout.cells.size))).semantics { this.selected = selected },
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(20.dp)).background(if (selected) c.accent else lerpColor(c.card, c.ink, .07f)).padding(12.dp)) {
                LayoutIcon(layout, if (selected) c.onAccent else lerpColor(c.card, c.ink, .52f), Modifier.fillMaxSize())
            }
            Column(Modifier.padding(horizontal = 2.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                CText(layout.title, size = 13.sp, weight = 700, maxLines = 1)
                CText(Strings.plural(R.plurals.p_photos, layout.cells.size), size = 11.sp, color = c.muted)
            }
        }
        if (layout.mine) {
            Box(
                Modifier.align(Alignment.TopEnd).padding(6.dp).size(28.dp).shadow(3.dp, CircleShape).clip(CircleShape).background(c.card).pressable(onEdit, label = Strings.get(R.string.edit_item, layout.title)),
                contentAlignment = Alignment.Center,
            ) { CIcon(Icon.Pencil, size = 15.dp) }
        }
    }
}

fun Modifier.dashedBorder(color: Color, radius: Int): Modifier = this.drawWithContent {
    drawContent()
    drawRoundRect(color, Offset(1f, 1f), Size(size.width - 2f, size.height - 2f), CornerRadius(radius.dp.toPx()), style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 12f))))
}
