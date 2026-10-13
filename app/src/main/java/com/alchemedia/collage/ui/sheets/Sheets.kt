package com.alchemedia.collage.ui.sheets

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alchemedia.collage.BuildConfig
import com.alchemedia.collage.R
import com.alchemedia.collage.data.BillingManager
import com.alchemedia.collage.data.Exporter
import com.alchemedia.collage.model.HistoryEntry
import com.alchemedia.collage.model.LayoutCatalog
import com.alchemedia.collage.render.CollageRenderer
import com.alchemedia.collage.ui.AppViewModel
import com.alchemedia.collage.ui.EditorState
import com.alchemedia.collage.ui.components.BtnStyle
import com.alchemedia.collage.ui.components.CButton
import com.alchemedia.collage.ui.components.CText
import com.alchemedia.collage.ui.components.LinkBtn
import com.alchemedia.collage.ui.components.MetaText
import com.alchemedia.collage.ui.components.ProTag
import com.alchemedia.collage.ui.components.SheetHost
import com.alchemedia.collage.ui.components.SheetTitle
import com.alchemedia.collage.ui.components.lerpColor
import com.alchemedia.collage.ui.components.pressable
import com.alchemedia.collage.ui.theme.CTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date

/** The framed "print" every sheet shows its picture on. */
@Composable
private fun Print(image: ImageBitmap?, ratio: Float, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(
            Modifier.graphicsLayer { rotationZ = -1.5f }.heightIn(max = 250.dp)
                .shadow(12.dp, RoundedCornerShape(4.dp), spotColor = Color(0x4D101828)).background(Color.White, RoundedCornerShape(4.dp)).padding(9.dp),
        ) {
            if (image != null) {
                Image(image, "Collage preview", Modifier.heightIn(max = 232.dp).then(if (ratio >= 1f) Modifier.fillMaxWidth(.8f) else Modifier), contentScale = ContentScale.Fit)
            } else {
                Box(Modifier.size((200 * ratio.coerceAtMost(1.3f)).dp, 200.dp / ratio.coerceAtLeast(1f)))
            }
        }
    }
}

// ----------------------------------------------------------------------------------------------- Pro

@Composable
fun ProSheet(vm: AppViewModel) {
    val c = CTheme.colors
    val app = vm.getApplication<com.alchemedia.collage.CollageApp>()
    val billing = app.billing
    val pro by vm.settings.pro.collectAsStateWithLifecycle()
    val price by billing.price.collectAsStateWithLifecycle()
    val message by billing.message.collectAsStateWithLifecycle()
    val busy by billing.busy.collectAsStateWithLifecycle()
    val activity = LocalActivity.current
    LaunchedEffect(Unit) { billing.connect() }
    SheetHost(onDismiss = vm::closeSheet) {
        Box { ProTag() }
        SheetTitle(if (pro) "You're Pro" else "Photo Collage Pro")
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            CText(price, size = 46.sp, weight = 800, display = true, letterSpacing = (-1.4).sp)
            CText("/ month", Modifier.padding(bottom = 8.dp), size = 16.sp, weight = 600, color = c.muted)
        }
        Perk("Removes the watermark from every export")
        MetaText(if (pro) "Your subscription is active. Manage or cancel it any time in Google Play." else "Billed monthly through Google Play. Cancel any time in Google Play.")
        message?.let { CText(it, Modifier.fillMaxWidth(), size = 13.sp, weight = 600, color = c.danger, align = androidx.compose.ui.text.style.TextAlign.Center) }
        if (pro) {
            val context = LocalContext.current
            CButton("Manage subscription", {
                val uri = Uri.parse("https://play.google.com/store/account/subscriptions?sku=${BillingManager.PRODUCT_ID}&package=${context.packageName}")
                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
            })
            CButton("Done", vm::closeSheet, style = BtnStyle.Ghost)
        } else {
            CButton("Subscribe for $price / month", { activity?.let(billing::subscribe) }, enabled = !busy && activity != null)
            CButton("Not now", vm::closeSheet, style = BtnStyle.Ghost)
            Box(Modifier.align(Alignment.CenterHorizontally)) { LinkBtn("Restore purchase", billing::restore) }
        }
        if (BuildConfig.DEBUG) {
            CButton(if (pro) "Debug: switch Pro off" else "Debug: switch Pro on (no payment)", { vm.settings.setPro(!pro) }, style = BtnStyle.Ghost, small = true, modifier = Modifier.align(Alignment.CenterHorizontally))
        }
    }
}

