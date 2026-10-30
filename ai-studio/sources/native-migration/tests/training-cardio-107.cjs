'use strict';
process.env.TZ='Europe/Budapest';
const assert=require('node:assert/strict');
const {run}=require('./helpers/app-runtime.cjs')();
const plain=x=>JSON.parse(JSON.stringify(x));
const workout=(id,started,activity,sets)=>({id,started,finished:started,exercises:[{id:activity,measurementType:'distance',loadType:'bodyweight',repUnit:'mp',sets}]});
const fixtures=[
 workout('fast','2026-10-01T10:00:00+02:00','running',[{reps:'1800',distanceMeters:5000,done:true}]),
 workout('slow','2026-10-02T10:00:00+02:00','running',[{reps:'2100',distanceMeters:5000,done:true},{reps:'300',distanceMeters:1000,done:false}]),
 workout('bike','2026-10-02T11:00:00+02:00','cycling',[{reps:'3600',distanceMeters:20000,done:true}]),
 workout('indoor','2026-10-02T12:00:00+02:00','running',[{reps:'600',done:true},{reps:'300',distanceMeters:0,done:true}]),
 workout('distance-only','2026-09-28T00:00:00+02:00','running',[{reps:'',distanceMeters:1000,done:true}]),
 workout('last-week','2026-09-27T23:59:59+02:00','walking',[{reps:'600',distanceMeters:1000,done:true}]),
 workout('future','2026-10-04T10:00:00+02:00','running',[{reps:'1800',distanceMeters:5000,done:true}]),
 workout('invalid','invalid','running',[{reps:'1800',distanceMeters:5000,done:true}]),
 workout('bad-numbers','2026-10-01T10:00:00+02:00','running',[{reps:'bad',distanceMeters:-100,done:true}])
];
run('var fixtures107='+JSON.stringify(fixtures));
const m=plain(run("tp107CardioModel(fixtures107,new Date('2026-10-03T12:00:00+02:00'))"));
assert.equal(m.week.meters,31000);assert.equal(m.previousWeek.meters,1000);assert.equal(m.totals.timeOnlySeconds,900);
assert.equal(m.totals.pace,8100/31,'pace must use only paired time/distance, weighted by distance');
const record=m.records.find(x=>x.id==='running'&&x.meters===5000);assert.equal(record.best.seconds,1800);assert.equal(record.previous.seconds,2100);assert.equal(record.count,2);
assert.equal(m.records.filter(r=>r.id==='cycling').length,1,'different activities keep separate records');
assert.equal(run('tp107FormatPace(359.6)'),'6:00 /km','pace rounding must not yield 5:60');
const filtered=plain(run("tp107CardioModel(fixtures107,new Date('2026-10-03T12:00:00+02:00'),'running')"));assert.equal(filtered.week.meters,11000);
const before=Date.now;let now=Date.parse('2026-10-03T12:00:00+02:00');Date.now=()=>now;
try{
 run("state.session={started:'2026-10-03T10:00:00Z',workout:'quick',exercises:[{...byId('running'),sets:[{set:1,weight:0,reps:'',done:false},{set:2,weight:0,reps:'',done:false}]}]};state.current=0;tp107ToggleIndoor()");
 now+=62500;run('tp107StopIndoor()');assert.equal(run('state.session.exercises[0].sets[0].reps'),'62');assert.equal(run('state.session.exercises[0].sets[0].distanceMeters'),undefined);
 now+=30000;run('tp107ToggleIndoor()');now+=10000;run('tp107StopIndoor()');assert.equal(run('state.session.exercises[0].sets[0].reps'),'72','paused wall time excluded');
 run("tp107ToggleIndoor();state.session=null");now+=10000;run('tp107PaintIndoor()');assert.equal(run("db.get('draft').session.exercises[0].sets[0].reps"),'82','background/reload uses persisted timestamp');
 run("state.session=db.get('draft').session;state.session.exercises[0].sets.unshift({set:0,reps:'',done:true});tp107PaintIndoor()");now+=1000;run('tp107StopIndoor(true)');assert.equal(run('state.session.exercises[0].sets[1].reps'),'83','owner survives index shifts');assert.equal(run('state.session.indoorTimer107'),undefined);
 run("tp107ToggleIndoor();tp107StopIndoor();upd(0,1,'reps','95');tp107StopIndoor(true)");assert.equal(run('state.session.exercises[0].sets[1].reps'),'95','paused manual corrections survive final stop');
 run("tp106GpsState={active:true};tp107ToggleIndoor()");assert.equal(run('state.session.indoorTimer107'),undefined,'GPS and indoor timing cannot run together');
}finally{Date.now=before;}
console.log('PASS training/cardio: local Monday boundaries, weighted pace, exact-distance fastest records, invalid/future/unfinished data, time-only, paused/draft/resumed stable timer without GPS');
