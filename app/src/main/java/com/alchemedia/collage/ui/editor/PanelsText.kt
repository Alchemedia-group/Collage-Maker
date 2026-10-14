package com.alchemedia.collage.ui.editor

import com.alchemedia.collage.Strings
import com.alchemedia.collage.R
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alchemedia.collage.model.CanvasItem
import com.alchemedia.collage.model.EmojiCatalog
import com.alchemedia.collage.model.FontCatalog
import com.alchemedia.collage.model.FontDef
import com.alchemedia.collage.model.ItemType
import com.alchemedia.collage.model.StickerKind
import com.alchemedia.collage.model.TextColors
import com.alchemedia.collage.model.TextGradients
import com.alchemedia.collage.model.TextPresets
import com.alchemedia.collage.model.TextTemplates
import com.alchemedia.collage.render.Fonts
import com.alchemedia.collage.render.ItemRenderer
import com.alchemedia.collage.render.Shapes
import com.alchemedia.collage.ui.ColorTarget
import com.alchemedia.collage.ui.EditorState
import com.alchemedia.collage.ui.ItemSlider
import com.alchemedia.collage.ui.TextSub
import com.alchemedia.collage.ui.components.BtnStyle
import com.alchemedia.collage.ui.components.CButton
import com.alchemedia.collage.ui.components.CIcon
import com.alchemedia.collage.ui.components.CText
import com.alchemedia.collage.ui.components.ColorRow
import com.alchemedia.collage.ui.components.DoneBtn
import com.alchemedia.collage.ui.components.Icon
import com.alchemedia.collage.ui.components.ScrollRow
import com.alchemedia.collage.ui.components.SliderOne
import com.alchemedia.collage.ui.components.SquareBtn
import com.alchemedia.collage.ui.components.SquareIconBtn
import com.alchemedia.collage.ui.components.TabChip
import com.alchemedia.collage.ui.components.pressable
import com.alchemedia.collage.ui.theme.CTheme
import androidx.compose.ui.text.font.Typeface as ComposeTypeface

private val DARK_TILE = Color(0xFF1F2430)

@Composable
fun TextPanel(
    ed: EditorState, onCustomColor: (Long, (Long) -> Unit) -> Unit,
    favouriteFonts: Set<String> = emptySet(), onToggleFavourite: (String) -> Unit = {},
) {
    val item = ed.selectedItem?.takeIf { it.isText }
    if (item == null) TextHome(ed) else TextEdit(ed, item, onCustomColor, favouriteFonts, onToggleFavourite)
}

@Composable
private fun TextHome(ed: EditorState) {
    val c = CTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CButton(Strings.get(R.string.add_text), { ed.addItem(CanvasItem.text()); ed.focusRequest++ }, small = true, icon = Icon.Add)
            CText(Strings.get(R.string.or_start_style), size = 12.sp, color = c.muted)
        }
        ScrollRow(padding = 14.dp, spacing = 10.dp, tiles = true) {
            TextTemplates.all.forEach { t ->
                val items = remember(t.name) { t.make() }
                Canvas(Modifier.size(120.dp, 58.dp).clip(RoundedCornerShape(12.dp)).pressable({ ed.addTemplate(t.make()) }, label = Strings.get(R.string.add_item, t.title))) {
                    drawRect(Brush.linearGradient(listOf(Color(0xFF3B4252), Color(0xFF1B2029)), Offset.Zero, Offset(size.width, size.height)))
                    val ys = items.map { it.y }
                    val mid = (ys.min() + ys.max()) / 2f
                    drawIntoCanvas { canvas ->
                        items.forEach { i -> ItemRenderer.draw(canvas.nativeCanvas, i.copy(x = .5f, y = .5f + (i.y - mid) * 1.8f, size = i.size * 1.5f), size.width, size.height) }
                    }
                }
            }
        }
    }
}

