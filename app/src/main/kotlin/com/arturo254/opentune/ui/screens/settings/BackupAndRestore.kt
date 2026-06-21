/*
 * OpenTune Project Original (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.arturo254.opentune.ui.screens.settings

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Message
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.arturo254.opentune.LocalPlayerAwareWindowInsets
import com.arturo254.opentune.LocalPlayerConnection
import com.arturo254.opentune.R
import com.arturo254.opentune.constants.ShowSpotifyPlaylistsKey
import com.arturo254.opentune.db.entities.Song
import com.arturo254.opentune.spotify.SpotifyAccountUiState
import com.arturo254.opentune.spotify.SpotifyAccountViewModel
import com.arturo254.opentune.spotify.SpotifyAuth
import com.arturo254.opentune.ui.component.Material3SettingsGroup
import com.arturo254.opentune.ui.component.Material3SettingsItem
import com.arturo254.opentune.ui.component.NewActionButton
import com.arturo254.opentune.ui.component.IconButton
import com.arturo254.opentune.ui.menu.AddToPlaylistDialogOnline
import com.arturo254.opentune.ui.menu.LoadingScreen
import com.arturo254.opentune.ui.utils.backToMain
import com.arturo254.opentune.utils.rememberPreference
import com.arturo254.opentune.utils.resetAuthWebViewSession
import com.arturo254.opentune.viewmodels.BackupRestoreViewModel
import com.arturo254.opentune.viewmodels.CloudUploadState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val CSV_MIME_TYPES =
    arrayOf(
        "text/csv",
        "text/x-csv",
        "text/comma-separated-values",
        "text/x-comma-separated-values",
        "application/csv",
        "application/x-csv",
        "application/vnd.ms-excel",
        "text/plain",
        "text/*",
        "application/octet-stream",
    )

private val SpotifyAccountIconSize = 44.dp
private const val SpotifyLoginUserAgent =
    "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"

@SuppressLint("LocalContextGetResourceValueCall")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupAndRestore(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: BackupRestoreViewModel = hiltViewModel(),
    spotifyAccountViewModel: SpotifyAccountViewModel = hiltViewModel(),
) {
    var importedTitle by remember { mutableStateOf("") }
    val importedSongs = remember { mutableStateListOf<Song>() }
    var showChoosePlaylistDialogOnline by rememberSaveable { mutableStateOf(false) }
    var isProgressStarted by rememberSaveable { mutableStateOf(false) }
    var progressStatus by remember { mutableStateOf("") }
    var progressPercentage by rememberSaveable { mutableIntStateOf(0) }
    var showSpotifyLogin by rememberSaveable { mutableStateOf(false) }

    val spotifyState by spotifyAccountViewModel.uiState.collectAsState()
    val (showSpotifyPlaylists, onShowSpotifyPlaylistsChange) = rememberPreference(
        ShowSpotifyPlaylistsKey,
        false
    )

    val backupRestoreProgress by viewModel.backupRestoreProgress.collectAsState()
    val cloudState by viewModel.cloudUploadState.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    // Cargar estado inicial del switch
    LaunchedEffect(Unit) {
        val enabled = viewModel.loadCloudBackupEnabled(context)
        viewModel.setCloudBackupEnabled(context, enabled)
    }

    // ── Spotify auto-logout on authentication ────────────────────────────────
    LaunchedEffect(spotifyState.isAuthenticated) {
        if (spotifyState.isAuthenticated) {
            showSpotifyLogin = false
        }
    }

    val backupLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
            if (uri != null) {
                viewModel.backup(context, uri)
                // Si el switch está activado, subir a la nube después del backup
                if (cloudState.isEnabled) {
                    coroutineScope.launch {
                        delay(2000) // Esperar a que termine el backup
                        viewModel.uploadExistingBackupToCloud(context, uri)
                    }
                }
            }
        }

    val restoreLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) viewModel.restore(context, uri)
        }

    val importPlaylistFromCsv =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            coroutineScope.launch {
                val result = viewModel.importPlaylistFromCsv(context, uri)
                importedSongs.clear()
                importedSongs.addAll(result)
                if (importedSongs.isNotEmpty()) showChoosePlaylistDialogOnline = true
            }
        }

    val importM3uLauncherOnline =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            coroutineScope.launch {
                val result = viewModel.loadM3UOnline(context, uri)
                importedSongs.clear()
                importedSongs.addAll(result)
                if (importedSongs.isNotEmpty()) showChoosePlaylistDialogOnline = true
            }
        }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.backup_restore)) },
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
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .padding(innerPadding)
                .windowInsetsPadding(
                    LocalPlayerAwareWindowInsets.current.only(
                        WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
                    )
                ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp, bottom = 16.dp, top = 8.dp
            ),
        ) {

            // ── Backup / Restore card ─────────────────────────────────────────
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                            ) {
                                Box(
                                    modifier = Modifier.size(52.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.backup),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(26.dp),
                                    )
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.backup_restore),
                                    style = MaterialTheme.typography.titleLarge,
                                )
                                Row(
                                    modifier = Modifier
                                        .padding(top = 10.dp)
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    AssistChip(
                                        onClick = {},
                                        label = { Text(".backup") },
                                        colors = AssistChipDefaults.assistChipColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        ),
                                    )
                                    AssistChip(
                                        onClick = {},
                                        label = { Text(".m3u") },
                                        colors = AssistChipDefaults.assistChipColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        ),
                                    )
                                    AssistChip(
                                        onClick = {},
                                        label = { Text(".csv") },
                                        colors = AssistChipDefaults.assistChipColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        ),
                                    )
                                }
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            NewActionButton(
                                icon = {
                                    Icon(
                                        painter = painterResource(R.drawable.backup),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    )
                                },
                                text = stringResource(R.string.action_backup),
                                onClick = {
                                    val formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
                                    backupLauncher.launch(
                                        "${context.getString(R.string.app_name)}_${
                                            LocalDateTime.now().format(formatter)
                                        }.backup"
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                backgroundColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                            NewActionButton(
                                icon = {
                                    Icon(
                                        painter = painterResource(R.drawable.restore),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    )
                                },
                                text = stringResource(R.string.action_restore),
                                onClick = { restoreLauncher.launch(arrayOf("application/octet-stream")) },
                                modifier = Modifier.weight(1f),
                                backgroundColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        }
                    }
                }
            }

            // ── Cloud Backup card ─────────────────────────────────────────────
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer,
                            ) {
                                Box(
                                    modifier = Modifier.size(52.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.cloud_upload),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.size(26.dp),
                                    )
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.cloud_backup),
                                    style = MaterialTheme.typography.titleLarge,
                                )
                                Text(
                                    text = stringResource(R.string.cloud_backup_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 4.dp),
                                )
                            }

                            Switch(
                                checked = cloudState.isEnabled,
                                onCheckedChange = { enabled ->
                                    viewModel.setCloudBackupEnabled(context, enabled)
                                },
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }

                        // Mostrar estado de subida
                        AnimatedVisibility(
                            visible = cloudState.isUploading,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically(),
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                LinearProgressIndicator(
                                    progress = { cloudState.uploadProgress / 100f },
                                    modifier = Modifier.fillMaxWidth(),
                                    color = MaterialTheme.colorScheme.secondary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                    strokeCap = StrokeCap.Round,
                                )
                                Text(
                                    text = stringResource(R.string.cloud_uploading, cloudState.uploadProgress),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        // Mostrar último enlace si existe
                        AnimatedVisibility(
                            visible = !cloudState.isUploading && cloudState.lastUploadUrl != null,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically(),
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.link),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(16.dp),
                                        )
                                        Text(
                                            text = stringResource(R.string.last_cloud_url),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        )
                                    }

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = cloudState.lastUploadUrl.orEmpty(),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable {
                                                    clipboardManager.setText(
                                                        AnnotatedString(
                                                            cloudState.lastUploadUrl.orEmpty()
                                                        )
                                                    )
                                                    android.widget.Toast.makeText(
                                                        context,
                                                        R.string.link_copied,
                                                        android.widget.Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                        )

                                        AssistChip(
                                            onClick = {
                                                clipboardManager.setText(AnnotatedString(cloudState.lastUploadUrl.orEmpty()))
                                                android.widget.Toast.makeText(context, R.string.link_copied, android.widget.Toast.LENGTH_SHORT).show()
                                            },
                                            label = { Text(stringResource(R.string.copy_link)) },
                                            leadingIcon = {
                                                Icon(
                                                    painter = painterResource(R.drawable.content_copy),
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            },
                                            colors = AssistChipDefaults.assistChipColors(
                                                containerColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f),
                                                labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        // Mostrar error si existe
                        AnimatedVisibility(
                            visible = cloudState.lastError != null && !cloudState.isUploading,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically(),
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.errorContainer,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.error),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Text(
                                        text = stringResource(R.string.cloud_upload_error_detail, cloudState.lastError.orEmpty()),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── Import playlist ───────────────────────────────────────────────
            item {
                Material3SettingsGroup(
                    title = stringResource(R.string.import_playlist),
                    items = listOf(
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.playlist_add),
                            title = {
                                Text(
                                    text = stringResource(R.string.import_online),
                                    style = MaterialTheme.typography.titleMedium,
                                )
                            },
                            description = {
                                Text(
                                    text = "audio/*",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                            onClick = { importM3uLauncherOnline.launch(arrayOf("audio/*")) },
                        ),
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.playlist_add),
                            title = {
                                Text(
                                    text = stringResource(R.string.import_csv),
                                    style = MaterialTheme.typography.titleMedium,
                                )
                            },
                            description = {
                                Text(
                                    text = "text/csv",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                            onClick = { importPlaylistFromCsv.launch(CSV_MIME_TYPES) },
                        ),
                    )
                )
            }

            // ── Spotify Account card ──────────────────────────────────────────
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        // Header
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                            ) {
                                Box(
                                    modifier = Modifier.size(52.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.spotify_icon),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                        modifier = Modifier.size(26.dp),
                                    )
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.spotify_account),
                                    style = MaterialTheme.typography.titleLarge,
                                )
                                Text(
                                    text = if (spotifyState.isAuthenticated) {
                                        if (spotifyState.accountName.isNotBlank()) {
                                            stringResource(
                                                R.string.spotify_connected_as,
                                                spotifyState.accountName
                                            )
                                        } else {
                                            stringResource(R.string.spotify_connected)
                                        }
                                    } else {
                                        stringResource(R.string.spotify_not_connected)
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 2.dp),
                                )
                            }

                            AnimatedVisibility(visible = spotifyState.isLoading) {
                                CircularWavyProgressIndicator(
                                    modifier = Modifier.size(28.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }

                        // Spotify account info when connected
                        if (spotifyState.isAuthenticated) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                            ) {
                                SpotifyAccountIcon(avatarUrl = spotifyState.accountAvatarUrl)

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = spotifyState.accountName.takeIf { it.isNotBlank() }
                                            ?: stringResource(R.string.spotify_account),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                    )
                                    Text(
                                        text = if (spotifyState.playlistCount > 0) {
                                            stringResource(
                                                R.string.spotify_available_count,
                                                spotifyState.playlistCount
                                            )
                                        } else {
                                            stringResource(R.string.spotify_no_sources)
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }

                            // Switch para mostrar playlists
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.spotify_icon),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp),
                                )
                                Text(
                                    text = stringResource(R.string.spotify_show_playlist),
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f),
                                )
                                Switch(
                                    checked = showSpotifyPlaylists,
                                    onCheckedChange = onShowSpotifyPlaylistsChange,
                                    enabled = !spotifyState.isLoading,
                                )
                            }

                            // Botones de acción
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                NewActionButton(
                                    icon = {
                                        Icon(
                                            painter = painterResource(R.drawable.sync),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                        )
                                    },
                                    text = stringResource(R.string.spotify_reload_playlist),
                                    onClick = { spotifyAccountViewModel.reloadPlaylists() },
                                    modifier = Modifier.weight(1f),
                                    backgroundColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                    enabled = !spotifyState.isLoading,
                                )
                                NewActionButton(
                                    icon = {
                                        Icon(
                                            painter = painterResource(R.drawable.logout),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onErrorContainer,
                                        )
                                    },
                                    text = stringResource(R.string.action_logout),
                                    onClick = { spotifyAccountViewModel.logout() },
                                    modifier = Modifier.weight(1f),
                                    backgroundColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                                    enabled = !spotifyState.isLoading,
                                )
                            }
                        } else {
                            // Botón conectar
                            NewActionButton(
                                icon = {
                                    Icon(
                                        painter = painterResource(R.drawable.spotify_icon),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                    )
                                },
                                text = stringResource(R.string.spotify_connect),
                                onClick = { showSpotifyLogin = true },
                                modifier = Modifier.fillMaxWidth(),
                                backgroundColor = MaterialTheme.colorScheme.tertiaryContainer,
                                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                enabled = !spotifyState.isLoading,
                            )
                        }

                        // Mostrar error de Spotify
                        spotifyState.errorMessage?.let { error ->
                            AnimatedVisibility(
                                visible = true,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically(),
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.error),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onErrorContainer,
                                            modifier = Modifier.size(18.dp),
                                        )
                                        Text(
                                            text = error,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onErrorContainer,
                                            modifier = Modifier.weight(1f)
                                        )
                                        TextButton(
                                            onClick = { spotifyAccountViewModel.dismissError() },
                                            colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                                                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                                            ),
                                        ) {
                                            Text(stringResource(android.R.string.ok))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            // ─────────────────────────────────────────────────────────────────
        }
    }

    AddToPlaylistDialogOnline(
        isVisible = showChoosePlaylistDialogOnline,
        allowSyncing = false,
        initialTextFieldValue = importedTitle,
        songs = importedSongs,
        onDismiss = { showChoosePlaylistDialogOnline = false },
        onProgressStart = { newVal -> isProgressStarted = newVal },
        onPercentageChange = { newPercentage -> progressPercentage = newPercentage },
        onStatusChange = { progressStatus = it }
    )

    LaunchedEffect(progressPercentage, isProgressStarted) {
        if (isProgressStarted && progressPercentage == 99) {
            delay(10000)
            if (progressPercentage == 99) {
                isProgressStarted = false
                progressPercentage = 0
            }
        }
    }

    LoadingScreen(
        isVisible = backupRestoreProgress != null || isProgressStarted,
        value = backupRestoreProgress?.percent ?: progressPercentage,
        title = backupRestoreProgress?.title,
        stepText = backupRestoreProgress?.step ?: progressStatus,
        indeterminate = backupRestoreProgress?.indeterminate ?: false,
    )

    // ── Spotify Login Sheet ──────────────────────────────────────────────────
    if (showSpotifyLogin) {
        SpotifyLoginSheet(
            onDismiss = { showSpotifyLogin = false },
            onCookiesCaptured = { spDc, spKey ->
                showSpotifyLogin = false
                spotifyAccountViewModel.connectWithCookies(spDc = spDc, spKey = spKey)
            },
        )
    }
}

@Composable
private fun SpotifyAccountIcon(avatarUrl: String?) {
    val context = LocalContext.current
    val requestSize = with(LocalDensity.current) { SpotifyAccountIconSize.roundToPx() }
    val accountIcon = painterResource(R.drawable.spotify_icon)
    val imageRequest =
        remember(context, avatarUrl, requestSize) {
            avatarUrl
                ?.takeIf(String::isNotBlank)
                ?.let {
                    ImageRequest
                        .Builder(context)
                        .data(it)
                        .size(requestSize)
                        .build()
                }
        }

    Box(
        modifier =
            Modifier
                .size(SpotifyAccountIconSize)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        if (imageRequest != null) {
            AsyncImage(
                model = imageRequest,
                contentDescription = null,
                placeholder = accountIcon,
                error = accountIcon,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            Icon(
                painter = accountIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun SpotifyLoginSheet(
    onDismiss: () -> Unit,
    onCookiesCaptured: (spDc: String, spKey: String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var webView by remember { mutableStateOf<WebView?>(null) }
    var mainWebView by remember { mutableStateOf<WebView?>(null) }
    var captured by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            webView?.destroySpotifyLoginWebView()
            mainWebView?.takeIf { it !== webView }?.destroySpotifyLoginWebView()
            webView = null
            mainWebView = null
        }
    }

    BackHandler(enabled = webView != null) {
        val activeWebView = webView
        val rootWebView = mainWebView
        when {
            activeWebView?.canGoBack() == true -> {
                activeWebView.goBack()
            }

            activeWebView != null && rootWebView != null && activeWebView !== rootWebView -> {
                activeWebView.destroySpotifyLoginWebView()
                webView = rootWebView
            }

            else -> {
                onDismiss()
            }
        }
    }

    ModalBottomSheet(
        modifier = Modifier.fillMaxHeight(),
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.spotify_login_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.spotify_waiting_for_login),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            AndroidView(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(MaterialTheme.shapes.large),
                factory = { context ->
                    val container = FrameLayout(context)
                    val spotifyWebView =
                        WebView(context).apply {
                            val cookieManager = CookieManager.getInstance()
                            cookieManager.setAcceptCookie(true)
                            cookieManager.setAcceptThirdPartyCookies(this, true)
                            configureSpotifyLoginWebView()

                            fun captureCookies(url: String?): Boolean {
                                if (captured) return true
                                val cookies = readSpotifyCookies(cookieManager, url)
                                val spDc = cookies["sp_dc"].orEmpty()
                                if (spDc.isBlank()) return false
                                captured = true
                                cookieManager.flush()
                                onCookiesCaptured(spDc, cookies["sp_key"].orEmpty())
                                return true
                            }

                            webViewClient =
                                object : WebViewClient() {
                                    override fun shouldOverrideUrlLoading(
                                        view: WebView,
                                        request: WebResourceRequest,
                                    ): Boolean =
                                        shouldOverrideSpotifyLoginUrl(
                                            view = view,
                                            url = request.url?.toString(),
                                            captureCookies = { url -> captureCookies(url) },
                                        )

                                    @Deprecated("Deprecated in Java")
                                    override fun shouldOverrideUrlLoading(
                                        view: WebView,
                                        url: String?,
                                    ): Boolean =
                                        shouldOverrideSpotifyLoginUrl(
                                            view = view,
                                            url = url,
                                            captureCookies = { targetUrl -> captureCookies(targetUrl) },
                                        )

                                    override fun onPageStarted(
                                        view: WebView,
                                        url: String?,
                                        favicon: android.graphics.Bitmap?,
                                    ) {
                                        captureCookies(url)
                                    }

                                    override fun onPageFinished(
                                        view: WebView,
                                        url: String?,
                                    ) {
                                        captureCookies(url)
                                    }
                                }
                            webChromeClient =
                                SpotifyLoginWebChromeClient(
                                    container = container,
                                    parentWebView = this,
                                    captureCookies = { url -> captureCookies(url) },
                                    onActiveWebViewChanged = { activeWebView ->
                                        webView = activeWebView
                                    },
                                )
                            webView = this
                            mainWebView = this
                            resetAuthWebViewSession(context, this) {
                                loadUrl(SpotifyAuth.LOGIN_URL)
                            }
                        }
                    container.addView(
                        spotifyWebView,
                        FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        ),
                    )
                    container
                },
                update = {
                    webView = webView ?: mainWebView
                },
            )
        }
    }
}

private fun WebView.destroySpotifyLoginWebView() {
    stopLoading()
    loadUrl("about:blank")
    (parent as? ViewGroup)?.removeView(this)
    destroy()
}

@SuppressLint("SetJavaScriptEnabled")
private fun WebView.configureSpotifyLoginWebView() {
    settings.apply {
        javaScriptEnabled = true
        domStorageEnabled = true
        javaScriptCanOpenWindowsAutomatically = true
        setSupportMultipleWindows(true)
        setSupportZoom(true)
        builtInZoomControls = true
        displayZoomControls = false
        mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        userAgentString = SpotifyLoginUserAgent
    }
}

private class SpotifyLoginWebChromeClient(
    private val container: FrameLayout,
    private val parentWebView: WebView,
    private val captureCookies: (String?) -> Boolean,
    private val onActiveWebViewChanged: (WebView) -> Unit,
) : WebChromeClient() {
    override fun onCreateWindow(
        view: WebView,
        isDialog: Boolean,
        isUserGesture: Boolean,
        resultMsg: Message,
    ): Boolean {
        closePopupWebViews()

        val popupWebView =
            WebView(view.context).apply {
                val cookieManager = CookieManager.getInstance()
                cookieManager.setAcceptCookie(true)
                cookieManager.setAcceptThirdPartyCookies(this, true)
                configureSpotifyLoginWebView()
                webViewClient =
                    object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(
                            view: WebView,
                            request: WebResourceRequest,
                        ): Boolean =
                            shouldOverrideSpotifyLoginUrl(
                                view = view,
                                url = request.url?.toString(),
                                captureCookies = captureCookies,
                            )

                        @Deprecated("Deprecated in Java")
                        override fun shouldOverrideUrlLoading(
                            view: WebView,
                            url: String?,
                        ): Boolean =
                            shouldOverrideSpotifyLoginUrl(
                                view = view,
                                url = url,
                                captureCookies = captureCookies,
                            )

                        override fun onPageStarted(
                            view: WebView,
                            url: String?,
                            favicon: android.graphics.Bitmap?,
                        ) {
                            captureCookies(url)
                        }

                        override fun onPageFinished(
                            view: WebView,
                            url: String?,
                        ) {
                            captureCookies(url)
                        }
                    }
            }

        val transport = resultMsg.obj as? WebView.WebViewTransport ?: return false
        container.addView(
            popupWebView,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )
        popupWebView.bringToFront()
        popupWebView.requestFocus()
        onActiveWebViewChanged(popupWebView)
        transport.webView = popupWebView
        resultMsg.sendToTarget()
        return true
    }

    override fun onCloseWindow(window: WebView) {
        window.destroySpotifyLoginWebView()
        onActiveWebViewChanged(parentWebView)
    }

    private fun closePopupWebViews() {
        for (index in container.childCount - 1 downTo 0) {
            val child = container.getChildAt(index) as? WebView ?: continue
            if (child !== parentWebView) {
                child.destroySpotifyLoginWebView()
            }
        }
        onActiveWebViewChanged(parentWebView)
    }
}

private fun shouldOverrideSpotifyLoginUrl(
    view: WebView,
    url: String?,
    captureCookies: (String?) -> Boolean,
): Boolean {
    if (captureCookies(url)) return true

    val targetUrl = url?.takeIf(String::isNotBlank) ?: return false
    if (targetUrl.isWebViewLoadableUrl()) return false

    targetUrl.intentBrowserFallbackUrl()?.let { fallbackUrl -> view.loadUrl(fallbackUrl) }
    return true
}

private fun String.isWebViewLoadableUrl(): Boolean {
    val scheme = runCatching { Uri.parse(this).scheme?.lowercase() }.getOrNull()
    return scheme == "http" ||
            scheme == "https" ||
            scheme == "javascript" ||
            scheme == "data" ||
            scheme == "blob"
}

private fun String.intentBrowserFallbackUrl(): String? =
    runCatching { Intent.parseUri(this, Intent.URI_INTENT_SCHEME) }
        .getOrNull()
        ?.getStringExtra("browser_fallback_url")
        ?.takeIf { it.isWebViewLoadableUrl() }

private fun readSpotifyCookies(
    cookieManager: CookieManager,
    currentUrl: String?,
): Map<String, String> {
    val urls =
        linkedSetOf(
            "https://open.spotify.com",
            "https://accounts.spotify.com",
            "https://spotify.com",
        )
    currentUrl?.toSpotifyCookieOrigin()?.let(urls::add)
    val cookies = linkedMapOf<String, String>()
    cookieManager.flush()
    urls.forEach { url ->
        cookieManager
            .getCookie(url)
            ?.split(";")
            ?.map(String::trim)
            ?.filter(String::isNotBlank)
            ?.forEach { part ->
                val separator = part.indexOf('=')
                if (separator <= 0) return@forEach
                val key = part.substring(0, separator).trim()
                val value = part.substring(separator + 1).trim()
                if (key.isNotBlank()) {
                    cookies[key] = value
                }
            }
    }
    return cookies
}

private fun String.toSpotifyCookieOrigin(): String? {
    val uri = runCatching { Uri.parse(this) }.getOrNull() ?: return null
    val host = uri.host?.lowercase() ?: return null
    if (host != "spotify.com" && !host.endsWith(".spotify.com")) return null
    val scheme =
        uri.scheme
            ?.takeIf {
                it.equals("https", ignoreCase = true) || it.equals(
                    "http",
                    ignoreCase = true
                )
            }
            ?: "https"
    return "$scheme://$host"
}