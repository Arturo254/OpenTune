package com.arturo254.opentune.ui.component

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arturo254.opentune.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

// Data classes mejoradas
data class ChangelogState(
    val releases: List<Release> = emptyList(),
    val commits: List<Commit> = emptyList(),
    val isLoadingReleases: Boolean = true,
    val isLoadingCommits: Boolean = true,
    val releasesError: String? = null,
    val commitsError: String? = null,
    val lastUpdated: String? = null
)

data class Release(
    val tagName: String,
    val name: String,
    val body: String,
    val publishedAt: String,
    val isPrerelease: Boolean,
    val htmlUrl: String,
    val author: String? = null
)

data class Commit(
    val sha: String,
    val message: String,
    val author: String,
    val date: String,
    val htmlUrl: String,
    val shortSha: String = sha.take(7)
)

enum class ChangelogTab(val title: String) {
    RELEASES("Releases"),
    COMMITS("Commits")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangelogButton(
    viewModel: ChangelogViewModel = viewModel()
) {
    var showBottomSheet by remember { mutableStateOf(false) }

    SettingsCategoryItem(
        title = { Text(stringResource(R.string.Changelog)) },
        icon = painterResource(R.drawable.schedule),
        onClick = { showBottomSheet = true }
    )

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            dragHandle = {
                Surface(
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .width(32.dp)
                        .height(4.dp),
                    shape = RoundedCornerShape(2.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                ) {}
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                ChangelogScreen(viewModel)
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun ChangelogScreen(viewModel: ChangelogViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf(ChangelogTab.RELEASES) }

    LaunchedEffect(Unit) {
        viewModel.loadChangelog("Arturo254", "OpenTune")
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header con título principal
        Text(
            text = stringResource(R.string.changelogs),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        // Tabs mejoradas con Material Design 3
        ImprovedTabRow(
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it }
        )

        // Contenido basado en la tab seleccionada
        when (selectedTab) {
            ChangelogTab.RELEASES -> {
                ReleasesContent(
                    releases = uiState.releases,
                    isLoading = uiState.isLoadingReleases,
                    error = uiState.releasesError,
                    lastUpdated = uiState.lastUpdated,
                    onRetry = { viewModel.loadChangelog("Arturo254", "OpenTune") }
                )
            }

            ChangelogTab.COMMITS -> {
                CommitsContent(
                    commits = uiState.commits,
                    isLoading = uiState.isLoadingCommits,
                    error = uiState.commitsError,
                    lastUpdated = uiState.lastUpdated,
                    onRetry = { viewModel.loadChangelog("Arturo254", "OpenTune") }
                )
            }
        }
    }
}

@Composable
private fun ImprovedTabRow(
    selectedTab: ChangelogTab,
    onTabSelected: (ChangelogTab) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ChangelogTab.values().forEach { tab ->
                val isSelected = selectedTab == tab
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onTabSelected(tab) },
                    color = if (isSelected)
                        MaterialTheme.colorScheme.primary
                    else
                        Color.Transparent,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = tab.title,
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isSelected)
                            MaterialTheme.colorScheme.onPrimary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ReleasesContent(
    releases: List<Release>,
    isLoading: Boolean,
    error: String?,
    lastUpdated: String?,
    onRetry: () -> Unit
) {
    when {
        isLoading -> LoadingIndicator(message = "Cargando releases…")
        error != null -> ErrorContent(error, onRetry)
        releases.isEmpty() -> EmptyContent("No hay releases disponibles")
        else -> SuccessReleasesContent(releases, lastUpdated)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CommitsContent(
    commits: List<Commit>,
    isLoading: Boolean,
    error: String?,
    lastUpdated: String?,
    onRetry: () -> Unit
) {
    when {
        isLoading -> LoadingIndicator(message = "Cargando commits…")
        error != null -> ErrorContent(error, onRetry)
        commits.isEmpty() -> EmptyContent("No hay commits disponibles")
        else -> SuccessCommitsContent(commits, lastUpdated)
    }
}

@ExperimentalMaterial3ExpressiveApi
@Composable
private fun LoadingIndicator(
    message: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            androidx.compose.material3.LoadingIndicator(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ErrorContent(error: String, onRetry: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.schedule), // Cambiar por icono de error
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = "Error al cargar",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Button(
                onClick = onRetry,
                modifier = Modifier.align(Alignment.End),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text("Reintentar")
            }
        }
    }
}

@Composable
private fun EmptyContent(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.schedule), // Cambiar por icono apropiado
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SuccessReleasesContent(
    releases: List<Release>,
    lastUpdated: String?
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        LastUpdatedIndicator(lastUpdated)
        releases.forEach { release ->
            ReleaseCard(release = release)
        }
    }
}

@Composable
private fun SuccessCommitsContent(
    commits: List<Commit>,
    lastUpdated: String?
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LastUpdatedIndicator(lastUpdated)
        commits.forEach { commit ->
            CommitCard(commit = commit)
        }
    }
}

@Composable
private fun LastUpdatedIndicator(lastUpdated: String?) {
    lastUpdated?.let {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "Última actualización: $it",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun CommitCard(commit: Commit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer
                        ) {
                            Text(
                                text = commit.shortSha,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Text(
                            text = commit.author,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = commit.message.lines().first(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = formatDate(commit.date),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ReleaseCard(release: Release) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(
            containerColor = if (release.isPrerelease)
                MaterialTheme.colorScheme.tertiaryContainer
            else
                MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (expanded) 4.dp else 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = release.tagName,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (release.isPrerelease) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.tertiary
                            ) {
                                Text(
                                    text = "Pre-release",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onTertiary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                    if (release.name.isNotEmpty() && release.name != release.tagName) {
                        Text(
                            text = release.name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    release.author?.let { author ->
                        Text(
                            text = "por $author",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatDate(release.publishedAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Icon(
                        painter = painterResource(
                            if (expanded) R.drawable.schedule else R.drawable.schedule // Cambiar por iconos de expandir/contraer
                        ),
                        contentDescription = if (expanded) "Contraer" else "Expandir",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(20.dp)
                            .padding(top = 4.dp)
                    )
                }
            }

            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(animationSpec = tween(300)) + expandVertically(
                    animationSpec = tween(300)
                ),
                exit = fadeOut(animationSpec = tween(300)) + shrinkVertically(
                    animationSpec = tween(300)
                )
            ) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        thickness = 1.dp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    AdvancedMarkdownText(
                        markdown = release.body,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

// [El resto de las funciones de markdown permanecen igual...]
@Composable
fun AdvancedMarkdownText(
    markdown: String,
    modifier: Modifier = Modifier
) {
    val cleanedMarkdown = cleanMarkdown(markdown)
    val lines = cleanedMarkdown.lines()

    var inCodeBlock by remember { mutableStateOf(false) }
    var codeBlockContent by remember { mutableStateOf("") }
    var codeBlockLanguage by remember { mutableStateOf("") }
    var inList by remember { mutableStateOf(false) }
    var listItems by remember { mutableStateOf(mutableListOf<String>()) }

    Column(modifier = modifier) {
        for ((index, line) in lines.withIndex()) {
            val trimmedLine = line.trim()

            when {
                trimmedLine.startsWith("```") -> {
                    if (inList) {
                        ListContainer(listItems.toList())
                        listItems.clear()
                        inList = false
                    }

                    if (inCodeBlock) {
                        CodeBlock(
                            code = codeBlockContent.trimEnd(),
                            language = codeBlockLanguage
                        )
                        codeBlockContent = ""
                        codeBlockLanguage = ""
                        inCodeBlock = false
                    } else {
                        codeBlockLanguage = trimmedLine.substring(3).trim()
                        inCodeBlock = true
                    }
                }

                inCodeBlock -> {
                    codeBlockContent += line + "\n"
                }

                trimmedLine.matches(Regex("^#{1,6}\\s+.*")) -> {
                    if (inList) {
                        ListContainer(listItems.toList())
                        listItems.clear()
                        inList = false
                    }

                    val level = trimmedLine.takeWhile { it == '#' }.length
                    val text = trimmedLine.substring(level).trim()
                    HeaderText(text = text, level = level)
                }

                trimmedLine.matches(Regex("^[-*+]\\s+.*")) -> {
                    val content = trimmedLine.substring(2).trim()
                    if (!inList) {
                        inList = true
                        listItems.clear()
                    }
                    listItems.add(content)
                }

                trimmedLine.matches(Regex("^\\d+\\.\\s+.*")) -> {
                    val content = trimmedLine.substringAfter(". ").trim()
                    if (!inList) {
                        inList = true
                        listItems.clear()
                    }
                    listItems.add(content)
                }

                trimmedLine.startsWith("> ") -> {
                    if (inList) {
                        ListContainer(listItems.toList())
                        listItems.clear()
                        inList = false
                    }
                    BlockQuote(trimmedLine.substring(2))
                }

                trimmedLine.matches(Regex("^[-*_]{3,}$")) -> {
                    if (inList) {
                        ListContainer(listItems.toList())
                        listItems.clear()
                        inList = false
                    }
                    HorizontalRule()
                }

                trimmedLine.isEmpty() -> {
                    if (inList) {
                        ListContainer(listItems.toList())
                        listItems.clear()
                        inList = false
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                else -> {
                    if (inList) {
                        ListContainer(listItems.toList())
                        listItems.clear()
                        inList = false
                    }
                    FormattedText(trimmedLine)
                }
            }
        }

        if (inList && listItems.isNotEmpty()) {
            ListContainer(listItems.toList())
        }
    }
}

private fun cleanMarkdown(markdown: String): String {
    var cleaned = markdown

    cleaned = cleaned.replace(Regex("<[^>]+>"), "")
    cleaned = cleaned.replace(Regex("!\\[([^\\]]*)\\]\\([^)]*\\)"), "")
    cleaned = cleaned.replace(Regex("\\[([^\\]]+)\\]\\([^)]*\\)")) { matchResult ->
        matchResult.groupValues[1]
    }
    cleaned = cleaned.replace(Regex("\\[([^\\]]+)\\]\\[[^\\]]*\\]")) { matchResult ->
        matchResult.groupValues[1]
    }
    cleaned = cleaned.replace(Regex("^\\[[^\\]]+\\]:.*$", RegexOption.MULTILINE), "")

    val htmlEntities = mapOf(
        "&amp;" to "&",
        "&lt;" to "<",
        "&gt;" to ">",
        "&quot;" to "\"",
        "&apos;" to "'",
        "&nbsp;" to " ",
        "&#39;" to "'",
        "&#x27;" to "'",
        "&hellip;" to "...",
        "&mdash;" to "—",
        "&ndash;" to "–"
    )

    for ((entity, replacement) in htmlEntities) {
        cleaned = cleaned.replace(entity, replacement)
    }

    cleaned = cleaned.replace(Regex("\n{3,}"), "\n\n")
    return cleaned.trim()
}

@Composable
private fun HeaderText(text: String, level: Int) {
    val style = when (level) {
        1 -> MaterialTheme.typography.headlineLarge
        2 -> MaterialTheme.typography.headlineMedium
        3 -> MaterialTheme.typography.headlineSmall
        4 -> MaterialTheme.typography.titleLarge
        else -> MaterialTheme.typography.titleMedium
    }

    Text(
        text = text,
        style = style,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = (12 - level * 2).coerceAtLeast(4).dp)
    )
}

@Composable
private fun ListContainer(items: List<String>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items.forEach { item ->
                UnorderedListItem(item)
            }
        }
    }
}

@Composable
private fun UnorderedListItem(content: String) {
    Row(
        modifier = Modifier.padding(vertical = 2.dp),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            modifier = Modifier
                .size(6.dp)
                .padding(top = 8.dp),
            shape = RoundedCornerShape(3.dp),
            color = MaterialTheme.colorScheme.primary
        ) {}
        Spacer(modifier = Modifier.width(12.dp))
        FormattedText(
            text = content,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun BlockQuote(content: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(40.dp)
                    .background(MaterialTheme.colorScheme.primary)
            )
            FormattedText(
                text = content,
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontStyle = FontStyle.Italic
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CodeBlock(code: String, language: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            if (language.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                ) {
                    Text(
                        text = language,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
            Text(
                text = code.trimEnd(),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace
                ),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Composable
private fun HorizontalRule() {
    HorizontalDivider(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outline
    )
}

@Composable
private fun FormattedText(
    text: String,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyMedium,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    val annotatedString = buildAnnotatedString {
        parseMarkdownText(text, this)
    }

    Text(
        text = annotatedString,
        style = style,
        color = color,
        modifier = modifier.padding(vertical = 2.dp)
    )
}

private fun parseMarkdownText(text: String, builder: AnnotatedString.Builder) {
    var currentIndex = 0
    val processedText = text.trim()

    val patterns = listOf(
        Triple(
            Regex("`([^`]+)`"),
            { match: MatchResult ->
                builder.withStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                    )
                ) {
                    append(match.groupValues[1])
                }
            },
            1
        ),
        Triple(
            Regex("\\*\\*([^*]+)\\*\\*"),
            { match: MatchResult ->
                builder.withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(match.groupValues[1])
                }
            },
            2
        ),
        Triple(
            Regex("__([^_]+)__"),
            { match: MatchResult ->
                builder.withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(match.groupValues[1])
                }
            },
            2
        ),
        Triple(
            Regex("(?<!\\*)\\*([^*\\s][^*]*[^*\\s])\\*(?!\\*)"),
            { match: MatchResult ->
                builder.withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                    append(match.groupValues[1])
                }
            },
            3
        ),
        Triple(
            Regex("(?<!_)_([^_\\s][^_]*[^_\\s])_(?!_)"),
            { match: MatchResult ->
                builder.withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                    append(match.groupValues[1])
                }
            },
            3
        ),
        Triple(
            Regex("~~([^~]+)~~"),
            { match: MatchResult ->
                builder.withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                    append(match.groupValues[1])
                }
            },
            4
        )
    )

    val allMatches = patterns.flatMap { (pattern, handler, priority) ->
        pattern.findAll(processedText).map { match ->
            Triple(match, handler, priority)
        }
    }.sortedWith(compareBy({ it.first.range.first }, { it.third }))

    val processedRanges = mutableListOf<IntRange>()

    for ((match, handler, _) in allMatches) {
        val range = match.range
        val overlaps = processedRanges.any { it.intersect(range).isNotEmpty() }

        if (!overlaps) {
            if (range.first > currentIndex) {
                builder.append(processedText.substring(currentIndex, range.first))
            }
            handler(match)
            currentIndex = range.last + 1
            processedRanges.add(range)
        }
    }

    if (currentIndex < processedText.length) {
        builder.append(processedText.substring(currentIndex))
    }

    if (builder.length == 0) {
        builder.append(processedText)
    }
}

private fun formatDate(dateString: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
        val outputFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val date = inputFormat.parse(dateString)
        outputFormat.format(date ?: Date())
    } catch (e: Exception) {
        try {
            // Formato alternativo para commits
            val inputFormat2 = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            val date = inputFormat2.parse(dateString)
            outputFormat.format(date ?: Date())
        } catch (e2: Exception) {
            dateString
        }
    }
}

// ViewModel mejorado con funcionalidad de commits
class ChangelogViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ChangelogState())
    val uiState: StateFlow<ChangelogState> = _uiState.asStateFlow()

    private val cache = ConcurrentHashMap<String, Pair<Any, Long>>()
    private val cacheTimeMs = 30 * 60 * 1000 // 30 minutos

    fun loadChangelog(repoOwner: String, repoName: String) {
        viewModelScope.launch {
            // Cargar releases y commits en paralelo
            launch { loadReleases(repoOwner, repoName) }
            launch { loadCommits(repoOwner, repoName) }
        }
    }

    private suspend fun loadReleases(repoOwner: String, repoName: String) {
        _uiState.update { it.copy(isLoadingReleases = true, releasesError = null) }

        try {
            val cacheKey = "releases_$repoOwner/$repoName"
            val cachedData = getCachedData<List<Release>>(cacheKey)

            if (cachedData != null) {
                _uiState.update {
                    it.copy(
                        releases = cachedData,
                        isLoadingReleases = false,
                        lastUpdated = getCurrentTimestamp()
                    )
                }
                return
            }

            val releases = fetchReleases(repoOwner, repoName)
            cacheData(cacheKey, releases)

            _uiState.update {
                it.copy(
                    releases = releases,
                    isLoadingReleases = false,
                    lastUpdated = getCurrentTimestamp()
                )
            }
        } catch (e: Exception) {
            Log.e("ChangelogViewModel", "Error cargando releases", e)
            _uiState.update {
                it.copy(
                    isLoadingReleases = false,
                    releasesError = "Error al cargar releases: ${e.message}"
                )
            }
        }
    }

    private suspend fun loadCommits(repoOwner: String, repoName: String) {
        _uiState.update { it.copy(isLoadingCommits = true, commitsError = null) }

        try {
            val cacheKey = "commits_$repoOwner/$repoName"
            val cachedData = getCachedData<List<Commit>>(cacheKey)

            if (cachedData != null) {
                _uiState.update {
                    it.copy(
                        commits = cachedData,
                        isLoadingCommits = false,
                        lastUpdated = getCurrentTimestamp()
                    )
                }
                return
            }

            val commits = fetchCommits(repoOwner, repoName)
            cacheData(cacheKey, commits)

            _uiState.update {
                it.copy(
                    commits = commits,
                    isLoadingCommits = false,
                    lastUpdated = getCurrentTimestamp()
                )
            }
        } catch (e: Exception) {
            Log.e("ChangelogViewModel", "Error cargando commits", e)
            _uiState.update {
                it.copy(
                    isLoadingCommits = false,
                    commitsError = "Error al cargar commits: ${e.message}"
                )
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> getCachedData(key: String): T? {
        val cached = cache[key] ?: return null
        val (data, timestamp) = cached

        if (System.currentTimeMillis() - timestamp > cacheTimeMs) {
            cache.remove(key)
            return null
        }

        return data as? T
    }

    private fun cacheData(key: String, data: Any) {
        cache[key] = data to System.currentTimeMillis()
    }

    private suspend fun fetchReleases(owner: String, repo: String): List<Release> =
        withContext(Dispatchers.IO) {
            repeat(3) { attempt ->
                try {
                    val url = URL("https://api.github.com/repos/$owner/$repo/releases")
                    val connection = url.openConnection() as HttpURLConnection
                    connection.connectTimeout = 15000
                    connection.readTimeout = 15000
                    connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
                    connection.setRequestProperty("User-Agent", "OpenTune-App")

                    if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                        if (attempt == 2) throw IOException("Error HTTP: ${connection.responseCode}")
                        delay(1000)
                        return@repeat
                    }

                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val jsonArray = JSONArray(response)
                    val releases = mutableListOf<Release>()

                    for (i in 0 until jsonArray.length().coerceAtMost(15)) {
                        val releaseJson = jsonArray.getJSONObject(i)
                        val author = releaseJson.optJSONObject("author")?.optString("login")

                        releases.add(
                            Release(
                                tagName = releaseJson.optString("tag_name", ""),
                                name = releaseJson.optString("name", ""),
                                body = releaseJson.optString("body", ""),
                                publishedAt = releaseJson.optString("published_at", ""),
                                isPrerelease = releaseJson.optBoolean("prerelease", false),
                                htmlUrl = releaseJson.optString("html_url", ""),
                                author = author
                            )
                        )
                    }

                    return@withContext releases
                } catch (e: Exception) {
                    if (attempt == 2) throw e
                    delay(1000)
                }
            }

            throw IOException("No se pudo obtener la información después de los reintentos")
        }

    private suspend fun fetchCommits(owner: String, repo: String): List<Commit> =
        withContext(Dispatchers.IO) {
            repeat(3) { attempt ->
                try {
                    val url = URL("https://api.github.com/repos/$owner/$repo/commits?per_page=20")
                    val connection = url.openConnection() as HttpURLConnection
                    connection.connectTimeout = 15000
                    connection.readTimeout = 15000
                    connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
                    connection.setRequestProperty("User-Agent", "OpenTune-App")

                    if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                        if (attempt == 2) throw IOException("Error HTTP: ${connection.responseCode}")
                        delay(1000)
                        return@repeat
                    }

                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val jsonArray = JSONArray(response)
                    val commits = mutableListOf<Commit>()

                    for (i in 0 until jsonArray.length().coerceAtMost(20)) {
                        val commitJson = jsonArray.getJSONObject(i)
                        val commitData = commitJson.getJSONObject("commit")
                        val author = commitData.getJSONObject("author")

                        commits.add(
                            Commit(
                                sha = commitJson.optString("sha", ""),
                                message = commitData.optString("message", ""),
                                author = author.optString("name", "Unknown"),
                                date = author.optString("date", ""),
                                htmlUrl = commitJson.optString("html_url", "")
                            )
                        )
                    }

                    return@withContext commits
                } catch (e: Exception) {
                    if (attempt == 2) throw e
                    delay(1000)
                }
            }

            throw IOException("No se pudo obtener los commits después de los reintentos")
        }

    private fun getCurrentTimestamp(): String {
        val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
        return formatter.format(Date())
    }
}