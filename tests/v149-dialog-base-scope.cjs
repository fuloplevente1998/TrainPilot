const assert=require('node:assert/strict'),fs=require('node:fs');
const sectionPath='www/trainpilot-149-dialog-i18n.js';
const app=fs.existsSync(sectionPath)?fs.readFileSync(sectionPath,'utf8'):fs.readFileSync('www/app.js','utf8');
const start=app.indexOf('// @section trainpilot-149-dialog-i18n.js');
const end=app.indexOf('// @endsection trainpilot-149-dialog-i18n.js');
const code=start>=0&&end>start?app.slice(start,end):app;
assert.ok(code.includes('const TP149_DIALOG_BASES={'),'localized dialog base capture missing');
for(const key of ['startWorkout','resetPlan','activateProgram','deleteHistory','weightEdit','editSchedule','dayChoice','cancelSchedule']){
 assert.ok(code.includes(key+':typeof')||code.includes(key+':'),'missing captured base '+key);
}
const stale=[...code.matchAll(/\btp2629[A-Za-z0-9_]*Base\b/g)].map(m=>m[0]);
assert.deepEqual([...new Set(stale)],[],'localized dialog wrappers still reference inaccessible block-scoped 2629 base functions');
assert.ok(!code.includes('finishWorkout=function'),'localized partial confirmation belongs to the canonical finalizer, without a finish wrapper');
const runtime=fs.readFileSync('www/app.js','utf8');
assert.ok(runtime.includes("tp2628Confirm(tp149T('dialog.partial.message')"),'canonical finalizer keeps localized asynchronous partial-workout confirmation');
assert.ok(code.includes('TP149_DIALOG_BASES.startWorkout(dayId,scheduleId,programId)'),'start wrapper must call stable captured base');
console.log('PASS 1.4.9 localized dialog wrappers use stable captured base functions');
