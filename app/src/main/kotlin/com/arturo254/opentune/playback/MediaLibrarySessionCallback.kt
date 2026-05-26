/*
 * OpenTune Project Original (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package com.arturo254.opentune.playback

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.KeyEvent
import android.os.Bundle
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.offline.Download
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionError
import androidx.media3.session.SessionResult
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.arturo254.opentune.R
import com.arturo254.opentune.constants.AudioQuality
import com.arturo254.opentune.constants.AudioQualityKey
import com.arturo254.opentune.constants.AudioNormalizationKey
import com.arturo254.opentune.constants.AudioCrossfadeDurationKey
import com.arturo254.opentune.constants.AutoLoadMoreKey
import com.arturo254.opentune.constants.AutoPlayKey
import com.arturo254.opentune.constants.AutoQualityKey
import com.arturo254.opentune.constants.DiscordTokenKey
import com.arturo254.opentune.constants.EnableDiscordRPCKey
import com.arturo254.opentune.constants.EnableLastFMScrobblingKey
import com.arturo254.opentune.constants.LastFMSessionKey
import com.arturo254.opentune.constants.ListenBrainzEnabledKey
import com.arturo254.opentune.constants.ListenBrainzTokenKey
import com.arturo254.opentune.constants.EqualizerBassBoostEnabledKey
import com.arturo254.opentune.constants.EqualizerBassBoostStrengthKey
import com.arturo254.opentune.constants.MediaSessionConstants
import com.arturo254.opentune.constants.MediaButtonBehaviorKey
import com.arturo254.opentune.constants.NotificationInCarKey
import com.arturo254.opentune.constants.StopOnBluetoothDisconnectKey
import com.arturo254.opentune.constants.SongSortType
import com.arturo254.opentune.db.MusicDatabase
import com.arturo254.opentune.db.entities.PlaylistEntity
import com.arturo254.opentune.db.entities.SearchHistory
import com.arturo254.opentune.db.entities.Song
import com.arturo254.opentune.extensions.metadata
import com.arturo254.opentune.extensions.currentMetadata
import com.arturo254.opentune.extensions.toMediaItem
import com.arturo254.opentune.innertube.YouTube
import com.arturo254.opentune.innertube.models.SongItem
import com.arturo254.opentune.extensions.toggleRepeatMode
import com.arturo254.opentune.ui.screens.settings.DiscordPresenceManager
import com.arturo254.opentune.utils.PreferenceStore
import com.arturo254.opentune.utils.dataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.guava.future
import kotlinx.coroutines.plus
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import android.os.SystemClock
import kotlin.math.min

class MediaLibrarySessionCallback
@Inject
constructor(
    @ApplicationContext val context: Context,
    val database: MusicDatabase,
    val downloadUtil: DownloadUtil,
) : MediaLibrarySession.Callback {
    private val scope = CoroutineScope(Dispatchers.Main) + Job()
    var musicService: MusicService? = null
    var toggleLike: () -> Unit = {}
    var toggleStartRadio: () -> Unit = {}
    var toggleLibrary: () -> Unit = {}
    var toggleDownload: () -> Unit = {}
    var denyPlayerCommands: () -> Boolean = { false }

    private var cachedRootChildren: List<MediaItem>? = null
    private var rootChildrenCacheGeneration: Long = 0L
    private var persistentQueueCacheTime: Long = 0L
    private var cachedPersistentQueueItems: List<MediaItem>? = null

    override fun onPlayerCommandRequest(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
        playerCommand: Int,
    ): Int {
        if (denyPlayerCommands() && playerCommand in DENIED_GUEST_COMMANDS) {
            return SessionResult.RESULT_ERROR_PERMISSION_DENIED
        }
        return SessionResult.RESULT_SUCCESS
    }

    private fun browsableExtras(
        browsableHint: Int = CONTENT_STYLE_GRID_ITEM,
        playableHint: Int = CONTENT_STYLE_LIST_ITEM,
    ) = Bundle().apply {
        putBoolean(EXTRA_CONTENT_STYLE_SUPPORTED, true)
        putInt(EXTRA_CONTENT_STYLE_BROWSABLE_HINT, browsableHint)
        putInt(EXTRA_CONTENT_STYLE_PLAYABLE_HINT, playableHint)
        putBoolean("com.google.android.gms.car.media.ALWAYS_RESOLVE_URI", true)
    }

    private fun playableExtras(
        playableHint: Int = CONTENT_STYLE_LIST_ITEM,
    ) = Bundle().apply {
        putBoolean(EXTRA_CONTENT_STYLE_SUPPORTED, true)
        putInt(EXTRA_CONTENT_STYLE_PLAYABLE_HINT, playableHint)
        putBoolean("com.google.android.gms.car.media.ALWAYS_RESOLVE_URI", true)
    }

    override fun onConnect(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
    ): MediaSession.ConnectionResult {
        val connectionResult = super.onConnect(session, controller)

        val commands = connectionResult.availableSessionCommands
            .buildUpon()
            .apply {
                add(MediaSessionConstants.CommandToggleLike)
                add(MediaSessionConstants.CommandToggleStartRadio)
                add(MediaSessionConstants.CommandToggleLibrary)
                add(MediaSessionConstants.CommandToggleShuffle)
                add(MediaSessionConstants.CommandToggleRepeatMode)
                add(MediaSessionConstants.CommandToggleDownload)
            }
            .build()

        return MediaSession.ConnectionResult.accept(
            commands,
            connectionResult.availablePlayerCommands,
        )
    }

    override fun onDisconnected(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
    ) {
        musicService?.refreshPlaybackNotification()
    }

    override fun onMediaButtonEvent(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
        intent: Intent,
    ): Boolean {
        val behavior = PreferenceStore.get(MediaButtonBehaviorKey) ?: "all"
        if (behavior == "all") return false
        val event = intent.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT) ?: return false
        if (event.keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) return false
        return true
    }

    override fun onCustomCommand(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
        customCommand: SessionCommand,
        args: Bundle,
    ): ListenableFuture<SessionResult> {
        if (denyPlayerCommands() && customCommand.customAction in DENIED_GUEST_CUSTOM_COMMANDS) {
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_ERROR_PERMISSION_DENIED))
        }
        when (customCommand.customAction) {
            MediaSessionConstants.ACTION_TOGGLE_LIKE -> toggleLike()
            MediaSessionConstants.ACTION_TOGGLE_START_RADIO -> toggleStartRadio()
            MediaSessionConstants.ACTION_TOGGLE_LIBRARY -> toggleLibrary()
            MediaSessionConstants.ACTION_TOGGLE_SHUFFLE -> session.player.shuffleModeEnabled =
                !session.player.shuffleModeEnabled

            MediaSessionConstants.ACTION_TOGGLE_REPEAT_MODE -> session.player.toggleRepeatMode()
            MediaSessionConstants.ACTION_TOGGLE_DOWNLOAD -> toggleDownload()
        }
        return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
    }

    override fun onGetLibraryRoot(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        params: MediaLibraryService.LibraryParams?,
    ): ListenableFuture<LibraryResult<MediaItem>> =
        Futures.immediateFuture(
            LibraryResult.ofItem(
                MediaItem
                    .Builder()
                    .setMediaId(MusicService.ROOT)
                    .setMediaMetadata(
                        MediaMetadata
                            .Builder()
                            .setTitle(context.getString(R.string.app_name))
                            .setIsPlayable(false)
                            .setIsBrowsable(true)
                            .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                            .setExtras(browsableExtras())
                            .build(),
                    ).build(),
                params,
            ),
        )

    override fun onSearch(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        query: String,
        params: MediaLibraryService.LibraryParams?,
    ): ListenableFuture<LibraryResult<Void>> {
        Log.d(TAG, "onSearch: query='$query'")
        session.notifySearchResultChanged(browser, query, 10, params)
        return Futures.immediateFuture(LibraryResult.ofVoid(params))
    }

    override fun onGetSearchResult(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        query: String,
        page: Int,
        pageSize: Int,
        params: MediaLibraryService.LibraryParams?,
    ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> =
        scope.future(Dispatchers.IO) {
            try {
                val q = query.trim()
                if (q.isBlank() || pageSize <= 0 || page < 0) {
                    return@future LibraryResult.ofItemList(emptyList(), params)
                }

                runCatching { database.insert(SearchHistory(query = q)) }

                val requested = (page + 1) * pageSize
                val items = ArrayList<MediaItem>(min(requested, 200))

                val songsDeferred = async(Dispatchers.IO) {
                    runCatching {
                        withTimeout(10_000L) {
                            database.searchSongs(q, previewSize = requested).first()
                                .map { it.toMediaItem(MusicService.SONG).buildUpon()
                                    .setRequestMetadata(MediaItem.RequestMetadata.Builder().setSearchQuery(q).build())
                                    .build()
                                }
                        }
                    }.getOrDefault(emptyList())
                }

                val artistsDeferred = async(Dispatchers.IO) {
                    runCatching {
                        withTimeout(10_000L) {
                            database.searchArtists(q, previewSize = requested).first()
                                .map { artist ->
                                    browsableMediaItem(
                                        "${MusicService.ARTIST}/${artist.id}",
                                        artist.title,
                                        context.resources.getQuantityString(
                                            R.plurals.n_song, artist.songCount, artist.songCount,
                                        ),
                                        artist.thumbnailUrl?.toUri(),
                                        MediaMetadata.MEDIA_TYPE_ARTIST,
                                    ).buildUpon()
                                        .setRequestMetadata(MediaItem.RequestMetadata.Builder().setSearchQuery(q).build())
                                        .build()
                                }
                        }
                    }.getOrDefault(emptyList())
                }

                val albumsDeferred = async(Dispatchers.IO) {
                    runCatching {
                        withTimeout(10_000L) {
                            database.searchAlbums(q, previewSize = requested).first()
                                .map { album ->
                                    browsableMediaItem(
                                        "${MusicService.ALBUM}/${album.id}",
                                        album.title,
                                        album.artists.joinToString { it.name },
                                        album.thumbnailUrl?.toUri(),
                                        MediaMetadata.MEDIA_TYPE_ALBUM,
                                    ).buildUpon()
                                        .setRequestMetadata(MediaItem.RequestMetadata.Builder().setSearchQuery(q).build())
                                        .build()
                                }
                        }
                    }.getOrDefault(emptyList())
                }

                val playlistsDeferred = async(Dispatchers.IO) {
                    runCatching {
                        withTimeout(10_000L) {
                            database.searchPlaylists(q, previewSize = requested).first()
                                .map { playlist ->
                                    browsableMediaItem(
                                        "${MusicService.PLAYLIST}/${playlist.id}",
                                        playlist.title,
                                        context.resources.getQuantityString(
                                            R.plurals.n_song, playlist.songCount, playlist.songCount,
                                        ),
                                        playlist.thumbnails.firstOrNull()?.toUri(),
                                        MediaMetadata.MEDIA_TYPE_PLAYLIST,
                                    ).buildUpon()
                                        .setRequestMetadata(MediaItem.RequestMetadata.Builder().setSearchQuery(q).build())
                                        .build()
                                }
                        }
                    }.getOrDefault(emptyList())
                }

                items += songsDeferred.await()
                if (items.size < requested) items += artistsDeferred.await().take(requested - items.size)
                if (items.size < requested) items += albumsDeferred.await().take(requested - items.size)
                if (items.size < requested) items += playlistsDeferred.await().take(requested - items.size)

                if (items.size < 5) {
                    runCatching {
                        YouTube.search(q, YouTube.SearchFilter.FILTER_SONG).getOrNull()
                            ?.items
                            ?.filterIsInstance<SongItem>()
                            ?.take(5 - items.size)
                            ?.map { it.toMediaItem() }
                            ?.forEach { items += it }
                    }
                }

                val from = page * pageSize
                if (from >= items.size) return@future LibraryResult.ofItemList(emptyList(), params)
                val to = min(from + pageSize, items.size)

                LibraryResult.ofItemList(items.subList(from, to), params)
            } catch (e: TimeoutCancellationException) {
                Log.w(TAG, "onGetSearchResult timed out for query: $query", e)
                LibraryResult.ofItemList(emptyList(), params)
            } catch (e: Exception) {
                Log.e(TAG, "onGetSearchResult failed for query: $query", e)
                LibraryResult.ofItemList(emptyList(), params)
            }
        }

    override fun onSubscribe(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        parentId: String,
        params: MediaLibraryService.LibraryParams?,
    ): ListenableFuture<LibraryResult<Void>> {
        Log.d(TAG, "onSubscribe: parentId=$parentId")
        return Futures.immediateFuture(LibraryResult.ofVoid(params))
    }

    override fun onUnsubscribe(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        parentId: String,
    ): ListenableFuture<LibraryResult<Void>> {
        Log.d(TAG, "onUnsubscribe: parentId=$parentId")
        return Futures.immediateFuture(LibraryResult.ofVoid(null))
    }

    override fun onGetChildren(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        parentId: String,
        page: Int,
        pageSize: Int,
        params: MediaLibraryService.LibraryParams?,
    ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> =
        scope.future(Dispatchers.IO) {
            try {
                if (pageSize <= 0 || page < 0) {
                    return@future LibraryResult.ofItemList(emptyList(), params)
                }

                val allItems = when (parentId) {
                MusicService.ROOT -> {
                    val now = SystemClock.elapsedRealtime()
                    if (cachedRootChildren != null && now - rootChildrenCacheGeneration < 30_000L) {
                        return@future LibraryResult.ofItemList(cachedRootChildren!!, params)
                    }
                    val items = mutableListOf<MediaItem>()

                    items += playableMediaItem(
                        MusicService.RADIO,
                        context.getString(R.string.start_radio),
                        null,
                        drawableUri(R.drawable.radio),
                        MediaMetadata.MEDIA_TYPE_MUSIC,
                    )

                    items += browsableMediaItem(
                        MusicService.QUICK_PICKS,
                        context.getString(R.string.quick_picks),
                        null,
                        drawableUri(R.drawable.trending_up),
                        MediaMetadata.MEDIA_TYPE_PLAYLIST,
                    )

                    items += browsableMediaItem(
                        MusicService.MOST_PLAYED,
                        context.getString(R.string.most_played_songs),
                        null,
                        drawableUri(R.drawable.trending_up),
                        MediaMetadata.MEDIA_TYPE_PLAYLIST,
                    )

                    items += browsableMediaItem(
                        MusicService.RECENTLY_ADDED,
                        context.getString(R.string.recently_added),
                        null,
                        drawableUri(R.drawable.library_add),
                        MediaMetadata.MEDIA_TYPE_PLAYLIST,
                    )

                    items += browsableMediaItem(
                        MusicService.CHARTS,
                        context.getString(R.string.charts),
                        null,
                        drawableUri(R.drawable.trending_up),
                        MediaMetadata.MEDIA_TYPE_PLAYLIST,
                    )

                    items += browsableMediaItem(
                        MusicService.NEW_RELEASES,
                        context.getString(R.string.new_release_albums),
                        null,
                        drawableUri(R.drawable.album),
                        MediaMetadata.MEDIA_TYPE_ALBUM,
                    )

                    val persistentQueueFile = context.filesDir.resolve("persistent_queue.json")
                    val legacyQueueFile = context.filesDir.resolve("persistent_queue.data")
                    if ((persistentQueueFile.exists() && persistentQueueFile.length() > 0) ||
                        (legacyQueueFile.exists() && legacyQueueFile.length() > 0)) {
                        items += browsableMediaItem(
                            MusicService.RECENTLY_PLAYED,
                            context.getString(R.string.history),
                            null,
                            drawableUri(R.drawable.history),
                            MediaMetadata.MEDIA_TYPE_PLAYLIST,
                        )
                    }

                    items += browsableMediaItem(
                        MusicService.SONG,
                        context.getString(R.string.songs),
                        null,
                        drawableUri(R.drawable.music_note),
                        MediaMetadata.MEDIA_TYPE_PLAYLIST,
                    )
                    items += browsableMediaItem(
                        MusicService.ARTIST,
                        context.getString(R.string.artists),
                        null,
                        drawableUri(R.drawable.artist),
                        MediaMetadata.MEDIA_TYPE_FOLDER_ARTISTS,
                    )
                    items += browsableMediaItem(
                        MusicService.ARTISTS_ALPHABETICAL,
                        context.getString(R.string.artists_a_z),
                        null,
                        drawableUri(R.drawable.artist),
                        MediaMetadata.MEDIA_TYPE_FOLDER_ARTISTS,
                    )
                    items += browsableMediaItem(
                        MusicService.RECENT_SEARCHES,
                        context.getString(R.string.recent_searches),
                        null,
                        drawableUri(R.drawable.history),
                        MediaMetadata.MEDIA_TYPE_PLAYLIST,
                    )
                    items += browsableMediaItem(
                        MusicService.PLAYLIST,
                        context.getString(R.string.playlists),
                        null,
                        drawableUri(R.drawable.queue_music),
                        MediaMetadata.MEDIA_TYPE_FOLDER_PLAYLISTS,
                    )

                    items += browsableMediaItem(
                        MusicService.DOWNLOAD_MANAGER,
                        context.getString(R.string.download_manager),
                        null,
                        drawableUri(R.drawable.download),
                        MediaMetadata.MEDIA_TYPE_FOLDER_GENRES,
                    )

                    items += browsableMediaItem(
                        MusicService.LYRICS,
                        context.getString(R.string.lyrics),
                        null,
                        drawableUri(R.drawable.lyrics),
                        MediaMetadata.MEDIA_TYPE_FOLDER_GENRES,
                    )

                    cachedRootChildren = items.toList()
                    rootChildrenCacheGeneration = now
                    items
                }

                MusicService.SONG -> database.songsByCreateDateDescPaged(pageSize, page * pageSize)
                    .map { it.toMediaItem(parentId) }

                MusicService.ARTIST ->
                    database.artistsByCreateDateDescPaged(pageSize, page * pageSize).map { artist ->
                        browsableMediaItem(
                            "${MusicService.ARTIST}/${artist.id}",
                            artist.artist.name,
                            context.resources.getQuantityString(
                                R.plurals.n_song,
                                artist.songCount,
                                artist.songCount
                            ),
                            artist.artist.thumbnailUrl?.toUri(),
                            MediaMetadata.MEDIA_TYPE_ARTIST,
                        )
                    }

                MusicService.ARTISTS_ALPHABETICAL -> {
                    if (page == 0) {
                        val buckets = listOf(
                            "#-9" to ('0'..'9'),
                            "A-B" to ('A'..'B'),
                            "C-D" to ('C'..'D'),
                            "E-F" to ('E'..'F'),
                            "G-H" to ('G'..'H'),
                            "I-J" to ('I'..'J'),
                            "K-L" to ('K'..'L'),
                            "M-N" to ('M'..'N'),
                            "O-P" to ('O'..'P'),
                            "Q-R" to ('Q'..'R'),
                            "S-T" to ('S'..'T'),
                            "U-V" to ('U'..'V'),
                            "W-Z" to ('W'..'Z'),
                        )
                        buckets.map { (label, _) ->
                            browsableMediaItem(
                                "${MusicService.ARTISTS_ALPHABETICAL}/$label",
                                label,
                                null,
                                drawableUri(R.drawable.artist),
                                MediaMetadata.MEDIA_TYPE_FOLDER_ARTISTS,
                            )
                        }
                    } else {
                        emptyList()
                    }
                }

                MusicService.RECENT_SEARCHES -> {
                    val histories = database.searchHistory().firstWithTimeout().take(10)
                    if (histories.isEmpty()) {
                        listOf(
                            browsableMediaItem(
                                "${MusicService.RECENT_SEARCHES}/empty",
                                context.getString(R.string.no_recent_searches),
                                null,
                                drawableUri(R.drawable.history),
                                MediaMetadata.MEDIA_TYPE_PLAYLIST,
                            )
                        )
                    } else {
                        histories.map { history ->
                            playableMediaItem(
                                "${MusicService.RECENT_SEARCHES}/${history.query}",
                                history.query,
                                null,
                                drawableUri(R.drawable.history),
                                MediaMetadata.MEDIA_TYPE_MUSIC,
                            )
                        } + playableMediaItem(
                            "${MusicService.RECENT_SEARCHES}/clear",
                            context.getString(R.string.clear_search_history_aa),
                            null,
                            drawableUri(R.drawable.delete),
                            MediaMetadata.MEDIA_TYPE_MUSIC,
                        )
                    }
                }

                MusicService.ALBUM ->
                    database.albumsByCreateDateDescPaged(pageSize, page * pageSize).map { album ->
                        browsableMediaItem(
                            "${MusicService.ALBUM}/${album.id}",
                            album.album.title,
                            album.artists.joinToString {
                                it.name
                            },
                            album.album.thumbnailUrl?.toUri(),
                            MediaMetadata.MEDIA_TYPE_ALBUM,
                        )
                    }

                MusicService.PLAYLIST -> {
                    val likedSongCount = database.likedSongsCount().firstWithTimeout()
                    val downloadedSongCount = downloadUtil.downloads.value.size
                    val smartItems = listOf(
                        browsableMediaItem(
                            "${MusicService.PLAYLIST}/${PlaylistEntity.LIKED_PLAYLIST_ID}",
                            context.getString(R.string.liked_songs),
                            context.resources.getQuantityString(
                                R.plurals.n_song,
                                likedSongCount,
                                likedSongCount
                            ),
                            drawableUri(R.drawable.favorite),
                            MediaMetadata.MEDIA_TYPE_PLAYLIST,
                        ),
                        browsableMediaItem(
                            "${MusicService.PLAYLIST}/${PlaylistEntity.DOWNLOADED_PLAYLIST_ID}",
                            context.getString(R.string.downloaded_songs),
                            context.resources.getQuantityString(
                                R.plurals.n_song,
                                downloadedSongCount,
                                downloadedSongCount
                            ),
                            drawableUri(R.drawable.download),
                            MediaMetadata.MEDIA_TYPE_PLAYLIST,
                        ),
                    )

                    if (page == 0) {
                        smartItems + database.playlistsByCreateDateDescPaged(pageSize, 0)
                            .map { playlist ->
                                browsableMediaItem(
                                    "${MusicService.PLAYLIST}/${playlist.id}",
                                    playlist.playlist.name,
                                    context.resources.getQuantityString(
                                        R.plurals.n_song,
                                        playlist.songCount,
                                        playlist.songCount
                                    ),
                                    playlist.thumbnails.firstOrNull()?.toUri(),
                                    MediaMetadata.MEDIA_TYPE_PLAYLIST,
                                )
                            }
                    } else {
                        val userPlaylistOffset = maxOf(0, (page * pageSize) - smartItems.size)
                        database.playlistsByCreateDateDescPaged(pageSize, userPlaylistOffset)
                            .map { playlist ->
                                browsableMediaItem(
                                    "${MusicService.PLAYLIST}/${playlist.id}",
                                    playlist.playlist.name,
                                    context.resources.getQuantityString(
                                        R.plurals.n_song,
                                        playlist.songCount,
                                        playlist.songCount
                                    ),
                                    playlist.thumbnails.firstOrNull()?.toUri(),
                                    MediaMetadata.MEDIA_TYPE_PLAYLIST,
                                )
                            }
                    }
                }

                MusicService.RECENTLY_PLAYED -> {
                    val cacheAge = SystemClock.elapsedRealtime() - persistentQueueCacheTime
                    if (cachedPersistentQueueItems == null || cacheAge > 10_000L) {
                        val jsonFile = context.filesDir.resolve("persistent_queue.json")
                        val legacyFile = context.filesDir.resolve("persistent_queue.data")
                        val targetFile = if (jsonFile.exists()) jsonFile else legacyFile
                        cachedPersistentQueueItems = if (!targetFile.exists() || targetFile.length() == 0L) {
                            emptyList()
                        } else {
                            runCatching {
                                if (targetFile == jsonFile) {
                                    kotlinx.serialization.json.Json.decodeFromString<com.arturo254.opentune.models.PersistQueue>(targetFile.readText())
                                } else {
                                    targetFile.inputStream().use { fis ->
                                        java.io.ObjectInputStream(fis).use { input ->
                                            input.readObject() as? com.arturo254.opentune.models.PersistQueue
                                        }
                                    }
                                }
                            }.getOrNull()
                                ?.items
                                ?.take(50)
                                ?.mapNotNull { metadata ->
                                    val mediaId = metadata.id.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                                    val title = if (metadata.explicit) "${metadata.title} E" else metadata.title
                                    MediaItem.Builder()
                                        .setMediaId(mediaId)
                                        .setMediaMetadata(
                                            MediaMetadata.Builder()
                                                .setTitle(title)
                                                .setArtist(metadata.artists.joinToString { it.name })
                                                .setAlbumTitle(metadata.album?.title)
                                                .setArtworkUri(metadata.thumbnailUrl?.toUri())
                                                .setIsPlayable(true)
                                                .setIsBrowsable(false)
                                                .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                                                .setExtras(playableExtras())
                                                .build(),
                                        ).build()
                                }
                                ?: emptyList()
                        }
                        persistentQueueCacheTime = SystemClock.elapsedRealtime()
                    }
                    cachedPersistentQueueItems!!
                }

                MusicService.QUICK_PICKS -> {
                    val allItems = database.quickPicks().firstWithTimeout()
                        .map { it.toMediaItem(MusicService.QUICK_PICKS) }
                    val from = page * pageSize
                    if (from >= allItems.size) emptyList()
                    else allItems.subList(from, min(from + pageSize, allItems.size))
                }

                MusicService.MOST_PLAYED -> {
                    val allItems = database.mostPlayedSongs(fromTimeStamp = 0L, limit = 100).firstWithTimeout()
                        .map { it.toMediaItem(MusicService.MOST_PLAYED) }
                    val from = page * pageSize
                    if (from >= allItems.size) emptyList()
                    else allItems.subList(from, min(from + pageSize, allItems.size))
                }

                MusicService.RECENTLY_ADDED -> {
                    database.songsByDateDownloadDescPaged(pageSize, page * pageSize)
                        .map { it.toMediaItem(MusicService.RECENTLY_ADDED) }
                }

                MusicService.MOODS_AND_GENRES -> {
                    YouTube.explore().getOrNull()?.moodAndGenres?.map { mood ->
                        browsableMediaItem(
                            "${MusicService.MOODS_AND_GENRES}/${mood.endpoint.browseId}/${mood.endpoint.params}",
                            mood.title,
                            null,
                            null,
                            MediaMetadata.MEDIA_TYPE_GENRE,
                        )
                    }.orEmpty()
                }

                MusicService.CHARTS -> {
                    YouTube.getChartsPage().getOrNull()?.sections?.map { section ->
                        browsableMediaItem(
                            "${MusicService.CHARTS}/${section.chartType.name}",
                            section.title,
                            null,
                            null,
                            MediaMetadata.MEDIA_TYPE_PLAYLIST,
                        )
                    }.orEmpty()
                }

                MusicService.NEW_RELEASES -> {
                    YouTube.newReleaseAlbums().getOrNull()?.map { album ->
                        browsableMediaItem(
                            "${MusicService.ALBUM}/${album.browseId}",
                            album.title,
                            album.artists?.joinToString { it.name },
                            album.thumbnail.toUri(),
                            MediaMetadata.MEDIA_TYPE_ALBUM,
                        )
                    }.orEmpty()
                }

                MusicService.STREAM_QUALITY -> {
                    val autoQualityEnabled = PreferenceStore.get(AutoQualityKey) ?: false
                    val currentQuality = PreferenceStore.get(AudioQualityKey) ?: AudioQuality.AUTO.name
                    val items = mutableListOf<MediaItem>()
                    items += playableMediaItem(
                        "${MusicService.AUTO_QUALITY}/toggle",
                        context.getString(R.string.auto_quality),
                        if (autoQualityEnabled) "\u2713" else null,
                        drawableUri(R.drawable.equalizer),
                        MediaMetadata.MEDIA_TYPE_MUSIC,
                    )
                    if (!autoQualityEnabled) {
                        items += listOf(
                            AudioQuality.AUTO,
                            AudioQuality.LOW,
                            AudioQuality.HIGH,
                            AudioQuality.HIGHEST,
                        ).map { quality ->
                            val isSelected = quality.name == currentQuality
                            MediaItem.Builder()
                                .setMediaId("${MusicService.STREAM_QUALITY}/${quality.name}")
                                .setMediaMetadata(
                                    MediaMetadata.Builder()
                                        .setTitle(
                                            when (quality) {
                                                AudioQuality.AUTO -> context.getString(R.string.audio_quality_auto)
                                                AudioQuality.LOW -> context.getString(R.string.audio_quality_low)
                                                AudioQuality.HIGH -> context.getString(R.string.audio_quality_high)
                                                AudioQuality.HIGHEST -> context.getString(R.string.audio_quality_max)
                                            }
                                        )
                                        .setSubtitle(if (isSelected) "\u2713" else null)
                                        .setArtworkUri(drawableUri(R.drawable.equalizer))
                                        .setIsPlayable(true)
                                        .setIsBrowsable(false)
                                        .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                                        .setExtras(playableExtras())
                                        .build(),
                                )
                                .build()
                        }
                    }
                    items
                }

                MusicService.AUDIO_SETTINGS -> {
                    listOf(
                        browsableMediaItem(
                            MusicService.EQUALIZER_PRESETS,
                            context.getString(R.string.equalizer),
                            null,
                            drawableUri(R.drawable.equalizer),
                            MediaMetadata.MEDIA_TYPE_FOLDER_GENRES,
                        ),
                        playableMediaItem(
                            MusicService.VOLUME_NORMALIZATION,
                            context.getString(R.string.audio_normalization),
                            null,
                            drawableUri(R.drawable.equalizer),
                            MediaMetadata.MEDIA_TYPE_MUSIC,
                        ),
                        browsableMediaItem(
                            MusicService.CROSSFADE_SETTINGS,
                            context.getString(R.string.audio_crossfade_title),
                            null,
                            drawableUri(R.drawable.equalizer),
                            MediaMetadata.MEDIA_TYPE_FOLDER_GENRES,
                        ),
                        browsableMediaItem(
                            MusicService.BASS_BOOST_SETTINGS,
                            context.getString(R.string.eq_bass_boost),
                            null,
                            drawableUri(R.drawable.equalizer),
                            MediaMetadata.MEDIA_TYPE_FOLDER_GENRES,
                        ),
                        playableMediaItem(
                            MusicService.AUTO_PLAY,
                            context.getString(R.string.auto_play),
                            null,
                            drawableUri(R.drawable.equalizer),
                            MediaMetadata.MEDIA_TYPE_MUSIC,
                        ),
                    )
                }

                MusicService.AUTO_PLAY -> {
                    val enabled = PreferenceStore.get(AutoPlayKey) ?: true
                    listOf(
                        playableMediaItem(
                            "${MusicService.AUTO_PLAY}/true",
                            context.getString(R.string.enabled),
                            if (enabled) "\u2713" else null,
                            drawableUri(R.drawable.equalizer),
                            MediaMetadata.MEDIA_TYPE_MUSIC,
                        ),
                        playableMediaItem(
                            "${MusicService.AUTO_PLAY}/false",
                            context.getString(R.string.disabled),
                            if (!enabled) "\u2713" else null,
                            drawableUri(R.drawable.equalizer),
                            MediaMetadata.MEDIA_TYPE_MUSIC,
                        ),
                    )
                }

                MusicService.EQUALIZER_PRESETS -> {
                    val items = mutableListOf<MediaItem>()
                    val selectedProfileId = PreferenceStore.get(com.arturo254.opentune.constants.EqualizerSelectedProfileIdKey) ?: "flat"
                    items += playableMediaItem(
                        "${MusicService.EQUALIZER_PRESETS}/flat",
                        context.getString(R.string.eq_preset_flat),
                        if (selectedProfileId == "flat") "\u2713" else null,
                        drawableUri(R.drawable.equalizer),
                        MediaMetadata.MEDIA_TYPE_MUSIC,
                    )
                    val eq = musicService?.equalizer
                    if (eq != null) {
                        val presetCount = runCatching { eq.numberOfPresets.toInt() }.getOrNull() ?: 0
                        for (i in 0 until presetCount) {
                            val name = runCatching { eq.getPresetName(i.toShort()).toString() }.getOrNull() ?: "Preset ${i + 1}"
                            val isSelected = selectedProfileId == "system:$i"
                            items += playableMediaItem(
                                "${MusicService.EQUALIZER_PRESETS}/system:$i",
                                name,
                                if (isSelected) "\u2713" else null,
                                drawableUri(R.drawable.equalizer),
                                MediaMetadata.MEDIA_TYPE_MUSIC,
                            )
                        }
                    }
                    items
                }

                MusicService.VOLUME_NORMALIZATION -> {
                    val enabled = PreferenceStore.get(AudioNormalizationKey) ?: true
                    listOf(
                        playableMediaItem(
                            "${MusicService.VOLUME_NORMALIZATION}/true",
                            context.getString(R.string.enabled),
                            if (enabled) "\u2713" else null,
                            drawableUri(R.drawable.equalizer),
                            MediaMetadata.MEDIA_TYPE_MUSIC,
                        ),
                        playableMediaItem(
                            "${MusicService.VOLUME_NORMALIZATION}/false",
                            context.getString(R.string.disabled),
                            if (!enabled) "\u2713" else null,
                            drawableUri(R.drawable.equalizer),
                            MediaMetadata.MEDIA_TYPE_MUSIC,
                        ),
                    )
                }

                MusicService.CROSSFADE_SETTINGS -> {
                    val currentSeconds = PreferenceStore.get(AudioCrossfadeDurationKey) ?: 0
                    listOf(0, 5, 10, 15).map { seconds ->
                        val isSelected = currentSeconds == seconds
                        val title = when (seconds) {
                            0 -> context.getString(R.string.crossfade_off)
                            5 -> context.getString(R.string.crossfade_5s)
                            10 -> context.getString(R.string.crossfade_10s)
                            15 -> context.getString(R.string.crossfade_15s)
                            else -> "${seconds}s"
                        }
                        playableMediaItem(
                            "${MusicService.CROSSFADE_SETTINGS}/$seconds",
                            title,
                            if (isSelected) "\u2713" else null,
                            drawableUri(R.drawable.equalizer),
                            MediaMetadata.MEDIA_TYPE_MUSIC,
                        )
                    }
                }

                MusicService.BASS_BOOST_SETTINGS -> {
                    val enabled = PreferenceStore.get(EqualizerBassBoostEnabledKey) ?: false
                    val strength = PreferenceStore.get(EqualizerBassBoostStrengthKey) ?: 0
                    val currentLevel = if (!enabled) 0 else when {
                        strength <= 333 -> 1
                        strength <= 666 -> 2
                        else -> 3
                    }
                    listOf(0 to context.getString(R.string.bass_boost_off), 1 to context.getString(R.string.bass_boost_low), 2 to context.getString(R.string.bass_boost_medium), 3 to context.getString(R.string.bass_boost_high)).map { (level, title) ->
                        val isSelected = currentLevel == level
                        playableMediaItem(
                            "${MusicService.BASS_BOOST_SETTINGS}/$level",
                            title,
                            if (isSelected) "\u2713" else null,
                            drawableUri(R.drawable.equalizer),
                            MediaMetadata.MEDIA_TYPE_MUSIC,
                        )
                    }
                }

                MusicService.DOWNLOAD_MANAGER -> {
                    val currentMediaItem = musicService?.player?.currentMediaItem
                    val currentSongId = currentMediaItem?.metadata?.id
                    val isCurrentDownloaded = currentSongId?.let {
                        downloadUtil.downloads.value[it]?.state == Download.STATE_COMPLETED
                    } == true

                    listOf(
                        playableMediaItem(
                            MusicService.DOWNLOAD_CURRENT,
                            context.getString(R.string.download_current_song),
                            if (isCurrentDownloaded) "\u2713" else null,
                            drawableUri(R.drawable.download),
                            MediaMetadata.MEDIA_TYPE_MUSIC,
                        ),
                        playableMediaItem(
                            MusicService.DOWNLOAD_QUEUE,
                            context.getString(R.string.download_queue),
                            null,
                            drawableUri(R.drawable.queue_music),
                            MediaMetadata.MEDIA_TYPE_MUSIC,
                        ),
                        playableMediaItem(
                            MusicService.REMOVE_DOWNLOAD_CURRENT,
                            context.getString(R.string.remove_download_current),
                            null,
                            drawableUri(R.drawable.offline),
                            MediaMetadata.MEDIA_TYPE_MUSIC,
                        ),
                    )
                }

                MusicService.LYRICS -> {
                    val currentMediaItem = musicService?.player?.currentMediaItem
                    val currentMetadata = currentMediaItem?.metadata
                    val lyricsEntity = currentMetadata?.id?.let { id ->
                        database.getLyricsById(id)
                    }
                    val items = mutableListOf<MediaItem>()
                    if (lyricsEntity != null && lyricsEntity.lyrics != com.arturo254.opentune.db.entities.LyricsEntity.LYRICS_NOT_FOUND) {
                        val parsedLyrics = com.arturo254.opentune.lyrics.LyricsUtils.parseLyrics(lyricsEntity.lyrics)
                        val currentPosition = musicService?.player?.currentPosition ?: 0L
                        val currentLineIndex = com.arturo254.opentune.lyrics.LyricsUtils.findCurrentLineIndex(parsedLyrics, currentPosition)
                        val currentLineText = if (currentLineIndex >= 0 && currentLineIndex < parsedLyrics.size) parsedLyrics[currentLineIndex].text else null
                        items += browsableMediaItem(
                            "${MusicService.LYRICS}/current",
                            context.getString(R.string.current_lyrics),
                            currentLineText,
                            drawableUri(R.drawable.lyrics),
                            MediaMetadata.MEDIA_TYPE_MUSIC,
                        )
                    } else {
                        items += browsableMediaItem(
                            "${MusicService.LYRICS}/current",
                            context.getString(R.string.current_lyrics),
                            context.getString(R.string.no_lyrics_available),
                            drawableUri(R.drawable.lyrics),
                            MediaMetadata.MEDIA_TYPE_MUSIC,
                        )
                    }
                    items += playableMediaItem(
                        "${MusicService.LYRICS}/view",
                        context.getString(R.string.view_lyrics),
                        null,
                        drawableUri(R.drawable.lyrics),
                        MediaMetadata.MEDIA_TYPE_MUSIC,
                    )
                    items
                }

                MusicService.CAR_SETTINGS -> {
                    val bluetoothStop = PreferenceStore.get(StopOnBluetoothDisconnectKey) ?: false
                    val notificationInCar = PreferenceStore.get(NotificationInCarKey) ?: true
                    listOf(
                        browsableMediaItem(
                            MusicService.SLEEP_TIMER,
                            context.getString(R.string.sleep_timer),
                            null,
                            drawableUri(R.drawable.timer),
                            MediaMetadata.MEDIA_TYPE_FOLDER_GENRES,
                        ),
                        playableMediaItem(
                            "${MusicService.BLUETOOTH_STOP}/toggle",
                            context.getString(R.string.stop_on_bluetooth_disconnect),
                            if (bluetoothStop) "\u2713" else null,
                            drawableUri(R.drawable.bluetooth),
                            MediaMetadata.MEDIA_TYPE_MUSIC,
                        ),
                        browsableMediaItem(
                            MusicService.MEDIA_BUTTON_BEHAVIOR,
                            context.getString(R.string.media_button_behavior),
                            null,
                            drawableUri(R.drawable.equalizer),
                            MediaMetadata.MEDIA_TYPE_FOLDER_GENRES,
                        ),
                        playableMediaItem(
                            "${MusicService.NOTIFICATION_SETTINGS}/toggle",
                            context.getString(R.string.notification_in_car),
                            if (notificationInCar) "\u2713" else null,
                            drawableUri(R.drawable.notifications),
                            MediaMetadata.MEDIA_TYPE_MUSIC,
                        ),
                    )
                }

                MusicService.SOCIAL_INTEGRATIONS -> {
                    val lastfmEnabled = PreferenceStore.get(EnableLastFMScrobblingKey) ?: false
                    val lastfmSession = PreferenceStore.get(LastFMSessionKey) ?: ""
                    val lastfmConfigured = lastfmSession.isNotBlank()
                    val lastfmStatus = if (lastfmEnabled && lastfmConfigured) "\u2713" else "\u2717"

                    val lbEnabled = PreferenceStore.get(ListenBrainzEnabledKey) ?: false
                    val lbToken = PreferenceStore.get(ListenBrainzTokenKey) ?: ""
                    val lbConfigured = lbToken.isNotBlank()
                    val lbStatus = if (lbEnabled && lbConfigured) "\u2713" else "\u2717"

                    val discordEnabled = PreferenceStore.get(EnableDiscordRPCKey) ?: true
                    val discordToken = PreferenceStore.get(DiscordTokenKey) ?: ""
                    val discordConfigured = discordToken.isNotBlank() && DiscordPresenceManager.isRunning()
                    val discordStatus = if (discordConfigured) "\u2713" else "\u2717"

                    val currentMetadata = withContext(Dispatchers.Main) { musicService?.player?.currentMetadata }
                    val nowPlayingText = if (lastfmEnabled && lastfmConfigured && currentMetadata != null) {
                        context.getString(R.string.now_scrobbling, currentMetadata.title)
                    } else {
                        null
                    }

                    listOf(
                        playableMediaItem(
                            "${MusicService.LASTFM_TOGGLE}/toggle",
                            context.getString(R.string.lastfm_scrobbling),
                            if (lastfmConfigured) lastfmStatus else context.getString(R.string.not_configured),
                            drawableUri(R.drawable.share),
                            MediaMetadata.MEDIA_TYPE_MUSIC,
                        ).buildUpon()
                            .setMediaMetadata(
                                MediaMetadata.Builder()
                                    .setTitle(context.getString(R.string.lastfm_scrobbling))
                                    .setSubtitle(nowPlayingText ?: if (lastfmConfigured) lastfmStatus else context.getString(R.string.not_configured))
                                    .setArtworkUri(drawableUri(R.drawable.share))
                                    .setIsPlayable(true)
                                    .setIsBrowsable(false)
                                    .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                                    .setExtras(playableExtras())
                                    .build(),
                            )
                            .build(),
                        playableMediaItem(
                            "${MusicService.LISTENBRAINZ_TOGGLE}/toggle",
                            context.getString(R.string.listenbrainz_scrobbling),
                            if (lbConfigured) lbStatus else context.getString(R.string.not_configured),
                            drawableUri(R.drawable.share),
                            MediaMetadata.MEDIA_TYPE_MUSIC,
                        ),
                        playableMediaItem(
                            "${MusicService.DISCORD_TOGGLE}/toggle",
                            context.getString(R.string.discord_rpc),
                            if (discordConfigured) discordStatus else context.getString(R.string.not_configured),
                            drawableUri(R.drawable.share),
                            MediaMetadata.MEDIA_TYPE_MUSIC,
                        ),
                        playableMediaItem(
                            MusicService.SHAZAM_CURRENT,
                            context.getString(R.string.shazam_current_song),
                            null,
                            drawableUri(R.drawable.share),
                            MediaMetadata.MEDIA_TYPE_MUSIC,
                        ),
                    )
                }

                MusicService.SLEEP_TIMER -> {
                    val sleepTimer = musicService?.sleepTimer
                    val isActive = sleepTimer?.isActive == true
                    val items = mutableListOf<MediaItem>()
                    if (isActive) {
                        val timeLeft = if (sleepTimer!!.pauseWhenSongEnd) {
                            -1L
                        } else {
                            sleepTimer.triggerTime - System.currentTimeMillis()
                        }
                        val timeText = if (timeLeft == -1L) {
                            context.getString(R.string.end_of_song)
                        } else {
                            com.arturo254.opentune.utils.makeTimeString(timeLeft)
                        }
                        items += playableMediaItem(
                            "${MusicService.SLEEP_TIMER}/cancel",
                            context.getString(R.string.cancel_sleep_timer),
                            timeText,
                            drawableUri(R.drawable.timer),
                            MediaMetadata.MEDIA_TYPE_MUSIC,
                        )
                    }
                    items += listOf(5, 10, 15, 30, 45, 60).map { minutes ->
                        playableMediaItem(
                            "${MusicService.SLEEP_TIMER}/$minutes",
                            "$minutes min",
                            null,
                            drawableUri(R.drawable.timer),
                            MediaMetadata.MEDIA_TYPE_MUSIC,
                        )
                    } + playableMediaItem(
                        "${MusicService.SLEEP_TIMER}/end_of_song",
                        context.getString(R.string.end_of_song),
                        null,
                        drawableUri(R.drawable.timer),
                        MediaMetadata.MEDIA_TYPE_MUSIC,
                    )
                    items
                }

                MusicService.MEDIA_BUTTON_BEHAVIOR -> {
                    val currentValue = PreferenceStore.get(MediaButtonBehaviorKey) ?: "all"
                    listOf(
                        playableMediaItem(
                            "${MusicService.MEDIA_BUTTON_BEHAVIOR}/play_pause",
                            context.getString(R.string.play_pause_only),
                            if (currentValue == "play_pause") "\u2713" else null,
                            drawableUri(R.drawable.equalizer),
                            MediaMetadata.MEDIA_TYPE_MUSIC,
                        ),
                        playableMediaItem(
                            "${MusicService.MEDIA_BUTTON_BEHAVIOR}/all",
                            context.getString(R.string.all_controls),
                            if (currentValue == "all") "\u2713" else null,
                            drawableUri(R.drawable.equalizer),
                            MediaMetadata.MEDIA_TYPE_MUSIC,
                        ),
                    )
                }

                else ->
                    when {
                        parentId.startsWith("${MusicService.ARTIST}/") ->
                            database.artistSongsByCreateDateDescPaged(parentId.removePrefix("${MusicService.ARTIST}/"), pageSize, page * pageSize)
                                .map { it.toMediaItem(parentId) }

                        parentId.startsWith("${MusicService.ARTISTS_ALPHABETICAL}/") -> {
                            val bucketLabel = parentId.removePrefix("${MusicService.ARTISTS_ALPHABETICAL}/")
                            val (startLetter, endLetter) = when (bucketLabel) {
                                "#-9" -> '0' to '9'
                                "A-B" -> 'A' to 'B'
                                "C-D" -> 'C' to 'D'
                                "E-F" -> 'E' to 'F'
                                "G-H" -> 'G' to 'H'
                                "I-J" -> 'I' to 'J'
                                "K-L" -> 'K' to 'L'
                                "M-N" -> 'M' to 'N'
                                "O-P" -> 'O' to 'P'
                                "Q-R" -> 'Q' to 'R'
                                "S-T" -> 'S' to 'T'
                                "U-V" -> 'U' to 'V'
                                "W-Z" -> 'W' to 'Z'
                                else -> 'A' to 'Z'
                            }
                            database.artistsByLetterRangePaged(
                                startLetter.uppercaseChar().toString(),
                                endLetter.uppercaseChar().toString(),
                                pageSize,
                                page * pageSize
                            ).map { artist ->
                                browsableMediaItem(
                                    "${MusicService.ARTIST}/${artist.id}",
                                    artist.artist.name,
                                    context.resources.getQuantityString(
                                        R.plurals.n_song,
                                        artist.songCount,
                                        artist.songCount
                                    ),
                                    artist.artist.thumbnailUrl?.toUri(),
                                    MediaMetadata.MEDIA_TYPE_ARTIST,
                                )
                            }
                        }

                        parentId.startsWith("${MusicService.ALBUM}/") ->
                            database.albumSongsPaged(parentId.removePrefix("${MusicService.ALBUM}/"), pageSize, page * pageSize)
                                .map { it.toMediaItem(parentId) }

                        parentId.startsWith("${MusicService.PLAYLIST}/") -> {
                            val playlistId = parentId.removePrefix("${MusicService.PLAYLIST}/")
                            when (playlistId) {
                                PlaylistEntity.LIKED_PLAYLIST_ID -> database.likedSongsPaged(pageSize, page * pageSize)
                                PlaylistEntity.DOWNLOADED_PLAYLIST_ID -> {
                                    val downloads = downloadUtil.downloads.value
                                    database.allSongs().firstWithTimeout()
                                        .filter { downloads[it.id]?.state == Download.STATE_COMPLETED }
                                        .sortedBy { downloads[it.id]?.updateTimeMs ?: 0L }
                                        .let { allDownloaded ->
                                            val from = page * pageSize
                                            if (from >= allDownloaded.size) emptyList()
                                            else allDownloaded.subList(from, minOf(from + pageSize, allDownloaded.size))
                                        }
                                }
                                else -> database.playlistSongsWithSongPaged(playlistId, pageSize, page * pageSize)
                            }.map { it.toMediaItem(parentId) }
                        }

                        else -> emptyList()
                    }
            }

            LibraryResult.ofItemList(allItems, params)
        } catch (e: Exception) {
                Log.e(TAG, "onGetChildren failed for parentId=$parentId page=$page", e)
                LibraryResult.ofItemList(emptyList(), params)
            }
        }

    override fun onGetItem(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        mediaId: String,
    ): ListenableFuture<LibraryResult<MediaItem>> =
        scope.future(Dispatchers.IO) {
            val result = try {
                when {
                mediaId == MusicService.ROOT ->
                    LibraryResult.ofItem(
                        MediaItem
                            .Builder()
                            .setMediaId(MusicService.ROOT)
                            .setMediaMetadata(
                                MediaMetadata
                                    .Builder()
                                    .setIsPlayable(false)
                                    .setIsBrowsable(true)
                                    .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                                    .setExtras(browsableExtras())
                                    .build(),
                            ).build(),
                        null,
                    )

                mediaId == MusicService.SONG ->
                    LibraryResult.ofItem(
                        browsableMediaItem(
                            MusicService.SONG,
                            context.getString(R.string.songs),
                            null,
                            drawableUri(R.drawable.music_note),
                            MediaMetadata.MEDIA_TYPE_PLAYLIST,
                        ),
                        null,
                    )

                mediaId == MusicService.ARTIST ->
                    LibraryResult.ofItem(
                        browsableMediaItem(
                            MusicService.ARTIST,
                            context.getString(R.string.artists),
                            null,
                            drawableUri(R.drawable.artist),
                            MediaMetadata.MEDIA_TYPE_FOLDER_ARTISTS,
                        ),
                        null,
                    )

                mediaId == MusicService.ARTISTS_ALPHABETICAL ->
                    LibraryResult.ofItem(
                        browsableMediaItem(
                            MusicService.ARTISTS_ALPHABETICAL,
                            context.getString(R.string.artists_a_z),
                            null,
                            drawableUri(R.drawable.artist),
                            MediaMetadata.MEDIA_TYPE_FOLDER_ARTISTS,
                        ),
                        null,
                    )

                mediaId == MusicService.RECENT_SEARCHES ->
                    LibraryResult.ofItem(
                        browsableMediaItem(
                            MusicService.RECENT_SEARCHES,
                            context.getString(R.string.recent_searches),
                            null,
                            drawableUri(R.drawable.history),
                            MediaMetadata.MEDIA_TYPE_PLAYLIST,
                        ),
                        null,
                    )

                mediaId == MusicService.ALBUM ->
                    LibraryResult.ofItem(
                        browsableMediaItem(
                            MusicService.ALBUM,
                            context.getString(R.string.albums),
                            null,
                            drawableUri(R.drawable.album),
                            MediaMetadata.MEDIA_TYPE_FOLDER_ALBUMS,
                        ),
                        null,
                    )

                mediaId == MusicService.PLAYLIST ->
                    LibraryResult.ofItem(
                        browsableMediaItem(
                            MusicService.PLAYLIST,
                            context.getString(R.string.playlists),
                            null,
                            drawableUri(R.drawable.queue_music),
                            MediaMetadata.MEDIA_TYPE_FOLDER_PLAYLISTS,
                        ),
                        null,
                    )

                mediaId == MusicService.RADIO ->
                    LibraryResult.ofItem(
                        playableMediaItem(
                            MusicService.RADIO,
                            context.getString(R.string.start_radio),
                            null,
                            drawableUri(R.drawable.radio),
                            MediaMetadata.MEDIA_TYPE_MUSIC,
                        ),
                        null,
                    )

                mediaId == MusicService.RECENTLY_PLAYED ->
                    LibraryResult.ofItem(
                        browsableMediaItem(
                            MusicService.RECENTLY_PLAYED,
                            context.getString(R.string.history),
                            null,
                            drawableUri(R.drawable.history),
                            MediaMetadata.MEDIA_TYPE_PLAYLIST,
                        ),
                        null,
                    )

                mediaId == MusicService.QUICK_PICKS ->
                    LibraryResult.ofItem(
                        browsableMediaItem(
                            MusicService.QUICK_PICKS,
                            context.getString(R.string.quick_picks),
                            null,
                            drawableUri(R.drawable.trending_up),
                            MediaMetadata.MEDIA_TYPE_PLAYLIST,
                        ),
                        null,
                    )

                mediaId == MusicService.MOST_PLAYED ->
                    LibraryResult.ofItem(
                        browsableMediaItem(
                            MusicService.MOST_PLAYED,
                            context.getString(R.string.most_played_songs),
                            null,
                            drawableUri(R.drawable.trending_up),
                            MediaMetadata.MEDIA_TYPE_PLAYLIST,
                        ),
                        null,
                    )

                mediaId == MusicService.RECENTLY_ADDED ->
                    LibraryResult.ofItem(
                        browsableMediaItem(
                            MusicService.RECENTLY_ADDED,
                            context.getString(R.string.recently_added),
                            null,
                            drawableUri(R.drawable.library_add),
                            MediaMetadata.MEDIA_TYPE_PLAYLIST,
                        ),
                        null,
                    )

                mediaId == MusicService.MOODS_AND_GENRES ->
                    LibraryResult.ofItem(
                        browsableMediaItem(
                            MusicService.MOODS_AND_GENRES,
                            context.getString(R.string.mood_and_genres),
                            null,
                            drawableUri(R.drawable.mood),
                            MediaMetadata.MEDIA_TYPE_FOLDER_GENRES,
                        ),
                        null,
                    )

                mediaId == MusicService.CHARTS ->
                    LibraryResult.ofItem(
                        browsableMediaItem(
                            MusicService.CHARTS,
                            context.getString(R.string.charts),
                            null,
                            drawableUri(R.drawable.trending_up),
                            MediaMetadata.MEDIA_TYPE_PLAYLIST,
                        ),
                        null,
                    )

                mediaId == MusicService.NEW_RELEASES ->
                    LibraryResult.ofItem(
                        browsableMediaItem(
                            MusicService.NEW_RELEASES,
                            context.getString(R.string.new_release_albums),
                            null,
                            drawableUri(R.drawable.album),
                            MediaMetadata.MEDIA_TYPE_ALBUM,
                        ),
                        null,
                    )

                mediaId == MusicService.STREAM_QUALITY ->
                    LibraryResult.ofItem(
                        browsableMediaItem(
                            MusicService.STREAM_QUALITY,
                            context.getString(R.string.audio_quality),
                            null,
                            drawableUri(R.drawable.equalizer),
                            MediaMetadata.MEDIA_TYPE_FOLDER_GENRES,
                        ),
                        null,
                    )

                mediaId == MusicService.AUDIO_SETTINGS ->
                    LibraryResult.ofItem(
                        browsableMediaItem(
                            MusicService.AUDIO_SETTINGS,
                            context.getString(R.string.audio_settings),
                            null,
                            drawableUri(R.drawable.equalizer),
                            MediaMetadata.MEDIA_TYPE_FOLDER_GENRES,
                        ),
                        null,
                    )

                mediaId == MusicService.DOWNLOAD_MANAGER ->
                    LibraryResult.ofItem(
                        browsableMediaItem(
                            MusicService.DOWNLOAD_MANAGER,
                            context.getString(R.string.download_manager),
                            null,
                            drawableUri(R.drawable.download),
                            MediaMetadata.MEDIA_TYPE_FOLDER_GENRES,
                        ),
                        null,
                    )

                mediaId == MusicService.LYRICS ->
                    LibraryResult.ofItem(
                        browsableMediaItem(
                            MusicService.LYRICS,
                            context.getString(R.string.lyrics),
                            null,
                            drawableUri(R.drawable.lyrics),
                            MediaMetadata.MEDIA_TYPE_FOLDER_GENRES,
                        ),
                        null,
                    )

                mediaId == MusicService.CAR_SETTINGS ->
                    LibraryResult.ofItem(
                        browsableMediaItem(
                            MusicService.CAR_SETTINGS,
                            context.getString(R.string.car_settings),
                            null,
                            drawableUri(R.drawable.settings),
                            MediaMetadata.MEDIA_TYPE_FOLDER_GENRES,
                        ),
                        null,
                    )

                mediaId == MusicService.SOCIAL_INTEGRATIONS ->
                    LibraryResult.ofItem(
                        browsableMediaItem(
                            MusicService.SOCIAL_INTEGRATIONS,
                            context.getString(R.string.social_integrations),
                            null,
                            drawableUri(R.drawable.share),
                            MediaMetadata.MEDIA_TYPE_FOLDER_GENRES,
                        ),
                        null,
                    )

                mediaId == MusicService.SLEEP_TIMER ->
                    LibraryResult.ofItem(
                        browsableMediaItem(
                            MusicService.SLEEP_TIMER,
                            context.getString(R.string.sleep_timer),
                            null,
                            drawableUri(R.drawable.timer),
                            MediaMetadata.MEDIA_TYPE_FOLDER_GENRES,
                        ),
                        null,
                    )

                mediaId == MusicService.MEDIA_BUTTON_BEHAVIOR ->
                    LibraryResult.ofItem(
                        browsableMediaItem(
                            MusicService.MEDIA_BUTTON_BEHAVIOR,
                            context.getString(R.string.media_button_behavior),
                            null,
                            drawableUri(R.drawable.equalizer),
                            MediaMetadata.MEDIA_TYPE_FOLDER_GENRES,
                        ),
                        null,
                    )

                mediaId == MusicService.EQUALIZER_PRESETS ->
                    LibraryResult.ofItem(
                        browsableMediaItem(
                            MusicService.EQUALIZER_PRESETS,
                            context.getString(R.string.equalizer),
                            null,
                            drawableUri(R.drawable.equalizer),
                            MediaMetadata.MEDIA_TYPE_FOLDER_GENRES,
                        ),
                        null,
                    )

                mediaId == MusicService.CROSSFADE_SETTINGS ->
                    LibraryResult.ofItem(
                        browsableMediaItem(
                            MusicService.CROSSFADE_SETTINGS,
                            context.getString(R.string.audio_crossfade_title),
                            null,
                            drawableUri(R.drawable.equalizer),
                            MediaMetadata.MEDIA_TYPE_FOLDER_GENRES,
                        ),
                        null,
                    )

                mediaId == MusicService.BASS_BOOST_SETTINGS ->
                    LibraryResult.ofItem(
                        browsableMediaItem(
                            MusicService.BASS_BOOST_SETTINGS,
                            context.getString(R.string.eq_bass_boost),
                            null,
                            drawableUri(R.drawable.equalizer),
                            MediaMetadata.MEDIA_TYPE_FOLDER_GENRES,
                        ),
                        null,
                    )

                mediaId.startsWith("${MusicService.SONG}/") ->
                    database.song(mediaId.removePrefix("${MusicService.SONG}/")).firstWithTimeout()?.let {
                        LibraryResult.ofItem(it.toMediaItem(MusicService.SONG), null)
                    } ?: LibraryResult.ofError(SessionError.ERROR_UNKNOWN)

                mediaId.startsWith("${MusicService.ARTIST}/") ->
                    database.artist(mediaId.removePrefix("${MusicService.ARTIST}/")).firstWithTimeout()?.let { artist ->
                        LibraryResult.ofItem(
                            browsableMediaItem(
                                "${MusicService.ARTIST}/${artist.id}",
                                artist.title,
                                context.resources.getQuantityString(
                                    R.plurals.n_song,
                                    artist.songCount,
                                    artist.songCount,
                                ),
                                artist.thumbnailUrl?.toUri(),
                                MediaMetadata.MEDIA_TYPE_ARTIST,
                            ),
                            null,
                        )
                    } ?: LibraryResult.ofError(SessionError.ERROR_UNKNOWN)

                mediaId.startsWith("${MusicService.ALBUM}/") ->
                    database.album(mediaId.removePrefix("${MusicService.ALBUM}/")).firstWithTimeout()?.let { album ->
                        LibraryResult.ofItem(
                            browsableMediaItem(
                                "${MusicService.ALBUM}/${album.id}",
                                album.title,
                                album.artists.joinToString { it.name },
                                album.thumbnailUrl?.toUri(),
                                MediaMetadata.MEDIA_TYPE_ALBUM,
                            ),
                            null,
                        )
                    } ?: LibraryResult.ofError(SessionError.ERROR_UNKNOWN)

                mediaId.startsWith("${MusicService.PLAYLIST}/") ->
                    database.playlist(mediaId.removePrefix("${MusicService.PLAYLIST}/")).firstWithTimeout()?.let { playlist ->
                        LibraryResult.ofItem(
                            browsableMediaItem(
                                "${MusicService.PLAYLIST}/${playlist.id}",
                                playlist.title,
                                context.resources.getQuantityString(
                                    R.plurals.n_song,
                                    playlist.songCount,
                                    playlist.songCount,
                                ),
                                playlist.thumbnails.firstOrNull()?.toUri(),
                                MediaMetadata.MEDIA_TYPE_PLAYLIST,
                            ),
                            null,
                        )
                    } ?: LibraryResult.ofError(SessionError.ERROR_UNKNOWN)

                else -> {
                    val songId = mediaId.substringAfterLast("/").ifEmpty { mediaId }
                    val song = database.song(songId).firstWithTimeout()
                    if (song != null) {
                        LibraryResult.ofItem(song.toMediaItem(MusicService.SONG), null)
                    } else {
                        val results = database.searchSongs(songId, previewSize = 1).firstWithTimeout()
                        results.firstOrNull()?.let {
                            LibraryResult.ofItem(it.toMediaItem(MusicService.SONG), null)
                        } ?: LibraryResult.ofError(SessionError.ERROR_UNKNOWN)
                    }
                }
            }
            } catch (e: Exception) {
                Log.e(TAG, "onGetItem failed for mediaId=$mediaId", e)
                LibraryResult.ofError(SessionError.ERROR_UNKNOWN)
            }
            result
        }

    override fun onSetMediaItems(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo,
        mediaItems: MutableList<MediaItem>,
        startIndex: Int,
        startPositionMs: Long,
    ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> =
        scope.future(Dispatchers.IO) {
            val defaultResult =
                MediaSession.MediaItemsWithStartPosition(emptyList(), startIndex, startPositionMs)
            val firstItem = mediaItems.firstOrNull() ?: return@future defaultResult
            val path = firstItem.mediaId.split("/").filter { it.isNotBlank() }

            val windowSize = 200

            fun chunkSongs(
                songs: List<Song>,
                targetSongId: String?,
            ): MediaSession.MediaItemsWithStartPosition {
                val targetIndex = songs.indexOfFirst { it.id == targetSongId }
                    .takeIf { it != -1 } ?: 0
                val from = maxOf(0, targetIndex - windowSize / 2)
                val to = minOf(songs.size, targetIndex + windowSize / 2)
                val chunk = songs.subList(from, to).map { it.toMediaItem() }
                return MediaSession.MediaItemsWithStartPosition(
                    chunk,
                    targetIndex - from,
                    startPositionMs,
                )
            }

            val result = try {
                when (path.firstOrNull()) {
                    MusicService.STREAM_QUALITY -> {
                        val qualityName = path.getOrNull(1)
                        if (qualityName != null) {
                            runCatching {
                                val quality = enumValueOf<AudioQuality>(qualityName)
                                PreferenceStore.launchEdit(context.dataStore) {
                                    this[AudioQualityKey] = quality.name
                                }
                            }
                        }
                        defaultResult
                    }

                    MusicService.EQUALIZER_PRESETS -> {
                        val presetValue = path.getOrNull(1)
                        if (presetValue != null) {
                            if (presetValue == "flat") {
                                musicService?.applyEqFlatPreset()
                            } else if (presetValue.startsWith("system:")) {
                                val index = presetValue.removePrefix("system:").toIntOrNull()
                                if (index != null) {
                                    musicService?.applySystemEqPreset(index)
                                }
                            }
                        }
                        defaultResult
                    }

                    MusicService.VOLUME_NORMALIZATION -> {
                        val value = path.getOrNull(1)
                        if (value != null) {
                            val enabled = value.toBoolean()
                            PreferenceStore.launchEdit(context.dataStore) {
                                this[AudioNormalizationKey] = enabled
                            }
                        }
                        defaultResult
                    }

                    MusicService.CROSSFADE_SETTINGS -> {
                        val value = path.getOrNull(1)
                        if (value != null) {
                            val seconds = value.toIntOrNull() ?: 0
                            PreferenceStore.launchEdit(context.dataStore) {
                                this[AudioCrossfadeDurationKey] = seconds
                            }
                        }
                        defaultResult
                    }

                    MusicService.BASS_BOOST_SETTINGS -> {
                        val value = path.getOrNull(1)
                        if (value != null) {
                            val level = value.toIntOrNull() ?: 0
                            val (enabled, strength) = when (level) {
                                0 -> false to 0
                                1 -> true to 333
                                2 -> true to 666
                                3 -> true to 1000
                                else -> false to 0
                            }
                            PreferenceStore.launchEdit(context.dataStore) {
                                this[EqualizerBassBoostEnabledKey] = enabled
                                this[EqualizerBassBoostStrengthKey] = strength
                            }
                        }
                        defaultResult
                    }

                    MusicService.AUTO_PLAY -> {
                        val value = path.getOrNull(1)
                        if (value != null) {
                            val enabled = value.toBoolean()
                            PreferenceStore.launchEdit(context.dataStore) {
                                this[AutoPlayKey] = enabled
                            }
                            if (enabled) {
                                musicService?.onInfiniteQueueEnabled()
                            } else {
                                musicService?.onInfiniteQueueDisabled()
                            }
                        }
                        defaultResult
                    }

                    MusicService.AUTO_QUALITY -> {
                        val currentValue = PreferenceStore.get(AutoQualityKey) ?: false
                        PreferenceStore.launchEdit(context.dataStore) {
                            this[AutoQualityKey] = !currentValue
                        }
                        if (!currentValue) {
                            musicService?.applyAutoQuality()
                            musicService?.registerAutoQualityNetworkCallback()
                        } else {
                            musicService?.unregisterAutoQualityNetworkCallback()
                        }
                        defaultResult
                    }

                    MusicService.DOWNLOAD_CURRENT -> {
                        val currentMediaItem = musicService?.player?.currentMediaItem
                        if (currentMediaItem != null) {
                            val metadata = currentMediaItem.metadata
                            if (metadata != null) {
                                database.transaction { insert(metadata) }
                                val downloadRequest = androidx.media3.exoplayer.offline.DownloadRequest
                                    .Builder(metadata.id, metadata.id.toUri())
                                    .setCustomCacheKey(metadata.id)
                                    .setData(metadata.title.toByteArray())
                                    .build()
                                androidx.media3.exoplayer.offline.DownloadService.sendAddDownload(
                                    context,
                                    ExoDownloadService::class.java,
                                    downloadRequest,
                                    false
                                )
                            }
                        }
                        defaultResult
                    }

                    MusicService.DOWNLOAD_QUEUE -> {
                        val player = musicService?.player
                        val queueSize = player?.mediaItemCount ?: 0
                        for (i in 0 until queueSize) {
                            val mediaItem = player?.getMediaItemAt(i)
                            if (mediaItem != null) {
                                val metadata = mediaItem.metadata
                                if (metadata != null) {
                                    val existingDownload = downloadUtil.downloads.value[metadata.id]
                                    if (existingDownload == null || existingDownload.state != Download.STATE_COMPLETED) {
                                        database.transaction { insert(metadata) }
                                        val downloadRequest = androidx.media3.exoplayer.offline.DownloadRequest
                                            .Builder(metadata.id, metadata.id.toUri())
                                            .setCustomCacheKey(metadata.id)
                                            .setData(metadata.title.toByteArray())
                                            .build()
                                        androidx.media3.exoplayer.offline.DownloadService.sendAddDownload(
                                            context,
                                            ExoDownloadService::class.java,
                                            downloadRequest,
                                            false
                                        )
                                    }
                                }
                            }
                        }
                        defaultResult
                    }

                    MusicService.REMOVE_DOWNLOAD_CURRENT -> {
                        val currentMediaItem = musicService?.player?.currentMediaItem
                        if (currentMediaItem != null) {
                            val metadata = currentMediaItem.metadata
                            if (metadata != null) {
                                androidx.media3.exoplayer.offline.DownloadService.sendRemoveDownload(
                                    context,
                                    ExoDownloadService::class.java,
                                    metadata.id,
                                    false
                                )
                            }
                        }
                        defaultResult
                    }

                    MusicService.SLEEP_TIMER -> {
                        val value = path.getOrNull(1)
                        if (value != null) {
                            if (value == "cancel") {
                                musicService?.sleepTimer?.clear()
                            } else if (value == "end_of_song") {
                                musicService?.sleepTimer?.start(-1)
                            } else {
                                val minutes = value.toIntOrNull()
                                if (minutes != null && minutes > 0) {
                                    musicService?.sleepTimer?.start(minutes)
                                }
                            }
                        }
                        defaultResult
                    }

                    MusicService.LASTFM_TOGGLE -> {
                        val currentValue = PreferenceStore.get(EnableLastFMScrobblingKey) ?: false
                        PreferenceStore.launchEdit(context.dataStore) {
                            this[EnableLastFMScrobblingKey] = !currentValue
                        }
                        defaultResult
                    }

                    MusicService.LISTENBRAINZ_TOGGLE -> {
                        val currentValue = PreferenceStore.get(ListenBrainzEnabledKey) ?: false
                        PreferenceStore.launchEdit(context.dataStore) {
                            this[ListenBrainzEnabledKey] = !currentValue
                        }
                        defaultResult
                    }

                    MusicService.DISCORD_TOGGLE -> {
                        val currentValue = PreferenceStore.get(EnableDiscordRPCKey) ?: true
                        PreferenceStore.launchEdit(context.dataStore) {
                            this[EnableDiscordRPCKey] = !currentValue
                        }
                        if (!currentValue) {
                            try { DiscordPresenceManager.stop() } catch (_: Exception) {}
                        }
                        defaultResult
                    }

                    MusicService.SHAZAM_CURRENT -> {
                        val currentMediaItem = musicService?.player?.currentMediaItem
                        if (currentMediaItem != null) {
                            val metadata = currentMediaItem.metadata
                            if (metadata != null) {
                                runCatching {
                                    val signatureGenerator = com.arturo254.opentune.shazamkit.ShazamSignatureGenerator()
                                    val signature = signatureGenerator.nextSignatureOrNull()
                                    if (signature != null) {
                                        val result = com.arturo254.opentune.shazamkit.Shazam.recognize(signature.uri, signature.sampleDurationMs)
                                        result.onSuccess { recognition ->
                                            val title = recognition.title
                                            val artist = recognition.artist
                                            android.os.Handler(android.os.Looper.getMainLooper()).post {
                                                android.widget.Toast.makeText(
                                                    context,
                                                    context.getString(R.string.shazam_found, title, artist),
                                                    android.widget.Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        }.onFailure {
                                            android.os.Handler(android.os.Looper.getMainLooper()).post {
                                                android.widget.Toast.makeText(
                                                    context,
                                                    context.getString(R.string.shazam_no_match),
                                                    android.widget.Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    }
                                }.onFailure {
                                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                                        android.widget.Toast.makeText(
                                            context,
                                            context.getString(R.string.shazam_no_match),
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                        }
                        defaultResult
                    }

                    MusicService.BLUETOOTH_STOP -> {
                        val currentValue = PreferenceStore.get(StopOnBluetoothDisconnectKey) ?: false
                        PreferenceStore.launchEdit(context.dataStore) {
                            this[StopOnBluetoothDisconnectKey] = !currentValue
                        }
                        (mediaSession as? MediaLibrarySession)?.notifyChildrenChanged(MusicService.CAR_SETTINGS, -1, null)
                        defaultResult
                    }

                    MusicService.MEDIA_BUTTON_BEHAVIOR -> {
                        val value = path.getOrNull(1)
                        if (value != null) {
                            PreferenceStore.launchEdit(context.dataStore) {
                                this[MediaButtonBehaviorKey] = value
                            }
                        }
                        (mediaSession as? MediaLibrarySession)?.notifyChildrenChanged(MusicService.MEDIA_BUTTON_BEHAVIOR, -1, null)
                        defaultResult
                    }

                    MusicService.NOTIFICATION_SETTINGS -> {
                        val currentValue = PreferenceStore.get(NotificationInCarKey) ?: true
                        PreferenceStore.launchEdit(context.dataStore) {
                            this[NotificationInCarKey] = !currentValue
                        }
                        musicService?.refreshPlaybackNotification()
                        (mediaSession as? MediaLibrarySession)?.notifyChildrenChanged(MusicService.CAR_SETTINGS, -1, null)
                        defaultResult
                    }

                    MusicService.LYRICS -> {
                        defaultResult
                    }

                    MusicService.RADIO -> {
                        val allSongs = database.allSongs().firstWithTimeout()
                        if (allSongs.isEmpty()) return@future defaultResult
                        val randomSong = allSongs.random()
                        chunkSongs(allSongs, randomSong.id)
                    }

                    MusicService.RECENT_SEARCHES -> {
                        val query = path.getOrNull(1)
                        if (query == "clear") {
                            database.clearSearchHistory()
                            defaultResult
                        } else if (query != null && query != "empty") {
                            val matchedSongs = database.searchSongs(query, previewSize = 50).firstWithTimeout()
                            if (matchedSongs.isNotEmpty()) {
                                val songId = matchedSongs.first().id
                                val allSongs = database.allSongs().firstWithTimeout()
                                chunkSongs(allSongs, songId)
                            } else {
                                val matchingArtists = database.searchArtists(query, previewSize = 5).firstWithTimeout()
                                if (matchingArtists.isNotEmpty()) {
                                    val artistId = matchingArtists.first().id
                                    val songs = database.artistSongsByCreateDateAsc(artistId).firstWithTimeout()
                                    chunkSongs(songs, songs.firstOrNull()?.id)
                                } else {
                                    defaultResult
                                }
                            }
                        } else {
                            defaultResult
                        }
                    }

                    MusicService.SONG -> {
                        val songId = path.getOrNull(1) ?: return@future defaultResult
                        val allSongs = database.allSongs().firstWithTimeout()
                        chunkSongs(allSongs, songId)
                    }

                    MusicService.ARTIST -> {
                        val songId = path.getOrNull(2) ?: return@future defaultResult
                        val artistId = path.getOrNull(1) ?: return@future defaultResult
                        val songs = database.artistSongsByCreateDateAsc(artistId).firstWithTimeout()
                        chunkSongs(songs, songId)
                    }

                    MusicService.ALBUM -> {
                        val songId = path.getOrNull(2) ?: return@future defaultResult
                        val albumId = path.getOrNull(1) ?: return@future defaultResult
                        val albumWithSongs =
                            database.albumWithSongs(albumId).firstWithTimeout() ?: return@future defaultResult
                        chunkSongs(albumWithSongs.songs, songId)
                    }

                    MusicService.PLAYLIST -> {
                        val songId = path.getOrNull(2) ?: return@future defaultResult
                        val playlistId = path.getOrNull(1) ?: return@future defaultResult
                        val songs =
                            when (playlistId) {
                                PlaylistEntity.LIKED_PLAYLIST_ID -> database.likedSongs(
                                    SongSortType.CREATE_DATE,
                                    descending = true
                                )

                                PlaylistEntity.DOWNLOADED_PLAYLIST_ID -> {
                                    val downloads = downloadUtil.downloads.value
                                    database
                                        .allSongs()
                                        .flowOn(Dispatchers.IO)
                                        .map { songs ->
                                            songs.filter {
                                                downloads[it.id]?.state == Download.STATE_COMPLETED
                                            }
                                        }.map { songs ->
                                            songs
                                                .map { it to downloads[it.id] }
                                                .sortedBy { it.second?.updateTimeMs ?: 0L }
                                                .map { it.first }
                                        }
                                }

                                else ->
                                    database.playlistSongs(playlistId).map { list ->
                                        list.map { it.song }
                                    }
                            }.firstWithTimeout()
                        chunkSongs(songs, songId)
                    }

                    else -> {
                        val query = firstItem.requestMetadata.searchQuery?.trim().orEmpty()
                        if (query.isNotBlank()) {
                            val matchedSongs = database.searchSongs(query, previewSize = 50).firstWithTimeout()
                            if (matchedSongs.isNotEmpty()) {
                                val songId = matchedSongs.first().id
                                val allSongs = database.allSongs().firstWithTimeout()
                                chunkSongs(allSongs, songId)
                            } else {
                                val matchingArtists = database.searchArtists(query, previewSize = 5).firstWithTimeout()
                                if (matchingArtists.isNotEmpty()) {
                                    val artistId = matchingArtists.first().id
                                    val songs = database.artistSongsByCreateDateAsc(artistId).firstWithTimeout()
                                    chunkSongs(songs, songs.firstOrNull()?.id)
                                } else {
                                    val matchingAlbums = database.searchAlbums(query, previewSize = 5).firstWithTimeout()
                                    if (matchingAlbums.isNotEmpty()) {
                                        val albumWithSongs = database.albumWithSongs(matchingAlbums.first().id).firstWithTimeout()
                                        if (albumWithSongs != null) {
                                            chunkSongs(albumWithSongs.songs, albumWithSongs.songs.firstOrNull()?.id)
                                        } else defaultResult
                                    } else defaultResult
                                }
                            }
                        } else {
                            val mediaId = firstItem.mediaId
                            if (mediaId.isNotBlank()) {
                                val songId = mediaId.substringAfterLast("/").ifEmpty { mediaId }
                                var song = database.song(songId).firstWithTimeout()
                                if (song == null) {
                                    val results = database.searchSongs(songId, previewSize = 1).firstWithTimeout()
                                    song = results.firstOrNull()
                                }
                                if (song != null) {
                                    val allSongs = database.allSongs().firstWithTimeout()
                                    val result = chunkSongs(allSongs, song.id)
                                    result
                                } else {
                                    Log.w(TAG, "onSetMediaItems else->: song not found for mediaId=$mediaId")
                                    defaultResult
                                }
                            } else {
                                Log.w(TAG, "onSetMediaItems else->: mediaId is blank")
                                defaultResult
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "onSetMediaItems failed for mediaId=${firstItem?.mediaId}", e)
                defaultResult
            }
            result
        }

    private suspend fun <T> Flow<T>.firstWithTimeout(timeoutMs: Long = 15_000L): T =
        withTimeout(timeoutMs) { first() }

    private fun drawableUri(
        @DrawableRes id: Int,
    ) = Uri
        .Builder()
        .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
        .authority(context.resources.getResourcePackageName(id))
        .appendPath(context.resources.getResourceTypeName(id))
        .appendPath(context.resources.getResourceEntryName(id))
        .build()

    private fun browsableMediaItem(
        id: String,
        title: String,
        subtitle: String?,
        iconUri: Uri?,
        mediaType: Int = MediaMetadata.MEDIA_TYPE_MUSIC,
    ) = MediaItem
        .Builder()
        .setMediaId(id)
        .setMediaMetadata(
            MediaMetadata
                .Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setArtworkUri(iconUri)
                .setIsPlayable(false)
                .setIsBrowsable(true)
                .setMediaType(mediaType)
                .setExtras(browsableExtras())
                .build(),
        ).build()

    private fun playableMediaItem(
        id: String,
        title: String,
        subtitle: String?,
        iconUri: Uri?,
        mediaType: Int = MediaMetadata.MEDIA_TYPE_MUSIC,
    ) = MediaItem
        .Builder()
        .setMediaId(id)
        .setMediaMetadata(
            MediaMetadata
                .Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setArtworkUri(iconUri)
                .setIsPlayable(true)
                .setIsBrowsable(false)
                .setMediaType(mediaType)
                .setExtras(playableExtras())
                .build(),
        ).build()

    private fun Song.toMediaItem(path: String) =
        MediaItem
            .Builder()
            .setMediaId("$path/$id")
            .setMediaMetadata(
                MediaMetadata
                    .Builder()
                    .setTitle(buildTitle(this, downloadUtil.downloads.value[id]?.state))
                    .setSubtitle(artists.joinToString { it.name })
                    .setArtist(artists.joinToString { it.name })
                    .setAlbumTitle(song.albumName)
                    .setArtworkUri(song.thumbnailUrl?.toUri())
                    .setIsPlayable(true)
                    .setIsBrowsable(false)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                    .setDurationMs((song.duration * 1000L).coerceAtLeast(0L))
                    .setExtras(playableExtras())
                    .build(),
            ).build()

    private fun buildTitle(song: Song, downloadState: Int?): String {
        val baseTitle = if (song.song.explicit) "${song.song.title} E" else song.song.title
        return if (downloadState == Download.STATE_COMPLETED) "$baseTitle \u2B07" else baseTitle
    }

    companion object {
        private const val TAG = "MediaLibraryCallback"
        private val DENIED_GUEST_COMMANDS = setOf(
            Player.COMMAND_PLAY_PAUSE,
            Player.COMMAND_PREPARE,
            Player.COMMAND_STOP,
            Player.COMMAND_SEEK_TO_DEFAULT_POSITION,
            Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM,
            Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
            Player.COMMAND_SEEK_TO_PREVIOUS,
            Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
            Player.COMMAND_SEEK_TO_NEXT,
            Player.COMMAND_SEEK_TO_MEDIA_ITEM,
            Player.COMMAND_SEEK_BACK,
            Player.COMMAND_SEEK_FORWARD,
            Player.COMMAND_SET_SPEED_AND_PITCH,
            Player.COMMAND_SET_SHUFFLE_MODE,
            Player.COMMAND_SET_REPEAT_MODE,
            Player.COMMAND_SET_MEDIA_ITEMS_METADATA,
            Player.COMMAND_SET_PLAYLIST_METADATA,
            Player.COMMAND_SET_MEDIA_ITEM,
            Player.COMMAND_CHANGE_MEDIA_ITEMS,
            Player.COMMAND_SET_TRACK_SELECTION_PARAMETERS,
        )
        private val DENIED_GUEST_CUSTOM_COMMANDS = setOf(
            MediaSessionConstants.ACTION_TOGGLE_SHUFFLE,
            MediaSessionConstants.ACTION_TOGGLE_REPEAT_MODE,
        )

        private const val EXTRA_CONTENT_STYLE_SUPPORTED = "android.media.browse.CONTENT_STYLE_SUPPORTED"
        private const val EXTRA_CONTENT_STYLE_BROWSABLE_HINT =
            "android.media.browse.CONTENT_STYLE_BROWSABLE_HINT"
        private const val EXTRA_CONTENT_STYLE_PLAYABLE_HINT =
            "android.media.browse.CONTENT_STYLE_PLAYABLE_HINT"

        private const val CONTENT_STYLE_LIST_ITEM = 1
        private const val CONTENT_STYLE_GRID_ITEM = 2
    }
}
