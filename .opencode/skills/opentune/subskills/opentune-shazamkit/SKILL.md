---
name: opentune-shazamkit
description: Skill for the :shazamkit module - Shazam music recognition API client for identifying songs.
---

# ShazamKit Module

Package: `com.arturo254.opentune.shazamkit`
Build: `shazamkit/build.gradle.kts`

## Key Files

| File | Purpose |
|------|---------|
| `Shazam.kt` | Shazam music recognition client - rate limiting, queue management, caching, retry logic |
| `ShazamSignatureGenerator.kt` | Generates cryptographic signatures for Shazam API requests |
| `models/ShazamModels.kt` | Request/response models: ShazamRequestJson, ShazamResponseJson, RecognitionResult |

## Key Dependencies

- Ktor client (CIO engine)
- Kotlinx Serialization

## Architecture

- Full Shazam API client with signature-based auth
- Includes rate limiting, request queue, and retry logic
- Caches recognition results
- Used by `app/ui/screens/musicrecognition/` for the music recognition feature
- Uses CIO engine (shared with lrclib, simpmusic modules)
