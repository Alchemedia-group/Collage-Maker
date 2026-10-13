package com.alchemedia.collage.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alchemedia.collage.ui.theme.CTheme

/** A bottom sheet over a dimmed scrim. Tapping the scrim dismisses it. */
@Composable
fun SheetHost(onDismiss: () -> Unit, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val c = CTheme.colors
    val visible = remember { MutableTransitionState(false).apply { targetState = true } }
    Box(modifier.fillMaxSize()) {
        AnimatedVisibility(visible, enter = fadeIn(tween(160)), exit = fadeOut(tween(120))) {
            Box(
                Modifier.fillMaxSize().background(Color(0x8C0A0E16))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
            )
        }
        BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            val maxH = maxHeight * .94f
            AnimatedVisibility(
                visible,
                enter = slideInVertically(tween(260)) { it / 3 } + fadeIn(tween(200)),
                exit = slideOutVertically(tween(160)) { it / 3 } + fadeOut(tween(120)),
            ) {
                Column(
                    Modifier.fillMaxWidth().heightIn(max = maxH)
                        .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)).background(c.paper)
                        .pointerInput(Unit) { /* swallow taps so they do not reach the scrim */ }
                        .verticalScroll(rememberScrollState()).navigationBarsPadding()
                        .padding(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 26.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Box(Modifier.width(40.dp).height(5.dp).clip(CircleShape).background(c.line).align(Alignment.CenterHorizontally))
                    content()
                }
            }
        }
    }
}

@Composable
fun SheetTitle(text: String) = CText(text, size = 24.sp, weight = 800, display = true, letterSpacing = (-.6).sp)

/** One line of centred small print. */
@Composable
fun MetaText(text: String, modifier: Modifier = Modifier) =
    CText(text, modifier.fillMaxWidth(), size = 12.sp, weight = 500, color = CTheme.colors.muted, align = androidx.compose.ui.text.style.TextAlign.Center)

class ToastMessage(val text: String, val actionLabel: String? = null, val action: (() -> Unit)? = null, val id: Long = System.nanoTime())

/** The dark pill at the bottom of the screen, with an optional action such as Undo. */
@Composable
fun ToastHost(toast: ToastMessage?, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val c = CTheme.colors
    Box(modifier.fillMaxSize().navigationBarsPadding(), contentAlignment = Alignment.BottomCenter) {
        AnimatedVisibility(toast != null, enter = fadeIn(tween(140)) + slideInVertically(tween(180)) { it / 2 }, exit = fadeOut(tween(140))) {
            val t = toast ?: return@AnimatedVisibility
            Row(
                Modifier.padding(start = 16.dp, end = 16.dp, bottom = 26.dp).fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.ink)
                    .padding(horizontal = 15.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CText(t.text, Modifier.weight(1f), size = 13.sp, weight = 500, color = c.paper)
                if (t.actionLabel != null) {
                    Box(Modifier.pressable({ onDismiss(); t.action?.invoke() }).padding(horizontal = 6.dp, vertical = 4.dp)) {
                        CText(t.actionLabel.uppercase(), size = 13.sp, weight = 800, color = c.toastAccent, letterSpacing = .3.sp)
                    }
                }
            }
        }
    }
}
