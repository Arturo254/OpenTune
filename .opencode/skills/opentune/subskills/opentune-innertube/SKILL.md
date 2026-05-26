---
name: opentune-innertube
description: Skill for the :innertube module - YouTube Music / InnerTube API client used by OpenTune.
---

# InnerTube (YouTube Music) Module

Package: `com.arturo254.opentune.innertube`
Build: `innertube/build.gradle.kts`

## Key Files

| File | Purpose |
|------|---------|
| `InnerTube.kt` | Main HTTP client providing YouTube Music API access |
| `YouTube.kt` | High-level API parsers - search, browse, albums, artists, playlists, suggestions, lyrics, queue |
| `models/` | 25+ data models for all response types (carousels, shelves, list items, thumbnails, badges, menus, endpoints) |
| `models/body/` | 12 request body models (Browse, Search, Next, Player, Like, Subscribe, Playlist, Queue, Transcript, Account) |
| `models/response/` | 12 response models (Browse, Search, Next, Player, Queue, Transcript, Playlist, Account, Continuation) |
| `pages/` | 20+ page-level response parsers (Home, Search, Album, Artist, Playlist, Charts, Explore, History, Library, MoodAndGenres, NewRelease, etc.) |
| `utils/PoTokenGenerator.kt` | YouTube bot challenge token generator |
| `utils/Utils.kt` | Cookie parsing, SHA1 hashing |

## Key Dependencies

- Ktor client (OkHttp engine)
- NewPipeExtractor (YouTube parsing)
- Brotli (decompression)
- RE2J (regex)
- Rhino (JavaScript engine for YouTube cipher)

## Architecture

- `InnerTube.kt` is the low-level HTTP client that signs and sends requests to YouTube's InnerTube API
- `YouTube.kt` provides high-level typed interfaces that parse raw responses into domain models
- Each `pages/XPage.kt` file handles a specific endpoint's response parsing
- Models use Kotlinx Serialization for JSON parsing
- Uses continuation tokens for pagination
