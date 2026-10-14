const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),http=require('node:http');
const {chromium}=require('playwright'),root=path.resolve('www');
const server=http.createServer((req,res)=>{const u=new URL(req.url,'http://local'),file=path.resolve(root,'.'+(u.pathname==='/'?'/index.html':u.pathname));if(!file.startsWith(root+path.sep)){res.writeHead(403);res.end();return;}fs.readFile(file,(e,d)=>{if(e){res.writeHead(404);res.end();return;}res.setHeader('Content-Type',file.endsWith('.js')?'text/javascript':file.endsWith('.css')?'text/css':file.endsWith('.html')?'text/html':'application/octet-stream');res.end(d);});});
(async()=>{await new Promise(r=>server.listen(0,'127.0.0.1',r));let browser;try{
 browser=await chromium.launch({headless:true,executablePath:process.env.TRAINPILOT_CHROMIUM||undefined,args:['--no-sandbox']});
 const page=await browser.newPage({viewport:{width:393,height:873},locale:'hu-HU'});
 await page.addInitScript(()=>localStorage.setItem('repforge:onboarding128',JSON.stringify('skipped')));
 await page.goto('http://127.0.0.1:'+server.address().port+'/');await page.waitForFunction(()=>window.TrainPilotBoot?.finished);
 await page.evaluate(()=>{
  window.__feedbackTrial={commands:[],acks:[]};
  window.Capacitor={isNativePlatform:()=>true,Plugins:{WearSync:{publish:async()=>{},publishHome:async()=>{},clear:async()=>{},
   startWatchMeasurement:async()=>({opened:true}),pendingCommands:async()=>({commands:__feedbackTrial.commands.slice()}),
   ackCommand:async({commandId})=>{__feedbackTrial.acks.push(commandId);__feedbackTrial.commands=__feedbackTrial.commands.filter(c=>c.commandId!==commandId);}}}};
  db.set('history',[]);db.set('draft',null);db.set('wearClosedWorkouts',[]);
 });
 async function finishPhone(){return page.evaluate(async()=>{
  await TrainPilotQuickWorkout.start('db-floor-press');
  for(const e of state.session.exercises)for(const s of e.sets){s.done=true;s.reps='10';}
  const id=state.session.syncId||state.session.started;persistDraft();await finishWorkout();return id;
 });}
 const phoneId=await finishPhone();
 assert.equal(await page.locator('.rf232-effort button').count(),5);
 await page.getByRole('button',{name:'4 Kicsit nehéz'}).click();
 const phone=await page.evaluate(()=>({row:history()[0],tab:state.tab,prompt:!!document.querySelector('.tp122-feedback'),pending:TrainPilotWearSync.makeHomeSnapshot().pendingFeedback}));
 assert.equal(phone.row.feedback.effort,'challenging');assert.equal(phone.row.feedback.effortScore,4);assert.equal(phone.row.feedback.rating,'hard');
 assert.equal(phone.tab,'home');assert.equal(phone.prompt,false);assert.equal(phone.pending,null);
 await page.evaluate(()=>go('history'));
 await page.locator('details.history').first().evaluate(node=>node.open=true);
 await page.getByRole('button',{name:'Edzésértékelés · Kicsit nehéz'}).click();
 assert.equal(await page.locator('.tp122-feedback').count(),1,'saved suggestion remains reachable from Journal');
 await page.getByRole('button',{name:'4 Kicsit nehéz'}).click();
 assert.equal(await page.evaluate(()=>tp140Occurrence({workout:history()[0],exercise:history()[0].exercises[0]},{}).effort),'challenging');
 assert.equal(await page.evaluate(()=>tp140Occurrence({workout:history()[0],exercise:{effort:'light',sets:[]}},{}).effort),'light','exercise answer has priority');
 const watch=await page.evaluate(async()=>{
  await TrainPilotQuickWorkout.start('goblet-squat');for(const e of state.session.exercises)for(const s of e.sets){s.done=true;s.reps='12';}persistDraft();
  const id=state.session.syncId,command={commandId:'rated-finish',workoutId:id,action:'finishWorkout',confirmed:true,finishedAt:new Date().toISOString(),feedback:{effort:'light'},sequence:1};
  __feedbackTrial.commands=[command];await TrainPilotWearSync.drainCommands();
  __feedbackTrial.commands=[{...command,commandId:'rated-replay',feedback:{effort:'hard'},sequence:2}];await TrainPilotWearSync.drainCommands();
  return {row:history()[0],count:history().length,prompt:!!document.querySelector('.tp122-feedback'),pending:TrainPilotWearSync.makeHomeSnapshot().pendingFeedback};
 });
 assert.equal(watch.count,2);assert.equal(watch.row.feedback.effort,'light');assert.equal(watch.row.feedback.source,'watch');assert.equal(watch.prompt,false);assert.equal(watch.pending,null);
 const sharedId=await finishPhone();
 assert.equal(await page.evaluate(()=>TrainPilotWearSync.makeHomeSnapshot().pendingFeedback.workoutId),sharedId);
 await page.evaluate(async(id)=>{__feedbackTrial.commands=[{commandId:'watch-answer',action:'saveWorkoutFeedback',workoutId:id,feedback:{effort:'good'},sequence:3}];await TrainPilotWearSync.drainCommands();},sharedId);
 assert.equal(await page.evaluate(()=>state.tab),'home');assert.equal(await page.locator('.tp122-feedback').count(),0);
 assert.equal(await page.evaluate(()=>history()[0].feedback.effortScore),3);
 await page.evaluate(async(id)=>{__feedbackTrial.commands=[{commandId:'answer-replay',action:'saveWorkoutFeedback',workoutId:id,feedback:{effort:'hard'},sequence:4}];await TrainPilotWearSync.drainCommands();},sharedId);
 assert.equal(await page.evaluate(()=>history()[0].feedback.effort),'good');
 assert.equal(await page.evaluate(()=>TrainPilotWorkoutFeedback.accept('other-workout',{effort:'hard'})),false);
 assert.equal(await page.evaluate(id=>TrainPilotWorkoutFeedback.accept(id,{effort:'invalid'}),phoneId),false);
 await finishPhone();await page.getByRole('button',{name:'Most kihagyom'}).click();
 assert.equal(await page.evaluate(()=>history()[0].feedback.skipped),true);assert.equal(await page.evaluate(()=>state.tab),'home');
 await finishPhone();await page.getByRole('button',{name:'Fájdalom / kellemetlenség',exact:true}).click();
 assert.equal(await page.evaluate(()=>history()[0].feedback.rating),'pain');
 await page.reload();await page.waitForFunction(()=>window.TrainPilotBoot?.finished);
 assert.equal(await page.evaluate(()=>history()[0].feedback.rating),'pain','answer survives restart');
 assert.equal(await page.evaluate(()=>TrainPilotWearSync.makeHomeSnapshot().pendingFeedback),null);
 console.log('PASS workout completion feedback: five scores, automatic home, shared watch answer, durable storage, replay protection, exercise priority and skip/pain.');
}finally{await browser?.close();server.close();}})().catch(e=>{console.error(e);process.exitCode=1;});
