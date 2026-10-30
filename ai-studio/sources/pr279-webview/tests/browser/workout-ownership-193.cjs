'use strict';
const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),http=require('node:http'),{chromium}=require('playwright');
const root=path.resolve('www'),server=http.createServer((req,res)=>{const url=new URL(req.url,'http://local'),file=path.resolve(root,'.'+(url.pathname==='/'?'/index.html':url.pathname));if(!file.startsWith(root+path.sep)){res.writeHead(403);return res.end();}fs.readFile(file,(e,d)=>{if(e){res.writeHead(404);return res.end();}res.setHeader('Content-Type',file.endsWith('.js')?'text/javascript':file.endsWith('.css')?'text/css':'text/html');res.end(d);});});
(async()=>{await new Promise(r=>server.listen(0,'127.0.0.1',r));let browser;try{
 browser=await chromium.launch({executablePath:process.env.TRAINPILOT_CHROMIUM||undefined,args:['--no-sandbox']});
 const p=await browser.newPage({viewport:{width:393,height:873},locale:'hu-HU'}),errors=[];p.on('pageerror',e=>errors.push(String(e)));p.on('dialog',d=>d.accept());
 await p.addInitScript(()=>localStorage.setItem('repforge:onboarding128',JSON.stringify('skipped')));await p.goto('http://127.0.0.1:'+server.address().port);await p.waitForFunction(()=>TrainPilotBoot.finished);
 await p.evaluate(()=>{
  window.__ui193={journal:{},commands:[],acks:[],pauses:[],phoneResumes:0};const copy=v=>JSON.parse(JSON.stringify(v));
  const api={publish:async({snapshot})=>{__ui193.host=copy(snapshot);return {};},publishHome:async()=>({}),
   ownershipStatus193:async()=>({state:copy(__ui193.journal)}),pendingCommands:async()=>({commands:copy(__ui193.commands)}),ackCommand:async r=>{__ui193.acks.push(r);__ui193.commands=__ui193.commands.filter(x=>x.commandId!==r.commandId);},
   setWorkoutPause:async({snapshot})=>{__ui193.pauses.push(copy(snapshot));if(!snapshot.workoutPaused){if(db.get('draft').session.phoneExecution162?.generation!==1000)throw Error('Resume preceded durable generation');__ui193.phoneResumes++;}},
   prepareOwnership193:async({snapshot,requestId})=>{const f=copy(snapshot);f.active=true;f.handoff193={protocol:1,id:requestId,source:'phone',target:'watch',epoch:1,state:'offered'};__ui193.journal={stage:'offered',snapshot:f};return {state:copy(__ui193.journal)};},
   requestOwnership193:async()=>{
    const f=copy(TrainPilotWearSync.makeSnapshot());f.active=true;f.timerOwner='phone';f.ownerEpoch193=2;f.workoutPaused=true;f.pauseRevision=Math.max(Date.now(),f.pauseRevision+1);f.handoff193={protocol:1,id:'return-ui193',source:'watch',target:'phone',epoch:2,state:'committed'};
    f.activeIntervals.forEach(i=>{if(!i.end)i.end=new Date().toISOString();});f.exercises[0].sets[0].reps='29';__ui193.journal={stage:'installed',snapshot:f};return {state:copy(__ui193.journal)};
   },
   preparePhoneResume193:async()=>({snapshot:{...copy(__ui193.journal.snapshot),generation:1000,executionRevision:1,ownershipImport193:{id:'return-ui193',epoch:2}}}),
   installPhoneResume193:async()=>({snapshot:{...copy(__ui193.journal.snapshot),generation:1000,executionRevision:1,ownershipImport193:{id:'return-ui193',epoch:2}}})};
  window.Capacitor={isNativePlatform:()=>true,Plugins:{WearSync:api}};
  const started=new Date().toISOString();state.session={syncId:'ui193',started,quickWorkout:true,type:'quick',timerOwner:'phone',ownershipProtocol193:1,ownerEpoch193:0,automatic162:{enabled:false,owner:'phone'},activeIntervals:[{start:started,end:null}],exercises:[{...byId('plank'),sets:[{set:1,setId:'plank-ui193',reps:'5',weight:0,leftSeconds:0,rightSeconds:0,distanceMeters:0,done:false}]}]};
  state.tab='plan';state.workout='quick';state.current=0;persistDraft();renderWorkout();
 });
 for(const width of [320,393])for(const lang of ['hu','en','de','ro']){
  await p.setViewportSize({width,height:873});await p.evaluate(lang=>{db.set('language',lang);renderWorkout();},lang);
  assert.ok(await p.evaluate(()=>document.documentElement.scrollWidth-innerWidth)<=1,'ownership panel fits '+width+'/'+lang);
 }
 await p.evaluate(()=>{db.set('language','hu');renderWorkout();});
 await p.locator('#rf110StopwatchToggle').click();await p.waitForTimeout(1100);
 await p.evaluate(async()=>{__ui193.journal={stage:'incoming',request:{id:'take-ui193',workoutId:'ui193',epoch:0}};await TrainPilotOwnership193.sync();});
 assert.equal(await p.evaluate(()=>rf110Stopwatch.running),false,'source manual timer freezes before the offered snapshot');
 assert.ok(Number(await p.evaluate(()=>state.session.exercises[0].sets[0].reps))>=6);
 await p.waitForFunction(()=>document.querySelector('[data-tp193-action="retry"]'));
 assert.equal(await p.locator('#rf110Stopwatch').count(),0,'frozen source does not expose a second phone stopwatch');
 await p.evaluate(async()=>{const f=__ui193.journal.snapshot;f.timerOwner='watch';f.ownerEpoch193=1;f.handoff193.state='committed';__ui193.journal.stage='committed';await TrainPilotOwnership193.sync();});
 await p.waitForFunction(()=>state.session.timerOwner==='watch');
 const intervals=await p.evaluate(()=>JSON.stringify(state.session.activeIntervals));const nativePauses=await p.evaluate(()=>__ui193.pauses.length);
 await p.evaluate(async()=>{window.tp2628Confirm=async()=>true;await go('home');});await p.waitForFunction(()=>state.session===null);
 assert.equal(await p.evaluate(()=>JSON.stringify(db.get('draft').session.activeIntervals)),intervals);
 await p.evaluate(()=>resumeDraft());await p.waitForFunction(()=>!!state.session);
 assert.equal(await p.evaluate(()=>JSON.stringify(state.session.activeIntervals)),intervals,'phone mirror reopening retains source activity intervals');
 assert.equal(await p.evaluate(()=>__ui193.pauses.length),nativePauses,'phone navigation cannot pause or resume the watch owner');
 await p.evaluate(async()=>{
  const frame=JSON.parse(JSON.stringify(__ui193.journal.snapshot));frame.workoutPaused=false;frame.pauseRevision=Date.now()+1;frame.activeIntervals.push({start:new Date().toISOString(),end:null});
  __ui193.commands=[{schema:1,commandId:'resume-ui193',action:'resumeWorkout',workoutId:'ui193',ownerEpoch193:1,pauseRevision:frame.pauseRevision,sequence:1,createdAt:Date.now(),finalSnapshot:frame}];
  await TrainPilotWearSync.drainCommands();
 });
 assert.equal(await p.evaluate(()=>state.session.workoutPaused),false);assert.equal(await p.evaluate(()=>__ui193.phoneResumes),0);
 const editable=await p.locator('main.tp153-workout input:not([disabled])').count();assert.equal(editable,0,'watch owner keeps phone measurement/edit controls read only');
 await p.locator('[data-tp193-action="take"]').click();await p.waitForFunction(()=>state.session.ownerEpoch193===2&&state.session.ownershipAwaitingResume193===true);
 assert.equal(await p.evaluate(()=>__ui193.phoneResumes),0,'confirmed reverse handoff remains paused until a separate explicit continuation');
 assert.equal(await p.evaluate(()=>state.session.exercises[0].sets[0].reps),'29','returned source measurements reach canonical phone data');
 await p.locator('[data-tp193-action="resume"]').click();await p.waitForFunction(()=>state.session.workoutPaused===false&&state.session.phoneExecution162?.generation===1000);
 assert.equal(await p.evaluate(()=>__ui193.phoneResumes),1);assert.equal(await p.evaluate(()=>state.session.exercises[0].sets[0].reps),'29');
 fs.mkdirSync('ui-evidence',{recursive:true});await p.locator('[data-tp193-panel]').scrollIntoViewIfNeeded();await p.screenshot({path:'ui-evidence/workout-ownership-193.png'});
 await p.evaluate(()=>{delete state.session.ownershipProtocol193;renderWorkout();});assert.equal(await p.locator('[data-tp193-panel]').count(),0,'legacy workouts do not advertise the new capability');
 assert.deepEqual(errors,[]);console.log('PASS #193 phone UI: four languages, source stopwatch freeze, pending/commit gates, read-only watch mirror, safe navigation, independent watch resume, fresh paused return and explicit phone continuation');
 }finally{await browser?.close();await new Promise(r=>server.close(r));}})().catch(e=>{console.error(e);process.exitCode=1;});
