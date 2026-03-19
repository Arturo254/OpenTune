package com.arturo254.opentune.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arturo254.innertube.YouTube
import com.arturo254.opentune.constants.statToPeriod
import com.arturo254.opentune.db.MusicDatabase
import com.arturo254.opentune.db.entities.ArtistEntity
import com.arturo254.opentune.ui.screens.OptionStats
import com.arturo254.opentune.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneOffset
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class StatsViewModel
@Inject
constructor(
    val database: MusicDatabase,
) : ViewModel() {
    val selectedOption = MutableStateFlow(OptionStats.CONTINUOUS)
    val indexChips = MutableStateFlow(0)

    val mostPlayedSongsStats =
        combine(selectedOption, indexChips) { first, second -> Pair(first, second) }
            .flatMapLatest { (selection, t) ->
                database.mostPlayedSongsStats(
                    fromTimeStamp = statToPeriod(selection, t),
                    limit = -1,
                    toTimeStamp =
                        if (selection == OptionStats.CONTINUOUS || t == 0)
                            LocalDateTime.now().toInstant(ZoneOffset.UTC).toEpochMilli()
                        else
                            statToPeriod(selection, t - 1),
                )
            }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val mostPlayedSongs =
        combine(selectedOption, indexChips) { first, second -> Pair(first, second) }
            .flatMapLatest { (selection, t) ->
                database.mostPlayedSongs(
                    fromTimeStamp = statToPeriod(selection, t),
                    limit = -1,
                    toTimeStamp =
                        if (selection == OptionStats.CONTINUOUS || t == 0)
                            LocalDateTime.now().toInstant(ZoneOffset.UTC).toEpochMilli()
                        else
                            statToPeriod(selection, t - 1),
                )
            }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val mostPlayedArtists =
        combine(selectedOption, indexChips) { first, second -> Pair(first, second) }
            .flatMapLatest { (selection, t) ->
                database.mostPlayedArtists(
                    statToPeriod(selection, t),
                    limit = -1,
                    toTimeStamp =
                        if (selection == OptionStats.CONTINUOUS || t == 0)
                            LocalDateTime.now().toInstant(ZoneOffset.UTC).toEpochMilli()
                        else
                            statToPeriod(selection, t - 1),
                ).map { artists ->
                    artists.filter {
                        it.id.startsWith("UC") ||
                        it.id.startsWith("FEmusic_library_privately_owned_artist")
                    }
                }
            }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val mostPlayedAlbums =
        combine(selectedOption, indexChips) { first, second -> Pair(first, second) }
            .flatMapLatest { (selection, t) ->
                database.mostPlayedAlbums(
                    statToPeriod(selection, t),
                    limit = -1,
                    toTimeStamp =
                        if (selection == OptionStats.CONTINUOUS || t == 0)
                            LocalDateTime.now().toInstant(ZoneOffset.UTC).toEpochMilli()
                        else
                            statToPeriod(selection, t - 1),
                )
            }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val firstEvent =
        database.firstEvent()
            .stateIn(viewModelScope, SharingStarted.Lazily, null)

    init {
        // Refresh stale artist thumbnails in the background
        viewModelScope.launch {
            mostPlayedArtists.collect { artists ->
                artists
                    .filter {
                        it.thumbnailUrl == null ||
                        Duration.between(it.lastUpdateTime, LocalDateTime.now()) > Duration.ofDays(10)
                    }
                    .forEach { artistStats ->
                        YouTube.artist(artistStats.id).onSuccess { artistPage ->
                            database.query {
                                val entity = ArtistEntity(
                                    id = artistStats.id,
                                    name = artistStats.name,
                                    thumbnailUrl = artistStats.thumbnailUrl,
                                    channelId = artistStats.channelId,
                                )
                                update(entity, artistPage)
                            }
                        }
                    }
            }
        }
        // Refresh albums with missing song counts
        viewModelScope.launch {
            mostPlayedAlbums.collect { albums ->
                albums
                    .filter { it.songCountListened == 0 }
                    .forEach { albumStats ->
                        YouTube.album(albumStats.id)
                            .onSuccess { albumPage ->
                                database.query {
                                    val entity = albumEntityById(albumStats.id)
                                    if (entity != null) {
                                        update(entity, albumPage)
                                    }
                                }
                            }
                            .onFailure {
                                reportException(it)
                                if (it.message?.contains("NOT_FOUND") == true) {
                                    database.query {
                                        val entity = albumEntityById(albumStats.id)
                                        if (entity != null) {
                                            delete(entity)
                                        }
                                    }
                                }
                            }
                    }
            }
        }
    }
}
