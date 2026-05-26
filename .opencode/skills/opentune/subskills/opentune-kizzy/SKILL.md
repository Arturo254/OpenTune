---
name: opentune-kizzy
description: Skill for the :kizzy module - Discord Rich Presence integration (Kizzy library).
---

# Kizzy (Discord RPC) Module

Package: `com.my.kizzy`
Build: `kizzy/build.gradle.kts`

## Key Files

| File | Purpose |
|------|---------|
| `KizzyLogger.kt` | Logging abstraction interface (injectable via Timber on Android) |
| `gateway/DiscordWebSocket.kt` | WebSocket client connecting to Discord Gateway |
| `gateway/entities/` | Payload, HeartBeat, Identify, Ready, Resume gateway entities |
| `gateway/entities/op/` | OpCode enum + serializer |
| `gateway/entities/presence/` | Activity, Assets, Metadata, Timestamps, Presence models |
| `remote/ApiService.kt` | Discord REST API client |
| `repository/KizzyRepository.kt` | Data repository |
| `rpc/KizzyRPC.kt` | Discord RPC integration |
| `rpc/RpcImage.kt` | RPC image handling |
| `rpc/UserInfo.kt` | Discord user info |
| `utils/Ext.kt` | Extension utilities |

## Key Dependencies

- Ktor client (OkHttp engine)
- Kotlinx Serialization
- org.json

## Architecture

- Full Discord Gateway implementation for Rich Presence
- WebSocket client for real-time communication with Discord
- Has dummy AndroidManifest.xml for Android compatibility
- Used by `app/utils/DiscordRPC.kt` for Discord integration in the app
- Logging is abstracted via `KizzyLogger` to remain JVM-compatible
