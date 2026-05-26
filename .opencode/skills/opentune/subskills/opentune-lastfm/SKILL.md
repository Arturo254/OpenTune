---
name: opentune-lastfm
description: Skill for the :lastfm module - LastFM scrobbling API client.
---

# LastFM Module

Package: `com.arturo254.opentune.lastfm` | Build: `lastfm/build.gradle.kts`

## File Patterns

Files matching these patterns activate this skill:
- `lastfm/**/*.kt`

## Key Files

| File | Purpose |
|------|---------|
| `LastFM.kt` | API client — token/session auth flow, API signing with MD5, scrobble/now-playing updates |
| `models/Authentication.kt` | Auth models: `TokenResponse`, `SessionResponse`, error types |

## Key Dependencies

- Ktor client (OkHttp engine)
- Kotlinx Serialization

## Key Entry Points

| Class | File | Role |
|-------|------|------|
| `LastFM` | `LastFM.kt` | Main client: `getToken()`, `getSession(token)`, `scrobble()`, `nowPlaying()` |
| `ScrobbleManager` | `app/.../utils/ScrobbleManager.kt` | App-level scrobbling orchestration |
| `LastFMSettings` | `app/.../ui/screens/settings/` | Settings UI for LastFM login |

## Architecture

Implements the standard LastFM API auth flow (REST):
1. Get a request token via `auth.getToken`
2. User authorizes in browser (handled via WebView/LoginScreen)
3. Get session key via `auth.getSession`
4. Sign every API call with MD5(`method + api_key + params + secret`)

## Common Tasks

### Adding a new LastFM API call
1. Add the method in `LastFM.kt` following existing patterns
2. Add `api_sig` parameter (MD5 signature) to the request
3. Parse response with Kotlinx Serialization
4. Handle `LastFmError` responses

## Testing

```bash
./gradlew :lastfm:test
```

## Pitfalls

- All API calls must be signed with MD5 (except `auth.getToken` and `auth.getSession` in some flows)
- The MD5 signature must be generated with parameters in **alphabetical order** (parameter names sorted A-Z)
- Session key is sensitive — stored via DataStore in the app, not hardcoded
- LastFM API has rate limits — scrobble calls should be queued
- Uses OkHttp engine (consistent with most modules)
