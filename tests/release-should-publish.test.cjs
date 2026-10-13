'use strict';
// Tests for scripts/release-should-publish.sh — the release-page shortness gate.
// The workflow skips publishing a preview release when a published release tag
// already exists for the current versionName (docs/test-only pushes on the
// same version), so the releases page stays at one entry per app version.
const { execFileSync } = require('node:child_process');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const test = require('node:test');
const assert = require('node:assert/strict');

const ROOT = path.join(__dirname, '..');
const SCRIPT = path.join(ROOT, 'scripts', 'release-should-publish.sh');

function tagsFile(tags) {
  const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'release-should-publish-'));
  const file = path.join(dir, 'tags.txt');
  fs.writeFileSync(file, tags.join('\n') + (tags.length ? '\n' : ''));
  return file;
}

function decide(version, tags) {
  const out = execFileSync('bash', [SCRIPT, version, '--tags', tagsFile(tags)], {
    encoding: 'utf8',
    stdio: ['ignore', 'pipe', 'pipe'],
  });
  return out.trim();
}

test('publishes when no release exists for the version', () => {
  assert.equal(decide('1.65.0', ['v1.64.0-preview.101', 'v1.64.0-preview.100']), 'publish');
});

test('publishes when the tags list is empty', () => {
  assert.equal(decide('1.64.0', []), 'publish');
});

test('skips when a published release tag exists for the same version', () => {
  assert.equal(decide('1.64.0', ['v1.64.0-preview.101', 'v1.63.0-preview.99']), 'skip');
});

test('matches the full version, not a prefix (1.64 vs 1.64.0)', () => {
  assert.equal(decide('1.64', ['v1.64.0-preview.101']), 'publish');
  assert.equal(decide('1.64.0', ['v1.64-preview.5']), 'publish');
});

test('ignores malformed tags that do not match the preview scheme', () => {
  assert.equal(decide('1.64.0', ['v1.64.0-preview', 'v1.64.0-preview.abc', '1.64.0-preview.101']), 'publish');
});

test('requires the version argument', () => {
  assert.throws(() => execFileSync('bash', [SCRIPT], { stdio: ['ignore', 'pipe', 'pipe'] }));
});
