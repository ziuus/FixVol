#!/usr/bin/env bash
set -euo pipefail

CHANGELOG="CHANGELOG.md"

# Extract the first H1 and any following lines until the next H1 or EOF,
# then prepend a new v1.5.0 section title above them.
if [ ! -f "$CHANGELOG" ]; then
    echo "# Changelog" > "$CHANGELOG"
    echo "" >> "$CHANGELOG"
fi

# Read existing content, keep from the first real heading onward
head_lines=$(grep -n -m1 '^# ' "$CHANGELOG" | cut -d: -f1 || true)
if [ -z "$head_lines" ]; then
    echo "# Changelog" > "$CHANGELOG"
    echo "" >> "$CHANGELOG"
    head_lines=1
fi

# Throw away everything before the first H1, keep the rest
prefix=$(sed -n "1,$((head_lines-1))p" "$CHANGELOG")
body=$(sed -n "$((head_lines+1)),\$p" "$CHANGELOG")

# Remove leading blank lines
body=$(echo "$body" | sed '/./,$!d')

tmpfile=$(mktemp)
echo "# Changelog" > "$tmpfile"
echo "" >> "$tmpfile"
echo "## v1.5.0 — $(date +%Y-%m-%d)" >> "$tmpfile"
echo "" >> "$tmpfile"
echo "- Fixed: Intelligent volume toggle now starts OFF on first install and is correctly activated when enabled (first-launch toggle bug fixed)" >> "$tmpfile"
echo "- Added: Broken-button quick controls — lock screen, screenshot, and per-category volume panel triggers" >> "$tmpfile"
echo "- Added: Splash screen with FixVol logo shown briefly on app launch" >> "$tmpfile"
echo "" >> "$tmpfile"
echo "Press any key to release now..."
read -rs -n1 -p "Press any key to continue with release build..."
echo "" >> "$tmpfile"
echo "" >> "$tmpfile"
echo "$body" >> "$tmpfile"

mv "$tmpfile" "$CHANGELOG"
echo "CHANGELOG.md updated successfully"
