package com.arturo254.opentune.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.arturo254.opentune.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReleaseNotesCard() {
    var releaseNotes by remember { mutableStateOf<List<ReleaseNoteItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isExpanded by remember { mutableStateOf(false) }
    var hasError by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isLoading = true
        hasError = false
        try {
            releaseNotes = fetchReleaseNotes()
        } catch (e: Exception) {
            hasError = true
            releaseNotes = listOf(
                ReleaseNoteItem(
                    type = ReleaseNoteType.ERROR,
                    text = "No se pudieron cargar las notas de la versión: ${e.message}"
                )
            )
        } finally {
            isLoading = false
        }
    }

    // Animación para el color del contenedor
    val containerColor by animateColorAsState(
        targetValue = if (isExpanded)
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
        else
            MaterialTheme.colorScheme.surfaceContainerLow,
        animationSpec = tween(durationMillis = 300),
        label = "containerColorAnimation"
    )

    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isExpanded) 8.dp else 4.dp,
            pressedElevation = 12.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColorFor(containerColor)
        ),
        shape = RoundedCornerShape(20.dp),
        onClick = { isExpanded = !isExpanded }
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Header con icono expresivo
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary,
                        tonalElevation = 2.dp
                    ) {
                        Icon(
                            imageVector = Icons.Filled.NewReleases,
                            contentDescription = null,
                            modifier = Modifier
                                .padding(10.dp)
                                .size(24.dp),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }

                    Column {
                        Text(
                            text = stringResource(R.string.release_notes),
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Última versión de OpenTune",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = if (isExpanded)
                            "Contraer notas de versión"
                        else
                            "Expandir notas de versión",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                LoadingIndicator()
            } else if (hasError && releaseNotes.isNotEmpty()) {
                ErrorContent(releaseNotes.first())
            } else if (releaseNotes.isNotEmpty()) {
                if (isExpanded) {
                    ExpandedContent(releaseNotes)
                } else {
                    CollapsedPreview(releaseNotes)
                }
            } else {
                EmptyState()
            }

            // Footer con acciones
            if (isExpanded && !isLoading && !hasError && releaseNotes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    tonalElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        FilledTonalButton(
                            onClick = { /* Acción para ver en GitHub */ },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            elevation = ButtonDefaults.filledTonalButtonElevation(
                                defaultElevation = 2.dp
                            )
                        ) {
                            Text("Ver en GitHub")
                        }
                    }
                }
            }
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
}

