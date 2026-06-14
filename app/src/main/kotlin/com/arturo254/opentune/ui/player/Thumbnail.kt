/*
 * OpenTune Project Original (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 *
 * Canvas multi-proveedor portado desde ViviMusic:
 *   AUTO: Apple Music HLS → OpenTune API → Tidal MP4
 *   Selector de fuente: CanvasSource enum (DataStore preference)
 *   Validación cliente: artist + title + album match antes de mostrar
 *   Cache LRU en memoria + persistencia JSON en disco
 */

package com.arturo254.opentune.ui.player

import android.annotation.SuppressLint
import android.content.Context
import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridItemInfo
import androidx.compose.foundation.lazy.grid.LazyGridLayoutInfo
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.ui.AspectRatioFrameLayout
import coil3.compose.AsyncImage
import com.arturo254.opentune.LocalPlayerConnection
import com.arturo254.opentune.R
import com.arturo254.opentune.canvas.CanvasArtwork
import com.arturo254.opentune.constants.CanvasSource
import com.arturo254.opentune.constants.CanvasSourceKey
import com.arturo254.opentune.constants.CropThumbnailToSquareKey
import com.arturo254.opentune.constants.HidePlayerThumbnailKey
import com.arturo254.opentune.constants.MaxCanvasCacheSizeKey
import com.arturo254.opentune.constants.OpenTuneCanvasKey
import com.arturo254.opentune.constants.PlayerBackgroundStyle
import com.arturo254.opentune.constants.PlayerBackgroundStyleKey
import com.arturo254.opentune.constants.PlayerDesignStyle
import com.arturo254.opentune.constants.PlayerDesignStyleKey
import com.arturo254.opentune.constants.PlayerHorizontalPadding
import com.arturo254.opentune.constants.SeekExtraSeconds
import com.arturo254.opentune.constants.SwipeThumbnailKey
import com.arturo254.opentune.constants.ThumbnailCornerRadiusKey
import com.arturo254.opentune.extensions.metadata
import com.arturo254.opentune.extensions.toMediaItem
import com.arturo254.opentune.ui.utils.highRes
import com.arturo254.opentune.utils.rememberEnumPreference
import com.arturo254.opentune.utils.rememberPreference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.io.File
import java.util.LinkedHashMap
import java.util.Locale
import kotlin.math.abs

// =============================================================================
// Cache de canvas — LRU en memoria + persistencia JSON en disco
// =============================================================================

object CanvasArtworkPlaybackCache {

    private const val DEFAULT_MAX_SIZE = 256
    private const val PERSIST_FILE = "canvas_artwork_cache.json"
    private const val PERSIST_DEBOUNCE_MS = 2_000L

    private val map = LinkedHashMap<String, CanvasArtwork>(DEFAULT_MAX_SIZE, 0.75f, true)

    @Volatile
    private var maxSize = DEFAULT_MAX_SIZE
    @Volatile private var cacheFile: File? = null

    private val persistScope = CoroutineScope(Dispatchers.IO)
    private var persistJob: Job? = null

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    private val mapSerializer = MapSerializer(String.serializer(), CanvasArtwork.serializer())

    fun init(context: Context) {
        cacheFile = File(context.filesDir, PERSIST_FILE)
        loadFromDisk()
    }

    @Synchronized
    fun get(key: String): CanvasArtwork? {
        if (maxSize <= 0) return null
        return map[key]
    }

    @Synchronized
    fun put(key: String, artwork: CanvasArtwork) {
        val limit = maxSize
        if (limit <= 0 || key.isBlank()) return
        map[key] = artwork
        while (map.size > limit) {
            val it = map.entries.iterator()
            if (it.hasNext()) {
                it.next(); it.remove()
            }
        }
        schedulePersist()
    }

    @Synchronized
    fun size(): Int = map.size

    @Synchronized
    fun clear() {
        map.clear()
        schedulePersist()
    }

    @Synchronized
    fun setMaxSize(value: Int) {
        maxSize = value.coerceAtLeast(0)
        if (maxSize == 0) {
            map.clear(); schedulePersist(); return
        }
        var evicted = false
        while (map.size > maxSize) {
            val it = map.entries.iterator()
            if (it.hasNext()) {
                it.next(); it.remove(); evicted = true
            } else break
        }
        if (evicted) schedulePersist()
    }

