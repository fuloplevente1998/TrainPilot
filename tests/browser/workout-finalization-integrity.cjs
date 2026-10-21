'use strict';
const assert=require('node:assert/strict');
const fs=require('node:fs'),path=require('node:path'),http=require('node:http');
const {chromium}=require('playwright');
const root=path.resolve(__dirname,'../../www');
const server=http.createServer((req,res)=>{
 const file=path.resolve(root,'.'+new URL(req.url,'http://local').pathname.replace(/\/$/,'/index.html'));
 if(!file.startsWith(root+path.sep)){res.writeHead(403);return res.end();}
 fs.readFile(file,(error,data)=>{if(error){res.writeHead(404);return res.end();}res.setHeader('Content-Type',file.endsWith('.js')?'text/javascript':file.endsWith('.css')?'text/css':file.endsWith('.html')?'text/html':'text/plain');res.end(data);});
});
(async()=>{
 await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));
 let browser;let checks=0;
 try{
  browser=await chromium.launch({headless:true,executablePath:process.env.TRAINPILOT_CHROMIUM||undefined,args:['--no-sandbox']});
  async function fixture(){
   const page=await browser.newPage({viewport:{width:393,height:873},locale:'hu-HU'});
   page.errors=[];page.on('pageerror',error=>page.errors.push(error.message));
   await page.addInitScript(()=>localStorage.setItem('repforge:onboarding128',JSON.stringify('skipped')));
   await page.goto('http://127.0.0.1:'+server.address().port);await page.waitForFunction(()=>window.TrainPilotBoot?.finished);
   await page.evaluate(()=>{
    document.querySelector('#tpBoot')?.remove();window.__originalAlert=window.alert;window.__alerts=[];window.alert=text=>__alerts.push(String(text));
    startWorkout(activeProgram().days[0].id,null,activeProgram().id);
    for(const e of state.session.exercises)for(const s of e.sets){s.done=true;s.reps='10';s.previousValues228={weight:s.weight,reps:s.reps};}
    state.session.scheduleId='integrity-schedule';db.set('scheduled',[{id:'integrity-schedule',status:'planned'}]);
    state.current=state.session.exercises.length-1;persistDraft();renderWorkout();
    window.__originalSet=Storage.prototype.setItem;
   });
   return page;
  }
  for(const mode of ['all','history','scheduled','draft-first','draft-cleanup','rollback']){
   const page=await fixture();
   const failed=await page.evaluate(async mode=>{
    const oldHistory=localStorage.getItem('repforge:history'),oldSchedule=localStorage.getItem('repforge:scheduled');
    const id=state.session.syncId||state.session.started;let historyWrites=0;
    Storage.prototype.setItem=function(k,v){
     if(k==='repforge:history')historyWrites++;
     const fail=mode==='all'||mode===k.slice('repforge:'.length)||mode==='draft-first'&&k==='repforge:draft'||
      mode==='draft-cleanup'&&k==='repforge:draft'&&v==='null'||mode==='rollback'&&(k==='repforge:scheduled'||k==='repforge:history'&&historyWrites>1);
     if(fail)throw new DOMException('Injected '+mode,'QuotaExceededError');return __originalSet.call(this,k,v);
    };
    const result=await finishWorkout({wearConfirmed:true});Storage.prototype.setItem=__originalSet;
    const failed={result,history:history().length,active:!!state.session,draft:!!db.get('draft',null),alerts:__alerts.slice(),sameHistory:oldHistory===localStorage.getItem('repforge:history'),sameSchedule:oldSchedule===localStorage.getItem('repforge:scheduled')};
    const retry=await finishWorkout({wearConfirmed:true});
    return {failed,retry,after:{history:history().length,ids:history().map(x=>x.syncId||x.started),active:!!state.session,draft:db.get('draft',null),schedule:scheduled()[0].status,internal:history()[0].exercises.some(e=>e.sets.some(s=>'previousValues228'in s)),pending:'finalizationPending'in history()[0]},id};
   },mode);
   assert.equal(failed.failed.result,false,mode);assert.equal(failed.failed.active,true,mode);assert.equal(failed.failed.draft,true,mode);
   assert.ok(failed.failed.alerts.length>0,mode+' has visible error feedback');
   if(!['draft-cleanup','rollback'].includes(mode)){assert.equal(failed.failed.sameHistory,true,mode+' rolls back history');assert.equal(failed.failed.sameSchedule,true,mode+' rolls back calendar');}
   assert.equal(failed.retry,true,mode+' retry succeeds');assert.equal(failed.after.history,1,mode+' retry cannot duplicate history');assert.deepEqual(failed.after.ids,[failed.id]);
   assert.equal(failed.after.active,false);assert.equal(failed.after.draft,null);assert.equal(failed.after.schedule,'completed');
   assert.equal(failed.after.internal,false,'UI-only prefill data stays out of history');assert.equal(failed.after.pending,false,'finish recovery marker stays out of history');
   assert.deepEqual(page.errors,[]);checks++;await page.close();
  }
  {
   const page=await fixture(),dialogs=[];page.on('dialog',async dialog=>{dialogs.push(dialog.message());await dialog.accept();});
   await page.evaluate(()=>{
    window.alert=__originalAlert;
    Storage.prototype.setItem=function(k,v){if(k==='repforge:history')throw new DOMException('UI save quota','QuotaExceededError');return __originalSet.call(this,k,v);};
   });
   await page.locator('.tp16-workout-next').click();
   await page.waitForFunction(()=>!TrainPilotWorkoutFinalization.isFinishing());
   const value=await page.evaluate(()=>({active:!!state.session,rows:history().length,message:document.querySelector('#tp2628Dialog')?.innerText||'',failure:TrainPilotWorkoutFinalization.failure()}));
   assert.equal(value.active,true);assert.equal(value.rows,0);assert.equal(value.failure.name,'QuotaExceededError');
   assert.match([value.message,...dialogs].join(' '),/Az edzés lezárása nem sikerült/,'real finish button shows the storage failure');
   assert.deepEqual(page.errors,[],'real button must not produce an unhandled promise rejection');checks++;await page.close();
  }
  for(const key of ['history','scheduled']){
   const page=await fixture();const value=await page.evaluate(async key=>{
    const raw='[{"recoverable":';localStorage.setItem('repforge:'+key,raw);const result=await finishWorkout({wearConfirmed:true});
    return {result,raw:localStorage.getItem('repforge:'+key),active:!!state.session,draft:!!db.get('draft',null),alerts:__alerts};
   },key);
   assert.equal(value.result,false);assert.equal(value.raw,'[{"recoverable":');assert.equal(value.active,true);assert.equal(value.draft,true);assert.match(value.alerts.join(' '),/sérült/);checks++;await page.close();
  }
  // A running bilateral clock must flush before the immutable history snapshot;
  // stopping it after clearing the draft would resurrect a stale draft.
  {
   const page=await fixture();const value=await page.evaluate(async()=>{
    state.session.exercises=[{...byId('side-plank'),sets:[{set:1,setId:'side-save',weight:0,reps:'7',done:true,leftSeconds:0,rightSeconds:7}]}];state.current=0;
    tp1481SideStopwatch={exerciseIndex:0,setIndex:0,side:'left',elapsedMs:2000,startedAt:Date.now()-3100,running:true,timer:null,lastSecond:-1};persistDraft();
    const result=await finishWorkout({wearConfirmed:true});return {result,set:history()[0]?.exercises[0].sets[0],draft:db.get('draft',null),running:tp1481SideStopwatch.running};
   });
   assert.equal(value.result,true);assert.ok(value.set.leftSeconds>=5);assert.equal(value.set.rightSeconds,7);assert.equal(value.draft,null);assert.equal(value.running,false);checks++;await page.close();
  }
  // A save that reached history but failed draft cleanup is still pending after
  // reload, and a replayed watch command must not receive a premature receipt.
  {
   const page=await fixture();await page.evaluate(async()=>{
    Storage.prototype.setItem=function(k,v){if(k==='repforge:draft'&&v==='null')throw new DOMException('cleanup','QuotaExceededError');return __originalSet.call(this,k,v);};
    await finishWorkout({wearConfirmed:true});Storage.prototype.setItem=__originalSet;
   });
   await page.reload();await page.waitForFunction(()=>window.TrainPilotBoot?.finished);
   const value=await page.evaluate(async()=>{
    window.alert=()=>{};const pending=db.get('draft',null).session,id=pending.syncId||pending.started;
    const queue=[{commandId:'pending-cleanup',workoutId:id,action:'finishWorkout',confirmed:true,sequence:1,createdAt:Date.now()}],receipts=[];
    window.Capacitor={isNativePlatform:()=>true,Plugins:{WearSync:{publish:async()=>({revision:1}),publishHome:async()=>({}),pendingCommands:async()=>({commands:queue.slice()}),ackCommand:async r=>{receipts.push(r);queue.length=0;}}}};
    const original=Storage.prototype.setItem;Storage.prototype.setItem=function(k,v){if(k==='repforge:draft'&&v==='null')throw new DOMException('cleanup','QuotaExceededError');return original.call(this,k,v);};
    await TrainPilotWearSync.drainCommands();Storage.prototype.setItem=original;
    const before={receipts:receipts.slice(),pending:!!db.get('draft',null),rows:history().length};
    await TrainPilotWearSync.drainCommands();return {before,receipts,rows:history().length,draft:db.get('draft',null),session:!!state.session};
   });
   assert.equal(value.before.receipts.length,0,'no saved receipt while draft cleanup is failing');assert.equal(value.before.pending,true);assert.equal(value.before.rows,1);
   assert.equal(value.receipts.length,1);assert.equal(value.receipts[0].status,'saved');assert.equal(value.rows,1);assert.equal(value.draft,null);assert.equal(value.session,false);checks++;await page.close();
  }
  // Native close failure leaves a retryable draft and cannot acknowledge success.
  {
   const page=await fixture();const value=await page.evaluate(async()=>{
    const session=state.session,id=session.syncId||session.started;session.phoneExecution162={generation:811,revision:1,sequence:0};persistDraft();
    const native={active:true,closes:0,blocked:[]};window.__native=native;
    window.Capacitor={isNativePlatform:()=>true,Plugins:{WearSync:{
     executionStatus:async()=>({snapshot:{active:native.active,workoutId:id,generation:811,executionRevision:1},events:[],timer:{}}),
     blockExecution:async({blocked})=>native.blocked.push(blocked),
     closeExecution:async()=>{native.closes++;if(native.closes===1)throw Error('native close failed');native.active=false;},
     publish:async()=>({revision:1}),publishHome:async()=>({}),pendingCommands:async()=>({commands:[]})
    }}};
    const first=await finishWorkout({wearConfirmed:true});const before={first,active:!!state.session,draft:!!db.get('draft',null),rows:history().length,native:native.active};
    const second=await finishWorkout({wearConfirmed:true});return {before,second,rows:history().length,draft:db.get('draft',null),active:!!state.session,native};
   });
   assert.equal(value.before.first,false);assert.equal(value.before.active,true);assert.equal(value.before.draft,true);assert.equal(value.before.native,true);
   assert.equal(value.second,true);assert.equal(value.rows,1);assert.equal(value.draft,null);assert.equal(value.active,false);assert.equal(value.native.active,false);assert.deepEqual(value.native.blocked,[true,false,true,false]);checks++;await page.close();
  }
  // Await the partial-workout dialog while holding the native fence. Double
  // clicks join one attempt, cancellation leaves calendar/native execution open.
  for(const confirm of [false,true]){
   const page=await fixture();await page.evaluate(()=>{
    state.session.exercises[0].sets[0].done=false;persistDraft();window.__fence={blocked:false,closes:0};
    window.__originalFence=TrainPilotPhoneExecution162.finishFence;
    TrainPilotPhoneExecution162.finishFence=async work=>{__fence.blocked=true;try{const value=await work();if(value===true)__fence.closes++;return value;}finally{__fence.blocked=false;}};
    window.__one=finishWorkout();window.__two=finishWorkout();
   });
   await page.locator('#tp2628Dialog').waitFor({state:'visible'});
   assert.equal(await page.evaluate(()=>__fence.blocked),true,'native fence awaits confirmation');assert.equal(await page.evaluate(()=>__one===__two),true,'double click joins the same pending attempt');
   await page.locator(confirm?'[data-tp2628-confirm]':'[data-tp2628-cancel]').click();
   const value=await page.evaluate(async()=>({result:await __one,rows:history().length,schedule:scheduled()[0].status,active:!!state.session,fence:__fence}));
   assert.equal(value.result,confirm);assert.equal(value.rows,confirm?1:0);assert.equal(value.schedule,confirm?'completed':'planned');assert.equal(value.active,!confirm);assert.equal(value.fence.blocked,false);assert.equal(value.fence.closes,confirm?1:0);checks++;await page.close();
  }
  console.log('PASS workout finalization integrity: '+checks+' cases; quota boundaries, rollback failure, retry/reload, corrupt data, native close, withheld receipts, confirmation and metadata.');
 }finally{await browser?.close();await new Promise(resolve=>server.close(resolve));}
})().catch(error=>{console.error(error);process.exitCode=1;});
