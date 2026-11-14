# IBRAHIM IPTV — Android TV

Native Android app built with Kotlin, Jetpack Compose, AndroidX TV Material and Media3 ExoPlayer.

## Requested navigation

The left navigation contains:
- **IPTV** — live channels
- **MOVIES** — VOD movies
- **TV SHOWS** — series
- Home, Search and Settings

## Xtream Codes sign-in

Open Settings and enter these three values:
1. **XTREAM SERVER** — provider base URL, for example `https://provider.example:port`
2. **USERNAME**
3. **PASSWORD**

Use credentials and a server address supplied by an IPTV provider you are authorized to access. This project currently requires an HTTPS server URL.

## Stream formats

Live channels use the Xtream live endpoint with the **`.ts` transport-stream extension**:
`/live/username/password/stream_id.ts`

Movie playback uses the container extension returned by the provider. Series are listed from the provider catalogue; episode browsing/playback is a future enhancement.

## Build an APK

Requirements:
- Android Studio Ladybug or newer
- JDK 17
- Android SDK Platform 35
- Internet access

1. Open this project folder in Android Studio.
2. Allow Gradle sync to finish.
3. Select **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
4. The debug APK is normally created at `app/build/outputs/apk/debug/app-debug.apk`.

GitHub Actions also builds a debug APK when changes are pushed to `main`. Download it from the successful workflow run's **Artifacts** section.

## Android TV

- Includes `LEANBACK_LAUNCHER` and declares touchscreen optional.
- Landscape-first layout with focusable navigation and poster cards.
- Media3 ExoPlayer provides playback controls.
- Minimum Android API 23.

## Notes

This is a debug build, not a signed production release. Test with your provider and target TV devices before distribution. Credentials are currently stored in app-private preferences; encrypted credential storage should be added before public release.
