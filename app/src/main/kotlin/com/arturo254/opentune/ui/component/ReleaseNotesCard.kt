/*
 * OpenTune Project Original (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package com.arturo254.opentune.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.arturo254.opentune.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

@Composable
fun ReleaseNotesCard() {
    var markdownContent by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        markdownContent = fetchReleaseNotesMarkdown()
    }

    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.release_notes),
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Usar el componente MarkdownText que acabamos de crear
            MarkdownText(
                markdown = markdownContent,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                linkColor = MaterialTheme.colorScheme.primary,
                isTextSelectable = true
            )
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
}

suspend fun fetchReleaseNotesMarkdown(): String {
    return withContext(Dispatchers.IO) {
        try {
            val document =
                Jsoup.connect("https://github.com/Arturo254/OpenTune/releases/latest").get()
            // Obtener el contenido HTML del release
            val changelogElement = document.selectFirst(".markdown-body")

            if (changelogElement != null) {
                // Jsoup no convierte HTML a Markdown directamente,
                // pero podemos obtener el texto con formato básico
                val html = changelogElement.html()

                // Convertir HTML a formato Markdown simple
                htmlToMarkdown(html)
            } else {
                "No release notes available"
            }
        } catch (e: Exception) {
            "Error loading release notes:\n\n${e.message}"
        }
    }
}

// Función auxiliar para convertir HTML básico a Markdown
private fun htmlToMarkdown(html: String): String {
    var markdown = html

    // Headers
    markdown = markdown.replace(Regex("<h1>(.*?)</h1>"), "# $1")
    markdown = markdown.replace(Regex("<h2>(.*?)</h2>"), "## $1")
    markdown = markdown.replace(Regex("<h3>(.*?)</h3>"), "### $1")

    // Listas
    markdown = markdown.replace(Regex("<li>(.*?)</li>"), "- $1")
    markdown = markdown.replace(Regex("<ul>(.*?)</ul>"), "$1")
    markdown = markdown.replace(Regex("<ol>(.*?)</ol>"), "$1")

    // Enlaces
    markdown = markdown.replace(Regex("<a href=\"(.*?)\">(.*?)</a>"), "[$2]($1)")

    // Negritas y cursivas
    markdown = markdown.replace(Regex("<strong>(.*?)</strong>"), "**$1**")
    markdown = markdown.replace(Regex("<b>(.*?)</b>"), "**$1**")
    markdown = markdown.replace(Regex("<em>(.*?)</em>"), "*$1*")
    markdown = markdown.replace(Regex("<i>(.*?)</i>"), "*$1*")

    // Código
    markdown = markdown.replace(Regex("<code>(.*?)</code>"), "`$1`")
    markdown = markdown.replace(Regex("<pre><code>(.*?)</code></pre>"), "```\n$1\n```")

    // Párrafos y saltos de línea
    markdown = markdown.replace(Regex("<p>(.*?)</p>"), "$1\n\n")
    markdown = markdown.replace(Regex("<br\\s*/?>"), "\n")

    // Eliminar etiquetas HTML restantes
    markdown = markdown.replace(Regex("<[^>]*>"), "")

    // Limpiar espacios extra
    markdown = markdown.replace(Regex("\\n{3,}"), "\n\n")

    return markdown.trim()
}