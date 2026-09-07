# FixVol Architecture Documentation

FixVol is designed around a single core technical objective:

> Detect system-wide audio playback starting from another application and trigger the genuine Android SystemUI volume panel without changing the volume.

---

## Data & Event Flow

```text
AudioPlaybackConfiguration (Android System)
        │
        ▼
   PlaybackMonitor
   (Intersects Android AudioPlaybackCallback)
        │
        ▼
  PlaybackClassifier
  (Maps AudioAttributes to internal AudioCategory)
        │
        ▼
     AppResolver
     (Lazy package metadata resolution & caching)
        │
        ▼
     RuleEngine
     (Pure, deterministic decision logic)
        │
        ▼
  EventCoordinator
  (Debouncing, priority coalescing, anti-spam)
        │
        ▼
 NativeVolumeController
 (Calls adjustStreamVolume with FLAG_SHOW_UI & ADJUST_SAME)
        │
        ▼
 Android SystemUI Volume Panel
```

---

## Component Breakdown

### 1. `PlaybackMonitor`
- Registers `AudioManager.AudioPlaybackCallback` with system.
- Receives active `AudioPlaybackConfiguration` items.
- Resolves package identity (UID to package name) via `AppResolver`.
- Emits normalized `PlaybackEvent` flow.

### 2. `PlaybackClassifier`
- Maps Android `AudioAttributes` usage constants (`USAGE_MEDIA`, `USAGE_ALARM`, `USAGE_NOTIFICATION`, `USAGE_VOICE_COMMUNICATION`, etc.) into internal `AudioCategory` enum values.
- Safely handles unknown or non-standard usage values without throwing exceptions.

### 3. `AppResolver`
- Maintains an in-memory cache of application labels, package names, icons, and UIDs.
- Prevents expensive repeated package enumeration during audio event callbacks.

### 4. `RuleEngine`
- Pure, 100% deterministic decision engine.
- Evaluates rules in strict precedence order:
  1. Explicit App + Category override
  2. Explicit App Mode (`ALWAYS_SHOW` / `NEVER_SHOW`)
  3. Global Category Toggle (Media, Alarm, Ringtone, etc.)
  4. Global Default (`IGNORE`)
- Contains zero heuristics or non-deterministic behavior.

### 5. `EventCoordinator`
- Enforces anti-spam debouncing (default 2.5s cooldown).
- Handles event priority across simultaneous sounds (`VOICE_CALL` > `ALARM` > `RINGTONE` > `MEDIA` > `NOTIFICATION` > `SYSTEM`).
- Prevents UI flickering or repeated volume slider triggers for single audio sessions.

### 6. `NativeVolumeController`
- The only component responsible for invoking Android's SystemUI volume panel.
- Uses `AudioManager.adjustStreamVolume(streamType, AudioManager.ADJUST_SAME, AudioManager.FLAG_SHOW_UI)`.
- Does NOT raise, lower, or mute volume.
- Includes automated test verification method (`testNativeVolumePanel`) to verify volume levels before and after request remain equal.

### 7. `PlaybackMonitorService` & `MonitoringController`
- Lightweight Android Lifecycle Foreground Service.
- Complies with Android 14+ / Android 17 background audio hardening requirements.
- Uses minimal notification overhead with zero promotional material.
