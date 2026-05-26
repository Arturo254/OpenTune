---
name: opentune
description: Master skill for the OpenTune (SimpMusic) Android music player project. Activates when working across modules or on project-wide concerns.
---

# OpenTune (SimpMusic) Project Skill

Multi-module Android (Jetpack Compose) music player. Integrates YouTube Music (InnerTube), KuGou, LRCLib, LastFM, Shazam, and multiple lyrics sources.

## Project Modules

```
:app            -- Main Android app (UI, ViewModels, DI, Room DB, Media3 playback)
:innertube      -- YouTube Music / InnerTube API client (Kotlin/JVM)
:kugou          -- KuGou music search & lyrics API (Kotlin/JVM)
:lrclib         -- LRCLib.net lyrics API (Kotlin/JVM)
:lastfm         -- LastFM scrobbling API (Kotlin/JVM)
:betterlyrics   -- BetterLyrics API + TTML parser (Kotlin/JVM)
:canvas         -- Animated album artwork API (Kotlin/JVM)
:kizzy          -- Discord Rich Presence (Kotlin/JVM, has dummy manifest)
:shazamkit      -- Shazam music recognition (Kotlin/JVM, has dummy manifest)
:simpmusic      -- SimpMusic legacy lyrics API (Kotlin/JVM)
```

## Key Technologies

| Tech | Detail |
|------|--------|
| **Language** | Kotlin 2.3.20, Java 21 toolchain |
| **UI** | Jetpack Compose + Material3 1.5.0 |
| **DI** | Hilt 2.59.2 (SingletonComponent) |
| **Database** | Room 2.8.4 (16 entities, 3 views, version 27) |
| **Playback** | Media3 ExoPlayer 1.10.0 |
| **Networking** | Ktor 3.4.2 (OkHttp / CIO engines) |
| **Serialization** | Kotlinx Serialization |
| **Image Loading** | Coil 3.4.0 |
| **Build** | AGP 9.1.0, KSP, version catalog `gradle/libs.versions.toml` |
| **SDK** | Min 26, Target 36, Compile 36 |

## Conventions

- Root namespace: `com.arturo254.opentune`; submodules mirror this (`com.arturo254.opentune.innertube`, etc.)
- Library modules are pure Kotlin/JVM (no Android deps), except `:kizzy` and `:shazamkit` which have dummy manifests
- Ktor engine varies per module: OkHttp for most, CIO for `:lrclib`, `:simpmusic`, `:shazamkit`
- Lyrics providers implement `LyricsProvider` interface (singleton `object`) and are registered in `LyricsHelper.baseProviders` ordered by user preference
- Room database lives in `app/.../db/`, entities in `db/entities/`, DAO via `DatabaseDao`, migrations via `InternalDatabase`
- Screens registered via `NavGraphBuilder.navigationBuilder()` extension in `NavigationBuilder.kt`
- ViewModels use `@HiltViewModel` with `@Inject constructor` and `SavedStateHandle`
- Gradle version catalog uses `[versions]`, `[libraries]`, `[plugins]` sections

## Common Workflows

### Adding a new screen
1. Create the screen composable in `ui/screens/` or a subdirectory
2. Create a ViewModel in `viewmodels/` with `@HiltViewModel`
3. Register the route in `NavigationBuilder.kt` via `composable(...)` and add the route/icon to `Screens.kt` if it goes in the bottom nav
4. Add string resources and navigation calls

### Adding a new Room entity
1. Create the entity data class in `db/entities/`
2. Add it to `MusicDatabase.kt` `@Database(entities = [...])`
3. Add DAO methods in `DatabaseDao.kt`
4. Write a migration in `InternalDatabase.kt` and increment `CURRENT_VERSION`
5. Create a ViewModel to expose the data

### Adding a new lyrics provider
1. Create an `object` implementing `LyricsProvider` in `app/.../lyrics/`
2. Add it to `LyricsHelper.baseProviders` list
3. Add an entry in `PreferredLyricsProvider` enum for user ordering preference
4. Add a toggle in settings if needed

### Adding a new API client module
1. Create a Kotlin/JVM library module with `build.gradle.kts`
2. Use Ktor + Kotlinx Serialization
3. Keep it Android-free (pure JVM target)
4. Add `include(":module-name")` to `settings.gradle.kts`
5. Add `implementation(project(":module-name"))` to `app/build.gradle.kts`
6. Wire it into the app via Hilt or a provider class

## Pitfalls

- `simpmusic` is included in `settings.gradle.kts` as `include("simpmusic")` (no leading `:`) — don't change it without testing
- If a Room migration breaks, the app will crash on startup — always test migrations
- YouTube InnerTube endpoints are unstable and may require `PoTokenGenerator` updates
- Ktor CIO engine modules (`lrclib`, `simpmusic`, `shazamkit`) can't share OkHttp-specific config
- Database version 27 uses fallback to destructive migration — reverting entities may wipe user data

## Build Commands

```bash
./gradlew assembleDebug          # Build debug APK
./gradlew installDebug           # Install on device
./gradlew test                   # Run all unit tests
./gradlew :app:lint              # Lint the app module
./gradlew :innertube:test        # Test a specific module
```

## Subskills

- `subskills/opentune-app` - Main Android app module
- `subskills/opentune-innertube` - YouTube/InnerTube API
- `subskills/opentune-kugou` - KuGou music API
- `subskills/opentune-lrclib` - LRCLib lyrics API
- `subskills/opentune-lastfm` - LastFM scrobbling API
- `subskills/opentune-betterlyrics` - BetterLyrics API
- `subskills/opentune-canvas` - Canvas artwork API
- `subskills/opentune-kizzy` - Discord RPC (Kizzy)
- `subskills/opentune-shazamkit` - Shazam recognition
- `subskills/opentune-simpmusic` - SimpMusic legacy API