    @Synchronized
    private fun loadFromDisk() {
        val file = cacheFile ?: return
        if (!file.exists()) return
        try {
            val raw = file.readText()
            if (raw.isBlank()) return
            val restored = json.decodeFromString(mapSerializer, raw)
            map.clear()
            map.putAll(restored)
            while (maxSize > 0 && map.size > maxSize) {
                val it = map.entries.iterator()
                if (it.hasNext()) {
                    it.next(); it.remove()
                } else break
            }
            Timber.d("Canvas cache restaurado: ${map.size} entradas desde disco")
        } catch (e: Exception) {
            Timber.e(e, "No se pudo restaurar canvas cache desde disco")
            runCatching { file.delete() }
        }
    }

    private fun schedulePersist() {
        persistJob?.cancel()
        persistJob = persistScope.launch {
            delay(PERSIST_DEBOUNCE_MS)
            writeToDisk()
        }
    }

    private fun writeToDisk() {
        val file = cacheFile ?: return
        try {
            val snapshot: Map<String, CanvasArtwork>
            synchronized(this@CanvasArtworkPlaybackCache) { snapshot = LinkedHashMap(map) }
            val raw = json.encodeToString(mapSerializer, snapshot)
            file.writeText(raw)
        } catch (e: Exception) {
            Timber.e(e, "No se pudo persistir canvas cache en disco")
        }
    }
}

// =============================================================================
// Modelo de página del carrusel
// =============================================================================

private data class ThumbnailPage(
    val slotKey: String,
    val windowIndex: Int,
    val mediaItem: MediaItem,
)

// =============================================================================
// Composable principal
// =============================================================================

