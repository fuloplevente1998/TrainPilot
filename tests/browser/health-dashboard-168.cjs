const assert=require('node:assert/strict');
const fs=require('node:fs'),path=require('node:path'),http=require('node:http');
const {chromium}=require('playwright');
(async()=>{
 const root=path.resolve('www');
 const server=http.createServer((req,res)=>{const file=path.resolve(root,'.'+new URL(req.url,'http://local').pathname.replace(/\/$/,'/index.html'));if(!file.startsWith(root+path.sep)){res.writeHead(403);res.end();return}fs.readFile(file,(e,d)=>{if(e){res.writeHead(404);res.end();return}res.setHeader('Content-Type',file.endsWith('.js')?'text/javascript':file.endsWith('.css')?'text/css':file.endsWith('.html')?'text/html':'text/plain');res.end(d)})});
 await new Promise(r=>server.listen(0,'127.0.0.1',r));let browser;
 try{
  browser=await chromium.launch({headless:true,executablePath:process.env.TRAINPILOT_CHROMIUM||undefined,args:['--no-sandbox']});
  const page=await browser.newPage({viewport:{width:393,height:873},locale:'hu-HU'});
  await page.goto(`http://127.0.0.1:${server.address().port}`);await page.waitForFunction(()=>window.TrainPilotBoot?.finished);
  await page.evaluate(()=>{
   const now=new Date(),days={};for(let i=6;i>=0;i--){const d=new Date(now);d.setDate(d.getDate()-i);days[rf240DayKey(d)]={averageHeartRate:68+i,restingHeartRate:i===0?58:57,maxHeartRate:118+i,minHeartRate:52+i,steps:10000+i*100}}
   db.set('weights',[{kg:65.9,date:new Date(Date.now()-20*86400000).toISOString()},{kg:66.3,date:new Date(Date.now()-10*86400000).toISOString()},{kg:66.5,date:new Date().toISOString()}]);
   db.set('recoveryHistory',[
    {day:rf240DayKey(now),sleepMinutes:429,hrvRmssdMs:54},
    {day:rf240DayKey(new Date(now.getTime()-86400000)),sleepMinutes:401,hrvRmssdMs:51}
   ]);
   window.__tp169LedgerBase=rf240Ledger;rf240Ledger=()=>({days,lastSyncAt:new Date().toISOString()});
   state.health=state.health||{};state.health.wellness={};
   rf200SetTheme('classicGreen');go('health');
  });
  await page.waitForSelector('main.rf263-health.tp168-health');await page.waitForTimeout(120);
  await page.evaluate(()=>TrainPilot168Health.decorate());await page.waitForTimeout(30);

  assert.equal(await page.locator('#rf235HealthCoach,.rf235-health-coach').count(),0,'#23 Health Coach must stay removed');
  assert.equal(await page.locator('main.rf263-health>.tp168-health-head').count(),0,'standalone Health heading/subtitle must stay removed');

  const titleLine=page.locator('.tp169-today-titleline');assert.equal(await titleLine.count(),1,'Today title/date line missing');
  assert.equal(await titleLine.locator('h2').count(),1,'Today title missing');
  assert.equal(await titleLine.locator('time.tp168-today-date').count(),1,'Today date must sit beside title');
  assert.ok(parseFloat(await titleLine.locator('time').evaluate(e=>getComputedStyle(e).fontSize))>=13,'Today date must use a larger readable size');
  const titleBox=await titleLine.locator('h2').boundingBox(),dateBox=await titleLine.locator('time').boundingBox();assert.ok(titleBox&&dateBox&&dateBox.y<titleBox.y+titleBox.height+5,'Today date must visually share the title line');

  assert.equal(await page.locator('.tp168-today-card .tp168-metric').count(),4,'Today status must expose four primary metrics');
  assert.equal(await page.locator('.tp168-today-card .tp168-status').count(),2,'recovery/resting pulse status row missing');

  const pulse=page.locator('.tp169-pulse-journal');assert.equal(await pulse.count(),1,'Pulse trend disclosure missing');
  assert.equal(await page.locator('main.rf263-health>details.tp168-pulse-panel').count(),0,'standalone Pulse panel must be removed');
  const miniPulsePath=await pulse.locator('.tp168-spark path').getAttribute('d');assert.match(miniPulsePath,/C/,'Pulse mini trend must use a smooth curve');
  await pulse.locator('.tp169-pulse-summary').click();await page.waitForTimeout(40);
  assert.equal(await pulse.locator('.tp168-pulse-grid>div').count(),4,'expanded Pulse trend must preserve pulse data');
  const recovery=pulse.locator('.tp168-recovery-history');assert.equal(await recovery.count(),1,'Recovery history disclosure missing');
  await recovery.locator('summary').click();await page.waitForTimeout(30);
  const recoveryText=await recovery.innerText();
  assert.match(recoveryText,/Alvás/i,'Recovery history must contain sleep history');
  assert.match(recoveryText,/HRV/i,'Recovery history must contain HRV history');
  assert.doesNotMatch(recoveryText,/Engedélyezett adattípusok|Források:\s*Health Connect|Pulzus\s*Engedélyezve/i,'Health Connect capabilities must not leak into Recovery history');

  const connect=page.locator('main.rf263-health>details.tp168-connect-panel');assert.equal(await connect.count(),1,'Health Connect panel missing');
  await connect.locator('summary').click();await page.waitForTimeout(20);
  assert.match(await connect.innerText(),/Health Connect|Engedély|Forrás|támogatott/i,'Health Connect diagnostics must remain in their own panel');

  const weight=page.locator('.tp168-weight-journal');assert.equal(await weight.count(),1,'inline weight journal missing');
  assert.match(await weight.locator('.tp168-weight-summary').innerText(),/66[,.]5 kg/,'current weight belongs in the journal header');
  await weight.locator('.tp168-weight-summary').click();await page.waitForTimeout(40);
  assert.equal(await page.locator('main.rf263-health>.tp168-body-card').count(),0,'standalone Body/Fitness card must be removed');
  const body=weight.locator('.tp169-body-inline');assert.equal(await body.count(),1,'Body/Fitness data must remain inside weight journal');
  assert.equal(await body.locator('.stat').count(),3,'Body/Fitness must contain only body fat, SpO2 and VO2max');
  const labels=await body.locator('.stat small').allInnerTexts();assert.deepEqual(labels.map(x=>x.trim()),['Testzsír','SpO₂','VO₂max']);
  assert.equal(await body.getByText('Testsúly',{exact:true}).count(),0,'duplicate Body/Fitness weight tile must be removed');
  assert.equal(await weight.locator('#rf251WeightJournal,.rf251-weight-btn').count(),0,'duplicate inner Weight journal button must be removed');

  assert.equal(await weight.locator('.tp168-weight-stats>div').count(),2,'weight summary stats must only keep change and min/max');
  assert.doesNotMatch(await weight.locator('.tp168-weight-stats').innerText(),/Aktuális/,'duplicate Current weight stat must be removed');
  const entry=weight.locator('details.tp169-weight-entry');assert.equal(await entry.count(),1,'New measurement disclosure missing');
  assert.equal(await entry.getAttribute('open'),null,'New measurement must be collapsed by default');
  await entry.locator('summary').click();assert.notEqual(await entry.getAttribute('open'),null,'New measurement must expand in place');
  assert.equal(await entry.locator('#tp168WeightKg').count(),1);assert.equal(await entry.locator('#tp168WeightDate').count(),1);
  const weightPath=await weight.locator('.tp169-weight-curve path').getAttribute('d');assert.match(weightPath,/C/,'weight graph must use a smooth curve');

  await page.evaluate(()=>rf215WeightScreen());await page.waitForTimeout(60);
  assert.equal(await page.locator('main.rf263-health.tp168-health').count(),1,'weight shortcut must remain on the new Health dashboard');
  assert.equal(await page.locator('.tp168-weight-journal.tp168-open').count(),1,'weight shortcut must open the inline journal');

  await page.evaluate(()=>{const k=rf240DayKey(new Date());rf240Ledger=()=>({days:{[k]:{averageHeartRate:75,restingHeartRate:0,steps:10523}}});TrainPilot168Health.decorate()});await page.waitForTimeout(30);
  const resting=await page.locator('.tp168-resting').innerText();assert.doesNotMatch(resting,/0\s*bpm/i,'0 bpm must never be presented as a real resting heart-rate measurement');

  const matte=await page.evaluate(()=>({today:getComputedStyle(document.querySelector('.tp168-today-card')).boxShadow,nav:getComputedStyle(document.querySelector('.top.tp154-nav-grid .active')).boxShadow,family:document.documentElement.dataset.tpThemeFamily}));
  assert.equal(matte.family,'basic');assert.equal(matte.today,'none');assert.equal(matte.nav,'none');
  await page.evaluate(()=>rf200SetTheme('green'));await page.waitForTimeout(60);
  const vivid=await page.evaluate(()=>({today:getComputedStyle(document.querySelector('.tp168-today-card')).boxShadow,nav:getComputedStyle(document.querySelector('.top.tp154-nav-grid .active')).boxShadow,family:document.documentElement.dataset.tpThemeFamily}));
  assert.equal(vivid.family,'vivid');assert.notEqual(vivid.today,'none');assert.notEqual(vivid.nav,'none');

  assert.equal(await page.locator('main.rf263-health>details.tp168-more-panel').count(),1,'More Health panel missing');
  assert.equal(await page.locator('main.rf263-health>details.tp155-health-bottom-panel').count(),2,'only More Health and Health Connect should remain as bottom panels');
  assert.equal(await page.locator('main.rf263-health>.rf263-sync-card.tp168-sync-tail').count(),1,'Health sync controls must remain available');

  let overflow=await page.evaluate(()=>({sw:document.documentElement.scrollWidth,cw:document.documentElement.clientWidth}));assert.ok(overflow.sw<=overflow.cw+1,'393px Health dashboard overflow: '+JSON.stringify(overflow));
  await page.setViewportSize({width:320,height:640});await page.waitForTimeout(60);overflow=await page.evaluate(()=>({sw:document.documentElement.scrollWidth,cw:document.documentElement.clientWidth}));assert.ok(overflow.sw<=overflow.cw+1,'320px Health dashboard overflow: '+JSON.stringify(overflow));
  console.log('PASS TrainPilot 1.6.9 minor2 compact Health cleanup');
 }finally{if(browser)await browser.close();await new Promise(r=>server.close(r))}
})().catch(e=>{console.error(e);process.exit(1)});
