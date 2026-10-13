package com.alchemedia.collage.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alchemedia.collage.ui.theme.CTheme

/** The screen's top bar: optional back button, a title (or any content) and actions. */
@Composable
fun AppBar(
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    backIcon: Icon = Icon.Back,
    backLabel: String = "Back",
    height: Dp = 52.dp,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier.fillMaxWidth().height(height).padding(start = if (onBack != null) 6.dp else 18.dp, end = 14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (onBack != null) IconBtn(backIcon, backLabel, onBack)
        content()
    }
}

@Composable
fun BarTitle(text: String, modifier: Modifier = Modifier) =
    CText(text, modifier, size = 23.sp, weight = 700, display = true, letterSpacing = (-.5).sp, maxLines = 1)

/** A plain single-line field with a placeholder. */
@Composable
fun CTextField(
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    textStyle: TextStyle = TextStyle.Default,
    placeholderColor: androidx.compose.ui.graphics.Color = CTheme.colors.muted,
    singleLine: Boolean = true,
    maxLength: Int = Int.MAX_VALUE,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
    imeAction: ImeAction = ImeAction.Done,
    onDone: (() -> Unit)? = null,
    leading: (@Composable () -> Unit)? = null,
) {
    val c = CTheme.colors
    BasicTextField(
        value = value,
        onValueChange = { if (it.length <= maxLength) onChange(it) },
        modifier = modifier,
        singleLine = singleLine,
        textStyle = textStyle.copy(color = c.ink),
        cursorBrush = SolidColor(c.accent),
        keyboardOptions = KeyboardOptions(capitalization = capitalization, imeAction = imeAction),
        keyboardActions = KeyboardActions(onDone = { onDone?.invoke() }, onSearch = { onDone?.invoke() }),
        decorationBox = { inner ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (leading != null) leading()
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty()) androidx.compose.material3.Text(placeholder, style = textStyle, color = placeholderColor, maxLines = 1)
                    inner()
                }
            }
        },
    )
}

@Composable
fun SearchField(value: String, onChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier) {
    val c = CTheme.colors
    val shape = RoundedCornerShape(15.dp)
    Box(modifier.fillMaxWidth().height(46.dp).clip(shape).background(c.card).border(1.dp, c.line, shape).padding(horizontal = 14.dp), contentAlignment = Alignment.CenterStart) {
        CTextField(
            value, onChange, Modifier.fillMaxWidth(), placeholder,
            textStyle = TextStyle(fontFamily = CTheme.fonts.body, fontSize = 15.sp, fontWeight = FontWeight.Medium),
            imeAction = ImeAction.Search, onDone = null,
            leading = { CIcon(Icon.Search, Modifier.padding(end = 12.dp), size = 20.dp, tint = c.muted) },
        )
    }
}
