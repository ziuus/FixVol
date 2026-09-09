# FixVol

**The most user-friendly and privacy-respecting volume control for Android devices with broken, unreliable physical volume buttons.**
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
git clone https://github.com/ziuus/FixVol.git
cd FixVol

# Run unit tests
./gradlew test

# Build Debug APK
./gradlew assembleDebug
```

---

## 📲 Installing via Obtainium

[Obtainium](https://github.com/ImranR98/Obtainium) lets you install and auto-update FixVol directly from GitHub — no Play Store required.

1. Install Obtainium from its [GitHub Releases](https://github.com/ImranR98/Obtainium/releases).
2. Tap **Add App**.
3. Enter the source URL: `https://github.com/ziuus/FixVol`
4. Obtainium will detect the latest GitHub Release and download `FixVol-vX.Y.Z.apk` automatically.
5. Tap **Install** when prompted.

Future releases are downloaded and applied automatically in the background.

---

## 🔑 Setting Up Release Signing (for maintainers)

The release workflow requires a Java keystore uploaded as GitHub Secrets. **Without this, the release workflow will fail intentionally** — unsigned APKs cannot be installed on Android.

### 1 — Generate a keystore (one-time setup)

```bash
keytool -genkey -v \
  -keystore fixvol-release.jks \
  -keyalg RSA -keysize 4096 \
  -validity 10000 \
  -alias fixvol \
  -storepass YOUR_STORE_PASSWORD \
  -keypass YOUR_KEY_PASSWORD \
  -dname "CN=FixVol, O=FixVol, C=IN"
```

> **Keep `fixvol-release.jks` safe and backed up.** Losing it means you can never ship an update that installs over the existing app.

### 2 — Encode the keystore as base64

```bash
base64 -w 0 fixvol-release.jks > fixvol-release.jks.b64
cat fixvol-release.jks.b64   # copy this output
```

### 3 — Add GitHub Secrets

Go to **GitHub → Settings → Secrets and variables → Actions → New repository secret** and add:

| Secret name | Value |
|---|---|
| `KEYSTORE_BASE64` | The base64 string from step 2 |
| `KEYSTORE_PASSWORD` | Your `--storepass` value |
| `KEY_ALIAS` | `fixvol` |
| `KEY_PASSWORD` | Your `--keypass` value |

### 4 — Publish a new release

```bash
git tag v1.3.0
git push origin v1.3.0
```

The release workflow triggers automatically, builds a signed APK, and publishes a GitHub Release that Obtainium picks up.

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
