const fs = require('node:fs');
const path = require('node:path');

function validateWearMetadata(source, gradle, refType, refName) {
  if (!/^\d+\.\d+\.\d+(?:-[0-9A-Za-z.-]+)?$/.test(source.version)) {
    throw new Error('Invalid Wear release version');
  }
  if (!Number.isSafeInteger(source.versionCode) || source.versionCode <= 0) {
    throw new Error('Invalid Wear versionCode');
  }
  const versionName = gradle.match(/versionName\s+"([^"]+)"/)?.[1];
  const versionCode = Number(gradle.match(/versionCode\s+(\d+)/)?.[1]);
  if (versionName !== (source.displayVersion || source.version)) {
    throw new Error('Wear version mismatch');
  }
  if (versionCode !== source.versionCode) throw new Error('Wear versionCode mismatch');
  const tag = `wear-v${source.version}`;
  if (refType === 'tag' && refName !== tag) throw new Error(`Wear tag mismatch: expected ${tag}`);
  return { versionName, versionCode, tag };
}

if (require.main === module) {
  const root = path.resolve(__dirname, '..');
  const source = JSON.parse(fs.readFileSync(path.join(root, 'SOURCE_VERSION.json'), 'utf8'));
  const gradle = fs.readFileSync(path.join(root, 'build.gradle'), 'utf8');
  const result = validateWearMetadata(source, gradle, process.env.GITHUB_REF_TYPE, process.env.GITHUB_REF_NAME);
  console.log(`Wear metadata OK: ${result.versionName} (${result.versionCode}), ${result.tag}`);
}

module.exports = { validateWearMetadata };
