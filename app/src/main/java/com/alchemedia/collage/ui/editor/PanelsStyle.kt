package com.alchemedia.collage.ui.editor

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.alchemedia.collage.R
import com.alchemedia.collage.model.AspectPreset
import com.alchemedia.collage.model.PhotoRef
import com.alchemedia.collage.model.TextColors
import com.alchemedia.collage.render.Filters
import com.alchemedia.collage.render.Textures
import com.alchemedia.collage.ui.AppViewModel
import com.alchemedia.collage.ui.BrowseTarget
import com.alchemedia.collage.ui.EditorState
import com.alchemedia.collage.ui.EditorTab
import com.alchemedia.collage.ui.MAX_ZOOM
import com.alchemedia.collage.ui.StyleSub
import com.alchemedia.collage.ui.components.BtnStyle
import com.alchemedia.collage.ui.components.CButton
import com.alchemedia.collage.ui.components.CIcon
import com.alchemedia.collage.ui.components.CText
import com.alchemedia.collage.ui.components.ColorGrid
import com.alchemedia.collage.ui.components.Icon
import com.alchemedia.collage.ui.components.LayoutPicker
import com.alchemedia.collage.ui.components.ScrollRow
import com.alchemedia.collage.ui.components.SliderRow
import com.alchemedia.collage.ui.components.TabChip
import com.alchemedia.collage.ui.components.hexOf
import com.alchemedia.collage.ui.components.percentText
import com.alchemedia.collage.ui.components.pressable
import com.alchemedia.collage.ui.sampleDrawable
import com.alchemedia.collage.ui.theme.CTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@Composable
fun LayoutPanel(vm: AppViewModel, ed: EditorState) {
    LayoutPicker(
        category = vm.category, onCategory = { vm.category = it }, photoCount = ed.project.photos.size, selectedId = ed.project.layoutId,
        onPick = ed::pickLayout, onBrowse = { vm.openBrowse(BrowseTarget.Editor) }, onDesign = { vm.openDesigner(BrowseTarget.Editor) },
        mineVersion = vm.mineVersion,
    )
}

@Composable
fun ShapePanel(ed: EditorState) {
    val c = CTheme.colors
    ScrollRow(padding = 14.dp, spacing = 10.dp, tiles = true) {
        AspectPreset.entries.forEach { a ->
            val on = ed.project.aspect == a
            val k = 30f / maxOf(a.width, a.height)
            Column(
                Modifier.width(64.dp).pressable({ ed.setAspect(a) }, role = Role.RadioButton, label = "Aspect ${a.label}").semantics { selected = on },
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                    Box(
                        Modifier.size((a.width * k).dp, (a.height * k).dp).clip(RoundedCornerShape(5.dp))
                            .background(if (on) c.accent.copy(alpha = .18f) else Color.Transparent)
                            .border(2.5.dp, if (on) c.accent else c.muted, RoundedCornerShape(5.dp)),
                    )
                }
                CText(a.label, size = 12.sp, weight = 700, color = if (on) c.accent else c.muted)
            }
        }
    }
}

