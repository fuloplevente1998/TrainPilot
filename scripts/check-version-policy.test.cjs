'use strict';
const {test} = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const {inspect, transition, checkTag} = require('./check-version-policy.cjs');
const read = p => fs.readFileSync(p, 'utf8');
test('repository metadata agrees, including visible app and both Android modules', () => {
  const result = inspect(read);
  assert.equal(result.version, JSON.parse(read('package.json')).version);
  assert.equal(result.code, JSON.parse(read('SOURCE_VERSION.json')).versionCode);
});
test('a stale lockfile or Wear build cannot pass', () => {
  for (const path of ['package-lock.json', 'android/wear/SOURCE_VERSION.json', 'android/wear/build.gradle', 'www/app.js']) {
    assert.throws(() => inspect(p => p === path ? read(p).replaceAll(JSON.parse(read('package.json')).version, '0.0.0') : read(p)));
  }
});
test('unchanged metadata is allowed; partial bumps and rollback are rejected', () => {
  const old = {version: '1.3.6', code: 2733};
  transition(old, {...old});
  transition(old, {version: '1.3.7', code: 2734});
  assert.throws(() => transition(old, {version: '1.3.7', code: 2733}));
  assert.throws(() => transition(old, {version: '1.3.6', code: 2734}));
  assert.throws(() => transition(old, {version: '1.3.5', code: 2734}));
  transition({version: '1.3.7', code: 2734}, {version: '1.3.7', code: 2734});
  assert.throws(() => transition({version: '1.3.7', code: 2734}, {version: '1.3.6', code: 2735}));
});
test('phone, Wear and candidate tags have explicit identities', () => {
  const current = {version: '1.3.71', code: 2735};
  for (const tag of ['v1.3.71', 'wear-v1.3.71', 'candidate-1.3.71-2735']) checkTag(tag, current);
  for (const tag of ['v1.3.6', 'wear-v1.3.6', 'candidate-1.3.71-2733']) assert.throws(() => checkTag(tag, current));
});

test('approved 1.3.71 release increases both version and Android code', () => {
  transition({version: '1.3.7', code: 2734}, {version: '1.3.71', code: 2735});
  assert.throws(() => transition({version: '1.3.7', code: 2734}, {version: '1.3.71', code: 2734}));
});
