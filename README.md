# AMz Browser (Android — Kotlin + Jetpack Compose)

Single-module, single-Activity Android browser built with Jetpack Compose, Android System WebView, DataStore Preferences, Haze Liquid Glass blur, Gyro Tilt physics, and built-in Ad Shield.

## Option 1: Build & Install via USB Debugging (Android Studio or CLI)

1. Extract this ZIP and open the `AMzBrowser-Android` folder in **Android Studio** (or install JDK 17 + Android SDK Platform 35).
2. Enable **USB Debugging** on your Android phone (Settings → Developer options → USB debugging) and connect via USB.
3. Run from Android Studio (**Run ▶**), or from terminal:

```bash
gradle assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

The compiled APK will be at:
`app/build/outputs/apk/debug/app-debug.apk`

## Option 2: Build the APK in the Cloud (No Android Studio Needed)

1. Upload the contents of this folder to a new GitHub repository.
2. Open the **Actions** tab and run **Build AMz Browser APK** (it also runs automatically on every push).
3. Download the compiled `AMzBrowser-debug-apk` artifact straight to your phone and install it.

## Pluggable Slots
- **Download Engine**: Swap `BrowserServices.downloadEngine` in `app/src/main/java/com/amz/browser/download/DownloadEngine.kt`.
- **Card Results (Beta)**: Swap `BrowserServices.searchProvider` in `app/src/main/java/com/amz/browser/search/SearchProvider.kt`.
