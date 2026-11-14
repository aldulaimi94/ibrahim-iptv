# IBRAHIM IPTV — Android TV

Native Android application starter built with Kotlin, Jetpack Compose, AndroidX TV Material dependency, Media3 ExoPlayer and OkHttp.

## Requirements

- Android Studio Ladybug or newer
- JDK 17
- Android SDK Platform 35
- Internet access
- Authorized Xtream Codes-compatible IPTV subscription

## Build an APK

1. Open this folder in Android Studio.
2. Allow Gradle sync to finish.
3. Select **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
4. The debug APK is normally created at:
   `app/build/outputs/apk/debug/app-debug.apk`

For a signed release APK, use **Build → Generate Signed Bundle / APK** and create a release keystore. Keep the keystore private and backed up.

## Android TV support

- Includes `LEANBACK_LAUNCHER` and declares touchscreen optional.
- Landscape-first layout, focusable controls and horizontal content rails.
- Remote navigation uses Android focus/D-pad behavior.
- Media3 ExoPlayer provides playback controls.
- Minimum Android API 23.

## Current implementation scope

- Xtream Codes authentication and catalog retrieval for live, VOD and series listings.
- Provider credentials saved in app-private SharedPreferences.
- Home dashboard, left navigation, search, settings and full-screen playback.
- Uses the provider's poster/icon URLs when available.

## Important next steps before public release

- Implement proper encrypted credential storage using Android Keystore.
- Add category browsing, series episode lookup and resume/watch history.
- Test remote focus order on actual Android TV devices and multiple OEM launchers.
- Add app icon, TV banner artwork, privacy policy, error telemetry and accessibility review.
- Confirm provider terms authorize the app and the streams being played.
- Configure HTTPS provider endpoints. The starter intentionally rejects plain HTTP.
- Test playback with the specific provider: some services impose browser/device restrictions or use formats requiring additional Media3 modules.

This project is source code, not a precompiled APK. Build the APK with Android Studio using the steps above.