@Composable
private fun Perk(text: String) {
    val c = CTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.size(20.dp).clip(CircleShape).background(c.tint), contentAlignment = Alignment.Center) {
            com.alchemedia.collage.ui.components.CIcon(com.alchemedia.collage.ui.components.Icon.Check, size = 12.dp, tint = c.accent, strokeWidth = 3f)
        }
        CText(text, size = 14.sp, weight = 600)
    }
}

// ----------------------------------------------------------------------------------------------- export

@Composable
fun ExportSheet(vm: AppViewModel, ed: EditorState) {
    val c = CTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pro by vm.settings.pro.collectAsStateWithLifecycle()
    val price by vm.getApplication<com.alchemedia.collage.CollageApp>().billing.price.collectAsStateWithLifecycle()
    val project = remember { ed.project }
    var usePro by remember { mutableStateOf(pro) }
    var preview by remember { mutableStateOf<ImageBitmap?>(null) }
    var saving by remember { mutableStateOf(false) }
    val (w, h) = remember(project) { Exporter.sizeFor(project) }
    LaunchedEffect(pro) { if (pro) usePro = true }
    LaunchedEffect(usePro) {
        preview = withContext(Dispatchers.Default) {
            val bmp = Exporter.render(vm.photos, project, watermark = !usePro, longSide = 900)
            bmp.asImageBitmap()
        }
    }

    fun perform(share: Boolean) {
        if (saving) return
        saving = true
        scope.launch {
            val bmp = Exporter.renderWithFallback(vm.photos, project, watermark = !usePro)
            if (bmp == null) { saving = false; vm.showToast("There isn't enough memory to save this collage. Close other apps and try again."); return@launch }
            val uri = Exporter.saveToGallery(context, bmp)
            saving = false
            if (uri == null) { vm.showToast("Couldn't save the picture. Check there's space on this device and try again."); return@launch }
            vm.store.recordHistory(project, bmp.width, bmp.height, watermark = !usePro)
            vm.closeSheets()
            if (share) {
                val send = Intent(Intent.ACTION_SEND).apply { type = "image/jpeg"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }
                runCatching { context.startActivity(Intent.createChooser(send, "Share collage")) }
            }
            vm.showToast("Saved to your gallery.")
        }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) perform(false) else vm.showToast("Allow storage access to save your collage, or use Share instead.")
    }
    fun save() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else perform(false)
    }

    SheetHost(onDismiss = vm::closeSheet) {
        SheetTitle("Save collage")
        Print(preview, project.aspect.ratio)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PlanCard(Modifier.weight(1f), selected = !usePro, onClick = { usePro = false }) {
                CText("Free", size = 17.sp, weight = 800, display = true)
                CText("Small Photo Collage mark in the corner.", size = 12.sp, color = c.muted, lineHeight = 16.sp)
            }
            PlanCard(Modifier.weight(1f), selected = usePro, onClick = { if (pro) usePro = true else vm.openSheet(com.alchemedia.collage.ui.Sheet.Pro) }, badge = { ProTag(Modifier.align(Alignment.TopEnd).offset(x = (-10).dp, y = (-10).dp)) }) {
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    CText("Pro", size = 17.sp, weight = 800, display = true)
                    CText("$price / month", Modifier.padding(bottom = 1.dp), size = 12.sp, weight = 600, color = c.muted, maxLines = 1)
                }
                CText("Removes the watermark from every export.", size = 12.sp, color = c.muted, lineHeight = 16.sp)
            }
        }
        MetaText("$w × $h px${if (usePro) ", no watermark" else ", with watermark"}")
        CButton(if (saving) "Saving…" else "Save to gallery", ::save, enabled = !saving)
        CButton("Share", { perform(true) }, style = BtnStyle.Tonal, enabled = !saving)
        CButton("Keep editing", vm::closeSheet, style = BtnStyle.Ghost)
    }
}

