/*
 * OpenTune Project Original (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.arturo254.opentune.ui.screens.settings

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.arturo254.opentune.R
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import com.arturo254.opentune.LocalPlayerAwareWindowInsets
import com.arturo254.opentune.constants.UpdateChannel
import com.arturo254.opentune.constants.UpdateChannelKey
import com.arturo254.opentune.ui.component.IconButton
import com.arturo254.opentune.ui.component.MarkdownText
import com.arturo254.opentune.ui.utils.backToMain
import com.arturo254.opentune.utils.NightlyInfo
import com.arturo254.opentune.utils.ReleaseInfo
import com.arturo254.opentune.utils.Updater
import com.arturo254.opentune.utils.dataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.compose.ui.platform.LocalLocale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangelogScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Estado para el canal actual
    var updateChannel by remember { mutableStateOf(UpdateChannel.STABLE) }

    // Estados para releases estables
    var releases by remember { mutableStateOf<List<ReleaseInfo>>(emptyList()) }

    // Estado para nightly
    var nightlyInfo by remember { mutableStateOf<NightlyInfo?>(null) }

    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    // Cargar el canal actual desde DataStore
    LaunchedEffect(Unit) {
        updateChannel = context.dataStore.data.map {
            it[UpdateChannelKey]?.let { value ->
                try {
                    UpdateChannel.valueOf(value)
                } catch (e: Exception) {
                    UpdateChannel.STABLE
                }
            } ?: UpdateChannel.STABLE
        }.first()
    }

    // Función para cargar el contenido según el canal
    suspend fun loadContent() {
        if (updateChannel == UpdateChannel.NIGHTLY) {
            // Usar el método existente de Updater para obtener nightly info
            Updater.getLatestReleaseInfo().onSuccess { releaseInfo ->
                nightlyInfo = NightlyInfo(
                    versionName = releaseInfo.tagName,
                    apkUrl = "", // No necesitamos la URL aquí
                    changelog = releaseInfo.body,
                    publishedAt = releaseInfo.publishedAt
                )
                error = null
            }.onFailure { e ->
                if (nightlyInfo == null) {
                    error = e.message ?: "Error loading nightly info"
                }
            }
        } else {
            // Cargar releases estables
            Updater.getAllReleases(forceRefresh = true).onSuccess { result ->
                releases = result
                error = null
            }.onFailure { e ->
                if (releases.isEmpty()) {
                    error = e.message ?: "Error loading releases"
                }
            }
        }
        isLoading = false
    }

    // Cargar datos iniciales cuando el canal cambie
    LaunchedEffect(updateChannel) {
        isLoading = true
        error = null

        if (updateChannel == UpdateChannel.STABLE) {
            // Intentar usar caché primero para releases estables
            val cachedReleases = Updater.getCachedReleases()
            if (cachedReleases.isNotEmpty()) {
                releases = cachedReleases
                isLoading = false
            }
        }

        loadContent()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (updateChannel == UpdateChannel.NIGHTLY)
                            "Nightly Changelog"
                        else
                            stringResource(R.string.changelog)
                    )
                },
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
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .windowInsetsPadding(
                    LocalPlayerAwareWindowInsets.current.only(
                        WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
                    )
                )
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                error != null &&
                        ((updateChannel == UpdateChannel.STABLE && releases.isEmpty()) ||
                                (updateChannel == UpdateChannel.NIGHTLY && nightlyInfo == null)) -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Error loading changelog: ${error ?: "Unknown error"}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = {
                            isLoading = true
                            error = null
                            coroutineScope.launch {
                                loadContent()
                            }
                        }) {
                            Text(stringResource(R.string.retry))
                        }
                    }
                }

                updateChannel == UpdateChannel.NIGHTLY && nightlyInfo != null -> {
                    // Mostrar contenido nightly
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item { Spacer(modifier = Modifier.height(8.dp)) }

                        item {
                            NightlyChangelogCard(nightlyInfo = nightlyInfo!!)
                        }

                        item { Spacer(modifier = Modifier.height(8.dp)) }
                    }
                }

                updateChannel == UpdateChannel.STABLE && releases.isEmpty() -> {
                    Text(
                        text = stringResource(R.string.no_releases),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                updateChannel == UpdateChannel.STABLE -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item { Spacer(modifier = Modifier.height(8.dp)) }

                        items(releases) { release ->
                            ReleaseCard(release = release)
                        }

                        item { Spacer(modifier = Modifier.height(8.dp)) }
                    }
                }
            }
        }
    }
}

@SuppressLint("NonObservableLocale")
@Composable
private fun NightlyChangelogCard(nightlyInfo: NightlyInfo) {
    // Soporte para formato dd-MM-yyyy (como "30-05-2026")
    val inputDateFormat1 = SimpleDateFormat("dd-MM-yyyy", LocalLocale.current.platformLocale)
    val inputDateFormat2 = SimpleDateFormat("yyyy-MM-dd", LocalLocale.current.platformLocale)
    val displayDateFormat = SimpleDateFormat("MMMM d, yyyy", LocalLocale.current.platformLocale)

    val formattedDate = remember(nightlyInfo.publishedAt) {
        try {
            // Intentar con dd-MM-yyyy primero
            val date = inputDateFormat1.parse(nightlyInfo.publishedAt)
                ?: inputDateFormat2.parse(nightlyInfo.publishedAt)
            date?.let { displayDateFormat.format(it) } ?: nightlyInfo.publishedAt
        } catch (e: Exception) {
            nightlyInfo.publishedAt
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Nightly ${nightlyInfo.versionName}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Mostrar el changelog con soporte Markdown
            if (!nightlyInfo.changelog.isNullOrBlank()) {
                MarkdownText(
                    markdown = nightlyInfo.changelog,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            } else {
                Text(
                    text = "No changelog available for this nightly build.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ReleaseCard(release: ReleaseInfo) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val displayDateFormat = remember { SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()) }

    val formattedDate = remember(release.publishedAt) {
        try {
            val date = dateFormat.parse(release.publishedAt.substring(0, 10))
            date?.let { displayDateFormat.format(it) } ?: release.publishedAt
        } catch (e: Exception) {
            release.publishedAt
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = release.name.ifBlank { release.tagName },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!release.body.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                MarkdownText(
                    markdown = release.body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}