@SuppressLint("LocalContextGetResourceValueCall")
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Thumbnail(
    sliderPositionProvider: () -> Long?,
    modifier: Modifier = Modifier,
    isPlayerExpanded: Boolean = true,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val context = LocalContext.current
    val view = LocalView.current
    val layoutDirection = LocalLayoutDirection.current

    // --- Estados del reproductor ---
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val error by playerConnection.error.collectAsState()
    val queueTitle by playerConnection.queueTitle.collectAsState()
    val canSkipPrevious by playerConnection.canSkipPrevious.collectAsState()
    val canSkipNext by playerConnection.canSkipNext.collectAsState()

    // --- Preferencias ---
    val swipeThumbnail by rememberPreference(SwipeThumbnailKey, true)
    val hidePlayerThumbnail by rememberPreference(HidePlayerThumbnailKey, false)
    val canvasEnabled by rememberPreference(OpenTuneCanvasKey, false)
    val playerDesignStyle by rememberEnumPreference(PlayerDesignStyleKey, PlayerDesignStyle.V4)
    val (maxCanvasCacheSize, _) = rememberPreference(MaxCanvasCacheSizeKey, 256)
    val (thumbnailCornerRadius, _) = rememberPreference(ThumbnailCornerRadiusKey, 16f)
    val cropThumbnailToSquare by rememberPreference(CropThumbnailToSquareKey, false)
    val playerBackground by rememberEnumPreference(
        PlayerBackgroundStyleKey,
        PlayerBackgroundStyle.DEFAULT
    )


    val canvasSource by rememberEnumPreference(CanvasSourceKey, CanvasSource.AUTO)

    val textColor = when (playerBackground) {
        PlayerBackgroundStyle.DEFAULT -> MaterialTheme.colorScheme.onBackground
        else -> Color.White
    }

    LaunchedEffect(maxCanvasCacheSize) {
        CanvasArtworkPlaybackCache.setMaxSize(maxCanvasCacheSize)
    }

    // --- Carrusel ---
    val thumbnailLazyGridState = rememberLazyGridState()

    val timeline = playerConnection.player.currentTimeline
    val currentIndex = playerConnection.player.currentMediaItemIndex
    val shuffleModeEnabled = playerConnection.player.shuffleModeEnabled

    val previousWindowIndex = if (swipeThumbnail && !timeline.isEmpty) {
        timeline.getPreviousWindowIndex(currentIndex, Player.REPEAT_MODE_OFF, shuffleModeEnabled)
    } else C.INDEX_UNSET

    val nextWindowIndex = if (swipeThumbnail && !timeline.isEmpty) {
        timeline.getNextWindowIndex(currentIndex, Player.REPEAT_MODE_OFF, shuffleModeEnabled)
    } else C.INDEX_UNSET

    val previousMediaItem = if (previousWindowIndex != C.INDEX_UNSET) {
        runCatching { playerConnection.player.getMediaItemAt(previousWindowIndex) }.getOrNull()
    } else null

    val nextMediaItem = if (nextWindowIndex != C.INDEX_UNSET) {
        runCatching { playerConnection.player.getMediaItemAt(nextWindowIndex) }.getOrNull()
    } else null

    val currentMediaItem = remember(mediaMetadata) {
        mediaMetadata?.toMediaItem()
            ?: runCatching { playerConnection.player.currentMediaItem }.getOrNull()
    }

    val thumbnailPages = buildList {
        if (previousMediaItem != null)
            add(ThumbnailPage("previous", previousWindowIndex, previousMediaItem))
        if (currentMediaItem != null)
            add(ThumbnailPage("current", currentIndex, currentMediaItem))
        if (nextMediaItem != null)
            add(ThumbnailPage("next", nextWindowIndex, nextMediaItem))
    }
    val currentMediaIndex = thumbnailPages.indexOfFirst { it.slotKey == "current" }

    // Snap
    val snapProvider = remember(thumbnailLazyGridState) {
        SnapLayoutInfoProvider(
            lazyGridState = thumbnailLazyGridState,
            positionInLayout = { layoutSize, itemSize -> layoutSize / 2f - itemSize / 2f },
            velocityThreshold = 500f,
        )
    }

    val currentItem by remember { derivedStateOf { thumbnailLazyGridState.firstVisibleItemIndex } }
    val itemScrollOffset by remember { derivedStateOf { thumbnailLazyGridState.firstVisibleItemScrollOffset } }

    // Swipe para cambiar canción
    LaunchedEffect(itemScrollOffset) {
        if (!thumbnailLazyGridState.isScrollInProgress || !swipeThumbnail
            || itemScrollOffset != 0 || currentMediaIndex < 0
        ) return@LaunchedEffect
        when {
            currentItem > currentMediaIndex && canSkipNext -> {
                playerConnection.player.seekToNext()
                view.performHapticFeedback(HapticFeedbackConstants.GESTURE_END)
            }

            currentItem < currentMediaIndex && canSkipPrevious -> {
                playerConnection.player.seekToPreviousMediaItem()
                view.performHapticFeedback(HapticFeedbackConstants.GESTURE_END)
            }
        }
    }

    LaunchedEffect(mediaMetadata, currentMediaItem?.mediaId, canSkipPrevious, canSkipNext) {
        val index = maxOf(0, currentMediaIndex)
        if (index < thumbnailPages.size) {
            runCatching { thumbnailLazyGridState.animateScrollToItem(index) }
                .onFailure { thumbnailLazyGridState.scrollToItem(index) }
        }
    }

    LaunchedEffect(playerConnection.player.currentMediaItemIndex, currentMediaItem?.mediaId) {
        val index = currentMediaIndex
        if (index >= 0 && index != currentItem) thumbnailLazyGridState.scrollToItem(index)
    }

    // Seek por doble tap
    var showSeekEffect by remember { mutableStateOf(false) }
    var seekDirection by remember { mutableStateOf("") }

    Box(modifier = modifier) {
        // Vista de error
        AnimatedVisibility(
            visible = error != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .padding(32.dp)
                .align(Alignment.Center),
        ) {
            error?.let { playbackError ->
                PlaybackError(
                    error = playbackError,
                    mediaId = currentMediaItem?.mediaId,
                    retry = {
                        playerConnection.player.prepare()
                        playerConnection.player.play()
                    },
                )
            }
        }

        // Vista principal
        AnimatedVisibility(
            visible = error == null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Encabezado "Now Playing"
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 32.dp, vertical = 16.dp),
                ) {
                    Text(
                        text = stringResource(R.string.now_playing),
                        style = MaterialTheme.typography.titleMedium,
                        color = textColor,
                    )
                    val playingFrom = queueTitle ?: mediaMetadata?.album?.title
                    if (!playingFrom.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = playingFrom,
                            style = MaterialTheme.typography.titleMedium,
                            color = textColor.copy(alpha = 0.8f),
                            maxLines = 1,
                            modifier = Modifier.basicMarquee(),
                        )
                    }
                }

                // Carrusel de thumbnails
                BoxWithConstraints(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    val itemWidth = maxWidth
                    val containerSize = maxWidth - (PlayerHorizontalPadding * 2)

                    LazyHorizontalGrid(
                        state = thumbnailLazyGridState,
                        rows = GridCells.Fixed(1),
                        flingBehavior = rememberSnapFlingBehavior(snapProvider),
                        userScrollEnabled = swipeThumbnail && isPlayerExpanded,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(
                            items = thumbnailPages,
                            key = { page ->
                                "${page.slotKey}:${page.windowIndex}:${page.mediaItem.mediaId.ifEmpty { "unknown" }}"
                            },
                            contentType = { "thumbnailPage" },
                        ) { page ->
                            val item = page.mediaItem
                            val incrementalSeekEnabled by rememberPreference(
                                SeekExtraSeconds,
                                false
                            )
                            var skipMultiplier by remember { mutableStateOf(1) }
                            var lastTapTime by remember { mutableLongStateOf(0L) }

                            val itemMetadata = remember(item) { item.metadata }
                            val storefront = remember {
                                Locale.getDefault().country
                                    .takeIf { it.length == 2 }
                                    ?.lowercase(Locale.ROOT) ?: "us"
                            }

                            // Solo animar canvas para el ítem actual y si canvas está habilitado
                            val shouldAnimateCanvas = canvasEnabled
                                    && playerDesignStyle != PlayerDesignStyle.V7
                                    && item.mediaId.isNotBlank()
                                    && item.mediaId == currentMediaItem?.mediaId

                            // Clave de cache incluye la fuente para separar resultados por proveedor
                            val cacheKey = "${item.mediaId}:${canvasSource.name}"

                            var canvasArtwork by remember(item.mediaId) { mutableStateOf<CanvasArtwork?>(null) }
                            var canvasFetchInFlight by remember(item.mediaId) { mutableStateOf(false) }

                            // Resetear canvas cuando se deshabilita
                            LaunchedEffect(shouldAnimateCanvas) {
                                if (!shouldAnimateCanvas) {
                                    canvasArtwork = null
                                    canvasFetchInFlight = false
                                }
                            }

                            // Fetch de canvas con cache + multi-proveedor
                            LaunchedEffect(shouldAnimateCanvas, item.mediaId, canvasSource) {
                                if (!shouldAnimateCanvas) return@LaunchedEffect

                                // 1. Revisar cache
                                CanvasArtworkPlaybackCache.get(cacheKey)?.let { cached ->
                                    canvasArtwork = cached
                                    return@LaunchedEffect
                                }

                                // 2. Extraer metadatos
                                val songTitleRaw = itemMetadata?.title?.takeIf { it.isNotBlank() }
                                    ?: item.mediaMetadata.title?.toString()
                                    ?: return@LaunchedEffect

                                val artistNameRaw = itemMetadata?.artists?.firstOrNull()?.name
                                    ?.takeIf { it.isNotBlank() }
                                    ?: item.mediaMetadata.artist?.toString()
                                    ?: item.mediaMetadata.subtitle?.toString()
                                    ?: ""

                                val albumNameRaw = itemMetadata?.album?.title
                                    ?: item.mediaMetadata.albumTitle?.toString()

                                if (canvasFetchInFlight) return@LaunchedEffect
                                canvasFetchInFlight = true

                                val fetched = withContext(Dispatchers.IO) {
                                    fetchCanvasArtworkForPlayback(
                                        songTitleRaw = songTitleRaw,
                                        artistNameRaw = artistNameRaw,
                                        albumName = albumNameRaw,
                                        storefront = storefront,
                                        source = canvasSource,
                                    )
                                }

                                // 3. Validación cliente — evita canvas incorrecto
                                val validated = fetched?.let { artwork ->
                                    val reqArtist = artistNameRaw
                                    val reqTitle = songTitleRaw
                                    val reqAlbum = albumNameRaw ?: ""

                                    val artistOk = artwork.artist?.let { resultArtist ->
                                        if (reqArtist.isBlank()) true
                                        else {
                                            val reqList = splitAndNormalizeArtists(reqArtist)
                                            val resList = splitAndNormalizeArtists(resultArtist)
                                            reqList.any { req ->
                                                resList.any {
                                                    it.contains(req) || req.contains(
                                                        it
                                                    )
                                                }
                                            }
                                        }
                                    } ?: true

                                    val titleOk = artwork.name?.let { resultName ->
                                        if (reqTitle.isBlank()) true
                                        else resultName.trim()
                                            .equals(reqTitle.trim(), ignoreCase = true)
                                    } ?: true

                                    val albumOk = artwork.albumName?.let { resultAlbum ->
                                        if (reqAlbum.isBlank()) true
                                        else resultAlbum.trim()
                                            .equals(reqAlbum.trim(), ignoreCase = true)
                                    } ?: true

                                    Timber.d("CanvasValidation: artist=$artistOk, title=$titleOk, album=$albumOk | '${artwork.name}' by '${artwork.artist}'")

                                    // Requiere coincidencia de artista; título y álbum son opcionales
                                    // si el proveedor no devuelve esa info
                                    if (artistOk && (titleOk || artwork.name == null) && (albumOk || artwork.albumName == null)) {
                                        artwork
                                    } else null
                                }

                                canvasArtwork = validated
                                if (validated != null) {
                                    CanvasArtworkPlaybackCache.put(cacheKey, validated)
                                }
                                canvasFetchInFlight = false
                            }

                            // --- UI del ítem ---
                            Box(
                                modifier = Modifier
                                    .width(itemWidth)
                                    .fillMaxSize()
                                    .padding(horizontal = PlayerHorizontalPadding)
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onDoubleTap = { offset ->
                                                val pos = playerConnection.player.currentPosition
                                                val dur = playerConnection.player.duration
                                                val now = System.currentTimeMillis()

                                                if (incrementalSeekEnabled && now - lastTapTime < 1_000) {
                                                    skipMultiplier++
                                                } else {
                                                    skipMultiplier = 1
                                                }
                                                lastTapTime = now

                                                val skip = 5_000 * skipMultiplier
                                                val isLeft =
                                                    (layoutDirection == LayoutDirection.Ltr && offset.x < size.width / 2) ||
                                                            (layoutDirection == LayoutDirection.Rtl && offset.x > size.width / 2)

                                                if (isLeft) {
                                                    playerConnection.player.seekTo(
                                                        (pos - skip).coerceAtLeast(
                                                            0
                                                        )
                                                    )
                                                    seekDirection = context.getString(
                                                        R.string.seek_backward_dynamic,
                                                        skip / 1000
                                                    )
                                                } else {
                                                    playerConnection.player.seekTo(
                                                        (pos + skip).coerceAtMost(
                                                            dur
                                                        )
                                                    )
                                                    seekDirection = context.getString(
                                                        R.string.seek_forward_dynamic,
                                                        skip / 1000
                                                    )
                                                }
                                                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                                showSeekEffect = true
                                            },
                                        )
                                    },
                                contentAlignment = Alignment.Center,
                            ) {

                                Box(
                                    modifier = Modifier
                                        .size(containerSize)
                                        .clip(RoundedCornerShape(thumbnailCornerRadius.dp)),
                                ) {
                                    if (hidePlayerThumbnail) {
                                        // Placeholder cuando se oculta el thumbnail
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(MaterialTheme.colorScheme.surfaceVariant),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.opentune),
                                                contentDescription = stringResource(R.string.hide_player_thumbnail),
                                                tint = textColor.copy(alpha = 0.7f),
                                                modifier = Modifier.size(120.dp),
                                            )
                                        }
                                    } else {
                                        val artworkUrl = itemMetadata?.thumbnailUrl?.highRes()
                                            ?: item.mediaMetadata.artworkUri?.toString()

                                        val shouldCrop = cropThumbnailToSquare
                                                && playerDesignStyle != PlayerDesignStyle.V7

                                        // Fondo desenfocado (blur)
                                        AsyncImage(
                                            model = artworkUrl,
                                            contentDescription = null,
                                            contentScale = ContentScale.FillBounds,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .let { if (shouldCrop) it.aspectRatio(1f) else it }
                                                .graphicsLayer(
                                                    renderEffect = BlurEffect(60f, 60f),
                                                    alpha = 0.6f,
                                                ),
                                        )

                                        // Artwork principal
                                        AsyncImage(
                                            model = artworkUrl,
                                            contentDescription = null,
                                            contentScale = if (shouldCrop) ContentScale.Crop else ContentScale.Fit,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .let { if (shouldCrop) it.aspectRatio(1f) else it },
                                        )

                                        // Canvas animado (superpuesto sobre el artwork)
                                        val primaryUrl = canvasArtwork?.animated
                                        val fallbackUrl = canvasArtwork?.videoUrl
                                        if (shouldAnimateCanvas
                                            && (!primaryUrl.isNullOrBlank() || !fallbackUrl.isNullOrBlank())
                                        ) {
                                            CanvasArtworkPlayer(
                                                primaryUrl = primaryUrl,
                                                fallbackUrl = fallbackUrl,
                                                isPlaying = isPlaying,
                                                modifier = Modifier.fillMaxSize(),
                                                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Efecto visual de seek
        LaunchedEffect(showSeekEffect) {
            if (showSeekEffect) {
                delay(1_000); showSeekEffect = false
            }
        }

        AnimatedVisibility(
            visible = showSeekEffect,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center),
        ) {
            Text(
                text = seekDirection,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                    .padding(8.dp),
            )
        }
    }
}

