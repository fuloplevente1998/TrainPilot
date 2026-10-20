'use strict';
const {test} = require('node:test');
const assert = require('node:assert/strict');
const {originalBody, bodyFor, plan, sha} = require('./release-metadata.cjs');
const body = '# Original\n\nHistoric notes, including trailing whitespace.  \n';
const commit = 'a'.repeat(40);
const release = {id: 1, tag_name: 'v1.3.6', name: 'Original title', draft: false, prerelease: false,
  published_at: '2026-10-19T09:57:44Z', target_commitish: 'main', body,
  assets: [{id: 2, name: 'original.apk', size: 123, digest: 'sha256:original', browser_download_url: 'https://example.invalid/original.apk'}]};
const entry = {...release, platform: 'Phone', tagCommit: 'b'.repeat(40), originalBodySha256: sha(body)};
delete entry.body;
const catalog = {schemaVersion: 1, repository: 'fuloplevente1998/TrainPilot', releases: [entry]};
test('managed header preserves original notes byte for byte and is idempotent', () => {
  const managed = bodyFor(entry, body, commit);
  assert.equal(originalBody(managed), body);
  assert.equal(plan(catalog, [{...release, body: managed}], commit)[0].changed, false);
  assert.equal(plan(catalog, [release], commit)[0].changed, true);
});
test('changed body, asset, status, title, missing or duplicate identity blocks preflight', () => {
  for (const changed of [{body: body + 'edit'}, {assets: []}, {prerelease: true}, {name: 'other'}, {tag_name: 'v1.3.5'}]) {
    assert.throws(() => plan(catalog, [{...release, ...changed}], commit));
  }
  assert.throws(() => plan(catalog, [], commit));
  assert.throws(() => plan({...catalog, releases: [entry, entry]}, [release], commit));
});
test('a partial earlier run is safely resumable and unknown releases remain outside the plan', () => {
  const managed = bodyFor(entry, body, 'c'.repeat(40));
  const changes = plan(catalog, [{...release, body: managed}, {...release, id: 99}], commit);
  assert.equal(changes.length, 1);
  assert.equal(originalBody(changes[0].body), body);
});
test('malformed managed header is rejected', () => {
  assert.throws(() => originalBody('<!-- trainpilot:release-metadata:start -->broken'));
});
