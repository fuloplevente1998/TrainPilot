const assert=require('node:assert/strict'),runtime=require('./helpers/app-runtime.cjs');
const copy=x=>JSON.parse(JSON.stringify(x));
function fixture(){
 const app=runtime(),calls=[];
 app.run(`state.session={started:'2026-10-14T10:00:00.000Z',syncId:'pause-test',programId:'p',dayId:'A',exercises:[{id:'pushup',sets:[{set:1,setId:'set',reps:'5',done:false}]}],activeIntervals:[{start:'2026-10-14T10:00:00.000Z',end:null}],automatic162:{enabled:true,phase:'rest',run:{key:'clock',running:true}}};state.current=0;state.workout='A';state.restEndAt=Date.now()+120000;persistDraft();`);
 app.context.window.Capacitor={isNativePlatform:()=>true,Plugins:{WearSync:{setWorkoutPause:async({snapshot})=>{calls.push(['pause',copy(snapshot)]);assert.equal(app.run('db.get("draft").session.workoutPaused'),snapshot.workoutPaused,'durable draft precedes native transition');}}}};
 app.context.window.TrainPilotAutomatic162={owner:()=>({meta:app.run('state.session.automatic162')}),command:async action=>{calls.push([action]);app.run(`state.session.automatic162.run.running=${action==='resume'};persistDraft();`);return true;}};
 app.context.window.TrainPilotIntervals162={owner:()=>null};
 app.context.window.TrainPilotWearSync={makeSnapshot:()=>app.run(`({active:true,workoutId:state.session.syncId,workoutPaused:state.session.workoutPaused,pauseRevision:state.session.pauseRevision,activeIntervals:state.session.activeIntervals})`),syncNow:async()=>calls.push(['sync'])};
 return {...app,calls,pause:app.context.window.TrainPilotWorkoutPause201};
}
(async()=>{
 const app=fixture(),session=app.run('state.session');
 assert.equal(await app.pause.pause(session),true);
 assert.equal(session.workoutPaused,true);assert.equal(session.automatic162.run.running,false);
 assert.ok(session.activeIntervals[0].end);assert.equal(app.run('state.restEndAt'),null);
 const revision=session.pauseRevision;assert.deepEqual(app.calls.map(c=>c[0]),['pause','pause','sync']);
 app.context.document.hidden=true;
 for(const fn of app.documentEvents.get('visibilitychange')||[])fn();
 assert.equal(session.workoutPaused,true,'background status does not resume explicit pause');
 // Resume only on an explicit continuation, with a new interval and a newer pause fence.
 app.run('state.session.activeIntervals.push({start:new Date().toISOString(),end:null});');
 assert.equal(await app.pause.resume(session),true);assert.equal(session.workoutPaused,false);
 assert.ok(session.pauseRevision>revision);assert.equal(session.automatic162.run.running,true);
 const locked=fixture();locked.context.document.hidden=true;
 for(const fn of locked.documentEvents.get('visibilitychange')||[])fn();
 assert.notEqual(locked.run('state.session.workoutPaused'),true,'screen lock alone leaves the workout active');
 const failure=fixture();failure.context.window.Capacitor.Plugins.WearSync.setWorkoutPause=async()=>{throw Error('storage');};
 assert.equal(await failure.pause.pause(failure.run('state.session')),false);
 assert.equal(failure.run('db.get("draft").session.workoutPaused'),true,'failed bridge retains the saved pause for retry');
 const lost=fixture();await lost.pause.pause(lost.run('state.session'));
 let nativePause={paused:true,revision:lost.run('state.session.pauseRevision')},resumedAt=0;
 lost.context.window.Capacitor.Plugins.WearSync.setWorkoutPause=async({snapshot})=>{
  assert.ok(snapshot.pauseRevision>nativePause.revision,'inverse pause must use a newer revision than the native resume');
  nativePause={paused:snapshot.workoutPaused,revision:snapshot.pauseRevision};
  if(!snapshot.workoutPaused){resumedAt=snapshot.pauseRevision;throw Error('response lost after successful native resume');}
 };
 assert.equal(await lost.pause.resume(lost.run('state.session')),false);
 assert.equal(nativePause.paused,true,'lost resume acknowledgement is followed by an explicit durable native pause');
 assert.ok(nativePause.revision>resumedAt);assert.equal(lost.run('state.session.workoutPaused'),true);
 const cancelled=fixture();cancelled.run('tp2628Confirm=async()=>false;go("home")');await new Promise(setImmediate);
 assert.ok(cancelled.run('state.session'));assert.notEqual(cancelled.run('state.session.workoutPaused'),true,'cancelled leave does not pause');
 console.log('PASS #201 explicit leave, durable pause before native control, deadline preservation, continuation, background distinction, bridge failure and cancelled leave');
})().catch(e=>{console.error(e);process.exitCode=1});
