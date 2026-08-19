package com.arturo254.opentune.utils

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Stores the Discord Social SDK access/refresh tokens in an Android-Keystore-backed encrypted
 * file, unlike the legacy kizzy path's plaintext DataStore [com.arturo254.opentune.constants.DiscordTokenKey].
 */
object DiscordSocialSdkTokenStore {
    private const val FILE_NAME = "discord_social_sdk_tokens"
    private const val KEY_ACCESS_TOKEN = "access_token"
    private const val KEY_REFRESH_TOKEN = "refresh_token"

    private fun prefs(context: Context) = EncryptedSharedPreferences.create(
        context,
        FILE_NAME,
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    fun save(context: Context, accessToken: String, refreshToken: String) {
        prefs(context).edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .apply()
    }

    fun accessToken(context: Context): String? = prefs(context).getString(KEY_ACCESS_TOKEN, null)

    fun refreshToken(context: Context): String? = prefs(context).getString(KEY_REFRESH_TOKEN, null)

    fun clear(context: Context) {
        prefs(context).edit().clear().apply()
    }
}
