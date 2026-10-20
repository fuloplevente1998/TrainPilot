'use strict';
const fs = require('node:fs');
const assert = require('node:assert/strict');
const {createHash} = require('node:crypto');
const {execFileSync} = require('node:child_process');
const REPO = 'fuloplevente1998/TrainPilot';
const START = '<!-- trainpilot:release-metadata:start -->';
const END = '<!-- trainpilot:release-metadata:end -->';
const sha = text => createHash('sha256').update(text).digest('hex');
const fields = ['id', 'tag_name', 'name', 'draft', 'prerelease', 'published_at', 'target_commitish'];
const assetFields = ['id', 'name', 'size', 'digest', 'browser_download_url'];
const pick = (value, keys) => Object.fromEntries(keys.map(k => [k, value[k] ?? null]));
const assets = release => release.assets.map(a => pick(a, assetFields)).sort((a, b) => a.id - b.id);
function originalBody(body) {
  body = body || '';
  if (!body.startsWith(START)) return body;
  const stop = body.indexOf(`${END}\n\n`);
  assert.ok(stop >= START.length, 'Malformed managed release header');
  return body.slice(stop + END.length + 2);
}
function bodyFor(entry, original, commit) {
  assert.match(commit, /^[0-9a-f]{40}$/);
  const current = ['v1.3.6', 'wear-v1.3.6'].includes(entry.tag_name);
  const status = entry.prerelease ? 'Történeti előzetes / jelölt' : current ? 'Legutóbb közzétett APK dátum szerint (2026-10-20-i állapot)' : 'Történeti kiadás';
  return `${START}\n**${entry.platform} · ${entry.tag_name} · ${status}**\n\n` +
    `Közzétéve: ${entry.published_at.slice(0, 10)} (UTC). Tagforrás: \`${entry.tagCommit}\`.\n\n` +
    `[Kiadási jegyzék és a kiadott APK/main különbsége](https://github.com/${REPO}/blob/${commit}/docs/releases/INDEX.md). ` +
    (current ? 'Ez az október 19-i APK nem tartalmazza a későbbi #222 Samsung/RIR-javításait. ' : '') +
    `Az alábbi eredeti kiadási feljegyzés az akkori buildre vonatkozik; a mellékletek és a tag változatlanok.\n${END}\n\n${original}`;
}
function validateCatalog(catalog) {
  assert.equal(catalog.schemaVersion, 1);
  assert.equal(catalog.repository, REPO);
  assert.ok(catalog.releases.length > 0);
  assert.equal(new Set(catalog.releases.map(r => r.id)).size, catalog.releases.length);
  assert.equal(new Set(catalog.releases.map(r => r.tag_name)).size, catalog.releases.length);
  for (const r of catalog.releases) {
    assert.ok(Number.isSafeInteger(r.id) && r.id > 0 && !r.draft);
    assert.match(r.tag_name, /^(?:wear-v|v|candidate-)\d+\.\d+\.\d+(?:-[a-zA-Z0-9.-]+)?$/);
    assert.match(r.tagCommit, /^[0-9a-f]{40}$/);
    assert.match(r.originalBodySha256, /^[0-9a-f]{64}$/);
    assert.ok(['Phone', 'Wear OS'].includes(r.platform));
    assert.ok(!Number.isNaN(Date.parse(r.published_at)));
    assert.ok(Array.isArray(r.assets));
  }
}
function plan(catalog, live, commit) {
  validateCatalog(catalog);
  // Complete preflight before the first write. Unknown new releases are never touched.
  return catalog.releases.map(entry => {
    const release = live.find(r => r.id === entry.id);
    assert.ok(release, `Missing release ${entry.tag_name}`);
    assert.deepEqual(pick(release, fields), pick(entry, fields), `Release metadata drift: ${entry.tag_name}`);
    assert.deepEqual(assets(release), assets(entry), `Asset drift: ${entry.tag_name}`);
    const original = originalBody(release.body);
    assert.equal(sha(original), entry.originalBodySha256, `Original notes changed: ${entry.tag_name}`);
    const body = bodyFor(entry, original, commit);
    return {entry, release, body, changed: body !== release.body};
  });
}
function api(method, path, payload) {
  const args = ['api', '--method', method, `repos/${REPO}/${path}`];
  if (payload) args.push('--input', '-');
  return JSON.parse(execFileSync('gh', args, {encoding: 'utf8',
    input: payload ? JSON.stringify(payload) : undefined, maxBuffer: 16 * 1024 * 1024}));
}
function run() {
  const mode = process.argv[2];
  assert.ok(['--check', '--preview', '--apply'].includes(mode) && process.argv.length === 3,
    'Usage: node scripts/release-metadata.cjs --check|--preview|--apply');
  const catalog = JSON.parse(fs.readFileSync('docs/releases/github-release-index.json', 'utf8'));
  validateCatalog(catalog);
  if (mode === '--check') {
    console.log(`Release catalog OK: ${catalog.releases.length} existing releases; no remote writes`);
    return;
  }
  const commit = process.env.GITHUB_SHA || execFileSync('git', ['rev-parse', 'HEAD'], {encoding: 'utf8'}).trim();
  if (mode === '--apply') {
    assert.equal(process.env.GITHUB_REPOSITORY, REPO);
    assert.equal(process.env.TP_APPLY_RELEASE_METADATA, '1', 'Explicit metadata write flag required');
    assert.equal(execFileSync('git', ['rev-parse', 'HEAD'], {encoding: 'utf8'}).trim(), commit, 'Checkout source mismatch');
  }
  const live = catalog.releases.map(e => api('GET', `releases/${e.id}`));
  const changes = plan(catalog, live, commit);
  function verifyTags() {
    for (const {entry} of changes) {
      const ref = api('GET', `git/ref/tags/${encodeURIComponent(entry.tag_name)}`);
      assert.equal(ref.object.type, 'commit', 'Annotated tags require separate review');
      assert.equal(ref.object.sha, entry.tagCommit, `Tag moved: ${entry.tag_name}`);
    }
  }
  verifyTags();
  const report = [];
  for (const change of changes) {
    const {entry, body, changed} = change;
    if (changed && mode === '--apply') {
      // Recheck immediately before each write as well as during full preflight.
      const fresh = api('GET', `releases/${entry.id}`);
      plan({...catalog, releases: [entry]}, [fresh], commit);
      const updated = api('PATCH', `releases/${entry.id}`, {body});
      const checked = plan({...catalog, releases: [entry]}, [updated], commit)[0];
      assert.equal(checked.changed, false, 'Release body write did not persist');
    }
    report.push({tag: entry.tag_name, changed, applied: changed && mode === '--apply',
      originalBodySha256: entry.originalBodySha256, managedBodySha256: sha(body), assets: entry.assets.length});
  }
  if (mode === '--apply') verifyTags();
  fs.mkdirSync('ui-results', {recursive: true});
  fs.writeFileSync('ui-results/release-metadata-report.json', JSON.stringify({repository: REPO, source: commit, mode, releases: report}, null, 2) + '\n');
  console.log(`${mode}: ${changes.length} releases verified; ${changes.filter(c => c.changed).length} metadata headers ${mode === '--apply' ? 'updated' : 'planned'}. Tags/assets unchanged.`);
}
if (require.main === module) run();
module.exports = {originalBody, bodyFor, plan, validateCatalog, sha};