@Composable
private fun PlanCard(modifier: Modifier, selected: Boolean, onClick: () -> Unit, badge: (@Composable androidx.compose.foundation.layout.BoxScope.() -> Unit)? = null, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    val c = CTheme.colors
    Box(modifier) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(c.card).border(2.dp, if (selected) c.accent else c.line, RoundedCornerShape(18.dp))
                .pressable(onClick, role = Role.RadioButton).semantics { this.selected = selected }.padding(horizontal = 13.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            content = content,
        )
        badge?.invoke(this)
    }
}

// ----------------------------------------------------------------------------------------------- card menu

@Composable
fun CardSheet(vm: AppViewModel, projectId: String) {
    val projects by vm.store.projects.collectAsStateWithLifecycle()
    val project = projects.firstOrNull { it.id == projectId }
    if (project == null) { LaunchedEffect(Unit) { vm.closeSheet() }; return }
    val photos = project.cells.count { it.photoId != null }
    val date = DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(project.updatedAt))
    SheetHost(onDismiss = vm::closeSheet) {
        SheetTitle(project.name.ifBlank { "Untitled collage" })
        MetaText("$photos photo${if (photos == 1) "" else "s"} · ${project.layout.name} · edited $date")
        CButton("Open", { vm.closeSheet(); vm.openProject(projectId) })
        CButton("Duplicate", { vm.closeSheet(); vm.duplicateProject(projectId) }, style = BtnStyle.Tonal)
        CButton("Delete collage", { vm.closeSheet(); vm.deleteProject(projectId) }, style = BtnStyle.GhostDanger)
        CButton("Cancel", vm::closeSheet, style = BtnStyle.Ghost)
    }
}

// ----------------------------------------------------------------------------------------------- history item

@Composable
fun HistoryItemSheet(vm: AppViewModel, entryId: String) {
    val history by vm.store.history.collectAsStateWithLifecycle()
    val entry = history.firstOrNull { it.id == entryId }
    if (entry == null) { LaunchedEffect(Unit) { vm.closeSheet() }; return }
    var image by remember { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(entry.id) {
        image = withContext(Dispatchers.Default) { runCatching { renderEntry(vm, entry, 900).asImageBitmap() }.getOrNull() }
    }
    val time = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(entry.time))
    val day = DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(entry.time))
    SheetHost(onDismiss = vm::closeSheet) {
        SheetTitle(entry.project.name.ifBlank { "Untitled collage" })
        Print(image, entry.project.aspect.ratio)
        MetaText("Saved $day at $time · ${entry.width} × ${entry.height}${if (entry.watermark) " · with watermark" else " · no watermark"}")
        CButton("Edit again", { vm.closeSheets(); vm.editAgain(entry) })
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CButton("Delete", { vm.deleteHistory(entry.id) }, Modifier.weight(1f), style = BtnStyle.GhostDanger)
            CButton("Close", vm::closeSheet, Modifier.weight(1f), style = BtnStyle.Ghost)
        }
    }
}

fun renderEntry(vm: AppViewModel, entry: HistoryEntry, longSide: Int): Bitmap {
    val (w, h) = CollageRenderer.exportSize(entry.project.aspect.ratio, longSide)
    return CollageRenderer.render(entry.project, w, h, vm.photos.source(entry.project, longSide, blocking = true), entry.watermark)
}

