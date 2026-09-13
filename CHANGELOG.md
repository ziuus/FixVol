# Changelog

## [1.5.1] - 2026-09-13

### Added
- Added: Notification quick-control action buttons — Volume, Screenshot, Lock screen (power button), Power Menu, and Open App (jump to app) directly from the foreground service notification

### Fixed
- Fixed: Notification no longer shows "Native volume control active" text in content; actions are now the primary interaction surface

## [1.5.2] - 2026-09-13

### Fixed
- Fixed: Notification lock screen button crash — replaced broken DeviceAdminReceiver approach with reliable PowerManager.goToSleep() via reflection (API 29+) + ACTION_SCREEN_OFF broadcast fallback
- Fixed: Screenshot capture broken — switched from unreliable ImageFormat.PRIVATE + HardwareBuffer.wrapHardwareBuffer() to ImageFormat.JPEG + direct byte buffer; added 300ms render delay for reliable capture
- Fixed: Toggle button always showed "on" on fresh install — added serviceRunning state derivation so toggle reflects actual background watcher state (serviceRunning && settings.enabled)
- Fixed: BootReceiver crash — flow.first() suspend call from BroadcastReceiver now wrapped in runBlocking
- Fixed: SettingsRepository missing floatingControlsEnabled persistence — added KEY_FLOATING_CONTROLS_ENABLED, settingsFlow mapping, and setFloatingControlsEnabled() setter
- Fixed: Duplicate Notification/NotificationChannel/NotificationManager/PendingIntent imports in PlaybackMonitorService
- Fixed: App crash on launch — splash screen used XML adaptive icon via `R.mipmap.ic_launcher` which Compose's `Image` composable cannot decode; splash screen and all compose animation transitions permanently removed to eliminate the crash on this device.
- Fixed: Intelligent volume toggle now starts OFF on first install and is correctly activated when enabled (first-launch toggle bug fixed)

### Added
- Added: Broken-button quick controls — lock screen, screenshot, and per-category volume panel triggers
- Added: Debug diagnostics screen with live volume readout and system capability verifier

### Changed
- Changed: Privacy hardening — removed Obtainium reference from debug config

### Removed
- Removed: Obtainium debug dependency reference
- Removed: Splash screen (`FixVolSplash.kt`) and all compose animation transitions — caused `IllegalArgumentException` on this device with no viable workaround without Compose version upgrade.

## [1.5.3] - 2026-09-13

### Fixed
- Fixed: Screenshot now works on Android 10 (API 29) — was incorrectly requiring Android 11 (API 30) in `ScreenshotActivity` and `QuickControlsCard`. MediaProjection is available from API 29.
- Fixed: Lock screen button no longer silently fails — removed dead `ACTION_SCREEN_OFF` broadcast fallback (third-party apps cannot trigger this). Now logs when `PowerManager.goToSleep` fails instead of falling back to a no-op.
- Fixed: Power Menu button no longer opens wrong settings screen — replaced broken `ACTION_USAGE_ACCESS_SETTINGS` intent (copy-paste bug) with honest diagnostics logging. No reliable public API exists for third-party apps to trigger the system power menu.
- Fixed: `ScreenshotCapture` composable now uses `ImageFormat.JPEG` instead of `ImageFormat.PRIVATE` for consistent capture across devices.
- Fixed: QuickControlsCard lock button exception handling — now logs failures instead of silently catching and discarding all exceptions.
