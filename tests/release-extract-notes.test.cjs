'use strict';
// Tests for scripts/release-extract-notes.sh — the release notes are the
// RELEASE_NOTES.md section matching the released versionName, not blindly
// the top section (v1.64.1-preview.104 shipped with the "# 1.64.0" notes
// because the "# 1.64.1" entry landed after the release was published).
const { execFileSync, spawnSync } = require('node:child_process');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const test = require('node:test');
const assert = require('node:assert/strict');

const ROOT = path.join(__dirname, '..');
const SCRIPT = path.join(ROOT, 'scripts', 'release-extract-notes.sh');

function notesFile(content) {
  const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'release-extract-notes-'));
  const file = path.join(dir, 'RELEASE_NOTES.md');
  fs.writeFileSync(file, content);
  return file;
}

function extract(version, file) {
  return execFileSync('bash', [SCRIPT, version, file], {
    encoding: 'utf8',
    stdio: ['ignore', 'pipe', 'pipe'],
  });
}

const FIXTURE = `# 1.64.1

- Check for updates fix: the updater read the wrong endpoint.

# 1.64.0

- Encrypt remaining plaintext gaps.

# 1.63.0

- Localization + multi-currency.
`;

test('extracts the matching section when it is not the top one', () => {
  const out = extract('1.64.1', notesFile(FIXTURE));
  assert.ok(out.startsWith('# 1.64.1\n'), 'starts at the 1.64.1 header');
  assert.ok(out.includes('Check for updates fix'), 'contains the 1.64.1 body');
  assert.ok(!out.includes('1.64.0'), 'stops before the next section');
});

test('extracts the top section when the version is the top one', () => {
  const out = extract('1.64.0', notesFile(`# 1.64.0\n\n- Notes here.\n\n# 1.63.0\n\n- Older.\n`));
  assert.ok(out.startsWith('# 1.64.0\n'));
  assert.ok(!out.includes('1.63.0'));
});

test('falls back to the top section when the version section is missing', () => {
  const file = notesFile(FIXTURE);
  // spawnSync so we can inspect stderr on the success path too.
  const r = spawnSync('bash', [SCRIPT, '9.9.9', file], { encoding: 'utf8' });
  // The fallback must not fail the release: exit 0 even when unmatched.
  assert.equal(r.status, 0, 'script exits 0 for a missing section');
  assert.ok(r.stdout.startsWith('# 1.64.1\n'), 'falls back to the old top-section behavior');
  assert.match(r.stderr, /warning: no '# 9\.9\.9' section/);
});

test('header match tolerates trailing whitespace on the header line', () => {
  const out = extract('1.64.1', notesFile(`# 1.64.1   \n\n- Body.\n\n# 1.64.0\n\n- Older.\n`));
  assert.ok(out.startsWith('# 1.64.1'));
  assert.ok(out.includes('Body.'));
});

test('real RELEASE_NOTES.md: extracts the current versionName section', () => {
  // versionName is the single source of truth (see scripts/release-naming.sh).
  const naming = execFileSync('bash', [path.join(ROOT, 'scripts', 'release-naming.sh')], {
    encoding: 'utf8',
    cwd: ROOT,
  });
  const version = naming.split('\n').find((l) => l.startsWith('VERSION=')).slice('VERSION='.length);
  const out = execFileSync('bash', [SCRIPT, version], { encoding: 'utf8', cwd: ROOT });
  assert.ok(out.startsWith(`# ${version}\n`), `top of output is the # ${version} header`);
});
