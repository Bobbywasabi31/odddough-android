#!/usr/bin/env bash
# Release publish decision — keeps the GitHub releases page short.
#
# Rule: publish a preview release only when no published (non-draft) release
# tag for this versionName exists yet, i.e. no v<VERSION>-preview.<n> tag.
# Docs/test-only pushes on an already-released version skip the publish step
# (CI still builds and tests everything), so the releases page stays at one
# entry per app version with the latest APK right at the top.
#
# Fails closed toward publishing: a broken tag lookup prints "publish" so a
# real version release is never silently suppressed.
#
# Usage: scripts/release-should-publish.sh <version> [--tags <file>]
#   --tags feeds tag names (one per line) from a file instead of querying
#   `gh release list` — used by tests and local dry-runs.
#   Prints "publish" or "skip" to stdout. Exit 0 on a decision, 1 on bad args.
set -euo pipefail

VERSION="${1:?usage: release-should-publish.sh <version> [--tags <file>]}"
TAGS_FILE=""
if [[ "${2:-}" == "--tags" ]]; then
  TAGS_FILE="${3:?missing tags file after --tags}"
fi

list_tags() {
  if [[ -n "$TAGS_FILE" ]]; then
    cat "$TAGS_FILE"
  else
    gh release list --exclude-drafts --limit 200 --json tagName --jq '.[].tagName'
  fi
}

# A failed lookup must not suppress a real release — treat it as no tags seen.
TAGS="$(list_tags 2>/dev/null || true)"
if [[ -n "$TAGS" ]] && printf '%s\n' "$TAGS" | grep -qx "v${VERSION}-preview\.[0-9][0-9]*"; then
  echo "skip"
else
  echo "publish"
fi
