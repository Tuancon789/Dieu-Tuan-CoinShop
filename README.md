# Dynamic Pill Pro v1.1

Android overlay inspired by iPhone's black Dynamic Island. The project is separate from Coin Shop and maintained only on the `dynamic-pill` branch.

## Updates in v1.1
- Detect the center of the display cutout (where Android provides that information), plus manual ±5 dp horizontal/vertical adjustment and width resizing.
- A user-enabled **foreground service** shows an ongoing notification to keep the process alive after leaving the app or removing the Activity from recents.
- `BootReceiver` attempts to restart service after a reboot or app update when enabled and allowed by the phone.
- Notification on lock screen: displays an ongoing service notification if the user allows lock-screen notifications. Overlay itself may not be visible above Android's keyguard; `TYPE_APPLICATION_OVERLAY` is below critical system windows including status bar.
- MediaSession metadata and controls from apps supporting standard Android sessions; notification listener and charging updates; message previews disabled by default.
- Permission buttons, battery optimization settings shortcut and preview action.

## Limitations
Android may terminate a foreground service for battery optimization, force-stop, OEM restrictions or user action. **No third-party Android app can guarantee uninterrupted background operation** on every device. A new reboot may be restricted by Android's foreground-service rules. Check Android's app battery settings and allow background operation/auto-start when needed.

The display cutout bounds are approximate. Android status bar and lock screen may cover the camera area, and the app cannot reproduce the exact native iPhone island. Lock screen notifications contain no private message contents.

### Permissions
Display over other apps; notification listener access; notification posting (Android 13+) for persistent status; foreground service; receive boot completed. No internet permission, no screen capture or Accessibility service.

## Build
GitHub Actions → Build Dynamic Pill APK on branch `dynamic-pill`, download `DynamicPillPro-v1.1-debug`, extract `app-debug.apk`.
