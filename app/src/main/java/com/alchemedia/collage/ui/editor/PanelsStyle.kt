package com.alchemedia.collage.ui.editor

import com.alchemedia.collage.Strings
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
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
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
import com.alchemedia.collage.model.SocialPresets
import com.alchemedia.collage.model.TextColors
import com.alchemedia.collage.render.Filters
import com.alchemedia.collage.render.Frames
import com.alchemedia.collage.render.Textures
import com.alchemedia.collage.ui.AppViewModel
import com.alchemedia.collage.ui.BrowseTarget
import com.alchemedia.collage.ui.EditorState
import com.alchemedia.collage.ui.EditorTab
import com.alchemedia.collage.ui.LightSlider
import com.alchemedia.collage.ui.MAX_ZOOM
import com.alchemedia.collage.ui.PhotoSub
import com.alchemedia.collage.ui.StyleSub
import com.alchemedia.collage.ui.components.BtnStyle
import com.alchemedia.collage.ui.components.CButton
import com.alchemedia.collage.ui.components.CIcon
import com.alchemedia.collage.ui.components.CText
import com.alchemedia.collage.ui.components.ColorGrid
import com.alchemedia.collage.ui.components.ColorRow
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
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ScrollRow(padding = 14.dp, spacing = 10.dp, tiles = true) {
            AspectPreset.entries.forEach { a ->
                val on = ed.project.aspect == a
                val k = 30f / maxOf(a.width, a.height)
                Column(
                    Modifier.width(64.dp).pressable({ ed.setAspect(a) }, role = Role.RadioButton, label = Strings.get(R.string.aspect_desc, a.label)).semantics { selected = on },
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
        CText(Strings.get(R.string.label_sizes_for), Modifier.padding(horizontal = 16.dp), size = 11.sp, weight = 700, color = c.muted, letterSpacing = 1.sp)
        ScrollRow(padding = 14.dp, spacing = 6.dp) {
            SocialPresets.all.forEach { s -> TabChip(s.title, ed.project.aspect == s.aspect, { ed.setAspect(s.aspect) }) }
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
                    CText(Strings.get(R.string.background), size = 13.sp, color = CTheme.colors.muted)
                    CText(hexOf(p.background), size = 13.sp, weight = 700, letterSpacing = .5.sp)
                }
            }
            StyleSub.Image -> {
                val bg = p.backgroundImage
                Row(Modifier.padding(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    val c = CTheme.colors
                    Box(
                        Modifier.size(64.dp).clip(RoundedCornerShape(14.dp)).background(if (bg == null) c.tint else Color.Transparent).pressable(onPickBackground, label = Strings.get(R.string.choose_bg_image)),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (bg == null) CIcon(Icon.Photo, size = 26.dp, tint = c.onTint)
                        else AsyncImage(if (bg.fileName.startsWith("res:")) sampleDrawable(bg) else vm.store.photoFile(p.id, bg), null, Modifier.size(64.dp), contentScale = ContentScale.Crop)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        CText(if (bg == null) Strings.get(R.string.bg_hint_none) else Strings.get(R.string.bg_hint_set), size = 13.sp, color = CTheme.colors.muted, lineHeight = 18.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CButton(if (bg == null) Strings.get(R.string.choose_image) else Strings.get(R.string.replace), onPickBackground, small = true, icon = Icon.Photo)
                            if (bg != null) CButton(Strings.get(R.string.remove), ed::clearBackgroundImage, small = true, style = BtnStyle.GhostDanger, icon = Icon.Trash)
                        }
                    }
                }
            }
            StyleSub.Texture -> {
                ScrollRow(padding = 14.dp, spacing = 10.dp, tiles = true) {
                    Textures.all.forEach { t ->
                        TileWithLabel(t.title, p.texture == t.id, { ed.setTexture(t.id, t.base) }) { TextureThumb(t.id, t.base) }
                    }
                }
                val enabled = p.texture != "none"
                Box(Modifier.alpha(if (enabled) 1f else .4f)) {
                    SliderRow(Strings.get(R.string.strength), p.textureAmount * 100f, 0f..150f, ::percentText, { if (enabled) ed.setTextureAmount(it / 100f) }, step = 1f)
                }
            }
            StyleSub.Spacing -> {
                SliderRow(Strings.get(R.string.gap), p.gapPercent, 0f..12f, { "${it}%" }, ed::setGap, step = .25f)
                SliderRow(Strings.get(R.string.margin), p.marginPercent, -10f..12f, { "${it}%" }, ed::setMargin, step = .25f)
                SliderRow(Strings.get(R.string.corners), p.cornerPercent, 0f..100f, ::percentText, ed::setCorner, step = 1f)
            }
            StyleSub.Frame -> {
                ScrollRow(padding = 14.dp, spacing = 10.dp, tiles = true) {
                    Frames.all.forEach { f -> TileWithLabel(f.title, p.frame == f.id, { ed.setFrame(f.id) }) { FrameThumb(f.id) } }
                }
                val on = p.frame != Frames.NONE
                Box(Modifier.alpha(if (on) 1f else .4f)) {
                    SliderRow(Strings.get(R.string.width), p.frameAmount, 0f..Frames.MAX_AMOUNT, { "${(it * 10).roundToInt() / 10f}%" }, { if (on) ed.setFrameAmount(it) }, step = .5f)
                }
                if (on) {
                    ColorRow(TextColors.palette, p.frameColor, { col -> if (col != null) ed.setFrameColor(col) }, onCustom = { onCustomColor(p.frameColor, ed::setFrameColor) })
                }
            }
            StyleSub.Filter -> {
                ScrollRow(padding = 14.dp, spacing = 10.dp, tiles = true) {
                    Filters.all.forEach { f ->
                        TileWithLabel(f.title, p.filter == f.id, { ed.setFilter(f.id) }) {
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
                    SliderRow(Strings.get(R.string.strength), p.filterAmount * 100f, 0f..100f, ::percentText, { if (enabled) ed.setFilterAmount(it / 100f) }, step = 1f)
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

/** A small sample of a frame around a stand-in photo, drawn by the same code that frames the real ones. */
@Composable
private fun FrameThumb(id: String) {
    val c = CTheme.colors
    val def = Frames.byId(id)
    androidx.compose.foundation.Canvas(Modifier.size(56.dp)) {
        drawRect(c.tint)
        drawIntoCanvas { canvas ->
            val nc = canvas.nativeCanvas
            val m = size.width * .16f
            val r = android.graphics.RectF(m, m, size.width - m, size.height - m)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = android.graphics.LinearGradient(r.left, r.top, r.right, r.bottom, 0xFF6FB1FF.toInt(), 0xFFFF9D5C.toInt(), android.graphics.Shader.TileMode.CLAMP)
            }
            // Thumbnails are small, so the frame is drawn thicker than it would be on a real photo.
            Frames.draw(nc, id, r, size.width * .03f, def.color.toInt(), def.amount * 2.2f, 3) { inner, radius -> nc.drawRoundRect(inner, radius, radius, paint) }
        }
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
        CText(Strings.get(R.string.tap_cell_edit_photo), Modifier.padding(start = 18.dp, end = 18.dp, top = 6.dp, bottom = 8.dp), size = 13.sp, color = c.muted, lineHeight = 19.sp)
        return
    }
    val filled = cell.photoId != null
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ScrollRow(padding = 14.dp, spacing = 6.dp) {
            PhotoSub.entries.forEach { s -> TabChip(s.label, ed.photoSub == s, { ed.photoSub = s }) }
        }
        when (ed.photoSub) {
            PhotoSub.Photo -> PhotoMain(vm, ed, cell, filled, onAddPhotos)
            PhotoSub.Rotate -> PhotoRotate(ed, cell, filled)
            PhotoSub.Light -> PhotoLight(ed, cell, filled)
        }
    }
}

@Composable
private fun PhotoMain(vm: AppViewModel, ed: EditorState, cell: com.alchemedia.collage.model.CellContent, filled: Boolean, onAddPhotos: () -> Unit) {
    val c = CTheme.colors
    val p = ed.project
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.alpha(if (filled) 1f else .4f)) {
            // Zoom goes up to 20x, so the slider is logarithmic: the same finger movement is the same proportional change.
            val top = kotlin.math.ln(MAX_ZOOM)
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CText(Strings.get(R.string.zoom), Modifier.width(62.dp), size = 14.sp, weight = 600)
                com.alchemedia.collage.ui.components.BigSlider(
                    kotlin.math.ln(cell.zoom) / top, { if (filled) ed.setZoom(kotlin.math.exp(it * top)) }, 0f..1f, Modifier.weight(1f), label = Strings.get(R.string.zoom),
                )
                CText("${(cell.zoom * 100f).roundToInt()}%", Modifier.width(52.dp), size = 13.sp, weight = 600, align = TextAlign.End)
            }
        }
        Row(Modifier.padding(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CButton(Strings.get(R.string.reset), ed::resetCell, small = true, style = BtnStyle.Tonal, enabled = filled, icon = Icon.Reset)
            CButton(if (ed.swapping) Strings.get(R.string.tap_a_cell) else Strings.get(R.string.swap), { ed.swapping = !ed.swapping }, small = true, style = if (ed.swapping) BtnStyle.Primary else BtnStyle.Tonal, enabled = filled, icon = Icon.Swap)
            CButton(Strings.get(R.string.remove), ed::emptyCell, small = true, style = BtnStyle.GhostDanger, enabled = filled, icon = Icon.Trash)
        }
        CText(Strings.get(R.string.label_replace_with), Modifier.padding(horizontal = 16.dp), size = 11.sp, weight = 700, color = c.muted, letterSpacing = 1.sp)
        ScrollRow(padding = 14.dp, spacing = 10.dp, state = rememberScrollState(), tiles = true) {
            Box(
                Modifier.size(46.dp).clip(RoundedCornerShape(12.dp)).background(c.tint).pressable(onAddPhotos, label = Strings.get(R.string.add_photo_device)),
                contentAlignment = Alignment.Center,
            ) { CIcon(Icon.Add, size = 22.dp, tint = c.onTint, strokeWidth = 2.6f) }
            p.photos.forEach { ref -> Mini(vm, p.id, ref, ref.id == cell.photoId) { ed.useExistingPhoto(ref) } }
        }
    }
}

/** Straighten, quarter turns and mirroring. */
@Composable
private fun PhotoRotate(ed: EditorState, cell: com.alchemedia.collage.model.CellContent, filled: Boolean) {
    Column(Modifier.alpha(if (filled) 1f else .4f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SliderRow(Strings.get(R.string.straighten), cell.angle, -45f..45f, { "${it.roundToInt()}°" }, { if (filled) ed.setAngle(it) }, step = 1f)
        ScrollRow(padding = 14.dp, spacing = 8.dp) {
            CButton(Strings.get(R.string.sub_rotate), { if (filled) ed.rotateCell() }, small = true, style = BtnStyle.Tonal, enabled = filled, icon = Icon.Rotate)
            CButton(Strings.get(R.string.mirror), { if (filled) ed.mirrorCell(false) }, small = true, style = BtnStyle.Tonal, enabled = filled, icon = Icon.Flip)
            CButton(Strings.get(R.string.flip), { if (filled) ed.mirrorCell(true) }, small = true, style = BtnStyle.Tonal, enabled = filled, icon = Icon.FlipV)
            CButton(Strings.get(R.string.reset), { if (filled) ed.resetCell() }, small = true, style = BtnStyle.Ghost, enabled = filled && cell.hasTurn, icon = Icon.Reset)
        }
    }
}

/** Brightness, contrast and saturation for this one photo (the collage-wide filter is separate). */
@Composable
private fun PhotoLight(ed: EditorState, cell: com.alchemedia.collage.model.CellContent, filled: Boolean) {
    Column(Modifier.alpha(if (filled) 1f else .4f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LightSlider.entries.forEach { slider ->
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CText(slider.label, Modifier.width(86.dp), size = 14.sp, weight = 600, maxLines = 1)
                com.alchemedia.collage.ui.components.BigSlider(
                    slider.get(cell), { if (filled) ed.setLook(slider, it) }, -100f..100f, Modifier.weight(1f), step = 1f, label = slider.label,
                )
                CText("${slider.get(cell).roundToInt()}", Modifier.width(40.dp), size = 13.sp, weight = 600, align = TextAlign.End)
            }
        }
        Row(Modifier.padding(horizontal = 14.dp)) {
            CButton(Strings.get(R.string.reset_light), { if (filled) ed.resetLook() }, small = true, style = BtnStyle.Ghost, enabled = filled && cell.hasLook, icon = Icon.Reset)
        }
    }
}

@Composable
private fun Mini(vm: AppViewModel, projectId: String, ref: PhotoRef, selected: Boolean, onClick: () -> Unit) {
    val c = CTheme.colors
    val model: Any = if (ref.fileName.startsWith("res:")) sampleDrawable(ref) else vm.store.photoFile(projectId, ref)
    Box(
        Modifier.size(46.dp).then(if (selected) Modifier.border(2.5.dp, c.accent, RoundedCornerShape(12.dp)).padding(3.dp) else Modifier).clip(RoundedCornerShape(12.dp))
            .pressable(onClick, label = Strings.get(R.string.use_this_photo)),
    ) { AsyncImage(model, null, Modifier.fillMaxWidth().size(46.dp), contentScale = ContentScale.Crop) }
}
