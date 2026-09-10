# Privacy Policy for FixVol

**Last updated: September 10, 2026**

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

FixVol is an entirely offline Android system utility. It does not declare an `INTERNET` permission and never connects to remote servers. All functionality works without any network connectivity.

---

## 3. No Advertising or Tracking

FixVol contains no advertising SDKs, no analytics libraries, and no tracking code of any kind. There are no third-party dependencies that collect or transmit data.

---

## 4. Local On-Device Storage

FixVol stores user preferences (such as global audio category toggles, per-app rules, and cooldown settings) exclusively on your device using Android's Jetpack DataStore framework. This data never leaves your device.

---

## 5. Permissions

FixVol uses only the minimal set of standard system permissions required for operation:

- `FOREGROUND_SERVICE`: Used to keep playback event monitoring active when configured by the user.
- `FOREGROUND_SERVICE_SPECIAL_USE`: Declares the foreground service subtype required by Android 14+.
- `POST_NOTIFICATIONS`: Used exclusively to display the required, ongoing status notification when the foreground service is active.

---

## 6. Contact & Open Source Transparency

FixVol is 100% open source under the Apache License 2.0. The complete source code is publicly inspectable at [https://github.com/ziuus/FixVol](https://github.com/ziuus/FixVol).
