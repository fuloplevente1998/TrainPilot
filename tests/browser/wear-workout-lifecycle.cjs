const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),http=require('node:http');
const {chromium}=require('playwright');
const root=path.resolve('www');
const server=http.createServer((req,res)=>{const u=new URL(req.url,'http://local'),file=path.resolve(root,'.'+(u.pathname==='/'?'/index.html':u.pathname));if(!file.startsWith(root+path.sep)){res.writeHead(403);res.end();return}fs.readFile(file,(e,d)=>{if(e){res.writeHead(404);res.end();return}res.setHeader('Content-Type',file.endsWith('.js')?'text/javascript':file.endsWith('.css')?'text/css':file.endsWith('.html')?'text/html':'application/octet-stream');res.end(d)})});
(async()=>{await new Promise(r=>server.listen(0,'127.0.0.1',r));let browser;try{
 browser=await chromium.launch({headless:true,executablePath:process.env.TRAINPILOT_CHROMIUM||undefined,args:['--no-sandbox']});
 const page=await browser.newPage({viewport:{width:393,height:873},locale:'hu-HU'});page.on('dialog',d=>d.accept());
 await page.addInitScript(()=>{localStorage.setItem('repforge:onboarding128',JSON.stringify('skipped'))});
 await page.goto('http://127.0.0.1:'+server.address().port+'/');await page.waitForFunction(()=>window.TrainPilotBoot?.finished);
 const result=await page.evaluate(async()=>{
  let commands=[],acks=[],watchOpens=[];const bridge={startWatchMeasurement:async({workoutId})=>{watchOpens.push(workoutId);return {opened:true}},publish:async()=>{},publishHome:async()=>{},clear:async()=>{},pendingCommands:async()=>({commands:commands.slice()}),ackCommand:async({commandId})=>{acks.push(commandId);commands=commands.filter(c=>c.commandId!==commandId)}};
  window.Capacitor={isNativePlatform:()=>true,Plugins:{WearSync:bridge}};
  db.set('history',[]);db.set('draft',null);db.set('wearClosedWorkouts',[]);db.set('wearWorkoutResult',null);state.session=null;state.workout=null;
  startWorkout(activeProgram().days[0].id);await new Promise(r=>setTimeout(r,0));const id=state.session.syncId||state.session.id||state.session.started;
  const assertFirstWatchStart=watchOpens.length===1&&watchOpens[0]===id;
  const set=state.session.exercises[0].sets[0];set.reps='12';set.done=true;state.session.scheduleId='wear-schedule';db.set('scheduled',[{id:'wear-schedule',status:'planned'}]);persistDraft();
  const finishedAt=new Date().toISOString();
  commands=[{commandId:'watch-finish',workoutId:id,action:'finishWorkout',confirmed:true,finishedAt,createdAt:Date.now(),sequence:1}];
  await window.TrainPilotWearSync.drainCommands();
  const first={history:history().length,status:db.get('wearWorkoutResult')?.status,session:!!state.session,draft:!!db.get('draft'),dialog:!!document.querySelector('#tp2628Dialog')};
  commands=[{commandId:'watch-retry',workoutId:id,action:'finishWorkout',confirmed:true,createdAt:Date.now(),sequence:2}];await window.TrainPilotWearSync.drainCommands();
  const retry=history().length,finishedMatches=history()[0].finished===finishedAt,scheduleStatus=scheduled().find(s=>s.id==='wear-schedule')?.status;
  state.session=null;db.set('draft',null);startWorkout(activeProgram().days[0].id);await new Promise(r=>setTimeout(r,0));const draftId=state.session.syncId||state.session.id||state.session.started;persistDraft();state.session=null;state.workout=null;go('home');
  const draftSnapshot=window.TrainPilotWearSync.makeSnapshot();
  commands=[{commandId:'watch-discard',workoutId:draftId,action:'discardWorkout',confirmed:true,createdAt:Date.now(),sequence:3}];await window.TrainPilotWearSync.drainCommands();
  return {first,retry,finishedMatches,scheduleStatus,assertFirstWatchStart,watchOpens,draftSnapshot:!!draftSnapshot,lastStatus:db.get('wearWorkoutResult')?.status,draft:!!db.get('draft'),history:history().length,acks};
 });
 assert.equal(result.assertFirstWatchStart,true,'real browser phone start opens the matching watch workout');assert.equal(result.watchOpens.length,2,'watch commands and draft resume cannot reopen the watch');
 assert.deepEqual(result.first,{history:1,status:'saved',session:false,draft:false,dialog:false},JSON.stringify(result));assert.equal(result.retry,1);assert.equal(result.finishedMatches,true);assert.equal(result.scheduleStatus,'completed');assert.equal(result.draftSnapshot,true);assert.equal(result.lastStatus,'discarded');assert.equal(result.draft,false);assert.equal(result.history,1);assert.deepEqual(result.acks,['watch-finish','watch-retry','watch-discard']);
 const deleted=await page.evaluate(async()=>{
  await TrainPilotQuickWorkout.start('db-floor-press');await Promise.resolve();
  const snapshot=TrainPilotWearSync.makeSnapshot();
  window.__discardTrial={snapshot,commands:[],acks:[]};
  Capacitor.Plugins.WearSync.pendingCommands=async()=>({commands:__discardTrial.commands.slice()});
  Capacitor.Plugins.WearSync.ackCommand=async({commandId})=>{__discardTrial.acks.push(commandId);__discardTrial.commands=__discardTrial.commands.filter(c=>c.commandId!==commandId);};
  persistDraft();state.session=null;state.workout=null;go('home');
  return snapshot.workoutId;
 });
 await page.evaluate(()=>{void discardDraft143();});await page.locator('[data-tp2628-cancel]').click();
 assert.equal(await page.evaluate(()=>db.get('draft').session.syncId),deleted,'cancel keeps the original draft');
 await page.evaluate(()=>{void discardDraft143();});await page.locator('[data-tp2628-confirm]').click();await page.waitForFunction(()=>!db.get('draft'));
 const replay=await page.evaluate(async()=>{
  const root=__discardTrial.snapshot,id=root.workoutId;
  __discardTrial.commands=[
   {schema:1,commandId:'deleted-start',action:'startQuickWorkout',workoutId:id,started:root.started,exerciseId:root.exercises[0].id,sequence:1,createdAt:1,finalSnapshot:root},
   {schema:1,commandId:'deleted-auto',action:'watchAutomationState',workoutId:id,sequence:2,createdAt:2,finalSnapshot:{...root,timerOwner:'watch',automation:{owner:'watch',enabled:true,phase:'ready',key:'old-watch-clock',phaseNumber:1,workSeconds:30,restSeconds:90,remainingMs:0}}}
  ];
  await TrainPilotWearSync.drainCommands();await TrainPilotWearSync.drainCommands();
  return {draft:!!db.get('draft'),session:!!state.session,history:history().length,acks:__discardTrial.acks,receipt:db.get('wearWorkoutResult')?.status};
 });
 assert.equal(replay.draft,false,'late watch packets cannot recreate a phone-deleted draft');
 assert.equal(replay.session,false);assert.equal(replay.history,1);assert.deepEqual(replay.acks,['deleted-start','deleted-auto']);assert.equal(replay.receipt,'discarded');
 await page.evaluate(()=>TrainPilotQuickWorkout.start('goblet-squat'));
 assert.notEqual(await page.evaluate(()=>state.session.syncId),deleted,'a fresh workout still starts normally');
 const fenced=await page.evaluate(async()=>{
  const session=state.session,id=session.syncId||session.started,base=session.exercises[0];
  session.exercises=[{...base,sets:[{set:1,setId:'finish-before',reps:'',weight:5,done:false},{set:2,setId:'finish-during',reps:'',weight:0,done:false}]}];
  session.timerOwner='phone';session.automatic162={enabled:false};session.phoneExecution162={generation:991,revision:1,sequence:0};
  session.scheduleId='native-finish-schedule';db.set('scheduled',[{id:session.scheduleId,status:'planned'}]);persistDraft();
  Object.assign(rf110Stopwatch,{key:'finish-test-clock',elapsedMs:3000,startedAt:Date.now()-1000,running:true,timer:null});
  state.tab='history';render();
  const native={active:true,events:[],calls:[]},bridge=Capacitor.Plugins.WearSync;
  bridge.executionStatus=async()=>({snapshot:{active:native.active,workoutId:id,generation:991,executionRevision:native.events.length?2:1},events:JSON.parse(JSON.stringify(native.events)),timer:{}});
  bridge.suspendExecution=async()=>{};
  bridge.blockExecution=async({blocked})=>{
   native.calls.push(blocked?'block':'unblock');
   if(blocked)native.events=[{sequence:1,workoutId:id,generation:991,executionRevision:2,exerciseId:base.id,setId:'finish-during',values:{done:true,reps:'8',weight:5}}];
  };
  bridge.acknowledgeExecution=async()=>{
   if(db.get('draft').session.exercises[0].sets[1].done!==true)throw Error('Native event acknowledged before durable draft');
   native.calls.push('event-ack');native.events=[];
  };
  bridge.closeExecution=async()=>{native.calls.push('close');native.active=false;};
  let queue=[{commandId:'native-finish',workoutId:id,action:'finishWorkout',confirmed:true,createdAt:Date.now(),sequence:1,finishedAt:new Date().toISOString()}];
  const receipts=[];bridge.pendingCommands=async()=>({commands:queue});bridge.ackCommand=async receipt=>{receipts.push({...receipt,nativeActive:native.active});queue=[];};
  await TrainPilotWearSync.drainCommands();
  const saved=history().find(h=>(h.syncId||h.started)===id);
  return {done:saved?.exercises[0].sets.map(s=>s.done),reps:saved?.exercises[0].sets[1].reps,
   schedule:scheduled()[0].status,stopwatch:rf110Stopwatch.running,nativeActive:native.active,tab:state.tab,
   feedback:!!document.querySelector('.tp122-feedback'),dialog:!!document.querySelector('#tp2628Dialog'),receipts,calls:native.calls};
 });
 assert.deepEqual(fenced.done,[false,true],'A native set arriving during finish must reach the saved journal');
 assert.equal(fenced.reps,'8');assert.equal(fenced.schedule,'completed');assert.equal(fenced.stopwatch,false);assert.equal(fenced.nativeActive,false);
 assert.equal(fenced.tab,'history','A remote finish keeps the phone page instead of replaying the finish UI');
 assert.equal(fenced.feedback,false);assert.equal(fenced.dialog,false);
 assert.deepEqual(fenced.receipts,[{commandId:'native-finish',status:'saved',nativeActive:false}]);
 assert.ok(fenced.calls.indexOf('event-ack')<fenced.calls.indexOf('close'));
 console.log('PASS Wear handoff/lifecycle and real phone draft discard: confirmation/cancel, durable closed identity, late start/automation replay, preserved history and fresh start.');
}finally{await browser?.close();server.close()}})().catch(e=>{console.error(e);process.exitCode=1});
