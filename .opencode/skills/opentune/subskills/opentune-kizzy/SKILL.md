---
name: opentune-kizzy
description: Skill for the :kizzy module - Discord Rich Presence integration (Kizzy library).
---

# Kizzy (Discord RPC) Module

Package: `com.my.kizzy` | Build: `kizzy/build.gradle.kts`

## File Patterns

Files matching these patterns activate this skill:
- `kizzy/**/*.kt`

## Key Files

| File | Purpose |
|------|---------|
| `KizzyLogger.kt` | Logging abstraction — injectable via Timber on Android, no-op on JVM |
| `gateway/DiscordWebSocket.kt` | WebSocket client for Discord Gateway real-time connection |
| `gateway/entities/` | Gateway entities: `Payload`, `HeartBeat`, `Identify`, `Ready`, `Resume` |
| `gateway/entities/op/` | `OpCode` enum (`DISPATCH`, `HEARTBEAT`, `IDENTIFY`, `PRESENCE_UPDATE`, etc.) + serializer |
| `gateway/entities/presence/` | `Activity`, `Assets`, `Metadata`, `Timestamps`, `Presence` models |
| `remote/ApiService.kt` | Discord REST API client (for app info, user info) |
| `repository/KizzyRepository.kt` | Repository layer combining API + gateway |
| `rpc/KizzyRPC.kt` | High-level RPC integration |
| `rpc/RpcImage.kt` | Image handling for Discord rich presence assets |
| `rpc/UserInfo.kt` | Discord user info model |
| `utils/Ext.kt` | Extension utilities |

## Key Dependencies

| Dependency | Purpose |
|------------|---------|
| Ktor OkHttp | HTTP + WebSocket client |
| Kotlinx Serialization | JSON for gateway payloads and REST |
| org.json | Additional JSON handling |

## Key Entry Points

| Class | File | Role |
|-------|------|------|
| `DiscordWebSocket` | `gateway/DiscordWebSocket.kt` | Real-time Gateway connection |
| `KizzyRPC` | `rpc/KizzyRPC.kt` | High-level presence management |
| `KizzyRepository` | `repository/KizzyRepository.kt` | Data access layer |
| `DiscordRPC` | `app/.../utils/DiscordRPC.kt` | App-level integration point |

## Common Tasks

### Updating presence/activity
Use `KizzyRPC` to set activity with details (song name), state (artist), timestamps, and assets (album art). The `Activity` model maps to Discord's Rich Presence fields.

### Handling reconnect
The `DiscordWebSocket` handles reconnection via `HeartBeat`/`Resume` entities. The Gateway uses opcode-based protocol — see `OpCode` enum for all message types.

## Testing

```bash
./gradlew :kizzy:test
```

## Pitfalls

- Package namespace is `com.my.kizzy` (different from the project's `com.arturo254.opentune` convention)
- Has a dummy `AndroidManifest.xml` for Android compatibility — don't add Android deps to build.gradle.kts
- `KizzyLogger` is abstracted to stay JVM-compatible — use `Timber` on Android side
- Discord Gateway requires opcode-specific handling — `HeartBeat` must be sent on interval matching the `heartbeat_interval` from Ready payload
- WebSocket may disconnect — `Resume` payloads handle reconnection with session IDs
- org.json is used alongside Kotlinx Serialization — be consistent when adding new models
