'use strict';
const assert=require('node:assert/strict'),fs=require('node:fs');
const app=fs.readFileSync('www/app.js','utf8');
const canonical=fs.existsSync('www/v12.js')?fs.readFileSync('www/v12.js','utf8'):app;
const source=canonical.split('// @prefill228-start\n')[1].split('// @prefill228-end')[0];
let id=0;const api=new Function('window','crypto',source+'\nreturn window.TrainPilotPrefill228;')({}, {randomUUID:()=> 'fresh-'+(++id)});
const exercise={id:'press',loadType:'total',repUnit:'ism.'},context={programId:'p',dayId:'A'};
const row=(date,day='A',program='p',weight=12)=>({programId:program,dayId:day,started:date+'T10:00:00Z',finished:date+'T11:00:00Z',
 exercises:[{...exercise,sets:[{set:1,setId:'old',weight,reps:'10',done:true,actualRir:0,gpsRouteKey193:'private',completedAt:123},
 {set:2,weight:14,reps:'8',done:true},{set:3,weight:100,reps:'99',done:false}]}]});
const rows=[row('2020-01-01'),row('2020-01-08','B','p',90),row('2020-01-07','A','other',80),row('2020-01-05','A','p','12,5')];
const before=JSON.stringify(rows),build=(items=rows,e=exercise,opts={})=>api.build(context,e,4,5,items,opts);
const sets=build();
assert.deepEqual(sets.map(s=>[s.weight,s.reps,s.done]),[[90,'10',false],[14,'8',false],[5,'',false],[5,'',false]],'Latest matching exercise across days and programs');
assert.equal(api.latest(rows,exercise).started,'2020-01-08T10:00:00Z');
assert.equal(sets.every(s=>!Object.hasOwn(s,'actualRir')&&!Object.hasOwn(s,'completedAt')&&!Object.hasOwn(s,'gpsRouteKey193')),true);
assert.equal(new Set(sets.map(s=>s.setId)).size,4);assert.equal(sets.some(s=>s.setId==='old'),false);
assert.equal(JSON.stringify(rows),before,'Journal is read-only');
assert.equal(api.untouched(sets[0]),true);sets[0].reps='7';assert.equal(api.untouched(sets[0]),false);
assert.equal(build().some(s=>sets.some(old=>old.setId===s.setId)),false,'New workout gets new IDs');
assert.equal(build([rows[2]])[0].weight,80,'Other-program history is eligible');
assert.equal(build([rows[1]])[0].weight,90,'Other-day history is eligible');
const deleted=row('2020-01-09');deleted.deletedAt=123;
const draft=row('2020-01-10');delete draft.finished;
const future=row('2999-01-01');
assert.equal(api.latest([...rows,deleted,draft,future],exercise).started,'2020-01-08T10:00:00Z');
assert.equal(api.latest(rows,exercise,r=>r.dayId==='B').started,'2020-01-07T10:00:00Z');
const latest=row('2020-01-11');latest.exercises=[];
assert.equal(build([...rows,latest])[0].weight,90,'Newest workout without exercise does not mask journal entry');
const duplicate=row('2020-01-11');duplicate.exercises.push(duplicate.exercises[0]);
assert.equal(build([...rows,duplicate])[0].weight,90,'Ambiguous repeats are excluded');
const partial=row('2020-01-12','quick',null,45);partial.exercises[0].sets[0].done=false;
assert.equal(build([...rows,partial])[0].weight,90,'Incomplete recent set falls back to last completed one');
assert.equal(build([...rows,partial])[1].weight,14);
for(const old of [{weight:-1},{reps:'bad'},{reps:0},{weight:''},{leftSeconds:20},{distanceMeters:2}]){
 const invalid=row('2020-01-12','quick',null,45);Object.assign(invalid.exercises[0].sets[0],old);
 assert.equal(build([...rows,invalid])[0].weight,90,'Invalid latest entry must not replace older valid data');
}
for(const modified of [{loadType:'per_hand'},{repUnit:'mp'},{measurementType:'distance'}]){
 assert.equal(build(rows,{...exercise,...modified})[0].reps,'','Never mix units and measurement types');
}
assert.equal(api.build(null,exercise,2,5,rows).length,2,'Current plan controls set count');
assert.equal(api.build(null,exercise,1,5,rows)[0].weight,90,'Quick workout with no program/day can prefill');
const watch=build(rows,exercise,{template:true});
assert.deepEqual(watch.map(s=>[s.weight,s.reps]),[[90,'10'],[14,'8'],[5,''],[5,'']]);
assert.equal(watch.every(s=>!s.setId&&!s.previousValues228),true,'Wear templates retain only suggestions');
// The compatibility runner splits app.js into independent section files.
const quickSource=fs.existsSync('www/trainpilot-150-quick-workout.js')?
 fs.readFileSync('www/trainpilot-150-quick-workout.js','utf8'):app;
