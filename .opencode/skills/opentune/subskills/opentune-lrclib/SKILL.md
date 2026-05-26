---
name: opentune-lrclib
description: Skill for the :lrclib module - LRCLib.net synchronized lyrics API client.
---

# LRCLib Module

Package: `com.arturo254.opentune.lrclib`
Build: `lrclib/build.gradle.kts`

## Key Files

| File | Purpose |
|------|---------|
| `LrcLib.kt` | API client for lrclib.net - searches and fetches synchronized LRC lyrics with best-matching algorithm |
| `models/Track.kt` | Track response model with best-matching logic |

## Key Dependencies

- Ktor client (CIO engine - different from most other modules)
- Kotlinx Serialization

## Architecture

- Lightweight client accessing lrclib.net REST API
- Includes a best-matching algorithm to find closest lyrics by song duration
- Used by `app/lyrics/LrcLibLyricsProvider.kt`
- Uses CIO engine instead of OkHttp (unlike innertube, kugou, lastfm)
