'use strict';
const fs = require('node:fs');
const {execFileSync} = require('node:child_process');
const assert = require('node:assert/strict');

const paths = ['package.json', 'package-lock.json', 'SOURCE_VERSION.json',
  'android/wear/SOURCE_VERSION.json', 'android/app/build.gradle',
  'android/wear/build.gradle', 'www/app.js'];
function inspect(read) {
  const [pkg, lock, phone, wear] = paths.slice(0, 4).map(p => JSON.parse(read(p)));
  const version = pkg.version;
  assert.match(version, /^(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)(?:-[0-9A-Za-z]+(?:[.-][0-9A-Za-z]+)*)?$/, 'Invalid version');
  for (const value of [lock.version, lock.packages[''].version, phone.version,
    phone.displayVersion, wear.version, wear.displayVersion]) {
    assert.equal(value, version, 'Package, lock and phone/Wear source versions must agree');
  }
  const code = phone.versionCode;
  assert.ok(Number.isSafeInteger(code) && code > 0 && code <= 2100000000, 'Invalid Android versionCode');
  assert.equal(wear.versionCode, code, 'Phone/Wear versionCode mismatch');
  for (const path of paths.slice(4, 6)) {
    const gradle = read(path);
    assert.equal(gradle.match(/\bversionName\s+"([^"]+)"/)[1], version, path);
    assert.equal(Number(gradle.match(/\bversionCode\s+(\d+)/)[1]), code, path);
  }
  assert.equal(read('www/app.js').match(/const TRAINPILOT_VERSION='([^']+)'/)[1], version, 'Visible app version mismatch');
  return {version, code};
}
function transition(before, after) {
  if (before.version === after.version) {
    assert.equal(after.code, before.code, 'A build-code change requires a coordinated version change');
    return;
  }
  const numeric = v => v.split('-')[0].split('.').map(Number);
  const a = numeric(before.version), b = numeric(after.version);
  const index = a.findIndex((n, i) => n !== b[i]);
  assert.ok(index >= 0 ? b[index] > a[index] : before.version.includes('-') && !after.version.includes('-'), 'Version must increase (prerelease transitions require explicit review)');
  assert.ok(after.code > before.code, 'Android versionCode must increase with version');
}
function checkTag(tag, current) {
  assert.ok([`v${current.version}`, `wear-v${current.version}`,
    `candidate-${current.version}-${current.code}`].includes(tag), 'Tag must match source version and platform');
}
if (require.main === module) {
  const current = inspect(p => fs.readFileSync(p, 'utf8'));
  const args = process.argv.slice(2);
  assert.ok(args.length === 0 || (args.length === 2 && args[0] === '--base'), 'Usage: node scripts/check-version-policy.cjs [--base SHA]');
  if (args.length) {
    assert.match(args[1], /^[0-9a-f]{40}$/, 'Base must be an exact commit SHA');
    transition(inspect(p => execFileSync('git', ['show', `${args[1]}:${p}`], {encoding: 'utf8', maxBuffer: 10 * 1024 * 1024})), current);
  }
  if (process.env.GITHUB_REF_TYPE === 'tag') checkTag(process.env.GITHUB_REF_NAME, current);
  console.log(`Version policy OK: phone + Wear ${current.version} /${current.code}`);
}
module.exports = {inspect, transition, checkTag};
