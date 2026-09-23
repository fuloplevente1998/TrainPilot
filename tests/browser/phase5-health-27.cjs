const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),http=require('node:http'),{chromium}=require('playwright');
const root=path.resolve('www');
const server=http.createServer((req,res)=>{let p=new URL(req.url,'http://local').pathname;if(p==='/')p='/index.html';const file=path.resolve(root,'.'+p);if(!file.startsWith(root+path.sep)){res.writeHead(403);return res.end()}fs.readFile(file,(e,d)=>{if(e){res.writeHead(404);return res.end()}res.setHeader('Content-Type',file.endsWith('.js')?'text/javascript':file.endsWith('.css')?'text/css':file.endsWith('.html')?'text/html':'text/plain');res.end(d)})});
(async()=>{
 await new Promise(r=>server.listen(0,'127.0.0.1',r));const browser=await chromium.launch({headless:true}),page=await browser.newPage({viewport:{width:393,height:852},locale:'hu-HU'}),native=[];
 page.on('dialog',async d=>{native.push({type:d.type(),message:d.message()});await d.dismiss()});
 try{
  await page.goto('http://127.0.0.1:'+server.address().port+'/');await page.waitForFunction(()=>window.TrainPilotBoot?.finished);
  await page.evaluate(()=>{
   db.set('language','hu');document.documentElement.lang='hu';
   const now=new Date();now.setHours(12,0,0,0);const days={};
   const stale=new Date(now);stale.setDate(stale.getDate()-30);days[rf240DayKey(stale)]={averageHeartRate:199};
   for(let off=6;off>=0;off--){const d=new Date(now);d.setDate(now.getDate()-off);const i=6-off;days[rf240DayKey(d)]={averageHeartRate:i===1?0:(i===3?null:70+i),restingHeartRate:i===6?0:56+i,maxHeartRate:120+i,minHeartRate:50+i,steps:8000+i*100}}
   window.__tp5LedgerBase=rf240Ledger;rf240Ledger=()=>({version:1,days,lastSyncAt:new Date().toISOString()});
   const date=new Date(now);date.setDate(date.getDate()-2);db.set('weights',[{id:'tp5-weight',kg:66.5,date:date.toISOString()}]);
   state.health=state.health||{};state.health.wellness={};go('health');
  });
  await page.waitForSelector('main.rf263-health.tp168-health');await page.evaluate(()=>TrainPilot168Health.decorate());await page.waitForTimeout(60);
  assert.equal(await page.evaluate(()=>TrainPilotPhase5?.version),'phase5-health-27-r1');
  const rows=await page.evaluate(()=>tp5PulseTrendRows().map(x=>({key:x.key,value:x.value})));
  assert.equal(rows.length,7,'trend must represent exactly seven calendar days');
  assert.equal(rows.some(x=>x.value===199),false,'stale older readings must not enter the seven-day trend');
  assert.ok(rows.some(x=>x.value===null),'zero/missing average heart rate must remain missing');
  const pulse=page.locator('.tp169-pulse-journal');assert.equal(await pulse.locator('.tp5-pulse-today').count(),1,'today pulse section missing');assert.equal(await pulse.locator('.tp5-pulse-trend-section').count(),1,'7-day trend section missing');
  const source=await pulse.locator('.tp5-pulse-source').innerText();assert.match(source,/Health Connect/i);assert.match(source,/healthLedgerV1/i);assert.match(source,/averageHeartRate/i);
  assert.doesNotMatch(await pulse.innerText(),/\b0\s*bpm\b/i,'zero pulse must not render as a real measurement');
  assert.equal(await pulse.locator('.tp5-pulse-day').count(),7,'trend must show seven daily slots');

  const weight=page.locator('.tp168-weight-journal');await weight.locator('.tp168-weight-summary').click();await page.waitForTimeout(30);
  await weight.locator('.tp168-weight-recent button').first().click();
  const dialog=page.locator('#tp2628Dialog[data-tp5-weight-edit="1"]');await dialog.waitFor({state:'visible'});
  const input=dialog.locator('[data-tp2629-input]');assert.equal(Number(await input.inputValue()),66.5);await input.fill('67.2');await dialog.locator('[data-tp2628-confirm]').click();await dialog.waitFor({state:'detached'});
  assert.equal(await page.evaluate(()=>Number(weights().find(x=>x.id==='tp5-weight')?.kg)),67.2,'edited weight must persist in storage');
  await page.evaluate(()=>TrainPilot168Health.decorate());await page.waitForTimeout(20);
  await page.locator('.tp168-weight-journal .tp168-weight-recent button').first().click();await dialog.waitFor({state:'visible'});
  assert.equal(Number(await dialog.locator('[data-tp2629-input]').inputValue()),67.2,'reopened editor must show persisted edited weight');
  await dialog.locator('[data-tp2628-cancel]').last().click();await dialog.waitFor({state:'detached'});
  assert.deepEqual(native,[],'Phase 5 weight edit must not open native WebView dialogs');

  for(const width of [320,412]){await page.setViewportSize({width,height:700});await page.waitForTimeout(30);const o=await page.evaluate(()=>({sw:document.documentElement.scrollWidth,cw:document.documentElement.clientWidth}));assert.ok(o.sw<=o.cw+1,'Phase 5 Health overflow at '+width+': '+JSON.stringify(o))}
  console.log('PASS Phase 5 browser: seven-calendar-day pulse source + persistent compact weight editor.');
 }finally{await browser.close();server.close()}
})().catch(e=>{console.error(e);process.exit(1)});
