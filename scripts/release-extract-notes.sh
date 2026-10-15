#!/usr/bin/env bash
# Release notes extraction — which RELEASE_NOTES.md section becomes a published
# release's notes.
#
# The old workflow always took the TOP section ("latest entry"). That broke
# when a release went out before its notes entry landed: v1.64.1-preview.104
# shipped with the "# 1.64.0" notes because the "# 1.64.1" entry was added to
# RELEASE_NOTES.md only afterwards.
#
# This script takes a version (e.g. 1.64.1) and prints that version's section
# (header line included, up to the next "# " header) to stdout.
#
# Fail-closed toward publishing: if no section matches, it prints the top
# section (the old behavior) and warns on stderr — a notes-format hiccup must
# never block or corrupt a release.
#
# Usage: scripts/release-extract-notes.sh <version> [release-notes-path]
#   release-notes-path defaults to RELEASE_NOTES.md (repo-relative).
#
# Example dry-run:  scripts/release-extract-notes.sh 1.64.1
set -euo pipefail

VERSION="${1:?usage: release-extract-notes.sh <version> [release-notes-path]}"
NOTES_FILE="${2:-RELEASE_NOTES.md}"

# Print the "# <version>" section (header included) to stdout. Exits 0 with
# empty output when no section matches.
extract_section() {
  awk -v ver="$1" '
    /^#[[:space:]]/ {
      if (started) exit
      hdr = $0
      sub(/^#[[:space:]]+/, "", hdr)
      sub(/[[:space:]]+$/, "", hdr)
      if (hdr == ver) started = 1
    }
    started { print }
  ' "$NOTES_FILE"
}

# Print the top section (old behavior) to stdout.
top_section() {
  awk '/^#[[:space:]]/{if (++c > 1) exit} {print}' "$NOTES_FILE"
}

SECTION="$(extract_section "$VERSION")"
if [ -z "$SECTION" ]; then
  echo "warning: no '# $VERSION' section in $NOTES_FILE; using the top section" >&2
  SECTION="$(top_section)"
fi

printf '%s\n' "$SECTION"
