const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { test } = require('node:test');
const { validateWearMetadata } = require('./release-metadata.cjs');

const root = path.resolve(__dirname, '../../..');
const source = { version: '2.0.3', displayVersion: '2.0.3', versionCode: 3005 };
const gradle = 'versionCode 3005\nversionName "2.0.3"';

test('Wear versions validate without any phone metadata', () => {
  assert.deepEqual(validateWearMetadata(source, gradle), {
    versionName: '2.0.3', versionCode: 3005, tag: 'wear-v2.0.3'
  });
});

test('Wear rejects mismatching native versions and codes', () => {
  assert.throws(() => validateWearMetadata(source, gradle.replace('2.0.3', '2.0.2')), /version mismatch/);
  assert.throws(() => validateWearMetadata(source, gradle.replace('3005', '3004')), /versionCode mismatch/);
});

test('Wear accepts its own release tag and rejects a phone tag', () => {
  assert.equal(validateWearMetadata(source, gradle, 'tag', 'wear-v2.0.3').tag, 'wear-v2.0.3');
  assert.throws(() => validateWearMetadata(source, gradle, 'tag', 'v2.0.3'), /tag mismatch/);
});

test('Wear rejects invalid version metadata', () => {
  assert.throws(() => validateWearMetadata({ ...source, version: '../2.0.3' }, gradle), /Invalid Wear release version/);
  assert.throws(() => validateWearMetadata({ ...source, versionCode: 0 }, gradle), /Invalid Wear versionCode/);
  assert.throws(() => validateWearMetadata({ ...source, versionCode: '3005' }, gradle), /Invalid Wear versionCode/);
});

test('Wear prereleases retain an independent tag', () => {
  const prerelease = { ...source, version: '2.0.3-rc1', displayVersion: '2.0.3-rc1' };
  assert.equal(validateWearMetadata(prerelease, gradle.replace('2.0.3', prerelease.version)).tag, 'wear-v2.0.3-rc1');
});

test('each release gate builds and publishes only its own APK', () => {
  const wear = fs.readFileSync(path.join(root, '.github/workflows/wear-release.yml'), 'utf8');
  const phone = fs.readFileSync(path.join(root, '.github/workflows/build-apk.yml'), 'utf8');
  assert.match(wear, /:wear:testReleaseUnitTest :wear:assembleRelease/);
  assert.doesNotMatch(wear, /:app:assembleRelease|require\('\.\/package\.json'\)|require\('\.\/SOURCE_VERSION\.json'\)/);
  assert.match(wear, /TAG="wear-v\$VERSION"/);
  assert.match(wear, /--latest=false/);
  assert.match(phone, /:app:testReleaseUnitTest :app:assembleRelease/);
  assert.doesNotMatch(phone, /:wear:assembleRelease|wear-apk-badging|WEAR_APK|wearVersion|wearCode/);
  assert.match(phone, /'android\/wear\/\*\*'/);
  assert.match(phone, /'\.github\/workflows\/wear-release\.yml'/);
});
