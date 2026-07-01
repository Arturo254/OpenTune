/*
 * OpenTune Project Original (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.arturo254.opentune.ui.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import dev.jeziellago.compose.markdowntext.MarkdownText as ComposeMarkdownText

@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    color: Color = MaterialTheme.colorScheme.onSurface,
    linkColor: Color = MaterialTheme.colorScheme.primary,
    isTextSelectable: Boolean = false,
) {
    ComposeMarkdownText(
        markdown = markdown,
        modifier = modifier,
        style = style.copy(
            color = color,
            fontSize = style.fontSize.takeIf { it.value > 0f } ?: 16.sp
        ),
        linkColor = linkColor,
        isTextSelectable = isTextSelectable,
    )
}