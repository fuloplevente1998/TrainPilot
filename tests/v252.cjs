const fs=require('fs'),assert=require('node:assert/strict');
const s=fs.readFileSync('www/v252.js','utf8');
for(const marker of [
 "RF252_VERSION='2.5.2'",
 '--tp-primary:var(--accent)',
 '.btn.tp-primary',
 '.btn.tp-emphasis',
 'rf252ButtonRole',
 'rf252ApplyButtonHierarchy',
 'RF252_SELECTION_ACTION',
 'RF252_PRIMARY_ACTION',
 'RF252_EMPHASIS_ACTION',
 'Minden támogatott Health-adat szinkronizálása',
 'Testsúly napló és grafikon',
 'rf244UnifiedSync',
 'startWorkout',
 'rf151SaveHistory\\(false\\)',
 "makeBackup=function(){return {...rf252Backup(),appVersion:RF252_VERSION}}"
])assert.ok(s.includes(marker),'Missing '+marker);
assert.ok(s.indexOf('danger')<s.indexOf("return 'control'"),'Danger classification must win before control/primary');
assert.ok(s.includes("if(!el.classList.contains('secondary'))return 'primary'"),'Existing historical primary buttons must stay primary');
const prepPath='../scripts/prepare-build-252.py';
const prep=fs.existsSync(prepPath)?fs.readFileSync(prepPath,'utf8'):'';
if(prep)assert.ok(prep.includes('versionCode 2502')&&prep.includes('versionName "2.5.2"'));
console.log('PASS 2.5.2: app-wide primary/emphasis/secondary/danger button hierarchy.');
