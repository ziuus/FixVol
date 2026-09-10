# Changelog

All notable changes to **FixVol** will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [1.3.0] - 2026-09-10

### Removed

- **AdMob advertising SDK**: Removed all AdMob dependencies, initialization, and banner ad UI. FixVol now operates fully offline with no internet permission and no advertising.

### Fixed

- **Release pipeline**: Added signing keystore and GitHub Actions secrets so the release workflow produces signed release APKs (`FixVol-vX.Y.Z.apk`) usable by Obtainium.
- **Obtainium integration**: Added Obtainium install badge to README with one-tap import link.
- **Documentation accuracy**: Corrected privacy claims, CONTRIBUTING guide, and repository URLs to match the actual codebase.

### Changed

- **Session debounce**: Renamed "First Play Only session tracking" to "Trigger Only On First Play" debounce — local-only session window to suppress duplicate volume panel triggers. No telemetry, no remote data.

---

## [1.2.0] - 2026-09-08

### Added

- **Premium app icon**: New waveform icon with indigo-violet gradient.

---

## [1.1.0] - 2026-09-07

### Added

- **First Play Only debounce**: Volume panel fires once per audio session, suppressing continuous triggers during active playback.
- **KMP Shared Module**: Extracted core business logic into a Kotlin Multiplatform shared module for future iOS support.

---

## [1.0.0] - 2026-09-07

### Added

- **Core Playback Monitoring**: Intercepts Android `AudioPlaybackCallback` and active `AudioPlaybackConfiguration` events.
- **Audio Classification**: `PlaybackClassifier` maps Android `AudioAttributes` usages to `AudioCategory` (Media, Alarm, Ringtone, Notifications, Calls, System).
- **Audio Route Detection**: Detects Speaker, Bluetooth, Headset, USB output routing.
- **Native SystemUI Controller**: Displays native Android volume slider via `adjustStreamVolume` with zero volume level changes.
- **Deterministic Rule Engine**: Prioritized rules (App Category Overrides > App Mode > Global Category > Fallback).
- **Anti-Spam Event Coordinator**: Configurable cooldown debouncing (default 2.5s) and simultaneous sound priority handling.
- **Jetpack Compose M3 Interface**: Main status screen, per-app controls, debug diagnostics dashboard, and settings screen.
- **DataStore Settings**: Preferences persistence using Jetpack DataStore.
- **Foreground Service**: `PlaybackMonitorService` complying with Android 14–17 background audio hardening rules.
- **Unit Test Suite**: Complete test coverage for `RuleEngine`, `PlaybackClassifier`, and `EventCoordinator`.
