package com.alchemedia.collage

import android.content.Context
import android.content.res.Configuration
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import java.util.Locale

/** A language the app is translated into. [tag] is a BCP-47 language tag; [nativeName] is how its speakers write it. */
data class AppLanguage(val tag: String, val nativeName: String)

/**
 * Looks up text in the current language from anywhere in the app (composables, view models, catalogues). The language is
 * the phone's unless the user picked one in the app ([setLanguage]). Before the application exists, as in plain unit tests,
 * nothing is available and lookups return their fallback or an empty string.
 */
object Strings {
    val languages = listOf(
        AppLanguage("en", "English"), AppLanguage("es", "Español"), AppLanguage("de", "Deutsch"), AppLanguage("fr", "Français"),
        AppLanguage("it", "Italiano"), AppLanguage("pt", "Português"), AppLanguage("hi", "हिन्दी"), AppLanguage("id", "Bahasa Indonesia"),
    )

    @Volatile private var app: Context? = null
    @Volatile private var localized: Context? = null
    @Volatile private var chosen: String = ""
    private val ids = java.util.concurrent.ConcurrentHashMap<String, Int>()

    fun init(context: Context) {
        app = context.applicationContext
        ids.clear()
        setLanguage(chosen)
    }

    /** The tag the user picked in the app, or "" while the app follows the phone's language. */
    val language: String get() = chosen

    /** Switches the app's language. "" goes back to the phone's language. Screens must be recreated to show it. */
    fun setLanguage(tag: String) {
        chosen = tag
        val base = app ?: return
        if (tag.isEmpty()) { localized = null; return }
        val locale = Locale.forLanguageTag(tag)
        Locale.setDefault(locale)
        localized = base.createConfigurationContext(Configuration(base.resources.configuration).apply { setLocale(locale) })
    }

    /** Gives an activity the chosen language, from its attachBaseContext. */
    fun wrap(base: Context): Context {
        if (chosen.isEmpty()) return base
        val locale = Locale.forLanguageTag(chosen)
        return base.createConfigurationContext(Configuration(base.resources.configuration).apply { setLocale(locale) })
    }

    private val context: Context? get() = localized ?: app

    fun get(@StringRes id: Int): String = context?.getString(id) ?: ""

    fun get(@StringRes id: Int, vararg args: Any): String = context?.getString(id, *args) ?: ""

    /** "1 photo" / "3 photos": the plural form of [id] for [count], with the count filled in. */
    fun plural(@PluralsRes id: Int, count: Int): String = context?.resources?.getQuantityString(id, count, count) ?: ""

    /**
     * Text looked up by its resource name, for items that carry an id instead of a reference (a filter, a layout). Returns
     * [fallback] when the name has no translation, such as for layouts the user made.
     */
    fun named(name: String, fallback: String): String {
        val c = context ?: return fallback
        val base = app ?: return fallback
        val id = ids.getOrPut(name) { base.resources.getIdentifier(name, "string", base.packageName) }
        return if (id != 0) c.getString(id) else fallback
    }
}
