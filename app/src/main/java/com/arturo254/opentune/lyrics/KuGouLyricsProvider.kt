package com.arturo254.opentune.lyrics

import android.content.Context
import com.arturo254.kugou.KuGou
import com.arturo254.opentune.constants.EnableKugouKey
import com.arturo254.opentune.utils.dataStore
import com.arturo254.opentune.utils.get

object KuGouLyricsProvider : LyricsProvider {
    override val name = "Kugou"
    override fun isEnabled(context: Context): Boolean =
        context.dataStore[EnableKugouKey] ?: true

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int
    ): Result<String> =
        KuGou.getLyrics(title, artist, duration)

    override suspend fun getAllLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
        callback: (String) -> Unit
    ) {
        KuGou.getAllPossibleLyricsOptions(title, artist, duration, callback)
    }
}
