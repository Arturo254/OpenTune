/*
 * OpenTune Project Original (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.arturo254.opentune.ui.screens.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import com.arturo254.opentune.BuildConfig
import com.arturo254.opentune.LocalPlayerAwareWindowInsets
import com.arturo254.opentune.R
import com.arturo254.opentune.constants.EnableUpdateNotificationKey
import com.arturo254.opentune.constants.UpdateChannel
import com.arturo254.opentune.constants.UpdateChannelKey
import com.arturo254.opentune.ui.component.EnumListPreference
import com.arturo254.opentune.ui.component.IconButton
import com.arturo254.opentune.ui.component.PreferenceGroupTitle
import com.arturo254.opentune.ui.component.SwitchPreference
import com.arturo254.opentune.ui.utils.backToMain
import com.arturo254.opentune.utils.GitCommit
import com.arturo254.opentune.utils.UpdateInfo
import com.arturo254.opentune.utils.UpdateNotificationManager
import com.arturo254.opentune.utils.Updater
import com.arturo254.opentune.utils.rememberEnumPreference
import com.arturo254.opentune.utils.rememberPreference
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

// ─── Estado de comprobación de actualización ──────────────────────────────────

private sealed interface UpdateCheckState {
    data object Idle : UpdateCheckState
    data object Loading : UpdateCheckState
    data object UpToDate : UpdateCheckState
    data class UpdateAvailable(val info: UpdateInfo) : UpdateCheckState
    data class Error(val message: String) : UpdateCheckState
}

// ─── Pantalla ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val coroutineScope = rememberCoroutineScope()
    val nightlyInstallUrl =
        "https://github.com/Arturo254/OpenTune/releases/download/latest/app-release.apk"

    val (enableUpdateNotification, onEnableUpdateNotificationChange) = rememberPreference(
        EnableUpdateNotificationKey, defaultValue = false
    )
    val (updateChannel, onUpdateChannelChange) = rememberEnumPreference(
        UpdateChannelKey, defaultValue = UpdateChannel.STABLE
    )

    var commits by remember { mutableStateOf<List<GitCommit>>(emptyList()) }
    var isLoadingCommits by remember { mutableStateOf(true) }
    var latestVersion by remember { mutableStateOf<String?>(null) }
    var isExpanded by remember { mutableStateOf(true) }

    var updateCheckState by remember { mutableStateOf<UpdateCheckState>(UpdateCheckState.Idle) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var pendingUpdateInfo by remember { mutableStateOf<UpdateInfo?>(null) }

    var showNightlyConfirmDialog by remember { mutableStateOf(false) }
    var showNotifConfirmDialog by remember { mutableStateOf(false) }
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                        PackageManager.PERMISSION_GRANTED
            else true
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotificationPermission = granted
        if (granted) {
            onEnableUpdateNotificationChange(true)
            UpdateNotificationManager.schedulePeriodicUpdateCheck(context)
        }
    }

    // ── Función de comprobación ───────────────────────────────────────────────
    fun checkForUpdate() {
        if (updateCheckState == UpdateCheckState.Loading) return
        coroutineScope.launch {
            updateCheckState = UpdateCheckState.Loading
            Updater.checkForUpdate(BuildConfig.VERSION_NAME)
                .onSuccess { info ->
                    if (info == null) {
                        updateCheckState = UpdateCheckState.UpToDate
                    } else {
                        pendingUpdateInfo = info
                        updateCheckState = UpdateCheckState.UpdateAvailable(info)
                        showUpdateDialog = true
                    }
                }
                .onFailure { err ->
                    updateCheckState = UpdateCheckState.Error(err.message ?: "Error desconocido")
                }
        }
    }

    // ── Diálogos ──────────────────────────────────────────────────────────────
    if (showUpdateDialog) {
        pendingUpdateInfo?.let { info ->
            UpdateAvailableDialog(
                info = info,
                onDownload    = { showUpdateDialog = false; uriHandler.openUri(info.downloadUrl) },
                onViewRelease = { showUpdateDialog = false; uriHandler.openUri(info.releasePageUrl) },
                onDismiss     = { showUpdateDialog = false }
            )
        }
    }

    if (showNotifConfirmDialog) {
        BuildChannelInfoDialog(
            title = stringResource(R.string.enable_update_notification),
            onConfirm = {
                showNotifConfirmDialog = false
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission)
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                else {
                    onEnableUpdateNotificationChange(true)
                    UpdateNotificationManager.schedulePeriodicUpdateCheck(context)
                }
            },
            onDismiss = { showNotifConfirmDialog = false }
        )
    }

    if (showNightlyConfirmDialog) {
        BuildChannelInfoDialog(
            title = stringResource(R.string.channel_nightly),
            onConfirm = { showNightlyConfirmDialog = false; onUpdateChannelChange(UpdateChannel.NIGHTLY) },
            onDismiss = { showNightlyConfirmDialog = false }
        )
    }

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            Updater.getLatestVersionName().onSuccess { latestVersion = it }
            Updater.getCommitHistory(30).onSuccess { commits = it }.onFailure { commits = emptyList() }
            isLoadingCommits = false
        }
    }

    val rotationAngle by animateFloatAsState(if (isExpanded) 180f else 0f, label = "rotation")

    // ── Layout ────────────────────────────────────────────────────────────────
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.updates)) },
                navigationIcon = {
                    IconButton(
                        onClick = navController::navigateUp,
                        onLongClick = navController::backToMain
                    ) { Icon(painterResource(R.drawable.arrow_back), null) }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .windowInsetsPadding(
                    LocalPlayerAwareWindowInsets.current.only(
                        WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
                    )
                )
                .padding(horizontal = 16.dp)
        ) {

            // ── Tarjeta versión + botón comprobar ─────────────────────────────
            item {
                Spacer(Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.update),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(Modifier.width(16.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.current_version),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = BuildConfig.VERSION_NAME,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                latestVersion?.let { latest ->
                                    if (!Updater.isSameVersion(latest, BuildConfig.VERSION_NAME)) {
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            text = stringResource(R.string.latest_version_format, latest),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        // Botón con estado animado
                        UpdateCheckButton(
                            state = updateCheckState,
                            onClick = ::checkForUpdate
                        )

                        // Feedback textual bajo el botón
                        AnimatedVisibility(
                            visible = updateCheckState == UpdateCheckState.UpToDate ||
                                    updateCheckState is UpdateCheckState.Error
                        ) {
                            val (msg, color) = when (val s = updateCheckState) {
                                UpdateCheckState.UpToDate  ->
                                    "Ya tienes la versión más reciente." to MaterialTheme.colorScheme.tertiary
                                is UpdateCheckState.Error  ->
                                    "Error: ${s.message}" to MaterialTheme.colorScheme.error
                                else -> "" to MaterialTheme.colorScheme.onSurface
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(text = msg, style = MaterialTheme.typography.bodySmall, color = color)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // ── Preferencias ──────────────────────────────────────────────────
            item { PreferenceGroupTitle(title = stringResource(R.string.notification_settings)) }

            item {
                SwitchPreference(
                    title = { Text(stringResource(R.string.enable_update_notification)) },
                    description = stringResource(R.string.enable_update_notification_desc),
                    icon = { Icon(painterResource(R.drawable.new_release), null) },
                    checked = enableUpdateNotification,
                    onCheckedChange = { enabled ->
                        if (enabled) showNotifConfirmDialog = true
                        else {
                            onEnableUpdateNotificationChange(false)
                            UpdateNotificationManager.cancelPeriodicUpdateCheck(context)
                        }
                    }
                )
            }

            item {
                EnumListPreference(
                    title = { Text(stringResource(R.string.update_channel)) },
                    icon = { Icon(painterResource(R.drawable.tune), null) },
                    selectedValue = updateChannel,
                    valueText = { ch ->
                        when (ch) {
                            UpdateChannel.STABLE  -> stringResource(R.string.channel_stable)
                            UpdateChannel.NIGHTLY -> stringResource(R.string.channel_nightly)
                        }
                    },
                    onValueSelected = { ch ->
                        if (ch == UpdateChannel.NIGHTLY && updateChannel != UpdateChannel.NIGHTLY)
                            showNightlyConfirmDialog = true
                        else
                            onUpdateChannelChange(ch)
                    }
                )
            }

            item {
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { navController.navigate("settings/changelog") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(painterResource(R.drawable.update), null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.view_changelog))
                }
            }

            // ── Card nightly ──────────────────────────────────────────────────
            item {
                AnimatedVisibility(visible = updateChannel == UpdateChannel.NIGHTLY) {
                    val latestHash = commits.firstOrNull()?.sha ?: "—"
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Nightly Builds", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Latest features and fixes from the development branch. May contain experimental features and occasional bugs",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(10.dp))
                            Text(latestHash, style = MaterialTheme.typography.labelMedium, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(14.dp))
                            Button(onClick = { uriHandler.openUri(nightlyInstallUrl) }, modifier = Modifier.fillMaxWidth()) {
                                Text("Install")
                            }
                        }
                    }
                }
            }

            // ── Historial de commits ──────────────────────────────────────────
            item {
                Spacer(Modifier.height(16.dp))
                PreferenceGroupTitle(title = stringResource(R.string.commit_history))
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth().animateContentSize(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isExpanded = !isExpanded }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(painterResource(R.drawable.history), null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(12.dp))
                                Text(stringResource(R.string.recent_commits), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                            }
                            Icon(painterResource(R.drawable.expand_more), null, modifier = Modifier.rotate(rotationAngle))
                        }
                        AnimatedVisibility(visible = isExpanded) {
                            Column {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                                if (isLoadingCommits) {
                                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (isExpanded && !isLoadingCommits) {
                items(commits) { commit ->
                    CommitItem(commit = commit, onClick = { uriHandler.openUri(commit.url) })
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

// ─── UpdateCheckButton ────────────────────────────────────────────────────────

@Composable
private fun UpdateCheckButton(
    state: UpdateCheckState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        enabled = state != UpdateCheckState.Loading,
        modifier = modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = when (state) {
                UpdateCheckState.UpToDate           -> MaterialTheme.colorScheme.tertiary
                is UpdateCheckState.Error           -> MaterialTheme.colorScheme.error
                else                                -> MaterialTheme.colorScheme.primary
            }
        )
    ) {
        AnimatedContent(targetState = state, label = "btnContent") { s ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                when (s) {
                    UpdateCheckState.Loading -> {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        Spacer(Modifier.width(8.dp))
                        Text("Comprobando…")
                    }
                    UpdateCheckState.UpToDate -> {
                        Icon(painterResource(R.drawable.done), null, Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Al día")
                    }
                    is UpdateCheckState.UpdateAvailable -> {
                        Icon(painterResource(R.drawable.update), null, Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Ver actualización (${s.info.versionName})")
                    }
                    is UpdateCheckState.Error -> {
                        Text("Reintentar")
                    }
                    else -> {
                        Icon(painterResource(R.drawable.update), null, Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.updates))
                    }
                }
            }
        }
    }
}

// ─── UpdateAvailableDialog ────────────────────────────────────────────────────

@Composable
private fun UpdateAvailableDialog(
    info: UpdateInfo,
    onDownload: () -> Unit,
    onViewRelease: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(painterResource(R.drawable.update), null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text("Nueva versión disponible") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Column {
                        Text("Instalada", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(BuildConfig.VERSION_NAME, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }
                    Column {
                        Text("Disponible", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(info.versionName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
                if (info.releaseNotes?.isNotBlank() == true) {
                    HorizontalDivider()
                    Text("Novedades", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    val notes: String = info.releaseNotes?.take(300)
                        .let { info.releaseNotes?.length?.let { it1 -> if (it1 > 300) "$it…" else it } as String }
                    Text(
                        text = notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.height(120.dp).verticalScroll(rememberScrollState())
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onDownload) {
                Icon(painterResource(R.drawable.download), null, Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Descargar APK")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onViewRelease) { Text("Ver release") }
                TextButton(onClick = onDismiss)    { Text("Ahora no") }
            }
        }
    )
}

// ─── BuildChannelInfoDialog ───────────────────────────────────────────────────

@Composable
private fun BuildChannelInfoDialog(
    title: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                Text("OpenTune provides two download channels for builds:", style = MaterialTheme.typography.bodyMedium)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("• Stable builds", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text("Distributed via official GitHub Releases.", style = MaterialTheme.typography.bodySmall)
                    Text("These versions are tested and recommended for most users.", style = MaterialTheme.typography.bodySmall)
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("• Nightly builds", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text("Automatically generated development builds hosted via nightly.link.", style = MaterialTheme.typography.bodySmall)
                    Text("Nightly builds may include experimental features, unfinished changes, or temporary regressions.", style = MaterialTheme.typography.bodySmall)
                }
                Text("By continuing, you acknowledge that nightly builds may be unstable and use them at your own risk.", style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(android.R.string.ok)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) } }
    )
}

// ─── CommitItem ───────────────────────────────────────────────────────────────

@Composable
private fun CommitItem(commit: GitCommit, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        onClick = onClick
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.Top) {
            Box(Modifier.padding(top = 4.dp).size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(commit.message, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(commit.sha, style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.primary)
                    Text("•", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(commit.author, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (commit.date.isNotEmpty()) {
                        Text("•", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(formatCommitDate(commit.date), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Icon(painterResource(R.drawable.arrow_forward), null, Modifier.padding(start = 8.dp).size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
        }
    }
}

private fun formatCommitDate(isoDate: String): String = try {
    val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    SimpleDateFormat("MMM d", Locale.getDefault()).format(inputFormat.parse(isoDate)!!)
} catch (e: Exception) {
    isoDate.take(10)
}