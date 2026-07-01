/*
 * OpenTune Project Original (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.arturo254.opentune.ui.menu

import android.R.attr.progress
import android.annotation.SuppressLint
import android.app.SearchManager
import android.content.Intent
import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import me.bush.translator.Translator
import me.bush.translator.Language
import com.arturo254.opentune.utils.TranslatorLanguages
import com.arturo254.opentune.utils.TranslatorLang
import androidx.compose.runtime.produceState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.components.FilledButton
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.graphics.PaintingStyle.Companion.Stroke
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.graphics.shapes.RoundedPolygon
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.arturo254.opentune.LocalDatabase
import com.arturo254.opentune.R
import com.arturo254.opentune.db.entities.LyricsEntity
import com.arturo254.opentune.lyrics.LyricsUtils.isTtml
import com.arturo254.opentune.lyrics.LyricsUtils.parseLyrics
import com.arturo254.opentune.lyrics.LyricsUtils.parseTtml
import com.arturo254.opentune.models.MediaMetadata
import com.arturo254.opentune.ui.component.DefaultDialog
import com.arturo254.opentune.ui.component.ListDialog
import com.arturo254.opentune.ui.component.TextFieldDialog
import com.arturo254.opentune.viewmodels.LyricsMenuViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.UUID

@SuppressLint("LocalContextGetResourceValueCall")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LyricsMenu(
    lyricsProvider: () -> LyricsEntity?,
    mediaMetadataProvider: () -> MediaMetadata,
    onDismiss: () -> Unit,
    viewModel: LyricsMenuViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT

    var showEditDialog by rememberSaveable { mutableStateOf(false) }
    var showTranslateDialog by rememberSaveable { mutableStateOf(false) }
    var showSearchDialog by rememberSaveable { mutableStateOf(false) }
    var showSearchResultDialog by rememberSaveable { mutableStateOf(false) }
    var toolbarExpanded by rememberSaveable { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()
    val isNetworkAvailable by viewModel.isNetworkAvailable.collectAsState()

    if (showEditDialog) {
        TextFieldDialog(
            onDismiss = { showEditDialog = false },
            icon = { Icon(painter = painterResource(R.drawable.edit), contentDescription = null) },
            title = { Text(text = mediaMetadataProvider().title) },
            initialTextFieldValue = TextFieldValue(lyricsProvider()?.lyrics.orEmpty()),
            singleLine = false,
            onDone = {
                viewModel.updateLyrics(mediaMetadataProvider(), it)
            },
        )
    }

    val searchMediaMetadata = remember(showSearchDialog) { mediaMetadataProvider() }
    val (titleField, onTitleFieldChange) = rememberSaveable(
        showSearchDialog,
        stateSaver = TextFieldValue.Saver
    ) {
        mutableStateOf(TextFieldValue(text = mediaMetadataProvider().title))
    }
    val (artistField, onArtistFieldChange) = rememberSaveable(
        showSearchDialog,
        stateSaver = TextFieldValue.Saver
    ) {
        mutableStateOf(TextFieldValue(text = mediaMetadataProvider().artists.joinToString { it.name }))
    }

    if (showSearchDialog) {
        DefaultDialog(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            onDismiss = { showSearchDialog = false },
            icon = {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = CircleShape
                ) {
                    Icon(
                        painter = painterResource(R.drawable.search),
                        contentDescription = null,
                        modifier = Modifier
                            .padding(12.dp)
                            .size(24.dp)
                    )
                }
            },
            title = {
                Text(
                    text = stringResource(R.string.search_lyrics),
                    style = MaterialTheme.typography.headlineSmallEmphasized
                )
            },
            buttons = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(
                        ButtonGroupDefaults.ConnectedSpaceBetween
                    )
                ) {
                    ToggleButton(
                        checked = false,
                        onCheckedChange = {
                            showSearchDialog = false
                        },
                        shapes = ButtonGroupDefaults.connectedLeadingButtonShapes()
                    ) {
                        Text(stringResource(android.R.string.cancel))
                    }

                    ToggleButton(
                        checked = false,
                        onCheckedChange = {
                            showSearchDialog = false
                            onDismiss()

                            try {
                                context.startActivity(
                                    Intent(Intent.ACTION_WEB_SEARCH).apply {
                                        putExtra(
                                            SearchManager.QUERY,
                                            "${artistField.text} ${titleField.text} lyrics"
                                        )
                                    }
                                )
                            } catch (_: Exception) {
                            }
                        },
                        shapes = ButtonGroupDefaults.connectedMiddleButtonShapes()
                    ) {
                        Text(stringResource(R.string.search_online))
                    }

                    ToggleButton(
                        checked = false,
                        onCheckedChange = {
                            viewModel.search(
                                searchMediaMetadata.id,
                                titleField.text,
                                artistField.text,
                                searchMediaMetadata.duration
                            )

                            showSearchResultDialog = true

                            if (!isNetworkAvailable) {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.error_no_internet),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        shapes = ButtonGroupDefaults.connectedTrailingButtonShapes()
                    ) {
                        Text(stringResource(android.R.string.ok))
                    }
                }
            },
        ) {
            OutlinedTextField(
                value = titleField,
                onValueChange = onTitleFieldChange,
                singleLine = true,
                label = { Text(stringResource(R.string.song_title)) },
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = artistField,
                onValueChange = onArtistFieldChange,
                singleLine = true,
                label = { Text(stringResource(R.string.song_artists)) },
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (showSearchResultDialog) {
        val results by viewModel.results.collectAsState()
        val isLoading by viewModel.isLoading.collectAsState()
        var expandedItemIndex by rememberSaveable { mutableIntStateOf(-1) }

        ListDialog(
            onDismiss = { showSearchResultDialog = false },
        ) {
            itemsIndexed(results) { index, result ->

                ElevatedCard(
                    onClick = {
                        onDismiss()
                        viewModel.cancelSearch()
                        viewModel.updateLyrics(
                            searchMediaMetadata,
                            result.lyrics
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .animateContentSize(),
                    shape = MaterialTheme.shapes.extraLarge
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {

                        val displayLyrics = remember(result.lyrics) {
                            val raw = result.lyrics.trim()

                            when {
                                isTtml(raw) -> {
                                    parseTtml(raw)
                                        .joinToString("\n") { it.text }
                                        .trim()
                                }

                                raw.startsWith("[") -> {
                                    parseLyrics(raw)
                                        .joinToString("\n") { it.text }
                                        .trim()
                                }

                                else -> raw
                            }
                        }

                        Text(
                            text = displayLyrics,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = if (index == expandedItemIndex) {
                                Int.MAX_VALUE
                            } else {
                                3
                            },
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            SuggestionChip(
                                onClick = {},
                                label = {
                                    Text(result.providerName)
                                }
                            )

                            if (
                                result.lyrics.startsWith("[") ||
                                isTtml(result.lyrics)
                            ) {
                                Spacer(Modifier.width(8.dp))

                                AssistChip(
                                    onClick = {},
                                    leadingIcon = {
                                        Icon(
                                            painter = painterResource(R.drawable.sync),
                                            contentDescription = null
                                        )
                                    },
                                    label = {
                                        Text("Synced")
                                    }
                                )
                            }

                            Spacer(Modifier.weight(1f))

                            ToggleButton(
                                checked = expandedItemIndex == index,
                                onCheckedChange = {
                                    expandedItemIndex =
                                        if (expandedItemIndex == index) {
                                            -1
                                        } else {
                                            index
                                        }
                                }
                            ) {
                                Icon(
                                    painter = painterResource(
                                        if (expandedItemIndex == index)
                                            R.drawable.expand_less
                                        else
                                            R.drawable.expand_more
                                    ),
                                    contentDescription = null
                                )
                            }
                        }
                    }
                }
            }



            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularWavyProgressIndicator()
                    }
                }
            }

            if (!isLoading && results.isEmpty()) {
                item {
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        shape = MaterialTheme.shapes.extraLarge
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {

                            Icon(
                                painter = painterResource(R.drawable.search),
                                contentDescription = null,
                                modifier = Modifier.size(32.dp)
                            )

                            Spacer(Modifier.height(12.dp))

                            Text(
                                text = stringResource(R.string.lyrics_not_found),
                                style = MaterialTheme.typography.titleMediumEmphasized,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }

    if (showTranslateDialog) {
        val initialText = lyricsProvider()?.lyrics.orEmpty()
        val (textFieldValue, setTextFieldValue) =
            rememberSaveable(stateSaver = TextFieldValue.Saver) {
                mutableStateOf(TextFieldValue(text = initialText))
            }

        val languages by produceState(initialValue = emptyList<TranslatorLang>()) {
            withContext(Dispatchers.IO) {
                value = TranslatorLanguages.load(context)
            }
        }
        var expanded by remember { mutableStateOf(false) }
        var selectedLanguageCode by rememberSaveable { mutableStateOf("ENGLISH") }
        var isTranslating by remember { mutableStateOf(false) }
        val selectedLanguageName =
            languages.firstOrNull { it.code == selectedLanguageCode }?.name ?: selectedLanguageCode

        DefaultDialog(
            onDismiss = { showTranslateDialog = false },
            icon = {
                Icon(painter = painterResource(R.drawable.translate), contentDescription = null)
            },
            title = { Text(stringResource(R.string.translate)) },
            buttons = {
                TextButton(onClick = { showTranslateDialog = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
                Spacer(Modifier.width(8.dp))
                if (isTranslating) {
                    val density = LocalDensity.current

                    CircularWavyProgressIndicator(
                        modifier = Modifier
                            .size(20.dp)
                            .align(Alignment.CenterVertically),
                        stroke = Stroke(
                            width = with(density) { 2.dp.toPx() }
                        )
                    )
                } else {
                    TextButton(onClick = {
                        isTranslating = true
                        val inputText = textFieldValue.text
                        val languageCode = selectedLanguageCode
                        val languageName = selectedLanguageName
                        coroutineScope.launch {
                            try {
                                val lang = try {
                                    Language(languageCode)
                                } catch (e: Exception) {
                                    try {
                                        Language(languageName)
                                    } catch (_: Exception) {
                                        null
                                    }
                                }

                                if (lang == null) {
                                    Toast.makeText(
                                        context,
                                        "Unsupported language: $languageName",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return@launch
                                }

                                val translatedLyrics = withContext(Dispatchers.IO) {
                                    val translator = Translator()

                                    val lines = inputText.split("\n")
                                    val tsRegex =
                                        Regex("^((?:\\[[0-9]{2}:[0-9]{2}(?:\\.[0-9]+)?\\])+)")
                                    val contents = mutableListOf<String?>()
                                    val stampsFor = mutableListOf<String?>()

                                    for (line in lines) {
                                        val trimmed = line.trimEnd()
                                        val m = tsRegex.find(trimmed)
                                        if (m != null) {
                                            val stamps = m.groupValues[1]
                                            val content =
                                                trimmed.substring(m.range.last + 1).trimStart()
                                            stampsFor.add(stamps)
                                            contents.add(content.ifBlank { null })
                                        } else {
                                            stampsFor.add(null)
                                            contents.add(trimmed.ifBlank { null })
                                        }
                                    }

                                    val translatableIndices =
                                        contents.mapIndexedNotNull { idx, c -> if (c != null) idx else null }
                                    val translatedMap = mutableMapOf<Int, String>()

                                    if (translatableIndices.isNotEmpty()) {
                                        var sep = "<<<SEP-${UUID.randomUUID()}>>>"
                                        while (contents.any { it?.contains(sep) == true }) {
                                            sep = "<<<SEP-${UUID.randomUUID()}>>>"
                                        }

                                        val maxCharsPerRequest = 4000
                                        val maxItemsPerBatch = 50

                                        var cursor = 0
                                        while (cursor < translatableIndices.size) {
                                            var currentChars = 0
                                            val batchIndices = mutableListOf<Int>()
                                            while (cursor < translatableIndices.size && batchIndices.size < maxItemsPerBatch) {
                                                val idx = translatableIndices[cursor]
                                                val pieceLen = contents[idx]!!.length
                                                if (batchIndices.isEmpty() || currentChars + pieceLen + sep.length <= maxCharsPerRequest) {
                                                    batchIndices.add(idx)
                                                    currentChars += pieceLen + sep.length
                                                    cursor++
                                                } else break
                                            }

                                            val batchTexts = batchIndices.map { contents[it]!! }
                                            val joined = batchTexts.joinToString(separator = sep)
                                            val translatedJoined =
                                                translator.translateBlocking(
                                                    joined,
                                                    lang
                                                ).translatedText

                                            val parts = translatedJoined.split(sep)
                                            if (parts.size == batchTexts.size) {
                                                for (i in batchIndices.indices) {
                                                    translatedMap[batchIndices[i]] = parts[i]
                                                }
                                            } else {
                                                for (idx in batchIndices) {
                                                    val original = contents[idx]!!
                                                    val singleTranslated = runCatching {
                                                        translator.translateBlocking(
                                                            original,
                                                            lang
                                                        ).translatedText
                                                    }.getOrNull() ?: original
                                                    translatedMap[idx] = singleTranslated
                                                }
                                            }
                                        }
                                    }

                                    val out = mutableListOf<String>()
                                    for (i in contents.indices) {
                                        val stamp = stampsFor[i]
                                        val c = contents[i]
                                        if (c == null) {
                                            if (stamp != null) out.add(stamp) else out.add("")
                                        } else {
                                            val translatedText = translatedMap[i] ?: c
                                            if (stamp != null) out.add("$stamp $translatedText") else out.add(
                                                translatedText
                                            )
                                        }
                                    }

                                    out.joinToString("\n")
                                }
                                viewModel.updateLyrics(mediaMetadataProvider(), translatedLyrics)
                                showTranslateDialog = false
                            } catch (e: Exception) {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.translation_failed) + ": " + (e.localizedMessage
                                        ?: e.toString()),
                                    Toast.LENGTH_SHORT
                                ).show()
                            } finally {
                                isTranslating = false
                            }
                        }
                    }) {
                        Text(stringResource(R.string.translate))
                    }
                }
            }
        ) {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = textFieldValue,
                    onValueChange = setTextFieldValue,
                    singleLine = false,
                    label = { Text(stringResource(R.string.lyrics)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 80.dp, max = 220.dp)
                )

                Spacer(Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.language_label),
                        modifier = Modifier.width(96.dp)
                    )

                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = it },
                        modifier = Modifier.weight(1f),
                    ) {
                        OutlinedTextField(
                            value = selectedLanguageName,
                            onValueChange = {},
                            readOnly = true,
                            singleLine = true,
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                            },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )

                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            languages.forEach { lang ->
                                DropdownMenuItem(
                                    text = { Text(lang.name) },
                                    onClick = {
                                        selectedLanguageCode = lang.code
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    onDismiss()
                }
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f))
        )

        LazyColumn(
            userScrollEnabled = !isPortrait,
            contentPadding = PaddingValues(
                start = 0.dp,
                top = 0.dp,
                end = 0.dp,
                bottom = 80.dp + WindowInsets.systemBars.asPaddingValues().calculateBottomPadding(),
            ),
        ) {
            item {
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        HorizontalFloatingToolbar(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = (-16).dp)
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding()
                ),
            expanded = toolbarExpanded,
            colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            shape = MaterialTheme.shapes.extraLarge,
            content = {
                IconButton(
                    onClick = { showEditDialog = true },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.edit),
                        contentDescription = stringResource(R.string.edit),
                        modifier = Modifier.size(24.dp)
                    )
                }

                IconButton(
                    onClick = {
                        viewModel.refetchLyrics(mediaMetadataProvider(), lyricsProvider())
                        onDismiss()
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.cached),
                        contentDescription = stringResource(R.string.refetch),
                        modifier = Modifier.size(24.dp)
                    )
                }

                IconButton(
                    onClick = { showTranslateDialog = true },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.translate),
                        contentDescription = stringResource(R.string.translate),
                        modifier = Modifier.size(24.dp)
                    )
                }

                IconButton(
                    onClick = { showSearchDialog = true },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.search),
                        contentDescription = stringResource(R.string.search),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        )
    }
}
