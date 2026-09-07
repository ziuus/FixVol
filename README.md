# FixVol

**Native Android volume control for broken volume buttons.**

Play audio.
FixVol detects it.
Android's native volume panel appears.

---

> **Fix the volume-button problem without replacing Android.**

FixVol is a tiny, open-source Android utility that restores convenient volume control on phones whose physical volume buttons are broken, unreliable, inaccessible, or inconvenient.

FixVol should feel almost invisible.

* It does not replace Android's volume interface.
* It does not create floating controls or bubbles.
* It does not create its own volume slider or overlay.
* It uses the **real Android SystemUI volume controller**.

---

## ⚡ How It Works

```text
Spotify / YouTube / Media starts playing
        ↓
FixVol detects playback via AudioPlaybackCallback
        ↓
Identify package & audio usage category
        ↓
Evaluate deterministic user rules
        ↓
Request Android native volume UI (adjustStreamVolume)
        ↓
Android SystemUI volume panel appears
```

The actual volume level remains 100% unchanged.

---

## ✨ Features

- **Genuine SystemUI Controller**: Triggers Android's native volume slider without altering volume levels.
- **Deterministic Rule Engine**: Custom rules per audio type (Media, Alarm, Ringtone, Calls) and per application.
- **Smart Debouncing**: Prevents repetitive volume UI triggers on audio state changes.
- **100% Offline & Private**: Zero analytics, zero cloud services, zero remote configuration, zero tracking.
- **Material 3 Design**: Built using Kotlin, Jetpack Compose, and DataStore.
- **Zero Heavy Permissions**: No accessibility hacks, no screen overlay permissions, no microphone access.

---

## ⚠️ Important Operating & Device Notes

> **Warning**: Android restricts background audio interactions differently across OS versions and device manufacturers. Some devices may require additional configuration or may expose less playback information than others.

### Supported Android Versions Matrix

| Android Version | Playback Monitoring | Native UI Trigger | Background Service |
| :--- | :--- | :--- | :--- |
| **Android 10 – 13** | Fully Supported | Fully Supported | Optional |
| **Android 14 – 15** | Fully Supported | Fully Supported | Required (Foreground Service) |
| **Android 16 – 17** | Fully Supported | Hardening Compliant | Required |

---

## 🏗️ Architecture

Read the complete technical architecture guide in [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md).

```text
PlaybackMonitor (AudioPlaybackCallback)
        ↓
PlaybackClassifier
        ↓
AppResolver (In-Memory Cache)
        ↓
RuleEngine (Deterministic Rules)
        ↓
EventCoordinator (Debouncing & Prioritization)
        ↓
NativeVolumeController (SystemUI adjustStreamVolume)
        ↓
Android SystemUI
```

---

## 🛠️ Building from Source

### Prerequisites
- JDK 17
- Android SDK 35 / 37

### Commands
```bash
# Clone the repository
git clone https://github.com/fixvol/fixvol.git
cd fixvol

# Run unit tests
./gradlew test

# Build Debug APK
./gradlew assembleDebug
```

---

## 🔒 Privacy Policy

FixVol:
- Does not collect personal data.
- Does not send data to any server.
- Does not require an account.
- Does not use advertising.
- Does not use analytics or crash reporting.
- Does not record microphone audio or inspect media contents.
- Stores user settings locally on-device using Jetpack DataStore.

Read the full [`PRIVACY.md`](PRIVACY.md).

---

## 📄 License

FixVol is open-source software licensed under the [Apache License 2.0](LICENSE).
