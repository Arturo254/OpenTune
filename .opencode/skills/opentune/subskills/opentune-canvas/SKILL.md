---
name: opentune-canvas
description: Skill for the :canvas module - animated album artwork (canvas) API client.
---

# Canvas Module

Package: `com.arturo254.opentune.canvas`
Build: `canvas/build.gradle.kts`

## Key Files

| File | Purpose |
|------|---------|
| `OpenTuneCanvas.kt` | API client for artwork-ArchiveTune.koiiverse.cloud - fetches dynamic/animated album artwork |
| `models/CanvasArtworkModel.kt` | Data models for canvas artwork responses |

## Key Dependencies

- Ktor client (OkHttp engine)
- Kotlinx Serialization

## Architecture

- Simple REST client fetching animated album artwork (canvas backgrounds)
- Used in player UI for dynamic artwork backgrounds
- Kotlinx Serialization for JSON parsing
