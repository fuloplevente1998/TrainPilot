'use strict';
const assert=require('node:assert/strict'),runtime=require('./helpers/app-runtime.cjs');
const copy=v=>JSON.parse(JSON.stringify(v));
function draft(){return {session:{syncId:'w193',started:'2026-10-18T10:00:00Z',notes:'Keep my notes',automatic162:{enabled:false},exercises:[{id:'plank',prescription:{rest:90},sets:[{setId:'set193',reps:'12',weight:0,done:false,comment:'Keep this too'}]}]},current:0,workout:'A'};}
function root(state='committed',epoch=1){return {active:true,ownershipProtocol193:1,workoutId:'w193',ownerEpoch193:state==='committed'?epoch:epoch-1,timerOwner:state==='committed'?'watch':'phone',workoutPaused:true,pauseRevision:5,currentExercise:0,pausedRestMillis:2500,activeIntervals:[{start:'2026-10-18T10:00:00Z',end:'2026-10-18T10:00:12Z'}],handoff193:{protocol:1,id:'transfer193',epoch,source:'phone',target:'watch',state},exercises:[{id:'plank',sets:[{setId:'set193',reps:'18',weight:0,done:false}]}]};}
const app=runtime(),core=app.context.window.TrainPilotOwnership193Core;
const initial=draft(),text=JSON.stringify(initial),adopted=core.merge(initial,root());
assert.equal(JSON.stringify(initial),text,'merge is pure before durable storage');
assert.equal(adopted.session.exercises[0].sets[0].reps,'18');
assert.equal(adopted.session.exercises[0].prescription.rest,90);
assert.equal(adopted.session.exercises[0].sets[0].comment,'Keep this too');
assert.equal(adopted.session.notes,'Keep my notes');
assert.equal(adopted.session.timerOwner,'watch');assert.equal(adopted.session.workoutPaused,true);
adopted.session.exercises[0].sets[0].reps='25';adopted.session.workoutPaused=false;
assert.equal(core.merge(adopted,root()),null,'duplicate journal commit cannot erase newer watch data or resume');
assert.equal(core.merge(adopted,root('offered')),null,'late offered state cannot roll back committed ownership');
const old=root();old.ownerEpoch193=0;old.handoff193.epoch=0;assert.throws(()=>core.merge(initial,old),/checkpoint/);
const different=root();different.handoff193.id='other';assert.throws(()=>core.merge(adopted,different),/Conflicting/);
const wrong=root();wrong.exercises[0].sets[0].setId='wrong';assert.throws(()=>core.merge(initial,wrong),/set changed/);
const invalid=root();invalid.exercises[0].sets[0].reps='NaN';assert.throws(()=>core.merge(initial,invalid),/Invalid transferred/);
const offered=core.merge(initial,root('offered'));assert.equal(core.pending(offered.session),true);
assert.equal(core.merge(offered,root()).session.ownerEpoch193,1,'offered -> committed is a distinct durable transition');
const reverse=root('committed',2);reverse.handoff193={...reverse.handoff193,id:'reverse193',source:'watch',target:'phone'};reverse.timerOwner='phone';
assert.equal(core.merge(adopted,reverse).session.timerOwner,'phone');
assert.equal(core.merge(adopted,reverse).session.ownershipAwaitingResume193,true,'returning to the phone cannot revive its retired execution generation');
assert.equal(core.merge(initial,{...root(),workoutId:'other'}),null);
assert.equal(core.commandAllowed(adopted.session,{workoutId:'w193',action:'completeSet',ownerEpoch193:0}),false,'queued commands are rechecked after native delivery');
assert.equal(core.commandAllowed(adopted.session,{workoutId:'w193',action:'completeSet',ownerEpoch193:1}),true);
assert.equal(core.commandAllowed(offered.session,{workoutId:'w193',action:'completeSet',ownerEpoch193:0}),false);
assert.equal(core.commandAllowed(offered.session,{workoutId:'w193',action:'healthSummary',ownerEpoch193:0}),true);
function fixture(status){
 const f=runtime();let current=copy(status);const calls=[];
 f.context.window.Capacitor={isNativePlatform:()=>true,Plugins:{WearSync:{ownershipStatus193:async()=>copy(current)}}};
 f.context.input=draft();f.run('state.session=input.session;state.current=0;state.workout="A";state.tab="home";db.set("draft",input);savedSet193=state.session.exercises[0].sets[0];');
 return {...f,calls,setStatus:v=>current=copy(v),sync:f.context.window.TrainPilotOwnership193.sync};
}
(async()=>{
 const f=fixture({state:{snapshot:root()}});assert.equal(await f.sync(),true);
 assert.equal(f.run('state.session.timerOwner'),'watch');assert.equal(f.run('savedSet193===state.session.exercises[0].sets[0]'),true);
 assert.equal(JSON.parse(f.values.get('repforge:draft')).session.ownerEpoch193,1);
 f.run('state.session.exercises[0].sets[0].reps="29";state.session.workoutPaused=false');await f.sync();
 assert.equal(f.run('state.session.exercises[0].sets[0].reps'),'29');assert.equal(f.run('state.session.workoutPaused'),false);
 const quota=fixture({state:{snapshot:root()}}),setItem=quota.context.localStorage.setItem;
 quota.context.localStorage.setItem=(k,v)=>{if(k==='repforge:draft')throw Error('quota');setItem(k,v);};
 assert.equal(await quota.sync(),false);assert.equal(quota.run('state.session.timerOwner'),undefined,'failed durable commit never enables a new owner');
 const pending=fixture({state:{snapshot:root('offered')}});assert.equal(await pending.sync(),false,'pending handoff blocks ordinary publishing');
 const background=fixture({state:{stage:'incoming',request:{id:'request193',workoutId:'w193',epoch:0}}});
 background.run('state.session.ownershipProtocol193=1');background.context.document.hidden=true;
 assert.equal(await background.sync(),false,'source cannot freeze from hidden WebView');
 const fresh=fixture({state:{stage:'incoming',request:{id:'request193',workoutId:'w193',epoch:0}}});
 fresh.run('state.session.ownershipProtocol193=1');
 fresh.context.window.TrainPilotPhoneExecution162={sync:async()=>{fresh.calls.push('reconcile');return true;}};
 fresh.context.window.TrainPilotWorkoutPause201={pause:async s=>{fresh.calls.push('pause');s.workoutPaused=true;return true;}};
 fresh.context.window.TrainPilotWearSync.makeSnapshot=()=>({workoutId:'w193',workoutPaused:true});
 fresh.context.window.Capacitor.Plugins.WearSync.prepareOwnership193=async({requestId,snapshot})=>{
  fresh.calls.push('prepare');assert.equal(requestId,'request193');assert.equal(snapshot.workoutPaused,true);return {state:{snapshot:root('offered')}};
 };
 assert.equal(await fresh.sync(),false);assert.deepEqual(fresh.calls,['reconcile','pause','reconcile','prepare'],'pause effects must be durably reconciled before offering');
 const legacy=runtime();assert.equal(await legacy.context.window.TrainPilotOwnership193.sync(),true,'browser and legacy plugins retain ordinary behavior');
 console.log('PASS #193 canonical ownership import: atomic commit, stable IDs, annotations, replay/epoch fencing, reverse handoff, source pause ordering and legacy fallback');
})().catch(e=>{console.error(e);process.exitCode=1;});
