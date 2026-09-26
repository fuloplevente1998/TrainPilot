/* #60: complete workouts, comparable PRs, accurate weighted volume and group shares. */
'use strict';
const assert=require('node:assert/strict'),fs=require('node:fs'),vm=require('node:vm');
const split='www/trainpilot-177-progress-model.js';
let code='';
if(fs.existsSync(split))code=fs.readFileSync(split,'utf8');
else{
 const source=fs.readFileSync('www/app.js','utf8');
 const hit=source.match(/\/\/ @section trainpilot-177-progress-model\.js\n([\s\S]*?)\/\/ @endsection trainpilot-177-progress-model\.js/);
 assert.ok(hit,'#60 progress model section missing from canonical app.js');
 code=hit[1];
}
const box={module:{exports:{}},exports:{},window:{},console,Date};
const ctx=vm.createContext(box);
vm.runInContext(code,ctx,{filename:'trainpilot-177-progress-model.js'});
const m=box.module.exports;
const when=d=>d+'T16:00:00+02:00';
const workout=(id,day,exercises,extra={})=>({
 id,started:day+'T15:00:00+02:00',finished:when(day),exercises,...extra
});
const press=(kg,reps)=>({id:'press',repUnit:'ism.',sets:[{weight:kg,reps,done:true}]});
const plank=(left,right)=>({id:'side-plank',repUnit:'mp/oldal',
 sets:[{leftSeconds:left,rightSeconds:right,reps:String(Math.min(left,right)),done:true}]});
const raw=[
 workout('baseline','2026-09-15',[press(10,10),plank(30,30)]),
 workout('recent','2026-09-22',[press(12,10),plank(36,32)]),
 workout('draft','2026-09-23',[press(500,10)],{status:'draft'}),
 {id:'invalid',started:when('2026-09-24'),exercises:[press(999,9)]},
 workout('empty','2026-09-24',[{id:'press',sets:[{weight:800,reps:3,done:false}]}])
];
const prepared=m.prepare(raw,{groupMap:e=>e.id==='press'?['chest','triceps']:['core']});
assert.equal(prepared.length,3,'only dated, completed, non-draft Journal rows');
assert.equal(prepared[0].volume,100);
assert.equal(prepared[1].volume,120,'timed side sets must not masquerade as weight');
assert.equal(prepared[1].sets,2);
assert.equal(prepared[2].volume,0,'undone sets must not count');
assert.equal(prepared[2].sets,0);
const records=prepared[1].pr;
assert.ok(records.some(x=>x.kind==='weight'&&x.previous===10&&x.value===12));
assert.ok(records.some(x=>x.kind==='left'&&x.previous===30&&x.value===36));
assert.ok(records.some(x=>x.kind==='right'&&x.previous===30&&x.value===32));
assert.ok(records.every(x=>x.previous!==undefined),'no first-ever baseline should count as PR');
const makeDate=expr=>vm.runInContext(expr,ctx);
const today=makeDate('new Date(2026,8,25,12,0,0)');
const week=m.build(prepared,'7d',today);
assert.equal(week.currentMetrics.workouts,2);
assert.equal(week.previousMetrics.workouts,1);
assert.equal(week.currentMetrics.volume,120);
assert.equal(week.previousMetrics.volume,100);
assert.equal(week.currentMetrics.weightedCount,1);
assert.equal(week.currentMetrics.avgVolume,120);
assert.equal(week.changes.volume,20);
assert.equal(week.buckets.length,7);
assert.equal(week.buckets.reduce((s,b)=>s+b.volume,0),120);
assert.equal(week.currentMetrics.pr.length,3);
const chest=week.groups.find(x=>x.key==='chest'),arms=week.groups.find(x=>x.key==='arms'),core=week.groups.find(x=>x.key==='core');
assert.equal(chest.sets,.7);
assert.equal(arms.sets,.3);
assert.equal(core.sets,1);
assert.equal(core.percent,100,'group percentage is relative to the busiest group, not recovery');
assert.equal(week.currentMetrics.sets,2);
assert.equal(m.build(prepared,'30d',today).buckets.length,5);
assert.equal(m.build(prepared,'3m',today).buckets.length,13);
const fresh=m.build(prepared,'7d',makeDate('new Date(2027,1,1,9)'));
assert.equal(fresh.currentMetrics.workouts,0);
assert.equal(fresh.changes.volume,null,'no previous baseline is not +100%');
assert.ok(!JSON.stringify(fresh).includes('8.4'),'never invent a workout intensity score');
const deleted=m.prepare(raw,{isDeleted:w=>w.id==='recent'});
assert.ok(!deleted.some(x=>x.key==='recent'),'deleted workouts excluded');
const legacy=m.prepare([workout(undefined,'2026-09-20',[press(10,10)],{programId:'home-basic',dayId:'A'})]);
assert.equal(legacy[0].key,legacy[0].source.started+'|home-basic|A','legacy workout links use the same key as Journal');
console.log('PASS #60: 7d/30d/3m bars, completed-only data, weighted volume, PRs, bilateral time, relative muscle loads, no invented intensity.');