@Composable
private fun TextEdit(ed: EditorState, item: CanvasItem, onCustomColor: (Long, (Long) -> Unit) -> Unit, favouriteFonts: Set<String>, onToggleFavourite: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (!ed.typing) {
            Row(Modifier.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ScrollRow(Modifier.weight(1f), padding = 0.dp, spacing = 6.dp) {
                    TextSub.entries.forEach { s -> TabChip(s.label, ed.textSub == s, { ed.textSub = s }) }
                }
                DoneBtn(ed::doneEditing)
            }
        }
        when (ed.textSub) {
            TextSub.Text -> TextBox(ed, item)
            TextSub.Font -> FontPanel(ed, item, favouriteFonts, onToggleFavourite)
            TextSub.Color -> ColorPanel(ed, item, onCustomColor)
            TextSub.Style -> StylePanelText(ed, item)
            TextSub.Adjust -> AdjustPanel(ed, item)
        }
    }
}

@Composable
private fun TextBox(ed: EditorState, item: CanvasItem) {
    val c = CTheme.colors
    val focus = remember { FocusRequester() }
    val manager = LocalFocusManager.current
    var value by remember(item.id) { mutableStateOf(TextFieldValue(item.text)) }
    LaunchedEffect(ed.focusRequest) {
        if (ed.focusRequest > 0) {
            // Select everything so typing replaces the placeholder text instead of landing in front of it.
            value = value.copy(selection = androidx.compose.ui.text.TextRange(0, value.text.length))
            runCatching { focus.requestFocus() }
        }
    }
    Row(Modifier.padding(horizontal = 14.dp), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            Modifier.weight(1f).heightIn(min = 44.dp).clip(RoundedCornerShape(14.dp)).background(c.paper).border(1.dp, c.line, RoundedCornerShape(14.dp)).padding(horizontal = 12.dp, vertical = 9.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            BasicTextField(
                value = value,
                onValueChange = { v -> if (v.text.length <= 80) { value = v; ed.itemOp { it.copy(text = v.text) } } },
                modifier = Modifier.fillMaxWidth().focusRequester(focus).onFocusChanged { ed.typing = it.isFocused },
                textStyle = TextStyle(fontFamily = CTheme.fonts.body, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = c.ink),
                cursorBrush = SolidColor(c.accent), maxLines = 3,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                decorationBox = { inner ->
                    if (value.text.isEmpty()) Text("Type something", color = c.muted, fontSize = 16.sp, fontFamily = CTheme.fonts.body)
                    inner()
                },
            )
        }
        if (ed.typing) DoneBtn({ manager.clearFocus() }, Modifier.padding(top = 7.dp))
    }
    if (!ed.typing) {
        Row(Modifier.padding(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            SquareIconBtn(Icon.AlignLeft, Strings.get(R.string.align_left), { ed.itemOp { it.copy(align = "left") } }, pressed = item.align == "left")
            SquareIconBtn(Icon.AlignCenter, Strings.get(R.string.align_centre), { ed.itemOp { it.copy(align = "center") } }, pressed = item.align == "center")
            SquareIconBtn(Icon.AlignRight, Strings.get(R.string.align_right), { ed.itemOp { it.copy(align = "right") } }, pressed = item.align == "right")
            SquareBtn(Strings.get(R.string.bold), { ed.itemOp { it.copy(bold = !it.bold) } }, pressed = item.bold) {
                CText("B", size = 16.sp, weight = 800, color = if (item.bold) c.onAccent else c.ink)
            }
            SquareIconBtn(Icon.Add, Strings.get(R.string.add_another_text), { ed.addItem(CanvasItem.text()); ed.focusRequest++ })
            SquareIconBtn(Icon.Duplicate, Strings.get(R.string.duplicate), ed::duplicateItem)
            SquareIconBtn(Icon.Trash, Strings.get(R.string.delete), ed::deleteItem, danger = true)
        }
    }
}

private fun fontFamilyOf(def: FontDef, bold: Boolean = false): FontFamily = FontFamily(ComposeTypeface(Fonts.typeface(def, bold)))

@Composable
private fun FontPanel(ed: EditorState, item: CanvasItem, favourites: Set<String>, onToggleFavourite: (String) -> Unit) {
    val c = CTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ScrollRow(padding = 14.dp, spacing = 6.dp) {
            TabChip(Strings.get(R.string.fontcat_fav), ed.fontCategory == FontCatalog.FAVOURITES, { ed.fontCategory = FontCatalog.FAVOURITES })
            FontCatalog.categories.forEach { (id, _) -> TabChip(FontCatalog.categoryTitle(id), ed.fontCategory == id, { ed.fontCategory = id }) }
        }
        val fonts = remember(ed.fontCategory, favourites) {
            if (ed.fontCategory == FontCatalog.FAVOURITES) FontCatalog.favourites(favourites) else FontCatalog.inCategory(ed.fontCategory)
        }
        if (fonts.isEmpty()) {
            CText(Strings.get(R.string.tap_star_font), Modifier.padding(horizontal = 18.dp, vertical = 8.dp), size = 13.sp, color = c.muted)
        }
        ScrollRow(padding = 14.dp, spacing = 10.dp, state = rememberScrollState(), tiles = true) {
            fonts.forEach { f ->
                val on = f.id == item.font
                val starred = f.id in favourites
                Box(Modifier.width(108.dp).height(50.dp)) {
                    Column(
                        Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(12.dp)).background(c.paper)
                            .border(2.dp, if (on) c.accent else Color.Transparent, RoundedCornerShape(12.dp))
                            .pressable({ ed.itemOp { it.copy(font = f.id) } }, role = Role.RadioButton, label = f.name).semantics { selected = on },
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
                    ) {
                        Text("Collage", fontFamily = fontFamilyOf(f), fontSize = 18.sp, color = c.ink, maxLines = 1, softWrap = false)
                        CText(f.name, size = 10.sp, weight = 600, color = c.muted, maxLines = 1)
                    }
                    Box(
                        Modifier.align(Alignment.TopEnd).size(28.dp).pressable({ onToggleFavourite(f.id) }, label = if (starred) Strings.get(R.string.fav_remove, f.name) else Strings.get(R.string.fav_add, f.name)),
                        contentAlignment = Alignment.Center,
                    ) {
                        CIcon(if (starred) Icon.StarFilled else Icon.Star, size = 14.dp, tint = if (starred) c.accent else c.muted, strokeWidth = 2f)
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorPanel(ed: EditorState, item: CanvasItem, onCustomColor: (Long, (Long) -> Unit) -> Unit) {
    val target = ed.colorTarget
    val current: Long? = when (target) {
        ColorTarget.Fill -> item.fill
        ColorTarget.Gradient -> item.fill2
        ColorTarget.Stroke -> if (item.strokeW > 0f) item.strokeColor else null
        ColorTarget.Highlight -> item.bg
    }
    val set: (Long?) -> Unit = { col ->
        ed.itemOp { x ->
            when (target) {
                ColorTarget.Fill -> if (col == null && !(x.strokeW > 0f)) x.copy(fill = null, strokeW = .07f, strokeColor = 0xFFFFFFFF) else x.copy(fill = col)
                ColorTarget.Gradient -> x.copy(fill2 = col)
                ColorTarget.Stroke -> if (col != null) x.copy(strokeColor = col, strokeW = if (x.strokeW > 0f) x.strokeW else .09f) else x.copy(strokeW = 0f)
                ColorTarget.Highlight -> x.copy(bg = col)
            }
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ScrollRow(padding = 14.dp, spacing = 6.dp) { ColorTarget.entries.forEach { t -> TabChip(t.label, target == t, { ed.colorTarget = t }) } }
        if (target == ColorTarget.Gradient) GradientPicker(ed, item, onCustomColor)
        else ColorRow(TextColors.palette, current, set, onCustom = { onCustomColor(current ?: 0xFFFF4F8B, { col -> set(col) }) }, allowNone = target != ColorTarget.Fill)
    }
}

/** Gradient presets, and once one is on, its end colour and angle. */
@Composable
private fun GradientPicker(ed: EditorState, item: CanvasItem, onCustomColor: (Long, (Long) -> Unit) -> Unit) {
    val c = CTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ScrollRow(padding = 14.dp, spacing = 10.dp, state = rememberScrollState(), tiles = true) {
            TileWithLabel(Strings.get(R.string.off), item.fill2 == null, { ed.itemOp { it.copy(fill2 = null) } }) {
                Box(Modifier.fillMaxSize().background(DARK_TILE), contentAlignment = Alignment.Center) { CText("Aa", size = 18.sp, weight = 800, color = Color.White) }
            }
            TextGradients.all.forEach { g ->
                TileWithLabel(g.title, item.fill2 == g.to && item.fill == g.from, { ed.itemOp { g.on(it) } }) {
                    Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(g.from.toInt()), Color(g.to.toInt())), Offset.Zero, Offset(120f, 120f))))
                }
            }
        }
        val end = item.fill2
        if (end != null) {
            CText(Strings.get(R.string.label_end_colour), Modifier.padding(horizontal = 16.dp), size = 11.sp, weight = 700, color = c.muted, letterSpacing = 1.sp)
            ColorRow(TextColors.palette, end, { col -> if (col != null) ed.itemOp { it.copy(fill2 = col) } }, onCustom = { onCustomColor(end) { col -> ed.itemOp { it.copy(fill2 = col) } } })
            Box(Modifier.padding(horizontal = 14.dp)) {
                SliderOne(ItemSlider.Gradient.get(item), ItemSlider.Gradient.min..ItemSlider.Gradient.max, ItemSlider.Gradient::format, { v -> ed.itemOp { ItemSlider.Gradient.set(it, v) } }, step = 1f, label = Strings.get(R.string.gradient_angle))
            }
        }
    }
}

@Composable
private fun StylePanelText(ed: EditorState, item: CanvasItem) {
    val c = CTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ScrollRow(padding = 14.dp, spacing = 10.dp, state = rememberScrollState(), tiles = true) {
            TextPresets.all.forEach { preset ->
                val demo = remember(item.font, item.bold, preset.id) {
                    preset.apply.invoke(CanvasItem.text("Aa", font = item.font, size = .5f, fill = 0xFFFFFFFF).copy(bold = item.bold, x = .5f, y = .5f))
                }
                Canvas(Modifier.size(70.dp, 48.dp).clip(RoundedCornerShape(12.dp)).background(DARK_TILE).pressable({ ed.itemOp { preset.apply.invoke(it) } }, label = preset.title)) {
                    drawIntoCanvas { ItemRenderer.draw(it.nativeCanvas, demo, size.width, size.height) }
                }
            }
        }
        SliderTargets(ed.styleSlider, listOf(ItemSlider.Stroke, ItemSlider.Shadow, ItemSlider.Opacity), { ed.styleSlider = it }, ed, item)
    }
}

