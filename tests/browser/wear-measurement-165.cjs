'use strict';
const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),http=require('node:http'),{chromium}=require('playwright');
const root=path.resolve('www'),server=http.createServer((req,res)=>{const pathname=new URL(req.url,'http://local').pathname,file=path.resolve(root,'.'+(pathname==='/'?'/index.html':pathname));if(!file.startsWith(root+path.sep)){res.writeHead(403);return res.end()}fs.readFile(file,(e,data)=>{if(e){res.writeHead(404);return res.end()}res.setHeader('Content-Type',file.endsWith('.js')?'text/javascript':file.endsWith('.css')?'text/css':file.endsWith('.html')?'text/html':'application/octet-stream');res.end(data)});});
(async()=>{
 await new Promise(r=>server.listen(0,'127.0.0.1',r));let browser;
 try {
  browser=await chromium.launch({headless:true,args:['--no-sandbox']});
  const page=await browser.newPage({viewport:{width:360,height:800},locale:'hu-HU'}),errors=[];page.on('pageerror',e=>errors.push(e.message));
  await page.addInitScript(()=>localStorage.setItem('repforge:onboarding128',JSON.stringify('skipped')));
  await page.goto('http://127.0.0.1:'+server.address().port+'/');await page.waitForFunction(()=>TrainPilotBoot.finished);
  await page.evaluate(()=>{db.set('draft',null);go('plan');startWorkout(activeProgram().days[0].id);TrainPilotWearMeasurement165.waiting(state.session.syncId||state.session.started);window.wear165Id=state.session.syncId||state.session.started;window.wear165Revision=0;window.wear165Receive=(phase,extra={})=>TrainPilotWearMeasurement165.receive({schema:1,workoutId:wear165Id,watchId:'watch',revision:++wear165Revision,state:phase,updatedAt:Date.now(),...extra});});
  assert.match(await page.locator('[data-wear-measurement165]').innerText(),/Edzés átadása/);
  for(const language of ['hu','en','de','ro'])for(const width of [320,393]){
   await page.setViewportSize({width,height:873});
   await page.evaluate(lang=>{db.set('language',lang);rf212SetLang(lang);wear165Receive('permissionRequired');renderWorkout();},language);
   const panel=page.locator('[data-wear-measurement165]');assert.equal(await panel.count(),1);assert.equal(await panel.locator('button').count(),1);
   assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth>innerWidth),false,'status panel must fit narrow screens');
  }
  await page.evaluate(async()=>{rf212SetLang('hu');renderWorkout();let payload={schema:1,workoutId:wear165Id,watchId:'watch',revision:++wear165Revision,state:'active',updatedAt:Date.now(),summary:{schema:1,source:'wear_health_services',workoutId:wear165Id,watchId:'sensor',revision:1,state:'active',heartRate:109,totalCalories:7}};window.wear165Payload=payload;window.Capacitor={isNativePlatform:()=>true,Plugins:{WearSync:{pendingCommands:async()=>({commands:[],liveHealth:wear165Payload}),ackCommand:async()=>{},publish:async()=>{},clear:async()=>{},publishHome:async()=>{},startWatchMeasurement:async()=>({opened:true})}}};await TrainPilotWearSync.drainCommands();});
  assert.match(await page.locator('[data-wear-measurement165]').innerText(),/109 bpm · 7 kcal/);
  const before=await page.evaluate(()=>JSON.stringify(state.session.exercises));
  await page.evaluate(()=>wear165Receive('paused'));assert.match(await page.locator('[data-wear-measurement165]').innerText(),/szünetel/);
  assert.equal(await page.evaluate(()=>JSON.stringify(state.session.exercises)),before,'measurement messages do not change sets');
  await page.evaluate(()=>wear165Receive('error',{message:'<img src=x onerror=alert(1)>'}));assert.equal(await page.locator('[data-wear-measurement165] img').count(),0,'remote error text is escaped');
  await page.locator('[data-wear-measurement165] button').click();assert.match(await page.locator('[data-wear-measurement165]').innerText(),/Edzés átadása/);
  await page.evaluate(()=>{go('home');resumeDraft();});assert.equal(await page.locator('[data-wear-measurement165]').count(),1,'navigation retains one status panel');
  await page.evaluate(()=>{
   for(const e of state.session.exercises)for(const s of e.sets){s.done=true;s.reps='10';}
   finishWorkout({wearConfirmed:true});go('history');
  });
  assert.equal(await page.evaluate(()=>history()[0].healthWear129.totalCalories),7,'phone save keeps the live Wear measurement');
  await page.evaluate(async()=>{
   wear165Payload={...wear165Payload,revision:++wear165Revision,state:'ended',updatedAt:Date.now(),summary:{...wear165Payload.summary,revision:2,state:'ended',averageHeartRate:109,totalCalories:9}};
   await TrainPilotWearSync.drainCommands();
  });
  assert.equal(await page.evaluate(()=>history()[0].healthWear129.totalCalories),9,'late live final updates the saved Journal');
  assert.equal(await page.evaluate(()=>history().length),1);
  assert.equal(await page.evaluate(()=>state.session),null);
  assert.match(await page.evaluate(()=>rfHistoryHealthHtml(0)),/9 kcal/);
  assert.deepEqual(errors,[]);console.log('PASS Wear measurement UI: 8 language/width combinations, real live pulse/energy, paused/error state, escaped errors, manual handoff retry and unchanged sets');
 } finally {await browser?.close();server.close();}
})().catch(e=>{console.error(e);process.exitCode=1;server.close()});
