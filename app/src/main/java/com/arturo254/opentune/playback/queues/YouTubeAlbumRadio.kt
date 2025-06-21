package com.arturo254.opentune.playback.queues

import androidx.media3.common.MediaItem
import com.arturo254.innertube.YouTube
import com.arturo254.innertube.models.WatchEndpoint
import com.arturo254.opentune.extensions.toMediaItem
import com.arturo254.opentune.models.MediaMetadata
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * A queue implementation that plays YouTube album tracks followed by radio-like recommendations.
 * 
 * @property playlistId The ID of the YouTube playlist/album to play
 * @property discoveryFilter Optional filter to determine which media items should be included
 */
class YouTubeAlbumRadio(
    private var playlistId: String,
    private val discoveryFilter: ((MediaItem) -> Boolean)? = null
) : Queue {
    
    companion object {
        private const val RADIO_PARAMS = "wAEB"
        private const val MIN_ITEMS_THRESHOLD = 5
    }

    override val preloadItem: MediaMetadata? = null

    private data class QueueState(
        var albumSongCount: Int = 0,
        var continuation: String? = null,
        var firstTimeLoaded: Boolean = false,
        var albumTitle: String = ""
    )

    private val state = QueueState()

    private val endpoint: WatchEndpoint
        get() = WatchEndpoint(
            playlistId = playlistId,
            params = RADIO_PARAMS
        )

    /**
     * Fetches and returns the initial queue status.
     * 
     * @throws IOException if there's an error fetching the album songs
     * @return Queue.Status containing the initial playlist state
     */
    override suspend fun getInitialStatus(): Queue.Status = withContext(IO) {
        try {
            val albumSongs = YouTube.albumSongs(playlistId).getOrThrow()
            require(albumSongs.isNotEmpty()) { "Album songs list cannot be empty" }
            
            state.apply {
                albumSongCount = albumSongs.size
                albumTitle = albumSongs.first().album?.name.orEmpty()
            }

            Queue.Status(
                title = state.albumTitle,
                items = filterItems(albumSongs.map { it.toMediaItem() }),
                mediaItemIndex = 0
            )
        } catch (e: Exception) {
            throw IOException("Failed to fetch album songs", e)
        }
    }

    /**
     * Checks if there are more items available to load.
     */
    override fun hasNextPage(): Boolean = 
        !state.firstTimeLoaded || state.continuation != null

    /**
     * Loads the next page of items in the queue.
     * 
     * @throws IOException if there's an error fetching the next items
     * @return List of MediaItems for the next page
     */
    override suspend fun nextPage(): List<MediaItem> = withContext(IO) {
        try {
            val nextResult = YouTube.next(endpoint, state.continuation).getOrThrow()
            state.continuation = nextResult.continuation

            val newItems = if (!state.firstTimeLoaded) {
                state.firstTimeLoaded = true
                nextResult.items
                    .drop(state.albumSongCount)
                    .map { it.toMediaItem() }
            } else {
                nextResult.items.map { it.toMediaItem() }
            }

            filterItems(newItems).also {
                if (it.size < MIN_ITEMS_THRESHOLD) {
                    prefetchNextBatch()
                }
            }
        } catch (e: Exception) {
            throw IOException("Failed to fetch next page", e)
        }
    }

    /**
     * Applies the discovery filter to the items if one is set.
     */
    private fun filterItems(items: List<MediaItem>): List<MediaItem> =
        discoveryFilter?.let { filter -> items.filter(filter) } ?: items

    /**
     * Prefetches the next batch of items in the background.
     */
    private suspend fun prefetchNextBatch() {
        // Implementation for prefetching next batch of items
        // This could be implemented based on your specific needs
    }
}
