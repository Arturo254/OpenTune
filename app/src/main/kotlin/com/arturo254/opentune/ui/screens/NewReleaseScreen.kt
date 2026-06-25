/*
 * OpenTune Project Original (2026)
 * Material 3 Expressive - New Release Screen
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.arturo254.opentune.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.HorizontalUncontainedCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.arturo254.opentune.LocalPlayerAwareWindowInsets
import com.arturo254.opentune.LocalPlayerConnection
import com.arturo254.opentune.R
import com.arturo254.opentune.innertube.models.AlbumItem
import com.arturo254.opentune.innertube.models.AlbumReleaseType
import com.arturo254.opentune.ui.component.IconButton
import com.arturo254.opentune.ui.component.LocalMenuState
import com.arturo254.opentune.ui.component.shimmer.ShimmerHost
import com.arturo254.opentune.ui.menu.YouTubeAlbumMenu
import com.arturo254.opentune.ui.utils.backToMain
import com.arturo254.opentune.viewmodels.NewReleaseUiState
import com.arturo254.opentune.viewmodels.NewReleaseViewModel

@OptIn(
    ExperimentalFoundationApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
)
@Composable
fun NewReleaseScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: NewReleaseViewModel = hiltViewModel(),
) {
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val uiState by viewModel.uiState.collectAsState()
    val albumsList by viewModel.albums.collectAsState(initial = emptyList())
    val singlesList by viewModel.singles.collectAsState(initial = emptyList())
    val epsList by viewModel.eps.collectAsState(initial = emptyList())

    val coroutineScope = rememberCoroutineScope()
    val systemBarsTopPadding =
        with(LocalDensity.current) { WindowInsets.systemBars.getTop(this).toDp() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
    ) {
        LazyColumn(
            contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            // Espacio TopAppBar
            item { Spacer(modifier = Modifier.height(72.dp)) }

            // ── Hero Stats ──────────────────────────────────────────────────
            item {
                Box(modifier = Modifier.fillMaxWidth()) {
                    AnimatedVisibility(
                        visible = uiState is NewReleaseUiState.Success,
                        enter = fadeIn(tween(500)) + slideInVertically(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMediumLow,
                            ),
                            initialOffsetY = { -it / 2 },
                        ),
                    ) {
                        NewReleaseHero(
                            albumCount = albumsList.size,
                            singleCount = singlesList.size,
                            epCount = epsList.size,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                        )
                    }
                }
            }

            item {
                Box(modifier = Modifier.fillMaxWidth()) {
                    AnimatedVisibility(visible = uiState is NewReleaseUiState.Loading) {
                        ShimmerHost {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(96.dp)
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                            )
                        }
                    }
                }
            }

            // ── Carrusel Principal Destacado ──────────────────────────────
            item {
                Box(modifier = Modifier.fillMaxWidth()) {
                    AnimatedVisibility(
                        visible = uiState is NewReleaseUiState.Success && albumsList.isNotEmpty(),
                        enter = fadeIn(tween(600)) + slideInVertically(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMediumLow,
                            ),
                            initialOffsetY = { -it / 2 },
                        ),
                    ) {
                        FeaturedCarousel(
                            albums = albumsList,
                            isPlaying = isPlaying,
                            mediaMetadata = mediaMetadata,
                            navController = navController,
                            haptic = haptic,
                            menuState = menuState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                        )
                    }
                }
            }

            // ── Sección Álbumes ──────────────────────────────────────────────
            item {
                ReleaseSectionHeader(
                    title = stringResource(R.string.albums),
                    count = if (uiState is NewReleaseUiState.Loading) null else albumsList.size,
                    iconRes = R.drawable.album,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = 16.dp,
                        bottom = 4.dp
                    ),
                )
            }
            item {
                Box(modifier = Modifier.fillMaxWidth()) {
                    AnimatedContent(
                        targetState = uiState is NewReleaseUiState.Loading,
                        transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(250)) },
                        label = "albums_carousel",
                    ) { isLoading ->
                        if (isLoading || albumsList.isEmpty()) {
                            CarouselShimmerRow()
                        } else {
                            ExpressiveCarousel(
                                items = albumsList,
                                releaseType = AlbumReleaseType.ALBUM,
                                isPlaying = isPlaying,
                                mediaMetadata = mediaMetadata,
                                navController = navController,
                                haptic = haptic,
                                menuState = menuState,
                            )
                        }
                    }
                }
            }

            item {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                )
            }

            // ── Sección Singles ─────────────────────────────────────────────
            item {
                ReleaseSectionHeader(
                    title = stringResource(R.string.singles),
                    count = if (uiState is NewReleaseUiState.Loading) null else singlesList.size,
                    iconRes = R.drawable.music_note,
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = 4.dp
                    ),
                )
            }
            item {
                Box(modifier = Modifier.fillMaxWidth()) {
                    AnimatedContent(
                        targetState = uiState is NewReleaseUiState.Loading,
                        transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(250)) },
                        label = "singles_carousel",
                    ) { isLoading ->
                        if (isLoading || singlesList.isEmpty()) {
                            CarouselShimmerRow()
                        } else {
                            ExpressiveCarousel(
                                items = singlesList,
                                releaseType = AlbumReleaseType.SINGLE,
                                isPlaying = isPlaying,
                                mediaMetadata = mediaMetadata,
                                navController = navController,
                                haptic = haptic,
                                menuState = menuState,
                            )
                        }
                    }
                }
            }

            item {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                )
            }

            // ── Sección EPs ─────────────────────────────────────────────────
            item {
                ReleaseSectionHeader(
                    title = "EPs",
                    count = if (uiState is NewReleaseUiState.Loading) null else epsList.size,
                    iconRes = R.drawable.queue_music,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = 4.dp
                    ),
                )
            }
            item {
                Box(modifier = Modifier.fillMaxWidth()) {
                    AnimatedContent(
                        targetState = uiState is NewReleaseUiState.Loading,
                        transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(250)) },
                        label = "eps_carousel",
                    ) { isLoading ->
                        if (isLoading || epsList.isEmpty()) {
                            CarouselShimmerRow()
                        } else {
                            ExpressiveCarousel(
                                items = epsList,
                                releaseType = AlbumReleaseType.EP,
                                isPlaying = isPlaying,
                                mediaMetadata = mediaMetadata,
                                navController = navController,
                                haptic = haptic,
                                menuState = menuState,
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            // ── Error / Empty ───────────────────────────────────────────────
            when (val state = uiState) {
                is NewReleaseUiState.Error -> item {
                    ErrorState(
                        message = state.throwable?.message
                            ?: stringResource(R.string.error_unknown),
                        onRetry = viewModel::retry,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 32.dp),
                    )
                }

                is NewReleaseUiState.Empty -> item {
                    EmptyState(
                        onRefresh = viewModel::retry,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 32.dp),
                    )
                }

                else -> Unit
            }
        }

        // TopAppBar flotante
        TopAppBar(
            modifier = Modifier.align(Alignment.TopCenter),
            title = { Text(stringResource(R.string.new_release_albums)) },
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
            scrollBehavior = scrollBehavior,
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// Hero Stats - M3 Expressive
// ═══════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun NewReleaseHero(
    albumCount: Int,
    singleCount: Int,
    epCount: Int,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HeroStat(
                count = albumCount,
                label = stringResource(R.string.albums),
                iconRes = R.drawable.album,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            VerticalDividerLine()
            HeroStat(
                count = singleCount,
                label = stringResource(R.string.singles),
                iconRes = R.drawable.music_note,
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            VerticalDividerLine()
            HeroStat(
                count = epCount,
                label = "EPs",
                iconRes = R.drawable.queue_music,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

@Composable
private fun HeroStat(
    count: Int,
    label: String,
    iconRes: Int,
    containerColor: Color,
    contentColor: Color,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = containerColor,
            modifier = Modifier.size(52.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
        AnimatedContent(
            targetState = count,
            transitionSpec = {
                (fadeIn(tween(300)) + slideInVertically { it / 2 }) togetherWith
                        fadeOut(tween(200))
            },
            label = "hero_count_$label",
        ) { c ->
            Text(
                text = c.toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun VerticalDividerLine() {
    Box(
        modifier = Modifier
            .height(52.dp)
            .width(1.dp)
            .background(
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                RoundedCornerShape(1.dp),
            ),
    )
}

// ═══════════════════════════════════════════════════════════════════════════
// Section Header — M3 Expressive pill style
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun ReleaseSectionHeader(
    title: String,
    count: Int?,
    iconRes: Int,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Surface(
            shape = CircleShape,
            color = containerColor,
            modifier = Modifier.size(36.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )

        AnimatedVisibility(visible = count != null && count > 0) {
            Surface(
                shape = RoundedCornerShape(50),
                color = containerColor,
            ) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = contentColor,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// Featured Carousel - M3 Expressive (Destacado)
// ═══════════════════════════════════════════════════════════════════════════

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalFoundationApi::class
)
@Composable
private fun FeaturedCarousel(
    albums: List<AlbumItem>,
    isPlaying: Boolean,
    mediaMetadata: com.arturo254.opentune.models.MediaMetadata?,
    navController: NavController,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback,
    menuState: com.arturo254.opentune.ui.component.MenuState,
    modifier: Modifier = Modifier,
) {
    val distinctItems = albums.take(5).distinctBy { it.id }
    val carouselState = rememberCarouselState { distinctItems.size }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Título de la sección destacada
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(28.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(R.drawable.star),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Text(
                text = "Lo más destacado",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        HorizontalUncontainedCarousel(
            state = carouselState,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(vertical = 8.dp),
            itemWidth = 280.dp,
            itemSpacing = 12.dp,
            contentPadding = PaddingValues(horizontal = 16.dp),
        ) { i ->
            val album = distinctItems[i]
            val isActive = mediaMetadata?.album?.id == album.id

            FeaturedCarouselCard(
                album = album,
                isActive = isActive,
                isPlaying = isPlaying,
                onClick = { navController.navigate("album/${album.id}") },
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    menuState.show {
                        YouTubeAlbumMenu(
                            albumItem = album,
                            navController = navController,
                            onDismiss = menuState::dismiss,
                        )
                    }
                },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun FeaturedCarouselCard(
    album: AlbumItem,
    isActive: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val elevation by animateFloatAsState(
        targetValue = if (isActive) 12f else 4f,
        animationSpec = spring(
            Spring.DampingRatioMediumBouncy,
            Spring.StiffnessMedium,
        ),
        label = "featured_elevation",
    )

    Card(
        modifier = Modifier
            .width(280.dp)
            .height(160.dp)
            .shadow(
                elevation = elevation.dp,
                shape = RoundedCornerShape(
                    topStart = 20.dp,
                    topEnd = 20.dp,
                    bottomStart = 16.dp,
                    bottomEnd = 16.dp
                ),
                clip = false
            )
            .clip(
                RoundedCornerShape(
                    topStart = 20.dp,
                    topEnd = 20.dp,
                    bottomStart = 16.dp,
                    bottomEnd = 16.dp
                )
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isActive) 4.dp else 2.dp
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            AsyncImage(
                model = album.thumbnail,
                contentDescription = album.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(
                        RoundedCornerShape(
                            topStart = 20.dp,
                            topEnd = 20.dp,
                            bottomStart = 16.dp,
                            bottomEnd = 16.dp
                        )
                    )
            )

            // Gradiente overlay para legibilidad
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.6f)
                            ),
                            startY = 0.5f
                        )
                    )
            )

            // Información superpuesta
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = album.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                album.artists?.firstOrNull()?.let { artist ->
                    Text(
                        text = artist.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                album.year?.let { year ->
                    Text(
                        text = year.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }

            // Badge "Destacado"
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
            ) {
                Text(
                    text = "★",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = isActive && isPlaying,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp),
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(R.drawable.play),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// Expressive Carousel - M3 Expressive con formas diferenciadas
// ═══════════════════════════════════════════════════════════════════════════

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalFoundationApi::class
)
@Composable
private fun ExpressiveCarousel(
    items: List<AlbumItem>,
    releaseType: AlbumReleaseType,
    isPlaying: Boolean,
    mediaMetadata: com.arturo254.opentune.models.MediaMetadata?,
    navController: NavController,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback,
    menuState: com.arturo254.opentune.ui.component.MenuState,
) {
    val distinctItems = items.distinctBy { it.id }
    val carouselState = rememberCarouselState { distinctItems.size }

    // Forma diferenciada según el tipo de lanzamiento
    val cardShape = when (releaseType) {
        AlbumReleaseType.ALBUM -> RoundedCornerShape(
            topStart = 20.dp,
            topEnd = 20.dp,
            bottomStart = 16.dp,
            bottomEnd = 16.dp
        )

        AlbumReleaseType.SINGLE -> RoundedCornerShape(
            topStart = 12.dp,
            topEnd = 24.dp,
            bottomStart = 24.dp,
            bottomEnd = 12.dp
        )

        AlbumReleaseType.EP -> RoundedCornerShape(
            topStart = 16.dp,
            topEnd = 16.dp,
            bottomStart = 20.dp,
            bottomEnd = 20.dp
        )
    }

    // Color de acento según el tipo
    val accentColor = when (releaseType) {
        AlbumReleaseType.ALBUM -> MaterialTheme.colorScheme.primary
        AlbumReleaseType.SINGLE -> MaterialTheme.colorScheme.tertiary
        AlbumReleaseType.EP -> MaterialTheme.colorScheme.secondary
    }

    HorizontalMultiBrowseCarousel(
        state = carouselState,
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(vertical = 8.dp),
        preferredItemWidth = 148.dp,
        itemSpacing = 10.dp,
        contentPadding = PaddingValues(horizontal = 16.dp),
    ) { i ->
        val album = distinctItems[i]
        val isActive = mediaMetadata?.album?.id == album.id

        ExpressiveCarouselCard(
            album = album,
            releaseType = releaseType,
            isActive = isActive,
            isPlaying = isPlaying,
            shape = cardShape,
            accentColor = accentColor,
            onClick = { navController.navigate("album/${album.id}") },
            onLongClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                menuState.show {
                    YouTubeAlbumMenu(
                        albumItem = album,
                        navController = navController,
                        onDismiss = menuState::dismiss,
                    )
                }
            },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ExpressiveCarouselCard(
    album: AlbumItem,
    releaseType: AlbumReleaseType,
    isActive: Boolean,
    isPlaying: Boolean,
    shape: RoundedCornerShape,
    accentColor: Color,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val elevation by animateFloatAsState(
        targetValue = if (isActive) 10f else 3f,
        animationSpec = spring(
            Spring.DampingRatioMediumBouncy,
            Spring.StiffnessMedium,
        ),
        label = "expressive_elevation",
    )

    Column(
        modifier = Modifier
            .width(148.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(148.dp)
                .shadow(
                    elevation = elevation.dp,
                    shape = shape,
                    clip = false
                )
                .clip(shape)
                .let { modifier ->
                    if (isActive) {
                        modifier.border(
                            width = 3.dp,
                            color = accentColor,
                            shape = shape
                        )
                    } else {
                        modifier
                    }
                }
        ) {
            AsyncImage(
                model = album.thumbnail,
                contentDescription = album.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shape)
            )

            // Badge según tipo de lanzamiento
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = accentColor.copy(alpha = 0.9f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                Text(
                    text = when (releaseType) {
                        AlbumReleaseType.ALBUM -> "LP"
                        AlbumReleaseType.SINGLE -> "S"
                        AlbumReleaseType.EP -> "EP"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }

            // Indicador de reproducción
            androidx.compose.animation.AnimatedVisibility(
                visible = isActive && isPlaying,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp),
            ) {
                Surface(
                    shape = CircleShape,
                    color = accentColor,
                    modifier = Modifier.size(24.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(R.drawable.play),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(12.dp),
                        )
                    }
                }
            }
        }

        Text(
            text = album.title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
            color = if (isActive) accentColor else MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
        )

        album.artists?.firstOrNull()?.let { artist ->
            Text(
                text = artist.name,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
            )
        }

        album.year?.let { year ->
            Text(
                text = year.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                maxLines = 1,
                textAlign = TextAlign.Center,
            )
        }

        Spacer(modifier = Modifier.height(4.dp))
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// Shimmer de carga para los carruseles
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun CarouselShimmerRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        repeat(4) {
            ShimmerHost {
                Column(
                    modifier = Modifier.width(148.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(148.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    )
                    Box(
                        modifier = Modifier
                            .width(96.dp)
                            .height(12.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    )
                    Box(
                        modifier = Modifier
                            .width(64.dp)
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// Error & Empty states — M3 Expressive
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.errorContainer,
            modifier = Modifier.size(64.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(R.drawable.error),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(32.dp),
                )
            }
        }
        Text(
            text = stringResource(R.string.error_unknown),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Button(
            onClick = onRetry,
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
        ) {
            Icon(
                painter = painterResource(R.drawable.replay),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.retry))
        }
    }
}

@Composable
private fun EmptyState(
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.size(64.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(R.drawable.library_music),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(32.dp),
                )
            }
        }
        Text(
            text = stringResource(R.string.no_results_found),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.SemiBold,
        )
        Button(
            onClick = onRefresh,
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ),
        ) {
            Icon(
                painter = painterResource(R.drawable.replay),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.refresh))
        }
    }
}