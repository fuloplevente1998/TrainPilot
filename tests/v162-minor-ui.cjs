const assert=require('node:assert/strict'),fs=require('node:fs');
const app=fs.readFileSync(fs.existsSync('www/trainpilot-162-minor-ui.js')?'www/trainpilot-162-minor-ui.js':'www/app.js','utf8');
const pkg=require('../package.json'),src=require('../SOURCE_VERSION.json'),gradle=fs.readFileSync('android/app/build.gradle','utf8');
assert.equal(pkg.version,'1.6.2');assert.equal(src.version,'1.6.2');assert.equal(src.versionCode,2651);
assert.match(gradle,/versionCode\s+2651/);assert.match(gradle,/versionName\s+"1\.6\.2"/);
for(const marker of ["window.TrainPilot162={version:TP162_VERSION","window.tp162VisibleHealthSessions","String(s?.source||'')!==ownPackage",".tp-brand-strip{display:none!important}","main.rf221-home>.onboarding",'[data-panel="exercises"] .tp155-exercise-panel>.hero','[data-panel="calendar"] .cal-cell{min-height:42px',"host?.dataset?.panel==='exercises'","top:14px!important;right:14px!important"])assert.ok(app.includes(marker),'missing 1.6.2 marker: '+marker);
console.log('PASS TrainPilot 1.6.2 minor UI / stale Health-count guards');
