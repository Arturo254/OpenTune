# app/libs

`discord_partner_sdk.aar` is not committed here — it's Discord's proprietary Social SDK binary
(not our code, and GitHub forks can't accept Git LFS objects anyway).

To build the Discord Social SDK integration locally:

1. Go to the [Discord Developer Portal](https://discord.com/developers/applications), open your
   application, and download the **Social SDK** (`DiscordSocialSdk-<version>.zip`) — no approval
   needed, it's a public download once you have an application created.
2. Extract `discord_social_sdk/lib/release/discord_partner_sdk.aar` from the archive.
3. Copy it into this folder as `app/libs/discord_partner_sdk.aar`.
4. Set `DISCORD_SOCIAL_SDK_CLIENT_ID=<your application's client ID>` in `local.properties`
   (also gitignored, same as the other API keys there), and register the OAuth2 redirect
   `discord-<CLIENT_ID>:/authorize/callback` for your application in the Developer Portal.

Without the AAR present, the app still builds — Gradle just fails at the CMake step for the
`discord_bridge` native target, so only skip this if you're not touching Discord code.
