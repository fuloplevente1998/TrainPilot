const assert=require('node:assert/strict'),fs=require('node:fs');
const app=fs.readFileSync('www/app.js','utf8');
for(const marker of ['tp2629Prompt','tp2629ConfirmBypass','TrainPilotDialogAudit','Biztonsági mentés visszatöltése','Google-fiók kapcsolása','Részleges edzés mentése','Testsúly szerkesztése','Edzés áthelyezése'])assert.ok(app.includes(marker),marker);
const gradle=fs.readFileSync('android/app/build.gradle','utf8');assert.match(gradle,/versionCode\s+2629/);assert.match(gradle,/versionName\s+"1\.4\.5"/);
const src=JSON.parse(fs.readFileSync('SOURCE_VERSION.json','utf8')),pkg=JSON.parse(fs.readFileSync('package.json','utf8'));assert.equal(src.version,'1.4.5');assert.equal(src.versionCode,2629);assert.equal(pkg.version,'1.4.5');
console.log('PASS 2629 themed active-dialog audit + 1.4.5 metadata');
