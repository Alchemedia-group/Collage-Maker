package com.alchemedia.collage.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.alchemedia.collage.R
import com.alchemedia.collage.Strings
import com.alchemedia.collage.TestAssets
import com.alchemedia.collage.model.AspectPreset
import com.alchemedia.collage.model.EmojiCatalog
import com.alchemedia.collage.model.FontCatalog
import com.alchemedia.collage.model.LayoutCatalog
import com.alchemedia.collage.model.LayoutCategories
import com.alchemedia.collage.model.SocialPresets
import com.alchemedia.collage.model.TextGradients
import com.alchemedia.collage.model.TextPresets
import com.alchemedia.collage.model.TextTemplates
import com.alchemedia.collage.render.Filters
import com.alchemedia.collage.render.Frames
import com.alchemedia.collage.render.Textures
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Checks the app really speaks each language: strings, plurals, every catalogue name, and the switcher on Home. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w390dp-h844dp-xhdpi")
class LanguageTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    private lateinit var h: UiHarness

    private val saveIn = mapOf("en" to "Save", "es" to "Guardar", "de" to "Speichern", "fr" to "Enregistrer", "it" to "Salva", "pt" to "Salvar", "hi" to "सहेजें", "id" to "Simpan")

    @Before fun setUp() {
        TestAssets.loadCatalog()
        LayoutCatalog.setMine(emptyList())
        h = UiHarness(rule)
    }

    @After fun tearDown() { Strings.setLanguage("") }

    @Test fun everyLanguageTranslatesTheBasics() {
        saveIn.forEach { (tag, word) ->
            Strings.setLanguage(tag)
            assertEquals(tag, word, Strings.get(R.string.save))
        }
        Strings.setLanguage("")
        assertEquals("Save", Strings.get(R.string.save))
    }

    @Test fun formatsAndPluralsFollowTheLanguage() {
        Strings.setLanguage("es")
        assertEquals("Ver todos (110)", Strings.get(R.string.see_all, 110))
        assertEquals("1 foto", Strings.plural(R.plurals.p_photos, 1))
        assertEquals("3 fotos", Strings.plural(R.plurals.p_photos, 3))
        Strings.setLanguage("de")
        assertEquals("1 Foto", Strings.plural(R.plurals.p_photos, 1))
        assertEquals("4 Fotos", Strings.plural(R.plurals.p_photos, 4))
        Strings.setLanguage("id")
        assertEquals("1 foto", Strings.plural(R.plurals.p_photos, 1))
        assertEquals("5 foto", Strings.plural(R.plurals.p_photos, 5))
        Strings.setLanguage("hi")
        assertEquals("2 फ़ोटो", Strings.plural(R.plurals.p_photos, 2))
    }

    @Test fun layoutNamesAreTranslatedExceptNumbersAndWords() {
        Strings.setLanguage("es")
        assertEquals("Individual", LayoutCatalog.byId("g1x1").title)
        assertEquals("4 corazones", LayoutCatalog.byId("hearts4").title)
        assertEquals("2 × 2", LayoutCatalog.byId("g2x2").title)
        assertEquals("LOVE", LayoutCatalog.byId("w-love").title) // the letters shown in its cells
        Strings.setLanguage("de")
        assertEquals("Einzeln", LayoutCatalog.byId("g1x1").title)
        Strings.setLanguage("")
        assertEquals("Single", LayoutCatalog.byId("g1x1").title)
    }

    @Test fun everyNamedLayoutHasATranslation() {
        // Names without letters (2 × 2) and the lettered word layouts are meant to stay as they are; any other name must be translated.
        Strings.setLanguage("fr")
        val untranslatable = Regex("^[\\d\\s×·+]+$")
        LayoutCatalog.builtInLayouts.filter { !it.id.startsWith("w-") && !untranslatable.matches(it.name) }.forEach { l ->
            assertNotEquals("${l.id} (${l.name}) has no entry in the translation tables", "", Strings.named("layout_${l.id}", ""))
        }
    }

    @Test fun layoutsMadeByTheUserKeepTheirOwnName() {
        Strings.setLanguage("de")
        val mine = LayoutCatalog.byId("g1x1").copy(id = "my-1", name = "Single", mine = true)
        assertEquals("Single", mine.title)
    }

    @Test fun searchFindsLayoutsByTheirTranslatedName() {
        Strings.setLanguage("es")
        assertTrue(LayoutCatalog.search("corazón", com.alchemedia.collage.model.CountFilter.Any, null).any { it.id == "hearts4" })
        assertTrue("accents are optional", LayoutCatalog.search("corazon", com.alchemedia.collage.model.CountFilter.Any, null).any { it.id == "hearts4" })
        Strings.setLanguage("fr")
        assertTrue(LayoutCatalog.search("coeur", com.alchemedia.collage.model.CountFilter.Any, null).any { it.id == "hearts4" })
        Strings.setLanguage("es")
        assertTrue("English words still work", LayoutCatalog.search("heart", com.alchemedia.collage.model.CountFilter.Any, null).any { it.id == "hearts4" })
    }

    @Test fun everyCatalogueItemHasANameInEveryLanguage() {
        saveIn.keys.forEach { tag ->
            Strings.setLanguage(tag)
            val names = Filters.all.map { it.title } + Textures.all.map { it.title } + Frames.all.map { it.title } + TextPresets.all.map { it.title } +
                TextGradients.all.map { it.title } + TextTemplates.all.map { it.title } + SocialPresets.all.map { it.title } +
                EmojiCatalog.categories.map { it.title } + FontCatalog.categories.map { FontCatalog.categoryTitle(it.first) } +
                LayoutCategories.all.map { LayoutCategories.label(it.first) } + AspectPreset.entries.map { it.label }
            names.forEach { assertTrue("$tag has a blank name", it.isNotBlank()) }
            if (tag != "en") {
                // a translated name is never the empty lookup fallback "layout_..." and the filter row is not all English
                val english = Filters.all.count { it.title == it.name }
                assertTrue("$tag left $english of ${Filters.all.size} filters in English", english < Filters.all.size / 2)
            }
        }
    }

    @Test fun homeSpeaksTheChosenLanguage() {
        Strings.setLanguage("es")
        h.start()
        rule.onNodeWithText("Diseña tu propio diseño").assertIsDisplayed()
        rule.onNodeWithText("Nuevo collage").assertIsDisplayed()
    }

    @Test fun theGlobeOnHomeOpensTheLanguageList() {
        h.start()
        rule.onNodeWithContentDescription("Language").performClick(); h.settle(300)
        assertTrue(h.vm.sheets.isNotEmpty())
        // the list scrolls inside the sheet, so the later rows may sit below the visible part
        listOf("Phone language", "English", "Español", "Deutsch").forEach { rule.onNodeWithText(it).assertIsDisplayed() }
        listOf("Français", "Italiano", "Português", "हिन्दी", "Bahasa Indonesia").forEach { rule.onNodeWithText(it).assertExists() }
    }

    @Test fun pickingALanguageSavesItAndSwitchesTheApp() {
        h.start()
        rule.onNodeWithContentDescription("Language").performClick(); h.settle(300)
        rule.onNodeWithText("Deutsch").performClick(); h.settle(300)
        assertEquals("de", h.app.settings.language.value)
        assertEquals("de", Strings.language)
        assertEquals("Speichern", Strings.get(R.string.save))
    }
}
