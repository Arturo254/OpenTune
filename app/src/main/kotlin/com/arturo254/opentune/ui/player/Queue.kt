/*
 * OpenTune Project Original (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package com.arturo254.opentune.ui.player

import androidx.activity.compose.BackHandler
import android.annotation.SuppressLint
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.exoplayer.source.ShuffleOrder.DefaultShuffleOrder
import androidx.navigation.NavController
import com.arturo254.opentune.LocalPlayerConnection
import com.arturo254.opentune.R
import com.arturo254.opentune.constants.ListItemHeight
import com.arturo254.opentune.constants.PlayerDesignStyle
import com.arturo254.opentune.constants.PlayerDesignStyleKey
import com.arturo254.opentune.constants.QueueEditLockKey
import com.arturo254.opentune.constants.AutoLoadMoreKey
import com.arturo254.opentune.extensions.metadata
import com.arturo254.opentune.extensions.move
import com.arturo254.opentune.extensions.togglePlayPause
import com.arturo254.opentune.extensions.toggleRepeatMode
import com.arturo254.opentune.models.MediaMetadata
import com.arturo254.opentune.ui.component.BottomSheet
import com.arturo254.opentune.ui.component.BottomSheetState
import com.arturo254.opentune.ui.component.LocalBottomSheetPageState
import com.arturo254.opentune.ui.component.LocalMenuState
import com.arturo254.opentune.ui.component.MediaMetadataListItem
import com.arturo254.opentune.ui.menu.PlayerMenu
import com.arturo254.opentune.ui.menu.SelectionMediaMetadataMenu
import com.arturo254.opentune.ui.utils.ShowMediaInfo
import com.arturo254.opentune.utils.rememberEnumPreference
import com.arturo254.opentune.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import androidx.datastore.preferences.core.booleanPreferencesKey


@SuppressLint("UnrememberedMutableState", "LocalContextGetResourceValueCall", "StringFormatInvalid")
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Queue(
    state: BottomSheetState,
    playerBottomSheetState: BottomSheetState,
    navController: NavController,
    modifier: Modifier = Modifier,
    backgroundColor: Color,
    onBackgroundColor: Color,
    TextBackgroundColor: Color,
    textButtonColor: Color,
    iconButtonColor: Color,
    onShowLyrics: () -> Unit = {},
    pureBlack: Boolean,
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val clipboardManager = LocalClipboard.current
    val menuState = LocalMenuState.current
    val bottomSheetPageState = LocalBottomSheetPageState.current

    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val repeatMode by playerConnection.repeatMode.collectAsState()

    val currentWindowIndex by playerConnection.currentWindowIndex.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val currentFormat by playerConnection.currentFormat.collectAsState(initial = null)

    val selectedSongs = remember { mutableStateListOf<MediaMetadata>() }
    val selectedItems = remember { mutableStateListOf<Timeline.Window>() }
    var selection by remember { mutableStateOf(false) }

    if (selection) {
        BackHandler {
            selection = false
        }
    }

    var locked by rememberPreference(QueueEditLockKey, defaultValue = true)
    var infiniteQueueEnabled by rememberPreference(AutoLoadMoreKey, defaultValue = true)
    val togetherSessionState by playerConnection.service.togetherSessionState.collectAsState()
    val togetherForcesLock =
        togetherSessionState is com.arturo254.opentune.together.TogetherSessionState.Joined &&
                (togetherSessionState as com.arturo254.opentune.together.TogetherSessionState.Joined).role is com.arturo254.opentune.together.TogetherRole.Guest
    val effectiveLocked = locked || togetherForcesLock

    val playerDesignStyle by rememberEnumPreference(
        key = PlayerDesignStyleKey,
        defaultValue = PlayerDesignStyle.V4
    )

    val snackbarHostState = remember { SnackbarHostState() }
    var dismissJob: Job? by remember { mutableStateOf(null) }

    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var sleepTimerValue by remember { mutableStateOf(30f) }
    val sleepTimerEnabled = remember(
        playerConnection.service.sleepTimer.triggerTime,
        playerConnection.service.sleepTimer.pauseWhenSongEnd
    ) {
        playerConnection.service.sleepTimer.isActive
    }
    var sleepTimerTimeLeft by remember { mutableStateOf(0L) }

    val (showCodecOnPlayer) = rememberPreference(
        key = booleanPreferencesKey("show_codec_on_player"),
        defaultValue = false
    )

    LaunchedEffect(sleepTimerEnabled) {
        if (sleepTimerEnabled) {
            while (isActive) {
                sleepTimerTimeLeft = if (playerConnection.service.sleepTimer.pauseWhenSongEnd) {
                    playerConnection.player.duration - playerConnection.player.currentPosition
                } else {
                    playerConnection.service.sleepTimer.triggerTime - System.currentTimeMillis()
                }
                delay(1000L)
            }
        }
    }

    BottomSheet(
        state = state,
        backgroundColor = Color.Unspecified,
        modifier = modifier,
        collapsedContent = {
            when (playerDesignStyle) {
                PlayerDesignStyle.V2 -> {
                    QueueCollapsedContentV2(
                        showCodecOnPlayer = showCodecOnPlayer,
                        currentFormat = currentFormat,
                        textBackgroundColor = TextBackgroundColor,
                        textButtonColor = textButtonColor,
                        iconButtonColor = iconButtonColor,
                        sleepTimerEnabled = sleepTimerEnabled,
                        sleepTimerTimeLeft = sleepTimerTimeLeft,
                        repeatMode = repeatMode,
                        mediaMetadata = mediaMetadata,
                        onExpandQueue = { state.expandSoft() },
                        onSleepTimerClick = {
                            if (sleepTimerEnabled) {
                                playerConnection.service.sleepTimer.clear()
                            } else {
                                showSleepTimerDialog = true
                            }
                        },
                        onShowLyrics = onShowLyrics,
                        onRepeatModeClick = { playerConnection.player.toggleRepeatMode() },
                        onMenuClick = {
                            menuState.show {
                                PlayerMenu(
                                    mediaMetadata = mediaMetadata,
                                    navController = navController,
                                    playerBottomSheetState = playerBottomSheetState,
                                    onShowDetailsDialog = {
                                        mediaMetadata?.id?.let {
                                            bottomSheetPageState.show {
                                                ShowMediaInfo(it)
                                            }
                                        }
                                    },
                                    onDismiss = menuState::dismiss
                                )
                            }
                        }
                    )
                }

                PlayerDesignStyle.V3 -> {
                    QueueCollapsedContentV3(
                        showCodecOnPlayer = showCodecOnPlayer,
                        currentFormat = currentFormat,
                        textBackgroundColor = TextBackgroundColor,
                        sleepTimerEnabled = sleepTimerEnabled,
                        sleepTimerTimeLeft = sleepTimerTimeLeft,
                        onExpandQueue = { state.expandSoft() },
                        onSleepTimerClick = {
                            if (sleepTimerEnabled) {
                                playerConnection.service.sleepTimer.clear()
                            } else {
                                showSleepTimerDialog = true
                            }
                        },
                        onShowLyrics = onShowLyrics,
                        onMenuClick = {
                            menuState.show {
                                PlayerMenu(
                                    mediaMetadata = mediaMetadata,
                                    navController = navController,
                                    playerBottomSheetState = playerBottomSheetState,
                                    onShowDetailsDialog = {
                                        mediaMetadata?.id?.let {
                                            bottomSheetPageState.show {
                                                ShowMediaInfo(it)
                                            }
                                        }
                                    },
                                    onDismiss = menuState::dismiss
                                )
                            }
                        }
                    )
                }

                PlayerDesignStyle.V5 -> {
                    QueueCollapsedContentV3(
                        showCodecOnPlayer = showCodecOnPlayer,
                        currentFormat = currentFormat,
                        textBackgroundColor = TextBackgroundColor,
                        sleepTimerEnabled = sleepTimerEnabled,
                        sleepTimerTimeLeft = sleepTimerTimeLeft,
                        onExpandQueue = { state.expandSoft() },
                        onSleepTimerClick = {
                            if (sleepTimerEnabled) {
                                playerConnection.service.sleepTimer.clear()
                            } else {
                                showSleepTimerDialog = true
                            }
                        },
                        onShowLyrics = onShowLyrics,
                        onMenuClick = {
                            menuState.show {
                                PlayerMenu(
                                    mediaMetadata = mediaMetadata,
                                    navController = navController,
                                    playerBottomSheetState = playerBottomSheetState,
                                    onShowDetailsDialog = {
                                        mediaMetadata?.id?.let {
                                            bottomSheetPageState.show {
                                                ShowMediaInfo(it)
                                            }
                                        }
                                    },
                                    onDismiss = menuState::dismiss
                                )
                            }
                        }
                    )
                }

                PlayerDesignStyle.V4 -> {
                    QueueCollapsedContentV4(
                        showCodecOnPlayer = showCodecOnPlayer,
                        currentFormat = currentFormat,
                        textBackgroundColor = TextBackgroundColor,
                        textButtonColor = textButtonColor,
                        iconButtonColor = iconButtonColor,
                        sleepTimerEnabled = sleepTimerEnabled,
                        sleepTimerTimeLeft = sleepTimerTimeLeft,
                        mediaMetadata = mediaMetadata,
                        onExpandQueue = { state.expandSoft() },
                        onSleepTimerClick = {
                            if (sleepTimerEnabled) {
                                playerConnection.service.sleepTimer.clear()
                            } else {
                                showSleepTimerDialog = true
                            }
                        },
                        onShowLyrics = onShowLyrics
                    )
                }

                PlayerDesignStyle.V1 -> {
                    QueueCollapsedContentV1(
                        showCodecOnPlayer = showCodecOnPlayer,
                        currentFormat = currentFormat,
                        textBackgroundColor = TextBackgroundColor,
                        sleepTimerEnabled = sleepTimerEnabled,
                        sleepTimerTimeLeft = sleepTimerTimeLeft,
                        onExpandQueue = { state.expandSoft() },
                        onSleepTimerClick = {
                            if (sleepTimerEnabled) {
                                playerConnection.service.sleepTimer.clear()
                            } else {
                                showSleepTimerDialog = true
                            }
                        },
                        onShowLyrics = onShowLyrics
                    )
                }

                PlayerDesignStyle.V6 -> {
                    QueueCollapsedContentV4(
                        showCodecOnPlayer = showCodecOnPlayer,
                        currentFormat = currentFormat,
                        textBackgroundColor = TextBackgroundColor,
                        textButtonColor = textButtonColor,
                        iconButtonColor = iconButtonColor,
                        sleepTimerEnabled = sleepTimerEnabled,
                        sleepTimerTimeLeft = sleepTimerTimeLeft,
                        mediaMetadata = mediaMetadata,
                        onExpandQueue = { state.expandSoft() },
                        onSleepTimerClick = {
                            if (sleepTimerEnabled) {
                                playerConnection.service.sleepTimer.clear()
                            } else {
                                showSleepTimerDialog = true
                            }
                        },
                        onShowLyrics = onShowLyrics
                    )
                }

                PlayerDesignStyle.V7 -> {
                    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager }
                    val activeDevice = remember(audioManager) {
                        audioManager.getDevices(android.media.AudioManager.GET_DEVICES_OUTPUTS)
                            .firstOrNull { it.type == android.media.AudioDeviceInfo.TYPE_BLUETOOTH_A2DP || it.type == android.media.AudioDeviceInfo.TYPE_BLUETOOTH_SCO || it.type == android.media.AudioDeviceInfo.TYPE_BLE_HEADSET }
                            ?.productName?.toString() ?: "Speaker"
                    }
                    QueueCollapsedContentV7(
                        showCodecOnPlayer = showCodecOnPlayer,
                        currentFormat = currentFormat,
                        textBackgroundColor = TextBackgroundColor,
                        onExpandQueue = { state.expandSoft() },
                        onShowLyrics = onShowLyrics,
                        onDeviceClick = {
                            val intent = android.content.Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS)
                            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)
                        },
                        deviceName = activeDevice
                    )
                }
            }

            if (showSleepTimerDialog) {
                SleepTimerDialog(
                    onDismiss = { showSleepTimerDialog = false },
                    onConfirm = { minutes ->
                        showSleepTimerDialog = false
                        playerConnection.service.sleepTimer.start(minutes)
                    },
                    onEndOfSong = {
                        showSleepTimerDialog = false
                        playerConnection.service.sleepTimer.start(-1)
                    },
                    initialValue = sleepTimerValue
                )
            }
        },
    ) {
        val queueTitle by playerConnection.queueTitle.collectAsState()
        val queueWindows by playerConnection.queueWindows.collectAsState()
        val mutableQueueWindows = remember { mutableStateListOf<Timeline.Window>() }
        val queueLength by remember {
            derivedStateOf {
                queueWindows.sumOf { it.mediaItem.metadata?.duration ?: 0 }
            }
        }

        val coroutineScope = rememberCoroutineScope()



        val headerItems = 1
        val lazyListState = rememberLazyListState()
        var dragInfo by remember { mutableStateOf<Pair<Int, Int>?>(null) }

        var shouldScrollToCurrent by remember { mutableStateOf(false) }
        var lastScrolledUid by remember { mutableStateOf<Long?>(null) }

        val currentPlayingUid = remember(currentWindowIndex, queueWindows) {
            if (currentWindowIndex in queueWindows.indices) {
                queueWindows[currentWindowIndex].uid
            } else null
        }

        val reorderableState = rememberReorderableLazyListState(
            lazyListState = lazyListState,
            scrollThresholdPadding = WindowInsets.systemBars.add(
                WindowInsets(
                    top = ListItemHeight,
                    bottom = ListItemHeight
                )
            ).asPaddingValues()
        ) { from, to ->
            val currentDragInfo = dragInfo
            dragInfo = if (currentDragInfo == null) {
                from.index to to.index
            } else {
                currentDragInfo.first to to.index
            }

            val safeFrom = (from.index - headerItems).coerceIn(0, mutableQueueWindows.lastIndex)
            val safeTo = (to.index - headerItems).coerceIn(0, mutableQueueWindows.lastIndex)

            mutableQueueWindows.move(safeFrom, safeTo)

            if (selection && currentWindowIndex in mutableQueueWindows.indices) {
                val draggedItemUid = mutableQueueWindows[if (to.index > from.index) safeTo else safeFrom].uid
                val currentItem = queueWindows.getOrNull(currentWindowIndex)

                if (currentItem?.uid == draggedItemUid) {
                    val newIndex = mutableQueueWindows.indexOfFirst { it.uid == draggedItemUid }
                    if (newIndex != -1) {
                        selectedSongs.clear()
                        selectedItems.clear()
                        mutableQueueWindows.getOrNull(newIndex)?.let { window ->
                            window.mediaItem.metadata?.let { metadata ->
                                selectedSongs.add(metadata)
                                selectedItems.add(window)
                            }
                        }
                    }
                }
            }
        }

        LaunchedEffect(mutableQueueWindows) {
            if (mutableQueueWindows.isNotEmpty() && !shouldScrollToCurrent) {
                shouldScrollToCurrent = true
            }
        }

        LaunchedEffect(currentPlayingUid, shouldScrollToCurrent) {
            if (currentPlayingUid != null && shouldScrollToCurrent) {
                val indexInMutableList = mutableQueueWindows.indexOfFirst { it.uid == currentPlayingUid }
                if (indexInMutableList != -1) {
                    lazyListState.scrollToItem(indexInMutableList + 1)
                }
            }
        }

        LaunchedEffect(reorderableState.isAnyItemDragging) {
            if (!reorderableState.isAnyItemDragging) {
                dragInfo?.let { (from, to) ->
                    val safeFrom = (from - headerItems).coerceIn(0, queueWindows.lastIndex)
                    val safeTo = (to - headerItems).coerceIn(0, queueWindows.lastIndex)

                    if (!playerConnection.player.shuffleModeEnabled) {
                        playerConnection.player.moveMediaItem(safeFrom, safeTo)
                    } else {
                        playerConnection.player.setShuffleOrder(
                            DefaultShuffleOrder(
                                queueWindows.map { it.firstPeriodIndex }
                                    .toMutableList()
                                    .move(safeFrom, safeTo)
                                    .toIntArray(),
                                System.currentTimeMillis()
                            )
                        )
                    }
                    dragInfo = null
                }
            }
        }

        LaunchedEffect(queueWindows) {
            mutableQueueWindows.apply {
                clear()
                addAll(queueWindows)
            }
        }

        LaunchedEffect(state.isCollapsed) {
            if (!state.isCollapsed && currentPlayingUid != null) {
                val indexInMutableList = mutableQueueWindows.indexOfFirst { it.uid == currentPlayingUid }
                if (indexInMutableList != -1) {
                    // Scroll to the item + headerItems (Spacer)
                    // The Spacer is at index 0, so the first song is at index 1.
                    // If indexInMutableList is 0 (first song), we want to scroll to index 1.
                    lazyListState.scrollToItem(indexInMutableList + 1)
                }
            }
        }

        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(backgroundColor),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                CurrentSongHeader(
                    sheetState = state,
                    mediaMetadata = mediaMetadata,
                    isPlaying = isPlaying,
                    repeatMode = repeatMode,
                    shuffleModeEnabled = playerConnection.player.shuffleModeEnabled,
                    locked = effectiveLocked,
                    songCount = queueWindows.size,
                    queueDuration = queueLength,
                    infiniteQueueEnabled = infiniteQueueEnabled,
                    backgroundColor = backgroundColor,
                    onBackgroundColor = onBackgroundColor,
                    onToggleLike = {
                        playerConnection.service.toggleLike()
                    },
                    onMenuClick = {
                        menuState.show {
                            PlayerMenu(
                                mediaMetadata = mediaMetadata,
                                navController = navController,
                                playerBottomSheetState = playerBottomSheetState,
                                onShowDetailsDialog = {
                                    mediaMetadata?.id?.let {
                                        bottomSheetPageState.show {
                                            ShowMediaInfo(it)
                                        }
                                    }
                                },
                                onDismiss = menuState::dismiss
                            )
                        }
                    },
                    onRepeatClick = { playerConnection.player.toggleRepeatMode() },
                    onShuffleClick = {
                        coroutineScope.launch(Dispatchers.Main) {
                            playerConnection.player.shuffleModeEnabled = !playerConnection.player.shuffleModeEnabled
                        }
                    },
                    onLockClick = {
                        if (togetherForcesLock) {
                            Toast.makeText(context, R.string.not_allowed, Toast.LENGTH_SHORT).show()
                        } else {
                            locked = !locked
                        }
                    },
                    onInfiniteQueueClick = {
                        val nextInfiniteQueueEnabled = !infiniteQueueEnabled
                        infiniteQueueEnabled = nextInfiniteQueueEnabled
                        if (nextInfiniteQueueEnabled) {
                            playerConnection.service.onInfiniteQueueEnabled()
                        } else {
                            playerConnection.service.onInfiniteQueueDisabled()
                        }
                    }
                )

                LazyColumn(
                    state = lazyListState,
                    contentPadding =
                        WindowInsets.systemBars
                            .only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)
                            .add(
                                WindowInsets(
                                    bottom = ListItemHeight + 8.dp,
                                ),
                            ).asPaddingValues(),
                    modifier = Modifier
                        .weight(1f)
                        .nestedScroll(state.preUpPostDownNestedScrollConnection)
                ) {

                    item {
                        Spacer(
                            modifier =
                                Modifier
                                    .animateContentSize()
                                    .height(if (selection) 48.dp else 0.dp),
                        )
                    }

                    itemsIndexed(
                        items = mutableQueueWindows,
                        key = { _, item -> item.uid.hashCode() },
                    ) { index, window ->
                        ReorderableItem(
                            state = reorderableState,
                            key = window.uid.hashCode(),
                        ) {
                            val currentItem by rememberUpdatedState(window)
                            val isActive = window.uid == currentPlayingUid
                            val dismissBoxState =
                                rememberSwipeToDismissBoxState(
                                    positionalThreshold = { totalDistance -> totalDistance }
                                )

                            var processedDismiss by remember { mutableStateOf(false) }
                            LaunchedEffect(dismissBoxState.currentValue) {
                                val dv = dismissBoxState.currentValue
                                if (!processedDismiss && (
                                            dv == SwipeToDismissBoxValue.StartToEnd ||
                                                    dv == SwipeToDismissBoxValue.EndToStart
                                            )
                                ) {
                                    processedDismiss = true
                                    playerConnection.player.removeMediaItem(currentItem.firstPeriodIndex)
                                    dismissJob?.cancel()
                                    dismissJob = coroutineScope.launch {
                                        val snackbarResult = snackbarHostState.showSnackbar(
                                            message = context.getString(
                                                R.string.removed_song_from_playlist,
                                                currentItem.mediaItem.metadata?.title,
                                            ),
                                            actionLabel = context.getString(R.string.undo),
                                            duration = SnackbarDuration.Short,
                                        )
                                        if (snackbarResult == SnackbarResult.ActionPerformed) {
                                            playerConnection.player.addMediaItem(currentItem.mediaItem)
                                            playerConnection.player.moveMediaItem(
                                                mutableQueueWindows.size,
                                                currentItem.firstPeriodIndex,
                                            )
                                        }
                                    }
                                }
                                if (dv == SwipeToDismissBoxValue.Settled) {
                                    processedDismiss = false
                                }
                            }

                            val content: @Composable () -> Unit = {
                                Row(
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.graphicsLayer {
                                        // Enable hardware acceleration for smoother dragging
                                        compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen
                                    }
                                ) {
                                    val shouldLoadImages by remember {
                                        derivedStateOf {
                                            state.value > state.collapsedBound + 80.dp
                                        }
                                    }

                                    val trackMetadata = window.mediaItem.metadata ?: return@Row
                                    MediaMetadataListItem(
                                        mediaMetadata = trackMetadata,
                                        isSelected = selection && trackMetadata in selectedSongs,
                                        isActive = isActive,
                                        isPlaying = isPlaying && isActive,
                                        shouldLoadImage = shouldLoadImages,
                                        trailingContent = {
                                            IconButton(
                                                onClick = {
                                                    menuState.show {
                                                        PlayerMenu(
                                                            mediaMetadata = trackMetadata,
                                                            navController = navController,
                                                            playerBottomSheetState = playerBottomSheetState,
                                                            isQueueTrigger = true,
                                                            onShowDetailsDialog = {
                                                                window.mediaItem.mediaId.let {
                                                                    bottomSheetPageState.show {
                                                                        ShowMediaInfo(it)
                                                                    }
                                                                }
                                                            },
                                                            onDismiss = menuState::dismiss,
                                                        )
                                                    }
                                                },
                                            ) {
                                                Icon(
                                                    painter = painterResource(R.drawable.more_vert),
                                                    contentDescription = null,
                                                )
                                            }
                                            if (!effectiveLocked) {
                                                IconButton(
                                                    onClick = { },
                                                    modifier = Modifier
                                                        .draggableHandle()
                                                        .graphicsLayer {
                                                            // Improve touch response
                                                            alpha = 0.99f
                                                        }
                                                ) {
                                                    Icon(
                                                        painter = painterResource(R.drawable.drag_handle),
                                                        contentDescription = null,
                                                    )
                                                }
                                            }
                                        },
                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .background(backgroundColor)
                                                .combinedClickable(
                                                    onClick = {
                                                        if (selection) {
                                                            if (trackMetadata in selectedSongs) {
                                                                selectedSongs.remove(trackMetadata)
                                                                selectedItems.remove(currentItem)
                                                            } else {
                                                                selectedSongs.add(trackMetadata)
                                                                selectedItems.add(currentItem)
                                                            }
                                                        } else {
                                                            if (index == currentWindowIndex) {
                                                                playerConnection.player.togglePlayPause()
                                                            } else {
                                                                val joined =
                                                                    togetherSessionState as? com.arturo254.opentune.together.TogetherSessionState.Joined
                                                                val isGuest = joined?.role is com.arturo254.opentune.together.TogetherRole.Guest
                                                                if (isGuest) {
                                                                    if (joined?.roomState?.settings?.allowGuestsToControlPlayback != true) {
                                                                        Toast.makeText(context, R.string.not_allowed, Toast.LENGTH_SHORT).show()
                                                                        return@combinedClickable
                                                                    }
                                                                    val trackId =
                                                                        window.mediaItem.metadata?.id?.trim().orEmpty().ifBlank {
                                                                            window.mediaItem.mediaId.trim()
                                                                        }
                                                                    if (trackId.isBlank()) return@combinedClickable
                                                                    Toast.makeText(context, R.string.together_requesting_song_change, Toast.LENGTH_SHORT).show()
                                                                    playerConnection.service.requestTogetherControl(
                                                                        com.arturo254.opentune.together.ControlAction.SeekToTrack(
                                                                            trackId = trackId,
                                                                            positionMs = 0L,
                                                                        ),
                                                                    )
                                                                    shouldScrollToCurrent = false
                                                                } else {
                                                                    playerConnection.player.seekToDefaultPosition(
                                                                        window.firstPeriodIndex,
                                                                    )
                                                                    playerConnection.player.playWhenReady = true
                                                                    shouldScrollToCurrent = false
                                                                }
                                                            }
                                                        }
                                                    },
                                                    onLongClick = {
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        if (!selection) {
                                                            selection = true
                                                        }
                                                        selectedSongs.clear() // Clear all selections
                                                        selectedSongs.add(trackMetadata) // Select current item
                                                    },
                                                ),
                                    )
                                }
                            }

                            if (effectiveLocked) {
                                content()
                            } else {
                                SwipeToDismissBox(
                                    state = dismissBoxState,
                                    backgroundContent = {},
                                ) {
                                    content()
                                }
                            }
                        }
                    }


                }
            }

            // Old header hidden - now using sticky header at top of queue
            Column(
                modifier =
                    Modifier
                        .height(0.dp)
            ) {

                AnimatedVisibility(
                    visible = selection,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                ) {
                    Row(
                        modifier =
                            Modifier
                                .height(48.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val count = selectedSongs.size
                        IconButton(
                            onClick = {
                                selection = false
                            },
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.close),
                                contentDescription = null,
                            )
                        }
                        Text(
                            text = stringResource(R.string.elements_selected, count),
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                if (count == mutableQueueWindows.size) {
                                    selectedSongs.clear()
                                    selectedItems.clear()
                                } else {
                                    queueWindows
                                        .filter { it.mediaItem.metadata!! !in selectedSongs }
                                        .forEach {
                                            selectedSongs.add(it.mediaItem.metadata!!)
                                            selectedItems.add(it)
                                        }
                                }
                            },
                        ) {
                            Icon(
                                painter =
                                    painterResource(
                                        if (count == mutableQueueWindows.size) {
                                            R.drawable.deselect
                                        } else {
                                            R.drawable.select_all
                                        },
                                    ),
                                contentDescription = null,
                            )
                        }

                        IconButton(
                            onClick = {
                                menuState.show {
                                    SelectionMediaMetadataMenu(
                                        songSelection = selectedSongs,
                                        onDismiss = menuState::dismiss,
                                        clearAction = {
                                            selectedSongs.clear()
                                            selectedItems.clear()
                                        },
                                        currentItems = selectedItems,
                                    )
                                }
                            },
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.more_vert),
                                contentDescription = null,
                                tint = LocalContentColor.current,
                            )
                        }
                    }
                }
                if (pureBlack) {
                    HorizontalDivider()
                }
            }

            val shuffleModeEnabled by playerConnection.shuffleModeEnabled.collectAsState()

            // Bottom bar hidden - controls now in sticky header
            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier =
                        Modifier
                            .height(0.dp)
                            .align(Alignment.BottomCenter)
                ) {
                    IconButton(
                        modifier = Modifier.align(Alignment.CenterStart),
                        onClick = {
                            coroutineScope
                                .launch {
                                    lazyListState.animateScrollToItem(
                                        if (playerConnection.player.shuffleModeEnabled) playerConnection.player.currentMediaItemIndex else 0,
                                    )
                                }.invokeOnCompletion {
                                    playerConnection.player.shuffleModeEnabled =
                                        !playerConnection.player.shuffleModeEnabled
                                }
                        },
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.shuffle),
                            contentDescription = null,
                            modifier = Modifier.alpha(if (shuffleModeEnabled) 1f else 0.5f),
                        )
                    }

                    Icon(
                        painter = painterResource(R.drawable.expand_more),
                        contentDescription = null,
                        modifier = Modifier.align(Alignment.Center),
                    )

                    IconButton(
                        modifier = Modifier.align(Alignment.CenterEnd),
                        onClick = playerConnection.player::toggleRepeatMode,
                    ) {
                        Icon(
                            painter =
                                painterResource(
                                    when (repeatMode) {
                                        Player.REPEAT_MODE_OFF, Player.REPEAT_MODE_ALL -> R.drawable.repeat
                                        Player.REPEAT_MODE_ONE -> R.drawable.repeat_one
                                        else -> throw IllegalStateException()
                                    },
                                ),
                            contentDescription = null,
                            modifier = Modifier.alpha(if (repeatMode == Player.REPEAT_MODE_OFF) 0.5f else 1f),
                        )
                    }
                }

                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier =
                        Modifier
                            .padding(
                                bottom =
                                    ListItemHeight +
                                            WindowInsets.systemBars
                                                .asPaddingValues()
                                                .calculateBottomPadding(),
                            )
                            .align(Alignment.BottomCenter),
                )
            }
        }
    }
}