package com.alchemedia.collage.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.Modifier
import com.alchemedia.collage.model.LayoutCatalog
import com.alchemedia.collage.ui.components.ToastHost
import com.alchemedia.collage.ui.designer.DesignerScreen
import com.alchemedia.collage.ui.designer.DesignerState
import com.alchemedia.collage.ui.editor.EditorScreen
import com.alchemedia.collage.ui.home.BrowseScreen
import com.alchemedia.collage.ui.home.HistoryScreen
import com.alchemedia.collage.ui.home.HomeScreen
import com.alchemedia.collage.ui.home.NewScreen
import com.alchemedia.collage.ui.sheets.CardSheet
import com.alchemedia.collage.ui.sheets.ExportSheet
import com.alchemedia.collage.ui.sheets.HistoryItemSheet
import com.alchemedia.collage.ui.sheets.LanguageSheet
import com.alchemedia.collage.ui.sheets.ProSheet
import com.alchemedia.collage.ui.theme.CTheme

/** The whole app: one screen at a time, full-screen overlays on top of it, bottom sheets on top of those. */
@Composable
fun AppRoot(vm: AppViewModel) {
    val c = CTheme.colors
    BackHandler(enabled = vm.canGoBack) { vm.back() }

    Box(Modifier.fillMaxSize().background(c.paper)) {
        val covered = vm.overlays.isNotEmpty() || vm.sheets.isNotEmpty()
        AnimatedContent(
            modifier = if (covered) Modifier.clearAndSetSemantics { } else Modifier,
            targetState = vm.screen,
            contentKey = { it::class },
            transitionSpec = { (fadeIn(tween(220)) + slideInVertically(tween(260)) { it / 40 }) togetherWith fadeOut(tween(120)) },
            label = "screen",
        ) { screen ->
            when (screen) {
                Screen.Home -> HomeScreen(vm)
                Screen.New -> NewScreen(vm)
                is Screen.Editor -> vm.editor?.let { ed -> key(screen.projectId) { EditorScreen(vm, ed) } }
            }
        }

        vm.overlays.toList().let { stack ->
          stack.forEachIndexed { index, overlay ->
            key(overlay) {
                OverlayFrame(covered = index < stack.lastIndex || vm.sheets.isNotEmpty()) {
                    when (overlay) {
                        is Overlay.Browse -> BrowseScreen(
                            vm, overlay.target,
                            selectedId = when (overlay.target) {
                                BrowseTarget.Editor -> vm.editor?.project?.layoutId
                                BrowseTarget.New -> vm.newLayoutId
                                else -> null
                            },
                            onChoose = { id -> chooseLayout(vm, overlay.target, id) },
                        )
                        is Overlay.Designer -> {
                            val state = remember(overlay) {
                                DesignerState(overlay.editId?.let { LayoutCatalog.find(it) } ?: overlay.baseId?.let { LayoutCatalog.find(it) }, overlay.editId)
                            }
                            DesignerScreen(vm, state, overlay.target)
                        }
                        Overlay.History -> HistoryScreen(vm)
                    }
                }
            }
          }
        }

        vm.sheets.toList().forEach { sheet ->
            key(sheet) {
                when (sheet) {
                    Sheet.Pro -> ProSheet(vm)
                    Sheet.Language -> LanguageSheet(vm)
                    Sheet.Export -> vm.editor?.let { ExportSheet(vm, it) }
                    is Sheet.Card -> CardSheet(vm, sheet.projectId)
                    is Sheet.HistoryItem -> HistoryItemSheet(vm, sheet.entryId)
                }
            }
        }

        ToastHost(vm.toast, onDismiss = vm::dismissToast)
    }
}

private fun chooseLayout(vm: AppViewModel, target: BrowseTarget, id: String) {
    val layout = LayoutCatalog.byId(id)
    vm.closeOverlay()
    when (target) {
        BrowseTarget.Designer -> vm.designerBase = id
        BrowseTarget.Editor -> vm.editor?.pickLayout(id)
        BrowseTarget.New -> vm.pickLayoutForNew(id)
        BrowseTarget.Home -> vm.startNew(id, samples = true)
    }
    vm.category = layout.cat
}

@Composable
private fun OverlayFrame(covered: Boolean, content: @Composable () -> Unit) {
    val c = CTheme.colors
    val visible = remember { MutableTransitionState(false).apply { targetState = true } }
    AnimatedVisibility(visible, enter = fadeIn(tween(200)) + slideInVertically(tween(240)) { it / 30 }) {
        Box(
            Modifier.fillMaxSize().background(c.paper)
                // Taps on empty parts of an overlay must not reach the screen underneath, and screen readers skip what is covered.
                .pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent() } }
                .then(if (covered) Modifier.clearAndSetSemantics { } else Modifier),
        ) { content() }
    }
}
