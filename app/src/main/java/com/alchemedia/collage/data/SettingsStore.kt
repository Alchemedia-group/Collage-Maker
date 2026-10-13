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

    fun setPro(value: Boolean) {
        prefs.edit().putBoolean(KEY_PRO, value).apply()
        _pro.value = value
    }

    private companion object {
        const val KEY_PRO = "pro"
    }
}
