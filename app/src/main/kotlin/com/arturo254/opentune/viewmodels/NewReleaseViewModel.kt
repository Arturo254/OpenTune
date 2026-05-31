/*
 * OpenTune Project Original (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 *
 * Actualizado para soportar categorías (Álbumes, Singles, EP)
 * Usando inferType() para clasificar por título
 */

package com.arturo254.opentune.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import com.arturo254.opentune.innertube.YouTube
import com.arturo254.opentune.innertube.models.AlbumItem
import com.arturo254.opentune.innertube.models.AlbumType
import com.arturo254.opentune.innertube.models.filterExplicit
import com.arturo254.opentune.innertube.models.filterVideo
import com.arturo254.opentune.constants.HideExplicitKey
import com.arturo254.opentune.constants.HideVideoKey
import com.arturo254.opentune.db.MusicDatabase
import com.arturo254.opentune.utils.dataStore
import com.arturo254.opentune.utils.get
import com.arturo254.opentune.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Modelo de datos para el contenido de New Releases con categorías
 */
data class NewReleaseContent(
    val albums: List<AlbumItem>,
    val singles: List<AlbumItem>,
    val eps: List<AlbumItem>,
) {
    val totalReleases: Int get() = albums.size + singles.size + eps.size

    val isEmpty: Boolean get() = albums.isEmpty() && singles.isEmpty() && eps.isEmpty()
}

/**
 * UI States para la pantalla de New Releases
 */
sealed interface NewReleaseUiState {
    data object Loading : NewReleaseUiState
    data class Success(val content: NewReleaseContent) : NewReleaseUiState
    data object Empty : NewReleaseUiState
    data class Error(val message: String, val throwable: Throwable? = null) : NewReleaseUiState
}

@HiltViewModel
class NewReleaseViewModel
@Inject
constructor(
    @ApplicationContext val context: Context,
    private val database: MusicDatabase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<NewReleaseUiState>(NewReleaseUiState.Loading)
    val uiState = _uiState.asStateFlow()

    private val _newReleaseAlbums = MutableStateFlow<List<AlbumItem>>(emptyList())
    val newReleaseAlbums = _newReleaseAlbums.asStateFlow()

    val content = _uiState.asStateFlow()

    init {
        load()
    }

    fun retry() {
        load()
    }

    private fun load() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { NewReleaseUiState.Loading }
            try {
                // Usar el nuevo método categorizado
                val categorizedResponse = YouTube.newReleasesCategorized().getOrThrow()

                // Obtener ranking de artistas
                val artistRanking = getArtistRanking()
                val favouriteArtistIds = artistRanking.favourites
                val allArtistRank = artistRanking.all

                // Aplicar ordenamiento y filtros
                val filteredContent = NewReleaseContent(
                    albums = sortByArtistRelevance(
                        categorizedResponse.albums,
                        favouriteArtistIds,
                        allArtistRank
                    ),
                    singles = sortByArtistRelevance(
                        categorizedResponse.singles,
                        favouriteArtistIds,
                        allArtistRank
                    ),
                    eps = sortByArtistRelevance(
                        categorizedResponse.eps,
                        favouriteArtistIds,
                        allArtistRank
                    ),
                ).let { content ->
                    applyContentFilters(content)
                }

                // Actualizar StateFlows
                _newReleaseAlbums.update {
                    filteredContent.albums + filteredContent.singles + filteredContent.eps
                }

                _uiState.update {
                    if (filteredContent.isEmpty) {
                        NewReleaseUiState.Empty
                    } else {
                        NewReleaseUiState.Success(filteredContent)
                    }
                }

            } catch (t: Throwable) {
                reportException(t)
                _uiState.update {
                    NewReleaseUiState.Error(
                        message = t.message ?: "Unknown error",
                        throwable = t
                    )
                }
            }
        }
    }

    private suspend fun getArtistRanking(): ArtistRanking {
        val allArtists = mutableMapOf<Int, String>()
        val favouriteArtists = mutableMapOf<Int, String>()

        database.allArtistsByPlayTime().first().let { list ->
            var favIndex = 0
            for ((artistsIndex, artist) in list.withIndex()) {
                allArtists[artistsIndex] = artist.id
                if (artist.artist.bookmarkedAt != null) {
                    favouriteArtists[favIndex] = artist.id
                    favIndex++
                }
            }
        }

        return ArtistRanking(
            favourites = favouriteArtists.values.toSet(),
            all = allArtists
        )
    }

    private fun categorizeAndSortReleases(
        releases: List<AlbumItem>,
        favouriteArtistIds: Set<String>,
        allArtistRank: Map<Int, String>,
    ): NewReleaseContent {
        val allAlbums = mutableListOf<AlbumItem>()
        val allSingles = mutableListOf<AlbumItem>()
        val allEps = mutableListOf<AlbumItem>()

        releases.forEach { album ->
            // Usar inferType() para determinar la categoría basado en el título
            when (album.inferType()) {
                AlbumType.ALBUM -> allAlbums.add(album)
                AlbumType.SINGLE -> allSingles.add(album)
                AlbumType.EP -> allEps.add(album)
            }
        }

        return NewReleaseContent(
            albums = sortByArtistRelevance(allAlbums, favouriteArtistIds, allArtistRank),
            singles = sortByArtistRelevance(allSingles, favouriteArtistIds, allArtistRank),
            eps = sortByArtistRelevance(allEps, favouriteArtistIds, allArtistRank),
        )
    }

    private fun sortByArtistRelevance(
        albums: List<AlbumItem>,
        favouriteArtistIds: Set<String>,
        allArtistRank: Map<Int, String>,
    ): List<AlbumItem> {
        return albums.sortedWith(
            compareBy<AlbumItem> { album ->
                val artistIds = album.artists.orEmpty().mapNotNull { it.id }
                artistIds.none { it in favouriteArtistIds }
            }.thenBy { album ->
                val artistIds = album.artists.orEmpty().mapNotNull { it.id }
                artistIds.minOfOrNull { artistId ->
                    allArtistRank.entries.firstOrNull { it.value == artistId }?.key ?: Int.MAX_VALUE
                } ?: Int.MAX_VALUE
            }.thenBy { album ->
                album.title.lowercase()
            }
        )
    }

    private suspend fun applyContentFilters(content: NewReleaseContent): NewReleaseContent {
        val hideExplicit = context.dataStore.get(HideExplicitKey, false)
        val hideVideo = context.dataStore.get(HideVideoKey, false)

        return NewReleaseContent(
            albums = content.albums
                .filterExplicit(hideExplicit)
                .filterVideo(hideVideo),
            singles = content.singles
                .filterExplicit(hideExplicit)
                .filterVideo(hideVideo),
            eps = content.eps
                .filterExplicit(hideExplicit)
                .filterVideo(hideVideo),
        )
    }

    private data class ArtistRanking(
        val favourites: Set<String>,
        val all: Map<Int, String>,
    )
}