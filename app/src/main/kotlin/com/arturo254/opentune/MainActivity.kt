/*
 * OpenTune Project Original (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package com.arturo254.opentune

import android.annotation.SuppressLint
import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.core.content.ContextCompat
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.datastore.preferences.core.edit
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.window.core.layout.WindowSizeClass
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.valentinilk.shimmer.LocalShimmerTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import com.arturo254.opentune.utils.PreferenceStore
import kotlinx.coroutines.withContext
import com.arturo254.opentune.constants.AppBarHeight
import com.arturo254.opentune.constants.AppLanguageKey
import com.arturo254.opentune.constants.CustomThemeColorKey
import com.arturo254.opentune.constants.DarkModeKey
import com.arturo254.opentune.constants.DefaultOpenTabKey
import com.arturo254.opentune.constants.DisableScreenshotKey
import com.arturo254.opentune.constants.DynamicThemeKey
import com.arturo254.opentune.constants.FloatingToolbarBottomPadding
import com.arturo254.opentune.constants.FloatingToolbarHeight
import com.arturo254.opentune.constants.FloatingToolbarHorizontalPadding
import com.arturo254.opentune.constants.HasPressedStarKey
import com.arturo254.opentune.constants.LaunchCountKey
import com.arturo254.opentune.constants.LiquidGlassNavBarKey
import com.arturo254.opentune.constants.MiniPlayerBottomSpacing
import com.arturo254.opentune.constants.MiniPlayerHeight
import com.arturo254.opentune.constants.MiniPlayerLastAnchorKey
import com.arturo254.opentune.constants.NavigationBarAnimationSpec
import com.arturo254.opentune.constants.PauseSearchHistoryKey
import com.arturo254.opentune.constants.PureBlackKey
import com.arturo254.opentune.constants.RemindAfterKey
import com.arturo254.opentune.constants.SYSTEM_DEFAULT
import com.arturo254.opentune.constants.SearchSource
import com.arturo254.opentune.constants.SearchSourceKey
import com.arturo254.opentune.constants.SlimFloatingToolbarHeight
import com.arturo254.opentune.constants.SlimNavBarKey
import com.arturo254.opentune.constants.StopMusicOnTaskClearKey
import com.arturo254.opentune.constants.UseNewMiniPlayerDesignKey
import com.arturo254.opentune.constants.UseSystemFontKey
import com.arturo254.opentune.db.MusicDatabase
import com.arturo254.opentune.db.entities.SearchHistory
import com.arturo254.opentune.db.entities.Album
import com.arturo254.opentune.db.entities.Artist
import com.arturo254.opentune.db.entities.Playlist
import com.arturo254.opentune.db.entities.Song
import com.arturo254.opentune.innertube.YouTube
import com.arturo254.opentune.innertube.models.AlbumItem
import com.arturo254.opentune.innertube.models.ArtistItem
import com.arturo254.opentune.innertube.models.PlaylistItem
import com.arturo254.opentune.innertube.models.SongItem
import com.arturo254.opentune.extensions.toMediaItem
import com.arturo254.opentune.models.toMediaMetadata
import com.arturo254.opentune.playback.DownloadUtil
import com.arturo254.opentune.playback.MusicService
import com.arturo254.opentune.playback.MusicService.MusicBinder
import com.arturo254.opentune.playback.PlayerConnection
import com.arturo254.opentune.playback.queues.LocalAlbumRadio
import com.arturo254.opentune.playback.queues.ListQueue
import com.arturo254.opentune.playback.queues.YouTubeAlbumRadio
import com.arturo254.opentune.playback.queues.YouTubeQueue
import com.arturo254.opentune.ui.component.AccountSettingsDialog
import com.arturo254.opentune.ui.component.BottomSheetMenu
import com.arturo254.opentune.ui.component.BottomSheetPage
import com.arturo254.opentune.ui.component.COLLAPSED_ANCHOR
import com.arturo254.opentune.ui.component.DISMISSED_ANCHOR
import com.arturo254.opentune.ui.component.EXPANDED_ANCHOR
import com.arturo254.opentune.ui.component.FloatingNavigationToolbar
import com.arturo254.opentune.ui.component.IconButton
import com.arturo254.opentune.ui.component.LocalBottomSheetPageState
import com.arturo254.opentune.ui.component.LocalMenuState
import com.mikepenz.markdown.m3.Markdown
import com.arturo254.opentune.constants.TogetherDisplayNameKey
import com.arturo254.opentune.ui.component.BottomSheetPageState
import com.arturo254.opentune.ui.component.MenuState
import com.arturo254.opentune.ui.component.TopSearch
import com.arturo254.opentune.ui.component.rememberBottomSheetState
import com.arturo254.opentune.ui.component.shimmer.ShimmerTheme
import com.arturo254.opentune.ui.menu.YouTubeSongMenu
import com.arturo254.opentune.ui.player.BottomSheetPlayer
import com.arturo254.opentune.ui.screens.LOGIN_URL_ARGUMENT
import com.arturo254.opentune.ui.screens.Screens
import com.arturo254.opentune.ui.screens.buildLoginRoute
import com.arturo254.opentune.ui.screens.musicrecognition.MusicRecognitionRoute
import com.arturo254.opentune.ui.screens.navigationBuilder
import com.arturo254.opentune.ui.screens.search.LocalSearchScreen
import com.arturo254.opentune.ui.screens.search.OnlineSearchScreen
import com.arturo254.opentune.ui.screens.settings.DarkMode
import com.arturo254.opentune.ui.screens.settings.DiscordPresenceManager
import com.arturo254.opentune.ui.screens.settings.NavigationTab
import com.arturo254.opentune.ui.screens.settings.ThemePalettes
import com.arturo254.opentune.ui.theme.OpenTuneTheme
import com.arturo254.opentune.ui.theme.ColorSaver
import com.arturo254.opentune.ui.theme.DefaultThemeColor
import com.arturo254.opentune.ui.theme.ThemeSeedPalette
import com.arturo254.opentune.ui.theme.ThemeSeedPaletteCodec
import com.arturo254.opentune.ui.theme.extractThemeColor
import com.arturo254.opentune.ui.utils.appBarScrollBehavior
import com.arturo254.opentune.ui.utils.backToMain
import com.arturo254.opentune.ui.utils.resetHeightOffset
import com.arturo254.opentune.utils.SyncUtils
import com.arturo254.opentune.utils.UpdateNotificationManager
import com.arturo254.opentune.utils.Updater
import com.arturo254.opentune.utils.dataStore
import com.arturo254.opentune.utils.get
import com.arturo254.opentune.utils.rememberEnumPreference
import com.arturo254.opentune.utils.rememberPreference
import com.arturo254.opentune.utils.reportException
import com.arturo254.opentune.utils.setAppLocale
import com.arturo254.opentune.viewmodels.HomeViewModel
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.Locale
import javax.inject.Inject
import kotlin.random.Random
import kotlin.time.Duration.Companion.days

@Suppress("DEPRECATION", "ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE")
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var database: MusicDatabase

    @Inject
    lateinit var downloadUtil: DownloadUtil

    @Inject
    lateinit var syncUtils: SyncUtils

    private lateinit var navController: NavHostController
    private var pendingIntent: Intent? = null
    private var pendingDeepLinkSong: PendingDeepLinkSong? = null
    private var pendingTogetherJoinLink: String? = null
    private var latestVersionName by mutableStateOf(BuildConfig.VERSION_NAME)

    private var playerConnection by mutableStateOf<PlayerConnection?>(null)
    private var isMusicServiceBound = false
    
    private val serviceConnection =
        object : ServiceConnection {
            override fun onServiceConnected(
                name: ComponentName?,
                service: IBinder?,
            ) {
                isMusicServiceBound = true
                if (service is MusicBinder) {
                    playerConnection =
                        PlayerConnection(this@MainActivity, service, database, lifecycleScope)
                    playPendingDeepLinkSongIfReady()
                    joinPendingTogetherIfReady()
                }
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                isMusicServiceBound = false
                playerConnection?.dispose()
                playerConnection = null
            }
        }

    private data class PendingDeepLinkSong(
        val mediaItem: MediaItem,
    )

    private fun playPendingDeepLinkSongIfReady() {
        val pending = pendingDeepLinkSong ?: return
        val connection = playerConnection ?: return
        pendingDeepLinkSong = null
        connection.playQueue(ListQueue(items = listOf(pending.mediaItem)))
    }

    private fun joinPendingTogetherIfReady() {
        val pending = pendingTogetherJoinLink ?: return
        val connection = playerConnection ?: return
        pendingTogetherJoinLink = null
        lifecycleScope.launch(Dispatchers.IO) {
            val displayName =
                runCatching { dataStore.data.first()[TogetherDisplayNameKey] }
                    .getOrNull()
                    ?.trim()
                    .orEmpty()
                    .ifBlank { Build.MODEL ?: getString(R.string.app_name) }
            withContext(Dispatchers.Main) {
                connection.service.joinTogether(pending, displayName)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        isMusicServiceBound =
            bindService(
                Intent(this, MusicService::class.java),
                serviceConnection,
                BIND_AUTO_CREATE
            )
        playPendingDeepLinkSongIfReady()
    }

    private fun safeUnbindMusicService() {
        if (!isMusicServiceBound) return
        try {
            unbindService(serviceConnection)
        } catch (e: IllegalArgumentException) {
        } catch (e: Exception) {
            reportException(e)
        } finally {
            isMusicServiceBound = false
        }
    }

    override fun onStop() {
        safeUnbindMusicService()
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Only clear/stop presence when the activity is actually finishing (not on rotation)
        // and do not clear it for transient configuration changes.
        if (isFinishing && !isChangingConfigurations) {
            try { DiscordPresenceManager.stop() } catch (_: Exception) {}
        }

        val shouldStopOnTaskClear =
            if (!isFinishing) {
                false
            } else {
                dataStore.get(StopMusicOnTaskClearKey, false)
            }

        if (shouldStopOnTaskClear) {
            safeUnbindMusicService()
            stopService(Intent(this, MusicService::class.java))
            playerConnection = null
        }
    }





    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (::navController.isInitialized) {
            handleDeepLinkIntent(intent, navController)
        } else {
            pendingIntent = intent
        }
    }


    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.layoutDirection = View.LAYOUT_DIRECTION_LTR
        WindowCompat.setDecorFitsSystemWindows(window, false)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            val initialLocale = PreferenceStore.get(AppLanguageKey)
                ?.takeUnless { it == SYSTEM_DEFAULT }
                ?.let { Locale.forLanguageTag(it) }
                ?: Locale.getDefault()
            setAppLocale(this, initialLocale)

            lifecycleScope.launch(Dispatchers.IO) {
                runCatching {
                    dataStore.data.first()[AppLanguageKey]
                }.onSuccess { lang ->
                    val targetLocale = lang
                        ?.takeUnless { it == SYSTEM_DEFAULT }
                        ?.let { Locale.forLanguageTag(it) }
                        ?: Locale.getDefault()
                    if (targetLocale != initialLocale) {
                        withContext(Dispatchers.Main) {
                            setAppLocale(this@MainActivity, targetLocale)
                            recreate()
                        }
                    }
                }
            }
        }
        
        lifecycleScope.launch(Dispatchers.IO) {
            dataStore.data
                .map { it[DisableScreenshotKey] ?: false }
                .distinctUntilChanged()
                .collectLatest {
                    withContext(Dispatchers.Main) {
                        if (it) {
                            window.setFlags(
                                WindowManager.LayoutParams.FLAG_SECURE,
                                WindowManager.LayoutParams.FLAG_SECURE,
                            )
                        } else {
                            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                        }
                    }
                }
        }

        setContent {
            val notificationPermissionLauncher =
                rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                    if (isGranted) {
                        playerConnection?.service?.refreshPlaybackNotification()
                    }
                }

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }

                if (System.currentTimeMillis() - Updater.lastCheckTime > 1.days.inWholeMilliseconds) {
                    Updater.getLatestVersionName().onSuccess {
                        latestVersionName = it
                    }
                }
                UpdateNotificationManager.checkForUpdates(this@MainActivity)
            }

                    // Use remembered instances so the same state object is used everywhere
                    // (previously retrieving the composition local directly created different
                    // instances in different composition scopes which caused the update
                    // bottom sheet to not appear and overlay interactions to be blocked).
                    val bottomSheetPageState = remember { BottomSheetPageState() }
            val (liquidGlassNavBar) = rememberPreference(LiquidGlassNavBarKey, defaultValue = false)
                    val menuState = remember { MenuState() }
                    val uriHandler = LocalUriHandler.current
                    val releaseNotesState = remember { mutableStateOf<String?>(null) }
                    val updateSheetContent: @Composable ColumnScope.() -> Unit = { // receiver: ColumnScope
                        Text(
                            text = stringResource(R.string.new_update_available),
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(top = 16.dp)
                        )

                        Spacer(Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = {},
                            shape = CircleShape,
                            contentPadding = PaddingValues(
                                horizontal = 5.dp,
                                vertical = 5.dp
                            )
                        ) {
                            Text(text = latestVersionName, style = MaterialTheme.typography.labelLarge)
                        }

                        Spacer(Modifier.height(12.dp))

                        Box(modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState())
                        ) {
                            val notes = releaseNotesState.value
                            if (notes != null && notes.isNotBlank()) {
                                Markdown(
                                    content = notes,
                                    modifier = Modifier
                                        .fillMaxWidth().padding(end = 8.dp)
                                )
                            } else {
                                Text(
                                    text = stringResource(R.string.release_notes_unavailable),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        Button(
                            onClick = {
                                try {
                                    uriHandler.openUri(Updater.getLatestDownloadUrl())
                                } catch (_: Exception) {}
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(text = stringResource(R.string.update_text))
                        }
                    }

                    // fetch release notes and show sheet when a new version is detected
                    LaunchedEffect(latestVersionName) {
                        if (!Updater.isSameVersion(latestVersionName, BuildConfig.VERSION_NAME)) {
                            Updater.getLatestReleaseNotes().onSuccess {
                                releaseNotesState.value = it
                            }.onFailure {
                                releaseNotesState.value = null
                            }

                            bottomSheetPageState.show(updateSheetContent)
                        }
                    }

            val enableDynamicTheme by rememberPreference(DynamicThemeKey, defaultValue = true)
            val customThemeColorValue by rememberPreference(CustomThemeColorKey, defaultValue = "default")
            val darkTheme by rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.AUTO)
            val useSystemFont by rememberPreference(UseSystemFontKey, defaultValue = false)
            val isSystemInDarkTheme = isSystemInDarkTheme()
            val useDarkTheme =
                remember(darkTheme, isSystemInDarkTheme) {
                    if (darkTheme == DarkMode.AUTO) isSystemInDarkTheme else darkTheme == DarkMode.ON
                }
            LaunchedEffect(useDarkTheme) {
                setSystemBarAppearance(useDarkTheme)
            }
            val pureBlackEnabled by rememberPreference(PureBlackKey, defaultValue = false)
            val pureBlack = pureBlackEnabled && useDarkTheme

            val customThemeSeedPalette = remember(customThemeColorValue) {
                if (customThemeColorValue.startsWith("#")) {
                    null
                } else if (customThemeColorValue.startsWith("seedPalette:")) {
                    ThemeSeedPaletteCodec.decodeFromPreference(customThemeColorValue)
                } else {
                    ThemePalettes
                        .findById(customThemeColorValue)
                        ?.let {
                            ThemeSeedPalette(
                                primary = it.primary,
                                secondary = it.secondary,
                                tertiary = it.tertiary,
                                neutral = it.neutral,
                            )
                        }
                }
            }

            val customThemeColor = remember(customThemeColorValue, customThemeSeedPalette) {
                if (customThemeColorValue.startsWith("#")) {
                    try {
                        val colorString = customThemeColorValue.removePrefix("#")
                        Color(android.graphics.Color.parseColor("#$colorString"))
                    } catch (e: Exception) {
                        DefaultThemeColor
                    }
                } else {
                    customThemeSeedPalette?.primary ?: DefaultThemeColor
                }
            }

            var themeColor by rememberSaveable(stateSaver = ColorSaver) {
                mutableStateOf(DefaultThemeColor)
            }

            LaunchedEffect(playerConnection, enableDynamicTheme, isSystemInDarkTheme, customThemeColor) {
                val playerConnection = playerConnection
                if (!enableDynamicTheme || playerConnection == null) {
                    themeColor = if (!enableDynamicTheme) customThemeColor else DefaultThemeColor
                    return@LaunchedEffect
                }
                playerConnection.service.currentMediaMetadata.collectLatest { song ->
                    if (song != null) {
                        withContext(Dispatchers.Default) {
                            try {
                                val result = imageLoader.execute(
                                    ImageRequest
                                        .Builder(this@MainActivity)
                                        .data(song.thumbnailUrl)
                                        .allowHardware(false)
                                        .build(),
                                )
                                val extractedColor = result.image?.toBitmap()?.extractThemeColor()
                                withContext(Dispatchers.Main) {
                                    themeColor = extractedColor ?: DefaultThemeColor
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    themeColor = DefaultThemeColor
                                }
                            }
                        }
                    } else {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            themeColor = DefaultThemeColor
                        } else {
                            themeColor = customThemeColor
                        }
                    }
                }
            }

            OpenTuneTheme(
                darkTheme = useDarkTheme,
                pureBlack = pureBlack,
                themeColor = themeColor,
                seedPalette = if (!enableDynamicTheme) customThemeSeedPalette else null,
                useSystemFont = useSystemFont,
            ) {
                    BoxWithConstraints(
                        modifier =
                        Modifier
                            .fillMaxSize()
                            .background(
                                if(pureBlack) Color.Black else MaterialTheme.colorScheme.surface
                            )
                    ) {
                    val focusManager = LocalFocusManager.current
                    val density = LocalDensity.current
                    val windowsInsets = WindowInsets.systemBars
                    val bottomInset = with(density) { windowsInsets.getBottom(density).toDp() }
                    val bottomInsetDp = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding()
                        
                    val useRail = currentWindowAdaptiveInfo().windowSizeClass
                        .isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)

                    val navController = rememberNavController()
                    val homeViewModel: HomeViewModel = hiltViewModel()
                    val accountImageUrl by homeViewModel.accountImageUrl.collectAsState()
                    val allLocalItems by homeViewModel.allLocalItems.collectAsState()
                    val allYtItems by homeViewModel.allYtItems.collectAsState()
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val (previousTab) = rememberSaveable { mutableStateOf("home") }
                    val currentRoute = navBackStackEntry?.destination?.route
                    val isYearInMusicScreen = currentRoute == "year_in_music"

                    val navigationItems = remember { Screens.MainScreens }
                    val (slimNav) = rememberPreference(SlimNavBarKey, defaultValue = false)
                    val (useNewMiniPlayerDesign) = rememberPreference(UseNewMiniPlayerDesignKey, defaultValue = true)
                    val (savedMiniPlayerAnchor, setSavedMiniPlayerAnchor) = rememberPreference(
                        MiniPlayerLastAnchorKey,
                        defaultValue = COLLAPSED_ANCHOR
                    )
                    val defaultOpenTab by rememberEnumPreference(DefaultOpenTabKey, NavigationTab.HOME)
                    val pauseSearchHistory by rememberPreference(PauseSearchHistoryKey, defaultValue = false)
                    val tabOpenedFromShortcut =
                        remember {
                            when (intent?.action) {
                                ACTION_LIBRARY -> NavigationTab.LIBRARY
                                ACTION_SEARCH -> NavigationTab.SEARCH
                                else -> null
                            }
                        }

                    val topLevelScreens =
                        listOf(
                            Screens.Home.route,
                            Screens.Search.route,
                            Screens.MoodAndGenres.route,
                            Screens.Library.route,
                            "settings",
                        )

                    val (query, onQueryChange) =
                        rememberSaveable(stateSaver = TextFieldValue.Saver) {
                            mutableStateOf(TextFieldValue())
                        }

                    var active by rememberSaveable {
                        mutableStateOf(false)
                    }

                    val onActiveChange: (Boolean) -> Unit = { newActive ->
                        active = newActive
                        if (!newActive) {
                            focusManager.clearFocus()
                            if (navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route }) {
                                onQueryChange(TextFieldValue())
                            }
                        }
                    }



                    var searchSource by rememberEnumPreference(SearchSourceKey, SearchSource.ONLINE)

                    val searchBarFocusRequester = remember { FocusRequester() }

                    val onSearch: (String) -> Unit = {
                        if (it.isNotEmpty()) {
                            onActiveChange(false)
                            navController.navigate("search/${URLEncoder.encode(it, "UTF-8")}")
                            if (!pauseSearchHistory) {
                                database.query {
                                    insert(SearchHistory(query = it))
                                }
                            }
                        }
                    }

                    var openSearchImmediately: Boolean by remember {
                        mutableStateOf(intent?.action == ACTION_SEARCH)
                    }

                    val shouldShowSearchBar =
                        remember(active, navBackStackEntry) {
                            active ||
                                    navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route } ||
                                    navBackStackEntry?.destination?.route?.startsWith("search/") == true
                        }

                    val shouldShowNavigationBar =
                        remember(navBackStackEntry, active) {
                            navBackStackEntry?.destination?.route == null ||
                                    navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route } &&
                                    !active
                        }

                    val shouldShowHomeShuffleButton =
                        currentRoute == Screens.Home.route &&
                            (allLocalItems.isNotEmpty() || allYtItems.isNotEmpty())

                    fun getBottomNavPadding(): Dp {
                        return if (shouldShowNavigationBar && !useRail) {
                            if (slimNav) SlimFloatingToolbarHeight else FloatingToolbarHeight
                        } else {
                            0.dp
                        }
                    }

                    val floatingBarsBottomPadding = FloatingToolbarBottomPadding
                    val navVisibleHeight = if (slimNav) SlimFloatingToolbarHeight else FloatingToolbarHeight

                    val bottomNavigationBarHeight by animateDpAsState(
                        targetValue = if (shouldShowNavigationBar && !useRail) navVisibleHeight else 0.dp,
                        animationSpec = NavigationBarAnimationSpec,
                        label = "",
                    )

                    val playerBottomSheetState =
                        rememberBottomSheetState(
                            dismissedBound = 0.dp,
                            collapsedBound =
                                bottomInset +
                                    (if (shouldShowNavigationBar && !useRail) floatingBarsBottomPadding else 0.dp) +
                                    getBottomNavPadding() +
                                    (if (useNewMiniPlayerDesign) MiniPlayerBottomSpacing else 0.dp) +
                                    MiniPlayerHeight,
                            expandedBound = maxHeight,
                        )

                    val miniPlayerAnchor by remember {
                        derivedStateOf {
                            when {
                                playerBottomSheetState.isExpanded -> EXPANDED_ANCHOR
                                playerBottomSheetState.isDismissed -> DISMISSED_ANCHOR
                                else -> COLLAPSED_ANCHOR
                            }
                        }
                    }

                    var miniPlayerAnchorPersistenceEnabled by remember(playerConnection) {
                        mutableStateOf(false)
                    }

                    LaunchedEffect(miniPlayerAnchor, isYearInMusicScreen, miniPlayerAnchorPersistenceEnabled) {
                        if (!isYearInMusicScreen && miniPlayerAnchorPersistenceEnabled) {
                            setSavedMiniPlayerAnchor(miniPlayerAnchor)
                        }
                        }

                    var yearInMusicSavedPlayerAnchor by rememberSaveable { mutableStateOf(-1) }

                    LaunchedEffect(isYearInMusicScreen) {
                        val controller = WindowCompat.getInsetsController(window, window.decorView)
                        if (isYearInMusicScreen) {
                            controller.systemBarsBehavior =
                                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                            controller.hide(WindowInsetsCompat.Type.statusBars())
                        } else {
                            controller.show(WindowInsetsCompat.Type.statusBars())
                        }
                    }

                    LaunchedEffect(isYearInMusicScreen, playerConnection) {
                        val player = playerConnection?.player ?: return@LaunchedEffect

                        if (isYearInMusicScreen) {
                            if (yearInMusicSavedPlayerAnchor == -1) {
                                yearInMusicSavedPlayerAnchor =
                                    when {
                                        playerBottomSheetState.isExpanded -> EXPANDED_ANCHOR
                                        playerBottomSheetState.isCollapsed -> COLLAPSED_ANCHOR
                                        playerBottomSheetState.isDismissed -> DISMISSED_ANCHOR
                                        else -> COLLAPSED_ANCHOR
                                    }
                            }

                            if (!playerBottomSheetState.isDismissed) {
                                playerBottomSheetState.dismiss()
                            }
                        } else if (yearInMusicSavedPlayerAnchor != -1) {
                            val anchorToRestore = yearInMusicSavedPlayerAnchor
                            yearInMusicSavedPlayerAnchor = -1

                            if (player.currentMediaItem == null) {
                                playerBottomSheetState.dismiss()
                            } else {
                                when (anchorToRestore) {
                                    EXPANDED_ANCHOR -> playerBottomSheetState.expandSoft()
                                    COLLAPSED_ANCHOR -> playerBottomSheetState.collapseSoft()
                                    DISMISSED_ANCHOR -> playerBottomSheetState.dismiss()
                                    else -> playerBottomSheetState.collapseSoft()
                                }
                            }
                        }
                    }



                    val playerAwareWindowInsets =
                        remember(
                            useRail,
                            bottomInset,
                            shouldShowNavigationBar,
                            playerBottomSheetState.isDismissed,
                        ) {
                            var bottom = bottomInset
                            if (shouldShowNavigationBar && !useRail) bottom += getBottomNavPadding()
                            if (!playerBottomSheetState.isDismissed) bottom += MiniPlayerHeight
                            windowsInsets
                                .only((if(useRail) {
                                    WindowInsetsSides.Right
                                } else WindowInsetsSides.Horizontal) + WindowInsetsSides.Top)
                                .add(WindowInsets(top = AppBarHeight, bottom = bottom))
                        }

                    appBarScrollBehavior(
                        canScroll = {
                            navBackStackEntry?.destination?.route?.startsWith("search/") == false &&
                                    (playerBottomSheetState.isCollapsed || playerBottomSheetState.isDismissed)
                        }
                    )

                    val searchBarScrollBehavior =
                        appBarScrollBehavior(
                            canScroll = {
                                navBackStackEntry?.destination?.route?.startsWith("search/") == false &&
                                        (playerBottomSheetState.isCollapsed || playerBottomSheetState.isDismissed)
                            },
                        )
                    val topAppBarScrollBehavior =
                        appBarScrollBehavior(
                            canScroll = {
                                navBackStackEntry?.destination?.route?.startsWith("search/") == false &&
                                        (playerBottomSheetState.isCollapsed || playerBottomSheetState.isDismissed)
                            },
                        )

                    var previousRoute by rememberSaveable { mutableStateOf<String?>(null) }

                    LaunchedEffect(navBackStackEntry) {
                        val currentRoute = navBackStackEntry?.destination?.route
                        val wasOnNonTopLevelScreen = previousRoute != null &&
                            previousRoute !in topLevelScreens &&
                            previousRoute?.startsWith("search/") != true
                        val isReturningToHomeOrLibrary = currentRoute == Screens.Home.route ||
                            currentRoute == Screens.Library.route

                        if (wasOnNonTopLevelScreen && isReturningToHomeOrLibrary) {
                            searchBarScrollBehavior.state.resetHeightOffset()
                            topAppBarScrollBehavior.state.resetHeightOffset()
                        }

                        previousRoute = currentRoute

                        if (navBackStackEntry?.destination?.route?.startsWith("search/") == true) {
                            val searchQuery =
                                withContext(Dispatchers.IO) {
                                    if (navBackStackEntry
                                            ?.arguments
                                            ?.getString(
                                                "query",
                                            )!!
                                            .contains(
                                                "%",
                                            )
                                    ) {
                                        navBackStackEntry?.arguments?.getString(
                                            "query",
                                        )!!
                                    } else {
                                        URLDecoder.decode(
                                            navBackStackEntry?.arguments?.getString("query")!!,
                                            "UTF-8"
                                        )
                                    }
                                }
                            onQueryChange(
                                TextFieldValue(
                                    searchQuery,
                                    TextRange(searchQuery.length)
                                )
                            )
                        } else if (navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route } || navBackStackEntry?.destination?.route in topLevelScreens) {
                            onQueryChange(TextFieldValue())
                            if (navBackStackEntry?.destination?.route != Screens.Home.route) {
                                searchBarScrollBehavior.state.resetHeightOffset()
                                topAppBarScrollBehavior.state.resetHeightOffset()
                            }
                        }
                    }
                    LaunchedEffect(active) {
                        if (active) {
                            searchBarScrollBehavior.state.resetHeightOffset()
                            topAppBarScrollBehavior.state.resetHeightOffset()
                            searchBarFocusRequester.requestFocus()
                        }
                    }

                    var restoredMiniPlayerAnchor by remember(playerConnection) { mutableStateOf(false) }

                    LaunchedEffect(playerConnection, savedMiniPlayerAnchor, isYearInMusicScreen) {
                        if (restoredMiniPlayerAnchor) return@LaunchedEffect
                        val player = playerConnection?.player ?: return@LaunchedEffect
                        val connection = playerConnection ?: return@LaunchedEffect
                        connection.queueRestoreCompleted.first { it }
                        if (player.currentMediaItem == null) {
                            if (!playerBottomSheetState.isDismissed) {
                                playerBottomSheetState.dismiss()
                            }
                        } else {
                            if (!isYearInMusicScreen) {
                                when (savedMiniPlayerAnchor) {
                                    EXPANDED_ANCHOR -> playerBottomSheetState.expandSoft()
                                    COLLAPSED_ANCHOR -> playerBottomSheetState.collapseSoft()
                                    DISMISSED_ANCHOR -> playerBottomSheetState.dismiss()
                                    else -> playerBottomSheetState.collapseSoft()
                                }
                            }
                        }
                        restoredMiniPlayerAnchor = true
                        miniPlayerAnchorPersistenceEnabled = true
                    }

                    DisposableEffect(playerConnection, playerBottomSheetState) {
                        val player =
                            playerConnection?.player ?: return@DisposableEffect onDispose { }
                        val listener =
                            object : Player.Listener {
                                override fun onMediaItemTransition(
                                    mediaItem: MediaItem?,
                                    reason: Int,
                                ) {
                                    if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED &&
                                        mediaItem != null &&
                                        playerBottomSheetState.isDismissed &&
                                        !isYearInMusicScreen
                                    ) {
                                        playerBottomSheetState.collapseSoft()
                                    }
                                }
                            }
                        player.addListener(listener)
                        onDispose {
                            player.removeListener(listener)
                        }
                    }

                    var shouldShowTopBar by rememberSaveable { mutableStateOf(false) }

                    LaunchedEffect(navBackStackEntry) {
                        shouldShowTopBar =
                            !active && navBackStackEntry?.destination?.route in topLevelScreens && navBackStackEntry?.destination?.route != "settings"
                    }

                    val coroutineScope = rememberCoroutineScope()
                    var sharedSong: SongItem? by remember {
                        mutableStateOf(null)
                    }

                    LaunchedEffect(Unit) {
                        if (pendingIntent != null) {
                            handleDeepLinkIntent(pendingIntent!!, navController)
                            pendingIntent = null
                        } else {
                            handleDeepLinkIntent(intent, navController)
                        }
                    }

                    var showStarDialog by remember { mutableStateOf(false) }

                    LaunchedEffect(Unit) {
                        delay(3000)
                        
                        withContext(Dispatchers.IO) {
                            val current = dataStore[LaunchCountKey] ?: 0
                            val newCount = current + 1
                            dataStore.edit { prefs ->
                                prefs[LaunchCountKey] = newCount
                            }
                        }

                        val shouldShow = withContext(Dispatchers.IO) {
                            val hasPressed = dataStore[HasPressedStarKey] ?: false
                            val remindAfter = dataStore[RemindAfterKey] ?: 3
                            !hasPressed && (dataStore[LaunchCountKey] ?: 0) >= remindAfter
                        }

                        if (shouldShow) {
                            var waited = 0L
                            val waitStep = 500L
                            val maxWait = 30_000L
                            while (bottomSheetPageState.isVisible && waited < maxWait) {
                                delay(waitStep)
                                waited += waitStep
                            }
                            showStarDialog = true
                        }
                    }


                    val currentTitleRes = remember(navBackStackEntry) {
                        when (navBackStackEntry?.destination?.route) {
                            Screens.Home.route -> R.string.home
                            Screens.Search.route -> R.string.search
                            Screens.Library.route -> R.string.filter_library
                            else -> null
                        }
                    }

                    var showAccountDialog by remember { mutableStateOf(false) }



                    CompositionLocalProvider(
                        LocalDatabase provides database,
                        LocalContentColor provides if (pureBlack) Color.White else contentColorFor(MaterialTheme.colorScheme.surface),
                        LocalPlayerConnection provides playerConnection,
                        LocalPlayerAwareWindowInsets provides playerAwareWindowInsets,
                        LocalDownloadUtil provides downloadUtil,
                        LocalShimmerTheme provides ShimmerTheme,
                        LocalSyncUtils provides syncUtils,
                        LocalBottomSheetPageState provides bottomSheetPageState,
                        LocalMenuState provides menuState,
                    ) {
                        Row {
                            AnimatedVisibility(useRail && shouldShowNavigationBar) {
                                NavigationRail(
                                    containerColor = if(pureBlack) Color.Black else MaterialTheme.colorScheme.surfaceContainer,
                                    contentColor = if(pureBlack) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    header = { Spacer(Modifier.height(24.dp)) }
                                ) {
                                    navigationItems.fastForEach { screen ->
                                        val isSelected =
                                            navBackStackEntry?.destination?.hierarchy?.any { it.route == screen.route } == true

                                        NavigationRailItem(
                                            selected = isSelected,
                                            icon = {
                                                Icon(
                                                    painter = painterResource(
                                                        id = if (isSelected) screen.iconIdActive else screen.iconIdInactive
                                                    ),
                                                    contentDescription = null,
                                                )
                                            },
                                            label = {
                                                if (!slimNav) {
                                                    Text(
                                                        text = stringResource(screen.titleId),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }

                                            },
                                            onClick = {
                                                val wasPlayerActive = playerBottomSheetState.isExpanded
                                                
                                                if(wasPlayerActive) {
                                                    playerBottomSheetState.collapse(spring())
                                                }
                                                
                                                if (screen.route == Screens.Search.route) {
                                                    onActiveChange(true)
                                                } else if (isSelected) {
                                                    if(wasPlayerActive) return@NavigationRailItem
                                                    
                                                    navController.currentBackStackEntry?.savedStateHandle?.set("scrollToTop", true)
                                                    coroutineScope.launch {
                                                        searchBarScrollBehavior.state.resetHeightOffset()
                                                    }
                                                } else {
                                                    navController.navigate(screen.route) {
                                                        popUpTo(navController.graph.startDestinationId) {
                                                            saveState = true
                                                        }
                                                        launchSingleTop = true
                                                        restoreState = true
                                                    }
                                                }
                                            },
                                        )
                                    }
                                }
                            }
                            
                            Scaffold(
                                topBar = {
                                    if (shouldShowTopBar) {
                                        val shouldUseFloatingTopBar = remember(navBackStackEntry) {
                                            navBackStackEntry?.destination?.route == Screens.Home.route ||
                                                navBackStackEntry?.destination?.route == Screens.MoodAndGenres.route ||
                                                navBackStackEntry?.destination?.route == Screens.Library.route
                                        }
                                        val shouldShowBlurBackground = remember(navBackStackEntry) {
                                            shouldUseFloatingTopBar
                                        }

                                        val surfaceColor = MaterialTheme.colorScheme.surface
                                        val currentScrollBehavior = if (shouldUseFloatingTopBar) searchBarScrollBehavior else topAppBarScrollBehavior

                                        Box(
                                            modifier = Modifier.offset {
                                                IntOffset(
                                                    x = 0,
                                                    y = currentScrollBehavior.state.heightOffset.toInt()
                                                )
                                            }
                                        ) {
                                            // Gradient shadow background
                                            if (shouldShowBlurBackground) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(AppBarHeight + with(LocalDensity.current) {
                                                            WindowInsets.systemBars.getTop(LocalDensity.current).toDp()
                                                        })
                                                        .background(
                                                            Brush.verticalGradient(
                                                                colors = listOf(
                                                                    surfaceColor.copy(alpha = 0.95f),
                                                                    surfaceColor.copy(alpha = 0.85f),
                                                                    surfaceColor.copy(alpha = 0.6f),
                                                                    Color.Transparent
                                                                )
                                                            )
                                                        )
                                                )
                                            }

                                            TopAppBar(
                                                windowInsets = WindowInsets.safeDrawing.only((if(useRail) {
                                                    WindowInsetsSides.Right
                                                } else WindowInsetsSides.Horizontal) + WindowInsetsSides.Top),
                                                title = {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        // app icon
                                                        Icon(
                                                            painter = painterResource(R.drawable.opentune),
                                                            contentDescription = null,
                                                            modifier = Modifier
                                                                .size(35.dp)
                                                                .padding(end = 3.dp)
                                                        )

                                                        Text(
                                                            text = stringResource(R.string.app_name),
                                                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                },
                                                actions = {
                                                    IconButton(onClick = { navController.navigate("history") }) {
                                                        Icon(
                                                            painter = painterResource(R.drawable.history),
                                                            contentDescription = stringResource(R.string.history)
                                                        )
                                                    }
                                                    IconButton(onClick = { navController.navigate("stats") }) {
                                                        Icon(
                                                            painter = painterResource(R.drawable.stats),
                                                            contentDescription = stringResource(R.string.stats)
                                                        )
                                                    }
                                                    IconButton(onClick = { navController.navigate("new_release") }) {
                                                        Icon(
                                                            painter = painterResource(R.drawable.notifications),
                                                            contentDescription = stringResource(R.string.new_release_albums)
                                                        )
                                                    }
                                                    IconButton(onClick = { showAccountDialog = true }) {
                                                        BadgedBox(badge = {
                                                            if (!Updater.isSameVersion(latestVersionName, BuildConfig.VERSION_NAME)) {
                                                                Badge()
                                                            }
                                                        }) {
                                                            if (accountImageUrl != null) {
                                                                AsyncImage(
                                                                    model = accountImageUrl,
                                                                    contentDescription = stringResource(R.string.account),
                                                                    modifier = Modifier
                                                                        .size(24.dp)
                                                                        .clip(CircleShape)
                                                                )
                                                            } else {
                                                                Icon(
                                                                    painter = painterResource(R.drawable.account),
                                                                    contentDescription = stringResource(R.string.account),
                                                                    modifier = Modifier.size(24.dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                },
                                                scrollBehavior = if (shouldUseFloatingTopBar) searchBarScrollBehavior else topAppBarScrollBehavior,
                                                colors = TopAppBarDefaults.topAppBarColors(
                                                    containerColor = if (shouldUseFloatingTopBar) Color.Transparent else if (pureBlack) Color.Black else MaterialTheme.colorScheme.surface,
                                                    scrolledContainerColor = if (shouldUseFloatingTopBar) Color.Transparent else if (pureBlack) Color.Black else MaterialTheme.colorScheme.surface,
                                                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                                                    actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    navigationIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            )
                                        }
                                    }
                                    AnimatedVisibility(
                                        visible = active || navBackStackEntry?.destination?.route?.startsWith("search/") == true,
                                        enter = fadeIn(animationSpec = tween(durationMillis = 300)),
                                        exit = fadeOut(animationSpec = tween(durationMillis = 200))
                                    ) {
                                        TopSearch(
                                            query = query,
                                            onQueryChange = onQueryChange,
                                            onSearch = onSearch,
                                            active = active,
                                            onActiveChange = onActiveChange,
                                            placeholder = {
                                                Text(
                                                    text = stringResource(
                                                        when (searchSource) {
                                                            SearchSource.LOCAL -> R.string.search_library
                                                            SearchSource.ONLINE -> R.string.search_yt_music
                                                        }
                                                    ),
                                                )
                                            },
                                            leadingIcon = {
                                                IconButton(
                                                    onClick = {
                                                        when {
                                                            active -> onActiveChange(false)
                                                            !navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route } -> {
                                                                navController.navigateUp()
                                                            }

                                                            else -> onActiveChange(true)
                                                        }
                                                    },
                                                    onLongClick = {
                                                        when {
                                                            active -> {}
                                                            !navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route } -> {
                                                                navController.backToMain()
                                                            }
                                                            else -> {}
                                                        }
                                                    },
                                                ) {
                                                    Icon(
                                                        painterResource(
                                                            if (active ||
                                                                !navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route }
                                                            ) {
                                                                R.drawable.arrow_back
                                                            } else {
                                                                R.drawable.search
                                                            },
                                                        ),
                                                        contentDescription = null,
                                                    )
                                                }
                                            },
                                            trailingIcon = {
                                                Row {
                                                    if (active) {
                                                        if (query.text.isNotEmpty()) {
                                                            IconButton(
                                                                onClick = {
                                                                    onQueryChange(
                                                                        TextFieldValue(
                                                                            ""
                                                                        )
                                                                    )
                                                                },
                                                            ) {
                                                                Icon(
                                                                    painter = painterResource(R.drawable.close),
                                                                    contentDescription = null,
                                                                )
                                                            }
                                                        }
                                                        IconButton(
                                                            onClick = {
                                                                searchSource =
                                                                    if (searchSource == SearchSource.ONLINE) SearchSource.LOCAL else SearchSource.ONLINE
                                                            },
                                                        ) {
                                                            Icon(
                                                                painter = painterResource(
                                                                    when (searchSource) {
                                                                        SearchSource.LOCAL -> R.drawable.library_music
                                                                        SearchSource.ONLINE -> R.drawable.language
                                                                    },
                                                                ),
                                                                contentDescription = null,
                                                            )
                                                        }
                                                    }
                                                }
                                            },
                                            modifier =
                                                Modifier
                                                    .focusRequester(searchBarFocusRequester)
                                                    .let { with(this@BoxWithConstraints) { it.align(Alignment.TopCenter) } },
                                            focusRequester = searchBarFocusRequester,
                                            colors = if (pureBlack && active) {
                                                SearchBarDefaults.colors(
                                                    containerColor = Color.Black,
                                                    dividerColor = Color.DarkGray,
                                                    inputFieldColors = TextFieldDefaults.colors(
                                                        focusedTextColor = Color.White,
                                                        unfocusedTextColor = Color.Gray,
                                                        focusedContainerColor = Color.Transparent,
                                                        unfocusedContainerColor = Color.Transparent,
                                                        cursorColor = Color.White,
                                                        focusedIndicatorColor = Color.Transparent,
                                                        unfocusedIndicatorColor = Color.Transparent,
                                                    )
                                                )
                                            } else {
                                                SearchBarDefaults.colors(
                                                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                                                )
                                            }
                                        ) {
                                            Crossfade(
                                                targetState = searchSource,
                                                label = "",
                                                modifier =
                                                    Modifier
                                                        .fillMaxSize()
                                                        .padding(bottom = if(!playerBottomSheetState.isDismissed) MiniPlayerHeight else 0.dp)
                                                        .navigationBarsPadding(),
                                            ) { searchSource ->
                                                when (searchSource) {
                                                    SearchSource.LOCAL ->
                                                        LocalSearchScreen(
                                                            query = query.text,
                                                            navController = navController,
                                                            onDismiss = { onActiveChange(false) },
                                                            pureBlack = pureBlack,
                                                        )

                                                    SearchSource.ONLINE ->
                                                        OnlineSearchScreen(
                                                            query = query.text,
                                                            onQueryChange = onQueryChange,
                                                            navController = navController,
                                                            onSearch = {
                                                                navController.navigate(
                                                                    "search/${
                                                                        URLEncoder.encode(
                                                                            it,
                                                                            "UTF-8"
                                                                        )
                                                                    }"
                                                                )
                                                                if (!pauseSearchHistory) {
                                                                    database.query {
                                                                        insert(SearchHistory(query = it))
                                                                    }
                                                                }
                                                            },
                                                            onDismiss = { onActiveChange(false) },
                                                            pureBlack = pureBlack
                                                        )
                                                }
                                            }
                                        }
                                    }
                                },
                                bottomBar = {
                                    Box {
                                        BottomSheetPlayer(
                                            state = playerBottomSheetState,
                                            navController = navController,
                                            pureBlack = pureBlack
                                        )

                                        if(useRail) return@Box

                        val navSlideDistance =
                            bottomInset + floatingBarsBottomPadding + navVisibleHeight

                        Box(
                            modifier =
                                Modifier
                                    .align(Alignment.BottomCenter)
                                    .height(navSlideDistance)
                                    .offset {
                                        if (bottomNavigationBarHeight == 0.dp) {
                                            IntOffset(
                                                x = 0,
                                                y = navSlideDistance.roundToPx(),
                                            )
                                        } else {
                                            val slideOffset =
                                                navSlideDistance *
                                                    playerBottomSheetState.progress.coerceIn(
                                                        0f,
                                                        1f,
                                                    )
                                            val hideOffset =
                                                navSlideDistance *
                                                    (1 - bottomNavigationBarHeight.coerceAtMost(navVisibleHeight) / navVisibleHeight)
                                            IntOffset(
                                                x = 0,
                                                y = (slideOffset + hideOffset).roundToPx(),
                                            )
                                        }
                                    },
                        ) {
                            FloatingNavigationToolbar(
                                items = navigationItems,
                                pureBlack = pureBlack,
                                liquidGlass = liquidGlassNavBar,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(
                                        start = FloatingToolbarHorizontalPadding,
                                        end = FloatingToolbarHorizontalPadding,
                                        bottom = bottomInset + floatingBarsBottomPadding,
                                    )
                                    .height(navVisibleHeight),
                                onShuffleClick = if (shouldShowHomeShuffleButton) {
                                    {
                                        val useLocalSource = when {
                                            allLocalItems.isNotEmpty() && allYtItems.isNotEmpty() -> Random.nextFloat() < 0.5f
                                            allLocalItems.isNotEmpty() -> true
                                            else -> false
                                        }

                                        coroutineScope.launch(Dispatchers.Main) {
                                            if (useLocalSource) {
                                                when (val luckyItem = allLocalItems.random()) {
                                                    is Song -> {
                                                        playerConnection?.playQueue(
                                                            YouTubeQueue.radio(luckyItem.toMediaMetadata())
                                                        )
                                                    }

                                                    is Album -> {
                                                        val albumWithSongs = withContext(Dispatchers.IO) {
                                                            database.albumWithSongs(luckyItem.id).first()
                                                        }

                                                        albumWithSongs?.let {
                                                            playerConnection?.playQueue(LocalAlbumRadio(it))
                                                        }
                                                    }

                                                    is Artist -> Unit
                                                    is Playlist -> Unit
                                                }
                                            } else {
                                                when (val luckyItem = allYtItems.random()) {
                                                    is SongItem -> {
                                                        playerConnection?.playQueue(
                                                            YouTubeQueue.radio(luckyItem.toMediaMetadata())
                                                        )
                                                    }

                                                    is AlbumItem -> {
                                                        playerConnection?.playQueue(
                                                            YouTubeAlbumRadio(luckyItem.playlistId)
                                                        )
                                                    }

                                                    is ArtistItem -> {
                                                        luckyItem.radioEndpoint?.let {
                                                            playerConnection?.playQueue(YouTubeQueue(it))
                                                        }
                                                    }

                                                    is PlaylistItem -> {
                                                        luckyItem.playEndpoint?.let {
                                                            playerConnection?.playQueue(YouTubeQueue(it))
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else null,
                                shuffleIconRes = if (shouldShowHomeShuffleButton) R.drawable.shuffle else null,
                                shuffleContentDescription = if (shouldShowHomeShuffleButton) stringResource(R.string.shuffle) else "",
                                onMusicRecognitionClick = if (shouldShowHomeShuffleButton) {
                                    { navController.navigate(MusicRecognitionRoute) }
                                } else null,
                                musicRecognitionContentDescription = if (shouldShowHomeShuffleButton) stringResource(R.string.music_recognition) else "",
                                isSelected = { screen ->
                                    navBackStackEntry?.destination?.hierarchy?.any { it.route == screen.route } ==
                                            true
                                },
                                onItemClick = { screen, isSelected ->
                                    if (screen.route == Screens.Search.route) {
                                        onActiveChange(true)
                                    } else if (isSelected) {
                                        navController.currentBackStackEntry?.savedStateHandle?.set(
                                            "scrollToTop",
                                            true
                                        )
                                        coroutineScope.launch {
                                            searchBarScrollBehavior.state.resetHeightOffset()
                                        }
                                    } else {
                                        navController.navigate(screen.route) {
                                            popUpTo(navController.graph.startDestinationId) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                            )
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .nestedScroll(searchBarScrollBehavior.nestedScrollConnection)
                            ) {
                                var transitionDirection =
                                    AnimatedContentTransitionScope.SlideDirection.Left

                                if (navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route }) {
                                    if (navigationItems.fastAny { it.route == previousTab }) {
                                        val curIndex = navigationItems.indexOf(
                                            navigationItems.fastFirstOrNull {
                                                it.route == navBackStackEntry?.destination?.route
                                            }
                                        )

                                        val prevIndex = navigationItems.indexOf(
                                            navigationItems.fastFirstOrNull {
                                                it.route == previousTab
                                            }
                                        )

                                        if (prevIndex > curIndex)
                                            AnimatedContentTransitionScope.SlideDirection.Right.also {
                                                transitionDirection = it
                                            }
                                    }
                                }

                                NavHost(
                                    navController = navController,
                                    startDestination = when (tabOpenedFromShortcut ?: defaultOpenTab) {
                                        NavigationTab.HOME -> Screens.Home
                                        NavigationTab.LIBRARY -> Screens.Library
                                        else -> Screens.Home
                                    }.route,
                                    enterTransition = {
                                        if (initialState.destination.route in topLevelScreens && targetState.destination.route in topLevelScreens) {
                                            fadeIn(tween(250))
                                        } else {
                                            fadeIn(tween(250)) + slideInHorizontally { it / 2 }
                                        }
                                    },
                                    exitTransition = {
                                        if (initialState.destination.route in topLevelScreens && targetState.destination.route in topLevelScreens) {
                                            fadeOut(tween(200))
                                        } else {
                                            fadeOut(tween(200)) + slideOutHorizontally { -it / 2 }
                                        }
                                    },
                                    popEnterTransition = {
                                        if ((initialState.destination.route in topLevelScreens || initialState.destination.route?.startsWith("search/") == true) && targetState.destination.route in topLevelScreens) {
                                            fadeIn(tween(250))
                                        } else {
                                            fadeIn(tween(250)) + slideInHorizontally { -it / 2 }
                                        }
                                    },
                                    popExitTransition = {
                                        if ((initialState.destination.route in topLevelScreens || initialState.destination.route?.startsWith("search/") == true) && targetState.destination.route in topLevelScreens) {
                                            fadeOut(tween(200))
                                        } else {
                                            fadeOut(tween(200)) + slideOutHorizontally { it / 2 }
                                        }
                                    },
                                    modifier = Modifier.nestedScroll(
                                        if (navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route } ||
                                            navBackStackEntry?.destination?.route?.startsWith("search/") == true
                                        ) {
                                            searchBarScrollBehavior.nestedScrollConnection
                                        } else {
                                            topAppBarScrollBehavior.nestedScrollConnection
                                        }
                                    )
                                ) {
                                    navigationBuilder(
                                        navController,
                                        topAppBarScrollBehavior,
                                        latestVersionName
                                    )
                                }
                            }
                        }

                        BottomSheetMenu(
                            state = LocalMenuState.current,
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )

                        BottomSheetPage(
                            state = LocalBottomSheetPageState.current,
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )

                        if (showAccountDialog) {
                            AccountSettingsDialog(
                                navController = navController,
                                onDismiss = { showAccountDialog = false },
                                latestVersionName = latestVersionName
                            )
                        }

                        sharedSong?.let { song ->
                            playerConnection?.let {
                                Dialog(
                                    onDismissRequest = { sharedSong = null },
                                    properties = DialogProperties(usePlatformDefaultWidth = false),
                                ) {
                                    Surface(
                                        modifier = Modifier.padding(24.dp),
                                        shape = RoundedCornerShape(16.dp),
                                        color = AlertDialogDefaults.containerColor,
                                        tonalElevation = AlertDialogDefaults.TonalElevation,
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                        ) {
                                            YouTubeSongMenu(
                                                song = song,
                                                navController = navController,
                                                onDismiss = { sharedSong = null },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    LaunchedEffect(shouldShowSearchBar, openSearchImmediately) {
                        if (shouldShowSearchBar && openSearchImmediately) {
                            onActiveChange(true)
                            try {
                                delay(100)
                                searchBarFocusRequester.requestFocus()
                            } catch (_: Exception) {
                            }
                            openSearchImmediately = false
                        }
                    }
                }
            }
        }
    }

    private fun handleDeepLinkIntent(intent: Intent, navController: NavHostController) {
        val uri = intent.data ?: intent.extras?.getString(Intent.EXTRA_TEXT)?.toUri() ?: return
        val coroutineScope = lifecycleScope

        val authority = uri.authority?.lowercase()
        if (uri.scheme.equals("OpenTune", ignoreCase = true) && authority == "together") {
            pendingTogetherJoinLink = uri.toString()
            startMusicServiceSafely()
            joinPendingTogetherIfReady()
            return
        }

        if (uri.scheme.equals("OpenTune", ignoreCase = true) && authority == "login") {
            navController.navigate(buildLoginRoute(uri.getQueryParameter(LOGIN_URL_ARGUMENT)))
            return
        }

        when (val path = uri.pathSegments.firstOrNull()) {
            "playlist" -> uri.getQueryParameter("list")?.let { playlistId ->
                if (playlistId.startsWith("OLAK5uy_")) {
                    coroutineScope.launch {
                        YouTube.albumSongs(playlistId).onSuccess { songs ->
                            songs.firstOrNull()?.album?.id?.let { browseId ->
                                navController.navigate("album/$browseId")
                            }
                        }.onFailure { reportException(it) }
                    }
                } else {
                    navController.navigate("online_playlist/$playlistId")
                }
            }

            "browse" -> uri.lastPathSegment?.let { browseId ->
                navController.navigate("album/$browseId")
            }

            "channel", "c" -> uri.lastPathSegment?.let { artistId ->
                navController.navigate("artist/$artistId")
            }

            else -> {
                val videoId = when {
                    path == "watch" -> uri.getQueryParameter("v")
                    uri.host == "youtu.be" -> uri.pathSegments.firstOrNull()
                    else -> null
                }
                
                val playlistId = uri.getQueryParameter("list")

                videoId?.let { vid ->
                    coroutineScope.launch {
                        val result = withContext(Dispatchers.IO) {
                            YouTube.queue(listOf(vid), playlistId)
                        }

                        result.onSuccess { queued ->
                            val mediaItem =
                                queued.firstOrNull { it.id == vid }?.toMediaItem()
                                    ?: queued.firstOrNull()?.toMediaItem()
                                    ?: MediaItem
                                        .Builder()
                                        .setMediaId(vid)
                                        .setUri(vid)
                                        .setCustomCacheKey(vid)
                                        .build()
                            pendingDeepLinkSong =
                                PendingDeepLinkSong(
                                    mediaItem = mediaItem,
                                )
                            startMusicServiceSafely()
                            playPendingDeepLinkSongIfReady()
                        }.onFailure {
                            reportException(it)
                        }
                    }
                }
            }
        }
    }

    private fun startMusicServiceSafely() {
        runCatching { startService(Intent(this, MusicService::class.java)) }
            .onFailure { reportException(it) }
    }

    @SuppressLint("ObsoleteSdkInt")
    private fun setSystemBarAppearance(isDark: Boolean) {
        WindowCompat.getInsetsController(window, window.decorView.rootView).apply {
            isAppearanceLightStatusBars = !isDark
            isAppearanceLightNavigationBars = !isDark
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            window.statusBarColor =
                (if (isDark) Color.Transparent else Color.Black.copy(alpha = 0.2f)).toArgb()
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            window.navigationBarColor =
                (if (isDark) Color.Transparent else Color.Black.copy(alpha = 0.2f)).toArgb()
        }
    }

    companion object {
        const val ACTION_SEARCH = "com.arturo254.opentune.action.SEARCH"
        const val ACTION_LIBRARY = "com.arturo254.opentune.action.LIBRARY"
    }
}

val LocalDatabase = staticCompositionLocalOf<MusicDatabase> { error("No database provided") }
val LocalPlayerConnection =
    staticCompositionLocalOf<PlayerConnection?> { error("No PlayerConnection provided") }
val LocalPlayerAwareWindowInsets =
    compositionLocalOf<WindowInsets> { error("No WindowInsets provided") }
val LocalDownloadUtil = staticCompositionLocalOf<DownloadUtil> { error("No DownloadUtil provided") }
val LocalSyncUtils = staticCompositionLocalOf<SyncUtils> { error("No SyncUtils provided") }
