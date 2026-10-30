const fs=require('fs'),assert=require('node:assert/strict');
const s=fs.readFileSync('www/v251.js','utf8');
for(const marker of [
 "RF251_VERSION='2.5.1'",
 'overflow-y:auto!important',
 'height:auto!important',
 'Testsúly napló és grafikon',
 'rf251RepairHomeCoach',
 "r.score==null?'—':r.score+'/100'",
 'rf251SyncMessage',
 'rf251MissingPermissions',
 'rf240BackgroundSync=function(){}',
 "return stored&&RF200_THEMES?.[stored]?stored:'green'"
])assert.ok(s.includes(marker),'Missing '+marker);
assert.ok(!/\(r\.errors\|\|\[\]\)\.map\(e=>/.test(s),'Raw sync errors must not be rendered to users');
const prepPath='../scripts/prepare-build-251.py';
const prep=fs.existsSync(prepPath)?fs.readFileSync(prepPath,'utf8'):'';
if(prep)assert.ok(prep.includes('versionCode 2501')&&prep.includes('versionName "2.5.1"'));
console.log('PASS 2.5.1: responsive home, null-safe Coach, restored weight journal, green default and friendly Health report.');
