package com.alchemedia.collage.ui.editor

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alchemedia.collage.ui.AppViewModel
import com.alchemedia.collage.ui.EditorState
import com.alchemedia.collage.ui.EditorTab
import com.alchemedia.collage.ui.MAX_PHOTOS
import com.alchemedia.collage.ui.Sheet
import com.alchemedia.collage.ui.components.AppBar
import com.alchemedia.collage.ui.components.CIcon
import com.alchemedia.collage.ui.components.CText
import com.alchemedia.collage.ui.components.CTextField
import com.alchemedia.collage.ui.components.CustomColorSheet
import com.alchemedia.collage.ui.components.Icon
import com.alchemedia.collage.ui.components.Pill
import com.alchemedia.collage.ui.components.ProTag
import com.alchemedia.collage.ui.components.lerpColor
import com.alchemedia.collage.ui.components.pressable
import com.alchemedia.collage.ui.theme.CTheme

private fun EditorTab.icon(): Icon = when (this) {
    EditorTab.Layout -> Icon.LayoutTool
    EditorTab.Shape -> Icon.ShapeTool
    EditorTab.Style -> Icon.StyleTool
    EditorTab.Photo -> Icon.Photo
    EditorTab.Text -> Icon.TextTool
    EditorTab.Sticker -> Icon.StickerTool
}

@Composable
fun EditorScreen(vm: AppViewModel, ed: EditorState) {
    val c = CTheme.colors
    val pro by vm.settings.pro.collectAsStateWithLifecycle()
    var colorRequest by remember { mutableStateOf<Pair<Long, (Long) -> Unit>?>(null) }
    val focus = LocalFocusManager.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(MAX_PHOTOS)) { uris ->
        if (uris.isNotEmpty()) ed.addFromDevice(uris)
    }
    val bgPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> if (uri != null) ed.setBackgroundFromDevice(uri) }
    val onCustomColor: (Long, (Long) -> Unit) -> Unit = { initial, apply -> colorRequest = initial to apply }

    Box(Modifier.fillMaxSize().background(c.paper)) {
        BoxWithConstraints(Modifier.fillMaxSize().statusBarsPadding().imePadding()) {
            val panelMax = maxHeight * .42f
            Column(Modifier.fillMaxSize()) {
                AppBar(height = 52.dp, onBack = { vm.back() }) {
                    Box(Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).padding(horizontal = 4.dp, vertical = 6.dp)) {
                        CTextField(
                            ed.project.name, ed::rename, Modifier.fillMaxWidth(), "Untitled collage",
                            textStyle = TextStyle(fontFamily = CTheme.fonts.display(22f), fontSize = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = (-.4).sp),
                            placeholderColor = c.ink.copy(alpha = .9f), maxLength = 40, onDone = { focus.clearFocus() },
                        )
                    }
                    if (pro) ProTag()
                    Pill("Save", {
                        focus.clearFocus()
                        if (!ed.hasPhotos) vm.showToast("Add a photo first.")
                        else { ed.selectedItemId = null; ed.selectedCell = -1; vm.openSheet(Sheet.Export) }
                    }, icon = Icon.Save)
                }
                Desk(Modifier.weight(1f).padding(horizontal = 10.dp)) { EditorCanvas(ed) }
                if (ed.panelOpen) {
                    Column(
                        Modifier.padding(start = 10.dp, end = 10.dp, top = 8.dp).fillMaxWidth().heightIn(max = panelMax)
                            .shadow(8.dp, RoundedCornerShape(22.dp), spotColor = Color(0x24101828)).clip(RoundedCornerShape(22.dp)).background(c.card)
                            .verticalScroll(rememberScrollState()).padding(top = 10.dp, bottom = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        when (ed.tab) {
                            EditorTab.Layout -> LayoutPanel(vm, ed)
                            EditorTab.Shape -> ShapePanel(ed)
                            EditorTab.Style -> StylePanel(ed, vm, onCustomColor) { bgPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                            EditorTab.Photo -> PhotoPanel(vm, ed) { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                            EditorTab.Text -> TextPanel(ed, onCustomColor)
                            EditorTab.Sticker -> StickerPanel(ed, onCustomColor)
                        }
                    }
                }
                Dock(ed)
            }
        }
        colorRequest?.let { (initial, apply) ->
            CustomColorSheet(initial, apply, onDismiss = { colorRequest = null })
        }
    }
}

/** The dotted desk the collage sits on. */
@Composable
private fun Desk(modifier: Modifier, content: @Composable () -> Unit) {
    val c = CTheme.colors
    Box(
        modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp))
            .drawBehind {
                drawRect(c.desk)
                val step = 16.dp.toPx()
                var y = step / 2
                while (y < size.height) {
                    var x = step / 2
                    while (x < size.width) { drawCircle(c.dot, 1.3.dp.toPx(), Offset(x, y)); x += step }
                    y += step
                }
            }.padding(horizontal = 14.dp, vertical = 10.dp),
    ) { content() }
}

@Composable
private fun Dock(ed: EditorState) {
    val c = CTheme.colors
    Row(
        Modifier.padding(start = 10.dp, end = 10.dp, top = 8.dp, bottom = 10.dp).navigationBarsPadding().fillMaxWidth()
            .clip(RoundedCornerShape(24.dp)).background(c.dockBg).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        EditorTab.entries.forEach { t ->
            val on = ed.panelOpen && ed.tab == t
            val fg = if (on) c.onAccent else c.dockFg.copy(alpha = .72f)
            Column(
                Modifier.weight(1f).height(46.dp)
                    // The selected tab is painted as a plain rounded rectangle behind the label. (Clipping, backgrounds and an
                    // alpha layer stacked on one node left it invisible on some phones.)
                    .drawBehind { if (on) drawRoundRect(c.accent, cornerRadius = androidx.compose.ui.geometry.CornerRadius(18.dp.toPx())) }
                    .pressable({ ed.selectTab(t) }, role = Role.Tab, label = t.label).semantics { selected = on },
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
            ) {
                CIcon(t.icon(), size = 20.dp, tint = fg)
                CText(t.label, size = 10.5.sp, weight = 700, color = fg, maxLines = 1)
            }
        }
    }
}
