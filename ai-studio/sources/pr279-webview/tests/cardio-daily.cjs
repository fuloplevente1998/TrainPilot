'use strict';
process.env.TZ='Europe/Budapest';
const assert=require('node:assert/strict'),{run}=require('./helpers/app-runtime.cjs')();
const plain=x=>JSON.parse(JSON.stringify(x)),now='2026-10-08T12:00:00+02:00';
run("db.set('history',[]);rf240Ledger=()=>({preferences:{provider:'samsung_health'},days:{'2026-10-08':{steps:6000,provider:'samsung_health'},'2026-10-07':{steps:0,provider:'samsung_health'}}})");
let model=plain(run("window.TrainPilotCardioDaily.model(new Date('"+now+"'))"));
assert.equal(model.todayRow.total,6000);assert.equal(model.todayRow.outside,6000,'No recorded workout means all measured steps are outside recorded workouts');
assert.equal(model.week.value,6000);assert.equal(model.week.measured,2,'Measured zero is a known day');
assert.equal(model.previous.value,null,'An unmeasured week must not turn into zero');
const workouts=[
 {id:'first',started:'2026-10-08T08:00:00+02:00',finished:'2026-10-08T09:00:00+02:00'},
 {id:'overlap',started:'2026-10-08T08:30:00+02:00',finished:'2026-10-08T10:00:00+02:00'},
 {id:'duplicate',started:'2026-10-08T08:00:00+02:00',finished:'2026-10-08T09:00:00+02:00'},
 {id:'night',started:'2026-10-07T23:30:00+02:00',finished:'2026-10-08T00:30:00+02:00'},
 {id:'future',started:'2026-10-08T13:00:00+02:00',finished:'2026-10-08T14:00:00+02:00'},
 {id:'invalid',started:'broken',finished:now},
 {id:'unfinished',started:'2026-10-08T11:00:00+02:00'}
];
run('db.set("history",'+JSON.stringify(workouts)+')');model=plain(run("window.TrainPilotCardioDaily.model(new Date('"+now+"'))"));
assert.equal(model.todayRow.outside,null,'Daily total alone cannot identify workout steps');
assert.deepEqual(model.todayRow.spans.map(([a,b])=>[new Date(a).toISOString(),new Date(b).toISOString()]),[
 ['2026-10-07T22:00:00.000Z','2026-10-07T22:30:00.000Z'],['2026-10-08T06:00:00.000Z','2026-10-08T08:00:00.000Z']]);
assert.equal(model.rows.find(r=>r.day==='2026-10-07').spans.length,1,'A workout crossing midnight is split by local day');
run("rf240Ledger=()=>({preferences:{provider:'samsung_health'},days:{'2026-10-08':{steps:6000,channel:'samsung_health',activityOrigin:'com.sec.android.app.shealth',stepsOrigin:'phone.origin',metricProviders:{steps:'health_connect'}}}})");
model=plain(run("window.TrainPilotCardioDaily.model(new Date('"+now+"'))"));assert.equal(model.todayRow.provider,'health_connect');assert.equal(model.todayRow.origin,'phone.origin','A fallback must retain its actual step origin');
run("db.set('history',[]);rf240Ledger=()=>({days:{'2026-10-08':{steps:null},'2026-10-07':{steps:-1},'2026-10-06':{steps:'123'}}})");
model=plain(run("window.TrainPilotCardioDaily.model(new Date('"+now+"'))"));assert.equal(model.week.value,null,'Null, negative or malformed counts must not become measurements');
run("rf240Ledger=()=>({days:{'2026-10-25':{steps:1000}}})");
model=plain(run("window.TrainPilotCardioDaily.model(new Date('2026-10-25T23:30:00+01:00'))"));
assert.equal(model.todayRow.end-model.todayRow.start,24.5*3600000,'Local days must preserve the autumn DST hour');
console.log('PASS daily cardio model: sources, null/zero, partial weeks, overlap union, midnight split, future/invalid exclusion and DST');
