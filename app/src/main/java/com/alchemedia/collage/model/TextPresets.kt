package com.alchemedia.collage.model

import com.alchemedia.collage.Strings

private const val INK = 0xFF111827L
private const val WHITE = 0xFFFFFFFFL

private fun luma(argb: Long): Float {
    val r = (argb shr 16 and 0xFF) / 255f
    val g = (argb shr 8 and 0xFF) / 255f
    val b = (argb and 0xFF) / 255f
    return r * .299f + g * .587f + b * .114f
}

/** White on dark fills, ink on light ones. */
fun contrastColor(argb: Long?): Long = if (argb != null && luma(argb) < .55f) WHITE else INK

data class TextPreset(val id: String, val name: String, val apply: (CanvasItem) -> CanvasItem) {
    val title: String get() = Strings.named("preset_$id", name)
}

/** A two-colour fade for text; [angle] runs 0 = left to right, 90 = top to bottom. */
data class TextGradient(val name: String, val from: Long, val to: Long, val angle: Float) {
    val title: String get() = Strings.named("grad_$name", name)
    fun on(item: CanvasItem): CanvasItem = item.copy(fill = from, fill2 = to, gradientAngle = angle)
}

object TextGradients {
    val all = listOf(
        TextGradient("Sunset", 0xFFFFB347, 0xFFFF2E93, 90f),
        TextGradient("Ocean", 0xFF3AD1FF, 0xFF2557D6, 90f),
        TextGradient("Berry", 0xFFFF4F8B, 0xFF8E5BFF, 45f),
        TextGradient("Gold", 0xFFFFF176, 0xFFE0A100, 90f),
        TextGradient("Mint", 0xFFA3E635, 0xFF2FD6A5, 0f),
        TextGradient("Fire", 0xFFFFD23F, 0xFFE63946, 90f),
        TextGradient("Candy", 0xFFFF8AD8, 0xFF3AD1FF, 0f),
        TextGradient("Dusk", 0xFF8E5BFF, 0xFFFF6B3D, 20f),
    )

    fun named(name: String): TextGradient = all.first { it.name == name }
}

object TextPresets {
    val all = listOf(
        TextPreset("plain", "Plain") { it.copy(strokeW = 0f, shadow = null, bg = null) },
        TextPreset("outline", "Outline") { it.copy(strokeColor = contrastColor(it.fill), strokeW = .09f, shadow = null, bg = null) },
        TextPreset("sticker", "Sticker") {
            it.copy(fill = if (it.fill == WHITE) INK else it.fill, strokeColor = WHITE, strokeW = .2f, shadow = ShadowSpec(0x66000000, .12f, 0f, .06f), bg = null)
        },
        TextPreset("shadow", "Shadow") { it.copy(strokeW = 0f, shadow = ShadowSpec(0x99000000, 0f, .06f, .06f), bg = null) },
        TextPreset("retro", "Retro") { it.copy(strokeW = 0f, shadow = ShadowSpec(0xFF2557D6, 0f, .07f, .07f), bg = null) },
        TextPreset("label", "Label") { it.copy(fill = INK, strokeW = 0f, bg = WHITE, shadow = ShadowSpec(0x4D000000, .1f, 0f, .05f)) },
        TextPreset("marker", "Marker") { it.copy(fill = INK, strokeW = 0f, bg = 0xFFFFD23F, shadow = null) },
        TextPreset("neon", "Neon") { it.copy(fill = WHITE, strokeW = 0f, bg = null, shadow = ShadowSpec(0xFFFF4FD8, .5f, 0f, 0f)) },
        TextPreset("hollow", "Hollow") { it.copy(fill = null, strokeColor = WHITE, strokeW = .07f, shadow = null, bg = null) },
        TextPreset("bubble", "Bubble") {
            it.copy(fill = WHITE, fill2 = null, strokeColor = 0xFFFF4F8B, strokeW = .16f, shadow = ShadowSpec(0x55000000, .12f, 0f, .05f), bg = null)
        },
        TextPreset("comic", "Comic") {
            it.copy(fill = 0xFFFFD23F, fill2 = null, strokeColor = INK, strokeW = .12f, shadow = ShadowSpec(INK, 0f, .07f, .07f), bg = null)
        },
        TextPreset("glow", "Glow") { it.copy(fill = WHITE, fill2 = null, strokeW = 0f, bg = null, shadow = ShadowSpec(0xFF3AD1FF, .5f, 0f, 0f)) },
        TextPreset("highlight", "Highlight") { it.copy(fill = INK, fill2 = null, strokeW = 0f, bg = 0xFFFF8AD8, shadow = null) },
        TextPreset("sunset", "Sunset") {
            TextGradients.named("Sunset").on(it).copy(strokeW = 0f, bg = null, shadow = ShadowSpec(0x59000000, .15f, 0f, .05f))
        },
        TextPreset("ocean", "Ocean") { TextGradients.named("Ocean").on(it).copy(strokeColor = WHITE, strokeW = .08f, bg = null, shadow = null) },
        TextPreset("gold", "Gold") {
            TextGradients.named("Gold").on(it).copy(strokeColor = 0xFF7A4B00, strokeW = .06f, bg = null, shadow = ShadowSpec(0x66000000, .1f, 0f, .05f))
        },
        TextPreset("candy", "Candy") { TextGradients.named("Candy").on(it).copy(strokeW = 0f, bg = null, shadow = ShadowSpec(0x40000000, .1f, 0f, .05f)) },
    )
}

