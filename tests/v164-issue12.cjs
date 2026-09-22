const assert=require('node:assert/strict'),fs=require('node:fs');
const split='www/trainpilot-164-issue12-side-set-fields.js';
const standalone=fs.existsSync(split);
const src=fs.readFileSync(standalone?split:'www/app.js','utf8');
if(!standalone)assert.ok(src.includes('// @section trainpilot-164-issue12-side-set-fields.js'),'missing #12 section marker');
for(const marker of [
 'tp164-side-set-fields',
 'tp164-side-set-input',
 'window.tp161SyncPerSideRow=window.tp164SyncPerSideRow',
 'separateSetSideFields:true',
 'noCompatibilityOverwrite:true',
 'bothSidesRequired:true',
 'coachPerSideTime:true',
 'coachPerSideStats:true',
 'timeBasedAdvice:true',
 'tp164PerSideStats',
 'tp164IsPerSideId'
])assert.ok(src.includes(marker),'missing #12 marker: '+marker);
const section=standalone?src:src.slice(src.indexOf('// @section trainpilot-164-issue12-side-set-fields.js'),src.indexOf('// @endsection trainpilot-164-issue12-side-set-fields.js'));
assert.match(section,/stopwatch\.left/);assert.match(section,/stopwatch\.right/);
assert.match(section,/check\.disabled=!set\.done&&!ready/);
assert.match(section,/tp1481SetSideSeconds\(setIndex,side,value\)/);
assert.doesNotMatch(section,/input\.value=set\.reps/);
console.log('PASS TrainPilot #12 bilateral lower set fields stay separate and completion-gated');
