'use strict';
const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),http=require('node:http'),{chromium}=require('playwright');
const root=path.resolve('www');
const server=http.createServer((req,res)=>{
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
  await page.goto('http://127.0.0.1:'+server.address().port+'/');await page.waitForFunction(()=>TrainPilotBoot.finished&&TrainPilot137Motion);
  const replay=await page.evaluate(async()=>{
   let commands=[],acks=[],homes=[],failures=0;
   const bridge={publish:async()=>{},clear:async()=>{},publishHome:async({snapshot})=>homes.push(snapshot),pendingCommands:async()=>({commands:commands.slice()}),ackCommand:async({commandId})=>{if(failures-->0)throw Error('offline ACK');acks.push(commandId);commands=commands.filter(c=>c.commandId!==commandId)}};
   window.Capacitor={isNativePlatform:()=>true,Plugins:{WearSync:bridge}};
   window.tpReplay={set:next=>{commands=next},acks,homes};
   db.set('history',[]);db.set('draft',null);db.set('wearClosedWorkouts',[]);state.session=null;
   const started=new Date(Date.now()-300000).toISOString().replace('Z','456789Z'),p=activeProgram(),day=p.days[0];
   commands=[{commandId:'watch-start-nanos',workoutId:started,action:'startWorkout',started,dayId:day.id,programId:p.id,createdAt:1}];
   await TrainPilotWearSync.drainCommands();
   const final=TrainPilotWearSync.makeSnapshot();final.exercises[0].sets[0].reps='12';final.exercises[0].sets[0].done=true;
   const finish={commandId:'watch-finish-nanos',workoutId:started,action:'finishWorkout',confirmed:true,finalSnapshot:final,finishedAt:new Date().toISOString(),createdAt:2};
   commands=[finish];failures=50;
   for(let i=0;i<51;i++)await TrainPilotWearSync.drainCommands();
   const result={count:history().length,syncId:history()[0]?.syncId,status:db.get('wearWorkoutResult')?.status,pending:commands.length,acks:acks.slice(),receipt:homes.at(-1)?.workoutResult?.status};
   // Recreate the old-build failure: identical rows with a nanosecond timestamp,
   // no stable identity, and the terminal DataItem still pending on the phone.
   const original=structuredClone(db.get('history')[0]);delete original.syncId;original.started=started;
   const rows=Array.from({length:56},(_,i)=>{const row=structuredClone(original);row.exercises[0].rf152Recommendation={action:i?'hold':'start',historyCount:i,text:'Derived advice '+i};return row});
   rows[55].photos=[{id:'original-photo',updatedAt:1}];rows[5].photos=[{id:'other-photo',updatedAt:2}];rows[7].feedback={rating:'right'};
   const unrelated=structuredClone(original);unrelated.started=new Date(Date.parse(started)-60000).toISOString();unrelated.exercises[0].sets[0].reps='8';
   db.set('history',[...rows,unrelated]);db.set('wearClosedWorkouts',[]);db.set('wearWorkoutResult',null);
   commands=[{...finish,commandId:'legacy-pending'}];await TrainPilotWearSync.drainCommands();
   result.legacyCount=history().length;result.legacyReceipt=db.get('wearWorkoutResult')?.status;
   window.tpReplay.finish=finish;window.tpReplay.unrelated=unrelated;
   go('history');return result;
  });
  assert.equal(replay.count,1);assert.ok(replay.syncId.endsWith('456789Z'));assert.equal(replay.status,'saved');assert.equal(replay.receipt,'saved');assert.equal(replay.pending,0);assert.deepEqual(replay.acks,['watch-start-nanos','watch-finish-nanos']);assert.equal(replay.legacyCount,57);assert.equal(replay.legacyReceipt,'saved');
  assert.deepEqual(await page.evaluate(()=>TrainPilotHistoryRepair.scan()),{groups:1,copies:55});
  await page.locator('.tp-history-merge').click();await page.locator('[data-tp2628-cancel]').click();assert.equal(await page.evaluate(()=>history().length),57);
  await page.locator('.tp-history-merge').click();await page.locator('[data-tp2628-confirm]').click();await page.waitForFunction(()=>history().length===2);
  const repair=await page.evaluate(()=>({photos:history()[0].photos.map(p=>p.id).sort(),feedback:history()[0].feedback.rating,unrelated:history()[1].exercises[0].sets[0].reps,copies:TrainPilotHistoryRepair.scan().copies}));
  assert.deepEqual(repair,{photos:['original-photo','other-photo'],feedback:'right',unrelated:'8',copies:0});
  await page.evaluate(()=>db.set('wearClosedWorkouts',[]));
  await page.locator('details.rf263-history').first().evaluate(e=>{e.open=true});
  await page.locator('.tp155-history-delete').first().click();await page.locator('[data-tp2628-confirm]').click();await page.waitForFunction(()=>history().length===1);
  assert.equal(await page.locator('[data-tp2628-confirm]').count(),0,'one confirmation completes deletion without another dialog');
  const afterDelete=await page.evaluate(async()=>{tpReplay.set([{...tpReplay.finish,commandId:'after-user-deletion'}]);await TrainPilotWearSync.drainCommands();return {count:history().length,reps:history()[0].exercises[0].sets[0].reps}});
  assert.deepEqual(afterDelete,{count:1,reps:'8'},'replayed watch finish cannot resurrect a deleted workout');
  // Exercise actual rendered CSS + WAAPI together during rapid page changes.
  for(const theme of ['classicBlue','blue']){
   await page.evaluate(t=>rf200SetTheme(t),theme);
   for(let i=0;i<20;i++){
    const frame=await page.evaluate(i=>{go(['plan','health','home','history'][i%4]);const m=document.querySelector('#app main'),s=getComputedStyle(m);return {opacity:s.opacity,transform:s.transform,animations:m.getAnimations().map(a=>a.animationName||'WAAPI'),mains:document.querySelectorAll('#app main').length}},i);
    assert.equal(frame.opacity,'1',JSON.stringify(frame));assert.equal(frame.transform,'none',JSON.stringify(frame));assert.deepEqual(frame.animations,[],JSON.stringify(frame));assert.equal(frame.mains,1);
   }
  }
  assert.deepEqual(errors,[]);
  console.log('PASS actual phone runtime: nanosecond watch start, 50 failed ACKs, legacy receipt recovery, 55-copy repair/cancel, preserved photos/feedback, one-confirm deletion, no resurrection and opaque rapid navigation.');
 }finally{await browser?.close();server.close()}
})().catch(e=>{console.error(e);process.exitCode=1;server.close()});
