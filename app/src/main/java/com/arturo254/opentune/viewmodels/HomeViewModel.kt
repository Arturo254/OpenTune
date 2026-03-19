package com.arturo254.opentune.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arturo254.innertube.YouTube
import com.arturo254.innertube.models.AlbumItem
import com.arturo254.innertube.models.Artist
import com.arturo254.innertube.models.PlaylistItem
import com.arturo254.innertube.models.SongItem
import com.arturo254.innertube.models.WatchEndpoint
import com.arturo254.innertube.models.YTItem
import com.arturo254.innertube.pages.ExplorePage
import com.arturo254.innertube.pages.HomePage
import com.arturo254.innertube.utils.completedLibraryPage
import com.arturo254.opentune.constants.InnerTubeCookieKey
import com.arturo254.opentune.db.MusicDatabase
import com.arturo254.opentune.db.entities.Album
import com.arturo254.opentune.db.entities.AlbumEntity
import com.arturo254.opentune.db.entities.Artist as LocalArtist
import com.arturo254.opentune.db.entities.ArtistEntity
import com.arturo254.opentune.db.entities.LocalItem
import com.arturo254.opentune.db.entities.Playlist
import com.arturo254.opentune.db.entities.Song
import com.arturo254.opentune.models.SimilarRecommendation
import com.arturo254.opentune.utils.dataStore
import com.arturo254.opentune.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext context: Context,
    val database: MusicDatabase,
) : ViewModel() {
    val isRefreshing = MutableStateFlow(false)
    val isLoading = MutableStateFlow(false)

    // MEJORA 1: Estado de randomización
    val isRandomizing = MutableStateFlow(false)

    // MEJORA 2: Estado de carga de paginación
    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    val quickPicks = MutableStateFlow<List<Song>?>(null)
    val forgottenFavorites = MutableStateFlow<List<Song>?>(null)
    val keepListening = MutableStateFlow<List<LocalItem>?>(null)
    val similarRecommendations = MutableStateFlow<List<SimilarRecommendation>?>(null)
    val accountPlaylists = MutableStateFlow<List<PlaylistItem>?>(null)
    val homePage = MutableStateFlow<HomePage?>(null)
    val explorePage = MutableStateFlow<ExplorePage?>(null)
    val recentActivity = MutableStateFlow<List<YTItem>?>(null)
    val recentPlaylistsDb = MutableStateFlow<List<Playlist>?>(null)

    val allLocalItems = MutableStateFlow<List<LocalItem>>(emptyList())
    val allYtItems = MutableStateFlow<List<YTItem>>(emptyList())

    val accountName = MutableStateFlow("Guest")
    val accountImageUrl = MutableStateFlow<String?>(null)

    // MEJORA 3: Tracking mejorado de cuenta
    private var lastProcessedCookie: String? = null
    private var isProcessingAccountData = false

    private suspend fun load() {
        isLoading.value = true

        quickPicks.value = database.quickPicks()
            .first().shuffled().take(20)

        forgottenFavorites.value = database.forgottenFavorites()
            .first().shuffled().take(20)

        val fromTimeStamp = System.currentTimeMillis() - 86400000 * 7 * 2
        val keepListeningSongs = database.mostPlayedSongs(fromTimeStamp, limit = 15, offset = 5)
            .first().shuffled().take(10)
        val keepListeningAlbums = database.mostPlayedAlbums(fromTimeStamp, limit = 8, offset = 2)
            .first().filter { it.thumbnailUrl != null }.shuffled().take(5)
            .map { albumStats ->
                Album(
                    album = AlbumEntity(
                        id = albumStats.id,
                        title = albumStats.title,
                        thumbnailUrl = albumStats.thumbnailUrl,
                        songCount = 0,
                        duration = 0,
                    ),
                    artists = emptyList(),
                )
            }
        val keepListeningArtists = database.mostPlayedArtists(fromTimeStamp)
            .first().filter {
                (it.id.startsWith("UC") || it.id.startsWith("FEmusic_library_privately_owned_artist")) &&
                it.thumbnailUrl != null
            }
            .shuffled().take(5)
            .map { artistStats ->
                LocalArtist(
                    artist = ArtistEntity(
                        id = artistStats.id,
                        name = artistStats.name,
                        thumbnailUrl = artistStats.thumbnailUrl,
                        channelId = artistStats.channelId,
                    )
                )
            }
        keepListening.value =
            (keepListeningSongs + keepListeningAlbums + keepListeningArtists).shuffled()

        allLocalItems.value =
            (quickPicks.value.orEmpty() + forgottenFavorites.value.orEmpty() + keepListening.value.orEmpty())
                .filter { it is Song || it is Album }

        if (YouTube.cookie != null) {
            YouTube.library("FEmusic_liked_playlists").completedLibraryPage().onSuccess {
                accountPlaylists.value = it.items.filterIsInstance<PlaylistItem>()
                    .filterNot { it.id == "SE" }
            }.onFailure {
                reportException(it)
            }
        }

        // Similar to artists
        val artistRecommendations =
            database.mostPlayedArtists(fromTimeStamp, limit = 10).first()
                .filter {
                    it.id.startsWith("UC") ||
                    it.id.startsWith("FEmusic_library_privately_owned_artist")
                }
                .shuffled().take(3)
                .mapNotNull { artistStats ->
                    val items = mutableListOf<YTItem>()
                    YouTube.artist(artistStats.id).onSuccess { page ->
                        items += page.sections.getOrNull(page.sections.size - 2)?.items.orEmpty()
                        items += page.sections.lastOrNull()?.items.orEmpty()
                    }
                    SimilarRecommendation(
                        title = LocalArtist(
                            artist = ArtistEntity(
                                id = artistStats.id,
                                name = artistStats.name,
                                thumbnailUrl = artistStats.thumbnailUrl,
                                channelId = artistStats.channelId,
                            )
                        ),
                        items = items
                            .shuffled()
                            .ifEmpty { return@mapNotNull null }
                    )
                }
        // Similar to songs
        val songRecommendations =
            database.mostPlayedSongs(fromTimeStamp, limit = 10).first()
                .filter { it.album != null }
                .shuffled().take(2)
                .mapNotNull { song ->
                    val endpoint =
                        YouTube.next(WatchEndpoint(videoId = song.id)).getOrNull()?.relatedEndpoint
                            ?: return@mapNotNull null
                    val page = YouTube.related(endpoint).getOrNull() ?: return@mapNotNull null
                    SimilarRecommendation(
                        title = song,
                        items = (page.songs.shuffled().take(8) +
                                page.albums.shuffled().take(4) +
                                page.artists.shuffled().take(4) +
                                page.playlists.shuffled().take(4))
                            .shuffled()
                            .ifEmpty { return@mapNotNull null }
                    )
                }
        similarRecommendations.value = (artistRecommendations + songRecommendations).shuffled()

        YouTube.home().onSuccess { page ->
            homePage.value = page
        }.onFailure {
            reportException(it)
        }

        YouTube.explore().onSuccess { page ->
            val artists: Set<String>
            val favouriteArtists: Set<String>
            database.artistsBookmarkedByCreateDateAsc().first().let { list ->
                artists = list.map(com.arturo254.opentune.db.entities.Artist::id).toHashSet()
                favouriteArtists = list
                    .filter { it.artist.bookmarkedAt != null }
                    .map { it.id }
                    .toHashSet()
            }
            explorePage.value = page.copy(
                newReleaseAlbums = page.newReleaseAlbums
                    .sortedBy { album ->
                        if (album.artists.orEmpty().any { it.id in favouriteArtists }) 0
                        else if (album.artists.orEmpty().any { it.id in artists }) 1
                        else 2
                    }
            )
        }.onFailure {
            reportException(it)
        }

        allYtItems.value = similarRecommendations.value?.flatMap { it.items }.orEmpty() +
                homePage.value?.sections?.flatMap { it.items }.orEmpty() +
                explorePage.value?.newReleaseAlbums.orEmpty()

        isLoading.value = false
    }


    // MEJORA 1: Randomización mejorada con inteligencia 80/20
    suspend fun getRandomItem(): YTItem? {
        return try {
            isRandomizing.value = true
            // Delay para feedback visual
            delay(1000)

            val userSongs = mutableListOf<YTItem>()
            val otherSources = mutableListOf<YTItem>()

            // Colectar de Quick Picks
            quickPicks.value?.forEach { song ->
                userSongs.add(SongItem(
                    id = song.id,
                    title = song.title,
                    artists = song.artists.map { Artist(name = it.name, id = it.id) },
                    thumbnail = song.thumbnailUrl ?: "",
                    explicit = false
                ))
            }

            // Colectar de Keep Listening
            keepListening.value?.forEach { item ->
                when (item) {
                    is Song -> userSongs.add(SongItem(
                        id = item.id,
                        title = item.title,
                        artists = item.artists.map { Artist(name = it.name, id = it.id) },
                        thumbnail = item.thumbnailUrl ?: "",
                        explicit = false
                    ))
                    is Album -> otherSources.add(AlbumItem(
                        browseId = item.id,
                        playlistId = item.album.playlistId ?: "",
                        title = item.title,
                        artists = item.artists.map { Artist(name = it.name, id = it.id) },
                        year = item.album.year,
                        thumbnail = item.thumbnailUrl ?: ""
                    ))
                    else -> {}
                }
            }

            // Añadir todas las fuentes de YouTube
            otherSources.addAll(allYtItems.value)

            // Probabilidad: 80% User Songs, 20% Otras Fuentes
            val item = if (userSongs.isNotEmpty() &&
                (otherSources.isEmpty() || Random.nextFloat() < 0.8f)) {
                userSongs.distinctBy { it.id }.shuffled().firstOrNull()
            } else {
                otherSources.distinctBy { it.id }.shuffled().firstOrNull()
            }

            item ?: userSongs.firstOrNull() ?: otherSources.firstOrNull()
        } finally {
            isRandomizing.value = false
        }
    }

    fun refresh() {
        if (isRefreshing.value) return
        viewModelScope.launch(Dispatchers.IO) {
            isRefreshing.value = true
            load()
            isRefreshing.value = false
        }
    }

    init {
        // MEJORA 3: Carga inicial con tracking de cookie
        viewModelScope.launch(Dispatchers.IO) {
            context.dataStore.data
                .map { it[InnerTubeCookieKey] }
                .distinctUntilChanged()
                .first()

            load()
        }

        // MEJORA 3: Listener reactivo de cambios de cuenta
        viewModelScope.launch(Dispatchers.IO) {
            context.dataStore.data
                .map { it[InnerTubeCookieKey] }
                .collect { cookie ->
                    // Evitar procesamiento redundante
                    if (isProcessingAccountData) return@collect

                    lastProcessedCookie = cookie
                    isProcessingAccountData = true

                    try {
                        if (cookie != null && cookie.isNotEmpty()) {
                            // Actualizar cookie en YouTube API
                            YouTube.cookie = cookie

                            // Obtener información de cuenta
                            YouTube.accountInfo().onSuccess { info ->
                                accountName.value = info.name
                            }.onFailure {
                                reportException(it)
                            }
                        } else {
                            // Usuario deslogueado
                            accountName.value = "Guest"
                            accountImageUrl.value = null
                            accountPlaylists.value = null
                        }
                    } finally {
                        isProcessingAccountData = false
                    }
                }
        }
    }
}