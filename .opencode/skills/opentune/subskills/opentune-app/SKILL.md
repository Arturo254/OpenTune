---
name: opentune-app
description: Skill for the main :app Android application module of OpenTune. UI, ViewModels, DI, Room database, Media3 playback, and orchestration logic.
---

# OpenTune App Module

Package: `com.arturo254.opentune`
Build: `app/build.gradle.kts`

## Key Directories

| Path | Purpose |
|------|---------|
| `app/src/main/kotlin/com/arturo254/opentune/` | Source root |
| `.../App.kt` | Application class |
| `.../MainActivity.kt` | Entry activity with Compose NavHost |
| `.../constants/` | App constants (dimensions, prefs, filters) |
| `.../db/` | Room database, DAOs, entities (Song, Album, Artist, Playlist, etc.) |
| `.../di/` | Hilt DI modules (AppModule, NetworkModule) |
| `.../extensions/` | Kotlin extension functions |
| `.../lyrics/` | Lyrics providers & orchestration (LyricsHelper) |
| `.../models/` | Domain models (MediaMetadata, Queue state, etc.) |
| `.../playback/` | Media3 MusicService, ExoPlayer, queues, sleep timer |
| `.../together/` | "Music Together" social listening (WebSocket server/client) |
| `.../ui/component/` | Reusable Compose UI components |
| `.../ui/player/` | Player UI (MiniPlayer, Full Player, Queue, Lyrics) |
| `.../ui/screens/` | Screen composables (Home, Browse, Album, Artist, Search, Settings, etc.) |
| `.../ui/theme/` | Material3 theming |
| `.../ui/utils/` | UI utility extensions |
| `.../utils/` | App utilities (DataStore, DiscordRPC, Scrobbling, Network, Updater) |
| `.../viewmodels/` | ViewModels for all screens |

## Architecture Notes

- UI is fully Jetpack Compose with Material3
- Hilt for dependency injection throughout
- Room database with entities under `db/entities/` (22+ entity types)
- ViewModels use Kotlin coroutines and Flow
- Navigation via Compose NavHost in `MainActivity.kt`
- Media3 ExoPlayer service in `playback/MusicService.kt`
- Lyrics are aggregated from multiple providers via `lyrics/LyricsHelper.kt`

## Key Patterns

- Screens follow `@Composable fun XScreen(viewModel: XViewModel)` pattern
- ViewModels inject repositories via Hilt `@HiltViewModel` + `@Inject constructor`
- Database accessed through `MusicDatabase` Room database class and `DatabaseDao`
- Network module uses `NetworkModule.kt` Hilt module providing Ktor HttpClient
- Theme uses dynamic color (Material You) with fallback
