'use strict';
const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),http=require('node:http');
const {chromium}=require('playwright');
function seed(){
 const key=rf240DayKey(new Date()),days={};
 for(let i=0;i<7;i++){
  const date=new Date();date.setDate(date.getDate()-i);
  days[rf240DayKey(date)]={provider:'samsung_health',averageHeartRate:i===2?null:95+i,heartRateSamples:1200+i};
 }
 const old=new Date();old.setDate(old.getDate()-20);days[rf240DayKey(old)]={averageHeartRate:220};
 const future=new Date();future.setDate(future.getDate()+1);days[rf240DayKey(future)]={averageHeartRate:230};
 const previous=new Date();previous.setDate(previous.getDate()-1);
 days[rf240DayKey(previous)].metricProviders={averageHeartRate:'health_connect'};
 const measured=new Date();measured.setDate(measured.getDate()-3);
 days[rf240DayKey(measured)].bodyFatPercent=16.9;
 Object.assign(days[key],{steps:6594,totalCalories:2423,activeCalories:392,distanceMeters:1490,
  minHeartRate:54,maxHeartRate:162,restingHeartRate:58,heartRateSamples:2945,
  hrvRmssdMs:35,bloodPressureSystolic:126,bloodPressureDiastolic:82,bloodGlucoseMmolL:5.2,respiratoryRate:15,
  weightKg:66.5,weightTime:new Date().toISOString(),oxygenSaturationPercent:98,
  skeletalMuscleMassKg:29.6,bodyFatMassKg:11.2,totalBodyWaterLiters:40.1,fatFreeMassKg:54.8,
  basalMetabolicRateKcal:1554,bodyMassIndex:22.1,metricProviders:{averageHeartRate:'samsung_health',hrvRmssdMs:'health_connect'}});
 window.healthDetailFixture={days,key};rf240Ledger=()=>({version:1,days,lastSyncAt:new Date().toISOString()});
 db.set('weights',[{kg:67.5,date:measured.toISOString()},{kg:64,date:new Date().toISOString()}]);
 db.set('recoveryHistory',[{day:key,sleepMinutes:450,hrvRmssdMs:35,sleepEnd:new Date().toISOString()}]);
 go('health');
}
(async()=>{
 const root=path.resolve('www'),server=http.createServer((req,res)=>{
  const file=path.resolve(root,'.'+new URL(req.url,'http://local').pathname.replace(/\/$/,'/index.html'));
  if(!file.startsWith(root+path.sep)){res.writeHead(403);return res.end();}
  fs.readFile(file,(error,data)=>{if(error){res.writeHead(404);return res.end();}res.setHeader('Content-Type',file.endsWith('.js')?'text/javascript':file.endsWith('.css')?'text/css':file.endsWith('.html')?'text/html':'application/octet-stream');res.end(data);});
 });
 await new Promise(r=>server.listen(0,'127.0.0.1',r));let browser;
 try{
  browser=await chromium.launch({headless:true,args:['--no-sandbox']});
  const page=await browser.newPage({viewport:{width:393,height:873},locale:'hu-HU',timezoneId:'Europe/Budapest',reducedMotion:'reduce'}),errors=[];
  page.on('pageerror',e=>errors.push(e.message));await page.clock.setFixedTime(new Date('2026-10-08T12:00:00Z'));
  await page.addInitScript(()=>localStorage.setItem('repforge:onboarding128',JSON.stringify('skipped')));
  await page.goto('http://127.0.0.1:'+server.address().port);await page.waitForFunction(()=>TrainPilotBoot.finished);await page.evaluate(seed);
  const more=page.locator('.tp168-more-panel');assert.equal(await more.count(),1);
  assert.equal(await page.locator('details.tp-samsung-composition').count(),0);
  assert.equal(await more.locator('[data-health-group]>h3').count(),2);
  assert.equal(await more.evaluate(n=>n.open),false);
  assert.equal(await page.evaluate(()=>document.body.classList.contains('tp107-fixed-view')),true,'Closed Health stays compact with six Today metrics');
  assert.equal(await page.locator('.tp168-today-card .tp5-today-metric').count(),6);
  assert.match(await page.locator('.tp5-today-calories').innerText(),/Aktív kalória.*392 kcal/s);
  assert.equal(await more.locator('summary .tp168-chevron').innerText(),'›');
  await page.locator('.tp5-today-calories').click();assert.equal(await more.evaluate(n=>n.open),true);
  assert.match(await more.locator('[data-health-detail="activeCalories"]').innerText(),/392 kcal/);
  assert.match(await more.locator('[data-health-detail="totalCalories"]').innerText(),/2\s*423 kcal/);
  await more.locator('summary').click();await page.locator('.tp5-today-steps').click();assert.equal(await more.evaluate(n=>n.open),true);
  await page.evaluate(()=>TrainPilotHealthDetails.open('totalBodyWaterLiters'));
  assert.equal(await more.evaluate(n=>n.open),true);assert.equal(await page.locator('[data-health-detail="totalBodyWaterLiters"]').evaluate(n=>n===document.activeElement),true);
  assert.match(await more.locator('[data-health-detail="totalBodyWaterLiters"]').innerText(),/40,1 L/);
  await page.evaluate(()=>TrainPilotHealthDetails.open('bloodPressureSystolic'));assert.match(await more.locator('[data-health-detail="bloodPressureSystolic"]').innerText(),/126\/82 mmHg/);
  assert.match(await more.locator('[data-health-detail="bodyFatPercent"]').innerText(),/16,9 %/,'Older body fat remains dated in details');
  assert.match(await more.locator('[data-health-detail="oxygenSaturationPercent"]').innerText(),/98 %/);
  const before=await more.locator('[data-health-detail]').count();
  await page.evaluate(()=>{delete healthDetailFixture.days[healthDetailFixture.key].totalBodyWaterLiters;TrainPilot168Health.decorate();});
  assert.equal(await more.locator('[data-health-detail="totalBodyWaterLiters"]').count(),0);
  assert.equal(await more.locator('[data-health-detail]').count(),before-1);
  assert.equal(await page.locator('.tp168-today-card .tp5-today-metric').count(),6,'Dynamic details must not add Today tiles');
  await page.evaluate(()=>{healthDetailFixture.days[healthDetailFixture.key].totalBodyWaterLiters=40.1;TrainPilot168Health.decorate();});
  assert.equal(await more.locator('[data-health-detail="totalBodyWaterLiters"]').count(),1);
  await page.locator('.tp5-today-pulse').click();const pulse=page.locator('.tp169-pulse-journal');
  assert.equal(await pulse.locator('button').first().getAttribute('aria-expanded'),'true');
  assert.match(await pulse.locator('.tp-health-pulse-meta').innerText(),/Samsung Health.*2\s*945/);
  assert.match(await pulse.locator('.tp5-pulse-source').innerText(),/Samsung Health.*Health Connect/);
  assert.doesNotMatch(await pulse.locator('.tp5-pulse-source').innerText(),/healthLedgerV1|averageHeartRate/);
  const trend=await page.evaluate(()=>tp5PulseTrendRows().map(r=>r.value));assert.equal(trend.length,7);assert.equal(trend[4],null);
  assert.ok(!trend.includes(220)&&!trend.includes(230));
  const chart=await pulse.locator('.tp5-pulse-large-svg path').getAttribute('d');assert.equal((chart.match(/M/g)||[]).length,2,'The chart must leave a gap across an unmeasured day');
  await page.evaluate(()=>TrainPilotHealthDetails.open('weightKg'));assert.equal(await page.locator('.tp168-weight-summary').getAttribute('aria-expanded'),'true');
  assert.match(await page.locator('.tp168-weight-journal [data-health-detail="weightKg"]').innerText(),/66,5 kg.*Samsung Health/s,'Weight shortcut must reveal its actual health reading instead of an unrelated manual measurement');
  for(const lang of ['hu','en','de','ro'])for(const width of [320,360,393,412]){
   await page.setViewportSize({width,height:873});await page.evaluate(lang=>{db.set('language',lang);go('health');TrainPilot168Health.decorate();},lang);
   if(!await more.evaluate(n=>n.open))await more.locator('summary').click();
   assert.ok(await page.evaluate(()=>document.documentElement.scrollWidth-innerWidth)<=1,lang+'/'+width+' overflow');
   assert.equal(await more.locator('[data-health-group]>h3').count(),2);
   assert.ok(await more.evaluate(n=>n.scrollHeight<=n.clientHeight+1),lang+'/'+width+' must not clip the expanded body composition group');
  }
  if(process.env.TRAINPILOT_HEALTH_PREVIEW){
   const folder=path.resolve(process.env.TRAINPILOT_HEALTH_PREVIEW);fs.mkdirSync(folder,{recursive:true});
   await page.setViewportSize({width:393,height:1440});await page.evaluate(()=>{db.set('language','hu');rf200SetTheme('blue');TrainPilot168Health.moreOpen=false;TrainPilot168Health.pulseOpen=false;TrainPilot168Health.weightOpen=false;go('health');TrainPilot168Health.decorate();});
   await page.locator('.tp168-today-card').screenshot({path:path.join(folder,'health-overview-compact.png')});
   if(!await more.evaluate(n=>n.open))await more.locator('summary').click();await more.screenshot({path:path.join(folder,'health-details-compact.png')});
   await page.locator('.tp5-today-pulse').click();await pulse.screenshot({path:path.join(folder,'health-pulse-compact.png')});
  }
  assert.deepEqual(errors,[]);console.log('PASS shared Health details: metric navigation, dated dynamic values, provider provenance, pulse gaps and 16 localized layouts');
 }finally{await browser?.close();await new Promise(r=>server.close(r));}
})().catch(error=>{console.error(error);process.exitCode=1;});
