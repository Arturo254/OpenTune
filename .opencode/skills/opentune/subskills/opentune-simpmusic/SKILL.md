---
name: opentune-simpmusic
description: Skill for the :simpmusic module - legacy SimpMusic lyrics API client.
---

# SimpMusic Module

Package: `com.arturo254.opentune.simpmusic` | Build: `simpmusic/build.gradle.kts`

## File Patterns

Files matching these patterns activate this skill:
- `simpmusic/**/*.kt`

## Key Files

| File | Purpose |
|------|---------|
| `SimpMusicLyrics.kt` | API client for `api-lyrics.simpmusic.org/v1/` — fetches synchronized LRC lyrics by title, artist, duration |
| `models/LyricsResponse.kt` | Response models: `SimpMusicApiResponse`, `LyricsData` |

## Key Dependencies

| Dependency | Note |
|------------|------|
| Ktor CIO | **CIO engine** (shared with lrclib, shazamkit) |
| Kotlinx Serialization | JSON parsing |

## Key Entry Points

| Class | File | Role |
|-------|------|------|
| `SimpMusicLyrics` | `SimpMusicLyrics.kt` | Main client: `getLyrics(title, artist, duration)` |
| `SimpMusicLyricsProvider` | `app/.../lyrics/SimpMusicLyricsProvider.kt` | Wraps module in app's LyricsProvider interface |

## Architecture

Lightweight REST client for SimpMusic's legacy lyrics API. Simple request-response: POST song title, artist name, and duration; receive LRC-format lyrics back.

## Testing

```bash
./gradlew :simpmusic:test
```

## Pitfalls

- Uses **CIO engine** — don't share OkHttp-specific config
- This is a **legacy API** — may be deprecated or removed; prefer LRCLib or BetterLyrics for new development
- The module is included in `settings.gradle.kts` as `include("simpmusic")` **without a leading colon** — unlike all other modules which use `include(":module")`
- Despite the include syntax, it's referenced as `project(":simpmusic")` in `app/build.gradle.kts`
- Module is pure Kotlin/JVM — no Android dependencies
