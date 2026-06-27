/*
 * OpenTune Project Original (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.arturo254.opentune.ui.screens.settings

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.arturo254.opentune.LocalPlayerAwareWindowInsets
import com.arturo254.opentune.R
import com.arturo254.opentune.constants.AndroidAutoConstants
import com.arturo254.opentune.constants.AlbumSortType
import com.arturo254.opentune.constants.ArtistSortType
import com.arturo254.opentune.constants.SongSortType
import com.arturo254.opentune.ui.component.*
import com.arturo254.opentune.ui.utils.backToMain
import com.arturo254.opentune.utils.rememberEnumPreference
import com.arturo254.opentune.utils.rememberPreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AndroidAutoSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    context: Context
) {
    val (androidAutoEnabled, onAndroidAutoEnabledChange) = rememberPreference(
        AndroidAutoConstants.AndroidAutoEnabledKey,
        defaultValue = true
    )
    val (showLikedSongs, onShowLikedSongsChange) = rememberPreference(
        AndroidAutoConstants.AndroidAutoShowLikedSongsKey,
        defaultValue = true
    )
    val (showDownloaded, onShowDownloadedChange) = rememberPreference(
        AndroidAutoConstants.AndroidAutoShowDownloadedKey,
        defaultValue = true
    )
    val (showYouTubePlaylists, onShowYouTubePlaylistsChange) = rememberPreference(
        AndroidAutoConstants.AndroidAutoShowYouTubePlaylistsKey,
        defaultValue = true
    )
    val (showHistory, onShowHistoryChange) = rememberPreference(
        AndroidAutoConstants.AndroidAutoShowHistoryKey,
        defaultValue = true
    )
    val (simplifiedMode, onSimplifiedModeChange) = rememberPreference(
        AndroidAutoConstants.AndroidAutoSimplifiedModeKey,
        defaultValue = false
    )
    val (songSortType, onSongSortTypeChange) = rememberEnumPreference(
        AndroidAutoConstants.AndroidAutoSongSortTypeKey,
        defaultValue = SongSortType.CREATE_DATE
    )
    val (albumSortType, onAlbumSortTypeChange) = rememberEnumPreference(
        AndroidAutoConstants.AndroidAutoAlbumSortTypeKey,
        defaultValue = AlbumSortType.CREATE_DATE
    )
    val (artistSortType, onArtistSortTypeChange) = rememberEnumPreference(
        AndroidAutoConstants.AndroidAutoArtistSortTypeKey,
        defaultValue = ArtistSortType.CREATE_DATE
    )
    val (itemLimit, onItemLimitChange) = rememberPreference(
        AndroidAutoConstants.AndroidAutoItemLimitKey,
        defaultValue = 500
    )

    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
            .verticalScroll(rememberScrollState()),
    ) {
        PreferenceGroupTitle(
            title = stringResource(R.string.android_auto_general),
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.android_auto_enable)) },
            description = stringResource(R.string.android_auto_enable_desc),
            icon = { Icon(painterResource(R.drawable.directions_car), null) },
            checked = androidAutoEnabled,
            onCheckedChange = onAndroidAutoEnabledChange,
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.android_auto_simplified_mode)) },
            description = stringResource(R.string.android_auto_simplified_mode_desc),
            icon = { Icon(painterResource(R.drawable.visibility_off), null) },
            checked = simplifiedMode,
            onCheckedChange = onSimplifiedModeChange,
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Tutorial Section
        PreferenceGroupTitle(title = stringResource(R.string.android_auto_tutorial_title))

        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Step 1
                TutorialStep(
                    stepNumber = 1,
                    text = stringResource(R.string.android_auto_tutorial_step1),
                    icon = R.drawable.android_auto,
                )

                // Step 2
                TutorialStep(
                    stepNumber = 2,
                    text = stringResource(R.string.android_auto_tutorial_step2),
                    icon = R.drawable.settings,
                )

                // Step 3
                TutorialStep(
                    stepNumber = 3,
                    text = stringResource(R.string.android_auto_tutorial_step3),
                    icon = R.drawable.code,
                )

                // Step 4
                TutorialStep(
                    stepNumber = 4,
                    text = stringResource(R.string.android_auto_tutorial_step4),
                    icon = R.drawable.replay,
                )

                // Step 5
                TutorialStep(
                    stepNumber = 5,
                    text = stringResource(R.string.android_auto_tutorial_step5),
                    icon = R.drawable.check,
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        PreferenceGroupTitle(
            title = stringResource(R.string.android_auto_library),
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.android_auto_show_history)) },
            icon = { Icon(painterResource(R.drawable.history), null) },
            checked = showHistory,
            onCheckedChange = onShowHistoryChange,
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.android_auto_show_liked_songs)) },
            icon = { Icon(painterResource(R.drawable.favorite), null) },
            checked = showLikedSongs,
            onCheckedChange = onShowLikedSongsChange,
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.android_auto_show_downloaded)) },
            icon = { Icon(painterResource(R.drawable.offline), null) },
            checked = showDownloaded,
            onCheckedChange = onShowDownloadedChange,
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.android_auto_show_youtube_playlists)) },
            description = stringResource(R.string.android_auto_show_youtube_playlists_desc),
            icon = { Icon(painterResource(R.drawable.playlist_play), null) },
            checked = showYouTubePlaylists,
            onCheckedChange = onShowYouTubePlaylistsChange,
        )

        Spacer(modifier = Modifier.height(24.dp))

        PreferenceGroupTitle(
            title = stringResource(R.string.android_auto_sorting),
        )

        EnumListPreference(
            title = { Text(stringResource(R.string.android_auto_song_sort)) },
            icon = { Icon(painterResource(R.drawable.music_note), null) },
            selectedValue = songSortType,
            valueText = { sortType ->
                when (sortType) {
                    SongSortType.CREATE_DATE -> stringResource(R.string.sort_by_create_date)
                    SongSortType.NAME -> stringResource(R.string.sort_by_name)
                    SongSortType.ARTIST -> stringResource(R.string.sort_by_artist)
                    SongSortType.PLAY_TIME -> stringResource(R.string.sort_by_play_time)
                }
            },
            onValueSelected = onSongSortTypeChange
        )

        EnumListPreference(
            title = { Text(stringResource(R.string.android_auto_album_sort)) },
            icon = { Icon(painterResource(R.drawable.album), null) },
            selectedValue = albumSortType,
            valueText = { sortType ->
                when (sortType) {
                    AlbumSortType.CREATE_DATE -> stringResource(R.string.sort_by_create_date)
                    AlbumSortType.NAME -> stringResource(R.string.sort_by_name)
                    AlbumSortType.ARTIST -> stringResource(R.string.sort_by_artist)
                    AlbumSortType.YEAR -> stringResource(R.string.sort_by_year)
                    AlbumSortType.SONG_COUNT -> stringResource(R.string.sort_by_song_count)
                    AlbumSortType.LENGTH -> stringResource(R.string.sort_by_length)
                    AlbumSortType.PLAY_TIME -> stringResource(R.string.sort_by_play_time)
                }
            },
            onValueSelected = onAlbumSortTypeChange
        )

        EnumListPreference(
            title = { Text(stringResource(R.string.android_auto_artist_sort)) },
            icon = { Icon(painterResource(R.drawable.artist), null) },
            selectedValue = artistSortType,
            valueText = { sortType ->
                when (sortType) {
                    ArtistSortType.CREATE_DATE -> stringResource(R.string.sort_by_create_date)
                    ArtistSortType.NAME -> stringResource(R.string.sort_by_name)
                    ArtistSortType.SONG_COUNT -> stringResource(R.string.sort_by_song_count)
                    ArtistSortType.PLAY_TIME -> stringResource(R.string.sort_by_play_time)
                }
            },
            onValueSelected = onArtistSortTypeChange
        )

        Spacer(modifier = Modifier.height(24.dp))

        PreferenceGroupTitle(
            title = stringResource(R.string.android_auto_performance),
        )

        NumberPickerPreference(
            title = { Text(stringResource(R.string.android_auto_item_limit)) },
            icon = { Icon(painterResource(R.drawable.list), null) },
            value = itemLimit,
            onValueChange = onItemLimitChange,
            minValue = 100,
            maxValue = 2000,
            valueText = { "$it items" }
        )
    }

    TopAppBar(
        title = { Text(stringResource(R.string.android_auto)) },
        navigationIcon = {
            com.arturo254.opentune.ui.component.IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain,
            ) {
                Icon(
                    painter = painterResource(R.drawable.arrow_back),
                    contentDescription = null,
                )
            }
        },
        scrollBehavior = scrollBehavior,
    )
}

@Composable
private fun TutorialStep(
    stepNumber: Int,
    text: String,
    icon: Int,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Step number badge
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(36.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = stepNumber.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }

        // Icon
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.tertiaryContainer,
            modifier = Modifier.size(32.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        // Text
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
    }
}
