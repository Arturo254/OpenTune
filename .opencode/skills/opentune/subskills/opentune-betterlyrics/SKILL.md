---
name: opentune-betterlyrics
description: Skill for the :betterlyrics module - BetterLyrics API client and TTML subtitle parser.
---

# BetterLyrics Module

Package: `com.arturo254.opentune.betterlyrics`
Build: `betterlyrics/build.gradle.kts`

## Key Files

| File | Purpose |
|------|---------|
| `BetterLyrics.kt` | API client for lyrics-api.boidu.dev - fetches synchronized lyrics |
| `TTMLParser.kt` | Parses TTML (Timed Text Markup Language) subtitle/lyrics XML format |
| `models/Track.kt` | Track data model for lyrics response |
| `TestTTML.kt` | Test file for TTML parsing |

## Key Dependencies

- Ktor client (OkHttp engine)
- Kotlinx Serialization

## Architecture

- Fetches lyrics from a third-party aggregator (lyrics-api.boidu.dev)
- Parses TTML XML format used for timed lyrics/subtitles
- Used by `app/lyrics/BetterLyricsProvider.kt`
- Test file available for TTML parser validation
