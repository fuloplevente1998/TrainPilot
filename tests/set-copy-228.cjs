'use strict';
const assert=require('node:assert/strict'),fs=require('node:fs');
const app=fs.readFileSync('www/app.js','utf8');
const canonical=fs.existsSync('www/v12.js')?fs.readFileSync('www/v12.js','utf8'):app;
const source=canonical.split('// @prefill228-start\n')[1].split('// @prefill228-end')[0];
let id=0;const api=new Function('window','crypto',source+'\nreturn window.TrainPilotPrefill228;')({}, {randomUUID:()=> 'fresh-'+(++id)});
const context={programId:'p',dayId:'A'},exercise={id:'press',loadType:'total',repUnit:'ism.'};
const row=(date,day='A',program='p',weight=12)=>({programId:program,dayId:day,started:date+'T10:00:00Z',finished:date+'T11:00:00Z',
 exercises:[{...exercise,sets:[{set:1,setId:'old',weight,reps:'10',done:true,actualRir:0,gpsRouteKey193:'private',completedAt:123},
 {set:2,weight:14,reps:'8',done:true},{set:3,weight:100,reps:'99',done:false}]}]});
const rows=[row('2020-01-01'),row('2020-01-08','B','p',90),row('2020-01-07','A','other',80),row('2020-01-05','A','p','12,5')];
const before=JSON.stringify(rows),build=(history=rows,e=exercise,opts={})=>api.build(context,e,4,5,history,opts);
const sets=build();
assert.deepEqual(sets.map(s=>[s.weight,s.reps,s.done]),[[12.5,'10',false],[14,'8',false],[5,'',false],[5,'',false]]);
assert.equal(sets.every(s=>!Object.hasOwn(s,'actualRir')&&!Object.hasOwn(s,'completedAt')&&!Object.hasOwn(s,'gpsRouteKey193')),true);
assert.equal(new Set(sets.map(s=>s.setId)).size,4);assert.equal(sets.some(s=>s.setId==='old'),false);
assert.equal(JSON.stringify(rows),before,'History must remain immutable');
assert.equal(api.untouched(sets[0]),true);sets[0].reps='7';assert.equal(api.untouched(sets[0]),false);
assert.equal(build().some(s=>sets.some(old=>old.setId===s.setId)),false,'New workout gets new identities');
assert.deepEqual(build([rows[1],rows[2]]).map(s=>s.reps),['','','',''],'No cross-day/program fallback');
const deleted=row('2020-01-09');deleted.deletedAt=123;
const draft=row('2020-01-10');delete draft.finished;
const future=row('2999-01-01');
assert.equal(api.latest([...rows,deleted,draft,future],context).started,'2020-01-05T10:00:00Z');
assert.equal(api.latest(rows,context,r=>r.started.startsWith('2020-01-05')).started,'2020-01-01T10:00:00Z');
const latest=row('2020-01-11');latest.exercises=[];assert.equal(build([...rows,latest])[0].reps,'','Missing exercise uses plan, not an older occurrence');
const duplicate=row('2020-01-11');duplicate.exercises.push(duplicate.exercises[0]);assert.equal(build([duplicate])[0].reps,'');
for(const old of [{weight:-1},{reps:'bad'},{reps:0},{weight:''},{leftSeconds:20},{distanceMeters:2}]){
 const invalid=row('2020-01-05');Object.assign(invalid.exercises[0].sets[0],old);
 assert.equal(build([invalid])[0].reps,'','Invalid or measured data must not be copied');
}
for(const modified of [{loadType:'per_hand'},{repUnit:'mp'},{measurementType:'distance'}]){
 assert.equal(build(rows,{...exercise,...modified})[0].reps,'');
}
assert.equal(api.build(context,exercise,2,5,rows).length,2,'Current plan count wins');
const watch=build(rows,exercise,{template:true});
assert.deepEqual(watch.map(s=>[s.weight,s.reps]),[[12.5,'10'],[14,'8'],[5,''],[5,'']]);
assert.equal(watch.every(s=>!s.setId&&!s.previousValues228),true,'Watch templates remain stable and carry values only');
assert.equal(app.includes('tp228-copy'),false,'No separate copy controls remain');
console.log('PASS #228 automatic same-program/day prefill: latest saved occurrence, clean identities, partial/invalid/missing data, no cross-day fallback, plan count, stable watch templates and no copy button');
