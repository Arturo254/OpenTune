---
name: opentune-betterlyrics
description: Skill for the :betterlyrics module - BetterLyrics API client and TTML subtitle/lyrics parser.
---

# BetterLyrics Module

Package: `com.arturo254.opentune.betterlyrics` | Build: `betterlyrics/build.gradle.kts`

## File Patterns

Files matching these patterns activate this skill:
- `betterlyrics/**/*.kt`

## Key Files

| File | Purpose |
|------|---------|
| `BetterLyrics.kt` | API client for `lyrics-api.boidu.dev` — fetches synchronized lyrics REST |
| `TTMLParser.kt` | Parses TTML (Timed Text Markup Language) XML — converts subtitle XML to timed lyrics |
| `models/Track.kt` | Track data model for lyrics response |
| `TestTTML.kt` | Unit test for TTML parsing |

## Key Dependencies

- Ktor client (OkHttp engine)
- Kotlinx Serialization

## Key Entry Points

| Class | File | Role |
|-------|------|------|
| `BetterLyrics` | `BetterLyrics.kt` | Main client: `getLyrics(title, artist, duration)` |
| `TTMLParser` | `TTMLParser.kt` | Parses TTML XML into list of `(timestamp, text)` pairs |
| `BetterLyricsProvider` | `app/.../lyrics/BetterLyricsProvider.kt` | Wraps module in app's LyricsProvider interface |

## Architecture

Fetches lyrics from a third-party aggregator (`lyrics-api.boidu.dev`). The API returns TTML (Timed Text Markup Language) XML format. The `TTMLParser` converts this XML into timed lyric segments. This is different from LRC format used by other providers.

## Testing

```bash
./gradlew :betterlyrics:test
```

Run the `TestTTML.kt` tests specifically to validate TTML parsing.

## Pitfalls

- TTML XML format can vary between providers — `TTMLParser` may need updates for new XML structures
- The external API (`lyrics-api.boidu.dev`) is third-party — may go down or change schema
- TTML timestamps are in `hh:mm:ss.fff` format — ensure parser handles edge cases
- Module is pure Kotlin/JVM — no Android dependencies
