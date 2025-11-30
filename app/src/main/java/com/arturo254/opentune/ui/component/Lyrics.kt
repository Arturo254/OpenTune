package com.arturo254.opentune.ui.component

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.Player.STATE_ENDED
import coil.compose.AsyncImage
import com.arturo254.opentune.LocalDatabase
import com.arturo254.opentune.LocalPlayerConnection
import com.arturo254.opentune.R
import com.arturo254.opentune.constants.AnimateLyricsKey
import com.arturo254.opentune.constants.DarkModeKey
import com.arturo254.opentune.constants.LyricsClickKey
import com.arturo254.opentune.constants.LyricsScrollKey
import com.arturo254.opentune.constants.LyricsTextPositionKey
import com.arturo254.opentune.constants.PlayerBackgroundStyle
import com.arturo254.opentune.constants.PlayerBackgroundStyleKey
import com.arturo254.opentune.constants.RotateBackgroundKey
import com.arturo254.opentune.constants.ShowLyricsKey
import com.arturo254.opentune.constants.SliderStyle
import com.arturo254.opentune.constants.SliderStyleKey
import com.arturo254.opentune.db.entities.LyricsEntity
import com.arturo254.opentune.db.entities.LyricsEntity.Companion.LYRICS_NOT_FOUND
import com.arturo254.opentune.extensions.togglePlayPause
import com.arturo254.opentune.extensions.toggleRepeatMode
import com.arturo254.opentune.lyrics.LyricsEntry
import com.arturo254.opentune.lyrics.LyricsUtils.findCurrentLineIndex
import com.arturo254.opentune.lyrics.LyricsUtils.parseLyrics
import com.arturo254.opentune.ui.component.shimmer.ShimmerHost
import com.arturo254.opentune.ui.component.shimmer.TextPlaceholder
import com.arturo254.opentune.ui.menu.LyricsMenu
import com.arturo254.opentune.ui.screens.settings.DarkMode
import com.arturo254.opentune.ui.screens.settings.LyricsPosition
import com.arturo254.opentune.ui.utils.fadingEdge
import com.arturo254.opentune.utils.makeTimeString
import com.arturo254.opentune.utils.rememberEnumPreference
import com.arturo254.opentune.utils.rememberPreference
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.saket.squiggles.SquigglySlider
import kotlin.time.Duration.Companion.seconds