@Composable
fun StylePanel(ed: EditorState, vm: AppViewModel, onCustomColor: (Long, (Long) -> Unit) -> Unit, onPickBackground: () -> Unit) {
    val p = ed.project
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ScrollRow(padding = 14.dp, spacing = 6.dp) {
            StyleSub.entries.forEach { s -> TabChip(s.label, ed.styleSub == s, { ed.styleSub = s }) }
        }
        when (ed.styleSub) {
            StyleSub.Color -> {
                ColorGrid(TextColors.backgrounds, p.background, ed::setBackground, onCustom = { onCustomColor(p.background, ed::setBackground) })
                Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    CText("Background", size = 13.sp, color = CTheme.colors.muted)
                    CText(hexOf(p.background), size = 13.sp, weight = 700, letterSpacing = .5.sp)
                }
            }
            StyleSub.Image -> {
                val bg = p.backgroundImage
                Row(Modifier.padding(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    val c = CTheme.colors
                    Box(
                        Modifier.size(64.dp).clip(RoundedCornerShape(14.dp)).background(if (bg == null) c.tint else Color.Transparent).pressable(onPickBackground, label = "Choose a background image"),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (bg == null) CIcon(Icon.Photo, size = 26.dp, tint = c.onTint)
                        else AsyncImage(if (bg.fileName.startsWith("res:")) sampleDrawable(bg) else vm.store.photoFile(p.id, bg), null, Modifier.size(64.dp), contentScale = ContentScale.Crop)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        CText(if (bg == null) "Use one of your pictures behind the photos." else "The picture fills the whole background.", size = 13.sp, color = CTheme.colors.muted, lineHeight = 18.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CButton(if (bg == null) "Choose image" else "Replace", onPickBackground, small = true, icon = Icon.Photo)
                            if (bg != null) CButton("Remove", ed::clearBackgroundImage, small = true, style = BtnStyle.GhostDanger, icon = Icon.Trash)
                        }
                    }
                }
            }
            StyleSub.Texture -> {
                ScrollRow(padding = 14.dp, spacing = 10.dp, tiles = true) {
                    Textures.all.forEach { t ->
                        TileWithLabel(t.name, p.texture == t.id, { ed.setTexture(t.id, t.base) }) { TextureThumb(t.id, t.base) }
                    }
                }
                val enabled = p.texture != "none"
                Box(Modifier.alpha(if (enabled) 1f else .4f)) {
                    SliderRow("Strength", p.textureAmount * 100f, 0f..150f, ::percentText, { if (enabled) ed.setTextureAmount(it / 100f) }, step = 1f)
                }
            }
            StyleSub.Spacing -> {
                SliderRow("Gap", p.gapPercent, 0f..12f, { "${it}%" }, ed::setGap, step = .25f)
                SliderRow("Margin", p.marginPercent, -10f..12f, { "${it}%" }, ed::setMargin, step = .25f)
                SliderRow("Corners", p.cornerPercent, 0f..100f, ::percentText, ed::setCorner, step = 1f)
            }
            StyleSub.Filter -> {
                ScrollRow(padding = 14.dp, spacing = 10.dp, tiles = true) {
                    Filters.all.forEach { f ->
                        TileWithLabel(f.name, p.filter == f.id, { ed.setFilter(f.id) }) {
                            val matrix = Filters.matrix(f.id)?.array
                            AsyncImage(
                                R.drawable.photo_beach, null, Modifier.size(60.dp), contentScale = ContentScale.Crop,
                                colorFilter = matrix?.let { ColorFilter.colorMatrix(ColorMatrix(it)) },
                            )
                        }
                    }
                }
                val enabled = p.filter != "none"
                Box(Modifier.alpha(if (enabled) 1f else .4f)) {
                    SliderRow("Strength", p.filterAmount * 100f, 0f..100f, ::percentText, { if (enabled) ed.setFilterAmount(it / 100f) }, step = 1f)
                }
            }
        }
    }
}

/** A 60dp rounded tile with a label under it; selected tiles get an accent border. */
@Composable
fun TileWithLabel(label: String, selected: Boolean, onClick: () -> Unit, content: @Composable () -> Unit) {
    val c = CTheme.colors
    Column(
        Modifier.width(66.dp).pressable(onClick, role = Role.RadioButton, label = label).semantics { this.selected = selected },
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.size(60.dp).clip(RoundedCornerShape(14.dp)).border(2.dp, if (selected) c.accent else Color.Transparent, RoundedCornerShape(14.dp)).padding(2.dp).clip(RoundedCornerShape(12.dp))) { content() }
        CText(label, size = 11.sp, weight = 600, color = if (selected) c.ink else c.muted, maxLines = 1, align = TextAlign.Center)
    }
}

