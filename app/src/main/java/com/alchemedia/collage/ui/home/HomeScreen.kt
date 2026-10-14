package com.alchemedia.collage.ui.home

import com.alchemedia.collage.Strings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.alpha
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.alchemedia.collage.R
import com.alchemedia.collage.model.AspectPreset
import com.alchemedia.collage.model.CollageProject
import com.alchemedia.collage.model.LayoutCatalog
import com.alchemedia.collage.model.Samples
import com.alchemedia.collage.ui.AppViewModel
import com.alchemedia.collage.ui.BrowseTarget
import com.alchemedia.collage.ui.Sheet
import com.alchemedia.collage.ui.components.CIcon
import com.alchemedia.collage.ui.components.CText
import com.alchemedia.collage.ui.components.CollagePreview
import com.alchemedia.collage.ui.components.Icon
import com.alchemedia.collage.ui.components.IconBtn
import com.alchemedia.collage.ui.components.LinkBtn
import com.alchemedia.collage.ui.components.Pill
import com.alchemedia.collage.ui.components.ProTag
import com.alchemedia.collage.ui.components.edgeFade
import com.alchemedia.collage.ui.components.lerpColor
import com.alchemedia.collage.ui.components.pressable
import com.alchemedia.collage.ui.components.rememberPhotos
import com.alchemedia.collage.ui.theme.CTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HomeScreen(vm: AppViewModel) {
    val c = CTheme.colors
    val projects by vm.store.projects.collectAsStateWithLifecycle()
    val pro by vm.settings.pro.collectAsStateWithLifecycle()
    val loaded by vm.store.loaded.collectAsStateWithLifecycle()
    val total = remember(vm.mineVersion) { LayoutCatalog.all.size }

    Column(Modifier.fillMaxSize().background(c.paper).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().height(52.dp).padding(start = 16.dp, end = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(0.dp)) {
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Logo(Modifier.size(28.dp))
                // the bar also holds language, history and Go Pro, so the name has to fit beside them on a 360dp phone
                CText("Photo Collage", size = 20.sp, weight = 800, display = true, letterSpacing = (-.75).sp, maxLines = 1)
            }
            if (pro) ProTag(Modifier.padding(end = 4.dp))
            IconBtn(Icon.Globe, Strings.get(R.string.language), { vm.openSheet(Sheet.Language) }, size = 36.dp)
            IconBtn(Icon.History, Strings.get(R.string.history), { vm.openHistory() }, size = 36.dp)
            if (!pro) Pill(Strings.get(R.string.go_pro), { vm.openSheet(Sheet.Pro) }, Modifier.padding(start = 6.dp), ink = true)
        }
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(top = 4.dp, bottom = 100.dp)) {
            item { Hero(onNew = { vm.startNew() }, onDesign = { vm.openDesigner(BrowseTarget.Home) }) }
            item {
                SectionHeader(Strings.get(R.string.quick_layouts)) {
                    LinkBtn(Strings.get(R.string.see_all, total), { vm.openBrowse(BrowseTarget.Home) }, vertical = 0.dp)
                }
            }
            item { QuickRow(vm) }
            item {
                SectionHeader(Strings.get(R.string.pinned)) {
                    if (projects.isNotEmpty()) CText(Strings.plural(R.plurals.p_collages, projects.size), size = 12.sp, color = c.muted)
                }
            }
            if (projects.isEmpty() && loaded) {
                item {
                    Column(Modifier.fillMaxWidth().padding(vertical = 40.dp, horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        CText(Strings.get(R.string.nothing_pinned), size = 22.sp, weight = 700, display = true)
                        CText(Strings.get(R.string.start_and_show), Modifier.padding(top = 6.dp), size = 14.sp, color = c.muted)
                    }
                }
            } else if (projects.isNotEmpty()) {
                items(projects.chunked(2), key = { row -> row.joinToString { it.id } }) { row ->
                    Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp).padding(bottom = 14.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Top) {
                        row.forEach { p ->
                            ProjectCard(p, Modifier.weight(1f), onOpen = { vm.openProject(p.id) }, onMenu = { vm.openSheet(Sheet.Card(p.id)) })
                        }
                        if (row.size == 1) Box(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, trailing: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 24.dp, bottom = 10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
        CText(title, size = 19.sp, weight = 700, display = true, letterSpacing = (-.4).sp, lineHeight = 23.sp)
        trailing()
    }
}

/** The app icon (the pinwheel), drawn from the same two layers as the launcher icon. */
@Composable
fun Logo(modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.clip(RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
        // The icon's artwork sits inside the middle of a 108 unit square, so the layers are drawn larger than the box and cropped.
        val layer = maxWidth * 1.55f
        androidx.compose.foundation.Image(painterResource(R.drawable.ic_launcher_background), null, Modifier.requiredSize(layer))
        androidx.compose.foundation.Image(painterResource(R.drawable.ic_launcher_foreground), null, Modifier.requiredSize(layer))
    }
}

// --------------------------------------------------------------------------- hero

@Composable
private fun Hero(onNew: () -> Unit, onDesign: () -> Unit) {
    val c = CTheme.colors
    val shape = RoundedCornerShape(28.dp)
    Box(
        Modifier.padding(start = 16.dp, end = 16.dp, top = 6.dp).fillMaxWidth().height(282.dp)
            .shadow(16.dp, shape, ambientColor = Color(0x4D101828), spotColor = Color(0x4D101828))
            .clip(shape)
            .drawBehind {
                drawRect(c.hero)
                drawRect(Brush.radialGradient(listOf(Color(0x8C5C84FF), Color.Transparent), center = Offset(size.width * .88f, -size.height * .1f), radius = size.width * .85f))
                drawRect(Brush.radialGradient(listOf(Color(0x612D5FE6), Color.Transparent), center = Offset(-size.width * .05f, size.height * 1.05f), radius = size.width * .75f))
                drawRect(Brush.verticalGradient(listOf(Color(0x0FFFFFFF), Color.Transparent), endY = size.height * .38f))
            },
    ) {
        HeroArt(Modifier.align(Alignment.CenterEnd).width(166.dp).fillMaxSize())
        Column(Modifier.fillMaxSize().padding(start = 20.dp, top = 18.dp, bottom = 22.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Column {
                CText(Strings.get(R.string.start_a_collage), Modifier.width(124.dp), size = 29.sp, weight = 800, display = true, color = Color.White, letterSpacing = (-1).sp, lineHeight = 28.4.sp)
                CText(Strings.get(R.string.hero_sub), Modifier.width(118.dp).padding(top = 5.dp).alpha(.85f), size = 13.sp, color = Color.White, lineHeight = 18.2.sp)
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                GoButton(onNew)
                DesignButton(onDesign)
            }
        }
    }
}

@Composable
private fun GoButton(onClick: () -> Unit) {
    val c = CTheme.colors
    val t = rememberInfiniteTransition(label = "sweep")
    val sweep by t.animateFloat(-1.3f, 1.3f, infiniteRepeatable(tween(4800, easing = LinearEasing), RepeatMode.Restart), label = "x")
    Row(
        Modifier.height(44.dp).shadow(8.dp, RoundedCornerShape(14.dp), spotColor = Color(0x4D000000)).clip(RoundedCornerShape(14.dp)).background(Color.White)
            .drawWithContent {
                drawContent()
                val x = size.width * sweep
                drawRect(Brush.horizontalGradient(listOf(Color.Transparent, Color(0x382557D6), Color.Transparent), startX = x - size.width * .3f, endX = x + size.width * .3f))
            }
            .pressable(onClick, label = Strings.get(R.string.new_collage)).padding(start = 14.dp, end = 18.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CIcon(Icon.Add, size = 22.dp, tint = c.hero, strokeWidth = 2.6f)
        CText(Strings.get(R.string.new_collage), size = 14.sp, weight = 800, color = c.hero, maxLines = 1)
    }
}

/** The way in to the layout designer, right under the main button. */
@Composable
private fun DesignButton(onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        Modifier.height(40.dp).clip(shape).background(Color(0x29FFFFFF)).border(1.dp, Color(0x59FFFFFF), shape)
            .pressable(onClick, label = Strings.get(R.string.design_own_layout)).padding(start = 11.dp, end = 14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CIcon(Icon.Pencil, size = 18.dp, tint = Color.White, strokeWidth = 2.2f)
        CText(Strings.get(R.string.design_own_layout), size = 12.5.sp, weight = 700, color = Color.White, maxLines = 1)
    }
}

@Composable
private fun HeroArt(modifier: Modifier) {
    val t = rememberInfiniteTransition(label = "float")
    @Composable fun bob(delayMs: Int) = t.animateFloat(0f, 1f, infiniteRepeatable(tween(3500, easing = androidx.compose.animation.core.FastOutSlowInEasing), RepeatMode.Reverse, initialStartOffset = androidx.compose.animation.core.StartOffset(delayMs)), label = "bob")
    val b1 by bob(0)
    val b2 by bob(1500)
    val b3 by bob(3000)
    val heroProject = remember {
        CollageProject.create("hero", 0, listOf(Samples.moraine, Samples.milky, Samples.beach, Samples.wheat), LayoutCatalog.byId("w-love"), AspectPreset.Square)
            .copy(background = 0xFFFFFFFF, gapPercent = 3f, marginPercent = 3f, cornerPercent = 0f)
    }
    val photos = rememberPhotos(heroProject, 420)
    Box(modifier) {
        Print(Modifier.offset(x = 0.dp, y = 30.dp).size(74.dp, 92.dp), -11f, b1) {
            AsyncImage(R.drawable.photo_forest, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
        Print(Modifier.align(Alignment.TopEnd).offset(x = (-2).dp, y = 12.dp).size(80.dp, 98.dp), 9f, b3) {
            AsyncImage(R.drawable.photo_boat, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
        Print(Modifier.align(Alignment.BottomStart).offset(x = 46.dp, y = 10.dp).size(108.dp), -3f, b2) {
            CollagePreview(heroProject, photos, Modifier.fillMaxSize(), fixedRatio = false)
        }
        Sparkle(Modifier.offset(x = 20.dp, y = 8.dp).size(15.dp), 0)
        Sparkle(Modifier.align(Alignment.TopEnd).offset(x = (-58).dp, y = 8.dp).size(10.dp), 1100)
        Sparkle(Modifier.align(Alignment.BottomEnd).offset(x = (-4).dp, y = (-40).dp).size(12.dp), 2200)
    }
}

@Composable
private fun Print(modifier: Modifier, rotation: Float, bob: Float, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(6.dp)
    Box(
        modifier.graphicsLayer { rotationZ = rotation; translationY = -5.dp.toPx() * bob }
            .shadow(14.dp, shape, spotColor = Color(0x80000000)).background(Color.White, shape).padding(5.dp).clip(RoundedCornerShape(3.dp)),
    ) {
        content()
        Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0x66FFFFFF), Color(0x1FFFFFFF), Color.Transparent), start = Offset.Zero, end = Offset(400f, 400f)), alpha = .5f))
    }
}

@Composable
private fun Sparkle(modifier: Modifier, delayMs: Int) {
    val t = rememberInfiniteTransition(label = "twinkle")
    val v by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1600), RepeatMode.Reverse, initialStartOffset = androidx.compose.animation.core.StartOffset(delayMs)), label = "v")
    Canvas(modifier.graphicsLayer { alpha = .25f + .75f * v; val s = .6f + .5f * v; scaleX = s; scaleY = s; rotationZ = 25f * v }) {
        val s = size.width / 24f
        val p = Path().apply {
            moveTo(12f * s, 0f)
            cubicTo(12.6f * s, 6.4f * s, 17.6f * s, 11.4f * s, 24f * s, 12f * s)
            cubicTo(17.6f * s, 12.6f * s, 12.6f * s, 17.6f * s, 12f * s, 24f * s)
            cubicTo(11.4f * s, 17.6f * s, 6.4f * s, 12.6f * s, 0f, 12f * s)
            cubicTo(6.4f * s, 11.4f * s, 11.4f * s, 6.4f * s, 12f * s, 0f)
            close()
        }
        drawPath(p, Color(0xFFFFF4C9))
    }
}

// --------------------------------------------------------------------------- quick layouts and cards

@Composable
private fun QuickRow(vm: AppViewModel) {
    val state = rememberLazyListState()
    LazyRow(
        Modifier.fillMaxWidth().edgeFade(state), state = state, contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(LayoutCatalog.quickIds.mapNotNull { LayoutCatalog.find(it) }, key = { it.id }) { layout ->
            val draft = remember(layout.id) { Samples.draft(layout, offset = LayoutCatalog.quickIds.indexOf(layout.id)) }
            val photos = rememberPhotos(draft, 260)
            Column(Modifier.width(92.dp).pressable({ vm.startNew(layout.id, samples = true) }, label = layout.title), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(92.dp).shadow(6.dp, RoundedCornerShape(14.dp), spotColor = Color(0x38101828)).clip(RoundedCornerShape(14.dp)).background(Color.White)) {
                    CollagePreview(draft, photos, Modifier.fillMaxSize(), fixedRatio = false)
                }
                CText(layout.title, size = 12.sp, weight = 600, maxLines = 1, lineHeight = 15.sp)
            }
        }
    }
}

