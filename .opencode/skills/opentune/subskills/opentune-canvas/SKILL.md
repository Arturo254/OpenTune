---
name: opentune-canvas
description: Skill for the :canvas module - animated album artwork (canvas/visualizer) API client.
---

# Canvas Module

Package: `com.arturo254.opentune.canvas` | Build: `canvas/build.gradle.kts`

## File Patterns

Files matching these patterns activate this skill:
- `canvas/**/*.kt`

## Key Files

| File | Purpose |
|------|---------|
| `OpenTuneCanvas.kt` | API client for `artwork-ArchiveTune.koiiverse.cloud` — fetches dynamic/animated album artwork |
| `models/CanvasArtworkModel.kt` | Response model: artwork URL, type, metadata |

## Key Dependencies

- Ktor client (OkHttp engine)
- Kotlinx Serialization

## Key Entry Points

| Class | File | Role |
|-------|------|------|
| `OpenTuneCanvas` | `OpenTuneCanvas.kt` | Main client: `getCanvas(id, ...)` |
| `CanvasArtworkPlayer` | `app/.../ui/player/` | Player UI component that renders the artwork |

## Architecture

Simple REST client that fetches animated album artwork URLs. The app renders these as dynamic backgrounds in the player UI (album art with animated effects). Uses OkHttp engine and Kotlinx Serialization.

## Testing

```bash
./gradlew :canvas:test
```

## Pitfalls

- Artwork service is third-party — may be unavailable
- Animated artwork can be large files — consider caching behavior
- Canvas resolution may vary — check response for multiple quality options
- Module is pure Kotlin/JVM
