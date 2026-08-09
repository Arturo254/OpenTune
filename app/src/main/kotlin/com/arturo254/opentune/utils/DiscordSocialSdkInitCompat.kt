package com.arturo254.opentune.utils

import android.app.Activity
import android.content.Context

/**
 * Reflection wrapper around `com.discord.socialsdk.DiscordSocialSdkInit`, a plain static-methods
 * class shipped inside discord_partner_sdk.aar. That AAR is Discord's proprietary binary and
 * isn't committed to the repo (see app/libs/README.md), so builds without it locally present
 * can't reference the class directly — a compile-time import would break the whole app's build,
 * not just the Discord feature. Reflection keeps everything else buildable; when the class isn't
 * on the classpath, these calls just no-op/return null, matching [DiscordSocialSdkBridge]'s own
 * `isAvailable` degradation.
 */
object DiscordSocialSdkInitCompat {
    private const val CLASS_NAME = "com.discord.socialsdk.DiscordSocialSdkInit"

    private val clazz: Class<*>? = runCatching { Class.forName(CLASS_NAME) }.getOrNull()

    fun setEngineActivity(activity: Activity) {
        val cls = clazz ?: return
        runCatching {
            cls.getMethod("setEngineActivity", Activity::class.java).invoke(null, activity)
        }
    }

    fun getApplicationContext(): Context? {
        val cls = clazz ?: return null
        return runCatching {
            cls.getMethod("getApplicationContext").invoke(null) as? Context
        }.getOrNull()
    }
}
