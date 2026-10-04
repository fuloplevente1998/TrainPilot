'use strict';

const fs = require('fs');
const path = require('path');

const root = path.resolve(__dirname, '..');
const pkg = JSON.parse(fs.readFileSync(path.join(root, 'package.json'), 'utf8'));
const source = JSON.parse(fs.readFileSync(path.join(root, 'SOURCE_VERSION.json'), 'utf8'));
const gradle = fs.readFileSync(path.join(root, 'android/app/build.gradle'), 'utf8');

const versionName = gradle.match(/versionName\s+"([^"]+)"/)?.[1];
const versionCode = Number(gradle.match(/versionCode\s+(\d+)/)?.[1]);
const version = pkg.version;
const displayVersion = source.displayVersion || source.version;

// TrainPilot intentionally accepts only stable SemVer or numbered alpha/beta/rc builds.
// Examples: 1.2.7, 1.3.0-rc.1
const allowed = /^(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)(?:-(?:alpha|beta|rc)\.(0|[1-9]\d*))?$/;
if (!allowed.test(version)) {
  throw new Error(`Invalid TrainPilot SemVer: ${version}. Use X.Y.Z or X.Y.Z-(alpha|beta|rc).N`);
}
if (source.version !== version || displayVersion !== version || versionName !== version) {
  throw new Error(`Version mismatch: package=${version}, source=${source.version}, display=${displayVersion}, Android=${versionName}`);
}
if (!Number.isInteger(source.versionCode) || source.versionCode <= 0 || versionCode !== source.versionCode) {
  throw new Error(`versionCode mismatch: source=${source.versionCode}, Android=${versionCode}`);
}

const prerelease = version.includes('-');
if (process.env.GITHUB_REF === 'refs/heads/main' && prerelease) {
  throw new Error('Prerelease versions must never be merged to main. Promote to a stable X.Y.Z first.');
}
if (process.env.GITHUB_REF_TYPE === 'tag') {
  const expected = `v${version}`;
  if (process.env.GITHUB_REF_NAME !== expected) {
    throw new Error(`Tag/version mismatch: expected ${expected}, got ${process.env.GITHUB_REF_NAME}`);
  }
}

console.log(`Version policy OK: ${version} / ${versionCode}${prerelease ? ' (prerelease)' : ' (stable)'}`);