@RequiresApi(Build.VERSION_CODES.M)
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@SuppressLint("UnusedBoxWithConstraintsScope", "StringFormatInvalid")
@Composable
fun Lyrics(
    sliderPositionProvider: () -> Long?,
    onNavigateBack: (() -> Unit)? = null,
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val menuState = LocalMenuState.current
    val density = LocalDensity.current
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val scope = rememberCoroutineScope()
    val database = LocalDatabase.current

    val isFullscreen = onNavigateBack != null
    val sliderStyle by rememberEnumPreference(SliderStyleKey, SliderStyle.DEFAULT)
    val landscapeOffset = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val lyricsTextPosition by rememberEnumPreference(LyricsTextPositionKey, LyricsPosition.CENTER)
    val changeLyrics by rememberPreference(LyricsClickKey, true)
    val scrollLyrics by rememberPreference(LyricsScrollKey, true)
    val animateLyrics by rememberPreference(AnimateLyricsKey, true)

    var showLyrics by rememberPreference(ShowLyricsKey, defaultValue = false)


    val infiniteTransition = rememberInfiniteTransition(label = "backgroundRotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 20000, // 20 seconds per complete rotation
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val rotateBackground by rememberPreference(RotateBackgroundKey, defaultValue = false)

    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val currentSongId = mediaMetadata?.id

    // States that reset when the song changes
    var currentLineIndex by remember { mutableIntStateOf(-1) }
    var deferredCurrentLineIndex by remember(currentSongId) { mutableIntStateOf(0) }
    var previousLineIndex by remember(currentSongId) { mutableIntStateOf(0) }
    var lastPreviewTime by remember(currentSongId) { mutableLongStateOf(0L) }
    var isSeeking by remember { mutableStateOf(false) }
    var initialScrollDone by remember(currentSongId) { mutableStateOf(false) }
    var shouldScrollToFirstLine by remember(currentSongId) { mutableStateOf(true) }
    var isAppMinimized by rememberSaveable { mutableStateOf(false) }
    var sliderPosition by remember { mutableStateOf<Long?>(null) }
    var showImageOverlay by remember { mutableStateOf(false) }
    var showControls by remember { mutableStateOf(true) }
    var cornerRadius by remember { mutableFloatStateOf(16f) }

    // Improved selection system
    var isSelectionModeActive by remember(currentSongId) { mutableStateOf(false) }
    val selectedIndices = remember(currentSongId) { mutableStateListOf<Int>() }
    var showMaxSelectionToast by remember { mutableStateOf(false) }

    // States for sharing
    var showProgressDialog by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }
    var shareDialogData by remember { mutableStateOf<Triple<String, String, String>?>(null) }

    val lazyListState = rememberLazyListState()
    var isAnimating by remember { mutableStateOf(false) }
    val maxSelectionLimit = 5

    // Optimized cache system
    var lyricsCache by remember { mutableStateOf<Map<String, LyricsEntity>>(emptyMap()) }
    var currentLyricsEntity by remember(currentSongId) {
        mutableStateOf<LyricsEntity?>(lyricsCache[currentSongId])
    }
    var isLoadingLyrics by remember(currentSongId) { mutableStateOf(false) }


    val lyricsEntity by playerConnection.currentLyrics.collectAsState(initial = null)
    val lyrics = remember(lyricsEntity) { lyricsEntity?.lyrics?.trim() }

    val playbackState by playerConnection.playbackState.collectAsState()
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val repeatMode by playerConnection.repeatMode.collectAsState()
    val canSkipPrevious by playerConnection.canSkipPrevious.collectAsState()
    val canSkipNext by playerConnection.canSkipNext.collectAsState()
    val currentSong by playerConnection.currentSong.collectAsState(initial = null)

    val playerBackground by rememberEnumPreference(
        key = PlayerBackgroundStyleKey,
        defaultValue = PlayerBackgroundStyle.DEFAULT
    )

    val darkTheme by rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.AUTO)
    val isSystemInDarkTheme = isSystemInDarkTheme()
    val useDarkTheme = remember(darkTheme, isSystemInDarkTheme) {
        if (darkTheme == DarkMode.AUTO) isSystemInDarkTheme else darkTheme == DarkMode.ON
    }

    LaunchedEffect(currentSongId) {
        currentSongId?.let { songId ->
            if (lyricsCache.containsKey(songId)) {
                currentLyricsEntity = lyricsCache[songId]
                return@LaunchedEffect
            }

            isLoadingLyrics = true

            withContext(Dispatchers.IO) {
                try {
                    // First, attempt to load from the database.
                    val existingLyrics = try {
                        database.getLyrics(songId)
                    } catch (e: Throwable) {
                        null
                    }

                    if (existingLyrics != null) {
                        val newCache = lyricsCache.toMutableMap().apply {
                            put(songId, existingLyrics)
                        }
                        lyricsCache = newCache
                        currentLyricsEntity = existingLyrics
                    } else {
                        // If not in the database, try to obtain from the API using LyricsHelper.
                        try {
                            val entryPoint = EntryPointAccessors.fromApplication(
                                context.applicationContext,
                                com.arturo254.opentune.di.LyricsHelperEntryPoint::class.java
                            )
                            val lyricsHelper = entryPoint.lyricsHelper()
                            val fetchedLyrics: String? = mediaMetadata?.let { lyricsHelper.getLyrics(it) }

                            val entity = if (!fetchedLyrics.isNullOrBlank()) {
                                LyricsEntity(songId, fetchedLyrics)
                            } else {
                                LyricsEntity(songId, LYRICS_NOT_FOUND)
                            }

                            // Save/upsert to database for cache
                            try {
                                database.query {
                                    upsert(entity)
                                }
                            } catch (e: Throwable) {
                                // If the upsert fails, do not block the flow: continue with temporary cache
                            }

                            val newCache = lyricsCache.toMutableMap().apply {
                                put(songId, entity)
                            }
                            lyricsCache = newCache
                            currentLyricsEntity = entity
                        } catch (e: Throwable) {
                            // If retrieval via network/entry point fails, save “not found” marker.
                            val errorEntity = LyricsEntity(songId, LYRICS_NOT_FOUND)
                            val newCache = lyricsCache.toMutableMap().apply {
                                put(songId, errorEntity)
                            }
                            lyricsCache = newCache
                            currentLyricsEntity = errorEntity
                        }
                    }
                } catch (e: Exception) {
                    val errorEntity = LyricsEntity(songId, LYRICS_NOT_FOUND)
                    val newCache = lyricsCache.toMutableMap().apply {
                        put(songId, errorEntity)
                    }
                    lyricsCache = newCache
                    currentLyricsEntity = errorEntity
                } finally {
                    isLoadingLyrics = false
                }
            }
        }
    }

