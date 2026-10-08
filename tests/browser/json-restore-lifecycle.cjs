'use strict';
const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),http=require('node:http'),{chromium}=require('playwright');
const root=path.resolve('www'),server=http.createServer((req,res)=>{
 const file=path.resolve(root,'.'+(new URL(req.url,'http://local').pathname==='/'?'/index.html':new URL(req.url,'http://local').pathname));
 if(!file.startsWith(root+path.sep)){res.writeHead(403);return res.end()}
 fs.readFile(file,(e,d)=>{if(e){res.writeHead(404);return res.end()}res.setHeader('Content-Type',file.endsWith('.js')?'text/javascript':file.endsWith('.css')?'text/css':file.endsWith('.html')?'text/html':'application/octet-stream');res.end(d)});
});
(async()=>{
 await new Promise(r=>server.listen(0,'127.0.0.1',r));let browser;
 try{
  browser=await chromium.launch({headless:true,executablePath:process.env.TRAINPILOT_CHROMIUM||undefined,args:['--no-sandbox']});
  const page=await browser.newPage({viewport:{width:393,height:873},locale:'hu-HU'}),errors=[];
  page.on('pageerror',e=>errors.push(e.message));
  await page.addInitScript(()=>localStorage.setItem('repforge:onboarding128',JSON.stringify('skipped')));
  await page.goto('http://127.0.0.1:'+server.address().port+'/');await page.waitForFunction(()=>TrainPilotBoot.finished&&TrainPilotHealthJournal);
  await page.evaluate(async()=>{
   const projection={schemaVersion:1,days:{},preferences:{autoOnOpen:true}},p=activeProgram(),day=p.days[0];
   window.restoreProbe={opens:0,wearReads:0,alerts:[],prepared:[],failInstall:false,projectionReads:0};
   const probe=restoreProbe;
   window.Capacitor={isNativePlatform:()=>true,Plugins:{
    NativeFiles:{open:()=>{probe.opens++;return new Promise(resolve=>probe.select=resolve)}},
    HealthJournal:{pendingRestore:async()=>({}),initialize:async()=>projection,getProjection:async()=>{probe.projectionReads++;return projection},prepareRestore:async({snapshot})=>{probe.prepared.push(snapshot);return {token:'restore-probe'}},installRestore:async()=>{if(probe.failInstall)throw Error('install failed')},finishRestore:async()=>{}},
    WearSync:{publish:async()=>{},publishHome:async()=>{},clear:async()=>{},pendingCommands:async()=>{probe.wearReads++;return {commands:[probe.command]}},ackCommand:async()=>{probe.acked=true}}
   }};
   window.alert=message=>probe.alerts.push(String(message));
   await TrainPilotHealthJournal.ready();
   const row={id:'morning-backup',workout:day.id,dayId:day.id,programId:p.id,started:'2026-10-08T08:00:00.000Z',finished:'2026-10-08T08:30:00.000Z',exercises:[{id:'db-squat',hu:'Guggolás',loadType:'per_hand',repUnit:'ism.',sets:[{set:1,weight:8,reps:'8',done:true}]}]};
   db.set('history',[row]);const data=makeBackup();data.appVersion='1.2.7';data.healthIncluded=true;data.healthLedger={days:{'2026-10-08':{steps:1200}}};data.recoveryHistory=[{day:'2026-10-08',hrvRmssdMs:40}];delete data.healthJournal;
   probe.backup=JSON.stringify(data);db.set('history',[{...row,id:'current-data'}]);
   probe.command={commandId:'pending-watch-start',action:'startWorkout',workoutId:'2026-10-08T09:00:00.000Z',started:'2026-10-08T09:00:00.000Z',programId:p.id,dayId:day.id};
   go('settings');probe.promise=chooseImport();
  });
  assert.equal(await page.evaluate(()=>TrainPilotBackupBusy),true,'JSON picker reserves the same lock as ZIP');
  const resume=await page.evaluate(async()=>{
   const before=restoreProbe.projectionReads;document.dispatchEvent(new Event('visibilitychange'));await TrainPilotHealthJournal.auto();await TrainPilotWearSync.drainCommands();
   return {syncing:TrainPilotHealthJournal.isSyncing(),reads:restoreProbe.projectionReads-before,wear:restoreProbe.wearReads,session:!!state.session};
  });
  assert.deepEqual(resume,{syncing:false,reads:0,wear:0,session:false},'resume cannot start automatic Health sync or apply watch commands during restore');
  await page.evaluate(()=>restoreProbe.select({data:restoreProbe.backup}));
  await page.locator('[data-tp2628-confirm]').click();await page.evaluate(()=>restoreProbe.promise);
  const restored=await page.evaluate(()=>({id:history()[0].id,busy:TrainPilotBackupBusy,owner:tp105RestoreOwner,legacy:restoreProbe.prepared[0].days,alerts:restoreProbe.alerts}));
  assert.equal(restored.id,'morning-backup');assert.equal(restored.busy,false);assert.equal(restored.owner,null);assert.equal(restored.legacy[0].channel,'legacy');assert.equal(restored.legacy[0].data.steps,1200);assert.equal(restored.legacy[0].data.hrvRmssdMs,40);assert.ok(restored.alerts.some(m=>m.includes('Visszatöltés kész')));
  const snapshot=()=>page.evaluate(()=>JSON.stringify(Object.fromEntries(Object.keys(localStorage).filter(k=>k.startsWith('repforge:')).sort().map(k=>[k,localStorage.getItem(k)]))));
  for(const mode of ['picker-cancel','invalid-json','confirm-cancel','install-failure']){
   const before=await snapshot();
   await page.evaluate(mode=>{restoreProbe.failInstall=mode==='install-failure';restoreProbe.promise=chooseImport();restoreProbe.select(mode==='picker-cancel'?{cancelled:true}:{data:mode==='invalid-json'?'not JSON':restoreProbe.backup});},mode);
   if(mode==='confirm-cancel'||mode==='install-failure')await page.locator(mode==='confirm-cancel'?'[data-tp2628-cancel]':'[data-tp2628-confirm]').click();
   await page.evaluate(()=>restoreProbe.promise);assert.equal(await page.evaluate(()=>TrainPilotBackupBusy),false,mode+' releases restore lock');assert.equal(await snapshot(),before,mode+' preserves every previous local key');
  }
  const blocked=await page.evaluate(async()=>{
   const opens=restoreProbe.opens;state.session={started:'active'};await chooseImport();state.session=null;state.health.busy=true;await chooseImport();state.health.busy=false;
   return restoreProbe.opens-opens;
  });assert.equal(blocked,0,'actual active workout or Health sync is still protected');
  // Browser file reading and the confirmation also own the lock, not just native JSON.
  await page.evaluate(()=>{restoreProbe.failInstall=false;restoreProbe.promise=importData({size:100,text:()=>new Promise(resolve=>restoreProbe.readFile=resolve)});});
  assert.equal(await page.evaluate(()=>TrainPilotBackupBusy),true);await page.evaluate(()=>chooseImport());assert.equal(await page.evaluate(()=>TrainPilotBackupBusy),true,'a second import cannot release the first lock');await page.evaluate(()=>restoreProbe.readFile(restoreProbe.backup));await page.locator('[data-tp2628-cancel]').click();await page.evaluate(()=>restoreProbe.promise);assert.equal(await page.evaluate(()=>TrainPilotBackupBusy),false);
  // Even the cooldown projection-only refresh is an in-flight Health operation.
  await page.evaluate(async()=>{
   await TrainPilotHealthJournal.auto();const bridge=Capacitor.Plugins.HealthJournal;restoreProbe.projection=bridge.getProjection;
   bridge.getProjection=()=>new Promise(resolve=>restoreProbe.releaseAuto=resolve);restoreProbe.autoFlight=TrainPilotHealthJournal.auto();
  });
  await page.waitForFunction(()=>!!restoreProbe.releaseAuto);
  const healthFlight=await page.evaluate(async()=>{const opens=restoreProbe.opens;await chooseImport();return {syncing:TrainPilotHealthJournal.isSyncing(),opened:restoreProbe.opens-opens}});
  assert.deepEqual(healthFlight,{syncing:true,opened:0},'cooldown Health refresh must finish before restore starts');
  await page.evaluate(async()=>{const bridge=Capacitor.Plugins.HealthJournal;restoreProbe.releaseAuto(await restoreProbe.projection());await restoreProbe.autoFlight;bridge.getProjection=restoreProbe.projection;});
  const inFlight=await page.evaluate(async()=>{
   const bridge=Capacitor.Plugins.WearSync,read=bridge.pendingCommands;bridge.pendingCommands=()=>new Promise(resolve=>restoreProbe.releaseCommand=resolve);
   const pending=TrainPilotWearSync.drainCommands(),opens=restoreProbe.opens;await chooseImport();const blocked=restoreProbe.opens===opens&&TrainPilotWearSync.isApplyingCommand();
   restoreProbe.releaseCommand({commands:[]});await pending;bridge.pendingCommands=read;return blocked;
  });assert.equal(inFlight,true,'in-flight Wear command owns its state until complete');
  // Deferred Wear commands resume after the completed/cancelled restore.
  await page.evaluate(()=>TrainPilotWearSync.drainCommands());assert.equal(await page.evaluate(()=>restoreProbe.acked),true);assert.equal(await page.evaluate(()=>state.session.syncId), '2026-10-08T09:00:00.000Z');
  assert.deepEqual(errors,[]);
  console.log('PASS native JSON restore: picker/resume/confirmation lock, pre-Samsung backup legacy Health migration, deferred Wear command, cancellation/invalid JSON/install rollback, browser file read and active workout/sync protection.');
 }finally{await browser?.close();server.close()}
})().catch(e=>{console.error(e);process.exitCode=1;server.close()});