@Composable
private fun TextureThumb(id: String, base: Long?) {
    val image: ImageBitmap? by produceState<ImageBitmap?>(null, id) {
        value = withContext(Dispatchers.Default) {
            val bmp = Bitmap.createBitmap(120, 120, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            canvas.drawColor((base ?: 0xFFD9DDE6L).toInt())
            Textures.draw(canvas, id, 1.15f, 120, 120)
            if (id == "none") {
                val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x59101828; strokeWidth = 3f; style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
                canvas.drawLine(30f, 90f, 90f, 30f, p)
            }
            bmp.asImageBitmap()
        }
    }
    Box(Modifier.size(56.dp)) {
        image?.let { Image(it, null, Modifier.size(56.dp), contentScale = ContentScale.Crop) }
    }
}

@Composable
fun PhotoPanel(vm: AppViewModel, ed: EditorState, onAddPhotos: () -> Unit) {
    val c = CTheme.colors
    val p = ed.project
    val cell = p.cells.getOrNull(ed.selectedCell)
    if (cell == null) {
        CText("Tap a cell on the collage to edit its photo.", Modifier.padding(start = 18.dp, end = 18.dp, top = 6.dp, bottom = 8.dp), size = 13.sp, color = c.muted, lineHeight = 19.sp)
        return
    }
    val filled = cell.photoId != null
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.alpha(if (filled) 1f else .4f)) {
            // Zoom goes up to 20x, so the slider is logarithmic: the same finger movement is the same proportional change.
            val top = kotlin.math.ln(MAX_ZOOM)
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CText("Zoom", Modifier.width(62.dp), size = 14.sp, weight = 600)
                com.alchemedia.collage.ui.components.BigSlider(
                    kotlin.math.ln(cell.zoom) / top, { if (filled) ed.setZoom(kotlin.math.exp(it * top)) }, 0f..1f, Modifier.weight(1f), label = "Zoom",
                )
                CText("${(cell.zoom * 100f).roundToInt()}%", Modifier.width(52.dp), size = 13.sp, weight = 600, align = TextAlign.End)
            }
        }
        Row(Modifier.padding(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CButton("Reset", ed::resetCell, small = true, style = BtnStyle.Tonal, enabled = filled, icon = Icon.Reset)
            CButton(if (ed.swapping) "Tap a cell…" else "Swap", { ed.swapping = !ed.swapping }, small = true, style = if (ed.swapping) BtnStyle.Primary else BtnStyle.Tonal, enabled = filled, icon = Icon.Swap)
            CButton("Remove", ed::emptyCell, small = true, style = BtnStyle.GhostDanger, enabled = filled, icon = Icon.Trash)
        }
        CText("REPLACE WITH", Modifier.padding(horizontal = 16.dp), size = 11.sp, weight = 700, color = c.muted, letterSpacing = 1.sp)
        ScrollRow(padding = 14.dp, spacing = 10.dp, state = rememberScrollState(), tiles = true) {
            Box(
                Modifier.size(46.dp).clip(RoundedCornerShape(12.dp)).background(c.tint).pressable(onAddPhotos, label = "Add a photo from this device"),
                contentAlignment = Alignment.Center,
            ) { CIcon(Icon.Add, size = 22.dp, tint = c.onTint, strokeWidth = 2.6f) }
            p.photos.forEach { ref -> Mini(vm, p.id, ref, ref.id == cell.photoId) { ed.useExistingPhoto(ref) } }
        }
    }
}

@Composable
private fun Mini(vm: AppViewModel, projectId: String, ref: PhotoRef, selected: Boolean, onClick: () -> Unit) {
    val c = CTheme.colors
    val model: Any = if (ref.fileName.startsWith("res:")) sampleDrawable(ref) else vm.store.photoFile(projectId, ref)
    Box(
        Modifier.size(46.dp).then(if (selected) Modifier.border(2.5.dp, c.accent, RoundedCornerShape(12.dp)).padding(3.dp) else Modifier).clip(RoundedCornerShape(12.dp))
            .pressable(onClick, label = "Use this photo"),
    ) { AsyncImage(model, null, Modifier.fillMaxWidth().size(46.dp), contentScale = ContentScale.Crop) }
}
