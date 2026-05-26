---
name: opentune
description: Master skill for the OpenTune (SimpMusic) Android music player project. Activates when working across modules or on project-wide concerns.
---

# OpenTune (SimpMusic) Project Skill

This is a multi-module Android (Jetpack Compose) music player app. The project integrates YouTube Music (InnerTube), KuGou, LRCLib, LastFM, Shazam, and multiple lyrics sources.

## Project Structure

```
OpenTune/
  app/          -- Main Android app (UI, ViewModels, DI, Room, Media3)
  innertube/    -- YouTube Music / InnerTube API client
  kugou/        -- KuGou music search & lyrics API
  lrclib/       -- LRCLib.net lyrics API
  lastfm/       -- LastFM scrobbling API
  betterlyrics/ -- BetterLyrics API + TTML parser
  canvas/       -- Animated album artwork API
  kizzy/        -- Discord Rich Presence (Kotlin/JVM)
  shazamkit/    -- Shazam music recognition API
  simpmusic/    -- SimpMusic lyrics API (legacy)
```

## Key Technologies

- **Language:** Kotlin 2.3.20, Java 21 toolchain
- **UI:** Jetpack Compose + Material3
- **DI:** Hilt
- **Database:** Room
- **Playback:** Media3 ExoPlayer
- **Networking:** Ktor (OkHttp/CIO engines)
- **Serialization:** Kotlinx Serialization
- **Image Loading:** Coil
- **Build:** Gradle with version catalog (libs.versions.toml)
- **Min SDK:** 26, Target SDK: 36

## Conventions

- Package namespace: `com.arturo254.opentune` (root), submodules mirror this (`com.arturo254.opentune.innertube`, etc.)
- Library modules are pure Kotlin/JVM (no Android deps), except `:kizzy` and `:shazamkit` which have dummy manifests
- Network clients use Ktor with specific engines (OkHttp for most, CIO for lrclib/simpmusic/shazamkit)
- Lyrics providers are orchestrated via `LyricsHelper` in `:app`
- Room database entities live in `app/src/.../db/entities/`

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

## Build & Run

```bash
# Build
./gradlew assembleDebug

# Install on device
./gradlew installDebug

# Run tests
./gradlew test

# Lint
./gradlew lint
```
