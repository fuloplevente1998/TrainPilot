'use strict';
const assert=require('node:assert/strict');
const runtime=require('./helpers/app-runtime.cjs');
const flush=()=>new Promise(resolve=>setImmediate(resolve));
function fixture(nativeAutomatic=false){
 const app=runtime(),events=[],tasks=[];
 let held=null,heldScan=null,failPublish=false,failOpen=false,failPrepare=false,commands=[],nativeFrame=null,armed=false;
 const bridge={publish:async({snapshot})=>{
  const ids=snapshot.exercises.flatMap(e=>e.sets.map(s=>s.setId));
  assert.ok(ids.every(id=>typeof id==='string'&&id.length>0),'every phone-published set needs an identity for watch undo');
  assert.equal(new Set(ids).size,ids.length,'set identities are unique across the workout');
  assert.deepEqual(app.run('db.get("draft").session.exercises.flatMap(e=>e.sets.map(s=>s.setId))').join('|'),ids.join('|'),'identities reach the durable draft before publication');
  if(nativeAutomatic){assert.equal(snapshot.nativeExecutionProtocol,2);assert.equal(snapshot.generation,nativeFrame.generation);assert.equal(snapshot.executionRevision,nativeFrame.executionRevision);assert.equal(snapshot.workoutId,nativeFrame.workoutId);}events.push(['publish',snapshot.workoutId]);if(held)await held;if(failPublish)throw Error('offline')},clear:async()=>{},publishHome:async()=>{},
 startWatchMeasurement:async({workoutId})=>{events.push(['open',workoutId]);if(failOpen)throw Error('remote refused');return {opened:true}},
 pendingCommands:async()=>{if(heldScan)await heldScan;return {commands:commands.slice()}},ackCommand:async({commandId})=>{commands=commands.filter(c=>c.commandId!==commandId)}};
 app.context.window.Capacitor={isNativePlatform:()=>true,Plugins:{WearSync:bridge}};
 if(nativeAutomatic){
  bridge.executionStatus=async()=>({snapshot:nativeFrame?JSON.parse(JSON.stringify(nativeFrame)):null,events:[],timer:{},controllerArmed:armed});
  bridge.prepareExecution=async({snapshot,expectedRevision})=>{
   events.push(['prepare',snapshot.workoutId]);if(failPrepare)throw Error('native not prepared');
   assert.equal(expectedRevision,nativeFrame?.executionRevision||0);assert.ok(snapshot.exercises.every(e=>e.sets.every(s=>s.setId)));
   nativeFrame={...JSON.parse(JSON.stringify(snapshot)),executionRevision:expectedRevision+1};return {snapshot:JSON.parse(JSON.stringify(nativeFrame))};
  };
  app.context.window.Capacitor.Plugins.IntervalTimer={armWorkout:async({workoutId,generation})=>{assert.equal(workoutId,nativeFrame.workoutId);assert.equal(generation,nativeFrame.generation);events.push(['arm',workoutId]);armed=true;}};
  app.context.TrainPilotInterval162Core=app.context.window.TrainPilotInterval162Core;
  app.run('nativeQuickRender=renderWorkout;renderWorkout=function(){if(state.session&&!state.session.automatic162)state.session.automatic162={enabled:true,phase:"work"};return nativeQuickRender.apply(this,arguments)}');
 }
 app.context.setTimeout=fn=>{tasks.push(fn);return 1};
 app.context.CSS={escape:value=>value};
 app.run('db.set("draft",null);state.session=null;state.workout=null;');
 return {...app,events,tasks,sync:app.context.window.TrainPilotWearSync,hold:p=>held=p,holdScan:p=>heldScan=p,failPublish:v=>failPublish=v,failOpen:v=>failOpen=v,failPrepare:v=>failPrepare=v,commands:v=>commands=v,
  start:()=>app.run('startWorkout(activeProgram().days[0].id);state.session?.syncId||state.session?.id||state.session?.started')};
}
(async()=>{
 const app=fixture();let release;app.hold(new Promise(r=>release=r));
 const id=app.start();
 await flush();
 assert.deepEqual(app.events,[['publish',id]],'publish must complete before a remote open');
 app.hold(null);release();await flush();
 assert.deepEqual(app.events,[['publish',id],['open',id]],'one explicit program start opens the matching watch workout');
 const publishedIds=app.run('state.session.exercises.flatMap(e=>e.sets.map(s=>s.setId)).join("|")');
 await app.sync.syncNow();await app.sync.syncNow();
 assert.equal(app.run('state.session.exercises.flatMap(e=>e.sets.map(s=>s.setId)).join("|")'),publishedIds,'routine sync preserves published identities');
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
 // #157: the user-visible inline quick-start must survive transport failures
 // without opening the watch on stale data or relaunching it on polling.
 const offlineInline=fixture();offlineInline.failPublish(true);
 await offlineInline.run('tp152StartQuickInline(exercises()[0].id)');await flush();
 const offlineInlineId=offlineInline.sync.makeSnapshot().workoutId;
 assert.equal(offlineInline.events.filter(e=>e[0]==='open').length,0,'an offline quick-start cannot launch stale watch data');
 offlineInline.failPublish(false);await offlineInline.sync.syncNow();
 assert.deepEqual(offlineInline.events.at(-1),['open',offlineInlineId],'a prompt successful retry opens the current quick workout');
 await offlineInline.sync.syncNow();
 assert.equal(offlineInline.events.filter(e=>e[0]==='open').length,1,'quick-start handoff is not repeated by polling');
 const rejectedInline=fixture();rejectedInline.failOpen(true);
 await rejectedInline.run('tp152StartQuickInline(exercises()[0].id)');await flush();
 await rejectedInline.sync.syncNow();
 assert.equal(rejectedInline.events.filter(e=>e[0]==='open').length,1,'a refused quick-start is not reopened automatically');
 assert.ok(rejectedInline.run('state.session && db.get("draft",null)'),'watch refusal cannot discard the phone quick workout');

 const automaticInline=fixture(true);await automaticInline.run('tp152StartQuickInline(exercises()[0].id)');await flush();
 const automaticId=automaticInline.sync.makeSnapshot().workoutId;
 assert.deepEqual(automaticInline.events,[['prepare',automaticId],['arm',automaticId],['publish',automaticId],['open',automaticId]],'native automatic quick-start prepares and arms before publishing and opening the same workout');
 await automaticInline.sync.syncNow();await automaticInline.sync.syncNow();
 assert.equal(automaticInline.events.filter(e=>e[0]==='open').length,1,'native clock polling cannot repeat the quick-start launch');
 const preparationRetry=fixture(true);preparationRetry.failPrepare(true);
 await preparationRetry.run('tp152StartQuickInline(exercises()[0].id)');await flush();
 assert.equal(preparationRetry.events.filter(e=>['publish','open','arm'].includes(e[0])).length,0,'failed native preparation cannot publish or open stale controls');
 preparationRetry.failPrepare(false);await preparationRetry.sync.syncNow();await preparationRetry.sync.syncNow();
 assert.equal(preparationRetry.events.filter(e=>e[0]==='open').length,1,'quick-start retries its pending handoff after successful native preparation');

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

 const restored=fixture();restored.run('state.session=null;db.set("draft",{session:{syncId:"restored-phone",started:"2026-10-15T12:00:00Z",timerOwner:"phone",notes:"Keep notes",healthWear129:{totalCalories:7},exercises:[{id:"x",sets:[{set:1,setId:"keep-id",reps:"5",weight:5,done:true},{set:2,reps:"",weight:5,done:false}]}]},current:0,workout:"A",restEndAt:1234,custom:"Keep draft"})');
 await restored.sync.syncNow();
 const saved=JSON.parse(restored.values.get('repforge:draft')),savedIds=saved.session.exercises[0].sets.map(s=>s.setId);
 assert.equal(savedIds[0],'keep-id');assert.ok(savedIds[1]);assert.equal(saved.restEndAt,1234);assert.equal(saved.custom,'Keep draft');
 assert.equal(saved.session.notes,'Keep notes');assert.equal(saved.session.healthWear129.totalCalories,7);assert.equal(saved.session.exercises[0].sets[0].done,true);
 await restored.sync.syncNow();assert.equal(JSON.parse(restored.values.get('repforge:draft')).session.exercises[0].sets[1].setId,savedIds[1],'restored draft identities survive repeated publication');
 assert.equal(restored.run('state.session'),null,'repairing a saved draft does not resume the workout');
 const quota=fixture();quota.run('state.session={syncId:"quota",started:"2026-10-16T08:00:00Z",timerOwner:"phone",exercises:[{id:"x",sets:[{set:1,reps:"5",done:false}]}]};persistDraft()');
 const write=quota.context.localStorage.setItem;quota.context.localStorage.setItem=(k,v)=>{if(k==='repforge:draft')throw Error('quota');return write(k,v);};
 await quota.sync.syncNow();assert.equal(quota.events.length,0,'uncommitted identities cannot reach the watch');assert.equal(quota.run('state.session.exercises[0].sets[0].setId'),undefined,'failed persistence restores the prior identity');
 quota.context.localStorage.setItem=write;await quota.sync.syncNow();assert.equal(quota.events.length,1,'retry persists identities before publishing');

 const phoneRecord=fixture();phoneRecord.run('state.session={syncId:"phone-record",started:"2026-10-16T08:00:00Z",timerOwner:"phone",automatic162:{enabled:false,phase:"work"},notes:"Keep notes",healthWear129:{totalCalories:7},exercises:[{id:"db-floor-press",repUnit:"ism.",loadType:"weight",sets:[{set:1,reps:"5",weight:5.5,done:false},{set:2,reps:"",weight:5.5,done:false}]}]};state.current=0;state.workout="quick";');
 phoneRecord.windowEvents.get('DOMContentLoaded').find(fn=>fn.toString().includes('const base=toggleSet;toggleSet=function'))();
 await phoneRecord.run('toggleSet(0,0)');await flush();
 const recordedSnapshot=phoneRecord.sync.makeSnapshot(),candidate=recordedSnapshot.lastRecord188;
 assert.ok(candidate?.recordId,'phone UI completion publishes a durable undo candidate to the watch');
 assert.equal(candidate.setId,recordedSnapshot.exercises[0].sets[0].setId);assert.equal(recordedSnapshot.exercises[0].sets[0].done,true);
 assert.equal(phoneRecord.run('db.get("draft").session.lastRecord188.recordId'),candidate.recordId);
 phoneRecord.context.TrainPilotAutomatic162={command:async()=>true};phoneRecord.context.TrainPilotIntervals162={command:async()=>true};
 phoneRecord.commands([{...candidate,commandId:'watch-undo-phone-record',action:'undoSet',sequence:1,createdAt:Date.now()}]);
 await phoneRecord.sync.drainCommands();
 assert.equal(phoneRecord.run('state.session.exercises[0].sets[0].done'),false,'watch inverse also reverses a set completed on the phone');
 assert.equal(phoneRecord.run('state.session.healthWear129.totalCalories'),7);assert.equal(phoneRecord.run('state.session.notes'),'Keep notes');
 assert.equal(phoneRecord.run('state.session.undo188.status'),'applied');

 const metadataQuota=fixture();metadataQuota.run('state.session={syncId:"metadata-quota",started:"2026-10-16T08:00:00Z",timerOwner:"phone",automatic162:{enabled:false},exercises:[{id:"db-floor-press",repUnit:"ism.",loadType:"weight",sets:[{set:1,setId:"stable",reps:"5",weight:5.5,done:false}]}]};state.current=0;');
 metadataQuota.windowEvents.get('DOMContentLoaded').find(fn=>fn.toString().includes('const base=toggleSet;toggleSet=function'))();
 const metadataWrite=metadataQuota.context.localStorage.setItem;
 metadataQuota.context.localStorage.setItem=(k,v)=>{if(k==='repforge:draft'&&JSON.parse(v)?.session?.lastRecord188)throw Error('metadata quota');return metadataWrite(k,v);};
 let quotaFailed=false;try{await metadataQuota.run('toggleSet(0,0)');}catch(_){quotaFailed=true;}
 assert.equal(quotaFailed,true);assert.equal(metadataQuota.run('state.session.lastRecord188'),undefined,'uncommitted undo metadata is rolled back');
 metadataQuota.context.localStorage.setItem=metadataWrite;await metadataQuota.sync.syncNow();assert.equal(metadataQuota.sync.makeSnapshot().lastRecord188,undefined,'polling cannot publish failed undo metadata');

 const refused=fixture();refused.failOpen(true);refused.start();await flush();await refused.sync.syncNow();
 assert.ok(refused.run('state.session && db.get("draft",null)'),'remote refusal must preserve the phone workout');
 assert.equal(refused.events.filter(e=>e[0]==='open').length,1,'remote refusal is not retried by routine polling');
 console.log('PASS full runtime phone → watch measurement handoff: publish order, real program/quick starts, native automatic preparation/retry, async confirmation, origin, stable manual IDs, draft repair/quota rollback, phone recording → watch inverse, busy/retry, restore, cancellation and remote refusal');
})().catch(error=>{console.error(error);process.exitCode=1});
