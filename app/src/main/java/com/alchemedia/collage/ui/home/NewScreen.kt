package com.alchemedia.collage.ui.home

import com.alchemedia.collage.Strings
import com.alchemedia.collage.R
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.alchemedia.collage.model.AspectPreset
import com.alchemedia.collage.model.CollageProject
import com.alchemedia.collage.model.LayoutCatalog
import com.alchemedia.collage.model.PhotoRef
import com.alchemedia.collage.render.PhotoSource
import com.alchemedia.collage.render.UiOptions
import com.alchemedia.collage.ui.AppViewModel
import com.alchemedia.collage.ui.BrowseTarget
import com.alchemedia.collage.ui.MAX_PHOTOS
import com.alchemedia.collage.ui.PickItem
import com.alchemedia.collage.ui.components.AppBar
import com.alchemedia.collage.ui.components.BarTitle
import com.alchemedia.collage.ui.components.CButton
import com.alchemedia.collage.ui.components.CIcon
import com.alchemedia.collage.ui.components.CText
import com.alchemedia.collage.ui.components.CollagePreview
import com.alchemedia.collage.ui.components.CountBadge
import com.alchemedia.collage.ui.components.Icon
import com.alchemedia.collage.ui.components.IconBtn
import com.alchemedia.collage.ui.components.LayoutPicker
import com.alchemedia.collage.ui.components.lerpColor
import com.alchemedia.collage.ui.components.pressable
import com.alchemedia.collage.ui.theme.CTheme

@Composable
fun NewScreen(vm: AppViewModel) {
    val c = CTheme.colors
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(MAX_PHOTOS)) { uris: List<Uri> ->
        if (uris.isNotEmpty()) vm.addDevicePhotos(uris)
    }
    val launchPicker = {
        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
    val device = vm.available.filter { it.uri != null }
    val samples = vm.available.filter { it.sample != null }
    val layout = LayoutCatalog.byId(vm.newLayoutId)
    val n = vm.selection.size

    Column(Modifier.fillMaxSize().background(c.paper).statusBarsPadding()) {
        AppBar(onBack = { vm.back() }) {
            BarTitle(Strings.get(R.string.choose_photos), Modifier.weight(1f))
            CountBadge("$n/$MAX_PHOTOS")
            IconBtn(Icon.Photo, Strings.get(R.string.add_photos_device), launchPicker)
        }
        LazyVerticalGrid(
            GridCells.Fixed(3), Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "add") { AddTile(launchPicker) }
            if (device.isNotEmpty()) {
                item(key = "yours", span = { GridItemSpan(maxLineSpan) }) { Label(Strings.get(R.string.label_your_photos)) }
                items(device, key = { it.id }) { Thumb(vm, it) }
                item(key = "samples", span = { GridItemSpan(maxLineSpan) }) { Label(Strings.get(R.string.label_sample_photos)) }
            }
            items(samples, key = { it.id }) { Thumb(vm, it) }
        }
        Tray(vm, layout.title, n, launchPicker)
    }
}

@Composable
private fun Label(text: String) = CText(text, Modifier.padding(top = 4.dp), size = 11.sp, weight = 700, color = CTheme.colors.muted, letterSpacing = 1.sp)

@Composable
private fun AddTile(onClick: () -> Unit) {
    val c = CTheme.colors
    Column(
        Modifier.aspectRatio(1f).clip(RoundedCornerShape(16.dp)).background(c.card).pressable(onClick, label = Strings.get(R.string.add_photos_device))
            .drawDash(c.line),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
    ) {
        CIcon(Icon.Photo, size = 28.dp, tint = c.accent)
        CText(Strings.get(R.string.add_photos), Modifier.padding(top = 6.dp), size = 12.sp, weight = 700, color = c.accent)
    }
}

private fun Modifier.drawDash(color: Color): Modifier = this.drawWithContent {
    drawContent()
    drawRoundRect(color, Offset(1f, 1f), Size(size.width - 2f, size.height - 2f), CornerRadius(16.dp.toPx()), style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 12f))))
}

