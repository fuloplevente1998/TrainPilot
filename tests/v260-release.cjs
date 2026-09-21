const fs=require('fs');
const assert=require('assert');
const path=require('path');
const root=path.resolve(__dirname,'..');
const read=p=>fs.readFileSync(path.join(root,p),'utf8');
const json=p=>JSON.parse(read(p));

const pkg=json('package.json');
const src=json('SOURCE_VERSION.json');
const android=read('android/app/build.gradle');
const ui=read('www/v260.js');
const sw=read('www/sw.js');
const index=read('www/index.html');
const manifest=json('www/manifest.json');
const runtime=read('www/runtime-manifest.txt').split(/\r?\n/).map(x=>x.trim()).filter(Boolean);
const build=read('.github/workflows/build-apk.yml');
const validate=read('.github/workflows/validate-branch.yml');

// Final 2.6.2 patch metadata must be internally consistent.
assert.equal(pkg.version,'1.1.2');
assert.equal(src.version,'1.1.2');
assert.equal(src.versionCode,2609);
assert.match(android,/versionCode\s+2609/);
assert.match(android,/versionName\s+"1\.1\.2"/);
assert.match(ui,/RF260_UI_VERSION='2\.6\.2'/);

// User-facing install metadata must use the current product name.
assert.equal(manifest.name,'TrainPilot');
assert.equal(manifest.short_name,'TrainPilot');
assert.equal(json('capacitor.config.json').appName,'TrainPilot');

// Every runtime script loaded by index.html must be declared in runtime-manifest.txt and exist.
const scripts=[...index.matchAll(/<script(?:\s+defer)?\s+src="([^"]+)"/g)].map(m=>m[1]);
assert.ok(scripts.length>50,'expected full runtime script chain');
for(const file of scripts){
  assert.ok(runtime.includes(file),`runtime-manifest missing ${file}`);
  assert.ok(fs.existsSync(path.join(root,'www',file)),`missing runtime file ${file}`);
}
for(const file of runtime)assert.ok(fs.existsSync(path.join(root,'www',file)),`runtime-manifest points to missing ${file}`);
assert.match(sw,/trainpilot-v112/);
assert.match(sw,/runtime-manifest\.txt/);
assert.match(sw,/cache\.addAll\(\[\.\.\.new Set\(assets\)\]\)/);

// Feature branches validate only. Main is the only APK-producing branch.
assert.match(build,/branches:\s*\[ main \]/);
assert.match(build,/if:\s*github\.ref == 'refs\/heads\/main'/);
assert.doesNotMatch(build,/RepForge-v1\.4\.0\.apk/);
assert.match(build,/TrainPilot-\$VERSION\.apk/);
assert.match(build,/require\('\.\/package\.json'\)\.version/);
assert.match(build,/Verify release metadata consistency/);
assert.match(build,/Version mismatch: package=/);
assert.match(build,/versionCode mismatch:/);

assert.match(validate,/branches-ignore:\s*\[ main \]/);
assert.match(validate,/pull_request:[\s\S]*branches:\s*\[ main \]/);
assert.doesNotMatch(validate,/setup-java/);
assert.doesNotMatch(validate,/gradlew/);
assert.doesNotMatch(validate,/assembleDebug|assembleRelease/);
assert.doesNotMatch(validate,/upload-artifact/);

console.log('v260 release/CI guards OK');

