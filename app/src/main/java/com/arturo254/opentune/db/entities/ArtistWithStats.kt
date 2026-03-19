package com.arturo254.opentune.db.entities

import androidx.compose.runtime.Immutable
import java.time.LocalDateTime

@Immutable
data class ArtistWithStats(
    val id: String,
    val name: String,
    val thumbnailUrl: String?,
    val channelId: String?,
    val songCount: Int,
    val lastUpdateTime: LocalDateTime,
    val bookmarkedAt: LocalDateTime?,
    val timeListened: Long?,
)