@Composable
private fun Thumb(vm: AppViewModel, item: PickItem) {
    val c = CTheme.colors
    val index = vm.selection.indexOf(item.id)
    val on = index >= 0
    Box(
        Modifier.aspectRatio(1f).pressable({ vm.togglePick(item.id) }, role = Role.Checkbox, label = Strings.get(R.string.tab_photo))
            .semantics { selected = on },
    ) {
        val ring = if (on) Modifier.border(3.dp, c.accent, RoundedCornerShape(16.dp)).padding(3.dp) else Modifier
        Box(Modifier.fillMaxSize().then(ring).clip(RoundedCornerShape(if (on) 12.dp else 16.dp)).background(c.card)) {
            AsyncImage(
                item.model, null,
                Modifier.fillMaxSize().scale(if (on) .94f else 1f).clip(RoundedCornerShape(if (on) 12.dp else 16.dp)),
                contentScale = ContentScale.Crop,
                colorFilter = if (on) null else ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(.8f) }),
            )
        }
        if (on) {
            Box(
                Modifier.align(Alignment.TopEnd).padding(8.dp).size(width = 26.dp, height = 26.dp).clip(CircleShape).background(c.accent).border(2.5.dp, c.paper, CircleShape),
                contentAlignment = Alignment.Center,
            ) { CText("${index + 1}", size = 13.sp, weight = 800, color = c.onAccent) }
        }
    }
}

@Composable
private fun Tray(vm: AppViewModel, layoutName: String, n: Int, onAdd: () -> Unit) {
    val c = CTheme.colors
    val layout = LayoutCatalog.byId(vm.newLayoutId)
    val left = n - layout.cells.size
    val shape = RoundedCornerShape(26.dp)
    Column(
        Modifier.padding(start = 12.dp, end = 12.dp, bottom = 14.dp).navigationBarsPadding().fillMaxWidth()
            .shadow(14.dp, shape, spotColor = Color(0x29101828)).clip(shape).background(c.card).padding(top = 12.dp, bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(76.dp).clip(RoundedCornerShape(12.dp)).background(lerpColor(c.card, c.ink, .06f)), contentAlignment = Alignment.Center) {
                PickPreview(vm, Modifier.size(66.dp))
            }
            Column(Modifier.weight(1f)) {
                CText(if (n > 0) layout.title else Strings.get(R.string.pick_a_photo), size = 17.sp, weight = 700, display = true, maxLines = 1)
                CText(
                    if (n > 0) Strings.plural(R.plurals.p_cells, layout.cells.size).let { cells -> if (left > 0) Strings.get(R.string.new_summary_left, cells, Strings.plural(R.plurals.p_photos_left, left)) else cells }
                    else Strings.get(R.string.layout_suggested),
                    size = 12.sp, color = c.muted,
                )
            }
        }
        LayoutPicker(
            category = vm.category, onCategory = { vm.category = it }, photoCount = n, selectedId = vm.newLayoutId,
            onPick = { vm.pickLayoutForNew(it) }, onBrowse = { vm.openBrowse(BrowseTarget.New) }, onDesign = { vm.openDesigner(BrowseTarget.New) },
            mineVersion = vm.mineVersion,
        )
        CButton(if (n > 0) Strings.get(R.string.create_collage_n, n) else Strings.get(R.string.create_collage), { vm.createCollage() }, Modifier.padding(horizontal = 12.dp), enabled = n > 0 && !vm.creating)
    }
}

@Composable
private fun PickPreview(vm: AppViewModel, modifier: Modifier) {
    val layout = LayoutCatalog.byId(vm.newLayoutId)
    val chosen = vm.selection.toList()
    val project = remember(chosen, vm.newLayoutId, vm.newAspect) {
        val refs = chosen.map { PhotoRef(it, "pick", 1, 1) }
        CollageProject.create("pick", 0, refs, layout, vm.newAspect).copy(gapPercent = 2.5f, marginPercent = 2.5f, cornerPercent = 14f)
    }
    val source = remember { PhotoSource { id -> vm.thumbs[id] } }
    val ratio = vm.newAspect.ratio
    Box(modifier, contentAlignment = Alignment.Center) {
        CollagePreview(project, source, Modifier.then(if (ratio >= 1f) Modifier.fillMaxWidth() else Modifier.fillMaxSize()).shadow(2.dp, RoundedCornerShape(3.dp)).clip(RoundedCornerShape(3.dp)).background(Color.White), ui = if (chosen.isEmpty()) UiOptions() else null)
    }
}
