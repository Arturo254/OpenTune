/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.arturo254.opentune.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.arturo254.opentune.R
import com.arturo254.opentune.constants.ListThumbnailSize
import com.arturo254.opentune.constants.ThumbnailCornerRadius
import com.arturo254.opentune.db.entities.Playlist
import com.arturo254.opentune.db.entities.PlaylistEntity
import com.arturo254.opentune.spotify.SpotifyMapper
import com.arturo254.opentune.spotify.models.SpotifyPlaylist
import com.arturo254.opentune.spotify.models.SpotifyTrack
import com.arturo254.opentune.ui.utils.resize
import com.arturo254.opentune.utils.joinByBullet
import com.arturo254.opentune.utils.makeTimeString

@Composable
fun SpotifyLibraryPlaylistListItem(
    playlist: SpotifyPlaylist,
    navController: NavController,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(26.dp),
) {
    val libraryPlaylist = remember(playlist) { playlist.toLibraryPlaylist() }
    val openPlaylist = {
        navController.navigate("spotify_playlist/${playlist.id}")
    }
    val trailing: @Composable RowScope.() -> Unit = {
        Icon(
            painter = painterResource(R.drawable.spotify_icon),
            contentDescription = stringResource(R.string.spotify_account),
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
    }

    LibraryPlaylistFeatureCard(
        playlist = libraryPlaylist,
        modifier =
            modifier
                .fillMaxWidth()
                .focusable()
                .clickable(onClick = openPlaylist),
    )
}

@Composable
fun SpotifyTrackListItem(
    track: SpotifyTrack,
    modifier: Modifier = Modifier,
    albumIndex: Int? = null,
    badges: @Composable RowScope.() -> Unit = {
        if (track.explicit) {
            Icon(
                painter = painterResource(R.drawable.explicit),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
        }
    },
    isSelected: Boolean = false,
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    showSongIconPlaceholder: Boolean = true,
    trailingContent: @Composable RowScope.() -> Unit = {},
) {
    val duration =
        track.durationMs
            .takeIf { it > 0 }
            ?.toLong()
            ?.let(::makeTimeString)
    val subtitle =
        joinByBullet(
            track.artists.joinToString { it.name },
            duration,
        )

    ListItem(
        title = track.name,
        subtitle = subtitle,
        badges = badges,
        thumbnailContent = {
            ItemThumbnail(
                thumbnailUrl = SpotifyMapper.getTrackThumbnail(track)?.resize(200, 200),
                albumIndex = albumIndex,
                isSelected = isSelected,
                isActive = isActive,
                isPlaying = isPlaying,
                shape = RoundedCornerShape(ThumbnailCornerRadius),
                placeholderIconRes = if (showSongIconPlaceholder) R.drawable.music_note else null,
                modifier = Modifier.size(ListThumbnailSize),
            )
        },
        trailingContent = trailingContent,
        modifier = modifier,
        isActive = isActive,
    )
}

private fun SpotifyPlaylist.toLibraryPlaylist(): Playlist =
    Playlist(
        playlist =
            PlaylistEntity(
                id = "SPOTIFY_PLAYLIST_$id",
                name = name,
                thumbnailUrl = SpotifyMapper.getPlaylistThumbnail(this),
                remoteSongCount = tracks?.total ?: 0,
                isEditable = false,
            ),
        songCount = tracks?.total ?: 0,
        songThumbnails = images.map { it.url },
    )
