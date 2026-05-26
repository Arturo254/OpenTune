---
name: opentune-shazamkit
description: Skill for the :shazamkit module - Shazam music recognition API client.
---

# ShazamKit Module

Package: `com.arturo254.opentune.shazamkit` | Build: `shazamkit/build.gradle.kts`

## File Patterns

Files matching these patterns activate this skill:
- `shazamkit/**/*.kt`

## Key Files

| File | Purpose |
|------|---------|
| `Shazam.kt` | Main recognition client — rate limiting, request queue, caching, retry logic |
| `ShazamSignatureGenerator.kt` | Generates cryptographic signatures for Shazam API authentication |
| `models/ShazamModels.kt` | `ShazamRequestJson`, `ShazamResponseJson`, `RecognitionResult` |

## Key Dependencies

| Dependency | Note |
|------------|------|
| Ktor CIO | **CIO engine** (shared with lrclib, simpmusic) |
| Kotlinx Serialization | JSON parsing |

## Key Entry Points

| Class | File | Role |
|-------|------|------|
| `Shazam` | `Shazam.kt` | Main client: `recognize(audioData)` with rate limiting, queue, cache, retry |
| `ShazamSignatureGenerator` | `ShazamSignatureGenerator.kt` | Generates API request signatures |
| `MusicRecognitionScreen` | `app/.../ui/screens/musicrecognition/` | UI for the recognition feature |

## Architecture

Full Shazam API client with:
- **Signature-based auth** — each request signed via `ShazamSignatureGenerator`
- **Rate limiting** — prevents API abuse
- **Request queue** — sequential processing of recognition requests
- **Caching** — avoids re-recognizing known songs
- **Retry logic** — handles transient failures

## Testing

```bash
./gradlew :shazamkit:test
```

## Pitfalls

- Uses **CIO engine** — don't share OkHttp-specific config
- Signature generation is critical — if the algorithm changes, recognition breaks
- Rate limits are enforced client-side — adjust limits if API returns 429 responses
- Has a dummy `AndroidManifest.xml` for Android compatibility
- Recognition requires raw audio data — ensure format matches Shazam expectations
