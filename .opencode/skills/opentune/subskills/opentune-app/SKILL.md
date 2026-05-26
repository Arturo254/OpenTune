---
name: opentune-app
description: Skill for the main :app Android application module. UI, ViewModels, Hilt DI, Room database, Media3 playback, lyrics orchestration.
---

# OpenTune App Module

Package: `com.arturo254.opentune` | Build: `app/build.gradle.kts`

## File Patterns

Files matching these patterns activate this skill:
- `app/**/*.kt`
- `app/**/*.kts`
- `app/src/main/res/**`

## Key Directories

| Path | Purpose |
|------|---------|
| `.../di/` | Hilt modules: `AppModule` (DB, player cache), `NetworkModule` (connectivity observer) |
| `.../db/` | Room database: `MusicDatabase`, `DatabaseDao`, 16 entities, 3 views |
| `.../db/entities/` | `SongEntity`, `ArtistEntity`, `AlbumEntity`, `PlaylistEntity`, `SongArtistMap`, `SongAlbumMap`, `AlbumArtistMap`, `PlaylistSongMap`, `SearchHistory`, `FormatEntity`, `LyricsEntity`, `Event`, `RelatedSongMap`, `SetVideoIdEntity`, `PlayCountEntity`, `TagEntity`, `PlaylistTagMap` |
| `.../lyrics/` | 6 `LyricsProvider` objects + `LyricsHelper` orchestrator + `LyricsPreloadManager` |
| `.../playback/` | `MusicService` (Media3), `ExoDownloadService`, `PlayerConnection`, `SleepTimer`, queues |
| `.../playback/queues/` | `Queue`, `ListQueue`, `EmptyQueue`, `YouTubeQueue`, `LocalAlbumRadio`, `LocalMixQueue`, `YouTubeAlbumRadio` |
| `.../together/` | "Music Together" WebSocket server/client for social listening |
| `.../ui/component/` | 40+ reusable Compose components (lyrics display, player slider, bottom sheets, shimmer, search bar, dialogs) |
| `.../ui/player/` | `Player.kt`, `MiniPlayer.kt`, `Queue.kt`, `LyricsScreen.kt`, `CanvasArtworkPlayer` |
| `.../ui/screens/` | Top-level screens: Home, Search, Library, Browse, Album, Artist, Playlist, Charts, Stats, History, Settings (30+), etc. |
| `.../ui/theme/` | Material3 theming with dynamic color (Material You) |
| `.../viewmodels/` | 24+ ViewModels for every screen |
| `.../constants/` | `Dimensions.kt`, `PreferenceKeys.kt`, `HistorySource.kt`, `LibraryFilter.kt`, `MediaSessionConstants.kt`, `StatPeriod.kt` |
| `.../extensions/` | 10 Kotlin extension files (Context, Coroutines, ExoPlayer, Files, Lists, MediaItem, Player, Queue, String, Utils) |
| `.../models/` | Domain models: `ItemsPage`, `MediaMetadata`, `PersistPlayerState`, `PersistQueue`, `PlaylistSuggestion`, `SimilarRecommendation` |
| `.../utils/` | Utilities: DataStore, DiscordRPC, ScrobbleManager, NetworkObserver, Updater, Sync, Translator, Bitmap, Cache |

## Key Entry Points

| Class | File | Role |
|-------|------|------|
| `App` | `App.kt` | Application class, Hilt entry point |
| `MainActivity` | `MainActivity.kt` | Entry activity, sets up Compose NavHost |
| `NavigationBuilder` | `ui/screens/NavigationBuilder.kt` | Registers all routes via `NavGraphBuilder` extension |
| `Screens` | `ui/screens/Screens.kt` | Route/icon definitions for bottom nav |
| `MusicDatabase` | `db/MusicDatabase.kt` | Room database (16 entities, version 27) |
| `DatabaseDao` | `db/DatabaseDao.kt` | Single DAO interface for all DB operations |
| `MusicService` | `playback/MusicService.kt` | Media3 ExoPlayer service |
| `LyricsHelper` | `lyrics/LyricsHelper.kt` | Orchestrates 6 lyrics providers |
| `LyricsProvider` | `lyrics/LyricsProvider.kt` | Interface that all lyrics providers implement |
| `AppModule` | `di/AppModule.kt` | Hilt module providing DB and caches |
| `NetworkModule` | `di/NetworkModule.kt` | Hilt module providing connectivity observer |

## Common Tasks

### Adding a new screen
1. Create `ui/screens/XxxScreen.kt` with `@Composable fun XxxScreen(...)` and `ui/viewmodels/XxxViewModel.kt` with `@HiltViewModel`
2. Register in `NavigationBuilder.kt`: `composable("route") { XxxScreen(...) }`
3. Add to `Screens.kt` if bottom nav item, or navigate via `navController.navigate("route")`
4. Add string resource + icon if needed

### Adding a new Room entity
1. Create data class in `db/entities/`
2. Add to `MusicDatabase.kt` `@Database(entities = [...])`
3. Add DAO methods in `DatabaseDao.kt`
4. Write migration in `InternalDatabase.kt`, bump `CURRENT_VERSION`
5. Create ViewModel to expose data

### Adding a new lyrics provider
1. Create `object XxxProvider : LyricsProvider` in `app/.../lyrics/`
2. Add to `LyricsHelper.baseProviders` list
3. Add entry in `PreferredLyricsProvider` enum for ordering
4. API module goes in its own `:xxx` library module; or implement inline

### Navigating routes
Routes use string paths: `"album/{albumId}"`, `"artist/{artistId}"`, `"online_playlist/{playlistId}"`, `"local_playlist/{playlistId}"`, `"search/{query}"`, etc. Pass args via `navController.navigate("album/$albumId")`.

### Using DataStore preferences
`PreferenceKeys.kt` defines all keys. Access via `dataStore.data.map { it[PreferenceKey] }`. Write via `dataStore.edit { it[PreferenceKey] = value }`.

## Testing

```bash
./gradlew :app:testDebugUnitTest   # Unit tests
./gradlew :app:lint                # Lint check
```

Room migrations are tested separately via migration test classes.

## Pitfalls

- Never remove a Room entity or column without writing a migration — DB version 27
- `MusicService` is a foreground service — must handle lifecycle correctly
- Lyrics provider order matters: user preference reorders the base list
- ExoPlayer cache size is configurable via DataStore (default 1024 MB)
- Navigation routes use route templates — ensure parameter names match between route def and `NavArgument`
