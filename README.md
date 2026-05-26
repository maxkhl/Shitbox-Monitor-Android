# Shitbox Monitor — Android

Companion Android app + home-screen widget for the camper monitor.

## What's in it

- **App**: full dashboard showing Battery, MPPT/Solar, GPS, LTE — polls `/api/data` every 5 s while open.
- **Widget**: home-screen tile showing Battery SOC %, Solar W, LTE signal bars. Refreshes every ~15 min via WorkManager.

## Configuring the server URL

The default is `http://192.168.1.50:8000`. To change it: open the app,
tap the **⚙** in the top-right of the header, enter the URL, hit Speichern.
The dashboard re-polls immediately and the widget refreshes too.

The default is defined in
`app/src/main/java/com/shitbox/monitor/data/SettingsStore.kt`
if you want to change the first-launch value.

## Build

Easiest: open the project root in **Android Studio** (Iguana or newer). It will:
1. Generate the Gradle wrapper (`gradlew`, `gradle-wrapper.jar`) on first sync — these aren't committed.
2. Resolve all dependencies.
3. Let you Run on a connected phone / emulator.

CLI-only build (after first opening once in Android Studio so the wrapper is generated):
```sh
./gradlew assembleDebug
# APK lands at app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

For a signed release APK (to share / sideload without USB):
```sh
./gradlew assembleRelease
```
You'll need to set up a signing config — Android Studio's *Build → Generate Signed Bundle / APK* walks through it.

## Adding the widget

After install: long-press home screen → Widgets → "Shitbox Monitor" → drag to home.

## Notes

- App talks plain HTTP (cleartext). `usesCleartextTraffic="true"` is set in the manifest for LAN convenience.
- Widget refresh minimum is 15 min — Android's WorkManager limit. If you need faster updates, open the app.
- Only the dashboard polls live (every 5 s) while it's foregrounded.
