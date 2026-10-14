package com.alchemedia.collage.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Small preferences. [pro] mirrors the subscription state; the billing layer is the only writer. */
class SettingsStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val _pro = MutableStateFlow(prefs.getBoolean(KEY_PRO, false))
    val pro: StateFlow<Boolean> = _pro.asStateFlow()

    private val _favouriteFonts = MutableStateFlow(prefs.getStringSet(KEY_FAVOURITE_FONTS, emptySet()).orEmpty().toSet())

    /** Ids of the fonts the user starred, shown together under "Favourites" in the font picker. */
    val favouriteFonts: StateFlow<Set<String>> = _favouriteFonts.asStateFlow()

    fun toggleFavouriteFont(id: String) {
        val next = _favouriteFonts.value.let { if (id in it) it - id else it + id }
        prefs.edit().putStringSet(KEY_FAVOURITE_FONTS, next).apply()
        _favouriteFonts.value = next
    }

    private val _language = MutableStateFlow(prefs.getString(KEY_LANGUAGE, "").orEmpty())

    /** The language picked in the app, as a tag such as "es", or "" to follow the phone. */
    val language: StateFlow<String> = _language.asStateFlow()

    fun setLanguage(tag: String) {
        prefs.edit().putString(KEY_LANGUAGE, tag).apply()
        _language.value = tag
    }

    /** Debug builds only: keeps Pro on even when Play says there is no subscription, so the Pro features can be tried. */
    var debugPro: Boolean
        get() = prefs.getBoolean(KEY_DEBUG_PRO, false)
        set(value) { prefs.edit().putBoolean(KEY_DEBUG_PRO, value).apply() }

    fun setPro(value: Boolean) {
        prefs.edit().putBoolean(KEY_PRO, value).apply()
        _pro.value = value
    }

    private companion object {
        const val KEY_PRO = "pro"
        const val KEY_DEBUG_PRO = "debug_pro"
        const val KEY_FAVOURITE_FONTS = "favourite_fonts"
        const val KEY_LANGUAGE = "language"
    }
}
