package com.alchemedia.collage.ui.home

import com.alchemedia.collage.Strings
import com.alchemedia.collage.R
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alchemedia.collage.model.HistoryEntry
import com.alchemedia.collage.ui.AppViewModel
import com.alchemedia.collage.ui.Sheet
import com.alchemedia.collage.ui.components.AppBar
import com.alchemedia.collage.ui.components.BarTitle
import com.alchemedia.collage.ui.components.CIcon
import com.alchemedia.collage.ui.components.CText
import com.alchemedia.collage.ui.components.CollagePreview
import com.alchemedia.collage.ui.components.CountBadge
import com.alchemedia.collage.ui.components.Icon
import com.alchemedia.collage.ui.components.LinkBtn
import com.alchemedia.collage.ui.components.lerpColor
import com.alchemedia.collage.ui.components.pressable
import com.alchemedia.collage.ui.components.rememberPhotos
import com.alchemedia.collage.ui.theme.CTheme
import kotlinx.coroutines.delay
import java.text.DateFormat
import java.util.Calendar
import java.util.Date

internal fun dayLabel(time: Long, now: Long = System.currentTimeMillis()): String {
    fun day(t: Long) = Calendar.getInstance().apply { timeInMillis = t; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
    val diff = Math.round((day(now) - day(time)) / 86_400_000.0)
    return when { diff <= 0 -> Strings.get(R.string.today); diff == 1L -> Strings.get(R.string.yesterday); diff < 7 -> Strings.get(R.string.this_week); else -> Strings.get(R.string.earlier) }
}

/** Every collage you have saved, newest first, ready to open again. */
@Composable
fun HistoryScreen(vm: AppViewModel) {
    val c = CTheme.colors
    val history by vm.store.history.collectAsStateWithLifecycle()
    var armed by remember { mutableStateOf(false) }
    LaunchedEffect(armed) { if (armed) { delay(3000); armed = false } }

    Column(Modifier.fillMaxSize().background(c.paper).statusBarsPadding()) {
        AppBar(onBack = { vm.closeOverlay() }) {
            BarTitle(Strings.get(R.string.history), Modifier.weight(1f))
            CountBadge(history.size.toString())
            if (history.isNotEmpty()) {
                LinkBtn(if (armed) Strings.get(R.string.tap_again_clear) else Strings.get(R.string.clear_all), { if (!armed) armed = true else { armed = false; vm.clearHistory() } })
            }
        }
        if (history.isEmpty()) {
            Column(Modifier.fillMaxWidth().padding(vertical = 40.dp, horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                CText(Strings.get(R.string.no_saved_collages), size = 22.sp, weight = 700, display = true)
                CText(Strings.get(R.string.saved_show_here), Modifier.padding(top = 6.dp), size = 14.sp, color = c.muted)
            }
        } else {
            val groups = remember(history) { history.groupBy { dayLabel(it.time) } }
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 28.dp)) {
                groups.forEach { (label, entries) ->
                    item(key = "h-$label") { CText(label, Modifier.padding(start = 2.dp, top = 18.dp, bottom = 8.dp), size = 18.sp, weight = 700, display = true, letterSpacing = (-.4).sp) }
                    items(entries, key = { it.id }) { entry -> HistoryRow(vm, entry, label) }
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(vm: AppViewModel, entry: HistoryEntry, label: String) {
    val c = CTheme.colors
    val photos = rememberPhotos(entry.project, 240)
    val name = entry.project.name.ifBlank { Strings.get(R.string.untitled) }
    val whenText = if (label == Strings.get(R.string.today) || label == Strings.get(R.string.yesterday)) DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(entry.time))
    else DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(entry.time))
    Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(c.card).pressable({ vm.openSheet(Sheet.HistoryItem(entry.id)) }, label = Strings.get(R.string.open_item, name)).padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(64.dp).clip(RoundedCornerShape(10.dp)).background(lerpColor(c.card, c.ink, .06f)).padding(5.dp), contentAlignment = Alignment.Center) {
                CollagePreview(entry.project, photos, Modifier.shadow(2.dp, RoundedCornerShape(2.dp)).clip(RoundedCornerShape(2.dp)))
            }
            Column(Modifier.weight(1f)) {
                CText(name, size = 15.sp, weight = 700, display = true, maxLines = 1)
                CText("$whenText · ${entry.width} × ${entry.height} · ${entry.project.layout.title}", size = 12.sp, color = c.muted, maxLines = 2)
            }
            Box(Modifier.clip(CircleShape).background(if (entry.watermark) c.tint else c.pro).padding(horizontal = 7.dp, vertical = 3.dp)) {
                CText(if (entry.watermark) Strings.get(R.string.watermark_tag) else Strings.get(R.string.pro_tag), size = 10.sp, weight = 800, color = if (entry.watermark) c.onTint else c.onPro, letterSpacing = .6.sp)
            }
        }
        Box(
            Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(c.card).pressable({ vm.deleteHistory(entry.id) }, label = Strings.get(R.string.delete_from_history, name)),
            contentAlignment = Alignment.Center,
        ) { CIcon(Icon.Trash, size = 20.dp, tint = c.danger) }
    }
}
