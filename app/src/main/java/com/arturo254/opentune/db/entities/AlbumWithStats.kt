package com.arturo254.opentune.db.entities

import androidx.compose.runtime.Immutable

@Immutable
data class AlbumWithStats(
    val id: String,
    val title: String,
    val thumbnailUrl: String?,
    val songCountListened: Int,
    val timeListened: Long?,
)
