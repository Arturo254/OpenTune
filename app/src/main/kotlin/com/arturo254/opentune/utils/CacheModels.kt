package com.arturo254.opentune.utils

import androidx.compose.runtime.Immutable
import kotlin.math.log10
import kotlin.math.pow

@Immutable
data class CacheItem(
    val key: String,
    val title: String,
    val subtitle: String?,
    val thumbnailUrl: String?,
    val sizeBytes: Long,
    val type: CacheType
)

enum class CacheType {
    SONG, IMAGE, OTHER
}

data class CacheSectionState(
    val type: CacheType,
    val items: List<CacheItem>,
    val totalSize: Long,
    val isExpanded: Boolean = true
)

fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (log10(bytes.toDouble()) / log10(1024.0)).toInt()
    val value = bytes / 1024.0.pow(digitGroups.toDouble())
    return "%.1f %s".format(value, units[digitGroups.coerceAtMost(units.lastIndex)])
}