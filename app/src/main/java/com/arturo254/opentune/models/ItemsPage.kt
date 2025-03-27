package com.arturo254.opentune.models

import com.arturo254.innertube.models.YTItem

data class ItemsPage(
    val items: List<YTItem>,
    val continuation: String?,
)
