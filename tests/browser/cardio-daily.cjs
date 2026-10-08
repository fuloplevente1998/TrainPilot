'use strict';
const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),http=require('node:http'),{chromium}=require('playwright');
const root=path.resolve('www'),server=http.createServer((req,res)=>{
 const file=path.resolve(root,'.'+new URL(req.url,'http://local').pathname.replace(/\/$/,'/index.html'));
 if(!file.startsWith(root+path.sep)){res.writeHead(403);return res.end();}
 fs.readFile(file,(error,data)=>{if(error){res.writeHead(404);return res.end();}res.setHeader('Content-Type',file.endsWith('.js')?'text/javascript':file.endsWith('.css')?'text/css':file.endsWith('.html')?'text/html':'application/octet-stream');res.end(data);});
});
function seed(){
 const now=new Date(),days={};for(let i=0;i<14;i++){const d=new Date(now);d.setDate(d.getDate()-i);days[rf240DayKey(d)]={steps:6000+i*100,provider:'samsung_health',readAt:now.toISOString()};}
 window.dailyFixture={days,preferences:{provider:'samsung_health'},calls:[],missing:false,inconsistent:false,delay:null};
 rf240Ledger=()=>({days,preferences:dailyFixture.preferences,lastSyncAt:now.toISOString()});
 db.set('history',[
  {id:'run',started:'2026-10-08T08:00:00+02:00',finished:'2026-10-08T09:00:00+02:00',exercises:[{...byId('running'),sets:[{done:true,reps:'1800',distanceMeters:5000}]}]},
  {id:'overlap',started:'2026-10-08T08:30:00+02:00',finished:'2026-10-08T10:00:00+02:00',exercises:[]},
  {id:'night',started:'2026-10-07T23:30:00+02:00',finished:'2026-10-08T00:30:00+02:00',exercises:[]}
 ]);
 isNative=()=>true;
 const readStepsWindow=provider=>async args=>{
  dailyFixture.calls.push({provider,...args});if(dailyFixture.delay)await dailyFixture.delay;
  const start=new Date(args.start),duration=Date.parse(args.end)-+start,part=duration<=7200000;
  return {steps:part?(dailyFixture.missing?null:dailyFixture.inconsistent?20000:duration===7200000?1500:1000):days[rf240DayKey(start)]?.steps??null,
   provider,activityOrigin:args.origin||'',start:args.start,end:args.end,permissions:{READ_STEPS:true},warnings:[]};
 };
 // Native startup also checks Google connection status. Keep that unrelated
 // bridge present with no signed-in account when advancing the test clock.
 window.Capacitor=window.Capacitor||{};window.Capacitor.Plugins={...window.Capacitor.Plugins,GoogleSync:{status:async()=>({profile:null})},SamsungHealth:{readStepsWindow:readStepsWindow('samsung_health')},HealthBridge:{readStepsWindow:readStepsWindow('health_connect')}};
 tp107CardioActivity='';tp107OpenCardio();
}
(async()=>{
 await new Promise(r=>server.listen(0,'127.0.0.1',r));let browser;
 try{
  browser=await chromium.launch({headless:true,args:['--no-sandbox']});
  const page=await browser.newPage({viewport:{width:393,height:873},locale:'hu-HU',timezoneId:'Europe/Budapest',reducedMotion:'reduce'}),errors=[];
  page.on('pageerror',e=>errors.push(e.stack||e.message));await page.clock.setFixedTime(new Date('2026-10-08T12:00:00Z'));
  await page.addInitScript(()=>localStorage.setItem('repforge:onboarding128',JSON.stringify('skipped')));
  await page.goto('http://127.0.0.1:'+server.address().port);await page.waitForFunction(()=>TrainPilotBoot.finished);await page.evaluate(seed);
  const card=page.locator('.tp-cardio-daily');assert.equal(await card.count(),1);
  assert.equal(await page.locator('.tp107-record').count(),1,'Recorded running keeps its separate distance record');
  await page.evaluate(()=>tp107ChangeCardio({value:'running'}));assert.equal(await card.count(),0);assert.equal(await page.locator('.tp107-cardio select option[value="@daily-steps"]').count(),1,'Daily activity must stay selectable from another activity');
  await page.evaluate(()=>tp107ChangeCardio({value:''}));
  assert.equal(await card.locator('[data-daily-metric="outside"]').innerText(),'—','Daily total cannot identify workout steps by itself');
  await page.evaluate(()=>{tp107ChangeCardio({value:'@daily-steps'});});
  assert.equal(await page.locator('.tp107-cardio select').inputValue(),'@daily-steps');assert.equal(await page.locator('.tp107-record').count(),0);
  await card.locator('[data-daily-refresh]').click();await page.waitForFunction(()=>!document.querySelector('[data-daily-refresh]').disabled);
  assert.equal(await card.locator('[data-daily-metric="today"]').innerText(),'6000');
  assert.equal(await card.locator('[data-daily-metric="outside"]').innerText(),'3500');
  const todayCalls=await page.evaluate(()=>dailyFixture.calls.filter(c=>rf240DayKey(c.start)==='2026-10-08'));
  assert.equal(todayCalls.length,3,'Overlapping workouts must be merged before reading their steps');
  assert.ok(todayCalls.every(c=>c.provider==='samsung_health'));
  await page.clock.setFixedTime(new Date('2026-10-09T12:00:00Z'));await page.evaluate(()=>render());
  assert.equal(await card.locator('[data-daily-day="2026-10-08"] [data-daily-outside]').innerText(),'—','An earlier partial-day query must expire when that day ends');
  await page.clock.setFixedTime(new Date('2026-10-08T12:00:00Z'));await page.evaluate(()=>render());
  await page.evaluate(()=>{dailyFixture.missing=true;});await card.locator('[data-daily-refresh]').click();await page.waitForFunction(()=>!document.querySelector('[data-daily-refresh]').disabled);
  assert.equal(await card.locator('[data-daily-metric="outside"]').innerText(),'—');assert.match(await card.locator('[data-daily-status]').innerText(),/nem frissült/);
  await page.evaluate(()=>{dailyFixture.missing=false;dailyFixture.inconsistent=true;});await card.locator('[data-daily-refresh]').click();await page.waitForFunction(()=>!document.querySelector('[data-daily-refresh]').disabled);
  assert.equal(await card.locator('[data-daily-metric="outside"]').innerText(),'—','Inconsistent counters must not be clamped into a fake zero');
  await page.evaluate(()=>{dailyFixture.inconsistent=false;dailyFixture.preferences.provider='health_connect';const key=rf240DayKey(new Date());dailyFixture.days[key]={steps:6000,channel:'samsung_health',metricProviders:{steps:'health_connect'},stepsOrigin:'phone.origin',activityOrigin:'com.sec.android.app.shealth'};render();});
  assert.equal(await card.locator('[data-daily-metric="outside"]').innerText(),'—','Source change invalidates the previously measured split');
  await card.locator('[data-daily-refresh]').click();await page.waitForFunction(()=>!document.querySelector('[data-daily-refresh]').disabled);
  const fallback=await page.evaluate(()=>dailyFixture.calls.filter(c=>c.provider==='health_connect'));
  assert.equal(fallback.length,3);assert.ok(fallback.every(c=>c.origin==='phone.origin'),'Window and daily reads must share the actual fallback origin');
  await page.evaluate(()=>{db.set('history',[]);render();});assert.equal(await card.locator('[data-daily-metric="outside"]').innerText(),'6000','Deleted workouts must no longer exclude their steps');
  // A workout deleted during a request must not resurrect the old subtraction.
  await page.evaluate(()=>{db.set('history',[{started:'2026-10-08T08:00:00+02:00',finished:'2026-10-08T09:00:00+02:00',exercises:[]}]);dailyFixture.delay=new Promise(resolve=>dailyFixture.release=resolve);render();void TrainPilotCardioDaily.refresh();});
  await page.evaluate(()=>{db.set('history',[]);dailyFixture.delay=null;dailyFixture.release();});await page.waitForFunction(()=>!document.querySelector('[data-daily-refresh]').disabled);
  assert.equal(await card.locator('[data-daily-metric="outside"]').innerText(),'6000');
  for(const lang of ['hu','en','de','ro'])for(const width of [320,360,393,412]){
   await page.setViewportSize({width,height:873});await page.evaluate(lang=>{db.set('language',lang);render();},lang);
   assert.ok(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1),lang+'/'+width+' overflow');
   assert.equal(await card.locator('.tp-cardio-daily-row').count(),7);assert.equal(await page.locator('.tp107-cardio select').inputValue(),'@daily-steps');
  }
  if(process.env.TRAINPILOT_CARDIO_PREVIEW){
   const folder=path.resolve(process.env.TRAINPILOT_CARDIO_PREVIEW);fs.mkdirSync(folder,{recursive:true});
   await page.setViewportSize({width:393,height:1050});await page.evaluate(seed);await page.evaluate(async()=>{db.set('language','hu');rf200SetTheme('blue');tp107CardioActivity='@daily-steps';render();await TrainPilotCardioDaily.refresh();});
   await card.screenshot({path:path.join(folder,'daily-activity.png')});
   await page.evaluate(()=>{tp107ChangeCardio({value:''});});await page.locator('.tp107-cardio').screenshot({path:path.join(folder,'cardio-groups.png')});
  }
  assert.deepEqual(errors,[]);console.log('PASS daily cardio UI: group filtering, source-pinned windows, overlap, missing/inconsistent data, deletion/races and 16 localized layouts');
 }finally{await browser?.close();await new Promise(r=>server.close(r));}
})().catch(e=>{console.error(e);process.exitCode=1;});
