# Privacy Policy for FixVol

**Last updated: September 7, 2026**

FixVol is designed with privacy as a foundational principle.

---

## 1. No Data Collection

FixVol **does not collect, store, or transmit any personal data**.

Specifically:
- No personal identification information
- No IP addresses or location data
- No audio content recordings
- No microphone data
- No app usage statistics or telemetry
- No crash logs sent to external servers

---

## 2. No Network Access

FixVol is an entirely offline Android system utility. It does not declare internet network permissions and never connects to remote servers.

---

## 3. Local On-Device Storage

FixVol stores user preferences (such as global audio category toggles, per-app rules, and cooldown settings) exclusively on your device using Android's Jetpack DataStore framework. This data never leaves your device.

---

## 4. Permissions

FixVol uses only the minimal set of standard system permissions required for operation:
- `FOREGROUND_SERVICE`: Used to keep playback event monitoring active when configured by the user.
- `POST_NOTIFICATIONS`: Used exclusively to display the required, ongoing status notification when the foreground service is active.

---

## 5. Contact & Open Source Transparency

FixVol is 100% open source under the Apache License 2.0. The complete source code is publicly inspectable at [https://github.com/fixvol/fixvol](https://github.com/fixvol/fixvol).
