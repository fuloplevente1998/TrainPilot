const assert=require('node:assert/strict'),fs=require('node:fs');
const index=fs.readFileSync('www/index.html','utf8');
for(const marker of [
 "TP4_VERSION='phase4-coach-17-21-r1'",
 'allHistoryExercises:true',
 'allTargetExercises:true',
 'timedStats:true',
 'bilateralStats:true',
 'nextWorkoutSummary:true',
 'rf220ExerciseStats=function(id)',
 'rf220ProgressRows=function()',
 'rf233CoachPlan=function()',
 'tp4PolishCoachTarget',
 'tp4-next-summary'
])assert.ok(index.includes(marker),'missing Phase 4 marker: '+marker);
assert.doesNotMatch(index.slice(index.indexOf('// @section Phase 4')),/rows\.slice\(0,12\)/,'Phase 4 must not cap Coach/Statistics exercise history to 12 rows');
assert.match(index,/leftSeconds/,'bilateral history must be recognized');
assert.match(index,/rightSeconds/,'bilateral history must be recognized');
assert.match(index,/typeof v==='string'\?v:v\?\.id/,'Coach target exercises must normalize string/object exercise entries');
console.log('PASS Phase 4 static: #17 full Coach/stat coverage + #21 complete next-workout summary.');
