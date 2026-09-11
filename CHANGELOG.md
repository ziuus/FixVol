# Changelog

## [1.5.0] - 2026-09-11

### Fixed
- Fixed: App crash on launch — splash screen used XML adaptive icon via `R.mipmap.ic_launcher` which Compose's `Image` composable cannot decode; replaced with raster PNG in `drawable/`.
- Fixed: Intelligent volume toggle now starts OFF on first install and is correctly activated when enabled (first-launch toggle bug fixed)

### Added
- Added: Broken-button quick controls — lock screen, screenshot, and per-category volume panel triggers
- Added: Splash screen with FixVol logo shown briefly on app launch
- Added: Debug diagnostics screen with live volume readout and system capability verifier

### Changed
- Changed: Privacy hardening — removed Obtainium reference from debug config

### Removed
- Removed: Obtainium debug dependency reference
