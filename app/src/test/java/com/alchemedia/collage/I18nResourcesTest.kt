package com.alchemedia.collage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/** Reads the translation files themselves: every language must carry every string, with the same placeholders. */
class I18nResourcesTest {
    private val res = File("src/main/res")
    private val languages = listOf("values-es", "values-de", "values-fr", "values-it", "values-pt", "values-hi", "values-in")

    private class Strings(val strings: Map<String, String>, val plurals: Map<String, Map<String, String>>)

    private fun read(folder: String): Strings {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(File(res, "$folder/strings.xml"))
        val strings = LinkedHashMap<String, String>()
        val plurals = LinkedHashMap<String, Map<String, String>>()
        val root = doc.documentElement
        for (i in 0 until root.childNodes.length) {
            val n = root.childNodes.item(i) as? Element ?: continue
            when (n.tagName) {
                "string" -> if (n.getAttribute("translatable") != "false") strings[n.getAttribute("name")] = n.textContent
                "plurals" -> plurals[n.getAttribute("name")] = (0 until n.childNodes.length).mapNotNull { n.childNodes.item(it) as? Element }
                    .associate { it.getAttribute("quantity") to it.textContent }
            }
        }
        return Strings(strings, plurals)
    }

    private val placeholder = Regex("%\\d\\$[sd]|%d")
    private fun placeholders(s: String) = placeholder.findAll(s).map { it.value }.sorted().toList()

    private val english = read("values")

    @Test fun theEnglishSourceHasTextToTranslate() {
        assertTrue("strings: ${english.strings.size}", english.strings.size > 300)
        assertTrue(english.plurals.isNotEmpty())
    }

    @Test fun everyLanguageHasEveryString() {
        languages.forEach { lang ->
            val t = read(lang)
            val missing = english.strings.keys - t.strings.keys
            val extra = t.strings.keys - english.strings.keys
            assertTrue("$lang is missing $missing", missing.isEmpty())
            assertTrue("$lang has unknown strings $extra", extra.isEmpty())
            val missingPlurals = english.plurals.keys - t.plurals.keys
            assertTrue("$lang is missing plurals $missingPlurals", missingPlurals.isEmpty())
        }
    }

    @Test fun placeholdersMatchTheEnglish() {
        languages.forEach { lang ->
            val t = read(lang)
            english.strings.forEach { (key, en) ->
                assertEquals("$lang/$key placeholders", placeholders(en), placeholders(t.strings.getValue(key)))
            }
            english.plurals.forEach { (key, forms) ->
                t.plurals.getValue(key).forEach { (quantity, text) ->
                    assertEquals("$lang/$key/$quantity should hold the count once", 1, placeholders(text).size)
                }
                assertTrue("$lang/$key needs an 'other' form", "other" in t.plurals.getValue(key))
                assertTrue(forms.isNotEmpty())
            }
        }
    }

    @Test fun noTranslationIsBlank() {
        languages.forEach { lang ->
            val t = read(lang)
            t.strings.forEach { (key, text) -> assertTrue("$lang/$key is blank", text.isNotBlank()) }
            t.plurals.forEach { (key, forms) -> forms.forEach { (q, text) -> assertTrue("$lang/$key/$q is blank", text.isNotBlank()) } }
        }
    }

    @Test fun translationsAreActuallyTranslated() {
        // Words that stay the same in a language are fine, but a language that copies most of the English was not translated.
        languages.forEach { lang ->
            val t = read(lang)
            val same = english.strings.count { (key, en) -> t.strings[key] == en && en.any { it.isLetter() } }
            assertTrue("$lang copies $same of ${english.strings.size} English strings", same < english.strings.size * 0.2)
        }
    }

    @Test fun theLanguageListMatchesTheFolders() {
        val config = File(res, "xml/locales_config.xml").readText()
        listOf("en", "es", "de", "fr", "it", "pt", "hi", "id").forEach { assertTrue("$it missing from locales_config", "android:name=\"$it\"" in config) }
        languages.forEach { assertTrue("$it/strings.xml missing", File(res, "$it/strings.xml").exists()) }
    }
}
