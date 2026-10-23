# Dynamic Pill Pro — Android

Black iPhone-inspired dynamic pill overlay for Android, without using Accessibility Service, screen recording, internet, or analytics. Separate new Android app from Coin Shop.

## Main capabilities
- Compact black floating pill, tap to expand/collapse; user can turn it off
- Media playback label and play/pause/skip controls using Android MediaSessionManager when a compatible media session is available
- Incoming notification toast from apps you allow via Android notification access; by default only app name is displayed (notification previews must be opted into in app)
- Charging event with battery percentage
- On/off preference and a preview command
- Source-only Android Java app with a GitHub Actions debug APK build

## Permissions & privacy
- **Display over other apps**: Android SYSTEM_ALERT_WINDOW; needed to show the pill overlay, must be granted manually.
- **Notification access**: enables music session metadata, controls and other app notifications. Powerful permission — only grant if you trust the app. Notification content stays on device; app contains no network permission. Preview contents are off by default.
- Notifications and MediaSession control depend on apps and the phone OS. On Android, `TYPE_APPLICATION_OVERLAY` appears **under critical system windows** including the status bar; it cannot exactly reproduce the native iPhone camera cutout effect.
- The overlay is hosted in Android's NotificationListenerService. The OS can disconnect/kill it during aggressive battery optimization, so uptime is not guaranteed; user may need to re-enable the service.
- To uninstall, disable overlay in app and revoke notification permission in system settings.

## Build
On GitHub open Actions → **Build Dynamic Pill APK** → Run workflow. Download Artifact **DynamicPillPro-v1-debug** and extract **app-debug.apk**.

Gradle 8.9, Android Gradle Plugin 8.7.3, Java 17, compileSdk 35, minSdk 26, targetSdk 34. No third-party runtime dependencies.

## Project branch
The connected GitHub access cannot create a repository. This self-contained project is kept in the dedicated `dynamic-pill` branch of the existing repository; the `main` branch remains Coin Shop. The source tree of `dynamic-pill` contains only Dynamic Pill Pro files. Do not merge it into Coin Shop.
