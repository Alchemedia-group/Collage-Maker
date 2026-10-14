package com.alchemedia.collage.data

import android.content.Context
import android.content.Intent
import android.net.Uri

/** A social app the finished picture can be sent to in one tap. */
data class ShareTarget(val id: String, val label: String, val packageName: String)

object ShareTargets {
    val all = listOf(
        ShareTarget("instagram", "Instagram", "com.instagram.android"),
        ShareTarget("tiktok", "TikTok", "com.zhiliaoapp.musically"),
        ShareTarget("whatsapp", "WhatsApp", "com.whatsapp"),
        ShareTarget("facebook", "Facebook", "com.facebook.katana"),
        ShareTarget("pinterest", "Pinterest", "com.pinterest"),
        ShareTarget("x", "X", "com.twitter.android"),
        ShareTarget("snapchat", "Snapchat", "com.snapchat.android"),
        ShareTarget("telegram", "Telegram", "org.telegram.messenger"),
    )

    /** The apps from [all] that are installed on this device. */
    @Suppress("DEPRECATION")
    fun installed(context: Context): List<ShareTarget> {
        val pm = context.packageManager
        return all.filter { t -> runCatching { pm.getApplicationInfo(t.packageName, 0) }.isSuccess }
    }

    /** A "send this picture" intent, aimed at one app when [packageName] is given and at the system chooser otherwise. */
    fun sendIntent(uri: Uri, packageName: String? = null): Intent =
        Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            packageName?.let { setPackage(it) }
        }
}
