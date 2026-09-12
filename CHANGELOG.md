# Changelog

## [1.5.0] - 2026-09-11

### Fixed
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
