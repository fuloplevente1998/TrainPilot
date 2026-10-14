'use strict';
const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),http=require('node:http');
const {chromium}=require('playwright');
(async()=>{
 const root=path.resolve(process.env.TRAINPILOT_TEST_WWW||'www');
 const server=http.createServer((req,res)=>{
  const file=path.resolve(root,'.'+new URL(req.url,'http://local').pathname.replace(/\/$/,'/index.html'));
  if(!file.startsWith(root+path.sep)){res.writeHead(403);res.end();return;}
  fs.readFile(file,(error,data)=>{if(error){res.writeHead(404);res.end();return;}res.setHeader('Content-Type',file.endsWith('.js')?'text/javascript':file.endsWith('.css')?'text/css':file.endsWith('.html')?'text/html':'application/octet-stream');res.end(data);});
 });
 await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));let browser;
 try{
  browser=await chromium.launch({headless:true,executablePath:process.env.TRAINPILOT_CHROMIUM||undefined,args:['--no-sandbox']});
  const page=await browser.newPage({viewport:{width:393,height:800},timezoneId:'Europe/Budapest',locale:'hu-HU'}),errors=[];
  page.on('pageerror',error=>errors.push(error.message));
  await page.addInitScript(()=>localStorage.setItem('repforge:onboarding128',JSON.stringify('skipped')));
  await page.goto(`http://127.0.0.1:${server.address().port}`);await page.waitForFunction(()=>window.TrainPilotBoot?.finished);
  await page.evaluate(()=>{
   window.fixture158={id:'source158',syncId:'source158',type:'quick',quickWorkout:true,workout:'A',dayId:'A',programName:'Forráspróba',
    started:'2026-10-14T08:00:00Z',finished:'2026-10-14T08:45:36Z',exercises:[{id:'pushup',hu:'Fekvőtámasz',sets:[{set:1,reps:10,weight:0,done:true}]}],
    healthWear129:{schema:1,source:'wear_health_services',workoutId:'source158',watchId:'watch7',revision:4,totalCalories:390,averageHeartRate:121.2,maxHeartRate:158,steps:0,activeDurationSeconds:2736},
    health240:{windowStart:'2026-10-14T08:00:00.000Z',windowEnd:'2026-10-14T08:45:36.000Z',totalCalories:49,activeCalories:1,source:'all',sources:['com.fitbit.FitbitMobile'],sourceLabels:{'com.fitbit.FitbitMobile':'Fitbit'}}};
   db.set('history',[fixture158]);db.set('draft',null);state.session=null;rfHistoryHealthCache.clear();
   window.calls158=[];window.pending158=[];window.acks158=[];window.homes158=[];window.fail158=false;
   healthPlugin=()=>({readWorkout:async args=>{calls158.push(args);if(fail158)throw Error('Átmeneti olvasási hiba');return structuredClone(fixture158.health240);}});
   window.Capacitor={isNativePlatform:()=>true,Plugins:{WearSync:{clear:async()=>{},publish:async()=>{},
    pendingCommands:async()=>({commands:pending158.slice()}),ackCommand:async({commandId})=>{acks158.push(commandId);pending158=pending158.filter(c=>c.commandId!==commandId);},
    publishHome:async({snapshot})=>homes158.push(snapshot)}}};
   // A global preference must not turn another app's workout into Samsung data.
   state.health.source='com.sec.android.app.shealth';go('history');
  });
  await page.locator('details.rf263-history > summary').first().click();
  const block=page.locator('[data-rf-history-health]');
  await page.waitForFunction(()=>document.querySelector('[data-rf-history-health]')?.textContent.includes('390 kcal'));
  assert.match(await block.innerText(),/390 kcal/);assert.doesNotMatch(await block.innerText(),/49 kcal|Samsung Health/);
  assert.equal(await block.locator('.stat').filter({hasText:'Lépések'}).locator('strong').innerText(),'0');
  const before=await page.evaluate(()=>JSON.stringify(history()[0].healthWear129));
  await block.getByRole('button',{name:'Health Connect',exact:true}).click();
  assert.match(await block.innerText(),/49 kcal/);assert.match(await block.innerText(),/Health Connect · Fitbit/);
  assert.doesNotMatch(await block.innerText(),/390 kcal|Samsung Health/);
  assert.equal(await page.evaluate(()=>JSON.stringify(history()[0].healthWear129)),before,'selection is read-only');
  await page.evaluate(()=>{
   const row=history()[0];row.health240.sources=['com.sec.android.app.shealth'];row.health240.sourceLabels={'com.sec.android.app.shealth':'Samsung Health'};db.set('history',[row]);rfHistoryHealthCache.clear();rfHistoryHealthPaint(0);
  });
  assert.match(await block.innerText(),/Health Connect · Samsung Health/);
  assert.equal(await block.getByRole('button',{name:'Health Connect',exact:true}).count(),1,'origin and reading channel differ');
  await page.evaluate(()=>{
   const row=history()[0];row.health240.provider='samsung_health';db.set('history',[row]);rfHistoryHealthPaint(0);
  });
  assert.equal(await block.getByRole('button',{name:'Samsung Health · közvetlen',exact:true}).count(),1);
  await page.evaluate(()=>{
   const row=history()[0];row.health240.metricProviders={steps:'health_connect',heartRate:'samsung_health'};db.set('history',[row]);rfHistoryHealthPaint(0);
  });
  assert.equal(await block.getByRole('button',{name:'Samsung Health / Health Connect',exact:true}).count(),1);
  // Keep the original disclosure mounted while its index moves from 0 to 1.
  await page.evaluate(()=>{
   const other={...structuredClone(fixture158),id:'other158',syncId:'other158',started:'2026-10-14T09:00:00Z',finished:'2026-10-14T09:01:00Z'};
   other.healthWear129={...other.healthWear129,workoutId:'other158',totalCalories:999};
   db.set('history',[other,history()[0]]);
   pending158=[{commandId:'late158',action:'healthSummary',workoutId:'source158',createdAt:1,healthSummary:{...fixture158.healthWear129,revision:5,totalCalories:400,partial:true}}];
   window.disclosure158=document.querySelector('details.rf263-history');window.scrollTo(0,200);
   window.scroll158=window.scrollY;
  });
  await page.evaluate(()=>TrainPilotWearSync.drainCommands());
  assert.equal(await page.evaluate(()=>history().length),2,'late arrival cannot duplicate the workout');
  assert.equal(await page.evaluate(()=>history()[1].healthWear129.totalCalories),400);
  assert.match(await block.innerText(),/49 kcal/,'late arrival preserves manual HC selection');
  assert.doesNotMatch(await block.innerText(),/400 kcal|999 kcal/);
  assert.equal(await page.evaluate(()=>disclosure158===document.querySelector('details.rf263-history')&&disclosure158.open),true,'no full journal re-render');
  assert.equal(await page.evaluate(()=>window.scrollY),await page.evaluate(()=>scroll158));
  assert.equal(await page.evaluate(()=>homes158.at(-1).recentWorkouts.find(x=>x.workoutId==='source158').wear.totalCalories),400,'Mini Journal projection receives the same revision');
  await block.getByRole('button',{name:'TrainPilot óra',exact:true}).click();
  assert.match(await block.innerText(),/400 kcal/);assert.match(await block.innerText(),/Részleges mérés/);
  assert.doesNotMatch(await block.innerText(),/999 kcal|49 kcal/);
  const callCount=await page.evaluate(()=>calls158.length);
  await page.locator('.rf-history-health-refresh').click();
  assert.equal(await page.evaluate(()=>calls158.length),callCount,'watch refresh does not read HC');
  await page.evaluate(async()=>{
   pending158=[{commandId:'stale158',action:'healthSummary',workoutId:'source158',createdAt:2,healthSummary:{...fixture158.healthWear129,revision:4,totalCalories:200}},
    {commandId:'foreign158',action:'healthSummary',workoutId:'source158',createdAt:3,healthSummary:{...fixture158.healthWear129,workoutId:'wrong158',revision:6,totalCalories:777}}];
   await TrainPilotWearSync.drainCommands();
  });
  assert.equal(await page.evaluate(()=>history()[1].healthWear129.totalCalories),400,'stale or foreign summary cannot replace the valid source');
  await block.getByRole('button',{name:'Samsung Health / Health Connect',exact:true}).click();
  await page.evaluate(()=>{fail158=true;});
  await page.locator('.rf-history-health-refresh').click();
  await page.waitForFunction(()=>document.querySelector('.tp158-refresh-error'));
  assert.match(await block.innerText(),/49 kcal/,'failed refresh retains valid saved measurements');
  assert.match(await block.innerText(),/Átmeneti olvasási hiba/);
  const lastCall=await page.evaluate(()=>calls158.at(-1));assert.equal(Date.parse(lastCall.start),Date.parse('2026-10-14T08:00:00Z'),'stable refresh target after list reorder');
  await page.locator('.rf-history-health-toggle').click();assert.equal(await page.locator('.rf-history-health-body').isVisible(),false);
  await page.locator('.rf-history-health-toggle').click();assert.equal(await page.locator('.rf-history-health-body').isVisible(),true);
  await page.evaluate(()=>{
   const row=history()[1];delete row.health240;
   db.set('history',[history()[0],row]);
   rfHistoryHealthCache.set(row.started,{workoutId:'other158',windowStart:row.started,windowEnd:row.finished,summary:{totalCalories:888}});
   rfHistoryHealthPaint(1);
  });
  assert.doesNotMatch(await block.innerText(),/888 kcal|49 kcal/,'foreign cache is not a source for this workout');
  await page.evaluate(()=>{
   const row=history()[1];rfHistoryHealthCache.set(row.started,{...rfHistoryHealthScope(row),windowEnd:'2026-10-14T08:46:00Z',summary:{totalCalories:888}});rfHistoryHealthPaint(1);
  });
  assert.doesNotMatch(await block.innerText(),/888 kcal/,'another measurement window is not reused');
  await page.evaluate(()=>{
   const row=history()[1];row.healthWear129={...row.healthWear129,workoutId:'foreign158'};db.set('history',[history()[0],row]);rfHistoryHealthPaint(1);
  });
  assert.match(await block.innerText(),/nem érkezett külön órás mérési összesítés/);assert.doesNotMatch(await block.innerText(),/400 kcal/);
  for(const width of [320,393]){
   await page.setViewportSize({width,height:800});
   await page.evaluate(()=>{
    db.set('history',[structuredClone(fixture158)]);rfHistoryHealthCache.clear();go('history');document.documentElement.classList.add('tp110-large-text');
   });
   await page.locator('details.rf263-history > summary').first().click();
   assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth<=window.innerWidth),true,'no horizontal overflow at '+width);
  }
  assert.deepEqual(errors,[]);
  console.log('PASS #158 shipped journal: truthful channel/origin, 45:36 fixture, 390 vs49 kcal, real zero, stable source/refresh/disclosure after reorder, late400 revision and Mini Journal, foreign/stale/window rejection, retained data on failed refresh, partial measurements and narrow/large-font layouts');
 }finally{await browser?.close();await new Promise(resolve=>server.close(resolve));}
})().catch(error=>{console.error(error);process.exitCode=1;});