data class TextTemplate(val name: String, val make: () -> List<CanvasItem>) {
    val title: String get() = Strings.named("tpl_" + name.lowercase().replace(" ", "_"), name)
}

object TextTemplates {
    private val softShadow = ShadowSpec(0x59000000, .15f, 0f, .05f)

    val all = listOf(
        TextTemplate("Happy Birthday") {
            listOf(
                CanvasItem.text("HAPPY", font = "playfair", size = .06f, y = .2f, rot = -6f, strokeW = 0f, spacing = .28f, shadow = softShadow),
                CanvasItem.text("Birthday", font = "pacifico", size = .17f, y = .32f, rot = -8f, strokeColor = 0xFFFF4F8B, strokeW = .12f),
            )
        },
        TextTemplate("Summer vibes") {
            listOf(
                CanvasItem.text("SUMMER", font = "bebas", size = .17f, y = .78f, strokeW = 0f, spacing = .08f, shadow = ShadowSpec(0x59000000, .12f, 0f, .05f)),
                CanvasItem.text("vibes", font = "lobster", size = .13f, y = .9f, rot = -4f, fill = 0xFFFFD23F, strokeW = .1f),
            )
        },
        TextTemplate("Best day ever") {
            listOf(CanvasItem.text("best day\never", font = "caveat", size = .14f, rot = -5f, strokeW = 0f, shadow = ShadowSpec(0x80000000, .12f, 0f, .05f)))
        },
        TextTemplate("Love you") {
            listOf(CanvasItem.text("love you", font = "dancing", size = .16f, rot = -6f, strokeColor = 0xFFE63977, strokeW = .12f))
        },
        TextTemplate("Memories") {
            listOf(
                CanvasItem.text("MEMORIES", font = "anton", size = .12f, y = .46f, strokeW = 0f, spacing = .06f),
                CanvasItem.text("2026", font = "bric", size = .06f, y = .57f, fill = 0xFFFACC15, strokeW = 0f, spacing = .5f),
            )
        },
        TextTemplate("Enjoy") {
            listOf(CanvasItem.text("Enjoy", font = "pixel", size = .075f, rot = 4f, fill = INK, strokeW = 0f, bg = WHITE, shadow = ShadowSpec(0x59000000, .1f, 0f, .05f)))
        },
        TextTemplate("Wedding") {
            listOf(
                CanvasItem.text("Mr & Mrs", font = "greatvibes", size = .16f, y = .42f, strokeW = 0f, shadow = softShadow),
                CanvasItem.text("12 · 06 · 2026", font = "cinzel", size = .04f, y = .55f, strokeW = 0f, spacing = .4f, shadow = softShadow),
            )
        },
        TextTemplate("Adventure") {
            listOf(
                TextGradients.named("Sunset").on(CanvasItem.text("ADVENTURE", font = "bebas", size = .15f, y = .42f, strokeW = 0f, spacing = .05f, shadow = softShadow)),
                CanvasItem.text("awaits", font = "dancing", size = .09f, y = .55f, rot = -4f, strokeW = 0f, shadow = softShadow),
            )
        },
        TextTemplate("Weekend") {
            listOf(
                TextGradients.named("Ocean").on(CanvasItem.text("Weekend", font = "lobster", size = .15f, y = .44f, rot = -5f, strokeColor = WHITE, strokeW = .1f)),
                CanvasItem.text("vibes only", font = "caveat", size = .08f, y = .56f, rot = -5f, strokeW = 0f, shadow = softShadow),
            )
        },
        TextTemplate("Thank you") {
            listOf(TextGradients.named("Berry").on(CanvasItem.text("thank you", font = "sacramento", size = .2f, rot = -6f, strokeColor = WHITE, strokeW = .1f)))
        },
        TextTemplate("Year in review") {
            listOf(
                TextGradients.named("Gold").on(CanvasItem.text("2026", font = "anton", size = .22f, y = .4f, strokeW = 0f, shadow = softShadow)),
                CanvasItem.text("YEAR IN REVIEW", font = "oswald", size = .05f, y = .55f, strokeW = 0f, spacing = .3f, shadow = softShadow),
            )
        },
        TextTemplate("Squad goals") {
            listOf(TextGradients.named("Candy").on(CanvasItem.text("SQUAD\nGOALS", font = "bungee", size = .1f, strokeColor = INK, strokeW = .08f)))
        },
    )
}

object TextColors {
    val palette = listOf(
        0xFFFFFFFF, 0xFF111827, 0xFFFF4F8B, 0xFFFF6B3D, 0xFFFFD23F, 0xFFA3E635, 0xFF2FD6A5,
        0xFF3AD1FF, 0xFF2557D6, 0xFF8E5BFF, 0xFFFF8AD8, 0xFFB9B9CC, 0xFF8B5A2B, 0xFFE63946,
    )
    val backgrounds = listOf(
        0xFFFFFFFF, 0xFFF3F4F6, 0xFFD1D5DB, 0xFF6B7280, 0xFF374151, 0xFF111827, 0xFF000000, 0xFFF1E7DC,
        0xFFDBE4F5, 0xFFE3ECE6, 0xFFF4DDE3, 0xFFFBE7C6, 0xFF2557D6, 0xFF1F3A5F, 0xFF0F766E,
    )
}