private fun formatDay(millis: Long): String =
    DateTimeFormatter.ofPattern("MMM d", Locale.getDefault()).format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()))

@Composable
private fun ProjectCard(project: CollageProject, modifier: Modifier, onOpen: () -> Unit, onMenu: () -> Unit) {
    val c = CTheme.colors
    val photos = rememberPhotos(project, 420)
    val name = project.name.ifBlank { Strings.get(R.string.untitled) }
    Box(modifier) {
        Column(
            Modifier.fillMaxWidth().shadow(8.dp, RoundedCornerShape(18.dp), spotColor = Color(0x1A101828)).clip(RoundedCornerShape(18.dp)).background(c.card)
                .pressable(onOpen, role = Role.Button, label = Strings.get(R.string.open_item, name)).padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Box(
                Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(11.dp)).background(lerpColor(c.card, c.ink, .06f)).padding(9.dp),
                contentAlignment = Alignment.Center,
            ) {
                CollagePreview(project, photos, Modifier.shadow(3.dp, RoundedCornerShape(3.dp)).clip(RoundedCornerShape(3.dp)))
            }
            Column(Modifier.padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                CText(name, size = 15.sp, weight = 700, display = true, maxLines = 1, lineHeight = 19.sp)
                CText(formatDay(project.updatedAt), size = 11.sp, color = c.muted, lineHeight = 14.sp)
            }
        }
        Box(
            Modifier.align(Alignment.TopEnd).padding(top = 14.dp, end = 14.dp).size(34.dp).shadow(4.dp, CircleShape).clip(CircleShape).background(c.card.copy(alpha = .92f))
                .pressable(onMenu, label = Strings.get(R.string.more_options, name)),
            contentAlignment = Alignment.Center,
        ) { CIcon(Icon.More, size = 18.dp) }
    }
}