const wearSource=fs.existsSync('www/wear-sync.js')?
 fs.readFileSync('www/wear-sync.js','utf8'):app;
assert.match(quickSource,/function tp150QuickExercise\(id\)\{[\s\S]{0,400}sets:tp228PrefillSets\(null,e,/);
assert.match(wearSource,/function watchQuickExercises\(lang\)\{[\s\S]{0,1000}sets:tp228PrefillSets\(null,e,/);
// Both phone Quick Workout configuration paths must preserve the journal
// model, and the optional user weight edit must not clear historical reps.
const unified=fs.existsSync('www/trainpilot-151-unified-design.js')?
 fs.readFileSync('www/trainpilot-151-unified-design.js','utf8'):app;
const inline=fs.existsSync('www/trainpilot-152-ux-hierarchy.js')?
 fs.readFileSync('www/trainpilot-152-ux-hierarchy.js','utf8'):app;
const helperSource=unified.split('var tp151QuickConfiguredSets=')[1]?.split('var tp151QuickConfig=')[0];
assert.ok(helperSource,'Quick Workout config helper must exist in v151 section');
const configured=new Function('tp228PrefillSets',
 'var tp151QuickConfiguredSets='+helperSource+'; return tp151QuickConfiguredSets;')(
 (_context,e,n)=>api.build(null,e,n,e.weight??0,rows)
);
const strengthExercise={...exercise,weight:5};
const originalQuick={sets:api.build(null,strengthExercise,3,5,rows)};
const prepared=configured(strengthExercise,originalQuick,4,'90');
assert.deepEqual(prepared.map(s=>[s.weight,s.reps,s.done]),[[90,'10',false],[14,'8',false],[5,'',false],[5,'',false]],
 'Unchanged single weight control keeps per-set journal weights/reps when adding a set');
const override=configured(strengthExercise,originalQuick,4,'22.5');
assert.deepEqual(override.map(s=>[s.weight,s.reps,s.done]),[[22.5,'10',false],[22.5,'8',false],[22.5,'',false],[22.5,'',false]],
 'Explicit weight change overrides only loads, never erases repetitions');
assert.equal(override.every(s=>s.done===false),true);
assert.equal(override.every(s=>!s.actualRir&&!s.completedAt),true);
const unchanged=configured(strengthExercise,originalQuick,2,null);
assert.deepEqual(unchanged.map(s=>[s.weight,s.reps]),[[90,'10'],[14,'8']],
 'Changing only set count and/or target may not clear journal values');
assert.ok(unified.includes('b.sets=tp151QuickConfiguredSets(e,b,n,raw)'),'Legacy quick form must preserve journal records');
assert.ok(inline.includes('base.sets=tp151QuickConfiguredSets(e,base,n,raw)'),'Inline quick form must preserve journal records');
assert.equal(app.includes('tp228-copy'),false,'No manual copy button');
console.log('PASS #228 Napló-based exercise-wide prefill: latest completed matching exercise, cross-day/program/quick, safe fallback and immutable journal, clean new sets, Wear/quick coverage');