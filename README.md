# Dynamic Pill Pro v1.3 — stable Android overlay

The v1.2 full-screen-cutout flags caused real problems on Android 14 / some OEM firmware: disappearing pill and system status icons drawn in front of music details.

## Fix
- Remove forced cutout, full-screen and no-limits window flags.
- Use a normal TYPE_APPLICATION_OVERLAY at the top of the allowed overlay area **under the status bar**, avoiding icon overlap.
- Keep horizontal camera-cutout centering where Android reports the location; allow manual x/y shift and width adjustment, but clamp vertical y to the safe region.
- Add **KHÔI PHỤC VỊ TRÍ & HIỆN VIÊN THUỐC** and **HIỆN LẠI VIÊN THUỐC**. Recreate the overlay window from scratch without disabling and re-enabling the app.
- Record last known overlay status in local preferences for troubleshooting.
- Retain existing foreground service, media session, notification listener, battery charging and restart attempt after reboot.

## Limitation
Third-party TYPE_APPLICATION_OVERLAY cannot reliably draw above Android status-bar icons, physical camera cutout or lock screen; positioning in the normal overlay area is the stable fallback. Nothing bypasses Android's system permission restrictions.

## Build
Branch `dynamic-pill`, Build Dynamic Pill APK workflow → artifact `DynamicPillPro-v1.3-debug`. Requires Android 8+, Java 17 and standard Android permissions.
