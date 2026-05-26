---
name: opentune-innertube
description: Skill for the :innertube module - YouTube Music / InnerTube API client.
---

# InnerTube (YouTube Music) Module

Package: `com.arturo254.opentune.innertube` | Build: `innertube/build.gradle.kts`

## File Patterns

Files matching these patterns activate this skill:
- `innertube/**/*.kt`

## Key Files

| File | Purpose |
|------|---------|
| `InnerTube.kt` | Low-level HTTP client — signs and sends requests to YouTube's InnerTube API |
| `YouTube.kt` | High-level API — parses InnerTube responses into domain models (search, browse, albums, artists, playlists, suggestions, lyrics, queue) |
| `models/` | 25+ data models: `MusicCarouselShelfRenderer`, `MusicResponsiveListItemRenderer`, `MusicTwoRowItemRenderer`, `MusicShelfRenderer`, `MusicPlaylistShelfRenderer`, `MusicQueueRenderer`, `SectionListRenderer`, `GridRenderer`, `Thumbnails`, `Badges`, `Button`, `Menu`, `Endpoint`, `NavigationEndpoint`, `Context`, `Continuation`, `YTItem`, `MediaInfo`, `SearchSuggestions`, `AccountInfo`, `YouTubeClient`, `YouTubeLocale`, `YouTubeDataPage` |
| `models/body/` | 12 request body models: `BrowseBody`, `SearchBody`, `NextBody`, `PlayerBody`, `LikeBody`, `SubscribeBody`, `CreatePlaylistBody`, `EditPlaylistBody`, `GetQueueBody`, `GetSearchSuggestionsBody`, `GetTranscriptBody`, `AccountMenuBody` |
| `models/response/` | 12 response models: `BrowseResponse`, `SearchResponse`, `NextResponse`, `PlayerResponse`, `GetQueueResponse`, `GetTranscriptResponse`, `GetSearchSuggestionsResponse`, `CreatePlaylistResponse`, `AddItemYouTubePlaylistResponse`, `AccountMenuResponse`, `ContinuationResponse` |
| `pages/` | 20+ page parsers: `HomePage`, `SearchPage`, `SearchSuggestionPage`, `SearchSummaryPage`, `AlbumPage`, `ArtistPage`, `ArtistItemsPage`, `ArtistItemsContinuationPage`, `PlaylistPage`, `PlaylistContinuationPage`, `ChartsPage`, `ExplorePage`, `HistoryPage`, `LibraryPage`, `LibraryAlbumsPage`, `LibraryContinuationPage`, `MoodAndGenres`, `NewReleaseAlbumPage`, `NextPage`, `RelatedPage`, `BrowseResult`, `PageHelper`, `NewPipe` |
| `utils/PoTokenGenerator.kt` | YouTube bot challenge token generator (uses Rhino JS engine) |
| `utils/Utils.kt` | Cookie parsing, SHA1 hashing |

## Key Dependencies

| Dependency | Purpose |
|------------|---------|
| Ktor OkHttp | HTTP client engine |
| NewPipeExtractor | YouTube response parsing helpers |
| Brotli | Decompression of YouTube responses |
| RE2J | Regex (replaces Java regex for YouTube cipher) |
| Rhino | JavaScript engine for YouTube signature deciphering |

## Key Entry Points

| Class | File | Role |
|-------|------|------|
| `InnerTube` | `InnerTube.kt` | HTTP client — all YouTube API calls go through this |
| `YouTube` | `YouTube.kt` | Typed API layer — what app code calls directly |
| `PoTokenGenerator` | `utils/PoTokenGenerator.kt` | Generates PoTokens for bot-protected endpoints |
| `NewPipe` | `pages/NewPipe.kt` | NewPipeExtractor integration for video/stream extraction |

## Common Tasks

### Adding a new API endpoint
1. Add request body model in `models/body/` (extends `BaseBody` with `context`)
2. Add response model in `models/response/` (Kotlinx Serialization `@Serializable`)
3. Add a method in `InnerTube.kt` that POSTs to the endpoint
4. Add a typed parser method in `YouTube.kt` that calls `InnerTube` and maps to domain types
5. Optionally add a page parser in `pages/` if the response needs complex transformation

### Adding a new response field
1. Add the field to the existing `@Serializable` data class in `models/response/`
2. Use `@SerialName` if the JSON key differs from the field name
3. Add `@EncodeDefault` or provide a default value for optional fields
4. Update the page parser in `pages/` if the field drives UI logic

### Handling YouTube API changes
- YouTube frequently changes InnerTube response structure — check `models/response/` for parse failures
- The `NewPipe` page may need updates when YouTube changes video formats
- `PoTokenGenerator` may break if YouTube updates its bot challenge

### Using continuation/pagination
Continuation tokens are returned in responses. Pass them back in the next request body. Each page parser has a `continuation` field — use it to load more items by calling the same endpoint with the continuation body.

## Architecture

```
App Code -> YouTube.kt (typed methods) -> InnerTube.kt (HTTP client) -> YouTube API
                  \                          /
              models/response/       models/body/
                        \              /
                    pages/XPage.kt (parsers)
```

- Each `pages/XPage.kt` receives a raw JSON response and extracts typed domain objects
- Models use Kotlinx Serialization with `@SerialName` annotations for YouTube's snake_case JSON
- Continuation is handled by passing back opaque continuation tokens

## Pitfalls

- YouTube API is unstable — endpoints change without notice
- `PoTokenGenerator` uses Rhino JS engine which can be slow — cache tokens when possible
- NewPipeExtractor version may need updates when YouTube changes streaming formats
- Response models should use `@EncodeDefault` / defaults for new optional fields to avoid breaking existing parses
- Brotli decompression is required — ensure the Ktor pipeline includes Brotli plugin
- Some endpoints require specific `context.client.version` values — check existing bodies for reference
