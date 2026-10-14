package com.alchemedia.collage.model

/** A canvas size named after where the picture is going. */
data class SocialPreset(val id: String, val name: String, val aspect: AspectPreset)

object SocialPresets {
    val all = listOf(
        SocialPreset("ig-post", "Instagram post", AspectPreset.Portrait45),
        SocialPreset("ig-story", "Instagram story", AspectPreset.Story916),
        SocialPreset("tiktok", "TikTok", AspectPreset.Story916),
        SocialPreset("pinterest", "Pinterest pin", AspectPreset.Portrait23),
        SocialPreset("facebook", "Facebook post", AspectPreset.Square),
        SocialPreset("whatsapp", "WhatsApp status", AspectPreset.Story916),
        SocialPreset("youtube", "YouTube thumbnail", AspectPreset.Wide169),
        SocialPreset("x-header", "X header", AspectPreset.Banner31),
        SocialPreset("print-a4", "A4 print", AspectPreset.A4),
        SocialPreset("print-46", "4×6 print", AspectPreset.Portrait23),
        SocialPreset("print-57", "5×7 print", AspectPreset.Print57),
    )

    /** The names of every preset that uses [aspect], e.g. "Instagram story · TikTok · WhatsApp status". */
    fun namesFor(aspect: AspectPreset): List<String> = all.filter { it.aspect == aspect }.map { it.name }
}
