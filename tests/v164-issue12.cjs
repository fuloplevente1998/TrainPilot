const assert=require('node:assert/strict'),fs=require('node:fs');
const src=fs.readFileSync('www/app.js','utf8');
for(const marker of [
 '// @section trainpilot-164-issue12-side-set-fields.js',
 'tp164-side-set-fields',
 'tp164-side-set-input',
 'window.tp161SyncPerSideRow=window.tp164SyncPerSideRow',
 'separateSetSideFields:true',
 'noCompatibilityOverwrite:true',
 'bothSidesRequired:true'
])assert.ok(src.includes(marker),'missing #12 marker: '+marker);
const section=src.slice(src.indexOf('// @section trainpilot-164-issue12-side-set-fields.js'));
assert.match(section,/stopwatch\.left/);assert.match(section,/stopwatch\.right/);
assert.match(section,/check\.disabled=!set\.done&&!ready/);
assert.match(section,/tp1481SetSideSeconds\(setIndex,side,value\)/);
assert.doesNotMatch(section,/input\.value=set\.reps/);
console.log('PASS TrainPilot #12 bilateral lower set fields stay separate and completion-gated');
