package com.alchemedia.collage.model

/** A bundled font. Variable fonts get their weight from [weight]; static files are used as they are. */
data class FontDef(
    val id: String,
    val name: String,
    val family: String,
    val file: String,
    val weight: Int,
    val variable: Boolean,
    val categories: List<String>,
)

object FontCatalog {
    val categories = listOf("pop" to "Popular", "bold" to "Bold", "script" to "Script", "hand" to "Handwritten", "serif" to "Serif", "clean" to "Clean", "fun" to "Fun")

    val all: List<FontDef> = listOf(
        FontDef("bric", "Bricolage", "Bricolage Grotesque", "BricolageGrotesque-var.ttf", 800, true, listOf("pop", "bold")),
        FontDef("pacifico", "Pacifico", "Pacifico", "Pacifico.ttf", 400, false, listOf("pop", "script")),
        FontDef("bebas", "Bebas Neue", "Bebas Neue", "BebasNeue.ttf", 400, false, listOf("pop", "bold")),
        FontDef("caveat", "Caveat", "Caveat", "Caveat-var.ttf", 700, true, listOf("pop", "hand")),
        FontDef("fredoka", "Fredoka", "Fredoka", "Fredoka-var.ttf", 600, true, listOf("pop", "fun")),
        FontDef("anton", "Anton", "Anton", "Anton.ttf", 400, false, listOf("bold")),
        FontDef("righteous", "Righteous", "Righteous", "Righteous.ttf", 400, false, listOf("bold", "fun")),
        FontDef("marker", "Marker", "Permanent Marker", "PermanentMarker.ttf", 400, false, listOf("bold", "hand")),
        FontDef("lobster", "Lobster", "Lobster", "Lobster.ttf", 400, false, listOf("script")),
        FontDef("dancing", "Dancing", "Dancing Script", "DancingScript-var.ttf", 700, true, listOf("script")),
        FontDef("shadows", "Shadows", "Shadows Into Light", "ShadowsIntoLight.ttf", 400, false, listOf("hand")),
        FontDef("playfair", "Playfair", "Playfair Display", "PlayfairDisplay-var.ttf", 700, true, listOf("serif")),
        FontDef("abril", "Abril", "Abril Fatface", "AbrilFatface.ttf", 400, false, listOf("serif", "bold")),
        FontDef("dmserif", "DM Serif", "DM Serif Display", "DMSerifDisplay.ttf", 400, false, listOf("serif")),
        FontDef("pixel", "Pixel", "Press Start 2P", "PressStart2P.ttf", 400, false, listOf("fun")),
        FontDef("figtree", "Figtree", "Figtree", "Figtree-var.ttf", 700, true, listOf("pop", "clean")),
        FontDef("oswald", "Oswald", "Oswald", "Oswald-var.ttf", 600, true, listOf("bold", "clean")),
        FontDef("archivo", "Archivo Black", "Archivo Black", "ArchivoBlack.ttf", 400, false, listOf("bold")),
        FontDef("bungee", "Bungee", "Bungee", "Bungee.ttf", 400, false, listOf("bold", "fun")),
        FontDef("staatliches", "Staatliches", "Staatliches", "Staatliches.ttf", 400, false, listOf("bold")),
        FontDef("luckiest", "Luckiest Guy", "Luckiest Guy", "LuckiestGuy.ttf", 400, false, listOf("bold", "fun")),
        FontDef("titan", "Titan One", "Titan One", "TitanOne.ttf", 400, false, listOf("bold", "fun")),
        FontDef("rozha", "Rozha One", "Rozha One", "RozhaOne.ttf", 400, false, listOf("bold", "serif")),
        FontDef("montserrat", "Montserrat", "Montserrat", "Montserrat-var.ttf", 800, true, listOf("pop", "clean", "bold")),
        FontDef("poppins", "Poppins", "Poppins", "Poppins-Bold.ttf", 700, false, listOf("pop", "clean")),
        FontDef("raleway", "Raleway", "Raleway", "Raleway-var.ttf", 800, true, listOf("clean")),
        FontDef("rubik", "Rubik", "Rubik", "Rubik-var.ttf", 700, true, listOf("clean", "bold")),
        FontDef("spacegro", "Space Grotesk", "Space Grotesk", "SpaceGrotesk-var.ttf", 700, true, listOf("clean")),
        FontDef("dmsans", "DM Sans", "DM Sans", "DMSans-var.ttf", 700, true, listOf("clean")),
        FontDef("spacemono", "Space Mono", "Space Mono", "SpaceMono-Bold.ttf", 700, false, listOf("clean", "fun")),
        FontDef("satisfy", "Satisfy", "Satisfy", "Satisfy.ttf", 400, false, listOf("pop", "script")),
        FontDef("greatvibes", "Great Vibes", "Great Vibes", "GreatVibes.ttf", 400, false, listOf("script")),
        FontDef("sacramento", "Sacramento", "Sacramento", "Sacramento.ttf", 400, false, listOf("script")),
        FontDef("kaushan", "Kaushan", "Kaushan Script", "KaushanScript.ttf", 400, false, listOf("script")),
        FontDef("yellowtail", "Yellowtail", "Yellowtail", "Yellowtail.ttf", 400, false, listOf("script")),
        FontDef("parisienne", "Parisienne", "Parisienne", "Parisienne.ttf", 400, false, listOf("script")),
        FontDef("allura", "Allura", "Allura", "Allura.ttf", 400, false, listOf("script")),
        FontDef("amatic", "Amatic SC", "Amatic SC", "AmaticSC-Bold.ttf", 700, false, listOf("hand")),
        FontDef("indie", "Indie Flower", "Indie Flower", "IndieFlower.ttf", 400, false, listOf("hand")),
        FontDef("rocksalt", "Rock Salt", "Rock Salt", "RockSalt.ttf", 400, false, listOf("hand")),
        FontDef("homemade", "Homemade", "Homemade Apple", "HomemadeApple.ttf", 400, false, listOf("hand")),
        FontDef("gloria", "Gloria", "Gloria Hallelujah", "GloriaHallelujah.ttf", 400, false, listOf("hand")),
        FontDef("patrick", "Patrick Hand", "Patrick Hand", "PatrickHand.ttf", 400, false, listOf("hand")),
        FontDef("cormorant", "Cormorant", "Cormorant Garamond", "CormorantGaramond-var.ttf", 700, true, listOf("serif")),
        FontDef("lora", "Lora", "Lora", "Lora-var.ttf", 700, true, listOf("serif")),
        FontDef("baskerville", "Baskerville", "Libre Baskerville", "LibreBaskerville-var.ttf", 700, true, listOf("serif")),
        FontDef("bodoni", "Bodoni", "Bodoni Moda", "BodoniModa-var.ttf", 700, true, listOf("serif")),
        FontDef("cinzel", "Cinzel", "Cinzel", "Cinzel-var.ttf", 700, true, listOf("pop", "serif")),
        FontDef("fraunces", "Fraunces", "Fraunces", "Fraunces-var.ttf", 800, true, listOf("serif")),
        FontDef("bangers", "Bangers", "Bangers", "Bangers.ttf", 400, false, listOf("pop", "fun")),
        FontDef("chewy", "Chewy", "Chewy", "Chewy.ttf", 400, false, listOf("fun")),
        FontDef("lilita", "Lilita One", "Lilita One", "LilitaOne.ttf", 400, false, listOf("fun")),
        FontDef("special", "Typewriter", "Special Elite", "SpecialElite.ttf", 400, false, listOf("fun")),
        FontDef("orbitron", "Orbitron", "Orbitron", "Orbitron-var.ttf", 700, true, listOf("fun")),
        FontDef("monoton", "Monoton", "Monoton", "Monoton.ttf", 400, false, listOf("fun")),
        FontDef("baloo", "Baloo", "Baloo 2", "Baloo2-var.ttf", 700, true, listOf("fun")),
    )

    fun byId(id: String): FontDef = all.firstOrNull { it.id == id } ?: all.first()

    fun inCategory(category: String): List<FontDef> = all.filter { category in it.categories }
}
