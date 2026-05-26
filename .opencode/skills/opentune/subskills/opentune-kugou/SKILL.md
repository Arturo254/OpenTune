---
name: opentune-kugou
description: Skill for the :kugou module - KuGou music search and lyrics download API client.
---

# KuGou Module

Package: `com.arturo254.opentune.kugou`
Build: `kugou/build.gradle.kts`

## Key Files

| File | Purpose |
|------|---------|
| `KuGou.kt` | API client for KuGou music service - song search and lyrics download |
| `models/SearchSongResponse.kt` | Song search response model |
| `models/SearchLyricsResponse.kt` | Lyrics search response model |
| `models/DownloadLyricsResponse.kt` | Lyrics download response model |
| `models/Keyword.kt` | Keyword search model |

## Key Dependencies

- Ktor client (OkHttp engine)
- Kotlinx Serialization

## Architecture

- Simple REST client with search and download endpoints
- Used by `app/lyrics/KuGouLyricsProvider.kt` for lyrics fetching
- Kotlinx Serialization for JSON parsing
