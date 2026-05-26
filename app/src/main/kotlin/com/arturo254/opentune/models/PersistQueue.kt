/*
 * OpenTune Project Original (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package com.arturo254.opentune.models

import kotlinx.serialization.Serializable

@Serializable
data class PersistQueue(
    val title: String?,
    val items: List<MediaMetadata>,
    val mediaItemIndex: Int,
    val position: Long,
    val queueType: QueueType = QueueType.LIST,
    val queueData: QueueData? = null,
)

@Serializable
sealed class QueueType {
    @Serializable
    data object LIST : QueueType()

    @Serializable
    data object YOUTUBE : QueueType()

    @Serializable
    data object YOUTUBE_ALBUM_RADIO : QueueType()

    @Serializable
    data object LOCAL_ALBUM_RADIO : QueueType()
}

@Serializable
sealed class QueueData {
    @Serializable
    data class YouTubeData(
        val endpoint: String,
        val continuation: String? = null
    ) : QueueData()

    @Serializable
    data class YouTubeAlbumRadioData(
        val playlistId: String,
        val albumSongCount: Int = 0,
        val continuation: String? = null,
        val firstTimeLoaded: Boolean = false
    ) : QueueData()

    @Serializable
    data class LocalAlbumRadioData(
        val albumId: String,
        val startIndex: Int = 0,
        val playlistId: String? = null,
        val continuation: String? = null,
        val firstTimeLoaded: Boolean = false
    ) : QueueData()
}