@Composable
private fun AdjustPanel(ed: EditorState, item: CanvasItem) {
    SliderTargets(ed.adjustSlider, listOf(ItemSlider.Size, ItemSlider.Spacing, ItemSlider.Curve, ItemSlider.Rotate), { ed.adjustSlider = it }, ed, item, withOrder = true)
}

/** Target chips (what the slider changes) plus one slider; optional front/back buttons. */
@Composable
fun SliderTargets(selected: ItemSlider, options: List<ItemSlider>, onSelect: (ItemSlider) -> Unit, ed: EditorState, item: CanvasItem, withOrder: Boolean = false) {
    Column(Modifier.padding(horizontal = 14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ScrollRow(Modifier.weight(1f), padding = 0.dp, spacing = 6.dp) { options.forEach { o -> TabChip(o.label, selected == o, { onSelect(o) }) } }
            if (withOrder) {
                SquareIconBtn(Icon.ToFront, Strings.get(R.string.bring_front), { ed.reorderItem(true) })
                SquareIconBtn(Icon.ToBack, Strings.get(R.string.send_back), { ed.reorderItem(false) })
            }
        }
        SliderOne(selected.get(item), selected.min..selected.max, selected::format, { v -> ed.itemOp { selected.set(it, v) } }, step = selected.step, label = selected.label)
    }
}

