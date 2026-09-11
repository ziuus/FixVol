#!/usr/bin/env bash
set -euo pipefail

CHANGELOG="CHANGELOG.md"

if [ ! -f "$CHANGELOG" ]; then
    echo "# Changelog" > "$CHANGELOG"
    echo "" >> "$CHANGELOG"
fi

head_lines=$(grep -n -m1 '^# ' "$CHANGELOG" | cut -d: -f1 || true)
if [ -z "$head_lines" ]; then
    echo "# Changelog" > "$CHANGELOG"
    echo "" >> "$CHANGELOG"
    head_lines=1
fi

prefix=$(sed -n "1,$((head_lines-1))p" "$CHANGELOG")
body=$(sed -n "$((head_lines+1)),\$p" "$CHANGELOG")
body=$(echo "$body" | sed '/./,$!d')

tmpfile=$(mktemp)
echo "# Changelog" > "$tmpfile"
echo "" >> "$tmpfile"
echo "## [1.5.0] - $(date +%Y-%m-%d)" >> "$tmpfile"
echo "" >> "$tmpfile"
echo "### Fixed" >> "$tmpfile"
echo "" >> "$tmpfile"
echo "- Fixed: Intelligent volume toggle now starts OFF on first install and is correctly activated when enabled (first-launch toggle bug fixed)" >> "$tmpfile"
echo "- Added: Broken-button quick controls — lock screen, screenshot, and per-category volume panel triggers" >> "$tmpfile"
echo "- Added: Splash screen with FixVol logo shown briefly on app launch" >> "$tmpfile"
echo "- Added: Debug diagnostics screen with live volume readout and system capability verifier" >> "$tmpfile"
echo "- Removed: Obtainium reference from debug config (privacy hardening)" >> "$tmpfile"
echo "" >> "$tmpfile"
echo "$body" >> "$tmpfile"

mv "$tmpfile" "$CHANGELOG"
echo "CHANGELOG.md updated successfully"
