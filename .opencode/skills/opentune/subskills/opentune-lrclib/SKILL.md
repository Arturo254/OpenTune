---
name: opentune-lrclib
description: Skill for the :lrclib module - LRCLib.net synchronized lyrics API client.
---

# LRCLib Module

Package: `com.arturo254.opentune.lrclib` | Build: `lrclib/build.gradle.kts`

## File Patterns

Files matching these patterns activate this skill:
- `lrclib/**/*.kt`

## Key Files

| File | Purpose |
|------|---------|
| `LrcLib.kt` | API client for lrclib.net — search/fetch synchronized LRC lyrics with best-matching algorithm |
| `models/Track.kt` | Track response model — includes `bestMatch()` logic based on duration delta |

## Key Dependencies

| Dependency | Note |
|------------|------|
| Ktor CIO | **CIO engine** (not OkHttp like most other modules) |
| Kotlinx Serialization | JSON parsing |

## Key Entry Points

| Class | File | Role |
|-------|------|------|
| `LrcLib` | `LrcLib.kt` | Main client: `search()`, `getLyrics()` with duration-based best matching |
| `Track` | `models/Track.kt` | Response model with `bestMatch(trackDurationMs)` method |
| `LrcLibLyricsProvider` | `app/.../lyrics/LrcLibLyricsProvider.kt` | Wraps LrcLib in the app's LyricsProvider interface |

## Architecture

Lightweight REST client for lrclib.net. Key feature: duration-based best-matching algorithm that finds the closest lyrics when exact match isn't available. The `Track.bestMatch()` method compares request duration with available tracks and returns the closest.

## Testing

```bash
./gradlew :lrclib:test
```

## Pitfalls

- Uses **CIO engine** (not OkHttp) — don't share OkHttp-specific config (connection pooling, interceptors) with this module
- Best-matching is duration-based — songs with similar durations may return wrong lyrics
- lrclib.net rate limits may apply — consider retry/caching if adding features
- No Android dependencies — this is pure Kotlin/JVM
