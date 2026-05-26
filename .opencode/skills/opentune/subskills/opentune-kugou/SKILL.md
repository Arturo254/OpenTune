---
name: opentune-kugou
description: Skill for the :kugou module - KuGou music search and lyrics download API client.
---

# KuGou Module

Package: `com.arturo254.opentune.kugou` | Build: `kugou/build.gradle.kts`

## File Patterns

Files matching these patterns activate this skill:
- `kugou/**/*.kt`

## Key Files

| File | Purpose |
|------|---------|
| `KuGou.kt` | API client — search songs and download lyrics from KuGou |
| `models/SearchSongResponse.kt` | Song search response (`@Serializable`) |
| `models/SearchLyricsResponse.kt` | Lyrics search response |
| `models/DownloadLyricsResponse.kt` | Lyrics download response |
| `models/Keyword.kt` | Keyword search model |

## Key Dependencies

- Ktor client (OkHttp engine)
- Kotlinx Serialization

## Key Entry Points

| Class | File | Role |
|-------|------|------|
| `KuGou` | `KuGou.kt` | Main client: `searchSong()`, `searchLyrics()`, `downloadLyrics()` |
| `KuGouLyricsProvider` | `app/.../lyrics/KuGouLyricsProvider.kt` | Wraps KuGou in the app's LyricsProvider interface |

## Architecture

Simple REST client with 3 endpoints (search songs, search lyrics, download lyrics). The app's `KuGouLyricsProvider.kt` wraps calls to this module. Kotlinx Serialization for JSON parsing.

## Testing

```bash
./gradlew :kugou:test
```

## Pitfalls

- KuGou API is a Chinese music service — may have regional restrictions or latency
- Lyrics search requires both song title and artist for best results
- Uses OkHttp engine (consistent with most modules, differs from CIO modules)
