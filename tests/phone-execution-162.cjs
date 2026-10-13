'use strict';
const assert=require('node:assert/strict'),fs=require('node:fs'),vm=require('node:vm');
const runtime=require('./helpers/app-runtime.cjs'),copy=v=>JSON.parse(JSON.stringify(v));
function draft(){return {session:{syncId:'workout',started:'2026-10-13T10:00:00Z',phoneExecution162:{generation:10,revision:1,sequence:0},automatic162:{enabled:true,phase:'work'},notes:'Keep notes',exercises:[{id:'press',repUnit:'ism.',name:'Press',effort:'Keep feedback',sets:[{setId:'b',done:false,reps:'',weight:5},{setId:'a',done:false,reps:'9',weight:7}]}]},current:0,workout:'A',restEndAt:null};}
function result(){return {snapshot:{active:true,workoutId:'workout',generation:10,executionRevision:2},events:[{sequence:1,workoutId:'workout',generation:10,executionRevision:2,exerciseId:'press',setId:'a',values:{done:true,reps:'6',weight:5.5}}],timer:{}};}
const base=runtime(),merge=base.context.window.TrainPilotPhoneExecution162Core.merge;
const original=draft(),originalText=JSON.stringify(original),merged=merge(original,result());
assert.equal(JSON.stringify(original),originalText,'merge never mutates the source before storage commits');
assert.equal(merged.draft.session.exercises[0].sets[1].reps,'6','stable ID wins over position');
assert.equal(merged.draft.session.exercises[0].sets[0].done,false);
assert.equal(merged.draft.session.notes,'Keep notes');assert.equal(merged.draft.session.exercises[0].effort,'Keep feedback');
assert.equal(merged.draft.session.phoneExecution162.sequence,1);
assert.equal(merge(merged.draft,result()).changed,false,'receipt replay does not repeat a completion');
assert.equal(merge(original,{...result(),snapshot:{...result().snapshot,workoutId:'old'}}),null);
assert.equal(merge(original,{...result(),snapshot:{...result().snapshot,generation:9}}),null);
const conflicting=draft();conflicting.session.exercises[0].sets[1].done=true;
assert.throws(()=>merge(conflicting,result()),/conflicting edits/);
const edited=draft(),withBefore=result();withBefore.events[0].before={reps:'9',weight:7};edited.session.exercises[0].sets[1].reps='10';assert.throws(()=>merge(edited,withBefore),/conflicting edits/);
const broken=result();broken.events.push({...broken.events[0],sequence:2,setId:'missing'});
assert.throws(()=>merge(original,broken),/target changed/);assert.equal(JSON.stringify(original),originalText);
const plan={key:'native|10|2',workoutId:'workout',started:original.session.started,native:true,restOnly:true,autoAdvance:false,initialRest:0,steps:[{exerciseId:'press',setId:'b',work:1,rest:120,rounds:1}],target:{exerciseId:'press',setId:'b'}};
const rest=result();rest.snapshot.transition={state:'started',phase:'rest',currentExercise:0,plan};rest.events[0].transition=rest.snapshot.transition;
rest.timer={key:plan.key,elapsedMs:37000,remainingMs:83000,running:true,phaseNumber:1};
const resumed=merge(original,rest).draft.session;
assert.equal(resumed.automatic162.run.elapsedMs,37000);assert.equal(resumed.automatic162.run.running,true);
assert.equal(resumed.exercises[0].sets[0].reps,'','rest cannot invent repetitions');
rest.snapshot.transition.state='interrupted';assert.equal(merge(original,rest).draft.session.automatic162.phase,'interrupted');
assert.equal(merge(original,rest).draft.session.automatic162.run.running,false);
console.log('PASS native reconciliation: stable IDs, atomic draft/cursor, preserved annotations, stale generations, replay/conflict and interrupted clocks');
function fixture(nativeResult=result()){
 const app=runtime(),calls=[];let state=copy(nativeResult),failedAck=false,failPrepare=false;
 const api={executionStatus:async()=>copy(state),acknowledgeExecution:async({sequence})=>{
  calls.push(['ack',sequence]);const saved=JSON.parse(app.values.get('repforge:draft'));assert.equal(saved.session.phoneExecution162.sequence,sequence,'ack follows durable cursor');
  if(failedAck)throw Error('bridge interrupted');state.events=[];
 },prepareExecution:async({snapshot,expectedRevision})=>{calls.push(['prepare',expectedRevision]);if(failPrepare)throw Error('CAS conflict');assert.equal(expectedRevision,state.snapshot.executionRevision);state.snapshot={...copy(snapshot),executionRevision:expectedRevision+1};return {snapshot:copy(state.snapshot)};},
 closeExecution:async()=>{calls.push(['close']);state.snapshot.active=false;},suspendExecution:async()=>calls.push(['suspend']),
 publish:async()=>calls.push(['publish']),publishHome:async()=>{},blockExecution:async({blocked})=>calls.push(['block',blocked])};
 const clock={armWorkout:async()=>calls.push(['arm']),disarmWorkout:async()=>calls.push(['disarm'])};
 app.context.window.Capacitor={isNativePlatform:()=>true,Plugins:{WearSync:api,IntervalTimer:clock}};
 app.context.TrainPilotInterval162Core=app.context.window.TrainPilotInterval162Core;
 app.run('renderWorkout=function(){throw Error("Do not replace a focused editor")};');
 app.context.document.activeElement={matches:()=>true};
 app.context.inputDraft=draft();app.run('state.session=inputDraft.session;state.current=0;state.workout="A";state.tab="plan";db.set("draft",inputDraft);');
 return {...app,calls,api,native:app.context.window.TrainPilotPhoneExecution162,failAck:v=>failedAck=v,failPrepare:v=>failPrepare=v,setResult:v=>state=copy(v)};
}
(async()=>{
 const app=fixture();app.run("savedSetRef=state.session.exercises[0].sets[1]");assert.equal(await app.native.sync({prepare:false}),true);assert.deepEqual(app.calls,[['ack',1]]);
 assert.equal(app.run('state.session.exercises[0].sets[1].done'),true,'actual active session adopts durable recording');
 assert.equal(app.run('state.session.notes'),'Keep notes');assert.equal(app.run('savedSetRef===state.session.exercises[0].sets[1]&&savedSetRef.done'),true,'open editors keep their set reference');
 const retry=fixture();retry.failAck(true);assert.equal(await retry.native.sync({prepare:false}),false);
 assert.equal(retry.run('db.get("draft").session.phoneExecution162.sequence'),1,'failed ack retains committed cursor');
 retry.failAck(false);assert.equal(await retry.native.sync({prepare:false}),true);assert.deepEqual(retry.calls,[['ack',1],['ack',1]]);
 const quota=fixture();const setItem=quota.context.localStorage.setItem;quota.context.localStorage.setItem=(k,v)=>{if(k==='repforge:draft')throw Error('quota');return setItem(k,v);};
 assert.equal(await quota.native.sync({prepare:false}),false);assert.equal(quota.calls.length,0,'failed storage never acknowledges native effects');
 assert.equal(quota.run('state.session.exercises[0].sets[1].done'),false,'failed commit does not mutate active data');
 const guarded=fixture();guarded.context.window.TrainPilotBackupBusy=true;assert.equal(await guarded.native.sync(),false);assert.equal(guarded.calls.length,0);
 const ready=fixture({snapshot:{active:true,workoutId:'workout',generation:10,executionRevision:1},events:[],timer:{}});
 assert.equal(await ready.native.sync(),true);assert.deepEqual(ready.calls,[['prepare',1],['arm']]);
 assert.equal(await ready.native.sync(),true);assert.deepEqual(ready.calls,[['prepare',1],['arm']],'ordinary polling cannot churn execution revision or rearm');
 ready.run('state.session.automatic162.enabled=false;');assert.equal(await ready.native.sync(),true);assert.deepEqual(ready.calls.slice(-2),[['suspend'],['disarm']]);
 ready.run('state.session.automatic162.enabled=true;');assert.equal(await ready.native.sync(),true);assert.deepEqual(ready.calls.slice(-2),[['prepare',2],['arm']],'reenabling same workout preserves execution identity');
 const conflict=fixture({snapshot:{active:true,workoutId:'workout',generation:10,executionRevision:1},events:[],timer:{}});conflict.failPrepare(true);
 await conflict.context.window.TrainPilotWearSync.syncNow();assert.deepEqual(conflict.calls,[['prepare',1]],'failed prepare cannot publish stale controls or arm a timer');
 const source=fs.readFileSync('www/app.js','utf8'),section=source.match(/\/\/ @section automatic-workout-162.js\n([\s\S]*?)\/\/ @endsection automatic-workout-162.js/)[1];
 const functionSource=section.match(/async function startTimed\(initialRest=0\)\{([\s\S]*?)\n  \}/)[0];
 let started;const c=vm.createContext({settings:()=>({intervalPreset110:{work:20,rest:10,rounds:3,vibration:false}}),TrainPilotIntervals162:{startConfigured:async(config,rest)=>{started={config,rest};return true;}},document:{querySelector(){throw Error('DOM dependency');}}});
 vm.runInContext(functionSource,c);assert.equal(await c.startTimed(120),true);assert.equal(started.rest,120);assert.equal(started.config.work,20);assert.equal(started.config.autoAdvance,true);assert.equal(started.config.vibration,false);
 console.log('PASS full runtime native bridge: commit-before-ack, crash retry, quota/restore/editor guards, prepare-before-arm, toggle lifecycle and DOM-free timed start');
})().catch(error=>{console.error(error);process.exitCode=1;});
