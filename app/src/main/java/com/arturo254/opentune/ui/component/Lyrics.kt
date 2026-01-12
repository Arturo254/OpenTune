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
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.ui.graphics.toArgb
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
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
import com.arturo254.opentune.constants.SliderStyle
import com.arturo254.opentune.constants.SliderStyleKey
import com.arturo254.opentune.db.entities.LyricsEntity
import com.arturo254.opentune.db.entities.LyricsEntity.Companion.LYRICS_NOT_FOUND
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
@SuppressLint("UnusedBoxWithConstraintsScope", "StringFormatInvalid",
    "LocalContextGetResourceValueCall"
)
@Composable
fun Lyrics(
    sliderPositionProvider: () -> Long?,
    onNavigateBack: (() -> Unit)? = null,
    mediaMetadata: com.arturo254.opentune.models.MediaMetadata? = null,
    onBackClick: () -> Unit = {},
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier,
    backgroundAlpha: () -> Float = { 1f }
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

    val rotateBackground by rememberPreference(RotateBackgroundKey, defaultValue = false)

    // Usar mediaMetadata proporcionada o la del playerConnection
    val currentMetadata = mediaMetadata ?: playerConnection.mediaMetadata.collectAsState().value
    val currentSongId = currentMetadata?.id

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
    var cornerRadius by remember { mutableFloatStateOf(16f) }

    var isAutoScrollEnabled by rememberSaveable { mutableStateOf(true) }

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

    var position by rememberSaveable(playbackState) { mutableLongStateOf(playerConnection.player.currentPosition) }
    var duration by rememberSaveable(playbackState) { mutableLongStateOf(playerConnection.player.duration) }

    val expressiveAccent = when (playerBackground) {
        PlayerBackgroundStyle.DEFAULT -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.tertiary
    }
    val textColor = expressiveAccent

    // Determinar color del texto según el fondo
    val textBackgroundColor = when (playerBackground) {
        PlayerBackgroundStyle.DEFAULT -> MaterialTheme.colorScheme.onBackground
        PlayerBackgroundStyle.BLUR, PlayerBackgroundStyle.GRADIENT, PlayerBackgroundStyle.APPLE_MUSIC -> Color.White
    }

    // Background colors para gradientes
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val surfaceColor = MaterialTheme.colorScheme.surface

    var gradientColors by remember { mutableStateOf<List<Color>>(emptyList()) }
    val gradientColorsCache = remember { mutableMapOf<String, List<Color>>() }
    val fallbackColor = MaterialTheme.colorScheme.surface.toArgb()

    LaunchedEffect(currentMetadata?.id, playerBackground) {
        if ((playerBackground == PlayerBackgroundStyle.GRADIENT || playerBackground == PlayerBackgroundStyle.APPLE_MUSIC) && currentMetadata?.thumbnailUrl != null) {
            val cachedColors = gradientColorsCache[currentMetadata.id]
            if (cachedColors != null) {
                gradientColors = cachedColors
                return@LaunchedEffect
            }

            withContext(Dispatchers.IO) {
                try {
                    // Usar colores del tema como fallback
                    val fallbackColors = listOf(primaryColor, secondaryColor, tertiaryColor)
                    gradientColorsCache[currentMetadata.id] = fallbackColors
                    withContext(Dispatchers.Main) { gradientColors = fallbackColors }
                } catch (e: Exception) {
                    val fallbackColors = listOf(primaryColor, secondaryColor, tertiaryColor)
                    gradientColorsCache[currentMetadata.id] = fallbackColors
                    withContext(Dispatchers.Main) { gradientColors = fallbackColors }
                }
            }
        } else {
            gradientColors = emptyList()
        }
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
                        try {
                            val entryPoint = EntryPointAccessors.fromApplication(
                                context.applicationContext,
                                com.arturo254.opentune.di.LyricsHelperEntryPoint::class.java
                            )
                            val lyricsHelper = entryPoint.lyricsHelper()
                            val fetchedLyrics: String? = currentMetadata?.let { lyricsHelper.getLyrics(it) }

                            val entity = if (!fetchedLyrics.isNullOrBlank()) {
                                LyricsEntity(songId, fetchedLyrics)
                            } else {
                                LyricsEntity(songId, LYRICS_NOT_FOUND)
                            }

                            try {
                                database.query {
                                    upsert(entity)
                                }
                            } catch (e: Throwable) {}

                            val newCache = lyricsCache.toMutableMap().apply {
                                put(songId, entity)
                            }
                            lyricsCache = newCache
                            currentLyricsEntity = entity
                        } catch (e: Throwable) {
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

    LaunchedEffect(lines) {
        isSelectionModeActive = false
        selectedIndices.clear()
        currentLineIndex = -1
        deferredCurrentLineIndex = 0
        previousLineIndex = 0
        initialScrollDone = false
        shouldScrollToFirstLine = true
        isAutoScrollEnabled = true
    }

    val isSynced = remember(lyrics) {
        !lyrics.isNullOrEmpty() && lyrics.startsWith("[")
    }

    BackHandler(enabled = isSelectionModeActive || isFullscreen) {
        when {
            isSelectionModeActive -> {
                isSelectionModeActive = false
                selectedIndices.clear()
            }
            isFullscreen -> onNavigateBack?.invoke()
        }
    }

    // Disable auto-scroll cuando el usuario interactúa
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (source == NestedScrollSource.UserInput) {
                    isAutoScrollEnabled = false
                }
                if (!isSelectionModeActive) {
                    lastPreviewTime = System.currentTimeMillis()
                }
                return super.onPostScroll(consumed, available, source)
            }

            override suspend fun onPostFling(
                consumed: Velocity,
                available: Velocity
            ): Velocity {
                isAutoScrollEnabled = false
                if (!isSelectionModeActive) {
                    lastPreviewTime = System.currentTimeMillis()
                }
                return super.onPostFling(consumed, available)
            }
        }
    }

    LaunchedEffect(Unit) {
        if (isFullscreen) {
            cornerRadius = 16f
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

    LaunchedEffect(currentLineIndex, lastPreviewTime, initialScrollDone, isAutoScrollEnabled) {
        if (!isSynced) return@LaunchedEffect

        if (isAutoScrollEnabled) {
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
    ) {
        // Fondo para modo fullscreen
        if (isFullscreen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = backgroundAlpha() }
            ) {
                when (playerBackground) {
                    PlayerBackgroundStyle.BLUR -> {
                        currentMetadata?.let { metadata ->
                            AsyncImage(
                                model = metadata.thumbnailUrl,
                                contentDescription = null,
                                contentScale = ContentScale.FillBounds,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .blur(if (useDarkTheme) 150.dp else 100.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.3f))
                            )
                        }
                    }
                    PlayerBackgroundStyle.GRADIENT -> {
                        if (gradientColors.isNotEmpty()) {
                            val gradientColorStops = if (gradientColors.size >= 3) {
                                arrayOf(
                                    0.0f to gradientColors[0],
                                    0.5f to gradientColors[1],
                                    1.0f to gradientColors[2]
                                )
                            } else {
                                arrayOf(
                                    0.0f to gradientColors[0],
                                    0.6f to gradientColors[0].copy(alpha = 0.7f),
                                    1.0f to Color.Black
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(androidx.compose.ui.graphics.Brush.verticalGradient(colorStops = gradientColorStops))
                                    .background(Color.Black.copy(alpha = 0.2f))
                            )
                        }
                    }
                    PlayerBackgroundStyle.APPLE_MUSIC -> {
                        Box(modifier = Modifier.fillMaxSize()) {
                            if (gradientColors.isNotEmpty()) {
                                // Sophisticated blurred gradient background
                                val color1 = gradientColors[0]
                                val color2 = gradientColors.getOrElse(1) { gradientColors[0].copy(alpha = 0.8f) }
                                val color3 = gradientColors.getOrElse(2) { gradientColors[0].copy(alpha = 0.6f) }

                                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize().blur(100.dp)) {
                                    // Main vertical gradient base
                                    drawRect(
                                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                            listOf(color1, color2, color3)
                                        )
                                    )

                                    // Multiple circular "color blobs" for a dynamic feel
                                    drawCircle(
                                        brush = androidx.compose.ui.graphics.Brush.radialGradient(
                                            colors = listOf(color1, Color.Transparent),
                                            center = Offset(size.width * 0.2f, size.height * 0.2f),
                                            radius = size.width * 0.8f
                                        ),
                                        center = Offset(size.width * 0.2f, size.height * 0.2f),
                                        radius = size.width * 0.8f
                                    )

                                    drawCircle(
                                        brush = androidx.compose.ui.graphics.Brush.radialGradient(
                                            colors = listOf(color2, Color.Transparent),
                                            center = Offset(size.width * 0.8f, size.height * 0.5f),
                                            radius = size.width * 0.7f
                                        ),
                                        center = Offset(size.width * 0.8f, size.height * 0.5f),
                                        radius = size.width * 0.7f
                                    )

                                    drawCircle(
                                        brush = androidx.compose.ui.graphics.Brush.radialGradient(
                                            colors = listOf(color3, Color.Transparent),
                                            center = Offset(size.width * 0.3f, size.height * 0.8f),
                                            radius = size.width * 0.9f
                                        ),
                                        center = Offset(size.width * 0.3f, size.height * 0.8f),
                                        radius = size.width * 0.9f
                                    )
                                }

                                // Dark overlay for text readability
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.25f))
                                )
                            }
                        }
                    }
                    PlayerBackgroundStyle.DEFAULT -> {
                        // DEFAULT background
                    }
                }

                if (playerBackground != PlayerBackgroundStyle.DEFAULT) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.3f))
                    )
                }
            }
        }

        // Layout principal con letras y controles
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(WindowInsets.systemBars.asPaddingValues())
        ) {
            // Espacio para las letras
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                BoxWithConstraints(
                    contentAlignment = Alignment.TopStart,
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    val topPadding = with(LocalDensity.current) {
                        100.dp + WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
                    }

                    LazyColumn(
                        state = lazyListState,
                        contentPadding = PaddingValues(
                            top = topPadding,
                            bottom = if (isFullscreen) 180.dp else 0.dp,
                            start = 8.dp,
                            end = 8.dp
                        ),
                        modifier = Modifier
                            .fadingEdge(vertical = 32.dp)
                            .nestedScroll(nestedScrollConnection)
                    ) {
                        val displayedCurrentLineIndex =
                            if (!isAutoScrollEnabled || isSeeking || isSelectionModeActive)
                                deferredCurrentLineIndex
                            else
                                currentLineIndex

                        if (isLoadingLyrics) {
                            item {
                                ShimmerHost {
                                    repeat(6) {
                                        Box(
                                            contentAlignment = when (lyricsTextPosition) {
                                                LyricsPosition.LEFT -> Alignment.CenterStart
                                                LyricsPosition.CENTER -> Alignment.Center
                                                LyricsPosition.RIGHT -> Alignment.CenterEnd
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(
                                                    horizontal = 16.dp,
                                                    vertical = 6.dp
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

                                val distance = kotlin.math.abs(index - displayedCurrentLineIndex)
                                val animatedScale by animateFloatAsState(when {
                                    !isSynced || index == displayedCurrentLineIndex -> 1.05f
                                    distance == 1 -> 1f
                                    distance >= 2 -> 0.95f
                                    else -> 1f
                                }, tween(if (animateLyrics) 400 else 0))

                                val animatedAlpha by animateFloatAsState(when {
                                    !isSynced || (isSelectionModeActive && isSelected) -> 1f
                                    index == displayedCurrentLineIndex -> 1f
                                    kotlin.math.abs(index - displayedCurrentLineIndex) == 1 -> 0.7f
                                    kotlin.math.abs(index - displayedCurrentLineIndex) == 2 -> 0.4f
                                    else -> 0.2f
                                }, tween(if (animateLyrics) 400 else 0))

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
                                                    performSmoothPageScroll(index, 1500)
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
                                        horizontal = 24.dp,
                                        vertical = 4.dp
                                    )
                                    .graphicsLayer {
                                        this.alpha = animatedAlpha
                                        this.scaleX = animatedScale
                                        this.scaleY = animatedScale
                                    }

                                val isActiveLine = index == displayedCurrentLineIndex && isSynced

                                Column(
                                    modifier = itemModifier,
                                    horizontalAlignment = when (lyricsTextPosition) {
                                        LyricsPosition.LEFT -> Alignment.Start
                                        LyricsPosition.CENTER -> Alignment.CenterHorizontally
                                        LyricsPosition.RIGHT -> Alignment.End
                                    }
                                ) {
                                    if (isActiveLine) {
                                        val fillProgress = remember { Animatable(0f) }
                                        val pulseProgress = remember { Animatable(0f) }

                                        LaunchedEffect(index) {
                                            fillProgress.snapTo(0f)
                                            fillProgress.animateTo(
                                                targetValue = 1f,
                                                animationSpec = tween(
                                                    durationMillis = 1200,
                                                    easing = FastOutSlowInEasing
                                                )
                                            )
                                        }

                                        LaunchedEffect(Unit) {
                                            while (true) {
                                                pulseProgress.animateTo(
                                                    targetValue = 1f,
                                                    animationSpec = tween(
                                                        durationMillis = 3000,
                                                        easing = LinearEasing
                                                    )
                                                )
                                                pulseProgress.snapTo(0f)
                                            }
                                        }

                                        val fill = fillProgress.value
                                        val pulse = pulseProgress.value
                                        val pulseEffect = (kotlin.math.sin(pulse * Math.PI.toFloat()) * 0.15f).coerceIn(0f, 0.15f)
                                        val glowIntensity = (fill + pulseEffect).coerceIn(0f, 1.2f)

                                        val glowBrush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                                            0.0f to expressiveAccent.copy(alpha = 0.3f),
                                            (fill * 0.7f).coerceIn(0f, 1f) to expressiveAccent.copy(alpha = 0.9f),
                                            fill to expressiveAccent,
                                            (fill + 0.1f).coerceIn(0f, 1f) to expressiveAccent.copy(alpha = 0.7f),
                                            1.0f to expressiveAccent.copy(alpha = if (fill >= 1f) 1f else 0.3f)
                                        )

                                        val styledText = buildAnnotatedString {
                                            withStyle(
                                                style = SpanStyle(
                                                    shadow = androidx.compose.ui.graphics.Shadow(
                                                        color = expressiveAccent.copy(alpha = 0.8f * glowIntensity),
                                                        offset = Offset(0f, 0f),
                                                        blurRadius = 28f * (1f + pulseEffect)
                                                    ),
                                                    brush = glowBrush
                                                )
                                            ) {
                                                append(item.text)
                                            }
                                        }

                                        val bounceScale = if (fill < 0.3f) {
                                            1f + (kotlin.math.sin(fill * 3.33f * Math.PI.toFloat()) * 0.03f)
                                        } else {
                                            1f
                                        }

                                        Text(
                                            text = styledText,
                                            fontSize = 25.sp,
                                            textAlign = when (lyricsTextPosition) {
                                                LyricsPosition.LEFT -> TextAlign.Left
                                                LyricsPosition.CENTER -> TextAlign.Center
                                                LyricsPosition.RIGHT -> TextAlign.Right
                                            },
                                            fontWeight = FontWeight.ExtraBold,
                                            modifier = Modifier
                                                .graphicsLayer {
                                                    scaleX = bounceScale
                                                    scaleY = bounceScale
                                                }
                                        )
                                    } else {
                                        Text(
                                            text = item.text,
                                            fontSize = 25.sp,
                                            color = expressiveAccent.copy(alpha = 0.7f),
                                            textAlign = when (lyricsTextPosition) {
                                                LyricsPosition.LEFT -> TextAlign.Left
                                                LyricsPosition.CENTER -> TextAlign.Center
                                                LyricsPosition.RIGHT -> TextAlign.Right
                                            },
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Lyrics not found
                    if (lyrics == LYRICS_NOT_FOUND) {
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
                    }
                }
            }

            // REPRODUCTOR FIJO - IGUAL AL ORIGINAL
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp)
            ) {
                // Album artwork, title, artist, and buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left side - Album artwork and text info
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    if (playbackState == androidx.media3.common.Player.STATE_ENDED) {
                                        playerConnection.player.seekTo(0, 0)
                                        playerConnection.player.playWhenReady = true
                                    } else {
                                        if (isPlaying) playerConnection.player.pause() else playerConnection.player.play()
                                    }
                                }
                        ) {
                            currentMetadata?.let { metadata ->
                                AsyncImage(
                                    model = metadata.thumbnailUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            // Overlay and Icon
                            val overlayAlpha by androidx.compose.animation.core.animateFloatAsState(
                                targetValue = if (isPlaying) 0.4f else 0.4f,
                                label = "overlay_alpha"
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = overlayAlpha))
                            )

                            androidx.compose.animation.AnimatedVisibility(
                                visible = playbackState == androidx.media3.common.Player.STATE_ENDED || !isPlaying || isPlaying,
                                enter = fadeIn(),
                                exit = fadeOut()
                            ) {
                                Icon(
                                    painter = painterResource(
                                        if (playbackState == androidx.media3.common.Player.STATE_ENDED) {
                                            R.drawable.replay
                                        } else if (isPlaying) {
                                            R.drawable.pause
                                        } else {
                                            R.drawable.play
                                        }
                                    ),
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.weight(1f)
                        ) {
                            currentMetadata?.let { metadata ->
                                Text(
                                    text = metadata.title,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = textBackgroundColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = if (metadata.artists.isNotEmpty()) {
                                        metadata.artists.joinToString(", ") { it.name }
                                    } else {
                                        stringResource(R.string.unknown)
                                    },
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 14.sp
                                    ),
                                    color = textBackgroundColor.copy(alpha = 0.7f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Right side - Action buttons
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Favorite button
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clickable {
                                    playerConnection.toggleLike()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(
                                    if (currentSong?.song?.liked == true)
                                        R.drawable.favorite
                                    else R.drawable.favorite_border
                                ),
                                contentDescription = if (currentSong?.song?.liked == true) stringResource(R.string.remove_from_library) else stringResource(R.string.add_to_library),
                                tint = if (currentSong?.song?.liked == true)
                                    MaterialTheme.colorScheme.error
                                else
                                    textBackgroundColor.copy(alpha = 0.8f),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // More button
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clickable {
                                    currentMetadata?.let { metadata ->
                                        menuState.show {
                                            LyricsMenu(
                                                lyricsProvider = { currentLyricsEntity },
                                                mediaMetadataProvider = { metadata },
                                                onDismiss = menuState::dismiss
                                            )
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.more_horiz),
                                contentDescription = stringResource(R.string.more_options),
                                tint = textBackgroundColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Slider
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
                            colors = androidx.compose.material3.SliderDefaults.colors(
                                activeTrackColor = textBackgroundColor,
                                inactiveTrackColor = textBackgroundColor.copy(alpha = 0.3f),
                                thumbColor = textBackgroundColor
                            ),
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
                            colors = androidx.compose.material3.SliderDefaults.colors(
                                activeTrackColor = textBackgroundColor,
                                inactiveTrackColor = textBackgroundColor.copy(alpha = 0.3f),
                                thumbColor = textBackgroundColor
                            ),
                            squigglesSpec = SquigglySlider.SquigglesSpec(
                                amplitude = if (isPlaying) (4.dp).coerceAtLeast(2.dp) else 0.dp,
                                strokeWidth = 3.dp,
                                wavelength = 36.dp,
                            ),
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
                            colors = androidx.compose.material3.SliderDefaults.colors(
                                activeTrackColor = textBackgroundColor,
                                inactiveTrackColor = textBackgroundColor.copy(alpha = 0.3f)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Time display
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = makeTimeString(sliderPosition ?: position),
                        style = MaterialTheme.typography.labelMedium,
                        color = textBackgroundColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    Text(
                        text = if (duration != C.TIME_UNSET) makeTimeString(duration) else "",
                        style = MaterialTheme.typography.labelMedium,
                        color = textBackgroundColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        // Auto-scroll button
        AnimatedVisibility(
            visible = !isAutoScrollEnabled && isSynced,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 220.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                tonalElevation = 4.dp,
                modifier = Modifier
                    .clickable {
                        scope.launch {
                            performSmoothPageScroll(currentLineIndex, 1500)
                        }
                        isAutoScrollEnabled = true
                    }
                    .padding(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.sync),
                        contentDescription = stringResource(R.string.auto_scroll),
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.auto_scroll),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        if (isFullscreen && isSelectionModeActive) {
            AnimatedVisibility(
                visible = isSelectionModeActive,
                enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { it },
                exit = fadeOut(tween(300)) + slideOutVertically(tween(300)) { it },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 180.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Botón de cancelar
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f),
                        tonalElevation = 4.dp,
                        modifier = Modifier
                            .size(56.dp)
                            .clickable {
                                isSelectionModeActive = false
                                selectedIndices.clear()
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(id = R.drawable.close),
                                contentDescription = stringResource(R.string.cancel),
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    // Botón de compartir (solo visible si hay selección)
                    if (selectedIndices.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(28.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
                            tonalElevation = 4.dp,
                            modifier = Modifier
                                .clickable {
                                    val sortedIndices = selectedIndices.sorted()
                                    val selectedLyricsText = sortedIndices
                                        .mapNotNull { lines.getOrNull(it)?.text }
                                        .joinToString("\n")

                                    if (selectedLyricsText.isNotBlank()) {
                                        shareDialogData = Triple(
                                            selectedLyricsText,
                                            currentMetadata?.title ?: "",
                                            currentMetadata?.artists?.joinToString { it.name } ?: ""
                                        )
                                        showShareDialog = true
                                    }
                                    isSelectionModeActive = false
                                    selectedIndices.clear()
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.media3_icon_share),
                                    contentDescription = stringResource(R.string.share_selected),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = stringResource(R.string.share),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
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
            mediaMetadata = currentMetadata,
            onDismiss = {
                showShareDialog = false
                shareDialogData = null
            }
        )
    }
}

@SuppressLint("LocalContextGetResourceValueCall")
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