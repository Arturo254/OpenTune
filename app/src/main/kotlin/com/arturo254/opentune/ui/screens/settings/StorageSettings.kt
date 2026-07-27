/*
 * OpenTune Project Original (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.arturo254.opentune.ui.screens.settings

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil3.annotation.ExperimentalCoilApi
import coil3.imageLoader
import com.arturo254.opentune.LocalPlayerAwareWindowInsets
import com.arturo254.opentune.LocalPlayerConnection
import com.arturo254.opentune.R
import com.arturo254.opentune.canvas.CanvasCacheManager
import com.arturo254.opentune.constants.MaxCanvasCacheSizeKey
import com.arturo254.opentune.constants.MaxImageCacheSizeKey
import com.arturo254.opentune.constants.MaxSongCacheSizeKey
import com.arturo254.opentune.constants.SmartTrimmerKey
import com.arturo254.opentune.extensions.directorySizeBytes
import com.arturo254.opentune.extensions.tryOrNull
import com.arturo254.opentune.ui.component.ActionPromptDialog
import com.arturo254.opentune.ui.component.IconButton
import com.arturo254.opentune.ui.component.ListPreference
import com.arturo254.opentune.ui.component.PreferenceEntry
import com.arturo254.opentune.ui.component.SwitchPreference
import com.arturo254.opentune.ui.player.CanvasArtworkPlaybackCache
import com.arturo254.opentune.ui.utils.backToMain
import com.arturo254.opentune.ui.utils.formatFileSize
import com.arturo254.opentune.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(
    ExperimentalCoilApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class
)
@Composable
fun StorageSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val context = LocalContext.current
    val imageDiskCache = context.imageLoader.diskCache ?: return
    val playerCache = LocalPlayerConnection.current?.service?.playerCache ?: return
    val downloadCache = LocalPlayerConnection.current?.service?.downloadCache ?: return

    val downloadCacheDir = remember { context.filesDir.resolve("download") }
    val playerCacheDir = remember { context.filesDir.resolve("exoplayer") }

    val coroutineScope = rememberCoroutineScope()
    val (smartTrimmer, onSmartTrimmerChange) = rememberPreference(
        key = SmartTrimmerKey,
        defaultValue = false
    )
    val (maxImageCacheSize, onMaxImageCacheSizeChange) = rememberPreference(
        key = MaxImageCacheSizeKey,
        defaultValue = 512
    )
    val (maxSongCacheSize, onMaxSongCacheSizeChange) = rememberPreference(
        key = MaxSongCacheSizeKey,
        defaultValue = 1024
    )
    val (maxCanvasCacheSize, onMaxCanvasCacheSizeChange) = rememberPreference(
        key = MaxCanvasCacheSizeKey,
        defaultValue = 256,
    )
    var clearCacheDialog by remember { mutableStateOf(false) }
    var clearDownloads by remember { mutableStateOf(false) }
    var clearImageCacheDialog by remember { mutableStateOf(false) }
    var clearCanvasCacheDialog by remember { mutableStateOf(false) }

    var imageCacheSize by remember {
        mutableStateOf(imageDiskCache.size)
    }
    var playerCacheSize by remember {
        mutableStateOf(0L)
    }
    var downloadCacheSize by remember {
        mutableStateOf(0L)
    }
    var canvasCacheCount by remember {
        mutableStateOf(CanvasArtworkPlaybackCache.size())
    }
    var canvasVideoCacheCount by remember { mutableStateOf(0) }
    var canvasVideoCacheSize by remember { mutableStateOf(0L) }

    val imageCacheProgress by animateFloatAsState(
        targetValue = if (imageDiskCache.maxSize > 0) {
            (imageCacheSize.toFloat() / imageDiskCache.maxSize).coerceIn(0f, 1f)
        } else 0f,
        label = "imageCacheProgress",
    )
    val maxSongCacheSizeBytes = if (maxSongCacheSize > 0) maxSongCacheSize * 1024 * 1024L else 0L
    val playerCacheProgress by animateFloatAsState(
        targetValue = if (maxSongCacheSizeBytes > 0) {
            (playerCacheSize.toFloat() / maxSongCacheSizeBytes).coerceIn(0f, 1f)
        } else 0f,
        label = "playerCacheProgress",
    )
    val canvasCacheProgress by animateFloatAsState(
        targetValue = if (maxCanvasCacheSize > 0) {
            (canvasCacheCount.toFloat() / maxCanvasCacheSize).coerceIn(0f, 1f)
        } else 0f,
        label = "canvasCacheProgress",
    )
    val canvasVideoCacheProgress by animateFloatAsState(
        targetValue = if (maxCanvasCacheSize > 0) {
            (canvasVideoCacheCount.toFloat() / maxCanvasCacheSize).coerceIn(0f, 1f)
        } else 0f,
        label = "canvasVideoCacheProgress",
    )

    val isSmartTrimmerAvailable = maxImageCacheSize != 0 || maxSongCacheSize != 0
    LaunchedEffect(isSmartTrimmerAvailable) {
        if (!isSmartTrimmerAvailable && smartTrimmer) onSmartTrimmerChange(false)
    }

    LaunchedEffect(maxImageCacheSize) {
        if (maxImageCacheSize == 0) {
            coroutineScope.launch(Dispatchers.IO) {
                imageDiskCache.clear()
                com.arturo254.opentune.utils.ArtworkStorage.clear(context)
            }
        }
    }
    LaunchedEffect(maxSongCacheSize) {
        if (maxSongCacheSize == 0) {
            coroutineScope.launch(Dispatchers.IO) {
                playerCache.keys.forEach { key ->
                    playerCache.removeResource(key)
                }
            }
        }
    }
    LaunchedEffect(maxCanvasCacheSize) {
        CanvasArtworkPlaybackCache.setMaxSize(maxCanvasCacheSize)
        if (maxCanvasCacheSize == 0) {
            CanvasArtworkPlaybackCache.clear()
            coroutineScope.launch {
                CanvasCacheManager.clearCache()
            }
        }
    }

    LaunchedEffect(imageDiskCache) {
        while (isActive) {
            delay(500)
            imageCacheSize = imageDiskCache.size
        }
    }
    LaunchedEffect(playerCache, playerCacheDir) {
        while (isActive) {
            delay(500)
            playerCacheSize =
                withContext(Dispatchers.IO) {
                    val cacheSpace = tryOrNull { playerCache.cacheSpace } ?: 0L
                    if (cacheSpace == 0L) playerCacheDir.directorySizeBytes() else cacheSpace
                }
        }
    }
    LaunchedEffect(downloadCache, downloadCacheDir) {
        while (isActive) {
            delay(500)
            downloadCacheSize =
                withContext(Dispatchers.IO) {
                    val cacheSpace = tryOrNull { downloadCache.cacheSpace } ?: 0L
                    if (cacheSpace == 0L) downloadCacheDir.directorySizeBytes() else cacheSpace
                }
        }
    }
    LaunchedEffect(Unit) {
        while (isActive) {
            delay(500)
            canvasCacheCount = CanvasArtworkPlaybackCache.size()
        }
    }
    LaunchedEffect(Unit) {
        while (isActive) {
            delay(500)
            // Si no está inicializado, intentar inicializar
            if (!CanvasCacheManager.isInitialized()) {
                // La inicialización se hace en Application, pero por si acaso
            }
            canvasVideoCacheCount = CanvasCacheManager.getCacheCount()
            canvasVideoCacheSize = CanvasCacheManager.getCacheSize()
        }
    }

    // ✅ Definir los items de caché
    val cacheItems = listOf(
        CacheItem(
            id = "downloads",
            icon = R.drawable.ic_download,
            title = stringResource(R.string.downloaded_songs),
            description = stringResource(R.string.size_used, formatFileSize(downloadCacheSize)),
            progress = null,
            onClear = { clearDownloads = true },
            onConfigure = null
        ),
        CacheItem(
            id = "songs",
            icon = R.drawable.ic_music,
            title = stringResource(R.string.song_cache),
            description = if (maxSongCacheSize == -1) {
                stringResource(R.string.size_used, formatFileSize(playerCacheSize))
            } else {
                "${formatFileSize(playerCacheSize)} / ${formatFileSize(maxSongCacheSize * 1024 * 1024L)}"
            },
            progress = if (maxSongCacheSize > 0) playerCacheProgress else null,
            onClear = { clearCacheDialog = true },
            onConfigure = {
                ListPreference(
                    title = { Text(stringResource(R.string.max_cache_size)) },
                    selectedValue = maxSongCacheSize,
                    values = listOf(0, 128, 256, 512, 1024, 2048, 4096, 8192, -1),
                    valueText = {
                        when (it) {
                            0 -> stringResource(R.string.disable)
                            -1 -> stringResource(R.string.unlimited)
                            else -> formatFileSize(it * 1024 * 1024L)
                        }
                    },
                    onValueSelected = onMaxSongCacheSizeChange,
                )
            }
        ),
        CacheItem(
            id = "images",
            icon = R.drawable.image,
            title = stringResource(R.string.image_cache),
            description = if (maxImageCacheSize > 0) {
                "${formatFileSize(imageCacheSize)} / ${formatFileSize(imageDiskCache.maxSize)}"
            } else {
                stringResource(R.string.disable)
            },
            progress = if (maxImageCacheSize > 0) imageCacheProgress else null,
            onClear = { clearImageCacheDialog = true },
            onConfigure = {
                ListPreference(
                    title = { Text(stringResource(R.string.max_cache_size)) },
                    selectedValue = maxImageCacheSize,
                    values = listOf(0, 128, 256, 512, 1024, 2048, 4096, 8192),
                    valueText = {
                        when (it) {
                            0 -> stringResource(R.string.disable)
                            else -> formatFileSize(it * 1024 * 1024L)
                        }
                    },
                    onValueSelected = onMaxImageCacheSizeChange,
                )
            }
        ),
        CacheItem(
            id = "canvas",
            icon = R.drawable.motion_photos_on,
            title = stringResource(R.string.canvas_cache),
            description = if (maxCanvasCacheSize > 0) {
                stringResource(
                    R.string.canvas_cache_usage,
                    formatFileSize(canvasVideoCacheSize),
                    stringResource(R.string.canvas_cache_items, maxCanvasCacheSize)
                )
            } else {
                stringResource(R.string.disable)
            },
            progress = if (maxCanvasCacheSize > 0) canvasVideoCacheProgress else null,
            onClear = { clearCanvasCacheDialog = true },
            onConfigure = {
                ListPreference(
                    title = { Text(stringResource(R.string.max_cache_size)) },
                    selectedValue = maxCanvasCacheSize,
                    values = listOf(0, 64, 128, 256, 512, 1024),
                    valueText = {
                        when (it) {
                            0 -> stringResource(R.string.disable)
                            else -> stringResource(R.string.canvas_cache_items, it)
                        }
                    },
                    onValueSelected = onMaxCanvasCacheSizeChange,
                )
            }
        )
    )

    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
            .verticalScroll(rememberScrollState())
            .padding(12.dp)
    ) {
        Spacer(
            Modifier.windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Top)
            )
        )

        // ✅ Smart Trimmer con estilo MD3 Expressive
        SwitchPreference(
            title = { Text(stringResource(R.string.smart_trimmer)) },
            description = stringResource(R.string.smart_trimmer_description),
            checked = smartTrimmer && isSmartTrimmerAvailable,
            onCheckedChange = onSmartTrimmerChange,
            isEnabled = isSmartTrimmerAvailable,
        )

        // ✅ Tarjetas de caché con estilo MD3 Expressive
        cacheItems.forEach { item ->
            MD3ExpressiveCacheCard(
                icon = item.icon,
                title = item.title,
                description = item.description,
                progress = item.progress,
                onClear = item.onClear,
                configureContent = item.onConfigure
            )
        }

        // Diálogos de confirmación
        if (clearDownloads) {
            ActionPromptDialog(
                title = stringResource(R.string.clear_all_downloads),
                onDismiss = { clearDownloads = false },
                onConfirm = {
                    coroutineScope.launch(Dispatchers.IO) {
                        downloadCache.keys.forEach { key ->
                            downloadCache.removeResource(key)
                        }
                    }
                    clearDownloads = false
                },
                onCancel = { clearDownloads = false },
                content = {
                    Text(text = stringResource(R.string.clear_downloads_dialog))
                }
            )
        }

        if (clearCacheDialog) {
            ActionPromptDialog(
                title = stringResource(R.string.clear_song_cache),
                onDismiss = { clearCacheDialog = false },
                onConfirm = {
                    coroutineScope.launch(Dispatchers.IO) {
                        playerCache.keys.forEach { key ->
                            playerCache.removeResource(key)
                        }
                    }
                    clearCacheDialog = false
                },
                onCancel = { clearCacheDialog = false },
                content = {
                    Text(text = stringResource(R.string.clear_song_cache_dialog))
                }
            )
        }

        if (clearImageCacheDialog) {
            ActionPromptDialog(
                title = stringResource(R.string.clear_image_cache),
                onDismiss = { clearImageCacheDialog = false },
                onConfirm = {
                    coroutineScope.launch(Dispatchers.IO) {
                        imageDiskCache.clear()
                        com.arturo254.opentune.utils.ArtworkStorage.clear(context)
                    }
                    clearImageCacheDialog = false
                },
                onCancel = { clearImageCacheDialog = false },
                content = {
                    Text(text = stringResource(R.string.clear_image_cache_dialog))
                }
            )
        }

        if (clearCanvasCacheDialog) {
            ActionPromptDialog(
                title = stringResource(R.string.clear_canvas_cache),
                onDismiss = { clearCanvasCacheDialog = false },
                onConfirm = {
                    coroutineScope.launch {
                        CanvasArtworkPlaybackCache.clear()
                        CanvasCacheManager.clearCache()
                        clearCanvasCacheDialog = false
                    }
                },
                onCancel = { clearCanvasCacheDialog = false },
                content = {
                    Text(text = stringResource(R.string.clear_canvas_cache_dialog))
                }
            )
        }
    }

    TopAppBar(
        title = { Text(stringResource(R.string.storage)) },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain,
            ) {
                Icon(
                    painterResource(R.drawable.arrow_back),
                    contentDescription = null,
                )
            }
        },
        scrollBehavior = scrollBehavior
    )
}

// ============================================================
// MD3 Expressive Cache Card Component
// ============================================================

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MD3ExpressiveCacheCard(
    icon: Int,
    title: String,
    description: String,
    progress: Float?,
    onClear: () -> Unit,
    configureContent: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header con icono y título
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Icono circular con fondo
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(icon),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Botón de limpiar
                FilledTonalButton(
                    onClick = onClear,
                    modifier = Modifier,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.delete),
                        contentDescription = stringResource(R.string.clear),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(
                        text = stringResource(R.string.clear),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Barra de progreso (si existe)
            if (progress != null) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    LinearProgressIndicator(
                        progress = progress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        strokeCap = StrokeCap.Round
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${(progress * 100).toInt()}% utilizado",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (progress > 0.8f) {
                            AssistChip(
                                onClick = { },
                                label = {
                                    Text(
                                        text = "⚠️ Casi lleno",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(R.drawable.error),
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    labelColor = MaterialTheme.colorScheme.onErrorContainer,
                                    leadingIconContentColor = MaterialTheme.colorScheme.onErrorContainer
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }
            }

            // Configuración adicional (si existe)
            configureContent?.let {
                Spacer(modifier = Modifier.height(4.dp))
                it()
            }
        }
    }
}

// ============================================================
// Data Class para Cache Items
// ============================================================

private data class CacheItem(
    val id: String,
    val icon: Int,
    val title: String,
    val description: String,
    val progress: Float?,
    val onClear: () -> Unit,
    val onConfigure: (@Composable () -> Unit)? = null
)