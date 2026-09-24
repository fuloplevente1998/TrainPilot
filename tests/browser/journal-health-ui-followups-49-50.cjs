'use strict';
const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),http=require('node:http'),{chromium}=require('playwright');
const root=path.resolve('www');
const server=http.createServer((req,res)=>{let p=new URL(req.url,'http://local').pathname;if(p==='/')p='/index.html';const file=path.resolve(root,'.'+p);if(!file.startsWith(root+path.sep)){res.writeHead(403);return res.end()}fs.readFile(file,(e,d)=>{if(e){res.writeHead(404);return res.end()}res.setHeader('Content-Type',file.endsWith('.js')?'text/javascript':file.endsWith('.css')?'text/css':file.endsWith('.html')?'text/html':'text/plain');res.end(d)})});
(async()=>{await new Promise(r=>server.listen(0,'127.0.0.1',r));let browser;try{
 browser=await chromium.launch({headless:true,executablePath:process.env.TRAINPILOT_CHROMIUM||undefined,args:['--no-sandbox']});
 const page=await browser.newPage({viewport:{width:393,height:873},locale:'hu-HU',timezoneId:'Europe/Budapest'}),errors=[];
 page.on('pageerror',e=>errors.push(String(e)));
 await page.goto('http://127.0.0.1:'+server.address().port+'/');await page.waitForFunction(()=>window.TrainPilotBoot?.finished);
 await page.evaluate(()=>{
  db.set('language','hu');
  const started=new Date(),finished=new Date(started.getTime()+40*60000);
  db.set('history',[{id:'issue49-first-open',workout:'A',dayId:'A',programId:'home-basic',programName:'First-open picker',started:started.toISOString(),finished:finished.toISOString(),exercises:[{id:'db-squat',loadType:'per_hand',repUnit:'ism.',sets:[{set:1,weight:10,reps:'10',done:true}]}],photos:[]}]);state.session=null;
 });
 for(const width of [320,360,393,412]){
  await page.setViewportSize({width,height:873});
  await page.evaluate(()=>{state.tp3HistoryOpenKey=null;go('history')});
  const workout=page.locator('details.rf263-history').first();await workout.locator(':scope > summary').click();
  const add=workout.locator('details.tp3-add-exercise');
  // The native select must already be upgraded by the first lazy hydration.
  const ready=await add.evaluate(el=>{
   const select=el.querySelector('select.field[data-tp3-add-exercise]'),wrap=select?.closest('.tp-select');
   return {enhanced:select?.dataset.tpCustomSelect,wrap:!!wrap,hidden:select?getComputedStyle(select).pointerEvents:null,menus:wrap?.querySelectorAll('.tp-select-menu').length};
  });
  assert.equal(ready.enhanced,'1','picker must be enhanced before first tap at '+width);
  assert.equal(ready.wrap,true,'branded dropdown missing at '+width);
  assert.equal(ready.hidden,'none','native WebView select must not intercept first tap at '+width);
  assert.equal(ready.menus,1,'one custom listbox per expanded workout');
  await add.locator(':scope > summary').click();
  await add.locator('.tp-select-trigger').click();
  const layout=await add.evaluate(el=>{
   const body=el.querySelector('.tp3-add-exercise-body'),trigger=el.querySelector('.tp-select-trigger'),menu=el.querySelector('.tp-select-menu'),labels=[...menu.querySelectorAll('.tp-select-option .tp-select-label')],long=labels.find(x=>x.textContent.includes('Román'))||labels.find(x=>x.textContent.length>20);
   const b=body.getBoundingClientRect(),t=trigger.getBoundingClientRect(),m=menu.getBoundingClientRect(),l=long?getComputedStyle(long):null;
   return {bodyW:b.width,triggerW:t.width,menuW:m.width,visible:getComputedStyle(menu).visibility,labelWhiteSpace:l?.whiteSpace,labelOverflow:l?.overflow,optionCount:labels.length,columns:getComputedStyle(body).gridTemplateColumns};
  });
  assert.equal(layout.visible,'visible','branded menu must open on first click at '+width);
  assert.ok(layout.menuW>=layout.triggerW-2,'menu must be as wide as its full-row trigger at '+width+': '+JSON.stringify(layout));
  assert.ok(layout.menuW>=Math.min(260,width-85),'exercise names need wider responsive menu at '+width+': '+JSON.stringify(layout));
  assert.equal(layout.labelWhiteSpace,'normal','long exercise names must wrap rather than truncate');
  assert.equal(layout.labelOverflow,'visible','no ellipsis truncation for long menu options');
  assert.ok(layout.optionCount>4,'exercise library options must remain available');
  if(width===393){
   const choices=add.locator('.tp-select-option[data-value]:not([data-value=""])');
   await choices.first().click();
   const chosen=await add.locator('select.field[data-tp3-add-exercise]').inputValue();assert.ok(chosen,'custom option must update underlying select');
   await add.getByRole('button',{name:/Gyakorlat hozzáadása/}).click();
   await page.waitForSelector('details.tp3-history-ex-new[open] .tp3-exercise-inline-editor');
   assert.equal(await page.locator('details.tp3-history-ex-new[open]').count(),1,'new exercise flow must keep accepted local editor');
   // Keep subsequent viewport tests on the original stored workout.
   await page.evaluate(()=>{state.tp3HistoryExerciseEdit=null});
  }
 }
 await page.evaluate(()=>{
  const now=new Date(),days={};for(let offset=6;offset>=0;offset--){const d=new Date(now);d.setDate(now.getDate()-offset);days[rf240DayKey(d)]={averageHeartRate:70+offset,restingHeartRate:54,maxHeartRate:142,minHeartRate:48}}
  rf240Ledger=()=>({version:1,days,lastSyncAt:now.toISOString()});
  db.set('weights',[{kg:66.5,date:now.toISOString()}]);state.health=state.health||{};state.health.wellness={};go('health');
 });
 await page.waitForSelector('main.rf263-health .tp169-pulse-journal .tp5-pulse-trend-section');await page.waitForTimeout(90);
 const pulse=page.locator('main.rf263-health .tp169-pulse-journal'),button=pulse.locator('.tp169-pulse-summary');
 await page.evaluate(()=>{
  window.__pulsePanel=document.querySelector('.tp169-pulse-journal');
  window.__pulseTrend=window.__pulsePanel.querySelector('.tp5-pulse-trend-section');
  window.__pulseToday=window.__pulsePanel.querySelector('.tp5-pulse-today');
 });
 for(let i=0;i<4;i++){
  await button.click();await page.waitForTimeout(30);
  const actual=await page.evaluate(()=>{
   const p=document.querySelector('.tp169-pulse-journal');
   return {samePanel:p===window.__pulsePanel,sameTrend:p.querySelector('.tp5-pulse-trend-section')===window.__pulseTrend,sameToday:p.querySelector('.tp5-pulse-today')===window.__pulseToday,open:p.classList.contains('tp169-open'),aria:p.querySelector('.tp169-pulse-summary')?.getAttribute('aria-expanded'),trendCount:p.querySelectorAll('.tp5-pulse-trend-section').length,dayCount:p.querySelectorAll('.tp5-pulse-day').length,legacyChartVisible:[...p.querySelectorAll(':scope > .tp169-pulse-body > .tp169-pulse-chart')].some(x=>getComputedStyle(x).display!=='none')};
  });
  assert.equal(actual.samePanel,true,'pulse disclosure must not rebuild the parent panel');
  assert.equal(actual.sameTrend,true,'pulse disclosure must keep the decorated 7-day chart, not flash the legacy chart');
  assert.equal(actual.sameToday,true,'pulse disclosure must preserve today data DOM');
  assert.equal(actual.open,i%2===0,'pulse toggles first-open and repeat-open in place');
  assert.equal(actual.aria,String(actual.open),'pulse accessible expanded state must match');
  assert.equal(actual.trendCount,1);assert.equal(actual.dayCount,7);
  assert.equal(actual.legacyChartVisible,false,'legacy chart must never become visible during open');
 }
 assert.match(await pulse.locator('.tp5-pulse-source').innerText(),/Health Connect.*healthLedgerV1.*averageHeartRate/i);
 await page.evaluate(()=>{go('home');go('health')});await page.waitForSelector('main.rf263-health .tp5-pulse-trend-section');
 await page.locator('.tp169-pulse-summary').click();
 assert.equal(await page.locator('.tp169-pulse-journal.tp169-open .tp5-pulse-trend-section').count(),1,'pulse still opens correctly after route navigation');
 const weight=page.locator('.tp168-weight-journal');await weight.locator('.tp168-weight-summary').click();assert.equal(await page.locator('.tp168-weight-journal.tp168-open').count(),1,'weight disclosure must remain functional');
 assert.deepEqual(errors,[],'page errors: '+errors.join('\n'));
 console.log('PASS #49 first-open custom dropdown, full-width wrapped options at 320/360/393/412 px; #50 stable pulse disclosure DOM, 7-day semantics and weight parity.');
}finally{await browser?.close();await new Promise(r=>server.close(r))}})().catch(e=>{console.error(e);process.exit(1)});