// ---------------------------------------------------------------------------------- stickers

@Composable
fun StickerPanel(ed: EditorState, onCustomColor: (Long, (Long) -> Unit) -> Unit) {
    val c = CTheme.colors
    val item = ed.selectedItem?.takeIf { !it.isText }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (item != null) {
            SliderTargets(ed.stickerSlider, listOf(ItemSlider.Size, ItemSlider.Rotate, ItemSlider.Opacity), { ed.stickerSlider = it }, ed, item)
            Row(Modifier.padding(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                SquareIconBtn(Icon.Flip, Strings.get(R.string.flip), { ed.itemOp { it.copy(flip = !it.flip) } }, pressed = item.flip)
                SquareIconBtn(Icon.ToFront, Strings.get(R.string.bring_front), { ed.reorderItem(true) })
                SquareIconBtn(Icon.ToBack, Strings.get(R.string.send_back), { ed.reorderItem(false) })
                SquareIconBtn(Icon.Duplicate, Strings.get(R.string.duplicate), ed::duplicateItem)
                SquareIconBtn(Icon.Trash, Strings.get(R.string.delete), ed::deleteItem, danger = true)
                Box(Modifier.weight(1f))
                DoneBtn(ed::doneEditing)
            }
            if (item.kind == StickerKind.SHAPE) {
                ColorRow(TextColors.palette, item.stickerFill, { col -> if (col != null) ed.itemOp { it.copy(stickerFill = col) } }, onCustom = { onCustomColor(item.stickerFill) { col -> ed.itemOp { it.copy(stickerFill = col) } } })
            }
        } else {
            ScrollRow(padding = 14.dp, spacing = 6.dp) {
                EmojiCatalog.categories.forEach { cat -> TabChip(cat.title, ed.emojiCategory == cat.id, { ed.emojiCategory = cat.id }) }
                TabChip(Strings.get(R.string.emoji_shapes), ed.emojiCategory == "shapes", { ed.emojiCategory = "shapes" })
            }
        }
        if (ed.emojiCategory == "shapes") {
            ScrollRow(padding = 12.dp, spacing = 2.dp, state = rememberScrollState(), tiles = true) {
                EmojiCatalog.shapeStickers.forEach { s -> ShapeCell(s) { ed.addItem(CanvasItem.shape(s)) } }
            }
        } else {
            val emoji = EmojiCatalog.categories.firstOrNull { it.id == ed.emojiCategory }?.items ?: emptyList()
            if (item != null) {
                ScrollRow(padding = 10.dp, spacing = 0.dp, state = rememberScrollState()) { emoji.forEach { e -> EmojiCell(e, Modifier.width(44.dp)) { ed.addItem(CanvasItem.emoji(e)) } } }
            } else {
                LazyVerticalGrid(GridCells.Fixed(7), Modifier.fillMaxWidth().heightIn(max = 112.dp).padding(horizontal = 12.dp)) {
                    items(emoji) { e -> EmojiCell(e, Modifier) { ed.addItem(CanvasItem.emoji(e)) } }
                }
            }
        }
    }
}

@Composable
private fun EmojiCell(emoji: String, modifier: Modifier, onClick: () -> Unit) {
    Box(modifier.height(42.dp).clip(RoundedCornerShape(10.dp)).pressable(onClick, label = Strings.get(R.string.add_item, emoji)), contentAlignment = Alignment.Center) {
        Text(emoji, fontSize = 26.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun ShapeCell(shape: String, onClick: () -> Unit) {
    Box(Modifier.size(44.dp, 42.dp).clip(RoundedCornerShape(10.dp)).pressable(onClick, label = Strings.get(R.string.sticker_desc, Strings.named("shape_$shape", shape))), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(26.dp)) {
            drawIntoCanvas { canvas ->
                val nc = canvas.nativeCanvas
                nc.save()
                nc.scale(size.width / 100f, size.height / 100f)
                nc.drawPath(Shapes.base(shape), android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFF4C542.toInt() })
                nc.restore()
            }
        }
    }
}

