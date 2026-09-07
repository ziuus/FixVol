# Contributing to FixVol

Thank you for your interest in contributing to FixVol!

---

## Product Philosophy

Before submitting pull requests or proposing changes, please keep in mind our product philosophy:

> **Fix the volume-button problem without replacing Android.**

FixVol should:
- Never draw floating UI, bubbles, overlays, or persistent widgets.
- Never replace Android's volume slider with custom sliders.
- Never alter device volume levels.
- Never introduce tracking, analytics, or external network dependencies.
- Remain simple, deterministic, and lightweight.

---

## Development Setup

1. JDK 17+ and Android SDK 35/37 installed.
2. Clone the repository:
   ```bash
   git clone https://github.com/fixvol/fixvol.git
   cd fixvol
   ```
3. Run test suite:
   ```bash
   ./gradlew test
   ```

---

## Pull Request Guidelines

1. Ensure all existing unit tests pass before submitting.
2. Add tests for any new deterministic rule or classifier logic.
3. Keep code clean, modular, and compliant with Kotlin standard conventions.
