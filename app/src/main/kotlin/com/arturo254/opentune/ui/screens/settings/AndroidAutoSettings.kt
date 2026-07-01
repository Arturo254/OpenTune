@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

/*
 * OpenTune Project Original (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.arturo254.opentune.ui.screens.settings

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.arturo254.opentune.LocalPlayerAwareWindowInsets
import com.arturo254.opentune.R
import com.arturo254.opentune.constants.AlbumSortType
import com.arturo254.opentune.constants.AndroidAutoConstants
import com.arturo254.opentune.constants.ArtistSortType
import com.arturo254.opentune.constants.SongSortType
import com.arturo254.opentune.ui.component.EnumListPreference
import com.arturo254.opentune.ui.component.NumberPickerPreference
import com.arturo254.opentune.ui.component.PreferenceGroup
import com.arturo254.opentune.ui.component.PreferenceGroupDivider
import com.arturo254.opentune.ui.component.SwitchPreference
import com.arturo254.opentune.ui.utils.backToMain
import com.arturo254.opentune.utils.rememberEnumPreference
import com.arturo254.opentune.utils.rememberPreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AndroidAutoSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    context: Context,
) {
    val (androidAutoEnabled, onAndroidAutoEnabledChange) = rememberPreference(
        AndroidAutoConstants.AndroidAutoEnabledKey,
        defaultValue = true,
    )
    val (showLikedSongs, onShowLikedSongsChange) = rememberPreference(
        AndroidAutoConstants.AndroidAutoShowLikedSongsKey,
        defaultValue = true,
    )
    val (showDownloaded, onShowDownloadedChange) = rememberPreference(
        AndroidAutoConstants.AndroidAutoShowDownloadedKey,
        defaultValue = true,
    )
    val (showYouTubePlaylists, onShowYouTubePlaylistsChange) = rememberPreference(
        AndroidAutoConstants.AndroidAutoShowYouTubePlaylistsKey,
        defaultValue = true,
    )
    val (showHistory, onShowHistoryChange) = rememberPreference(
        AndroidAutoConstants.AndroidAutoShowHistoryKey,
        defaultValue = true,
    )
    val (simplifiedMode, onSimplifiedModeChange) = rememberPreference(
        AndroidAutoConstants.AndroidAutoSimplifiedModeKey,
        defaultValue = false,
    )
    val (songSortType, onSongSortTypeChange) = rememberEnumPreference(
        AndroidAutoConstants.AndroidAutoSongSortTypeKey,
        defaultValue = SongSortType.CREATE_DATE,
    )
    val (albumSortType, onAlbumSortTypeChange) = rememberEnumPreference(
        AndroidAutoConstants.AndroidAutoAlbumSortTypeKey,
        defaultValue = AlbumSortType.CREATE_DATE,
    )
    val (artistSortType, onArtistSortTypeChange) = rememberEnumPreference(
        AndroidAutoConstants.AndroidAutoArtistSortTypeKey,
        defaultValue = ArtistSortType.CREATE_DATE,
    )
    val (itemLimit, onItemLimitChange) = rememberPreference(
        AndroidAutoConstants.AndroidAutoItemLimitKey,
        defaultValue = 500,
    )

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            LargeFlexibleTopAppBar(
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
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
                scrollBehavior = scrollBehavior,
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            AndroidAutoHeroCard(
                androidAutoEnabled = androidAutoEnabled,
                onAndroidAutoEnabledChange = onAndroidAutoEnabledChange,
                simplifiedMode = simplifiedMode,
                onSimplifiedModeChange = onSimplifiedModeChange,
                itemLimit = itemLimit,
            )

            SettingsSectionHeader(
                title = stringResource(R.string.android_auto_library),
                description = stringResource(R.string.android_auto_show_youtube_playlists_desc),
                icon = R.drawable.playlist_play,
            )
            PreferenceGroup {
                SwitchPreference(
                    title = { Text(stringResource(R.string.android_auto_show_history)) },
                    icon = { Icon(painterResource(R.drawable.history), null) },
                    checked = showHistory,
                    onCheckedChange = onShowHistoryChange,
                )
                PreferenceGroupDivider()
                SwitchPreference(
                    title = { Text(stringResource(R.string.android_auto_show_liked_songs)) },
                    icon = { Icon(painterResource(R.drawable.favorite), null) },
                    checked = showLikedSongs,
                    onCheckedChange = onShowLikedSongsChange,
                )
                PreferenceGroupDivider()
                SwitchPreference(
                    title = { Text(stringResource(R.string.android_auto_show_downloaded)) },
                    icon = { Icon(painterResource(R.drawable.offline), null) },
                    checked = showDownloaded,
                    onCheckedChange = onShowDownloadedChange,
                )
                PreferenceGroupDivider()
                SwitchPreference(
                    title = { Text(stringResource(R.string.android_auto_show_youtube_playlists)) },
                    description = stringResource(R.string.android_auto_show_youtube_playlists_desc),
                    icon = { Icon(painterResource(R.drawable.playlist_play), null) },
                    checked = showYouTubePlaylists,
                    onCheckedChange = onShowYouTubePlaylistsChange,
                )
            }

            SettingsSectionHeader(
                title = stringResource(R.string.android_auto_sorting),
                description = stringResource(R.string.sort_by_create_date),
                icon = R.drawable.list,
            )
            PreferenceGroup {
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
                    onValueSelected = onSongSortTypeChange,
                )
                PreferenceGroupDivider()
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
                    onValueSelected = onAlbumSortTypeChange,
                )
                PreferenceGroupDivider()
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
                    onValueSelected = onArtistSortTypeChange,
                )
            }

            SettingsSectionHeader(
                title = stringResource(R.string.android_auto_performance),
                description = stringResource(R.string.android_auto_item_limit_desc),
                icon = R.drawable.speed,
            )
            PreferenceGroup {
                NumberPickerPreference(
                    title = { Text(stringResource(R.string.android_auto_item_limit)) },
                    icon = { Icon(painterResource(R.drawable.list), null) },
                    value = itemLimit,
                    onValueChange = onItemLimitChange,
                    minValue = 100,
                    maxValue = 2000,
                    valueText = { "$it items" },
                )
            }

            AndroidAutoTutorialCard()
        }
    }
}

@Composable
private fun AndroidAutoHeroCard(
    androidAutoEnabled: Boolean,
    onAndroidAutoEnabledChange: (Boolean) -> Unit,
    simplifiedMode: Boolean,
    onSimplifiedModeChange: (Boolean) -> Unit,
    itemLimit: Int,
) {
    val heroBrush = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.20f),
            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.14f),
            MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    )
    val uncheckedColors = ToggleButtonDefaults.toggleButtonColors(
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
        contentColor = MaterialTheme.colorScheme.onSurface,
    )
    val checkedColors = ToggleButtonDefaults.toggleButtonColors(
        checkedContainerColor = MaterialTheme.colorScheme.primary,
        checkedContentColor = MaterialTheme.colorScheme.onPrimary,
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
        contentColor = MaterialTheme.colorScheme.onSurface,
    )
    val simplifiedColors = ToggleButtonDefaults.toggleButtonColors(
        checkedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
        checkedContentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
        contentColor = MaterialTheme.colorScheme.onSurface,
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 6.dp,
    ) {
        Column(
            modifier = Modifier
                .background(heroBrush)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(56.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(R.drawable.android_auto),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = stringResource(R.string.android_auto_enable),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = stringResource(R.string.android_auto_enable_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                ) {
                    Text(
                        text = stringResource(R.string.android_auto_simplified_mode),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                ) {
                    Text(
                        text = "$itemLimit items",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ToggleButton(
                    checked = androidAutoEnabled,
                    onCheckedChange = onAndroidAutoEnabledChange,
                    modifier = Modifier.weight(1f),
                    shapes = ButtonGroupDefaults.connectedLeadingButtonShapes(),
                    colors = if (androidAutoEnabled) checkedColors else uncheckedColors,
                ) {
                    ToggleButtonLabel(
                        icon = if (androidAutoEnabled) R.drawable.check else R.drawable.close,
                        text = stringResource(R.string.android_auto_enable),
                    )
                }
                ToggleButton(
                    checked = simplifiedMode,
                    onCheckedChange = onSimplifiedModeChange,
                    modifier = Modifier.weight(1f),
                    shapes = ButtonGroupDefaults.connectedTrailingButtonShapes(),
                    colors = if (simplifiedMode) simplifiedColors else uncheckedColors,
                ) {
                    ToggleButtonLabel(
                        icon = R.drawable.visibility_off,
                        text = stringResource(R.string.android_auto_simplified_mode),
                    )
                }
            }

            Text(
                text = stringResource(R.string.android_auto_simplified_mode_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SettingsSectionHeader(
    title: String,
    description: String,
    icon: Int,
) {
    Row(
        modifier = Modifier.padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AndroidAutoTutorialCard() {
    val steps = listOf(
        stringResource(R.string.android_auto_tutorial_step1) to R.drawable.android_auto,
        stringResource(R.string.android_auto_tutorial_step2) to R.drawable.settings,
        stringResource(R.string.android_auto_tutorial_step3) to R.drawable.code,
        stringResource(R.string.android_auto_tutorial_step4) to R.drawable.replay,
        stringResource(R.string.android_auto_tutorial_step5) to R.drawable.check,
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsSectionHeader(
            title = stringResource(R.string.android_auto_tutorial_title),
            description = stringResource(R.string.android_auto_enable_desc),
            icon = R.drawable.info,
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 2.dp,
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                steps.forEachIndexed { index, (text, icon) ->
                    TutorialStep(
                        stepNumber = index + 1,
                        text = text,
                        icon = icon,
                    )
                }
            }
        }
    }
}

@Composable
private fun ToggleButtonLabel(
    icon: Int,
    text: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun TutorialStep(
    stepNumber: Int,
    text: String,
    icon: Int,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(40.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = stepNumber.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer,
                modifier = Modifier.size(40.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(icon),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                }
            }

            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
