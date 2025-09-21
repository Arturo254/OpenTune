package com.arturo254.opentune.ui.component

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
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
import androidx.compose.ui.text.font.FontFamily
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
import com.arturo254.opentune.constants.DarkModeKey
import com.arturo254.opentune.constants.LyricsClickKey
import com.arturo254.opentune.constants.LyricsTextPositionKey
import com.arturo254.opentune.constants.PlayerBackgroundStyle
import com.arturo254.opentune.constants.PlayerBackgroundStyleKey
import com.arturo254.opentune.constants.ShowLyricsKey
import com.arturo254.opentune.constants.SliderStyle
import com.arturo254.opentune.constants.SliderStyleKey
import com.arturo254.opentune.db.entities.LyricsEntity
import com.arturo254.opentune.db.entities.LyricsEntity.Companion.LYRICS_NOT_FOUND
import com.arturo254.opentune.extensions.togglePlayPause
import com.arturo254.opentune.extensions.toggleRepeatMode
import com.arturo254.opentune.lyrics.LyricsEntry
import com.arturo254.opentune.lyrics.LyricsEntry.Companion.HEAD_LYRICS_ENTRY
import com.arturo254.opentune.lyrics.LyricsUtils.findCurrentLineIndex
import com.arturo254.opentune.lyrics.LyricsUtils.parseLyrics
import com.arturo254.opentune.ui.component.shimmer.ShimmerHost
import com.arturo254.opentune.ui.component.shimmer.TextPlaceholder
import com.arturo254.opentune.ui.menu.LyricsMenu
import com.arturo254.opentune.ui.screens.settings.DarkMode
import com.arturo254.opentune.ui.screens.settings.LyricsPosition
import com.arturo254.opentune.ui.utils.fadingEdge
import com.arturo254.opentune.utils.ComposeToImage
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

    var showLyrics by rememberPreference(ShowLyricsKey, defaultValue = false)

    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val currentSongId = mediaMetadata?.id

    // SOLUCIÓN 1: Cache persistente con mapa de letras por canción
    var lyricsCache by remember { mutableStateOf<Map<String, LyricsEntity>>(emptyMap()) }

    // SOLUCIÓN 2: Estado reactivo que se resetea cuando cambia la canción
    var currentLyricsEntity by remember(currentSongId) {
        mutableStateOf<LyricsEntity?>(lyricsCache[currentSongId])
    }

    // Estado de carga separado para evitar mostrar shimmer innecesariamente
    var isLoadingLyrics by remember(currentSongId) { mutableStateOf(false) }

    val lyrics = remember(currentLyricsEntity) { currentLyricsEntity?.lyrics?.trim() }

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

    // SOLUCIÓN 3: Cargar letras desde BD primero, luego desde API si no existen
    LaunchedEffect(currentSongId) {
        currentSongId?.let { songId ->
            // Si ya están en cache, no hacer nada
            if (lyricsCache.containsKey(songId)) {
                currentLyricsEntity = lyricsCache[songId]
                return@LaunchedEffect
            }

            isLoadingLyrics = true

            withContext(Dispatchers.IO) {
                try {
                    // TODO: Implementar consulta a BD cuando tengas el método correcto
                    // val existingLyrics = database.query { lyricsDao().get(songId) }
                    val existingLyrics: LyricsEntity? = null // Temporal

                    if (existingLyrics != null) {
                        // Actualizar cache y estado
                        val newCache = lyricsCache.toMutableMap().apply {
                            put(songId, existingLyrics)
                        }
                        lyricsCache = newCache
                        currentLyricsEntity = existingLyrics
                        isLoadingLyrics = false
                    } else {
                        // Si no están en BD, buscar en API
                        val entryPoint = EntryPointAccessors.fromApplication(
                            context.applicationContext,
                            com.arturo254.opentune.di.LyricsHelperEntryPoint::class.java
                        )
                        val lyricsHelper = entryPoint.lyricsHelper()

                        val fetchedLyrics = mediaMetadata?.let { lyricsHelper.getLyrics(it) }

                        val lyricsEntity = if (fetchedLyrics != null) {
                            val entity = LyricsEntity(songId, fetchedLyrics)
                            // Guardar en BD
                            database.query {
                                upsert(entity)
                            }
                            entity
                        } else {
                            val entity = LyricsEntity(songId, LYRICS_NOT_FOUND)
                            // Guardar que no se encontraron para evitar búsquedas repetidas
                            database.query {
                                upsert(entity)
                            }
                            entity
                        }

                        // Actualizar cache y estado
                        val newCache = lyricsCache.toMutableMap().apply {
                            put(songId, lyricsEntity)
                        }
                        lyricsCache = newCache
                        currentLyricsEntity = lyricsEntity
                    }
                } catch (e: Exception) {
                    // En caso de error, marcar como no encontrado
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

    // SOLUCIÓN 4: Resetear estados de UI cuando cambia la canción
    val lines = remember(lyrics, currentSongId) {
        if (lyrics == null || lyrics == LYRICS_NOT_FOUND) {
            emptyList()
        } else if (lyrics.startsWith("[")) {
            listOf(HEAD_LYRICS_ENTRY) + parseLyrics(lyrics)
        } else {
            lyrics.lines().mapIndexed { index, line -> LyricsEntry(index * 100L, line) }
        }
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

    // Estados que se resetean cuando cambia la canción
    var currentLineIndex by remember { mutableIntStateOf(-1) }
    var deferredCurrentLineIndex by remember(currentSongId) { mutableIntStateOf(0) }
    var previousLineIndex by remember(currentSongId) { mutableIntStateOf(0) }
    var lastPreviewTime by remember(currentSongId) { mutableLongStateOf(0L) }
    var isSeeking by remember { mutableStateOf(false) }
    var initialScrollDone by remember(currentSongId) { mutableStateOf(false) }
    var shouldScrollToFirstLine by remember(currentSongId) { mutableStateOf(true) }
    var isAppMinimized by rememberSaveable { mutableStateOf(false) }
    var position by rememberSaveable(playbackState) { mutableLongStateOf(playerConnection.player.currentPosition) }
    var duration by rememberSaveable(playbackState) { mutableLongStateOf(playerConnection.player.duration) }
    var sliderPosition by remember { mutableStateOf<Long?>(null) }
    var showImageOverlay by remember { mutableStateOf(false) }
    var showControls by remember { mutableStateOf(true) }
    var cornerRadius by remember { mutableFloatStateOf(16f) }
    var showProgressDialog by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }
    var shareDialogData by remember { mutableStateOf<Triple<String, String, String>?>(null) }
    var showColorPickerDialog by remember { mutableStateOf(false) }
    var previewBackgroundColor by remember { mutableStateOf(Color(0xFF242424)) }
    var previewTextColor by remember { mutableStateOf(Color.White) }
    var previewSecondaryTextColor by remember { mutableStateOf(Color.White.copy(alpha = 0.7f)) }

    // Estados de selección que también se resetean cuando cambia la canción
    var isSelectionModeActive by remember(currentSongId) { mutableStateOf(false) }
    val selectedIndices = remember(currentSongId) { mutableStateListOf<Int>() }
    var showMaxSelectionToast by remember { mutableStateOf(false) }

    // LazyListState que se resetea cuando cambia la canción
    val lazyListState = rememberLazyListState()
    val maxSelectionLimit = 5

    // Resto del código permanece igual...
    LaunchedEffect(Unit) {
        if (isFullscreen) {
            cornerRadius = AppConfig.getThumbnailCornerRadius(context)
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

    LaunchedEffect(currentLineIndex, lastPreviewTime, initialScrollDone) {
        fun countNewLine(str: String) = str.count { it == '\n' }

        fun calculateOffset() = with(density) {
            if (landscapeOffset) {
                16.dp.toPx().toInt() * countNewLine(lines[currentLineIndex].text)
            } else {
                20.dp.toPx().toInt() * countNewLine(lines[currentLineIndex].text)
            }
        }

        if (!isSynced) return@LaunchedEffect

        if ((currentLineIndex == 0 && shouldScrollToFirstLine) || !initialScrollDone) {
            shouldScrollToFirstLine = false
            lazyListState.scrollToItem(
                currentLineIndex,
                with(density) {
                    (if (isFullscreen) 72.dp else 36.dp).toPx().toInt()
                } + calculateOffset()
            )
            if (!isAppMinimized) {
                initialScrollDone = true
            }
        } else if (currentLineIndex != -1) {
            deferredCurrentLineIndex = currentLineIndex
            if (isSeeking) {
                lazyListState.scrollToItem(
                    currentLineIndex,
                    with(density) {
                        (if (isFullscreen) 72.dp else 36.dp).toPx().toInt()
                    } + calculateOffset()
                )
            } else if (lastPreviewTime == 0L || currentLineIndex != previousLineIndex) {
                val visibleItemsInfo = lazyListState.layoutInfo.visibleItemsInfo
                val isCurrentLineVisible = visibleItemsInfo.any { it.index == currentLineIndex }

                if (isCurrentLineVisible) {
                    if (isFullscreen) {
                        lazyListState.animateScrollToItem(
                            currentLineIndex,
                            with(density) { 72.dp.toPx().toInt() } + calculateOffset()
                        )
                    } else {
                        val viewportStartOffset = lazyListState.layoutInfo.viewportStartOffset
                        val viewportEndOffset = lazyListState.layoutInfo.viewportEndOffset
                        val currentLineOffset =
                            visibleItemsInfo.find { it.index == currentLineIndex }?.offset ?: 0
                        val previousLineOffset =
                            visibleItemsInfo.find { it.index == previousLineIndex }?.offset ?: 0

                        val centerRangeStart =
                            viewportStartOffset + (viewportEndOffset - viewportStartOffset) / 2
                        val centerRangeEnd =
                            viewportEndOffset - (viewportEndOffset - viewportStartOffset) / 8

                        if (currentLineOffset in centerRangeStart..centerRangeEnd ||
                            previousLineOffset in centerRangeStart..centerRangeEnd
                        ) {
                            lazyListState.animateScrollToItem(
                                currentLineIndex,
                                with(density) { 36.dp.toPx().toInt() } + calculateOffset()
                            )
                        }
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
    )  {
        if (isFullscreen) {
            BackHandler(enabled = true) {
                onNavigateBack?.invoke()
            }

            mediaMetadata?.let { metadata ->
                if (playerBackground == PlayerBackgroundStyle.BLUR) {
                    AsyncImage(
                        model = metadata.thumbnailUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .blur(50.dp)
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.7f))
                    )
                }
            }

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
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
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
                                                    mediaMetadata?.artists?.joinToString { it.name } ?: ""
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
            }

            // Resto del código de controles...
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
                                        inactiveTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
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
                                        inactiveTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
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
                                                inactiveTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
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
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .aspectRatio(1f)
                                    .scale(imageScale)
                                    .clip(RoundedCornerShape(cornerRadius))
                            )
                        }
                    }
                }
            }

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
                            text = stringResource(R.string.selection_mode_active, selectedIndices.size),
                            color = MaterialTheme.colorScheme.inverseOnSurface,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }

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

                    // SOLUCIÓN: Mostrar shimmer solo cuando está cargando, no cuando lyrics es null
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
                            key = { index, item -> "${currentSongId}-$index-${item.time}" }
                        ) { index, item ->
                            val isSelected = selectedIndices.contains(index)
                            val isCurrentLine = index == displayedCurrentLineIndex && isSynced

                            val transition = updateTransition(isCurrentLine, label = "lyricLineTransition")

                            val scale by transition.animateFloat(
                                transitionSpec = {
                                    tween(
                                        durationMillis = 300,
                                        easing = FastOutSlowInEasing
                                    )
                                },
                                label = "scale"
                            ) { current -> if (current) 1.02f else 1f }

                            val textColorAnim by transition.animateColor(
                                transitionSpec = { tween(durationMillis = 250) },
                                label = "textColor"
                            ) { current ->
                                when {
                                    current && isFullscreen -> MaterialTheme.colorScheme.primary
                                    current -> MaterialTheme.colorScheme.primary
                                    isSelected && isFullscreen -> MaterialTheme.colorScheme.onPrimaryContainer
                                    isFullscreen -> MaterialTheme.colorScheme.onSurface
                                    else -> textColor
                                }
                            }

                            val nonCurrentAlpha by transition.animateFloat(
                                transitionSpec = { tween(durationMillis = 200) },
                                label = "alpha"
                            ) { current ->
                                if (current) 1f else if (isFullscreen) 0.7f else 0.6f
                            }

                            val elevation by transition.animateDp(
                                transitionSpec = { tween(durationMillis = 200) },
                                label = "elevation"
                            ) { current -> if (current) 2.dp else 0.dp }

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
                                                lazyListState.animateScrollToItem(
                                                    index,
                                                    with(density) {
                                                        (if (isFullscreen) 60.dp else 36.dp).toPx().toInt()
                                                    } +
                                                            with(density) {
                                                                val count = item.text.count { it == '\n' }
                                                                (if (landscapeOffset) 12.dp.toPx() else 16.dp.toPx()).toInt() * count
                                                            }
                                                )
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
                                    when {
                                        isSelected && isSelectionModeActive && isFullscreen ->
                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                        isSelected && isSelectionModeActive ->
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                        isCurrentLine && isFullscreen ->
                                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)
                                        else -> Color.Transparent
                                    }
                                )
                                .padding(
                                    horizontal = if (isFullscreen) 16.dp else 24.dp,
                                    vertical = 8.dp
                                )
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                    alpha = if (isSelectionModeActive && !isSelected && isFullscreen) 0.38f
                                    else nonCurrentAlpha
                                }
                                .shadow(
                                    elevation = elevation,
                                    shape = RoundedCornerShape(4.dp),
                                    clip = true,
                                    ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                )

                            Text(
                                text = item.text,
                                style = when {
                                    isCurrentLine && isFullscreen -> MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 20.sp,
                                        fontFamily = FontFamily.SansSerif,
                                        letterSpacing = 0.15.sp
                                    )
                                    isSelected && isFullscreen -> MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 16.sp,
                                        fontFamily = FontFamily.SansSerif
                                    )
                                    isFullscreen -> MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 14.sp,
                                        fontFamily = FontFamily.SansSerif
                                    )
                                    else -> MaterialTheme.typography.bodyLarge.copy(
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.SansSerif
                                    )
                                },
                                color = textColorAnim,
                                textAlign = when (lyricsTextPosition) {
                                    LyricsPosition.LEFT -> TextAlign.Left
                                    LyricsPosition.CENTER -> TextAlign.Center
                                    LyricsPosition.RIGHT -> TextAlign.Right
                                },
                                lineHeight = when {
                                    isCurrentLine && isFullscreen -> 24.sp
                                    isSelected && isFullscreen -> 20.sp
                                    isFullscreen -> 18.sp
                                    else -> 24.sp
                                },
                                modifier = itemModifier
                            )
                        }
                    }
                }

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

        if (!isFullscreen) {
            mediaMetadata?.let { metadata ->
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!isSelectionModeActive) {
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
                    }

                    if (isSelectionModeActive) {
                        IconButton(
                            onClick = {
                                isSelectionModeActive = false
                                selectedIndices.clear()
                            }
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.close),
                                contentDescription = null,
                                tint = textColor
                            )
                        }

                        Spacer(Modifier.width(8.dp))

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
                                            metadata.title,
                                            metadata.artists.joinToString { it.name }
                                        )
                                        showShareDialog = true
                                    }
                                    isSelectionModeActive = false
                                    selectedIndices.clear()
                                }
                            },
                            enabled = selectedIndices.isNotEmpty()
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.media3_icon_share),
                                contentDescription = stringResource(R.string.share_selected),
                                tint = if (selectedIndices.isNotEmpty()) textColor else textColor.copy(
                                    alpha = 0.5f
                                )
                            )
                        }
                    } else {
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

    if (showProgressDialog) {
        BasicAlertDialog(onDismissRequest = { /* Don't dismiss */ }) {
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
        val (lyricsText, songTitle, artists) = shareDialogData!!
        ShareLyricsDialog(
            lyricsText = lyricsText,
            songTitle = songTitle,
            artists = artists,
            mediaMetadata = mediaMetadata,
            onDismiss = { showShareDialog = false }
        )
    }

    if (showColorPickerDialog && shareDialogData != null) {
        val (lyricsText, songTitle, artists) = shareDialogData!!
        BasicAlertDialog(onDismissRequest = { showColorPickerDialog = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = "Personalizar imagen",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = { showColorPickerDialog = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancelar")
                        }

                        Button(
                            onClick = {
                                showColorPickerDialog = false
                                showProgressDialog = true

                                scope.launch {
                                    try {
                                        val image = ComposeToImage.createLyricsImage(
                                            context = context,
                                            coverArtUrl = mediaMetadata?.thumbnailUrl,
                                            songTitle = songTitle,
                                            artistName = artists,
                                            lyrics = lyricsText,
                                            width = (configuration.screenWidthDp * density.density).toInt(),
                                            height = (configuration.screenHeightDp * density.density).toInt(),
                                            backgroundColor = previewBackgroundColor.toArgb(),
                                            textColor = previewTextColor.toArgb(),
                                            secondaryTextColor = previewSecondaryTextColor.toArgb(),
                                        )
                                        val timestamp = System.currentTimeMillis()
                                        val filename = "lyrics_$timestamp"
                                        val uri = ComposeToImage.saveBitmapAsFile(context, image, filename)
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "image/png"
                                            putExtra(Intent.EXTRA_STREAM, uri)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "Share Lyrics"))
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                    } finally {
                                        showProgressDialog = false
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Compartir")
                        }
                    }
                }
            }
        }
    }
}

const val ANIMATE_SCROLL_DURATION = 300L
val LyricsPreviewTime = 2.seconds