// REPLACE your remember(lyrics, currentSongId) function with:
    val lines = remember(lyrics, scope) {
        if (lyrics == null || lyrics == LYRICS_NOT_FOUND) {
            emptyList()
        } else if (lyrics.startsWith("[")) {
            val parsedLines = parseLyrics(lyrics)
            listOf(LyricsEntry.HEAD_LYRICS_ENTRY) + parsedLines
        } else {
            lyrics.lines().mapIndexed { index, line ->
                LyricsEntry(index * 100L, line)
            }
        }
    }




    // ADD this LaunchedEffect right after defining 'lines'
    LaunchedEffect(lines) {
        isSelectionModeActive = false
        selectedIndices.clear()
        currentLineIndex = -1
        deferredCurrentLineIndex = 0
        previousLineIndex = 0
        initialScrollDone = false
        shouldScrollToFirstLine = true
    }

    val isSynced = remember(lyrics) {
        !lyrics.isNullOrEmpty() && lyrics.startsWith("[")
    }

    val textColor = when (playerBackground) {
        PlayerBackgroundStyle.DEFAULT -> MaterialTheme.colorScheme.secondary
        else ->
            if (useDarkTheme)
                MaterialTheme.colorScheme.onSurface
            else
                MaterialTheme.colorScheme.onPrimary
    }

    var position by rememberSaveable(playbackState) { mutableLongStateOf(playerConnection.player.currentPosition) }
    var duration by rememberSaveable(playbackState) { mutableLongStateOf(playerConnection.player.duration) }


    // intelligent BackHandler
    BackHandler(enabled = isSelectionModeActive || isFullscreen) {
        when {
            isSelectionModeActive -> {
                isSelectionModeActive = false
                selectedIndices.clear()
            }

            isFullscreen -> onNavigateBack?.invoke()
        }
    }

    LaunchedEffect(Unit) {
        if (isFullscreen) {
            cornerRadius = 16f // Use fixed value instead of AppConfig
        }
    }

    LaunchedEffect(playbackState) {
        if (isFullscreen && playbackState == Player.STATE_READY) {
            while (isActive) {
                delay(100)
                position = playerConnection.player.currentPosition
                duration = playerConnection.player.duration
            }
        }
    }

    val imageScale by animateFloatAsState(
        targetValue = if (showImageOverlay) 1f else 0.95f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "imageScale"
    )

    LaunchedEffect(showMaxSelectionToast) {
        if (showMaxSelectionToast) {
            Toast.makeText(
                context,
                context.getString(R.string.max_selection_limit, maxSelectionLimit),
                Toast.LENGTH_SHORT
            ).show()
            showMaxSelectionToast = false
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                val visibleItemsInfo = lazyListState.layoutInfo.visibleItemsInfo
                val isCurrentLineVisible = visibleItemsInfo.any { it.index == currentLineIndex }
                if (isCurrentLineVisible) {
                    initialScrollDone = false
                }
                isAppMinimized = true
            } else if (event == Lifecycle.Event.ON_START) {
                isAppMinimized = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(lyrics) {
        if (lyrics.isNullOrEmpty() || !lyrics.startsWith("[")) {
            currentLineIndex = -1
            return@LaunchedEffect
        }
        while (isActive) {
            delay(50)
            val sliderPos = sliderPositionProvider()
            isSeeking = sliderPos != null
            currentLineIndex = findCurrentLineIndex(
                lines,
                sliderPos ?: playerConnection.player.currentPosition
            )
        }
    }

    LaunchedEffect(isSeeking, lastPreviewTime) {
        if (isSeeking) {
            lastPreviewTime = 0L
        } else if (lastPreviewTime != 0L) {
            delay(if (isFullscreen) 2.seconds else LyricsPreviewTime)
            lastPreviewTime = 0L
        }
    }


    // REPLACE your LaunchedEffect(currentLineIndex...) with:
    LaunchedEffect(currentLineIndex, lastPreviewTime, initialScrollDone) {
        if (!isSynced) return@LaunchedEffect

        suspend fun performSmoothPageScroll(targetIndex: Int, duration: Int = 1500) {
            if (isAnimating) return
            isAnimating = true

            try {
                val itemInfo = lazyListState.layoutInfo.visibleItemsInfo
                    .firstOrNull { it.index == targetIndex }
                if (itemInfo != null) {
                    val viewportHeight = lazyListState.layoutInfo.viewportEndOffset -
                            lazyListState.layoutInfo.viewportStartOffset
                    val center = lazyListState.layoutInfo.viewportStartOffset + (viewportHeight / 2)
                    val itemCenter = itemInfo.offset + itemInfo.size / 2
                    val offset = itemCenter - center

                    if (kotlin.math.abs(offset) > 10) {
                        lazyListState.animateScrollBy(
                            value = offset.toFloat(),
                            animationSpec = tween(durationMillis = duration)
                        )
                    }
                } else {
                    lazyListState.scrollToItem(targetIndex)
                }
            } finally {
                isAnimating = false
            }
        }

        if ((currentLineIndex == 0 && shouldScrollToFirstLine) || !initialScrollDone) {
            shouldScrollToFirstLine = false
            val initialCenterIndex = kotlin.math.max(0, currentLineIndex)
            performSmoothPageScroll(initialCenterIndex, 800)
            if (!isAppMinimized) {
                initialScrollDone = true
            }
        } else if (currentLineIndex != -1) {
            deferredCurrentLineIndex = currentLineIndex
            if (isSeeking) {
                val seekCenterIndex = kotlin.math.max(0, currentLineIndex - 1)
                performSmoothPageScroll(seekCenterIndex, 500)
            } else if ((lastPreviewTime == 0L || currentLineIndex != previousLineIndex) && scrollLyrics) {
                if (currentLineIndex != previousLineIndex) {
                    performSmoothPageScroll(currentLineIndex, 1500)
                }
            }
        }
        if (currentLineIndex > 0) {
            shouldScrollToFirstLine = true
        }
        previousLineIndex = currentLineIndex
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (isFullscreen) MaterialTheme.colorScheme.background else Color.Transparent)
            .then(
                if (isFullscreen) {
                    Modifier.pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                if (!isSelectionModeActive) {
                                    showControls = !showControls
                                }
                            }
                        )
                    }
                } else Modifier
            )
    ) {
        if (isFullscreen) {
            mediaMetadata?.let { metadata ->
                if (playerBackground == PlayerBackgroundStyle.BLUR) {
                    AsyncImage(
                        model = metadata.thumbnailUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .blur(50.dp) // Blur BEFORE the graphicsLayer
                            .graphicsLayer {
                                // Much more aggressive scaling to compensate for blur and rotation
                                scaleX = 2.5f
                                scaleY = 2.5f
                                if (rotateBackground) {
                                    rotationZ = rotation
                                    transformOrigin = TransformOrigin.Center
                                }
                            }
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.7f))
                    )
                }
            }

            // Header with controls
            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { -it },
                exit = fadeOut(tween(300)) + slideOutVertically(tween(300)) { -it },
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .zIndex(2f)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 1f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { onNavigateBack?.invoke() },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = Color.Transparent
                            )
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.close),
                                contentDescription = stringResource(R.string.back),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        mediaMetadata?.let { metadata ->
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = metadata.title,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = metadata.artists.joinToString { it.name },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Button to clear screen
                        IconButton(
                            onClick = { showControls = !showControls },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = Color.Transparent
                            )
                        ) {
                            Icon(
                                painter = painterResource(
                                    if (showControls) R.drawable.visibility_off else R.drawable.visibility
                                ),
                                contentDescription = if (showControls)
                                    stringResource(R.string.hide_controls)
                                else
                                    stringResource(R.string.show_controls),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        if (isSelectionModeActive) {
                            IconButton(
                                onClick = {
                                    if (selectedIndices.isNotEmpty()) {
                                        val sortedIndices = selectedIndices.sorted()
                                        val selectedLyricsText = sortedIndices
                                            .mapNotNull { lines.getOrNull(it)?.text }
                                            .joinToString("\n")

                                        if (selectedLyricsText.isNotBlank()) {
                                            shareDialogData = Triple(
                                                selectedLyricsText,
                                                mediaMetadata?.title ?: "",
                                                mediaMetadata?.artists?.joinToString { it.name }
                                                    ?: ""
                                            )
                                            showShareDialog = true
                                        }
                                        isSelectionModeActive = false
                                        selectedIndices.clear()
                                    }
                                },
                                enabled = selectedIndices.isNotEmpty(),
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = Color.Transparent
                                )
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.media3_icon_share),
                                    contentDescription = stringResource(R.string.share_selected),
                                    tint = if (selectedIndices.isNotEmpty())
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                        } else {
                            IconButton(
                                onClick = {
                                    mediaMetadata?.let { metadata ->
                                        menuState.show {
                                            LyricsMenu(
                                                lyricsProvider = { currentLyricsEntity },
                                                mediaMetadataProvider = { metadata },
                                                onDismiss = menuState::dismiss
                                            )
                                        }
                                    }
                                },
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = Color.Transparent
                                )
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.more_horiz),
                                    contentDescription = stringResource(R.string.more_options),
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // Playback controls
            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { it },
                exit = fadeOut(tween(300)) + slideOutVertically(tween(300)) { it },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .zIndex(2f)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(WindowInsets.systemBars.asPaddingValues())
                        .padding(16.dp)
                        .clip(RoundedCornerShape(20.dp)),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    tonalElevation = 6.dp,
                    shadowElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        when (sliderStyle) {
                            SliderStyle.DEFAULT -> {
                                Slider(
                                    value = (sliderPosition ?: position).toFloat(),
                                    valueRange = 0f..(if (duration == C.TIME_UNSET) 0f else duration.toFloat()),
                                    onValueChange = { sliderPosition = it.toLong() },
                                    onValueChangeFinished = {
                                        sliderPosition?.let {
                                            playerConnection.player.seekTo(it)
                                            position = it
                                        }
                                        sliderPosition = null
                                    },
                                    colors = SliderDefaults.colors(
                                        activeTrackColor = MaterialTheme.colorScheme.primary,
                                        inactiveTrackColor = MaterialTheme.colorScheme.outline.copy(
                                            alpha = 0.3f
                                        ),
                                        thumbColor = MaterialTheme.colorScheme.primary
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            SliderStyle.SQUIGGLY -> {
                                SquigglySlider(
                                    value = (sliderPosition ?: position).toFloat(),
                                    valueRange = 0f..(if (duration == C.TIME_UNSET) 0f else duration.toFloat()),
                                    onValueChange = { sliderPosition = it.toLong() },
                                    onValueChangeFinished = {
                                        sliderPosition?.let {
                                            playerConnection.player.seekTo(it)
                                            position = it
                                        }
                                        sliderPosition = null
                                    },
                                    colors = SliderDefaults.colors(
                                        activeTrackColor = MaterialTheme.colorScheme.primary,
                                        inactiveTrackColor = MaterialTheme.colorScheme.outline.copy(
                                            alpha = 0.3f
                                        ),
                                        thumbColor = MaterialTheme.colorScheme.primary
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
                                    squigglesSpec = SquigglySlider.SquigglesSpec(
                                        amplitude = if (isPlaying) 2.dp else 0.dp,
                                        strokeWidth = 3.dp
                                    )
                                )
                            }

                            SliderStyle.SLIM -> {
                                Slider(
                                    value = (sliderPosition ?: position).toFloat(),
                                    valueRange = 0f..(if (duration == C.TIME_UNSET) 0f else duration.toFloat()),
                                    onValueChange = { sliderPosition = it.toLong() },
                                    onValueChangeFinished = {
                                        sliderPosition?.let {
                                            playerConnection.player.seekTo(it)
                                            position = it
                                        }
                                        sliderPosition = null
                                    },
                                    thumb = { Spacer(modifier = Modifier.size(0.dp)) },
                                    track = { sliderState ->
                                        PlayerSliderTrack(
                                            sliderState = sliderState,
                                            colors = SliderDefaults.colors(
                                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                                inactiveTrackColor = MaterialTheme.colorScheme.outline.copy(
                                                    alpha = 0.3f
                                                )
                                            )
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp)
                        ) {
                            Text(
                                text = makeTimeString(sliderPosition ?: position),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Text(
                                text = if (duration != C.TIME_UNSET) makeTimeString(duration) else "",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(Modifier.height(16.dp))

                        Row(
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            IconButton(
                                onClick = { playerConnection.player.toggleRepeatMode() },
                                modifier = Modifier
                                    .size(48.dp)
                                    .alpha(if (repeatMode == Player.REPEAT_MODE_OFF) 0.5f else 1f)
                            ) {
                                Icon(
                                    painter = painterResource(
                                        when (repeatMode) {
                                            Player.REPEAT_MODE_OFF, Player.REPEAT_MODE_ALL -> R.drawable.repeat
                                            Player.REPEAT_MODE_ONE -> R.drawable.repeat_one
                                            else -> R.drawable.repeat
                                        }
                                    ),
                                    contentDescription = "Repeat",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            IconButton(
                                onClick = { playerConnection.seekToPrevious() },
                                enabled = canSkipPrevious,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.skip_previous),
                                    contentDescription = "Previous",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            FilledTonalIconButton(
                                onClick = {
                                    if (playbackState == STATE_ENDED) {
                                        playerConnection.player.seekTo(0, 0)
                                        playerConnection.player.playWhenReady = true
                                    } else {
                                        playerConnection.player.togglePlayPause()
                                    }
                                },
                                modifier = Modifier.size(64.dp)
                            ) {
                                Icon(
                                    painter = painterResource(
                                        when {
                                            playbackState == STATE_ENDED -> R.drawable.replay
                                            isPlaying -> R.drawable.pause
                                            else -> R.drawable.play
                                        }
                                    ),
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            IconButton(
                                onClick = { playerConnection.seekToNext() },
                                enabled = canSkipNext,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.skip_next),
                                    contentDescription = "Next",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            IconButton(
                                onClick = { playerConnection.toggleLike() },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    painter = painterResource(
                                        if (currentSong?.song?.liked == true) R.drawable.favorite
                                        else R.drawable.favorite_border
                                    ),
                                    contentDescription = "Like",
                                    tint = if (currentSong?.song?.liked == true)
                                        MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Image overlay
            AnimatedVisibility(
                visible = showImageOverlay,
                enter = fadeIn(tween(300)),
                exit = fadeOut(tween(300)),
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(1f)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(26.dp))
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = { showImageOverlay = false }
                            )
                        },
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        mediaMetadata?.let { metadata ->
                            AsyncImage(
                                model = metadata.thumbnailUrl,
                                contentDescription = null,
                                contentScale = ContentScale.FillBounds,
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .aspectRatio(1f)
                                    .scale(imageScale)
                                    .clip(RoundedCornerShape(cornerRadius))
                                    .then(
                                        if (rotateBackground) {
                                            Modifier.graphicsLayer {
                                                rotationZ = rotation
                                                transformOrigin = TransformOrigin.Center
                                            }
                                        } else {
                                            Modifier
                                        }
                                    )
                            )
                        }
                    }
                }
            }

            // Mode selection indicator
            AnimatedVisibility(
                visible = isSelectionModeActive,
                enter = slideInVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ) { it } + fadeIn(),
                exit = slideOutVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ) { it } + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .zIndex(3f)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.inverseSurface,
                    shadowElevation = 6.dp,
                    tonalElevation = 6.dp,
                    modifier = Modifier.padding(bottom = if (showControls) 180.dp else 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.check_circle),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.inverseOnSurface,
                            modifier = Modifier.size(18.dp)
                        )

                        Text(
                            text = stringResource(
                                R.string.selection_mode_active,
                                selectedIndices.size
                            ),
                            color = MaterialTheme.colorScheme.inverseOnSurface,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }

        // Main content of lyrics
        BoxWithConstraints(
            contentAlignment = if (isFullscreen) Alignment.TopStart else Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (isFullscreen) {
                        Modifier.padding(
                            top = 100.dp,
                            bottom = if (showControls) 180.dp else 0.dp
                        )
                    } else {
                        Modifier.padding(bottom = 12.dp)
                    }
                )
        ) {
            if (isFullscreen || showLyrics) {
                val topPadding = if (isFullscreen) {
                    with(LocalDensity.current) {
                        100.dp + WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
                    }
                } else {
                    0.dp
                }
                LazyColumn(
                    state = lazyListState,
                    contentPadding = if (isFullscreen) {
                        PaddingValues(
                            top = topPadding,
                            bottom = if (showControls) 180.dp else 0.dp,
                            start = 8.dp,
                            end = 8.dp
                        )
                    } else {
                        WindowInsets.systemBars
                            .only(WindowInsetsSides.Top)
                            .add(WindowInsets(top = maxHeight / 2, bottom = maxHeight / 2))
                            .asPaddingValues()
                    },
                    modifier = Modifier
                        .fadingEdge(vertical = if (isFullscreen) 32.dp else 64.dp)
                        .nestedScroll(remember {
                            object : NestedScrollConnection {
                                override fun onPostScroll(
                                    consumed: Offset,
                                    available: Offset,
                                    source: NestedScrollSource
                                ): Offset {
                                    if (!isSelectionModeActive) {
                                        lastPreviewTime = System.currentTimeMillis()
                                    }
                                    return super.onPostScroll(consumed, available, source)
                                }

                                override suspend fun onPostFling(
                                    consumed: Velocity,
                                    available: Velocity
                                ): Velocity {
                                    if (!isSelectionModeActive) {
                                        lastPreviewTime = System.currentTimeMillis()
                                    }
                                    return super.onPostFling(consumed, available)
                                }
                            }
                        })
                ) {
                    val displayedCurrentLineIndex =
                        if (isSeeking || isSelectionModeActive) deferredCurrentLineIndex else currentLineIndex

                    if (isLoadingLyrics) {
                        item {
                            ShimmerHost {
                                repeat(if (isFullscreen) 6 else 10) {
                                    Box(
                                        contentAlignment = when (lyricsTextPosition) {
                                            LyricsPosition.LEFT -> Alignment.CenterStart
                                            LyricsPosition.CENTER -> Alignment.Center
                                            LyricsPosition.RIGHT -> Alignment.CenterEnd
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(
                                                horizontal = if (isFullscreen) 16.dp else 24.dp,
                                                vertical = if (isFullscreen) 6.dp else 4.dp
                                            )
                                    ) {
                                        TextPlaceholder()
                                    }
                                }
                            }
                        }
                    } else {
                        itemsIndexed(
                            items = lines,
                            key = { index, item -> "$index-${item.time}" }
                        ) { index, item ->
                            val isSelected = selectedIndices.contains(index)

                            val distance =
                                kotlin.math.abs(index - displayedCurrentLineIndex)
                            val animatedScale by animateFloatAsState(when {
                                !isSynced || index == displayedCurrentLineIndex -> 1f
                                distance == 1 -> 0.95f
                                distance >= 2 -> 0.9f
                                else -> 1f
                            }, tween(if (animateLyrics) 250 else 0))
                            val animatedAlpha by animateFloatAsState(when {
                                !isSynced || (isSelectionModeActive && isSelected) -> 1f
                                index == displayedCurrentLineIndex -> 1f
                                kotlin.math.abs(index - displayedCurrentLineIndex) == 1 -> 0.7f
                                kotlin.math.abs(index - displayedCurrentLineIndex) == 2 -> 0.4f
                                else -> 0.2f
                            }, tween(if (animateLyrics) 250 else 0))
                            val itemModifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .combinedClickable(
                                    enabled = true,
                                    onClick = {
                                        if (isSelectionModeActive) {
                                            if (isSelected) {
                                                selectedIndices.remove(index)
                                                if (selectedIndices.isEmpty()) {
                                                    isSelectionModeActive = false
                                                }
                                            } else {
                                                if (selectedIndices.size < maxSelectionLimit) {
                                                    selectedIndices.add(index)
                                                } else {
                                                    showMaxSelectionToast = true
                                                }
                                            }
                                        } else if (isSynced && changeLyrics) {
                                            playerConnection.player.seekTo(item.time)
                                            scope.launch {
                                                lazyListState.scrollToItem(index)
                                                val itemInfo =
                                                    lazyListState.layoutInfo.visibleItemsInfo
                                                        .firstOrNull { it.index == index }
                                                if (itemInfo != null) {
                                                    val viewportHeight =
                                                        lazyListState.layoutInfo.viewportEndOffset -
                                                                lazyListState.layoutInfo.viewportStartOffset
                                                    val center =
                                                        lazyListState.layoutInfo.viewportStartOffset +
                                                                (viewportHeight / 2)
                                                    val itemCenter =
                                                        itemInfo.offset + itemInfo.size / 2
                                                    val offset = itemCenter - center

                                                    if (kotlin.math.abs(offset) > 10) {
                                                        lazyListState.animateScrollBy(
                                                            value = offset.toFloat(),
                                                            animationSpec = tween(durationMillis = 1500)
                                                        )
                                                    }
                                                }
                                            }
                                            lastPreviewTime = 0L
                                        }
                                    },
                                    onLongClick = {
                                        if (!isSelectionModeActive) {
                                            isSelectionModeActive = true
                                            selectedIndices.add(index)
                                        } else if (!isSelected && selectedIndices.size < maxSelectionLimit) {
                                            selectedIndices.add(index)
                                        } else if (!isSelected) {
                                            showMaxSelectionToast = true
                                        }
                                    }
                                )
                                .background(
                                    if (isSelected && isSelectionModeActive)
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                    else Color.Transparent
                                )
                                .padding(
                                    horizontal = if (isFullscreen) 16.dp else 24.dp,
                                    vertical = 8.dp
                                )
                                .alpha(animatedAlpha)
                                .graphicsLayer {
                                    scaleX = animatedScale
                                    scaleY = animatedScale
                                }

                            Text(
                                text = item.text,
                                fontSize = 25.sp,
                                color = animateColorAsState(if (index == displayedCurrentLineIndex && isSynced) {
                                    if (isFullscreen)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        textColor
                                } else {
                                    if (isFullscreen)
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                    else
                                        textColor.copy(alpha = 0.8f)
                                }, animationSpec = tween(if (animateLyrics) 250 else 0)).value,
                                textAlign = when (lyricsTextPosition) {
                                    LyricsPosition.LEFT -> TextAlign.Left
                                    LyricsPosition.CENTER -> TextAlign.Center
                                    LyricsPosition.RIGHT -> TextAlign.Right
                                },
                                fontWeight = if (index == displayedCurrentLineIndex && isSynced)
                                    FontWeight.ExtraBold
                                else
                                    FontWeight.Bold,
                                modifier = itemModifier
                            )
                        }
                    }
                }

                // Letters not found
                if (lyrics == LYRICS_NOT_FOUND) {
                    if (isFullscreen) {
                        Card(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .fillMaxWidth(0.8f)
                                .padding(vertical = 32.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.music_note),
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Text(
                                    text = stringResource(R.string.lyrics_not_found),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )

                                Text(
                                    text = "Las letras no están disponibles para esta canción",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        Text(
                            text = stringResource(R.string.lyrics_not_found),
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.secondary,
                            textAlign = when (lyricsTextPosition) {
                                LyricsPosition.LEFT -> TextAlign.Left
                                LyricsPosition.CENTER -> TextAlign.Center
                                LyricsPosition.RIGHT -> TextAlign.Right
                            },
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 8.dp)
                                .alpha(0.5f)
                        )
                    }
                }
            }

        }


        // Buttons for floating mode (not full screen)
        if (!isFullscreen) {
            mediaMetadata?.let { metadata ->
                if (isSelectionModeActive) {

                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Close button
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        color = Color.Black.copy(alpha = 0.3f),
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        isSelectionModeActive = false
                                        selectedIndices.clear()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.close),
                                    contentDescription = stringResource(R.string.cancel),
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Share button
                            Row(
                                modifier = Modifier
                                    .background(
                                        color = if (selectedIndices.isNotEmpty())
                                            Color.White.copy(alpha = 0.9f)
                                        else
                                            Color.White.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(24.dp)
                                    )
                                    .clickable(enabled = selectedIndices.isNotEmpty()) {
                                        if (selectedIndices.isNotEmpty()) {
                                            val sortedIndices = selectedIndices.sorted()
                                            val selectedLyricsText = sortedIndices
                                                .mapNotNull { lines.getOrNull(it)?.text }
                                                .joinToString("\n")

                                            if (selectedLyricsText.isNotBlank()) {
                                                shareDialogData = Triple(
                                                    selectedLyricsText,
                                                    metadata.title,
                                                    metadata.artists.joinToString { it.name }
                                                )
                                                showShareDialog = true
                                            }
                                            isSelectionModeActive = false
                                            selectedIndices.clear()
                                        }
                                    }
                                    .padding(horizontal = 24.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.media3_icon_share),
                                    contentDescription = stringResource(R.string.share_selected),
                                    tint = Color.Black,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = stringResource(R.string.share),
                                    color = Color.Black,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                } else {
                    // Normal buttons in floating mode
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { showLyrics = !showLyrics }
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.close),
                                contentDescription = null,
                                tint = textColor
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = {
                                menuState.show {
                                    LyricsMenu(
                                        lyricsProvider = { currentLyricsEntity },
                                        mediaMetadataProvider = { metadata },
                                        onDismiss = menuState::dismiss
                                    )
                                }
                            }
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.more_horiz),
                                contentDescription = stringResource(R.string.more_options),
                                tint = textColor
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialogs
    if (showProgressDialog) {
        BasicAlertDialog(onDismissRequest = { /* No permitir cerrar */ }) {
            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Box(modifier = Modifier.padding(32.dp)) {
                    Text(
                        text = stringResource(R.string.generating_image) + "\n" + stringResource(R.string.please_wait),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }

    if (showShareDialog && shareDialogData != null) {
        ShareLyricsDialog(
            lyricsText = shareDialogData!!.first,
            songTitle = shareDialogData!!.second,
            artists = shareDialogData!!.third,
            mediaMetadata = mediaMetadata,
            onDismiss = {
                showShareDialog = false
                shareDialogData = null
            }
        )
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShareLyricsDialog(
    lyricsText: String,
    songTitle: String,
    artists: String,
    mediaMetadata: com.arturo254.opentune.models.MediaMetadata?,
    onDismiss: () -> Unit,
    onShareAsImage: (String, String, String) -> Unit = { _, _, _ -> }
) {
    val context = LocalContext.current

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(0.85f)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = stringResource(R.string.share_lyrics),
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Share as text
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val shareIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                type = "text/plain"
                                val songLink =
                                    "https://music.youtube.com/watch?v=${mediaMetadata?.id}"
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "\"$lyricsText\"\n\n$songTitle - $artists\n$songLink"
                                )
                            }
                            context.startActivity(
                                Intent.createChooser(
                                    shareIntent,
                                    context.getString(R.string.share_lyrics)
                                )
                            )
                            onDismiss()
                        }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.media3_icon_share),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.share_as_text),
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Share as image
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onShareAsImage(lyricsText, songTitle, artists)
                        }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.media3_icon_share),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.share_as_image),
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Cancel button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    Text(
                        text = stringResource(R.string.cancel),
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .clickable { onDismiss() }
                            .padding(vertical = 8.dp, horizontal = 12.dp)
                    )
                }
            }
        }
    }
}


// Preview time constant
val LyricsPreviewTime = 2.seconds
const val ANIMATE_SCROLL_DURATION = 300L