// =============================================================================
// SnapLayoutInfoProvider (tomado de OuterTune / ViviMusic)
// =============================================================================

@ExperimentalFoundationApi
fun SnapLayoutInfoProvider(
    lazyGridState: LazyGridState,
    positionInLayout: (layoutSize: Float, itemSize: Float) -> Float =
        { layoutSize, itemSize -> layoutSize / 2f - itemSize / 2f },
    velocityThreshold: Float = 1_000f,
): SnapLayoutInfoProvider = object : SnapLayoutInfoProvider {

    private val layoutInfo: LazyGridLayoutInfo get() = lazyGridState.layoutInfo

    override fun calculateApproachOffset(velocity: Float, decayOffset: Float) = 0f

    override fun calculateSnapOffset(velocity: Float): Float {
        val bounds = snappingBounds()
        if (abs(velocity) < velocityThreshold) {
            return if (abs(bounds.start) < abs(bounds.endInclusive)) bounds.start
            else bounds.endInclusive
        }
        return when {
            velocity < 0 -> bounds.start
            velocity > 0 -> bounds.endInclusive
            else -> 0f
        }
    }

    private fun snappingBounds(): ClosedFloatingPointRange<Float> {
        var lower = Float.NEGATIVE_INFINITY
        var upper = Float.POSITIVE_INFINITY
        layoutInfo.visibleItemsInfo.fastForEach { item ->
            val offset = distanceToSnap(layoutInfo, item, positionInLayout)
            if (offset <= 0 && offset > lower) lower = offset
            if (offset >= 0 && offset < upper) upper = offset
        }
        return lower.rangeTo(upper)
    }
}

private fun distanceToSnap(
    layoutInfo: LazyGridLayoutInfo,
    item: LazyGridItemInfo,
    positionInLayout: (Float, Float) -> Float,
): Float {
    val containerSize = layoutInfo.singleAxisViewportSize -
            layoutInfo.beforeContentPadding - layoutInfo.afterContentPadding
    val desired = positionInLayout(containerSize.toFloat(), item.size.width.toFloat())
    return item.offset.x.toFloat() - desired
}

private val LazyGridLayoutInfo.singleAxisViewportSize: Int
    get() = if (orientation == Orientation.Vertical) viewportSize.height else viewportSize.width
