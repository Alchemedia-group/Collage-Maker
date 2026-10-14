package com.alchemedia.collage.model

import com.alchemedia.collage.Strings
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class ShadowSpec(val color: Long, val blur: Float, val dx: Float, val dy: Float)

object ItemType {
    const val TEXT = "text"
    const val STICKER = "sticker"
}

object StickerKind {
    const val EMOJI = "emoji"
    const val SHAPE = "shape"
}

/**
 * Something laid over the collage: a piece of text or a sticker. Position is the centre as a fraction of the canvas,
 * [size] is a fraction of the shorter canvas side, sizes of shadows and outlines are in "em" (multiples of the font size).
 */
@Serializable
data class CanvasItem(
    val id: String,
    val type: String,
    val x: Float = .5f,
    val y: Float = .5f,
    val size: Float = .11f,
    val rot: Float = 0f,
    val opacity: Float = 1f,
    // text
    val text: String = "",
    val font: String = "bric",
    val fill: Long? = 0xFFFFFFFF,
    /** When set, the text fades from [fill] to this colour along [gradientAngle] (0 = left to right, 90 = top to bottom). */
    val fill2: Long? = null,
    val gradientAngle: Float = 90f,
    val strokeColor: Long = 0xFF111827,
    val strokeW: Float = 0f,
    val shadow: ShadowSpec? = null,
    val bg: Long? = null,
    val spacing: Float = 0f,
    val curve: Float = 0f,
    val bold: Boolean = false,
    val align: String = "center",
    // sticker
    val kind: String = StickerKind.EMOJI,
    val emoji: String = "",
    val shape: String = "heart",
    val flip: Boolean = false,
    val stickerFill: Long = 0xFFF4C542,
) {
    val isText: Boolean get() = type == ItemType.TEXT

    companion object {
        fun newId(prefix: String) = prefix + UUID.randomUUID().toString().take(6)

        fun text(
            text: String = Strings.named("default_text", "Your text"), font: String = "bric", size: Float = .11f, y: Float = .5f, rot: Float = 0f,
            fill: Long? = 0xFFFFFFFF, strokeColor: Long = 0xFF111827, strokeW: Float = .09f, shadow: ShadowSpec? = null,
            bg: Long? = null, spacing: Float = 0f,
        ) = CanvasItem(
            id = newId("t"), type = ItemType.TEXT, y = y, size = size, rot = rot, text = text, font = font, fill = fill,
            strokeColor = strokeColor, strokeW = strokeW, shadow = shadow, bg = bg, spacing = spacing,
        )

        fun emoji(emoji: String) = CanvasItem(id = newId("s"), type = ItemType.STICKER, size = .16f, emoji = emoji, kind = StickerKind.EMOJI)

        fun shape(shape: String) = CanvasItem(id = newId("s"), type = ItemType.STICKER, size = .16f, shape = shape, kind = StickerKind.SHAPE)
    }
}