@Composable
private fun LoadingIndicator() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Puntos animados
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(3) { index ->
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                MaterialTheme.colorScheme.primary.copy(
                                    alpha = 0.3f + (index * 0.2f).coerceAtMost(0.7f)
                                )
                            )
                    )
                }
            }
            Text(
                text = "Cargando notas de versión...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ErrorContent(errorItem: ReleaseNoteItem) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = errorItem.text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
private fun CollapsedPreview(notes: List<ReleaseNoteItem>) {
    val previewNotes = notes.take(3)

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        previewNotes.forEach { note ->
            ReleaseNoteItemRow(note = note, isPreview = true)
        }

        if (notes.size > 3) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 1.dp
            ) {
                Text(
                    text = "+${notes.size - 3} cambios más",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun ExpandedContent(notes: List<ReleaseNoteItem>) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Encabezado de sección
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
        ) {
            Text(
                text = "Todos los cambios",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
        }

        // Lista completa
        notes.forEachIndexed { index, note ->
            ReleaseNoteItemRow(note = note, isPreview = false)
            if (index < notes.size - 1) {
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun ReleaseNoteItemRow(note: ReleaseNoteItem, isPreview: Boolean) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // Indicador de tipo
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = note.type.containerColor,
            tonalElevation = 1.dp,
            modifier = Modifier.size(24.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = note.type.symbol,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = note.type.contentColor,
                    modifier = Modifier.padding(2.dp)
                )
            }
        }

        // Texto con formato
        Text(
            text = buildAnnotatedString {
                if (!isPreview) {
                    withStyle(
                        style = SpanStyle(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    ) {
                        append("${note.type.displayName}: ")
                    }
                }
                withStyle(
                    style = SpanStyle(
                        color = if (isPreview)
                            MaterialTheme.colorScheme.onSurface
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                        fontWeight = if (isPreview) FontWeight.Normal else FontWeight.Normal
                    )
                ) {
                    append(note.text)
                }
            },
            style = if (isPreview)
                MaterialTheme.typography.bodyMedium
            else
                MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun EmptyState() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.NewReleases,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = "No hay notas de versión disponibles",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

data class ReleaseNoteItem(
    val type: ReleaseNoteType,
    val text: String
)

enum class ReleaseNoteType(
    val symbol: String,
    val displayName: String,
    val containerColor: Color,
    val contentColor: Color
) {
    FEATURE(
        symbol = "✨",
        displayName = "Nueva función",
        containerColor = Color(0xFFE8F5E9),
        contentColor = Color(0xFF2E7D32)
    ),
    IMPROVEMENT(
        symbol = "⚡",
        displayName = "Mejora",
        containerColor = Color(0xFFE3F2FD),
        contentColor = Color(0xFF1565C0)
    ),
    FIX(
        symbol = "🐛",
        displayName = "Corrección",
        containerColor = Color(0xFFFFF8E1),
        contentColor = Color(0xFFF57C00)
    ),
    SECURITY(
        symbol = "🔒",
        displayName = "Seguridad",
        containerColor = Color(0xFFFCE4EC),
        contentColor = Color(0xFFC2185B)
    ),
    DEPRECATION(
        symbol = "⚠️",
        displayName = "Obsoleto",
        containerColor = Color(0xFFEFEBE9),
        contentColor = Color(0xFF5D4037)
    ),
    ERROR(
        symbol = "❌",
        displayName = "Error",
        containerColor = Color(0xFFFFEBEE),
        contentColor = Color(0xFFC62828)
    )
}

suspend fun fetchReleaseNotes(): List<ReleaseNoteItem> {
    return withContext(Dispatchers.IO) {
        try {
            val document = Jsoup.connect("https://github.com/Arturo254/OpenTune/releases/latest").get()
            val changelogElement = document.selectFirst(".markdown-body")
            val htmlContent = changelogElement?.html() ?: "No release notes found"

            // Extraer texto y limpiar HTML
            val textContent = htmlContent
                .replace(Regex("<br.*?>|</p>"), "\n")
                .replace(Regex("<.*?>"), "")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")

            // Procesar líneas y categorizar
            textContent.split("\n")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .map { line ->
                    val (type, text) = categorizeReleaseNote(line)
                    ReleaseNoteItem(type, text)
                }
        } catch (e: Exception) {
            throw RuntimeException("Error fetching release notes: ${e.message}")
        }
    }
}

private fun categorizeReleaseNote(line: String): Pair<ReleaseNoteType, String> {
    val cleanedLine = line.replace(Regex("^[•\\-\\*\\+\\s]+"), "").trim()

    return when {
        Regex("(?i)(add(ed)?|new|feature|feat|implement(ed)?)").containsMatchIn(cleanedLine) ->
            ReleaseNoteType.FEATURE to cleanedLine
        Regex("(?i)(improve(d|ment)?|optimiz(e|ed|ation)|enhance(d|ment)?|speed|fast(er)?)").containsMatchIn(cleanedLine) ->
            ReleaseNoteType.IMPROVEMENT to cleanedLine
        Regex("(?i)(fix(ed)?|bug|error|issue|correct(ed)?|resolve(d)?)").containsMatchIn(cleanedLine) ->
            ReleaseNoteType.FIX to cleanedLine
        Regex("(?i)(security|vulnerability|secure|protect)").containsMatchIn(cleanedLine) ->
            ReleaseNoteType.SECURITY to cleanedLine
        Regex("(?i)(deprecat(e|ed|ion)|remove(d)?|obsolete|discontinue(d)?)").containsMatchIn(cleanedLine) ->
            ReleaseNoteType.DEPRECATION to cleanedLine
        else -> ReleaseNoteType.IMPROVEMENT to cleanedLine
    }
}