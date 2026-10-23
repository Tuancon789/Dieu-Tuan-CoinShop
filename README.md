# Dynamic Pill Pro v1.2

Android camera-aligned dynamic island inspired overlay, kept on branch `dynamic-pill` (Coin Shop remains on main).

## v1.2 camera fix
- Earlier builds placed `TYPE_APPLICATION_OVERLAY` below the status bar, and moving the y coordinate alone could not overcome cutout avoidance.
- The Android window now requests `FLAG_LAYOUT_IN_SCREEN | FLAG_LAYOUT_NO_LIMITS`, and `layoutInDisplayCutoutMode=ALWAYS` on API 30+, with `SHORT_EDGES` on API 28–29.
- Requests zero fit insets (API 30+) and an origin corrected by the status-bar height. The result depends on OEM system-window policy.
- Quick **ĐẶT VIÊN THUỐC SÁT CAMERA** position reset, manual directional adjustments in 10 dp steps (vertical −100 to 150 dp), adjustable pill width.
- These Android flags do not grant privileged status-bar or keyguard privileges. Some manufacturers still draw the status bar over the app or clip its overlay. The only guaranteed area is the permitted region below system chrome.

## Other features
- Ongoing foreground notification and Android foreground service, keeps operating after leaving Activity when system allows, restart attempt after boot.
- Media session listening and controls, charging indicator, new notification summary with private contents hidden by default.
- No Internet, no screen recording, no accessibility service.

## Build
GitHub Actions on branch `dynamic-pill` -> successful Build Dynamic Pill APK -> download `DynamicPillPro-v1.2-debug`, extract `app-debug.apk`.
