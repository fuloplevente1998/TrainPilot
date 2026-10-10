'use strict';
const assert=require('node:assert/strict');
const runtime=require('./helpers/app-runtime.cjs');
const flush=()=>new Promise(resolve=>setImmediate(resolve));
function fixture(){
 const app=runtime(),events=[],tasks=[];
 let held=null,heldScan=null,failPublish=false,failOpen=false,commands=[];
 const bridge={publish:async({snapshot})=>{events.push(['publish',snapshot.workoutId]);if(held)await held;if(failPublish)throw Error('offline')},clear:async()=>{},publishHome:async()=>{},
 startWatchMeasurement:async({workoutId})=>{events.push(['open',workoutId]);if(failOpen)throw Error('remote refused');return {opened:true}},
 pendingCommands:async()=>{if(heldScan)await heldScan;return {commands:commands.slice()}},ackCommand:async({commandId})=>{commands=commands.filter(c=>c.commandId!==commandId)}};
 app.context.window.Capacitor={isNativePlatform:()=>true,Plugins:{WearSync:bridge}};
 app.context.setTimeout=fn=>{tasks.push(fn);return 1};
 app.context.CSS={escape:value=>value};
 app.run('db.set("draft",null);state.session=null;state.workout=null;');
 return {...app,events,tasks,sync:app.context.window.TrainPilotWearSync,hold:p=>held=p,holdScan:p=>heldScan=p,failPublish:v=>failPublish=v,failOpen:v=>failOpen=v,commands:v=>commands=v,
  start:()=>app.run('startWorkout(activeProgram().days[0].id);state.session?.syncId||state.session?.id||state.session?.started')};
}
(async()=>{
 const app=fixture();let release;app.hold(new Promise(r=>release=r));
 const id=app.start();
 await flush();
 assert.deepEqual(app.events,[['publish',id]],'publish must complete before a remote open');
 app.hold(null);release();await flush();
 assert.deepEqual(app.events,[['publish',id],['open',id]],'one explicit program start opens the matching watch workout');
 await app.sync.syncNow();await app.sync.syncNow();
 for(const fn of app.windowEvents.get('focus')||[])fn();await flush();
 assert.equal(app.events.filter(e=>e[0]==='open').length,1,'polling and foreground resume cannot repeatedly open the watch');
 app.run('state.session=null;state.workout=null');await app.sync.syncNow();
 assert.equal(app.events.filter(e=>e[0]==='open').length,1,'a restored draft is not a new start');

 const scan=fixture();let releaseScan;scan.holdScan(new Promise(r=>releaseScan=r));const pendingScan=scan.sync.drainCommands();
 const scanId=scan.start();await flush();assert.deepEqual(scan.events.at(-1),['open',scanId],'routine asynchronous watch queue scanning cannot suppress a real phone start');
 scan.holdScan(null);releaseScan();await pendingScan;

 const canceled=fixture();canceled.run('db.set("draft",{session:{started:"old",exercises:[]}});tp2628Confirm=async()=>false');
 canceled.start();await flush();assert.equal(canceled.events.length,0,'canceled replacement never opens the watch');
 const replacement=fixture();replacement.run('db.set("draft",{session:{started:"old",exercises:[]}});tp2628Confirm=async()=>true');
 replacement.start();await flush();assert.equal(replacement.events.filter(e=>e[0]==='open').length,1,'accepted asynchronous confirmation triggers the handoff');

 const quick=fixture();quick.run('tp150QuickConfirmReplace=async()=>true');
 await quick.run('tp151SaveQuick(exercises()[0].id)');await flush();
 assert.equal(quick.events.filter(e=>e[0]==='open').length,1,'configured phone quick workout starts measurement');
 await quick.run('tp151SaveQuick(exercises()[1].id)');await quick.sync.syncNow();
 assert.equal(quick.events.filter(e=>e[0]==='open').length,1,'adding an exercise to quick workout does not start another recording');

 // The visible Quick picker uses tp152StartQuickInline, not tp151SaveQuick.
 const inline=fixture();let releaseInline;inline.hold(new Promise(r=>releaseInline=r));
 await inline.run('tp152StartQuickInline(exercises()[0].id)');const inlineId=inline.sync.makeSnapshot().workoutId;await flush();
 assert.deepEqual(inline.events,[['publish',inlineId]],'inline quick workout must publish before opening the watch');
 inline.hold(null);releaseInline();await flush();assert.deepEqual(inline.events,[['publish',inlineId],['open',inlineId]],'the actual inline quick start opens the matching watch workout once');
 await inline.run('tp152StartQuickInline(exercises()[1].id)');await inline.sync.syncNow();
 assert.equal(inline.events.filter(e=>e[0]==='open').length,1,'adding an inline quick exercise must not reopen/restart measurement');
 const canceledInline=fixture();canceledInline.run('db.set("draft",{session:{started:"old",exercises:[]}});tp2628Confirm=async()=>false');
 await canceledInline.run('tp152StartQuickInline(exercises()[0].id)');await flush();assert.equal(canceledInline.events.length,0,'canceled inline draft replacement must not launch the watch');
 const acceptedInline=fixture();acceptedInline.run('db.set("draft",{session:{started:"old",exercises:[]}});tp2628Confirm=async()=>true');
 await acceptedInline.run('tp152StartQuickInline(exercises()[0].id)');await flush();assert.equal(acceptedInline.events.filter(e=>e[0]==='open').length,1);
 const watchInline=fixture();watchInline.run('tp150QuickConfirmReplace=()=>new Promise(resolve=>confirmWatch=resolve)');watchInline.sync.isStartingFromWatch=()=>true;
 const watchInlineStart=watchInline.run('tp152StartQuickInline(exercises()[0].id)');watchInline.sync.isStartingFromWatch=()=>false;watchInline.run('confirmWatch(true)');await watchInlineStart;await flush();await watchInline.sync.syncNow();
 assert.equal(watchInline.events.filter(e=>e[0]==='open').length,0,'watch origin is captured before asynchronous inline confirmation');

 const watch=fixture();watch.commands([{commandId:'start-watch',action:'startWorkout',workoutId:'watch-origin',started:'2026-10-10T10:00:00Z',dayId:watch.run('activeProgram().days[0].id'),programId:watch.run('activeProgram().id'),createdAt:1}]);
 await watch.sync.drainCommands();assert.equal(watch.events.filter(e=>e[0]==='open').length,0,'watch-origin starts cannot bounce the UI back to the watch');
 assert.equal(watch.run('state.session.syncId'),'watch-origin');

 const retry=fixture();retry.failPublish(true);const retryId=retry.start();await flush();
 assert.equal(retry.events.filter(e=>e[0]==='open').length,0,'a failed publish must not open stale data');
 retry.failPublish(false);await retry.sync.syncNow();
 assert.deepEqual(retry.events.at(-1),['open',retryId],'a short transport failure can retry the explicit pending start');

 const busy=fixture();busy.run('state.session={syncId:"old-publish",started:"2026-10-09T10:00:00Z",exercises:[{id:"x",sets:[{set:1}]}]}');
 let unblock;busy.hold(new Promise(r=>unblock=r));const oldSend=busy.sync.syncNow();
 busy.run('state.session=null;db.set("draft",null)');const newId=busy.start();await flush();busy.hold(null);unblock();await oldSend;
 assert.equal(busy.events.filter(e=>e[0]==='open').length,0);
 const queued=busy.tasks.filter(fn=>fn.name==='syncNow');
 assert.equal(queued.length,1,'an explicit start while a previous send is busy must be retained');
 await queued[0]();assert.deepEqual(busy.events.at(-1),['open',newId]);

 const closed=fixture();let resolveClosed;closed.hold(new Promise(r=>resolveClosed=r));closed.start();
 await flush();
 closed.run('state.session=null;state.workout=null;db.set("draft",null)');closed.hold(null);resolveClosed();await flush();await closed.sync.syncNow();
 assert.equal(closed.events.filter(e=>e[0]==='open').length,0,'finishing/discarding before delivery cancels the launch');
 const hidden=fixture();hidden.failPublish(true);hidden.start();await flush();hidden.context.document.hidden=true;hidden.failPublish(false);await hidden.sync.syncNow();hidden.context.document.hidden=false;await hidden.sync.syncNow();
 assert.equal(hidden.events.filter(e=>e[0]==='open').length,0,'backgrounding cancels pending launch, including on later foreground');
 const expired=fixture();expired.failPublish(true);expired.start();await flush();expired.failPublish(false);
 const future=Date.now()+16000;expired.context.Date=class extends Date{static now(){return future}};
 await expired.sync.syncNow();assert.equal(expired.events.filter(e=>e[0]==='open').length,0,'reconnecting later must not open an old workout automatically');

 const refused=fixture();refused.failOpen(true);refused.start();await flush();await refused.sync.syncNow();
 assert.ok(refused.run('state.session && db.get("draft",null)'),'remote refusal must preserve the phone workout');
 assert.equal(refused.events.filter(e=>e[0]==='open').length,1,'remote refusal is not retried by routine polling');
 console.log('PASS full runtime phone → watch measurement handoff: publish order, real program/quick starts, async confirmation, origin, busy/retry, restore, cancellation and remote refusal');
})().catch(error=>{console.error(error);process.exitCode=1});
