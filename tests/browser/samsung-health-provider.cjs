'use strict';
const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),http=require('node:http');
const {chromium}=require('playwright');
function fixture(){
 localStorage.setItem('repforge:onboarding128',JSON.stringify('skipped'));
 const HC='health_connect',SH='samsung_health',key='test:samsung-journal';
 let saved=JSON.parse(localStorage.getItem(key)||'null')||{preferences:{provider:HC,autoOnOpen:false,hcDailySource:'auto'},hc:{},samsung:{}};
 const persist=()=>localStorage.setItem(key,JSON.stringify(saved));
 const projection=()=>{const days={};for(const day of new Set([...Object.keys(saved.hc),...Object.keys(saved.samsung)])){
  const hc=saved.hc[day],sh=saved.samsung[day],direct=saved.preferences.provider===SH&&sh;
  days[day]={...(hc||{}),...(direct?sh:{}),channel:direct?SH:HC,source:'@aggregate',day,metricProviders:{steps:direct?SH:HC,hrvRmssdMs:HC}};
 }return {version:1,days,preferences:{...saved.preferences},nativeJournal:true};};
 window.samFixture={calls:[],denied:false};
 window.Capacitor={isNativePlatform:()=>true,getPlatform:()=>'android',Plugins:{
  GoogleSync:{status:async()=>({profile:null})},AppFeedback:{appearance:async()=>({fontScale:1})},BackupArchive:{recover:async()=>({})},
  HealthJournal:{initialize:async()=>projection(),getProjection:async()=>projection(),pendingRestore:async()=>({token:''}),
   setPreferences:async args=>{saved.preferences={...saved.preferences,...args.preferences};persist();return projection();},getDirtyDays:async()=>({days:[]}),
   saveSyncMeta:async()=>({}),readPage:async()=>({records:[],days:Object.values(projection().days).slice(-1),preferences:saved.preferences}),
   exportSnapshot:async()=>({schemaVersion:1,records:[],days:Object.entries(saved.samsung).map(([day,data])=>({day,data,channel:SH,source:'@aggregate'}))})},
  HealthBridge:{getStatus:async()=>({permissions:{READ_HEART_RATE:true,READ_HEART_RATE_VARIABILITY:true}}),
   readHealthDay:async args=>{const day=args.day||window.rf240DayKey(args.start);saved.hc[day]={day,steps:314,activeCalories:26,totalCalories:2423,hrvRmssdMs:35,readAt:new Date().toISOString()};persist();return saved.hc[day];},
   readTrainingWindow:async()=>({workoutCalories:26,workoutEnergyVersion:1,workoutEnergyProvider:HC,averageHeartRate:111,maxHeartRate:162,heartRateSamples:2945,totalCalories:373,steps:336,sources:['com.sec.android.app.shealth'],warnings:[]})},
  SamsungHealth:{getStatus:async()=>({permissions:{READ_WEIGHT:true,READ_BODY_FAT:true,READ_EXERCISE:true}}),
   requestRead:async()=>{samFixture.calls.push('permission');return {permissions:{READ_WEIGHT:!samFixture.denied,READ_BODY_FAT:!samFixture.denied,READ_EXERCISE:!samFixture.denied}};},
   readHealthDay:async args=>{const day=args.day||window.rf240DayKey(args.start);samFixture.calls.push('day');saved.samsung[day]={day,provider:SH,steps:6594,activeCalories:392,totalCalories:2423,bodyFatPercent:18.4,skeletalMuscleMassKg:28.2,totalBodyWaterLiters:37.5,readAt:new Date().toISOString(),warnings:[]};persist();return saved.samsung[day];},
   readTrainingWindow:async()=>({provider:SH,workoutCalories:392,workoutEnergyVersion:1,workoutEnergyProvider:SH,workoutEnergySources:['com.sec.android.app.shealth'],workoutEnergySourceLabels:{'com.sec.android.app.shealth':'Samsung Health direct'},source:'com.sec.android.app.shealth',sourceLabels:{'com.sec.android.app.shealth':'Samsung Health direct'},sources:['com.sec.android.app.shealth'],warnings:[]})},
  HealthBackground:{getStatus:async()=>({preferences:saved.preferences})}
 }};
 window.alert=()=>{};
}
(async()=>{
 const root=path.resolve('www'),server=http.createServer((req,res)=>{const file=path.resolve(root,'.'+new URL(req.url,'http://local').pathname.replace(/\/$/,'/index.html'));if(!file.startsWith(root+path.sep)){res.writeHead(403);return res.end();}fs.readFile(file,(e,data)=>{if(e){res.writeHead(404);return res.end();}res.setHeader('Content-Type',file.endsWith('.js')?'text/javascript':file.endsWith('.css')?'text/css':file.endsWith('.html')?'text/html':'application/octet-stream');res.end(data);});});
 await new Promise(r=>server.listen(0,'127.0.0.1',r));let browser;
 try{
  browser=await chromium.launch({headless:true,args:['--no-sandbox']});const page=await browser.newPage({viewport:{width:360,height:800},locale:'hu-HU',timezoneId:'Europe/Budapest'}),errors=[];
  page.on('pageerror',e=>errors.push(e.message));await page.addInitScript(fixture);await page.goto('http://127.0.0.1:'+server.address().port);await page.waitForFunction(()=>TrainPilotBoot.finished);
  await page.evaluate(async()=>{await TrainPilotHealthJournal.ready();go('health');});
  assert.equal(await page.locator('[data-health-provider]').count(),1);
  await page.evaluate(()=>samFixture.denied=true);await page.locator('[data-health-provider]').selectOption('samsung_health');
  await page.waitForFunction(()=>state.health.message?.includes('Nem kaptunk'));
  assert.equal(await page.evaluate(()=>TrainPilotSamsungHealth.selected()),'health_connect');assert.equal(await page.evaluate(()=>samFixture.calls.includes('day')),false);
  await page.evaluate(()=>samFixture.denied=false);await page.locator('[data-health-provider]').selectOption('samsung_health');
  await page.waitForFunction(()=>TrainPilotSamsungHealth.selected()==='samsung_health'&&!state.health.busy&&samFixture.calls.filter(x=>x==='day').length===30).catch(async error=>{console.error(await page.evaluate(()=>({selected:TrainPilotSamsungHealth.selected(),health:state.health,calls:samFixture.calls,report:db.get('healthSyncReport250'),ledger:rf240Ledger()})));throw error;});
  const day=await page.evaluate(()=>rf240Ledger().days[rf240DayKey(new Date())]);assert.equal(day.activeCalories,392);assert.equal(day.hrvRmssdMs,35);assert.equal(day.bodyFatPercent,18.4);
  assert.equal(await page.locator('.tp-samsung-composition').count(),1);await page.locator('.tp-samsung-composition>summary').click();assert.match(await page.locator('.tp-samsung-composition').innerText(),/Vázizomtömeg/);
  const read=await page.evaluate(()=>healthPlugin().readTrainingWindow({start:'2026-10-07T22:00:00Z',end:'2026-10-07T23:00:00Z'}));
  assert.equal(read.workoutCalories,392);assert.equal(read.maxHeartRate,162);assert.equal(read.totalCalories,373);assert.equal(read.metricProviders.workoutCalories,'samsung_health');assert.equal(read.metricProviders.averageHeartRate,'health_connect');
  await page.reload();await page.waitForFunction(()=>TrainPilotBoot.finished);await page.evaluate(()=>TrainPilotHealthJournal.ready());assert.equal(await page.evaluate(()=>TrainPilotSamsungHealth.selected()),'samsung_health');
  await page.evaluate(()=>{go('health');tp155R4OpenPanel('health-journal');});await page.waitForSelector('.tp120-day');
  assert.match(await page.locator('.tp120-day').innerText(),/Samsung Health/);assert.match(await page.locator('.tp120-day').innerText(),/Testzsír/);assert.match(await page.locator('.tp120-day').innerText(),/Health Connect/);
  for(const width of [320,360,393,412]){await page.setViewportSize({width,height:800});assert.ok(await page.evaluate(()=>document.documentElement.scrollWidth-innerWidth)<=1);}
  assert.deepEqual(errors,[]);console.log('PASS direct Samsung provider: denied permission rollback; 30-day sync; distinct calories and body units; HR fallback provenance; source survives restart; readable phone UI');
 }finally{await browser?.close();await new Promise(r=>server.close(r));}
})().catch(e=>{console.error(e);process.exitCode=1;});
