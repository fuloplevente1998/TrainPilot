const assert=require('node:assert/strict');
const fs=require('node:fs');
const html=fs.readFileSync('www/index.html','utf8');

assert.match(html,/TrainPilotActiveWorkoutSetControls/,'active-workout set controls marker missing');
assert.match(html,/tpActiveWorkoutAddSet/,'add-set action missing');
assert.match(html,/tpActiveWorkoutRemoveSet/,'remove-set action missing');
assert.match(html,/sets\.push\(fresh\)/,'new set is not appended to the running exercise');
assert.match(html,/sets\.splice\(targetIndex,1\)/,'set deletion is not wired to the running exercise');
assert.match(html,/persistDraft\(\)/,'active set edits must persist to the resumable draft');
assert.match(html,/sets\.length>=10/,'active set count must keep the existing 10-set upper bound');
assert.match(html,/sets\.length<=1/,'at least one set must remain');
assert.match(html,/leftSeconds/,'per-side timed sets must be supported');
assert.match(html,/tp164-per-side-row\.tp-active-set-editable/,'per-side row layout must remain compatible');
assert.match(html,/Sorozat hozzáadása/,'Hungarian add-set label missing');
assert.match(html,/Sorozat törlése/,'Hungarian remove-set label missing');

console.log('PASS: active workout supports adding/removing sets, persists drafts, and keeps per-side timed rows compatible.');
