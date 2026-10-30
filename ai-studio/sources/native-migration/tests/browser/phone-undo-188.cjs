'use strict';
const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),http=require('node:http'),{chromium}=require('playwright');
const root=path.resolve('www'),server=http.createServer((req,res)=>{const file=path.resolve(root,'.'+(new URL(req.url,'http://local').pathname==='/'?'/index.html':new URL(req.url,'http://local').pathname));if(!file.startsWith(root+path.sep)){res.writeHead(403);return res.end();}fs.readFile(file,(e,d)=>{if(e){res.writeHead(404);return res.end();}res.setHeader('Content-Type',file.endsWith('.js')?'text/javascript':file.endsWith('.css')?'text/css':'text/html');res.end(d);});});
(async()=>{await new Promise(r=>server.listen(0,'127.0.0.1',r));let browser;try{
 browser=await chromium.launch({executablePath:process.env.TRAINPILOT_CHROMIUM||undefined,args:['--no-sandbox']});const p=await browser.newPage({viewport:{width:393,height:873},locale:'hu-HU'}),errors=[];p.on('pageerror',e=>errors.push(String(e)));p.on('dialog',d=>d.accept());
 await p.addInitScript(()=>localStorage.setItem('repforge:onboarding128',JSON.stringify('skipped')));await p.goto('http://127.0.0.1:'+server.address().port);await p.waitForFunction(()=>TrainPilotBoot.finished);
 await p.evaluate(()=>{window.__commands=[];window.__pending=[];Capacitor={isNativePlatform:()=>true,Plugins:{WearSync:{automaticCommand:async({command})=>{__commands.push(command);},publish:async()=>({revision:1}),publishHome:async()=>({}),pendingCommands:async()=>({commands:__pending.slice()}),ackCommand:async()=>{__pending.shift();}}}};});
 for(const quick of [true,false]){
  await p.evaluate(async quick=>{
   state.session=null;state.workout=null;db.set('draft',null);db.set('settings',{...settings(),automaticWorkout162:false});
   if(quick)await TrainPilotQuickWorkout.start('db-floor-press');else{const program=activeProgram();await startWorkout(program.days[0].id,null,program.id);}
   state.session.healthWear129={heartRate:93,totalCalories:7};state.session.notes='Keep notes';state.session.exercises[0].sets[0].reps='5';await toggleSet(0,0);
  },quick);
  await p.waitForFunction(()=>!document.querySelector('[data-tp188-undo]')?.hidden);
  assert.equal(await p.locator('[data-tp188-undo]').isVisible(),true,'phone undo is visible after actual '+(quick?'quick':'planned')+' recording');
  for(const width of [320,393])for(const lang of ['hu','en','de','ro']){
   await p.setViewportSize({width,height:873});await p.evaluate(lang=>{db.set('language',lang);renderWorkout();},lang);
   assert.ok(await p.evaluate(()=>{const b=document.querySelector('[data-tp188-undo]').getBoundingClientRect();return document.documentElement.scrollWidth<=innerWidth+1&&b.width>40&&b.left>=0&&b.right<=innerWidth&&b.top>=0&&b.bottom<=innerHeight;}),'undo stays visible in fixed controls '+width+'/'+lang);
  }
  await p.setViewportSize({width:393,height:873});await p.evaluate(()=>{db.set('language','hu');renderWorkout();});
  if(quick){fs.mkdirSync('ui-evidence',{recursive:true});await p.screenshot({path:'ui-evidence/phone-undo-188.png'});}
  await p.locator('[data-tp188-undo]').click();await p.waitForFunction(()=>!state.session.exercises[0].sets[0].done);
  assert.deepEqual(await p.evaluate(()=>[state.session.exercises[0].sets[0].reps,state.session.healthWear129.heartRate,state.session.healthWear129.totalCalories,state.session.notes,db.get('draft').session.exercises[0].sets[0].done]),['5',93,7,'Keep notes',false]);
  assert.equal(await p.locator('[data-tp188-undo]').isVisible(),false,'one recording can only be undone once');
 }
 await p.evaluate(async()=>{state.session.exercises[0].sets[0].reps='5';await toggleSet(0,0);state.session.exercises[0].sets[0].weight=17;TrainPilotPhoneUndo188.paint();});
 assert.equal(await p.locator('[data-tp188-undo]').isVisible(),false,'newer edit invalidates the old inverse');
 assert.equal(await p.evaluate(()=>TrainPilotPhoneUndo188.undo()),false);assert.equal(await p.evaluate(()=>state.session.exercises[0].sets[0].weight),17);
 await p.evaluate(()=>{
  const s=state.session;s.timerOwner='watch';s.syncId='watch-phone-undo';s.automatic162={enabled:false,owner:'watch',phase:'work'};s.watchAutomation162={enabled:false,owner:'watch',key:'owner-key',phaseNumber:1,phase:'ready',paused:false};
  const set=s.exercises[0].sets[0];set.done=false;const before=TrainPilotUndo188.values(set);set.done=true;TrainPilotUndo188.capture(s,{commandId:'watch-record188',workoutId:s.syncId,exerciseId:s.exercises[0].id,setId:set.setId},before);persistDraft();renderWorkout();
 });
 await p.locator('[data-tp188-undo]').click();
 assert.equal(await p.evaluate(()=>state.session.exercises[0].sets[0].done),true,'phone waits for watch-owned inverse');
 assert.deepEqual(await p.evaluate(()=>[__commands.at(-1).action,__commands.at(-1).recordId]),['undo','watch-record188']);
 assert.equal(await p.locator('[data-tp188-undo]').isDisabled(),true);
 const count=await p.evaluate(()=>__commands.length);await p.evaluate(()=>TrainPilotPhoneUndo188.undo());assert.equal(await p.evaluate(()=>__commands.length),count,'double click does not send another inverse');
 await p.evaluate(async()=>{
  const s=state.session,r=JSON.parse(JSON.stringify(s.lastRecord188));__pending.push({...r,schema:1,action:'undoSet',commandId:'watch-inverse188',sequence:500,createdAt:Date.now(),automation:{...s.watchAutomation162,paused:true}});await TrainPilotWearSync.drainCommands();
 });
 assert.equal(await p.evaluate(()=>state.session.exercises[0].sets[0].done),false,'watch response applies the guarded shared inverse');
 assert.equal(await p.evaluate(()=>state.session.healthWear129.heartRate),93);
 assert.deepEqual(errors,[]);console.log('PASS phone Undo #188: real quick/planned recording, 8 compact layouts, durable inverse, newer-edit guard, watch-owner waiting and double-click protection');
}finally{await browser?.close();await new Promise(r=>server.close(r));}})().catch(e=>{console.error(e);process.exitCode=1;});
