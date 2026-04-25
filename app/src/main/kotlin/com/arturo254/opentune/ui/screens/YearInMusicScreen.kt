/*
 * OpenTune Insight — Year in Music, redesigned (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.arturo254.opentune.ui.screens

import android.content.Intent
import android.view.View
import android.view.ViewTreeObserver
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.allowHardware
import com.arturo254.opentune.LocalPlayerAwareWindowInsets
import com.arturo254.opentune.LocalPlayerConnection
import com.arturo254.opentune.R
import com.arturo254.opentune.db.entities.Album
import com.arturo254.opentune.db.entities.Artist
import com.arturo254.opentune.db.entities.Song
import com.arturo254.opentune.db.entities.SongWithStats
import com.arturo254.opentune.ui.component.IconButton
import com.arturo254.opentune.ui.component.LocalMenuState
import com.arturo254.opentune.ui.menu.ArtistMenu
import com.arturo254.opentune.ui.menu.SongMenu
import com.arturo254.opentune.ui.utils.backToMain
import com.arturo254.opentune.utils.ComposeToImage
import com.arturo254.opentune.utils.joinByBullet
import com.arturo254.opentune.utils.makeTimeString
import com.arturo254.opentune.viewmodels.YearInMusicViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// ─────────────────────────────────────────────────────────────────────────────
// Design tokens
// ─────────────────────────────────────────────────────────────────────────────

private val Ink      = Color(0xFF040406)
private val Snow     = Color(0xFFFDFDFD)
private val SnowMid  = Color(0xB3FDFDFD)
private val SnowDim  = Color(0x55FDFDFD)
private val GlassHi  = Color(0x28FFFFFF)
private val GlassBorder = Color(0x18FFFFFF)

// Per-card gradient palettes ─ each card has its own visual identity
private val WelcomeA  = Color(0xFF110024)
private val WelcomeB  = Color(0xFF8A1FFF)
private val WelcomeC  = Color(0xFFFB3E7E)

private val MinutesA  = Color(0xFF000E30)
private val MinutesB  = Color(0xFF1560FF)
private val MinutesC  = Color(0xFF00CFFF)

private val SongsA    = Color(0xFF001409)
private val SongsB    = Color(0xFF1DB954)
private val SongsC    = Color(0xFFB3FF6E)

private val SpotA     = Color(0xFF200028)
private val SpotB     = Color(0xFFFF2D78)

private val ArtistA   = Color(0xFF0F001E)
private val ArtistB   = Color(0xFFBF00FF)
private val ArtistC   = Color(0xFF5500CC)

private val AlbumA    = Color(0xFF170900)
private val AlbumB    = Color(0xFFFF7A00)
private val AlbumC    = Color(0xFFFFD000)

private val PersonA   = Color(0xFF001918)
private val PersonB   = Color(0xFF00E5C3)

private val SummaryA  = Color(0xFF0E0018)
private val SummaryB  = Color(0xFF6A0DAD)
private val SummaryC  = Color(0xFF1560FF)
private val SummaryD  = Color(0xFF1DB954)

// ─────────────────────────────────────────────────────────────────────────────
// Listening Personality
// ─────────────────────────────────────────────────────────────────────────────

private data class InsightPersonality(
    val emoji: String,
    val title: String,
    val description: String,
    val gradTop: Color,
    val gradBot: Color,
    val accent: Color,
)

private val PersonalityDevoted = InsightPersonality(
    emoji = "🎯",
    title = "Devoted Fan",
    description = "When you find a song you love, you play it on repeat. Your loyalty to your favourites is unmatched — and that's your superpower.",
    gradTop = PersonA, gradBot = PersonB, accent = PersonB,
)
private val PersonalityExplorer = InsightPersonality(
    emoji = "🧭",
    title = "Music Explorer",
    description = "Always discovering, never settling. Your taste spans worlds, and your playlist never sounds the same twice.",
    gradTop = WelcomeA, gradBot = WelcomeB, accent = WelcomeB,
)
private val PersonalityAudiophile = InsightPersonality(
    emoji = "🎧",
    title = "True Audiophile",
    description = "Hours melt away when you're in the zone. Music isn't background noise for you — it's everything.",
    gradTop = MinutesA, gradBot = MinutesB, accent = MinutesC,
)
private val PersonalityCasual = InsightPersonality(
    emoji = "🌊",
    title = "Laid-Back Listener",
    description = "Music moves effortlessly with your life. It's always there when you need it, easy and perfectly in tune.",
    gradTop = SongsA, gradBot = SongsB, accent = SongsC,
)

private fun computePersonality(
    topSongs: List<SongWithStats>,
    totalPlayed: Long,
    totalTimeMs: Long,
): InsightPersonality {
    if (topSongs.isEmpty()) return PersonalityCasual
    val topRatio      = topSongs.first().songCountListened.toFloat() / totalPlayed.coerceAtLeast(1)
    val hoursListened = totalTimeMs / 3_600_000L
    return when {
        topRatio >= 0.25f   -> PersonalityDevoted
        hoursListened >= 50 -> PersonalityAudiophile
        topSongs.size >= 5  -> PersonalityExplorer
        else                -> PersonalityCasual
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Page enum
// ─────────────────────────────────────────────────────────────────────────────

private enum class InsightPage {
    Welcome, Minutes, TopSongsList, SongSpotlight,
    ArtistSpotlight, TopAlbumsList, Personality, Summary,
}

// ─────────────────────────────────────────────────────────────────────────────
// Main screen
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun YearInMusicScreen(
    navController: NavController,
    viewModel: YearInMusicViewModel = hiltViewModel(),
) {
    val context           = LocalContext.current
    val menuState         = LocalMenuState.current
    val haptic            = LocalHapticFeedback.current
    val playerConnection  = LocalPlayerConnection.current ?: return
    val coroutineScope    = rememberCoroutineScope()
    val view              = LocalView.current

    val availableYears      by viewModel.availableYears.collectAsState()
    val selectedYear        by viewModel.selectedYear.collectAsState()
    val topSongsStats       by viewModel.topSongsStats.collectAsState()
    val topSongs            by viewModel.topSongs.collectAsState()
    val topArtists          by viewModel.topArtists.collectAsState()
    val topAlbums           by viewModel.topAlbums.collectAsState()
    val totalListeningTime  by viewModel.totalListeningTime.collectAsState()
    val totalSongsPlayed    by viewModel.totalSongsPlayed.collectAsState()

    var isGeneratingImage   by remember { mutableStateOf(false) }
    var isShareCaptureMode  by remember { mutableStateOf(false) }
    var shareBounds         by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
    var isYearPickerOpen    by remember { mutableStateOf(false) }

    val shareArgb = Ink.toArgb()

    // Build page list dynamically based on available data
    val pages = remember(topSongsStats, topArtists, topAlbums, totalListeningTime, totalSongsPlayed) {
        buildList {
            add(InsightPage.Welcome)
            if (totalListeningTime > 0 || totalSongsPlayed > 0) add(InsightPage.Minutes)
            if (topSongsStats.isNotEmpty()) {
                add(InsightPage.TopSongsList)
                add(InsightPage.SongSpotlight)
            }
            if (topArtists.isNotEmpty()) add(InsightPage.ArtistSpotlight)
            if (topAlbums.isNotEmpty()) add(InsightPage.TopAlbumsList)
            if (topSongsStats.isNotEmpty()) add(InsightPage.Personality)
            add(InsightPage.Summary)
        }
    }

    val pagerState = rememberPagerState { pages.size }
    val currentPage = pagerState.currentPage
    val isLastPage  = currentPage == pages.lastIndex
    val hasData     = topSongsStats.isNotEmpty() || topArtists.isNotEmpty() || topAlbums.isNotEmpty()

    // Jump to summary when capturing share screenshot
    LaunchedEffect(isShareCaptureMode) {
        if (isShareCaptureMode) pagerState.scrollToPage(pages.lastIndex)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink)
            .onGloballyPositioned { shareBounds = it.boundsInRoot() }
    ) {

        // ── Story pager ────────────────────────────────────────────────────
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = !isShareCaptureMode,
            beyondViewportPageCount = 1,
        ) { pageIndex ->
            InsightPageContent(
                page               = pages.getOrNull(pageIndex) ?: InsightPage.Summary,
                year               = selectedYear,
                totalListeningTime = totalListeningTime,
                totalSongsPlayed   = totalSongsPlayed,
                topSongsStats      = topSongsStats,
                topSongs           = topSongs,
                topArtists         = topArtists,
                topAlbums          = topAlbums,
                menuState          = menuState,
                haptic             = haptic,
                navController      = navController,
                coroutineScope     = coroutineScope,
                modifier           = Modifier.fillMaxSize(),
            )
        }

        // ── Instagram-style tap zones (left = back, right = forward) ───────
        if (!isShareCaptureMode) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 110.dp) // below top chrome
                    .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Bottom))
                    .padding(bottom = 56.dp) // above bottom edge
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(0.28f)
                        .pointerInput(currentPage) {
                            detectTapGestures {
                                if (currentPage > 0) {
                                    coroutineScope.launch { pagerState.animateScrollToPage(currentPage - 1) }
                                }
                            }
                        }
                )
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(0.72f)
                        .pointerInput(currentPage) {
                            detectTapGestures {
                                if (currentPage < pages.lastIndex) {
                                    coroutineScope.launch { pagerState.animateScrollToPage(currentPage + 1) }
                                }
                            }
                        }
                )
            }
        }

        // ── Top chrome: progress bar + nav ─────────────────────────────────
        if (!isShareCaptureMode) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .zIndex(3f)
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Black.copy(alpha = 0.55f),
                            1f to Color.Transparent
                        )
                    )
                    .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Top))
                    .padding(top = 6.dp)
            ) {
                // Story-style progress segments
                InsightProgressBar(
                    totalPages  = pages.size,
                    currentPage = currentPage,
                    modifier    = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick   = navController::navigateUp,
                        onLongClick = navController::backToMain,
                    ) {
                        Icon(painterResource(R.drawable.arrow_back), null, tint = Snow)
                    }
                    Spacer(Modifier.weight(1f))
                    // Brand label — centered
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text       = "OpenTune",
                            style      = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color      = SnowDim,
                            letterSpacing = 1.5.sp,
                        )
                        Text(
                            text       = "Insight",
                            style      = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color      = Snow,
                            letterSpacing = 0.5.sp,
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    InsightYearChip(
                        year    = selectedYear,
                        onClick = { isYearPickerOpen = true },
                    )
                }
            }
        }

        // ── Share FAB ───────────────────────────────────────────────────────
        if (!isShareCaptureMode && isLastPage && hasData) {
            InsightShareFab(
                isGenerating = isGeneratingImage,
                onClick = {
                    if (!isGeneratingImage) {
                        isGeneratingImage = true
                        coroutineScope.launch {
                            try {
                                isShareCaptureMode = true
                                awaitNextPreDraw(view)
                                awaitNextPreDraw(view)
                                val raw = ComposeToImage.captureViewBitmap(
                                    view = view, backgroundColor = shareArgb
                                )
                                val bounds  = shareBounds
                                val cropped = if (bounds != null) {
                                    ComposeToImage.cropBitmap(
                                        raw,
                                        bounds.left.toInt(), bounds.top.toInt(),
                                        bounds.width.toInt(), bounds.height.toInt()
                                    )
                                } else raw
                                val fitted = ComposeToImage.fitBitmap(cropped, 1080, 1920, shareArgb)
                                val uri    = ComposeToImage.saveBitmapAsFile(
                                    context, fitted, "OpenTune_Insight_$selectedYear"
                                )
                                context.startActivity(
                                    Intent.createChooser(
                                        Intent(Intent.ACTION_SEND).apply {
                                            type = "image/png"
                                            putExtra(Intent.EXTRA_STREAM, uri)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        },
                                        context.getString(R.string.share_summary)
                                    )
                                )
                            } finally {
                                isShareCaptureMode = false
                                isGeneratingImage  = false
                            }
                        }
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
                    .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Bottom))
            )
        }

        // ── Year picker dialog ──────────────────────────────────────────────
        if (!isShareCaptureMode && isYearPickerOpen) {
            InsightYearPickerDialog(
                availableYears = availableYears,
                selectedYear   = selectedYear,
                onSelectYear   = { y -> viewModel.selectedYear.value = y; isYearPickerOpen = false },
                onDismiss      = { isYearPickerOpen = false },
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// awaitNextPreDraw
// ─────────────────────────────────────────────────────────────────────────────

private suspend fun awaitNextPreDraw(view: View) = suspendCancellableCoroutine<Unit> { cont ->
    val vto = view.viewTreeObserver
    val listener = object : ViewTreeObserver.OnPreDrawListener {
        override fun onPreDraw(): Boolean {
            if (vto.isAlive) vto.removeOnPreDrawListener(this)
            cont.resume(Unit)
            return true
        }
    }
    vto.addOnPreDrawListener(listener)
    cont.invokeOnCancellation { if (vto.isAlive) vto.removeOnPreDrawListener(listener) }
    view.invalidate()
}

// ─────────────────────────────────────────────────────────────────────────────
// Page content router
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun InsightPageContent(
    page: InsightPage,
    year: Int,
    totalListeningTime: Long,
    totalSongsPlayed: Long,
    topSongsStats: List<SongWithStats>,
    topSongs: List<Song>,
    topArtists: List<Artist>,
    topAlbums: List<Album>,
    menuState: com.arturo254.opentune.ui.component.MenuState,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback,
    navController: NavController,
    coroutineScope: CoroutineScope,
    modifier: Modifier = Modifier,
) {
    when (page) {
        InsightPage.Welcome ->
            WelcomePage(year = year, modifier = modifier)

        InsightPage.Minutes ->
            MinutesPage(
                totalListeningTimeMs = totalListeningTime,
                totalSongsPlayed     = totalSongsPlayed,
                modifier             = modifier,
            )

        InsightPage.TopSongsList ->
            TopSongsListPage(songs = topSongsStats, modifier = modifier)

        InsightPage.SongSpotlight ->
            SongSpotlightPage(
                song     = topSongsStats.firstOrNull(),
                modifier = modifier,
                onLongClick = {
                    topSongs.firstOrNull()?.let { entity ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        menuState.show {
                            SongMenu(
                                originalSong = entity,
                                navController = navController,
                                onDismiss = menuState::dismiss,
                            )
                        }
                    }
                },
            )

        InsightPage.ArtistSpotlight ->
            ArtistSpotlightPage(
                artist   = topArtists.firstOrNull(),
                modifier = modifier,
                onLongClick = {
                    topArtists.firstOrNull()?.let { artist ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        menuState.show {
                            ArtistMenu(
                                originalArtist = artist,
                                coroutineScope = coroutineScope,
                                onDismiss = menuState::dismiss,
                            )
                        }
                    }
                },
            )

        InsightPage.TopAlbumsList ->
            TopAlbumsPage(albums = topAlbums, modifier = modifier)

        InsightPage.Personality ->
            PersonalityPage(
                personality = computePersonality(topSongsStats, totalSongsPlayed, totalListeningTime),
                topSong     = topSongsStats.firstOrNull(),
                modifier    = modifier,
            )

        InsightPage.Summary ->
            SummaryPage(
                year               = year,
                totalListeningTime = totalListeningTime,
                totalSongsPlayed   = totalSongsPlayed,
                topSong            = topSongsStats.firstOrNull(),
                topArtist          = topArtists.firstOrNull(),
                topAlbum           = topAlbums.firstOrNull(),
                modifier           = modifier,
            )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 1. Welcome page
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun WelcomePage(year: Int, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "welcomeGlow")
    val glowPhase by infiniteTransition.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(6000, easing = LinearEasing)),
        label = "glowPhase",
    )
    val pulseScale by infiniteTransition.animateFloat(
        0.95f, 1.05f,
        infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse",
    )

    Box(
        modifier = modifier.background(
            Brush.verticalGradient(listOf(WelcomeA, WelcomeB, WelcomeC.copy(alpha = 0.6f), WelcomeA))
        )
    ) {
        // Animated glow orb
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width * (0.5f + 0.15f * cos(glowPhase * 2 * PI.toFloat()).toFloat())
            val cy = size.height * (0.35f + 0.08f * sin(glowPhase * 2 * PI.toFloat()).toFloat())
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(WelcomeC.copy(alpha = 0.45f), WelcomeB.copy(alpha = 0.15f), Color.Transparent),
                    center = Offset(cx, cy),
                    radius = size.width * 0.7f,
                )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp)
                .windowInsetsPadding(WindowInsets.systemBars),
            verticalArrangement = Arrangement.Center,
        ) {
            // "Your" label
            Text(
                text       = "your",
                style      = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Light,
                color      = SnowMid,
                letterSpacing = 2.sp,
            )
            Spacer(Modifier.height(4.dp))

            // "Insight" wordmark
            Text(
                text       = "Insight",
                style      = MaterialTheme.typography.displayLarge.copy(fontSize = 72.sp),
                fontWeight = FontWeight.ExtraBold,
                color      = Snow,
                letterSpacing = (-2).sp,
            )

            Spacer(Modifier.height(12.dp))

            // Year badge
            Box(
                modifier = Modifier
                    .scale(pulseScale)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(listOf(WelcomeC.copy(alpha = 0.9f), WelcomeB))
                    )
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Text(
                    text       = year.toString(),
                    style      = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color      = Snow,
                )
            }

            Spacer(Modifier.height(40.dp))

            // Tagline
            Text(
                text       = "Everything you listened to\nthis year, in one place.",
                style      = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color      = SnowMid,
                lineHeight = 28.sp,
            )

            Spacer(Modifier.height(48.dp))

            // Swipe hint
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                repeat(3) {
                    val dotAlpha by rememberInfiniteTransition(label = "dot$it").animateFloat(
                        0.2f, 0.9f,
                        infiniteRepeatable(
                            tween(600, delayMillis = it * 200, easing = FastOutSlowInEasing),
                            RepeatMode.Reverse
                        ),
                        label = "dotA$it",
                    )
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Snow.copy(alpha = dotAlpha), CircleShape)
                    )
                }
                Spacer(Modifier.width(4.dp))
                Text(
                    text  = "Swipe to explore",
                    style = MaterialTheme.typography.labelMedium,
                    color = SnowDim,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 2. Minutes page  (animated counter)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MinutesPage(
    totalListeningTimeMs: Long,
    totalSongsPlayed: Long,
    modifier: Modifier = Modifier,
) {
    val totalMinutes  = totalListeningTimeMs / 60_000L
    val totalHours    = totalMinutes / 60L
    val displayValue  = if (totalHours > 0) totalHours else totalMinutes
    val displayLabel  = if (totalHours > 0) "hours" else "minutes"

    val animatedValue = rememberAnimatedLong(displayValue, durationMs = 1800)

    Box(
        modifier = modifier.background(
            Brush.verticalGradient(listOf(MinutesA, MinutesB, MinutesC.copy(alpha = 0.3f), MinutesA))
        )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(MinutesC.copy(alpha = 0.3f), Color.Transparent),
                    center = Offset(size.width * 0.85f, size.height * 0.2f),
                    radius = size.width * 0.6f,
                )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp)
                .windowInsetsPadding(WindowInsets.systemBars),
            verticalArrangement = Arrangement.Center,
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(GlassHi),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(R.drawable.headphones),
                    contentDescription = null,
                    tint = Snow,
                    modifier = Modifier.size(32.dp),
                )
            }

            Spacer(Modifier.height(32.dp))

            Text(
                text       = "You listened to",
                style      = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Light,
                color      = SnowMid,
            )
            Spacer(Modifier.height(8.dp))

            // Giant animated number
            Text(
                text       = animatedValue.toString(),
                style      = MaterialTheme.typography.displayLarge.copy(fontSize = 96.sp),
                fontWeight = FontWeight.Black,
                color      = Snow,
                letterSpacing = (-3).sp,
                lineHeight = 96.sp,
            )

            Text(
                text       = displayLabel,
                style      = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color      = MinutesC,
                letterSpacing = 1.sp,
            )

            Text(
                text  = "of music",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Light,
                color = SnowMid,
            )

            Spacer(Modifier.height(40.dp))

            // Fun comparison
            if (totalHours > 0) {
                val movieCount = totalHours / 2
                InsightGlassCard {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text("🎬", fontSize = 28.sp)
                        Text(
                            text = "That's like watching $movieCount full movies back-to-back.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SnowMid,
                            lineHeight = 22.sp,
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            InsightGlassCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("🎵", fontSize = 28.sp)
                    Text(
                        text  = "Across $totalSongsPlayed plays of your top songs.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SnowMid,
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 3. Top Songs list page
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TopSongsListPage(songs: List<SongWithStats>, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(
            Brush.verticalGradient(listOf(SongsA, SongsB.copy(alpha = 0.85f), SongsA))
        )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(SongsC.copy(alpha = 0.15f), Color.Transparent),
                    center = Offset(0f, size.height),
                    radius = size.width * 0.8f,
                )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .windowInsetsPadding(WindowInsets.systemBars),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text       = "Your Top",
                style      = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Light,
                color      = SnowMid,
            )
            Text(
                text       = "Songs",
                style      = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Black,
                color      = SongsB,
                letterSpacing = (-1).sp,
            )

            Spacer(Modifier.height(28.dp))

            songs.take(5).forEachIndexed { index, song ->
                val imageModel = rememberSafeImageRequest(song.thumbnailUrl)
                val revealDelay = index * 80

                var visible by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    delay(revealDelay.toLong())
                    visible = true
                }
                val animAlpha by animateFloatAsState(
                    if (visible) 1f else 0f,
                    tween(350),
                    label = "songAlpha$index",
                )
                val animOffset by animateFloatAsState(
                    if (visible) 0f else 40f,
                    tween(350, easing = FastOutSlowInEasing),
                    label = "songOffset$index",
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(animAlpha)
                        .offset(x = animOffset.dp)
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    // Rank number
                    Text(
                        text       = "${index + 1}",
                        style      = MaterialTheme.typography.headlineLarge.copy(fontSize = 40.sp),
                        fontWeight = FontWeight.Black,
                        color      = if (index == 0) SongsB else SnowDim,
                        modifier   = Modifier.width(46.dp),
                        textAlign  = TextAlign.End,
                    )

                    // Album art
                    AsyncImage(
                        model             = imageModel,
                        contentDescription = null,
                        contentScale      = ContentScale.Crop,
                        modifier          = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .border(
                                width  = if (index == 0) 2.dp else 0.dp,
                                color  = SongsB,
                                shape  = RoundedCornerShape(10.dp),
                            )
                    )

                    // Title + play count
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text      = song.title,
                            style     = MaterialTheme.typography.titleMedium,
                            fontWeight = if (index == 0) FontWeight.ExtraBold else FontWeight.SemiBold,
                            color     = Snow,
                            maxLines  = 1,
                            overflow  = TextOverflow.Ellipsis,
                        )
                        Text(
                            text  = pluralStringResource(R.plurals.n_time, song.songCountListened, song.songCountListened),
                            style = MaterialTheme.typography.labelMedium,
                            color = SnowDim,
                        )
                    }

                    // #1 crown badge
                    if (index == 0) {
                        Text("👑", fontSize = 20.sp)
                    }
                }

                if (index < songs.size - 1 && index < 4) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 116.dp),
                        color    = GlassBorder,
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 4. Song spotlight (#1 song full-screen)
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SongSpotlightPage(
    song: SongWithStats?,
    modifier: Modifier = Modifier,
    onLongClick: () -> Unit = {},
) {
    if (song == null) return

    val imageModel = rememberSafeImageRequest(song.thumbnailUrl)

    Box(
        modifier = modifier.combinedClickable(onClick = {}, onLongClick = onLongClick)
    ) {
        // Blurred album art full-screen background
        AsyncImage(
            model             = imageModel,
            contentDescription = null,
            contentScale      = ContentScale.Crop,
            modifier          = Modifier.fillMaxSize().blur(20.dp),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            SpotA.copy(alpha = 0.4f),
                            SpotA.copy(alpha = 0.75f),
                            SpotA.copy(alpha = 0.95f),
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp)
                .windowInsetsPadding(WindowInsets.systemBars),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(Modifier.weight(0.25f))

            // Badge
            InsightBadge(
                text       = "#1 Song this year",
                background = Brush.linearGradient(listOf(SpotB, SpotA.copy(alpha = 0.6f))),
            )

            // Album art
            AsyncImage(
                model             = imageModel,
                contentDescription = null,
                contentScale      = ContentScale.Crop,
                modifier          = Modifier
                    .size(220.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .border(
                        width  = 2.dp,
                        brush  = Brush.linearGradient(listOf(SpotB, SpotA.copy(alpha = 0.3f))),
                        shape  = RoundedCornerShape(20.dp),
                    )
            )

            // Title
            Text(
                text       = song.title,
                style      = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Black,
                color      = Snow,
                maxLines   = 2,
                overflow   = TextOverflow.Ellipsis,
                letterSpacing = (-0.5).sp,
            )

            // Stat chips
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                InsightStatChip(
                    emoji  = "▶",
                    value  = pluralStringResource(R.plurals.n_time, song.songCountListened, song.songCountListened),
                    accent = SpotB,
                )
                InsightStatChip(
                    emoji  = "⏱",
                    value  = makeTimeString(song.timeListened),
                    accent = MinutesC,
                )
            }

            Spacer(Modifier.weight(0.5f))

            Text(
                text  = "Hold to see song options",
                style = MaterialTheme.typography.labelSmall,
                color = SnowDim,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 5. Artist spotlight
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ArtistSpotlightPage(
    artist: Artist?,
    modifier: Modifier = Modifier,
    onLongClick: () -> Unit = {},
) {
    if (artist == null) return

    val imageModel = rememberSafeImageRequest(artist.artist.thumbnailUrl)
    val infiniteTransition = rememberInfiniteTransition(label = "artistRing")
    val ringRotation by infiniteTransition.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(8000, easing = LinearEasing)),
        label = "ringRot",
    )

    Box(
        modifier = modifier.combinedClickable(onClick = {}, onLongClick = onLongClick)
    ) {
        // Blurred artist photo background
        AsyncImage(
            model             = imageModel,
            contentDescription = null,
            contentScale      = ContentScale.Crop,
            modifier          = Modifier.fillMaxSize().blur(24.dp),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        listOf(ArtistC.copy(alpha = 0.4f), ArtistA.copy(alpha = 0.9f))
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp)
                .windowInsetsPadding(WindowInsets.systemBars),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            InsightBadge(
                text       = "#1 Artist",
                background = Brush.linearGradient(listOf(ArtistB, ArtistC)),
            )

            Spacer(Modifier.height(28.dp))

            // Rotating gradient ring + artist photo
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .drawBehind {
                        rotate(ringRotation) {
                            drawCircle(
                                brush = Brush.sweepGradient(
                                    listOf(ArtistB, ArtistC, MinutesC, ArtistB)
                                ),
                                radius = size.minDimension / 2f + 5.dp.toPx(),
                                style  = Stroke(width = 4.dp.toPx()),
                            )
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                AsyncImage(
                    model             = imageModel,
                    contentDescription = null,
                    contentScale      = ContentScale.Crop,
                    modifier          = Modifier
                        .size(188.dp)
                        .clip(CircleShape)
                )
            }

            Spacer(Modifier.height(28.dp))

            Text(
                text       = artist.artist.name,
                style      = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black,
                color      = Snow,
                textAlign  = TextAlign.Center,
                maxLines   = 2,
                overflow   = TextOverflow.Ellipsis,
                letterSpacing = (-0.5).sp,
            )

            Spacer(Modifier.height(20.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                InsightStatChip(
                    emoji  = "▶",
                    value  = pluralStringResource(R.plurals.n_time, artist.songCount, artist.songCount),
                    accent = ArtistB,
                )
                artist.timeListened?.let { t ->
                    InsightStatChip(
                        emoji  = "⏱",
                        value  = makeTimeString(t.toLong()),
                        accent = PersonB,
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text  = "Hold to see artist options",
                style = MaterialTheme.typography.labelSmall,
                color = SnowDim,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 6. Top Albums page
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TopAlbumsPage(albums: List<Album>, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(
            Brush.verticalGradient(listOf(AlbumA, AlbumB.copy(alpha = 0.8f), AlbumA))
        )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(AlbumC.copy(alpha = 0.2f), Color.Transparent),
                    center = Offset(size.width, 0f),
                    radius = size.width * 0.7f,
                )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .windowInsetsPadding(WindowInsets.systemBars),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text       = "Your Top",
                style      = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Light,
                color      = SnowMid,
            )
            Text(
                text       = "Albums",
                style      = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Black,
                color      = AlbumB,
                letterSpacing = (-1).sp,
            )

            Spacer(Modifier.height(28.dp))

            albums.take(5).forEachIndexed { index, album ->
                val imageModel = rememberSafeImageRequest(album.thumbnailUrl)
                val artistNames = album.artists.take(2).joinToString(" · ") { it.name }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Text(
                        text       = "${index + 1}",
                        style      = MaterialTheme.typography.headlineLarge.copy(fontSize = 36.sp),
                        fontWeight = FontWeight.Black,
                        color      = if (index == 0) AlbumB else SnowDim,
                        modifier   = Modifier.width(40.dp),
                        textAlign  = TextAlign.End,
                    )
                    AsyncImage(
                        model             = imageModel,
                        contentDescription = null,
                        contentScale      = ContentScale.Crop,
                        modifier          = Modifier
                            .size(58.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .border(
                                width = if (index == 0) 2.dp else 0.dp,
                                color = AlbumB,
                                shape = RoundedCornerShape(10.dp),
                            )
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text       = album.album.title,
                            style      = MaterialTheme.typography.titleMedium,
                            fontWeight = if (index == 0) FontWeight.ExtraBold else FontWeight.SemiBold,
                            color      = Snow,
                            maxLines   = 1,
                            overflow   = TextOverflow.Ellipsis,
                        )
                        if (artistNames.isNotBlank()) {
                            Text(
                                text     = artistNames,
                                style    = MaterialTheme.typography.labelMedium,
                                color    = SnowDim,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    if (index == 0) Text("🏆", fontSize = 20.sp)
                }

                if (index < albums.size - 1 && index < 4) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 116.dp),
                        color    = GlassBorder,
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 7. Listening Personality page
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PersonalityPage(
    personality: InsightPersonality,
    topSong: SongWithStats?,
    modifier: Modifier = Modifier,
) {
    val emojiScale by rememberInfiniteTransition(label = "emojiPulse").animateFloat(
        0.92f, 1.08f,
        infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "emojiS",
    )

    Box(
        modifier = modifier.background(
            Brush.verticalGradient(
                listOf(personality.gradTop, personality.gradBot.copy(alpha = 0.9f), personality.gradTop)
            )
        )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(personality.accent.copy(alpha = 0.25f), Color.Transparent),
                    center = Offset(size.width / 2f, size.height * 0.35f),
                    radius = size.width * 0.75f,
                )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp)
                .windowInsetsPadding(WindowInsets.systemBars),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                text  = "Your listening\npersonality",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Light,
                color = SnowMid,
                lineHeight = 32.sp,
            )

            Spacer(Modifier.height(32.dp))

            // Giant emoji
            Text(
                text     = personality.emoji,
                fontSize = 88.sp,
                modifier = Modifier.scale(emojiScale),
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text       = personality.title,
                style      = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black,
                color      = personality.accent,
                letterSpacing = (-1).sp,
            )

            Spacer(Modifier.height(16.dp))

            InsightGlassCard {
                Text(
                    text       = personality.description,
                    style      = MaterialTheme.typography.bodyLarge,
                    color      = SnowMid,
                    lineHeight = 26.sp,
                )
            }

            topSong?.let { song ->
                Spacer(Modifier.height(12.dp))
                InsightGlassCard {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text("🎵", fontSize = 20.sp)
                        Text(
                            text  = "Your anthem this year: ${song.title}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SnowMid,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 8. Summary / Share page
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SummaryPage(
    year: Int,
    totalListeningTime: Long,
    totalSongsPlayed: Long,
    topSong: SongWithStats?,
    topArtist: Artist?,
    topAlbum: Album?,
    modifier: Modifier = Modifier,
) {
    val confettiParticles = remember {
        List(30) {
            Triple(
                Random.nextFloat(),  // x
                Random.nextFloat(),  // y
                listOf(WelcomeB, WelcomeC, MinutesC, SongsB, AlbumB, PersonB).random()
            )
        }
    }
    val infiniteTransition = rememberInfiniteTransition(label = "confetti")
    val confettiTime by infiniteTransition.animateFloat(
        0f, 1000f,
        infiniteRepeatable(tween(20000, easing = LinearEasing)),
        label = "confettiT",
    )

    Box(
        modifier = modifier.background(
            Brush.verticalGradient(
                listOf(SummaryA, SummaryB.copy(alpha = 0.7f), SummaryC.copy(alpha = 0.5f), SummaryD.copy(alpha = 0.3f), SummaryA)
            )
        )
    ) {
        // Confetti
        Canvas(modifier = Modifier.fillMaxSize()) {
            confettiParticles.forEach { (x, y, color) ->
                val px = ((x + confettiTime * 0.00003f) % 1f) * size.width
                val py = ((y + confettiTime * 0.00008f) % 1f) * size.height
                drawCircle(color = color.copy(alpha = 0.6f), radius = 4f, center = Offset(px, py))
                drawRect(
                    color    = color.copy(alpha = 0.4f),
                    topLeft  = Offset(px + 8f, py - 4f),
                    size     = androidx.compose.ui.geometry.Size(8f, 4f),
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .windowInsetsPadding(WindowInsets.systemBars),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Spacer(Modifier.height(4.dp))

            // Header
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                // OpenTune branding row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(GlassHi),
                        contentAlignment = Alignment.Center,
                    ) {
                        androidx.compose.foundation.Image(
                            painter = painterResource(R.drawable.opentune_monochrome),
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    Column {
                        Text(
                            "OpenTune Insight",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Snow,
                        )
                        Text(
                            year.toString(),
                            style = MaterialTheme.typography.labelMedium,
                            color = SnowDim,
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    text       = "Your year\nwrapped up.",
                    style      = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Black,
                    color      = Snow,
                    letterSpacing = (-1).sp,
                    lineHeight = 44.sp,
                )
            }

            // Stats grid
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    SummaryStatBox(
                        emoji = "⏱",
                        label = "Time listened",
                        value = makeTimeString(totalListeningTime),
                        accent = MinutesC,
                        modifier = Modifier.weight(1f),
                    )
                    SummaryStatBox(
                        emoji = "▶",
                        label = "Total plays",
                        value = totalSongsPlayed.toString(),
                        accent = SongsB,
                        modifier = Modifier.weight(1f),
                    )
                }

                // Highlights card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(GlassHi)
                        .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
                        .padding(18.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text(
                            text  = "Highlights",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = SnowMid,
                        )
                        topSong?.let {
                            SummaryHighlight(
                                emoji  = "🎵",
                                label  = "Top Song",
                                value  = it.title,
                                accent = SpotB,
                            )
                        }
                        topArtist?.let {
                            SummaryHighlight(
                                emoji  = "🎤",
                                label  = "Top Artist",
                                value  = it.artist.name,
                                accent = ArtistB,
                            )
                        }
                        topAlbum?.let {
                            SummaryHighlight(
                                emoji  = "💿",
                                label  = "Top Album",
                                value  = it.album.title,
                                accent = AlbumB,
                            )
                        }
                        if (topSong == null && topArtist == null && topAlbum == null) {
                            Text(
                                text  = stringResource(R.string.no_listening_data),
                                style = MaterialTheme.typography.bodyMedium,
                                color = SnowDim,
                            )
                        }
                    }
                }
            }

            // Footer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    joinByBullet("OpenTune", year.toString()),
                    style = MaterialTheme.typography.labelSmall,
                    color = SnowDim,
                )
                Text(
                    "OpenTune Insight",
                    style = MaterialTheme.typography.labelSmall,
                    color = SnowDim,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Reusable small composables
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun InsightProgressBar(
    totalPages: Int,
    currentPage: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(totalPages) { index ->
            val fillFraction by animateFloatAsState(
                targetValue = when {
                    index < currentPage -> 1f
                    index == currentPage -> 1f
                    else -> 0f
                },
                animationSpec = tween(250),
                label = "segFill$index",
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Snow.copy(alpha = 0.2f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fillFraction)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(2.dp))
                        .background(Snow)
                )
            }
        }
    }
}

@Composable
private fun InsightYearChip(year: Int, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(end = 6.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(GlassHi)
            .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(
                text       = year.toString(),
                style      = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color      = Snow,
            )
            Icon(
                painterResource(R.drawable.expand_more),
                contentDescription = null,
                tint     = SnowMid,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@Composable
private fun InsightShareFab(
    isGenerating: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rotationDeg by rememberInfiniteTransition(label = "fabRing").animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(2500, easing = LinearEasing)),
        label = "fabRingRot",
    )
    Box(
        modifier = modifier
            .size(60.dp)
            .drawBehind {
                rotate(rotationDeg) {
                    drawCircle(
                        brush = Brush.sweepGradient(
                            listOf(WelcomeC, WelcomeB, MinutesC, SongsB, AlbumB, WelcomeC)
                        ),
                        radius = size.minDimension / 2f + 3.dp.toPx(),
                        style  = Stroke(width = 3.dp.toPx()),
                    )
                }
            }
    ) {
        FloatingActionButton(
            onClick      = onClick,
            modifier     = Modifier.fillMaxSize(),
            shape        = CircleShape,
            containerColor = Color(0xFF111111),
            contentColor = Snow,
        ) {
            if (isGenerating) {
                CircularProgressIndicator(
                    modifier    = Modifier.size(22.dp),
                    strokeWidth = 2.dp,
                    color       = WelcomeC,
                )
            } else {
                Icon(painterResource(R.drawable.share), contentDescription = null)
            }
        }
    }
}

@Composable
private fun InsightYearPickerDialog(
    availableYears: List<Int>,
    selectedYear: Int,
    onSelectYear: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest  = onDismiss,
        containerColor    = Color(0xFF111118),
        titleContentColor = Snow,
        title = {
            Text(
                "Select year",
                fontWeight = FontWeight.ExtraBold,
                color      = Snow,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                availableYears.forEach { year ->
                    val isSelected = year == selectedYear
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (isSelected)
                                    Brush.linearGradient(listOf(WelcomeB, WelcomeC))
                                else
                                    Brush.linearGradient(listOf(GlassHi, GlassHi))
                            )
                            .border(
                                width = if (isSelected) 0.dp else 1.dp,
                                color = GlassBorder,
                                shape = RoundedCornerShape(14.dp),
                            )
                            .clickable { onSelectYear(year) }
                            .padding(horizontal = 20.dp, vertical = 14.dp)
                    ) {
                        Text(
                            text       = year.toString(),
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                            color      = Snow,
                            fontSize   = 16.sp,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dismiss), color = WelcomeB)
            }
        }
    )
}

@Composable
private fun InsightGlassCard(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(GlassHi)
            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        content()
    }
}

@Composable
private fun InsightBadge(text: String, background: Brush) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(background)
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(
            text       = text,
            style      = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color      = Snow,
        )
    }
}

@Composable
private fun InsightStatChip(emoji: String, value: String, accent: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(accent.copy(alpha = 0.15f))
            .border(1.dp, accent.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(emoji, fontSize = 13.sp)
            Text(
                text       = value,
                style      = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color      = Snow,
            )
        }
    }
}

@Composable
private fun SummaryStatBox(
    emoji: String,
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(GlassHi)
            .border(1.dp, GlassBorder, RoundedCornerShape(18.dp))
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(emoji, fontSize = 22.sp)
            Text(
                text       = value,
                style      = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color      = accent,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis,
            )
            Text(
                text  = label,
                style = MaterialTheme.typography.labelSmall,
                color = SnowDim,
            )
        }
    }
}

@Composable
private fun SummaryHighlight(emoji: String, label: String, value: String, accent: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(accent.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(emoji, fontSize = 18.sp)
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text  = label,
                style = MaterialTheme.typography.labelSmall,
                color = SnowDim,
            )
            Text(
                text       = value,
                style      = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color      = Snow,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Animated counter
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun rememberAnimatedLong(target: Long, durationMs: Int = 1400): Long {
    var current by remember { mutableLongStateOf(0L) }
    LaunchedEffect(target) {
        val startMs = System.currentTimeMillis()
        while (true) {
            val elapsed  = (System.currentTimeMillis() - startMs).coerceAtMost(durationMs.toLong())
            val progress = elapsed.toFloat() / durationMs
            val eased    = FastOutSlowInEasing.transform(progress)
            current      = (target * eased).toLong()
            if (elapsed >= durationMs) break
            delay(16L)
        }
    }
    return current
}

// ─────────────────────────────────────────────────────────────────────────────
// Safe image request (no hardware bitmap — required for share capture)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun rememberSafeImageRequest(data: Any?): Any? {
    val context = LocalContext.current
    return remember(data) {
        data?.let {
            ImageRequest.Builder(context)
                .data(it)
                .allowHardware(false)
                .build()
        }
    }
}