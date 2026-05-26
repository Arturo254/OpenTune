---
name: opentune-simpmusic
description: Skill for the :simpmusic module - legacy SimpMusic lyrics API client.
---

# SimpMusic Module

Package: `com.arturo254.opentune.simpmusic`
Build: `simpmusic/build.gradle.kts`

## Key Files

| File | Purpose |
|------|---------|
| `SimpMusicLyrics.kt` | API client for api-lyrics.simpmusic.org/v1/ - fetches synchronized lyrics by title, artist, and duration |
| `models/LyricsResponse.kt` | Response models: SimpMusicApiResponse, LyricsData |

## Key Dependencies

- Ktor client (CIO engine)
- Kotlinx Serialization

## Architecture

- Lightweight REST client for SimpMusic's legacy lyrics API
- Used by `app/lyrics/SimpMusicLyricsProvider.kt`
- Uses CIO engine (shared with lrclib, shazamkit modules)
- Simple request-response model with song title, artist name, and duration params
