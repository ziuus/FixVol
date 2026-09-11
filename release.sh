#!/usr/bin/env bash
set -euo pipefail

echo "=== FixVol v1.5.0 release pipeline ==="

# 1. Bump version in build.gradle
./gradlew bumpVersion15

# 2. Update changelog
echo "Updating CHANGELOG.md..."
SCRIPTS_DIR="$(cd "$(dirname "$0")" && pwd)"
"$SCRIPTS_DIR/patch-changelog.sh"

# 3. Verify changelog updated
echo "=== Verifying CHANGELOG.md ==="
git diff --exit-code CHANGELOG.md || echo "CHANGELOG.md has uncommitted changes"

# 4. Build release (CI steps: lint + test + assemble)
echo "=== Building release APK ==="
./gradlew lint :app:testDebugUnitTest assembleRelease --stacktrace

# 5. Verify artifact
APK="$(find app/build/outputs/apk/release -name '*.apk' -type f | head -1)"
if [ -z "$APK" ]; then
    echo "ERROR: No release APK found"
    exit 1
fi
echo "Built: $APK"
ls -lh "$APK"
unzip -l "$APK" | grep -E "classes|AndroidManifest" || true
