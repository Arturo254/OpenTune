package com.arturo254.opentune.ui.screens.settings

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.arturo254.opentune.R
import com.arturo254.opentune.constants.AnimateLyricsKey
import com.arturo254.opentune.constants.ChipSortTypeKey
import com.arturo254.opentune.constants.DarkModeKey
import com.arturo254.opentune.constants.DefaultMiniPlayerThumbnailShape
import com.arturo254.opentune.constants.DefaultOpenTabKey
import com.arturo254.opentune.constants.DefaultPlayPauseButtonShape
import com.arturo254.opentune.constants.DefaultSmallButtonsShape
import com.arturo254.opentune.constants.DynamicThemeKey
import com.arturo254.opentune.constants.GridItemSize
import com.arturo254.opentune.constants.GridItemsSizeKey
import com.arturo254.opentune.constants.LibraryFilter
import com.arturo254.opentune.constants.LyricsClickKey
import com.arturo254.opentune.constants.LyricsTextPositionKey
import com.arturo254.opentune.constants.MiniPlayerThumbnailShapeKey
import com.arturo254.opentune.constants.PlayPauseButtonShapeKey
import com.arturo254.opentune.constants.PlayerBackgroundStyle
import com.arturo254.opentune.constants.PlayerBackgroundStyleKey
import com.arturo254.opentune.constants.PlayerButtonsStyle
import com.arturo254.opentune.constants.PlayerButtonsStyleKey
import com.arturo254.opentune.constants.PlayerTextAlignmentKey
import com.arturo254.opentune.constants.PureBlackKey
import com.arturo254.opentune.constants.RotateBackgroundKey
import com.arturo254.opentune.constants.SliderStyle
import com.arturo254.opentune.constants.SliderStyleKey
import com.arturo254.opentune.constants.SlimNavBarKey
import com.arturo254.opentune.constants.SmallButtonsShapeKey
import com.arturo254.opentune.constants.SwipeThumbnailKey
import com.arturo254.opentune.ui.component.AvatarSelector
import com.arturo254.opentune.ui.component.DefaultDialog
import com.arturo254.opentune.ui.component.EnumListPreference
import com.arturo254.opentune.ui.component.LanguagePreference
import com.arturo254.opentune.ui.component.ListPreference
import com.arturo254.opentune.ui.component.PlayerSliderTrack
import com.arturo254.opentune.ui.component.PreferenceEntry
import com.arturo254.opentune.ui.component.SettingsGeneralCategory
import com.arturo254.opentune.ui.component.SettingsPage
import com.arturo254.opentune.ui.component.SwitchPreference
import com.arturo254.opentune.ui.component.ThumbnailCornerRadiusSelectorButton
import com.arturo254.opentune.ui.component.UnifiedShapeSelectorButton
import com.arturo254.opentune.utils.rememberEnumPreference
import com.arturo254.opentune.utils.rememberPreference
import me.saket.squiggles.SquigglySlider
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val (dynamicTheme, onDynamicThemeChange) = rememberPreference(
        DynamicThemeKey,
        defaultValue = true
    )
    val (playerTextAlignment, onPlayerTextAlignmentChange) =
        rememberEnumPreference(
            PlayerTextAlignmentKey,
            defaultValue = PlayerTextAlignment.CENTER,
        )

    val (darkMode, onDarkModeChange) = rememberEnumPreference(
        DarkModeKey,
        defaultValue = DarkMode.AUTO
    )

    val (playerButtonsStyle, onPlayerButtonsStyleChange) = rememberEnumPreference(
        PlayerButtonsStyleKey,
        defaultValue = PlayerButtonsStyle.DEFAULT
    )
    val (playerBackground, onPlayerBackgroundChange) =
        rememberEnumPreference(
            PlayerBackgroundStyleKey,
            defaultValue = PlayerBackgroundStyle.DEFAULT,
        )
    val (pureBlack, onPureBlackChange) = rememberPreference(PureBlackKey, defaultValue = false)
    val (defaultOpenTab, onDefaultOpenTabChange) = rememberEnumPreference(
        DefaultOpenTabKey,
        defaultValue = NavigationTab.HOME
    )
    val (lyricsPosition, onLyricsPositionChange) = rememberEnumPreference(
        LyricsTextPositionKey,
        defaultValue = LyricsPosition.CENTER
    )
    val (lyricsClick, onLyricsClickChange) = rememberPreference(LyricsClickKey, defaultValue = true)
    val (sliderStyle, onSliderStyleChange) = rememberEnumPreference(
        SliderStyleKey,
        defaultValue = SliderStyle.SQUIGGLY
    )
    val (swipeThumbnail, onSwipeThumbnailChange) = rememberPreference(
        SwipeThumbnailKey,
        defaultValue = true
    )
    val (gridItemSize, onGridItemSizeChange) = rememberEnumPreference(
        GridItemsSizeKey,
        defaultValue = GridItemSize.BIG
    )
    val (animateLyrics, onAnimateLyricsChange) = rememberPreference(
        AnimateLyricsKey,
        defaultValue = true
    )

    val (rotateBackground, onRotateBackgroundChange) = rememberPreference(
        key = RotateBackgroundKey,
        defaultValue = false
    )

    // Estados de formas
    val smallButtonsShapeState = rememberPreference(
        key = SmallButtonsShapeKey,
        defaultValue = DefaultSmallButtonsShape
    )

    val playPauseShapeState = rememberPreference(
        key = PlayPauseButtonShapeKey,
        defaultValue = DefaultPlayPauseButtonShape
    )

    val miniPlayerThumbnailShapeState = rememberPreference(
        key = MiniPlayerThumbnailShapeKey,
        defaultValue = DefaultMiniPlayerThumbnailShape
    )

    val (slimNav, onSlimNavChange) = rememberPreference(SlimNavBarKey, defaultValue = false)

    val isSystemInDarkTheme = isSystemInDarkTheme()
    val useDarkTheme =
        remember(darkMode, isSystemInDarkTheme) {
            if (darkMode == DarkMode.AUTO) isSystemInDarkTheme else darkMode == DarkMode.ON
        }

    // Automatically disable pureBlack when switching to light mode
    LaunchedEffect(useDarkTheme) {
        if (!useDarkTheme && pureBlack) {
            onPureBlackChange(false)
        }
    }

    val (defaultChip, onDefaultChipChange) = rememberEnumPreference(
        key = ChipSortTypeKey,
        defaultValue = LibraryFilter.LIBRARY
    )

    var showSliderOptionDialog by rememberSaveable {
        mutableStateOf(false)
    }

    if (showSliderOptionDialog) {
        DefaultDialog(
            buttons = {
                TextButton(
                    onClick = { showSliderOptionDialog = false }
                ) {
                    Text(text = stringResource(android.R.string.cancel))
                }
            },
            onDismiss = {
                showSliderOptionDialog = false
            }
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .aspectRatio(1f)
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .border(
                            1.dp,
                            if (sliderStyle == SliderStyle.DEFAULT) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                            RoundedCornerShape(16.dp)
                        )
                        .clickable {
                            onSliderStyleChange(SliderStyle.DEFAULT)
                            showSliderOptionDialog = false
                        }
                        .padding(16.dp)
                ) {
                    var sliderValue by remember {
                        mutableFloatStateOf(0.5f)
                    }
                    Slider(
                        value = sliderValue,
                        valueRange = 0f..1f,
                        onValueChange = {
                            sliderValue = it
                        },
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = stringResource(R.string.default_),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .aspectRatio(1f)
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .border(
                            1.dp,
                            if (sliderStyle == SliderStyle.SQUIGGLY) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                            RoundedCornerShape(16.dp)
                        )
                        .clickable {
                            onSliderStyleChange(SliderStyle.SQUIGGLY)
                            showSliderOptionDialog = false
                        }
                        .padding(16.dp)
                ) {
                    var sliderValue by remember {
                        mutableFloatStateOf(0.5f)
                    }
                    SquigglySlider(
                        value = sliderValue,
                        valueRange = 0f..1f,
                        onValueChange = {
                            sliderValue = it
                        },
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = stringResource(R.string.squiggly),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .aspectRatio(1f)
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .border(
                            1.dp,
                            if (sliderStyle == SliderStyle.SLIM) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                            RoundedCornerShape(16.dp)
                        )
                        .clickable {
                            onSliderStyleChange(SliderStyle.SLIM)
                            showSliderOptionDialog = false
                        }
                        .padding(16.dp)
                ) {
                    var sliderValue by remember {
                        mutableFloatStateOf(0.5f)
                    }
                    Slider(
                        value = sliderValue,
                        valueRange = 0f..1f,
                        onValueChange = {
                            sliderValue = it
                        },
                        thumb = { Spacer(modifier = Modifier.size(0.dp)) },
                        track = { sliderState ->
                            PlayerSliderTrack(
                                sliderState = sliderState,
                                colors = SliderDefaults.colors()
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = {}
                                )
                            }
                    )

                    Text(
                        text = stringResource(R.string.slim),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }


    SettingsPage(
        title = stringResource(R.string.appearance),
        navController = navController,
        scrollBehavior = scrollBehavior
    ) {
        SettingsGeneralCategory(
            title = stringResource(R.string.theme),
            items = listOf(
                {SwitchPreference(
                    title = { Text(stringResource(R.string.enable_dynamic_theme)) },
                    icon = { Icon(painterResource(R.drawable.palette), null) },
                    checked = dynamicTheme,
                    onCheckedChange = onDynamicThemeChange,
                )},
                {EnumListPreference(
                    title = { Text(stringResource(R.string.dark_theme)) },
                    icon = { Icon(painterResource(R.drawable.dark_mode), null) },
                    selectedValue = darkMode,
                    onValueSelected = onDarkModeChange,
                    valueText = {
                        when (it) {
                            DarkMode.ON -> stringResource(R.string.dark_theme_on)
                            DarkMode.OFF -> stringResource(R.string.dark_theme_off)
                            DarkMode.AUTO -> stringResource(R.string.dark_theme_follow_system)
                        }
                    },
                )},
                {AnimatedVisibility(useDarkTheme) {
                    SwitchPreference(
                        title = { Text(stringResource(R.string.pure_black)) },
                        icon = { Icon(painterResource(R.drawable.contrast), null) },
                        checked = pureBlack && useDarkTheme,
                        onCheckedChange = { newValue ->
                            if (useDarkTheme) {
                                onPureBlackChange(newValue)
                            }
                        },
                        isEnabled = useDarkTheme
                    )
                }}
            )
        )

        // Language preferences
        SettingsGeneralCategory(
            title = stringResource(R.string.app_language),
            items = listOf(
                { LanguagePreference() }
            )
        )

        // Determine the options available based on the Android version
        val availableBackgroundStyles = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            enumValues<PlayerBackgroundStyle>().toList()
        } else {
            enumValues<PlayerBackgroundStyle>().filter {
                it != PlayerBackgroundStyle.BLUR
            }
        }

        // Also ensure that the selected value is compatible.
        val safeSelectedValue = if (playerBackground == PlayerBackgroundStyle.BLUR &&
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S
        ) {
            PlayerBackgroundStyle.DEFAULT
        } else {
            playerBackground
        }

        SettingsGeneralCategory(
            title = stringResource(R.string.player),
            items = listOf(
                {EnumListPreference(
                    title = { Text(stringResource(R.string.player_background_style)) },
                    icon = { Icon(painterResource(R.drawable.gradient), null) },
                    selectedValue = safeSelectedValue,
                    onValueSelected = onPlayerBackgroundChange,
                    valueText = {
                        when (it) {
                            PlayerBackgroundStyle.DEFAULT -> stringResource(R.string.follow_theme)
                            PlayerBackgroundStyle.GRADIENT -> stringResource(R.string.gradient)
                            PlayerBackgroundStyle.BLUR -> stringResource(R.string.player_background_blur)
                            PlayerBackgroundStyle.APPLE_MUSIC -> stringResource(R.string.apple_music)
                        }
                    },
                    values = availableBackgroundStyles
                )},

                {ThumbnailCornerRadiusSelectorButton(
                    onRadiusSelected = { selectedRadius ->
                        Timber.tag("Thumbnail").d("Selected radio: $selectedRadius")
                    }
                )},

                {EnumListPreference(
                    title = { Text(stringResource(R.string.player_buttons_style)) },
                    icon = { Icon(painterResource(R.drawable.palette), null) },
                    selectedValue = playerButtonsStyle,
                    onValueSelected = onPlayerButtonsStyleChange,
                    valueText = {
                        when (it) {
                            PlayerButtonsStyle.DEFAULT -> stringResource(R.string.default_style)
                            PlayerButtonsStyle.PRIMARY -> stringResource(R.string.secondary_color_style)
                            PlayerButtonsStyle.TERTIARY -> stringResource(R.string.tertiary_color_style)
                        }
                    },
                )},

                {PreferenceEntry(
                    title = { Text(stringResource(R.string.player_slider_style)) },
                    description =
                        when (sliderStyle) {
                            SliderStyle.DEFAULT -> stringResource(R.string.default_)
                            SliderStyle.SQUIGGLY -> stringResource(R.string.squiggly)
                            SliderStyle.SLIM -> stringResource(R.string.slim)
                        },
                    icon = { Icon(painterResource(R.drawable.sliders), null) },
                    onClick = {
                        showSliderOptionDialog = true
                    },
                )},

                {SwitchPreference(
                    title = { Text(stringResource(R.string.enable_swipe_thumbnail)) },
                    icon = { Icon(painterResource(R.drawable.swipe), null) },
                    checked = swipeThumbnail,
                    onCheckedChange = onSwipeThumbnailChange,
                )},

                {SwitchPreference(
                    title = { Text(stringResource(R.string.Rotatelyricsbackground)) },
                    description = null,
                    icon = { Icon(painterResource(R.drawable.album), null) },
                    checked = rotateBackground,
                    onCheckedChange = onRotateBackgroundChange
                )},

                {EnumListPreference(
                    title = { Text(stringResource(R.string.player_text_alignment)) },
                    icon = {
                        Icon(
                            painter =
                                painterResource(
                                    when (playerTextAlignment) {
                                        PlayerTextAlignment.CENTER -> R.drawable.format_align_center
                                        PlayerTextAlignment.SIDED -> R.drawable.format_align_left
                                    },
                                ),
                            contentDescription = null,
                        )
                    },
                    selectedValue = playerTextAlignment,
                    onValueSelected = onPlayerTextAlignmentChange,
                    valueText = {
                        when (it) {
                            PlayerTextAlignment.SIDED -> stringResource(R.string.sided)
                            PlayerTextAlignment.CENTER -> stringResource(R.string.center)
                        }
                    },
                )},

                {EnumListPreference(
                    title = { Text(stringResource(R.string.lyrics_text_position)) },
                    icon = { Icon(painterResource(R.drawable.lyrics), null) },
                    selectedValue = lyricsPosition,
                    onValueSelected = onLyricsPositionChange,
                    valueText = {
                        when (it) {
                            LyricsPosition.LEFT -> stringResource(R.string.left)
                            LyricsPosition.CENTER -> stringResource(R.string.center)
                            LyricsPosition.RIGHT -> stringResource(R.string.right)
                        }
                    },
                )},

                {SwitchPreference(
                    title = { Text(stringResource(R.string.lyrics_click_change)) },
                    icon = { Icon(painterResource(R.drawable.lyrics), null) },
                    checked = lyricsClick,
                    onCheckedChange = onLyricsClickChange,
                )},

                {SwitchPreference(
                    title = { Text(stringResource(R.string.animate_lyrics)) },
                    icon = { Icon(painterResource(R.drawable.lyrics), null) },
                    description = stringResource(R.string.animate_lyrics_desc),
                    checked = animateLyrics,
                    onCheckedChange = onAnimateLyricsChange
                )}
            )
        )

        SettingsGeneralCategory(
            title = stringResource(R.string.misc),
            items = listOf(
                {EnumListPreference(
                    title = { Text(stringResource(R.string.default_open_tab)) },
                    icon = { Icon(painterResource(R.drawable.nav_bar), null) },
                    selectedValue = defaultOpenTab,
                    onValueSelected = onDefaultOpenTabChange,
                    valueText = {
                        when (it) {
                            NavigationTab.HOME -> stringResource(R.string.home)
                            NavigationTab.EXPLORE -> stringResource(R.string.explore)
                            NavigationTab.LIBRARY -> stringResource(R.string.filter_library)
                        }
                    },
                )},

                {ListPreference(
                    title = { Text(stringResource(R.string.default_lib_chips)) },
                    icon = { Icon(painterResource(R.drawable.tab), null) },
                    selectedValue = defaultChip,
                    values = listOf(
                        LibraryFilter.LIBRARY, LibraryFilter.PLAYLISTS, LibraryFilter.SONGS,
                        LibraryFilter.ALBUMS, LibraryFilter.ARTISTS
                    ),
                    valueText = {
                        when (it) {
                            LibraryFilter.SONGS -> stringResource(R.string.songs)
                            LibraryFilter.ARTISTS -> stringResource(R.string.artists)
                            LibraryFilter.ALBUMS -> stringResource(R.string.albums)
                            LibraryFilter.PLAYLISTS -> stringResource(R.string.playlists)
                            LibraryFilter.LIBRARY -> stringResource(R.string.filter_library)
                        }
                    },
                    onValueSelected = onDefaultChipChange,
                )},

                {SwitchPreference(
                    title = { Text(stringResource(R.string.slim_navbar)) },
                    icon = { Icon(painterResource(R.drawable.nav_bar), null) },
                    checked = slimNav,
                    onCheckedChange = onSlimNavChange
                )},

                {EnumListPreference(
                    title = { Text(stringResource(R.string.grid_cell_size)) },
                    icon = { Icon(painterResource(R.drawable.grid_view), null) },
                    selectedValue = gridItemSize,
                    onValueSelected = onGridItemSizeChange,
                    valueText = {
                        when (it) {
                            GridItemSize.SMALL -> stringResource(R.string.small)
                            GridItemSize.BIG -> stringResource(R.string.big)
                        }
                    },
                )},
            )
        )

        // New avatar selector
        AvatarSelector(modifier = Modifier.padding(vertical = 8.dp))
    }
}

enum class DarkMode {
    ON,
    OFF,
    AUTO,
}

enum class NavigationTab {
    HOME,
    EXPLORE,
    LIBRARY,
}

enum class LyricsPosition {
    LEFT,
    CENTER,
    RIGHT,
}

enum class PlayerTextAlignment {
    SIDED,
    CENTER,
}