---
name: opentune-lastfm
description: Skill for the :lastfm module - LastFM scrobbling API client for tracking played songs.
---

# LastFM Module

Package: `com.arturo254.opentune.lastfm`
Build: `lastfm/build.gradle.kts`

## Key Files

| File | Purpose |
|------|---------|
| `LastFM.kt` | LastFM API client - authentication (token/session flow), API signing with MD5, scrobbling track updates |
| `models/Authentication.kt` | Auth models (token, session), error types |

## Key Dependencies

- Ktor client (OkHttp engine)
- Kotlinx Serialization

## Architecture

- Implements LastFM API authentication flow (getToken -> getSession)
- Signs all API requests with MD5 per LastFM spec
- Used by `app/utils/ScrobbleManager.kt` for scrobbling
- Settings UI in `app/ui/screens/settings/` for LastFM login
